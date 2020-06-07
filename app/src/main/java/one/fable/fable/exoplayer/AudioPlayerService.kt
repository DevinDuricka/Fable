package one.fable.fable.exoplayer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.fable.fable.R
import timber.log.Timber

class AudioPlayerService : Service() {
    lateinit var exoPlayer : SimpleExoPlayer
    lateinit var exoPlayerNotificationManager : PlayerNotificationManager
    lateinit var mediaSession: MediaSessionCompat
    lateinit var mediaSessionConnector: MediaSessionConnector //You need to also depend on com.google.android.exoplayer:extension-mediasession:2.6.1

    val eventListener = ExoPlayerEventListener()

    val binder = LocalBinder()
    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.i("Task Removed")
        stopForeground(true)
        exoPlayer.playWhenReady = false
        //exoPlayer.stop()
        stopSelf()
        //ExoPlayerInterface.


        super.onTaskRemoved(rootIntent)
    }

    inner class LocalBinder : Binder() {
        fun getService() : AudioPlayerService = this@AudioPlayerService
    }

    inner class ExoPlayerEventListener() : Player.EventListener{
        var isPlayingBool = false

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            isPlayingBool = isPlaying
            Timber.i("Is playing changed to: " + isPlaying)
            if(!isPlaying){
                //Timber.i("Remove the notification")

                //I've pretty much figured this out, but felt I should make note of where I found the info
                //https://stackoverflow.com/questions/43596709/make-a-notification-from-a-foreground-service-cancelable-once-the-service-is-not
                //https://github.com/google/ExoPlayer/issues/4256
                stopForeground(false)
                CoroutineScope(Dispatchers.IO).launch {
                    ExoPlayerMasterObject.updateAudiobook()
                }
            }

        }
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Timber.i("OnStartCommand Called")
//        return START_STICKY
//        //return super.onStartCommand(intent, flags, startId)
//    }

    override fun onCreate() {

        Timber.i("On create Called")
        super.onCreate()
        exoPlayer = ExoPlayerMasterObject.exoPlayer
        exoPlayer.addListener(eventListener)
        //(application as Fable).exoPlayer = exoPlayer

        Timber.i("ExoPlayer Attached to the interface")


        //https://medium.com/google-exoplayer/exoplayer-2-11-whats-new-e0e0701e4b6c
        exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK)
        exoPlayer.setHandleAudioBecomingNoisy(true)


        //https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e
        val audioBookAudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()

        exoPlayer.setAudioAttributes(audioBookAudioAttributes, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Audiobook Playback"
            val descriptionText = "Fable audiobook player"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("PLAYBACK", name, importance)
            mChannel.description = descriptionText
            mChannel.setSound(null, null)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        //https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ui/PlayerNotificationManager.html
        //https://medium.com/google-exoplayer/playback-notifications-with-exoplayer-a2f1a18cf93b
        exoPlayerNotificationManager = PlayerNotificationManager(applicationContext, "PLAYBACK", 1, DescriptionAdapter(applicationContext), object:
            PlayerNotificationManager.NotificationListener {

            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                super.onNotificationCancelled(notificationId, dismissedByUser)
                //exoPlayer.stop()
                //stopSelf()
            }


            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean
            ) {
                super.onNotificationPosted(notificationId, notification, ongoing)

                //if(eventListener.isPlayingBool){
                if(exoPlayer.isPlaying){
                    startForeground(notificationId,notification)
                }
                Timber.i("Start Foreground called")
            }
        })

        exoPlayerNotificationManager.setPlayer(exoPlayer)

        //Todo: Implement the mediabuttonreceiver for api 19 https://developer.android.com/guide/topics/media-apps/mediabuttons

        mediaSession = MediaSessionCompat(applicationContext, Context.MEDIA_SESSION_SERVICE)
        mediaSession.isActive = true
        exoPlayerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)
        exoPlayerNotificationManager.setSmallIcon(R.drawable.fable_icon_notification)
        mediaSessionConnector = MediaSessionConnector(mediaSession)
//        mediaSessionConnector.setQueueNavigator(object: TimelineQueueNavigator(mediaSession){
//            override fun getMediaDescription(
//                player: Player?,
//                windowIndex: Int
//            ): MediaDescriptionCompat {
//                TODO("Not yet implemented")
//                return MediaDescriptionCompat.Builder()
//                    .setMediaId()
//                    .setIconBitmap()
//                    .setTitle()
//                    .setDescription()
//                    .setExtras()
//                    .build()
//            }
//        })
        mediaSessionConnector.setPlayer(exoPlayer)


    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onLowMemory() {
        super.onLowMemory()
        stopForeground(true)
        //exoPlayerNotificationManager.setPlayer(null)

    }

    override fun onDestroy() {
        stopForeground(true)
        ExoPlayerMasterObject.cancelSleepTimer()
        exoPlayerNotificationManager.setPlayer(null)
        mediaSessionConnector.setPlayer(null)
        //exoPlayer.stop()
        stopSelf()
        super.onDestroy()

        //https://stackoverflow.com/questions/6330200/how-to-quit-android-application-programmatically
        //I may want to do this if I start getting unintended crashes
    }
//
//    fun getExoPlayerInstance() : SimpleExoPlayer{
//        return exoPlayer
//    }


//    override fun onHandleIntent(intent: Intent?) {
//        // Normally we would do some work here, like download a file.
//        // For our sample, we just sleep for 5 seconds.
//        try {
//
//            //exoPlayer.prepare((Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3")))
//            Thread.sleep(5000)
//        } catch (e: InterruptedException) {
//            // Restore interrupt status.
//            Thread.currentThread().interrupt()
//        }
//
//    }

}