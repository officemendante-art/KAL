package com.kal.cashcounter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.view.animation.PathInterpolator
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kal.OnMainActivityListener
import com.kal.R
import com.kal.databinding.FragmentCashCounterBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

class CashCounterFragment : Fragment(), OnMainActivityListener {

    private var _binding: FragmentCashCounterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CashCounterViewModel by viewModels()

    private val iosDecelerateInterpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1f)
    private var isAnimating = false
    private var cachedNavBarHeight = 0
    private var isFresh = false

    private val handler = Handler(Looper.getMainLooper())
    private var hintResetRunnable: Runnable? = null

    private val indianFormat = NumberFormat.getInstance(Locale("en", "IN"))

    private val backgroundView: View?
        get() {
            val parent = view?.parent as? ViewGroup ?: return null
            val count = parent.childCount
            for (i in 0 until count) {
                val child = parent.getChildAt(i)
                if (child !== view && child.id != View.NO_ID) {
                    return child
                }
            }
            return null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashCounterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-position off-screen immediately so there is zero 1-frame flash or stutter
        val initialWidth = getScreenWidth()
        val shadowW = getShadowWidth()
        binding.contentContainer.translationX = initialWidth
        binding.leftEdgeShadow.translationX = initialWidth - shadowW
        binding.scrimOverlay.alpha = 0f
        backgroundView?.translationX = 0f

        setupWindowInsets()
        setupSwipeToDismiss()
        setupRowClickListeners()
        setupKeypadListeners()
        setupToolbarActions()
        setupHeroClickListeners()
        setupStateObservers()
        setupBackCallback()

        view.doOnPreDraw {
            runEnterAnimation()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            cachedNavBarHeight = navBarHeight

            val baseTopBarHeight = (56 * resources.displayMetrics.density).toInt()
            binding.topBar.updateLayoutParams<ViewGroup.LayoutParams> {
                height = baseTopBarHeight + statusBarHeight
            }
            binding.topBar.setPadding(
                (6 * resources.displayMetrics.density).toInt(),
                statusBarHeight,
                (10 * resources.displayMetrics.density).toInt(),
                0
            )

            applyBottomInsets(viewModel.activeDenomination.value != null)
            insets
        }
    }

    private fun applyBottomInsets(isKeypadVisible: Boolean) {
        val density = resources.displayMetrics.density
        if (isKeypadVisible) {
            binding.keypadLayout.setPadding(
                (12 * density).toInt(),
                (10 * density).toInt(),
                (12 * density).toInt(),
                cachedNavBarHeight + (16 * density).toInt()
            )
            binding.scrollView.setPadding(0, 0, 0, 0)
        } else {
            binding.scrollView.setPadding(0, 0, 0, cachedNavBarHeight + (16 * density).toInt())
        }
    }

    private fun setupSwipeToDismiss() {
        binding.cashCounterRoot.onSwipeListener = object : SwipeDismissLayout.OnSwipeListener {
            override fun onSwipeStart() {
                if (isAnimating) return
                binding.contentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                binding.leftEdgeShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                backgroundView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            override fun onSwipeProgress(translationX: Float, fraction: Float) {
                if (isAnimating) return
                val width = getScreenWidth()
                val shadowW = getShadowWidth()
                binding.contentContainer.translationX = translationX
                binding.leftEdgeShadow.translationX = translationX - shadowW
                binding.scrimOverlay.alpha = max(0f, (1f - fraction) * 0.25f)
                backgroundView?.translationX = -(1f - fraction) * (width / 3f)
            }

            override fun onSwipeDismissed(velocity: Float) {
                if (isAnimating) return
                completeDismissAnimation(velocity)
            }

            override fun onSwipeCanceled() {
                if (isAnimating) return
                cancelDismissAnimation()
            }
        }
    }

    private fun runEnterAnimation() {
        val width = getScreenWidth()
        val shadowW = getShadowWidth()

        binding.contentContainer.translationX = width
        binding.leftEdgeShadow.translationX = width - shadowW
        binding.scrimOverlay.alpha = 0f
        backgroundView?.translationX = 0f

        binding.contentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.leftEdgeShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        backgroundView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        isAnimating = true
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300L
            interpolator = iosDecelerateInterpolator
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val currentX = fraction * width
                binding.contentContainer.translationX = currentX
                binding.leftEdgeShadow.translationX = currentX - shadowW
                binding.scrimOverlay.alpha = (1f - fraction) * 0.25f
                backgroundView?.translationX = -(1f - fraction) * (width / 3f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.contentContainer.translationX = 0f
                    binding.leftEdgeShadow.translationX = -shadowW
                    binding.scrimOverlay.alpha = 0.25f
                    backgroundView?.translationX = -(width / 3f)
                    binding.contentContainer.setLayerType(View.LAYER_TYPE_NONE, null)
                    binding.leftEdgeShadow.setLayerType(View.LAYER_TYPE_NONE, null)
                    backgroundView?.setLayerType(View.LAYER_TYPE_NONE, null)
                    isAnimating = false
                }
            })
            start()
        }
    }

    private fun completeDismissAnimation(velocity: Float = 0f) {
        val width = getScreenWidth()
        val shadowW = getShadowWidth()
        val startX = binding.contentContainer.translationX

        binding.contentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.leftEdgeShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        backgroundView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        isAnimating = true
        val animDuration = if (velocity > 1000f) {
            val remaining = width - startX
            (remaining / velocity * 1000f).toLong().coerceIn(120L, 250L)
        } else {
            250L
        }

        ValueAnimator.ofFloat(startX, width).apply {
            duration = animDuration
            interpolator = iosDecelerateInterpolator
            addUpdateListener { animator ->
                val currentX = animator.animatedValue as Float
                val fraction = currentX / width
                binding.contentContainer.translationX = currentX
                binding.leftEdgeShadow.translationX = currentX - shadowW
                binding.scrimOverlay.alpha = max(0f, (1f - fraction) * 0.25f)
                backgroundView?.translationX = -(1f - fraction) * (width / 3f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    backgroundView?.translationX = 0f
                    isAnimating = false
                    if (isAdded) {
                        parentFragmentManager.popBackStack()
                    }
                }
            })
            start()
        }
    }

    private fun cancelDismissAnimation() {
        val width = getScreenWidth()
        val shadowW = getShadowWidth()
        val startX = binding.contentContainer.translationX

        isAnimating = true
        ValueAnimator.ofFloat(startX, 0f).apply {
            duration = 200L
            interpolator = iosDecelerateInterpolator
            addUpdateListener { animator ->
                val currentX = animator.animatedValue as Float
                val fraction = currentX / width
                binding.contentContainer.translationX = currentX
                binding.leftEdgeShadow.translationX = currentX - shadowW
                binding.scrimOverlay.alpha = (1f - fraction) * 0.25f
                backgroundView?.translationX = -(1f - fraction) * (width / 3f)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.contentContainer.translationX = 0f
                    binding.leftEdgeShadow.translationX = -shadowW
                    binding.scrimOverlay.alpha = 0.25f
                    backgroundView?.translationX = -(width / 3f)
                    binding.contentContainer.setLayerType(View.LAYER_TYPE_NONE, null)
                    binding.leftEdgeShadow.setLayerType(View.LAYER_TYPE_NONE, null)
                    backgroundView?.setLayerType(View.LAYER_TYPE_NONE, null)
                    isAnimating = false
                }
            })
            start()
        }
    }

    private fun setupRowClickListeners() {
        val denoms = viewModel.supportedDenominations
        denoms.forEach { value ->
            getCountTextView(value)?.setOnClickListener {
                onRowSelected(value)
            }
            getMinusButton(value)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.decrement(value)
                isFresh = false
            }
            getPlusButton(value)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.increment(value)
                isFresh = false
            }
        }
    }

    private fun onRowSelected(value: Int) {
        val currentActive = viewModel.activeDenomination.value
        if (currentActive == value) {
            // Already active, close
            closeKeypad()
        } else {
            isFresh = true
            viewModel.setActiveDenomination(value)
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            ensureRowVisible(value)
        }
    }

    private fun ensureRowVisible(value: Int) {
        val row = getRowView(value) ?: return
        binding.scrollView.post {
            val rowTop = row.top
            val rowBottom = row.bottom
            val scrollY = binding.scrollView.scrollY
            val height = binding.scrollView.height
            if (rowTop < scrollY) {
                binding.scrollView.smoothScrollTo(0, rowTop - 16)
            } else if (rowBottom > scrollY + height) {
                binding.scrollView.smoothScrollTo(0, rowBottom - height + 16)
            }
        }
    }

    private fun setupKeypadListeners() {
        val digitButtons = mapOf(
            binding.btnKey0 to 0,
            binding.btnKey1 to 1,
            binding.btnKey2 to 2,
            binding.btnKey3 to 3,
            binding.btnKey4 to 4,
            binding.btnKey5 to 5,
            binding.btnKey6 to 6,
            binding.btnKey7 to 7,
            binding.btnKey8 to 8,
            binding.btnKey9 to 9
        )

        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val active = viewModel.activeDenomination.value ?: return@setOnClickListener
                val limitHit = viewModel.appendDigit(active, digit, isFresh)
                if (limitHit) {
                    showLimitHitWarning(active)
                }
                isFresh = false
            }
        }

        binding.btnKeyBackspace.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val active = viewModel.activeDenomination.value ?: return@setOnClickListener
            viewModel.backspace(active, isFresh)
            isFresh = false
        }

        binding.btnKeyNext.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            closeKeypad()
        }

        binding.btnKeypadDone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            closeKeypad()
        }
    }

    private fun showLimitHitWarning(value: Int) {
        val countView = getCountTextView(value)
        countView?.let {
            val shake = TranslateAnimation(0f, 10f, 0f, 0f).apply {
                duration = 300
                interpolator = CycleInterpolator(3f)
            }
            it.startAnimation(shake)
        }

        hintResetRunnable?.let { handler.removeCallbacks(it) }
        binding.tvKeypadHint.text = getString(R.string.keypad_hint_limit)
        binding.tvKeypadHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_danger_text))

        hintResetRunnable = Runnable {
            if (_binding != null) {
                binding.tvKeypadHint.text = getString(R.string.keypad_hint_type)
                binding.tvKeypadHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_text_slate))
            }
        }
        handler.postDelayed(hintResetRunnable!!, 1800)
    }

    private fun setupToolbarActions() {
        binding.btnBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dismissWithAnimation()
        }

        binding.btnReset.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            viewModel.resetAll()
        }

        binding.btnCurrency.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            Toast.makeText(requireContext(), "Currency: Indian Rupee (INR)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHeroClickListeners() {
        binding.heroSection.setOnClickListener {
            if (viewModel.activeDenomination.value != null) {
                closeKeypad()
            }
        }
    }

    private fun setupBackCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.activeDenomination.value != null) {
                    closeKeypad()
                } else {
                    dismissWithAnimation()
                }
            }
        })
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.activeDenomination.value != null) {
            closeKeypad()
            return true
        }
        dismissWithAnimation()
        return true
    }

    private fun dismissWithAnimation() {
        if (isAnimating) return
        closeKeypad()
        completeDismissAnimation()
    }

    private fun closeKeypad() {
        viewModel.setActiveDenomination(null)
        isFresh = false
    }

    private fun setupStateObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.denominations.collect { items ->
                        renderDenominations(items)
                    }
                }
                launch {
                    viewModel.activeDenomination.collect { active ->
                        renderActiveState(active)
                    }
                }
            }
        }
    }

    private fun renderDenominations(items: List<CashDenomination>) {
        val colorObsidian = ContextCompat.getColor(requireContext(), R.color.cash_obsidian)
        val colorMuted = ContextCompat.getColor(requireContext(), R.color.cash_text_muted)

        items.forEach { item ->
            val countView = getCountTextView(item.value)
            val subtotalView = getSubtotalTextView(item.value)
            val minusBtn = getMinusButton(item.value)
            val plusBtn = getPlusButton(item.value)

            countView?.text = item.count.toString()
            subtotalView?.text = "₹" + indianFormat.format(item.subtotal)

            if (item.count == 0L) {
                countView?.setTextColor(colorMuted)
                subtotalView?.setTextColor(colorMuted)
                minusBtn?.isEnabled = false
                minusBtn?.alpha = 0.3f
            } else {
                countView?.setTextColor(colorObsidian)
                subtotalView?.setTextColor(colorObsidian)
                minusBtn?.isEnabled = true
                minusBtn?.alpha = 1.0f
            }

            if (item.count >= CashCounterViewModel.MAX_COUNT) {
                plusBtn?.isEnabled = false
                plusBtn?.alpha = 0.3f
            } else {
                plusBtn?.isEnabled = true
                plusBtn?.alpha = 1.0f
            }
        }

        // Hero Total Section
        val grandTotal = viewModel.grandTotal
        val notesCount = viewModel.totalNotes
        val coinsCount = viewModel.totalCoins
        val totalPieces = viewModel.totalPieces

        binding.tvGrandTotalAmount.text = indianFormat.format(grandTotal)
        binding.tvAmountInWords.text = IndianCurrencyWords.format(grandTotal)

        // Pill text — always show notes + coins breakdown
        val notesStr = "${indianFormat.format(notesCount)} " + if (notesCount == 1L) "note" else "notes"
        val pillText = if (coinsCount > 0L) {
            val coinsStr = "${indianFormat.format(coinsCount)} " + if (coinsCount == 1L) "coin" else "coins"
            "$notesStr + $coinsStr"
        } else {
            notesStr
        }
        binding.tvCountPill.text = pillText

        // Spectrum Bar
        renderSpectrumBar(items, grandTotal)

        // Reset Button enabled/disabled
        binding.btnReset.isEnabled = totalPieces > 0L
        binding.btnReset.alpha = if (totalPieces > 0L) 1.0f else 0.32f
    }

    private fun renderSpectrumBar(items: List<CashDenomination>, grandTotal: Long) {
        val spectrumViews = mapOf(
            500 to binding.spectrum500,
            200 to binding.spectrum200,
            100 to binding.spectrum100,
            50 to binding.spectrum50,
            20 to binding.spectrum20,
            10 to binding.spectrum10,
            5 to binding.spectrum5,
            2 to binding.spectrum2,
            1 to binding.spectrum1
        )

        if (grandTotal <= 0L) {
            spectrumViews.values.forEach { it.visibility = View.GONE }
            return
        }

        items.forEach { item ->
            val view = spectrumViews[item.value] ?: return@forEach
            if (item.subtotal > 0L) {
                view.visibility = View.VISIBLE
                val lp = view.layoutParams as LinearLayout.LayoutParams
                lp.weight = item.subtotal.toFloat()
                view.layoutParams = lp
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun renderActiveState(active: Int?) {
        val isKeypadOpen = active != null
        applyBottomInsets(isKeypadOpen)

        // Update each row's active/inactive background and count pill background
        viewModel.supportedDenominations.forEach { value ->
            val row = getRowView(value)
            val countView = getCountTextView(value)
            if (value == active) {
                row?.setBackgroundResource(R.drawable.bg_row_active)
                countView?.setBackgroundResource(if (isFresh) R.drawable.bg_count_fresh else R.drawable.bg_count_active)
            } else {
                row?.setBackgroundResource(0)
                countView?.setBackgroundResource(R.drawable.bg_count_normal)
            }
        }

        if (active != null) {
            // Keypad is open
            binding.keypadLayout.visibility = View.VISIBLE

            // Compact mode on Hero Section
            binding.tvAmountInWords.visibility = View.GONE
            binding.tvGrandTotalAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            binding.tvCurrencySymbol.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            binding.layoutSpectrum.updateLayoutParams<ViewGroup.LayoutParams> {
                height = (6 * resources.displayMetrics.density).toInt()
            }

            // Sync Keypad Header
            syncKeypadHeader(active)
        } else {
            // Keypad is closed
            binding.keypadLayout.visibility = View.GONE

            // Normal mode on Hero Section
            binding.tvAmountInWords.visibility = View.VISIBLE
            binding.tvGrandTotalAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)
            binding.tvCurrencySymbol.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            binding.layoutSpectrum.updateLayoutParams<ViewGroup.LayoutParams> {
                height = (8 * resources.displayMetrics.density).toInt()
            }
        }

        // Re-render hero pill for compact/normal mode
        renderDenominations(viewModel.denominations.value)
    }

    private fun syncKeypadHeader(active: Int) {
        // Active mini chip
        binding.tvActiveChip.text = "₹$active"
        val density = resources.displayMetrics.density
        val isCoin = (active == 5 || active == 2 || active == 1)
        if (isCoin) {
            val size = (32 * density).toInt()
            binding.tvActiveChip.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
        } else {
            binding.tvActiveChip.updateLayoutParams<ViewGroup.LayoutParams> {
                width = (56 * density).toInt()
                height = (28 * density).toInt()
            }
        }

        when (active) {
            500 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_500)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_500_text))
            }
            200 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_200)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_200_text))
            }
            100 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_100)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_100_text))
            }
            50 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_50)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_50_text))
            }
            20 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_20)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_20_text))
            }
            10 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_note_10)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_note_10_text))
            }
            5 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_coin_5)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_coin_5_text))
            }
            2 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_coin_2)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_coin_2_text))
            }
            1 -> {
                binding.tvActiveChip.setBackgroundResource(R.drawable.bg_coin_1)
                binding.tvActiveChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_coin_1_text))
            }
        }

        // Hint Text
        binding.tvKeypadHint.text = getString(R.string.keypad_hint_type)
        binding.tvKeypadHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.cash_text_slate))

        // Keypad action button is always Done
        binding.tvNextLabel.text = getString(R.string.keypad_done)
        binding.ivNextIcon.setImageResource(R.drawable.baseline_check)
    }

    private fun getRowView(value: Int): View? {
        return when (value) {
            500 -> binding.row500
            200 -> binding.row200
            100 -> binding.row100
            50 -> binding.row50
            20 -> binding.row20
            10 -> binding.row10
            5 -> binding.row5
            2 -> binding.row2
            1 -> binding.row1
            else -> null
        }
    }

    private fun getCountTextView(value: Int): TextView? {
        return when (value) {
            500 -> binding.pillCount500
            200 -> binding.pillCount200
            100 -> binding.pillCount100
            50 -> binding.pillCount50
            20 -> binding.pillCount20
            10 -> binding.pillCount10
            5 -> binding.pillCount5
            2 -> binding.pillCount2
            1 -> binding.pillCount1
            else -> null
        }
    }

    private fun getSubtotalTextView(value: Int): TextView? {
        return when (value) {
            500 -> binding.tvSubtotal500
            200 -> binding.tvSubtotal200
            100 -> binding.tvSubtotal100
            50 -> binding.tvSubtotal50
            20 -> binding.tvSubtotal20
            10 -> binding.tvSubtotal10
            5 -> binding.tvSubtotal5
            2 -> binding.tvSubtotal2
            1 -> binding.tvSubtotal1
            else -> null
        }
    }

    private fun getMinusButton(value: Int): View? {
        return when (value) {
            500 -> binding.btnMinus500
            200 -> binding.btnMinus200
            100 -> binding.btnMinus100
            50 -> binding.btnMinus50
            20 -> binding.btnMinus20
            10 -> binding.btnMinus10
            5 -> binding.btnMinus5
            2 -> binding.btnMinus2
            1 -> binding.btnMinus1
            else -> null
        }
    }

    private fun getPlusButton(value: Int): View? {
        return when (value) {
            500 -> binding.btnPlus500
            200 -> binding.btnPlus200
            100 -> binding.btnPlus100
            50 -> binding.btnPlus50
            20 -> binding.btnPlus20
            10 -> binding.btnPlus10
            5 -> binding.btnPlus5
            2 -> binding.btnPlus2
            1 -> binding.btnPlus1
            else -> null
        }
    }

    private fun getScreenWidth(): Float {
        return resources.displayMetrics.widthPixels.toFloat()
    }

    private fun getShadowWidth(): Float {
        return 16f * resources.displayMetrics.density
    }

    companion object {
        const val TAG = "CashCounterFragment"
        fun newInstance() = CashCounterFragment()
    }

    override fun onDestroyView() {
        hintResetRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }
}
