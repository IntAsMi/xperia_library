package com.example

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
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
      val isPortraitLocked by dapViewModel.lockPortraitFlow.collectAsStateWithLifecycle()

      // Handle orientation lock dynamically
      LaunchedEffect(isPortraitLocked) {
        requestedOrientation = if (isPortraitLocked) {
          ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
          ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
      }

      // Android 13+ notification permission for media notification
      val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) { /* Result handled */ }

      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      MyApplicationTheme(themeSetting = themeSetting) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF0A0C12)
        ) {
          DapMainScreen(viewModel = dapViewModel)
        }
      }
    }
  }
}
