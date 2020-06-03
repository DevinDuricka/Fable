package one.fable.fable.library

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.database.entities.PROGRESS_FINISHED
import one.fable.fable.database.entities.PROGRESS_IN_PROGRESS
import one.fable.fable.database.entities.PROGRESS_NOT_STARTED
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber

class LibraryTabViewModel(position:Int) : ViewModel()  {

    lateinit var audiobooks : LiveData<List<Audiobook>>

    init {
        when (position){
            PROGRESS_IN_PROGRESS -> {
                Timber.i("In Progress Audiobooks")
                audiobooks = ExoPlayerMasterObject.audiobookDao.getInProgressAudiobooks()
            }
            PROGRESS_NOT_STARTED -> {
                Timber.i("Not Started Audiobooks")
                audiobooks = ExoPlayerMasterObject.audiobookDao.getNotStartedAudiobooks()
            }
            PROGRESS_FINISHED -> {
                Timber.i("Finished Audiobooks")
                audiobooks = ExoPlayerMasterObject.audiobookDao.getFinishedAudiobooks()
            }
            else -> {
                audiobooks = ExoPlayerMasterObject.audiobookDao.getAudiobooks()
            }
        }
    }

}