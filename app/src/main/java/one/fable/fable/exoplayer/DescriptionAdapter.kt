package one.fable.fable.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import one.fable.fable.MainActivity


class DescriptionAdapter(context: Context) : PlayerNotificationManager.MediaDescriptionAdapter {
    val context = context

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        //TODO Open directly to the fragment
        //https://stackoverflow.com/questions/26608627/how-to-open-fragment-page-when-pressed-a-notification-in-android

        //val intent = Intent(context, MainActivity::class.java)
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        val window = Timeline.Window()
//        player.currentTimeline.getWindow(player.currentWindowIndex, window)
//
//        return window.tag.toString()
        return ExoPlayerMasterObject.audioPlaybackWindows[player.currentWindowIndex].Name.toString()
    }

    override fun getCurrentContentTitle(player: Player): CharSequence {
        //val window = Timeline.Window()
        //val period = Timeline.Period()
        //player.currentTimeline.getWindow(player.currentWindowIndex, window)
        //player.currentTimeline.getPeriod(player.currentPeriodIndex, period)

        return ExoPlayerMasterObject.audiobook.audiobookTitle


        //return "Test 1"
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        val bitmapUri = ExoPlayerMasterObject.audiobook.imgThumbnail
        if (bitmapUri != null){
            var bitmapSource : Bitmap? = null
            try {
                //https://stackoverflow.com/questions/56651444/deprecated-getbitmap-with-api-29-any-alternative-codes
                //"getBitmap" is deprecated. Someday I'll have to fix this. But clearly today is not that day.

                //https://stackoverflow.com/questions/48729200/how-to-convert-uri-to-bitmap/48729608
                bitmapSource = MediaStore.Images.Media.getBitmap(context.contentResolver, bitmapUri)
            } catch(e: Exception) {
                timber.log.Timber.e(e)
            }
            //val bitmapSource = bitmapUri?.let { ImageDecoder.createSource(context.contentResolver, it) }
            return bitmapSource

        } else{
            return null
        }

        //return MediaStore.Images.Media.getBitmap()
        //return context.resources.getDrawable()
        //return ExoPlayerInterface.ExoPlayerCompanionObject.coverUri.
    }
}
