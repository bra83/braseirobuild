package braseiro.ose.rules.advanced

import braseiro.ose.model.*
import braseiro.ose.rules.api.*
import braseiro.ose.rules.shared.SharedConfirmedRules

private fun ev(id:String, page:String)=RuleEvidence(id, RuleProfile.OSE_ADVANCED_FANTASY, "Old-School Essentials Advanced Fantasy — Player's Tome (interim canonical corpus)", page)
private fun c(id:String,hd:Int,s:SavingThrows,primary:Set<AttributeKey>,mins:Map<AttributeKey,Int>,race:String,page:String)=
    ClassLevelOneDescriptor(id,hd,19,0,s,primary,mins,race,ev("ADV.BASIC.CLASS.$id",page))

object AdvancedCatalog {
    val basicClasses: Map<String,ClassLevelOneDescriptor> = listOf(
        c("ACROBAT",4,SavingThrows(13,14,13,16,15),setOf(AttributeKey.DEX),emptyMap(),"HUMAN","pp.28-29"),
        c("ASSASSIN",4,SavingThrows(13,14,13,16,15),setOf(AttributeKey.DEX),emptyMap(),"HUMAN","pp.30-31"),
        c("BARBARIAN",8,SavingThrows(10,13,12,15,16),setOf(AttributeKey.CON,AttributeKey.STR),mapOf(AttributeKey.DEX to 9),"HUMAN","pp.32-33"),
        c("BARD",6,SavingThrows(13,14,13,16,15),setOf(AttributeKey.CHA),mapOf(AttributeKey.DEX to 9,AttributeKey.INT to 9),"HUMAN","pp.34-35"),
        c("CLERIC",6,SavingThrows(11,12,14,16,15),setOf(AttributeKey.WIS),emptyMap(),"HUMAN","pp.36-37"),
        c("DROW",6,SavingThrows(12,13,13,15,12),setOf(AttributeKey.STR,AttributeKey.WIS),mapOf(AttributeKey.INT to 9),"DROW","pp.38-39"),
        c("DRUID",6,SavingThrows(11,12,14,16,15),setOf(AttributeKey.WIS),emptyMap(),"HUMAN","pp.40-43"),
        c("DUERGAR",6,SavingThrows(8,9,10,13,12),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9,AttributeKey.INT to 9),"DUERGAR","pp.44-45"),
        c("DWARF",8,SavingThrows(8,9,10,13,12),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9),"DWARF","pp.46-47"),
        c("ELF",6,SavingThrows(12,13,13,15,15),setOf(AttributeKey.INT,AttributeKey.STR),mapOf(AttributeKey.INT to 9),"ELF","pp.48-49"),
        c("FIGHTER",8,SavingThrows(12,13,14,15,16),setOf(AttributeKey.STR),emptyMap(),"HUMAN","pp.50-51"),
        c("GNOME",4,SavingThrows(8,9,10,14,11),setOf(AttributeKey.DEX,AttributeKey.INT),mapOf(AttributeKey.CON to 9),"GNOME","pp.52-53"),
        c("HALF_ELF",6,SavingThrows(12,13,13,15,15),setOf(AttributeKey.INT,AttributeKey.STR),mapOf(AttributeKey.CHA to 9,AttributeKey.CON to 9),"HALF_ELF","pp.54-55"),
        c("HALFLING",6,SavingThrows(8,9,10,13,12),setOf(AttributeKey.DEX,AttributeKey.STR),mapOf(AttributeKey.CON to 9,AttributeKey.DEX to 9),"HALFLING","pp.56-57"),
        c("HALF_ORC",6,SavingThrows(13,14,13,16,15),setOf(AttributeKey.DEX,AttributeKey.STR),emptyMap(),"HALF_ORC","pp.60-61"),
        c("ILLUSIONIST",4,SavingThrows(13,14,13,16,15),setOf(AttributeKey.INT),mapOf(AttributeKey.DEX to 9),"HUMAN","pp.62-63"),
        c("KNIGHT",8,SavingThrows(12,13,14,15,16),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9,AttributeKey.DEX to 9),"HUMAN","pp.64-65"),
        c("MAGIC_USER",4,SavingThrows(13,14,13,16,15),setOf(AttributeKey.INT),emptyMap(),"HUMAN","pp.66-67"),
        c("PALADIN",8,SavingThrows(10,11,12,13,14),setOf(AttributeKey.STR,AttributeKey.WIS),mapOf(AttributeKey.CHA to 9),"HUMAN","pp.68-69"),
        c("RANGER",8,SavingThrows(12,13,14,15,16),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9,AttributeKey.WIS to 9),"HUMAN","pp.70-71"),
        c("SVIRFNEBLIN",6,SavingThrows(8,9,10,14,11),setOf(AttributeKey.STR),mapOf(AttributeKey.CON to 9),"SVIRFNEBLIN","pp.72-73"),
        c("THIEF",4,SavingThrows(13,14,13,16,15),setOf(AttributeKey.DEX),emptyMap(),"HUMAN","pp.74-75")
    ).associateBy { it.classId }

    val professionalClassIds = setOf("ACROBAT","ASSASSIN","BARBARIAN","BARD","CLERIC","DRUID","FIGHTER","ILLUSIONIST","KNIGHT","MAGIC_USER","PALADIN","RANGER","THIEF")

    private fun race(id:String, mins:Map<AttributeKey,Int>, mods:Map<AttributeKey,Int>, allowed:Map<String,Int>, npc:Set<String> = emptySet(), page:String)=
        RaceDescriptor(id,mins,mods,allowed,npc,ev("ADV.RACE.$id",page))

    val races: Map<String,RaceDescriptor> = listOf(
        race("DROW",mapOf(AttributeKey.INT to 9),mapOf(AttributeKey.DEX to 1,AttributeKey.CON to -1),mapOf("ACROBAT" to 10,"ASSASSIN" to 10,"CLERIC" to 11,"FIGHTER" to 7,"KNIGHT" to 9,"MAGIC_USER" to 9,"RANGER" to 9,"THIEF" to 11),setOf("CLERIC"),"p.79"),
        race("DUERGAR",mapOf(AttributeKey.INT to 9,AttributeKey.CON to 9),mapOf(AttributeKey.CON to 1,AttributeKey.CHA to -1),mapOf("ASSASSIN" to 9,"CLERIC" to 8,"FIGHTER" to 9,"THIEF" to 9),setOf("CLERIC"),"p.80"),
        race("DWARF",mapOf(AttributeKey.CON to 9),mapOf(AttributeKey.CON to 1,AttributeKey.CHA to -1),mapOf("ASSASSIN" to 9,"CLERIC" to 8,"FIGHTER" to 10,"THIEF" to 9),setOf("CLERIC"),"p.81"),
        race("ELF",mapOf(AttributeKey.INT to 9),mapOf(AttributeKey.DEX to 1,AttributeKey.CON to -1),mapOf("ACROBAT" to 10,"ASSASSIN" to 10,"CLERIC" to 7,"DRUID" to 8,"FIGHTER" to 7,"KNIGHT" to 11,"MAGIC_USER" to 11,"RANGER" to 11,"THIEF" to 10),setOf("CLERIC","DRUID"),"p.82"),
        race("GNOME",mapOf(AttributeKey.INT to 9,AttributeKey.CON to 9),emptyMap(),mapOf("ASSASSIN" to 6,"CLERIC" to 7,"FIGHTER" to 6,"ILLUSIONIST" to 7,"THIEF" to 8),setOf("CLERIC"),"p.83"),
        race("HALF_ELF",mapOf(AttributeKey.CON to 9,AttributeKey.CHA to 9),emptyMap(),mapOf("ACROBAT" to 12,"ASSASSIN" to 11,"BARD" to 12,"CLERIC" to 5,"DRUID" to 12,"FIGHTER" to 8,"KNIGHT" to 12,"MAGIC_USER" to 8,"PALADIN" to 12,"RANGER" to 8,"THIEF" to 12),emptySet(),"p.84"),
        race("HALFLING",mapOf(AttributeKey.DEX to 9,AttributeKey.CON to 9),mapOf(AttributeKey.DEX to 1,AttributeKey.STR to -1),mapOf("DRUID" to 6,"FIGHTER" to 6,"THIEF" to 8),setOf("DRUID"),"p.85"),
        race("HALF_ORC",emptyMap(),mapOf(AttributeKey.STR to 1,AttributeKey.CON to 1,AttributeKey.CHA to -2),mapOf("ACROBAT" to 8,"ASSASSIN" to 8,"CLERIC" to 4,"FIGHTER" to 10,"THIEF" to 8),emptySet(),"p.86"),
        race("HUMAN",emptyMap(),emptyMap(),professionalClassIds.associateWith{14},emptySet(),"p.86; deterministic table limit 14; beyond 14 = MISSING_EVIDENCE"),
        race("SVIRFNEBLIN",mapOf(AttributeKey.CON to 9),emptyMap(),mapOf("ASSASSIN" to 8,"CLERIC" to 7,"FIGHTER" to 6,"ILLUSIONIST" to 7,"THIEF" to 8),setOf("CLERIC"),"p.87")
    ).associateBy { it.raceId }
}

class AdvancedCharacterRules : CharacterRulesProvider {
    override val profile = RuleProfile.OSE_ADVANCED_FANTASY
    private val basicEvidence=ev("ADVANCED_BASIC_CHARACTER_CREATION","pp.14,16-17")
    private val advancedEvidence=ev("ADVANCED_METHOD_CHARACTER_CREATION","pp.14,18-19")

    override fun create(request: CharacterCreationRequest): CharacterCreationResult {
        if(request.profile!=profile || request.method==CreationMethod.CLASSIC) return reject(CreationFailureCode.PROFILE_METHOD_MISMATCH,"Advanced provider accepts ADVANCED_BASIC or ADVANCED_METHOD")
        return if(request.method==CreationMethod.ADVANCED_BASIC) createBasic(request) else createAdvanced(request)
    }

    private fun createBasic(r:CharacterCreationRequest):CharacterCreationResult {
        if(r.classIds.size!=1) return reject(CreationFailureCode.RACE_CLASS_ILLEGAL,"Advanced Basic chooses one class",basicEvidence)
        val clazz=AdvancedCatalog.basicClasses[r.classIds.single()] ?: return reject(CreationFailureCode.UNKNOWN_CLASS,r.classIds.single(),basicEvidence)
        val adjusted=applyAdjustments(r.rolledAttributes,r.adjustments,clazz.primary) ?: return reject(CreationFailureCode.ATTRIBUTE_ADJUSTMENT_ILLEGAL,"illegal -2/+1 adjustment",basicEvidence)
        clazz.minimums.entries.firstOrNull{adjusted.get(it.key)<it.value}?.let{return reject(CreationFailureCode.CLASS_PREREQUISITE,"${it.key} must be >= ${it.value}",clazz.evidence)}
        if(r.hpRolls.size!=1 || r.hpRolls[0] !in 1..clazz.hitDie) return reject(CreationFailureCode.HP_ROLL_INVALID,"Expected one 1d${clazz.hitDie} roll",clazz.evidence)
        val hp=maxOf(1,r.hpRolls[0]+SharedConfirmedRules.attributeModifier(adjusted.con))
        return created(r,adjusted,clazz.basicRaceId,listOf(clazz),HitPoints(hp,hp),basicEvidence)
    }

    private fun createAdvanced(r:CharacterCreationRequest):CharacterCreationResult {
        val raceId=r.raceId ?: return reject(CreationFailureCode.UNKNOWN_RACE,"race required",advancedEvidence)
        val race=AdvancedCatalog.races[raceId] ?: return reject(CreationFailureCode.UNKNOWN_RACE,raceId,advancedEvidence)
        if(r.classIds.isEmpty()) return reject(CreationFailureCode.UNKNOWN_CLASS,"at least one professional class required",advancedEvidence)
        if(r.classIds.size>3) return reject(CreationFailureCode.TOO_MANY_CLASSES,"Advanced multiclass maximum is three",advancedEvidence)
        if(r.classIds.size>1 && r.optionIds.map(OptionIds::canonical).none { it == OptionIds.MULTICLASS }) return reject(CreationFailureCode.MULTICLASS_OPTION_REQUIRED,"MULTICLASS option is OFF",advancedEvidence)
        val classes=r.classIds.map { id ->
            if(id !in AdvancedCatalog.professionalClassIds) return reject(CreationFailureCode.RACE_CLASS_ILLEGAL,"$id is not one of the 13 professional Advanced Method classes",advancedEvidence)
            AdvancedCatalog.basicClasses[id] ?: return reject(CreationFailureCode.UNKNOWN_CLASS,id,advancedEvidence)
        }
        race.minimums.entries.firstOrNull{r.rolledAttributes.get(it.key)<it.value}?.let{return reject(CreationFailureCode.RACE_PREREQUISITE,"${it.key} must be >= ${it.value}",race.evidence)}
        var attrs=r.rolledAttributes
        for((k,v) in race.modifiers){ val n=attrs.get(k)+v; if(n !in 3..18) return reject(CreationFailureCode.RACE_PREREQUISITE,"racial modifier would put $k outside 3..18",race.evidence); attrs=attrs.with(k,n) }
        for(clazz in classes){
            val max=race.allowedClassMaxLevel[clazz.classId] ?: return reject(CreationFailureCode.RACE_CLASS_ILLEGAL,"${race.raceId} cannot select ${clazz.classId}",race.evidence,clazz.evidence)
            if(clazz.classId in race.npcOnlyClasses && !r.allowNpcOnlyCombination) return reject(CreationFailureCode.NPC_ONLY_COMBINATION,"${race.raceId}/${clazz.classId} is referee-optional NPC only",race.evidence,clazz.evidence)
            if(max<1) return reject(CreationFailureCode.RACE_CLASS_ILLEGAL,"invalid racial max level",race.evidence)
            clazz.minimums.entries.firstOrNull{attrs.get(it.key)<it.value}?.let{return reject(CreationFailureCode.CLASS_PREREQUISITE,"${clazz.classId}: ${it.key} must be >= ${it.value}",clazz.evidence)}
        }
        val primaries=classes.flatMap{it.primary}.toSet()
        attrs=applyAdjustments(attrs,r.adjustments,primaries) ?: return reject(CreationFailureCode.ATTRIBUTE_ADJUSTMENT_ILLEGAL,"illegal -2/+1 adjustment",advancedEvidence)
        if(r.hpRolls.size!=classes.size) return reject(CreationFailureCode.HP_ROLL_INVALID,"one level-1 HP roll per class is required",advancedEvidence)
        val con=SharedConfirmedRules.attributeModifier(attrs.con)
        var numerator=0
        for((idx,clazz) in classes.withIndex()){
            val roll=r.hpRolls[idx]; if(roll !in 1..clazz.hitDie) return reject(CreationFailureCode.HP_ROLL_INVALID,"${clazz.classId} expects 1d${clazz.hitDie}",clazz.evidence)
            numerator += maxOf(1,roll+con)
        }
        val hp=HitPoints(numerator,numerator,classes.size)
        return created(r,attrs,race.raceId,classes,hp,advancedEvidence,race.evidence)
    }

    private fun created(r:CharacterCreationRequest, attrs:Attributes, raceId:String, classes:List<ClassLevelOneDescriptor>, hp:HitPoints, creation:RuleEvidence, vararg extra:RuleEvidence):CharacterCreationResult {
        val saves=SavingThrows(
            classes.minOf{it.saves.deathPoison},classes.minOf{it.saves.wands},classes.minOf{it.saves.paralysisPetrification},classes.minOf{it.saves.breath},classes.minOf{it.saves.spellsRodsStaves}
        )
        val thac0=classes.minOf{it.thac0}; val attackBonus=classes.maxOf{it.attackBonusAscending}
        val evidence=(listOf(creation)+extra+classes.map{it.evidence}).distinct()
        val primeMods=classes.mapNotNull { clazz ->
            clazz.primary.singleOrNull()?.let { clazz.classId to SharedConfirmedRules.singlePrimeXpModifier(attrs.get(it)) }
        }.toMap()
        val ch=CharacterSnapshot(r.characterId,r.name,r.method.name,raceId,classes.map{it.classId},classes.associate{it.classId to 1},classes.associate{it.classId to 0L},attrs,hp,
            SharedConfirmedRules.descendingArmorClass(dexterity=attrs.dex),SharedConfirmedRules.ascendingArmorClass(dexterity=attrs.dex),thac0,attackBonus,saves,
            flags=if(classes.size>1) setOf("MULTICLASS") else emptySet(),evidenceRefs=evidence.map{it.ref()},primeModifierPercentByClass=primeMods).canonical()
        return CharacterCreationResult.Created(ch,RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,evidence))
    }

    private fun applyAdjustments(base:Attributes, xs:List<AttributeAdjustment>, primaries:Set<AttributeKey>):Attributes? {
        var a=base
        for(x in xs){ if(x.decrease !in setOf(AttributeKey.STR,AttributeKey.INT,AttributeKey.WIS) || x.increase !in primaries) return null; val down=a.get(x.decrease)-2; val up=a.get(x.increase)+1; if(down<9||up>18)return null; a=a.with(x.decrease,down).with(x.increase,up) }
        return a
    }
    private fun reject(code:CreationFailureCode,detail:String,vararg evidence:RuleEvidence)=CharacterCreationResult.Rejected(code,detail,RuleTrace(if(code==CreationFailureCode.MISSING_EVIDENCE)EvidenceStatus.MISSING_EVIDENCE else EvidenceStatus.CANONICAL_PROCEDURE,evidence.toList()))
}
