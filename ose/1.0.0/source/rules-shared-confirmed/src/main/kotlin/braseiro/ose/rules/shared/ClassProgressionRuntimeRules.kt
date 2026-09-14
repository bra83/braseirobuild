package braseiro.ose.rules.shared

import braseiro.ose.model.*

/** Exact level-progression rows transcribed from GAME_SYSTEM_SPEC §§8-9. */
data class ClassProgressionRowRuntime(
    val level:Int, val xpThreshold:Long, val hitDiceExpression:String,
    val thac0:Int, val attackBonusAscending:Int,
    val saves:Map<String,Int>, val spellSlots:List<Int>, val sourceId:String
)

data class ClassProgressionTableRuntime(
    val profile:RuleProfile, val classId:String, val canonicalRuleId:String,
    val maxLevel:Int, val sourceId:String, val rows:List<ClassProgressionRowRuntime>
) {
    fun row(level:Int)=rows.firstOrNull{it.level==level}
}

data class LevelUpHpGainRuntime(val numerator:Int,val denominator:Int,val dieRolled:Int?=null,val conModifier:Int?=null)
data class LevelUpPlanRuntime(
    val recipientId:String,val classId:String,val fromLevel:Int,val toLevel:Int,
    val row:ClassProgressionRowRuntime,val hpGain:LevelUpHpGainRuntime,
    val sessionId:String,val adjustedXpAfterClassicCap:Long?=null
)
data class LevelUpPlanResultRuntime(val plan:LevelUpPlanRuntime?=null,val blocker:String?=null){ val success:Boolean get()=plan!=null&&blocker==null }

object ClassProgressionRuntimeRules {
    private val saveKeys=listOf("DEATH_POISON","WANDS","PARALYSIS_PETRIFICATION","BREATH","SPELLS_RODS_STAVES")
    private fun r(level:Int,xp:Long,hd:String,thac0:Int,ab:Int,saves:List<Int>,slots:List<Int>,source:String)=ClassProgressionRowRuntime(level,xp,hd,thac0,ab,saveKeys.zip(saves).toMap(),slots,source)
    val tables:Map<Pair<RuleProfile,String>,ClassProgressionTableRuntime> = buildMap {
        put(RuleProfile.OSE_CLASSIC_FANTASY to "DWARF", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"DWARF","CLASSIC.CLASS.DWARF",12,"Classic Rules Tome, pp.22–23",listOf(
            r(1,0L,"1d8",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(2,2200L,"2d8",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(3,4400L,"3d8",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(4,8800L,"4d8",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(5,17000L,"5d8",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(6,35000L,"6d8",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(7,70000L,"7d8",14,5,listOf(4,5,6,7,8),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(8,140000L,"8d8",14,5,listOf(4,5,6,7,8),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(9,270000L,"9d8",14,5,listOf(4,5,6,7,8),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(10,400000L,"9d8+3*",12,7,listOf(2,3,4,4,6),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(11,530000L,"9d8+6*",12,7,listOf(2,3,4,4,6),emptyList(),"Classic Rules Tome, pp.22–23"),
            r(12,660000L,"9d8+9*",12,7,listOf(2,3,4,4,6),emptyList(),"Classic Rules Tome, pp.22–23"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLERIC", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"CLERIC","CLASSIC.CLASS.CLERIC",14,"Classic Rules Tome, pp.24–25",listOf(
            r(1,0L,"1d6",19,0,listOf(11,12,14,16,15),listOf(0,0,0,0,0),"Classic Rules Tome, pp.24–25"),
            r(2,1500L,"2d6",19,0,listOf(11,12,14,16,15),listOf(1,0,0,0,0),"Classic Rules Tome, pp.24–25"),
            r(3,3000L,"3d6",19,0,listOf(11,12,14,16,15),listOf(2,0,0,0,0),"Classic Rules Tome, pp.24–25"),
            r(4,6000L,"4d6",19,0,listOf(11,12,14,16,15),listOf(2,1,0,0,0),"Classic Rules Tome, pp.24–25"),
            r(5,12000L,"5d6",17,2,listOf(9,10,12,14,12),listOf(2,2,0,0,0),"Classic Rules Tome, pp.24–25"),
            r(6,25000L,"6d6",17,2,listOf(9,10,12,14,12),listOf(2,2,1,1,0),"Classic Rules Tome, pp.24–25"),
            r(7,50000L,"7d6",17,2,listOf(9,10,12,14,12),listOf(2,2,2,1,1),"Classic Rules Tome, pp.24–25"),
            r(8,100000L,"8d6",17,2,listOf(9,10,12,14,12),listOf(3,3,2,2,1),"Classic Rules Tome, pp.24–25"),
            r(9,200000L,"9d6",14,5,listOf(6,7,9,11,9),listOf(3,3,3,2,2),"Classic Rules Tome, pp.24–25"),
            r(10,300000L,"9d6+1*",14,5,listOf(6,7,9,11,9),listOf(4,4,3,3,2),"Classic Rules Tome, pp.24–25"),
            r(11,400000L,"9d6+2*",14,5,listOf(6,7,9,11,9),listOf(4,4,4,3,3),"Classic Rules Tome, pp.24–25"),
            r(12,500000L,"9d6+3*",14,5,listOf(6,7,9,11,9),listOf(5,5,4,4,3),"Classic Rules Tome, pp.24–25"),
            r(13,600000L,"9d6+4*",12,7,listOf(3,5,7,8,7),listOf(5,5,5,4,4),"Classic Rules Tome, pp.24–25"),
            r(14,700000L,"9d6+5*",12,7,listOf(3,5,7,8,7),listOf(6,5,5,5,4),"Classic Rules Tome, pp.24–25"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "ELF", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"ELF","CLASSIC.CLASS.ELF",10,"Classic Rules Tome, pp.26–27",listOf(
            r(1,0L,"1d6",19,0,listOf(12,13,13,15,15),listOf(1,0,0,0,0),"Classic Rules Tome, pp.26–27"),
            r(2,4000L,"2d6",19,0,listOf(12,13,13,15,15),listOf(2,0,0,0,0),"Classic Rules Tome, pp.26–27"),
            r(3,8000L,"3d6",19,0,listOf(12,13,13,15,15),listOf(2,1,0,0,0),"Classic Rules Tome, pp.26–27"),
            r(4,16000L,"4d6",17,2,listOf(10,11,11,13,12),listOf(2,2,0,0,0),"Classic Rules Tome, pp.26–27"),
            r(5,32000L,"5d6",17,2,listOf(10,11,11,13,12),listOf(2,2,1,0,0),"Classic Rules Tome, pp.26–27"),
            r(6,64000L,"6d6",17,2,listOf(10,11,11,13,12),listOf(2,2,2,0,0),"Classic Rules Tome, pp.26–27"),
            r(7,120000L,"7d6",14,5,listOf(8,9,9,10,10),listOf(3,2,2,1,0),"Classic Rules Tome, pp.26–27"),
            r(8,250000L,"8d6",14,5,listOf(8,9,9,10,10),listOf(3,3,2,2,0),"Classic Rules Tome, pp.26–27"),
            r(9,400000L,"9d6",14,5,listOf(8,9,9,10,10),listOf(3,3,3,2,1),"Classic Rules Tome, pp.26–27"),
            r(10,600000L,"9d6+2*",12,7,listOf(6,7,8,8,8),listOf(3,3,3,3,2),"Classic Rules Tome, pp.26–27"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "FIGHTER", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"FIGHTER","CLASSIC.CLASS.FIGHTER",14,"Classic Rules Tome, pp.28–29",listOf(
            r(1,0L,"1d8",19,0,listOf(12,13,14,15,16),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(2,2000L,"2d8",19,0,listOf(12,13,14,15,16),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(3,4000L,"3d8",19,0,listOf(12,13,14,15,16),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(4,8000L,"4d8",17,2,listOf(10,11,12,13,14),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(5,16000L,"5d8",17,2,listOf(10,11,12,13,14),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(6,32000L,"6d8",17,2,listOf(10,11,12,13,14),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(7,64000L,"7d8",14,5,listOf(8,9,10,10,12),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(8,120000L,"8d8",14,5,listOf(8,9,10,10,12),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(9,240000L,"9d8",14,5,listOf(8,9,10,10,12),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(10,360000L,"9d8+2*",12,7,listOf(6,7,8,8,10),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(11,480000L,"9d8+4*",12,7,listOf(6,7,8,8,10),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(12,600000L,"9d8+6*",12,7,listOf(6,7,8,8,10),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(13,720000L,"9d8+8*",10,9,listOf(4,5,6,5,8),emptyList(),"Classic Rules Tome, pp.28–29"),
            r(14,840000L,"9d8+10*",10,9,listOf(4,5,6,5,8),emptyList(),"Classic Rules Tome, pp.28–29"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "HALFLING", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"HALFLING","CLASSIC.CLASS.HALFLING",8,"Classic Rules Tome, pp.30–31",listOf(
            r(1,0L,"1d6",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(2,2000L,"2d6",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(3,4000L,"3d6",19,0,listOf(8,9,10,13,12),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(4,8000L,"4d6",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(5,16000L,"5d6",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(6,32000L,"6d6",17,2,listOf(6,7,8,10,10),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(7,64000L,"7d6",14,5,listOf(4,5,6,7,8),emptyList(),"Classic Rules Tome, pp.30–31"),
            r(8,120000L,"8d6",14,5,listOf(4,5,6,7,8),emptyList(),"Classic Rules Tome, pp.30–31"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "THIEF", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"THIEF","CLASSIC.CLASS.THIEF",14,"Classic Rules Tome, pp.32–33",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(2,1200L,"2d4",19,0,listOf(13,14,13,16,15),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(3,2400L,"3d4",19,0,listOf(13,14,13,16,15),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(4,4800L,"4d4",19,0,listOf(13,14,13,16,15),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(5,9600L,"5d4",17,2,listOf(12,13,11,14,13),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(6,20000L,"6d4",17,2,listOf(12,13,11,14,13),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(7,40000L,"7d4",17,2,listOf(12,13,11,14,13),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(8,80000L,"8d4",17,2,listOf(12,13,11,14,13),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(9,160000L,"9d4",14,5,listOf(10,11,9,12,10),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(10,280000L,"9d4+2*",14,5,listOf(10,11,9,12,10),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(11,400000L,"9d4+4*",14,5,listOf(10,11,9,12,10),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(12,520000L,"9d4+6*",14,5,listOf(10,11,9,12,10),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(13,640000L,"9d4+8*",12,7,listOf(8,9,7,10,8),emptyList(),"Classic Rules Tome, pp.32–33"),
            r(14,760000L,"9d4+10*",12,7,listOf(8,9,7,10,8),emptyList(),"Classic Rules Tome, pp.32–33"),
        )))
        put(RuleProfile.OSE_CLASSIC_FANTASY to "MAGIC_USER", ClassProgressionTableRuntime(RuleProfile.OSE_CLASSIC_FANTASY,"MAGIC_USER","CLASSIC.CLASS.MAGIC_USER",14,"Classic Rules Tome, pp.34–35",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),listOf(1,0,0,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(2,2500L,"2d4",19,0,listOf(13,14,13,16,15),listOf(2,0,0,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(3,5000L,"3d4",19,0,listOf(13,14,13,16,15),listOf(2,1,0,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(4,10000L,"4d4",19,0,listOf(13,14,13,16,15),listOf(2,2,0,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(5,20000L,"5d4",19,0,listOf(13,14,13,16,15),listOf(2,2,1,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(6,40000L,"6d4",17,2,listOf(11,12,11,14,12),listOf(2,2,2,0,0,0),"Classic Rules Tome, pp.34–35"),
            r(7,80000L,"7d4",17,2,listOf(11,12,11,14,12),listOf(3,2,2,1,0,0),"Classic Rules Tome, pp.34–35"),
            r(8,150000L,"8d4",17,2,listOf(11,12,11,14,12),listOf(3,3,2,2,0,0),"Classic Rules Tome, pp.34–35"),
            r(9,300000L,"9d4",17,2,listOf(11,12,11,14,12),listOf(3,3,3,2,1,0),"Classic Rules Tome, pp.34–35"),
            r(10,450000L,"9d4+1*",17,2,listOf(11,12,11,14,12),listOf(3,3,3,3,2,0),"Classic Rules Tome, pp.34–35"),
            r(11,600000L,"9d4+2*",14,5,listOf(8,9,8,11,8),listOf(4,3,3,3,2,1),"Classic Rules Tome, pp.34–35"),
            r(12,750000L,"9d4+3*",14,5,listOf(8,9,8,11,8),listOf(4,4,3,3,3,2),"Classic Rules Tome, pp.34–35"),
            r(13,900000L,"9d4+4*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,3,3,3),"Classic Rules Tome, pp.34–35"),
            r(14,1050000L,"9d4+5*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,4,3,3),"Classic Rules Tome, pp.34–35"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ACROBAT", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"ACROBAT","ADV.BASIC.CLASS.ACROBAT",14,"Advanced Player’s Tome, pp.28–29",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(2,1200L,"2d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(3,2400L,"3d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(4,4800L,"4d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(5,9600L,"5d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(6,20000L,"6d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(7,40000L,"7d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(8,80000L,"8d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(9,160000L,"9d4",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(10,280000L,"9d4+2*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(11,400000L,"9d4+4*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(12,520000L,"9d4+6*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(13,640000L,"9d4+8*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.28–29"),
            r(14,760000L,"9d4+10*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.28–29"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ASSASSIN", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"ASSASSIN","ADV.BASIC.CLASS.ASSASSIN",14,"Advanced Player’s Tome, pp.30–31",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(2,1500L,"2d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(3,3000L,"3d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(4,6000L,"4d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(5,12000L,"5d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(6,25000L,"6d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(7,50000L,"7d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(8,100000L,"8d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(9,200000L,"9d4",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(10,300000L,"9d4+2*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(11,425000L,"9d4+4*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(12,575000L,"9d4+6*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(13,750000L,"9d4+8*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.30–31"),
            r(14,900000L,"9d4+10*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.30–31"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "BARBARIAN", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"BARBARIAN","ADV.BASIC.CLASS.BARBARIAN",14,"Advanced Player’s Tome, pp.32–33",listOf(
            r(1,0L,"1d8",19,0,listOf(10,13,12,15,16),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(2,2500L,"2d8",19,0,listOf(10,13,12,15,16),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(3,5000L,"3d8",19,0,listOf(10,13,12,15,16),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(4,10000L,"4d8",17,2,listOf(8,11,10,13,13),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(5,18500L,"5d8",17,2,listOf(8,11,10,13,13),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(6,37000L,"6d8",17,2,listOf(8,11,10,13,13),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(7,85000L,"7d8",14,5,listOf(6,9,8,10,10),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(8,140000L,"8d8",14,5,listOf(6,9,8,10,10),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(9,270000L,"9d8",14,5,listOf(6,9,8,10,10),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(10,400000L,"9d8+3*",12,7,listOf(4,7,6,8,7),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(11,530000L,"9d8+6*",12,7,listOf(4,7,6,8,7),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(12,660000L,"9d8+9*",12,7,listOf(4,7,6,8,7),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(13,790000L,"9d8+12*",10,9,listOf(3,5,4,5,5),emptyList(),"Advanced Player’s Tome, pp.32–33"),
            r(14,920000L,"9d8+15*",10,9,listOf(3,5,4,5,5),emptyList(),"Advanced Player’s Tome, pp.32–33"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "BARD", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"BARD","ADV.BASIC.CLASS.BARD",14,"Advanced Player’s Tome, pp.34–35",listOf(
            r(1,0L,"1d6",19,0,listOf(13,14,13,16,15),listOf(0,0,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(2,2000L,"2d6",19,0,listOf(13,14,13,16,15),listOf(1,0,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(3,4000L,"3d6",19,0,listOf(13,14,13,16,15),listOf(2,0,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(4,8000L,"4d6",19,0,listOf(13,14,13,16,15),listOf(3,0,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(5,16000L,"5d6",17,2,listOf(12,13,11,14,13),listOf(3,1,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(6,32000L,"6d6",17,2,listOf(12,13,11,14,13),listOf(3,2,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(7,64000L,"7d6",17,2,listOf(12,13,11,14,13),listOf(3,3,0,0),"Advanced Player’s Tome, pp.34–35"),
            r(8,120000L,"8d6",17,2,listOf(12,13,11,14,13),listOf(3,3,1,0),"Advanced Player’s Tome, pp.34–35"),
            r(9,240000L,"9d6",14,5,listOf(10,11,9,12,10),listOf(3,3,2,0),"Advanced Player’s Tome, pp.34–35"),
            r(10,360000L,"9d6+2*",14,5,listOf(10,11,9,12,10),listOf(3,3,3,0),"Advanced Player’s Tome, pp.34–35"),
            r(11,480000L,"9d6+4*",14,5,listOf(10,11,9,12,10),listOf(3,3,3,1),"Advanced Player’s Tome, pp.34–35"),
            r(12,600000L,"9d6+6*",14,5,listOf(10,11,9,12,10),listOf(3,3,3,2),"Advanced Player’s Tome, pp.34–35"),
            r(13,720000L,"9d6+8*",12,7,listOf(8,9,7,10,8),listOf(3,3,3,3),"Advanced Player’s Tome, pp.34–35"),
            r(14,840000L,"9d6+10*",12,7,listOf(8,9,7,10,8),listOf(4,4,3,3),"Advanced Player’s Tome, pp.34–35"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "CLERIC", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"CLERIC","ADV.BASIC.CLASS.CLERIC",14,"Advanced Player’s Tome, pp.36–37",listOf(
            r(1,0L,"1d6",19,0,listOf(11,12,14,16,15),listOf(0,0,0,0,0),"Advanced Player’s Tome, pp.36–37"),
            r(2,1500L,"2d6",19,0,listOf(11,12,14,16,15),listOf(1,0,0,0,0),"Advanced Player’s Tome, pp.36–37"),
            r(3,3000L,"3d6",19,0,listOf(11,12,14,16,15),listOf(2,0,0,0,0),"Advanced Player’s Tome, pp.36–37"),
            r(4,6000L,"4d6",19,0,listOf(11,12,14,16,15),listOf(2,1,0,0,0),"Advanced Player’s Tome, pp.36–37"),
            r(5,12000L,"5d6",17,2,listOf(9,10,12,14,12),listOf(2,2,0,0,0),"Advanced Player’s Tome, pp.36–37"),
            r(6,25000L,"6d6",17,2,listOf(9,10,12,14,12),listOf(2,2,1,1,0),"Advanced Player’s Tome, pp.36–37"),
            r(7,50000L,"7d6",17,2,listOf(9,10,12,14,12),listOf(2,2,2,1,1),"Advanced Player’s Tome, pp.36–37"),
            r(8,100000L,"8d6",17,2,listOf(9,10,12,14,12),listOf(3,3,2,2,1),"Advanced Player’s Tome, pp.36–37"),
            r(9,200000L,"9d6",14,5,listOf(6,7,9,11,9),listOf(3,3,3,2,2),"Advanced Player’s Tome, pp.36–37"),
            r(10,300000L,"9d6+1*",14,5,listOf(6,7,9,11,9),listOf(4,4,3,3,2),"Advanced Player’s Tome, pp.36–37"),
            r(11,400000L,"9d6+2*",14,5,listOf(6,7,9,11,9),listOf(4,4,4,3,3),"Advanced Player’s Tome, pp.36–37"),
            r(12,500000L,"9d6+3*",14,5,listOf(6,7,9,11,9),listOf(5,5,4,4,3),"Advanced Player’s Tome, pp.36–37"),
            r(13,600000L,"9d6+4*",12,7,listOf(3,5,7,8,7),listOf(5,5,5,4,4),"Advanced Player’s Tome, pp.36–37"),
            r(14,700000L,"9d6+5*",12,7,listOf(3,5,7,8,7),listOf(6,5,5,5,4),"Advanced Player’s Tome, pp.36–37"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "DROW", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"DROW","ADV.BASIC.CLASS.DROW",10,"Advanced Player’s Tome, pp.38–39",listOf(
            r(1,0L,"1d6",19,0,listOf(12,13,13,15,12),listOf(1,0,0,0,0),"Advanced Player’s Tome, pp.38–39"),
            r(2,4000L,"2d6",19,0,listOf(12,13,13,15,12),listOf(2,0,0,0,0),"Advanced Player’s Tome, pp.38–39"),
            r(3,8000L,"3d6",19,0,listOf(12,13,13,15,12),listOf(2,1,0,0,0),"Advanced Player’s Tome, pp.38–39"),
            r(4,16000L,"4d6",17,2,listOf(10,11,11,13,10),listOf(2,2,0,0,0),"Advanced Player’s Tome, pp.38–39"),
            r(5,32000L,"5d6",17,2,listOf(10,11,11,13,10),listOf(2,2,1,0,0),"Advanced Player’s Tome, pp.38–39"),
            r(6,64000L,"6d6",17,2,listOf(10,11,11,13,10),listOf(2,2,2,1,0),"Advanced Player’s Tome, pp.38–39"),
            r(7,120000L,"7d6",14,5,listOf(8,9,9,10,8),listOf(3,3,2,2,1),"Advanced Player’s Tome, pp.38–39"),
            r(8,250000L,"8d6",14,5,listOf(8,9,9,10,8),listOf(3,3,3,2,2),"Advanced Player’s Tome, pp.38–39"),
            r(9,400000L,"9d6",14,5,listOf(8,9,9,10,8),listOf(4,4,3,3,2),"Advanced Player’s Tome, pp.38–39"),
            r(10,600000L,"9d6+2*",12,7,listOf(6,7,8,8,6),listOf(4,4,4,3,3),"Advanced Player’s Tome, pp.38–39"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "DRUID", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"DRUID","ADV.BASIC.CLASS.DRUID",14,"Advanced Player’s Tome, pp.40–43",listOf(
            r(1,0L,"1d6",19,0,listOf(11,12,14,16,15),listOf(1,0,0,0,0),"Advanced Player’s Tome, pp.40–43"),
            r(2,2000L,"2d6",19,0,listOf(11,12,14,16,15),listOf(2,0,0,0,0),"Advanced Player’s Tome, pp.40–43"),
            r(3,4000L,"3d6",19,0,listOf(11,12,14,16,15),listOf(2,1,0,0,0),"Advanced Player’s Tome, pp.40–43"),
            r(4,7500L,"4d6",19,0,listOf(11,12,14,16,15),listOf(2,2,0,0,0),"Advanced Player’s Tome, pp.40–43"),
            r(5,12500L,"5d6",17,2,listOf(9,10,12,14,12),listOf(2,2,1,1,0),"Advanced Player’s Tome, pp.40–43"),
            r(6,20000L,"6d6",17,2,listOf(9,10,12,14,12),listOf(2,2,2,1,1),"Advanced Player’s Tome, pp.40–43"),
            r(7,35000L,"7d6",17,2,listOf(9,10,12,14,12),listOf(3,3,2,2,1),"Advanced Player’s Tome, pp.40–43"),
            r(8,60000L,"8d6",17,2,listOf(9,10,12,14,12),listOf(3,3,3,2,2),"Advanced Player’s Tome, pp.40–43"),
            r(9,90000L,"9d6",14,5,listOf(6,7,9,11,9),listOf(4,4,3,3,2),"Advanced Player’s Tome, pp.40–43"),
            r(10,125000L,"9d6+1*",14,5,listOf(6,7,9,11,9),listOf(4,4,4,3,3),"Advanced Player’s Tome, pp.40–43"),
            r(11,200000L,"9d6+2*",14,5,listOf(6,7,9,11,9),listOf(5,5,4,4,3),"Advanced Player’s Tome, pp.40–43"),
            r(12,300000L,"9d6+3*",14,5,listOf(6,7,9,11,9),listOf(5,5,5,4,4),"Advanced Player’s Tome, pp.40–43"),
            r(13,750000L,"9d6+4*",12,7,listOf(3,5,7,8,7),listOf(6,5,5,5,4),"Advanced Player’s Tome, pp.40–43"),
            r(14,1500000L,"9d6+5*",12,7,listOf(3,5,7,8,7),listOf(6,6,5,5,5),"Advanced Player’s Tome, pp.40–43"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "DUERGAR", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"DUERGAR","ADV.BASIC.CLASS.DUERGAR",10,"Advanced Player’s Tome, pp.44–45",listOf(
            r(1,0L,"1d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(2,2800L,"2d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(3,5600L,"3d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(4,11200L,"4d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(5,23000L,"5d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(6,46000L,"6d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(7,100000L,"7d6",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(8,200000L,"8d6",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(9,300000L,"9d6",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.44–45"),
            r(10,400000L,"9d6+3*",12,7,listOf(2,3,4,4,6),emptyList(),"Advanced Player’s Tome, pp.44–45"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "DWARF", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"DWARF","ADV.BASIC.CLASS.DWARF",12,"Advanced Player’s Tome, pp.46–47",listOf(
            r(1,0L,"1d8",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(2,2200L,"2d8",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(3,4400L,"3d8",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(4,8800L,"4d8",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(5,17000L,"5d8",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(6,35000L,"6d8",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(7,70000L,"7d8",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(8,140000L,"8d8",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(9,270000L,"9d8",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(10,400000L,"9d8+3*",12,7,listOf(2,3,4,4,6),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(11,530000L,"9d8+6*",12,7,listOf(2,3,4,4,6),emptyList(),"Advanced Player’s Tome, pp.46–47"),
            r(12,660000L,"9d8+9*",12,7,listOf(2,3,4,4,6),emptyList(),"Advanced Player’s Tome, pp.46–47"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ELF", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"ELF","ADV.BASIC.CLASS.ELF",10,"Advanced Player’s Tome, pp.48–49",listOf(
            r(1,0L,"1d6",19,0,listOf(12,13,13,15,15),listOf(1,0,0,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(2,4000L,"2d6",19,0,listOf(12,13,13,15,15),listOf(2,0,0,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(3,8000L,"3d6",19,0,listOf(12,13,13,15,15),listOf(2,1,0,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(4,16000L,"4d6",17,2,listOf(10,11,11,13,12),listOf(2,2,0,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(5,32000L,"5d6",17,2,listOf(10,11,11,13,12),listOf(2,2,1,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(6,64000L,"6d6",17,2,listOf(10,11,11,13,12),listOf(2,2,2,0,0),"Advanced Player’s Tome, pp.48–49"),
            r(7,120000L,"7d6",14,5,listOf(8,9,9,10,10),listOf(3,2,2,1,0),"Advanced Player’s Tome, pp.48–49"),
            r(8,250000L,"8d6",14,5,listOf(8,9,9,10,10),listOf(3,3,2,2,0),"Advanced Player’s Tome, pp.48–49"),
            r(9,400000L,"9d6",14,5,listOf(8,9,9,10,10),listOf(3,3,3,2,1),"Advanced Player’s Tome, pp.48–49"),
            r(10,600000L,"9d6+2*",12,7,listOf(6,7,8,8,8),listOf(3,3,3,3,2),"Advanced Player’s Tome, pp.48–49"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "FIGHTER", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"FIGHTER","ADV.BASIC.CLASS.FIGHTER",14,"Advanced Player’s Tome, pp.50–51",listOf(
            r(1,0L,"1d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(2,2000L,"2d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(3,4000L,"3d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(4,8000L,"4d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(5,16000L,"5d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(6,32000L,"6d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(7,64000L,"7d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(8,120000L,"8d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(9,240000L,"9d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(10,360000L,"9d8+2*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(11,480000L,"9d8+4*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(12,600000L,"9d8+6*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(13,720000L,"9d8+8*",10,9,listOf(4,5,6,5,8),emptyList(),"Advanced Player’s Tome, pp.50–51"),
            r(14,840000L,"9d8+10*",10,9,listOf(4,5,6,5,8),emptyList(),"Advanced Player’s Tome, pp.50–51"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "GNOME", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"GNOME","ADV.BASIC.CLASS.GNOME",8,"Advanced Player’s Tome, pp.52–53",listOf(
            r(1,0L,"1d4",19,0,listOf(8,9,10,14,11),listOf(1,0,0,0),"Advanced Player’s Tome, pp.52–53"),
            r(2,3000L,"2d4",19,0,listOf(8,9,10,14,11),listOf(2,0,0,0),"Advanced Player’s Tome, pp.52–53"),
            r(3,6000L,"3d4",19,0,listOf(8,9,10,14,11),listOf(2,1,0,0),"Advanced Player’s Tome, pp.52–53"),
            r(4,12000L,"4d4",19,0,listOf(8,9,10,14,11),listOf(2,2,0,0),"Advanced Player’s Tome, pp.52–53"),
            r(5,30000L,"5d4",19,0,listOf(8,9,10,14,11),listOf(2,2,1,0),"Advanced Player’s Tome, pp.52–53"),
            r(6,60000L,"6d4",17,2,listOf(6,7,8,11,9),listOf(2,2,2,0),"Advanced Player’s Tome, pp.52–53"),
            r(7,120000L,"7d4",17,2,listOf(6,7,8,11,9),listOf(3,2,2,1),"Advanced Player’s Tome, pp.52–53"),
            r(8,240000L,"8d4",17,2,listOf(6,7,8,11,9),listOf(3,3,2,2),"Advanced Player’s Tome, pp.52–53"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "HALF_ELF", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"HALF_ELF","ADV.BASIC.CLASS.HALF_ELF",12,"Advanced Player’s Tome, pp.54–55",listOf(
            r(1,0L,"1d6",19,0,listOf(12,13,13,15,15),listOf(0,0,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(2,2500L,"2d6",19,0,listOf(12,13,13,15,15),listOf(1,0,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(3,5000L,"3d6",19,0,listOf(12,13,13,15,15),listOf(2,0,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(4,10000L,"4d6",17,2,listOf(10,11,11,13,12),listOf(2,0,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(5,20000L,"5d6",17,2,listOf(10,11,11,13,12),listOf(2,1,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(6,40000L,"6d6",17,2,listOf(10,11,11,13,12),listOf(2,2,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(7,80000L,"7d6",14,5,listOf(8,9,9,10,10),listOf(2,2,0,0),"Advanced Player’s Tome, pp.54–55"),
            r(8,150000L,"8d6",14,5,listOf(8,9,9,10,10),listOf(2,2,1,0),"Advanced Player’s Tome, pp.54–55"),
            r(9,300000L,"9d6",14,5,listOf(8,9,9,10,10),listOf(3,2,1,0),"Advanced Player’s Tome, pp.54–55"),
            r(10,450000L,"9d6+2*",12,7,listOf(6,7,8,8,8),listOf(3,2,2,0),"Advanced Player’s Tome, pp.54–55"),
            r(11,600000L,"9d6+4*",12,7,listOf(6,7,8,8,8),listOf(3,2,2,1),"Advanced Player’s Tome, pp.54–55"),
            r(12,750000L,"9d6+6*",12,7,listOf(6,7,8,8,8),listOf(3,3,2,1),"Advanced Player’s Tome, pp.54–55"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "HALFLING", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"HALFLING","ADV.BASIC.CLASS.HALFLING",8,"Advanced Player’s Tome, pp.56–57",listOf(
            r(1,0L,"1d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(2,2000L,"2d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(3,4000L,"3d6",19,0,listOf(8,9,10,13,12),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(4,8000L,"4d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(5,16000L,"5d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(6,32000L,"6d6",17,2,listOf(6,7,8,10,10),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(7,64000L,"7d6",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.56–57"),
            r(8,120000L,"8d6",14,5,listOf(4,5,6,7,8),emptyList(),"Advanced Player’s Tome, pp.56–57"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "HALF_ORC", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"HALF_ORC","ADV.BASIC.CLASS.HALF_ORC",8,"Advanced Player’s Tome, pp.60–61",listOf(
            r(1,0L,"1d6",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(2,1800L,"2d6",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(3,3600L,"3d6",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(4,7000L,"4d6",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(5,14000L,"5d6",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(6,28000L,"6d6",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(7,60000L,"7d6",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.60–61"),
            r(8,120000L,"8d6",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.60–61"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ILLUSIONIST", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"ILLUSIONIST","ADV.BASIC.CLASS.ILLUSIONIST",14,"Advanced Player’s Tome, pp.62–63",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),listOf(1,0,0,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(2,2500L,"2d4",19,0,listOf(13,14,13,16,15),listOf(2,0,0,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(3,5000L,"3d4",19,0,listOf(13,14,13,16,15),listOf(2,1,0,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(4,10000L,"4d4",19,0,listOf(13,14,13,16,15),listOf(2,2,0,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(5,20000L,"5d4",19,0,listOf(13,14,13,16,15),listOf(2,2,1,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(6,40000L,"6d4",17,2,listOf(11,12,11,14,12),listOf(2,2,2,0,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(7,80000L,"7d4",17,2,listOf(11,12,11,14,12),listOf(3,2,2,1,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(8,150000L,"8d4",17,2,listOf(11,12,11,14,12),listOf(3,3,2,2,0,0),"Advanced Player’s Tome, pp.62–63"),
            r(9,300000L,"9d4",17,2,listOf(11,12,11,14,12),listOf(3,3,3,2,1,0),"Advanced Player’s Tome, pp.62–63"),
            r(10,450000L,"9d4+1*",17,2,listOf(11,12,11,14,12),listOf(3,3,3,3,2,0),"Advanced Player’s Tome, pp.62–63"),
            r(11,600000L,"9d4+2*",14,5,listOf(8,9,8,11,8),listOf(4,3,3,3,2,1),"Advanced Player’s Tome, pp.62–63"),
            r(12,750000L,"9d4+3*",14,5,listOf(8,9,8,11,8),listOf(4,4,3,3,3,2),"Advanced Player’s Tome, pp.62–63"),
            r(13,900000L,"9d4+4*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,3,3,3),"Advanced Player’s Tome, pp.62–63"),
            r(14,1050000L,"9d4+5*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,4,3,3),"Advanced Player’s Tome, pp.62–63"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "KNIGHT", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"KNIGHT","ADV.BASIC.CLASS.KNIGHT",14,"Advanced Player’s Tome, pp.64–65",listOf(
            r(1,0L,"1d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(2,2500L,"2d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(3,5000L,"3d8",19,0,listOf(12,13,14,15,16),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(4,10000L,"4d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(5,18500L,"5d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(6,37000L,"6d8",17,2,listOf(10,11,12,13,14),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(7,85000L,"7d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(8,140000L,"8d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(9,270000L,"9d8",14,5,listOf(8,9,10,10,12),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(10,400000L,"9d8+2*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(11,530000L,"9d8+4*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(12,660000L,"9d8+6*",12,7,listOf(6,7,8,8,10),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(13,790000L,"9d8+8*",10,9,listOf(4,5,6,5,8),emptyList(),"Advanced Player’s Tome, pp.64–65"),
            r(14,920000L,"9d8+10*",10,9,listOf(4,5,6,5,8),emptyList(),"Advanced Player’s Tome, pp.64–65"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "MAGIC_USER", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"MAGIC_USER","ADV.BASIC.CLASS.MAGIC_USER",14,"Advanced Player’s Tome, pp.66–67",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),listOf(1,0,0,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(2,2500L,"2d4",19,0,listOf(13,14,13,16,15),listOf(2,0,0,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(3,5000L,"3d4",19,0,listOf(13,14,13,16,15),listOf(2,1,0,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(4,10000L,"4d4",19,0,listOf(13,14,13,16,15),listOf(2,2,0,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(5,20000L,"5d4",19,0,listOf(13,14,13,16,15),listOf(2,2,1,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(6,40000L,"6d4",17,2,listOf(11,12,11,14,12),listOf(2,2,2,0,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(7,80000L,"7d4",17,2,listOf(11,12,11,14,12),listOf(3,2,2,1,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(8,150000L,"8d4",17,2,listOf(11,12,11,14,12),listOf(3,3,2,2,0,0),"Advanced Player’s Tome, pp.66–67"),
            r(9,300000L,"9d4",17,2,listOf(11,12,11,14,12),listOf(3,3,3,2,1,0),"Advanced Player’s Tome, pp.66–67"),
            r(10,450000L,"9d4+1*",17,2,listOf(11,12,11,14,12),listOf(3,3,3,3,2,0),"Advanced Player’s Tome, pp.66–67"),
            r(11,600000L,"9d4+2*",14,5,listOf(8,9,8,11,8),listOf(4,3,3,3,2,1),"Advanced Player’s Tome, pp.66–67"),
            r(12,750000L,"9d4+3*",14,5,listOf(8,9,8,11,8),listOf(4,4,3,3,3,2),"Advanced Player’s Tome, pp.66–67"),
            r(13,900000L,"9d4+4*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,3,3,3),"Advanced Player’s Tome, pp.66–67"),
            r(14,1050000L,"9d4+5*",14,5,listOf(8,9,8,11,8),listOf(4,4,4,4,3,3),"Advanced Player’s Tome, pp.66–67"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "PALADIN", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"PALADIN","ADV.BASIC.CLASS.PALADIN",14,"Advanced Player’s Tome, pp.68–69",listOf(
            r(1,0L,"1d8",19,0,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(2,2750L,"2d8",19,0,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(3,5500L,"3d8",19,0,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(4,12000L,"4d8",17,2,listOf(8,9,10,11,12),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(5,24000L,"5d8",17,2,listOf(8,9,10,11,12),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(6,45000L,"6d8",17,2,listOf(8,9,10,11,12),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(7,95000L,"7d8",14,5,listOf(6,7,8,8,10),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(8,175000L,"8d8",14,5,listOf(6,7,8,8,10),listOf(0,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(9,350000L,"9d8",14,5,listOf(6,7,8,8,10),listOf(1,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(10,500000L,"9d8+2*",12,7,listOf(4,5,6,6,8),listOf(2,0,0),"Advanced Player’s Tome, pp.68–69"),
            r(11,650000L,"9d8+4*",12,7,listOf(4,5,6,6,8),listOf(2,1,0),"Advanced Player’s Tome, pp.68–69"),
            r(12,800000L,"9d8+6*",12,7,listOf(4,5,6,6,8),listOf(2,2,0),"Advanced Player’s Tome, pp.68–69"),
            r(13,950000L,"9d8+8*",10,9,listOf(2,3,4,3,6),listOf(2,2,1),"Advanced Player’s Tome, pp.68–69"),
            r(14,1100000L,"9d8+10*",10,9,listOf(2,3,4,3,6),listOf(3,2,1),"Advanced Player’s Tome, pp.68–69"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "RANGER", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"RANGER","ADV.BASIC.CLASS.RANGER",14,"Advanced Player’s Tome, pp.70–71",listOf(
            r(1,0L,"1d8",19,0,listOf(12,13,14,15,16),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(2,2250L,"2d8",19,0,listOf(12,13,14,15,16),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(3,4500L,"3d8",19,0,listOf(12,13,14,15,16),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(4,10000L,"4d8",17,2,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(5,20000L,"5d8",17,2,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(6,40000L,"6d8",17,2,listOf(10,11,12,13,14),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(7,90000L,"7d8",14,5,listOf(8,9,10,10,12),listOf(0,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(8,150000L,"8d8",14,5,listOf(8,9,10,10,12),listOf(1,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(9,300000L,"9d8",14,5,listOf(8,9,10,10,12),listOf(2,0,0),"Advanced Player’s Tome, pp.70–71"),
            r(10,425000L,"9d8+2*",12,7,listOf(6,7,8,8,10),listOf(2,1,0),"Advanced Player’s Tome, pp.70–71"),
            r(11,550000L,"9d8+4*",12,7,listOf(6,7,8,8,10),listOf(2,2,0),"Advanced Player’s Tome, pp.70–71"),
            r(12,675000L,"9d8+6*",12,7,listOf(6,7,8,8,10),listOf(2,2,1),"Advanced Player’s Tome, pp.70–71"),
            r(13,800000L,"9d8+8*",10,9,listOf(4,5,6,5,8),listOf(3,2,1),"Advanced Player’s Tome, pp.70–71"),
            r(14,925000L,"9d8+10*",10,9,listOf(4,5,6,5,8),listOf(3,2,2),"Advanced Player’s Tome, pp.70–71"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "SVIRFNEBLIN", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"SVIRFNEBLIN","ADV.BASIC.CLASS.SVIRFNEBLIN",8,"Advanced Player’s Tome, pp.72–73",listOf(
            r(1,0L,"1d6",19,0,listOf(8,9,10,14,11),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(2,2400L,"2d6",19,0,listOf(8,9,10,14,11),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(3,4800L,"3d6",19,0,listOf(8,9,10,14,11),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(4,10000L,"4d6",17,2,listOf(6,7,8,11,9),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(5,20000L,"5d6",17,2,listOf(6,7,8,11,9),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(6,40000L,"6d6",17,2,listOf(6,7,8,11,9),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(7,80000L,"7d6",14,5,listOf(4,5,6,9,7),emptyList(),"Advanced Player’s Tome, pp.72–73"),
            r(8,160000L,"8d6",14,5,listOf(4,5,6,9,7),emptyList(),"Advanced Player’s Tome, pp.72–73"),
        )))
        put(RuleProfile.OSE_ADVANCED_FANTASY to "THIEF", ClassProgressionTableRuntime(RuleProfile.OSE_ADVANCED_FANTASY,"THIEF","ADV.BASIC.CLASS.THIEF",14,"Advanced Player’s Tome, pp.74–75",listOf(
            r(1,0L,"1d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(2,1200L,"2d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(3,2400L,"3d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(4,4800L,"4d4",19,0,listOf(13,14,13,16,15),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(5,9600L,"5d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(6,20000L,"6d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(7,40000L,"7d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(8,80000L,"8d4",17,2,listOf(12,13,11,14,13),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(9,160000L,"9d4",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(10,280000L,"9d4+2*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(11,400000L,"9d4+4*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(12,520000L,"9d4+6*",14,5,listOf(10,11,9,12,10),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(13,640000L,"9d4+8*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.74–75"),
            r(14,760000L,"9d4+10*",12,7,listOf(8,9,7,10,8),emptyList(),"Advanced Player’s Tome, pp.74–75"),
        )))
    }

    private val explicitCanonicalAliases:Map<Pair<RuleProfile,String>,String> = buildMap {
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.DWARF", "DWARF")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.CLERIC", "CLERIC")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.ELF", "ELF")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.FIGHTER", "FIGHTER")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.HALFLING", "HALFLING")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.THIEF", "THIEF")
        put(RuleProfile.OSE_CLASSIC_FANTASY to "CLASSIC.CLASS.MAGIC_USER", "MAGIC_USER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.ACROBAT", "ACROBAT")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.ACROBAT", "ACROBAT")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.ASSASSIN", "ASSASSIN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.ASSASSIN", "ASSASSIN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.BARBARIAN", "BARBARIAN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.BARBARIAN", "BARBARIAN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.BARD", "BARD")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.BARD", "BARD")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.CLERIC", "CLERIC")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.CLERIC", "CLERIC")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.DROW", "DROW")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.DRUID", "DRUID")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.DRUID", "DRUID")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.DUERGAR", "DUERGAR")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.DWARF", "DWARF")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.ELF", "ELF")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.FIGHTER", "FIGHTER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.FIGHTER", "FIGHTER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.GNOME", "GNOME")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.HALF_ELF", "HALF_ELF")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.HALFLING", "HALFLING")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.HALF_ORC", "HALF_ORC")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.ILLUSIONIST", "ILLUSIONIST")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.ILLUSIONIST", "ILLUSIONIST")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.KNIGHT", "KNIGHT")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.KNIGHT", "KNIGHT")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.MAGIC_USER", "MAGIC_USER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.MAGIC_USER", "MAGIC_USER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.PALADIN", "PALADIN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.PALADIN", "PALADIN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.RANGER", "RANGER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.RANGER", "RANGER")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.SVIRFNEBLIN", "SVIRFNEBLIN")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.BASIC.CLASS.THIEF", "THIEF")
        put(RuleProfile.OSE_ADVANCED_FANTASY to "ADV.CLASS.THIEF", "THIEF")
    }

    fun table(profile:RuleProfile,classId:String):ClassProgressionTableRuntime? {
        val exact=explicitCanonicalAliases[profile to classId] ?: classId
        return tables[profile to exact]
    }

    fun planLevelUp(profile:RuleProfile, experience:ExperienceRuntimeSnapshot, recipientId:String, classId:String, rulesDie:(Int)->Int):LevelUpPlanResultRuntime {
        val recipient=experience.recipients[recipientId] ?: return LevelUpPlanResultRuntime(blocker="RECIPIENT_NOT_FOUND:$recipientId")
        if(recipient.profile!=profile) return LevelUpPlanResultRuntime(blocker="PROFILE_MISMATCH:${recipient.profile}:$profile")
        if(!recipient.surviving) return LevelUpPlanResultRuntime(blocker="RECIPIENT_NOT_SURVIVING:$recipientId")
        if(classId !in recipient.classIds) return LevelUpPlanResultRuntime(blocker="CLASS_NOT_OWNED:$recipientId:$classId")
        val table=table(profile,classId) ?: return LevelUpPlanResultRuntime(blocker="CLASS_PROGRESSION_NOT_BOUND:$profile:$classId")
        val current=recipient.levelByClass[classId] ?: 1
        if(current>=table.maxLevel) return LevelUpPlanResultRuntime(blocker=if(profile==RuleProfile.OSE_ADVANCED_FANTASY)"ADV_POST_MAX_NOT_AUTHORIZED:$classId:$current" else "MAX_LEVEL_REACHED:$classId:$current")
        val next=table.row(current+1) ?: return LevelUpPlanResultRuntime(blocker="MISSING_EVIDENCE:PROGRESSION_ROW:$classId:${current+1}")
        val xp=recipient.xpByClass[classId] ?: 0L
        if(xp<next.xpThreshold) return LevelUpPlanResultRuntime(blocker="XP_BELOW_THRESHOLD:$xp<${next.xpThreshold}")
        val session=experience.activeSessionId ?: return LevelUpPlanResultRuntime(blocker="CANONICAL_SESSION_ID_REQUIRED")
        val sessionKey="$recipientId:$classId"
        if(sessionKey in experience.leveledThisSession) return LevelUpPlanResultRuntime(blocker="ONE_LEVEL_PER_SESSION:$sessionKey:$session")
        val con=recipient.constitutionModifier
        val prev=table.row(current) ?: return LevelUpPlanResultRuntime(blocker="MISSING_EVIDENCE:PROGRESSION_ROW:$classId:$current")
        val hp=hpGain(prev.hitDiceExpression,next.hitDiceExpression,con,recipient.classIds.size,profile,rulesDie)
            ?: return LevelUpPlanResultRuntime(blocker="MISSING_EVIDENCE:LEVEL_HP_GAIN:$recipientId:$classId:${prev.hitDiceExpression}->${next.hitDiceExpression}")
        val following=table.row(current+2)
        val classicCap=if(profile==RuleProfile.OSE_CLASSIC_FANTASY && following!=null && xp>=following.xpThreshold) following.xpThreshold-1L else null
        return LevelUpPlanResultRuntime(LevelUpPlanRuntime(recipientId,classId,current,current+1,next,hp,session,classicCap))
    }

    fun applyLevelUp(experience:ExperienceRuntimeSnapshot, plan:LevelUpPlanRuntime):ExperienceRuntimeSnapshot {
        val recipient=experience.recipients.getValue(plan.recipientId)
        val nextXp=plan.adjustedXpAfterClassicCap ?: (recipient.xpByClass[plan.classId] ?: 0L)
        val applied=AppliedClassProgressionSnapshot(
            classId=plan.classId, level=plan.toLevel, xpThreshold=plan.row.xpThreshold,
            hitDiceExpression=plan.row.hitDiceExpression, thac0=plan.row.thac0, attackBonusAscending=plan.row.attackBonusAscending,
            savingThrows=plan.row.saves, spellSlots=plan.row.spellSlots, sourceId=plan.row.sourceId
        )
        val hpAccum=addFraction(recipient.pendingHpGainNumerator,recipient.pendingHpGainDenominator,plan.hpGain.numerator,plan.hpGain.denominator)
        val updated=recipient.copy(
            levelByClass=recipient.levelByClass+(plan.classId to plan.toLevel),
            xpByClass=recipient.xpByClass+(plan.classId to nextXp),
            progressionByClass=recipient.progressionByClass+(plan.classId to applied),
            pendingHpGainNumerator=hpAccum.first,pendingHpGainDenominator=hpAccum.second
        )
        val record=LevelUpRecordSnapshot(
            levelUpId="LEVEL:${plan.sessionId}:${plan.recipientId}:${plan.classId}:${plan.toLevel}",recipientId=plan.recipientId,classId=plan.classId,
            fromLevel=plan.fromLevel,toLevel=plan.toLevel,hpGainNumerator=plan.hpGain.numerator,hpGainDenominator=plan.hpGain.denominator,
            dieRolled=plan.hpGain.dieRolled,sourceId=plan.row.sourceId,sessionId=plan.sessionId
        )
        return experience.copy(
            recipients=experience.recipients+(plan.recipientId to updated),
            leveledThisSession=experience.leveledThisSession+"${plan.recipientId}:${plan.classId}",
            levelUps=experience.levelUps+record
        ).canonical()
    }

    private fun hpGain(prev:String,next:String,con:Int?,classCount:Int,profile:RuleProfile,rulesDie:(Int)->Int):LevelUpHpGainRuntime? {
        val p=parseHd(prev) ?: return null; val n=parseHd(next) ?: return null
        val denom=if(profile==RuleProfile.OSE_ADVANCED_FANTASY && classCount>1) classCount else 1
        if(n.diceCount==p.diceCount+1 && n.dieSides==p.dieSides && n.fixed==p.fixed) {
            val cm=con ?: return null
            val die=rulesDie(n.dieSides); if(die !in 1..n.dieSides) return null
            val gain=maxOf(1,die+cm)
            return LevelUpHpGainRuntime(gain,denom,die,cm)
        }
        if(n.diceCount==p.diceCount && n.dieSides==p.dieSides && n.fixed>p.fixed) {
            return LevelUpHpGainRuntime(n.fixed-p.fixed,denom,null,null)
        }
        return null
    }
    private data class Hd(val diceCount:Int,val dieSides:Int,val fixed:Int)
    private fun parseHd(v:String):Hd? {
        val m=Regex("""(\d+)d(\d+)(?:\+(\d+))?\*?""").matchEntire(v) ?: return null
        return Hd(m.groupValues[1].toInt(),m.groupValues[2].toInt(),m.groupValues[3].ifBlank{"0"}.toInt())
    }
    private fun addFraction(aNum:Int,aDen:Int,bNum:Int,bDen:Int):Pair<Int,Int>{
        val den=lcm(aDen,bDen); val num=aNum*(den/aDen)+bNum*(den/bDen); val g=gcd(kotlin.math.abs(num),den)
        return (num/g) to (den/g)
    }
    private fun gcd(a0:Int,b0:Int):Int{ var a=a0;var b=b0;while(b!=0){val t=a%b;a=b;b=t};return if(a==0)1 else a }
    private fun lcm(a:Int,b:Int)=a/gcd(a,b)*b
}
