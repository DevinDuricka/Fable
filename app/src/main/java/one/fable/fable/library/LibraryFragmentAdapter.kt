package one.fable.fable.library

import android.view.LayoutInflater.from
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.fable.fable.R
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.databinding.LibraryItemBinding
import one.fable.fable.durationRemaining
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber

class LibraryFragmentAdapter : ListAdapter<Audiobook, LibraryItemViewHolder>(LibraryDataDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryItemViewHolder {
        return LibraryItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: LibraryItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class LibraryItemViewHolder private constructor(val binding: LibraryItemBinding) : RecyclerView.ViewHolder(binding.root){

    fun bind(item: Audiobook){
        binding.gridItemTitle.text = item.audiobookTitle
        binding.gridItemAuthor.text = item.audiobookAuthor
        if (item.imgThumbnail != null){
            if (item.inCloud){
                binding.coverImage.setImageResource(R.drawable.ic_cloud)
            } else {
                binding.coverImage.setImageURI(item.imgThumbnail)
            }
        } else {
            binding.coverImage.setImageResource(R.drawable.ic_album)
        }

        binding.audiobookDuration.text =
            durationRemaining(item.timelineDuration, item.duration)
            //item.timelineDuration.millisToHoursMinutesSecondsString() + "/" + item.duration.millisToHoursMinutesSecondsString()
        binding.libraryItemProgressbar.max = item.duration.toInt()
        binding.libraryItemProgressbar.progress = item.timelineDuration.toInt()


        binding.coverImage.transitionName = item.audiobookTitle + "cover_image"
        binding.gridItemTitle.transitionName = item.audiobookTitle
        binding.gridItemAuthor.transitionName = item.audiobookTitle + item.audiobookAuthor
        //binding.libraryItemCardView.transitionName = item.audiobookTitle + "material_card"
        binding.libraryItemProgressbar.transitionName = item.audiobookTitle + "progressbar"

        binding.root.setOnClickListener {
            ExoPlayerMasterObject.loadAudiobook(item) //Todo - Switch this to a callback at some point

            //https://medium.com/@serbelga/shared-elements-in-android-navigation-architecture-component-bc5e7922ecdf
            val extras = FragmentNavigatorExtras(
                binding.coverImage to "player_view_cover_image",
                binding.gridItemTitle to "player_view_title",
                binding.gridItemAuthor to "player_view_author",
                //binding.libraryItemCardView to "player_view_card",
                binding.libraryItemProgressbar to "progressbar"
            )

            binding.root.findNavController().navigate(
                R.id.action_libraryFragment_to_audiobookPlayerFragment,
                null,
                null,
                extras
            )
        }

        binding.root.setOnLongClickListener {
            Timber.i("Long Press" + item.audiobookTitle)
            val bottomSheet = BottomSheetChangeAudiobookProgress(item)
            val transaction = (binding.root.context as AppCompatActivity).supportFragmentManager.beginTransaction()
            bottomSheet.show(transaction, "Test")
            true
        }
    }

    companion object {
        fun from(parent: ViewGroup): LibraryItemViewHolder {
            val layoutInflater = from(parent.context)

            val binding = LibraryItemBinding.inflate(layoutInflater, parent, false)

            return LibraryItemViewHolder(binding)
        }
    }
}

class LibraryDataDiffCallback : DiffUtil.ItemCallback<Audiobook>() {

    override fun areItemsTheSame(oldItem: Audiobook, newItem: Audiobook): Boolean {
        return oldItem.audiobookId == newItem.audiobookId
    }

    override fun areContentsTheSame(oldItem: Audiobook, newItem: Audiobook): Boolean {
        return oldItem == newItem
    }
}
