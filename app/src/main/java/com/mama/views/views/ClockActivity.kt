package com.mama.views.views

import android.os.Bundle
import com.mama.views.R
import com.mama.views.base.BaseActivity
import kotlinx.android.synthetic.main.activity_clock.*

/**
 * Created on: 2020-06-16 18:52
 * Author: mazongkun
 */
class ClockActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock)

        initView()
    }

    fun initView() {
        val vClock = clock;

    }
}