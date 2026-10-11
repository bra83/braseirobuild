package com.braseiro.dnd5e;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Seleciona uma abertura com estrutura, foco e linguagem realmente diferentes. */
public final class AdventureOpeningPlanner {
    public static final class Plan {
        public final String id,instruction,rulesFocus;
        Plan(String i,String x,String r){id=i;instruction=x;rulesFocus=r;}
    }
    private static final String[][] PLANS={
        {"negociacao_em_andamento","Abra no meio de uma negociação já em andamento. Um herói precisa responder a uma exigência concreta antes de compreender todo o contexto. Introduza um NPC nomeado com interesse, limite e gesto visual próprios."},
        {"rotina_interrompida","Comece durante uma tarefa cotidiana específica dos heróis e interrompa-a por uma consequência material, não por fumaça, ruído vago ou pressentimento. A primeira decisão deve envolver proteger algo, terminar a tarefa ou seguir a interrupção."},
        {"resultado_primeiro","Mostre primeiro o resultado estranho de um acontecimento já ocorrido. Só depois revele indícios incompatíveis sobre sua causa. Não comece em estrada, cruzamento, taverna ou diante de uma porta."},
        {"pedido_pessoal","Um NPC nomeado procura especificamente um dos heróis por causa de seu passado, profissão ou reputação. O pedido deve ter custo social e uma urgência verificável, sem usar o molde de mensageiro misterioso."},
        {"celebracao_com_fissura","Abra em uma celebração, mercado, rito ou competição ativa. Faça uma pequena irregularidade crescer diante dos personagens. Mantenha pessoas comuns agindo ao redor, em vez de esvaziar a cena."},
        {"escolha_moral_imediata","Comece com duas necessidades legítimas incompatíveis e tempo insuficiente para atender ambas. Não apresente vilão evidente; dê rosto e consequência às duas partes."},
        {"viagem_em_movimento","Abra com o grupo já em deslocamento e realizando uma ação concreta. Uma mudança de terreno, clima, recurso ou companhia altera o plano. Evite silêncio, fumaça, pegadas genéricas e patrulha como gatilho principal."},
        {"retorno_transformado","Os heróis chegam a um lugar conhecido que mudou de forma observável desde a última visita. Três detalhes cotidianos demonstram a mudança antes de qualquer explicação."},
        {"testemunho_contraditorio","Duas pessoas nomeadas descrevem o mesmo fato de maneiras incompatíveis enquanto uma consequência continua acontecendo à vista do grupo. As opções devem permitir intervir, separar ou verificar."},
        {"objeto_em_disputa","Abra com um objeto, animal, documento ou corpo sendo disputado publicamente. Cada lado possui uma razão compreensível e um risco imediato. Não esconda o conflito atrás de atmosfera vaga."},
        {"falha_do_ambiente","Uma estrutura, ponte, mecanismo, embarcação, mina ou proteção começa a falhar. A cena exige coordenação física e revela informação sobre o local durante a ação."},
        {"convite_perigoso","Uma autoridade, rival ou guilda oferece algo desejável em público, impondo uma condição que muda a posição social do grupo. O perigo deve estar no compromisso, não em ameaça anônima."},
        {"despertar_deslocado","Comece após uma elipse curta: o grupo desperta ou retoma a consciência em circunstâncias alteradas, mas sem perda total de memória. Objetos e relações permitem reconstruir o intervalo."},
        {"consequencia_de_reputacao","Uma decisão anterior, fama, dívida ou boato alcança o grupo na forma de pessoas concretas e demandas incompatíveis. Não recapitule a campanha; dramatize o efeito."},
        {"trabalho_em_progresso","Os heróis já aceitaram um trabalho simples e estão no meio dele quando descobrem que o objetivo foi descrito de forma enganosa. Mostre a discrepância materialmente."},
        {"evento_publico","Comece num julgamento, leilão, funeral, assembleia, recrutamento ou execução. Dê ao grupo posição clara no espaço e uma oportunidade de agir antes do evento seguir sem eles."},
        {"recurso_escasso","Um recurso necessário é insuficiente e três grupos presentes o reivindicam. A escassez deve ser mensurável; as relações e o ambiente mudam conforme a escolha."},
        {"aliado_em_erro","Um aliado ou NPC simpático está prestes a cometer um erro compreensível. Os heróis podem impedir, apoiar ou redirecionar a ação, cada opção com custo."},
        {"fenomeno_visivel","Um fenômeno mágico ou natural claramente visível altera regras locais de movimento, percepção ou tempo. Não o descreva apenas como brilho, névoa ou som; mostre efeitos concretos em pessoas e objetos."},
        {"inimigo_vulneravel","Uma figura potencialmente hostil aparece em condição vulnerável e pede ou necessita algo. A cena deve permitir ajuda, captura, negociação ou observação sem impor combate."},
        {"mapa_incorreto","O grupo constata que mapa, ordem, contrato ou informação oficial está objetivamente errado. Uma pessoa presente depende desse erro e outra se beneficia dele."},
        {"companhia_dividida","Comece com desacordo interno prático sobre rota, gasto, aliado ou prioridade. Cada posição deve nascer de objetivos já existentes nas fichas, sem transformar companheiros em caricaturas."},
        {"descoberta_durante_acao","A descoberta ocorre enquanto um herói usa uma habilidade, ferramenta ou profissão da ficha. O primeiro foco é competência e agência, não exposição do narrador."},
        {"tempo_limite_visivel","Mostre um prazo por um processo observável — água subindo, fila avançando, sol se pondo, ritual progredindo, tropa chegando. Cada escolha altera o que será perdido ou preservado."}
    };
    private AdventureOpeningPlanner(){}

    public static Plan choose(String world,String campaign,String tone,String hex,String recentRaw,long seed){return choose(world,campaign,tone,hex,recentRaw,"[]",seed);}
    public static Plan choose(String world,String campaign,String tone,String hex,String recentRaw,String recentTextsRaw,long seed){
        Set<String> recent=new HashSet<String>();try{JSONArray a=new JSONArray(recentRaw==null?"[]":recentRaw);for(int i=0;i<a.length();i++)recent.add(a.optString(i));}catch(Exception ignored){AppLog.ignored("AdventureOpeningPlanner","Histórico de abertura inválido; usando valores seguros.",ignored);}
        ArrayList<String[]> candidates=new ArrayList<String[]>();for(String[] p:PLANS)if(!recent.contains(p[0]))candidates.add(p);if(candidates.isEmpty())for(String[] p:PLANS)candidates.add(p);
        long mixed=seed^safe(world).hashCode()^((long)safe(campaign).hashCode()<<17)^((long)safe(hex).hashCode()<<3);int index=(int)Math.floorMod(mixed,candidates.size());String[] picked=candidates.get(index);
        String bans=recentMotifs(recentTextsRaw);
        String instruction="ARQUITETURA DE ABERTURA: "+picked[1]+" Use o tom “"+safe(tone)+"” sem repetir frases prontas. "+
                "A cena deve começar por ação, relação ou consequência concreta; varie tamanho dos parágrafos, ponto de vista, ritmo e quantidade de informação. "+
                "Não use como abertura padrão: grupo parado observando, fumaça entre árvores, ruído sutil, silêncio pesado, rocha usada como esconderijo, patrulha distante, guarita, encruzilhada ou uma entrada a examinar. "+
                "Não anuncie a estrutura nem force quatro capítulos. "+bans;
        return new Plan(picked[0],instruction.trim(),ruleFocusFor(picked[0]));
    }

    private static String ruleFocusFor(String id){
        if("negociacao_em_andamento".equals(id)||"pedido_pessoal".equals(id)||"convite_perigoso".equals(id)||"consequencia_de_reputacao".equals(id))
            return "interação social, atitude de NPC, Intuição, Persuasão, Enganação, reputação, contratos e negociação";
        if("testemunho_contraditorio".equals(id)||"mapa_incorreto".equals(id)||"resultado_primeiro".equals(id)||"retorno_transformado".equals(id))
            return "Investigação, Percepção, Intuição, pistas, documentos, busca e resolução de incerteza";
        if("viagem_em_movimento".equals(id)||"companhia_dividida".equals(id))
            return "viagem, ritmo de marcha, terreno, clima, navegação, provisões e encontros";
        if("falha_do_ambiente".equals(id)||"tempo_limite_visivel".equals(id)||"fenomeno_visivel".equals(id))
            return "perigos ambientais, testes de habilidade, ferramentas, movimento, visão, tempo e dano";
        if("objeto_em_disputa".equals(id)||"evento_publico".equals(id)||"celebracao_com_fissura".equals(id))
            return "interação social, iniciativa, multidões, disputa, itens, Persuasão e Intuição";
        if("inimigo_vulneravel".equals(id)||"aliado_em_erro".equals(id))
            return "ajudar, condições, agarrar, estabilizar, reação, iniciativa, moral e interação social";
        if("recurso_escasso".equals(id))
            return "recursos, carga, provisões, comércio, negociação, Persuasão e facções";
        if("despertar_deslocado".equals(id))
            return "descanso, condições, percepção, investigação, memória e recuperação";
        return "testes de habilidade, perícias, ferramentas, exploração, percepção, investigação e consequências";
    }
    public static String remember(String raw,String id){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();try{JSONArray old=new JSONArray(raw==null?"[]":raw);for(int i=Math.max(0,old.length()-7);i<old.length();i++)if(!id.equals(old.optString(i)))out.put(old.optString(i));}catch(Exception ignored){AppLog.ignored("AdventureOpeningPlanner","Histórico de abertura inválido; usando valores seguros.",ignored);}out.put(id);return out.toString();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static String rememberText(String raw,String text){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();try{JSONArray old=new JSONArray(raw==null?"[]":raw);for(int i=Math.max(0,old.length()-4);i<old.length();i++)out.put(old.optString(i));}catch(Exception ignored){AppLog.ignored("AdventureOpeningPlanner","Histórico de abertura inválido; usando valores seguros.",ignored);}String clean=safe(text).replaceAll("\\s+"," ").trim();if(clean.length()>700)clean=clean.substring(0,700);if(clean.length()>0)out.put(clean);return out.toString();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String recentMotifs(String raw){Set<String> motifs=new HashSet<String>();String all="";try{JSONArray a=new JSONArray(raw==null?"[]":raw);for(int i=0;i<a.length();i++)all+=" "+a.optString(i).toLowerCase(Locale.ROOT);}catch(Exception ignored){AppLog.ignored("AdventureOpeningPlanner","Histórico de abertura inválido; usando valores seguros.",ignored);}String[][] groups={{"estrada","estrada"},{"cruzamento","cruzamento/encruzilhada"},{"fumaca","fumaça"},{"silencio","silêncio"},{"rocha","rocha"},{"guarita","guarita"},{"patrulha","patrulha"},{"ruido","ruído misterioso"},{"porta","porta/entrada"},{"trilha","trilha/rastro"}};String norm=java.text.Normalizer.normalize(all,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");for(String[] g:groups)if(norm.contains(g[0]))motifs.add(g[1]);if(motifs.isEmpty())return "";StringBuilder b=new StringBuilder("ABERTURAS RECENTES já usaram ");int i=0;for(String m:motifs){if(i++>0)b.append(", ");b.append(m);}return b.append("; não use esses elementos como motor desta abertura.").toString();}
    private static String safe(String s){return s==null?"":s;}
}
