package com.kal.cashcounter

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.max

class SwipeDismissLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val edgeWidth = 56 * resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var velocityTracker: VelocityTracker? = null

    var isKeypadActive: Boolean = false

    var onSwipeListener: OnSwipeListener? = null

    interface OnSwipeListener {
        fun onSwipeStart()
        fun onSwipeProgress(translationX: Float, fraction: Float)
        fun onSwipeDismissed(velocity: Float)
        fun onSwipeCanceled()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                // Edge swipe starting from left edge
                val isEdgeSwipe = downX <= edgeWidth && dx > touchSlop && dx > abs(dy)
                // When keypad is NOT active, also allow horizontal swipe across screen
                val isHorizontalSwipe = !isKeypadActive && dx > touchSlop * 2 && dx > abs(dy) * 1.8f

                if (!isDragging && (isEdgeSwipe || isHorizontalSwipe)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    onSwipeListener?.onSwipeStart()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        velocityTracker?.addMovement(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = max(0f, ev.rawX - downX)
                    val fraction = if (width > 0) (dx / width.toFloat()).coerceIn(0f, 1f) else 0f
                    onSwipeListener?.onSwipeProgress(dx, fraction)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val dx = max(0f, ev.rawX - downX)
                    val fraction = if (width > 0) (dx / width.toFloat()).coerceIn(0f, 1f) else 0f

                    if (fraction > 0.35f || vx > 1000f) {
                        onSwipeListener?.onSwipeDismissed(vx)
                    } else {
                        onSwipeListener?.onSwipeCanceled()
                    }
                    isDragging = false
                    return true
                } else {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}
