package braseiro.ose.rules.classic

import braseiro.ose.model.*
import braseiro.ose.rules.api.*
import braseiro.ose.rules.shared.SharedConfirmedRules

private fun ev(id:String, page:String)=RuleEvidence(id, RuleProfile.OSE_CLASSIC_FANTASY, "Old-School Essentials Classic Fantasy — Tomo de Regras", page)

object ClassicCatalog {
    val classes: Map<String, ClassLevelOneDescriptor> = listOf(
        ClassLevelOneDescriptor("DWARF",8,19,0,SavingThrows(8,9,10,13,12),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9),"DWARF",ev("CLASSIC.CLASS.DWARF","pp.22-23")),
        ClassLevelOneDescriptor("CLERIC",6,19,0,SavingThrows(11,12,14,16,15),setOf(AttributeKey.WIS),emptyMap(),"HUMAN",ev("CLASSIC.CLASS.CLERIC","pp.24-25")),
        ClassLevelOneDescriptor("ELF",6,19,0,SavingThrows(12,13,13,15,15),setOf(AttributeKey.INT,AttributeKey.STR),mapOf(AttributeKey.INT to 9),"ELF",ev("CLASSIC.CLASS.ELF","pp.26-27")),
        ClassLevelOneDescriptor("FIGHTER",8,19,0,SavingThrows(12,13,14,15,16),setOf(AttributeKey.STR),emptyMap(),"HUMAN",ev("CLASSIC.CLASS.FIGHTER","pp.28-29")),
        ClassLevelOneDescriptor("HALFLING",6,19,0,SavingThrows(8,9,10,13,12),setOf(AttributeKey.DEX,AttributeKey.STR),mapOf(AttributeKey.CON to 9, AttributeKey.DEX to 9),"HALFLING",ev("CLASSIC.CLASS.HALFLING","pp.30-31")),
        ClassLevelOneDescriptor("THIEF",4,19,0,SavingThrows(13,14,13,16,15),setOf(AttributeKey.DEX),emptyMap(),"HUMAN",ev("CLASSIC.CLASS.THIEF","pp.32-33")),
        ClassLevelOneDescriptor("MAGIC_USER",4,19,0,SavingThrows(13,14,13,16,15),setOf(AttributeKey.INT),emptyMap(),"HUMAN",ev("CLASSIC.CLASS.MAGIC_USER","pp.34-35"))
    ).associateBy { it.classId }
}

class ClassicCharacterRules : CharacterRulesProvider {
    override val profile = RuleProfile.OSE_CLASSIC_FANTASY
    private val creationEvidence = ev("CLASSIC_CHARACTER_CREATION","pp.14-15")

    override fun create(request: CharacterCreationRequest): CharacterCreationResult {
        if (request.profile != profile || request.method != CreationMethod.CLASSIC) return reject(CreationFailureCode.PROFILE_METHOD_MISMATCH,"Classic provider accepts CLASSIC only")
        if (request.classIds.size != 1) return reject(CreationFailureCode.RACE_CLASS_ILLEGAL,"Classic creation chooses exactly one class")
        val clazz=ClassicCatalog.classes[request.classIds.single()] ?: return reject(CreationFailureCode.UNKNOWN_CLASS,request.classIds.single())
        val adjusted=applyAdjustments(request.rolledAttributes,request.adjustments,clazz.primary) ?: return reject(CreationFailureCode.ATTRIBUTE_ADJUSTMENT_ILLEGAL,"-2/+1 adjustment must reduce STR/INT/WIS, keep source >=9, and increase a prime requisite")
        val failed=clazz.minimums.entries.firstOrNull { adjusted.get(it.key) < it.value }
        if(failed!=null) return reject(CreationFailureCode.CLASS_PREREQUISITE,"${failed.key} must be >= ${failed.value}",clazz.evidence)
        if(request.hpRolls.size!=1 || request.hpRolls[0] !in 1..clazz.hitDie) return reject(CreationFailureCode.HP_ROLL_INVALID,"Expected one 1d${clazz.hitDie} roll")
        val hp=maxOf(1,request.hpRolls[0]+SharedConfirmedRules.attributeModifier(adjusted.con))
        val dex=adjusted.dex
        val c=CharacterSnapshot(
            characterId=request.characterId,name=request.name,creationMethod=CreationMethod.CLASSIC.name,raceId=clazz.basicRaceId,
            classIds=listOf(clazz.classId),levelByClass=mapOf(clazz.classId to 1),xpByClass=mapOf(clazz.classId to 0L),attributes=adjusted,
            hitPoints=HitPoints(hp,hp),armorClassDescending=SharedConfirmedRules.descendingArmorClass(dexterity=dex),armorClassAscending=SharedConfirmedRules.ascendingArmorClass(dexterity=dex),
            thac0=clazz.thac0,attackBonusAscending=clazz.attackBonusAscending,savingThrows=clazz.saves,
            evidenceRefs=listOf(creationEvidence.ref(),clazz.evidence.ref(),"ATTR.STR","ATTR.INT","ATTR.WIS","ATTR.DEX","ATTR.CON","ATTR.CHA"),
            primeModifierPercentByClass=classicPrimeModifier(clazz,adjusted)?.let { mapOf(clazz.classId to it) } ?: emptyMap()
        ).canonical()
        return CharacterCreationResult.Created(c, RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,listOf(creationEvidence,clazz.evidence)))
    }

    private fun classicPrimeModifier(clazz: ClassLevelOneDescriptor, a: Attributes): Int? = when(clazz.classId) {
        "ELF" -> when {
            a.int >= 16 && a.str >= 13 -> 10
            a.int >= 13 && a.str >= 13 -> 5
            else -> null // Combination below the explicit Elf bonus thresholds remains evidence-bound, never implicit 0.
        }
        "HALFLING" -> when {
            a.dex >= 13 && a.str >= 13 -> 10
            a.dex >= 13 || a.str >= 13 -> 5
            else -> null
        }
        else -> clazz.primary.singleOrNull()?.let { SharedConfirmedRules.singlePrimeXpModifier(a.get(it)) }
    }

    private fun applyAdjustments(base: Attributes, adjustments: List<AttributeAdjustment>, primaries: Set<AttributeKey>): Attributes? {
        var a=base
        for (x in adjustments) {
            if(x.decrease !in setOf(AttributeKey.STR,AttributeKey.INT,AttributeKey.WIS) || x.increase !in primaries) return null
            val down=a.get(x.decrease)-2
            if(down<9) return null
            val up=a.get(x.increase)+1
            if(up>18) return null
            a=a.with(x.decrease,down).with(x.increase,up)
        }
        return a
    }
    private fun reject(code: CreationFailureCode, detail:String, vararg evidence: RuleEvidence)=CharacterCreationResult.Rejected(code,detail,RuleTrace(if(code==CreationFailureCode.MISSING_EVIDENCE) EvidenceStatus.MISSING_EVIDENCE else EvidenceStatus.CANONICAL_PROCEDURE,listOf(creationEvidence)+evidence))
}
