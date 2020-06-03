package one.fable.fable.database

import android.net.Uri
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun stringToUri(uri: String?): Uri? {
        return if (uri == null) null else Uri.parse(uri)
    }

    @TypeConverter
    fun uriToString(uri: Uri?) :String? {
        return uri?.toString()
    }
}