package ki.zq.remfp.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ki.zq.remfp.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}