package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Registro mecânico pré-compilado de suplementos.
 *
 * RAG continua disponível para lore e conteúdo ainda não compilado, porém regras
 * presentes aqui são resolvidas pelo aplicativo e não dependem de busca no PDF.
 */
public final class SupplementContentRegistry {
    public static final String SOURCE_TYPE="compiled_supplement";
    private static final Set<String> CLASSES=new HashSet<String>(Arrays.asList("cozinheiro"));
    private static final Set<String> RULE_IDS=new HashSet<String>(Arrays.asList(
            "cozinheiro.pouch.restock","cozinheiro.snack.prepare","cozinheiro.snack.save_dc","cozinheiro.snack.feed",
            "cozinheiro.spice.retry","cozinheiro.accepting_orders","cozinheiro.comfort_meal","cozinheiro.leftovers",
            "animal.training.trick","animal.acclimation","animal.loyalty","animal.care.sustenance","animal.uptime",
            "tasha.patron.assist","tasha.patron.assignment","tasha.sidekick.create","tasha.sidekick.level",
            "tasha.monster.research","tasha.monster.negotiate","tasha.hazard.avalanche","tasha.subclass.change"
    ));
    private SupplementContentRegistry(){}

    public static boolean isKnownClass(String candidate){String n=normalize(candidate);for(String value:CLASSES)if(n.equals(value)||n.startsWith(value+" "))return true;return false;}

    public static JSONObject catalogSummary(){
        JSONObject out=new JSONObject();try{
            out.put("mode","compiled_first_rag_fallback");out.put("compiled_packs",new JSONArray().put(CookingRulesEngine.descriptor()).put(AnimalCompanionRulesEngine.descriptor()).put(DowntimeEconomyRulesEngine.descriptor()).put(CharacterOptionCatalogEngine.descriptor()).put(SpellItemCatalogEngine.descriptor()).put(TashaFeatCompiledRulesEngine.descriptor()).put(TashaCampaignSystemsEngine.descriptor()));
            CompiledSupplementProcedureBootstrap.install();int registered=0,executable=0,manual=0;for(String id:RULE_IDS){JSONObject g=AuthorizedRuleProcedureEngine.gate(id,new JSONObject());if(g.optBoolean("registered",false))registered++;String pt=AuthorizedRuleProcedureEngine.get(id).optString("procedure_type","");if("state_rule_gate".equalsIgnoreCase(pt))manual++;else if(g.optBoolean("registered",false))executable++;}out.put("rule_count",RULE_IDS.size()).put("registered_procedures",registered).put("executable_procedures",executable).put("manual_resolution_procedures",manual).put("rag_role","lore, explicação e suplementos ainda não compilados");
        }catch(Exception ignored){AppLog.ignored("SupplementContentRegistry","Falha ao montar catálogo.",ignored);}return out;
    }

    public static JSONObject matchAction(String action){
        JSONObject out=new JSONObject();String n=normalize(action);try{
            JSONArray packs=new JSONArray();JSONArray rules=new JSONArray();
            if(matchesCooking(n)){packs.put(CookingRulesEngine.descriptor());addRules(rules,"cozinheiro.");}
            if(matchesAnimal(n)){packs.put(AnimalCompanionRulesEngine.descriptor());addRules(rules,"animal.");}
            if(matchesDowntime(n)){packs.put(DowntimeEconomyRulesEngine.descriptor());}
            if(hasAny(n,"magia","feitico","spell","item magico","tatuagem","grimorio","shard")){packs.put(SpellItemCatalogEngine.descriptor());}
            if(matchesTashaCampaign(n)){packs.put(TashaCampaignSystemsEngine.descriptor());addRules(rules,"tasha.");}
            out.put("matched",packs.length()>0).put("packs",packs).put("rule_ids",rules)
                    .put("authority",packs.length()>0?"Use estas regras pré-compiladas antes de RAG. Para mutação mecânica use source_type=compiled_supplement e rule_id listado.":"Nenhum pacote compilado específico corresponde à ação.");
        }catch(Exception ignored){AppLog.ignored("SupplementContentRegistry","Falha ao selecionar capacidades.",ignored);}return out;
    }

    public static JSONObject rule(String ruleId){
        String id=ruleId==null?"":ruleId.trim();JSONObject out=new JSONObject();try{
            if(!RULE_IDS.contains(id))return out;out.put("rule_id",id).put("source_type",SOURCE_TYPE);
            if(id.startsWith("cozinheiro.")){out.put("book",CookingRulesEngine.BOOK);fillCookingRule(out,id);}else if(id.startsWith("animal.")){out.put("book",AnimalCompanionRulesEngine.BOOK);fillAnimalRule(out,id);}else{out.put("book",TashaCampaignSystemsEngine.BOOK);fillTashaRule(out,id);}
        }catch(Exception ignored){AppLog.ignored("SupplementContentRegistry","Falha ao carregar regra compilada.",ignored);}return out;
    }

    public static boolean verifyMechanicalBasis(JSONObject basis){
        if(basis==null)return false;String type=normalize(basis.optString("source_type"));if(!type.equals("compiled supplement")&&!type.equals("compiled_supplement"))return false;
        JSONObject canonical=rule(basis.optString("rule_id"));if(canonical.length()==0)return false;
        String supplied=normalize(basis.optString("evidence")),expected=normalize(canonical.optString("evidence"));
        if(supplied.length()<4||!supplied.equals(expected))return false;
        String book=normalize(basis.optString("book"));if(book.length()>0&&!book.equals(normalize(canonical.optString("book"))))return false;
        return true;
    }

    public static JSONObject mechanicalBasis(String ruleId){
        JSONObject canonical=rule(ruleId);JSONObject out=new JSONObject();try{if(canonical.length()==0)return out;out.put("source_type",SOURCE_TYPE).put("rule_id",ruleId).put("book",canonical.optString("book")).put("page",canonical.optInt("page")).put("evidence",canonical.optString("evidence")).put("excerpt",canonical.optString("evidence"));}catch(Exception ignored){AppLog.ignored("SupplementContentRegistry","Falha ao montar base mecânica.",ignored);}return out;
    }

    private static void fillCookingRule(JSONObject o,String id)throws Exception{
        if(id.equals("cozinheiro.pouch.restock"))o.put("page",3).put("evidence","Bolsa de Cozinheiro: reabastecer a cada 30 dias; 10 po no mercado ou 8 horas forrageando; substituir a bolsa custa 100 po e 8 horas.");
        else if(id.equals("cozinheiro.snack.prepare"))o.put("page",3).put("evidence","Petiscos preparados por descanso curto ou longo são iguais ao modificador de Constituição; só um efeito de petisco permanece ativo por criatura.");
        else if(id.equals("cozinheiro.snack.save_dc"))o.put("page",3).put("evidence","CD de Petisco = 8 + bônus de proficiência + modificador de Constituição.");
        else if(id.equals("cozinheiro.snack.feed"))o.put("page",3).put("evidence","Alimentar com um petisco usa uma ação, exige criatura ao alcance de toque e uma mão livre.");
        else if(id.equals("cozinheiro.spice.retry"))o.put("page",3).put("evidence","Temperos Aromáticos usam ação bônus e permitem novo teste contra enfeitiçado ou amedrontado; no 7º nível também atordoado e no 13º paralisado.");
        else if(id.equals("cozinheiro.accepting_orders"))o.put("page",4).put("evidence","Aceitando Pedidos concede petiscos extras iguais ao modificador de Constituição no descanso longo a partir do 7º nível e em descanso curto ou longo a partir do 11º; extras não podem exigir pré-requisito.");
        else if(id.equals("cozinheiro.comfort_meal"))o.put("page",4).put("evidence","No 17º nível, um petisco dado a criatura que compartilhou o descanso longo com o Cozinheiro pode ter sua duração dobrada.");
        else if(id.equals("cozinheiro.leftovers"))o.put("page",4).put("evidence","No 20º nível, ao rolar iniciativa sem petiscos restantes, surge na Bolsa um petisco conhecido que o Cozinheiro sabe preparar.");
    }
    private static void fillAnimalRule(JSONObject o,String id)throws Exception{
        if(id.equals("animal.training.trick"))o.put("page",19).put("evidence","Treinar um truque exige proficiência em Adestrar Animais e 5 dias de downtime; o animal memoriza no máximo 2 truques por ponto de Inteligência e o treinador pode ensinar o mesmo truque simultaneamente a até seu modificador de Sabedoria, mínimo 1, animais.");
        else if(id.equals("animal.acclimation"))o.put("page",89).put("evidence","Aclimatação exige Adestrar Animais; a maioria dos animais selvagens requer 20 dias de downtime, dobrados em cativeiro; animais domésticos, já domados, filhotes e companheiros de classe ou magia não precisam desse processo.");
        else if(id.equals("animal.loyalty"))o.put("page",34).put("evidence","Lealdade animal opcional varia de 0 a 20 e tem máximo igual à Sabedoria do tratador; domésticos começam em metade do máximo após adaptação, selvagens em 0 até aclimatação; eventos de ganho ou perda alteram 1d4.");
        else if(id.equals("animal.care.sustenance"))o.put("page",91).put("evidence","Necessidade diária de alimento por tamanho: Tiny 1/4 lb, Small ou Medium 1 lb, Large 4 lb, Huge 16 lb, Gargantuan 64 lb; água em galões igual às libras de alimento; falta de sustento causa exaustão pelas regras centrais.");
        else if(id.equals("animal.uptime"))o.put("page",92).put("evidence","Regra opcional de uptime credita meio dia de downtime em um dia de aventura quando ocorre descanso longo, desde que custos, ferramentas, materiais e recursos necessários estejam disponíveis.");
    }
    private static void fillTashaRule(JSONObject o,String id)throws Exception{
        if(id.equals("tasha.patron.assist"))o.put("page",85).put("evidence","Each group member can grant advantage to another member on an ability check, attack roll, or saving throw when they can see or hear each other and neither is incapacitated; that member regains this grant after a long rest.");
        else if(id.equals("tasha.patron.assignment"))o.put("page",85).put("evidence","A group patron can offer assignments that drive adventures; accepting or refusing them can create persistent story consequences.");
        else if(id.equals("tasha.sidekick.create"))o.put("page",140).put("evidence","A sidekick starts from a creature statblock with challenge rating 1/2 or lower and must have sufficient friendship/trust with at least one adventurer; Expert and Spellcaster require a spoken language.");
        else if(id.equals("tasha.sidekick.level"))o.put("page",140).put("evidence","When a sidekick gains a level it gains one Hit Die and increases maximum hit points by the Hit Die roll plus Constitution modifier, minimum 1; proficiency follows sidekick class level.");
        else if(id.equals("tasha.monster.research"))o.put("page",146).put("evidence","Monster research uses creature-type-appropriate skills; the relevant check DC equals 10 + the creature challenge rating and success reveals what the creature is likely to want.");
        else if(id.equals("tasha.monster.negotiate"))o.put("page",146).put("evidence","Offering a monster something it desires can grant advantage on ability checks used to communicate; the actual exchange still follows social interaction adjudication.");
        else if(id.equals("tasha.hazard.avalanche"))o.put("page",161).put("evidence","Avalanche moves at initiative 10 and 0, forces DC 15 Strength saves for 1d10 bludgeoning damage (half on success), moves and knocks victims prone, then buries them with blindness, restraint and total cover; Athletics DC 15 can escape, with exhaustion accruing during burial.");
        else if(id.equals("tasha.subclass.change"))o.put("page",7).put("evidence","With DM approval, subclass change can require training equal to twice the new class level in days and normally 100 gp times that level; returning to a previous subclass removes the gold cost and halves training, while dramatic transformation can allow immediate change.");
    }
    private static void addRules(JSONArray out,String prefix){for(String id:RULE_IDS)if(id.startsWith(prefix))out.put(id);}
    private static boolean matchesCooking(String n){return hasAny(n,"cozinh","cozinheiro","receita","petisco","refeicao","temper","ingrediente","comida","culinaria");}
    private static boolean matchesAnimal(String n){return hasAny(n,"cachorro","cao","cadela","animal","pet","mascote","adestrar","treinar meu","montaria","cavalo","falcao","lobo","companheiro animal","domar","aclimat");}
    private static boolean matchesTashaCampaign(String n){return hasAny(n,"patrono","patron","ajudante","sidekick","negociar monstro","pesquisa de monstro","pesquisar monstro","avalanche","mudanca de subclasse","trocar subclasse");}
    private static boolean matchesDowntime(String n){return hasAny(n,"trabalho","emprego","oficio","artesan","fabric","craft","forjar","comerc","negoci","pesquis","estud","administr","downtime","tempo livre");}
    private static boolean hasAny(String value,String... terms){for(String term:terms)if(value.contains(term))return true;return false;}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
