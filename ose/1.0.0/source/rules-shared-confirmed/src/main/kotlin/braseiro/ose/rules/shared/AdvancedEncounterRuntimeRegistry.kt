package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile

data class AdvancedEncounterContent(val type:String,val ref:String,val raw:String,val memberIds:List<String> = emptyList(),val metadata:Map<String,String> = emptyMap())
data class AdvancedDungeonEncounterResult(val tableId:String,val d4:Int,val d10:Int,val content:AdvancedEncounterContent)
data class AdvancedWildernessEncounterResult(val terrain:String,val d8:Int,val d20:Int,val tableId:String,val column:String,val content:AdvancedEncounterContent,val masterFootnote:String? = null)

/** Exact runtime binding of the audited Advanced Referee encounter tables. No name/fuzzy fallback. */
object AdvancedEncounterRuntimeRegistry {
    val PROFILE = RuleProfile.OSE_ADVANCED_FANTASY
    const val SOURCE_DATASET = "OSE_ADVANCED_REFEREE_ENCOUNTER_TABLES_V1"
    const val SOURCE_AUTHORITY = "Advanced Referee v0.3 supplemental table source; monster identities resolve through AdvancedMonsterRuntimeRegistry authority tags"

    private val DUNGEON_DATA = listOf(
        """ADV_DUNGEON_LEVEL_1|1|10|MONSTER_ID|advanced_referee_v0_3.monster.001|Acolyte (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|1|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|2|MONSTER_ID|advanced_referee_v0_3.monster.015|Beetle, Fire (1d8)||count=1d8;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|3|MONSTER_ID|advanced_referee_v0_3.monster.024|Brown Mould (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|4|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|5|MONSTER_ID|advanced_referee_v0_3.monster.043|Coffer Corpse (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|6|MONSTER_ID|advanced_referee_v0_3.monster.050|Dark Creeper (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|7|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild (2d6)||count=2d6;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|8|MONSTER_ID|advanced_referee_v0_3.monster.077|Drow (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|1|9|MONSTER_ID|advanced_referee_v0_3.monster.079|Duergar (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|10|MONSTER_ID|advanced_referee_v0_3.monster.080|Dwarf (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|1|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant (3d6)||count=3d6;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|2|MONSTER_ID|advanced_referee_v0_3.monster.105|Gas Spore (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|3|MONSTER_ID|advanced_referee_v0_3.monster.118|Gnome (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|4|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|5|MONSTER_ID|advanced_referee_v0_3.monster.130|Green Slime (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|6|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|7|MONSTER_ID|advanced_referee_v0_3.monster.136|Halfling (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|8|MONSTER_ID|advanced_referee_v0_3.monster.149|Homunculus (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|2|9|MONSTER_ID|advanced_referee_v0_3.monster.161|Killer Bee (1d10)||count=1d10;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|10|MONSTER_ID|advanced_referee_v0_3.monster.162|Kobold (4d4)||count=4d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|1|MONSTER_ID|advanced_referee_v0_3.monster.167|Leprechaun (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|2|MONSTER_ID|advanced_referee_v0_3.monster.175|Lizard, Gecko (1d3)||count=1d3;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|3|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|4|MONSTER_ID|advanced_referee_v0_3.monster.205|Mycelian (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|5|MONSTER_ID|advanced_referee_v0_3.monster.207|Necrophidius (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|6|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|7|MONSTER_ID|advanced_referee_v0_3.monster.222|Piercer (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|8|MONSTER_ID|advanced_referee_v0_3.monster.225|Poltergeist (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|3|9|MONSTER_ID|advanced_referee_v0_3.monster.244|Rot Grub (5d4)||count=5d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|10|MONSTER_ID|advanced_referee_v0_3.monster.260|Shrew, Giant (1d10)||count=1d10;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|1|MONSTER_ID|advanced_referee_v0_3.monster.262|Skeleton (3d4)||count=3d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|2|MONSTER_ID|advanced_referee_v0_3.monster.270|Snake, Cobra (1d6)||count=1d6;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|3|MONSTER_ID|advanced_referee_v0_3.monster.277|Spider, Crab (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|4|MONSTER_ID|advanced_referee_v0_3.monster.280|Sprite (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|5|MONSTER_ID|advanced_referee_v0_3.monster.283|Stirge (1d10)||count=1d10;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|6|MONSTER_ID|advanced_referee_v0_3.monster.285|Svirfneblin (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|7|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|8|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_1|4|9|MONSTER_FAMILY|family:Wolf|Wolf (2d6)|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|count=2d6;match=EXACT_FAMILY_HEADING;printed_page=132;table=ADV_DUNGEON_LEVEL_1
ADV_DUNGEON_LEVEL_2|1|10|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.003|Ankheg 3 HD (1d2)||count=1d2;parameter=3 HD;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|1|MONSTER_ID|advanced_referee_v0_3.monster.016|Beetle, Oil (1d8)||count=1d8;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|2|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|3|MONSTER_ID|advanced_referee_v0_3.monster.024|Brown Mould (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|4|MONSTER_ID|advanced_referee_v0_3.monster.033|Cat, Mntn. Lion (1d4)||count=1d4;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|5|MONSTER_ID|advanced_referee_v0_3.monster.043|Coffer Corpse (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|6|MONSTER_ID|advanced_referee_v0_3.monster.050|Dark Creeper (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|7|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild (2d6)||count=2d6;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|8|MONSTER_ID|advanced_referee_v0_3.monster.079|Duergar (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|1|9|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|10|MONSTER_ID|advanced_referee_v0_3.monster.103|Frog, Poisonous (1d6)||count=1d6;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|1|MONSTER_ID|advanced_referee_v0_3.monster.105|Gas Spore (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|2|MONSTER_ID|advanced_referee_v0_3.monster.109|Ghoul (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|3|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|4|MONSTER_ID|advanced_referee_v0_3.monster.131|Grey Ooze (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|5|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|6|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|7|MONSTER_ID|advanced_referee_v0_3.monster.149|Homunculus (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|8|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|2|9|MONSTER_ID|advanced_referee_v0_3.monster.173|Lizard, Draco (1d4)||count=1d4;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|10|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|1|MONSTER_ID|advanced_referee_v0_3.monster.205|Mycelian (1d12)||count=1d12;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|2|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal (1d10)||count=1d10;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|3|MONSTER_ID|advanced_referee_v0_3.monster.207|Necrophidius (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|4|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|5|MONSTER_ID|advanced_referee_v0_3.monster.222|Piercer (3d8)||count=3d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|6|MONSTER_ID|advanced_referee_v0_3.monster.224|Pixie (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|7|MONSTER_ID|advanced_referee_v0_3.monster.225|Poltergeist (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|8|MONSTER_ID|advanced_referee_v0_3.monster.226|Pseudo-Dragon (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|3|9|MONSTER_ID|advanced_referee_v0_3.monster.238|Robber Fly (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|10|MONSTER_ID|advanced_referee_v0_3.monster.242|Rock Baboon (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|1|MONSTER_ID|advanced_referee_v0_3.monster.244|Rot Grub (5d6)||count=5d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|2|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper (1d8)||count=1d8;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|3|MONSTER_ID|advanced_referee_v0_3.monster.276|Spider, Bl. Widow (1d3)||count=1d3;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|4|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous (1d8)||count=1d8;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|5|MONSTER_ID|advanced_referee_v0_3.monster.298|Troglodyte (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|6|MONSTER_ID|advanced_referee_v0_3.monster.305|Veteran (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|7|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|8|MONSTER_ID|advanced_referee_v0_3.monster.309|Water Fiend (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_2|4|9|MONSTER_ID|advanced_referee_v0_3.monster.327|Zombie (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_2
ADV_DUNGEON_LEVEL_3|1|10|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.003|Ankheg 3 HD (1d6)||count=1d6;parameter=3 HD;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|1|MONSTER_ID|advanced_referee_v0_3.monster.004|Ape, White (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|2|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adv. (1d4+4)||count=1d4+4;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|3|MONSTER_ID|advanced_referee_v0_3.monster.017|Beetle, Tiger (1d6)||count=1d6;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|4|MONSTER_ID|advanced_referee_v0_3.monster.024|Brown Mould (1d10)||count=1d10;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|5|MONSTER_ID|advanced_referee_v0_3.monster.026|Bugbear (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|6|MONSTER_ID|advanced_referee_v0_3.monster.030|Carcass Crawler (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|7|MONSTER_ID|advanced_referee_v0_3.monster.043|Coffer Corpse (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|8|MONSTER_ID|advanced_referee_v0_3.monster.050|Dark Creeper (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|1|9|MONSTER_ID|advanced_referee_v0_3.monster.051|Deep One (2d12)||count=2d12;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|10|MONSTER_ID|advanced_referee_v0_3.monster.060|Doppelgänger (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|1|MONSTER_ID|advanced_referee_v0_3.monster.076|Driver Ant (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|2|MONSTER_ID|advanced_referee_v0_3.monster.077|Drow (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|3|MONSTER_ID|advanced_referee_v0_3.monster.079|Duergar (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|4|MONSTER_ID|advanced_referee_v0_3.monster.101|Flail Snail (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|5|MONSTER_ID|advanced_referee_v0_3.monster.104|Gargoyle (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|6|MONSTER_ID|advanced_referee_v0_3.monster.105|Gas Spore (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|7|MONSTER_ID|advanced_referee_v0_3.monster.106|Gelatinous Cube (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|8|MONSTER_ID|advanced_referee_v0_3.monster.107|Ghast (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|2|9|MONSTER_ID|advanced_referee_v0_3.monster.137|Harpy (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|10|MONSTER_ID|advanced_referee_v0_3.monster.170|Lvng. Stat., Crystal (1d6)||count=1d6;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|1|MONSTER_ID|advanced_referee_v0_3.monster.186|Lycan., Wererat (1d8)||count=1d8;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|2|MONSTER_ID|advanced_referee_v0_3.monster.194|Medium (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|3|MONSTER_ID|advanced_referee_v0_3.monster.195|Medusa (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|4|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid (2d8)||count=2d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|5|MONSTER_ID|advanced_referee_v0_3.monster.205|Mycelian (2d8)||count=2d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|6|MONSTER_ID|advanced_referee_v0_3.monster.207|Necrophidius (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|7|MONSTER_ID|advanced_referee_v0_3.monster.213|Ochre Jelly (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|8|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|3|9|MONSTER_ID|advanced_referee_v0_3.monster.222|Piercer (4d6)||count=4d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|10|MONSTER_ID|advanced_referee_v0_3.monster.225|Poltergeist (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|1|MONSTER_ID|advanced_referee_v0_3.monster.244|Rot Grub (5d6)||count=5d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|2|MONSTER_ID|advanced_referee_v0_3.monster.255|Shadow (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|3|MONSTER_ID|advanced_referee_v0_3.monster.279|Spider, Tarantella (1d3)||count=1d3;match=TOKEN_FAMILY_MATCH;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|4|MONSTER_ID|advanced_referee_v0_3.monster.285|Svirfneblin (2d8)||count=2d8;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|5|MONSTER_ID|advanced_referee_v0_3.monster.287|Thoul (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|6|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous (2d6)||count=2d6;match=EXACT_TOKEN_SET;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|7|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|8|MONSTER_ID|advanced_referee_v0_3.monster.309|Water Fiend (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_3|4|9|MONSTER_ID|advanced_referee_v0_3.monster.317|Wight (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=132;table=ADV_DUNGEON_LEVEL_3
ADV_DUNGEON_LEVEL_4_5|1|10|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.003|Ankheg 5 HD (1d6)||count=1d6;parameter=5 HD;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|1|MONSTER_ID|advanced_referee_v0_3.monster.012|Bear, Cave (1d2)||count=1d2;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|2|MONSTER_ID|advanced_referee_v0_3.monster.020|Blink Dog (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|3|MONSTER_ID|advanced_referee_v0_3.monster.028|Caecilia (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|4|MONSTER_ID|advanced_referee_v0_3.monster.031|Caryatid Column (1d12)||count=1d12;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|5|MONSTER_ID|advanced_referee_v0_3.monster.042|Cockatrice (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|6|MONSTER_ID|advanced_referee_v0_3.monster.051|Deep One (3d12)||count=3d12;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|7|MONSTER_ID|advanced_referee_v0_3.monster.054|Disenchanter (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|8|MONSTER_ID|advanced_referee_v0_3.monster.060|Doppelgänger (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|1|9|MONSTER_ID|advanced_referee_v0_3.monster.077|Drow (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|10|MONSTER_ID|advanced_referee_v0_3.monster.079|Duergar (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|1|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adv. (1d6+3)||count=1d6+3;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|2|MONSTER_ID|advanced_referee_v0_3.monster.101|Flail Snail (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|3|MONSTER_ID|advanced_referee_v0_3.monster.107|Ghast (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|4|MONSTER_ID|advanced_referee_v0_3.monster.116|Gibbering Mouther (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|5|MONSTER_ID|advanced_referee_v0_3.monster.131|Grey Ooze (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|6|MONSTER_ID|advanced_referee_v0_3.monster.140|Hellhound (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|7|MONSTER_ID|advanced_referee_v0_3.monster.150|Hook Beast (2d6)||count=2d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|8|MONSTER_ID|advanced_referee_v0_3.monster.163|Krell (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|2|9|MONSTER_ID|advanced_referee_v0_3.monster.179|Lizard, Tuatara (1d2)||count=1d2;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|10|MONSTER_ID|advanced_referee_v0_3.monster.185|Lycan., Wereboar (1d4)||count=1d4;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|1|MONSTER_ID|advanced_referee_v0_3.monster.188|Lycan., Werewolf (1d6)||count=1d6;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|2|MONSTER_ID|advanced_referee_v0_3.monster.201|Minotaur (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|3|MONSTER_ID|advanced_referee_v0_3.monster.213|Ochre Jelly (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|4|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.217|Otyugh 6 HD (1)||count=1;parameter=6 HD;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|5|MONSTER_ID|advanced_referee_v0_3.monster.218|Owl Bear (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|6|MONSTER_ID|advanced_referee_v0_3.monster.235|Rhagodessa (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|7|MONSTER_ID|advanced_referee_v0_3.monster.245|Rust Monster (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|8|MONSTER_ID|advanced_referee_v0_3.monster.263|Slithering Tracker (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|3|9|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|10|MONSTER_ID|advanced_referee_v0_3.monster.271|Sp. of the Worm (1d3)||count=1d3;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|1|MONSTER_ID|advanced_referee_v0_3.monster.272|Spectre (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|2|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|3|MONSTER_ID|advanced_referee_v0_3.monster.291|Toad, Frost (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|4|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|5|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|6|MONSTER_ID|advanced_referee_v0_3.monster.309|Water Fiend (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|7|MONSTER_ID|advanced_referee_v0_3.monster.313|Weasel, Giant (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|8|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o’-the-Wisp (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_4_5|4|9|MONSTER_ID|advanced_referee_v0_3.monster.322|Wraith (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_4_5
ADV_DUNGEON_LEVEL_6_7|1|10|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|1|MONSTER_ID|advanced_referee_v0_3.monster.006|Banshee (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|2|MONSTER_ID|advanced_referee_v0_3.monster.007|Basilisk (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|3|MONSTER_ID|advanced_referee_v0_3.monster.012|Bear, Cave (1d2)||count=1d2;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|4|MONSTER_ID|advanced_referee_v0_3.monster.019|Black Pudding (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|5|MONSTER_ID|advanced_referee_v0_3.monster.028|Caecilia (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|6|MONSTER_ID|advanced_referee_v0_3.monster.031|Caryatid Column (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|7|MONSTER_ID|advanced_referee_v0_3.monster.071|Dragon, White (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|8|MONSTER_ID|advanced_referee_v0_3.monster.075|Drider (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|1|9|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adv. (1d6+3)||count=1d6+3;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|10|MONSTER_ID|advanced_referee_v0_3.monster.101|Flail Snail (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|1|MONSTER_ID|advanced_referee_v0_3.monster.107|Ghast (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|2|MONSTER_ID|advanced_referee_v0_3.monster.116|Gibbering Mouther (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|3|MONSTER_ID|advanced_referee_v0_3.monster.128|Gorgon (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|4|MONSTER_ID|advanced_referee_v0_3.monster.140|Hellhound (2d4)||count=2d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|5|MONSTER_ID|advanced_referee_v0_3.monster.150|Hook Beast (3d6)||count=3d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|6|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.156|Hydra, 1d4+4HD (1)||count=1;parameter=4 HD;source_correction=BASE_HYDRA_EXPLICIT;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|7|MONSTER_ID|advanced_referee_v0_3.monster.163|Krell (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|8|MONSTER_ID|advanced_referee_v0_3.monster.174|Lizard, Flame (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|2|9|MONSTER_ID|advanced_referee_v0_3.monster.178|Lizard, Subterr. (1d6)||count=1d6;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|10|MONSTER_ID|advanced_referee_v0_3.monster.187|Lycan., Weretiger (1d4)||count=1d4;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|1|MONSTER_ID|advanced_referee_v0_3.monster.200|Mind Lasher (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|2|MONSTER_ID|advanced_referee_v0_3.monster.201|Minotaur (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|3|MONSTER_ID|advanced_referee_v0_3.monster.203|Mummy (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|4|MONSTER_ID|advanced_referee_v0_3.monster.213|Ochre Jelly (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|5|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.217|Otyugh 6 HD (1d3)||count=1d3;parameter=6 HD;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|6|MONSTER_ID|advanced_referee_v0_3.monster.218|Owl Bear (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|7|MONSTER_ID|advanced_referee_v0_3.monster.230|Rakshasa (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|8|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.233|Remorhaz 7 HD (1)||count=1;parameter=7 HD;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|3|9|MONSTER_ID|advanced_referee_v0_3.monster.234|Revenant (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|10|MONSTER_ID|advanced_referee_v0_3.monster.245|Rust Monster (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|1|MONSTER_ID|advanced_referee_v0_3.monster.247|Salam., Flame (1d4+1)||count=1d4+1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|2|MONSTER_ID|advanced_referee_v0_3.monster.250|Scorpion, Giant (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|3|MONSTER_ID|advanced_referee_v0_3.monster.263|Slithering Tracker (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|4|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person (1d6)||count=1d6;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|5|MONSTER_ID|advanced_referee_v0_3.monster.272|Spectre (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|6|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|7|MONSTER_ID|advanced_referee_v0_3.monster.307|Warp Beast (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|8|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o’-the-Wisp (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_6_7|4|9|MONSTER_ID|advanced_referee_v0_3.monster.324|Xorn (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_6_7
ADV_DUNGEON_LEVEL_8_PLUS|1|10|MONSTER_ID|advanced_referee_v0_3.monster.019|Black Pudding (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|1|MONSTER_ID|advanced_referee_v0_3.monster.041|Chimera (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|2|MONSTER_ID|advanced_referee_v0_3.monster.061|Dragon, Black (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|3|MONSTER_ID|advanced_referee_v0_3.monster.062|Dragon, Blue (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|4|MONSTER_ID|advanced_referee_v0_3.monster.063|Dragon, Brass (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|5|MONSTER_ID|advanced_referee_v0_3.monster.064|Dragon, Bronze (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|6|MONSTER_ID|advanced_referee_v0_3.monster.065|Dragon, Copper (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|7|MONSTER_ID|advanced_referee_v0_3.monster.066|Dragon, Gold (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|8|MONSTER_ID|advanced_referee_v0_3.monster.067|Dragon, Green (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|1|9|MONSTER_ID|advanced_referee_v0_3.monster.068|Dragon, Red (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|10|MONSTER_ID|advanced_referee_v0_3.monster.070|Dragon, Silver (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|1|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|2|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adv. (1d6+3)||count=1d6+3;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|3|MONSTER_ID|advanced_referee_v0_3.monster.090|Eye of Terror (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|4|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|5|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill (1d4)||count=1d4;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|6|MONSTER_ID|advanced_referee_v0_3.monster.114|Giant, Stone (1d2)||count=1d2;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|7|MONSTER_ID|advanced_referee_v0_3.monster.120|Golem, Amber (1)||count=1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|8|MONSTER_ID|advanced_referee_v0_3.monster.121|Golem, Bone (1)||count=1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|2|9|MONSTER_ID|advanced_referee_v0_3.monster.125|Golem, Iron (1)||count=1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|10|MONSTER_ID|advanced_referee_v0_3.monster.126|Golem, Stone (1)||count=1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|1|MONSTER_ID|advanced_referee_v0_3.monster.134|Hag, Black (1)||count=1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|2|MONSTER_ID|advanced_referee_v0_3.monster.155|Hulker (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|3|PARAMETERIZED_REFERENCE|advanced_referee_v0_3.monster.156|Hydra, 1d4+8HD (1)||count=1;parameter=8 HD;source_correction=BASE_HYDRA_EXPLICIT;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|4|MONSTER_ID|advanced_referee_v0_3.monster.164|Lamia (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|5|MONSTER_ID|advanced_referee_v0_3.monster.182|Lurker Above (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|6|MONSTER_ID|advanced_referee_v0_3.monster.183|Lycan., Devil Swine (1d3)||count=1d3;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|7|MONSTER_ID|advanced_referee_v0_3.monster.184|Lycan., Werebear (1d4)||count=1d4;match=TOKEN_FAMILY_MATCH;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|8|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|3|9|MONSTER_ID|advanced_referee_v0_3.monster.199|Mimic (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|10|MONSTER_ID|advanced_referee_v0_3.monster.200|Mind Lasher (1d8)||count=1d8;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|1|MONSTER_ID|advanced_referee_v0_3.monster.229|Purple Worm (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|2|MONSTER_ID|advanced_referee_v0_3.monster.234|Revenant (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|3|MONSTER_ID|advanced_referee_v0_3.monster.243|Roper (1d2)||count=1d2;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|4|MONSTER_ID|advanced_referee_v0_3.monster.247|Salam., Flame (1d4+1)||count=1d4+1;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|5|MONSTER_ID|advanced_referee_v0_3.monster.248|Salamander, Frost (1d3)||count=1d3;match=EXACT_TOKEN_SET;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|6|MONSTER_ID|advanced_referee_v0_3.monster.256|Shambling Mound (1d3)||count=1d3;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|7|MONSTER_ID|advanced_referee_v0_3.monster.264|Slug, Giant (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|8|MONSTER_ID|advanced_referee_v0_3.monster.294|Trapper (1)||count=1;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS
ADV_DUNGEON_LEVEL_8_PLUS|4|9|MONSTER_ID|advanced_referee_v0_3.monster.304|Vampire (1d4)||count=1d4;match=EXACT_NORMALIZED_NAME;printed_page=133;table=ADV_DUNGEON_LEVEL_8_PLUS"""
    )

    private val MASTER_DATA = listOf(
        """BARREN_HILLS_MOUNTAINS|1|Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|ANIMAL|
CITY|1|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|1|Wilderness Encounter Sub-Table G: Clear, Grasslands|ANIMAL|
DESERT|1|Wilderness Encounter Sub-Table D: Desert|ANIMAL|
FOREST|1|Wilderness Encounter Sub-Table F: Forest|ANIMAL|
BARREN_HILLS_MOUNTAINS|2|Wilderness Encounter Sub-Table 1|DRAGON|
CITY|2|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|2|Wilderness Encounter Sub-Table 1|DRAGON|
DESERT|2|Wilderness Encounter Sub-Table D: Desert|ANIMAL|
FOREST|2|Wilderness Encounter Sub-Table 1|DRAGON|
BARREN_HILLS_MOUNTAINS|3|Wilderness Encounter Sub-Table 1|DRAGON|
CITY|3|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|3|Wilderness Encounter Sub-Table 1|FLYER|
DESERT|3|Wilderness Encounter Sub-Table 1|DRAGON|
FOREST|3|Wilderness Encounter Sub-Table 1|FLYER|
BARREN_HILLS_MOUNTAINS|4|Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|HUMAN|
CITY|4|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|4|Wilderness Encounter Sub-Table G: Clear, Grasslands|HUMAN|
DESERT|4|Wilderness Encounter Sub-Table D: Desert|HUMAN|
FOREST|4|Wilderness Encounter Sub-Table F: Forest|HUMAN|
BARREN_HILLS_MOUNTAINS|5|Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|HUMANOID|
CITY|5|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|5|Wilderness Encounter Sub-Table G: Clear, Grasslands|HUMANOID|
DESERT|5|Wilderness Encounter Sub-Table D: Desert|HUMAN|
FOREST|5|Wilderness Encounter Sub-Table F: Forest|HUMANOID|
BARREN_HILLS_MOUNTAINS|6|Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|HUMANOID|
CITY|6|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_CITY|
CLEAR_GRASSLANDS|6|Wilderness Encounter Sub-Table 1|INSECT|
DESERT|6|Wilderness Encounter Sub-Table D: Desert|HUMANOID|
FOREST|6|Wilderness Encounter Sub-Table 1|INSECT|
BARREN_HILLS_MOUNTAINS|7|Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|MONSTER|
CITY|7|Wilderness Encounter Sub-Table C: City, Settled|HUMANOID|
CLEAR_GRASSLANDS|7|Wilderness Encounter Sub-Table G: Clear, Grasslands|MONSTER|
DESERT|7|Wilderness Encounter Sub-Table D: Desert|MONSTER|
FOREST|7|Wilderness Encounter Sub-Table F: Forest|MONSTER|
BARREN_HILLS_MOUNTAINS|8|Wilderness Encounter Sub-Table 2|UNUSUAL|
CITY|8|Wilderness Encounter Sub-Table 2|UNDEAD|
CLEAR_GRASSLANDS|8|Wilderness Encounter Sub-Table 2|UNUSUAL|
DESERT|8|Wilderness Encounter Sub-Table 2|UNDEAD|
FOREST|8|Wilderness Encounter Sub-Table 2|UNUSUAL|
JUNGLE|1|Wilderness Encounter Sub-Table J: Jungle|ANIMAL|
LAKE_RIVER|1|Wilderness Encounter Sub-Table L: Lake, River|ANIMAL|
OCEAN_SEA|1|Wilderness Encounter Sub-Table 1|DRAGON|Re-roll at sea.
SETTLED|1|Wilderness Encounter Sub-Table C: City, Settled|ANIMAL|
SWAMP|1|Wilderness Encounter Sub-Table 1|DRAGON|
JUNGLE|2|Wilderness Encounter Sub-Table 1|DRAGON|
LAKE_RIVER|2|Wilderness Encounter Sub-Table 1|DRAGON|
OCEAN_SEA|2|Wilderness Encounter Sub-Table 1|FLYER|
SETTLED|2|Wilderness Encounter Sub-Table 1|DRAGON|
SWAMP|2|Wilderness Encounter Sub-Table 1|FLYER|
JUNGLE|3|Wilderness Encounter Sub-Table 1|FLYER|
LAKE_RIVER|3|Wilderness Encounter Sub-Table 1|FLYER|
OCEAN_SEA|3|Wilderness Sub-Table O: Ocean, Sea|HUMAN|
SETTLED|3|Wilderness Encounter Sub-Table 1|FLYER|
SWAMP|3|Wilderness Encounter Sub-Table S: Swamp|HUMAN|
JUNGLE|4|Wilderness Encounter Sub-Table J: Jungle|HUMAN|
LAKE_RIVER|4|Wilderness Encounter Sub-Table L: Lake, River|HUMAN|
OCEAN_SEA|4|Wilderness Sub-Table O: Ocean, Sea|SWIMMER|
SETTLED|4|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_SETTLED|
SWAMP|4|Wilderness Encounter Sub-Table S: Swamp|HUMANOID|
JUNGLE|5|Wilderness Encounter Sub-Table J: Jungle|HUMANOID|
LAKE_RIVER|5|Wilderness Encounter Sub-Table L: Lake, River|HUMANOID|
OCEAN_SEA|5|Wilderness Sub-Table O: Ocean, Sea|SWIMMER|
SETTLED|5|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_SETTLED|
SWAMP|5|Wilderness Encounter Sub-Table 1|INSECT|
JUNGLE|6|Wilderness Encounter Sub-Table 1|INSECT|
LAKE_RIVER|6|Wilderness Encounter Sub-Table 1|INSECT|
OCEAN_SEA|6|Wilderness Sub-Table O: Ocean, Sea|SWIMMER|
SETTLED|6|Wilderness Encounter Sub-Table C: City, Settled|HUMAN_SETTLED|
SWAMP|6|Wilderness Encounter Sub-Table S: Swamp|SWIMMER|
JUNGLE|7|Wilderness Encounter Sub-Table 1|INSECT|
LAKE_RIVER|7|Wilderness Encounter Sub-Table L: Lake, River|SWIMMER|
OCEAN_SEA|7|Wilderness Sub-Table O: Ocean, Sea|SWIMMER|
SETTLED|7|Wilderness Encounter Sub-Table C: City, Settled|HUMANOID|
SWAMP|7|Wilderness Encounter Sub-Table S: Swamp|MONSTER|
JUNGLE|8|Wilderness Encounter Sub-Table J: Jungle|MONSTER|
LAKE_RIVER|8|Wilderness Encounter Sub-Table L: Lake, River|SWIMMER|
OCEAN_SEA|8|Wilderness Sub-Table O: Ocean, Sea|SWIMMER|
SETTLED|8|Wilderness Encounter Sub-Table 1|INSECT|
SWAMP|8|Wilderness Encounter Sub-Table 2|UNDEAD|"""
    )

    private val DIRECT_DATA = listOf(
        """Wilderness Encounter Sub-Table 1|1|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.041|Chimera||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|1|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.042|Cockatrice||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|1|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.003|Ankheg||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|2|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.061|Dragon, Black||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|2|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.044|Couatl||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|2|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.015|Beetle, Fire||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|3|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.062|Dragon, Blue||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|3|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.104|Gargoyle||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|3|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.016|Beetle, Oil||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|4|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.063|Dragon, Brass||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|4|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.132|Griffon||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|4|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.017|Beetle, Tiger||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|5|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.064|Dragon, Bronze||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|5|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.138|Hawk, Giant||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|5|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.040|Centipede, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|6|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.065|Dragon, Copper||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|6|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.145|Hippogriff||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|6|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.076|Driver Ant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|7|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.066|Dragon, Gold||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|7|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.161|Killer Bee||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|7|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.076|Driver Ant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|8|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.067|Dragon, Green||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|8|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.192|Mantis, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|8|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.101|Flail Snail||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|9|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.068|Dragon, Red||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|9|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.208|Nightmare||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|9|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.161|Killer Bee||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|10|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.070|Dragon, Silver||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|10|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.219|Pegasus||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|10|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.192|Mantis, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|11|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.071|Dragon, White||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|11|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.220|Peryton||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|11|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.235|Rhagodessa||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|12|DRAGON|SOURCE_CHOICE|advanced_referee_v0_3.monster.156|Hydra (or Sea Hydra)|advanced_referee_v0_3.monster.156|choice_count=2;contains_unresolved=true;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|12|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.221|Phoenix||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|12|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.238|Robber Fly||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|13|DRAGON|SOURCE_CHOICE|advanced_referee_v0_3.monster.156|Hydra (or Sea Hydra)|advanced_referee_v0_3.monster.156|choice_count=2;contains_unresolved=true;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|13|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.224|Pixie||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|13|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.244|Rot Grub||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|14|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.226|Pseudo-Dragon||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|14|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.226|Pseudo-Dragon||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|14|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.250|Scorpion, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|15|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.323|Wyvern||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|15|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.238|Robber Fly||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|15|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.264|Slug, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|16|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena*||footnote=true;match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|16|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.241|Roc, Small||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|16|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.276|Spider, Black Widow||match=TOKEN_FAMILY_MATCH;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|17|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.007|Basilisk*||footnote=true;match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|17|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.273|Sphinx||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|17|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.277|Spider, Crab||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|18|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne*||footnote=true;match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|18|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.280|Sprite||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|18|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase||match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|19|DRAGON|MONSTER_ID|advanced_referee_v0_3.monster.174|Lizard, Flame*||footnote=true;match=EXACT_TOKEN_SET;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|19|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.283|Stirge||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|19|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.279|Spider, Tarantella||match=TOKEN_FAMILY_MATCH;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|20|DRAGON|MONSTER_FAMILY|family:Salamander|Salamander*|advanced_referee_v0_3.monster.247,advanced_referee_v0_3.monster.248|footnote=true;match=EXACT_FAMILY_HEADING;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|20|FLYER|MONSTER_ID|advanced_referee_v0_3.monster.308|Wasp, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 1|20|INSECT|MONSTER_ID|advanced_referee_v0_3.monster.308|Wasp, Giant||match=EXACT_NORMALIZED_NAME;printed_page=134;table=Wilderness Encounter Sub-Table 1
Wilderness Encounter Sub-Table 2|1|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.012|Bear, Cave||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|1|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.006|Banshee||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|1|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.007|Basilisk||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|2|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.012|Bear, Cave||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|2|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.043|Coffer Corpse||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|2|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.020|Blink Dog||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|3|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.035|Cat, Sabre-Toothed||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|3|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.043|Coffer Corpse||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|3|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.027|Bulette||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|4|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.035|Cat, Sabre-Toothed||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|4|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.107|Ghast||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|4|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.037|Catoblepas||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|5|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.046|Crocodile, Giant||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|5|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|5|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.039|Centaur||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|6|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.046|Crocodile, Giant||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|6|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|6|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.128|Gorgon||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|7|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.129|Gorilla||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|7|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.109|Ghoul||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|7|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.159|Jackalwere||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|8|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.146|Hippo, Behemoth||match=SOURCE_ALIAS;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|8|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.109|Ghoul||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|8|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.164|Lamia||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|9|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.193|Mastodon||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|9|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.203|Mummy||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|9|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.168|Leucrocotta||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|10|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.227|Pterosaur, Pteranodon||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|10|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.207|Necrophidius||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|10|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.184|Lycanthrope, Werebear||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|11|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.228|Pterosaur, Pterodactyl||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|11|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.225|Poltergeist||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|11|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.185|Lycanthrope, Wereboar||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|12|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.237|Rhino, Woolly||match=SOURCE_ALIAS;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|12|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.234|Revenant||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|12|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.186|Lycanthrope, Wererat||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|13|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.237|Rhino, Woolly||match=SOURCE_ALIAS;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|13|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.262|Skeleton||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|13|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.187|Lycanthrope, Weretiger||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|14|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|14|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.262|Skeleton||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|14|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.188|Lycanthrope, Werewolf||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|15|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.282|Stegosaurus||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|15|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.272|Spectre||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|15|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.195|Medusa||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|16|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.289|Titanothere||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|16|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.304|Vampire||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|16|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.217|Otyugh||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|17|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.296|Triceratops||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|17|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.317|Wight||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|17|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.243|Roper||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|18|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.302|Tyrannosaurus Rex||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|18|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.322|Wraith||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|18|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.256|Shambling Mound||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|19|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.319|Wolf, Dire||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|19|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.327|Zombie||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|19|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.295|Treant||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|20|PREHISTORIC_ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.326|Yeti||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|20|UNDEAD|MONSTER_ID|advanced_referee_v0_3.monster.327|Zombie||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table 2|20|UNUSUAL|MONSTER_ID|advanced_referee_v0_3.monster.307|Warp Beast||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table 2
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.004|Ape, White||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.080|Dwarf||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.104|Gargoyle||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.004|Ape, White||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.089|Ettin||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.132|Griffon||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.012|Bear, Cave||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.110|Giant, Cloud||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.137|Harpy||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.033|Cat, Mntn. Lion||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|4|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.112|Giant, Frost||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|4|MONSTER|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|5|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.138|Hawk, Giant||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|6|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.114|Giant, Stone||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.145|Hippogriff||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.129|Gorilla||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.115|Giant, Storm||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|7|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.164|Lamia||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|8|ANIMAL|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.118|Gnome||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.168|Leucrocotta||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|9|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|9|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|10|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.174|Lizard, Flame||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.134|Hag, Black||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|11|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.178|Lizard, Subterr.||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|11|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.159|Jackalwere||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.208|Nightmare||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.202|Mule||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.162|Kobold||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.219|Pegasus||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.242|Rock Baboon||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|13|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.220|Peryton||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|14|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.220|Peryton||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.266|Snake, Rattler||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|15|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.251|Scorpionoid||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.239|Roc, Giant||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|16|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.285|Svirfneblin||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.240|Roc, Large||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.308|Wasp, Giant||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.288|Titan||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.241|Roc, Small||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|18|ANIMAL|MONSTER_FAMILY|family:Wolf|Wolf|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|match=EXACT_FAMILY_HEADING;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.298|Troglodyte||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.273|Sphinx||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|19|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.319|Wolf, Dire||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o’-the-Wisp||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|20|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.321|Wolf, Winter||match=EXACT_TOKEN_SET;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal||match=TOKEN_FAMILY_MATCH;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.326|Yeti||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table B: Barren, Hills, Mountains|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.324|Xorn||match=EXACT_NORMALIZED_NAME;printed_page=135;table=Wilderness Encounter Sub-Table B: Barren, Hills, Mountains
Wilderness Encounter Sub-Table C: City, Settled|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|1|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.001|Acolyte||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|1|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.001|Acolyte||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|2|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.001|Acolyte||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|2|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.001|Acolyte||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.080|Dwarf||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled""",
        """Wilderness Encounter Sub-Table C: City, Settled|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.036|Cat, Tiger||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|3|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|3|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.080|Dwarf||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.057|Dog, Hunting||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|4|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|4|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.057|Dog, Hunting||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|5|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|5|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|6|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.058|Dog, War||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|6|HUMAN_CITY|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|6|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.092|Ferret, Giant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|7|HUMAN_CITY|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|7|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|8|ANIMAL|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|8|HUMAN_CITY|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|8|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.118|Gnome||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|9|ANIMAL|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|9|HUMAN_CITY|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|9|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.118|Gnome||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|10|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|10|HUMAN_CITY|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|10|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|11|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|11|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.194|Medium||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|11|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|12|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|12|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|12|HUMAN_SETTLED|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.136|Halfling||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.231|Rat, Giant||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|13|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|13|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.194|Medium||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.136|Halfling||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.231|Rat, Giant||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|14|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|14|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.260|Shrew, Giant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|15|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|15|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.167|Leprechaun||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|16|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|16|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.279|Spider, Tarantella||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|17|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|17|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|18|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.313|Weasel, Giant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|18|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|18|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|19|ANIMAL|MONSTER_FAMILY|family:Wolf|Wolf|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|19|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|19|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.224|Pixie||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|20|ANIMAL|MONSTER_FAMILY|family:Wolf|Wolf|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|20|HUMAN_CITY|MONSTER_ID|advanced_referee_v0_3.monster.305|Veteran||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|20|HUMAN_SETTLED|MONSTER_ID|advanced_referee_v0_3.monster.305|Veteran||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table C: City, Settled|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.280|Sprite||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table C: City, Settled
Wilderness Encounter Sub-Table D: Desert|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.029|Camel||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.053|Dervish||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.111|Giant, Fire||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.029|Camel||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.053|Dervish||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.027|Bulette||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.029|Camel||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.053|Dervish||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.032|Cat, Lion||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|4|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|4|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.104|Gargoyle||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.032|Cat, Lion||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|5|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.104|Gargoyle||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|6|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.032|Cat, Lion||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|6|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.159|Jackalwere||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.132|Griffon||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild Dog||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|7|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.191|Mantid||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|7|MONSTER|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|8|ANIMAL|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|8|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.138|Hawk, Giant||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|9|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|9|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.138|Hawk, Giant||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|10|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.164|Lamia||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|11|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|11|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.168|Leucrocotta||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.175|Lizard, Gecko||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|12|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.177|Lizard, Monitor||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|13|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.179|Lizard, Tuatara||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|14|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.192|Mantis, Giant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|15|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.224|Pixie||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.239|Roc, Giant||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|16|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.230|Rakshasa||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.240|Roc, Large||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.266|Snake, Rattler||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.251|Scorpionoid||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.241|Roc, Small||match=EXACT_TOKEN_SET;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|18|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.266|Snake, Rattler||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.273|Sphinx||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|19|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.279|Spider, Tarantella||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.280|Sprite||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.273|Sphinx||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|20|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.279|Spider, Tarantella||match=TOKEN_FAMILY_MATCH;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.287|Thoul||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table D: Desert|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.308|Wasp, Giant||match=EXACT_NORMALIZED_NAME;printed_page=136;table=Wilderness Encounter Sub-Table D: Desert
Wilderness Encounter Sub-Table F: Forest|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.013|Bear, Grizzly||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.037|Catoblepas||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.026|Bugbear||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.039|Centaur||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.034|Cat, Panther||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.049|Cyclops||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.042|Cockatrice||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.036|Cat, Tiger||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|4|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.078|Dryad||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|4|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.105|Gas Spore||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild Dog||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|5|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|6|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.089|Ettin||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.116|Gibbering Mouther||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.103|Frog, Poisonous||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|7|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|8|ANIMAL|MONSTER_FAMILY|family:Hawk|Hawk|advanced_referee_v0_3.monster.138,advanced_referee_v0_3.monster.139|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.217|Otyugh||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|9|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|9|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.218|Owl Bear||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|10|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.175|Lizard, Gecko||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|10|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.220|Peryton||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|11|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.179|Lizard, Tuatara||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|11|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.134|Hag, Black||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.225|Poltergeist||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.226|Pseudo-Dragon||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.277|Spider, Crab||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|13|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.167|Leprechaun||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.243|Roper||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.277|Spider, Crab||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|14|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.256|Shambling Mound||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest""",
        """Wilderness Encounter Sub-Table F: Forest|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|15|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.263|Slithering Tracker||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|16|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.275|Spider, Aranea||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.303|Unicorn||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.295|Treant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|18|ANIMAL|MONSTER_FAMILY|family:Wolf|Wolf|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.249|Satyr||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|19|ANIMAL|MONSTER_FAMILY|family:Wolf|Wolf|advanced_referee_v0_3.monster.319,advanced_referee_v0_3.monster.320,advanced_referee_v0_3.monster.321|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.287|Thoul||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o’-the-Wisp||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|20|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.319|Wolf, Dire||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table F: Forest|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.323|Wyvern||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table F: Forest
Wilderness Encounter Sub-Table G: Clear, Grasslands|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.003|Ankheg||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.026|Bugbear||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.032|Cat, Lion||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.020|Blink Dog||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.059|Dog, Wild||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|4|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|4|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.020|Blink Dog||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.087|Elephant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|5|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.027|Bulette||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|6|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.092|Ferret, Giant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.027|Bulette||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.138|Hawk, Giant||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.136|Halfling||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|7|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|8|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|8|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.073|Dragonne||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|9|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|9|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.159|Jackalwere||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.128|Gorgon||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|10|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.147|Hippo, Normal||match=SOURCE_ALIAS;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.167|Leprechaun||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.132|Griffon||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|11|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.154|Horse, Wild||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|11|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.191|Mantid||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.132|Griffon||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.174|Lizard, Flame||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.137|Harpy||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.192|Mantis, Giant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|13|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.145|Hippogriff||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.202|Mule||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|14|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.168|Leucrocotta||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.242|Rock Baboon||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|15|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.224|Pixie||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.190|Manticore||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|16|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.230|Rakshasa||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.201|Minotaur||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.266|Snake, Rattler||match=TOKEN_FAMILY_MATCH;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.210|Noble||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.208|Nightmare||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|18|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase||match=EXACT_TOKEN_SET;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.287|Thoul||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.219|Pegasus||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|19|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.308|Wasp, Giant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.211|Nomad||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.288|Titan||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.307|Warp Beast||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|20|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.313|Weasel, Giant||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table G: Clear, Grasslands|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.323|Wyvern||match=EXACT_NORMALIZED_NAME;printed_page=137;table=Wilderness Encounter Sub-Table G: Clear, Grasslands
Wilderness Encounter Sub-Table J: Jungle|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.026|Bugbear||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.002|Amphisbaena||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.034|Cat, Panther||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.049|Cyclops||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.007|Basilisk||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.087|Elephant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.028|Caecilia||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.103|Frog, Poisonous||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|4|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.089|Ettin||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|4|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.044|Couatl||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|5|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.129|Gorilla||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|5|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.111|Giant, Fire||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.044|Couatl||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|6|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.113|Giant, Hill||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.101|Flail Snail||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.146|Hippo, Behemoth||match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|7|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|8|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.147|Hippo, Normal||match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.116|Gibbering Mouther||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|9|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.173|Lizard, Draco||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|9|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|10|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.175|Lizard, Gecko||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.208|Nightmare||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|11|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.177|Lizard, Monitor||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|11|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.134|Hag, Black||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.217|Otyugh||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.176|Lzd., H. Chameleon||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.217|Otyugh||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.231|Rat, Giant||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|13|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.191|Mantid||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.226|Pseudo-Dragon||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.260|Shrew, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|14|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.243|Roper||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.264|Slug, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|15|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.243|Roper||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.267|Snake, Pit Viper||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|16|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.273|Sphinx||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.268|Snake, Rock Python||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.230|Rakshasa||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.278|Spider, Phase||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|18|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.270|Snake, Sp. Cobra||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|19|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.277|Spider, Crab||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.206|Neanderthal||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.298|Troglodyte||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.306|Violet Fungus||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|20|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table J: Jungle|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.307|Warp Beast||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table J: Jungle
Wilderness Encounter Sub-Table L: Lake, River|1|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.021|Boar||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|1|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.045|Crab, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|2|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.034|Cat, Panther||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.023|Brownie||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|2|SWIMMER|MONSTER_FAMILY|family:Crocodile|Crocodile|advanced_referee_v0_3.monster.046,advanced_referee_v0_3.monster.047,advanced_referee_v0_3.monster.048|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|3|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.036|Cat, Tiger||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|3|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.026|Bugbear||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|3|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.047|Crocodile, Large||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|4|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.045|Crab, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|4|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.088|Elf||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|4|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.094|Fish, Catfish||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|5|ANIMAL|MONSTER_FAMILY|family:Crocodile|Crocodile|advanced_referee_v0_3.monster.046,advanced_referee_v0_3.monster.047,advanced_referee_v0_3.monster.048|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|5|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|5|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.095|Fish, Electric Eel||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|6|ANIMAL|MONSTER_FAMILY|family:Crocodile|Crocodile|advanced_referee_v0_3.monster.046,advanced_referee_v0_3.monster.047,advanced_referee_v0_3.monster.048|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River""",
        """Wilderness Encounter Sub-Table L: Lake, River|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|6|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.095|Fish, Electric Eel||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|7|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.047|Crocodile, Large||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|7|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.096|Fish, Pike||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|8|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.097|Fish, Piranha||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|8|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.096|Fish, Pike||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|9|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|9|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.167|Leprechaun||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|9|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.097|Fish, Piranha||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|10|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|10|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.099|Fish, Sturgeon||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|11|ANIMAL|MONSTER_FAMILY|family:Herd Animal|Herd Animal|advanced_referee_v0_3.monster.141,advanced_referee_v0_3.monster.142,advanced_referee_v0_3.monster.143|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|11|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Exp. Adventurers||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|11|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|12|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.146|Hippo, Behemoth||match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|12|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant||match=TOKEN_FAMILY_MATCH;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|13|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.147|Hippo, Normal||match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|13|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|13|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|14|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.165|Lamprey, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|14|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|14|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.147|Hippo, Normal||match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|15|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|15|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.209|Nixie||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|15|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|16|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|16|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|16|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|17|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.231|Rat, Giant||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|17|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.209|Nixie||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|18|ANIMAL|MONSTER_ID|advanced_referee_v0_3.monster.260|Shrew, Giant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.280|Sprite||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|18|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.274|Spider, Aquatic||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|19|ANIMAL|MONSTER_FAMILY|family:Toad, Giant|Toad, Giant|advanced_referee_v0_3.monster.290,advanced_referee_v0_3.monster.291,advanced_referee_v0_3.monster.292|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.287|Thoul||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|19|SWIMMER|MONSTER_FAMILY|family:Water Termite|Termite, Water|advanced_referee_v0_3.monster.310,advanced_referee_v0_3.monster.311,advanced_referee_v0_3.monster.312|match=SOURCE_ALIAS;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|20|ANIMAL|MONSTER_FAMILY|family:Toad, Giant|Toad, Giant|advanced_referee_v0_3.monster.290,advanced_referee_v0_3.monster.291,advanced_referee_v0_3.monster.292|match=EXACT_FAMILY_HEADING;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Encounter Sub-Table L: Lake, River|20|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.301|Turtle, Snapping||match=EXACT_TOKEN_SET;printed_page=138;table=Wilderness Encounter Sub-Table L: Lake, River
Wilderness Sub-Table O: Ocean, Sea|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|1|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.074|Dragon Turtle||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|2|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.100|Fish, Swordfish||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|3|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.135|Hag, Sea||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|4|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.025|Buccaneer||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|4|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.144|Hippocampus||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|5|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Ex. Adventurers||printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|5|SWIMMER|PARAMETERIZED_MONSTER|advanced_referee_v0_3.monster.156|Hydra, Sea||match=SOURCE_ALIAS;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea;variant=SEA_ADAPTATION
Wilderness Sub-Table O: Ocean, Sea|6|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Ex. Adventurers||printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|6|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.160|Jellyfish, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|7|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.181|Locathah||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|8|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.197|Merman||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|9|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|9|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|10|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|10|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.214|Octopus, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|11|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|11|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.246|Sahuagin||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|12|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|12|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.069|Sea Dragon||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|13|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|13|SWIMMER|MONSTER_FAMILY|family:Sea Serpent|Sea Serpent|advanced_referee_v0_3.monster.253,advanced_referee_v0_3.monster.254|match=EXACT_FAMILY_HEADING;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|14|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|14|SWIMMER|MONSTER_FAMILY|family:Shark|Shark|advanced_referee_v0_3.monster.257,advanced_referee_v0_3.monster.258,advanced_referee_v0_3.monster.259|match=EXACT_FAMILY_HEADING;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|15|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|15|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.269|Snake, Sea||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|16|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|16|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.281|Squid, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|17|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|17|SWIMMER|MONSTER_FAMILY|family:Water Termite|Termite, Water|advanced_referee_v0_3.monster.310,advanced_referee_v0_3.monster.311,advanced_referee_v0_3.monster.312|match=SOURCE_ALIAS;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|18|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.297|Triton||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|19|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.300|Turtle, Sea||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.223|Pirate||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Sub-Table O: Ocean, Sea|20|SWIMMER|MONSTER_FAMILY|family:Whale|Whale|advanced_referee_v0_3.monster.314,advanced_referee_v0_3.monster.315,advanced_referee_v0_3.monster.316|match=EXACT_FAMILY_HEADING;printed_page=139;table=Wilderness Sub-Table O: Ocean, Sea
Wilderness Encounter Sub-Table S: Swamp|1|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|1|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.089|Ettin||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|1|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.006|Banshee||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|1|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.045|Crab, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|2|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|2|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.117|Gnoll||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|2|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.030|Carcass Crawler||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|2|SWIMMER|MONSTER_FAMILY|family:Crocodile|Crocodile|advanced_referee_v0_3.monster.046,advanced_referee_v0_3.monster.047,advanced_referee_v0_3.monster.048|match=EXACT_FAMILY_HEADING;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|3|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.005|Bandit||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|3|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.119|Goblin||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|3|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.037|Catoblepas||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|3|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.046|Crocodile, Giant||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|4|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_BASIC_ADVENTURERS|Basic Adventurers||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|4|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|4|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.102|Frog, Mutant||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|4|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.047|Crocodile, Large||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|5|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|5|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|5|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.103|Frog, Poisonous||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|5|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.094|Fish, Catfish||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|6|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.018|Berserker||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|6|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|6|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.108|Ghost||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|6|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.095|Fish, Electric Eel||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|7|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|7|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.134|Hag, Black||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|7|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.109|Ghoul||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|7|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.096|Fish, Pike||match=TOKEN_FAMILY_MATCH;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|8|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.022|Brigand||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|8|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.148|Hobgoblin||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|8|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.156|Hydra||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|8|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|9|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|9|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|9|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.168|Leucrocotta||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|9|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.133|Gullygug||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|10|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|10|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|10|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|10|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.157|Insect Swarm||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|11|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_EXPERT_ADVENTURERS|Expert Adventurers||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|11|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|11|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|11|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.157|Insect Swarm||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|12|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|12|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.198|Merrow||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|12|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.217|Otyugh||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|12|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.165|Lamprey, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|13|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_CLERIC|High-Level Cleric||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|13|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.204|Mutoid||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|13|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.244|Rot Grub||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|13|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.165|Lamprey, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|14|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|14|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.209|Nixie||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|14|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.256|Shambling Mound||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|14|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|15|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_FIGHTER|High-Level Fighter||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|15|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.215|Ogre||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|15|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.271|Spawn of the Worm||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|15|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.166|Leech, Giant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|16|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|16|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.216|Orc||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|16|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.292|Toad, Poisonous||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|16|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|17|HUMAN|NPC_GENERATOR_REFERENCE|ADV_NPC_HIGH_LEVEL_MAGIC_USER|High-Level MU||printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|17|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.265|Snake Person||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|17|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.309|Water Fiend||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|17|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.180|Lizard Man||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|18|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.196|Merchant||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|18|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.298|Troglodyte||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|18|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o'-the-Wisp||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|18|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.274|Spider, Aquatic||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|19|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|19|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|19|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.318|Will-o'-the-Wisp||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|19|SWIMMER|MONSTER_FAMILY|family:Water Termite|Termite, Water|advanced_referee_v0_3.monster.310,advanced_referee_v0_3.monster.311,advanced_referee_v0_3.monster.312|match=SOURCE_ALIAS;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|20|HUMAN|MONSTER_ID|advanced_referee_v0_3.monster.293|Trader||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|20|HUMANOID|MONSTER_ID|advanced_referee_v0_3.monster.299|Troll||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|20|MONSTER|MONSTER_ID|advanced_referee_v0_3.monster.322|Wraith||match=EXACT_NORMALIZED_NAME;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp
Wilderness Encounter Sub-Table S: Swamp|20|SWIMMER|MONSTER_ID|advanced_referee_v0_3.monster.301|Turtle, Snapping||match=EXACT_TOKEN_SET;printed_page=139;table=Wilderness Encounter Sub-Table S: Swamp"""
    )

    private data class Redirect(val tableId:String,val column:String,val footnote:String? = null)
    private val dungeon:Map<String,AdvancedEncounterContent> by lazy { parseContent(DUNGEON_DATA, 3) }
    private val direct:Map<String,AdvancedEncounterContent> by lazy { parseContent(DIRECT_DATA, 3) }
    private val master:Map<String,Redirect> by lazy {
        MASTER_DATA.asSequence().flatMap { it.lineSequence() }.filter { it.isNotBlank() }.associate { line ->
            val p=splitEscaped(line); check(p.size==5) { "ADV_MASTER_ROW_INVALID" }
            "${p[0]}:${p[1]}" to Redirect(p[2],p[3],p[4].ifBlank { null })
        }
    }

    fun dungeonTableIdForLevel(level:Int):String = when { level<=1 -> "ADV_DUNGEON_LEVEL_1"; level==2 -> "ADV_DUNGEON_LEVEL_2"; level==3 -> "ADV_DUNGEON_LEVEL_3"; level in 4..5 -> "ADV_DUNGEON_LEVEL_4_5"; level in 6..7 -> "ADV_DUNGEON_LEVEL_6_7"; else -> "ADV_DUNGEON_LEVEL_8_PLUS" }
    fun resolveDungeon(level:Int,d4:Int,d10:Int):AdvancedDungeonEncounterResult {
        require(level>=1); require(d4 in 1..4); require(d10 in 1..10)
        val tableId=dungeonTableIdForLevel(level); val c=dungeon.getValue("$tableId|$d4|$d10"); validateContent(c)
        return AdvancedDungeonEncounterResult(tableId,d4,d10,c)
    }

    fun resolveWilderness(terrain:String,d8:Int,d20:Int):AdvancedWildernessEncounterResult {
        require(d8 in 1..8); require(d20 in 1..20); val t=terrain.uppercase()
        val r=master["$t:$d8"] ?: error("ADV_WILDERNESS_TERRAIN_NOT_BOUND:$t")
        val c=direct.getValue("${r.tableId}|$d20|${r.column}"); validateContent(c)
        return AdvancedWildernessEncounterResult(t,d8,d20,r.tableId,r.column,c,r.footnote)
    }

    fun terrains():Set<String> = master.keys.map { it.substringBefore(':') }.toSortedSet()
    fun validateAll():List<String> {
        val e=mutableListOf<String>()
        for(level in listOf(1,2,3,4,6,8)) for(d4 in 1..4) for(d10 in 1..10) runCatching { resolveDungeon(level,d4,d10) }.exceptionOrNull()?.let { e += "D:$level:$d4:$d10:${it.message}" }
        for(t in terrains()) for(d8 in 1..8) for(d20 in 1..20) runCatching { resolveWilderness(t,d8,d20) }.exceptionOrNull()?.let { e += "W:$t:$d8:$d20:${it.message}" }
        return e
    }

    private fun parseContent(chunks:List<String>, prefix:Int):Map<String,AdvancedEncounterContent> = chunks.asSequence().flatMap { it.lineSequence() }.filter { it.isNotBlank() }.associate { line ->
        val p=splitEscaped(line); check(p.size==prefix+5) { "ADV_CONTENT_ROW_INVALID:${p.size}" }
        val key=p.take(prefix).joinToString("|")
        val members=p[prefix+3].split(',').filter { it.isNotBlank() }
        val meta=if(p[prefix+4].isBlank()) emptyMap() else p[prefix+4].split(';').associate { kv -> val i=kv.indexOf('='); check(i>0); kv.substring(0,i) to kv.substring(i+1) }
        key to AdvancedEncounterContent(p[prefix],p[prefix+1],p[prefix+2],members,meta)
    }

    private fun splitEscaped(line:String):List<String> {
        val out=mutableListOf<String>(); val b=StringBuilder(); var escaped=false
        for(ch in line) {
            if(escaped) { when(ch) { 'p' -> b.append('|'); 'n' -> b.append('\n'); '\\' -> b.append('\\'); else -> { b.append('\\'); b.append(ch) } }; escaped=false }
            else if(ch=='\\') escaped=true else if(ch=='|') { out+=b.toString(); b.setLength(0) } else b.append(ch)
        }
        if(escaped) b.append('\\'); out+=b.toString(); return out
    }

    private fun validateContent(c:AdvancedEncounterContent) { when(c.type) {
        "MONSTER_ID","PARAMETERIZED_REFERENCE","PARAMETERIZED_MONSTER" -> check(c.ref.isNotBlank() && AdvancedMonsterRuntimeRegistry.lookup(PROFILE,AdvancedMonsterRuntimeRegistry.DATASET_ID,c.ref)!=null) { "ADV_MONSTER_REF_NOT_BOUND:${c.ref}" }
        "MONSTER_FAMILY" -> check(c.memberIds.isNotEmpty() && c.memberIds.all { AdvancedMonsterRuntimeRegistry.lookup(PROFILE,AdvancedMonsterRuntimeRegistry.DATASET_ID,it)!=null }) { "ADV_MONSTER_FAMILY_NOT_BOUND:${c.raw}" }
        "NPC_GENERATOR_REFERENCE" -> check(c.ref.isNotBlank())
        "SOURCE_CHOICE" -> check(c.memberIds.all { AdvancedMonsterRuntimeRegistry.lookup(PROFILE,AdvancedMonsterRuntimeRegistry.DATASET_ID,it)!=null })
        else -> error("ADV_ENCOUNTER_RESOLUTION_UNSUPPORTED:${c.type}")
    } }
}
