package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Duration

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val trackUri: Uri,

    val audiobookTitle: String,

    //val chapterOrder : Int,

    var trackName : String?,

    var scannedSourceNames: String = "",

    var validSource : String = "",

    var trackLength : Long? = 0L
)

data class TrackWithChapters(
    @Embedded val track : Track,
    @Relation( parentColumn = "trackUri",
        entityColumn = "trackUri"
    )
    val chapters: List<Chapter>
)

data class Marker(
    val Name : String?,
    val Time : Long = 0L
)

data class AudioPlaybackWindow(
    val Name : String?,
    val startPos : Long = 0L,
    val endPos : Long? = null,
    val duration: Long? = null
)