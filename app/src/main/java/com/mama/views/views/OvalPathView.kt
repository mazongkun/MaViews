package com.mama.views.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.*
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi

/**
 * Created on: 2020-06-19 16:25
 * Author: mazongkun
 */
class OvalPathView : View {
    private val TAG = javaClass.simpleName
    private lateinit var ovalRect: RectF
    private lateinit var ovalPath: Path
    private lateinit var paint: Paint
    private lateinit var pathMeasure: PathMeasure
    private lateinit var valueAnimator: ValueAnimator

    private var mWidth  = 0
    private var mHeight = 0
    private val PI = Math.PI
    private val duration = 2000L
    private var animLength : Float = 0f
    private val pos : FloatArray by lazy { FloatArray(2) }
    private val tan : FloatArray by lazy { FloatArray(2) }

    constructor(context: Context?) : super(context) {init()}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        Log.d(TAG, "onVisibilityChanged : $visibility")
        when (visibility) {
            VISIBLE -> valueAnimator.start()
            else -> if (valueAnimator.isRunning) valueAnimator.end()
        }
    }

    private fun init() {
        ovalRect = RectF()
        ovalPath = Path()
        paint = Paint()
        pathMeasure = PathMeasure()
        valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = duration
        valueAnimator.addUpdateListener { animation ->
            if (animation.animatedValue is Float) {
                animLength = animation.animatedValue as Float
//                Log.d(TAG, "animation.animatedValue = ${animation.animatedValue}")
            }
            Log.d(TAG, "animLength = ${animLength}")
            postInvalidate()
        }

        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.repeatMode = RESTART
        valueAnimator.repeatCount = 1
        valueAnimator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth  = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        mHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null)
            return

        Log.d(TAG, "mWidth = $mWidth, mHeight = $mHeight")
        Log.d(TAG, "getWidth() = ${width}, getHeight() = ${height}")

        val left   = mWidth / 2 - mWidth / 3
        val right  = mWidth / 2 + mWidth / 3
        val top    = mHeight / 2 - mHeight / 3
        val bottom = mHeight / 2 + mHeight / 3

//        Log.d(TAG, "(left, top, right, bottom) -> ($left, $top, $right, $bottom)")
        ovalRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        ovalPath.reset()
//        ovalPath.addOval(ovalRect, Path.Direction.CW)
        ovalPath.addArc(ovalRect, 0f, 315f);

        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.RED
        canvas.drawPath(ovalPath, paint)
//        PathMeasure
        pathMeasure.setPath(ovalPath, false)
        val pathLength = pathMeasure.length
        pathMeasure.getPosTan(pathLength * animLength, pos, tan)
        Log.d(TAG, "animLength = $animLength, pos: (${pos[0]}, ${pos[1]}), tan: (${tan[0]}, ${tan[1]})")

        paint.style = Paint.Style.FILL
        paint.color = Color.BLUE
        canvas.drawCircle(pos[0], pos[1], 20f, paint)

    }
}