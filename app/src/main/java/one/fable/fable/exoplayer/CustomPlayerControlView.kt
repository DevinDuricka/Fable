package com.example.appleaudiobookcoverapi

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.media3.ui.PlayerControlView
import androidx.preference.PreferenceManager
import one.fable.fable.R
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.TimeUnit

class CustomPlayerControlView : PlayerControlView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr : Int) : super (context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr : Int, playbackAttributeSet: AttributeSet?) : super (context, attributeSet, defStyleAttr, playbackAttributeSet)

    val customRewind : ImageButton = findViewById(R.id.exo_audiobook_rewind)
    val customFastForward : ImageButton = findViewById(R.id.exo_audiobook_ffwd)

    private val rewind = "rewind"
    private val fastForward = "fastForward"

    init {
        customRewind.setOnClickListener{audiobookSeekInterval(rewind)}
        customFastForward.setOnClickListener{audiobookSeekInterval(fastForward)}
    }


    fun audiobookSeekInterval(seekDirection : String){

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val rewindAmountInSeconds = sharedPreferences.getString("rewind_seconds", "")
        val fastforwardAmountInSeconds = sharedPreferences.getString("fastforward_seconds", "")
        var rewindAmount : Long
        var fastforwardAmount : Long

        try {
            rewindAmount = TimeUnit.SECONDS.toMillis(rewindAmountInSeconds!!.toLong())
        } catch (e: Exception){
            rewindAmount = 10000L
        }

        try{
            fastforwardAmount = TimeUnit.SECONDS.toMillis(fastforwardAmountInSeconds!!.toLong())
        } catch (e: Exception) {
            fastforwardAmount = 30000L
        }

        val player = super.getPlayer()
        if(player != null){
            val timeline = player.currentTimeline
            if (!timeline.isEmpty){
                if (seekDirection == rewind){
                    ExoPlayerMasterObject.customRewind(rewindAmount)
                } else if (seekDirection == fastForward){
                    ExoPlayerMasterObject.customFastForward(fastforwardAmount)
                }
            }
        }
    }


}



