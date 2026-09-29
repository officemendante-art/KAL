package com.kal

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.kal.databinding.ActivityMainBinding
import com.kal.fragments.HistoryFragment.Companion.recyclerViewHistoryIsRecreated
import com.kal.fragments.MainFragment
import com.kal.fragments.land.MainLandFragment
import com.kal.fragments.small.SmallFragment
import com.kal.fragments.smallLand.SmallLandFragment
import com.kal.fragments.xLargeLand.XLargeLandFragment
import com.kal.settings.SettingsActivity.Companion.firstCreatedSettingsActivity
import com.kal.settings.Config
import com.kal.settings.Preferences
import com.kal.calculator.CalculatorViewModel
import com.kal.calculator.Evaluator
import com.kal.fragments.UnitConverterFragment
import com.kal.fragments.largeLand.LargeLandFragment
import com.kal.cashcounter.CashCounterFragment
import kotlin.properties.Delegates.notNull


interface OnMainActivityListener {
    fun onBackPressed(): Boolean
}

@Suppress("DEPRECATION")
@SuppressLint("DiscouragedApi")
class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding by notNull()
    private var preferences: Preferences by notNull()


    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = Preferences(this)


        Config.init(
            preferences.getTheme(),
            preferences.getColor(),
            preferences.getDynamicColor(),
            preferences.getGroupingSeparatorSymbol(),
            preferences.getDecimalSeparatorSymbol(),
            preferences.getNumberPrecision(),
            preferences.getMaxScientificNotationDigits(),
            preferences.getSwipeHistoryAndCalculator(),
            preferences.getSwipeDigitsAndScientificFunctions(),
            preferences.getAutoSavingResults(),
            preferences.getVibration(),
            preferences.getSoundEffects()
        )
        CalculatorViewModel.init(preferences.getDegreeMod())
        UnitConverterFragment.physicalQuantity = preferences.getPhysicalQuantity()
        UnitConverterFragment.unit = preferences.getUnit()


        if (!preferences.getDynamicColor()){
            setTheme(resources.getIdentifier(preferences.getColor(), "style", packageName))
        }else{
            setTheme(R.style.dynamicColors)
        }


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        if (preferences.getTheme() != AppCompatDelegate.MODE_NIGHT_NO) {
            preferences.setTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }


        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }


        when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> {
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        showFragment(SmallLandFragment())
                    }
                    else -> {
                        showFragment(SmallFragment())
                    }
                }
            }
            Configuration.SCREENLAYOUT_SIZE_LARGE -> {
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        showFragment(LargeLandFragment())
                    }
                    else -> {
                        showFragment(MainFragment())
                    }
                }
            }
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> {
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        showFragment(XLargeLandFragment())
                    }
                    else -> {
                        showFragment(MainFragment())
                    }
                }
            }
            else -> {
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        showFragment(MainLandFragment())
                    }
                    else -> {
                        showFragment(MainFragment())
                    }
                }
            }
        }

        Evaluator.converterResult.observe(this){
            val componentName = ComponentName(this, MyQSTileService::class.java)
            TileService.requestListeningState(this, componentName)
        }
    }

    private fun showFragment(fragment: Fragment){
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main)
        if (currentFragment != null) {
            supportFragmentManager
                .beginTransaction()
                .detach(currentFragment)
                .replace(R.id.main, fragment)
                .commit()
        }else{
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main, fragment)
                .commit()
        }
    }

    override fun onStop() {
        super.onStop()

        CalculatorViewModel.isDegreeModActivated.value?.let(preferences::setDegreeMod)
        preferences.setPhysicalQuantity(UnitConverterFragment.physicalQuantity)
        preferences.setUnit(UnitConverterFragment.unit)
    }

    override fun onDestroy() {
        super.onDestroy()

        recyclerViewHistoryIsRecreated = true
    }


    fun openCashCounter() {
        if (supportFragmentManager.findFragmentByTag(CashCounterFragment.TAG) != null) return
        val fragment = CashCounterFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main, fragment, CashCounterFragment.TAG)
            .addToBackStack(CashCounterFragment.TAG)
            .commit()
    }



    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val cashCounterFragment = supportFragmentManager.findFragmentByTag(CashCounterFragment.TAG) as? CashCounterFragment
        if (cashCounterFragment != null && cashCounterFragment.isAdded && cashCounterFragment.isVisible) {
            val handled = cashCounterFragment.onBackPressed()
            if (handled) return
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is OnMainActivityListener) {
            val handled = (fragment as OnMainActivityListener).onBackPressed()
            if (!handled) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }



    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == REQUEST_CODE_CHILD) {
            firstCreatedSettingsActivity = true
            recreate()
        }
    }

    companion object {
        const val REQUEST_CODE_CHILD = 0
    }
}
