package com.mama.views.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.mama.views.R
import com.mama.views.utils.DisplayUtils
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created on: 2020-06-16 17:40
 * Author: mazongkun
 */
class ClockView : View {
    private val TAG = javaClass.simpleName;
    private lateinit var paint : Paint
    private var mWidth  = 0
    private var mHeight = 0
    // dial
    private var dialColor : Int = Color.RED

    // letters
    private var letterColor : Int = Color.YELLOW

    // hands
    private val calendar : Calendar by lazy { Calendar.getInstance() }
    private var hourColor : Int = Color.BLUE
    private var minColor : Int = Color.CYAN
    private var secColor : Int = Color.GREEN

    // text
    private var textColor : Int = dialColor

    private lateinit var center : PointF
    private lateinit var clockTask : Runnable
    private lateinit var textCenter : PointF

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        // init attr
        if (context != null && attrs != null) {
            val ta : TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ClockView);
            dialColor = ta.getColor(R.styleable.ClockView_dialColor, dialColor)
            letterColor = ta.getColor(R.styleable.ClockView_lettersColor, letterColor)
            hourColor = ta.getColor(R.styleable.ClockView_hourHandColor, hourColor)
            minColor  = ta.getColor(R.styleable.ClockView_minHandColor, minColor)
            secColor  = ta.getColor(R.styleable.ClockView_secHandColor, secColor)
            textColor = ta.getColor(R.styleable.ClockView_textColor, dialColor) // default dialColor

            ta.recycle();
        }

        // draw
        paint = Paint()
        center = PointF()
        textCenter = PointF()
        clockTask = Runnable {
            println("clockTask")
            invalidate()
            handler.postDelayed(clockTask, 1000)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        Log.d(TAG, "onVisibilityChanged : $visibility")
        when (visibility) {
            VISIBLE -> handler.post(clockTask)
            else -> handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
//        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        Log.d(TAG, "onMeasure: suggestedMinimumWidth: $suggestedMinimumWidth, suggestedMinimumHeight: $suggestedMinimumHeight");
        Log.d(TAG, "onMeasure: widthMeasureSpec: $widthMeasureSpec, heightMeasureSpec: $heightMeasureSpec");

        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        Log.d(TAG, "onMeasure: widthSpecMode: ${widthSpecMode.toString(16)}, widthSpecSize: $widthSpecSize");
        val width = when (widthSpecMode) {
            MeasureSpec.UNSPECIFIED -> suggestedMinimumWidth
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> widthSpecSize
            else -> suggestedMinimumWidth
        }
        Log.d(TAG, "onMeasure: heightSpecMode: ${heightSpecMode.toString(16)}, heightSpecSize: $heightSpecSize");
        val height = when (heightSpecMode) {
            MeasureSpec.UNSPECIFIED -> suggestedMinimumHeight
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> heightSpecSize
            else -> suggestedMinimumHeight
        }
        Log.d(TAG, "onMeasure: w: $width, h: $height")
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = width
        mHeight = height
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        if (mWidth <= 0 || mHeight <= 0 || canvas == null)
            return

        // background
        paint.color = dialColor
        paint.style = Paint.Style.FILL
//        paint.strokeWidth = 2f
        val radius = if (mWidth < mHeight) { mWidth.toFloat() * 2/3 / 2 } else { mHeight.toFloat() *2/3 / 2}
        center.set(mWidth.toFloat()/2, mHeight.toFloat()/3)
        Log.d(TAG, "onDraw: center: $center, radius: $radius, paint: $paint")
        canvas.drawCircle(center.x, center.y, radius, paint)

        val PI = Math.PI
        // letters
        paint.color = letterColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 10f
        val centerX = center.x;
        val centerY = center.y
        val innerR = radius * 6/8
        val outerR = radius * 7/8
        for (i in 0..11) {
            val startX = centerX + sin(i * PI/6) * outerR
            val endX = centerX + sin(i * PI/6) * innerR
            val startY = centerY - cos(i * PI/6) * outerR
            val endY = centerY - cos(i * PI/6) * innerR
            canvas.drawLine(startX.toFloat(), startY.toFloat(),
                endX.toFloat(), endY.toFloat(), paint)
        }

        // hands
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min  = calendar.get(Calendar.MINUTE)
        val sec  = calendar.get(Calendar.SECOND)
        Log.d(TAG, "calender : $hour:$min:$sec")
        // hour hand
        paint.color = hourColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 12f
        var positiveR = radius / 3
        var oppositeR = radius / 8
        var startX = centerX + sin(PI + (hour + min/60f)*PI/6) * oppositeR
        var startY = centerY - cos(PI + (hour + min/60f)*PI/6) * oppositeR
        var endX = centerX + sin((hour + min/60f)*PI/6) * positiveR
        var endY = centerY - cos((hour + min/60f)*PI/6) * positiveR
//        Log.d(TAG, "hour hand : ($startX, $startY) -> ($endX, $endY)")
        canvas.drawLine(startX.toFloat(), startY.toFloat(),
            endX.toFloat(), endY.toFloat(), paint)

        // min hand
        paint.color = minColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 8f
        positiveR = radius * 2 / 3
        oppositeR = radius / 8
        startX = centerX + sin(PI + min / 60f * (2*PI)) * oppositeR
        startY = centerY - cos(PI + min / 60f * (2*PI)) * oppositeR
        endX = centerX + sin(min / 60f * (2*PI)) * positiveR
        endY = centerY - cos(min / 60f * (2*PI)) * positiveR
        canvas.drawLine(startX.toFloat(), startY.toFloat(),
            endX.toFloat(), endY.toFloat(), paint)

        // sec hand
        paint.color = secColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 6f
        positiveR = radius * 2 / 3
        oppositeR = radius / 8
        startX = centerX + sin(PI + sec / 60f * (2*PI)) * oppositeR
        startY = centerY - cos(PI + sec / 60f * (2*PI)) * oppositeR
        endX = centerX + sin(sec / 60f * (2*PI)) * positiveR
        endY = centerY - cos(sec / 60f * (2*PI)) * positiveR
        canvas.drawLine(startX.toFloat(), startY.toFloat(),
            endX.toFloat(), endY.toFloat(), paint)

        // text
        val textPixSize = DisplayUtils.sp2px(context, 50f);
        paint.color = textColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 4f
        paint.textSize = textPixSize.toFloat()
        paint.textAlign = Paint.Align.CENTER
//        center.set(mWidth.toFloat()/2, mHeight.toFloat()/3)
        textCenter.set(mWidth.toFloat()/2, mHeight.toFloat()*3/4)
        canvas.drawText( String.format("%02d:%02d:%02d", hour, min, sec), textCenter.x, textCenter.y, paint )
    }
}