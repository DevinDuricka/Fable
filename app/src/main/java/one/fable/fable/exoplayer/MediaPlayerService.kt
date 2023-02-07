package one.fable.fable.exoplayer

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

// Extend MediaSessionService
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, ExoPlayerMasterObject.exoPlayer).build()
    }
    // Return a MediaSession to link with the MediaController that is making this request.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession?
            = mediaSession

}
