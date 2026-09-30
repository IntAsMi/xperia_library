package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DapMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DapViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val dapViewModel: DapViewModel = viewModel()
      val themeSetting by dapViewModel.themeSetting.collectAsStateWithLifecycle()

      MyApplicationTheme(themeSetting = themeSetting) {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          DapMainScreen(viewModel = dapViewModel)
        }
      }
    }
  }
}
