package one.fable.fable

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import timber.log.Timber


class GlobalSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rewindPreference : EditTextPreference? = findPreference("rewind_seconds")
        val fastForwardPreference : EditTextPreference? = findPreference("fastforward_seconds")
        val speedSeekBar : SeekBarPreference? = findPreference("playback_speed_seekbar")
        val theme : ListPreference? = findPreference("theme")

        //--Rewind Preference--
        rewindPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER //Show only numbers for the rewind preference editor
        }
        //Add a listener to show a toast when the rewind speed changes
        //Don't add to the editTextListener above as it will show the toast when the keyboard appears
        rewindPreference?.setOnPreferenceChangeListener { _, _ ->
            Toast.makeText(context, "Preference will be reflected after app restart", Toast.LENGTH_LONG).show()
            true
        }


        //--Fast Forward Preference
        fastForwardPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER //Show only numbers for the fast forward preference editor
        }
        //Add a listener to show a toast when the fast forward speed changes
        //Don't add to the editTextListener above as it will show the toast when the keyboard appears
        fastForwardPreference?.setOnPreferenceChangeListener { _, _ ->
            Toast.makeText(context, "Preference will be reflected after app restart", Toast.LENGTH_LONG).show()
            true
        }

        //Theme preference listener
        theme?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue){
                "1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "2" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "-1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        //Speed Seekbar Preference
        speedSeekBar?.title = speedSeekBar?.value?.let { formatSpeedSeekBarText(it) } //This will initialize the speedseekbar with the applicable text
        speedSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            try {
                speedSeekBar.title = formatSpeedSeekBarText(newValue as Int) //The new value should always be an Int, but just in case, catch it
            } catch (e : Exception) {
                Timber.e(e)
            }
            true
        }
    }

    private fun formatSpeedSeekBarText(speed: Int) : String{
        val playbackSpeedFloat = speed/10.0
        return "Default playback speed: " + playbackSpeedFloat + "x"
    }
}
