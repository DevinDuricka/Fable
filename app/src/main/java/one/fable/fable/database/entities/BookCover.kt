package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_cover")
data class BookCover(
    @PrimaryKey
    val uri: Uri,

    val fileSize: Long = 0L,

    var bookTitle : String?,

    var lightVibrantSwatch_bodyTextColor : Int? = null,
    var lightVibrantSwatch_titleTextColor : Int? = null,
    var lightVibrantSwatch_rgb : Int? = null,

    var vibrantSwatch_bodyTextColor : Int? = null,
    var vibrantSwatch_titleTextColor : Int? = null,
    var vibrantSwatch_rgb : Int? = null,

    var darkVibrantSwatch_bodyTextColor : Int? = null,
    var darkVibrantSwatch_titleTextColor : Int? = null,
    var darkVibrantSwatch_rgb : Int? = null,

    var lightMutedSwatch_bodyTextColor : Int? = null,
    var lightMutedSwatch_titleTextColor : Int? = null,
    var lightMutedSwatch_rgb : Int? = null,

    var mutedSwatch_bodyTextColor : Int? = null,
    var mutedSwatch_titleTextColor : Int? = null,
    var mutedSwatch_rgb : Int? = null,

    var darkMutedSwatch_bodyTextColor : Int? = null,
    var darkMutedSwatch_titleTextColor : Int? = null,
    var darkMutedSwatch_rgb : Int? = null,

    var dominantSwatch_bodyTextColor : Int? = null,
    var dominantSwatch_titleTextColor : Int? = null,
    var dominantSwatch_rgb : Int? = null
)