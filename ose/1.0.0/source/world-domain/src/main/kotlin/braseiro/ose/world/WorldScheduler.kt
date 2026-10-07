package braseiro.ose.world

import braseiro.ose.model.*

/**
 * Automatic causal world scheduler.
 *
 * It runs whenever canonical game time advances through SessionEngine. Background
 * pulses and weather are VTT infrastructure: they persist and can inform narration,
 * but do not silently alter OSE attack, save, movement, encounter or resource rules.
 */
object WorldEventScheduler {
    private const val PULSE_TURNS = 36L
    private const val DAY_TURNS = 144L

    fun advance(game:GameExtensions,fromTurn:Long,toTurn:Long):GameExtensions {
        require(toTurn>=fromTurn)
        if(toTurn==fromTurn)return game

        var npcs=game.npcs
        val events=game.worldEvents.map { event ->
            if(!event.resolved && event.dueTurn in (fromTurn+1)..toTurn){
                val npc=npcs[event.targetId]
                if(npc!=null){
                    npcs=npcs+(npc.npcId to npc.copy(
                        reactionHistory=(npc.reactionHistory+"WORLD_EVENT:${event.eventId}:${event.kind}").takeLast(80),
                        memorySummaries=(npc.memorySummaries+event.payload).takeLast(20),
                        consequenceFlags=npc.consequenceFlags+"EVENT_${event.kind}"
                    ))
                }
                event.copy(resolved=true)
            } else event
        }

        var living=game.livingWorld
        val firstPulse=fromTurn/PULSE_TURNS+1
        val lastPulse=toTurn/PULSE_TURNS
        if(lastPulse>=firstPulse){
            for(pulse in firstPulse..lastPulse){
                npcs=advanceNpcBackground(npcs,pulse)
                living=living.copy(
                    lastProcessedTurn=pulse*PULSE_TURNS,
                    pulseCount=living.pulseCount+1
                )
            }
        }

        val firstDay=fromTurn/DAY_TURNS+1
        val lastDay=toTurn/DAY_TURNS
        if(lastDay>=firstDay){
            var weather=living.weather
            for(day in firstDay..lastDay) weather=nextWeather(game.rngRootSeed,day,weather)
            living=living.copy(weather=weather,lastProcessedTurn=maxOf(living.lastProcessedTurn,toTurn))
        } else {
            living=living.copy(lastProcessedTurn=maxOf(living.lastProcessedTurn,toTurn))
        }

        return game.copy(
            npcs=npcs,
            worldEvents=events,
            livingWorld=living,
            revision=game.revision+1
        ).canonical()
    }

    private fun advanceNpcBackground(npcs:Map<String,NpcSnapshot>,pulse:Long):Map<String,NpcSnapshot> {
        val day=(pulse*PULSE_TURNS)/DAY_TURNS+1
        val phase=when((pulse%4).toInt()){
            0->"NOITE"
            1->"MANHA"
            2->"TARDE"
            else->"ENTARDECER"
        }
        return npcs.mapValues { (_,npc) ->
            val routineFlag="ROUTINE_$phase"
            npc.copy(
                reactionHistory=(npc.reactionHistory+"WORLD_PULSE:$pulse:$routineFlag").takeLast(80),
                memorySummaries=(npc.memorySummaries+"Rotina autônoma: $phase do dia $day.").takeLast(20),
                consequenceFlags=(npc.consequenceFlags.filterNot{it.startsWith("ROUTINE_")}+routineFlag+"LIVING_WORLD_ACTIVE").toSet()
            )
        }
    }

    private fun nextWeather(seed:String,day:Long,current:WeatherSnapshot):WeatherSnapshot {
        val all=WeatherKind.entries
        if(all.size<=1)return current
        val hash=stableHash("$seed|weather|$day|${current.sequence}")
        var next=all[(hash%all.size.toLong()).toInt()]
        if(next==current.kind) next=all[(next.ordinal+1)%all.size]
        return WeatherSnapshot(next,day,current.sequence+1)
    }

    private fun stableHash(value:String):Long {
        var h=1125899906842597L
        for(ch in value) h=31L*h+ch.code.toLong()
        return h and Long.MAX_VALUE
    }
}
