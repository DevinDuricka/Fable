//package one.fable.fable.audiobookPlayer
//
//import android.app.Application
//import android.content.Context
//import android.content.Intent
//import android.media.MediaMetadataRetriever
//import android.net.Uri
//import android.os.Build
//import android.provider.DocumentsContract
//import android.provider.MediaStore
//import android.util.Log
//import android.util.Xml
//import androidx.documentfile.provider.DocumentFile
//import androidx.lifecycle.*
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.internal.synchronized
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.withContext
//import org.xmlpull.v1.XmlPullParser
//import timber.log.Timber
//import java.io.BufferedReader
////import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
////import nl.bravobit.ffmpeg.FFmpeg
////import nl.bravobit.ffmpeg.FFprobe
//import java.io.FileNotFoundException
//import java.io.InputStreamReader
//import java.io.StringReader
//import java.lang.IllegalStateException
//import java.util.concurrent.TimeUnit
//
//class AudiobookPlayerViewModel(title: String, application: Application) : AndroidViewModel(application) {
//
//    //val repository = (application as Fable).repository
//    //val exoPlayerInterface = (application as Fable).exoPlayerInterface
//
//    //lateinit var audiobook : Audiobook
//    //private val audiobooks = repository.audiobooks
//
//
////    val audiobooks : LiveData<List<Audiobook>>
////        get() = _audiobooks
//
//
//    //var file = "Test"
//    //val resolver = application.contentResolver
//
//
//    //private lateinit var audiobookRepository : AudiobookRepository
//
//    init {
//        Timber.i("AudiobookPlayerViewModel Initiated")
//        ExoPlayerInterface.updatePreviousBookLocation(title)
//        runBlocking {
//
//            ExoPlayerInterface.getTracks(title)
//        }
////        viewModelScope.launch {
////            ExoPlayerInterface.getTracks(title)
////        }
//        ExoPlayerInterface.loadTracks(title, application)
//    }
//
//}
//
