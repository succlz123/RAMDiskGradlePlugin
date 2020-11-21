package org.succlz123.ramdisk

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import org.succlz123.shrink.app.R

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (findViewById<View>(R.id.content) as TextView).text = "123"
    }
}