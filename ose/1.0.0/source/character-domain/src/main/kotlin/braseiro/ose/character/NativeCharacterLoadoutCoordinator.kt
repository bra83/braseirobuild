package braseiro.ose.character

import braseiro.ose.model.*
import braseiro.ose.rules.shared.InventoryRuntimeRules
import braseiro.ose.rules.shared.SharedConfirmedRules
import braseiro.ose.rules.shared.StartingEquipmentCatalog
import braseiro.ose.rules.shared.StartingEquipmentEntry

/**
 * Source-bound starting loadout bridge. The UI submits only canonical IDs and intent.
 * Prices, quantities, weights, weapon dice and armour values are resolved from the active profile catalog.
 */
class NativeCharacterLoadoutCoordinator {
    data class MutationResult(val envelope: CampaignEnvelope, val changed: Boolean, val status: String, val detail: String = "")

    fun purchase(envelope: CampaignEnvelope, characterId: String, catalogId: String): MutationResult {
        val ctx = context(envelope, characterId) ?: return MutationResult(envelope,false,"INVALID_LOADOUT","Character/loadout not available")
        val (character, loadout) = ctx
        if (loadout.completed) return MutationResult(envelope,false,"LOADOUT_ALREADY_COMPLETE")
        val entry = StartingEquipmentCatalog.lookup(envelope.ruleProfile, catalogId)
            ?: return MutationResult(envelope,false,"CATALOG_ITEM_NOT_FOUND",catalogId)
        val price = entry.priceGp ?: return MutationResult(envelope,false,"MISSING_EVIDENCE_PRICE",catalogId)
        if (price > loadout.remainingWealthGp) return MutationResult(envelope,false,"INSUFFICIENT_GOLD","need=$price remaining=${loadout.remainingWealthGp}")

        val oldGame=envelope.campaignState.game
        val existing=oldGame.inventory.items.values.firstOrNull { it.ownerId==characterId && it.catalogId==catalogId }
        val itemId=existing?.itemId ?: "START:$characterId:$catalogId"
        val nextItem = if (existing != null) existing.copy(quantity=existing.quantity+entry.purchaseQuantity) else entry.toInventoryItem(itemId, characterId)
        val nextInventory=oldGame.inventory.copy(items=oldGame.inventory.items + (itemId to nextItem)).canonical()
        val nextLoadout=loadout.copy(
            remainingWealthGp=loadout.remainingWealthGp-price,
            purchases=loadout.purchases + (catalogId to ((loadout.purchases[catalogId] ?: 0)+1))
        ).canonical()
        val nextGame=withDerivedExpedition(envelope, oldGame.copy(
            inventory=nextInventory,
            loadouts=oldGame.loadouts + (characterId to nextLoadout),
            actionLog=oldGame.actionLog + ActionLogEntry("BUY:${oldGame.revision}:$catalogId","LOADOUT","Purchased $catalogId",listOf(entry.sourceId)),
            revision=oldGame.revision+1
        ))
        return MutationResult(envelope.copy(campaignState=envelope.campaignState.copy(game=nextGame)),true,"PURCHASED","remaining=${nextLoadout.remainingWealthGp}")
    }

    fun setEquipped(envelope: CampaignEnvelope, characterId: String, itemId: String, equipped: Boolean): MutationResult {
        val ctx=context(envelope,characterId) ?: return MutationResult(envelope,false,"INVALID_LOADOUT")
        val (character, loadout)=ctx
        if (loadout.completed) return MutationResult(envelope,false,"LOADOUT_ALREADY_COMPLETE")
        val oldGame=envelope.campaignState.game
        val item=oldGame.inventory.items[itemId]?.takeIf { it.ownerId==characterId }
            ?: return MutationResult(envelope,false,"ITEM_NOT_OWNED",itemId)
        val entry=StartingEquipmentCatalog.lookup(envelope.ruleProfile,item.catalogId)
            ?: return MutationResult(envelope,false,"CATALOG_ITEM_NOT_FOUND",item.catalogId)
        if (entry.kind !in setOf(InventoryItemKind.WEAPON,InventoryItemKind.ARMOR,InventoryItemKind.SHIELD))
            return MutationResult(envelope,false,"ITEM_NOT_EQUIPPABLE",item.catalogId)

        val permission=equipmentPermission(envelope.ruleProfile,character,entry)
        if (!permission.permitted) return MutationResult(envelope,false,"EQUIPMENT_RESTRICTED",permission.note)

        val items=oldGame.inventory.items.toMutableMap()
        if (equipped) {
            when(entry.kind) {
                InventoryItemKind.WEAPON -> {
                    items.replaceAll { _,v -> if(v.ownerId==characterId && v.kind==InventoryItemKind.WEAPON) v.copy(equipped=false) else v }
                    if(entry.twoHandedOrLarge) items.replaceAll { _,v -> if(v.ownerId==characterId && v.kind==InventoryItemKind.SHIELD) v.copy(equipped=false) else v }
                }
                InventoryItemKind.ARMOR -> items.replaceAll { _,v -> if(v.ownerId==characterId && v.kind==InventoryItemKind.ARMOR) v.copy(equipped=false) else v }
                InventoryItemKind.SHIELD -> {
                    val twoHandedEquipped=items.values.firstOrNull { v -> v.ownerId==characterId && v.kind==InventoryItemKind.WEAPON && v.equipped }
                        ?.let { StartingEquipmentCatalog.lookup(envelope.ruleProfile,it.catalogId)?.twoHandedOrLarge == true } == true
                    if(twoHandedEquipped) return MutationResult(envelope,false,"TWO_HANDED_WEAPON_WITH_SHIELD")
                }
                else -> Unit
            }
        }
        items[itemId]=item.copy(equipped=equipped)
        val nextInventory=oldGame.inventory.copy(items=items).canonical()
        val nextCharacter=recalculateArmorClass(character,nextInventory,envelope.ruleProfile)
        val judgementRefs=if(permission.judgment) listOf("REFEREE_JUDGMENT:${entry.catalogId}") else listOf(entry.sourceId)
        var nextGame=oldGame.copy(
            inventory=nextInventory,
            characters=oldGame.characters + (characterId to nextCharacter),
            actionLog=oldGame.actionLog + ActionLogEntry("EQUIP:${oldGame.revision}:$itemId","LOADOUT",if(equipped) "Equipped ${entry.catalogId}" else "Unequipped ${entry.catalogId}",judgementRefs),
            revision=oldGame.revision+1
        )
        nextGame=withDerivedExpedition(envelope,nextGame)
        return MutationResult(envelope.copy(campaignState=envelope.campaignState.copy(game=nextGame)),true,if(permission.judgment) "EQUIPPED_REFEREE_JUDGMENT" else "EQUIPPED",permission.note)
    }

    fun finish(envelope: CampaignEnvelope, characterId: String): MutationResult {
        val ctx=context(envelope,characterId) ?: return MutationResult(envelope,false,"INVALID_LOADOUT")
        val (_, loadout)=ctx
        if(loadout.completed) return MutationResult(envelope,false,"ALREADY_ACTIVE")
        val old=envelope.campaignState.game
        val done=loadout.copy(completed=true).canonical()
        val coinId="START:$characterId:STARTING_GOLD_GP"
        val coinItems=if(done.remainingWealthGp>0 && old.inventory.items[coinId]==null) {
            old.inventory.items + (coinId to InventoryItemSnapshot(
                itemId=coinId,
                ownerId=characterId,
                catalogId="STARTING_GOLD_GP",
                profile=envelope.ruleProfile,
                sourceId=loadout.sourceRuleId,
                kind=InventoryItemKind.COIN,
                quantity=done.remainingWealthGp,
                carried=true,
                unitWeightCoins=1
            ))
        } else old.inventory.items
        val preAdventure = envelope.campaignState.time.turns == 0L &&
            old.session.visibleNarration.isBlank() &&
            old.session.phase in setOf("CHARACTER_LOADOUT", "READY_TO_START")
        val otherPending = old.loadouts.any { (id, entry) ->
            id != characterId && id in envelope.campaignState.party.characterIds && !entry.completed
        }
        val nextSession = if (preAdventure) old.session.copy(
            phase = if (otherPending) "CHARACTER_LOADOUT" else "READY_TO_START",
            visibleMechanicalFeedback = "Equipamento inicial confirmado."
        ) else old.session
        var game=old.copy(
            inventory=old.inventory.copy(items=coinItems).canonical(),
            loadouts=old.loadouts + (characterId to done),
            session=nextSession,
            actionLog=old.actionLog + ActionLogEntry("LOADOUT_DONE:${old.revision}:$characterId","LOADOUT","Starting loadout completed",listOf(loadout.sourceRuleId)),
            revision=old.revision+1
        )
        game=withDerivedExpedition(envelope,game)
        val state=envelope.campaignState.copy(game=game,
            resources=if (preAdventure)
                deriveResources(envelope.ruleProfile,game,envelope.campaignState.party.characterIds)
                else envelope.campaignState.resources)
        return MutationResult(envelope.copy(campaignState=state),true,
            if (preAdventure && !otherPending) "READY_TO_START" else "LOADOUT_COMPLETE")
    }

    private fun context(envelope:CampaignEnvelope, characterId:String): Pair<CharacterSnapshot,CharacterLoadoutSnapshot>? {
        // Individual loadout progress is independent of the global narrative phase.
        val c=envelope.campaignState.game.characters[characterId] ?: return null
        val l=envelope.campaignState.game.loadouts[characterId] ?: return null
        return c to l
    }

    private data class Permission(val permitted:Boolean,val judgment:Boolean=false,val note:String="")
    private fun equipmentPermission(profile:RuleProfile,c:CharacterSnapshot,e:StartingEquipmentEntry):Permission {
        if(e.kind !in setOf(InventoryItemKind.WEAPON,InventoryItemKind.ARMOR,InventoryItemKind.SHIELD)) return Permission(true)
        val classes=c.classIds.toSet()
        if(profile==RuleProfile.OSE_CLASSIC_FANTASY) {
            val cl=classes.single()
            return when(e.kind) {
                InventoryItemKind.WEAPON -> when(cl) {
                    "CLERIC" -> Permission(e.blunt,false,"Classic cleric: blunt weapons only")
                    "MAGIC_USER" -> Permission(e.displayName.contains("Adaga",true),false,"Classic magic-user: dagger restriction")
                    "DWARF","HALFLING" -> if(e.twoHandedOrLarge) Permission(true,true,"Weapon size classification requires referee judgment") else Permission(true)
                    else -> Permission(true)
                }
                InventoryItemKind.ARMOR -> when(cl) { "MAGIC_USER"->Permission(false); "THIEF"->Permission(e.armorLoadCategory==ArmorLoadCategory.LIGHT); else->Permission(true) }
                InventoryItemKind.SHIELD -> Permission(cl !in setOf("MAGIC_USER","THIEF"))
                else -> Permission(true)
            }
        }
        if("FIGHTER" in classes) return Permission(true)
        if("CLERIC" in classes && e.kind==InventoryItemKind.WEAPON) return Permission(e.blunt,false,"Advanced cleric: blunt weapons only")
        if("BARBARIAN" in classes) {
            return when(e.kind) {
                InventoryItemKind.ARMOR -> Permission(e.armorLoadCategory in setOf(ArmorLoadCategory.LIGHT,ArmorLoadCategory.HEAVY),false,"Barbarian: leather/chain-class armour")
                else -> Permission(true)
            }
        }
        // Other Advanced class/race combinations have source-specific equipment clauses. Do not invent a global fallback.
        return Permission(true,true,"Advanced class/race equipment clause requires source-bound referee adjudication")
    }

    private fun recalculateArmorClass(c:CharacterSnapshot,inventory:InventorySnapshot,profile:RuleProfile):CharacterSnapshot {
        val equipped=inventory.items.values.filter { it.ownerId==c.characterId && it.equipped }
        val armour=equipped.firstOrNull { it.kind==InventoryItemKind.ARMOR }?.let { StartingEquipmentCatalog.lookup(profile,it.catalogId) }
        val shield=equipped.any { it.kind==InventoryItemKind.SHIELD }
        val descBase=armour?.armorClassDescending ?: 9
        val ascBase=armour?.armorClassAscending ?: 10
        val desc=SharedConfirmedRules.descendingArmorClass(descBase,c.attributes.dex) - if(shield) 1 else 0
        val asc=SharedConfirmedRules.ascendingArmorClass(ascBase,c.attributes.dex) + if(shield) 1 else 0
        return c.copy(armorClassDescending=desc,armorClassAscending=asc).canonical()
    }

    private fun withDerivedExpedition(envelope:CampaignEnvelope,game:GameExtensions):GameExtensions {
        val d=InventoryRuntimeRules.derivePartyExpedition(envelope.ruleProfile,envelope.optionSet,envelope.campaignState.party.characterIds,game.inventory,game.expedition)
        return game.copy(expedition=d.expedition).canonical()
    }

    private fun deriveResources(profile:RuleProfile,game:GameExtensions,partyIds:List<String>):ResourceState {
        val owned=game.inventory.items.values.filter { it.ownerId in partyIds && it.quantity>0 }
        val torches=owned.filter { it.lightSourceKind==LightSourceKind.TORCH }.sumOf { it.quantity }
        val rations=owned.filter { it.kind==InventoryItemKind.FOOD }.sumOf { it.quantity }
        val gold=owned.filter { it.kind==InventoryItemKind.COIN }.sumOf { it.quantity }
        return ResourceState(listOf("gold:$gold","torch:$torches","rations:$rations","movement:${game.expedition.baseMoveMeters}")).canonical()
    }

    private fun StartingEquipmentEntry.toInventoryItem(itemId:String,ownerId:String)=InventoryItemSnapshot(
        itemId=itemId,ownerId=ownerId,catalogId=catalogId,profile=profile,sourceId=sourceId,kind=kind,
        quantity=purchaseQuantity,carried=true,equipped=false,unitWeightCoins=unitWeightCoins,
        armorLoadCategory=armorLoadCategory,lightSourceKind=lightSourceKind,weapon=weapon
    )
}
