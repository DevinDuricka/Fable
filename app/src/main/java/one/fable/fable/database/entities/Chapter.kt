package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    var chapterId: Long = 0L,

    val trackUri: Uri,

    var chapterName : String?,

    var chapterPositionMs : Long = 0L
)