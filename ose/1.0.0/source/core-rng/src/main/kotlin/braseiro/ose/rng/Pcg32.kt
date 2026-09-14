package braseiro.ose.rng

import kotlinx.serialization.Serializable

const val RNG_VERSION = "RNG_PCG32_V1"
@Serializable enum class RngStreamId { RULES_DICE, DUNGEON_GEN, HEX_WORLD_GEN, ENCOUNTERS, WORLD_EVENTS, CONTENT_TABLES }
@Serializable data class Pcg32State(val state: ULong, val increment: ULong, val counter: Long = 0, val version: String = RNG_VERSION)
data class RngDraw(val value: UInt, val next: Pcg32State)

object Pcg32 {
    private const val MULTIPLIER: ULong = 6364136223846793005uL
    fun seeded(initState: ULong, initSequence: ULong): Pcg32State {
        var s = Pcg32State(0u, (initSequence shl 1) or 1u, 0)
        s = next(s).next.copy(counter = 0)
        s = s.copy(state = s.state + initState)
        s = next(s).next.copy(counter = 0)
        return s
    }
    fun next(current: Pcg32State): RngDraw {
        require(current.version == RNG_VERSION)
        val old = current.state
        val newState = old * MULTIPLIER + current.increment
        val xorshifted = (((old shr 18) xor old) shr 27).toUInt()
        val rot = (old shr 59).toInt()
        val result = (xorshifted shr rot) or (xorshifted shl ((-rot) and 31))
        return RngDraw(result, current.copy(state = newState, counter = current.counter + 1))
    }
}

@Serializable data class SeedState(val rootSeed: ULong, val streams: Map<RngStreamId, Pcg32State>)

class NamedRngStreams private constructor(private var seedState: SeedState) {
    companion object {
        fun fromSeedState(seedState: SeedState): NamedRngStreams = NamedRngStreams(seedState)
        fun fromRootSeed(rootSeed: ULong): NamedRngStreams {
            val streams = RngStreamId.entries.associateWith { id ->
                val sequence = splitMix64(rootSeed + id.ordinal.toULong() + 1u)
                val init = splitMix64(rootSeed xor (0x9E3779B97F4A7C15uL + id.ordinal.toULong()))
                Pcg32.seeded(init, sequence)
            }
            return NamedRngStreams(SeedState(rootSeed, streams))
        }
        private fun splitMix64(input: ULong): ULong {
            var z = input + 0x9E3779B97F4A7C15uL
            z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
            z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
            return z xor (z shr 31)
        }
    }
    fun draw(id: RngStreamId): UInt {
        val draw = Pcg32.next(seedState.streams.getValue(id))
        seedState = seedState.copy(streams = seedState.streams + (id to draw.next))
        return draw.value
    }
    fun snapshot(): SeedState = seedState
}
