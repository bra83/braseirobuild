package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deterministic, source-linked timeline of already committed scene records.
 * This class never asks an LLM to decide what has already happened.
 *
 * Not an omniscient fact extractor: only explicit, unambiguous clue milestones
 * are counted. Ambiguity remains ambiguity; no retcon is ever made up.
 */
public final class CampaignContinuityGate {
    private CampaignContinuityGate(){}

    private static final Pattern ORDINAL=Pattern.compile(
        "(?iu)\\b(primeir[ao]|segund[ao]|terceir[ao]|quart[ao]|quint[ao]|sext[ao]|setim[ao]|oitav[ao]|non[ao]|decim[ao])\\s+pista\\b");
    private static final Pattern FIRST=Pattern.compile(
        "(?iu)\\b(?:primeira|primeiro)\\s+pista\\b");
    private static final Pattern CLUE_SUCCESS=Pattern.compile(
        "(?iu)(?:\\bpista\\b.{0,100}\\b(?:decifrad[ao]|desvendad[ao]|encontrad[ao]|revelad[ao]|resolvid[ao])\\b"
        +"|\\b(?:decifrad[ao]|desvendad[ao]|encontrad[ao]|revelad[ao]|resolvid[ao])\\b.{0,90}\\bpista\\b)");
    private static final Pattern HUNT=Pattern.compile("(?iu)(ca[cç]a\\s+(?:de\\s+)?boas[- ]vindas|ca[cç]a\\s+ao\\s+tesouro|orienta[cç][aã]o)");
    private static final int MAX_SCENES_SCANNED=120;

    public static boolean historicalQuestion(String question){
        String q=fold(question);
        if(q.isEmpty())return false;
        if(hasAny(q,"contradiz","contradicao","incoerenc","inconsisten","continuidade",
            "nao bate","nao faz sentido","esqueceu o que","ja tinha falado","ja havia falado",
            "ela mesma disse","ele mesmo disse","na cena anterior","nas cenas anteriores",
            "na penultima cena","voce disse antes","mestre disse antes","pistas anteriores"))return true;
        boolean clue=hasAny(q,"pista","enigmas","cac a","caca","tesouro","pergaminho");
        boolean temporal=hasAny(q,"primeira","segunda","terceira","quarta","quatro",
            "ja estou","ja peguei","ja encontrei","ja decifrei","antes","anterior",
            "ja tinha","ja fiz","outra cena","cade a","contagem","numero");
        return clue&&temporal;
    }

    /** Source references use actual journal row ids and are not AI inventions. */
    public static JSONObject canonicalContext(List<JSONObject> journal){
        JSONObject out=new JSONObject();
        JSONArray chronology=new JSONArray();
        JSONArray milestones=new JSONArray();
        List<JSONObject> scenes=scenesNewestFirst(journal);
        int highest=0,confirmations=0;
        int added=0;
        // Most recent 10 scenes plus significant clue moments from up to 120 scenes.
        for(int i=0;i<scenes.size();i++){
            JSONObject row=scenes.get(i);
            String body=row.optString("body","");
            String prose=narration(body);
            String folded=fold(prose),act=action(body);
            int ordinal=largestOrdinal(prose);
            boolean confirmed=CLUE_SUCCESS.matcher(folded).find();
            boolean aboutHunt=HUNT.matcher(folded).find()||HUNT.matcher(fold(act)).find();
            if(ordinal>highest)highest=ordinal;
            if(confirmed&&aboutHunt)confirmations++;
            JSONObject e=new JSONObject();
            try{
                e.put("journal_id",row.optLong("id",0));
                e.put("scene_title",clip(row.optString("title",""),100));
                e.put("declared_action",clip(act,240));
                e.put("narration_excerpt",clip(prose,650));
                e.put("explicit_clue_ordinal",ordinal);
                e.put("confirmed_clue_resolution",confirmed&&aboutHunt);
                if(i<10 || (confirmed&&aboutHunt) || ordinal>=2){
                    if(added<32){chronology.put(e);added++;}
                }
                if((confirmed&&aboutHunt)||ordinal>=2){
                    JSONObject milestone=new JSONObject()
                        .put("journal_id",row.optLong("id",0))
                        .put("explicit_clue_ordinal",ordinal)
                        .put("explicitly_resolved_clue",confirmed&&aboutHunt);
                    milestones.put(milestone);
                }
            }catch(Exception ignored){}
        }
        try{
            out.put("scenes_scanned",scenes.size());
            out.put("highest_explicit_clue_ordinal",highest);
            out.put("distinct_explicit_hunt_clue_resolutions",confirmations);
            out.put("source_linked_chronology_newest_first",chronology);
            out.put("clue_milestone_sources",milestones);
            out.put("trust_policy","Journal records are source evidence. NPC guesses and AI consultations "
                +"are not canon. Do not assert a clue ordinal, prior dialogue, an NPC's knowledge, "
                +"or a retcon without authoritative evidence. If facts conflict, stop and report "
                +"the inconsistency rather than explaining it away.");
        }catch(Exception ignored){}
        return out;
    }

    /**
     * Fail closed only on high-confidence numeric inconsistency. Other cases
     * require source-backed review; a detector should not invent facts either.
     */
    public static String contradiction(List<JSONObject> journal,String proposedNarration){
        if(proposedNarration==null||!FIRST.matcher(proposedNarration).find())return "";
        String p=fold(proposedNarration);
        if(!HUNT.matcher(p).find())return "";
        JSONObject memory=canonicalContext(journal);
        int highest=memory.optInt("highest_explicit_clue_ordinal",0);
        int resolved=memory.optInt("distinct_explicit_hunt_clue_resolutions",0);
        if(highest>=3 || resolved>=3){
            return "A nova fala apresenta a primeira pista da caça, mas o diário "
                +"registra "+(highest>=3?"uma pista de ordem "+highest:"pelo menos "+resolved+" descobertas distintas")
                +" anteriormente. Resposta bloqueada antes de confirmar a cena; "
                +"nenhuma explicação alternativa foi inventada.";
        }
        return "";
    }

    public static String auditAnswer(List<JSONObject> journal,String question,String currentNarration){
        JSONObject ctx=canonicalContext(journal);
        JSONArray entries=ctx.optJSONArray("source_linked_chronology_newest_first");
        int explicit=ctx.optInt("highest_explicit_clue_ordinal",0);
        int solved=ctx.optInt("distinct_explicit_hunt_clue_resolutions",0);
        StringBuilder out=new StringBuilder();
        out.append("AUDITORIA DE CONTINUIDADE — FORA DA FICÇÃO. Nenhum NPC falou e a história não avançou.\n\n");
        if(FIRST.matcher(currentNarration==null?"":currentNarration).find()
                &&HUNT.matcher(fold(currentNarration)).find())
            out.append("A CENA ATUAL usa a expressão 'primeira pista'. ");
        if(explicit>=3 || solved>=3){
            out.append("O diário possui evidência anterior de progresso incompatível com tratar a caça como começando agora. ");
            out.append("Não é válido justificar isso inventando rotas paralelas ou informações que os NPCs nunca deram.\n\n");
        }else{
            out.append("Não encontrei no histórico estruturado evidência suficiente para confirmar a NUMERAÇÃO exata de todas as pistas. ");
            out.append("Isso NÃO autoriza afirmar que seja a primeira ou inventar uma explicação. ");
            out.append("Sua contestação fica registrada como inconsistência a verificar, não como fato resolvido.\n\n");
        }
        out.append("TRECHOS DO DIÁRIO (registros mais recentes, sem recriar diálogos):\n");
        int evidence=0;
        if(entries!=null)for(int i=0;i<entries.length()&&evidence<7;i++){
            JSONObject e=entries.optJSONObject(i);if(e==null)continue;
            String n=e.optString("narration_excerpt","");
            boolean pertinent=HUNT.matcher(fold(n)).find()
                ||fold(n).contains("pista")||fold(e.optString("declared_action","")).contains("pista");
            if(!pertinent)continue;
            out.append("• Registro ").append(e.optLong("journal_id",0)).append(": ")
                .append(clip(n.replaceAll("\\s+"," "),210)).append("\n");
            evidence++;
        }
        if(evidence==0)out.append("• Nenhum trecho pertinente foi recuperado. Não é possível conferir a sequência por este resumo.\n");
        out.append("\nA CONSULTA não altera história, relação com NPCs, fichas ou relógio. ");
        out.append("A fala conflitante NÃO foi consertada retroativamente.");
        return out.toString();
    }

    public static boolean isSafeCanonicalSummary(String answer){
        return answer!=null&&answer.startsWith("AUDITORIA DE CONTINUIDADE — FORA DA FICÇÃO.");
    }

    private static List<JSONObject> scenesNewestFirst(List<JSONObject> journal){
        List<JSONObject> out=new ArrayList<JSONObject>();
        if(journal==null)return out;
        for(JSONObject row:journal){
            if(out.size()>=MAX_SCENES_SCANNED)break;
            if(row!=null&&"Cena".equalsIgnoreCase(row.optString("kind",""))
                &&row.optString("body","").trim().length()>0)out.add(row);
        }
        // Imported/legacy journals can have identical timestamps. IDs are
        // monotonically assigned by the local journal table.
        java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){
            @Override public int compare(JSONObject a,JSONObject b){
                return Long.compare(b.optLong("id",0),a.optLong("id",0));
            }
        });
        return out;
    }

    private static String narration(String body){
        if(body==null)return "";
        int i=body.indexOf("NARRAÇÃO DO MESTRE:");
        return i>=0?body.substring(i+"NARRAÇÃO DO MESTRE:".length()).trim():body;
    }

    private static String action(String body){
        if(body==null)return "";
        int i=body.indexOf("AÇÃO DECLARADA:");
        if(i<0)return "";
        int end=body.indexOf("\n\n",i);
        if(end<0)end=body.length();
        return body.substring(i+"AÇÃO DECLARADA:".length(),end).trim();
    }

    private static int largestOrdinal(String narration){
        Matcher m=ORDINAL.matcher(fold(narration));
        int max=0;
        while(m.find()){
            String v=m.group(1);
            int n=v.startsWith("primeir")?1:v.startsWith("segund")?2:v.startsWith("terceir")?3:
                v.startsWith("quart")?4:v.startsWith("quint")?5:v.startsWith("sext")?6:
                v.startsWith("setim")?7:v.startsWith("oitav")?8:v.startsWith("non")?9:10;
            max=Math.max(max,n);
        }
        return max;
    }

    private static String fold(String s){
        return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").replaceAll("\\s+"," ").trim();
    }
    private static boolean hasAny(String value,String... needles){
        for(String s:needles)if(value.contains(s))return true;
        return false;
    }
    private static String clip(String s,int limit){
        String clean=s==null?"":s.replaceAll("\\s+"," ").trim();
        return clean.length()>limit?clean.substring(0,limit-1)+"…":clean;
    }
}
