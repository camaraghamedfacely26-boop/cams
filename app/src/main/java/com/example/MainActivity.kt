package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainShopApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {

  private val shopViewModel: ShopViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        MainShopApp(viewModel = shopViewModel)
      }
    }
  }
}
