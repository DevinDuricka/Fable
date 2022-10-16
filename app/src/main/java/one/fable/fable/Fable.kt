package one.fable.fable

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import one.fable.fable.database.FableDatabase
import one.fable.fable.database.daos.AudiobookDao
import one.fable.fable.database.daos.DirectoryDao
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber

class Fable : Application() {

    lateinit var audiobookDao: AudiobookDao
    fun isAudiobookDaoInitialized() :Boolean = this::audiobookDao.isInitialized
    lateinit var directoryDao: DirectoryDao
    fun isdirectoriesDaoInitialized() :Boolean = this::directoryDao.isInitialized

    companion object {
        lateinit var instance: Fable private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.plant(Timber.DebugTree())
        initalizeDependecies()

        val theme = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("theme", "-1")
        if (theme != null) {
            AppCompatDelegate.setDefaultNightMode(theme.toInt())
        }
    }

    fun initalizeDependecies(){
        audiobookDao = FableDatabase.getInstance(applicationContext).audiobookDao
        directoryDao = FableDatabase.getInstance(applicationContext).directoryDao

        ExoPlayerMasterObject.setSharedPreferencesAndListener(PreferenceManager.getDefaultSharedPreferences(applicationContext))
        ExoPlayerMasterObject.audiobookDao = audiobookDao
    }

    fun setTheme(){

    }

}