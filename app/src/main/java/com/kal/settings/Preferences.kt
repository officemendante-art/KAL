package com.kal.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class Preferences(context: Context) {

    companion object {
        private const val KEY_THEME = "kal.THEME"

        private const val KEY_VIBRATION_STATUS = "kal.VIBRATION_STATUS"
        private const val KEY_SOUND_EFFECTS_STATUS = "kal.SOUND_EFFECTS_STATUS"
        private const val KEY_DYNAMIC_COLORS = "kal.DYNAMIC_COLORS"
        private const val KEY_COLOR = "kal.COLOR"

        private const val KEY_NUMBER_PRECISION = "kal.NUMBER_PRECISION"
        private const val KEY_MAX_SCIENTIFIC_NOTATION_DIGITS = "kal.MAX_SCIENTIFIC_NOTATION_DIGITS"
        private const val KEY_GROUPING_SEPARATOR_SYMBOL = "kal.GROUPING_SEPARATOR_SYMBOL"
        private const val KEY_DECIMAL_SEPARATOR_SYMBOL = "kal.DECIMAL_SEPARATOR_SYMBOL"

        private const val KEY_DEGREE_MOD = "kal.DEGREE_MOD"

        private const val KEY_AUTO_SAVING_RESULTS = "kal.AUTO_SAVING_RESULTS"
        private const val KEY_SWIPE_MAIN = "kal.SWIPE_HISTORY_AND_CALCULATOR"
        private const val KEY_SWIPE_DIGITS_AND_SCIENTIFIC_FUNCTIONS = "kal.SWIPE_DIGITS_AND_SCIENTIFIC_FUNCTIONS"

        private const val KEY_PHYSICAL_QUANTITY = "kal.PHYSICAL_QUANTITY"
        private const val KEY_UNIT = "kal.UNIT"

        private const val KEY_WORDS_FONT_SIZE_SP = "kal.WORDS_FONT_SIZE_SP"
        private const val KEY_HAS_SEEN_WORDS_FONT_HINT = "kal.HAS_SEEN_WORDS_FONT_HINT"
    }


    private val preferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)


    fun setTheme(input: Int) {
        val editor = preferences.edit()
        editor.putInt(KEY_THEME, input)
        editor.apply()
    }

    fun setVibration(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_VIBRATION_STATUS, input)
        editor.apply()
    }

    fun setSoundEffects(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_SOUND_EFFECTS_STATUS, input)
        editor.apply()
    }

    fun setDynamicColor(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_DYNAMIC_COLORS, false)
        editor.apply()
    }

    fun setColor(input: String){
        val editor = preferences.edit()
        editor.putString(KEY_COLOR, input)
        editor.apply()
    }

    fun setNumberPrecision(input: Int){
        val editor = preferences.edit()
        editor.putInt(KEY_NUMBER_PRECISION, input)
        editor.apply()
    }

    fun setMaxScientificNotationDigits(input: Int){
        val editor = preferences.edit()
        editor.putInt(KEY_MAX_SCIENTIFIC_NOTATION_DIGITS, input)
        editor.apply()
    }

    fun setGroupingSeparatorSymbol(input: String){
        val editor = preferences.edit()
        editor.putString(KEY_GROUPING_SEPARATOR_SYMBOL, input)
        editor.apply()
    }

    fun setDecimalSeparatorSymbol(input: String){
        val editor = preferences.edit()
        editor.putString(KEY_DECIMAL_SEPARATOR_SYMBOL, input)
        editor.apply()
    }

    fun setDegreeMod(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_DEGREE_MOD, input)
        editor.apply()
    }

    fun setSwipeHistoryAndCalculator(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_SWIPE_MAIN, input)
        editor.apply()
    }

    fun setSwipeDigitsAndScientificFunctions(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_SWIPE_DIGITS_AND_SCIENTIFIC_FUNCTIONS, input)
        editor.apply()
    }

    fun setPhysicalQuantity(input: Int){
        val editor = preferences.edit()
        editor.putInt(KEY_PHYSICAL_QUANTITY, input)
        editor.apply()
    }

    fun setUnit(input: Int){
        val editor = preferences.edit()
        editor.putInt(KEY_UNIT, input)
        editor.apply()
    }

    fun setAutoSavingResults(input: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(KEY_AUTO_SAVING_RESULTS, input)
        editor.apply()
    }







    fun getTheme(): Int{
        return preferences.getInt(KEY_THEME, 1)
    }

    fun getVibration(): Boolean{
        return preferences.getBoolean(KEY_VIBRATION_STATUS, true)
    }

    fun getSoundEffects(): Boolean{
        return preferences.getBoolean(KEY_SOUND_EFFECTS_STATUS, true)
    }

    fun getDynamicColor(): Boolean{
        return false
    }

    fun getColor(): String{
        return preferences.getString(KEY_COLOR, "color_0").toString()
    }

    fun getNumberPrecision(): Int{
        return preferences.getInt(KEY_NUMBER_PRECISION, 10)
    }

    fun getMaxScientificNotationDigits(): Int{
        return preferences.getInt(KEY_MAX_SCIENTIFIC_NOTATION_DIGITS, 15)
    }

    fun getGroupingSeparatorSymbol(): String{
        return preferences.getString(KEY_GROUPING_SEPARATOR_SYMBOL, ",").toString()
    }

    fun getDecimalSeparatorSymbol(): String{
        return preferences.getString(KEY_DECIMAL_SEPARATOR_SYMBOL, ".").toString()
    }

    fun getDegreeMod(): Boolean{
        return preferences.getBoolean(KEY_DEGREE_MOD, true)
    }

    fun getSwipeHistoryAndCalculator(): Boolean{
        return preferences.getBoolean(KEY_SWIPE_MAIN, true)
    }

    fun getSwipeDigitsAndScientificFunctions(): Boolean{
        return preferences.getBoolean(KEY_SWIPE_DIGITS_AND_SCIENTIFIC_FUNCTIONS, true)
    }

    fun getPhysicalQuantity(): Int{
        return preferences.getInt(KEY_PHYSICAL_QUANTITY, 10)
    }

    fun getUnit(): Int{
        return preferences.getInt(KEY_UNIT, 1010)
    }

    fun getAutoSavingResults(): Boolean{
        return preferences.getBoolean(KEY_AUTO_SAVING_RESULTS, false)
    }

    fun getWordsFontSize(): Float {
        return preferences.getFloat(KEY_WORDS_FONT_SIZE_SP, 24f)
    }

    fun setWordsFontSize(sizeSp: Float) {
        preferences.edit().putFloat(KEY_WORDS_FONT_SIZE_SP, sizeSp).apply()
    }

    fun hasSeenWordsFontHint(): Boolean {
        return preferences.getBoolean(KEY_HAS_SEEN_WORDS_FONT_HINT, false)
    }

    fun setHasSeenWordsFontHint(seen: Boolean) {
        preferences.edit().putBoolean(KEY_HAS_SEEN_WORDS_FONT_HINT, seen).apply()
    }
}
