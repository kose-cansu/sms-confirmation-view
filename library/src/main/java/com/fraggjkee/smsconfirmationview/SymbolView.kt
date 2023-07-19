package com.fraggjkee.smsconfirmationview

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Size
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px

private const val textPaintAlphaAnimDuration = 150L
private const val borderPaintAlphaAnimDuration = 200L

private const val cursorAlphaAnimDuration = 500L
private const val cursorAlphaAnimStartDelay = 200L

private const val cursorSymbol = "|"

@SuppressLint("ViewConstructor")
internal class SymbolView(context: Context, private val symbolStyle: Style) : View(context) {

    data class State(
        val symbol: Char? = null,
        val isActive: Boolean = false
    )

    var state: State = State()
        set(value) {
            if (field == value) return
            field = value
            updateState(state)
        }

    private val showCursor: Boolean get() = symbolStyle.showCursor

    private val desiredW: Int
    private val desiredH: Int
    private val textSizePx: Int
    private val cornerRadius: Float

    private val backgroundRect = RectF()

    private val backgroundPaint: Paint
    private val borderPaint: Paint
    private val textPaint: Paint

    private var textSize: Size

    private var textAnimator: Animator? = null

    init {
        desiredW = symbolStyle.width
        desiredH = symbolStyle.height
        textSizePx = symbolStyle.textSize
        cornerRadius = symbolStyle.borderCornerRadius

        backgroundPaint = Paint().apply {
            this.color = symbolStyle.backgroundColor
            this.style = Paint.Style.FILL
        }

        borderPaint = Paint().apply {
            this.isAntiAlias = true
            this.color = symbolStyle.borderColor
            this.style = Paint.Style.STROKE
            this.strokeWidth = symbolStyle.borderWidth.toFloat()
        }

        textPaint = Paint().apply {
            this.isAntiAlias = true
            this.color = symbolStyle.textColor
            this.textSize = textSizePx.toFloat()
            this.typeface = Typeface.DEFAULT_BOLD
            this.textAlign = Paint.Align.CENTER
        }

        textSize = calculateTextSize('0')
    }

    @Suppress("SameParameterValue")
    private fun calculateTextSize(symbol: Char): Size {
        val textBounds = Rect()
        textPaint.getTextBounds(symbol.toString(), 0, 1, textBounds)
        return Size(textBounds.width(), textBounds.height())
    }

    private fun updateState(state: State) = with(state) {
        textAnimator?.cancel()
        if (symbol == null && isActive && showCursor) {
            textPaint.color = symbolStyle.borderColorActive
            textAnimator = ObjectAnimator.ofInt(textPaint, "alpha", 255, 255, 0, 0)
                .apply {
                    duration = cursorAlphaAnimDuration
                    startDelay = cursorAlphaAnimStartDelay
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    addUpdateListener { invalidate() }
                }
        } else {
            textPaint.color = symbolStyle.textColor
            val startAlpha = if (symbol == null) 255 else 127
            val endAlpha = if (symbol == null) 0 else 255
            textAnimator = ObjectAnimator.ofInt(textPaint, "alpha", startAlpha, endAlpha)
                .apply {
                    duration = textPaintAlphaAnimDuration
                    addUpdateListener { invalidate() }
                }
        }

        textAnimator?.start()
        val isFilled = symbol != null
        backgroundColorChange(isFilled)
        borderColorChange(isFilled,isActive)
    }

    private fun borderColorChange(isFilled: Boolean,isActive: Boolean) {
        val borderColor = symbolStyle.borderColor
        val borderColorFilled = symbolStyle.filledBorderColor

        if (borderColor == borderColorFilled) {
            return
        }

        if (isFilled) {
            borderPaint.color = borderColorFilled
        } else {
            borderPaint.color = borderColor
        }

        val colorFrom =
            if (isActive) borderColor
            else if (!isFilled) borderColor
            else borderColorFilled
        val colorTo =
            if (isActive) borderColor
            else if (!isFilled) borderColor
            else borderColorFilled
        ObjectAnimator.ofObject(borderPaint, "color", ArgbEvaluator(), colorFrom, colorTo)
            .apply {
                duration = borderPaintAlphaAnimDuration
                addUpdateListener { invalidate() }
            }
            .start()

    }

    private fun backgroundColorChange(isFilled: Boolean) {
        val backgroundColor = symbolStyle.backgroundColor
        val backgroundColorFilled = symbolStyle.filledBackgroundColor

        if (isFilled) {
            backgroundPaint.color = backgroundColorFilled
        } else {
            backgroundPaint.color = backgroundColor
        }

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSizeAndState(desiredW, widthMeasureSpec, 0)
        val h = resolveSizeAndState(desiredH, heightMeasureSpec, 0)
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val borderWidthHalf = borderPaint.strokeWidth / 2
        backgroundRect.left = borderWidthHalf
        backgroundRect.top = borderWidthHalf
        backgroundRect.right = measuredWidth.toFloat() - borderWidthHalf
        backgroundRect.bottom = measuredHeight.toFloat() - borderWidthHalf
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(
            backgroundRect,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        )

        canvas.drawRoundRect(
            backgroundRect,
            cornerRadius,
            cornerRadius,
            borderPaint
        )

        canvas.drawText(
            if (state.isActive && showCursor) cursorSymbol else state.symbol?.toString() ?: "",
            backgroundRect.width() / 2 + borderPaint.strokeWidth / 2,
            backgroundRect.height() / 2 + textSize.height / 2 + borderPaint.strokeWidth / 2,
            textPaint
        )
    }

    data class Style(
        val showCursor: Boolean,
        @Px val width: Int,
        @Px val height: Int,
        @ColorInt val backgroundColor: Int,
        @ColorInt val borderColor: Int,
        @ColorInt val borderColorActive: Int,
        @ColorInt val filledBackgroundColor:Int,
        @ColorInt val filledBorderColor:Int,
        @Px val borderWidth: Int,
        val borderCornerRadius: Float,
        @ColorInt val textColor: Int,
        @Px val textSize: Int,
        val typeface: Typeface = Typeface.DEFAULT_BOLD
    )
}
