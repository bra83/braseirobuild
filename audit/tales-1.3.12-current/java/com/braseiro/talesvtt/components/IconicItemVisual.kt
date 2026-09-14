package com.braseiro.talesvtt.components

import com.braseiro.talesvtt.R

object IconicItemVisual {
  fun drawable(name: String): Int {
    val value = name.lowercase()
    return when {
      value.contains("caderno") || value.contains("notebook") || value.contains("mapa") || value.contains("anota") -> R.drawable.item_notebook
      value.contains("fita") || value.contains("cassete") -> R.drawable.item_audio_cassette
      value.contains("lanterna") -> R.drawable.item_flashlight
      value.contains("binócul") || value.contains("binocul") -> R.drawable.item_binoculars
      value.contains("câmera") || value.contains("camera") || value.contains("polaroid") -> R.drawable.item_instant_camera
      value.contains("chave inglesa") -> R.drawable.iconic_old_wrench
      value.contains("chave") -> R.drawable.item_brass_key
      value.contains("ferrament") || value.contains("kit") -> R.drawable.item_toolkit
      else -> R.drawable.item_iconic
    }
  }
}
