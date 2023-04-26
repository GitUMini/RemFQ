package ki.zq.remfq.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ki.zq.remfq.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}