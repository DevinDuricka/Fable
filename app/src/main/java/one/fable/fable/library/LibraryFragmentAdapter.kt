package one.fable.fable.library

import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.fable.fable.R
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.databinding.LibraryItemBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import one.fable.fable.millisToHoursMinutesSecondsString
import timber.log.Timber

class LibraryFragmentAdapter() : ListAdapter<Audiobook, LibraryItemViewHolder>(LibraryDataDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryItemViewHolder {
        return LibraryItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: LibraryItemViewHolder, position: Int) {
        val item = getItem(position)

        val progressObserver = Observer<Long> {
            Timber.i("3 Progress is: " + ExoPlayerMasterObject.progress.value)
            Timber.i("4 Progress is:" + it.toString())
            item.duration = it
        }
        holder.bind(item)

        if (ExoPlayerMasterObject.isAudiobookInitialized() && item.audiobookId == ExoPlayerMasterObject.audiobook.audiobookId){
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0,0)
        } else {
            holder.itemView.visibility = View.VISIBLE
            //holder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        holder.binding.lifecycleOwner?.let { ExoPlayerMasterObject.progress.observe(it, progressObserver) }
    }
}

class LibraryItemViewHolder private constructor(val binding: LibraryItemBinding) : RecyclerView.ViewHolder(binding.root) {
    public fun bind(item: Audiobook){

//        if (ExoPlayerMasterObject.isAudiobookInitialized() && item.audiobookId == ExoPlayerMasterObject.audiobook.audiobookId){
//            binding.root.visibility = View.GONE
//        } else {
//            binding.root.visibility = View.VISIBLE
//        }

        binding

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


        if (ExoPlayerMasterObject.isAudiobookInitialized() && item.audiobookId == ExoPlayerMasterObject.audiobook.audiobookId){
            Timber.i("Progress is: " + ExoPlayerMasterObject.progress.value)
            binding.audiobookDuration.text = ExoPlayerMasterObject.progress.value?.millisToHoursMinutesSecondsString()
            val progressObserver = Observer<Long> {
                Timber.i("1 Progress is: " + ExoPlayerMasterObject.progress.value)
                Timber.i("2 Progress is:" + it.toString())
                binding.audiobookDuration.text = it.millisToHoursMinutesSecondsString() + "/" + item.duration.millisToHoursMinutesSecondsString()
                binding.libraryItemProgressbar.max = item.duration.toInt()
                binding.libraryItemProgressbar.progress = it.toInt()
            }

            binding.root.parent
            binding.lifecycleOwner?.let { ExoPlayerMasterObject.progress.observe(it, progressObserver) }
            //chapterName.observe(viewLifecycleOwner, chapterNameObserver)
        } else {
            binding.audiobookDuration.text =
                item.timelineDuration.millisToHoursMinutesSecondsString() + "/" + item.duration.millisToHoursMinutesSecondsString()
            binding.libraryItemProgressbar.max = item.duration.toInt()
            binding.libraryItemProgressbar.progress = item.timelineDuration.toInt()
        }

        binding.coverImage.transitionName = item.audiobookTitle + "cover_image"
        binding.gridItemTitle.transitionName = item.audiobookTitle
        binding.gridItemAuthor.transitionName = item.audiobookTitle + item.audiobookAuthor
        binding.libraryItemCardView.transitionName = item.audiobookTitle + "material_card"
        binding.libraryItemProgressbar.transitionName = item.audiobookTitle + "progressbar"

        binding.root.setOnClickListener {
            binding.progressCircularLibraryItemLoadingBook.visibility = View.VISIBLE
            //todo: assigning the audiobook is probably hacky. We should add a loading animation and checks, etc.
            var loadNewBook = false

            if (!ExoPlayerMasterObject.isAudiobookInitialized()){
                ExoPlayerMasterObject.audiobook = item
                loadNewBook = true
            } else if (ExoPlayerMasterObject.audiobook.audiobookId != item.audiobookId){
                Timber.i("Books should be different")
                runBlocking {
                    ExoPlayerMasterObject.updateAudiobookObjectLocation()
                    ExoPlayerMasterObject.updateAudiobook()
                }
                ExoPlayerMasterObject.audiobook = item
                loadNewBook = true
            }

            if (loadNewBook){
                runBlocking {
                    ExoPlayerMasterObject.loadTracks(binding.root.context, item.audiobookTitle)

                }
            }
            //https://medium.com/@serbelga/shared-elements-in-android-navigation-architecture-component-bc5e7922ecdf
            val extras = FragmentNavigatorExtras(
                binding.coverImage to "player_view_cover_image",
                binding.gridItemTitle to "player_view_title",
                binding.gridItemAuthor to "player_view_author",
                binding.libraryItemCardView to "player_view_card",
                binding.libraryItemProgressbar to "progressbar"
            )

            binding.root.findNavController().navigate(R.id.action_libraryFragment_to_audiobookPlayerFragment, null, null, extras)



            //binding.root.findNavController().navigate(LibraryFragmentDirections.actionLibraryFragmentToAudiobookPlayerFragment(item.audiobookTitle, item.audiobookAuthor.toString(), item.imgThumbnail.toString()), extras)
            binding.progressCircularLibraryItemLoadingBook.visibility = View.GONE
        }
    }

    companion object {
        public fun from(parent: ViewGroup): LibraryItemViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
//            val view = layoutInflater.inflate(
//                R.layout.text_item_view, parent, false
//            ) as TextView

            val binding = LibraryItemBinding.inflate(layoutInflater, parent, false)

            //val binding = TextItemViewBinding.inflate(layoutInflater, parent, false)

            return LibraryItemViewHolder(binding)

            //return TextItemViewHolder(view)
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
