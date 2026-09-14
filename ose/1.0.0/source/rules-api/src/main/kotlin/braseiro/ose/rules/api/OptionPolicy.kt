package braseiro.ose.rules.api

import braseiro.ose.model.OptionSet
import braseiro.ose.model.RuleProfile

enum class OptionScope { CLASSIC, ADVANCED, BOTH }
enum class OptionValueKind { BOOLEAN, ENUM, POLICY }
data class OptionPolicy(val id:String,val scope:OptionScope,val kind:OptionValueKind,val defaultClassic:String?=null,val defaultAdvanced:String?=null,val allowedValues:Set<String> = setOf("ON","OFF"),val dependencies:Set<String> = emptySet())
data class OptionPolicyResult(val normalized:Map<String,String>,val errors:List<String>){val valid:Boolean get()=errors.isEmpty()}
object OptionPolicyContract {
 val policies=listOf(
  OptionPolicy("OPT.ASCENDING_AC",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.DIRECT_THAC0",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.VARIABLE_WEAPON_DAMAGE",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.INDIVIDUAL_INITIATIVE",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.MORALE",OptionScope.BOTH,OptionValueKind.BOOLEAN,"ON","ON"),
  OptionPolicy("OPT.ENCUMBRANCE",OptionScope.BOTH,OptionValueKind.ENUM,"BASIC","BASIC",setOf("NONE","BASIC","DETAILED")),
  OptionPolicy("OPT.REROLL_HP_1_2",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.CLASSIC_HIGH_LEVEL",OptionScope.CLASSIC,OptionValueKind.BOOLEAN,"OFF",null),
  OptionPolicy("OPT.ADV_CREATION_BASIC",OptionScope.ADVANCED,OptionValueKind.POLICY,null,"ALLOWED",setOf("ALLOWED","DISALLOWED")),
  OptionPolicy("OPT.ADV_CREATION_ADVANCED",OptionScope.ADVANCED,OptionValueKind.POLICY,null,"ALLOWED",setOf("ALLOWED","DISALLOWED")),
  OptionPolicy("OPT.ADV_MULTICLASS",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF",dependencies=setOf("OPT.ADV_CREATION_ADVANCED")),
  OptionPolicy("OPT.ADV_WEAPON_PROF",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_WEAPON_SPEC",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF",dependencies=setOf("OPT.ADV_WEAPON_PROF")),
  OptionPolicy("OPT.ADV_SECONDARY_SKILLS",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_RETURN_FROM_DEATH",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.LIFT_DEMIHUMAN_LIMITS",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF",dependencies=setOf("OPT.ADV_CREATION_ADVANCED")),
  OptionPolicy("OPT.HUMAN_COMPENSATION",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF",dependencies=setOf("OPT.LIFT_DEMIHUMAN_LIMITS")),
  OptionPolicy("OPT.ADV_TWO_WEAPON",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_CHARGE",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_MISSILE_INTO_MELEE",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_PARRY",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.ADV_SPLASH_WEAPONS",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.SUBDUAL",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.INVULNERABILITY_EXTENSION",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF"),
  OptionPolicy("OPT.ADV_ILLUSIONIST_STAFF",OptionScope.ADVANCED,OptionValueKind.BOOLEAN,null,"OFF"),
  OptionPolicy("OPT.VARIABLE_WEATHER_WIND",OptionScope.BOTH,OptionValueKind.BOOLEAN,"OFF","OFF")
 )
 private val byId=policies.associateBy{it.id}
 fun resolve(profile:RuleProfile,set:OptionSet):OptionPolicyResult{
  val errors=mutableListOf<String>(); val explicit=linkedMapOf<String,String>()
  set.optionIds.map(OptionIds::canonical).forEach{raw-> val id=raw.substringBefore("="); val p=byId[id]; if(p==null){errors+="UNKNOWN_OPTION:$id";return@forEach}; val scope=when(p.scope){OptionScope.BOTH->true;OptionScope.CLASSIC->profile==RuleProfile.OSE_CLASSIC_FANTASY;OptionScope.ADVANCED->profile==RuleProfile.OSE_ADVANCED_FANTASY}; if(!scope){errors+="OPTION_PROFILE_MISMATCH:$id";return@forEach}; val v=if("=" in raw)raw.substringAfter("=").uppercase() else "ON"; if(v !in p.allowedValues){errors+="OPTION_VALUE_INVALID:$id=$v";return@forEach}; if(explicit.put(id,v)!=null)errors+="OPTION_DUPLICATE:$id"}
  val normalized=linkedMapOf<String,String>(); policies.forEach{p-> val applies=when(p.scope){OptionScope.BOTH->true;OptionScope.CLASSIC->profile==RuleProfile.OSE_CLASSIC_FANTASY;OptionScope.ADVANCED->profile==RuleProfile.OSE_ADVANCED_FANTASY}; if(applies){val d=if(profile==RuleProfile.OSE_CLASSIC_FANTASY)p.defaultClassic else p.defaultAdvanced;if(d!=null)normalized[p.id]=explicit[p.id]?:d}}; explicit.forEach{(k,v)->normalized[k]=v}
  policies.forEach{p->if(normalized[p.id]=="ON")p.dependencies.forEach{d->val dv=normalized[d];if(dv!="ON"&&dv!="ALLOWED")errors+="OPTION_DEPENDENCY:${p.id} requires $d"}}
  if(profile==RuleProfile.OSE_ADVANCED_FANTASY&&normalized["OPT.ADV_CREATION_BASIC"]=="DISALLOWED"&&normalized["OPT.ADV_CREATION_ADVANCED"]=="DISALLOWED")errors+="ADV_CREATION_POLICY_EMPTY"
  return OptionPolicyResult(normalized.toSortedMap(),errors.distinct())
 }
 fun enabled(profile:RuleProfile,set:OptionSet,id:String)=resolve(profile,set).normalized[id]=="ON"
 fun value(profile:RuleProfile,set:OptionSet,id:String)=resolve(profile,set).normalized[id]
}
