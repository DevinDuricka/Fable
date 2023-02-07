package one.fable.fable.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.exoplayer.ExoPlayerMasterObject

class LibraryFragmentViewModel : ViewModel() {
    var notStartedAudiobooks : LiveData<List<Audiobook>> = ExoPlayerMasterObject.audiobookDao.getNotStartedAudiobooks()
    var inProgressAudiobooks : LiveData<List<Audiobook>> = ExoPlayerMasterObject.audiobookDao.getInProgressAudiobooks()
    var completeAudiobooks : LiveData<List<Audiobook>> = ExoPlayerMasterObject.audiobookDao.getFinishedAudiobooks()
    var anyAudiobook : Audiobook? = null
    var firstLoad = true

    init {
        CoroutineScope(Dispatchers.IO).launch {
            anyAudiobook = ExoPlayerMasterObject.audiobookDao.getAnyInProgressAudiobooks()
        }
    }

}