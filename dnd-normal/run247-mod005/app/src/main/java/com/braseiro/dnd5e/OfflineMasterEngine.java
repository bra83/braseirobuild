package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Mestre local de contingência. Ele mantém a campanha utilizável sem internet
 * e sem chave Gemini. O motor não inventa valores mecânicos: descreve a cena,
 * propõe decisões e encaminha incertezas ao rolador local.
 */
public final class OfflineMasterEngine {
    public JSONObject generate(String mode,String action,MasterOrchestrator.Plan plan,List<JSONObject> localHits){
        String safeMode=mode==null?"Narrador":mode;
        String inputChannel=plan==null?null:plan.inputChannel;
        BraseiroNarrativeDirector.Intent intent=BraseiroNarrativeDirector.classify(action,inputChannel);
        if("Regras RAW/RAI".equals(safeMode)||"Sortear dos livros".equals(safeMode))return manualAnswer(safeMode,action,localHits);
        if(BraseiroNarrativeDirector.isNonAdvancing(action,inputChannel))return clarification(action,plan,intent);
        return narrative(action,plan);
    }

    private JSONObject clarification(String action,MasterOrchestrator.Plan plan,BraseiroNarrativeDirector.Intent intent){/*BR_JSON_WRAP*/try{
        JSONObject root=parseObject(plan==null?"{}":plan.compactContext);JSONObject scene=root.optJSONObject("scene");if(scene==null)scene=new JSONObject();
        JSONObject memory=new JSONObject();JSONObject checkpoint=new JSONObject();
        String previous=root.optJSONObject("story")==null?"":root.optJSONObject("story").optString("last_narration","");
        try{checkpoint.put("narration",previous).put("location",scene.optString("location","esta região"));memory.put("checkpoint",checkpoint);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar checkpoint local.",ignored);}
        StringBuilder text=new StringBuilder();
        if(intent==BraseiroNarrativeDirector.Intent.RECAP_QUERY){text.append(previous.length()>0?"Até o ponto em que a cena foi interrompida: "+clean(previous,1000):"Ainda não há uma cena anterior suficiente para recapitular com segurança.");}
        else if(intent==BraseiroNarrativeDirector.Intent.SPATIAL_CLARIFICATION){text.append("A cena continua em ").append(clean(scene.optString("location","esta região"),100)).append(". O Mestre local só confirma a geometria já estabelecida; a pergunta não faz o tempo passar nem cria um novo obstáculo.");}
        else if(intent==BraseiroNarrativeDirector.Intent.PLANNING){text.append("Isso fica tratado apenas como uma possibilidade de plano. Nada foi executado e a cena permanece exatamente no mesmo instante.");}
        else{text.append("A pergunta é uma clarificação sobre a situação atual. Sem uma fonte local ou um detalhe já estabelecido que permita afirmar mais, o Mestre preserva o ponto da cena em vez de inventar uma resposta que altere o mundo.");}
        JSONObject out=emptyEnvelopeWithNarration(text.toString());try{out.put("_offline_master",new JSONObject().put("enabled",true).put("non_advancing",true));out.put("_non_advancing",true).put("_narrative_intent",intent.name());}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao marcar esclarecimento local.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject narrative(String action,MasterOrchestrator.Plan plan){/*BR_JSON_WRAP*/try{
        JSONObject root=parseObject(plan==null?"{}":plan.compactContext);
        JSONObject bus=root.optJSONObject("game_bus");if(bus==null)bus=new JSONObject();
        JSONObject scene=firstObject(root.optJSONObject("scene"),bus.optJSONObject("scene"));
        JSONObject story=root.optJSONObject("story");if(story==null)story=bus.optJSONObject("story");if(story==null)story=new JSONObject();
        JSONObject rules=root.optJSONObject("rules_frame");if(rules==null)rules=new JSONObject();
        JSONArray heroes=bus.optJSONArray("active_heroes_full");if(heroes==null)heroes=new JSONArray();
        JSONArray npcs=bus.optJSONArray("npcs");if(npcs==null)npcs=new JSONArray();

        String catalogLocation=extractBetween(action,"LOCAL NOMEADO:",";");
        String location=clean(catalogLocation.length()>0?catalogLocation:scene.optString("location","esta região"),90);if(location.length()==0)location="esta região";
        String weather=clean(scene.optString("weather",""),70);
        String danger=clean(scene.optString("danger",""),120);
        String objective=clean(scene.optString("objective",""),150);
        String previous=clean(story.optString("last_narration",""),480);
        String declared=playerFacingAction(action);
        JSONObject hero0=heroes.length()>0?heroes.optJSONObject(0):null;
        String actor=hero0!=null?clean(hero0.optString("name","A companhia"),70):"A companhia";
        String heroArmor=hero0!=null?clean(hero0.optString("visual_description",hero0.optString("armor","")),90):"";
        String heroEquip=hero0!=null?clean(hero0.optString("equipment",hero0.optString("weapons","")),90):"";
        String npc=npcs.length()>0?clean(npcs.optJSONObject(0).optString("name",""),70):"";
        String verified=resultMarker(action);
        String occasion=plan==null?"continuation":plan.barbaraStoryOccasion;if(occasion==null||occasion.trim().length()==0)occasion="continuation";
        boolean opening=isOpeningDirective(action)||"campaign_opening".equals(occasion);
        boolean presentation=WorldOnboardingNarrativeGuard.isPresentation(occasion);
        String catalog=clean(extractBetween(action,"REFERÊNCIA DO CATÁLOGO", "Use este local como âncora"),520);boolean localWater=!action.toUpperCase(new Locale("pt","BR")).contains("HIDROGRAFIA LOCAL CONFIRMADA PELO ATLAS: NENHUMA");

        StringBuilder text=new StringBuilder();
        if(presentation){
            if(opening)text.append("A chegada a ").append(location).append(" ganha forma pela luz e pelo relevo ao redor. ");
            else text.append("Ao alcançar ").append(location).append(", a companhia encontra o lugar já em movimento contínuo. ");
            if(weather.length()>0)text.append("O tempo está ").append(lowerFirst(weather)).append(", com o vento batendo no rosto e alterando o uso do espaço sob as botas. ");
            if(catalog.length()>0){String fact=naturalizeCatalog(catalog,localWater);if(fact.length()>0)text.append(clean(fact,320)).append(' ');}
            if(hero0!=null){
                text.append(actor);
                if(heroArmor.length()>0)text.append(", vestindo ").append(lowerFirst(heroArmor));
                if(heroEquip.length()>0)text.append(", sente o peso de ").append(lowerFirst(heroEquip)).append(" ao alcance das mãos");
                text.append(", mantendo a postura firme na marcha. ");
            }
            if(npc.length()>0)text.append(npc).append(" está em plena atividade e pode ser individualizado na cena. ");
            if(danger.length()>0)text.append("Há um sinal concreto de perigo: ").append(lowerFirst(danger)).append(". ");
            if(objective.length()>0)text.append("O objetivo já estabelecido continua sendo ").append(lowerFirst(objective)).append(". ");
            text.append("O restante permanece para ser descoberto pelas ações da companhia.");
        }else{
            text.append("Em ").append(location).append(", ");
            if(weather.length()>0)text.append("sob o tempo ").append(lowerFirst(weather)).append(", ");
            if(declared.length()>0)text.append(lowerFirst(declared)).append(". ");
            else text.append(actor).append(" observa por um instante antes de escolher o próximo gesto. ");
            if(hero0!=null&&declared.length()>0&&heroEquip.length()>0){
                text.append(actor).append(" firma ").append(lowerFirst(heroEquip)).append(" enquanto o vento move a barra das roupas. ");
            }
            if(npc.length()>0)text.append(npc).append(" está em atividade no local, percebe o movimento e reage sem antecipar o que a companhia fará depois. ");
            if(verified.length()>0){
                if("sucesso".equals(verified))text.append("A tentativa funciona, e a situação se abre exatamente a partir desse resultado. ");
                else if("falha".equals(verified))text.append("A tentativa não alcança o efeito pretendido, e a situação responde a essa falha. ");
                else text.append("O resultado já obtido passa a definir o que acontece a seguir. ");
            }else if(danger.length()>0)text.append("A ameaça continua presente: ").append(lowerFirst(danger)).append(". ");
            if(previous.length()>0)text.append("O que já estava em jogo permanece reconhecível: pessoas, posições e tensões não são substituídas por uma cena genérica. A ação atual modifica apenas o que ela alcança de fato. ");
            if(objective.length()>0)text.append("Adiante, ainda está em jogo ").append(lowerFirst(objective)).append(". ");
            text.append("O cenário oferece detalhes observáveis em tempo real, sem decidir a próxima ação pelo grupo. Há espaço para continuar a conversa, investigar o que mudou, reposicionar-se ou agir. A decisão permanece nas mãos da companhia.");
        }

        JSONObject out=emptyEnvelope();
        try{
            String narration=GeneratedTextSanitizer.narrative(text.toString());
            out.put("narration",narration);
            out.put("visual_prompt","Cena de fantasia em "+location+", atmosfera "+(weather.length()>0?weather:"coerente com o local")+", ação concreta, composição cinematográfica, sem texto na imagem");
            out.put("options",options(location,npc,danger,objective,declared));
            JSONObject suggested=rules.optJSONObject("suggested_check");
            if(!presentation&&verified.length()==0&&suggested!=null&&suggested.length()>0){
                JSONObject pending=new JSONObject();pending.put("required",true).put("action",declared.length()>0?declared:"ação declarada");
                copy(suggested,pending,"type","ability","skill","mode","save_type","thief_skill","general_skill","mechanic");
                JSONArray actors=rules.optJSONArray("actors");if(actors!=null&&actors.length()==1)pending.put("character",actors.optJSONObject(0).optString("name"));
                if(!pending.has("type"))pending.put("type","ability");
                if(!pending.has("ability"))pending.put("ability","Sabedoria");
                if(!"mystara".equalsIgnoreCase(bus.optString("world_profile")))pending.put("dc",10);
                out.put("pending_test",pending);out.put("options",new JSONArray());
            }
            out.put("_offline_master",new JSONObject().put("enabled",true).put("mechanics_authority","local").put("reason","contingency"));
        }catch(Exception failure){return emptyEnvelopeWithNarration("A cena não pôde ser concluída com segurança. Nenhuma consequência foi aplicada.");}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String completePresentationWithBarbara(String existingNarration,String occasion,MasterOrchestrator.Plan plan,JSONObject sceneState){
        String occ=occasion==null||occasion.trim().length()==0?(plan!=null&&plan.barbaraStoryOccasion!=null&&plan.barbaraStoryOccasion.trim().length()>0?plan.barbaraStoryOccasion.trim():"first_arrival"):occasion.trim();
        String draft=existingNarration==null?"":existingNarration.trim();
        if(WorldOnboardingNarrativeGuard.problem(draft,occ,null).length()==0)return draft;
        String loc=sceneState==null?"":sceneState.optString("location","");
        String reg=sceneState==null?"":sceneState.optString("region","");
        String geo=sceneState==null?"":sceneState.optString("geography","");
        String poi=sceneState==null?"":sceneState.optString("poi","");
        String weather=sceneState==null?"":sceneState.optString("weather_description","");
        String danger=sceneState==null?"":sceneState.optString("danger_description","");
        String obj=sceneState==null?"":sceneState.optString("objective","");
        String actor="A companhia";
        String npc="";
        if(sceneState!=null){
            JSONArray npcs=sceneState.optJSONArray("present_npcs");
            if(npcs!=null&&npcs.length()>0&&npcs.optJSONObject(0)!=null)npc=npcs.optJSONObject(0).optString("name","");
        }
        String supplement="campaign_opening".equals(occ)
            ?buildOpeningNarration(loc.length()>0?loc:"esta região","",reg,"",weather,danger,obj,actor,npc)
            :buildDiscoveryNarration(occ,loc,reg,geo,poi,weather,danger,obj,actor,npc);
        String combined=draft.length()>0?(draft+"\n\n"+supplement):supplement;
        return ensurePresentationMinimum(combined,occ,loc,geo,weather,obj);
    }

    private static String buildDiscoveryNarration(String occasion,String location,String region,String geography,String poi,String weather,String danger,String objective,String actor,String npc){
        String place=location.length()>0?location:"esta região",geo=geography.length()>0?geography:"o terreno ao redor",climate=weather.length()>0?weather:"o tempo atual";
        StringBuilder out=new StringBuilder();
        if("first_arrival".equals(occasion)){
            out.append("A chegada a ").append(place).append(" começa antes de qualquer conversa importante. Pela estrada e pelo caminho de aproximação, a companhia ganha primeiro uma noção de escala: ").append(region.length()>0?place+" faz parte de "+region:place+" se relaciona com a região percorrida").append(", enquanto ").append(geo).append(" define o relevo, a distância aparente e os limites que podem ser percebidos sem exigir que ninguém conheça um mapa de memória. O lugar surge como destino concreto, não como um nome que deveria ser familiar. O grupo pode comparar o horizonte, a direção de onde veio e a entrada à frente antes de escolher para onde voltar a atenção.\n\n");
            out.append("Ao se aproximar do limiar de ").append(place).append(", o que importa é a vida que já estava acontecendo. Pessoas, animais, cargas, portas, caminhos e serviços só entram na descrição quando o estado da cena permite vê-los; o Mestre local não inventa uma história secreta para preencher silêncio. O ritmo do lugar, o modo como os presentes ocupam o espaço e a maneira como reagem a recém-chegados formam a primeira leitura social. ").append(npc.length()>0?npc+" está entre as presenças já reconhecidas na cena, e sua posição passa a ter significado sem revelar o que essa pessoa não mostrou.":"Nenhuma figura recebe importância artificial apenas para fabricar um guia do cenário.").append("\n\n");
            out.append("O clima também participa dessa primeira impressão. ").append(weather.length()>0?"Neste momento, "+lowerFirst(climate)+" altera luz, sensação, ruído e a maneira como o espaço é usado.":"O tempo só ganha destaque na medida em que é perceptível e produz consequência concreta.").append(" A companhia percebe essas condições como parte do deslocamento e da rotina local, não como um painel separado da história. Se houver lama, abrigo, visibilidade reduzida, ruas vazias ou trabalho interrompido, isso precisa vir do estado ambiental persistente; se não houver, o Mestre não cria tais efeitos por decoração.\n\n");
            out.append("A arquitetura e os sinais de atividade ajudam a transformar ").append(place).append(" em um espaço navegável. Em vez de despejar nomes de facções, deuses ou bairros, a apresentação se concentra no que os personagens podem de fato perceber e no significado imediato desses sinais. Um símbolo só é explicado quando está presente; uma instituição só ganha nome quando passa a importar; uma rota só é tratada como existente quando foi confirmada. Assim, a companhia pode olhar ao redor sem precisar ter lido um livro do cenário para entender a diferença entre uma entrada, uma área pública, uma zona de trabalho e um caminho que leva adiante.\n\n");
            out.append("A economia e o poder aparecem do mesmo modo: através de atividade, espera, acesso, preço, vigilância ou ausência perceptível, jamais por um resumo de enciclopédia. O que ").append(place).append(" produz, teme ou disputa só se torna fato quando a campanha já o revelou ou quando fontes locais verificáveis o sustentam. Ainda assim, a chegada mostra que cada pessoa tem rotina e objetivo próprios. A companhia entra num mundo que se movia antes dela e continuará mudando depois, e isso é mais útil para jogar do que uma lista de datas e nomes sem contexto.\n\n");
            out.append("Só depois dessa orientação a situação imediata ganha foco. ").append(danger.length()>0?"Há uma ameaça já estabelecida na cena: "+lowerFirst(danger)+". Ela é apresentada pelo que pode ser percebido, sem resolver a reação da companhia por antecipação.":"Nenhuma ameaça nova é acrescentada apenas para terminar a introdução com ação.").append(' ').append(objective.length()>0?"O objetivo que já estava em jogo continua sendo "+lowerFirst(objective)+", agora colocado dentro de um espaço que o jogador consegue imaginar.":"Se existe um objetivo, ele permanece o que a campanha já registrou; a apresentação não fabrica um novo.").append("\n\n");
            out.append("Com o limiar atravessado, ").append(actor).append(" e os demais podem finalmente escolher o que observar, quem abordar e que direção seguir. O ponto importante é que a cidade, vila ou outro lugar nomeado já tem posição relativa, atmosfera, atividade e presença humana suficientes para sustentar uma decisão. O Mestre não pressupõe memória do jogador e não encerra a chegada com um catálogo de opções genéricas: a agência nasce do que está diante do grupo, e o próximo passo continua pertencendo à companhia.\n\n");
            out.append("A primeira chegada termina quando ").append(place).append(" deixou de ser apenas uma palavra e passou a ser um lugar jogável. O que ainda é desconhecido permanece desconhecido. Histórias, alianças, atalhos, segredos e reputações terão de ser descobertos por observação, conversa, pesquisa ou consequência. A cena entrega contexto suficiente para agir agora, mas conserva perguntas reais para depois.");
        }else if("poi_discovery".equals(occasion)){
            String landmark=poi.length()>0?poi:place;
            out.append("O novo ponto de interesse se separa do entorno primeiro pela aproximação. ").append(landmark).append(" ganha forma em relação a ").append(place).append(" e a ").append(geo).append(", de modo que a companhia consiga entender de onde veio, onde o marco está e que espaço existe ao redor dele. O nome não funciona como atalho de conhecimento: sua importância nasce do que pode ser visto, ouvido ou alcançado agora.\n\n");
            out.append("A entrada, os limites e as passagens visíveis são mais úteis do que uma história secreta inventada. O Mestre descreve apenas portas, caminhos, vigias, sinais, filas, ruínas ou atividade que o estado confirmado sustenta. Isso torna o lugar acionável: o grupo pode reconhecer onde parar, por onde se aproximar e que parte permanece fora de alcance sem precisar receber um mapa técnico em voz alta.\n\n");
            out.append("A função do lugar aparece através do uso atual do espaço. Pessoas e objetos já presentes mostram circulação, espera, trabalho, abandono ou vigilância conforme a cena realmente registra. ").append(npc.length()>0?npc+" continua sendo uma presença reconhecida e não recebe conhecimento extra só porque o foco mudou.":"Nenhum NPC novo é criado apenas para servir de guia.").append(" O marco passa a existir na ficção porque há algo concreto diante da companhia, não porque o Mestre decidiu recitar seu verbete.\n\n");
            out.append("Também fica claro o que ainda não é conhecido. A companhia pode perceber desgaste, símbolos, acessos, sons ou alterações somente quando eles constam da situação; motivo, autoria e segredo permanecem perguntas até que alguma ação os revele. Essa separação impede que uma descrição rica vire spoiler e preserva o valor da investigação.\n\n");
            out.append(weather.length()>0?"O clima atual — "+climate+" — modifica apenas os aspectos físicos que pode realmente alcançar, como sensação, visibilidade, piso ou circulação quando essas consequências estão presentes. Ele não inventa enchentes, rios ou passagens inexistentes.\n\n":"O ambiente físico continua obedecendo às condições já registradas, sem acrescentar obstáculos decorativos.\n\n");
            out.append(objective.length()>0?"O objetivo atual, "+lowerFirst(objective)+", agora pode ser relacionado a esse marco sem afirmar que o lugar contém a resposta. ":"Nenhum novo objetivo é imposto pela descoberta. ").append("A cena termina diante de uma decisão espacial real: aproximar-se, observar melhor, procurar acesso, falar com alguém já presente ou recuar continua sendo escolha da companhia.");
        }else if("geography_discovery".equals(occasion)){
            out.append("A mudança de geografia precisa ser sentida antes de ser nomeada. O caminho segue por ").append(geo).append(", e a companhia passa a medir o terreno pelo relevo, pela direção da estrada ou trilha, pelo horizonte e pela forma como a distância parece se abrir ou se fechar. O Mestre local evita códigos de mapa: o importante é saber o que está adiante, atrás e ao redor.\n\n");
            out.append("Solo, vegetação, rocha, encosta, vale, planície, floresta ou outra característica só entram quando são sustentados pelo Atlas ou pelo estado ambiental. A descrição transforma esses dados em orientação física: onde a marcha sobe ou desce, o que bloqueia visão, que direção parece mais fácil e que parte do terreno exige atenção. Nenhum rio, lago, ponte ou travessia é acrescentado quando a cartografia não os confirma.\n\n");
            out.append("A geografia também altera o ritmo da viagem sem decidir resultado mecânico por conta própria. ").append(weather.length()>0?"Com "+lowerFirst(climate)+", terreno e visibilidade são combinados apenas pelas consequências já calculadas pelo diretor ambiental.":"Sem mudança climática relevante, o terreno continua sendo a principal referência física desta batida.").append(" O grupo pode comparar rota, cobertura, exposição e esforço antes de escolher como avançar.\n\n");
            out.append("Essa orientação espacial não revela automaticamente quem vive além do horizonte nem que perigos existem fora da percepção. Ela mostra caminhos e limites, não segredos. Se houver sinais concretos de passagem, atividade ou ameaça, eles pertencem à cena; se não houver, o silêncio continua sendo silêncio em vez de convite para inventar uma emboscada.\n\n");
            out.append("Quando a apresentação termina, ").append(actor).append(" e os demais conseguem imaginar a região sem precisar abrir um atlas externo: sabem a direção geral do deslocamento, a relação entre caminho e relevo, o tipo de terreno que os cerca e por que isso pode importar à próxima decisão. O restante continua para ser descoberto em jogo.");
        }else if("weather_transition".equals(occasion)){
            out.append("A mudança chega primeiro pelos sentidos. ").append(climate).append(" altera céu, vento, temperatura, umidade, som ou visibilidade conforme o fenômeno registrado, e a transição é perceptível em vez de simplesmente trocar uma palavra no cabeçalho. A companhia sente que o tempo virou enquanto ainda ocupa ").append(place).append(".\n\n");
            out.append("O efeito importante vem logo depois: terreno, estrada, rua, mercado, patrulha, viagem, rastros ou procura por abrigo só mudam quando o diretor ambiental já calculou essa consequência. O Mestre não inventa água onde não há curso confirmado e não transforma chuva comum em enchente por dramatização. O clima passa a importar porque altera alguma possibilidade real do mundo vivo.\n\n");
            out.append("NPCs e atividades também obedecem ao mesmo vínculo geográfico. ").append(npc.length()>0?npc+" pode reagir apenas de modo compatível com sua presença e agenda atuais.":"Pessoas distantes não mudam de plano só porque a companhia viu o tempo fechar aqui.").append(" Entregas, patrulhas, caravanas ou trabalho externo podem sofrer atraso quando o sistema já registrou essa pressão, e o atraso continua existindo depois da cena.\n\n");
            out.append("Quando a batida termina, o clima não foi um efeito decorativo: existe uma condição física nova e pelo menos uma consequência observável para considerar. O próximo passo ainda é da companhia, e o rescaldo — lama, neve, poeira, atraso ou retomada — só permanecerá se estiver no estado persistente que sustenta as próximas cenas.");
        }else{ // changed_return
            out.append("O retorno a ").append(place).append(" começa pelo que a companhia realmente pode reconhecer. A direção de aproximação, ").append(geo).append(" e os contornos já conhecidos devolvem orientação antes que qualquer novidade seja destacada. A memória usada aqui é a da campanha, não uma suposição de que o jogador se lembra de um livro do cenário.\n\n");
            out.append("Elementos familiares são lembrados apenas o suficiente para localizar o grupo. Depois, diferenças perceptíveis ganham foco: atividade, acesso, presença de pessoas, sinais de desgaste, clima ou tensão mudam somente quando o estado persistente mostra que mudaram durante a ausência. O Mestre não reescreve o lugar inteiro para parecer novo.\n\n");
            out.append(npc.length()>0?npc+" continua ligado ao que já foi estabelecido sobre sua presença, memória e objetivos. Se sua atitude ou rotina mudou, isso deve aparecer por comportamento observável, não por exposição do pensamento privado.\n\n":"Nenhum rosto novo é inventado apenas para anunciar que o lugar mudou; as mudanças precisam surgir do mundo registrado.\n\n");
            out.append(weather.length()>0?"O clima atual, "+climate+", é comparado apenas ao que a cena torna perceptível e pode explicar diferenças de circulação ou terreno quando o sistema ambiental assim determinou.\n\n":"O ambiente continua com as consequências físicas que realmente sobreviveram desde a visita anterior.\n\n");
            out.append(objective.length()>0?"O objetivo em jogo permanece "+lowerFirst(objective)+", mas o retorno oferece contexto para reavaliar como persegui-lo dentro do lugar que mudou. ":"A apresentação não cria uma missão nova. ").append(danger.length()>0?"A ameaça já registrada, "+lowerFirst(danger)+", só recebe destaque na medida em que pode ser percebida agora.":"Riscos desconhecidos continuam desconhecidos.").append("\n\n");
            out.append("A reorientação termina quando a companhia consegue separar lembrança de novidade e está novamente em posição de escolher. O lugar conserva sua geometria conhecida, o mundo conserva as mudanças ocorridas durante a ausência e a próxima ação não é decidida pelo Mestre.");
        }
        return out.toString();
    }

    private static String ensurePresentationMinimum(String text,String occasion,String location,String geography,String weather,String objective){
        int minWords="campaign_opening".equals(occasion)?1200:("first_arrival".equals(occasion)?850:("weather_transition".equals(occasion)?350:("geography_discovery".equals(occasion)?500:600)));
        int minParas="campaign_opening".equals(occasion)?10:("first_arrival".equals(occasion)?8:("weather_transition".equals(occasion)?4:("geography_discovery".equals(occasion)?5:6)));
        String out=text==null?"":text.trim();int guard=0;
        while((WorldOnboardingNarrativeGuard.countWords(out)<minWords||WorldOnboardingNarrativeGuard.countParagraphs(out)<minParas)&&guard++<24){
            String place=location.length()>0?location:"esta região",geo=geography.length()>0?geography:"o terreno",climate=weather.length()>0?weather:"as condições do momento";
            String extra="A companhia ainda pode usar esta pausa para se orientar em "+place+": "+geo+" define espaço e direção, enquanto "+climate+" só altera aquilo que é fisicamente perceptível. O Mestre local preserva o que já está estabelecido e mantém desconhecido o que ainda não foi investigado. "+(objective.length()>0?"O objetivo existente — "+lowerFirst(objective)+" — continua visível como motivo para agir, sem transformar a apresentação em uma decisão automática. ":"")+"Pessoas, caminhos e consequências permanecem ligados ao estado persistente da campanha; nenhum detalhe novo é acrescentado apenas para preencher a descrição. Quando houver uma escolha significativa, ela continua pertencendo aos jogadores.";
            out+=(out.length()>0?"\n\n":"")+extra;
        }
        return out;
    }

    private static String buildOpeningNarration(String location,String category,String region,String placeFact,String weather,String danger,String objective,String actor,String npc){
        StringBuilder out=new StringBuilder();
        String scale=(category.length()>0?"Trata-se de "+lowerFirst(category):"Trata-se de um lugar habitado ou percorrido")+(region.length()>0?" dentro de "+region:"")+". ";
        String climate=weather.length()>0?weatherSentence(weather)+" ":"";
        out.append("Antes que ").append(location).append(" possa significar qualquer coisa para a companhia, é preciso situá-la no mundo que existe ao redor. ").append(scale).append("O nome não chega como uma senha que todos deveriam reconhecer: ele ganha sentido pela estrada, pelo relevo, pelas distâncias vencidas e por aquilo que muda à medida que a viagem se aproxima do destino. ").append(placeFact).append(" A região não está congelada à espera dos aventureiros. Pessoas trabalham, carregam coisas, escolhem caminhos, discutem preços, protegem interesses e medem desconhecidos com a atenção prática de quem tem uma vida própria. ").append(climate).append("A chegada acontece dentro desse movimento, e não acima dele.\n\n");
        out.append("A aproximação permite que ").append(actor).append(" e os demais reconheçam primeiro a escala do lugar, antes de qualquer detalhe virar pista ou ameaça. O caminho muda de textura, o ruído do trânsito local se distingue do silêncio ou da monotonia da viagem, e surgem marcas de uso que não precisam de legenda: rastros de passagem, pontos em que gente espera, espaços mais cuidados e outros evitados, trajetos que parecem cotidianos para quem mora aqui. Nada disso exige que a companhia conheça previamente mapas, reinos, facções ou histórias. O próprio cenário começa a ensinar como se organiza, mostrando por onde a vida entra, onde ela se concentra e de que maneira as pessoas ocupam o espaço.\n\n");
        out.append("Também fica claro que o nome ").append(location).append(" não designa apenas um ponto no mapa. Ele corresponde a relações concretas entre quem chega, quem parte, quem vende, quem guarda, quem observa e quem depende do lugar para continuar vivendo. O que é importante não aparece como verbete de manual, mas na diferença entre uma passagem usada e outra quase vazia, na forma como mercadorias ou ferramentas são tratadas, no cuidado com portas, animais, carroças, mochilas ou balcões, na quantidade de gente parada e na quantidade de gente com pressa. Se houver autoridade, riqueza, medo, fé ou rivalidade aqui, seus sinais precisam ser percebidos de forma visível e concreta nos detalhes da cena.\n\n");
        out.append("A companhia entra nesse quadro sem receber conhecimento emprestado. Ninguém lhes entrega mentalmente a história do lugar, e o simples fato de um nome existir não transforma o nome em algo conhecido. O que eles podem saber agora é aquilo que a viagem, a ficha, a memória real da campanha e os sentidos permitem reconhecer. Vozes se sobrepõem em distâncias diferentes; gestos repetidos revelam rotinas; certas pessoas parecem saber exatamente para onde vão, enquanto outras procuram informação ou esperam uma oportunidade. O cenário oferece referências suficientes para que o jogador humano se oriente junto com os personagens, em vez de ser obrigado a fingir que leu um livro de cenário antes de começar.\n\n");
        out.append("Quando os olhos deixam o panorama e se aproximam das pessoas, a diferença entre presença e decoração fica mais nítida. Há quem esteja ocupado demais para dar atenção à companhia, quem observe por curiosidade, quem calcule se os recém-chegados representam trabalho, problema ou lucro, e quem simplesmente queira atravessar o espaço sem participar de nada. ");
        if(npc.length()>0)out.append(npc).append(" está entre os rostos que podem ser individualizados. O nome só importa porque essa pessoa está concretamente presente e porque sua posição ou comportamento pode afetar a cena; nenhuma biografia oculta é revelada de graça. ");
        out.append("Outros figurantes permanecem exatamente isso: gente do lugar, reconhecível pelo que faz agora, sem identidades inventadas apenas para encher a narrativa.\n\n");
        out.append("O ambiente também mostra o custo de existir aqui. Onde existe circulação, há espera; onde existe comércio, há diferença entre quem pode pagar e quem precisa negociar; onde existe proteção, há alguma coisa que as pessoas julgam necessário proteger. O Braseiro não transforma essas relações em números invisíveis nem informa ao jogador fatos que os personagens não poderiam perceber. Em vez disso, deixa que o peso social apareça na maneira como se abre espaço para alguns e se fecha para outros, na qualidade dos objetos em uso, na conservação do lugar, no tom das conversas e na atenção dispensada a estrangeiros. É assim que a economia, a política e a cultura começam a ser compreendidas: por efeitos visíveis antes de explicações.\n\n");
        out.append("A orientação espacial continua enquanto a companhia avança apenas o suficiente para estar realmente dentro da cena. Atrás está o caminho que trouxe o grupo; adiante, o lugar se divide em possibilidades que podem ser vistas ou inferidas sem virar uma lista de botões. Há pontos de passagem, pessoas acessíveis, áreas de atividade e direções que exigiriam uma escolha deliberada para serem seguidas. O Mestre local não move ninguém automaticamente para dentro de uma construção, para perto de um desconhecido ou para o centro de um conflito. Ele estabelece o chão, as distâncias e o que está ao alcance para que qualquer ação seguinte tenha posição e consequência compreensíveis.\n\n");
        if(objective.length()>0)out.append("O motivo que trouxe a companhia até aqui não desaparece sob a apresentação do mundo. ").append("O objetivo já estabelecido é ").append(lowerFirst(objective)).append(". Ele funciona como uma linha de orientação, não como trilho obrigatório: ajuda a explicar por que certos detalhes chamam mais atenção, mas não força o grupo a ignorar pessoas, sinais ou oportunidades que surjam ao redor. O jogador não precisa conhecer previamente a importância histórica do destino para entender o que está tentando fazer agora; a relevância imediata nasce do objetivo já registrado e do que o cenário coloca diante dele.\n\n");
        else out.append("Não existe uma missão artificialmente acrescentada só para dar direção a esta abertura. Se o estado da campanha ainda não definiu um objetivo específico, a cena permanece honesta sobre isso. A companhia pode primeiro descobrir o que o lugar oferece, perguntar, observar e escolher a própria prioridade. Essa ausência de um trilho também é informação útil: evita que o sistema invente uma urgência ou uma autoridade que não existe nos dados persistidos apenas para fabricar uma aventura instantânea.\n\n");
        if(danger.length()>0)out.append("Há, porém, uma tensão concreta já registrada no estado da cena: ").append(lowerFirst(danger)).append(". Ela não surge como texto vermelho nem como conhecimento onisciente. O perigo precisa ocupar o mundo de maneira perceptível — alterando postura, ritmo, distância, conversa ou comportamento — antes que a companhia tenha motivo para tratá-lo como certeza. A presença dessa tensão não transforma a abertura em combate automático. Ela serve para mostrar por que a normalidade observada até aqui tem uma borda, e por que algumas escolhas podem ter custo sem que o Mestre decida antecipadamente qual será a resposta correta.\n\n");
        else out.append("Por enquanto, nenhuma ameaça confirmada precisa saltar sobre o grupo para provar que a aventura começou. A normalidade pode carregar pequenas fricções suficientes: alguém esperando, uma conversa que muda de tom, um trajeto menos usado, um olhar que demora, uma tarefa interrompida, uma porta que recebe mais atenção do que as outras. Esses sinais não são automaticamente pistas e não escondem uma verdade obrigatória; são textura de um mundo vivo. Se algum deles ganhar importância, será porque a companhia se aproximou, perguntou, observou ou porque o estado persistente realmente o transformou em acontecimento.\n\n");
        out.append("À medida que a câmera finalmente se fecha sobre o grupo, o cenário já deixou de ser apenas um nome. ").append(location).append(" agora possui escala, atividade, relações e caminhos perceptíveis. A companhia sabe onde está em termos humanos e práticos mesmo que ainda desconheça histórias, alianças e segredos. Isso é deliberado: conhecimento de cenário será conquistado em jogo. Quando um novo reino, templo, povo, organização, divindade ou conflito aparecer, ele deverá ganhar contexto no momento em que importar, em vez de ser despejado como uma sequência de nomes que só fariam sentido para quem estudou o material antes da sessão.\n\n");
        out.append("O presente imediato, então, ganha prioridade sobre qualquer enciclopédia. Pessoas continuam passando e trabalhando enquanto os aventureiros ocupam seu lugar entre elas. O som mais próximo, o rosto mais atento, o caminho mais direto e o motivo da viagem competem pela atenção sem se transformar em quatro opções fechadas. A companhia pode interromper uma rotina, iniciar uma conversa, examinar algo acessível, seguir em direção ao objetivo ou simplesmente permanecer um pouco mais e observar. O mundo não pausa enquanto essa decisão é tomada, mas também não avança uma consequência importante sem uma ação que a provoque.\n\n");
        out.append("É nesse ponto que a apresentação termina: não porque o lugar tenha sido explicado por completo, mas porque já existe contexto suficiente para agir sem fingir conhecimento que ninguém recebeu. ").append(actor).append(" e os demais estão dentro de ").append(location).append(", cercados por uma rotina que existia antes da chegada e por possibilidades que ainda não foram escolhidas. O próximo movimento pertence à companhia. Qualquer resposta do cenário deverá partir do que ela realmente fizer, mantendo pessoas, posições, memória e consequências coerentes com esta primeira visão do mundo.");
        return out.toString();
    }

    private static boolean isOpeningDirective(String action){String n=action==null?"":action.toUpperCase(new Locale("pt","BR"));return n.contains("INÍCIO AUTOMÁTICO")||n.contains("INICIO AUTOMATICO")||n.contains("PONTO INICIAL CATALOGADO DESTA AVENTURA");}
    private static String playerFacingAction(String action){
        String value=clean(action,1400);if(value.length()==0)return "";
        if(isOpeningDirective(value))return "";
        int marker=value.indexOf("MECANICA_LOCAL_VERIFICADA");if(marker>=0)value=value.substring(0,marker).trim();
        value=value.replaceAll("(?i)BRAS_[A-Z0-9_]+","").replaceAll("\\s+"," ").trim();
        if(value.length()>320)value=value.substring(0,320).trim()+"…";
        return value;
    }
    private static String extractBetween(String source,String start,String end){
        if(source==null)return "";String low=source.toLowerCase(new Locale("pt","BR")),a=start.toLowerCase(new Locale("pt","BR")),b=end.toLowerCase(new Locale("pt","BR"));int i=low.indexOf(a);if(i<0)return "";i+=a.length();int j=low.indexOf(b,i);if(j<0)j=source.length();return clean(source.substring(i,j).replaceAll("^[ :.-]+|[ .;-]+$",""),700);
    }
    private static String naturalizeCatalog(String catalog,boolean localWater){
        String c=clean(catalog,520);if(c.length()==0)return "";c=c.replaceFirst("^[^:]{0,80}:\\s*","");
        if(!localWater){StringBuilder keep=new StringBuilder();for(String sentence:c.split("(?<=[.!?])\\s+")){String n=sentence.toLowerCase(new Locale("pt","BR"));if(n.matches(".*\\b(rio|rios|riacho|riachos|c[oó]rrego|c[oó]rregos|canal|canais|margem|margens|costa|lago|lagos|mar|mares|delta|estu[aá]rio|porto|portos|doca|docas|navega[cç][aã]o|chionthar)\\b.*"))continue;if(keep.length()>0)keep.append(' ');keep.append(sentence.trim());}c=clean(keep.toString(),520);}
        if(c.length()==0)return "";c=c.replaceAll("(?i)^cidade de ","É uma cidade de ");if(!c.matches(".*[.!?…]$"))c+=".";return c;
    }
    private static String weatherSentence(String weather){String w=clean(weather,70);if(w.length()==0)return "";return "O tempo acompanha a chegada: "+lowerFirst(w)+".";}

    private JSONObject manualAnswer(String mode,String query,List<JSONObject> hits){/*BR_JSON_WRAP*/try{
        if(hits==null||hits.isEmpty())return emptyEnvelopeWithNarration("Não há trecho local verificável para responder a esse pedido. Nenhuma regra foi inventada.");
        JSONObject out=emptyEnvelope();JSONArray sources=new JSONArray();StringBuilder narration=new StringBuilder();
        narration.append("Consulta local aos manuais — ").append(clean(query,180)).append("\n\n");
        int limit=Math.min(4,hits.size());
        for(int i=0;i<limit;i++){
            JSONObject hit=hits.get(i);String book=clean(hit.optString("title",hit.optString("manual_title","Manual")),100);int page=hit.optInt("page",hit.optInt("page_number",0));String excerpt=clean(hit.optString("text",hit.optString("excerpt",hit.optString("content",""))),650);
            if("Sortear dos livros".equals(mode)&&i>0)break;
            narration.append(book);if(page>0)narration.append(", p. ").append(page);narration.append(": ").append(excerpt.length()>0?excerpt:"trecho indexado sem prévia textual").append("\n\n");
            try{sources.put(new JSONObject().put("book",book).put("page",page).put("excerpt",excerpt));}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}
        }
        narration.append("A resposta acima reproduz somente o material encontrado no índice do aparelho. Interpretação que não esteja explícita deve ser decidida pelo jogador ou pelo Mestre humano.");
        try{out.put("narration",narration.toString().trim()).put("sources",sources).put("rules_verified",sources.length()>0).put("_offline_master",new JSONObject().put("enabled",true).put("manual_lookup",true));}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray options(String location,String npc,String danger,String objective,String action)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        if(npc.length()>0)out.put(option("FALAR COM "+npc.toUpperCase(Locale.ROOT),"Falo com "+npc+" sobre o que está acontecendo agora."));
        if(danger.length()>0&&out.length()<2)out.put(option("REAGIR A "+clean(danger,60).toUpperCase(Locale.ROOT),"Mudo minha posição ou preparo uma resposta ao perigo já visível."));
        if(objective.length()>0&&out.length()<2)out.put(option("AVANÇAR NO OBJETIVO","Dou um passo concreto em direção a "+objective+"."));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject option(String label,String action)throws Exception{/*BR_JSON_WRAP*/try{return new JSONObject().put("label",label).put("description",action).put("action",action);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject emptyEnvelope(){/*BR_JSON_WRAP*/try{try{return new JSONObject().put("protocol_version",MasterProtocolV1.VERSION).put("narration","").put("visual_prompt","").put("options",new JSONArray()).put("pending_test",JSONObject.NULL).put("state_updates",new JSONArray()).put("combat",new JSONObject().put("action","none").put("enemies",new JSONArray()).put("updates",new JSONArray())).put("scene_state",new JSONObject()).put("npc_updates",new JSONArray()).put("npc_cognition_updates",new JSONArray()).put("knowledge_updates",new JSONArray()).put("quest_updates",new JSONArray()).put("faction_updates",new JSONArray()).put("guild_updates",new JSONArray()).put("clock_updates",new JSONArray()).put("world_events",new JSONArray()).put("event_resolutions",new JSONArray()).put("pending_resolutions",new JSONArray()).put("map_updates",new JSONArray()).put("tactical_updates",new JSONArray()).put("journal_updates",new JSONArray()).put("narrative_state",new JSONObject()).put("audio_plan",new JSONObject()).put("sources",new JSONArray()).put("rules_verified",false);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject emptyEnvelopeWithNarration(String text){/*BR_JSON_WRAP*/try{JSONObject out=emptyEnvelope();try{out.put("narration",text);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}
    private static JSONObject firstObject(JSONObject a,JSONObject b){return a!=null?a:(b!=null?b:new JSONObject());}
    private static void copy(JSONObject from,JSONObject to,String... keys)throws Exception{/*BR_JSON_WRAP*/try{for(String key:keys)if(from.has(key))to.put(key,from.opt(key));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String resultMarker(String action){String n=clean(action,900).toLowerCase(new Locale("pt","BR"));if(!n.contains("mecanica_local_verificada"))return "";if(n.contains("resultado=sucesso")||n.contains("resultado: sucesso"))return "sucesso";if(n.contains("resultado=falha")||n.contains("resultado: falha"))return "falha";return "resultado registrado";}
    private static String lowerFirst(String value){if(value==null||value.length()==0)return "";return Character.toLowerCase(value.charAt(0))+value.substring(1);}
    private static String clean(String value,int max){String out=value==null?"":value.replace('\n',' ').replace('\r',' ').replaceAll("\\s+"," ").trim();if(out.length()<=max)return out;return out.substring(0,Math.max(1,max-1)).trim()+"…";}
}
