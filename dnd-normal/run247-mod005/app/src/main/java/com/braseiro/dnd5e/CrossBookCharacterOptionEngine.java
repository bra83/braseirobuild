package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * RUN133 - opções de personagem reutilizáveis de múltiplos livros (RUN132 + Eberron).
 *
 * Este engine não escolhe nada pelo jogador. Quando uma raça exige escolha
 * (perícia, idioma, tamanho, aprimoramento ou ASI flexível), a escolha é
 * persistida como pendente em species_choice_pending.
 */
public final class CrossBookCharacterOptionEngine {
    private static final String STRIX="Strixhaven: A Curriculum of Chaos";
    private static final String VOLO="Volo's Guide to Monsters";
    private static final String RAVNICA="Guildmasters' Guide to Ravnica";
    private static final String EBERRON="Eberron: Rising from the Last War";

    private static final class Def {
        final String name,source; final int page;
        Def(String n,String s,int p){name=n;source=s;page=p;}
    }
    private static final LinkedHashMap<String,Def> SPECIES=new LinkedHashMap<String,Def>();
    static {
        add("Owlin",STRIX,29);
        add("Kenku",VOLO,114); add("Povo Lagarto",VOLO,117); add("Tabaxi",VOLO,119); add("Tritão",VOLO,121);
        add("Bugbear",VOLO,120); add("Goblin",VOLO,120); add("Hobgoblin",VOLO,120); add("Kobold",VOLO,120); add("Orc",VOLO,120); add("Yuan-ti Puro Sangue",VOLO,120);
        add("Centauro",RAVNICA,15); add("Loxodonte",RAVNICA,17); add("Minotauro",RAVNICA,18); add("Híbrido Simic",RAVNICA,20); add("Vedalkeano",RAVNICA,21);
        add("Forjado Bélico",EBERRON,21); add("Kalashtar",EBERRON,27); add("Visinfante",EBERRON,35);
        add("Morfera — Courofero",EBERRON,31); add("Morfera — Passovivo",EBERRON,31); add("Morfera — Presalonga",EBERRON,31); add("Morfera — Caçabrava",EBERRON,31);
        add("Orc (Eberron)",EBERRON,32);
    }
    private CrossBookCharacterOptionEngine(){}
    private static void add(String n,String s,int p){SPECIES.put(norm(n),new Def(n,s,p));}

    public static boolean isKnownSpecies(String species){return SPECIES.containsKey(norm(species));}
    public static JSONArray allSpecies(){JSONArray a=new JSONArray();for(Def d:SPECIES.values())a.put(d.name);return a;}
    public static JSONObject lookupSpecies(String species){
        JSONObject o=new JSONObject(); try{
            Def d=SPECIES.get(norm(species));
            if(d==null)return o.put("known",false);
            return o.put("known",true).put("species",d.name).put("source",d.source).put("page",d.page)
                    .put("runtime","CrossBookCharacterOptionEngine").put("mechanics_compiled",true);
        }catch(Exception ignored){return o;}
    }

    /** Retorna true quando esta engine tratou o ASI 2014. */
    public static boolean apply2014AbilityBonuses(JSONObject o){
        if(o==null)return false; String s=norm(o.optString("species"));
        try{
            if(s.equals("kenku")){addAbility(o,1,2);addAbility(o,4,1);markAsi(o,"+2 Destreza, +1 Sabedoria");return true;}
            if(s.equals("povo lagarto")||s.equals("homem lagarto")||s.equals("lizardfolk")){addAbility(o,2,2);addAbility(o,4,1);markAsi(o,"+2 Constituição, +1 Sabedoria");return true;}
            if(s.equals("tabaxi")){addAbility(o,1,2);addAbility(o,5,1);markAsi(o,"+2 Destreza, +1 Carisma");return true;}
            if(s.equals("tritao")||s.equals("triton")){addAbility(o,0,1);addAbility(o,2,1);addAbility(o,5,1);markAsi(o,"+1 Força, +1 Constituição, +1 Carisma");return true;}
            if(s.equals("bugbear")){addAbility(o,0,2);addAbility(o,1,1);markAsi(o,"+2 Força, +1 Destreza");return true;}
            if(s.equals("goblin")){addAbility(o,1,2);addAbility(o,2,1);markAsi(o,"+2 Destreza, +1 Constituição");return true;}
            if(s.equals("hobgoblin")){addAbility(o,2,2);addAbility(o,3,1);markAsi(o,"+2 Constituição, +1 Inteligência");return true;}
            if(s.equals("kobold")){addAbility(o,1,2);addAbility(o,0,-2);markAsi(o,"+2 Destreza, -2 Força");return true;}
            if(s.equals("orc")){addAbility(o,0,2);addAbility(o,2,1);addAbility(o,3,-2);markAsi(o,"+2 Força, +1 Constituição, -2 Inteligência");return true;}
            if(s.contains("yuan ti")){addAbility(o,5,2);addAbility(o,3,1);markAsi(o,"+2 Carisma, +1 Inteligência");return true;}
            if(s.equals("centauro")){addAbility(o,0,2);addAbility(o,4,1);markAsi(o,"+2 Força, +1 Sabedoria");return true;}
            if(s.equals("loxodonte")){addAbility(o,2,2);addAbility(o,4,1);markAsi(o,"+2 Constituição, +1 Sabedoria");return true;}
            if(s.equals("minotauro")){addAbility(o,0,2);addAbility(o,2,1);markAsi(o,"+2 Força, +1 Constituição");return true;}
            if(s.contains("hibrido simic")){addAbility(o,2,2);pending(o,"ASI: escolha +1 em outro atributo");markAsi(o,"+2 Constituição; +1 pendente de escolha");return true;}
            if(s.equals("vedalkeano")||s.equals("vedalken")){addAbility(o,3,2);addAbility(o,4,1);markAsi(o,"+2 Inteligência, +1 Sabedoria");return true;}
            if(s.equals("owlin")){pending(o,"ASI flexível: +2/+1 diferentes ou +1/+1/+1 em três atributos");markAsi(o,"ASI flexível pendente de escolha");return true;}
            if(s.equals("forjado belico")){addAbility(o,2,2);pending(o,"ASI: escolha +1 em outro atributo");markAsi(o,"+2 Constituição; +1 pendente de escolha");return true;}
            if(s.equals("kalashtar")){addAbility(o,4,2);addAbility(o,5,1);markAsi(o,"+2 Sabedoria, +1 Carisma");return true;}
            if(s.equals("visinfante")){addAbility(o,5,2);pending(o,"ASI: escolha +1 em outro atributo");markAsi(o,"+2 Carisma; +1 pendente de escolha");return true;}
            if(s.equals("morfera courofero")){addAbility(o,2,2);addAbility(o,0,1);markAsi(o,"+2 Constituição, +1 Força");return true;}
            if(s.equals("morfera passovivo")){addAbility(o,1,2);addAbility(o,5,1);markAsi(o,"+2 Destreza, +1 Carisma");return true;}
            if(s.equals("morfera presalonga")){addAbility(o,0,2);addAbility(o,1,1);markAsi(o,"+2 Força, +1 Destreza");return true;}
            if(s.equals("morfera cacabrava")){addAbility(o,4,2);addAbility(o,1,1);markAsi(o,"+2 Sabedoria, +1 Destreza");return true;}
            if(s.equals("orc eberron")){addAbility(o,0,2);addAbility(o,2,1);markAsi(o,"+2 Força, +1 Constituição");return true;}
        }catch(Exception ignored){}
        return false;
    }

    public static String speedOverride(String species,String profile){
        String s=norm(species); boolean revised=profile!=null&&(profile.startsWith("5.5")||norm(profile).contains("2024"));
        if(s.equals("centauro"))return "12 m";
        if(s.equals("owlin")||s.equals("kenku")||s.equals("povo lagarto")||s.equals("tabaxi")||s.equals("tritao")||s.equals("bugbear")||s.equals("goblin")||s.equals("hobgoblin")||s.equals("kobold")||s.equals("orc")||s.contains("yuan ti")||s.equals("loxodonte")||s.equals("minotauro")||s.contains("hibrido simic")||s.equals("vedalkeano")||s.equals("forjado belico")||s.equals("kalashtar")||s.equals("visinfante")||s.startsWith("morfera ")||s.equals("orc eberron"))return "9 m";
        // Golias 2014 de Volo não é registrado aqui; o Golias 2024 continua no core.
        return "";
    }

    public static JSONObject applySpeciesMechanics(JSONObject o){
        if(o==null)return new JSONObject(); String s=norm(o.optString("species")); Def d=SPECIES.get(s); if(d==null)return o;
        try{
            o.put("species_source",d.source).put("species_source_page",d.page).put("creature_type","Humanoide");
            if(s.equals("owlin")){
                o.put("size_options","Pequeno|Médio");pending(o,"Tamanho: Pequeno ou Médio");
                darkvision(o,36); grantSkill(o,"Furtividade"); appendFeature(o,"Voo igual ao deslocamento de caminhada; indisponível com armadura média ou pesada"); appendFeature(o,"Penas Silenciosas: proficiência em Furtividade");
                o.put("fly_speed","9 m (condicional à armadura)"); ensureLanguage(o,"Comum");pending(o,"Idioma: escolha 1 idioma adicional");
            } else if(s.equals("kenku")){
                appendFeature(o,"Perito em Falsificação: vantagem para duplicar escrita e objetos existentes"); appendFeature(o,"Mimetismo");
                pending(o,"Treinamento Kenku: escolha 2 entre Acrobacia, Enganação, Furtividade e Prestidigitação"); o.put("languages","Comum e Auran; fala somente por Mimetismo");
            } else if(s.equals("povo lagarto")||s.equals("homem lagarto")||s.equals("lizardfolk")){
                o.put("swim_speed","9 m"); appendFeature(o,"Mordida: arma natural 1d6 perfurante + Força"); appendFeature(o,"Artesão Habilidoso"); appendFeature(o,"Segurar Respiração: até 15 minutos"); appendFeature(o,"Tubarão Faminto: mordida especial com PV temporários; recarga descanso curto/longo");
                pending(o,"Conhecimento de Caçador: escolha 2 entre Adestrar Animais, Natureza, Percepção, Furtividade e Sobrevivência");
                int natural=13+mod(o.optInt("dex",10));o.put("natural_armor_ac",natural);pending(o,"CA: escolher Armadura Natural (13 + Destreza) ou outra fórmula disponível");o.put("languages","Comum, Dracônico");
            } else if(s.equals("tabaxi")){
                darkvision(o,18);grantSkill(o,"Percepção");grantSkill(o,"Furtividade");o.put("climb_speed","6 m"); appendFeature(o,"Agilidade Felina: dobra o deslocamento por um turno; recarrega após passar um turno sem se mover");appendFeature(o,"Garras do Gato: escalada 6 m e ataque natural cortante");o.put("languages","Comum e 1 idioma adicional");pending(o,"Idioma: escolha 1 idioma adicional");
            } else if(s.equals("tritao")||s.equals("triton")){
                o.put("swim_speed","9 m");appendFeature(o,"Anfíbio: respira ar e água");appendFeature(o,"Controle do Ar e Água: Névoa Obscurecente; Rajada de Vento no 3º; Muralha de Água no 5º; recarga descanso longo");appendFeature(o,"Emissário do Mar");appendFeature(o,"Guardiões das Profundezas");appendToken(o,"resistances","frio");o.put("languages","Comum e Primordial");
            } else if(s.equals("bugbear")){
                darkvision(o,18);grantSkill(o,"Furtividade");powerfulBuild(o);appendFeature(o,"Braços Longos: +1,5 m de alcance em ataques corpo a corpo no próprio turno");appendFeature(o,"Ataque Surpresa: +2d6 uma vez por combate contra criatura surpreendida no primeiro turno");o.put("languages","Comum, Goblin");
            } else if(s.equals("goblin")){
                darkvision(o,18);appendFeature(o,"Fúria dos Pequenos: dano extra igual ao nível contra criatura maior; recarga descanso curto/longo");appendFeature(o,"Escapada Ágil: Desengajar ou Esconder-se como ação bônus");o.put("size_options","Pequeno");o.put("languages","Comum, Goblin");
            } else if(s.equals("hobgoblin")){
                darkvision(o,18);appendFeature(o,"Salvando a Cara: bônus igual a aliados visíveis a 9 m, máximo +5; recarga descanso curto/longo");appendFeature(o,"Treinamento Marcial: armadura leve e 2 armas marciais");pending(o,"Treinamento Marcial: escolha 2 armas marciais");o.put("languages","Comum, Goblin");
            } else if(s.equals("kobold")){
                darkvision(o,18);appendFeature(o,"Bajular, Encolher-se e Implorar: ação; aliados ganham vantagem contra inimigos próximos; recarga descanso curto/longo");appendFeature(o,"Táticas de Bando: vantagem em ataques com aliado não incapacitado adjacente ao alvo");appendFeature(o,"Sensibilidade à Luz Solar");o.put("pack_tactics",true).put("sunlight_sensitivity",true).put("size_options","Pequeno").put("languages","Comum, Dracônico");
            } else if(s.equals("orc")){
                darkvision(o,18);grantSkill(o,"Intimidação");powerfulBuild(o);appendFeature(o,"Agressividade: ação bônus para mover-se em direção a inimigo visto ou ouvido");o.put("languages","Comum, Orc");
            } else if(s.contains("yuan ti")){
                darkvision(o,18);appendFeature(o,"Conjuração Inata: Rajada de Veneno; Amizade Animal apenas em serpentes; Sugestão a partir do 3º nível");appendFeature(o,"Resistência à Magia: vantagem em salvaguardas contra magias e efeitos mágicos");appendToken(o,"immunities","veneno");appendToken(o,"condition_immunities","envenenado");o.put("magic_resistance",true).put("languages","Comum, Abissal, Dracônico");
            } else if(s.equals("centauro")){
                o.put("creature_type","Fada");powerfulBuild(o);appendFeature(o,"Investida: após 9 m em linha reta e acerto corpo a corpo, pode atacar com cascos como ação bônus");appendFeature(o,"Cascos: arma natural 1d4 contundente + Força");appendFeature(o,"Corpo Equino: escalada que exige mãos e pés custa movimento extra");pending(o,"Sobrevivente: escolha 1 entre Adestrar Animais, Medicina, Natureza ou Sobrevivência");o.put("languages","Comum, Silvestre");
            } else if(s.equals("loxodonte")){
                powerfulBuild(o);appendFeature(o,"Serenidade Loxodonte: vantagem contra Enfeitiçado e Amedrontado");appendFeature(o,"Tromba: alcance 1,5 m para tarefas simples, agarrar e ataque desarmado; não executa precisão manual");appendFeature(o,"Olfato Aguçado: vantagem em Percepção, Sobrevivência e Investigação baseadas em olfato");int natural=12+mod(o.optInt("con",10));o.put("natural_armor_ac",natural);pending(o,"CA: escolher Armadura Natural (12 + Constituição) ou outra fórmula disponível");o.put("condition_save_advantage","enfeitiçado|amedrontado").put("languages","Comum, Loxodonte");
            } else if(s.equals("minotauro")){
                appendFeature(o,"Chifres: arma natural 1d6 perfurante + Força");appendFeature(o,"Corrida Sangrenta: após Correr e mover 6 m, ataque de chifres como ação bônus");appendFeature(o,"Martelando Chifres: após acerto corpo a corpo na ação Atacar, ação bônus para empurrar; CD 8 + PB + Força");pending(o,"Presença Imponente: escolha proficiência em Intimidação ou Persuasão");o.put("languages","Comum, Minotauro");
            } else if(s.contains("hibrido simic")){
                darkvision(o,18);o.put("languages","Comum e escolha entre Élfico ou Vedalkeano");pending(o,"Idioma: escolha Élfico ou Vedalkeano");pending(o,"Aprimoramento Animal de 1º nível: Planador Manta, Escalador Ágil ou Adaptação Subaquática");pending(o,"Aprimoramento Animal de 5º nível: opção de 1º não escolhida ou Apêndices de Luta, Carapaça ou Cuspir Ácido");appendFeature(o,"Aprimoramentos Simic são escolhas persistentes e não são sorteados automaticamente");
            } else if(s.equals("vedalkeano")||s.equals("vedalken")){
                appendFeature(o,"Desapego Vedalkeano: vantagem em salvaguardas de Inteligência, Sabedoria e Carisma");appendFeature(o,"Parcialmente Anfíbio: respira sob a água por até 1 hora; recarga descanso longo após atingir o limite");pending(o,"Precisão Incansável: escolha 1 perícia e 1 ferramenta; some 1d4 aos testes com ambas");pending(o,"Idioma: escolha 1 idioma adicional");o.put("saving_throw_advantage_abilities","Inteligência|Sabedoria|Carisma").put("languages","Comum, Vedalkeano e 1 idioma adicional");
            } else if(s.equals("forjado belico")){
                o.put("size_options","Médio");appendToken(o,"resistances","veneno");appendToken(o,"condition_save_advantage","envenenado");appendToken(o,"immunities","doença");appendFeature(o,"Resiliência de Forjado: vantagem contra Envenenado; resistência a veneno; imune a doença; não precisa comer, beber ou respirar; magia não pode fazê-lo dormir");appendFeature(o,"Repouso do Sentinela: descanso longo em 6 horas de inatividade imóvel, sem ficar inconsciente e mantendo visão/audição");appendFeature(o,"Proteção Integrada: +1 CA; integra ou remove armadura em 1 hora; somente armaduras com as quais tenha proficiência; armadura integrada não pode ser removida contra sua vontade");o.put("integrated_protection_ac_bonus",1).put("does_not_need_food_drink_breathe",true).put("magic_sleep_immune",true);pending(o,"Projeto Especializado: escolha 1 perícia");pending(o,"Projeto Especializado: escolha 1 ferramenta");pending(o,"Idioma: escolha 1 idioma adicional");o.put("languages","Comum e 1 idioma adicional");
            } else if(s.equals("kalashtar")){
                appendFeature(o,"Mente Dupla: vantagem em todas as salvaguardas de Sabedoria");appendToken(o,"resistances","psíquico");appendFeature(o,"Ligação Mental: telepatia com criatura visível a alcance de 3 × nível metros; pode permitir resposta telepática por 1 hora a uma criatura por vez");appendFeature(o,"Separado dos Sonhos: imune a magias e efeitos que exigem sonhar, mas não a efeitos que causam sono");o.put("saving_throw_advantage_abilities","Sabedoria").put("telepathy_range_formula_m","3 × nível").put("dream_effect_immune",true).put("languages","Comum, Quori e 1 idioma adicional");pending(o,"Idioma: escolha 1 idioma adicional");
            } else if(s.equals("visinfante")){
                appendFeature(o,"Metamorfo: como ação altera aparência e voz, mantendo estatísticas e tamanho; roupas e equipamentos não mudam; volta à forma verdadeira ao morrer");pending(o,"Instintos Visinfante: escolha 2 entre Enganação, Intuição, Intimidação e Persuasão");pending(o,"Idiomas: escolha 2 idiomas adicionais");o.put("shapechanger",true).put("languages","Comum e 2 idiomas adicionais");
            } else if(s.startsWith("morfera ")){
                darkvision(o,18);appendFeature(o,"Transição: ação bônus por 1 minuto; ganha PV temporários iguais a nível + modificador de Constituição (mínimo 1); recarga descanso curto ou longo");o.put("shifting_recharge","descanso curto ou longo").put("shifting_temp_hp_formula","nível + modificador de Constituição (mínimo 1)").put("languages","Comum");
                if(s.equals("morfera courofero")){grantSkill(o,"Atletismo");appendFeature(o,"Courofero: ao Transicionar recebe +1 CA e 1d6 PV temporários adicionais");o.put("shifting_ac_bonus",1).put("shifting_extra_temp_hp","1d6");}
                else if(s.equals("morfera passovivo")){grantSkill(o,"Acrobacia");appendFeature(o,"Passovivo: durante Transição +3 m de deslocamento; reação para mover 3 m sem provocar ataque de oportunidade quando inimigo termina turno a 1,5 m");o.put("shifting_speed_bonus_m",3);}
                else if(s.equals("morfera presalonga")){grantSkill(o,"Intimidação");appendFeature(o,"Presalonga: durante Transição pode morder como ação bônus, 1d6 + Força perfurante");o.put("shifting_bite_damage","1d6 + Força perfurante");}
                else if(s.equals("morfera cacabrava")){grantSkill(o,"Sobrevivência");appendFeature(o,"Caçabrava: durante Transição vantagem em testes de Sabedoria e criaturas a até 9 m não podem ter vantagem em ataques contra você, salvo se incapacitado");o.put("shifting_wisdom_check_advantage",true).put("shifting_blocks_enemy_attack_advantage_within_m",9);}
            } else if(s.equals("orc eberron")){
                darkvision(o,18);powerfulBuild(o);appendFeature(o,"Agressividade: ação bônus para mover-se até seu deslocamento em direção a inimigo visto ou ouvido, terminando mais perto");pending(o,"Intuição Primitiva: escolha 2 entre Adestrar Animais, Intuição, Intimidação, Medicina, Natureza, Percepção e Sobrevivência");o.put("languages","Comum, Orc");
            }
            o.put("species_mechanics_runtime",true);
        }catch(Exception ignored){}
        return o;
    }

    private static void markAsi(JSONObject o,String text)throws Exception{o.put("origin_ability_bonus",text+" (fonte da espécie 2014)");}
    private static void addAbility(JSONObject o,int idx,int delta)throws Exception{String[] k={"str","dex","con","int_score","wis","cha"};int v=o.optInt(k[idx],10)+delta;o.put(k[idx],Math.max(1,Math.min(20,v)));}
    private static int mod(int score){return (int)Math.floor((score-10)/2.0);}
    private static int pb(JSONObject o){int l=Math.max(1,Math.min(20,o.optInt("level",1)));return 2+(l-1)/4;}
    private static int skillMod(JSONObject o,String skill){String n=norm(skill);int score;if(n.equals("atletismo"))score=o.optInt("str",10);else if(n.equals("acrobacia")||n.equals("furtividade")||n.equals("prestidigitacao"))score=o.optInt("dex",10);else if(n.equals("arcanismo")||n.equals("historia")||n.equals("investigacao")||n.equals("natureza")||n.equals("religiao"))score=o.optInt("int_score",10);else if(n.equals("adestrar animais")||n.equals("intuicao")||n.equals("medicina")||n.equals("percepcao")||n.equals("sobrevivencia"))score=o.optInt("wis",10);else score=o.optInt("cha",10);return mod(score);}
    private static void grantSkill(JSONObject o,String skill)throws Exception{
        String raw=o.optString("skills","");String[] parts=raw.split("\\s*•\\s*");StringBuilder b=new StringBuilder();boolean found=false;String target=norm(skill);int bonus=skillMod(o,skill)+pb(o);
        for(String p:parts){if(p.trim().isEmpty())continue;String out=p.trim();if(norm(out).startsWith(target+" ")||norm(out).equals(target)){out=skill+" "+signed(bonus)+" ★";found=true;}if(b.length()>0)b.append(" • ");b.append(out);}if(!found){if(b.length()>0)b.append(" • ");b.append(skill).append(' ').append(signed(bonus)).append(" ★");}o.put("skills",b.toString());appendToken(o,"species_skill_proficiencies",skill);
    }
    private static String signed(int n){return n>=0?"+"+n:String.valueOf(n);}
    private static void darkvision(JSONObject o,int meters)throws Exception{o.put("darkvision_m",meters);String p=o.optString("passive_senses","");if(!norm(p).contains("visao no escuro"))o.put("passive_senses",p+(p.trim().isEmpty()?"":" • ")+"Visão no Escuro "+meters+" m");}
    private static void powerfulBuild(JSONObject o)throws Exception{o.put("powerful_build",true).put("carrying_capacity_multiplier",2);int kg=Math.max(1,o.optInt("str",10)*14);o.put("carrying_capacity",kg+" kg (aprox.; Constituição/Corpo Poderoso)");}
    private static void ensureLanguage(JSONObject o,String lang)throws Exception{appendToken(o,"languages",lang);}
    private static void appendFeature(JSONObject o,String feature)throws Exception{String f=o.optString("features","");if(!norm(f).contains(norm(feature)))o.put("features",f+(f.trim().isEmpty()?"":"\n")+"Traço de espécie: "+feature+".");}
    private static void pending(JSONObject o,String choice)throws Exception{JSONArray a=o.optJSONArray("species_choice_pending");if(a==null)a=new JSONArray();for(int i=0;i<a.length();i++)if(norm(choice).equals(norm(a.optString(i)))){o.put("species_choice_pending",a);return;}a.put(choice);o.put("species_choice_pending",a).put("species_choices_complete",false);}
    private static void appendToken(JSONObject o,String key,String value)throws Exception{String raw=o.optString(key,"");String nv=norm(value);for(String p:raw.split("[|,;•]"))if(norm(p).equals(nv))return;o.put(key,raw.trim().isEmpty()?value:raw+"|"+value);}
    private static String norm(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
