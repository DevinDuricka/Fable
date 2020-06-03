package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_cover")
data class BookCover(
    @PrimaryKey
    val uri: Uri,

    val fileName : String,

    val fileSize: Long = 0L,

    var bookTitle : String?
)