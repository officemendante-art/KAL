package com.kal.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kal.AboutActivity
import com.kal.App
import com.kal.utils.HapticAndSound
import com.kal.utils.InsertInExpression
import com.kal.MainActivity
import com.kal.OnMainActivityListener
import com.kal.R
import com.kal.databinding.FragmentMainBinding
import com.kal.fragments.Fragments.CALCULATOR_FRAGMENT
import com.kal.fragments.Fragments.HISTORY_FRAGMENT
import com.kal.fragments.Fragments.UNIT_CONVERTER_FRAGMENT
import com.kal.fragments.Fragments.currentItemMainPager
import com.kal.fragments.adapters.ViewPageAdapter
import com.kal.history.HistoryService
import com.kal.settings.SettingsActivity
import com.kal.settings.Config
import com.kal.calculator.CalculatorViewModel
import com.kal.expression.ExpressionViewModel
import com.kal.expression.ExpressionViewModel.cursorPositionStart
import com.kal.expression.ExpressionViewModel.expression
import com.kal.expression.ExpressionViewModel.oldExpression
import com.google.android.material.tabs.TabLayoutMediator
import com.kal.calculator.Evaluator
import com.kal.calculator.NumberToWordsConverter
import com.kal.calculator.TrigonometricFunction
import com.kal.settings.Config.autoSavingResults
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import com.kal.settings.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.random.Random
import kotlin.properties.Delegates.notNull

@Suppress("DEPRECATION")
class MainFragment : Fragment(),
    OnMainActivityListener,
    CalculatorFragment.OnButtonClickListener,
    HistoryFragment.OnButtonClickListener,
    UnitConverterFragment.OnButtonClickListener
{

    private var binding: FragmentMainBinding by notNull()
    private var hapticAndSound: HapticAndSound by notNull()
    private val preferences: Preferences by lazy { Preferences(requireContext()) }
    private var currentExpandedWords: String = ""
    private var currentWordsFontSizeSp: Float = 24f
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var isPinchingWords = false
    private var tooltipDismissRunnable: Runnable? = null
    private var isCopyCycleRunning = false
    private var wordsBackPressedCallback: OnBackPressedCallback? = null

    private var sheetCeilingHeight = 0
    private var sheetCompactHeight = 0
    private var isSheetExpandedAtCeiling = false
    private var heightAnimator: ValueAnimator? = null
    private var velocityTracker: VelocityTracker? = null

    private val historyService: HistoryService
        get() = (requireContext().applicationContext as App).historyService

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        val views: Array<View> = arrayOf(
            binding.degreeTitleText
        )
        hapticAndSound = HapticAndSound(requireContext(), views)

        binding.expressionEditText.showSoftInputOnFocus = false
        binding.expressionEditText.requestFocus()


        val adapter = ViewPageAdapter(childFragmentManager, lifecycle)

        adapter.addFragment(UnitConverterFragment())
        adapter.addFragment(CalculatorFragment())
        adapter.addFragment(HistoryFragment())

        binding.pager.adapter = adapter
        binding.pager.setCurrentItem(currentItemMainPager, false)
        binding.pager.offscreenPageLimit = 3

        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.setIcon(
                when (position) {
                    UNIT_CONVERTER_FRAGMENT -> R.drawable.baseline_autorenew
                    CALCULATOR_FRAGMENT -> R.drawable.baseline_calculate
                    HISTORY_FRAGMENT -> R.drawable.baseline_history
                    else -> throw IllegalArgumentException("Invalid position")
                }
            )
        }.attach()

        binding.pager.apply {
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        binding.toolbar.let { toolbar ->
            if (currentItemMainPager == HISTORY_FRAGMENT) {
                toolbar.menu.clear()
                toolbar.inflateMenu(R.menu.history_options_menu)
            } else {
                toolbar.menu.clear()
                toolbar.inflateMenu(R.menu.options_menu)
            }
        }


        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentItemMainPager = position

                if (currentItemMainPager == HISTORY_FRAGMENT){
                    if (Evaluator.isCalculated && autoSavingResults){
                        val result = binding.resultText.text.toString()

                        val expression: String = if (ExpressionViewModel.isSelected.value == true){
                            binding.expressionEditText.text
                                .toString()
                                .substring(
                                    binding.expressionEditText.selectionStart, binding.expressionEditText.selectionEnd
                                )
                        } else {
                            binding.expressionEditText.text.toString()
                        }

                        historyService.addHistoryData(expression, result)
                    }
                }

                binding.toolbar.let { toolbar ->
                    if (currentItemMainPager == HISTORY_FRAGMENT) {
                        toolbar.menu.clear()
                        toolbar.inflateMenu(R.menu.history_options_menu)
                    } else {
                        toolbar.menu.clear()
                        toolbar.inflateMenu(R.menu.options_menu)
                    }
                }
            }
        })

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.cashCounter -> {
                    (activity as? MainActivity)?.openCashCounter()
                    true
                }
                R.id.settings -> {
                    val intent = Intent(requireActivity(), SettingsActivity::class.java)
                    startActivityForResult(intent, MainActivity.REQUEST_CODE_CHILD)
                    true
                }
                R.id.about -> {
                    val intent = Intent(requireContext(), AboutActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.clearHistory -> {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder
                        .setMessage(getString(R.string.clear_history_title))
                        .setPositiveButton(getString(R.string.clear_history_clear)) { _, _ ->
                            historyService.clearHistoryData()
                            binding.pager.setCurrentItem(CALCULATOR_FRAGMENT, true)
                        }.setNegativeButton(getString(R.string.clear_history_dismiss)) { _, _ ->
                        }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                    true
                }
                else -> false
            }
        }


        binding.expressionEditText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                view.requestFocus()
            }
        }

        binding.expressionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun afterTextChanged(p0: Editable?) {
                Evaluator.setResultTextView(binding.expressionEditText, binding.resultText, ExpressionViewModel.isSelected.value ?: false, requireContext())
                binding.expressionEditText.autoSizeTextExpressionEditText(binding.expressionTextView)

                if (TrigonometricFunction.entries.any { binding.expressionEditText.text?.contains(it.text) == true }){
                    binding.degreeTitleText.visibility = ImageView.VISIBLE
                } else{
                    binding.degreeTitleText.visibility = ImageView.GONE
                }
                updateWordsPreview()
            }
        })

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.expressionEditText.autoSizeTextExpressionEditText(binding.expressionTextView)
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        ExpressionViewModel.isSelected.observe(requireActivity()){ isSelected ->
            Evaluator.setResultTextView(binding.expressionEditText, binding.resultText, isSelected, requireContext())
            updateWordsPreview()
        }

        binding.degreeTitleText.setOnClickListener {
            CalculatorViewModel.updateDegreeModActivated()
            hapticAndSound.vibrateEffectClick()
        }

        CalculatorViewModel.isDegreeModActivated.observe(requireActivity()) { isDegreeModActivated ->
            if (isDegreeModActivated) {
                binding.degreeTitleText.text = getString(R.string.deg)
            } else {
                binding.degreeTitleText.text = getString(R.string.rad)
            }

            Evaluator.setResultTextView(binding.expressionEditText, binding.resultText, ExpressionViewModel.isSelected.value ?: false, requireContext())
            updateWordsPreview()
        }

        setupInWords()

        binding.backdropWordsOverlay.setOnClickListener {
            dismissGestureTooltip()
            dismissWordsBottomSheet()
        }

        setupDragHandle()
        setupMatrixCopyButton()
        setupMatrixFontButton()
        setupWordsPinchGesture()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wordsBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (isSheetExpandedAtCeiling) {
                    animateSheetHeight(binding.bottomSheetWords.height, sheetCompactHeight)
                } else {
                    dismissWordsBottomSheet()
                }
            }
        }.also {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
        }
    }

    override fun onStart() {
        super.onStart()

        hapticAndSound.setHapticFeedback()
        hapticAndSound.setSoundEffects()
        binding.pager.isUserInputEnabled = Config.swipeMain
        binding.expressionEditText.setText(expression)
        binding.expressionEditText.setSelection(cursorPositionStart)
        setupInWordsFadeMasks()
        updateWordsPreview()
    }

    override fun onStop() {
        super.onStop()

        expression = binding.expressionEditText.text.toString()
        cursorPositionStart = binding.expressionEditText.selectionStart

        if (Evaluator.isCalculated && autoSavingResults){
            val result = binding.resultText.text.toString()

            val expression: String = if (ExpressionViewModel.isSelected.value == true){
                binding.expressionEditText.text
                    .toString()
                    .substring(
                        binding.expressionEditText.selectionStart, binding.expressionEditText.selectionEnd
                    )
            } else {
                binding.expressionEditText.text.toString()
            }

            historyService.addHistoryData(expression, result)
        }
    }

    override fun onDestroyView() {
        heightAnimator?.cancel()
        heightAnimator = null
        velocityTracker?.recycle()
        velocityTracker = null
        tooltipDismissRunnable?.let { binding.layoutGestureTooltip.removeCallbacks(it) }
        tooltipDismissRunnable = null
        super.onDestroyView()
    }

    override fun onBackPressed(): Boolean {
        if (binding.bottomSheetWords.visibility == View.VISIBLE) {
            dismissGestureTooltip()
            if (isSheetExpandedAtCeiling) {
                animateSheetHeight(binding.bottomSheetWords.height, sheetCompactHeight)
            } else {
                dismissWordsBottomSheet()
            }
            return true
        }
        return if (currentItemMainPager != CALCULATOR_FRAGMENT) {
            binding.pager.setCurrentItem(CALCULATOR_FRAGMENT, true)
            true
        } else {
            false
        }
    }

    override fun onDigitButtonClick(digit: String) {
        InsertInExpression.enterDigit(digit, binding.expressionEditText)
    }

    override fun onDotButtonClick() {
        InsertInExpression.enterDot(binding.expressionEditText)
    }

    override fun onBackspaceButtonClick() {
        InsertInExpression.enterBackspace(binding.expressionEditText)
    }

    override fun onClearExpressionButtonClick() {
        InsertInExpression.clearExpression(binding.expressionEditText)
    }

    override fun onOperatorButtonClick(operator: String) {
        InsertInExpression.enterOperator(operator, binding.expressionEditText)
    }

    override fun onScienceFunctionButtonClick(scienceFunction: String) {
        InsertInExpression.enterScienceFunction(scienceFunction, binding.expressionEditText)
    }

    override fun onAdditionalOperatorButtonClick(operator: String) {
        InsertInExpression.enterAdditionalOperator(operator, binding.expressionEditText)
    }

    override fun onConstantButtonClick(constant: String) {
        InsertInExpression.enterConstant(constant, binding.expressionEditText)
    }

    override fun onBracketButtonClick() {
        InsertInExpression.enterBracket(binding.expressionEditText)
    }

    override fun onDoubleBracketsButtonClick() {
        InsertInExpression.enterDoubleBrackets(binding.expressionEditText)
    }

    override fun onEqualsButtonClick() {
        if (Evaluator.isCalculated){
            val result = binding.resultText.text.toString()

            val expression: String = if (ExpressionViewModel.isSelected.value == true){
                binding.expressionEditText.text
                    .toString()
                    .substring(
                        binding.expressionEditText.selectionStart, binding.expressionEditText.selectionEnd
                    )
            } else {
                binding.expressionEditText.text.toString()
            }

            oldExpression = expression
            InsertInExpression.setExpression(result, binding.expressionEditText)
            historyService.addHistoryData(expression, result)
        }
    }

    override fun onEqualsButtonLongClick() {
        if (oldExpression.isNotEmpty()){
            InsertInExpression.setExpression(oldExpression, binding.expressionEditText)
        }
    }

    override fun onExpressionTextClick(expression: String) {
        InsertInExpression.insertHistoryExpression(expression, binding.expressionEditText)
    }

    override fun onResultTextClick(result: String) {
        InsertInExpression.insertHistoryResult(result, binding.expressionEditText)
    }

    override fun onUnitResultTextClick(result: String) {
        InsertInExpression.setExpression(result, binding.expressionEditText)
    }

    private fun updateWordsPreview() {
        val expr = binding.expressionEditText.text?.toString() ?: ""
        val result = binding.resultText.text?.toString() ?: ""
        val resolved = NumberToWordsConverter.resolveResultWords(expr, result)

        if (resolved != null && resolved.words.isNotEmpty()) {
            currentExpandedWords = resolved.words
            binding.tvWordStripText.text = highlightFinancialWords(resolved.words)
            binding.layoutWordStrip.visibility = View.VISIBLE
            binding.layoutWordStrip.alpha = if (resolved.isPending) 0.72f else 1.0f
            binding.scrollInWords.scrollTo(0, 0)
            binding.scrollInWords.post {
                updateInWordsFadeMasks(0)
            }
        } else {
            currentExpandedWords = ""
            binding.layoutWordStrip.visibility = View.GONE
            if (binding.bottomSheetWords.visibility == View.VISIBLE) {
                dismissWordsBottomSheet()
            }
        }
    }

    private fun setupInWords() {
        val openWordsSheet = View.OnClickListener {
            showWordsBottomSheet()
            hapticAndSound.vibrateEffectClick()
        }
        binding.layoutWordStrip.setOnClickListener(openWordsSheet)
        binding.scrollInWords.setOnClickListener(openWordsSheet)
        binding.layoutInWordsContent.setOnClickListener(openWordsSheet)
        binding.ivWordStripExpand.setOnClickListener(openWordsSheet)
        binding.tvWordStripText.setOnClickListener(openWordsSheet)

        setupInWordsFadeMasks()

        binding.scrollInWords.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            updateInWordsFadeMasks(scrollX)
        }
    }

    private fun setupInWordsFadeMasks() {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        val surfaceColor = if (typedValue.resourceId != 0) {
            ContextCompat.getColor(requireContext(), typedValue.resourceId)
        } else {
            typedValue.data
        }
        val transparentSurface = Color.argb(0, Color.red(surfaceColor), Color.green(surfaceColor), Color.blue(surfaceColor))

        binding.fadeMaskLeft.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(surfaceColor, transparentSurface)
        )
        binding.fadeMaskRight.background = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT,
            intArrayOf(surfaceColor, transparentSurface)
        )
    }

    private fun updateInWordsFadeMasks(scrollX: Int) {
        val contentWidth = binding.layoutInWordsContent.width
        val scrollWidth = binding.scrollInWords.width
        val maxScroll = (contentWidth - scrollWidth).coerceAtLeast(0)
        val fadeDistance = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics
        )

        if (maxScroll > 0) {
            binding.fadeMaskLeft.alpha = (scrollX / fadeDistance).coerceIn(0f, 1f)
            binding.fadeMaskRight.alpha = ((maxScroll - scrollX) / fadeDistance).coerceIn(0f, 1f)
        } else {
            binding.fadeMaskLeft.alpha = 0f
            binding.fadeMaskRight.alpha = 0f
        }
    }

    private fun getMintAccentColor(): Int {
        val typedValue = TypedValue()
        val hasTertiary = requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorTertiary,
            typedValue,
            true
        )
        return if (hasTertiary && typedValue.resourceId != 0) {
            ContextCompat.getColor(requireContext(), typedValue.resourceId)
        } else if (hasTertiary && typedValue.data != 0) {
            typedValue.data
        } else {
            Color.parseColor("#00B36E")
        }
    }

    private fun highlightFinancialWords(text: String): CharSequence {
        val spannable = SpannableString(text)
        val accentColor = getMintAccentColor()
        val matcher = DENOMINATION_WORD_PATTERN.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            spannable.setSpan(
                ForegroundColorSpan(accentColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun showWordsBottomSheet() {
        if (currentExpandedWords.isEmpty()) return

        currentWordsFontSizeSp = preferences.getWordsFontSize().coerceIn(18f, 34f)
        binding.tvExpandedWords.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentWordsFontSizeSp)

        binding.tvExpandedWords.text = highlightFinancialWords(currentExpandedWords)
        binding.scrollWordsBody.scrollTo(0, 0)
        resetMatrixCopyButton()

        wordsBackPressedCallback?.isEnabled = true
        binding.pager.isUserInputEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.pager.setRenderEffect(RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP))
        }

        binding.backdropWordsOverlay.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(220).start()
        }

        binding.bottomSheetWords.apply {
            visibility = View.VISIBLE
            translationY = 0f
            post {
                val ceiling = if (binding.pager.top > 0) {
                    binding.root.height - binding.pager.top
                } else {
                    (binding.root.height * 0.63f).toInt()
                }
                sheetCeilingHeight = ceiling

                val availableWidth = (width - paddingLeft - paddingRight).takeIf { it > 0 }
                    ?: (resources.displayMetrics.widthPixels - (48 * resources.displayMetrics.density).toInt())
                val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                binding.tvExpandedWords.measure(widthSpec, heightSpec)
                val textHeight = binding.tvExpandedWords.measuredHeight

                val nonTextHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    114f,
                    resources.displayMetrics
                ).toInt()

                sheetCompactHeight = (nonTextHeight + textHeight).coerceIn(120, sheetCeilingHeight)
                isSheetExpandedAtCeiling = false

                heightAnimator?.cancel()
                layoutParams.height = sheetCompactHeight
                requestLayout()

                translationY = sheetCompactHeight.toFloat()
                animate()
                    .translationY(0f)
                    .setDuration(240)
                    .setInterpolator(PathInterpolator(0.2f, 0.9f, 0.3f, 1f))
                    .withEndAction {
                        if (!preferences.hasSeenWordsFontHint()) {
                            showGestureTooltip()
                            preferences.setHasSeenWordsFontHint(true)
                        }
                    }
                    .start()
            }
        }
    }

    private fun dismissWordsBottomSheet() {
        dismissGestureTooltip()
        wordsBackPressedCallback?.isEnabled = false
        binding.pager.isUserInputEnabled = Config.swipeMain

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.pager.setRenderEffect(null)
        }

        binding.backdropWordsOverlay.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction { binding.backdropWordsOverlay.visibility = View.GONE }
            .start()

        heightAnimator?.cancel()
        val currentSheetHeight = binding.bottomSheetWords.height.toFloat().takeIf { it > 0f } ?: 600f
        binding.bottomSheetWords.animate()
            .translationY(currentSheetHeight)
            .setDuration(200)
            .withEndAction {
                binding.bottomSheetWords.visibility = View.GONE
                binding.bottomSheetWords.translationY = 0f
                isSheetExpandedAtCeiling = false
            }
            .start()
    }

    private fun showGestureTooltip() {
        tooltipDismissRunnable?.let { binding.layoutGestureTooltip.removeCallbacks(it) }
        binding.layoutGestureTooltip.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(220)
                .start()
        }
        val dismissRunnable = Runnable {
            dismissGestureTooltip()
        }
        tooltipDismissRunnable = dismissRunnable
        binding.layoutGestureTooltip.postDelayed(dismissRunnable, 3500)
    }

    private fun dismissGestureTooltip() {
        tooltipDismissRunnable?.let { binding.layoutGestureTooltip.removeCallbacks(it) }
        tooltipDismissRunnable = null
        if (binding.layoutGestureTooltip.visibility == View.VISIBLE) {
            binding.layoutGestureTooltip.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction {
                    binding.layoutGestureTooltip.visibility = View.GONE
                }
                .start()
        }
    }

    private fun setupMatrixFontButton() {
        binding.btnMatrixFont.setOnClickListener {
            dismissGestureTooltip()
            hapticAndSound.vibrateEffectClick()

            // Icon outward expand and spring bounce
            binding.ivMatrixFontIcon.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .setDuration(120)
                .setInterpolator(OvershootInterpolator(2.5f))
                .withEndAction {
                    binding.ivMatrixFontIcon.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                .start()

            // Button container pulse
            binding.btnMatrixFont.animate()
                .scaleX(0.90f)
                .scaleY(0.90f)
                .setDuration(80)
                .withEndAction {
                    binding.btnMatrixFont.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start()
                }
                .start()

            val currentIdx = FONT_SIZE_STEPS.indexOfFirst { it >= currentWordsFontSizeSp }.takeIf { it >= 0 } ?: 1
            val nextIdx = (currentIdx + 1) % FONT_SIZE_STEPS.size
            val nextSize = FONT_SIZE_STEPS[nextIdx]
            applyWordsFontSize(nextSize)
            preferences.setWordsFontSize(nextSize)
        }
    }

    private fun applyWordsFontSize(sizeSp: Float) {
        currentWordsFontSizeSp = sizeSp
        binding.tvExpandedWords.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        updateWordsSheetHeightForFontSize()
    }

    private fun updateWordsSheetHeightForFontSize() {
        if (binding.bottomSheetWords.visibility != View.VISIBLE) return

        val availableWidth = (binding.bottomSheetWords.width - binding.bottomSheetWords.paddingLeft - binding.bottomSheetWords.paddingRight).takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels - (48 * resources.displayMetrics.density).toInt())
        val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        binding.tvExpandedWords.measure(widthSpec, heightSpec)
        val textHeight = binding.tvExpandedWords.measuredHeight

        val nonTextHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            114f,
            resources.displayMetrics
        ).toInt()

        sheetCompactHeight = (nonTextHeight + textHeight).coerceIn(120, sheetCeilingHeight)

        if (!isSheetExpandedAtCeiling) {
            animateSheetHeight(binding.bottomSheetWords.height, sheetCompactHeight)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupWordsPinchGesture() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isPinchingWords = true
                binding.bottomSheetWords.requestDisallowInterceptTouchEvent(true)
                binding.scrollWordsBody.requestDisallowInterceptTouchEvent(true)
                dismissGestureTooltip()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = detector.scaleFactor
                val targetSize = (currentWordsFontSizeSp * scale).coerceIn(18f, 34f)
                if (abs(targetSize - currentWordsFontSizeSp) >= 0.3f) {
                    currentWordsFontSizeSp = targetSize
                    binding.tvExpandedWords.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentWordsFontSizeSp)
                }
                // Dynamic icon expand during pinch
                val iconScale = (1.0f + (scale - 1.0f) * 0.7f).coerceIn(0.85f, 1.45f)
                binding.ivMatrixFontIcon.scaleX = iconScale
                binding.ivMatrixFontIcon.scaleY = iconScale
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isPinchingWords = false
                binding.bottomSheetWords.requestDisallowInterceptTouchEvent(false)
                binding.scrollWordsBody.requestDisallowInterceptTouchEvent(false)

                // Spring back icon with organic overshoot bounce on release
                binding.ivMatrixFontIcon.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(180)
                    .setInterpolator(OvershootInterpolator(2.2f))
                    .start()

                val snappedSize = FONT_SIZE_STEPS.minByOrNull { abs(it - currentWordsFontSizeSp) } ?: 24f
                applyWordsFontSize(snappedSize)
                preferences.setWordsFontSize(snappedSize)
                hapticAndSound.vibrateEffectClick()
            }
        })

        val pinchTouchListener = View.OnTouchListener { v, event ->
            if (event.pointerCount >= 2) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                scaleGestureDetector?.onTouchEvent(event)
                true
            } else {
                if (isPinchingWords) {
                    scaleGestureDetector?.onTouchEvent(event)
                    true
                } else {
                    dismissGestureTooltip()
                    scaleGestureDetector?.onTouchEvent(event)
                    false
                }
            }
        }

        binding.scrollWordsBody.setOnTouchListener(pinchTouchListener)
        binding.tvExpandedWords.setOnTouchListener(pinchTouchListener)
        binding.layoutGestureTooltip.setOnClickListener { dismissGestureTooltip() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragHandle() {
        var startRawY = 0f
        var startHeight = 0
        var isDragging = false

        val touchListener = View.OnTouchListener { _, event ->
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker?.addMovement(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    heightAnimator?.cancel()
                    binding.bottomSheetWords.animate().cancel()
                    startRawY = event.rawY
                    startHeight = binding.bottomSheetWords.height
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawDeltaY = event.rawY - startRawY
                    if (abs(rawDeltaY) > 6f) {
                        isDragging = true
                    }

                    if (rawDeltaY < 0f) {
                        // User is dragging UP: stretch height UPWARDS towards ceiling
                        val stretchAmount = -rawDeltaY
                        var newHeight = (startHeight + stretchAmount).toInt()
                        if (newHeight > sheetCeilingHeight) {
                            val over = newHeight - sheetCeilingHeight
                            newHeight = sheetCeilingHeight + (over * 0.25f).toInt()
                        }
                        binding.bottomSheetWords.layoutParams.height = newHeight
                        binding.bottomSheetWords.translationY = 0f
                        binding.bottomSheetWords.requestLayout()
                        binding.backdropWordsOverlay.alpha = 1f
                    } else {
                        // User is dragging DOWN
                        if (startHeight > sheetCompactHeight) {
                            // Shrink height back down towards compact
                            val shrinkAmount = rawDeltaY
                            val newHeight = (startHeight - shrinkAmount).toInt().coerceAtLeast(sheetCompactHeight)
                            binding.bottomSheetWords.layoutParams.height = newHeight
                            binding.bottomSheetWords.translationY = 0f
                            binding.bottomSheetWords.requestLayout()
                            binding.backdropWordsOverlay.alpha = 1f
                        } else {
                            // Pull downwards from compact to dismiss
                            binding.bottomSheetWords.layoutParams.height = sheetCompactHeight
                            binding.bottomSheetWords.translationY = rawDeltaY
                            val dismissProgress = (rawDeltaY / sheetCompactHeight.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            binding.backdropWordsOverlay.alpha = (1f - dismissProgress).coerceIn(0f, 1f)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val yVelocity = velocityTracker?.yVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val currentHeight = binding.bottomSheetWords.height
                    val currentTranslationY = binding.bottomSheetWords.translationY
                    val totalDeltaY = event.rawY - startRawY

                    if (!isDragging || abs(totalDeltaY) < 10f) {
                        toggleWordsSheetExpanded()
                    } else if (currentTranslationY > 0f) {
                        if (currentTranslationY > 80f || yVelocity > 500f) {
                            dismissWordsBottomSheet()
                        } else {
                            binding.bottomSheetWords.animate()
                                .translationY(0f)
                                .setDuration(160)
                                .start()
                            binding.backdropWordsOverlay.animate().alpha(1f).setDuration(160).start()
                        }
                    } else {
                        val isFlingUp = yVelocity < -500f
                        val isFlingDown = yVelocity > 500f
                        val midHeight = (sheetCompactHeight + sheetCeilingHeight) / 2

                        val targetHeight = when {
                            isFlingUp -> sheetCeilingHeight
                            isFlingDown -> sheetCompactHeight
                            currentHeight > midHeight -> sheetCeilingHeight
                            else -> sheetCompactHeight
                        }

                        animateSheetHeight(currentHeight, targetHeight)
                    }
                    true
                }
                else -> false
            }
        }

        binding.layoutDragHandleArea.setOnTouchListener(touchListener)
        binding.dragHandle.setOnTouchListener(touchListener)
    }

    private fun toggleWordsSheetExpanded() {
        val currentH = binding.bottomSheetWords.height
        val targetH = if (isSheetExpandedAtCeiling) sheetCompactHeight else sheetCeilingHeight
        animateSheetHeight(currentH, targetH)
    }

    private fun animateSheetHeight(fromHeight: Int, toHeight: Int) {
        isSheetExpandedAtCeiling = (toHeight >= sheetCeilingHeight)
        heightAnimator?.cancel()

        binding.bottomSheetWords.translationY = 0f
        binding.backdropWordsOverlay.animate().alpha(1f).setDuration(160).start()

        val anim = ValueAnimator.ofInt(fromHeight, toHeight).apply {
            duration = 220
            interpolator = PathInterpolator(0.2f, 0.9f, 0.3f, 1f)
            addUpdateListener { va ->
                val h = va.animatedValue as Int
                binding.bottomSheetWords.layoutParams.height = h
                binding.bottomSheetWords.requestLayout()
            }
        }
        heightAnimator = anim
        anim.start()
    }

    private fun resetMatrixCopyButton() {
        isCopyCycleRunning = false
        binding.btnMatrixCopy.setBackgroundResource(R.drawable.bg_matrix_copy_idle)
        binding.ivMatrixCopyIcon.setImageResource(R.drawable.baseline_content_copy)
        binding.ivMatrixCopyIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#0D6E43"))
        binding.tvMatrixCopyText.setTextColor(Color.parseColor("#0D6E43"))
        binding.tvMatrixCopyText.text = "Copy"
        binding.btnMatrixCopy.scaleX = 1f
        binding.btnMatrixCopy.scaleY = 1f
    }

    private fun setupMatrixCopyButton() {
        binding.btnMatrixCopy.setOnClickListener {
            if (isCopyCycleRunning || currentExpandedWords.isEmpty()) return@setOnClickListener
            isCopyCycleRunning = true

            // Scale compression animation
            binding.btnMatrixCopy.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(80)
                .withEndAction {
                    binding.btnMatrixCopy.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start()
                }
                .start()

            hapticAndSound.vibrateEffectClick()

            // Copy plain text spelled-out words
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Numbers in Words", currentExpandedWords)
            clipboard.setPrimaryClip(clip)

            // Cyberpunk matrix scramble transition
            viewLifecycleOwner.lifecycleScope.launch {
                binding.btnMatrixCopy.setBackgroundResource(R.drawable.bg_matrix_copy_success)
                binding.ivMatrixCopyIcon.setImageResource(R.drawable.baseline_check)
                binding.ivMatrixCopyIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#00FF87"))
                binding.tvMatrixCopyText.setTextColor(Color.parseColor("#00FF87"))

                scrambleText("Done")

                delay(1900)

                scrambleText("Copy")

                resetMatrixCopyButton()
            }
        }
    }

    private suspend fun scrambleText(targetText: String) {
        val charPool = charArrayOf('0', '1', 'X', '_', '$', '!', '[', ']')
        val iterations = 6
        val stepDelay = 30L
        for (i in 0 until iterations) {
            val scrambled = buildString {
                for (j in targetText.indices) {
                    if (i >= iterations - 2 && j < targetText.length / 2) {
                        append(targetText[j])
                    } else if (i == iterations - 1) {
                        append(targetText[j])
                    } else {
                        append(charPool[Random.nextInt(charPool.size)])
                    }
                }
            }
            binding.tvMatrixCopyText.text = scrambled
            delay(stepDelay)
        }
        binding.tvMatrixCopyText.text = targetText
    }

    companion object {
        private val DENOMINATION_WORD_PATTERN: Pattern =
            Pattern.compile("\\b(Crore|Lakh|Thousand|Hundred|Point)\\b")
        private val FONT_SIZE_STEPS = floatArrayOf(18f, 24f, 28f, 34f)
    }
}
