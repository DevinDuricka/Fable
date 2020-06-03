package one.fable.fable

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import one.fable.fable.database.FableDatabase
import one.fable.fable.database.daos.AudiobookDao
import one.fable.fable.database.daos.DirectoryDao
import one.fable.fable.exoplayer.AudioPlayerService
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber

class Fable : Application() {

    lateinit var audiobookDao: AudiobookDao
    fun isAudiobookDaoInitialized() :Boolean = this::audiobookDao.isInitialized
    lateinit var directoryDao: DirectoryDao
    fun isdirectoriesDaoInitialized() :Boolean = this::directoryDao.isInitialized

    override fun onCreate() {
        super.onCreate()
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

        ExoPlayerMasterObject.buildExoPlayer(applicationContext)
        ExoPlayerMasterObject.setSharedPreferencesAndListener(PreferenceManager.getDefaultSharedPreferences(applicationContext))
        ExoPlayerMasterObject.audiobookDao = audiobookDao
    }

    fun setTheme(){

    }

}