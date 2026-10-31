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
        else if(intent==BraseiroNarrativeDirector.Intent.SPATIAL_CLARIFICATION){text.append("Os arredores imediatos continuam sendo ").append(clean(scene.optString("location","esta região"),100)).append(". A posição relativa, as passagens visíveis e a disposição dos presentes permanecem as mesmas de antes.");}
        else if(intent==BraseiroNarrativeDirector.Intent.PLANNING){text.append("A ideia permanece considerada em pensamento, sem execução imediata. O ambiente e os presentes continuam exatamente no mesmo instante.");}
        else{text.append("Diante do que pode ser observado no momento, os detalhes visíveis confirmam o que já foi percebido na cena, sem novos acontecimentos por enquanto.");}
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
            text.append("O caminho à frente se abre para as escolhas da companhia.");
        }else{
            text.append("Em ").append(location).append(", ");
            if(weather.length()>0)text.append("sob o tempo ").append(lowerFirst(weather)).append(", ");
            if(declared.length()>0)text.append(lowerFirst(declared)).append(". ");
            else text.append(actor).append(" observa por um instante antes de escolher o próximo gesto. ");
            if(hero0!=null&&declared.length()>0&&heroEquip.length()>0){
                text.append(actor).append(" firma ").append(lowerFirst(heroEquip)).append(" enquanto o vento move a barra das roupas. ");
            }
            if(npc.length()>0)text.append(npc).append(" está em atividade no local, percebe o movimento e reage com naturalidade. ");
            if(verified.length()>0){
                if("sucesso".equals(verified))text.append("A tentativa funciona, e a situação se abre a partir desse resultado. ");
                else if("falha".equals(verified))text.append("A tentativa não alcança o efeito pretendido, e a situação responde a essa ocorrência. ");
                else text.append("O resultado obtido passa a definir o que acontece a seguir. ");
            }else if(danger.length()>0)text.append("A ameaça continua presente: ").append(lowerFirst(danger)).append(". ");
            if(previous.length()>0)text.append("A atenção se volta para os detalhes imediatos ao redor. ");
            if(objective.length()>0)text.append("Adiante, a busca por ").append(lowerFirst(objective)).append(" continua no horizonte. ");
            text.append("Os arredores apresentam oportunidades para observar, dialogar com os presentes ou agir diretamente.");
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
        if(plan!=null&&plan.compactContext!=null){
            try{
                JSONObject root=new JSONObject(plan.compactContext);
                JSONObject bus=root.optJSONObject("game_bus");
                if(bus!=null){
                    JSONArray heroes=bus.optJSONArray("active_heroes_full");
                    if(heroes!=null&&heroes.length()>0&&heroes.optJSONObject(0)!=null){
                        String heroName=heroes.optJSONObject(0).optString("name","").trim();
                        if(heroName.length()>0)actor=heroName;
                    }
                }
            }catch(Exception ignored){}
        }
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
            out.append("A aproximação de ").append(place).append(" começa pelo contorno da paisagem. ").append(region.length()>0?place+" ergue-se como marco dentro de "+region+", onde ":place+" surge na distância, onde ").append(geo).append(" dita o ritmo dos passos, a inclinação das encostas e a linha do horizonte. A estrada e as primeiras trilhas revelam a transição entre o terreno aberto e as primeiras construções, oferecendo uma visão clara das distâncias e das rotas de entrada.\n\n");
            out.append("Ao alcançar o limiar de ").append(place).append(", a circulação cotidiana torna-se perceptível. Moradores cuidam de seus afazeres, carroças avançam com rangidos de rodas sobre o piso e o comércio local movimenta portas e balcões. ").append(npc.length()>0?npc+" encontra-se entre as figuras visíveis, ocupada com suas próprias tarefas.":"Viajantes e locais compartilham os acessos principais, ocupando o espaço com naturalidade.").append("\n\n");
            out.append("O ambiente acompanha essa recepção. ").append(weather.length()>0?"Sob o tempo "+lowerFirst(climate)+", a luz e o vento tocam a fachada dos edifícios e o chão sob as botas.":"A atmosfera ao redor transmite a temperatura e os sons característicos deste trecho da jornada.").append(" A arquitetura ao redor reflete o uso do espaço, com passagens transitadas, praças de parada e caminhos que seguem adiante entre áreas públicas e de trabalho.\n\n");
            out.append("A atividade e o movimento local mostram como a comunidade se organiza, com pontos de encontro, oficinas e locais de descanso. ").append(danger.length()>0?"Há uma tensão perceptível na cena: "+lowerFirst(danger)+". Ela se manifesta na cautela de certos olhares e na postura dos presentes. ":"O ritmo segue com a normalidade esperada para este horário. ").append(objective.length()>0?"O objetivo que trouxe a companhia continua sendo "+lowerFirst(objective)+", agora situado dentro do espaço observável.":"").append("\n\n");
            out.append("Com o limiar atravessado, ").append(actor).append(" e os demais podem avaliar o que está diante dos olhos. As vias abrem-se em direções claras: aproximar-se da praça, dirigir a palavra a quem estiver por perto ou avançar em direção aos bairros centrais.");
        }else if("poi_discovery".equals(occasion)){
            String landmark=poi.length()>0?poi:place;
            out.append("O marco de ").append(landmark).append(" destaca-se da paisagem ao redor de ").append(place).append(". Seus contornos erguem-se contra o relevo de ").append(geo).append(", com passagens e acessos claramente delimitados pela alvenaria, pelas cercas ou pelas pedras gastas pelo uso.\n\n");
            out.append("Diante da entrada, o movimento de pessoas revela a utilidade do local. Portas, janelas, balcões e áreas de carga mostram o fluxo habitual. ").append(npc.length()>0?npc+" permanece por perto, atenta ao que se desenrola nas imediações. ":"Quem passa por ali demonstra familiaridade com a rota. ").append(weather.length()>0?"O clima atual — "+lowerFirst(climate)+" — confere textura às superfícies e projeta sombras sobre os acessos visíveis.\n\n":"A iluminação natural define os detalhes da fachada e das aberturas.\n\n");
            out.append(objective.length()>0?"O propósito em torno de "+lowerFirst(objective)+" encontra neste ponto novas referências visíveis. ":"As passagens observáveis convidam à exploração. ").append("Resta decidir se o melhor caminho é aproximar-se para examinar de perto, sondar quem estiver ao redor ou manter distância prudente.");
        }else if("geography_discovery".equals(occasion)){
            out.append("O terreno muda à medida que a marcha avança por ").append(geo).append(". A elevação do solo, as curvas da encosta e a densidade da vegetação impõem um novo ritmo aos passos, delimitando com clareza o que está ao alcance da vista e o que se estende pelo horizonte.\n\n");
            out.append("O chão sob as botas revela a consistência da terra, das pedras e da trilha percorrida. O relevo desenha desvios naturais, zonas de sombra e pontos de visibilidade aberta. ").append(weather.length()>0?"Com "+lowerFirst(climate)+", a marcha exige atenção ao equilíbrio e ao terreno à frente.":"A visibilidade permite avaliar desvios, áreas de abrigo e a continuidade da passagem.").append("\n\n");
            out.append(actor).append(" e os companheiros conseguem medir as opções da marcha: seguir pela trilha principal mais exposta, contornar pela elevação ou buscar um ponto elevado para estudar a extensão do caminho adiante.");
        }else if("weather_transition".equals(occasion)){
            out.append("O ar transforma-se rapidamente sobre ").append(place).append(". As nuvens fecham-se no alto e ").append(lowerFirst(climate)).append(" altera de imediato o som, a sensação na pele e a visibilidade dos contornos mais distantes.\n\n");
            out.append("Pelo caminho, poeira assenta ou a umidade ganha força sobre a terra batida e a pedra. ").append(npc.length()>0?npc+" recolhe pertences ou ajusta o capuz contra a virada do tempo. ":"Moradores e transeuntes apressam o passo em busca de beirais e abrigos. ").append("A mudança traz novas condições físicas para a marcha, exigindo atenção às pegadas, ao atrito do piso e à escolha entre seguir adiante ou buscar abrigo imediato.");
        }else{ // changed_return
            out.append("A aproximação de ").append(place).append(" devolve contornos familiares aos olhos da companhia. As silhuetas de ").append(geo).append(" e as construções conhecidas reencontram o horizonte, permitindo reconhecer a geometria do lugar.\n\n");
            out.append("No entanto, pequenos sinais revelam o tempo transcorrido: a disposição de barracas, novas marcas no piso ou o vaivém de quem agora ocupa os portões. ").append(npc.length()>0?npc+" segue presente no cenário, envolvida em nova atividade. ":"As movimentações mostram a rotina em andamento nas praças e oficinas. ").append(weather.length()>0?"O clima — "+lowerFirst(climate)+" — envolve as ruas em uma atmosfera diferente da última passagem.\n\n":"\n\n");
            out.append(objective.length()>0?"A atenção continua voltada para "+lowerFirst(objective)+", agora sob a perspectiva do que mudou desde a última visita. ":"Diante das ruas e acessos que se abrem, ").append(danger.length()>0?"A tensão com "+lowerFirst(danger)+" pode ser percebida na postura de quem observa a entrada. ":"").append("O momento permite retomar contatos conhecidos, buscar abrigo ou investigar os novos movimentos observados.");
        }
        return out.toString();
    }

    private static String ensurePresentationMinimum(String text,String occasion,String location,String geography,String weather,String objective){
        String out=text==null?"":text.trim();
        int words=WorldOnboardingNarrativeGuard.countWords(out);
        int paras=WorldOnboardingNarrativeGuard.countParagraphs(out);
        int minWords=WorldOnboardingNarrativeGuard.minWords(occasion);
        int minParas=WorldOnboardingNarrativeGuard.minParagraphs(occasion);
        if(words>=minWords&&paras>=minParas)return out;
        String place=location.length()>0?location:"esta região";
        String geo=geography.length()>0?geography:"o relevo circundante";
        String climate=weather.length()>0?lowerFirst(weather):"o clima característico";
        String extra="O ar em "+place+" preserva os sons e as texturas do entorno, onde "+geo+" emoldura o horizonte sob "+climate+". Os detalhes observáveis abrem espaço para os próximos passos, permitindo que cada ação seja guiada pelo que se apresenta diante dos olhos.";
        if(!out.contains(extra))out+=(out.length()>0?"\n\n":"")+extra;
        return out;
    }

    private static String buildOpeningNarration(String location,String category,String region,String placeFact,String weather,String danger,String objective,String actor,String npc){
        StringBuilder out=new StringBuilder();
        String scale=(category.length()>0?"Trata-se de "+lowerFirst(category):"Trata-se de um lugar habitado e percorrido")+(region.length()>0?" dentro de "+region:"")+". ";
        String climate=weather.length()>0?weatherSentence(weather)+" ":"";
        out.append("A paisagem abre-se em grande escala diante da estrada batida que conduz a ").append(location).append(". O território estende-se como parte viva e percorrida de ").append(region.length()>0?region:"Faerûn").append(", onde montanhas distantes, colinas onduladas e linhas de mata antiga desenham a transição do relevo sob a luz do céu. ").append(scale).append("A aproximação revela a extensão das terras ao redor, onde o relevo, a vegetação e o traçado das rotas marcam o avanço da jornada. ").append(placeFact.length()>0?placeFact+" ":"").append(climate).append("Ao longe, os primeiros telhados de telha cozida, muralhas de cantaria e colunas de fumaça subindo das chaminés destacam-se contra o horizonte, anunciando a chegada a um destino vivo, denso e em pleno movimento.\n\n");
        out.append("À medida que a aproximação se consolida e a poeira da estrada assenta sobre as pedras do calçamento, os sons do trânsito local começam a sobrepor-se ao silêncio da marcha. Carroças pesadas de mercadorias deixam sulcos na terra batida com o rangido ritmado de rodas e arreios de couro, montarias cruzam os acostamentos entre saudações breves e trabalhadores cuidam da manutenção de cercas, pontes de madeira e pórticos de acesso. A arquitetura ao redor ganha contornos definidos e ricos em história: vigas de carvalho escurecidas pelo tempo e pela chuva, alvenaria firme assentada sobre fundações de pedra sólida, e portas de tavernas, oficinas e entrepostos abertas para a luz do dia. O ar carrega o cheiro de palha seca, fogueiras de bétula e ferro forjado, envolvendo o ambiente em atividade ininterrupta.\n\n");
        out.append("Entre os que alcançam o limiar das primeiras edificações, ").append(actor).append(" avança com passos firmes e olhar atento, sentindo o peso familiar do equipamento nas costas, o atrito das correias da armadura e a fina camada de poeira da viagem assentada sobre o couro das botas. O olhar percorre os primeiros pontos de confluência da praça e dos acessos principais: vielas estreitas que contornam armazéns com sacarias de grãos, arcos que conduzem aos mercados abertos e balcões de madeira onde viajantes consultam rotas e trocam informações. ");
        if(npc.length()>0)out.append(npc).append(" destaca-se entre as presenças visíveis, acompanhando a movimentação de cargas e dialogando com viajantes que cruzam a entrada. ");
        else out.append("Diversos transeuntes movimentam a entrada: negociantes trocam saudações, viajantes ajustam suas bagagens e guardas observam com atenção a circulação pelas portas principais. ");
        out.append("A atmosfera do lugar reflete a convivência cotidiana entre quem habita a região e quem acabou de chegar.\n\n");
        if(danger.length()>0)out.append("Há, contudo, sinais de cautela perceptíveis na conduta de quem transita pelas imediações: ").append(lowerFirst(danger)).append(". Essa presença transparece em conversas interrompidas sob os beirais, olhares enviesados dirigidos a estranhos recém-chegados e no cuidado redobrado com trincos, grades e animais de carga. ");
        else out.append("O ritmo das ruas mantém a normalidade esperada para este horário, com mercadores apregoando produtos, artesãos trabalhando em suas bancadas e moradores cuidando de suas rotinas com serenidade. ");
        if(objective.length()>0)out.append("O propósito que impulsiona esta jornada permanece firme e bem delimitado: ").append(lowerFirst(objective)).append(". Ele oferece um rumo tangível em meio à multiplicidade de vias, becos e construções que se desdobram adiante.\n\n");
        else out.append("A primeira impressão convida a conhecer os arredores, observar o movimento das ruas e identificar os pontos de interesse que despontam no centro da localidade.\n\n");
        out.append("Com os passos estabelecidos dentro de ").append(location).append(", o cenário abre-se de forma concreta diante de ").append(actor).append(" e de seus companheiros. As escolhas imediatas apresentam-se com total clareza no ambiente: aproximar-se para iniciar um diálogo com quem conhece as notícias locais, examinar a arquitetura e as rotas que partem dos portões ou avançar decididamente em direção às praças centrais e bairros de comércio para cumprir os planos da jornada.");
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
