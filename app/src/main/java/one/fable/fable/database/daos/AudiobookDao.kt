package one.fable.fable.database.daos

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.*
import one.fable.fable.database.entities.*

@Dao
interface AudiobookDao {
    @Query("select * from audiobook")
    fun getAudiobooks(): LiveData<List<Audiobook>>

    @Query("select * from audiobook")
    fun getAudiobooksList(): List<Audiobook>

    @Query("select * from audiobook where audiobookTitle like :query or audiobookAuthor like :query")
    fun getAudiobooks(query: String): List<Audiobook>

    @Query("select * from audiobook where progressState = $PROGRESS_NOT_STARTED AND canReadParentDirectory = 1 order by audiobookTitle")
    fun getNotStartedAudiobooks(): LiveData<List<Audiobook>>

    @Query("select * from audiobook where progressState = $PROGRESS_IN_PROGRESS AND canReadParentDirectory = 1 order by lastPlayedTimestamp DESC")
    fun getInProgressAudiobooks(): LiveData<List<Audiobook>>

    @Query("select * from audiobook where progressState = $PROGRESS_FINISHED AND canReadParentDirectory = 1 order by lastPlayedTimestamp DESC")
    fun getFinishedAudiobooks(): LiveData<List<Audiobook>>

    @Transaction
    @Query("SELECT * FROM audiobook where audiobookTitle = :title")
    fun getAllAudiobookTracks(title : String): List<AudiobookWithTracks>

    @Query("SELECT * FROM tracks where audiobookTitle = :title order by trackName ASC")
    fun getAudiobookTracks(title : String): List<Track>

    @Transaction
    @Query("SELECT * FROM tracks where audiobookTitle = :title order by trackName ASC")
    fun getAudiobookTracksWithChapters(title : String): List<TrackWithChapters>

    @Query("SELECT * FROM tracks")
    fun getTracks(): List<Track>

    @Query("SELECT * FROM chapters")
    fun getChapters(): List<Chapter>

    @Query("SELECT * FROM chapters where trackUri = :uri")
    fun getChapters(uri : Uri): List<Chapter>

    @Query("select * from audiobook where audiobookTitle = :audiobookTitle")
    fun getAudiobook(audiobookTitle: String) : Audiobook?

    @Query("select * from tracks where trackUri = :uri")
    fun getTrack(uri: Uri) : Track?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAudiobook(audiobook: Audiobook)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: Track)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapter(chapter: Chapter)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAudiobook(audiobook: Audiobook)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateTrack(track: Track)


}