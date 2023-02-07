package one.fable.fable.audiobookPlayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import one.fable.fable.databinding.TrackListBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject

class TrackListBottomSheetDialog : BottomSheetDialogFragment()  {
    lateinit var binding : TrackListBinding
    private val trackListTitleAdapter = TrackListTitleAdapter {
        onClickCallback(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TrackListBinding.inflate(inflater, container, false)

        binding.trackList.adapter = trackListTitleAdapter
        trackListTitleAdapter.submitList(ExoPlayerMasterObject.getListOfTimelineWindows())

        binding.trackList.scrollToPosition(ExoPlayerMasterObject.exoPlayer.currentMediaItemIndex)

        return binding.root
    }

    private fun onClickCallback(position : Int) {
        if (position != ExoPlayerMasterObject.exoPlayer.currentMediaItemIndex){
            ExoPlayerMasterObject.selectTrack(position)
        }
        this.dismiss()
    }
}