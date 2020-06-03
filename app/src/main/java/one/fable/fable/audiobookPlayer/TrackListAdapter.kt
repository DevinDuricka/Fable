package com.example.appleaudiobookcoverapi.exoplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.fable.fable.database.entities.Chapter
import one.fable.fable.databinding.TrackListItemBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import one.fable.fable.R
import one.fable.fable.database.entities.AudioPlaybackWindow

class TrackListAdapter() : ListAdapter<AudioPlaybackWindow, TrackListItemViewHolder>(TrackListDataDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackListItemViewHolder {
        return TrackListItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TrackListItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }
}

class TrackListItemViewHolder private constructor(val binding: TrackListItemBinding) : RecyclerView.ViewHolder(binding.root){
    fun bind (item: AudioPlaybackWindow, position : Int){
        binding.trackListItemName.text = item.Name



        if (position == ExoPlayerMasterObject.exoPlayer.currentWindowIndex){
            binding.trackListItemName.transitionName = "track_name"
            binding.trackListItemCard.transitionName = "track_name_card"
            binding.trackListItemEqualizer.visibility = View.VISIBLE
        } else {
            binding.trackListItemEqualizer.visibility = View.GONE
            binding.trackListItemName.transitionName = null
            binding.trackListItemCard.transitionName = null
        }

        binding.root.setOnClickListener {
            //todo
            ExoPlayerMasterObject.selectTrack(position)

            binding.trackListItemName.transitionName = "track_name"
            binding.trackListItemCard.transitionName = "track_name_card"

            val extras = FragmentNavigatorExtras(
                binding.trackListItemCard to "track_name_card",
                binding.trackListItemName to "exoplayer_track_name_transition"
            )

            //val navOptions = NavOptions.Builder().setPopUpTo(R.id.audiobookPlayerFragment, true).build()

            //binding.root.findNavController().navigate(R.id.action_trackListFragment_to_audiobookPlayerFragment)

            binding.root.findNavController().popBackStack()
        }
    }

companion object{
    fun from(parent: ViewGroup) : TrackListItemViewHolder{
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrackListItemBinding.inflate(layoutInflater, parent, false)
        return TrackListItemViewHolder(binding)
    }
}
}

class TrackListDataDiffCallback  : DiffUtil.ItemCallback<AudioPlaybackWindow>(){
    override fun areItemsTheSame(oldItem: AudioPlaybackWindow, newItem: AudioPlaybackWindow): Boolean {
        return oldItem.Name == newItem.Name && oldItem.startPos == newItem.startPos
    }

    override fun areContentsTheSame(oldItem: AudioPlaybackWindow, newItem: AudioPlaybackWindow): Boolean {
        return oldItem == newItem
    }
}
