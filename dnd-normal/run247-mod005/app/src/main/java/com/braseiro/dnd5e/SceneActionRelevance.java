package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scene-relative prioritization, NOT a mechanical adjudicator.
 * A noun being physically visible never makes it the plot's center.
 * The AI proposes intents; the game engine checks physical evidence and the
 * last-mile selector favors ongoing objectives and different decision targets.
 */
public final class SceneActionRelevance {
    private SceneActionRelevance(){}

    private static final String[][] TARGETS={
        {"caderno","caderno","anotacoes do caderno"},
        {"mapa","mapa","mapas"},
        {"livro","livro","livros","tomo"},
        {"pergaminho","pergaminho","velino"},
        {"bilhete","bilhete","recado"},
        {"roteiro","roteiro"},
        {"diario","diario"},
        {"papel","papel","folha"},
        {"porta","porta","portao"},
        {"escada","escada","escadaria"},
        {"caixote","caixote","caixotes"},
        {"cofre","cofre"},
        {"chave","chave","chaves"},
        {"mesa","mesa","bancada"},
        {"quadro","quadro","cartaz","avisos"},
        {"arbustos","arbusto","arbustos"},
        {"canteiro","canteiro","canteiros"},
        {"obelisco","obelisco","marco de pedra"},
        {"inscricoes","inscricoes","matricula","matriculas"},
        {"ensaio","ensaio","encenacao","teatro","apresentacao"},
        {"guilda","guilda"},
        {"professor","professor","professora"},
        {"estudantes","aluno","aluna","estudante","estudantes","calouros"},
        {"funcionaria","funcionaria","funcionario"},
        {"guarda","guarda","guardas","sentinela"},
        {"mercador","mercadora","mercador"},
        {"pesquisador","pesquisador","pesquisadora"},
        {"capitao","capitao"},
        {"barqueiro","barqueiro"},
        {"estrada","estrada","caminho","rota","trilha"},
        {"ponte","ponte"},
        {"jardim","jardim"},
        {"pista","pista","pistas"},
        {"charada","charada","enigma"},
        {"combate","inimigo","ameaca","combate","ataque"},
        {"convite","convite","proposta"},
        {"fogueira","fogueira"}
    };
    private static final Set<String> BACKGROUND=new HashSet<>();
    static{ Collections.addAll(BACKGROUND,"caderno","livro","papel","mesa","quadro",
        "caixote","chave","roteiro","mapa","diario"); }

    static String focus(JSONObject option){
        if(option==null)return "";
        String action=norm(option.optString("action",""));
        if(action.isEmpty())return "";
        for(String[] group:TARGETS){
            for(int i=1;i<group.length;i++){
                if(has(action,group[i]))return group[0];
            }
        }
        return "";
    }

    static List<JSONObject> order(List<JSONObject> candidates,String narration,
                                   String declaredAction,JSONObject scene){
        ArrayList<Ranked> ranked=new ArrayList<>();
        if(candidates==null)return new ArrayList<>();
        for(int i=0;i<candidates.size();i++){
            JSONObject item=candidates.get(i);
            if(item!=null)ranked.add(new Ranked(item,i,
                score(item,narration,declaredAction,scene)));
        }
        Collections.sort(ranked,new Comparator<Ranked>(){
            @Override public int compare(Ranked a,Ranked b){
                int priority=Integer.compare(b.priority,a.priority);
                return priority!=0?priority:Integer.compare(a.position,b.position);
            }
        });
        ArrayList<JSONObject> out=new ArrayList<>();
        for(Ranked item:ranked)out.add(item.option);
        return out;
    }

    static boolean diverse(JSONObject candidate,JSONArray selected,JSONObject scene,
                           String narration){
        String focus=focus(candidate);
        if(focus.isEmpty()||selected==null)return true;
        int count=0;
        for(int i=0;i<selected.length();i++){
            JSONObject old=selected.optJSONObject(i);
            if(focus.equals(focus(old)))count++;
        }
        if(count==0)return true;
        // A secondary scenery prop cannot monopolize the player's decisions.
        if(BACKGROUND.contains(focus)&&!goalMatch(candidate,scene))return false;
        // Other focal points may legitimately sustain several tactics when
        // they are the only meaningful focus (e.g. negotiating with guards).
        // The selector tries distinct foci before considering these repeats.
        return count<3;
    }

    static boolean focusAlreadyChosen(JSONObject candidate,JSONArray selected){
        String target=focus(candidate);
        if(target.isEmpty()||selected==null)return false;
        for(int i=0;i<selected.length();i++){
            if(target.equals(focus(selected.optJSONObject(i))))return true;
        }
        return false;
    }

    private static int score(JSONObject option,String narration,
                             String declaredAction,JSONObject scene){
        String action=norm(option.optString("action",""));
        String visible=norm(narration);
        String focus=focus(option);
        int score=0;
        if(goalMatch(option,scene))score+=14;
        String last=visible.substring(Math.max(0,visible.length()*2/3));
        if(!focus.isEmpty()&&has(last,focus))score+=3;
        String interlocutor=norm(SceneDialogueDecisionPlanner.namedInterlocutor(narration));
        if(!interlocutor.isEmpty()&&has(action,interlocutor))score+=5;
        if(hasAny(action,"respondo","pergunto","converso","abordo","decido","sigo a pista"))
            score+=2;
        if(hasAny(action,"inscricoes","matricula","ameaca","desafio","convite",
                "competicao","aula","prova","missao"))score+=3;
        if(BACKGROUND.contains(focus)&&!goalMatch(option,scene))score-=5;
        if(!norm(declaredAction).isEmpty()&&sharesMeaningfulWord(action,norm(declaredAction)))
            score+=1;
        return score;
    }

    static boolean goalMatch(JSONObject option,JSONObject scene){
        if(scene==null||option==null)return false;
        JSONArray quests=scene.optJSONArray("active_quests");
        if(quests==null)return false;
        String candidate=norm(option.optString("action",""));
        for(int i=0;i<quests.length();i++){
            JSONObject q=quests.optJSONObject(i);
            if(q==null)continue;
            String objective=norm(q.optString("title","")+" "+
                    q.optString("objective","")+" "+q.optString("stage",""));
            int shared=0;
            for(String token:objective.split(" ")){
                if(token.length()<5||stopword(token)||!has(candidate,token))continue;
                shared++;
            }
            if(shared>=2)return true;
        }
        return false;
    }

    private static boolean sharesMeaningfulWord(String a,String b){
        for(String x:b.split(" ")){if(x.length()>=6&&!stopword(x)&&has(a,x))return true;}
        return false;
    }
    private static boolean stopword(String s){
        return hasAny(s,"sobre","antes","depois","comigo","quando","como",
            "objetivo","conhecer","durante","tentar","tentativa","ainda",
            "nesta","neste","dessa","desse","pessoas","personagem",
            "missao","grupo","atual","proximo","historia","situacao");
    }
    private static boolean hasAny(String a,String... terms){
        for(String t:terms)if(has(a,t))return true;
        return false;
    }
    private static boolean has(String s,String term){
        return (" "+s+" ").contains(" "+norm(term)+" ");
    }
    private static String norm(String raw){
        return Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static final class Ranked{
        final JSONObject option;final int position,priority;
        Ranked(JSONObject o,int i,int s){option=o;position=i;priority=s;}
    }
}
