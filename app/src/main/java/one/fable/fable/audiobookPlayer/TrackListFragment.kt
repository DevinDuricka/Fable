package com.example.appleaudiobookcoverapi.exoplayer

import android.app.Activity
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.DisplayMetrics
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import one.fable.fable.R
import one.fable.fable.databinding.TrackListBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber

class TrackListFragment : Fragment(R.layout.track_list) {
    private lateinit var binding: TrackListBinding
    private val trackListAdapter = TrackListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(R.transition.change_bounds_and_transform)

        binding = TrackListBinding.bind(view)
        binding.setLifecycleOwner(this)

        binding.trackList.adapter = trackListAdapter
        trackListAdapter.submitList(ExoPlayerMasterObject.audioPlaybackWindows)

        //val args = TrackListFragmentArgs.fromBundle(requireArguments())


        val displayMetrics = DisplayMetrics()

        Timber.i("The recylerview is this many high: " + displayMetrics.heightPixels)

        //https://stackoverflow.com/questions/52504534/kotlin-recyclerview-scrolltopositionwithoffset-not-showing-up
        //https://stackoverflow.com/questions/37270265/how-to-center-the-clicked-position-in-the-recyclerview/44854796
        (binding.trackList.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(ExoPlayerMasterObject.exoPlayer.currentWindowIndex, 500)
        //binding.trackList.scrollToPosition(ExoPlayerInterface.exoPlayer.currentWindowIndex)

        view.doOnPreDraw { startPostponedEnterTransition() }

    }
}