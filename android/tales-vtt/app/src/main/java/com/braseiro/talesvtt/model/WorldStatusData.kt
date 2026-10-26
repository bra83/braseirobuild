package com.braseiro.talesvtt.model

data class WorldStatusData(
  val seasonYear: String = "Primavera, 1986",
  val weather: String = "Tarde Nublada",
  val time: String = "17:42",
  val weatherCondition: String = "overcast"
)

data class ContextItemData(
  val id: String,
  val label: String,
  val value: String? = null,
  val iconType: ContextIconType = ContextIconType.LOCATION,
  val isHighlighted: Boolean = false,
  val badgeColor: String? = null
)

enum class ContextIconType {
  ILLUSTRATION,
  CHARACTERS,
  LOCATION,
  SCENE_TONE,
  MYSTERY,
  DAYLIGHT,
  AUDIO_TTS
}
