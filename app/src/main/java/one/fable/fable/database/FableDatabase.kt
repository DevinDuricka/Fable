package one.fable.fable.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import one.fable.fable.database.daos.AudiobookDao
import one.fable.fable.database.daos.DirectoryDao
import one.fable.fable.database.entities.*

@Database(entities = [
    Audiobook::class,
    Track::class,
    Chapter::class,
    BookCover::class,
    DirectoryRoot::class],

    version = 1, exportSchema = false)

@TypeConverters(Converters::class)
abstract class FableDatabase : RoomDatabase() {

    abstract val directoryDao : DirectoryDao
    abstract val audiobookDao : AudiobookDao

    companion object{
        @Volatile
        private var INSTANCE: FableDatabase? = null

        fun getInstance(context: Context): FableDatabase {
            synchronized(this) {
                var instance = INSTANCE
                //context.deleteDatabase("fable_database")
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        FableDatabase::class.java,
                        "fable_database"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}