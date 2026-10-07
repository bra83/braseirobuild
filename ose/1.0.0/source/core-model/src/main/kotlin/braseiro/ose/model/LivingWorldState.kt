package braseiro.ose.model

import kotlinx.serialization.Serializable

/**
 * Persistent VTT infrastructure state. It is not an OSE rules modifier by itself:
 * weather and NPC background pulses become mechanical only if an explicit rules resolver
 * consumes them. This keeps the living world active without inventing hidden OSE bonuses.
 */
@Serializable
enum class WeatherKind { CLEAR, OVERCAST, RAIN, FOG, STORM, COLD, HOT }

@Serializable
data class WeatherSnapshot(
    val kind: WeatherKind = WeatherKind.CLEAR,
    val changedAtDay: Long = 0,
    val sequence: Long = 0
)

@Serializable
data class LivingWorldSnapshot(
    val weather: WeatherSnapshot = WeatherSnapshot(),
    val lastProcessedTurn: Long = 0,
    val pulseCount: Long = 0
)
