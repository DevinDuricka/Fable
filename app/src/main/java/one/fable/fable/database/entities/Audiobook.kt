package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.*

const val PROGRESS_NOT_STARTED = 0
const val PROGRESS_IN_PROGRESS = 1
const val PROGRESS_FINISHED = 2

@Entity(tableName = "audiobook")
data class Audiobook(

    @PrimaryKey(autoGenerate = true)
    var audiobookId: Long = 0L,

    val audiobookTitle: String,
    val parentDirectory: Uri,
    var existsParentDirectory: Boolean = true,
    var canReadParentDirectory: Boolean = true,

    var audiobookAuthor: String? = null,
    var audiobookDescription: String? = null,
    var audiobookGenre: String? = null,

    var imgThumbnail: Uri? = null,
    var highResImage: Uri? = null,
    var playbackSpeed: Int? = null,

    var progressState: Int = PROGRESS_NOT_STARTED,
    var lastPlayedTimeStamp : Long = 0L,

    var windowIndex: Int? = null,
    var windowLocation: Long? = null,

    var timelineDuration: Long = 0L, //the progress into the book that's been made
    var duration: Long = 0L, //the total length of the book

    var inCloud : Boolean = false
)

data class AudiobookWithTracks(
    @Embedded val audiobook: Audiobook,
    @Relation(
        parentColumn = "audiobookTitle",
        entityColumn = "audiobookTitle"
    )
    val tracks: List<Track>
)

data class AudiobookWithAlbumArt(
    @Embedded val audiobook: Audiobook,
    @Relation(
        parentColumn = "imgThumbnail",
        entityColumn = "uri"
    )
    val cover: BookCover

)

data class albumImage(
    val uri: Uri,
    val size : Long
)
