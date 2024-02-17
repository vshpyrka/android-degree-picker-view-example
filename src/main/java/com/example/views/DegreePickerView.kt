package com.example.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import java.lang.Math.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DegreePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val minSize = 200f.dpToPx(context)

    private val startAngle = -130f
    private val angle = 260f
    private val maxValue = 50
    private var degree = 30

    private val degreeTextSize = 30f.spToPx(context)
    private var textBounds = Rect()
    private val degreeHintPointSize = 10f.spToPx(context)
    private var textHintBounds = Rect()

    private val internalPadding = 0f.dpToPx(context)
    private val outerRingPointWidth = 3f.dpToPx(context)
    private val outerRingSmallPointHeight = 16f.dpToPx(context)
    private val outerRingLargePointHeight = 22f.dpToPx(context)
    private val outerRingPointMarginTopLarge = 8f.dpToPx(context)
    private val outerRingPointMarginTopSmall = 2f.dpToPx(context)

    private val innerCircleMarginTop = 8f.dpToPx(context)
    private val innerCirclePointSmallWidth = 2f.dpToPx(context)
    private val innerCirclePointSmallHeight = 4f.dpToPx(context)
    private val innerCirclePointLargeHeight = 8f.dpToPx(context)
    private val innerCircleHintPointMarginTop = 4f.dpToPx(context)

    private val outerPointsPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val gradientPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
    }

    private val outerRingNotFilledPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#172243")
    }

    private val degreeTextPaint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        textAlign = Paint.Align.CENTER
        textSize = degreeTextSize
        typeface = ResourcesCompat.getFont(context, R.font.roboto_regular)
        color = Color.WHITE
    }

    private val innerCirclePointsPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#505073")
    }

    private val innerCircleHintPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#505073")
        textSize = degreeHintPointSize
    }

    private var animator: Animator? = null

    private val colors = intArrayOf(
        Color.parseColor("#e60b21"),
        Color.parseColor("#e60b21"),
        Color.parseColor("#5742f5"),
        Color.parseColor("#5742f5"),
        Color.parseColor("#fcba03"),
        Color.parseColor("#fcba03"),
        Color.parseColor("#e60b21"),
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(minSize.toInt(), widthMeasureSpec)
        val height = resolveSize(minSize.toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientPaint.shader = SweepGradient(
            (w / 2).toFloat(),
            (h / 2).toFloat(),
            colors,
            null
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerWidth = (width / 2).toFloat()
        val centerHeight = (height / 2).toFloat()

        // Draw degree text
        val text = degree.toString()
        degreeTextPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(
            "$text°",
            centerHeight,
            centerHeight + (textBounds.height() / 2),
            degreeTextPaint
        )

        // Rotate canvas to left
        val rotationAngle = startAngle
        canvas.rotate(rotationAngle, centerWidth, centerHeight)

        // Calculate degree of each mark
        val pointsRotationDegree = angle / maxValue
        var rotationDegree = 0f
        var counter = 0

        while (rotationDegree < angle) {
            canvas.save()

            if (rotationDegree != 0f) {
                // Rotate canvas to points degree
                canvas.rotate(rotationDegree, centerWidth, centerHeight)
            }

            val topPadding =
                internalPadding + outerRingPointMarginTopLarge + outerRingSmallPointHeight + innerCircleMarginTop

            // Draw inner ring marks
            val tickHeight = if (counter % 10 == 0) {
                innerCirclePointLargeHeight
            } else {
                innerCirclePointSmallHeight
            }
            canvas.drawRect(
                centerWidth - (innerCirclePointSmallWidth / 2),
                topPadding,
                centerWidth + (innerCirclePointSmallWidth / 2),
                (topPadding + tickHeight),
                innerCirclePointsPaint,
            )

            // Draw outer ring marks
            val outerRingPointMarginTop: Float
            val outerRingPointHeight: Float
            if (counter == degree) {
                outerRingPointMarginTop = outerRingPointMarginTopSmall
                outerRingPointHeight = outerRingLargePointHeight
            } else {
                outerRingPointMarginTop = outerRingPointMarginTopLarge
                outerRingPointHeight = outerRingSmallPointHeight
            }
            canvas.drawRect(
                centerWidth - (outerRingPointWidth / 2),
                internalPadding + outerRingPointMarginTop,
                centerWidth + (outerRingPointWidth / 2),
                (internalPadding + outerRingPointMarginTop + outerRingPointHeight),
                if (counter > degree) {
                    outerRingNotFilledPaint
                } else {
                    outerPointsPaint
                },
            )

            if (counter == 0) {
                val textHint = "0"
                innerCircleHintPaint.getTextBounds(textHint, 0, textHint.length, textHintBounds)

                val x = centerWidth
                val y =
                    internalPadding + outerRingPointMarginTopLarge + outerRingSmallPointHeight + innerCircleMarginTop + innerCirclePointLargeHeight + innerCircleHintPointMarginTop
                canvas.rotate(-rotationAngle, x, y)
                canvas.drawText(
                    textHint,
                    x - (textHintBounds.width() / 2),
                    y,
                    innerCircleHintPaint
                )
                canvas.rotate(rotationAngle, x, y)
            } else if (counter == maxValue) {
                val textHint = "50"
                innerCircleHintPaint.getTextBounds(textHint, 0, textHint.length, textHintBounds)

                val x = centerWidth
                val y =
                    internalPadding + outerRingPointMarginTopLarge + outerRingSmallPointHeight + innerCircleMarginTop + innerCirclePointLargeHeight + innerCircleHintPointMarginTop
                canvas.rotate(-rotationAngle - rotationDegree, x, y)
                canvas.drawText(
                    textHint,
                    x - (textHintBounds.width() / 2),
                    y,
                    innerCircleHintPaint
                )
                canvas.rotate(rotationAngle + rotationDegree, x, y)
            }

            rotationDegree += pointsRotationDegree
            canvas.restore()
            counter++
        }

        // Rotate canvas back
        canvas.rotate(
            -rotationAngle,
            centerWidth,
            centerHeight
        )
        // Draw outer ring gradient
        canvas.drawCircle(
            centerWidth,
            centerHeight,
            centerHeight,
            gradientPaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isOnOuterRing(event, outerRingLargePointHeight)) {
            val touchDegree = getTouchDegree(event)
            if (touchDegree >= 140.0) {
                val newDegree = ((touchDegree - 140) / (angle / maxValue)).roundToInt()
                animateDegreeChange(degree, newDegree)
            } else if (touchDegree <= 40.0) {
                val newDegree = ((touchDegree + 220) / (angle / maxValue)).roundToInt()
                animateDegreeChange(degree, newDegree)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateDegreeChange(current: Int, to: Int) {
        animator?.cancel()
        ValueAnimator.ofInt(current, to)
            .apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    degree = it.animatedValue as Int
                    invalidate()
                }
            }
            .also {
                animator = it
            }
            .start()
    }

    private fun isOnOuterRing(event: MotionEvent, strokeWidth: Float): Boolean {
        val centerWidth = (width / 2).toFloat()
        val centerHeight = (height / 2).toFloat()
        // Figure the Euclidean distance from center point to touch point.
        val distance = distance(
            event.x, event.y,
            centerWidth, centerHeight
        )

        val radius = centerWidth

        return abs(distance - radius) <= strokeWidth
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        // c^2 = a^2 + b^2
        // c = sqrt(  (x1 - x2)^2 + (y1 - y2)^2  )
        return sqrt(
            (x1 - x2).pow(2f) + (y1 - y2).pow(2f)
        )
    }

    private fun getTouchDegree(event: MotionEvent): Double {
        val centerWidth = (width / 2).toFloat()
        val centerHeight = (height / 2).toFloat()

        val opposite = (event.y - centerHeight)
        val adjacent = (event.x - centerWidth)
        // tan = opposite / adjacent
        val arcTan = atan2(opposite, adjacent)

        // Convert from radians to degrees
        val at = arcTan * 180 / PI

//        val angle = (at + 360) % 360
        val angle = if (at < 0) {
            at + 360
        } else {
            at
        }
        return angle
    }

    private fun Float.dpToPx(context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
        )
    }

    private fun Float.spToPx(context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics
        )
    }
}
