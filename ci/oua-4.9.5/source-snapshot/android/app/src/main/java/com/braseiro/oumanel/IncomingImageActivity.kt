package com.braseiro.oumanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receptor sem interface. Ele nunca hospeda a WebView e, portanto, não consegue
 * criar uma segunda mesa dentro da tarefa do Gemini.
 */
class IncomingImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receive(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    private fun receive(source: Intent) {
        lifecycleScope.launch {
            val error = try {
                withContext(Dispatchers.IO) { SharedImageInbox.storeFromIntent(this@IncomingImageActivity, source) }
                null
            } catch (failure: Exception) {
                Log.e("BRASEIRO_SHARE", "Falha ao copiar imagem para a caixa de entrada privada", failure)
                failure.message ?: "Não foi possível receber a imagem compartilhada."
            }

            val openTable = Intent(this@IncomingImageActivity, MainActivity::class.java).apply {
                action = SharedImageInbox.ACTION_IMPORT_SHARED_IMAGE
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                if (error != null) putExtra(SharedImageInbox.EXTRA_IMPORT_ERROR, error)
            }
            startActivity(openTable)
            finish()
        }
    }
}
