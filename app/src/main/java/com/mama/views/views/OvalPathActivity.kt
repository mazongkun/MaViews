package com.mama.views.views

import android.os.Bundle
import com.mama.views.R
import com.mama.views.base.BaseActivity
import kotlinx.android.synthetic.main.activity_oval_path.*

/**
 * Created on: 2020-06-19 16:23
 * Author: mazongkun
 */
class OvalPathActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oval_path)

        initView()
    }

    private fun initView() {
        val vOvalPath = oval_path
    }
}