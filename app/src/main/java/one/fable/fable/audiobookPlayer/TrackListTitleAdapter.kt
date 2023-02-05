package one.fable.fable.audiobookPlayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.fable.fable.databinding.TrackListItemBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject

class TrackListTitleAdapter(private val onClickCallback : (Int) -> Unit) : ListAdapter<String, TrackListTitleViewHolder>(
    TrackListTitleDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackListTitleViewHolder {
        return TrackListTitleViewHolder.from(parent, onClickCallback = onClickCallback)
    }

    override fun onBindViewHolder(holder: TrackListTitleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }
}

class TrackListTitleViewHolder private constructor(val binding: TrackListItemBinding, private val onClickCallback: (Int) -> Unit) : RecyclerView.ViewHolder(binding.root){
    fun bind (title: String, position : Int){
        if (position == ExoPlayerMasterObject.exoPlayer.currentMediaItemIndex) {
            binding.trackListItemName.text = "$title - Now Playing..."
            binding.trackListItemEqualizer.visibility = View.VISIBLE
            binding.root.alpha = 0.5f
        } else {
            binding.trackListItemName.text = title
            binding.trackListItemEqualizer.visibility = View.GONE
            binding.root.alpha = 1f
        }

        binding.root.setOnClickListener {
            onClickCallback(position)
        }
    }

companion object{
    fun from(parent: ViewGroup, onClickCallback: (Int) -> Unit) : TrackListTitleViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrackListItemBinding.inflate(layoutInflater, parent, false)
        return TrackListTitleViewHolder(binding, onClickCallback)
    }
}
}

class TrackListTitleDiffCallback  : DiffUtil.ItemCallback<String>(){
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}
