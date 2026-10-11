package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compila passagens de manual em procedimentos mecânicos atômicos.
 *
 * A unidade executável não é mais a página inteira. Cada procedimento mantém
 * somente as frases que realmente pertencem ao mesmo gatilho/teste/efeito. Isso
 * impede que um dado encontrado em um parágrafo seja combinado com um efeito
 * não relacionado em outro parágrafo da página.
 */
public final class RuleProcedureCompiler {
    private static final Pattern DICE=Pattern.compile("(?iu)\\b(\\d{1,3})?d(\\d{1,4})(?:\\s*([+\\-])\\s*(\\d{1,3}))?\\b");
    private static final Pattern DC=Pattern.compile("(?iu)\\b(?:CD|DC|classe de dificuldade|difficulty class|teste contra|save dc)\\s*[:=]?\\s*(\\d{1,2})\\b");
    private static final Pattern ATTACK_BONUS=Pattern.compile("(?iu)\\b(?:b[oô]nus de ataque|attack bonus|to hit)\\s*[:=]?\\s*([+\\-]?\\d{1,2})\\b");
    private static final Pattern ARMOR_CLASS=Pattern.compile("(?iu)\\b(?:CA|AC|classe de armadura|armor class)\\s*[:=]?\\s*(\\d{1,2})\\b");
    private static final Pattern NUMBER_UNIT=Pattern.compile("(?iu)\\b(\\d+(?:[.,]\\d+)?)\\s*(p[eé]s?|feet|ft|metros?|m|quil[oô]metros?|km|milhas?|miles?|horas?|hours?|dias?|days?|rodadas?|rounds?|turnos?|turns?|minutos?|minutes?|po|pp|pc|gp|sp|cp|%)\\b");
    private static final Pattern FIXED_EFFECT=Pattern.compile("(?iu)\\b(?:sofre|recebe|causa|recupera|cura|perde|ganha|consome|gasta|reduz|costs?|takes?|deals?|heals?|regains?|loses?|reduces?)\\s+(\\d{1,4})\\b");
    private static final Pattern BONUS=Pattern.compile("(?iu)(?:b[oô]nus|penalidade|modifier|modificador|penalty)\\s*(?:de|of|:|=)?\\s*([+\\-]\\s*\\d{1,2})");
    private static final Pattern DURATION=Pattern.compile("(?iu)\\b(?:por|durante|for|until|até|ate)\\s+(?:até\\s+|ate\\s+|up to\\s+)?(\\d+(?:[.,]\\d+)?)?\\s*(rodadas?|rounds?|turnos?|turns?|minutos?|minutes?|horas?|hours?|dias?|days?|até o fim do próximo turno|ate o fim do proximo turno|until the end of your next turn)\\b");
    private static final Pattern RANGE=Pattern.compile("(?iu)\\b(?:alcance|range|within|a até|a ate|até|ate)\\s*(?:de|of|:)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(p[eé]s?|feet|ft|metros?|m|quil[oô]metros?|km|milhas?|miles?)\\b");
    private static final Pattern TARGET_COUNT=Pattern.compile("(?iu)\\b(?:até|ate|up to|uma|um|one|cada|each|todas?|todos?|all)\\s+(\\d+\\s+)?(?:criatura|criaturas|creature|creatures|alvo|alvos|target|targets|objeto|objetos|object|objects|aliado|aliados|ally|allies|inimigo|inimigos|enemy|enemies)\\b");
    private static final Pattern AREA=Pattern.compile("(?iu)\\b(?:cone|cubo|cube|esfera|sphere|cilindro|cylinder|linha|line|raio|radius)\\s*(?:de|of|:)?\\s*(\\d+(?:[.,]\\d+)?)?\\s*(p[eé]s?|feet|ft|metros?|m)?\\b");
    private static final Pattern ACTION_PHRASE=Pattern.compile("(?iu)\\b(?:como|com|usando|as)\\s+(?:uma|an)?\\s*(?:ação|acao|action)\\b");
    private static final Pattern BONUS_ACTION_PHRASE=Pattern.compile("(?iu)\\b(?:ação bônus|acao bonus|bonus action)\\b");
    private static final Pattern REACTION_PHRASE=Pattern.compile("(?iu)\\b(?:reação|reacao|reaction)\\b");
    private static final Pattern CROSS_REFERENCE=Pattern.compile("(?iu)\\b(?:consulte|veja|conforme|de acordo com|na tabela|tabela anterior|página|pagina|capítulo|capitulo|see|refer to|as shown|the table|previous table|chapter|page)\\b");
    private static final Pattern CONCENTRATION=Pattern.compile("(?iu)\\b(?:concentração|concentracao|concentration)\\b");
    private static final Pattern ADVANTAGE=Pattern.compile("(?iu)\\b(?:vantagem|advantage)\\b");
    private static final Pattern DISADVANTAGE=Pattern.compile("(?iu)\\b(?:desvantagem|disadvantage)\\b");
    private static final Pattern LEVEL_PREREQUISITE=Pattern.compile("(?iu)\\b(?:nível|nivel|level)\\s*(\\d{1,2})\\b");
    private static final Pattern REPEAT_TIMING=Pattern.compile("(?iu)\\b(?:ao final|no final|no início|no inicio|at the end|at the start|repete|repeat|novamente|again)\\b[^.!?]{0,100}\\b(?:turno|turn|rodada|round|dia|day|descanso|rest)\\b");
    private static final Pattern EXPLICIT_QUANTITY=Pattern.compile("(?iu)\\b(?:gasta|gaste|gastam|consome|consuma|consumir|perde|reduz|reduzir|recupera|recupere|ganha|recebe|concede|compra|vende|fabrica|encontra|destrói|destroi|spends?|costs?|consumes?|loses?|reduces?|regains?|recovers?|gains?|grants?|buys?|sells?|crafts?|finds?|destroys?)\\s+(exatamente\\s+|exactly\\s+)?(\\d{1,4}|um|uma|one|dois|duas|two|três|tres|three|metade|half|todos?|todas?|all)\\b");

    private static final String[] CONDITIONS={
            "Agarrado","Agarrada","Amedrontado","Amedrontada","Atordoado","Atordoada","Caído","Caída","Cego","Cega","Contido","Contida","Enfeitiçado","Enfeitiçada","Envenenado","Envenenada",
            "Exaustão","Incapacitado","Incapacitada","Inconsciente","Invisível","Paralisado","Paralisada","Petrificado","Petrificada","Restrained",
            "Grappled","Prone","Blinded","Frightened","Poisoned","Stunned","Unconscious","Charmed","Deafened","Exhaustion"
    };
    private static final String[] ABILITIES={"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma","Strength","Dexterity","Constitution","Intelligence","Wisdom","Charisma"};
    private static final String[] SKILLS={"Acrobacia","Arcanismo","Atletismo","Atuação","Enganação","Furtividade","História","Intimidação","Intuição","Investigação","Lidar com Animais","Medicina","Natureza","Percepção","Persuasão","Prestidigitação","Religião","Sobrevivência","Acrobatics","Arcana","Athletics","Deception","History","Insight","Intimidation","Investigation","Medicine","Nature","Perception","Performance","Persuasion","Religion","Sleight of Hand","Stealth","Survival"};
    private static final String[] DAMAGE_TYPES={"ácido","acido","acid","concussão","concussao","bludgeoning","cortante","slashing","perfurante","piercing","fogo","fire","frio","cold","elétrico","eletrico","lightning","necrótico","necrotico","necrotic","psíquico","psiquico","psychic","radiante","radiant","trovejante","thunder","veneno","poison","força","force"};

    private RuleProcedureCompiler(){}

    public static JSONObject compile(JSONObject source,JSONArray intents)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        String excerpt=clean(source==null?"":source.optString("excerpt"));
        String ruleId=source==null?"":source.optString("rule_id");
        List<String> sentences=splitSentences(excerpt);
        JSONArray sentenceRules=new JSONArray();
        for(int i=0;i<sentences.size()&&i<120;i++)sentenceRules.put(compileSentence(sentences.get(i),i+1));

        JSONArray procedures=buildAtomicProcedures(sentenceRules,ruleId,source);
        JSONArray formulas=new JSONArray(),numbers=new JSONArray(),checks=new JSONArray(),effects=new JSONArray(),conditions=new JSONArray(),triggers=new JSONArray(),exceptions=new JSONArray(),steps=new JSONArray(),durations=new JSONArray(),ranges=new JSONArray(),targets=new JSONArray(),successClauses=new JSONArray(),failureClauses=new JSONArray(),actionEconomy=new JSONArray(),mechanicKeys=new JSONArray(),ambiguity=new JSONArray();
        int executableCount=0,maxConfidence=0,crossRefs=0;
        for(int i=0;i<sentenceRules.length();i++){
            JSONObject atom=sentenceRules.optJSONObject(i);if(atom==null)continue;
            appendUnique(formulas,atom.optJSONArray("dice_expressions"));appendUnique(numbers,atom.optJSONArray("numeric_terms"));appendUnique(checks,atom.optJSONArray("checks"));appendUnique(effects,atom.optJSONArray("effects"));appendUnique(conditions,atom.optJSONArray("conditions"));appendUnique(triggers,atom.optJSONArray("triggers"));appendUnique(exceptions,atom.optJSONArray("exceptions"));appendUnique(durations,atom.optJSONArray("durations"));appendUnique(ranges,atom.optJSONArray("ranges"));appendUnique(targets,atom.optJSONArray("targets"));appendUnique(successClauses,atom.optJSONArray("success_clauses"));appendUnique(failureClauses,atom.optJSONArray("failure_clauses"));appendUnique(actionEconomy,atom.optJSONArray("action_economy"));appendUnique(mechanicKeys,atom.optJSONArray("mechanic_keys"));
            if(atom.optBoolean("cross_reference",false))crossRefs++;
            if(atom.optBoolean("mechanical",false))steps.put(atom);
        }
        for(int i=0;i<procedures.length();i++){
            JSONObject procedure=procedures.optJSONObject(i);if(procedure==null)continue;
            if(procedure.optBoolean("executable",false))executableCount++;
            maxConfidence=Math.max(maxConfidence,procedure.optInt("coherence_score",0));
        }
        if(executableCount>1)ambiguity.put("multiple_independent_procedures");
        if(crossRefs>0)ambiguity.put("unresolved_cross_reference_present");
        if(procedures.length()==0&&excerpt.length()>0)ambiguity.put("no_atomic_procedure_compiled");

        out.put("rule_id",ruleId);
        out.put("intents",copyArray(intents));
        out.put("procedure_text",excerpt);
        out.put("dice_expressions",formulas);
        out.put("numeric_terms",numbers);
        out.put("checks",checks);
        out.put("effects",effects);
        out.put("conditions",conditions);
        out.put("triggers",triggers);
        out.put("exceptions",exceptions);
        out.put("procedure_steps",steps);
        out.put("sentence_rules",sentenceRules);
        out.put("atomic_procedures",procedures);
        out.put("action_economy",actionEconomy);
        out.put("durations",durations);
        out.put("ranges",ranges);
        out.put("targets",targets);
        out.put("success_clauses",successClauses);
        out.put("failure_clauses",failureClauses);
        out.put("mechanic_keys",mechanicKeys);
        out.put("ambiguity_flags",ambiguity);
        out.put("confidence",Math.min(100,maxConfidence));
        out.put("executable",executableCount>0);
        out.put("executable_procedure_count",executableCount);
        out.put("requires_procedure_id",executableCount>1);
        out.put("requires_local_roll",containsAnyProcedureFlag(procedures,"requires_local_roll"));
        out.put("source",new JSONObject().put("book",source==null?"":source.optString("book")).put("page",source==null?0:source.optInt("page")).put("excerpt",excerpt));
        out.put("creature_blocks",CreatureStatBlockCompiler.compile(source));
        out.put("application_policy","Use somente um atomic_procedure coerente. Se houver mais de um procedimento executável, calculation.procedure_id é obrigatório. Rolagens exigem roll_id vinculado ao teste, ator, cena e ação; números de frases diferentes não podem ser combinados.");
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject compileSentence(String sentence,int order)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject atom=new JSONObject();String n=norm(sentence);
        JSONArray formulas=extractDice(sentence),numbers=extractNumbers(sentence),checks=new JSONArray(),effects=new JSONArray(),conditions=findConditions(sentence),triggers=new JSONArray(),exceptions=new JSONArray(),durations=new JSONArray(),ranges=new JSONArray(),targets=new JSONArray(),success=new JSONArray(),failure=new JSONArray(),economy=new JSONArray(),mechanicKeys=new JSONArray(),areas=new JSONArray(),damageTypes=findDamageTypes(sentence),prerequisites=findPrerequisites(sentence),repetitions=new JSONArray();
        boolean trigger=isTrigger(n),exception=isException(n),check=isCheckSentence(n),cross=CROSS_REFERENCE.matcher(sentence).find();
        if(trigger)triggers.put(sentence);if(exception)exceptions.put(sentence);
        if(check){JSONObject c=compileCheck(sentence);checks.put(c);putUnique(mechanicKeys,"check:"+c.optString("kind"));}
        JSONObject effect=compileEffect(sentence,conditions,damageTypes);if(effect.length()>0){effects.put(effect);putUnique(mechanicKeys,"effect:"+effect.optString("kind"));}
        appendMatches(durations,DURATION,sentence);appendMatches(ranges,RANGE,sentence);appendMatches(targets,TARGET_COUNT,sentence);appendMatches(areas,AREA,sentence);
        if(BONUS_ACTION_PHRASE.matcher(sentence).find()){putUnique(economy,"bonus_action");putUnique(mechanicKeys,"economy:bonus_action");}
        if(REACTION_PHRASE.matcher(sentence).find()){putUnique(economy,"reaction");putUnique(mechanicKeys,"economy:reaction");}
        // Não usar a palavra genérica "action": reaction contém action em inglês.
        if(!BONUS_ACTION_PHRASE.matcher(sentence).find()&&!REACTION_PHRASE.matcher(sentence).find()&&ACTION_PHRASE.matcher(sentence).find()){putUnique(economy,"action");putUnique(mechanicKeys,"economy:action");}
        if(contains(n,"em um sucesso","se obtiver sucesso","on a success","if successful","sucesso:"))success.put(sentence);
        if(contains(n,"em uma falha","se falhar","on a failure","if it fails","falha:"))failure.put(sentence);
        if(ADVANTAGE.matcher(sentence).find()&&!DISADVANTAGE.matcher(sentence).find()){putUnique(mechanicKeys,"roll:advantage");}
        if(DISADVANTAGE.matcher(sentence).find()){putUnique(mechanicKeys,"roll:disadvantage");}
        if(CONCENTRATION.matcher(sentence).find()){putUnique(mechanicKeys,"concentration");}
        appendMatches(repetitions,REPEAT_TIMING,sentence);
        if(contains(n,"resistencia a","resistance to","resistente a"))putUnique(mechanicKeys,"damage:resistance");
        if(contains(n,"vulnerabilidade a","vulnerability to","vulneravel a"))putUnique(mechanicKeys,"damage:vulnerability");
        if(contains(n,"imunidade a","immunity to","imune a"))putUnique(mechanicKeys,"damage:immunity");
        if(areas.length()>0){putUnique(mechanicKeys,"area");}
        Matcher atk=ATTACK_BONUS.matcher(sentence);if(atk.find()){atom.put("attack_bonus",Integer.parseInt(atk.group(1)));putUnique(mechanicKeys,"attack_bonus");}
        Matcher ac=ARMOR_CLASS.matcher(sentence);if(ac.find()){atom.put("armor_class",Integer.parseInt(ac.group(1)));putUnique(mechanicKeys,"armor_class");}
        boolean mechanical=check||effect.length()>0||economy.length()>0||conditions.length()>0||formulas.length()>0||atom.has("attack_bonus")||atom.has("armor_class");
        atom.put("order",order).put("text",sentence).put("mechanical",mechanical).put("cross_reference",cross)
                .put("dice_expressions",formulas).put("numeric_terms",numbers).put("checks",checks).put("effects",effects).put("conditions",conditions)
                .put("triggers",triggers).put("exceptions",exceptions).put("durations",durations).put("ranges",ranges).put("targets",targets).put("areas",areas)
                .put("success_clauses",success).put("failure_clauses",failure).put("action_economy",economy).put("damage_types",damageTypes).put("mechanic_keys",mechanicKeys)
                .put("prerequisites",prerequisites).put("repetitions",repetitions);
        return atom;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray buildAtomicProcedures(JSONArray atoms,String ruleId,JSONObject source)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();boolean[] consumed=new boolean[atoms.length()];
        for(int i=0;i<atoms.length();i++){
            if(consumed[i])continue;JSONObject first=atoms.optJSONObject(i);if(first==null||!first.optBoolean("mechanical",false))continue;
            ArrayList<JSONObject> group=new ArrayList<JSONObject>();group.add(first);consumed[i]=true;
            boolean needsOutcome=first.optJSONArray("effects").length()==0&&(first.optJSONArray("checks").length()>0||first.optJSONArray("triggers").length()>0||first.optJSONArray("action_economy").length()>0);
            if(needsOutcome){
                for(int j=i+1;j<atoms.length()&&j<=i+2;j++){
                    JSONObject next=atoms.optJSONObject(j);if(next==null)continue;
                    boolean newIndependent=next.optJSONArray("checks").length()>0||next.optJSONArray("triggers").length()>0||next.optJSONArray("action_economy").length()>0;
                    boolean continuation=next.optJSONArray("effects").length()>0||next.optJSONArray("success_clauses").length()>0||next.optJSONArray("failure_clauses").length()>0||next.optJSONArray("conditions").length()>0;
                    if(newIndependent&&!continuation)break;
                    if(continuation){group.add(next);consumed[j]=true;if(next.optJSONArray("effects").length()>0)break;}
                }
            }
            JSONObject procedure=mergeProcedure(group,ruleId,out.length()+1,source);out.put(procedure);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject mergeProcedure(List<JSONObject> group,String ruleId,int index,JSONObject source)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject p=new JSONObject();JSONArray orders=new JSONArray(),texts=new JSONArray(),checks=new JSONArray(),effects=new JSONArray(),conditions=new JSONArray(),triggers=new JSONArray(),exceptions=new JSONArray(),durations=new JSONArray(),ranges=new JSONArray(),targets=new JSONArray(),areas=new JSONArray(),success=new JSONArray(),failure=new JSONArray(),economy=new JSONArray(),formulas=new JSONArray(),numbers=new JSONArray(),mechanicKeys=new JSONArray(),damageTypes=new JSONArray(),prerequisites=new JSONArray(),repetitions=new JSONArray();
        boolean cross=false,sameSentenceCheckEffect=false,sameSentenceTriggerEffect=false;int mechanicalSentences=0;
        for(JSONObject atom:group){
            orders.put(atom.optInt("order"));texts.put(atom.optString("text"));if(atom.optBoolean("mechanical",false))mechanicalSentences++;
            if(atom.optJSONArray("checks").length()>0&&atom.optJSONArray("effects").length()>0)sameSentenceCheckEffect=true;
            if(atom.optJSONArray("triggers").length()>0&&atom.optJSONArray("effects").length()>0)sameSentenceTriggerEffect=true;
            cross|=atom.optBoolean("cross_reference",false);
            appendUnique(checks,atom.optJSONArray("checks"));appendUnique(effects,atom.optJSONArray("effects"));appendUnique(conditions,atom.optJSONArray("conditions"));appendUnique(triggers,atom.optJSONArray("triggers"));appendUnique(exceptions,atom.optJSONArray("exceptions"));appendUnique(durations,atom.optJSONArray("durations"));appendUnique(ranges,atom.optJSONArray("ranges"));appendUnique(targets,atom.optJSONArray("targets"));appendUnique(areas,atom.optJSONArray("areas"));appendUnique(success,atom.optJSONArray("success_clauses"));appendUnique(failure,atom.optJSONArray("failure_clauses"));appendUnique(economy,atom.optJSONArray("action_economy"));appendUnique(formulas,atom.optJSONArray("dice_expressions"));appendUnique(numbers,atom.optJSONArray("numeric_terms"));appendUnique(mechanicKeys,atom.optJSONArray("mechanic_keys"));appendUnique(damageTypes,atom.optJSONArray("damage_types"));
            appendUnique(prerequisites,atom.optJSONArray("prerequisites"));appendUnique(repetitions,atom.optJSONArray("repetitions"));
            if(atom.has("attack_bonus")&&!p.has("attack_bonus"))p.put("attack_bonus",atom.optInt("attack_bonus"));
            if(atom.has("armor_class")&&!p.has("armor_class"))p.put("armor_class",atom.optInt("armor_class"));
        }
        int coherence;
        if(sameSentenceCheckEffect)coherence=100;
        else if(sameSentenceTriggerEffect)coherence=95;
        else if(checks.length()>0&&effects.length()>0)coherence=88;
        else if(triggers.length()>0&&effects.length()>0)coherence=84;
        else if(effects.length()>0&&economy.length()>0)coherence=82;
        else if(effects.length()>0&&(formulas.length()>0||hasFixedEffect(effects)))coherence=80;
        else if(effects.length()>0&&hasGroundedQuantity(effects))coherence=80;
        else if(effects.length()>0&&hasConditionMutation(effects)&&(durations.length()>0||checks.length()>0))coherence=78;
        else if(effects.length()>0&&conditions.length()>0&&durations.length()>0)coherence=76;
        else if(effects.length()>0)coherence=62;
        else coherence=35;
        if(group.size()>2)coherence-=8;
        if(mechanicalSentences>2)coherence-=6;
        if(cross)coherence-=35;
        coherence=Math.max(0,Math.min(100,coherence));
        boolean executable=effects.length()>0&&coherence>=70&&!cross;
        p.put("procedure_id",(ruleId==null?"":ruleId)+":procedure:"+index).put("source_sentence_orders",orders).put("texts",texts)
                .put("checks",checks).put("effects",effects).put("conditions",conditions).put("triggers",triggers).put("exceptions",exceptions)
                .put("durations",durations).put("ranges",ranges).put("targets",targets).put("areas",areas).put("success_clauses",success).put("failure_clauses",failure)
                .put("action_economy",economy).put("dice_expressions",formulas).put("numeric_terms",numbers).put("mechanic_keys",mechanicKeys).put("damage_types",damageTypes)
                .put("cross_reference",cross).put("coherence_score",coherence).put("executable",executable).put("requires_local_roll",checks.length()>0||formulas.length()>0)
                .put("scope_text",join(texts," "));
        String system=source==null?"":source.optString("system",inferSystem(source));String edition=source==null?"":source.optString("edition",source.optString("rule_profile",""));String book=source==null?"":source.optString("book",source.optString("title",""));int pageStart=source==null?0:source.optInt("page",0),pageEnd=source==null?pageStart:source.optInt("page_end",pageStart);
        JSONArray costs=effectsByKind(effects,"resource_cost"),resources=resourceEffects(effects),sources=new JSONArray().put(new JSONObject().put("rule_id",ruleId==null?"":ruleId).put("book",book).put("page_start",pageStart).put("page_end",pageEnd).put("sentence_orders",orders));
        p.put("system",system).put("edition",edition).put("book",book).put("page_start",pageStart).put("page_end",pageEnd)
                .put("trigger",copyArray(triggers)).put("prerequisites",prerequisites).put("action_type",first(economy,inferActionType(checks)))
                .put("target_spec",copyArray(targets)).put("range_spec",copyArray(ranges)).put("test",copyArray(checks)).put("dc",firstDc(checks))
                .put("roll",copyArray(formulas)).put("formula",copyArray(formulas)).put("success",copyArray(success)).put("failure",copyArray(failure))
                .put("effect_spec",copyArray(effects)).put("duration",copyArray(durations)).put("repeat",repetitions).put("exception_spec",copyArray(exceptions))
                .put("costs",costs).put("resources",resources).put("sources",sources).put("confidence",coherence)
                .put("sistema",system).put("edicao",edition).put("livro",book).put("pagina_inicial",pageStart).put("pagina_final",pageEnd)
                .put("gatilho",copyArray(triggers)).put("pre_requisitos",prerequisites).put("tipo_de_acao",first(economy,inferActionType(checks)))
                .put("alvos",copyArray(targets)).put("alcance",copyArray(ranges)).put("teste",copyArray(checks)).put("CD",firstDc(checks))
                .put("rolagem",copyArray(formulas)).put("sucesso",copyArray(success)).put("falha",copyArray(failure)).put("efeitos",copyArray(effects))
                .put("duracao",copyArray(durations)).put("repeticao",repetitions).put("excecoes",copyArray(exceptions)).put("custos",costs).put("recursos",resources).put("fontes",sources).put("confianca",coherence)
                .put("review_required",!executable).put("non_executable_reason",executable?"":(cross?"Referência cruzada não resolvida.":"Coerência insuficiente para execução automática."));
        return p;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject compileCheck(String sentence)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();String n=norm(sentence);JSONArray abilities=new JSONArray(),skills=new JSONArray();
        for(String ability:ABILITIES)if(containsToken(n,norm(ability)))putUnique(abilities,ability);
        for(String skill:SKILLS)if(containsPhrase(n,norm(skill)))putUnique(skills,skill);
        Matcher dc=DC.matcher(sentence);if(dc.find())out.put("dc",Integer.parseInt(dc.group(1)));
        String kind=(contains(n,"saving throw","jogada de protecao","salvaguarda"))?"save":(contains(n,"attack roll","jogada de ataque","teste de ataque"))?"attack":"ability_check";
        String comparison=(contains(n,"igual ou abaixo","roll under","equal to or less"))?"roll_under":(contains(n,"igual ou acima","at least","equal to or greater"))?"roll_over":"dc_or_opposed";
        if(contains(n,"disputa","opposed","contested"))comparison="opposed";
        out.put("kind",kind).put("abilities",abilities).put("skills",skills).put("comparison",comparison).put("text",sentence);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject compileEffect(String sentence,JSONArray conditions,JSONArray damageTypes)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();String n=norm(sentence);String kind="",itemOperation=itemOperation(n);
        if(contains(n,"dano","damage","sofre","takes damage","causa dano","deals damage"))kind="damage";
        else if(contains(n,"cura","healing","recupera pontos de vida","regains hit points","heal"))kind="healing";
        else if(contains(n,"perde","gasta","consome","reduz","loses","spends","costs","consumes","reduces"))kind="resource_cost";
        else if(contains(n,"ganha","recebe","obtem","recupera","gains","receives","regains","recovers"))kind="resource_gain";
        else if(contains(n,"deslocamento","movimento","speed","movement"))kind="movement";
        else if(conditions.length()>0||contains(n,"condicao","condition","becomes blinded","fica cego","fica caido"))kind="condition";
        else if(contains(n,"classe de armadura","armor class"," ca "," ac "))kind="armor_class";
        else if(itemOperation.length()>0)kind="item";
        if(kind.length()==0)return out;
        out.put("kind",kind).put("text",sentence);
        String conditionOperation=conditionOperation(n,conditions);if(conditionOperation.length()>0)out.put("condition_operation",conditionOperation);
        String resourceOperation=resourceOperation(n);if(resourceOperation.length()>0){out.put("resource_operation",resourceOperation);String resource=resourceName(n);if(resource.length()>0)out.put("resource",resource);appendQuantity(out,sentence);}
        if(itemOperation.length()>0){out.put("item_operation",itemOperation);if(!out.has("quantity_mode"))appendQuantity(out,sentence);}
        JSONArray formulas=extractDice(sentence);if(formulas.length()>0)out.put("formulas",formulas);
        Matcher fixed=FIXED_EFFECT.matcher(sentence);if(fixed.find())out.put("fixed_value",Integer.parseInt(fixed.group(1)));
        Matcher bonus=BONUS.matcher(sentence);if(bonus.find())out.put("modifier",Integer.parseInt(bonus.group(1).replace(" ","")));
        JSONArray units=extractNumbers(sentence);if(units.length()>0)out.put("numeric_terms",units);
        if(conditions.length()>0)out.put("conditions",copyArray(conditions));
        if(conditions.length()>0){
            if(contains(n,"nao se acumula","não se acumula","substitui","replaces","does not stack"))out.put("stacking","replace");
            else if(contains(n,"renova a duracao","renova a duração","refreshes the duration"))out.put("stacking","refresh");
            else if(contains(n,"se acumula","acumula","stacks"))out.put("stacking","stack");
        }
        if(damageTypes.length()>0)out.put("damage_types",copyArray(damageTypes));
        if(CONCENTRATION.matcher(sentence).find())out.put("concentration",true);
        if(contains(n,"metade do dano","half damage","half as much"))out.put("divide_by",2).put("rounding","floor");
        if(contains(n,"nenhum dano","no damage"))out.put("success_zero",true);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static List<String> splitSentences(String excerpt){
        ArrayList<String> out=new ArrayList<String>();if(excerpt==null||excerpt.trim().length()==0)return out;
        String normalized=excerpt.replace('\u0000',' ').replace("\r\n","\n").replace('\r','\n');
        String[] paragraphs=normalized.split("\\n+");
        for(String paragraph:paragraphs){
            String p=clean(paragraph.replaceFirst("^\\s*(?:[-*•]|\\d{1,3}[.)])\\s+",""));if(p.length()==0)continue;
            Matcher m=Pattern.compile("(?s)(.*?[.!?](?=\\s|$)|.+$)").matcher(p);
            while(m.find()){String sentence=clean(m.group(1));if(sentence.length()>=4)out.add(sentence);}
        }
        return out;
    }

    private static JSONArray findPrerequisites(String sentence)throws Exception{
        JSONArray out=new JSONArray();String n=norm(sentence);Matcher level=LEVEL_PREREQUISITE.matcher(sentence==null?"":sentence);
        while(level.find())putUnique(out,"level:"+level.group(1));
        Matcher cls=Pattern.compile("(?iu)\\b(?:classe|class)\\s*[:=]?\\s*([\\p{L}][\\p{L} -]{1,40})").matcher(sentence==null?"":sentence);
        if(cls.find())putUnique(out,"class:"+clean(cls.group(1)).replaceAll("[.,;:].*$",""));
        if(contains(n,"descanso curto","short rest"))putUnique(out,"short_rest");
        if(contains(n,"descanso longo","long rest"))putUnique(out,"long_rest");
        if(CONCENTRATION.matcher(sentence==null?"":sentence).find())putUnique(out,"concentration");
        return out;
    }

    private static String conditionOperation(String n,JSONArray conditions){
        if(conditions==null||conditions.length()==0)return "";
        if(contains(n,"imune a","imunidade a","immune to","immunity to"))return "immune";
        if(contains(n,"vantagem contra","advantage against"))return "advantage_against";
        if(contains(n,"resistencia contra","resistência contra","resistance against"))return "resistance_against";
        if(contains(n,"impede","nao pode ficar","não pode ficar","prevents","cannot become"))return "prevent";
        if(contains(n,"remove","remova","termina a condicao","termina a condição","deixa de estar","remove the","ends the condition","is no longer"))return "remove";
        if(contains(n,"prolonga","estende a duracao","estende a duração","extends","prolongs"))return "extend_duration";
        if(contains(n,"reduz a duracao","reduz a duração","reduces the duration"))return "reduce_duration";
        if(contains(n,"suspende","suspensa","suspended","suspends"))return "suspend";
        if(contains(n,"fica","torna se","se torna","sofre a condicao","aplica a condicao","becomes","is poisoned","is blinded","is stunned","knocked prone","apply the condition"))return "apply";
        return "mention";
    }

    private static String resourceOperation(String n){
        if(contains(n,"gasta","gaste","consome","consuma","custa","spends","costs","consumes"))return "spend";
        if(contains(n,"perde","reduz","loses","reduces"))return "reduce";
        if(contains(n,"recupera","recupere","regains","recovers","restore"))return "recover";
        if(contains(n,"ganha","recebe","obtem","gains","receives","obtains"))return "gain";
        return "";
    }

    private static String resourceName(String n){
        if(contains(n,"espaco de magia","espacos de magia","spell slot","spell slots"))return "spell_slots";
        if(contains(n,"dado de vida","dados de vida","hit die","hit dice"))return "hit_dice";
        if(contains(n,"carga","cargas","charge","charges"))return "charges";
        if(contains(n,"exaustao","exhaustion"))return "exhaustion";
        if(contains(n,"ponto de foco","pontos de foco","focus point","focus points"," ki "))return "focus";
        if(contains(n,"ponto de feiticaria","pontos de feiticaria","sorcery point","sorcery points"))return "sorcery_points";
        if(contains(n,"furia","rage"))return "rage";
        return "";
    }

    private static void appendQuantity(JSONObject out,String sentence)throws Exception{/*BR_JSON_WRAP*/try{
        Matcher m=EXPLICIT_QUANTITY.matcher(sentence==null?"":sentence);if(!m.find())return;String raw=norm(m.group(2));
        out.put("quantity_text",m.group(2));out.put("quantity_exact",m.group(1)!=null&&m.group(1).trim().length()>0);
        if("metade".equals(raw)||"half".equals(raw))out.put("quantity_mode","half");
        else if("todo".equals(raw)||"todos".equals(raw)||"toda".equals(raw)||"todas".equals(raw)||"all".equals(raw))out.put("quantity_mode","all");
        else out.put("quantity_mode","fixed").put("quantity",wordNumber(raw));
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static int wordNumber(String raw){if("um".equals(raw)||"uma".equals(raw)||"one".equals(raw))return 1;if("dois".equals(raw)||"duas".equals(raw)||"two".equals(raw))return 2;if("tres".equals(raw)||"three".equals(raw))return 3;try{return Integer.parseInt(raw);}catch(Exception ignored){return 0;}}

    private static String itemOperation(String n){
        if(contains(n,"exige como componente","requer como componente","required component","material component"))return "component_mention";
        if(contains(n,"apenas menciona","mentions"))return "mention";
        if(contains(n,"desequipa","unequips"))return "unequip";
        if(contains(n,"equipa","equips"))return "equip";
        if(contains(n,"compra","purchases","buys"))return "buy";
        if(contains(n,"vende","sells"))return "sell";
        if(contains(n,"fabrica","cria o item","crafts"))return "craft";
        if(contains(n,"encontra","finds"))return "find";
        if(contains(n,"destroi","destrói","destroys"))return "destroy";
        if(contains(n,"consome","consuma","consumes"))return "consume";
        if(contains(n,"perde","loses"))return "lose";
        if(contains(n,"concede","recebe","ganha","grants","receives","gains"))return "grant";
        return "";
    }

    private static JSONArray effectsByKind(JSONArray effects,String kind)throws Exception{/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(effects!=null)for(int i=0;i<effects.length();i++){JSONObject e=effects.optJSONObject(i);if(e!=null&&kind.equals(e.optString("kind")))out.put(e);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONArray resourceEffects(JSONArray effects)throws Exception{/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(effects!=null)for(int i=0;i<effects.length();i++){JSONObject e=effects.optJSONObject(i);if(e!=null&&e.optString("resource_operation").length()>0)out.put(e);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String first(JSONArray values,String fallback){return values!=null&&values.length()>0?values.optString(0,fallback):fallback;}
    private static String inferActionType(JSONArray checks){if(checks!=null&&checks.length()>0){JSONObject check=checks.optJSONObject(0);if(check!=null&&"attack".equals(check.optString("kind")))return "action";}return "unspecified";}
    private static int firstDc(JSONArray checks){if(checks!=null)for(int i=0;i<checks.length();i++){JSONObject check=checks.optJSONObject(i);if(check!=null&&check.has("dc"))return check.optInt("dc");}return 0;}
    private static String inferSystem(JSONObject source){String n=norm(source==null?"":source.optString("rule_profile")+" "+source.optString("book"));return contains(n,"mystara","becmi","rules cyclopedia")?"mystara":"dnd5e";}

    private static JSONArray findConditions(String text)throws Exception{JSONArray out=new JSONArray();String n=norm(text);for(String condition:CONDITIONS)if(containsPhrase(n,norm(condition)))putUnique(out,condition);return out;}
    private static JSONArray findDamageTypes(String text)throws Exception{JSONArray out=new JSONArray();String n=norm(text);for(String type:DAMAGE_TYPES)if(containsToken(n,norm(type)))putUnique(out,type);return out;}
    private static JSONArray extractDice(String text)throws Exception{/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();Set<String> seen=new LinkedHashSet<String>();Matcher m=DICE.matcher(text==null?"":text);while(m.find()&&out.length()<24){String formula=m.group().replaceAll("\\s+","");if(seen.add(formula.toLowerCase(Locale.ROOT)))out.put(formula);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONArray extractNumbers(String text)throws Exception{/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();Set<String> seen=new LinkedHashSet<String>();Matcher m=NUMBER_UNIT.matcher(text==null?"":text);while(m.find()&&out.length()<32){String value=clean(m.group());if(seen.add(norm(value)))out.put(value);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean isCheckSentence(String n){return contains(n,"teste de","ability check","skill check","jogada de protecao","saving throw","salvaguarda","jogada de ataque","attack roll","roll a d20","role um d20");}
    private static boolean isTrigger(String n){return startsOrContainsClause(n,"quando ","sempre que ","se ","ao ","when ","whenever ","if ","after ","before ","depois de ","antes de ");}
    private static boolean isException(String n){return contains(n,"exceto","a menos que","nao pode","unless","except","cannot","does not","nao se aplica");}
    private static boolean startsOrContainsClause(String n,String... values){for(String value:values){String v=norm(value);if(n.startsWith(v)||n.contains(" "+v+" "))return true;}return false;}
    private static boolean hasFixedEffect(JSONArray effects){for(int i=0;i<effects.length();i++){JSONObject e=effects.optJSONObject(i);if(e!=null&&e.has("fixed_value"))return true;}return false;}
    private static boolean hasGroundedQuantity(JSONArray effects){for(int i=0;i<effects.length();i++){JSONObject e=effects.optJSONObject(i);if(e!=null&&(e.optInt("quantity",0)>0||"half".equals(e.optString("quantity_mode"))||"all".equals(e.optString("quantity_mode"))))return true;}return false;}
    private static boolean hasConditionMutation(JSONArray effects){for(int i=0;i<effects.length();i++){JSONObject e=effects.optJSONObject(i);if(e==null)continue;String operation=e.optString("condition_operation");if("apply".equals(operation)||"remove".equals(operation)||"extend_duration".equals(operation)||"reduce_duration".equals(operation)||"suspend".equals(operation))return true;}return false;}
    private static boolean containsAnyProcedureFlag(JSONArray procedures,String flag){for(int i=0;i<procedures.length();i++){JSONObject p=procedures.optJSONObject(i);if(p!=null&&p.optBoolean(flag,false))return true;}return false;}
    private static void appendMatches(JSONArray out,Pattern pattern,String text)throws Exception{Matcher m=pattern.matcher(text==null?"":text);while(m.find()&&out.length()<24)putUnique(out,clean(m.group()));}
    private static void appendUnique(JSONArray target,JSONArray source)throws Exception{if(source==null)return;for(int i=0;i<source.length();i++){Object value=source.opt(i);if(value instanceof JSONObject)putUniqueObject(target,(JSONObject)value);else putUnique(target,String.valueOf(value));}}
    private static void putUniqueObject(JSONArray out,JSONObject value)throws Exception{/*BR_JSON_WRAP*/try{String fingerprint=norm(value.toString());for(int i=0;i<out.length();i++)if(norm(String.valueOf(out.opt(i))).equals(fingerprint))return;out.put(value);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static void putUnique(JSONArray out,String value)throws Exception{/*BR_JSON_WRAP*/try{String n=norm(value);if(n.length()==0)return;for(int i=0;i<out.length();i++)if(norm(out.optString(i)).equals(n))return;out.put(value);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONArray copyArray(JSONArray source){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(source!=null)for(int i=0;i<source.length();i++)out.put(source.opt(i));return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String join(JSONArray values,String separator){StringBuilder b=new StringBuilder();if(values!=null)for(int i=0;i<values.length();i++){String v=values.optString(i);if(v.length()==0)continue;if(b.length()>0)b.append(separator);b.append(v);}return b.toString();}
    private static boolean contains(String n,String... values){for(String value:values)if(containsPhrase(n,norm(value)))return true;return false;}
    private static boolean containsPhrase(String normalized,String phrase){if(phrase.length()==0)return false;return (" "+normalized+" ").contains(" "+phrase+" ");}
    private static boolean containsToken(String normalized,String token){if(token.length()==0)return false;return Pattern.compile("(?:^|\\s)"+Pattern.quote(token)+"(?:$|\\s)").matcher(normalized).find();}
    private static String clean(String value){return value==null?"":value.replace('\u0000',' ').replaceAll("\\s+"," ").trim();}
    private static String norm(String value){String n=Normalizer.normalize(clean(value).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
