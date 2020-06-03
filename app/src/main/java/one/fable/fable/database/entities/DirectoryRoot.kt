package one.fable.fable.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "directory_root")
data class DirectoryRoot(
    @PrimaryKey
    val uri: Uri,
    var name : String?,
    var lastModified : Long = 0L,
    var size : Long = 0L,
    var inCloud : Boolean = false
)