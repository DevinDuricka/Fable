package one.fable.fable.library

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import one.fable.fable.MainActivity

import one.fable.fable.R
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.database.entities.PROGRESS_FINISHED
import one.fable.fable.database.entities.PROGRESS_IN_PROGRESS
import one.fable.fable.database.entities.PROGRESS_NOT_STARTED
import one.fable.fable.databinding.LibraryFragmentBinding
import one.fable.fable.databinding.LibraryTabFragmentBinding
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import one.fable.fable.millisToHoursMinutesSecondsString
import timber.log.Timber
import kotlin.Exception

val libraryTabText = arrayOf(
    R.string.not_started,
    R.string.in_progress,
    R.string.finished
)

val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

class LibraryFragment : Fragment(R.layout.library_fragment) {
    private lateinit var binding : LibraryFragmentBinding
    private lateinit var libraryFragmentViewModel: LibraryFragmentViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)
        //setHasOptionsMenu(true)
        binding = LibraryFragmentBinding.bind(view)
        binding.setLifecycleOwner(this)
        libraryFragmentViewModel = ViewModelProvider(requireActivity()).get(LibraryFragmentViewModel::class.java)

        //TODO: actually do something with the swipe to refresh functionality
//        binding.swipeRefreshLayoutLibrary.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener{
//            override fun onRefresh() {
//                Timber.i("Pull to refresh")
//                (activity as MainActivity).scanDirectories()
//                binding.swipeRefreshLayoutLibrary.isRefreshing = false
//            }
//        })

        //todo: change the color for the refresh
        //binding.swipeRefreshLayoutLibrary.setColorSchemeColors(resources.getColor(R.color.colorAccent) )
        //binding.swipeRefreshLayoutLibrary.isRefreshing = true

        val libraryPagerAdapter = LibraryPagerAdapter(this, libraryFragmentViewModel)
        binding.viewPagerLibrary.adapter = libraryPagerAdapter

        TabLayoutMediator(binding.tabLayoutLibrary, binding.viewPagerLibrary){tab, position ->
            tab.text = getString(libraryTabText[position])
            }.attach()

//        binding.libraryExoplayer.player = ExoPlayerMasterObject.exoPlayer
//        binding.libraryExoplayer.showTimeoutMs = -1

//        if (ExoPlayerMasterObject.isAudiobookInitialized()){
//            //BottomSheetBehavior.from(binding.libraryExoplayer).state = BottomSheetBehavior.STATE_EXPANDED
//            binding.libraryExoplayer.visibility = View.VISIBLE
//            //https://stackoverflow.com/questions/9685658/add-padding-on-view-programmatically
//            val scale = resources.displayMetrics.density
//            binding.viewPagerLibrary.setPadding(0,0,0, (90*scale + 0.5F).toInt())
//
//            val clickableArea = binding.libraryExoplayer.findViewById<ConstraintLayout>(R.id.library_bottom_audiobook_player_clickable_area)
//            clickableArea.setOnClickListener { findNavController().navigate(R.id.action_libraryFragment_to_audiobookPlayerFragment) }
//
//
//            val chapterNameObserver = Observer<String> {
//                binding.libraryExoplayer.findViewById<TextView>(R.id.library_bottom_chapter_name).text = it
//            }
//            ExoPlayerMasterObject.chapterName.observe(viewLifecycleOwner, chapterNameObserver)
//
//            binding.libraryExoplayer.findViewById<TextView>(R.id.library_bottom_book_name).text = ExoPlayerMasterObject.audiobook.audiobookTitle
//
//            val cover = binding.libraryExoplayer.findViewById<ImageView>(R.id.library_bottom_cover_image)
//            if (ExoPlayerMasterObject.audiobook.imgThumbnail != null){
//                cover.setImageURI(ExoPlayerMasterObject.audiobook.imgThumbnail)
//            } else {
//                cover.visibility = View.GONE
//            }
//
//        } else {
//            //BottomSheetBehavior.from(binding.libraryExoplayer).state = BottomSheetBehavior.STATE_HIDDEN
//
//            binding.libraryExoplayer.visibility = View.GONE
//        }

        if (libraryFragmentViewModel.firstLoad) {
            if (libraryFragmentViewModel.anyAudiobook != null){
                binding.tabLayoutLibrary.getTabAt(PROGRESS_IN_PROGRESS)?.select()
                binding.viewPagerLibrary.currentItem = PROGRESS_IN_PROGRESS
            } else {
                binding.tabLayoutLibrary.getTabAt(PROGRESS_NOT_STARTED)?.select()
                binding.viewPagerLibrary.currentItem = PROGRESS_NOT_STARTED
            }
            libraryFragmentViewModel.firstLoad = false
        }

        binding.libraryAppBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId){
                R.id.library_add_audiobook_directory ->{
                    addDirectory()
                    true
                }
                R.id.library_settings -> {
                    findNavController().navigate(R.id.action_libraryFragment_to_globalSettingsFragment)
                    true
                }
                else -> false
            }
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val firstLibraryLoad = sharedPreferences.getBoolean("first_library_load", true)
        if (firstLibraryLoad) {
            try {
                TapTargetView.showFor(
                    activity,
                    TapTarget.forView(
                        requireActivity().findViewById<ActionMenuItemView>(R.id.library_add_audiobook_directory),
                        "Add audiobook folder",
                        "New books will automatically be added to your library as you add them to the folder"
                    )
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
            sharedPreferences.edit().putBoolean("first_library_load", false).apply()
        }

        //view.doOnPreDraw { startPostponedEnterTransition() }


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val firstLibraryLoad = sharedPreferences.getBoolean("first_library_load", true)
        if (firstLibraryLoad) {
            Handler().post(object : Runnable {
                override fun run() {
                    try {
                        TapTargetView.showFor(
                            activity,
                            TapTarget.forView(
                                activity?.findViewById<ActionMenuItemView>(R.id.library_add_audiobook_directory),
                                "Add audiobook folder", "New books will automatically be added to your library as you add them to the folder"
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            })
            sharedPreferences.edit().putBoolean("first_library_load", false).apply()
        }




        //TODO: Add search functionality

        //val search = menu.findItem(R.id.app_bar_search)
        //val searchView = search.actionView as SearchView
//        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                Timber.i(query)
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                Timber.i(newText)
//                return false
//            }
//        })
//
//        search.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
//
//            //https://stackoverflow.com/questions/9327826/searchviews-oncloselistener-doesnt-work/18186164
//            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
//                binding.tabLayoutLibrary.visibility = View.VISIBLE
//                //submitAllAudiobooksToAdapterList()
//                return true
//            }
//
//            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
//                binding.tabLayoutLibrary.visibility = View.GONE
//                return true
//            }
//        })

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.library_add_audiobook_directory -> {
                addDirectory()
                true
            }
            R.id.library_settings -> {
                findNavController().navigate(R.id.action_libraryFragment_to_globalSettingsFragment)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private final val OPEN_DIRECTORY_REQUEST_CODE = 0xf11e
    fun addDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { flags }
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
    }

    class LibraryPagerAdapter(fragment: Fragment, val viewModel: LibraryFragmentViewModel) : FragmentStateAdapter(fragment){
        override fun getItemCount(): Int {
            return libraryTabText.size
        }

        override fun createFragment(position: Int): Fragment {
            return LibraryTabFragment(position, viewModel)
        }
    }


    class LibraryTabFragment(private val position: Int,
                             private val viewModel: LibraryFragmentViewModel)
        : Fragment(R.layout.library_tab_fragment){

        private lateinit var binding : LibraryTabFragmentBinding
        private val libraryItemAdapter = LibraryFragmentAdapter()

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            Timber.i("LibraryTabFragment Created")
            super.onViewCreated(view, savedInstanceState)

            binding = LibraryTabFragmentBinding.bind(view)
            binding.lifecycleOwner = parentFragment

            binding.recyclerViewLibrary.adapter = libraryItemAdapter

            when (position){
                PROGRESS_IN_PROGRESS -> {
                    viewModel.inProgressAudiobooks.observe(viewLifecycleOwner, Observer {
                        it?.let {
                            libraryItemAdapter.submitList(it)
                        }
                    })
                }
                PROGRESS_NOT_STARTED -> {
                    viewModel.notStartedAudiobooks.observe(viewLifecycleOwner, Observer {
                        it?.let {
                            libraryItemAdapter.submitList(it)
                        }
                    })
                }
                PROGRESS_FINISHED -> {
                    viewModel.completeAudiobooks.observe(viewLifecycleOwner, Observer {
                        it?.let {
                            libraryItemAdapter.submitList(it)
                        }
                    })
                }
            }



        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.i(data.toString())
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val directoryUri = data?.data ?: return
            (activity as MainActivity).takePersistablePermissionsToDatabaseEntries(directoryUri)
        }
    }

}
