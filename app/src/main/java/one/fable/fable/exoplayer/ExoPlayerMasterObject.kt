package one.fable.fable.exoplayer

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import one.fable.fable.Fable
import one.fable.fable.convertOverDriveTimeMarkersToMillisecondsAsLong
import one.fable.fable.database.daos.AudiobookDao
import one.fable.fable.database.entities.*
import one.fable.fable.millisToMinutesSecondsString
import one.fable.fable.utils.extensions.appendDurationSummation
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.concurrent.TimeUnit


/* ExoPlayer Notes/URLs that I dont want to forget:

Building feature-rich media apps with ExoPlayer (Google I/O '18) https://www.youtube.com/watch?v=svdq1BWl4r8&t=784s

*/

object ExoPlayerMasterObject {
    //To build the ExoPlayer object, we need to first get the app shared preferences (or set the shared preferences to default values) for the rewind and fast forward amounts
    private val sharedPreferences : SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(Fable.instance).also {
            registerOnSharedPreferenceChangedListener(it)
        }
    //Get the shared preference values
    private val rewindAmountInSeconds = sharedPreferences.getString("rewind_seconds", "10") //Get the string representation of the rewind amount (in seconds). If it doesn't exist yet, set to the default of 10 seconds
    private val fastForwardAmountInSeconds = sharedPreferences.getString("fastforward_seconds", "30") //Get the string representation of the fast forward amount (in seconds). If it doesn't exist yet, set to the default of 30 seconds
    private var globalPlaybackSpeed = sharedPreferences.getInt("playback_speed_seekbar", 10) //Initialize the global playback speed with the sharedPref value (or set the default of 10 aka 1.0x)
        set(value) { field = value
            //Essentially we're building on the sharedPref listener and when we change the global playback speed, we'll also trigger this code (from the set)
            if(isAudiobookInitialized()){
                if (audiobook.playbackSpeed == null){ //Only change the playback speed if the audiobook itself doesn't have one already set (i.e. the audiobook uses the global speed)
                    setPlaybackSpeed(value)
                }
            }
        }

    //This listener can actually notify of any shared preference change.
    //In this case, we only care about the playback speed, as rewind and fast forward cannot be changed after the ExoPlayer object has been built
    private fun registerOnSharedPreferenceChangedListener(defaultSharedPreferences : SharedPreferences) {
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "playback_speed_seekbar" -> { //Listen for the global speed change
                    sharedPreferences?.getInt(key, 10)?.let { updatedGlobalPlaybackSpeed ->
                        globalPlaybackSpeed = updatedGlobalPlaybackSpeed //Invoke the custom setter (kinda like a "observer", but it doesn't depend on a viewLifecycleOwner)
                    }
                }
            }
        }
    }

    //Build the ExoPlayer
    @SuppressLint("UnsafeOptInUsageError")
    val exoPlayer : ExoPlayer = ExoPlayer.Builder(Fable.instance)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true
        )
        .setSeekBackIncrementMs(
            rewindAmountInSeconds?.toLongOrNull()?.let { TimeUnit.SECONDS.toMillis(it) } ?: 10000L //Convert the rewind time in seconds to milliseconds. Or assign the default of 10000 milliseconds (10 seconds)
        )
        .setSeekForwardIncrementMs(
            fastForwardAmountInSeconds?.toLongOrNull()?.let { TimeUnit.SECONDS.toMillis(it) } ?: 30000L //Convert the fast forward time in seconds to milliseconds. Or assign the default of 30000 milliseconds (30 seconds)
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()


    lateinit var audiobookDao: AudiobookDao

    var coverByteArray : ByteArray? = null

    var coverUri : Uri? = null
    var title : String? = null
    val chapterName = MutableLiveData<String>()

    lateinit var audiobook : Audiobook
    fun isAudiobookInitialized() = this::audiobook.isInitialized

    var audiobookTracks = listOf<Track>()
    var tracksWithChapters = listOf<TrackWithChapters>()

    val windowDurationsSummation = mutableListOf<Long>()

    private var loadBookJob : Job? = null

    fun loadAudiobook(audiobookToLoad: Audiobook){
        var loadNewBook = false
        if (!isAudiobookInitialized()){
            audiobook = audiobookToLoad
            loadNewBook = true
        } else {
            if(audiobook.audiobookId != audiobookToLoad.audiobookId){
                updateAudiobookObjectLocation()
                val previousAudiobook = audiobook
                CoroutineScope(Dispatchers.IO).launch{
                    updateAudiobook(previousAudiobook)
                }
                audiobook = audiobookToLoad
                loadNewBook = true
            }
        }

        //showToastCoroutine("Test Toast", Toast.LENGTH_SHORT)

        if (loadNewBook){
            setCoverByteArray()
            touchAndUpdateAudiobook()
            loadBookJob = Job()
            CoroutineScope(Dispatchers.Main).launch {
                loadTracks(audiobookToLoad.audiobookTitle)
            }
        }
    }

    val setCoverByteArrayJob = Job()
    fun setCoverByteArray(){
        coverByteArray = null
        audiobook.imgThumbnail?.let {
            val inputStream = Fable.instance.contentResolver.openInputStream(it)
            val byteArray = inputStream?.readBytes()
            coverByteArray = byteArray
        }

//        for (index in 0 until exoPlayer.mediaItemCount){
//        }

        audiobook.imgThumbnail?.let {uri ->
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(Fable.instance.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(Fable.instance.contentResolver, uri)
            }
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream)
            coverByteArray = stream.toByteArray()
        } ?: return
    }

    suspend fun getTracks(title: String){
        withContext(Dispatchers.IO){
            audiobookTracks = audiobookDao.getAudiobookTracks(title)
            val tracks = audiobookTracks
            checkFileForChapterInfo(tracks)

            tracksWithChapters = audiobookDao.getAudiobookTracksWithChapters(title)
        }
    }

    fun touchAndUpdateAudiobook(){
        if(isAudiobookInitialized()){
            audiobook.lastPlayedTimeStamp = System.currentTimeMillis()
            CoroutineScope(Dispatchers.IO).launch {
                updateAudiobook()
            }
        }
    }

    suspend fun loadTracks(title: String){
        exoPlayer.clearMediaItems()
        windowDurationsSummation.clear()
        windowDurationsSummation.add(0L)
        var invalidTrackDurationTrigger = false

        withContext(Dispatchers.IO){
            getTracks(title)
        }

        for (track in tracksWithChapters) {
            val trackUri = track.track.trackUri
            //val tempDurationSummation = windowDurationsSummation.lastOrNull() ?: 0L //Capture the previous summation

            if (track.chapters.isEmpty()){
                val mediaItem = MediaItem.fromUri(trackUri)
                exoPlayer.addMediaItem(mediaItem)

                if (!invalidTrackDurationTrigger){
                    track.track.trackLength?.let { trackLength ->
                        windowDurationsSummation.appendDurationSummation(trackLength)
                    } ?: run {
                        invalidTrackDurationTrigger = true
                    }
                }


            } else {
                for (index in 0 .. track.chapters.lastIndex){
                    val startPosition = track.chapters.getOrNull(index)?.chapterPositionMs ?: 0L
                    val endPosition = track.chapters.getOrNull(index + 1)?.chapterPositionMs ?: track.track.trackLength ?: C.TIME_END_OF_SOURCE //Get the start position of the next chapter item. If it doesn't exist return null. We'll get the end of the track in that case.

//                    endPosition?.let{
//                        windowDurationsSummation.add(tempDurationSummation.plus(endPosition)) //The window of windowDurationsSummation should match up with exoPlayer timeline windows
//                    }


                    val clippingConfiguration =
                        MediaItem.ClippingConfiguration.Builder().apply {
                            setStartPositionMs(startPosition)
                            setEndPositionMs(endPosition)
                        }.build()

                    val mediaMetaData = MediaMetadata.Builder().apply {
                        setAlbumTitle(audiobook.audiobookTitle)
                        setAlbumArtist(audiobook.audiobookAuthor)
                        setGenre(audiobook.audiobookGenre)
                        setArtworkUri(audiobook.imgThumbnail)
                        setTitle(track.chapters.getOrNull(index)?.chapterName?.trim() ?: track.track.trackTitle ?: audiobook.audiobookTitle)
                    }.build()

                    val clippedMediaItem =
                        MediaItem.Builder()
                            .setUri(trackUri)
                            .setClippingConfiguration(clippingConfiguration)
                            .setMediaMetadata(mediaMetaData)
                            .build()

                    exoPlayer.addMediaItem(clippedMediaItem)

                    if (endPosition == C.TIME_END_OF_SOURCE){
                        invalidTrackDurationTrigger = true
                        break
                    } else {
                        val chapterDuration = endPosition - startPosition
                        if (chapterDuration <= 0) {
                            invalidTrackDurationTrigger = true
                            break
                        }
                        windowDurationsSummation.appendDurationSummation(chapterDuration)
                    }
                }
            }
        }

        exoPlayer.prepare()
        exoPlayer.playWhenReady = false



        audiobook.playbackSpeed?.let { audiobookPlaybackSpeed ->
            setPlaybackSpeed(audiobookPlaybackSpeed)
        } ?: run {
            setPlaybackSpeed(globalPlaybackSpeed) //Reset to the global value
        }

        if (invalidTrackDurationTrigger) {
            windowDurationsSummation.clear()
            //TODO Delay movement to Audiobook fragment until entire duration is captured
            val durationListener = DurationPlayerListener({})
            exoPlayer.addListener(durationListener)
            exoPlayer.seekTo(0, 0)
        } else {
            exoPlayer.addListener(PlayerListener)

            val windowIndex = audiobook.windowIndex
            val windowLocation = audiobook.windowLocation

            if (windowIndex != null && windowLocation != null) {
                try {
                    exoPlayer.seekTo(windowIndex, windowLocation)
                } catch (e : IllegalSeekPositionException){
                    Timber.e(e)
                }
            }
        }

        //getMediaItemDurations()
    }

    fun getListOfTimelineWindows() : ArrayList<String> {
        val mediaItemTitles = ArrayList<String>()
        for (index in 0 .. exoPlayer.currentTimeline.windowCount) {
            val window = Timeline.Window()
            exoPlayer.currentTimeline.getWindow(index, window)
            mediaItemTitles.add(window.mediaItem.mediaMetadata.title.toString())
        }
        return mediaItemTitles
    }

    suspend fun checkFileForChapterInfo(tracks: List<Track>) {
        val scanTracks = Job()
        withContext(Dispatchers.IO + scanTracks) {
            track@ for (track in tracks) {
                if (track.validSource.isNotEmpty()) {
                    continue@track
                }

                if (!track.scannedSourceNames.contains("OverDrive")) {

                    val contentResolver = Fable.instance.contentResolver

                    var inputStream = contentResolver.openInputStream(track.trackUri)
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    var line = ""
                    val iterator = bufferedReader.lineSequence().iterator()
                    var iteratorCounter = 0

                    while (iterator.hasNext() && iteratorCounter <= 500 && line.isBlank()) {
                        val fileLine = iterator.next()
                        if (fileLine.contains("OverDrive MediaMarkers")) {
                            line = fileLine
                        }
                        iteratorCounter += 1
                    }
                    bufferedReader.close()

                    if (line.isNotBlank()) {
                        var markers = listOf<Marker>()

                        line = line.filter { !it.isIdentifierIgnorable() || !it.isISOControl() }

                        //For some reason, the first filter isn't removing some of the special chars.
                        //This will remove the rest of the unknown ones.
                        line = line.replace("\\uFFFD".toRegex(), "")

                        //This doesn't seem to work. I'm still seeing these chars in the line.
                        //Luckily, I don't think this is part of the string we actually are going to use.
                        line = line.replace("\\u001D".toRegex(), "")

                        //Log.i("AudiobookPlayerVMTracks", line)

                        val chaptersXML =
                                line.substringAfter("<Markers>").substringBefore("</Markers>")
                        //Log.i("AudiobookPlayerVMTracks", chaptersXML)

                        if (chaptersXML != line) {
                            markers = getOverDriveMediaMarkers(chaptersXML)
                        }

                        if (markers.isNotEmpty()) {
                            for (marker in markers) {
                                audiobookDao.insertChapter(
                                    Chapter(
                                        trackUri = track.trackUri,
                                        chapterName = marker.Name,
                                        chapterPositionMs = marker.Time
                                    )
                                )
                            }
                            track.validSource = "OverDrive"
                        }
                    }

                    //track.hasBeenScanned = true
                    track.scannedSourceNames += "OverDrive "
                    audiobookDao.updateTrack(track)
                }
            }
        }
    }

    fun getOverDriveMediaMarkers(xmlString: String) : List<Marker>{
        var markers: MutableList<Marker> = mutableListOf<Marker>()

        var chapterNameHolder : String? = null
        var timestampHolder : String?

        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlString))
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.TEXT){
                if (chapterNameHolder == null){
                    chapterNameHolder = parser.text
                } else {
                    timestampHolder = parser.text

                    //var timestampAsLong = SimpleDateFormat("mmm:ss.SSS")
                    //println(timestampAsLong.parse(timestampHolder))
                    //val timestampLong = convertTimeToMilliseconds(timestampHolder)
                    val timestampLong = timestampHolder.convertOverDriveTimeMarkersToMillisecondsAsLong()
                    //TimeUnit.MINUTES.toMillis(0)
                    //println(Timestamp.valueOf(timestampHolder))
                    markers.add(Marker(chapterNameHolder, timestampLong))
                    //println(chapterNameHolder + " added at timestamp: " + timestampHolder)
                    //println(chapterNameHolder + " added at timestamp LONG: " + timestampLong)
                    chapterNameHolder = null
                    timestampHolder = null
                }
            }
            eventType = parser.next()
        }

        return markers
    }


    //PROGRESS TRACKER CODE todo
    val progressTrackerHandler = Handler(Looper.getMainLooper())
    val progressTrackerRunnable = object : Runnable{
        override fun run() {
            updateAudiobookObjectLocation()
            progressTrackerHandler.postDelayed(this, 5000) //Update the audiobook location every 5 seconds
        }
    }

    fun getTimelineDuration() : Long{
        var timelineDuration = try {windowDurationsSummation[exoPlayer.currentMediaItemIndex]}
            catch(e: IndexOutOfBoundsException){0L}
        timelineDuration += exoPlayer.currentPosition
        return timelineDuration
    }

    //This is kind of a beta item. It's intended to cycle through each media item to get the exact length of it.
    class DurationPlayerListener(private val callback: () -> Unit) : Player.Listener{
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            if (exoPlayer.contentDuration != C.TIME_UNSET){
                windowDurationsSummation.appendDurationSummation(exoPlayer.contentDuration)
//                windowDurationsSummation.add(
//                    exoPlayer.contentDuration + (windowDurationsSummation.lastOrNull() ?: 0L)
//                )

                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                } else {
                    exoPlayer.removeListener(this)
                    exoPlayer.addListener(PlayerListener)

                    val windowIndex = audiobook.windowIndex
                    val windowLocation = audiobook.windowLocation

                    if (windowIndex != null && windowLocation != null) {
                        try {
                            exoPlayer.seekTo(windowIndex, windowLocation)
                        } catch (e : IllegalSeekPositionException){
                            Timber.e(e)
                        }
                    }
                }
//                else {
//                    exoPlayer.isLoading
//                }
            }
        }
    }

    //PLAYER EVENT LISTENER CODE todo
    //private val eventListener = PlayerEventListener()

    object PlayerListener : Player.Listener{
        //var isPlayingBool = false

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //isPlayingBool = isPlaying
            if (isPlaying){
                progressTrackerHandler.post(progressTrackerRunnable)
            } else {
                progressTrackerHandler.removeCallbacks(progressTrackerRunnable)
            }
            CoroutineScope(Dispatchers.IO).launch{
                updateAudiobook()
            }
            super.onIsPlayingChanged(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            chapterName.value = mediaItem?.mediaMetadata?.title.toString()
            updateAudiobookObjectLocation()
            CoroutineScope(Dispatchers.IO).launch{
                updateAudiobook()
            }
        }

    }

    fun updateAudiobookObjectLocation(){
        audiobook.windowIndex = exoPlayer.currentMediaItemIndex
        audiobook.windowLocation = exoPlayer.currentPosition
        audiobook.timelineDuration = getTimelineDuration()
        audiobook.lastPlayedTimeStamp = System.currentTimeMillis()

        if (audiobook.duration > 0L) {
            if (exoPlayer.currentMediaItemIndex == exoPlayer.currentTimeline.windowCount - 1 && (audiobook.timelineDuration.toDouble() / audiobook.duration.toDouble()) >= 0.95){
                audiobook.progressState = PROGRESS_FINISHED
            } else {
                audiobook.progressState = PROGRESS_IN_PROGRESS
            }
        }
    }

    suspend fun updateAudiobook(){
        withContext(Dispatchers.IO){
            audiobook?.let { audiobookDao.updateAudiobook(it) }
        }
    }

    suspend fun updateAudiobook(audiobook: Audiobook){
        withContext(Dispatchers.IO){
            audiobook.let { audiobookDao.updateAudiobook(it) }
        }
    }


    /* ExoPlayer Manipulation Functions-------------------------------------------------------------

    */
    fun selectTrack(track : Int){
        if (track != exoPlayer.currentMediaItemIndex){
            exoPlayer.seekTo(track, 0)
            //progress.value = getTimelineDuration()
        }
    }

    /* Playback Speed Functions---------------------------------------------------------------------

    */
    val playbackSpeed = MutableLiveData<String>()
    val playbackSpeedAsInt = MutableLiveData<Int>()

    fun setPlaybackSpeed(speedAsInt: Int){
        val speedAsFloat = speedAsInt / 10.0f
        val playbackParameters = PlaybackParameters(speedAsFloat)
        exoPlayer.playbackParameters = playbackParameters
        playbackSpeedAsInt.value = speedAsInt
        playbackSpeed.value = speedAsFloat.toString() + "x"
    }

    fun convertSpeedIntToString(speedAsInt: Int) : String {
        val speedAsFloat = speedAsInt / 10.0f
        return speedAsFloat.toString() + "x"
    }

    fun resetSpeedToDefault(){
        setPlaybackSpeed(globalPlaybackSpeed)

        audiobook?.playbackSpeed = null
        CoroutineScope(Dispatchers.IO).launch{
            updateAudiobook()
        }
    }

    fun updateAudiobookSpeed(speed : Int){
        audiobook?.playbackSpeed = speed
        CoroutineScope(Dispatchers.IO).launch{
            updateAudiobook()
        }
    }


    /* Sleep Timer Functions------------------------------------------------------------------------

    */
    lateinit var sleepTimer : CountDownTimer
    val sleepTimerText = MutableLiveData<String>()
    val sleepTimerTimeLeftLong = MutableLiveData<Long>()
    val sleepTimerPaused = MutableLiveData<Boolean>()
    var sleepTimerPausedBoolean = false

    //https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
    fun startSleepTimer(length : Long){
        sleepTimer = object : CountDownTimer(length + 100, TimeUnit.SECONDS.toMillis(1)){
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerText.value = millisUntilFinished.millisToMinutesSecondsString()
                sleepTimerPaused.value = false
                sleepTimerPausedBoolean = false

                //I don't like this, but it seems to be an okayish way to do it. Since onTick is called
                //every second, we can only update the sleepTimerLeftLong every second. Honestly, no one will probably notice
                //But hey, at least I made an effort, huh?
                sleepTimerTimeLeftLong.value = millisUntilFinished - millisUntilFinished.rem(1000) + 500

            }

            override fun onFinish() {
                sleepTimerText.value = null
                sleepTimerTimeLeftLong.value = null
                exoPlayer.playWhenReady = false
                sleepTimerPaused.value = null
                sleepTimerPausedBoolean = false

            }
        }.start()
    }

    fun pauseSleepTimer(){
        sleepTimer.cancel()
        sleepTimerPaused.value = true
        sleepTimerPausedBoolean = true
    }

    fun resumeSleepTimer(){
        sleepTimerTimeLeftLong.value?.let { startSleepTimer(it) }
        sleepTimerPaused.value = false
        sleepTimerPausedBoolean = false
    }

    fun cancelSleepTimer(){
        if(this::sleepTimer.isInitialized) {
            sleepTimer.cancel()
            sleepTimerText.value = null
            sleepTimerTimeLeftLong.value = null
        }
    }

}