package one.fable.fable

import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.fable.fable.database.entities.*
import one.fable.fable.exoplayer.AudioPlayerService
import one.fable.fable.library.LibraryFragment
import one.fable.fable.library.flags
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.TimeUnit

/* Notes/URLs
https://commonsware.com/blog/2019/11/23/scoped-storage-stories-documentscontract.html
*/

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        /*https://github.com/software-mansion/react-native-screens/issues/17
        https://proandroiddev.com/make-your-custom-view-lifecycle-aware-its-a-piece-of-cake-90b7c0498686
        The saved state is actually a detriment right now. If the audiobook player is open, it will try
        to load that page. The problem is the ExoPlayerMasterObject no longer has an audiobook,
        but the AudiobookPlayerFragment will try to load it. Since audiobook is a lateinit, it will crash.
        To avoid that, just go back the home screen (library)
        */
        super.onCreate(null)

        setContentView(R.layout.activity_main)
        //takePersistablePermissionsToDatabaseEntries()
        scanDirectories()
        volumeControlStream = AudioManager.STREAM_MUSIC


    }

    fun navigateFirstTabWithClearStack() {
        val navController = Navigation.findNavController(this,R.id.nav_host_fragment)
        val navHostFragment: NavHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation)
        graph.startDestination = R.id.libraryFragment

        navController.graph = graph
    }



    override fun onStart() {
        val fable = (application as Fable)
        if (!fable.isAudiobookDaoInitialized() || !fable.isdirectoriesDaoInitialized()){
            fable.initalizeDependecies()
        }
//        if (!ExoPlayerMasterObject.isAudiobookInitialized() || !ExoPlayerMasterObject.isExoPlayerInitialized()){
//            //navigateFirstTabWithClearStack()
//            val navController = Navigation.findNavController(this,R.id.nav_host_fragment)
//            val navOptions = NavOptions.Builder().setPopUpTo(R.id.libraryFragment, true).build()
//            navController.navigate(R.id.libraryFragment, null, navOptions)
//        }
        startAudioPlayerService()
        super.onStart()
    }

    fun startAudioPlayerService(){
        val intent = Intent(applicationContext, AudioPlayerService::class.java)
        startService(intent)
    }


    fun takePersistablePermissionsToDatabaseEntries(directoryUri : Uri){
        val documentFile : DocumentFile = DocumentFile.fromTreeUri(applicationContext, directoryUri) ?: return
        val authority = documentFile.uri.authority ?: return

        if (!isLocal(authority)){
            Toast.makeText(applicationContext, "Only adding local storage is available at this time", Toast.LENGTH_LONG).show()
            return
        }

        try {
            contentResolver.takePersistableUriPermission(directoryUri, flags)
            Timber.i("PersistableUri Permission Granted at: " + directoryUri)
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(applicationContext, "There was an error adding the directory.", Toast.LENGTH_LONG).show()
            return
        }

        val directoryDao = (application as Fable).directoryDao
        CoroutineScope(Dispatchers.IO).launch{
            if(documentFile.isDirectory && documentFile.exists() && documentFile.canRead() && directoryDao.get(documentFile.uri) == null){
                directoryDao.insert(DirectoryRoot(uri = documentFile.uri, name = documentFile.name))
                getAllAudioInDirectory(documentFile.uri)
            }
        }
    }

    fun isLocal(authority: String) : Boolean{
        return authority.startsWith("com.android")
    }

    fun scanDirectories(){
        CoroutineScope(Dispatchers.IO).launch {
            checkIfDirectoriesAreAccessible()
            val directories = (application as Fable).directoryDao.getAllDirectories()
            for (directory in directories) {
                getAllAudioInDirectory(directory.uri)
            }
        }
    }

    suspend fun checkIfDirectoriesAreAccessible(){
        val audiobooksDao = (application as Fable).audiobookDao
        val audiobooks = audiobooksDao.getAudiobooksList()
        withContext(Dispatchers.IO) {
            audiobookLoop@ for (audiobook :Audiobook in audiobooks){
                val documentFile : DocumentFile = DocumentFile.fromSingleUri(applicationContext, audiobook.parentDirectory) ?: continue@audiobookLoop
                Timber.i("Is Directory: " + documentFile.isDirectory)
                Timber.i("Exists: " + documentFile.exists())
                Timber.i("Can read: " + documentFile.canRead())
                if(!documentFile.isDirectory || !documentFile.exists() || !documentFile.canRead()){
                    audiobook.canReadParentDirectory = false
                    audiobooksDao.updateAudiobook(audiobook)
                } else {
                    if (audiobook.canReadParentDirectory == false){
                        audiobook.canReadParentDirectory = true
                        audiobooksDao.updateAudiobook(audiobook)
                    }
                }
            }
        }
    }

    suspend fun getAllAudioInDirectory(uri: Uri) {

        val audiobooksDao = (application as Fable).audiobookDao
        val directory : DocumentFile = DocumentFile.fromSingleUri(applicationContext, uri) ?: return
        var title : String = directory.name ?: return

        //val folderName = directory.name

        //val resolver = contentResolver
        var isAudiobookDirectory = false
        var audiobookAdded = false


        var topLevelTitle = ""

        withContext(Dispatchers.IO) {

            val collection = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                DocumentsContract.getDocumentId(uri)
            )
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            var cover : albumImage? = null
            var images = mutableListOf<albumImage>()

            contentResolver.query(collection, projection, null, null, null)
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(projection[0])
                    val nameColumn = cursor.getColumnIndexOrThrow(projection[1])
                    val sizeColumn = cursor.getColumnIndexOrThrow(projection[2])
                    val fileTypeColumn = cursor.getColumnIndexOrThrow(projection[3])

                    cursor@ while (cursor.moveToNext()) {

                        val id = cursor.getString(idColumn)
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, id)

                        val mimeType = cursor.getString(fileTypeColumn)
                        val name = cursor.getString(nameColumn)

                        //var title :String? = null
                        var author :String? = null
                        var duration :Long? = null
                        val size = cursor.getLong(sizeColumn)
                        var inCloud = true

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            getAllAudioInDirectory(childUri)
                        //} else if (mimeType.startsWith("audio/") && mimeType != "audio/x-ms-wax" ) {
                        } else if (mimeType =="audio/mpeg") {
                            if (audiobooksDao.getTrack(childUri) == null){
                                val authority = childUri.authority ?: continue@cursor
                                if(isLocal(authority)){
                                    val mediaMetadataRetriever = MediaMetadataRetriever()
                                    mediaMetadataRetriever.setDataSource(applicationContext, childUri)
                                    duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                                    (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))?.let { title = it }
                                    author = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                    mediaMetadataRetriever.release()
                                    inCloud = false
                                }

//                                if (title.isNullOrEmpty()){
//                                    title = documentFile?.name ?: continue@cursor
//                                }

                                audiobooksDao.insertTrack(Track(childUri, title, name, trackLength = duration))

                                val audiobook = audiobooksDao.getAudiobook(title)
                                if (audiobook == null){
                                    audiobooksDao.insertAudiobook(Audiobook(audiobookTitle = title, audiobookAuthor = author, parentDirectory = uri, inCloud = inCloud, duration = duration ?: 0L))
                                    //Log.i("AudiobookPlayerVMTracks", title + " added to the database")
                                } else {
                                    if (duration != null) {
                                        audiobook.duration = audiobook.duration.plus(duration)
                                        audiobooksDao.updateAudiobook(audiobook)
                                    }
                                }
                            }

//                            if (audiobookAdded == false){
//                                audiobookAdded = true
//                                topLevelTitle = title
//                            }

                            //todo
                            //checkFileForChapterInfo(childUri)

                        }  else if (mimeType.startsWith("image/")) {
                            images.add(albumImage(childUri, size))
//                            if (cover == null) {
//                                cover = albumImage(childUri, size)
//                            }
//                            if (cover != null) {
//                                if (size > cover.size){
//                                    cover = albumImage(childUri, size)
//                                }
//                            }
                        }
                        //val fileType = cursor.getString(fileTypeColumn)

                    }
                    if (title.isNotBlank() && images.isNotEmpty()){
                        var audiobook = audiobooksDao.getAudiobook(title)
                        if (audiobook != null){
                            images.sortBy { it.size }
                            audiobook.imgThumbnail = images.last().uri
                            audiobooksDao.updateAudiobook(audiobook)

//                            images[-1].uri
//                            if (audiobook.imgThumbnail == null && cover != null) {
//                                audiobook.imgThumbnail = cover!!.uri
//                            }
                        }
                    }

                }


        }
    }



}

fun String.convertOverDriveTimeMarkersToMillisecondsAsLong() : Long {
    var totalMilliseconds = 0L

    //I was initially getting the time markers by substring, but it started crashing when there were
    //some tracks with hours, "00:00:00.000". Now it splits the strings on the colons and period and
    //uses the split strings to add up the milliseconds
    val timeSubString = this.split(":", ".")
    val timeSubstringReversed = timeSubString.asReversed()

    val milliseconds = timeSubstringReversed.getOrNull(0)
    var seconds = timeSubstringReversed.getOrNull(1)
    val minutes = timeSubstringReversed.getOrNull(2)
    val hours = timeSubstringReversed.getOrNull(3)

    //val minutes = this.substringBefore(":")
    if(milliseconds != null){
        totalMilliseconds += milliseconds.toLong()
    }

    if(seconds != null){
        totalMilliseconds += TimeUnit.SECONDS.toMillis(seconds.toLong())
    }

    if (minutes != null){
        totalMilliseconds += TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    if (hours != null){
        totalMilliseconds += TimeUnit.HOURS.toMillis(hours.toLong())
    }

    return totalMilliseconds
}

/*Millis (Long) to time string format functions
https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
Since I seem to be referencing this one page a lot, I'm making note of the link, just in case
These all use modulus division to calculate the remainder of the Hours -> Minutes -> Seconds */
fun Long.millisToHoursMinutesSecondsString() : String{ //HH:MM:SS
    return String.format("%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(this) % TimeUnit.MINUTES.toSeconds(1))
}

fun Long.millisToMinutesSecondsString() : String{ //MM:SS
    return String.format("%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this),
        TimeUnit.MILLISECONDS.toSeconds(this) % TimeUnit.MINUTES.toSeconds(1))
}

fun Long.millisToHoursMinutesString() : String{ //HH:MM
    return String.format("%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) % TimeUnit.HOURS.toSeconds(1))
}
