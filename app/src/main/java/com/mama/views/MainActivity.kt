package com.mama.views

import android.content.Intent
import android.os.Bundle
import com.mama.views.base.BaseActivity
import com.mama.views.views.OvalPathActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        sample_text.text = stringFromJNI() + TAG
        sample_text.setOnClickListener { v ->
            when(v.id) {
                R.id.sample_text -> {
                    startActivity(Intent(this, OvalPathActivity::class.java))
//                    startActivity(Intent(this, ClockActivity::class.java))
                }
//                else
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        private val TAG = "MainActivity"

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
