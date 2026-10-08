package braseiro.ose.app

import braseiro.ose.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Read-only projection of one canonical CampaignEnvelope for every UI surface. */
object AppViewStateProjector {
    fun project(envelope: CampaignEnvelope, selectedCharacterId: String? = null): JSONObject {
        val state = envelope.campaignState
        val game = state.game
        val party = state.party.characterIds.mapNotNull(game.characters::get)
        val selected = party.firstOrNull { it.characterId == selectedCharacterId } ?: party.firstOrNull()
        return JSONObject()
            .put("schemaVersion", 2)
            .put("campaign", JSONObject()
                .put("id", envelope.campaignId.value)
                .put("profile", envelope.ruleProfile.name)
                .put("authorityId", envelope.authorityRevision.authorityId)
                .put("authorityRevision", envelope.authorityRevision.revision)
                .put("stateRevision", game.revision)
                .put("mechanicalStateHash", CanonicalStateHash.sha256(envelope)))
            .put("time", JSONObject()
                .put("rounds", state.time.rounds)
                .put("turns", state.time.turns)
                .put("hours", state.time.hours)
                .put("days", state.time.days))
            .put("resources", JSONArray(state.resources.resourceFacts))
            .put("position", position(state.position.primary))
            .put("session", JSONObject()
                .put("phase", game.session.phase)
                .put("visibleNarration", game.session.visibleNarration)
                .put("visibleMechanicalFeedback", game.session.visibleMechanicalFeedback)
                .put("lastPlayerReaction", game.session.lastPlayerReaction)
                .put("pendingNarrationReactionId", game.session.pendingNarrationReactionId ?: JSONObject.NULL))
            .put("party", JSONArray(party.map(::characterSummary)))
            .put("selectedCharacterId", selected?.characterId ?: JSONObject.NULL)
            .put("selectedCharacter", selected?.let(::characterDetail) ?: JSONObject.NULL)
            .put("inventory", JSONArray(game.inventory.items.values
                .filter { it.ownerId in state.party.characterIds }
                .sortedBy { it.itemId }
                .map(::inventoryItem)))
            .put("lights", JSONArray(game.expedition.lightSources
                .filter { it.carrierId in state.party.characterIds }
                .sortedBy { it.lightId }
                .map(::light)))
            .put("creation", creation(game))
            .put("map", map(state))
            .put("localMaps", localMaps(state))
    }

    private fun creation(game: GameExtensions): JSONObject = JSONObject()
        .put("drafts", JSONArray(game.creationDrafts.values.sortedBy { it.draftId }.map { d ->
            JSONObject().put("draftId", d.draftId).put("profile", d.profile.name)
                .put("createdAtRevision", d.createdAtRevision).put("attributes", attrs(d.rolledAttributes))
        }))

    private fun characterSummary(c: CharacterSnapshot): JSONObject = JSONObject()
        .put("characterId", c.characterId).put("name", c.name).put("raceId", c.raceId)
        .put("classIds", JSONArray(c.classIds)).put("levelByClass", JSONObject(c.levelByClass))
        .put("hpCurrentNumerator", c.hitPoints.currentNumerator).put("hpMaxNumerator", c.hitPoints.maxNumerator)
        .put("hpDenominator", c.hitPoints.denominator).put("armorClassDescending", c.armorClassDescending)

    private fun characterDetail(c: CharacterSnapshot): JSONObject = characterSummary(c)
        .put("creationMethod", c.creationMethod).put("xpByClass", JSONObject(c.xpByClass))
        .put("attributes", attrs(c.attributes)).put("armorClassAscending", c.armorClassAscending)
        .put("thac0", c.thac0).put("attackBonusAscending", c.attackBonusAscending)
        .put("savingThrows", JSONObject()
            .put("deathPoison", c.savingThrows.deathPoison).put("wands", c.savingThrows.wands)
            .put("paralysisPetrification", c.savingThrows.paralysisPetrification)
            .put("breath", c.savingThrows.breath).put("spellsRodsStaves", c.savingThrows.spellsRodsStaves))
        .put("languages", JSONArray(c.languages)).put("flags", JSONArray(c.flags.toList()))
        .put("weaponProficiencies", JSONArray(c.weaponProficiencies.toList()))
        .put("weaponSpecialization", c.weaponSpecialization ?: JSONObject.NULL)
        .put("secondarySkills", JSONArray(c.secondarySkills))

    private fun attrs(a: Attributes): JSONObject = JSONObject()
        .put("str", a.str).put("int", a.int).put("wis", a.wis).put("dex", a.dex).put("con", a.con).put("cha", a.cha)

    private fun inventoryItem(i: InventoryItemSnapshot): JSONObject = JSONObject()
        .put("itemId", i.itemId).put("ownerId", i.ownerId).put("catalogId", i.catalogId)
        .put("kind", i.kind.name).put("quantity", i.quantity).put("carried", i.carried).put("equipped", i.equipped)

    private fun light(l: LightSourceSnapshot): JSONObject = JSONObject()
        .put("lightId", l.lightId).put("kind", l.kind.name).put("carrierId", l.carrierId)
        .put("remainingTurns", l.remainingTurns).put("lit", l.lit)

    private fun position(ref: SpatialRef): JSONObject = when (ref) {
        is SpatialRef.Hex -> JSONObject().put("kind","HEX").put("spatialEntityId",ref.spatialEntityId).put("q",ref.q).put("r",ref.r)
        is SpatialRef.Dungeon -> JSONObject().put("kind","DUNGEON").put("spatialEntityId",ref.spatialEntityId).put("nodeId",ref.nodeId)
        is SpatialRef.Settlement -> JSONObject().put("kind","SETTLEMENT").put("spatialEntityId",ref.spatialEntityId).put("anchorId",ref.anchorId)
        is SpatialRef.Scene -> JSONObject().put("kind","SCENE").put("spatialEntityId",ref.spatialEntityId).put("sceneId",ref.sceneId)
    }

    private fun localMaps(state: CampaignState): JSONObject {
        val game = state.game
        val out = JSONObject()
        game.settlement?.let { s ->
            val current = (state.position.primary as? SpatialRef.Settlement)
                ?.takeIf { it.spatialEntityId == s.settlementId }
                ?.anchorId
            out.put("settlement", JSONObject()
                .put("settlementId", s.settlementId)
                .put("currentAnchorId", current ?: JSONObject.NULL)
                .put("locations", JSONArray(s.locations.map { loc ->
                    JSONObject()
                        .put("locationId", loc.locationId)
                        .put("name", if (loc.discovered) loc.name else "Local desconhecido")
                        .put("discovered", loc.discovered)
                        .put("tags", JSONArray(if (loc.discovered) loc.tags.toList().sorted() else emptyList<String>()))
                })))
        }
        game.dungeon?.let { d ->
            val known = d.knownNodeIds
            val visible = d.visibleNodeIds
            val current = (state.position.primary as? SpatialRef.Dungeon)
                ?.takeIf { it.spatialEntityId == d.dungeonId }
                ?.nodeId
            val nodes = d.nodes.filter { it.nodeId in known }.map { node ->
                JSONObject()
                    .put("nodeId", node.nodeId)
                    .put("kind", node.kind)
                    .put("visible", node.nodeId in visible)
                    .put("current", node.nodeId == current)
            }
            val edges = d.edges
                .filter { it.from in known && it.to in known }
                .filter { !it.secret || (it.from in visible && it.to in visible) }
                .map { edge ->
                    JSONObject()
                        .put("from", edge.from)
                        .put("to", edge.to)
                        .put("kind", edge.kind)
                        .put("secret", edge.secret)
                }
            out.put("dungeon", JSONObject()
                .put("dungeonId", d.dungeonId)
                .put("seed", d.seed.toString())
                .put("entranceNodeId", d.entranceNodeId)
                .put("currentNodeId", current ?: JSONObject.NULL)
                .put("nodes", JSONArray(nodes))
                .put("edges", JSONArray(edges)))
        }
        return out
    }

    private fun map(state: CampaignState): Any {
        val world = state.game.hexWorld ?: return JSONObject.NULL
        val knownFacts = state.playerKnowledge.knownFactIds.toSet()
        val cells = world.cells.map { c ->
            val known = c.known || c.visible || "hex:${c.q},${c.r}" in knownFacts
            JSONObject().put("q",c.q).put("r",c.r).put("known",known).put("visible",c.visible)
                .put("terrain", if (known) c.terrain.name else "UNKNOWN")
                .put("hydrology", if (known) c.hydrology.name else "UNKNOWN")
                .put("road", known && c.road).put("trail", known && c.trail)
                .put("poiId", if (known) c.poiId ?: JSONObject.NULL else JSONObject.NULL)
        }
        return JSONObject().put("worldId",world.worldId).put("generatorVersion",world.generatorVersion)
            .put("geometryVersion",world.geometryVersion).put("width",world.width).put("height",world.height)
            .put("cells",JSONArray(cells))
    }
}
