package com.kal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kal.databinding.ActivityLicensesBinding
import com.kal.licenses.License
import com.kal.licenses.LicenseActionListener
import com.kal.licenses.LicenseAdapter
import com.kal.settings.Config
import com.kal.settings.Preferences
import com.kal.utils.InteractionAndroid
import kotlin.properties.Delegates.notNull

class LicensesActivity : AppCompatActivity() {

    private var binding: ActivityLicensesBinding by notNull()
    private var preferences: Preferences by notNull()
    private var adapter: LicenseAdapter by notNull()

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = Preferences(this)

        if (!Config.isDynamicColor){
            setTheme(resources.getIdentifier(preferences.getColor(), "style", packageName))
        }else{
            setTheme(R.style.dynamicColors)
        }

        val typedValue = TypedValue()
        this.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = ContextCompat.getColor(this, typedValue.resourceId)


        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater).also { setContentView(it.root) }


        adapter = LicenseAdapter(this, object : LicenseActionListener {
            override fun onSelectLicense(license: License) {
                InteractionAndroid.openUrl(license.url, this@LicensesActivity)
            }
        })

        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter


        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
    }
}
