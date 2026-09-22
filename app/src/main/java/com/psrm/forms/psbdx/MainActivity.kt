package com.psrm.forms.psbdx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.psrm.forms.psbdx.ui.navigation.PsrmNavHost
import com.psrm.forms.psbdx.ui.theme.PsrmFormsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PsrmApplication

        setContent {
            PsrmFormsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PsrmNavHost(app = app)
                }
            }
        }
    }
}
