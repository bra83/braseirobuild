package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Braseiro 6.1.0 — direção de mesa entre cenas.
 *
 * O BraseiroNarrativeDirector dirige a qualidade de cada batida. Esta classe dirige
 * o fluxo maior da mesa: apresentação de vários protagonistas, alternância de foco,
 * subgrupos simultâneos, convergência, sandbox de facções e arcos fechados.
 *
 * Não contém roteiro, aventura pronta ou imitação de um narrador real. Os padrões
 * abaixo foram abstraídos de mesas de referência como técnicas operacionais.
 */
public final class LiveTableNarrativeDirector {
    private LiveTableNarrativeDirector(){}

    public enum CampaignMode { OPEN_WORLD, FOCUSED_ADVENTURE, HYBRID }
    public enum TablePattern {
        ENSEMBLE_PROLOGUE,
        SPOTLIGHT_RELAY,
        PARALLEL_CROSSCUT,
        FACTION_SANDBOX,
        ADVENTURE_CAPSULE,
        TACTILE_INVESTIGATION,
        CINEMATIC_REVEAL,
        SOCIAL_BREATHING
    }

    public static CampaignMode inferMode(JSONObject campaign,JSONObject scene,JSONObject party,JSONObject world,String action){
        String profile=plain(opt(campaign,"tone")+" "+opt(campaign,"notes")+" "+opt(scene,"mode")+" "+opt(scene,"scene_mode")+" "+opt(scene,"objective")+" "+human(action));
        if(containsAny(profile,"mundo aberto","sandbox","hexcrawl","exploracao livre","exploração livre","campanha aberta","mundo vivo"))return CampaignMode.OPEN_WORLD;
        if(containsAny(profile,"one shot","one-shot","aventura focada","aventura fechada","misterio fechado","mistério fechado","dungeon curta","arco fechado"))return CampaignMode.FOCUSED_ADVENTURE;
        int factions=len(world,"factions"),quests=len(world,"quests"),clocks=len(world,"clocks"),events=len(world,"events_due_or_upcoming");
        if(factions>=2||clocks>=2||quests>=3||events>=3)return CampaignMode.OPEN_WORLD;
        if(quests==1&&factions<=1&&(opt(scene,"objective").length()>0||containsAny(profile,"mansao","mansão","templo","ruina","ruína","investigacao","investigação","resgate","assalto","infiltracao","infiltração")))return CampaignMode.FOCUSED_ADVENTURE;
        return CampaignMode.HYBRID;
    }

    public static TablePattern choosePattern(JSONObject campaign,JSONObject scene,JSONObject party,JSONObject world,JSONObject memory,String action){
        JSONObject live=memory==null?null:memory.optJSONObject("live_table");
        boolean noCheckpoint=memory==null||memory.optJSONObject("checkpoint")==null||memory.optJSONObject("checkpoint").length()==0;
        int heroes=party==null?0:party.optJSONArray("heroes")==null?0:party.optJSONArray("heroes").length();
        int subgroups=subgroupCount(party),queued=party==null?0:party.optJSONArray("queued_actions")==null?0:party.optJSONArray("queued_actions").length();
        String n=plain(action+" "+opt(scene,"mode")+" "+opt(scene,"scene_mode")+" "+opt(scene,"objective"));
        if((noCheckpoint||containsAny(n,"inicio da campanha","início da campanha","nova campanha","apresentar personagens","primeiro dia"))&&heroes>1)return TablePattern.ENSEMBLE_PROLOGUE;
        if(subgroups>1||queued>1||containsAny(n,"enquanto isso","outro grupo","subgrupo","ao mesmo tempo"))return TablePattern.PARALLEL_CROSSCUT;
        if(containsAny(n,"investigo","investigacao","investigação","examino","pista","rastro","mausoleu","mausoléu","mansao","mansão","assombr"))return TablePattern.TACTILE_INVESTIGATION;
        if(containsAny(n,"visao","visão","pesadelo","profecia","revelacao","revelação","apocalipse","calamidade")||temperature(scene,action)>=4)return TablePattern.CINEMATIC_REVEAL;
        CampaignMode mode=inferMode(campaign,scene,party,world,action);
        if(mode==CampaignMode.OPEN_WORLD&&len(world,"factions")>=2)return TablePattern.FACTION_SANDBOX;
        if(mode==CampaignMode.FOCUSED_ADVENTURE)return TablePattern.ADVENTURE_CAPSULE;
        if(containsAny(n,"converso","pergunto","falo","descanso","taverna","jantar","cafe","café")&&temperature(scene,action)<=1)return TablePattern.SOCIAL_BREATHING;
        if(live!=null){String last=live.optString("last_pattern","");if("ENSEMBLE_PROLOGUE".equals(last)&&heroes>1)return TablePattern.SPOTLIGHT_RELAY;}
        return TablePattern.SPOTLIGHT_RELAY;
    }

    /** Contrato de fluxo maior, anexado ao prompt depois do contrato de cena. */
    public static String contract(JSONObject campaign,JSONObject scene,JSONObject party,JSONObject world,JSONObject memory,String action){
        CampaignMode mode=inferMode(campaign,scene,party,world,action);
        TablePattern pattern=choosePattern(campaign,scene,party,world,memory,action);
        StringBuilder p=new StringBuilder();
        p.append("DIREÇÃO DE MESA VIVA 6.1 — coordene a campanha entre batidas, sem copiar voz, bordões ou texto de qualquer mestre de referência. Modo inferido: ").append(mode.name()).append("; padrão recomendado: ").append(pattern.name()).append(".\n");
        p.append("ABERTURA DE ELENCO: quando vários protagonistas ainda não convergiram, apresente um por vez em situação cotidiana já em movimento. Para cada herói use ROTINA/RELAÇÃO → DESEJO IMEDIATO → ATRITO PEQUENO → TRAÇO QUE SÓ ELE NOTARIA → GANCHO DE CONVERGÊNCIA. Use apenas dados da ficha e do estado; não despeje biografia e não descreva todos de uma vez.\n");
        p.append("CONVERGÊNCIA: antes de separar cenas, defina silenciosamente um alvo comum plausível — lugar, horário, evento, pessoa, ameaça ou obrigação. Faça as cenas individuais apontarem para ele por sinais diferentes. A reunião deve acontecer por causalidade, não porque o narrador declara que o grupo já está unido.\n");
        p.append("REVEZAMENTO DE PROTAGONISMO: corte o foco após uma decisão, descoberta, entrada ou mudança de posição, nunca no meio de uma pergunta do jogador. Cada retorno precisa lembrar local, instante e ação pendente em poucas palavras, sem recapitular a cena inteira. Personagem há mais tempo sem batida relevante recebe oportunidade natural, não evento artificial.\n");
        p.append("SUBGRUPOS E CORTE PARALELO: mantenha threads independentes com local, presentes, objetivo, última ação e relógio compartilhado. Resolva somente a thread da declaração atual. Um corte pode mostrar outro subgrupo quando há passagem de tempo, consequência comum ou tensão que amadureceu; nunca use o corte para decidir ação por jogador ausente.\n");
        p.append("MUNDO ABERTO: apresente oportunidades por consequências observáveis — preços, patrulhas, refugiados, rumores, lojas fechadas, aliados cobrando favores, territórios mudando — e não como lista de missões. Limite novos ganchos simultâneos. Facções continuam agindo quando ignoradas, mas o retorno deve vir da agenda e dos recursos persistidos. Nenhum arco tem final obrigatório.\n");
        p.append("AVENTURA FOCADA: ao cruzar o limiar de um arco fechado, preserve uma pergunta dramática central, um espaço delimitado e uma cadeia causal legível: APROXIMAÇÃO → LIMIAR → COMPLICAÇÃO → INVESTIGAÇÃO/OBSTÁCULO → VIRADA → CONSEQUÊNCIA/FECHAMENTO. Evite abrir missões não relacionadas no interior do arco. Recuo, negociação e abandono continuam possíveis.\n");
        p.append("CÁPSULA DENTRO DO SANDBOX: um objeto, visitante, pedido ou lugar pode abrir uma aventura concentrada no meio da campanha. Estabeleça claramente o limiar que muda o tipo de cena; enquanto a cápsula estiver ativa, pague suas promessas antes de devolver o mundo aberto, sem apagar consequências externas.\n");
        p.append("INVESTIGAÇÃO TÁTIL: dê aos objetos função manipulável — virar, comparar, iluminar, ouvir, montar, medir, cheirar, abrir, seguir — e não apenas texto para ler. Use ESCUTA/OBSERVAÇÃO → AÇÃO SOBRE OBJETO → RESPOSTA DO ESPAÇO → NOVA HIPÓTESE. Pista crítica deve ter caminhos redundantes e nunca depender de uma única rolagem.\n");
        p.append("REVELAÇÃO CINEMÁTICA: quando apropriado, comece pelo corpo, som ou movimento antes da explicação; revele contexto gradualmente. Use esse recurso com parcimônia. Visão, pesadelo ou prólogo não autoriza retirar agência nem transformar informação do público em conhecimento do personagem.\n");
        p.append("DRAMATIC IRONY COM FIREWALL: uma cena pode mostrar ao jogador algo que outro personagem ainda não sabe somente se essa cena foi efetivamente narrada. Marque internamente quem sabe. Nunca escreva na narração principal 'vocês não sabem, mas' para revelar bastidores do Mestre.\n");
        p.append("NPC TEMPORÁRIO/CONVIDADO: introduza com NECESSIDADE + CAPACIDADE ÚTIL + LIMITE. Ele pode catalisar a aventura e ter voz própria, mas não resolve o mistério, escolhe a rota ou conquista o clímax no lugar dos heróis.\n");
        p.append("HUMOR E RESPIRAÇÃO: humor nasce do contraste entre personalidade, instituição e situação concreta. Não force piada por parágrafo e não use gracejo para apagar luto, perigo ou consequência. Depois de grande revelação, permita reação, conversa ou silêncio antes da próxima escalada.\n");
        p.append("PROMESSA E PAGAMENTO: registre setups importantes como promessas de cena. Reapresente por eco antes do payoff; não invente retrospectivamente que um detalhe irrelevante sempre foi pista. Ao fechar um arco, responda a pergunta central, mostre custo e deixe apenas fios conscientemente não resolvidos.\n");
        p.append("TRANSIÇÃO: uma boa passagem de cena usa uma ponte curta de tempo, lugar, objeto, som, personagem ou consequência. Não reinicie clima, elenco e geografia. Não escreva 'corta para' repetidamente; faça o corte ser percebido pela nova âncora.\n");
        p.append("REAÇÃO DA MESA: depois de entrada marcante, pista decisiva, mudança de lealdade ou imagem final, pare cedo. O sistema não precisa completar um capítulo em cada mensagem.\n");
        p.append(patternInstruction(pattern));
        p.append(memoryPrompt(memory));
        p.append("NARRATIVE_STATE.LIVE_TABLE opcional: {mode:'OPEN_WORLD|FOCUSED_ADVENTURE|HYBRID',pattern:'...',spotlight:{focus:'',queue:[],reason:''},threads:[{key,owner,location,status,urgency,last_beat}],convergence:{target:'',readiness:0,signals:[]},shared_clock:{key:'',stage:0,max:0,visible:false},scene_promises:[{key,setup,status,payoff}],capsule:{premise,threshold,question,status,closure_condition},faction_choices:[{key,offer,cost,ignored_consequence}],dramatic_irony:[{key,fact,knowers:[]}],guest_role:{name,need,capability,limit,status}}. Preencha apenas o que a cena realmente estabeleceu.\n");
        return p.toString();
    }

    public static JSONObject promptState(JSONObject campaign,JSONObject scene,JSONObject party,JSONObject world,JSONObject memory,String action){
        JSONObject out=new JSONObject();
        try{
            CampaignMode mode=inferMode(campaign,scene,party,world,action);TablePattern pattern=choosePattern(campaign,scene,party,world,memory,action);
            out.put("mode",mode.name()).put("pattern",pattern.name()).put("heroes",party==null||party.optJSONArray("heroes")==null?0:party.optJSONArray("heroes").length()).put("subgroups",subgroupCount(party)).put("queued_actions",party==null||party.optJSONArray("queued_actions")==null?0:party.optJSONArray("queued_actions").length()).put("active_factions",len(world,"factions")).put("active_quests",len(world,"quests")).put("active_clocks",len(world,"clocks"));
            JSONObject live=memory==null?null:memory.optJSONObject("live_table");if(live!=null)out.put("memory",compactLive(live));
            out.put("contract","Fluxo orienta foco e transições; não cria roteiro, regra ou conhecimento novo.");
        }catch(Exception ignored){AppLog.ignored("LiveTableNarrativeDirector","Falha tolerada ao montar contexto de mesa viva.",ignored);}
        return out;
    }

    /** Mescla a memória proposta pelo modelo com defaults locais e limites seguros. */
    public static JSONObject record(JSONObject current,String action,String narration,JSONObject envelope,JSONObject scene){
        JSONObject root=copy(current),live=root.optJSONObject("live_table");if(live==null)live=new JSONObject();
        try{
            JSONObject ns=envelope==null?null:envelope.optJSONObject("narrative_state");JSONObject incoming=ns==null?null:ns.optJSONObject("live_table");
            String pattern=incoming==null?"":cleanEnum(incoming.optString("pattern"),TablePattern.class);if(pattern.length()==0)pattern=localPattern(action,scene).name();
            String mode=incoming==null?"":cleanEnum(incoming.optString("mode"),CampaignMode.class);if(mode.length()==0)mode=live.optString("mode",CampaignMode.HYBRID.name());
            live.put("mode",mode).put("last_pattern",pattern).put("last_action",tail(human(action),500)).put("last_beat",tail(human(narration),1200)).put("updated_at",System.currentTimeMillis());
            JSONObject counts=live.optJSONObject("pattern_counts");if(counts==null)counts=new JSONObject();counts.put(pattern,counts.optInt(pattern,0)+1);live.put("pattern_counts",counts);
            if(incoming!=null){
                copyObjectField(live,incoming,"spotlight");copyObjectField(live,incoming,"convergence");copyObjectField(live,incoming,"shared_clock");copyObjectField(live,incoming,"capsule");copyObjectField(live,incoming,"guest_role");
                copyArrayField(live,incoming,"threads",24);copyArrayField(live,incoming,"scene_promises",36);copyArrayField(live,incoming,"faction_choices",24);copyArrayField(live,incoming,"dramatic_irony",24);
            }
            JSONObject historyEntry=new JSONObject().put("pattern",pattern).put("location",scene==null?"":scene.optString("location")).put("action",tail(human(action),260)).put("at",System.currentTimeMillis());
            JSONArray old=live.optJSONArray("handoff_history"),next=new JSONArray();if(old!=null)for(int i=Math.max(0,old.length()-19);i<old.length();i++)next.put(old.opt(i));next.put(historyEntry);live.put("handoff_history",next);
            root.put("live_table",live);
        }catch(Exception ignored){AppLog.ignored("LiveTableNarrativeDirector","Falha tolerada ao registrar direção de mesa.",ignored);}
        return root;
    }

    public static String memoryPrompt(JSONObject runtimeMemory){
        JSONObject live=runtimeMemory==null?null:runtimeMemory.optJSONObject("live_table");if(live==null||live.length()==0)return "";
        StringBuilder p=new StringBuilder("MEMÓRIA DE MESA VIVA: modo=").append(live.optString("mode","HYBRID")).append("; último padrão=").append(live.optString("last_pattern",""));
        JSONObject spot=live.optJSONObject("spotlight");if(spot!=null)p.append("; foco=").append(spot.optString("focus","")).append("; fila=").append(tail(String.valueOf(spot.optJSONArray("queue")),300));
        JSONArray threads=live.optJSONArray("threads");if(threads!=null&&threads.length()>0)p.append("; threads=").append(tail(threads.toString(),900));
        JSONObject conv=live.optJSONObject("convergence");if(conv!=null&&conv.length()>0)p.append("; convergência=").append(tail(conv.toString(),500));
        JSONObject clock=live.optJSONObject("shared_clock");if(clock!=null&&clock.length()>0)p.append("; relógio compartilhado=").append(tail(clock.toString(),300));
        JSONArray promises=live.optJSONArray("scene_promises");if(promises!=null&&promises.length()>0)p.append("; promessas=").append(tail(promises.toString(),700));
        p.append(". Preserve threads e conhecimento separados; não force convergência ou payoff antes da causalidade.\n");return p.toString();
    }

    /** QA complementar ao QA de prosa do Narrative Director 2.0. */
    public static String qualityProblem(String narration,JSONObject memory,JSONObject scene,String action){
        String raw=narration==null?"":Normalizer.normalize(narration,Normalizer.Form.NFC).trim();String n=human(raw),p=plain(n);if(n.length()==0)return "";
        if(containsAny(p,"voces nao sabem","vocês não sabem","sem que voces saibam","sem que vocês saibam","o jogador sabe mas o personagem nao","o jogador sabe mas o personagem não"))return "vaza bastidor do Mestre por falsa ironia dramática";
        if(containsAny(p,"voce sente medo","você sente medo","voce confia nele","você confia nele","voce decide que","você decide que","voces decidem que","vocês decidem que"))return "determina emoção, confiança ou decisão dos personagens";
        int menuLines=0;for(String line:raw.split("\\r?\\n")){String x=line.trim();if(x.matches("^(?:[-•*]|[0-9]+[.)]).{3,}"))menuLines++;}if(menuLines>=3)return "transforma a narração em menu de missões ou escolhas";
        JSONObject live=memory==null?null:memory.optJSONObject("live_table");String mode=live==null?"":live.optString("mode","");
        if("FOCUSED_ADVENTURE".equals(mode)){int hookNoise=count(p,"outra missao","outra missão","alem disso","além disso","ao mesmo tempo","um novo pedido","outro problema");if(hookNoise>=3)return "abre ganchos demais dentro de uma aventura focada";}
        if(p.startsWith("todos voces")||p.startsWith("todos vocês")){TablePattern pattern=localPattern(action,scene);if(pattern==TablePattern.ENSEMBLE_PROLOGUE)return "apresenta todo o elenco como bloco genérico em vez de cenas individuais";}
        return "";
    }

    private static String patternInstruction(TablePattern pattern){
        switch(pattern){
            case ENSEMBLE_PROLOGUE:return "PADRÃO ATUAL — PRÓLOGO DE ELENCO: dê uma pequena cena completa ao protagonista atual, com pessoa/rotina concreta e um problema imediato; encerre com ponte para o próximo foco ou para o alvo de convergência. Não apresente ficha em forma de lista.\n";
            case PARALLEL_CROSSCUT:return "PADRÃO ATUAL — CORTE PARALELO: identifique silenciosamente qual thread recebe a ação, quanto tempo passa e se algum relógio comum avança. Termine esta thread num ponto de decisão antes de cortar.\n";
            case FACTION_SANDBOX:return "PADRÃO ATUAL — SANDBOX DE FACÇÕES: mostre uma pressão concreta de no máximo duas forças relevantes. Diferencie oferta, custo, memória da relação e consequência de ignorar; não anuncie qual escolha é a correta.\n";
            case ADVENTURE_CAPSULE:return "PADRÃO ATUAL — AVENTURA CÁPSULA: preserve o limiar, a pergunta central e as promessas já abertas. Cada cena deve aproximar, complicar ou transformar essa pergunta; evite desviar para um novo arco.\n";
            case TACTILE_INVESTIGATION:return "PADRÃO ATUAL — INVESTIGAÇÃO TÁTIL: apresente coisa manipulável e resposta verificável do ambiente. Pare antes da conclusão quando ainda houver hipótese real.\n";
            case CINEMATIC_REVEAL:return "PADRÃO ATUAL — REVELAÇÃO: percepção física primeiro, contexto depois, consequência humana por último. Não prolongue a imagem após a decisão já estar clara.\n";
            case SOCIAL_BREATHING:return "PADRÃO ATUAL — RESPIRAÇÃO SOCIAL: permita conversa, humor ou intimidade sem inserir ameaça obrigatória. Ainda assim preserve objetivos, subtexto e memória das relações.\n";
            default:return "PADRÃO ATUAL — REVEZAMENTO: foque um protagonista ou interlocutor por vez, responda ao que aconteceu e faça o handoff somente por causalidade espacial, temporal ou social.\n";
        }
    }

    private static TablePattern localPattern(String action,JSONObject scene){
        String n=plain(action+" "+opt(scene,"mode")+" "+opt(scene,"scene_mode")+" "+opt(scene,"objective"));
        if(containsAny(n,"investigo","investigacao","investigação","examino","pista","rastro"))return TablePattern.TACTILE_INVESTIGATION;
        if(containsAny(n,"visao","visão","pesadelo","profecia","revelacao","revelação")||temperature(scene,action)>=4)return TablePattern.CINEMATIC_REVEAL;
        if(containsAny(n,"converso","pergunto","falo","descanso")&&temperature(scene,action)<=1)return TablePattern.SOCIAL_BREATHING;
        return TablePattern.SPOTLIGHT_RELAY;
    }

    private static int temperature(JSONObject scene,String action){
        String n=plain(action+" "+opt(scene,"danger")+" "+opt(scene,"tension"));if(containsAny(n,"combate","ataco","mortal","explosao","explosão","calamidade"))return 4;if(containsAny(n,"ameaca","ameaça","persegu","interrog"))return 3;if(containsAny(n,"investig","examino","pista"))return 2;if(containsAny(n,"chego","entro","convers"))return 1;return 0;
    }

    private static JSONObject compactLive(JSONObject live){
        JSONObject out=new JSONObject();try{String[] keys={"mode","last_pattern","spotlight","threads","convergence","shared_clock","scene_promises","capsule","guest_role"};for(String key:keys)if(live.has(key))out.put(key,live.opt(key));}catch(Exception ignored){AppLog.ignored("LiveTableNarrativeDirector","Falha tolerada ao compactar memória.",ignored);}return out;
    }
    private static int subgroupCount(JSONObject party){if(party==null)return 0;JSONObject p=party.optJSONObject("party");JSONArray a=p==null?null:p.optJSONArray("subgroups");if(a==null)a=party.optJSONArray("subgroups");return a==null?0:a.length();}
    private static int len(JSONObject obj,String key){JSONArray a=obj==null?null:obj.optJSONArray(key);return a==null?0:a.length();}
    private static String opt(JSONObject obj,String key){return obj==null?"":human(obj.optString(key,""));}
    private static String cleanEnum(String raw,Class<?> type){String n=human(raw).toUpperCase(Locale.ROOT).replace(' ','_').replace('-','_');if(n.length()==0)return "";Object[] constants=type.getEnumConstants();if(constants!=null)for(Object c:constants)if(String.valueOf(c).equals(n))return n;return "";}
    private static void copyObjectField(JSONObject target,JSONObject source,String key)throws Exception{JSONObject value=source.optJSONObject(key);if(value!=null)target.put(key,new JSONObject(value.toString()));}
    private static void copyArrayField(JSONObject target,JSONObject source,String key,int max)throws Exception{JSONArray value=source.optJSONArray(key);if(value==null)return;JSONArray out=new JSONArray();for(int i=Math.max(0,value.length()-max);i<value.length();i++)out.put(value.opt(i));target.put(key,out);}
    private static JSONObject copy(JSONObject src){try{return new JSONObject(src==null?"{}":src.toString());}catch(Exception e){return new JSONObject();}}
    private static int count(String hay,String... needles){int c=0;for(String n:needles){int from=0;while((from=hay.indexOf(plain(n),from))>=0){c++;from+=Math.max(1,n.length());}}return c;}
    private static boolean containsAny(String n,String... values){for(String v:values)if(n.contains(plain(v)))return true;return false;}
    private static String human(String s){if(s==null)return "";return Normalizer.normalize(s,Normalizer.Form.NFC).replaceAll("\\s+"," ").trim();}
    private static String plain(String s){String x=Normalizer.normalize(human(s).toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("\\s+"," ").trim();}
    private static String tail(String value,int max){String s=value==null?"":value;if(s.length()<=max)return s;return "…"+s.substring(s.length()-Math.max(1,max-1));}
}
