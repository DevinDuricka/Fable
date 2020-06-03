package one.fable.fable.exoplayer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PREPARED
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import one.fable.fable.convertOverDriveTimeMarkersToMillisecondsAsLong
import one.fable.fable.database.daos.AudiobookDao
import one.fable.fable.database.entities.*
import one.fable.fable.millisToMinutesSecondsString
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.lang.Runnable
import java.util.concurrent.TimeUnit

/* ExoPlayer Notes/URLs that I dont want to forget:

Building feature-rich media apps with ExoPlayer (Google I/O '18) https://www.youtube.com/watch?v=svdq1BWl4r8&t=784s

*/

object ExoPlayerMasterObject {

    lateinit var audiobookDao: AudiobookDao

    val audioPlaybackWindows = mutableListOf<AudioPlaybackWindow>()

    var coverUri : Uri? = null
    var title : String? = null
    val chapterName = MutableLiveData<String>()

    lateinit var audiobook : Audiobook
    fun isAudiobookInitialized() = this::audiobook.isInitialized

    var audiobookTracks = listOf<Track>()
    var tracksWithChapters = listOf<TrackWithChapters>()

    val progress = MutableLiveData<Long>()

    val windowDurationsSummation = mutableListOf<Long>()

    val sleepTimerText = MutableLiveData<String>()
    val sleepTimerTimeLeftLong = MutableLiveData<Long>()
    val sleepTimerPaused = MutableLiveData<Boolean>()
    var sleepTimerPausedBoolean = false

    val playbackSpeed = MutableLiveData<String>()
    val playbackSpeedAsInt = MutableLiveData<Int>()
    val globalPlaybackSpeed = MutableLiveData<Int>()
    val audiobookPlaybackSpeed = MutableLiveData<Int>()

    lateinit var sharedPreferences: SharedPreferences

    val settingChangeEventListener = SharedPreferences.OnSharedPreferenceChangeListener{ sharedPreferences: SharedPreferences?, key: String? ->
        if (key == "playback_speed_seekbar"){
            globalPlaybackSpeed.value = sharedPreferences?.getInt(key, 10)
            if(isAudiobookInitialized()){
                if (audiobook.playbackSpeed == null){
                    globalPlaybackSpeed.value?.let { setPlaybackSpeed(it) }
                }
            }
        }
    }


    lateinit var exoPlayer : SimpleExoPlayer
    fun isExoPlayerInitialized() = this::exoPlayer.isInitialized //https://stackoverflow.com/questions/47549015/isinitialized-backing-field-of-lateinit-var-is-not-accessible-at-this-point
    fun buildExoPlayer(context: Context){
        if (!this::exoPlayer.isInitialized){
            exoPlayer = SimpleExoPlayer.Builder(context).build()
            exoPlayer.addListener(eventListener)
        }
    }

    fun setSharedPreferencesAndListener(preferences: SharedPreferences){
        sharedPreferences = preferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(settingChangeEventListener)
        globalPlaybackSpeed.value = sharedPreferences.getInt("playback_speed_seekbar", 10)
    }

    suspend fun getTracks(title: String, context: Context){
        withContext(Dispatchers.IO){
            audiobookTracks = audiobookDao.getAudiobookTracks(title)
            for (track in audiobookTracks){
                checkFileForChapterInfo(track.trackUri, context)
            }
            tracksWithChapters = audiobookDao.getAudiobookTracksWithChapters(title)
        }
    }

    suspend fun loadTracks(context: Context, title :String){
        Timber.i("Loading new tracks")

        val dataSourceFactory = DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "Fable"))

        val concatenatingMediaSource = ConcatenatingMediaSource()


        runBlocking { getTracks(title, context) }
//        CoroutineScope(Dispatchers.IO).launch {
//
//        }
        progress.value = audiobook.timelineDuration

        audioPlaybackWindows.clear()

        for (track in tracksWithChapters) {
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(track.track.trackUri)

            //If the track has no chapter data embeded, use the whole track as the window (a psuedo-chapter)
            if (track.chapters.isEmpty()) {
                audioPlaybackWindows.add(AudioPlaybackWindow(track.track.trackName))
                concatenatingMediaSource.addMediaSource(mediaSource)
            } else {

                val trackChapters = mutableListOf<AudioPlaybackWindow>()
                for (chapter in track.chapters){
                    trackChapters.add(AudioPlaybackWindow(chapter.chapterName, chapter.chapterPositionMs))
                }


                for (index in 0 until trackChapters.lastIndex){
                    val chapterSource = ClippingMediaSource(
                        mediaSource,
                        TimeUnit.MILLISECONDS.toMicros(trackChapters[index].startPos),
                        TimeUnit.MILLISECONDS.toMicros(trackChapters[index + 1].startPos),
                        false,
                        true,
                        true
                    )
                    audioPlaybackWindows.add(AudioPlaybackWindow(trackChapters[index].Name,
                        trackChapters[index].startPos,
                        trackChapters[index + 1].startPos,
                        trackChapters[index + 1].startPos - trackChapters[index].startPos))
                    concatenatingMediaSource.addMediaSource(chapterSource)
                }

                val chapterSource = ClippingMediaSource(
                    mediaSource,
                    TimeUnit.MILLISECONDS.toMicros(trackChapters.last().startPos),
                    C.TIME_END_OF_SOURCE
                )

                audioPlaybackWindows.add(AudioPlaybackWindow(trackChapters.last().Name, trackChapters.last().startPos, track.track.trackLength,
                    track.track.trackLength?.minus(trackChapters.last().startPos)
                ))
                concatenatingMediaSource.addMediaSource(chapterSource)

            }

        }

        exoPlayer.prepare(concatenatingMediaSource)
        exoPlayer.playWhenReady = false

        //var index = 0

//        while(index <= exoPlayer.currentTimeline.getLastWindowIndex(false)){
//            var window = Timeline.Window()
//            exoPlayer.currentTimeline.getWindow(index, window)
//            windowDurationsSummation.last()
//            index++
//        }

        val windowIndex = audiobook.windowIndex
        val windowLocation = audiobook.windowLocation

        if (windowIndex != null && windowLocation != null) {
            exoPlayer.seekTo(windowIndex, windowLocation)
        }

//        Timber.i("Last Index is: " + exoPlayer.currentTimeline.windowCount)
//        for (index in 0 .. exoPlayer.currentTimeline.windowCount){
//            var window = Timeline.Window()
//            exoPlayer.currentTimeline.getWindow(index, window)
//            windowDurationsSummation.add(windowDurationsSummation.last().plus(window.durationMs))
//        }

        audiobook.playbackSpeed?.let { setPlaybackSpeed(it) } ?: run {
            globalPlaybackSpeed.value?.let {
                setPlaybackSpeed(it)
            }
        }

    }

    suspend fun checkFileForChapterInfo(uri: Uri, context: Context) {
        var track = audiobookDao.getTrack(uri) ?: return

        if (!track.scanSourceNames.contains("OverDrive")) {
            withContext(Dispatchers.IO) {

                val contentResolver = context.contentResolver
                var inputStream = contentResolver.openInputStream(uri)
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                var line = ""
                val iterator = bufferedReader.lineSequence().iterator()
                var iteratorCounter = 0

                while (iterator.hasNext() && iteratorCounter <= 500 && line.isBlank()) {
                    val fileLine = iterator.next()
                    if (fileLine.contains("OverDrive MediaMarkers")) {
                        line = fileLine
                    }
                    iteratorCounter.inc()
                }
                bufferedReader.close()

                if (line.isNotBlank()) {
                    //var line = bufferedReader.readLine()
                    var markers = listOf<Marker>()

                    line = line.filter { !it.isIdentifierIgnorable() || !it.isISOControl() }

                    //For some reason, the first filter isn't removing some of the special chars.
                    //This will remove the rest of the unknown ones.
                    line = line.replace("\\uFFFD".toRegex(), "")

                    //This doesn't seem to work. I'm still seeing these chars in the line.
                    //Luckily, I don't think this is part of the string we actually are going to use.
                    line = line.replace("\\u001D".toRegex(), "")

                    //Log.i("AudiobookPlayerVMTracks", line)

                    val chaptersXML = line.substringAfter("<Markers>").substringBefore("</Markers>")
                    //Log.i("AudiobookPlayerVMTracks", chaptersXML)

                    if (chaptersXML != line) {
                        markers = getOverDriveMediaMarkers(chaptersXML)
                    }

                    if (markers.isNotEmpty()) {
                        for (marker in markers) {
                            audiobookDao.insertChapter(
                                Chapter(
                                    trackUri = uri,
                                    chapterName = marker.Name,
                                    chapterPositionMs = marker.Time
                                )
                            )
                        }
                    }
                }

                //track.hasBeenScanned = true
                track.scanSourceNames += "OverDrive "
                audiobookDao.updateTrack(track)
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

    fun selectTrack(track : Int){
        if (track != exoPlayer.currentWindowIndex){
            exoPlayer.seekTo(track, 0)
            //progress.value = getTimelineDuration()
        }
    }

    fun customFastForward(fastforwardAmount: Long){
        val currentPosition = exoPlayer.currentPosition
        val windowLength = exoPlayer.duration
        var windowIndex = exoPlayer.currentWindowIndex
        val trackRemainder = windowLength - currentPosition

        if (fastforwardAmount < trackRemainder){
            exoPlayer.seekTo(currentPosition + fastforwardAmount)
        } else {
            exoPlayer.seekTo(windowIndex + 1, fastforwardAmount - trackRemainder)
            //todo implement the same "while" loop we have in the customRewind.
            // It isn't guaranteed that the next window will have enough remaining time,
            // so we may have to continue seeking tracks
        }
    }

    fun customRewind(rewindAmount: Long){
        val currentPosition = exoPlayer.currentPosition //Position (time) in current window/period
        //val trackLength = exoPlayer.duration //Length of the window/period
        var windowIndex = exoPlayer.currentWindowIndex

        if (currentPosition > rewindAmount) {
            exoPlayer.seekTo(currentPosition - rewindAmount)
        } else {
            //If it's the first track, go to the start of the track
            if (windowIndex == 0){
                exoPlayer.seekToDefaultPosition()
                return
            }

            //If the
            var deficit = currentPosition - rewindAmount
            var previousWindowDuration = 0L
            var seekToWindowIndex : Int = exoPlayer.currentWindowIndex

            while (deficit < 0L && seekToWindowIndex > 0){
                seekToWindowIndex = seekToWindowIndex.dec()
                var seekWindow = Timeline.Window()
                exoPlayer.currentTimeline.getWindow(seekToWindowIndex, seekWindow)
                previousWindowDuration = seekWindow.durationMs
                deficit += previousWindowDuration
            }

            if (seekToWindowIndex == 0 && deficit < 0L){
                exoPlayer.seekTo(seekToWindowIndex, 0L)
            } else {
                exoPlayer.seekTo(seekToWindowIndex, deficit)
            }
        }
    }

    //PROGRESS TRACKER CODE todo
    val progressTrackerHandler = Handler()
    val progressTrackerRunnable = object : Runnable{
        override fun run() {
            updateAudiobookObjectLocation()
            progressTrackerHandler.postDelayed(this, 400)
        }
    }

    fun updateAudiobookObjectLocation(){
        audiobook.windowIndex = exoPlayer.currentWindowIndex
        audiobook.windowLocation = exoPlayer.currentPosition
        audiobook.timelineDuration = getTimelineDuration()
        progress.value = audiobook.timelineDuration
        audiobook.lastPlayedTimeStamp = System.currentTimeMillis()
        if (exoPlayer.currentWindowIndex == exoPlayer.currentTimeline.windowCount - 1){
            audiobook.progressState = PROGRESS_FINISHED
        } else {
            audiobook.progressState = PROGRESS_IN_PROGRESS
        }
    }

    fun getTimelineDuration() : Long{
        var timelineDuration = try {windowDurationsSummation[exoPlayer.currentWindowIndex -1]}
            catch(e: IndexOutOfBoundsException){0L}
        timelineDuration += exoPlayer.currentPosition
//        var index = 0
//        Timber.i("Current Window Index: " + exoPlayer.currentWindowIndex)
//        while(index < exoPlayer.currentWindowIndex){
//            var window = Timeline.Window()
//            exoPlayer.currentTimeline.getWindow(index, window)
//            timelineDuration += window.durationMs
//            index++
//        }
//        timelineDuration += exoPlayer.currentPosition

        return timelineDuration
    }

    //PLAYER EVENT LISTENER CODE todo
    private val eventListener = PlayerEventListener()
    class PlayerEventListener() : Player.EventListener{
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

        override fun onPositionDiscontinuity(reason: Int) {
            /*------Detecting when playback transitions to another item------
            https://stackoverflow.com/questions/53866692/android-exoplayer-concatenatingmediasource-detect-end-of-first-source
            https://exoplayer.dev/playlists.html

            The 2 discontinuity reasons are:
            1. Player.DISCONTINUITY_REASON_PERIOD_TRANSITION
            This happens when playback automatically transitions from one item to the next.
            2. Player.DISCONTINUITY_REASON_SEEK
            This happens when the current playback item changes as part of a seek operation, for example when calling Player.next. */
            super.onPositionDiscontinuity(reason)
            if (reason == Player.DISCONTINUITY_REASON_SEEK){
                if (!exoPlayer.currentTimeline.isEmpty){
                    progress.value = getTimelineDuration()
                }
            }

            //todo
            if (audioPlaybackWindows.isNotEmpty()) {
                Timber.i("Position Discontinuity")
                Timber.i(reason.toString())
                chapterName.value = audioPlaybackWindows[exoPlayer.currentWindowIndex].Name
            }
//            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            /*------Detecting when playback transitions to another item------
                        (continuation of the info above)
            This last one is actually a .onTimeLineChanged
            3. EventListener.onTimelineChanged with reason = Player.TIMELINE_CHANGE_REASON_DYNAMIC.
            This happens when the playlist changes, e.g. if items are added, moved, or removed.
             */
            //todo
            if (audioPlaybackWindows.isNotEmpty()){
                Timber.i("Timeline Changed")
                chapterName.value = audioPlaybackWindows[exoPlayer.currentWindowIndex].Name
                progress.value = getTimelineDuration()
            }

            if (reason == TIMELINE_CHANGE_REASON_PREPARED){
                windowDurationsSummation.clear()
                Timber.i("Last Index is: " + exoPlayer.currentTimeline.windowCount)
                for (index in 0 until timeline.windowCount){
                    var window = Timeline.Window()
                    timeline.getWindow(index, window)

                    var duration = window.durationMs
                    duration = window.getDurationUs() / 1000
                    //TODO: Someday when the fix gets pushed to a release, get rid of this
                    //https://github.com/google/ExoPlayer/issues/7314
                    if (duration < 0){
                        val outsideCalculatedDuration = audioPlaybackWindows[index].duration
                        if (outsideCalculatedDuration != null){
                            duration = outsideCalculatedDuration
                        } else {
                            val period = Timeline.Period()
                            timeline.getPeriod(window.firstPeriodIndex, period)

                            duration = period.durationMs - audioPlaybackWindows[index].startPos
                        }
                        if (duration < 0){
                            duration = 0
                        }
                    }

                    val previousValue = windowDurationsSummation.lastOrNull()
                    if (previousValue == null) {
                        windowDurationsSummation.add(duration)
                    } else {
                        windowDurationsSummation.add(previousValue.plus(duration))
                    }
                }
            }
            super.onTimelineChanged(timeline, reason)
        }
    }


    //fun preventNegative

    lateinit var sleepTimer : CountDownTimer
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

    fun setPlaybackSpeed(speedAsInt: Int){
        val speedAsFloat = speedAsInt / 10.0f
        val playbackParameters = PlaybackParameters(speedAsFloat)
        exoPlayer.setPlaybackParameters(playbackParameters)
        playbackSpeedAsInt.value = speedAsInt
        playbackSpeed.value = speedAsFloat.toString() + "x"
    }

    fun convertSpeedIntToString(speedAsInt: Int) : String {
        val speedAsFloat = speedAsInt / 10.0f
        return speedAsFloat.toString() + "x"
    }

    fun resetSpeedToDefault(){
        globalPlaybackSpeed.value?.let { setPlaybackSpeed(it) }
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

    suspend fun updateAudiobook(){
        withContext(Dispatchers.IO){
            audiobook?.let { audiobookDao.updateAudiobook(it) }
        }
    }




}