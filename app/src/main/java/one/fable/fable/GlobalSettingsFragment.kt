package one.fable.fable

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener


class GlobalSettingsFragment : PreferenceFragmentCompat() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var seekbarListener: SharedPreferences.OnSharedPreferenceChangeListener
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val rewindPreference : EditTextPreference? = findPreference("rewind_seconds")
        val fastForwardPreference : EditTextPreference? = findPreference("fastforward_seconds")
        val speedSeekBar : SeekBarPreference? = findPreference("playback_speed_seekbar")
        val theme : ListPreference? = findPreference("theme")

        val settingChangeEventListener: SharedPreferences.OnSharedPreferenceChangeListener
        //val seekbar : SeekBarPreference? = findPreference("playback_speed")


        rewindPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        fastForwardPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        theme?.setOnPreferenceChangeListener(object : Preference.OnPreferenceChangeListener{
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                when (newValue){
                    "1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "2" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "-1" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                return true
            }
        })
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        speedSeekBar?.title = speedSeekBar?.value?.let { setSpeedSeekBarText(it) }

        seekbarListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "playback_speed_seekbar"){
                speedSeekBar?.title = setSpeedSeekBarText(sharedPreferences.getInt(key, 10))
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(seekbarListener)

    }

    fun setSpeedSeekBarText(speed: Int) : String{
        val playbackSpeedFloat = speed/10.0
        return "Default playback speed: " + playbackSpeedFloat + "x"
    }
}
