package com.kal

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kal.databinding.ActivityAboutBinding
import com.kal.settings.Config
import com.kal.settings.Preferences
import com.kal.utils.InteractionAndroid
import kotlin.properties.Delegates

class AboutActivity : AppCompatActivity() {

    private var binding: ActivityAboutBinding by Delegates.notNull()
    private var preferences: Preferences by Delegates.notNull()

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = Preferences(this)

        if (!Config.isDynamicColor) {
            setTheme(resources.getIdentifier(preferences.getColor(), "style", packageName))
        } else {
            setTheme(R.style.dynamicColors)
        }

        val typedValue = TypedValue()
        this.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = ContextCompat.getColor(this, typedValue.resourceId)

        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater).also { setContentView(it.root) }

        binding.versionText.text = getAppVersionName()

        binding.developerLayout.setOnClickListener {
            InteractionAndroid.openUrl(getString(R.string.github_url), this)
        }

        binding.licensesLayout.setOnClickListener {
            val intent = Intent(this, LicensesActivity::class.java)
            startActivity(intent)
        }

        binding.privacyPolicyLayout.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showPrivacyPolicyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.privacy_dialog_title))
            .setMessage(getString(R.string.privacy_dialog_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "1.0.0"
        }
    }
}
