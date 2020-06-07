package one.fable.fable.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.exoplayer.ExoPlayerMasterObject

class LibraryFragmentViewModel : ViewModel() {
    lateinit var notStartedAudiobooks : LiveData<List<Audiobook>>
    lateinit var inProgressAudiobooks : LiveData<List<Audiobook>>
    lateinit var completeAudiobooks : LiveData<List<Audiobook>>

    init {
        inProgressAudiobooks = ExoPlayerMasterObject.audiobookDao.getInProgressAudiobooks()
        notStartedAudiobooks = ExoPlayerMasterObject.audiobookDao.getNotStartedAudiobooks()
        completeAudiobooks = ExoPlayerMasterObject.audiobookDao.getFinishedAudiobooks()
    }

}