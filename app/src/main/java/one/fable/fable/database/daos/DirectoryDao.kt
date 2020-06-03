package one.fable.fable.database.daos

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.*
import one.fable.fable.database.entities.DirectoryRoot

@Dao
interface DirectoryDao {
    @Query("select * from directory_root")
    fun getAllDirectories(): List<DirectoryRoot>

    //Should only ever be 0 or 1 since the URI is the primary key and either it exists or not
    @Query("select * from directory_root where uri = :uri")
    fun get(uri: Uri) : DirectoryRoot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(directory: DirectoryRoot)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(directory: DirectoryRoot)
}