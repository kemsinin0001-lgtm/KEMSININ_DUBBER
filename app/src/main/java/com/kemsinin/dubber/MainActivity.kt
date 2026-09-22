package com.kemsinin.dubber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kemsinin.dubber.ui.DubberApp
import com.kemsinin.dubber.ui.theme.KemsininTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            KemsininTheme {
                DubberApp()
            }
        }
    }
}
