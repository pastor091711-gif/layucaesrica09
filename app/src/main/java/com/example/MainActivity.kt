package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StreamRewardsApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StreamRewardsViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Programmatic Firebase Setup using User Config
    try {
      val options = FirebaseOptions.Builder()
        .setApiKey("AIzaSyC0hztlBePxNx_lMLMKLJx_MakxkdMvlQg")
        .setApplicationId("1:798858306217:web:7798548286d37df833ffb8")
        .setDatabaseUrl("https://jose-83986-default-rtdb.firebaseio.com")
        .setProjectId("jose-83986")
        .setStorageBucket("jose-83986.firebasestorage.app")
        .build()

      FirebaseApp.initializeApp(this, options)
      Log.d("StreamRewards", "Firebase initialized successfully in StreamRewards Pro.")
    } catch (e: Exception) {
      Log.e("StreamRewards", "Programmatic Firebase init warning: ${e.message}")
    }

    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val mainViewModel: StreamRewardsViewModel = viewModel()
          StreamRewardsApp(mainViewModel)
        }
      }
    }
  }
}
