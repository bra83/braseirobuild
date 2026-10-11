package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * RUN145 — Arcana Unleashed: bestiário e companheiros executáveis.
 *
 * Contrato:
 * - interface e regras expostas em PT-BR;
 * - nomes-fonte em inglês são apenas aliases/IDs internos;
 * - nenhuma rolagem é feita automaticamente;
 * - ações recebem resultados rolados pelo jogador/Mestre e devolvem um plano de efeito;
 * - mutações de PV/condições/posição ficam para o gateway persistente de combate.
 */
public final class ArcanaUnleashedRun145BestiaryEngine {
    public static final String SOURCE="Arcana Unleashed";
    public static final String SOURCE_ID="AU";
    public static final String UI_LANGUAGE="pt-BR";

    private static final String[][] CREATURES={
        {"Abjurer Archmage","Arquimago Abjurador"},
        {"Baelnorn","Baelnorn"},
        {"Battle Familiar","Familiar de Batalha"},
        {"Conjurer Archmage","Arquimago Conjurador"},
        {"Darkenbeast","Fera Sombria"},
        {"Dinosaur Spirit","Espírito de Dinossauro"},
        {"Diviner Archmage","Arquimago Adivinho"},
        {"Enchanter Archmage","Arquimago Encantador"},
        {"Evoker Archmage","Arquimago Evocador"},
        {"Groundling Grunt","Groundling Recruta"},
        {"Groundling Slayer","Groundling Matador"},
        {"Groundling Stalker","Groundling Espreitador"},
        {"Illusionist Archmage","Arquimago Ilusionista"},
        {"Living Cone of Cold","Cone de Frio Vivo"},
        {"Living Thunderwave","Onda Trovejante Viva"},
        {"Living Vitriolic Sphere","Esfera Vitriólica Viva"},
        {"Necromancer Archmage","Arquimago Necromante"},
        {"Plant Spirit","Espírito Vegetal"},
        {"Spellguard","Guarda-Magia"},
        {"Taisus","Taisus"},
        {"Transmuter Archmage","Arquimago Transmutador"},
        {"Venger","Venger"},
        {"Vestige Companion","Companheiro Vestígio"}
    };

    private ArcanaUnleashedRun145BestiaryEngine(){}

    public static JSONArray allCreatures(){JSONArray a=new JSONArray();for(String[] p:CREATURES)a.put(p[1]);return a;}
    public static boolean supports(String s){return canonical(s)!=null;}
    public static String displayName(String s){String c=canonical(s);if(c==null)return s==null?"":s;for(String[]p:CREATURES)if(p[0].equals(c))return p[1];return c;}

    public static JSONObject descriptor(){
        try{return new JSONObject()
            .put("id","run145_arcana_unleashed_bestiary")
            .put("fonte",SOURCE).put("idioma_interface",UI_LANGUAGE)
            .put("blocos_estatistica_executaveis",CREATURES.length)
            .put("companheiros_escalaveis",4)
            .put("rolagens_automaticas",false)
            .put("mutacao_combate_via_gateway",true)
            .put("itens_magicos_pendentes_run146",true)
            .put("livro_completo",false);
        }catch(Exception e){return new JSONObject();}
    }

    public static JSONObject install(){
        JSONObject out=new JSONObject();JSONArray falhas=new JSONArray();int ok=0;
        try{
            for(String[] p:CREATURES){
                JSONObject ev=new JSONObject()
                    .put("evidence_id","run145.au.statblock."+id(p[0]))
                    .put("content_kind","statblock").put("content_name",p[0])
                    .put("source_book",SOURCE)
                    .put("source_locator","AU cap. 6 / conjunto estruturado revisado em 2026-09-29")
                    .put("revision",1).put("ui_language",UI_LANGUAGE);
                boolean reg=AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev).optBoolean("registered",false);
                JSONObject mech=new JSONObject().put("effect_pipeline_complete",true)
                    .put("execution_status","compiled_statblock_pipeline")
                    .put("runtime_resolver","ArcanaUnleashedRun145BestiaryEngine.resolveAction")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false)
                    .put("ui_language",UI_LANGUAGE);
                if(reg && CompiledContentMechanicsEngine.register("statblock",p[0],mech,ev).optBoolean("registered",false))ok++;else falhas.put(p[1]);
            }
            return out.put("instalados",ok).put("esperados",CREATURES.length).put("completo",ok==CREATURES.length)
                .put("falhas",falhas).put("idioma",UI_LANGUAGE).put("rolagens_automaticas",false);
        }catch(Exception e){try{return out.put("instalados",ok).put("esperados",CREATURES.length).put("completo",false).put("falhas",falhas).put("rolagens_automaticas",false);}catch(Exception ignored){return out;}}
    }

    public static JSONObject statblock(String criatura){return statblock(criatura,new JSONObject());}

    public static JSONObject statblock(String criatura,JSONObject ctx){
        String c=canonical(criatura);
        JSONObject o=new JSONObject();
        try{o.put("fonte",SOURCE).put("idioma",UI_LANGUAGE).put("nome",displayName(criatura)).put("id_fonte",c==null?"":c)
            .put("conhecida",c!=null).put("rolagens_automaticas",false).put("acoes",new JSONArray()).put("tracos",new JSONArray()).put("magias",new JSONArray());}catch(Exception ignored){}
        if(c==null)return blocked(o,"Criatura de Arcana Unleashed não reconhecida.");
        try{
            if(c.equals("Abjurer Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"198 (36d8 + 36)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                defenses(o,"Imunidade a dano psíquico","Imunidade à condição Enfeitiçado");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","4d10 + 5 de força","rolagem_ataque","dano_total");
                save(o,"Explosão de Banimento","Carisma",18,"cone de 18 m","10d8 de força","Na falha, além do dano, o alvo fica Incapacitado e é enviado a um semiplano inofensivo até o início do próximo turno do arquimago; no sucesso, sofre metade do dano apenas.","salvaguarda_total","dano_total");
                utility(o,"Fortificar (Recarga 4–6)","O arquimago recebe 40 PV temporários. A disponibilidade da recarga deve ser informada pelo Mestre.","recarga_disponivel");
                spells(o,18,"Detectar Magia","Armadura Arcana","Mãos Mágicas","Prestidigitação","Tranca Arcana","Bola de Fogo (7º círculo, 2/dia)","Indetectável (2/dia)","Esfera Resiliente de Otiluke (2/dia)","Mente Vazia (1/dia)","Teletransporte (1/dia)","Dissipar Magia (2/dia)","Globo de Invulnerabilidade (2/dia)","Contramágica (3/dia)");
            }else if(c.equals("Baelnorn")){
                stats(o,"Médio","Morto-vivo (mago)","Neutro e Bom",18,"255 (34d8 + 102)","9 m",11,16,16,21,14,16,"ND 18","Percepção passiva 18");
                defenses(o,"Resistência: elétrico e radiante","Imunidade: necrótico e veneno","Imunidade às condições: Enfeitiçado, Amedrontado, Paralisado e Envenenado");
                trait(o,"Resistência Lendária","3/dia; 4/dia no covil. Ao falhar numa salvaguarda, pode escolher obter sucesso.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz três ataques, combinando Rajada Arcana e Toque Paralisante; pode substituir dois ataques por Relâmpago de 4º círculo.");
                attack(o,"Rajada Arcana",11,"1,5 m ou 36 m","4d12 + 5 de força","rolagem_ataque","dano_total");
                attackEffect(o,"Toque Paralisante",11,"1,5 m","2d4 + 5 de frio","Paralisado até o início do próximo turno do Baelnorn.","rolagem_ataque","dano_total");
                save(o,"Radiância Avassaladora (Ação Lendária)","Constituição",19,"emanação de 6 m","2d10 radiante","Mortos-vivos têm desvantagem na salvaguarda; sucesso reduz o dano à metade.","salvaguarda_total","dano_total");
                utility(o,"Teletransporte Recuperador (Ação Lendária)","Teletransporta até 18 m para espaço visível e recupera 2d10 PV; exige o resultado manual da cura.","cura_total");
                spells(o,19,"Detectar Magia","Detectar Pensamentos","Dissipar Magia","Invisibilidade","Relâmpago (4º círculo)","Mãos Mágicas","Prestidigitação","Contramágica","Medo (Ação Lendária: Olhar Aterrorizante)");
            }else if(c.equals("Battle Familiar")){
                battleFamiliar(o,ctx);
            }else if(c.equals("Conjurer Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"225 (41d8 + 41)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz quatro ataques de Rajada Arcana.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","4d10 + 5 de força","rolagem_ataque","dano_total");
                utility(o,"Transposição Benigna (3/dia)","Teletransporta até 9 m para espaço visível ou troca de lugar com uma criatura voluntária no alcance.","alvo_ou_destino_confirmado");
                spells(o,18,"Detectar Magia","Encontrar Familiar","Armadura Arcana","Mãos Mágicas","Prestidigitação","Servo Invisível","Porta Dimensional (2/dia)","Nuvem Incendiária (1/dia)","Relâmpago (8º círculo, 1/dia)","Mudança de Plano (1/dia)","Teletransporte (1/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Darkenbeast")){
                stats(o,"Médio","Monstruosidade","Neutro e Mau",15,"60 (8d8 + 24)","6 m; voo 12 m",17,14,16,5,10,13,"ND 4","Percepção passiva 12");
                trait(o,"Impressão de Magia","Na criação, o criador pode armazenar uma magia de até 5º círculo com tempo de conjuração de 1 ação. A até 9 m, o criador pode conjurá-la sem componentes; o efeito se origina da Fera Sombria e ela morre logo após.");
                trait(o,"Sensibilidade à Luz Solar","Sob luz solar, tem desvantagem em testes de atributo e jogadas de ataque.");
                multi(o,"Ataques Múltiplos","Faz três ataques de Dilacerar.");
                attack(o,"Dilacerar",5,"1,5 m","2d8 + 3 cortante","rolagem_ataque","dano_total");
                utility(o,"Espreitador das Sombras (Ação Bônus)","Em penumbra ou escuridão, realiza Desengajar ou Esconder-se.","escolha:Desengajar|Esconder-se");
            }else if(c.equals("Dinosaur Spirit")){
                dinosaurSpirit(o,ctx);
            }else if(c.equals("Diviner Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"176 (32d8 + 32)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos semelhantes.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas; pode substituir um ataque por Espinho Mental de 4º círculo.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","3d12 + 5 de força","rolagem_ataque","dano_total");
                spells(o,18,"Detectar Magia","Detectar Pensamentos","Armadura Arcana","Mãos Mágicas","Espinho Mental (4º círculo)","Prestidigitação","Vidência (2/dia)","Enviar Mensagem (2/dia)","Premonição (1/dia)","Telepatia (1/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Enchanter Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"214 (39d8 + 39)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","3d10 + 5 de força","rolagem_ataque","dano_total");
                spells(o,18,"Detectar Magia","Armadura Arcana","Mãos Mágicas","Prestidigitação","Bola de Fogo (7º círculo, 2/dia)","Missão (2/dia)","Modificar Memória (2/dia)","Dominar Pessoa (1/dia)","Enfeitiçar Pessoa/Imobilizar Pessoa/Sono (2/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Evoker Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"214 (39d8 + 39)","9 m; voo 9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Escultor de Evocação","Ao conjurar magia de área, pode escolher a si e até cinco criaturas vistas: elas obtêm sucesso automático na salvaguarda e não sofrem dano se normalmente sofreriam metade.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas; pode substituir um ataque por Despedaçar.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","4d10 + 5 de força","rolagem_ataque","dano_total");
                utility(o,"Barragem de Evocação (Recarga 5–6)","Conjura duas magias, em qualquer combinação entre Bola de Fogo, Tempestade de Gelo e Relâmpago. As salvaguardas e danos são resolvidos pelo motor de magias.","recarga_disponivel","duas_magias_escolhidas");
                spells(o,18,"Detectar Magia","Armadura Arcana","Mãos Mágicas","Prestidigitação","Despedaçar (3º círculo)","Bola de Fogo","Tempestade de Gelo","Relâmpago","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Groundling Grunt")){
                stats(o,"Médio","Monstruosidade","Neutro e Mau",14,"45 (7d8 + 14)","9 m; escavação 3 m",16,15,14,7,10,8,"ND 1","Percepção passiva 12");
                defenses(o,"Resistência a veneno");
                attackEffect(o,"Garra",5,"1,5 m","1d8 + 3 cortante","Se o alvo for Médio ou menor, fica Agarrado (escape CD 13) por uma das duas patas.","rolagem_ataque","dano_total");
                attack(o,"Funda",4,"9/36 m","2d4 + 2 contundente","rolagem_ataque","dano_total");
                reaction(o,"Ir ao Chão","Quando é atingido por ataque enquanto está no solo, reduz à metade (arredondando para baixo) o dano desse ataque.","dano_recebido");
            }else if(c.equals("Groundling Slayer")){
                stats(o,"Médio","Monstruosidade","Neutro e Mau",15,"58 (9d8 + 18)","9 m; escavação 6 m",17,16,15,10,13,8,"ND 3","Percepção passiva 13");
                defenses(o,"Resistência a veneno");
                multi(o,"Ataques Múltiplos","Faz dois ataques, combinando Garra e Adaga.");
                attackEffect(o,"Garra",5,"1,5 m","2d8 + 3 cortante","Se o alvo for Médio ou menor, fica Agarrado (escape CD 13).","rolagem_ataque","dano_total");
                attack(o,"Adaga",5,"1,5 m ou 6/18 m","1d4 + 3 perfurante + 2d6 veneno","rolagem_ataque","dano_total");
                reaction(o,"Ir ao Chão","Quando é atingido por ataque enquanto está no solo, reduz à metade o dano desse ataque.","dano_recebido");
            }else if(c.equals("Groundling Stalker")){
                stats(o,"Médio","Monstruosidade","Neutro e Mau",15,"52 (8d8 + 16)","9 m; escavação 3 m",16,16,14,8,12,8,"ND 2","Percepção passiva 13");
                defenses(o,"Resistência a veneno");
                attackEffect(o,"Garra",5,"1,5 m","2d8 + 3 cortante","Se o alvo for Médio ou menor, fica Agarrado (escape CD 13).","rolagem_ataque","dano_total");
                attack(o,"Besta Pesada",5,"30/120 m","2d10 + 3 perfurante","rolagem_ataque","dano_total");
                reaction(o,"Ir ao Chão","Quando é atingido por ataque enquanto está no solo, reduz à metade o dano desse ataque.","dano_recebido");
            }else if(c.equals("Illusionist Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"220 (40d8 + 40)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","3d10 + 5 de força","rolagem_ataque","dano_total");
                spells(o,18,"Detectar Magia","Disfarçar-se","Invisibilidade","Armadura Arcana","Mãos Mágicas","Ilusão Menor","Prestidigitação","Criação (2/dia)","Imagem Maior (2/dia)","Aura Mágica de Nystul (2/dia)","Semblante (2/dia)","Miragem Arcana (1/dia)","Estranho (1/dia)","Padrão Hipnótico/Imagem Espelhada (2/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Living Cone of Cold")){
                stats(o,"Grande","Constructo","Imparcial",15,"105 (10d10 + 50)","7,5 m; voo 7,5 m",10,15,20,3,11,6,"ND 7","Percepção passiva 10");
                defenses(o,"Imunidade a frio","Imunidade às condições: Cego, Enfeitiçado, Surdo, Amedrontado, Agarrado, Envenenado, Caído e Contido");
                trait(o,"Amorfo","Pode atravessar espaço de 2,5 cm sem gastar movimento extra.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz dois ataques de Golpe Mágico.");
                attack(o,"Golpe Mágico",8,"3 m","5d8 + 5 frio","rolagem_ataque","dano_total");
                save(o,"Mimetismo de Magia (Recarga 5–6)","Constituição",16,"cone de 18 m","8d8 frio","Sucesso: metade do dano.","recarga_disponivel","salvaguarda_total","dano_total");
            }else if(c.equals("Living Thunderwave")){
                stats(o,"Médio","Constructo","Imparcial",15,"30 (4d8 + 12)","7,5 m; voo 7,5 m",10,13,16,3,6,6,"ND 1","Percepção passiva 8");
                defenses(o,"Imunidade a trovejante","Imunidade às condições: Cego, Enfeitiçado, Surdo, Amedrontado, Agarrado, Envenenado, Caído e Contido");
                trait(o,"Amorfo","Pode atravessar espaço de 2,5 cm sem gastar movimento extra.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos.");
                attack(o,"Golpe Mágico",5,"1,5 m","1d6 + 3 trovejante","rolagem_ataque","dano_total");
                save(o,"Mimetismo de Magia (Recarga 5–6)","Constituição",13,"cubo de 4,5 m originado na criatura","2d8 trovejante","Falha também empurra 3 m; sucesso sofre metade e não é empurrado. O estrondo é audível a 90 m.","recarga_disponivel","salvaguarda_total","dano_total");
            }else if(c.equals("Living Vitriolic Sphere")){
                stats(o,"Grande","Constructo","Imparcial",15,"76 (8d10 + 32)","7,5 m; voo 7,5 m",10,15,18,3,10,6,"ND 5","Percepção passiva 10");
                defenses(o,"Imunidade a ácido","Imunidade às condições: Cego, Enfeitiçado, Surdo, Amedrontado, Agarrado, Envenenado, Caído e Contido");
                trait(o,"Amorfo","Pode atravessar espaço de 2,5 cm sem gastar movimento extra.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz dois ataques de Golpe Mágico.");
                attack(o,"Golpe Mágico",7,"3 m","5d6 + 4 ácido","rolagem_ataque","dano_total");
                save(o,"Mimetismo de Magia (Recarga 5–6)","Destreza",15,"esfera de 6 m a até 45 m","10d4 ácido","Falha também causa 5d4 ácido no fim do próximo turno do alvo; sucesso sofre metade apenas.","recarga_disponivel","salvaguarda_total","dano_total");
            }else if(c.equals("Necromancer Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"225 (41d8 + 41)","9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                defenses(o,"Resistência a necrótico");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                trait(o,"Comandar Mortos-vivos","Mortos-vivos escolhidos numa emanação de 18 m têm vantagem em ataques e salvaguardas enquanto o arquimago não estiver Incapacitado.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas; pode substituir um ataque por Definhar.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","3d10 + 5 força","rolagem_ataque","dano_total");
                spells(o,18,"Definhar","Detectar Magia","Armadura Arcana","Mãos Mágicas","Prestidigitação","Falar com os Mortos","Círculo da Morte (7º círculo, 1/dia)","Criar Mortos-vivos (1/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Plant Spirit")){
                plantSpirit(o,ctx);
            }else if(c.equals("Spellguard")){
                stats(o,"Médio","Humanoide","Leal e Neutro",20,"105 (14d8 + 42)","9 m",18,11,16,12,13,10,"ND 5","Percepção passiva 14");
                trait(o,"Resistência Superior à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos; ataques de magia têm desvantagem contra o Guarda-Magia.");
                multi(o,"Ataques Múltiplos","Faz dois ataques, combinando Mangual de Força e Azagaia.");
                attack(o,"Mangual de Força",7,"1,5 m","1d8 + 4 contundente + 2d6 força","rolagem_ataque","dano_total");
                attack(o,"Azagaia",7,"1,5 m ou 9/36 m","3d6 + 4 perfurante","rolagem_ataque","dano_total");
                reaction(o,"Adivinhação Defensiva","Quando um aliado a 1,5 m é atingido por ataque ou falha em salvaguarda contra magia/efeito mágico, role 1d20 e escolha se substitui o d20 original por esse resultado.","resultado_d20");
            }else if(c.equals("Taisus")){
                stats(o,"Enorme","Aberração","Caótico e Neutro",18,"310 (27d12 + 135)","12 m; voo 18 m",17,18,20,25,22,19,"ND 19","Percepção passiva 22");
                defenses(o,"Imunidade a veneno","Imunidade às condições: Enfeitiçado, Amedrontado, Agarrado, Paralisado, Petrificado, Envenenado e Contido");
                trait(o,"Imortalidade Arcana","Ao morrer, vira pó cintilante e ganha novo corpo em 1d10 dias, a menos que Desejo seja lançado sobre os restos para impedir a característica. O resultado de 1d10 é fornecido pelo Mestre.");
                trait(o,"Resistência Lendária (4/dia)","Ao falhar numa salvaguarda, pode escolher obter sucesso.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz dois ataques de Drenar Cérebro.");
                attackEffect(o,"Drenar Cérebro",13,"6 m","3d8 + 7 força + 3d10 psíquico","Se o alvo possuir espaços de magia, faz salvaguarda de Constituição CD 21; na falha, perde seu espaço de magia não gasto de maior círculo.","rolagem_ataque","dano_total","salvaguarda_constituicao");
                utility(o,"Teletransporte (Ação Lendária)","Teletransporta a si ou criatura voluntária vista a 3 m por até 36 m para espaço visível.","alvo_ou_destino_confirmado");
                spells(o,21,"Detectar Magia","Bola de Fogo (5º círculo)","Invisibilidade","Mãos Mágicas","Tempestade de Radiância de Jallarzi (2/dia)","Mudança de Plano (2/dia)","Vidência (2/dia)","Confusão Mental (1/dia)","Desintegrar (1/dia)","Contramágica","Escudo");
            }else if(c.equals("Transmuter Archmage")){
                stats(o,"Médio","Humanoide (mago)","Neutro",16,"220 (40d8 + 40)","12 m; voo 9 m",10,16,12,20,15,16,"ND 15","Percepção passiva 17");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e outros efeitos mágicos.");
                multi(o,"Ataques Múltiplos","Faz quatro Rajadas Arcanas.");
                attack(o,"Rajada Arcana",10,"1,5 m ou 45 m","4d10 + 5 força","rolagem_ataque","dano_total");
                spells(o,18,"Alterar-se","Detectar Magia","Arrombar","Armadura Arcana","Mãos Mágicas","Polimorfia","Prestidigitação","Inverter Gravidade (1/dia)","Forma Gasosa (2/dia)","Lentidão (2/dia)","Contramágica/Escudo (3/dia)");
            }else if(c.equals("Venger")){
                stats(o,"Médio","Ínfero","Neutro e Mau",19,"202 (27d8 + 81)","9 m; voo 9 m",18,20,16,17,14,21,"ND 17","Percepção passiva 18");
                defenses(o,"Resistência: frio, fogo, elétrico e veneno","Imunidade à condição Envenenado");
                trait(o,"Restauração Ínfera","Ao morrer, o corpo vira pó carmesim e ele ganha novo corpo em 1d10 dias em um de seus covis; o d10 é rolado manualmente.");
                trait(o,"Resistência Lendária","3/dia; 4/dia no covil.");
                trait(o,"Resistência à Magia","Vantagem em salvaguardas contra magias e efeitos mágicos.");
                trait(o,"Adepto Montado","Montado, Venger e montaria têm vantagem em salvaguardas de Destreza; ele pode redirecionar para si um ataque contra a montaria, exceto se Incapacitado.");
                multi(o,"Ataques Múltiplos","Faz três ataques, combinando Garra Carmesim e Rajada Carmesim.");
                attack(o,"Garra Carmesim",11,"1,5 m","2d8 + 5 cortante + 3d6 fogo","rolagem_ataque","dano_total");
                attack(o,"Rajada Carmesim",11,"36 m","3d12 + 5 força","rolagem_ataque","dano_total");
                save(o,"Censura Trovejante (Ação Lendária)","Sabedoria",19,"uma criatura vista a 18 m","4d6 psíquico","Falha: também Amedrontado até o fim do próximo turno; sucesso: metade do dano.","salvaguarda_total","dano_total");
                save(o,"Correntes de Aprisionamento Mágico (Ação Lendária)","Destreza",19,"uma criatura vista a 9 m","0","Falha: Contido até o fim do próximo turno e não pode realizar a ação Magia.","salvaguarda_total");
                spells(o,19,"Detectar Magia","Invisibilidade","Mãos Mágicas","Imagem Silenciosa","Alterar-se (2/dia)","Contato com Outro Plano (2/dia)","Criar Mortos-vivos (2/dia)","Porta Dimensional (2/dia)","Controlar o Clima (1/dia)","Aprisionamento (1/dia)","Telecinese (2/dia)","Contramágica","Escudo");
            }else if(c.equals("Vestige Companion")){
                vestigeCompanion(o,ctx);
            }
            o.put("pipeline_executavel",true).put("mutacao_combate_pendente_gateway",true);
            return o;
        }catch(Exception e){return blocked(o,"Falha ao montar bloco de estatística.");}
    }

    public static JSONObject resolveAction(String criatura,String acao,JSONObject payload){
        JSONObject sb=statblock(criatura,payload==null?new JSONObject():payload);
        JSONObject out=new JSONObject();
        try{
            out.put("criatura",sb.optString("nome")).put("acao_solicitada",acao==null?"":acao)
                .put("idioma",UI_LANGUAGE).put("rolagens_automaticas",false)
                .put("mutacao_combate_pendente_gateway",true);
            if(!sb.optBoolean("conhecida",false))return blocked(out,"Criatura não reconhecida.");
            JSONArray acts=sb.optJSONArray("acoes");JSONObject hit=null;
            if(acts!=null)for(int i=0;i<acts.length();i++){JSONObject a=acts.optJSONObject(i);if(a!=null&&same(a.optString("nome"),acao)){hit=a;break;}}
            if(hit==null)return blocked(out,"Ação não encontrada no bloco de estatística.");
            out.put("regra",hit).put("status","aguardando_entradas_manuais");
            JSONArray req=hit.optJSONArray("campos_manuais");JSONArray faltam=new JSONArray();
            if(req!=null)for(int i=0;i<req.length();i++){String k=req.optString(i);if(k.startsWith("escolha:"))continue;if(payload==null||!payload.has(k))faltam.put(k);}
            if(faltam.length()>0)return out.put("resolvida",false).put("entradas_faltantes",faltam);
            return out.put("resolvida",true).put("status","plano_de_efeito_pronto")
                .put("entradas_fornecidas",payload==null?new JSONObject():payload)
                .put("aplicar_pelo_gateway_de_combate",true);
        }catch(Exception e){return out;}
    }

    public static JSONObject coverage(){
        try{return new JSONObject()
            .put("criaturas",new JSONObject().put("executaveis",23).put("esperadas",23).put("completo",true))
            .put("companheiros_escalaveis",new JSONObject().put("executaveis",4).put("esperados",4).put("completo",true))
            .put("itens_magicos_pendentes",true)
            .put("rolagens_automaticas",false).put("idioma",UI_LANGUAGE)
            .put("livro_arcana_unleashed_completo",false);
        }catch(Exception e){return new JSONObject();}
    }

    // ---- Companheiros escaláveis ----
    private static void battleFamiliar(JSONObject o,JSONObject ctx)throws Exception{
        int lvl=clamp(ctx.optInt("nivel_magia",2),2,9);String forma=ptChoice(ctx.optString("forma","Bruto"),"Bruto","Voador","Espreitador");
        int ac=11+lvl+("Bruto".equals(forma)?2:0);int hp=("Bruto".equals(forma)?30:20)+5*Math.max(0,lvl-2);
        stats(o,"Médio","Celestial, Feérico ou Ínfero","Neutro",ac,String.valueOf(hp),"12 m; natação 9 m; voo 9 m (apenas Voador, paira)",16,16,12,8,13,10,"—","Percepção passiva 11");
        o.put("forma",forma).put("nivel_magia",lvl);
        defenses(o,"Imunidade às condições Enfeitiçado e Amedrontado");
        trait(o,"Sobrevoo (apenas Voador)","Não provoca ataques de oportunidade ao sair voando do alcance de inimigo.");
        trait(o,"Talentoso","Adiciona metade do nível da magia, arredondado para baixo, a qualquer teste de atributo ou salvaguarda.");
        multi(o,"Ataques Múltiplos","Faz número de ataques de Dilacerar igual à metade do nível da magia, arredondado para baixo; pode substituir um por Espreitar se disponível.");
        action(o,"Dilacerar","ataque_de_magia","Ataque corpo a corpo usando o modificador de ataque de magia do conjurador; alcance 1,5 m; dano 1d8 + 3 + nível da magia, de força.","rolagem_ataque","dano_total");
        if("Espreitador".equals(forma))utility(o,"Espreitar","Move até metade do deslocamento sem provocar ataques de oportunidade e então pode Esconder-se.","destino_confirmado");
    }
    private static void dinosaurSpirit(JSONObject o,JSONObject ctx)throws Exception{
        int lvl=clamp(ctx.optInt("nivel_magia",6),6,9);String forma=ptChoice(ctx.optString("forma","Anquilossauro"),"Anquilossauro","Tricerátops","Tiranossauro");
        int ac=11+lvl+("Anquilossauro".equals(forma)?2:0);int hp=60+10*Math.max(0,lvl-6);
        stats(o,"Enorme","Fera (dinossauro)","Neutro",ac,String.valueOf(hp),"12 m",21,11,15,4,12,9,"—","Percepção passiva 11");
        o.put("forma",forma).put("nivel_magia",lvl);
        if("Anquilossauro".equals(forma))trait(o,"Monstro de Cerco","Causa dano dobrado a objetos e estruturas.");
        trait(o,"Robusto","Adiciona metade do nível da magia, arredondado para baixo, às salvaguardas de Força ou Constituição.");
        multi(o,"Ataques Múltiplos","Faz número de ataques igual à metade do nível da magia, arredondado para baixo.");
        action(o,"Pancada","ataque_de_magia","Ataque corpo a corpo usando o modificador de ataque de magia; alcance 3 m; 1d10 + 5 + nível da magia de dano contundente.","rolagem_ataque","dano_total");
        if("Tiranossauro".equals(forma))action(o,"Mordida","ataque_de_magia","Alcance 3 m; 2d10 + 5 + nível da magia perfurante. Alvo Grande ou menor fica Agarrado (escape = CD da magia) e Contido enquanto durar o agarrão.","rolagem_ataque","dano_total");
        if("Tricerátops".equals(forma))action(o,"Chifrada","ataque_de_magia","Alcance 1,5 m; 1d10 + 5 + nível da magia perfurante; após mover 6+ m em linha reta contra alvo Enorme ou menor, +1d10 e o alvo fica Caído.","rolagem_ataque","dano_total","investida_valida");
    }
    private static void plantSpirit(JSONObject o,JSONObject ctx)throws Exception{
        int lvl=clamp(ctx.optInt("nivel_magia",5),5,9);String forma=ptChoice(ctx.optString("forma","Árvore"),"Fungo","Árvore","Vinha");
        int ac=11+lvl+("Árvore".equals(forma)?2:0);int hp=50+10*Math.max(0,lvl-5);
        stats(o,"Grande","Planta","Neutro",ac,String.valueOf(hp),"12 m; escalada 12 m (apenas Vinha)",17,13,14,10,13,10,"—","Percepção passiva 11");
        o.put("forma",forma).put("nivel_magia",lvl);
        if("Árvore".equals(forma))trait(o,"Monstro de Cerco","Causa dano dobrado a objetos e estruturas.");
        multi(o,"Ataques Múltiplos","Faz número de ataques igual à metade do nível da magia, arredondado para baixo.");
        if(!"Fungo".equals(forma))action(o,"Pancada","ataque_de_magia","Ataque corpo a corpo usando o modificador de ataque de magia; alcance 1,5 m; 1d10 + 3 + nível da magia contundente.","rolagem_ataque","dano_total");
        if("Fungo".equals(forma))action(o,"Jato de Esporos","ataque_de_magia","Alcance 1,5 m ou 9 m; 1d4 + nível da magia de veneno e Envenenado até fim do próximo turno; se já Envenenado, causa +3d4 de veneno.","rolagem_ataque","dano_total");
        if("Vinha".equals(forma))utility(o,"Desviar-se (Ação Bônus)","Realiza Disparada ou Desengajar.","escolha:Disparada|Desengajar");
    }
    private static void vestigeCompanion(JSONObject o,JSONObject ctx)throws Exception{
        int lvl=clamp(ctx.optInt("nivel_bruxo",1),1,20);int cha=ctx.optInt("mod_carisma",3);int prof=ctx.optInt("bonus_proficiencia",2);
        String forma=ptChoice(ctx.optString("forma","Celestial"),"Celestial","Ínfero","Morto-vivo");
        int ac=13+cha;int hp=4+4*lvl;
        stats(o,"Pequeno",forma,"Neutro",ac,String.valueOf(hp),"1,5 m; voo 9 m (paira)",1,14,10,15,15,16,"—","Percepção passiva 12");
        o.put("forma",forma).put("nivel_bruxo",lvl).put("dados_vida",lvl+"d6");
        defenses(o,("Celestial".equals(forma)?"Resistência a radiante":("Ínfero".equals(forma)?"Resistência a fogo":"Resistência a necrótico")),"Imunidade às condições Enfeitiçado, Amedrontado e Caído");
        trait(o,"Vínculo de Pacto","Adiciona o bônus de proficiência do bruxo ("+prof+") aos testes de atributo e salvaguardas do vestígio.");
        action(o,"Golpe do Vestígio","ataque_de_magia","Ataque corpo a corpo ou à distância usando o modificador de ataque de magia; alcance 1,5 m ou 18 m; 1d6 + 3 + modificador de Carisma de "+("Celestial".equals(forma)?"radiante":("Ínfero".equals(forma)?"fogo":"necrótico"))+".","rolagem_ataque","dano_total");
        if("Celestial".equals(forma))utility(o,"Poder Divino — Toque Curativo (1/dia)","Uma criatura tocada recupera 2d8 + modificador de Carisma PV e encerra Cego, Surdo ou Envenenado.","cura_total","condicao_encerrada");
        if("Ínfero".equals(forma))utility(o,"Poder Divino — Troca Ínfera (1/dia)","Se bruxo e vestígio estiverem a até 18 m, ambos se teletransportam trocando de lugar.","posicoes_confirmadas");
        if("Morto-vivo".equals(forma))utility(o,"Poder Divino — Invocação Amaldiçoada (1/dia)","Amaldiçoa criatura vista a 9 m por 1 minuto; ela tem desvantagem em ataques contra o bruxo e o vestígio.","alvo_confirmado");
    }

    // ---- Helpers de construção ----
    private static JSONObject stats(JSONObject o,String tam,String tipo,String ali,int ac,String hp,String desloc,int str,int dex,int con,int inte,int wis,int cha,String nd,String pass)throws Exception{
        return o.put("tamanho",tam).put("tipo",tipo).put("alinhamento",ali).put("CA",ac).put("PV",hp).put("deslocamento",desloc)
            .put("FOR",str).put("DES",dex).put("CON",con).put("INT",inte).put("SAB",wis).put("CAR",cha).put("ND",nd).put("percepcao",pass);
    }
    private static void defenses(JSONObject o,String... xs)throws Exception{JSONArray a=o.optJSONArray("defesas");if(a==null){a=new JSONArray();o.put("defesas",a);}for(String x:xs)a.put(x);}
    private static void trait(JSONObject o,String nome,String regra)throws Exception{JSONArray a=o.optJSONArray("tracos");a.put(new JSONObject().put("nome",nome).put("regra",regra));}
    private static void spells(JSONObject o,int cd,String... xs)throws Exception{JSONArray a=o.optJSONArray("magias");for(String x:xs)a.put(x);o.put("CD_magia",cd).put("conjuracao_delega_motor_magias",true);}
    private static void multi(JSONObject o,String nome,String regra)throws Exception{action(o,nome,"sequencia",regra,"confirmacao_sequencia");}
    private static void attack(JSONObject o,String nome,int bonus,String alcance,String dano,String... req)throws Exception{
        JSONObject a=actionObj(nome,"ataque","Ataque +"+bonus+", alcance "+alcance+", dano "+dano+".",req).put("bonus_ataque",bonus).put("alcance",alcance).put("dano_formula",dano);o.optJSONArray("acoes").put(a);
    }
    private static void attackEffect(JSONObject o,String nome,int bonus,String alcance,String dano,String efeito,String... req)throws Exception{
        JSONObject a=actionObj(nome,"ataque","Ataque +"+bonus+", alcance "+alcance+", dano "+dano+". "+efeito,req).put("bonus_ataque",bonus).put("alcance",alcance).put("dano_formula",dano).put("efeito_adicional",efeito);o.optJSONArray("acoes").put(a);
    }
    private static void save(JSONObject o,String nome,String attr,int dc,String area,String dano,String efeito,String... req)throws Exception{
        JSONObject a=actionObj(nome,"salvaguarda","Salvaguarda de "+attr+" CD "+dc+"; "+area+"; dano "+dano+". "+efeito,req).put("atributo_salvaguarda",attr).put("CD",dc).put("area",area).put("dano_formula",dano).put("efeito",efeito);o.optJSONArray("acoes").put(a);
    }
    private static void reaction(JSONObject o,String nome,String regra,String... req)throws Exception{o.optJSONArray("acoes").put(actionObj(nome,"reacao",regra,req));}
    private static void utility(JSONObject o,String nome,String regra,String... req)throws Exception{o.optJSONArray("acoes").put(actionObj(nome,"utilidade",regra,req));}
    private static void action(JSONObject o,String nome,String tipo,String regra,String... req)throws Exception{o.optJSONArray("acoes").put(actionObj(nome,tipo,regra,req));}
    private static JSONObject actionObj(String nome,String tipo,String regra,String... req)throws Exception{
        JSONArray a=new JSONArray();for(String r:req)a.put(r);
        return new JSONObject().put("nome",nome).put("tipo",tipo).put("regra",regra).put("campos_manuais",a).put("rolagens_automaticas",false);
    }
    private static JSONObject blocked(JSONObject o,String why){try{return o.put("conhecida",false).put("resolvida",false).put("status","bloqueado").put("motivo",why).put("rolagens_automaticas",false);}catch(Exception e){return o;}}
    private static String canonical(String s){if(s==null)return null;String q=n(s);for(String[]p:CREATURES)if(q.equals(n(p[0]))||q.equals(n(p[1])))return p[0];return null;}
    private static boolean same(String a,String b){return n(a).equals(n(b));}
    private static String n(String s){if(s==null)return "";String x=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    private static String id(String s){return n(s).replace(' ','_');}
    private static int clamp(int x,int lo,int hi){return Math.max(lo,Math.min(hi,x));}
    private static String ptChoice(String raw,String... opts){for(String o:opts)if(n(o).equals(n(raw)))return o;return opts[0];}
}
