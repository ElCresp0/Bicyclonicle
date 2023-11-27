package pg.eti.bicyclonicle.app_settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import pg.eti.bicyclonicle.R


/**
 * Preferences are managed by SharedPreferences so there is no binding.
 */
class AppSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_settings_preferences, rootKey)

        val appVersionPreference = findPreference<EditTextPreference>("key_app_version")
        appVersionPreference?.summary = getAppVersionName(requireContext())
    }

    @Suppress("DEPRECATION")
    private fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context
                .packageName, PackageManager.GET_META_DATA)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }
}