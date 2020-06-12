package one.fable.fable.audiobookPlayer

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.fragment.app.Fragment
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginTop
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import kotlinx.android.synthetic.main.exo_player_control_view.view.*
import one.fable.fable.MainActivity
import one.fable.fable.R
import one.fable.fable.databinding.AudiobookPlayerFragmentBinding
import one.fable.fable.exoplayer.AudioPlayerService
import one.fable.fable.exoplayer.ExoPlayerMasterObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AudiobookPlayerFragment : Fragment(R.layout.audiobook_player_fragment) {

    companion object {
        fun newInstance() = AudiobookPlayerFragment()
    }

    //private lateinit var viewModel: AudiobookPlayerViewModel //= ViewModelProvider(requireActivity()).get(AudiobookPlayerViewModel::class.java)
    //private lateinit var viewModelFactory: AudiobookPlayerViewModelFactory
    private lateinit var binding: AudiobookPlayerFragmentBinding
    private lateinit var sleepTimerTextView: TextView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).startAudioPlayerService()


        super.onViewCreated(view, savedInstanceState)
        //setHasOptionsMenu(true)
        postponeEnterTransition()

        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(R.transition.change_bounds_and_transform)


        binding = AudiobookPlayerFragmentBinding.bind(view)

        binding.playerViewAudiobookTitle.text = ExoPlayerMasterObject.audiobook.audiobookTitle

        if (ExoPlayerMasterObject.audiobook.imgThumbnail != null) {
            binding.playerViewCover.setImageURI(ExoPlayerMasterObject.audiobook.imgThumbnail)
        }
        binding.playerViewAudiobookAuthor.text = ExoPlayerMasterObject.audiobook.audiobookAuthor



        binding.exoplayer.showTimeoutMs = -1
        binding.exoplayer.player = ExoPlayerMasterObject.exoPlayer

        //todo: figure out how to add the multiwindow timebar
        //https://github.com/google/ExoPlayer/issues/2122
        //https://github.com/google/ExoPlayer/issues/6741
        //binding.exoplayer.setShowMultiWindowTimeBar(true)

        val chapterNameObserver = Observer<String> {
            binding.exoplayer.track_name.text = it
        }
        ExoPlayerMasterObject.chapterName.observe(viewLifecycleOwner, chapterNameObserver)

        binding.exoplayer.track_name_card.setOnClickListener {

            //view.height
//            var trackLocationOnScreen = IntArray(2)
//            binding.exoplayer.track_name.getLocationInWindow(trackLocationOnScreen)
            //binding.exoplayer.trackName.getLocationOnScreen(trackLocationOnScreen)
            //binding.exoplayer.trackName.getOffsetForPosition(x, y)
//            Timber.i("Track selector is at this point: " + trackLocationOnScreen[0])
//            Timber.i("Track selector is at this point: " + trackLocationOnScreen[1])
//            Timber.i("Fragment Height: " + view.height)


            val extras = FragmentNavigatorExtras(
                binding.exoplayer.track_name to "track_name",
                binding.exoplayer.track_name_card to "track_name_card"
            )


            findNavController().navigate(R.id.action_audiobookPlayerFragment_to_trackListFragment, null, null, extras)


            //binding.exoplayer

            //findNavController().navigate(AudiobookPlayerFragmentDirections.actionAudiobookPlayerFragmentToTrackListFragment(view.height/2), extras)
        }

        binding.audiobookPlayerAppBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId){
                R.id.audiobook_playback_speed -> {
                    playbackSpeedPicker(requireContext())
                    true
                }
                R.id.audiobook_sleep_timer -> {

                    if (ExoPlayerMasterObject.sleepTimerText.value.isNullOrEmpty()) {
                        sleepTimerNumberPicker(requireContext())
                    } else {
                        cancelOrPauseSleepTimer(requireContext())
                        Timber.i("Sleep Timer Running")
                    }
                    true
                }
                R.id.audiobook_player_to_app_settings -> {
                    findNavController().navigate(R.id.action_audiobookPlayerFragment_to_globalSettingsFragment)
                    true
                }
                else -> false
            }
        }

        //https://stackoverflow.com/questions/44514025/get-menuitems-view-reference-for-taptargetview
        Handler().post(object : Runnable {
            @SuppressLint("RestrictedApi")
            override fun run() {
                val sleepTimer = requireActivity().findViewById<FrameLayout>(R.id.audiobook_sleep_timer)
                //val sleepTimer = activity.findViewById<ActionMenuItemView>(R.id.audiobook_sleep_timer)

                sleepTimerTextView = requireActivity().findViewById(R.id.sleep_timer_text)
                sleepTimer.setOnClickListener {                     if (ExoPlayerMasterObject.sleepTimerText.value.isNullOrEmpty()) {
                                        sleepTimerNumberPicker(requireContext())
                                    } else {
                                        cancelOrPauseSleepTimer(requireContext())
                                        Timber.i("Sleep Timer Running")
                                    } }
                val timerObserver = Observer<String> { timeRemaining ->
                    sleepTimerTextView.text = timeRemaining
                }
                ExoPlayerMasterObject.sleepTimerText.observe(viewLifecycleOwner, timerObserver)
                val playbackSpeed = requireActivity().findViewById<ActionMenuItemView>(R.id.audiobook_playback_speed)
                val playbackSpeedObserver = Observer<String> { exoPlayerPlaybackSpeed ->
                    playbackSpeed.setTitle(exoPlayerPlaybackSpeed)
                }
                ExoPlayerMasterObject.playbackSpeed.observe(viewLifecycleOwner, playbackSpeedObserver)

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val firstAudiobookLoad = sharedPreferences.getBoolean("first_audiobook_load", true)
                if (firstAudiobookLoad) {
                    try {
                        TapTargetSequence(activity).targets(
                            TapTarget.forView(
                                requireActivity().findViewById<ActionMenuItemView>(R.id.audiobook_playback_speed),
                                "Change playback speed",
                                "The default speed can also be changed in the app settings"
                            ),
                            TapTarget.forView(
                                requireActivity().findViewById<FrameLayout>(R.id.audiobook_sleep_timer),
                                "Set a sleep timer",
                                "Your book will automatically pause at the end of the timer"
                            )
                        ).start()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    sharedPreferences.edit().putBoolean("first_audiobook_load", false).apply()
                }
            }
        })



        view.doOnPreDraw { startPostponedEnterTransition() }

    }


//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.audiobook_player_menu, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//
//        val sleepTimer = menu.findItem(R.id.audiobook_sleep_timer)
//        val sleepTimerActionView = sleepTimer.actionView
//
//        sleepTimerTextView = sleepTimerActionView.findViewById<TextView>(R.id.sleep_timer_text)
//        sleepTimerActionView.setOnClickListener { onOptionsItemSelected(sleepTimer) }
//        val timerObserver = Observer<String> { timeRemaining ->
//            sleepTimerTextView.text = timeRemaining
//        }
//        ExoPlayerMasterObject.sleepTimerText.observe(this, timerObserver)
//
//        val playbackSpeed = menu.findItem(R.id.audiobook_playback_speed)
//        //playbackSpeed.title = "2.0x"
//        val playbackSpeedActionView = playbackSpeed.actionView
//
//        //val playbackSpeedTextView = playbackSpeedActionView.findViewById<TextView>(R.id.playback_speed_text)
//        //playbackSpeedActionView.setOnClickListener { onOptionsItemSelected(playbackSpeed) }
//        val playbackSpeedObserver = Observer<String> { exoPlayerPlaybackSpeed ->
//            playbackSpeed.title = exoPlayerPlaybackSpeed
//        }
//        ExoPlayerMasterObject.playbackSpeed.observe(this, playbackSpeedObserver)
//
//        //sleepTimerText.text = "Test"
//        //val timerText = sleepTimer.actionView
//        //timerText.text
//
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        val firstAudiobookLoad = sharedPreferences.getBoolean("first_audiobook_load", true)
//        if (firstAudiobookLoad) {
//            Handler().post(object : Runnable {
//                override fun run() {
//                    try {
//                        TapTargetSequence(activity).targets(
//                            TapTarget.forView(
//                                activity?.findViewById<ActionMenuItemView>(R.id.audiobook_playback_speed),
//                                "Change playback speed",
//                                "The default speed can also be changed in the app settings"
//                            ),
//                            TapTarget.forView(
//                                activity?.findViewById<FrameLayout>(R.id.audiobook_sleep_timer),
//                                "Set a sleep timer",
//                                "Your book will automatically pause at the end of the timer"
//                            )
//                        ).start()
//                    } catch (e: Exception) {
//                        Timber.e(e)
//                    }
//                }
//            })
//            sharedPreferences.edit().putBoolean("first_audiobook_load", false).apply()
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.audiobook_playback_speed -> {
//                playbackSpeedPicker(requireContext())
//                //println("Play back Clicked")
//                //TODO
//                true
//            }
//            R.id.audiobook_sleep_timer -> {
//                //println("Sleep Timer Clicked")
//
//                if (ExoPlayerMasterObject.sleepTimerText.value.isNullOrEmpty()) {
//                    sleepTimerNumberPicker(requireContext())
//                } else {
//                    cancelOrPauseSleepTimer(requireContext())
//                    Timber.i("Sleep Timer Running")
//                }
//                //SleepTimerOptions.SleepTimerOptionsListener{}
//                //val sleepTimerOptions = SleepTimerOptions()
//
//                //sleepTimerOptions.listener.onDialogPositiveClick()
//                //sleepTimerOptions.show(parentFragmentManager, "sleep_timer")
//
//                //TimePickerFragment().show(parentFragmentManager, "timePicker")
//                //ExoPlayerMasterObject.startSleepTimer(10000)
//                //TODO
//                true
//            }
//            R.id.audiobook_player_to_app_settings -> {
//                findNavController().navigate(R.id.action_audiobookPlayerFragment_to_globalSettingsFragment)
//                true
//            }
//            //TODO
////            R.id.replace_album_cover -> {
////                //TODO
////                true
////            }
//            else -> return super.onOptionsItemSelected(item)
//        }
//
//    }


    fun sleepTimerNumberPicker(context: Context) {
        val numberPicker = NumberPicker(context)
        numberPicker.minValue = 1
        numberPicker.maxValue = 1000
        numberPicker.value = 30

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Sleep Timer")
        builder.setMessage("Select a timer length (minutes)")
        builder.setView(numberPicker)
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, which ->
                    ExoPlayerMasterObject.startSleepTimer(TimeUnit.MINUTES.toMillis(numberPicker.value.toLong()))
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which -> })

        builder.show()
    }

    fun cancelOrPauseSleepTimer(context: Context) {
        val textView = TextView(context)
        val builder = AlertDialog.Builder(context)
        val pausedBool = ExoPlayerMasterObject.sleepTimerPausedBoolean
        var neutralButtonText = "Pause"
        var messageAppend = ""
        if (pausedBool) {
            messageAppend = " (Paused)"
            neutralButtonText = "Resume"
        }

        val timerObserver = Observer<String> { timeRemaining ->
            textView.text = timeRemaining + messageAppend
            if (timeRemaining.isNullOrEmpty()) {
                textView.text = "00:00"
            }
        }

        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        //textView.layoutParams.width = MATCH_PARENT
        textView.gravity = TextView.TEXT_ALIGNMENT_GRAVITY
        textView.setTextSize(24F)

        ExoPlayerMasterObject.sleepTimerText.observe(viewLifecycleOwner, timerObserver)

        builder.setMessage("Time remaining: ")
        builder.setTitle("Sleep Timer")

        builder.setView(textView)
            .setPositiveButton("Stop Timer",
                DialogInterface.OnClickListener { dialog, which ->
                    ExoPlayerMasterObject.cancelSleepTimer()
                    //ExoPlayerMasterObject.startSleepTimer(TimeUnit.MINUTES.toMillis(numberPicker.value.toLong()))
                })
            .setNeutralButton(neutralButtonText,
                DialogInterface.OnClickListener { dialog, which ->
                    if (pausedBool) {
                        ExoPlayerMasterObject.resumeSleepTimer()
                    } else {
                        ExoPlayerMasterObject.pauseSleepTimer()
                    }
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which -> })

        builder.show()
    }

    fun playbackSpeedPicker(context: Context) {
        //------------------------------------------------------------------------------------------
        //Get the current speed Int and String that appears in the options menu.
        //These may be nullable since the source variables are MutableLiveData
        val currentSpeed = ExoPlayerMasterObject.playbackSpeedAsInt.value
        val currentSpeedString = ExoPlayerMasterObject.playbackSpeed.value

        //------------------------------------------------------------------------------------------
        //Programatically create the layout
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL

        val textView = TextView(context)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.gravity = TextView.TEXT_ALIGNMENT_GRAVITY
        textView.setTextSize(24F)

        val audiobookSpeedSeekBar = SeekBar(context)
        //Since the seekbar 'value' is actually progress and not a value, it starts at 0.
        //Make the max 79 and add 1 to the progress get the 1 to 80 scale
        audiobookSpeedSeekBar.max = 79

        //Use the current speed to initialize the values
        if (currentSpeed != null) {
            audiobookSpeedSeekBar.progress = currentSpeed - 1
        }
        textView.text = currentSpeedString

        //Create a listener that checks when the slider changes. REMEMBER TO ADD 1 TO THE PROGRESS
        audiobookSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = ExoPlayerMasterObject.convertSpeedIntToString(progress + 1)
                ExoPlayerMasterObject.setPlaybackSpeed(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        linearLayout.addView(textView)
        linearLayout.addView(audiobookSpeedSeekBar)

        //------------------------------------------------------------------------------------------
        //Programatically create the Dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Playback Speed")
        builder.setMessage("Change audiobook speed")

        builder.setView(linearLayout)
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, which ->
                    ExoPlayerMasterObject.updateAudiobookSpeed(audiobookSpeedSeekBar.progress + 1)
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which ->
                    if (currentSpeed != null) {
                        ExoPlayerMasterObject.setPlaybackSpeed(currentSpeed)
                    }
                })
            .setNeutralButton("Reset to Default",
                DialogInterface.OnClickListener { dialog, which ->
                    ExoPlayerMasterObject.resetSpeedToDefault()
                })

        val alert = builder.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }


    class SleepTimerOptions : DialogFragment() {
        internal lateinit var listener: SleepTimerOptionsListener

        interface SleepTimerOptionsListener {
            fun onDialogPositiveClick(dialog: DialogFragment, sleepTimerMinutes: Int)
            fun onDialogNegativeClick(dialog: DialogFragment)
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            try {
                // Instantiate the NoticeDialogListener so we can send events to the host
                listener = context as SleepTimerOptionsListener
            } catch (e: ClassCastException) {
                // The activity doesn't implement the interface, throw exception
                throw ClassCastException(
                    (context.toString() +
                            " must implement NoticeDialogListener")
                )
            }

        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val numberPicker = NumberPicker(activity)
                numberPicker.minValue = 1
                numberPicker.maxValue = 1000
                numberPicker.value = 30

                val builder = AlertDialog.Builder(it)
                val inflater = requireActivity().layoutInflater

                //builder.setView(inflater.inflate(R.layout.sleep_timer_selector, null))
                builder.setTitle("Sleep Timer")
                builder.setMessage("Select a timer length (minutes)")
                builder.setView(numberPicker)
                    .setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialog, which ->

                            listener.onDialogPositiveClick(this, numberPicker.value)
                        })
                    .setNegativeButton("Cancel",
                        DialogInterface.OnClickListener { dialog, which -> })

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")


            //return super.onCreateDialog(savedInstanceState)
        }
    }
}
