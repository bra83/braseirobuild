package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifica uma resolução mecânica produzida a partir dos manuais.
 *
 * O Gemini pode interpretar o texto, mas não pode inventar números. Uma
 * alteração só é aceita quando a fonte está no pacote recuperado e o cálculo
 * pode ser reproduzido com uma rolagem local recente ou com um valor fixo
 * literalmente compilado do trecho.
 */
public final class RuleResolutionVerifier {
    public static final class Result {
        public final boolean valid;
        public final String reason;
        public final JSONObject normalizedCalculation;
        Result(boolean ok,String why,JSONObject calculation){valid=ok;reason=why==null?"":why;normalizedCalculation=calculation==null?new JSONObject():calculation;}
    }

    private static final Pattern DICE=Pattern.compile("(?iu)\\b(\\d{1,3})?d(\\d{1,4})(?:([+\\-])(\\d{1,3}))?\\b");
    private static final Pattern ATTACK_BONUS=Pattern.compile("(?iu)(?:([+\\-]\\d{1,2})\\s*(?:para atingir|to hit)|(?:b[oô]nus de ataque|attack bonus|ataque corpo a corpo|ataque à distância|ataque a distancia|melee weapon attack|ranged weapon attack)\\s*[:=]?\\s*([+\\-]\\d{1,2})(?:\\s*(?:para atingir|to hit))?)");
    private static final Pattern SAVE_DC=Pattern.compile("(?iu)(?:CD|DC|classe de dificuldade|difficulty class|save dc)\\s*[:=]?\\s*(\\d{1,2})");
    private final AppDb db;

    public RuleResolutionVerifier(AppDb database){db=database;}

    public Result verifyStateUpdate(JSONObject basis,JSONObject update,JSONObject hero,JSONObject retrieval){/*BR_JSON_WRAP*/try{
        if(basis==null||update==null||hero==null)return fail("Base, alteração ou ficha ausente.");
        JSONObject calculation=basis.optJSONObject("calculation");
        if(calculation==null)calculation=basis.optJSONObject("resolution");
        if(calculation==null)return fail("A fonte do manual não trouxe calculation/resolution verificável.");
        JSONObject source=findSource(basis,retrieval);
        if(source==null)return fail("A fonte não pertence às passagens recuperadas nesta cena.");
        String excerpt=source.optString("excerpt");
        JSONObject compiled=findCompiledRule(source,retrieval);
        JSONObject procedure=findAtomicProcedure(compiled,basis,calculation);
        if(procedure==null)return fail(procedureFailureReason(compiled,basis,calculation));
        String procedureText=procedure.optString("scope_text",excerpt);
        try{
            int verifiedMutations=0;
            JSONObject normalized=copyObject(calculation);
            normalized.put("procedure_id",procedure.optString("procedure_id"));
            normalized.put("source_rule_id",source.optString("rule_id"));
            normalized.put("source_book",source.optString("book"));
            normalized.put("source_page",source.optInt("page"));

            if(update.has("hp_delta")||update.has("hp_set")){
                int before=Math.max(0,hero.optInt("hp",0));
                int max=Math.max(1,hero.optInt("max_hp",before));
                int expectedDelta=update.has("hp_delta")?update.optInt("hp_delta",0):Math.max(0,Math.min(max,update.optInt("hp_set",before)))-before;
                String operation=norm(calculation.optString("operation"));
                if(expectedDelta<0&&!contains(operation,"damage","dano","subtract","perda"))return fail("O cálculo não declara dano/perda para hp_delta negativo.");
                if(expectedDelta>0&&!contains(operation,"healing","cura","heal","add","ganho"))return fail("O cálculo não declara cura/ganho para hp_delta positivo.");
                Integer magnitude=verifiedMagnitude(calculation,procedureText,procedure,hero);
                if(magnitude==null)return fail("O valor de PV não pôde ser reproduzido por rolagem local ou valor fixo da regra.");
                int signed=expectedDelta<0?-Math.abs(magnitude):Math.abs(magnitude);
                if(signed!=expectedDelta)return fail("O cálculo reproduz "+signed+", mas a alteração propõe "+expectedDelta+" PV.");
                normalized.put("verified_value",signed).put("target_field",update.has("hp_set")?"hp_set":"hp_delta");verifiedMutations++;
            }

            JSONObject resources=update.optJSONObject("resource_changes");
            if(resources!=null&&resources.length()>0){
                Result verifiedResources=verifyResourceChanges(resources,calculation,hero,procedureText,procedure);if(!verifiedResources.valid)return verifiedResources;verifiedMutations++;
            }

            Result conditions=verifyConditionChanges(update,hero,procedureText,procedure);if(!conditions.valid)return conditions;if(arrayLength(update.optJSONArray("condition_add"))+arrayLength(update.optJSONArray("condition_remove"))>0)verifiedMutations++;
            Result items=verifyItemChanges(update,hero,procedureText,procedure);if(!items.valid)return items;if(arrayLength(update.optJSONArray("add_items"))+arrayLength(update.optJSONArray("remove_items"))>0)verifiedMutations++;
            String combatConditions=update.optString("conditions","").trim();if(combatConditions.length()>0){Result conditionText=verifyConditionText(combatConditions,procedureText,procedure);if(!conditionText.valid)return conditionText;verifiedMutations++;}
            if(verifiedMutations==0)return fail("A alteração não contém mutação mecânica suportada e verificável.");
            return new Result(true,"Cálculo manual reproduzido localmente.",normalized);
        }catch(Exception e){return fail("Falha ao normalizar o cálculo: "+safe(e));}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}


    /** Confere estatísticas de criaturas contra a passagem recuperada. Uma
     * citação genérica não autoriza números de um bloco de estatísticas. */
    public Result verifyEnemyStatBlocks(JSONObject basis,JSONArray enemies,JSONObject retrieval,boolean mystara){/*BR_JSON_WRAP*/try{
        if(basis==null||enemies==null||enemies.length()==0)return fail("Bloco de inimigos ausente.");
        JSONObject source=findSource(basis,retrieval);if(source==null)return fail("A fonte do inimigo não pertence à busca desta cena.");
        String excerpt=source.optString("excerpt");JSONObject compiled=findCompiledRule(source,retrieval);JSONArray blocks=compiled==null?null:compiled.optJSONArray("creature_blocks");
        if(blocks==null||blocks.length()==0)return fail("A passagem não possui blocos de criaturas com limites verificáveis.");
        try{
            for(int i=0;i<enemies.length();i++){
                JSONObject enemy=enemies.optJSONObject(i);if(enemy==null)continue;
                String name=enemy.optString("name").trim();JSONObject block=findCreatureBlock(blocks,name);if(block==null)return fail("A criatura ‘"+name+"’ não possui bloco independente na passagem recuperada.");
                if(enemy.optString("block_id").trim().length()>0&&!enemy.optString("block_id").equals(block.optString("block_id")))return fail("block_id divergente para "+name+".");
                String blockText=block.optString("scope_text");
                int hp=enemy.optInt("hp",Integer.MIN_VALUE),ac=enemy.optInt("ac",Integer.MIN_VALUE);
                if(hp==Integer.MIN_VALUE||hp!=block.optInt("hit_points",Integer.MIN_VALUE)||!labeledNumber(blockText,hp,"pv","pontos de vida","hit points","hp"))return fail("Os PV de "+name+" não pertencem ao bloco dessa criatura.");
                if(ac==Integer.MIN_VALUE||ac!=block.optInt("armor_class",Integer.MIN_VALUE)||!labeledNumber(blockText,ac,"ca","classe de armadura","armor class","ac"))return fail("A CA de "+name+" não pertence ao bloco dessa criatura.");
                if(mystara){
                    int morale=enemy.optInt("morale",Integer.MIN_VALUE);if(morale!=Integer.MIN_VALUE&&!labeledNumber(blockText,morale,"moral","morale"))return fail("A Moral de "+name+" não está fundamentada no mesmo bloco.");
                    String hd=compactFormula(enemy.optString("hit_dice"));if(hd.length()>0&&!compactFormula(blockText).contains(hd))return fail("Os Dados de Vida de "+name+" não aparecem no mesmo bloco.");
                }
                String attacks=enemy.optString("attacks");Matcher dice=DICE.matcher(attacks==null?"":attacks);
                while(dice.find())if(!compactFormula(blockText).contains(compactFormula(dice.group())))return fail("O dano de ataque de "+name+" não aparece no mesmo bloco.");
                Matcher attackBonus=ATTACK_BONUS.matcher(attacks==null?"":attacks);while(attackBonus.find()){String signed=firstNonEmpty(attackBonus.group(1),attackBonus.group(2));if(signed!=null&&!signedNumberGrounded(blockText,Integer.parseInt(signed),"para atingir","to hit","bonus de ataque","attack bonus","ataque corpo a corpo","ataque a distancia","melee weapon attack","ranged weapon attack"))return fail("O bônus de ataque de "+name+" não aparece no mesmo bloco.");}
                Matcher saveDc=SAVE_DC.matcher(attacks==null?"":attacks);while(saveDc.find())if(!labeledNumber(blockText,Integer.parseInt(saveDc.group(1)),"cd","dc","classe de dificuldade","difficulty class","save dc"))return fail("A CD de efeito de "+name+" não aparece no mesmo bloco.");
            }
            return new Result(true,"Blocos de estatísticas conferidos na passagem recuperada.",new JSONObject().put("source_rule_id",source.optString("rule_id")).put("enemy_count",enemies.length()));
        }catch(Exception error){return fail("Falha ao conferir bloco de estatísticas: "+safe(error));}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean signedNumberGrounded(String excerpt,int value,String... labels){
        String text=semanticText(excerpt),signed=(value>=0?"+":"")+value;
        for(String label:labels){String q=Pattern.quote(norm(label));String number="(?<![a-z0-9d])"+Pattern.quote(signed)+"(?!\\d)";if(Pattern.compile("(?iu)"+number+"\\s*(?:,\\s*)?"+q+"\\b").matcher(text).find())return true;if(Pattern.compile("(?iu)\\b"+q+"\\b\\s*[:=]?\\s*"+number).matcher(text).find())return true;}return false;
    }
    private static Result verifyConditionText(String value,String excerpt,JSONObject procedure){
        String[] entries=value.split("[,;\n]+");for(String entry:entries){String condition=entry.trim();if(condition.length()<2)continue;if(!procedureSupportsCondition(procedure,condition,"apply"))return fail("A condição ‘"+condition+"’ não possui operação de aplicação no procedimento selecionado.");}return new Result(true,"",new JSONObject());
    }
    private static int arrayLength(JSONArray array){return array==null?0:array.length();}

    private Result verifyResourceChanges(JSONObject changes,JSONObject calculation,JSONObject hero,String excerpt,JSONObject procedure){
        JSONObject declared=calculation.optJSONObject("resource_deltas");if(declared==null)return fail("resource_changes não possui resource_deltas reproduzível.");
        try{
            java.util.Iterator<String> keys=changes.keys();while(keys.hasNext()){
                String key=keys.next();ResourceState state=resourceState(hero,key);if(!state.exists)return fail("O recurso "+key+" não existe na ficha do personagem.");
                if(state.max<Integer.MIN_VALUE+1||state.max<state.min||state.current<state.min||state.current>state.max)return fail("Os limites atuais de "+key+" estão ausentes ou inválidos na ficha.");
                Integer proposed=resourceDelta(changes.opt(key),state.current);Integer claimed=integerValue(declared.opt(key));
                if(proposed==null||claimed==null||proposed.intValue()!=claimed.intValue())return fail("Recurso "+key+" não coincide com o cálculo declarado.");
                String heroClass=norm(hero==null?"":hero.optString("class_name"));String heroFeatures=norm((hero==null?"":hero.optString("features"))+" "+(hero==null?"":hero.optString("class_features")));int heroLevel=Math.max(0,hero==null?0:hero.optInt("level",0));
                if(state.ownerClass.length()==0&&state.ownerAbility.length()==0&&state.ownerSystem.length()==0)return fail("A classe, habilidade ou regra que possui "+key+" não está registrada na ficha.");
                if(state.ownerClass.length()>0&&!sourceMentions(heroClass,state.ownerClass))return fail("A classe do personagem não possui "+key+".");
                if(state.ownerAbility.length()>0&&!sourceMentions(heroFeatures,state.ownerAbility))return fail("A habilidade que possui "+key+" não está na ficha.");
                if(heroLevel<state.requiredLevel)return fail(key+" exige nível "+state.requiredLevel+".");
                JSONObject effect=matchingResourceEffect(procedure,key);if(effect==null)return fail("O procedimento não contém operação semântica para o recurso "+key+".");
                String operation=effect.optString("resource_operation");if(proposed<0&&!contains(operation,"spend","reduce"))return fail("O procedimento não autoriza gastar/reduzir "+key+".");
                if(proposed>0&&!contains(operation,"recover","gain"))return fail("O procedimento não autoriza recuperar/ganhar "+key+".");
                if(proposed<0&&!state.canSpend)return fail("A ficha impede gastar "+key+".");if(proposed>0&&!state.canRecover)return fail("A ficha impede recuperar "+key+".");
                if(proposed>0&&state.recoveryCondition.length()>0){String rest=norm((hero==null?"":hero.optString("rest_context"))+" "+(hero==null?"":hero.optString("conditions")));if(!sourceMentions(rest,state.recoveryCondition))return fail("A condição/descanso para recuperar "+key+" não foi satisfeita.");}
                int after=state.current+proposed;if(after<state.min||after>state.max)return fail("A alteração de "+key+" ultrapassa os limites "+state.min+"–"+state.max+".");
                int expected=expectedResourceMagnitude(effect,state,hero,calculation,excerpt,procedure);if(expected<0)return fail("A quantidade de "+key+" não foi derivada de regra, fórmula, ficha, nível ou rolagem vinculada.");
                if(Math.abs(proposed)!=expected)return fail("A regra demonstra "+expected+" unidade(s) de "+key+", não "+Math.abs(proposed)+".");
                if(!prerequisitesSatisfied(procedure,hero))return fail("Classe, nível, descanso ou condição exigida para "+key+" não foi satisfeita pela ficha/estado local.");
            }
            return new Result(true,"Recursos e limites verificados.",new JSONObject());
        }catch(Exception error){return fail("Falha ao verificar recursos: "+safe(error));}
    }

    private int expectedResourceMagnitude(JSONObject effect,ResourceState state,JSONObject hero,JSONObject calculation,String excerpt,JSONObject procedure){
        String mode=effect.optString("quantity_mode");if("fixed".equals(mode)&&effect.optInt("quantity",0)>0)return effect.optInt("quantity");
        if("half".equals(mode))return Math.max(1,(state.max+1)/2);
        if("all".equals(mode)){String operation=effect.optString("resource_operation");return contains(operation,"recover","gain")?state.max-state.current:state.current-state.min;}
        String text=norm(effect.optString("text"));int level=Math.max(1,hero.optInt("level",1));if(contains(text,"igual ao seu nivel","equal to your level","seu nivel","your level"))return level;
        JSONObject proofs=calculation.optJSONObject("resource_proofs"),proof=proofs==null?null:proofs.optJSONObject(effect.optString("resource",state.key));if(proof==null&&proofs!=null)proof=proofs.optJSONObject(state.key);
        if(proof!=null){String source=norm(proof.optString("quantity_source"));if("sheet current".equals(source)&&proof.optInt("value",Integer.MIN_VALUE)==state.current)return Math.max(0,state.current-state.min);if("sheet maximum".equals(source)&&proof.optInt("value",Integer.MIN_VALUE)==state.max)return Math.max(0,state.max-state.current);if("level".equals(source)&&proof.optInt("value",Integer.MIN_VALUE)==level)return level;}
        return -1;
    }

    private static JSONObject matchingResourceEffect(JSONObject procedure,String key){JSONArray effects=procedure==null?null:procedure.optJSONArray("effects");if(effects==null)return null;String canonical=canonicalResource(key);for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect==null||effect.optString("resource_operation").length()==0)continue;String declared=canonicalResource(effect.optString("resource"));if(declared.length()>0&&(declared.equals(canonical)||canonical.startsWith(declared)||declared.startsWith(canonical)))return effect;if(resourceGrounded(effect.optString("text"),effect,key))return effect;}return null;}

    private static ResourceState resourceState(JSONObject hero,String key){JSONObject sheet=parseObject(hero==null?"":hero.optString("sheet_json","{}"));JSONObject resources=hero==null?null:hero.optJSONObject("resources");if(resources==null)resources=sheet.optJSONObject("resources");JSONObject limits=hero==null?null:hero.optJSONObject("resource_limits");if(limits==null)limits=sheet.optJSONObject("resource_limits");if(resources==null)return new ResourceState(key,false,0,Integer.MIN_VALUE,0,false,false,"","","",0,"");Object raw=resources.opt(key);if(raw==null){String canonical=canonicalResource(key);java.util.Iterator<String> names=resources.keys();while(names.hasNext()){String candidate=names.next();if(canonicalResource(candidate).equals(canonical)){raw=resources.opt(candidate);break;}}}if(raw==null)return new ResourceState(key,false,0,Integer.MIN_VALUE,0,false,false,"","","",0,"");int current=0,max=Integer.MIN_VALUE,min=0,requiredLevel=0;boolean spend=true,recover=true;String ownerClass="",ownerAbility="",ownerSystem="",recoveryCondition="";if(raw instanceof JSONObject){JSONObject value=(JSONObject)raw;current=value.optInt("current",value.optInt("value",0));max=value.optInt("max",Integer.MIN_VALUE);min=value.optInt("min",0);spend=value.optBoolean("can_spend",true);recover=value.optBoolean("can_recover",true);ownerClass=value.optString("owner_class");ownerAbility=value.optString("owner_ability");ownerSystem=value.optString("owner_system");requiredLevel=Math.max(0,value.optInt("required_level",0));recoveryCondition=value.optString("recovery_condition");}else if(raw instanceof Number){current=((Number)raw).intValue();JSONObject bound=limits==null?null:limits.optJSONObject(key);if(bound!=null){max=bound.optInt("max",Integer.MIN_VALUE);min=bound.optInt("min",0);spend=bound.optBoolean("can_spend",true);recover=bound.optBoolean("can_recover",true);ownerClass=bound.optString("owner_class");ownerAbility=bound.optString("owner_ability");ownerSystem=bound.optString("owner_system");requiredLevel=Math.max(0,bound.optInt("required_level",0));recoveryCondition=bound.optString("recovery_condition");}else{max=resources.optInt(key+"_max",sheet.optInt(key+"_max",Integer.MIN_VALUE));min=resources.optInt(key+"_min",0);}}return new ResourceState(key,true,current,max,min,spend,recover,ownerClass,ownerAbility,ownerSystem,requiredLevel,recoveryCondition);}

    private static boolean prerequisitesSatisfied(JSONObject procedure,JSONObject hero){JSONArray req=procedure==null?null:procedure.optJSONArray("prerequisites");if(req==null)req=procedure==null?null:procedure.optJSONArray("pre_requisitos");if(req==null)return true;JSONObject sheet=parseObject(hero==null?"":hero.optString("sheet_json","{}"));String cls=norm(hero==null?"":hero.optString("class_name",sheet.optString("class_name")));int level=Math.max(hero==null?0:hero.optInt("level",sheet.optInt("level",0)),0);String rest=norm(sheet.optString("rest_context",hero==null?"":hero.optString("rest_context")));for(int i=0;i<req.length();i++){String r=req.optString(i),n=norm(r);if(n.startsWith("level ")){Matcher m=Pattern.compile("\\d+").matcher(n);if(m.find()&&level<Integer.parseInt(m.group()))return false;}else if(n.startsWith("class ")&&!cls.contains(n.substring(6).trim()))return false;else if("short rest".equals(n)&&!contains(rest,"short","curto"))return false;else if("long rest".equals(n)&&!contains(rest,"long","longo"))return false;}return true;}

    private static Result verifyConditionChanges(JSONObject update,JSONObject hero,String excerpt,JSONObject procedure){
        for(String key:new String[]{"condition_add","condition_remove"}){JSONArray values=update.optJSONArray(key);if(values==null)continue;String operation="condition_add".equals(key)?"apply":"remove";for(int i=0;i<values.length();i++){Object raw=values.opt(i);JSONObject spec=raw instanceof JSONObject?(JSONObject)raw:null;String name=spec==null?String.valueOf(raw).trim():spec.optString("name").trim();if(name.length()==0)return fail("Condição sem nome.");JSONObject conditionEffect=matchingConditionEffect(procedure,name,operation);if(conditionEffect==null)return fail("O procedimento não autoriza "+("apply".equals(operation)?"aplicar ":"remover ")+name+".");
            if("apply".equals(operation)){
                if(spec==null)return fail("A aplicação de "+name+" exige duração, origem e encerramento estruturados.");
                JSONArray durations=procedure.optJSONArray("durations");if(durations!=null&&durations.length()>0){if(spec.optString("duration").trim().length()==0)return fail("A duração de "+name+" é obrigatória.");if(!sourceMentions(durations.toString(),spec.optString("duration")))return fail("A duração de "+name+" diverge do procedimento.");}
                String origin=spec.optString("source").trim();if(origin.length()==0||!procedureSourceMatches(procedure,origin))return fail("A origem de "+name+" não coincide com o procedimento selecionado.");
                int expectedDc=firstProcedureDc(procedure);if(expectedDc>0&&spec.optInt("dc",0)!=expectedDc)return fail("A CD de encerramento/aplicação de "+name+" deve ser "+expectedDc+".");
                JSONArray repetitions=procedure.optJSONArray("repetitions");if(repetitions!=null&&repetitions.length()>0){if(spec.optString("end_save").trim().length()==0)return fail("O teste para encerrar "+name+" é obrigatório.");String timing=spec.optString("repeat_timing").trim();if(timing.length()==0||!sourceMentions(repetitions.toString(),timing))return fail("O momento de repetição do teste de "+name+" diverge do procedimento.");}
                if(procedureMentionsConcentration(procedure)&&!spec.optBoolean("concentration",false))return fail("A concentração de "+name+" não foi preservada.");
                if(heroHasCondition(hero,name)){String stacking=conditionEffect.optString("stacking");if(stacking.length()==0)return fail(name+" já está presente e o procedimento não define acumulação ou substituição.");String declared=spec.optString("stacking").trim();if(!stacking.equals(declared))return fail("A acumulação/substituição de "+name+" diverge do procedimento.");}
            }
        }}
        return new Result(true,"Condições verificadas semanticamente.",new JSONObject());
    }

    private static JSONObject matchingConditionEffect(JSONObject procedure,String condition,String operation){JSONArray effects=procedure==null?null:procedure.optJSONArray("effects");if(effects==null)return null;for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect==null||!operation.equals(effect.optString("condition_operation")))continue;JSONArray names=effect.optJSONArray("conditions");if(names!=null)for(int j=0;j<names.length();j++)if(norm(names.optString(j)).equals(norm(condition)))return effect;}return null;}
    private static boolean procedureSupportsCondition(JSONObject procedure,String condition,String operation){return matchingConditionEffect(procedure,condition,operation)!=null;}
    private static int firstProcedureDc(JSONObject procedure){JSONArray checks=procedure==null?null:procedure.optJSONArray("checks");if(checks!=null)for(int i=0;i<checks.length();i++){JSONObject check=checks.optJSONObject(i);if(check!=null&&check.optInt("dc",0)>0)return check.optInt("dc");}return procedure==null?0:procedure.optInt("dc",0);}
    private static boolean procedureSourceMatches(JSONObject procedure,String origin){JSONArray sources=procedure==null?null:procedure.optJSONArray("sources");if(sources==null)return false;for(int i=0;i<sources.length();i++){JSONObject source=sources.optJSONObject(i);if(source!=null&&(sourceMentions(source.optString("rule_id"),origin)||sourceMentions(source.optString("book"),origin)))return true;}return false;}
    private static boolean heroHasCondition(JSONObject hero,String condition){String existing=hero==null?"":hero.optString("conditions");JSONObject sheet=parseObject(hero==null?"":hero.optString("sheet_json","{}"));existing+=" "+sheet.optString("conditions");return sourceMentions(existing,condition);}
    private static boolean procedureMentionsConcentration(JSONObject procedure){JSONArray effects=procedure==null?null:procedure.optJSONArray("effects");if(effects!=null)for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect!=null&&effect.optBoolean("concentration",false))return true;}return false;}

    private static Result verifyItemChanges(JSONObject update,JSONObject hero,String excerpt,JSONObject procedure){
        for(String key:new String[]{"add_items","remove_items"}){JSONArray values=update.optJSONArray(key);if(values==null)continue;boolean add="add_items".equals(key);for(int i=0;i<values.length();i++){Object raw=values.opt(i);JSONObject spec=raw instanceof JSONObject?(JSONObject)raw:null;if(spec==null)return fail("Itens mecânicos exigem objeto com operação, quantidade e origem.");String name=spec.optString("name").trim();int quantity=spec.optInt("quantity",0);if(name.length()==0||quantity<=0)return fail("Item sem nome ou quantidade positiva.");JSONObject effect=matchingItemEffect(procedure,name,add);if(effect==null)return fail("O procedimento não autoriza "+(add?"conceder/comprar/fabricar/encontrar ":"vender/consumir/destruir/perder ")+name+".");String operation=effect.optString("item_operation"),declaredOperation=spec.optString("operation").trim();if(!operation.equals(declaredOperation))return fail("A operação de "+name+" deve ser "+operation+", não "+declaredOperation+".");int proven=effect.optInt("quantity",0);if(proven<=0&&quantity==1&&mentionsSingular(effect.optString("text"),name))proven=1;if(proven!=quantity)return fail("A quantidade de "+name+" não foi demonstrada pelo procedimento.");int stackBefore=inventoryQuantity(hero,name);if(spec.has("stack_before")&&spec.optInt("stack_before",-1)!=stackBefore)return fail("A pilha existente de "+name+" diverge do inventário.");if(!add&&stackBefore<quantity)return fail("O inventário não possui "+quantity+" unidade(s) de "+name+".");String origin=spec.optString("origin").trim();if(origin.length()==0)return fail("A origem de "+name+" é obrigatória.");if(("buy".equals(operation)||"find".equals(operation))&&(availableItemQuantity(hero,name,origin)<quantity))return fail("A origem "+origin+" não possui "+quantity+" unidade(s) verificáveis de "+name+".");if(!procedureSourceMatches(procedure,origin)&&!("buy".equals(operation)||"find".equals(operation))&&!localItemOriginExists(hero,name,origin))return fail("A origem de "+name+" não pertence à regra nem ao estado local.");Result metadata=verifyItemMetadata(spec,effect,excerpt,name);if(!metadata.valid)return metadata;if(add&&spec.has("unit_weight")){double carried=hero.optDouble("carried_weight",-1d),capacity=hero.optDouble("carrying_capacity",-1d);if(carried<0d||capacity<0d)return fail("Peso atual e capacidade de carga são obrigatórios para incluir "+name+".");if(carried+spec.optDouble("unit_weight",0d)*quantity>capacity)return fail("A inclusão de "+name+" excede o limite de carga.");}}}
        return new Result(true,"Itens verificados semanticamente.",new JSONObject());
    }

    private static JSONObject matchingItemEffect(JSONObject procedure,String name,boolean add){JSONArray effects=procedure==null?null:procedure.optJSONArray("effects");if(effects==null)return null;String[] allowed=add?new String[]{"grant","buy","craft","find"}:new String[]{"sell","consume","destroy","lose"};for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect==null||!sourceMentions(effect.optString("text"),name))continue;String operation=effect.optString("item_operation");for(String value:allowed)if(value.equals(operation))return effect;}return null;}
    private static boolean mentionsSingular(String text,String name){String semantic=semanticText(text),needle=norm(name);return Pattern.compile("(?iu)\\b(?:um|uma|one|a|an)\\s+"+Pattern.quote(needle)+"\\b").matcher(semantic).find();}
    private static int inventoryQuantity(JSONObject hero,String name){int total=0;JSONObject sheet=parseObject(hero==null?"":hero.optString("sheet_json","{}"));JSONArray inventory=hero==null?null:hero.optJSONArray("inventory");if(inventory==null)inventory=sheet.optJSONArray("inventory");if(inventory!=null)for(int i=0;i<inventory.length();i++){Object raw=inventory.opt(i);if(raw instanceof JSONObject){JSONObject item=(JSONObject)raw;if(norm(item.optString("name")).equals(norm(name)))total+=Math.max(0,item.optInt("quantity",1));}else if(norm(String.valueOf(raw)).equals(norm(name)))total++;}String equipment=hero==null?"":hero.optString("equipment");if(total==0&&sourceMentions(equipment,name))total=1;return total;}
    private static Result verifyItemMetadata(JSONObject spec,JSONObject effect,String excerpt,String name){String text=effect.optString("text")+" "+excerpt;for(String key:new String[]{"value","rarity"})if(spec.has(key)&&spec.optString(key).trim().length()>0&&!sourceMentions(text,spec.optString(key)))return fail("O campo "+key+" de "+name+" não aparece na regra.");if(spec.has("unit_weight")){double weight=spec.optDouble("unit_weight",-1d);if(weight<0d||!Pattern.compile("(?iu)(?<!\\d)"+Pattern.quote(trimDecimal(weight))+"(?:[.,]0+)?\\s*(?:kg|lb|lbs|libras?|quilogramas?)\\b").matcher(text).find())return fail("O peso de "+name+" não foi demonstrado pela regra.");}if(spec.has("max_charges")){int charges=spec.optInt("max_charges",-1);if(charges<0||!labeledNumber(text,charges,"cargas","charges","carga máxima","maximum charges"))return fail("O limite de cargas de "+name+" não foi demonstrado.");}if(spec.has("identified")){boolean identified=spec.optBoolean("identified",false);String n=norm(text);if(identified&&!contains(n,"identificado","identified"))return fail("A identificação de "+name+" não foi demonstrada.");if(!identified&&!contains(n,"nao identificado","não identificado","unidentified"))return fail("O estado não identificado de "+name+" não foi demonstrado.");}if(spec.has("attunement")){boolean attunement=spec.optBoolean("attunement",false);String n=norm(text);if(attunement&&!contains(n,"sintonia","attunement"))return fail("A sintonia de "+name+" não foi demonstrada.");if(!attunement&&!contains(n,"sem sintonia","nao requer sintonia","não requer sintonia","no attunement"))return fail("A ausência de sintonia de "+name+" não foi demonstrada.");}return new Result(true,"",new JSONObject());}
    private static String trimDecimal(double value){long whole=(long)value;return value==whole?String.valueOf(whole):String.valueOf(value).replace('.',',');}
    private static int availableItemQuantity(JSONObject hero,String name,String origin){int total=0;for(String key:new String[]{"available_items","scenario_items","npc_inventory"}){JSONArray items=hero==null?null:hero.optJSONArray(key);if(items==null)continue;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null||!norm(item.optString("name")).equals(norm(name)))continue;String owner=item.optString("origin",item.optString("owner",item.optString("source")));if(sourceMentions(owner,origin))total+=Math.max(0,item.optInt("quantity",1));}}return total;}
    private static boolean localItemOriginExists(JSONObject hero,String name,String origin){return availableItemQuantity(hero,name,origin)>0;}

    private static Integer resourceDelta(Object raw,int before){if(raw instanceof Number)return ((Number)raw).intValue();if(raw instanceof JSONObject){JSONObject change=(JSONObject)raw;if(change.has("delta"))return change.optInt("delta");if(change.has("set"))return change.optInt("set",before)-before;}return null;}
    private static Integer integerValue(Object raw){return raw instanceof Number?((Number)raw).intValue():null;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception ignored){return new JSONObject();}}
    private static String canonicalResource(String key){String n=norm(key).replaceAll("\\s+","_");if(n.startsWith("spell_slot")||n.startsWith("spell_slots")||n.startsWith("espaco_de_magia"))return "spell_slots";if(n.startsWith("hit_die")||n.startsWith("hit_dice")||n.startsWith("dado_de_vida"))return "hit_dice";if(n.contains("focus")||n.contains("foco")||"ki".equals(n))return "focus";if(n.contains("charge")||n.contains("carga"))return "charges";if(n.contains("exhaust")||n.contains("exaust"))return "exhaustion";return n;}
    private static JSONObject findCreatureBlock(JSONArray blocks,String name){if(blocks==null)return null;String wanted=norm(name);for(int i=0;i<blocks.length();i++){JSONObject block=blocks.optJSONObject(i);if(block!=null&&wanted.equals(norm(block.optString("name"))))return block;}return null;}
    private static String firstNonEmpty(String a,String b){return a!=null&&a.trim().length()>0?a:b;}
    private static String semanticText(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9+\\-:=]+"," ").replaceAll("\\s+"," ").trim();}

    private static final class ResourceState{final String key;final boolean exists;final int current,max,min,requiredLevel;final boolean canSpend,canRecover;final String ownerClass,ownerAbility,ownerSystem,recoveryCondition;ResourceState(String k,boolean e,int c,int x,int n,boolean s,boolean r,String oc,String oa,String os,int rl,String recovery){key=k;exists=e;current=c;max=x;min=n;canSpend=s;canRecover=r;ownerClass=norm(oc);ownerAbility=norm(oa);ownerSystem=norm(os);requiredLevel=Math.max(0,rl);recoveryCondition=norm(recovery);}}

    private static boolean labeledNumber(String excerpt,int value,String... labels){
        String text=excerpt==null?"":excerpt;String token=String.valueOf(value);
        for(String label:labels){String q=Pattern.quote(label);if(Pattern.compile("(?iu)\\b"+q+"\\b\\s*[:=]?\\s*"+Pattern.quote(token)+"(?!\\d)").matcher(text).find())return true;if(Pattern.compile("(?iu)(?<!\\d)"+Pattern.quote(token)+"\\s*\\b"+q+"\\b").matcher(text).find())return true;}
        return false;
    }

    public JSONObject bestEvidenceForTest(JSONObject test,JSONObject retrieval){/*BR_JSON_WRAP*/try{
        JSONArray evidence=retrieval==null?null:retrieval.optJSONArray("evidence");if(evidence==null)return null;
        JSONObject best=null,bestProcedure=null;int bestScore=Integer.MIN_VALUE;
        for(int i=0;i<evidence.length();i++){
            JSONObject source=evidence.optJSONObject(i);if(source==null)continue;JSONObject compiled=findCompiledRule(source,retrieval);JSONObject procedure=bestMatchingProcedureForTest(test,compiled);if(procedure==null)continue;
            int score=source.optInt("score",0)/10+procedure.optInt("coherence_score",0);
            String hay=norm(procedure.optString("scope_text")+" "+procedure.optJSONArray("checks"));
            for(String term:testTerms(test))if(term.length()>=3&&hay.contains(term))score+=25;
            if(score>bestScore){bestScore=score;best=source;bestProcedure=procedure;}
        }
        if(best==null||bestProcedure==null)return null;
        try{best.put("_matched_procedure_id",bestProcedure.optString("procedure_id"));best.put("_matched_procedure",bestProcedure);}catch(Exception error){return null;}
        return best;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject bestMatchingProcedureForTest(JSONObject test,JSONObject compiled){
        if(compiled==null)return null;JSONArray procedures=compiled.optJSONArray("atomic_procedures");if(procedures==null)return null;
        String requestedKind=norm(test==null?"":test.optString("type"));String ability=norm(test==null?"":test.optString("ability"));String skill=norm(test==null?"":test.optString("skill"));String save=norm(test==null?"":test.optString("save_type"));String thief=norm(test==null?"":test.optString("thief_skill"));
        JSONObject best=null;int bestScore=-1;
        for(int i=0;i<procedures.length();i++){
            JSONObject procedure=procedures.optJSONObject(i);if(procedure==null||!procedure.optBoolean("executable",false))continue;JSONArray checks=procedure.optJSONArray("checks");if(checks==null||checks.length()==0)continue;
            int score=procedure.optInt("coherence_score",0);String all=norm(procedure.optString("scope_text")+" "+checks.toString());
            if(requestedKind.length()>0){if(requestedKind.contains("save")&&contains(all,"save","saving throw","jogada de protecao","salvaguarda"))score+=50;else if(requestedKind.contains("attack")&&contains(all,"attack","ataque"))score+=50;else if(requestedKind.contains("ability")&&contains(all,"ability check","teste de"))score+=35;}
            if(ability.length()>0){if(!all.contains(ability))continue;score+=45;}
            if(skill.length()>0){if(!all.contains(skill))continue;score+=55;}
            if(save.length()>0){if(!all.contains(save))continue;score+=55;}
            if(thief.length()>0){if(!all.contains(thief))continue;score+=55;}
            if(score>bestScore){bestScore=score;best=procedure;}
        }
        return best;
    }

    private static String[] testTerms(JSONObject test){String text=(test==null?"":test.optString("type"))+" "+(test==null?"":test.optString("ability"))+" "+(test==null?"":test.optString("skill"))+" "+(test==null?"":test.optString("save_type"))+" "+(test==null?"":test.optString("thief_skill"));return norm(text).split("\s+");}

    private Result verifyNamedChanges(JSONObject update,String addKey,String removeKey,String excerpt,JSONObject compiled,String label){
        for(String key:new String[]{addKey,removeKey}){
            JSONArray values=update.optJSONArray(key);if(values==null)continue;
            for(int i=0;i<values.length();i++){
                String value=values.optString(i).trim();if(value.length()==0)continue;
                if(!sourceMentions(excerpt,value)&&!compiledMentions(compiled,value))return fail("A fonte recuperada não fundamenta o "+label+" ‘"+value+"’. ");
            }
        }
        return new Result(true,"",new JSONObject());
    }

    private Integer verifiedMagnitude(JSONObject calculation,String excerpt,JSONObject compiled,JSONObject hero){
        String formula=compactFormula(calculation.optString("roll_formula",calculation.optString("formula")));
        if(formula.length()>0){
            int total=calculation.optInt("roll_total",calculation.optInt("total",Integer.MIN_VALUE));if(total==Integer.MIN_VALUE)return null;
            int flat=calculation.optInt("flat_modifier",calculation.optInt("modifier",0));
            String modifierSource=calculation.optString("modifier_source",calculation.optString("ability_modifier_source"));
            if(modifierSource.trim().length()==0&&calculation.optString("ability").trim().length()>0)modifierSource=calculation.optString("ability")+"_mod";
            boolean exactFormula=formulaPresent(formula,excerpt,compiled);
            if(!exactFormula&&!symbolicFormulaAllowed(formula,modifierSource,excerpt,compiled,hero))return null;
            if(!recentRollMatches(calculation,hero))return null;
            int multiplier=Math.max(1,Math.min(20,calculation.optInt("multiplier",1)));
            if(multiplier!=1&&!multiplierAllowed(multiplier,excerpt,compiled))return null;
            if(flat!=0&&!modifierAllowed(flat,formula,excerpt,compiled)&&!abilityModifierAllowed(flat,modifierSource,excerpt,compiled,hero))return null;
            int computed=total*multiplier+flat;
            int divideBy=Math.max(1,Math.min(100,calculation.optInt("divide_by",calculation.optInt("divisor",1))));
            if(divideBy>1){
                if(!divisionAllowed(divideBy,excerpt,compiled))return null;
                String rounding=norm(calculation.optString("rounding","floor"));
                if(contains(rounding,"ceil","ceiling","cima","acima"))computed=(computed+divideBy-1)/divideBy;
                else if(contains(rounding,"nearest","mais proximo","arredondar"))computed=(int)Math.round(computed/(double)divideBy);
                else computed=Math.floorDiv(computed,divideBy);
            }
            if(calculation.has("computed_value")&&calculation.optInt("computed_value")!=computed)return null;
            return Math.abs(computed);
        }
        int fixed=calculation.optInt("fixed_value",Integer.MIN_VALUE);if(fixed==Integer.MIN_VALUE)fixed=calculation.optInt("computed_value",Integer.MIN_VALUE);
        if(fixed==Integer.MIN_VALUE)return null;
        if(!fixedValuePresent(fixed,excerpt,compiled))return null;
        return Math.abs(fixed);
    }

    private boolean recentRollMatches(JSONObject calculation,JSONObject hero){
        long rollId=calculation==null?0:calculation.optLong("roll_id",0);
        if(rollId<=0)return false;
        JSONObject roll=db.getDiceHistoryById(rollId);
        MechanicalRollBinding.Result result=MechanicalRollBinding.verify(roll,calculation,hero,System.currentTimeMillis());
        return result.valid;
    }

    private static JSONObject findAtomicProcedure(JSONObject compiled,JSONObject basis,JSONObject calculation){
        if(compiled==null)return null;
        JSONArray procedures=compiled.optJSONArray("atomic_procedures");
        if(procedures==null||procedures.length()==0)return null;
        String desired=calculation==null?"":calculation.optString("procedure_id","").trim();
        if(desired.length()==0&&basis!=null)desired=basis.optString("procedure_id","").trim();
        JSONObject only=null;int executable=0;
        for(int i=0;i<procedures.length();i++){
            JSONObject procedure=procedures.optJSONObject(i);if(procedure==null||!procedure.optBoolean("executable",false))continue;
            executable++;
            if(desired.length()>0&&desired.equals(procedure.optString("procedure_id")))return procedure;
            only=procedure;
        }
        if(desired.length()>0)return null;
        return executable==1?only:null;
    }

    private static String procedureFailureReason(JSONObject compiled,JSONObject basis,JSONObject calculation){
        if(compiled==null)return "A passagem recuperada não foi compilada em procedimento mecânico.";
        JSONArray procedures=compiled.optJSONArray("atomic_procedures");
        int executable=0;if(procedures!=null)for(int i=0;i<procedures.length();i++){JSONObject p=procedures.optJSONObject(i);if(p!=null&&p.optBoolean("executable",false))executable++;}
        String desired=calculation==null?"":calculation.optString("procedure_id","").trim();if(desired.length()==0&&basis!=null)desired=basis.optString("procedure_id","").trim();
        if(desired.length()>0)return "procedure_id não corresponde a um procedimento executável da fonte.";
        if(executable>1)return "A fonte contém múltiplos procedimentos; procedure_id é obrigatório.";
        return "A fonte não contém procedimento atômico executável e coerente.";
    }

    private static JSONObject findSource(JSONObject basis,JSONObject retrieval){
        JSONArray evidence=retrieval==null?null:retrieval.optJSONArray("evidence");if(evidence==null)return null;
        String ruleId=basis.optString("rule_id",basis.optString("source_rule_id"));String book=norm(basis.optString("book"));int page=basis.optInt("page",0);String excerpt=basis.optString("excerpt");
        for(int i=0;i<evidence.length();i++){
            JSONObject source=evidence.optJSONObject(i);if(source==null)continue;
            if(ruleId.length()>0&&ruleId.equals(source.optString("rule_id")))return source;
            boolean bookOk=book.length()>0&&(norm(source.optString("book")).contains(book)||book.contains(norm(source.optString("book"))));
            if(bookOk&&page==source.optInt("page")&&overlap(excerpt,source.optString("excerpt"))>=0.45d)return source;
        }
        return null;
    }

    private static JSONObject findCompiledRule(JSONObject source,JSONObject retrieval){
        JSONArray rules=retrieval==null?null:retrieval.optJSONArray("structured_rules");if(rules==null||source==null)return null;String id=source.optString("rule_id");
        for(int i=0;i<rules.length();i++){JSONObject rule=rules.optJSONObject(i);if(rule!=null&&id.equals(rule.optString("rule_id")))return rule;}return null;
    }

    private static boolean formulaPresent(String formula,String excerpt,JSONObject compiled){
        String f=compactFormula(formula);if(compactFormula(excerpt).contains(f))return true;JSONArray formulas=compiled==null?null:compiled.optJSONArray("dice_expressions");if(formulas!=null)for(int i=0;i<formulas.length();i++)if(compactFormula(formulas.optString(i)).equals(f))return true;return false;
    }
    private static boolean fixedValuePresent(int value,String excerpt,JSONObject compiled){
        JSONArray effects=compiled==null?null:compiled.optJSONArray("effects");
        if(effects!=null)for(int i=0;i<effects.length();i++){
            JSONObject effect=effects.optJSONObject(i);
            if(effect!=null&&effect.has("fixed_value")&&Math.abs(effect.optInt("fixed_value"))==Math.abs(value))return true;
        }
        // Não basta o número aparecer em qualquer lugar da página (pode ser
        // alcance, nível ou página). Ele precisa estar ligado a um verbo de
        // efeito na mesma oração.
        String token=String.valueOf(Math.abs(value));
        String pattern="(?iu)(?:sofre|recebe|causa|recupera|cura|perde|ganha|consome|gasta|takes?|deals?|heals?|regains?|loses?)\\s+"+Pattern.quote(token)+"(?!\\d)";
        return Pattern.compile(pattern).matcher(excerpt==null?"":excerpt).find();
    }
    private static boolean multiplierAllowed(int multiplier,String excerpt,JSONObject compiled){
        if(multiplier<=1)return true;String n=norm(excerpt+" "+(compiled==null?"":compiled.toString()));
        if(n.contains("x "+multiplier)||n.contains("x"+multiplier)||n.contains(multiplier+" vezes")||n.contains(multiplier+" times"))return true;
        return multiplier==2&&contains(n,"dobro","duas vezes","double","twice");
    }
    private static boolean modifierAllowed(int modifier,String formula,String excerpt,JSONObject compiled){
        String signed=(modifier>=0?"+":"")+modifier;
        if(compactFormula(formula).contains(signed))return true;
        String all=(excerpt==null?"":excerpt)+" "+(compiled==null?"":compiled.toString());
        return Pattern.compile("(?<!\\d)"+Pattern.quote(signed)+"(?!\\d)").matcher(all.replaceAll("\\s+","")).find();
    }
    private static boolean symbolicFormulaAllowed(String formula,String modifierSource,String excerpt,JSONObject compiled,JSONObject hero){
        String base=diceBase(formula);if(base.length()==0)return false;
        String all=compactFormula((excerpt==null?"":excerpt)+" "+(compiled==null?"":compiled.toString()));
        if(!all.contains(base))return false;
        Integer expected=abilityModifier(hero,modifierSource);if(expected==null||!symbolicModifierMentioned(modifierSource,excerpt,compiled))return false;
        Integer embedded=trailingFormulaModifier(formula);
        return embedded==null||embedded.intValue()==expected.intValue();
    }
    private static boolean abilityModifierAllowed(int modifier,String modifierSource,String excerpt,JSONObject compiled,JSONObject hero){
        Integer expected=abilityModifier(hero,modifierSource);return expected!=null&&expected.intValue()==modifier&&symbolicModifierMentioned(modifierSource,excerpt,compiled);
    }
    private static Integer abilityModifier(JSONObject hero,String modifierSource){
        if(hero==null)return null;String n=norm(modifierSource);int score;
        if(contains(n,"strength","forca","str"))score=hero.optInt("str",10);
        else if(contains(n,"dexterity","destreza","dex"))score=hero.optInt("dex",10);
        else if(contains(n,"constitution","constituicao","con"))score=hero.optInt("con",10);
        else if(contains(n,"intelligence","inteligencia","int"))score=hero.optInt("int_score",10);
        else if(contains(n,"wisdom","sabedoria","wis"))score=hero.optInt("wis",10);
        else if(contains(n,"charisma","carisma","cha"))score=hero.optInt("cha",10);
        else return null;
        return (int)Math.floor((score-10)/2.0d);
    }
    private static boolean symbolicModifierMentioned(String modifierSource,String excerpt,JSONObject compiled){
        String all=norm((excerpt==null?"":excerpt)+" "+(compiled==null?"":compiled.toString()));String n=norm(modifierSource);
        if(contains(all,"ability modifier","modificador de habilidade","modificador do atributo"))return true;
        if(contains(n,"strength","forca","str"))return contains(all,"strength modifier","modificador de forca","modificador da forca");
        if(contains(n,"dexterity","destreza","dex"))return contains(all,"dexterity modifier","modificador de destreza","modificador da destreza");
        if(contains(n,"constitution","constituicao","con"))return contains(all,"constitution modifier","modificador de constituicao","modificador da constituicao");
        if(contains(n,"intelligence","inteligencia","int"))return contains(all,"intelligence modifier","modificador de inteligencia","modificador da inteligencia");
        if(contains(n,"wisdom","sabedoria","wis"))return contains(all,"wisdom modifier","modificador de sabedoria","modificador da sabedoria");
        if(contains(n,"charisma","carisma","cha"))return contains(all,"charisma modifier","modificador de carisma","modificador do carisma");
        return false;
    }
    private static String diceBase(String formula){Matcher matcher=DICE.matcher(formula==null?"":formula);if(!matcher.find())return "";String value=matcher.group();return compactFormula(value).replaceAll("[+\\-]\\d+$","");}
    private static Integer trailingFormulaModifier(String formula){Matcher matcher=Pattern.compile("([+\\-])(\\d+)$").matcher(compactFormula(formula));if(!matcher.find())return null;int value=Integer.parseInt(matcher.group(2));return "-".equals(matcher.group(1))?-value:value;}
    private static boolean divisionAllowed(int divisor,String excerpt,JSONObject compiled){
        String n=norm((excerpt==null?"":excerpt)+" "+(compiled==null?"":compiled.toString()));
        if(divisor==2&&contains(n,"metade","meio do dano","half damage","half as much"))return true;
        if(divisor==3&&contains(n,"um terco","a third","one third"))return true;
        return n.contains("divide por "+divisor)||n.contains("dividido por "+divisor)||n.contains("divided by "+divisor);
    }
    private static boolean resourceGrounded(String excerpt,JSONObject compiled,String key){
        if(sourceMentions(excerpt,key)||compiledMentions(compiled,key))return true;String n=norm(key);String all=norm((excerpt==null?"":excerpt)+" "+(compiled==null?"":compiled.toString()));
        if(n.startsWith("spell slots")||n.startsWith("spell slot"))return contains(all,"spell slot","spell slots","espaco de magia","espacos de magia","circulo de magia","nivel de magia");
        if(n.startsWith("hit dice")||n.startsWith("hit die"))return contains(all,"hit die","hit dice","dado de vida","dados de vida");
        if(n.contains("rage")||n.contains("furia"))return contains(all,"rage","furia");
        if(n.contains("ki")||n.contains("foco"))return contains(all,"ki","ponto de foco","pontos de foco");
        if(n.contains("sorcery")||n.contains("feiticaria"))return contains(all,"sorcery point","ponto de feiticaria");
        return false;
    }
    private static JSONObject copyObject(JSONObject source)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();if(source==null)return out;java.util.Iterator<String> keys=source.keys();while(keys.hasNext()){String key=keys.next();out.put(key,source.opt(key));}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean sourceMentions(String excerpt,String value){String needle=norm(value);if(needle.length()<2)return false;String hay=norm(excerpt);if(hay.contains(needle))return true;int hits=0,total=0;for(String term:needle.split("\\s+")){if(term.length()<3)continue;total++;if(hay.contains(term))hits++;}return total>0&&hits/(double)total>=0.66d;}
    private static boolean compiledMentions(JSONObject compiled,String value){if(compiled==null)return false;return sourceMentions(compiled.toString(),value);}
    private static double overlap(String a,String b){Set<String> left=new HashSet<String>(),right=new HashSet<String>();for(String x:norm(a).split("\\s+"))if(x.length()>3)left.add(x);for(String x:norm(b).split("\\s+"))if(x.length()>3)right.add(x);if(left.isEmpty()||right.isEmpty())return 0d;int hit=0;for(String x:left)if(right.contains(x))hit++;return hit/(double)Math.min(left.size(),right.size());}
    private static String compactFormula(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("\\s+","").replace("−","-");}
    private static boolean contains(String source,String... values){for(String value:values)if(source.contains(norm(value)))return true;return false;}
    private static Result fail(String reason){return new Result(false,reason,new JSONObject());}
    private static String safe(Throwable e){String m=e==null?"":e.getMessage();return m==null||m.trim().length()==0?(e==null?"erro":e.getClass().getSimpleName()):m.replaceAll("[\\r\\n]+"," ").trim();}
    private static String norm(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
