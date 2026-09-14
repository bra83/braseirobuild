package com.braseiro.talesvtt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.braseiro.talesvtt.data.SceneImageExchange
import com.braseiro.talesvtt.game.GameViewModel
import com.braseiro.talesvtt.navigation.AppNavGraph
import com.braseiro.talesvtt.ui.theme.TalesVttTheme

class MainActivity : ComponentActivity() {
  private val gameViewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    consumeSharedSceneImage(intent)
    setContent {
      TalesVttTheme {
        AppNavGraph(modifier = Modifier.fillMaxSize(), gameViewModel = gameViewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    consumeSharedSceneImage(intent)
  }

  private fun consumeSharedSceneImage(intent: Intent?) {
    if (intent?.action != SceneImageExchange.ACTION_CONSUME) return
    val token = intent.getStringExtra(SceneImageExchange.EXTRA_TOKEN).orEmpty()
    if (token.isNotBlank()) gameViewModel.consumeSharedSceneImage(token)
  }
}

@Preview(showBackground = true, widthDp = 415, heightDp = 915)
@Composable
fun AppPreview() {
  TalesVttTheme { AppNavGraph() }
}
