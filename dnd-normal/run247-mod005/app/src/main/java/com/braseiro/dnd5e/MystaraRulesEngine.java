package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Núcleo determinístico de D&D Rules Cyclopedia/BECMI para o perfil Mystara.
 * Não contém nenhuma mecânica de D&D 5e.
 */
public final class MystaraRulesEngine {
    public static final String SYSTEM_ID = "mystara_rules_cyclopedia";
    public static final String SYSTEM_LABEL = "D&D Rules Cyclopedia (BECMI)";
    public static final String[] CLASSES = {"Clérigo","Guerreiro","Mago","Ladrão","Anão","Elfo","Halfling","Druida","Místico"};
    public static final String[] ALIGNMENTS = {"Ordeiro","Neutro","Caótico"};
    public static final String[] SAVE_NAMES = {"Raio Mortal ou Veneno","Varinhas Mágicas","Paralisia ou Petrificação","Baforada de Dragão","Bastões, Cajados ou Feitiços"};
    public static final String[] TERRAIN_LABELS = {"Trilha ou estrada","Aberto, cidade ou planície","Floresta, lama ou neve","Colina, deserto ou terreno acidentado","Montanha, pântano ou selva","Gelo ou geleira"};
    public static final String[] TRAVEL_MODES = {"A pé — sem carga","A pé — carga leve","A pé — carga pesada","Camelo","Elefante","Cavalo de montaria","Burro ou mula","Cavalo de guerra","Cavalo de carga","Boi","Barco de rio","Barco a vela","Canoa","Galeão grande — remando","Galeão grande — velejando","Galeão pequeno — remando","Galeão pequeno — velejando","Galeão de guerra — remando","Galeão de guerra — velejando","Bote salva-vidas","Barco viking — remando","Barco viking — velejando","Jangada","Navio a vela pequeno","Navio a vela grande","Transporte de tropas","Montaria aérea","Aparato aéreo"};
    public static final String[] ARMORS = {"Sem armadura","Armadura de couro","Armadura de escamas","Cota de malha","Armadura de bandas","Armadura de placas","Armadura completa"};

    public static final String[] GENERAL_SKILLS = {
        "Força Bruta — Força","Intimidação — Força","Luta — Força",
        "Alquimia — Inteligência","Armadilha — Inteligência","Arte — Inteligência","Artilharia — Inteligência","Caça — Inteligência","Ciência — Inteligência","Conhecimento — Inteligência","Conhecimento da Natureza — Inteligência","Construção de Navios — Inteligência","Cura — Inteligência","Cura Veterinária — Inteligência","Disfarce — Inteligência","Engenharia — Inteligência","Engenharia Mágica — Inteligência","Fazer Fogo — Inteligência","Geografia Planar — Inteligência","Idioma — Inteligência","Imitação — Inteligência","Leitura Labial — Inteligência","Magias Alternativas — Inteligência","Manufatura — Inteligência","Mapeamento/Cartografia — Inteligência","Navegação — Inteligência","Ofício — Inteligência","Profissão — Inteligência","Rastreamento — Inteligência","Sinalização — Inteligência","Sobrevivência — Inteligência","Táticas Militares — Inteligência",
        "Adestramento de Animais — Sabedoria","Arte — Sabedoria","Bravura — Sabedoria","Cerimonial — Sabedoria","Detectar Dissimulação — Sabedoria","Espeleologia — Sabedoria","Jogo — Sabedoria","Lei e Justiça — Sabedoria","Misticismo — Sabedoria","Senso de Perigo — Sabedoria",
        "Acrobacias — Destreza","Escapismo — Destreza","Furtividade — Destreza","Montanhismo — Destreza","Montaria — Destreza","Pilotagem — Destreza","Prontidão — Destreza","Saque Rápido — Destreza","Tiro às Cegas — Destreza","Trapaças — Destreza",
        "Provar Comida — Constituição","Resistência — Constituição",
        "Atuação — Carisma","Barganha — Carisma","Canto — Carisma","Dissimulação — Carisma","Liderança — Carisma","Música — Carisma","Narrativa — Carisma","Persuasão — Carisma"
    };
    public static final String[] WEAPONS = {"Adaga","Alabarda","Arco curto","Arco longo","Azagaia","Besta leve","Besta pesada","Cajado","Cesti","Clava","Espada bastarda — uma mão","Espada bastarda — duas mãos","Espada curta","Espada normal","Foice","Lança","Lança longa","Lança montada","Machado de batalha","Machado de mão","Maça","Martelo de guerra","Rede","Tridente","Funda"};
    public static final String[] MASTERY_LEVELS = {"Sem treinamento","Básico","Habilidoso","Especialista","Mestre","Grão-Mestre"};

    private final Random random = new Random();

    private static final Map<String,Integer> ARMOR_AC;
    static {
        LinkedHashMap<String,Integer> m=new LinkedHashMap<String,Integer>();
        m.put("Sem armadura",9);m.put("Armadura de couro",7);m.put("Armadura de escamas",6);m.put("Cota de malha",5);m.put("Armadura de bandas",4);m.put("Armadura de placas",3);m.put("Armadura completa",0);ARMOR_AC=Collections.unmodifiableMap(m);
    }

    private static final int[][] SAVES_CLERIC={
        {11,12,14,16,15},{9,10,12,14,13},{7,8,10,12,11},{6,7,8,10,9},{5,6,6,8,7},{4,5,5,6,5},{3,4,4,4,4},{2,3,3,3,3},{2,2,2,2,2}
    };
    private static final int[][] SAVES_FIGHTER={
        {12,13,14,15,16},{10,11,12,13,14},{8,9,10,11,12},{6,7,8,9,10},{6,6,7,8,9},{5,6,6,7,8},{5,5,6,6,7},{4,5,5,5,6},{4,4,5,4,5},{3,4,4,3,4},{3,3,3,2,3},{2,2,2,2,2}
    };
    private static final int[][] SAVES_MAGIC={
        {13,14,13,16,15},{11,12,11,14,12},{9,10,9,12,9},{7,8,7,10,6},{5,6,5,8,4},{4,4,4,6,3},{3,3,3,4,2},{2,2,2,2,2}
    };
    private static final int[][] SAVES_THIEF={
        {13,14,13,16,15},{11,12,11,14,13},{9,10,9,12,11},{7,8,7,10,9},{5,6,5,8,7},{4,5,4,6,5},{3,4,3,4,4},{2,3,2,3,3},{2,2,2,2,2}
    };
    private static final int[][] SAVES_DWARF={{8,9,10,13,12},{6,7,8,10,9},{4,5,6,7,6},{2,3,4,4,3}};
    private static final int[][] SAVES_ELF={{12,13,13,15,15},{8,10,10,11,11},{4,7,7,7,7},{2,4,4,3,3}};
    private static final int[][] SAVES_HALFLING={{8,9,10,13,12},{5,6,7,9,8},{2,3,4,5,4}};

    private static final int[][] THIEF_SKILLS={
        {15,10,10,87,20,10,20,30},{20,15,15,88,25,15,25,35},{25,20,20,89,30,20,30,40},{30,25,25,90,35,24,35,45},
        {35,30,30,91,40,28,40,50},{40,35,34,92,44,32,45,54},{45,40,38,93,48,35,50,58},{50,45,42,94,52,38,55,62},
        {54,50,46,95,55,41,60,66},{58,54,50,96,58,44,65,70},{62,58,54,97,61,47,70,74},{66,62,58,98,64,50,75,78},
        {69,66,61,99,66,53,80,81},{72,70,64,100,68,56,85,84},{75,73,67,101,70,58,90,87},{78,76,70,102,72,60,95,90},
        {81,80,73,103,74,62,100,92},{84,83,76,104,76,64,105,94},{86,86,79,105,78,66,110,96},{88,89,82,106,80,68,115,98},
        {90,92,85,107,82,70,120,100},{92,94,88,108,84,72,125,102},{94,96,91,109,86,74,130,104},{96,98,94,110,88,76,135,106},
        {98,99,97,111,89,78,140,108},{100,100,100,112,90,80,145,110},{102,101,103,113,91,82,150,112},{104,102,106,114,92,84,155,114},
        {106,103,109,115,93,86,160,116},{108,104,112,116,94,88,165,118},{110,105,115,117,95,90,170,120},{112,106,118,118,96,92,175,122},
        {114,107,121,118,97,94,180,124},{116,108,124,119,98,96,185,126},{118,109,127,119,99,98,190,128},{120,110,130,120,100,100,195,130}
    };
    public static final String[] THIEF_SKILL_NAMES={"Abrir Fechaduras","Encontrar Armadilhas","Remover Armadilhas","Escalar Paredes","Mover-se em Silêncio","Esconder-se nas Sombras","Bater Carteiras","Ouvir Ruídos"};
    public static final String[] UNDEAD_TYPES={"Esqueleto","Zumbi","Carniçal","Inumano","Aparição","Múmia","Espectro","Vampiro","Espírito Vingativo","Assombração","Espírito","Vulto Noturno","Lich","Especial"};
    private static final String[][] TURN_UNDEAD={
        {"7","A","A","D","D","D","D","D","D","D+","D+","D+","D+","D+","D#","D#","D#"},
        {"9","7","A","A","D","D","D","D","D","D","D+","D+","D+","D+","D+","D#","D#"},
        {"11","9","7","A","A","D","D","D","D","D","D","D+","D+","D+","D+","D+","D#"},
        {"-","11","9","7","A","A","D","D","D","D","D","D","D+","D+","D+","D+","D+"},
        {"-","-","11","9","7","A","A","D","D","D","D","D","D","D+","D+","D+","D+"},
        {"-","-","-","11","9","7","A","A","D","D","D","D","D","D","D+","D+","D+"},
        {"-","-","-","-","11","9","7","A","A","D","D","D","D","D","D","D+","D+"},
        {"-","-","-","-","-","11","9","7","A","A","D","D","D","D","D","D","D+"},
        {"-","-","-","-","-","-","11","9","7","A","A","D","D","D","D","D","D"},
        {"-","-","-","-","-","-","-","11","9","7","A","A","D","D","D","D","D"},
        {"-","-","-","-","-","-","-","-","11","9","7","A","A","D","D","D","D"},
        {"-","-","-","-","-","-","-","-","-","11","9","7","A","A","D","D","D"},
        {"-","-","-","-","-","-","-","-","-","-","11","9","7","A","A","A","A"},
        {"-","-","-","-","-","-","-","-","-","-","-","11","9","7","A","A","A"}
    };


    // Tabelas de experiência da Enciclopédia de Regras. Índice 0 = nível 1.
    private static final int[] XP_CLERIC={0,1500,3000,6000,12000,25000,50000,100000,200000,300000,400000,500000,600000,700000,800000,900000,1000000,1100000,1200000,1300000,1400000,1500000,1600000,1700000,1800000,1900000,2000000,2100000,2200000,2300000,2400000,2500000,2600000,2700000,2800000,2900000};
    private static final int[] XP_FIGHTER={0,2000,4000,8000,16000,32000,64000,120000,240000,360000,480000,600000,720000,840000,960000,1080000,1200000,1320000,1440000,1560000,1680000,1800000,1920000,2040000,2160000,2280000,2400000,2520000,2640000,2760000,2880000,3000000,3120000,3240000,3360000,3480000};
    private static final int[] XP_MAGIC={0,2500,5000,10000,20000,40000,80000,150000,300000,450000,600000,750000,900000,1050000,1200000,1350000,1500000,1650000,1800000,1950000,2100000,2250000,2400000,2550000,2700000,2850000,3000000,3150000,3300000,3450000,3600000,3750000,3900000,4050000,4200000,4350000};
    private static final int[] XP_THIEF={0,1200,2400,4800,9600,20000,40000,80000,160000,280000,400000,520000,640000,760000,880000,1000000,1120000,1240000,1360000,1480000,1600000,1720000,1840000,1960000,2080000,2200000,2320000,2440000,2560000,2680000,2800000,2920000,3040000,3160000,3280000,3400000};
    private static final int[] XP_DWARF={0,2200,4400,8800,17000,35000,70000,140000,270000,400000,530000,660000};
    private static final int[] XP_ELF={0,4000,8000,16000,32000,64000,120000,250000,400000,600000};
    private static final int[] XP_HALFLING={0,2000,4000,8000,16000,32000,64000,120000};
    private static final int[] XP_MYSTIC={0,2000,4000,8000,16000,32000,64000,120000,240000,360000,480000,600000,720000,840000,960000,1080000};

    // Feitiços por dia. Cada linha corresponde ao nível de personagem; colunas aos níveis de feitiço.
    private static final int[][] SPELLS_CLERIC={
        {0,0,0,0,0,0,0},{1,0,0,0,0,0,0},{2,0,0,0,0,0,0},{2,1,0,0,0,0,0},{2,2,0,0,0,0,0},{2,2,1,0,0,0,0},{3,2,2,0,0,0,0},{3,3,2,1,0,0,0},{3,3,3,2,0,0,0},{4,4,3,2,1,0,0},{4,4,3,3,2,1,0},{4,4,4,3,2,1,0},{5,5,4,3,2,2,0},{5,5,5,3,3,2,0},{6,5,5,3,3,3,0},{6,5,5,4,4,3,0},{6,6,5,4,4,3,1},{6,6,5,4,4,3,2},{7,6,5,4,4,4,2},{7,6,5,4,4,4,3},{7,6,5,5,5,4,3},{7,6,5,5,5,4,4},{7,7,6,6,5,4,4},{8,7,6,6,5,5,4},{8,7,6,6,5,5,5},{8,7,7,6,6,5,5},{8,8,7,6,6,6,5},{8,8,7,7,7,6,5},{8,8,7,7,7,6,6},{8,8,8,7,7,7,6},{8,8,8,8,8,7,6},{9,8,8,8,8,7,7},{9,9,8,8,8,8,7},{9,9,9,8,8,8,8},{9,9,9,9,9,8,8},{9,9,9,9,9,9,9}
    };
    private static final int[][] SPELLS_MAGIC={
        {1,0,0,0,0,0,0,0,0},{2,0,0,0,0,0,0,0,0},{2,1,0,0,0,0,0,0,0},{2,2,0,0,0,0,0,0,0},{2,2,1,0,0,0,0,0,0},{2,2,2,0,0,0,0,0,0},{3,2,2,1,0,0,0,0,0},{3,3,2,2,0,0,0,0,0},{3,3,3,2,1,0,0,0,0},{3,3,3,3,2,0,0,0,0},{4,3,3,3,2,1,0,0,0},{4,4,4,3,2,1,0,0,0},{4,4,4,3,2,2,0,0,0},{4,4,4,4,3,2,0,0,0},{5,4,4,4,3,2,1,0,0},{5,5,5,4,3,2,2,0,0},{6,5,5,4,4,3,2,0,0},{6,5,5,4,4,3,2,1,0},{6,5,5,5,4,3,2,2,0},{6,5,5,5,4,4,3,2,0},{6,5,5,5,4,4,3,2,1},{6,6,5,5,5,4,3,2,2},{6,6,6,6,5,4,3,3,2},{7,7,6,6,5,5,4,3,2},{7,7,6,6,5,5,4,4,3},{7,7,7,6,6,5,5,4,3},{7,7,7,6,6,5,5,5,4},{8,8,7,6,6,6,6,5,4},{8,8,7,7,7,6,6,5,5},{8,8,8,7,7,7,6,6,5},{8,8,8,7,7,7,7,6,6},{9,8,8,8,8,7,7,7,6},{9,9,9,8,8,8,7,7,7},{9,9,9,9,8,8,8,8,7},{9,9,9,9,9,9,8,8,8},{9,9,9,9,9,9,9,9,9}
    };
    private static final int[][] SPELLS_ELF={
        {1,0,0,0,0},{2,0,0,0,0},{2,1,0,0,0},{2,2,0,0,0},{2,2,1,0,0},{2,2,2,0,0},{3,2,2,1,0},{3,3,2,2,0},{3,3,3,2,1},{3,3,3,3,2}
    };

    public int rollDie(int sides){return random.nextInt(Math.max(2,sides))+1;}
    public int roll3d6(){return rollDie(6)+rollDie(6)+rollDie(6);}
    public int abilityModifier(int score){if(score<=3)return -3;if(score<=5)return -2;if(score<=8)return -1;if(score<=12)return 0;if(score<=15)return 1;if(score<=17)return 2;return 3;}
    public String signed(int n){return n>=0?"+"+n:String.valueOf(n);}

    public String primeRequisite(String cls){
        if("Clérigo".equals(cls)||"Druida".equals(cls))return "Sabedoria";
        if("Guerreiro".equals(cls)||"Anão".equals(cls))return "Força";
        if("Mago".equals(cls))return "Inteligência";
        if("Ladrão".equals(cls))return "Destreza";
        if("Elfo".equals(cls))return "Força e Inteligência";
        if("Halfling".equals(cls))return "Força e Destreza";
        if("Místico".equals(cls))return "Força e Destreza";
        return "";
    }
    public int hitDie(String cls){if("Guerreiro".equals(cls)||"Anão".equals(cls))return 8;if("Clérigo".equals(cls)||"Elfo".equals(cls)||"Halfling".equals(cls)||"Druida".equals(cls)||"Místico".equals(cls))return 6;return 4;}
    public int hpAfterNine(String cls){if("Guerreiro".equals(cls)||"Místico".equals(cls))return 2;return 1;}
    public int maxLevel(String cls){if("Anão".equals(cls))return 12;if("Elfo".equals(cls))return 10;if("Halfling".equals(cls))return 8;if("Místico".equals(cls))return 16;return 36;}
    public String levelLimitNote(String cls){
        if("Druida".equals(cls))return "Nível máximo 36; a partir do 30º nível é obrigatório desafiar um druida do nível recém-alcançado.";
        if("Anão".equals(cls))return "Nível máximo 12; depois progride pelas categorias de ataque C–M conforme XP.";
        if("Elfo".equals(cls))return "Nível máximo 10; depois progride pelas categorias de ataque C–M conforme XP.";
        if("Halfling".equals(cls))return "Nível máximo 8; depois progride pelas categorias de ataque A–K conforme XP.";
        return "Nível máximo "+maxLevel(cls)+".";
    }
    public String ancestryForClass(String cls){if("Anão".equals(cls)||"Elfo".equals(cls)||"Halfling".equals(cls))return cls;return "Humano";}

    public boolean classRequirementsMet(String cls,int str,int intel,int wis,int dex,int con,String alignment,int clericLevel){
        if("Anão".equals(cls))return con>=9;
        if("Elfo".equals(cls))return intel>=9;
        if("Halfling".equals(cls))return dex>=9&&con>=9;
        if("Druida".equals(cls))return "Neutro".equals(alignment)&&clericLevel>=9;
        if("Místico".equals(cls))return wis>=13&&dex>=13;
        return true;
    }
    public String classRequirementText(String cls){
        if("Anão".equals(cls))return "Constituição 9 ou mais";
        if("Elfo".equals(cls))return "Inteligência 9 ou mais";
        if("Halfling".equals(cls))return "Destreza 9 ou mais e Constituição 9 ou mais";
        if("Druida".equals(cls))return "Alinhamento Neutro e 9º nível prévio como Clérigo";
        if("Místico".equals(cls))return "Sabedoria 13 ou mais e Destreza 13 ou mais";
        return "Sem requisito mínimo";
    }

    public int xpBonus(String cls,int str,int intel,int wis,int dex){
        int score;
        if("Clérigo".equals(cls)||"Druida".equals(cls))score=wis;else if("Mago".equals(cls))score=intel;else if("Ladrão".equals(cls))score=dex;else score=str;
        if("Elfo".equals(cls)){int a=bonusByScore(str),b=bonusByScore(intel);if(str>=13&&intel>=13)return 10;if(str>=13||intel>=13)return 5;return Math.min(a,b);}
        if("Halfling".equals(cls)){if(str>=13&&dex>=13)return 10;if(str>=13||dex>=13)return 5;return Math.min(bonusByScore(str),bonusByScore(dex));}
        if("Místico".equals(cls)){if(score<=5)return -10;if(score<=8)return -5;if(score<=12)return 0;if(score<=15)return 5;return 10;}
        return bonusByScore(score);
    }
    private int bonusByScore(int score){if(score<=5)return -20;if(score<=8)return -10;if(score<=12)return 0;if(score<=15)return 5;return 10;}

    public int[] savingThrows(String cls,int level){
        level=Math.max(1,level);
        if("Clérigo".equals(cls)||"Druida".equals(cls))return copyRow(SAVES_CLERIC,Math.min(8,(level-1)/4));
        if("Guerreiro".equals(cls)||"Místico".equals(cls))return copyRow(SAVES_FIGHTER,Math.min(11,(level-1)/3));
        if("Mago".equals(cls))return copyRow(SAVES_MAGIC,Math.min(7,(level-1)/5));
        if("Ladrão".equals(cls))return copyRow(SAVES_THIEF,Math.min(8,(level-1)/4));
        if("Anão".equals(cls))return copyRow(SAVES_DWARF,Math.min(3,(level-1)/3));
        if("Elfo".equals(cls))return copyRow(SAVES_ELF,level>=10?3:Math.min(2,(level-1)/3));
        if("Halfling".equals(cls))return copyRow(SAVES_HALFLING,level>=7?2:Math.min(1,(level-1)/3));
        return new int[]{14,15,16,17,17};
    }
    private int[] copyRow(int[][] rows,int i){return Arrays.copyOf(rows[Math.max(0,Math.min(rows.length-1,i))],5);}

    public int attackBand(String cls,int level){
        level=Math.max(1,level);
        if("Mago".equals(cls)){
            if(level<=5)return 0;if(level<=10)return 1;if(level<=15)return 2;if(level<=20)return 3;if(level<=25)return 4;if(level<=30)return 5;if(level<=35)return 6;return 7;
        }
        if("Clérigo".equals(cls)||"Druida".equals(cls)||"Ladrão".equals(cls)){
            if(level<=4)return 0;if(level<=8)return 1;if(level<=12)return 2;if(level<=16)return 3;if(level<=20)return 4;if(level<=24)return 5;if(level<=28)return 6;if(level<=32)return 7;if(level<=35)return 8;return 9;
        }
        // Guerreiro, Místico e semi-humanos antes de atingirem a categoria de ataque.
        if(level<=3)return 0;if(level<=6)return 1;if(level<=9)return 2;if(level<=12)return 3;if(level<=15)return 4;if(level<=18)return 5;if(level<=21)return 6;if(level<=24)return 7;if(level<=27)return 8;if(level<=30)return 9;if(level<=33)return 10;return 11;
    }
    /** Resultado necessário no d20 para atingir a CA descendente informada. */
    public int attackTarget(String cls,int level,int targetAc){int raw=19-(2*attackBand(cls,level))-targetAc;return Math.max(2,raw);}
    public int attackExcessDamage(String cls,int level,int targetAc){int raw=19-(2*attackBand(cls,level))-targetAc;return raw<2?2-raw:0;}
    public int attackTargetAc0(String cls,int level){return attackTarget(cls,level,0);}
    /** Categorias de ataque semi-humanas: 1=A ... 13=M. */
    public String attackRankLabel(int rank){if(rank<=0)return "—";int r=Math.max(1,Math.min(13,rank));return String.valueOf((char)('A'+r-1));}
    public int attackRankNumber(String label){if(label==null||label.trim().isEmpty())return 0;char c=Character.toUpperCase(label.trim().charAt(0));return c>='A'&&c<='M'?c-'A'+1:0;}
    /** A categoria A começa entre as bandas normais 2 e 3: contra CA 0 precisa 15; cada categoria seguinte melhora 1. */
    public int attackTarget(String cls,int level,int rank,int targetAc){if(rank<=0)return attackTarget(cls,level,targetAc);int raw=16-Math.max(1,Math.min(13,rank))-targetAc;return Math.max(2,raw);}
    public int attackExcessDamage(String cls,int level,int rank,int targetAc){if(rank<=0)return attackExcessDamage(cls,level,targetAc);int raw=16-Math.max(1,Math.min(13,rank))-targetAc;return raw<2?2-raw:0;}
    public int attackTargetAc0(String cls,int level,int rank){return attackTarget(cls,level,rank,0);}
    public int derivedAttackRank(String cls,int level,int xp){
        xp=Math.max(0,xp);
        if("Halfling".equals(cls)){int[] t={120000,300000,600000,900000,1200000,1500000,1800000,2100000,2400000,2700000,3000000};int r=0;for(int i=0;i<t.length;i++)if(xp>=t[i])r=i+1;return r;}
        if("Elfo".equals(cls)){int[] t={600000,850000,1100000,1350000,1600000,1850000,2100000,2350000,2600000,2850000,3100000};int r=0;for(int i=0;i<t.length;i++)if(xp>=t[i])r=i+3;return Math.min(13,r);}
        if("Anão".equals(cls)){int[] t={660000,800000,1000000,1200000,1400000,1600000,1800000,2000000,2200000,2400000,2600000};int r=0;for(int i=0;i<t.length;i++)if(xp>=t[i])r=i+3;return Math.min(13,r);}
        return 0;
    }

    /** Ataques de monstros usam Dados de Vida, nunca Guerreiro de 1º nível. */
    public double parseMonsterHitDice(String text){
        String x=text==null?"":text.trim().toLowerCase(Locale.US).replace(',', '.');if(x.isEmpty())return 0d;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+-]\\s*\\d+)?").matcher(x);if(!m.find())return 0d;
        double hd=Double.parseDouble(m.group(1));String plus=m.group(2);if(plus!=null&&!plus.trim().isEmpty()){int p=Integer.parseInt(plus.replace(" ",""));hd+=p>0?.5d:p<0?-.5d:0d;}return Math.max(0d,hd);
    }
    public int monsterAttackTargetAc0(String hitDice){
        double hd=parseMonsterHitDice(hitDice);if(hd<=0)return 20;if(hd<=1)return 19;if(hd<=2)return 18;if(hd<=3)return 17;if(hd<=4)return 16;if(hd<=5)return 15;if(hd<=6)return 14;if(hd<=7)return 13;if(hd<=8)return 12;if(hd<=9)return 11;if(hd<=11)return 10;if(hd<=13)return 9;if(hd<=15)return 8;if(hd<=17)return 7;if(hd<=19)return 6;if(hd<=21)return 5;if(hd<=23)return 4;if(hd<=25)return 3;if(hd<=27)return 2;return 2;
    }
    public int monsterAttackTarget(String hitDice,int targetAc){int raw=monsterAttackTargetAc0(hitDice)-targetAc;return Math.max(2,raw);}
    public int monsterAttackExcessDamage(String hitDice,int targetAc){int raw=monsterAttackTargetAc0(hitDice)-targetAc;return raw<2?2-raw:0;}

    private int[] xpTable(String cls){if("Guerreiro".equals(cls))return XP_FIGHTER;if("Mago".equals(cls))return XP_MAGIC;if("Ladrão".equals(cls))return XP_THIEF;if("Anão".equals(cls))return XP_DWARF;if("Elfo".equals(cls))return XP_ELF;if("Halfling".equals(cls))return XP_HALFLING;if("Místico".equals(cls))return XP_MYSTIC;return XP_CLERIC;}
    public int minimumXpForLevel(String cls,int level){int[] t=xpTable(cls);int i=Math.max(0,Math.min(t.length-1,level-1));return t[i];}
    public int nextLevelXp(String cls,int level){int[] t=xpTable(cls);return level>=t.length?-1:t[level];}
    public int levelForXp(String cls,int xp){int[] t=xpTable(cls);int level=1;for(int i=0;i<t.length;i++)if(xp>=t[i])level=i+1;return Math.min(maxLevel(cls),level);}
    public String progressionSummary(String cls,int level,int xp){int min=minimumXpForLevel(cls,level),next=nextLevelXp(cls,level);StringBuilder b=new StringBuilder();b.append("XP ").append(xp).append(" • mínimo do nível ").append(level).append(": ").append(min);if(next>=0)b.append(" • próximo nível: ").append(next).append(" (faltam ").append(Math.max(0,next-xp)).append(')');else b.append(" • nível máximo da classe");int rank=derivedAttackRank(cls,level,xp);if(rank>0)b.append(" • categoria de ataque ").append(attackRankLabel(rank));return b.toString();}
    public int[] spellSlots(String cls,int level){level=Math.max(1,level);if("Mago".equals(cls))return Arrays.copyOf(SPELLS_MAGIC[Math.min(35,level-1)],9);if("Elfo".equals(cls))return Arrays.copyOf(SPELLS_ELF[Math.min(9,level-1)],5);if("Clérigo".equals(cls)||"Druida".equals(cls))return Arrays.copyOf(SPELLS_CLERIC[Math.min(35,level-1)],7);return new int[0];}
    public String spellSlotsDisplay(String cls,int level){int[] slots=spellSlots(cls,level);if(slots.length==0)return "Não lança feitiços pela classe.";StringBuilder b=new StringBuilder();for(int i=0;i<slots.length;i++){if(i>0)b.append(" • ");b.append(i+1).append("º: ").append(slots[i]);}return b.toString();}

    public int armorClass(String armor,boolean shield,int dex){int base=ARMOR_AC.containsKey(armor)?ARMOR_AC.get(armor):9;return base-(shield?1:0)-abilityModifier(dex);}
    public boolean armorAllowed(String cls,String armor,boolean shield){
        boolean none="Sem armadura".equals(armor);if("Mago".equals(cls)||"Místico".equals(cls))return none&&!shield;
        if("Ladrão".equals(cls))return (none||"Armadura de couro".equals(armor))&&!shield;
        if("Druida".equals(cls))return !"Cota de malha".equals(armor)&&!"Armadura de bandas".equals(armor)&&!"Armadura de placas".equals(armor)&&!"Armadura completa".equals(armor);
        return true;
    }
    public String weaponRestriction(String cls){
        if("Clérigo".equals(cls))return "Somente armas sem corte ou ponta";
        if("Mago".equals(cls))return "Adaga apenas";
        if("Ladrão".equals(cls))return "Armas de uma mão e projéteis permitidos ao ladrão";
        if("Halfling".equals(cls)||"Anão".equals(cls))return "Armas compatíveis com o tamanho; sem armas grandes demais";
        if("Druida".equals(cls))return "Armas naturais permitidas ao druida; armadura não metálica";
        return "Qualquer arma permitida pela classe";
    }

    public int encumbranceMosFromKg(double kg){return Math.max(0,(int)Math.round(kg/0.045d));}
    public String loadBand(int mos){if(mos<=400)return "Sem carga";if(mos<=800)return "Carga leve";if(mos<=1200)return "Carga moderada";if(mos<=1600)return "Carga pesada";if(mos<=2400)return "Carga extrema";return "Imóvel";}
    public double normalMoveMeters(int mos){if(mos<=400)return 36;if(mos<=800)return 27;if(mos<=1200)return 18;if(mos<=1600)return 9;if(mos<=2400)return 4.5;return 0;}
    public double encounterMoveMeters(int mos){return normalMoveMeters(mos)/3d;}
    public double runMoveMeters(int mos){return normalMoveMeters(mos);}

    public String canonicalTerrain(String raw){
        String t=norm(raw);
        if(t.contains("road")||t.contains("trail")||t.contains("estrada")||t.contains("trilha"))return "trail";
        if(t.contains("deep sea")||t.contains("shallow water")||t.contains("water")||t.contains("ocean")||t.contains("sea")||t.contains("river")||t.contains("lago")||t.contains("mar")||t.contains("oceano")||t.contains("rio"))return "water";
        if(t.contains("swamp")||t.contains("marsh")||t.contains("pantano"))return "swamp";
        if(t.contains("jungle")||t.contains("selva"))return "jungle";
        if(t.contains("desert")||t.contains("badland")||t.contains("scrub")||t.contains("arid")||t.contains("deserto")||t.contains("terra arida"))return "desert";
        if(t.contains("mountain")||t.contains("montanh"))return "mountain";
        if(t.contains("ice")||t.contains("glacier")||t.contains("snow")||t.contains("gelo")||t.contains("geleira")||t.contains("neve"))return "ice";
        if(t.contains("forest")||t.contains("wood")||t.contains("floresta")||t.contains("bosque")||t.contains("mud")||t.contains("lama"))return "forest";
        if(t.contains("hill")||t.contains("rough")||t.contains("colina")||t.contains("acident"))return "hill";
        return "open";
    }
    public String terrainPt(String raw){String t=canonicalTerrain(raw);if("trail".equals(t))return "trilha ou estrada";if("forest".equals(t))return "floresta, lama ou neve";if("hill".equals(t))return "colina ou terreno acidentado";if("desert".equals(t))return "deserto ou terras áridas";if("mountain".equals(t))return "montanha";if("swamp".equals(t))return "pântano";if("jungle".equals(t))return "selva";if("ice".equals(t))return "gelo ou geleira";if("water".equals(t))return "rio, lago, mar ou oceano";return "aberto, cidade ou planície";}
    /** Modificador diário. Trilha/estrada é cumulativa com lama/neve, conforme a nota da tabela. */
    public double terrainMultiplier(String raw,boolean road,boolean paved){
        String t=canonicalTerrain(raw);double terrain=1d;
        if("forest".equals(t)||"hill".equals(t)||"desert".equals(t))terrain=2d/3d;
        else if("mountain".equals(t)||"swamp".equals(t)||"jungle".equals(t)||"ice".equals(t))terrain=.5d;
        if(!road)return "trail".equals(t)?1.5d:terrain;
        boolean snowOrMud="ice".equals(t)||norm(raw).contains("mud")||norm(raw).contains("lama")||norm(raw).contains("snow")||norm(raw).contains("neve");
        if(paved&&!norm(raw).contains("snow")&&!norm(raw).contains("neve"))terrain=1d;
        else if(!paved&&!snowOrMud)terrain=1d;
        return 1.5d*terrain;
    }

    public boolean isWaterMode(String mode){String m=norm(mode);return m.contains("barco")||m.contains("galeao")||m.contains("navio")||m.contains("jangada")||m.contains("bote")||m.contains("transporte de tropas");}
    public boolean isAirMode(String mode){return norm(mode).contains("aerea")||norm(mode).contains("aereo");}
    public double openKmPerDay(String mode){
        if("A pé — sem carga".equals(mode))return 36;if("A pé — carga leve".equals(mode))return 27;if("A pé — carga pesada".equals(mode))return 18;
        if("Camelo".equals(mode))return 45;if("Elefante".equals(mode))return 36;if("Cavalo de montaria".equals(mode))return 72;if("Burro ou mula".equals(mode))return 36;if("Cavalo de guerra".equals(mode))return 36;if("Cavalo de carga".equals(mode))return 27;if("Boi".equals(mode))return 18;
        if("Barco de rio".equals(mode))return 54;if("Barco a vela".equals(mode))return 108;if("Canoa".equals(mode))return 27;if("Galeão grande — remando".equals(mode))return 27;if("Galeão grande — velejando".equals(mode))return 108;if("Galeão pequeno — remando".equals(mode))return 27;if("Galeão pequeno — velejando".equals(mode))return 135;if("Galeão de guerra — remando".equals(mode))return 18;if("Galeão de guerra — velejando".equals(mode))return 108;if("Bote salva-vidas".equals(mode))return 27;if("Barco viking — remando".equals(mode))return 27;if("Barco viking — velejando".equals(mode))return 135;if("Jangada".equals(mode))return 18;if("Navio a vela pequeno".equals(mode))return 135;if("Navio a vela grande".equals(mode))return 108;if("Transporte de tropas".equals(mode))return 90;
        if("Montaria aérea".equals(mode))return 108;if("Aparato aéreo".equals(mode))return 180;return 36;
    }
    public double waterWeatherMultiplier(int roll2d6){if(roll2d6<=2)return 0;if(roll2d6<=4)return 1d/3d;if(roll2d6==5)return 2d/3d;if(roll2d6<=8)return 1;if(roll2d6==9)return 4d/3d;if(roll2d6==10)return 1.5d;if(roll2d6==11)return 2;return 3;}
    public String waterWeatherLabel(int roll2d6){if(roll2d6<=2)return "sem vento";if(roll2d6==3)return "brisa extremamente leve";if(roll2d6==4)return "brisa leve";if(roll2d6==5)return "brisa moderada";if(roll2d6<=8)return "ventos normais";if(roll2d6==9)return "brisa forte";if(roll2d6==10)return "ventos fortes";if(roll2d6==11)return "ventos extremamente fortes";return "vendaval";}
    public double travelKmPerDay(String mode,String terrain,boolean road,boolean paved,boolean forage,boolean forcedMarch){return travelKmPerDay(mode,terrain,road,paved,forage,forcedMarch,7);}
    public double travelKmPerDay(String mode,String terrain,boolean road,boolean paved,boolean forage,boolean forcedMarch,int windRoll){
        double rate=openKmPerDay(mode);
        if(isWaterMode(mode))rate*=waterWeatherMultiplier(Math.max(2,Math.min(12,windRoll)));
        else if(!isAirMode(mode))rate*=terrainMultiplier(terrain,road,paved);
        if(forage&&!isWaterMode(mode)&&!isAirMode(mode))rate*=2d/3d;
        // O +50% de marcha forçada da Rules Cyclopedia é uma regra de forças militares.
        // Viagem comum de aventureiros não recebe este multiplicador automaticamente.
        return Math.max(0d,rate);
    }
    public int lostChanceOnD6(String terrain,boolean roadRiverOrGuide){if(roadRiverOrGuide)return 0;String t=canonicalTerrain(terrain);if("open".equals(t))return 1;if("swamp".equals(t)||"jungle".equals(t)||"desert".equals(t))return 3;return 2;}
    public int encounterChanceOnD6(String terrain){String t=canonicalTerrain(terrain);if("open".equals(t))return 1;if("swamp".equals(t)||"jungle".equals(t)||"mountain".equals(t))return 3;return 2;}
    public boolean canForage(String terrain){String t=canonicalTerrain(terrain);return !"water".equals(t)&&!"ice".equals(t);}
    public int forageChanceOnD6(String terrain){return canForage(terrain)?3:0;}
    public int longDistancePenalty(int completedTravelDaysWithoutRest){return -Math.max(0,completedTravelDaysWithoutRest/6);}

    public int intelligenceSkillBonusSlots(int intelligence){if(intelligence>=18)return 3;if(intelligence>=16)return 2;if(intelligence>=13)return 1;return 0;}
    public int generalSkillSlots(String cls,int level,int intelligence,int xp){
        int slots=4+intelligenceSkillBonusSlots(intelligence);level=Math.max(1,level);
        if("Halfling".equals(cls)){if(level>=5)slots++;if(xp>=300000)slots++;if(xp>=1500000)slots+=(xp-300000)/1200000;return slots;}
        if("Anão".equals(cls)){if(level>=5)slots++;if(level>=9)slots++;if(xp>=1200000)slots++;if(xp>1200000)slots+=(xp-1200000)/800000;return slots;}
        if("Elfo".equals(cls)){if(level>=5)slots++;if(level>=9)slots++;if(xp>=1350000)slots++;if(xp>1350000)slots+=(xp-1350000)/1000000;return slots;}
        slots+=(level-1)/4;return slots;
    }
    public int weaponOptions(String cls,int level,int xp){
        int[] levels="Guerreiro".equals(cls)?new int[]{1,3,6,9,11,15,19,23,27,30,33,36}:new int[]{1,3,6,9,11,15,23,30,36};
        int options="Guerreiro".equals(cls)?4:2;for(int i=1;i<levels.length;i++)if(level>=levels[i])options++;
        // Semi-humanos já são Básicos em todas as armas permitidas; estas opções
        // continuam existindo para treinamento posterior e custam somente acima do Básico.
        int maxLevel=maxLevel(cls);if(level>=maxLevel&&xp>0){int threshold=experienceAtMaxLevel(cls);if(xp>threshold)options+=(xp-threshold)/200000;}
        return options;
    }
    public int experienceAtMaxLevel(String cls){return minimumXpForLevel(cls,maxLevel(cls));}
    public int weaponMasteryAttackBonus(String mastery,boolean primary){int i=Arrays.asList(MASTERY_LEVELS).indexOf(mastery);if(i<2)return 0;if(i==2)return primary?2:1;if(i==3)return primary?4:2;if(i==4)return primary?6:4;return primary?8:6;}

    private int turnUndeadColumn(int level){if(level<=8)return Math.max(0,level-1);if(level<=10)return 8;if(level<=12)return 9;if(level<=14)return 10;if(level<=16)return 11;if(level<=20)return 12;if(level<=24)return 13;if(level<=28)return 14;if(level<=32)return 15;return 16;}
    public String turnUndeadCode(int clericLevel,String undeadType){int row=Arrays.asList(UNDEAD_TYPES).indexOf(undeadType);if(row<0)return "-";return TURN_UNDEAD[row][turnUndeadColumn(Math.max(1,clericLevel))];}
    public String turnUndeadResult(int clericLevel,String undeadType,int roll2d6){String code=turnUndeadCode(clericLevel,undeadType);if("-".equals(code))return "Sem efeito possível neste nível.";if("A".equals(code))return "Afastamento automático: role 2d6 para os Dados de Vida afetados.";if("D".equals(code))return "Destruição automática: role 2d6 para os Dados de Vida destruídos.";if("D+".equals(code))return "Destruição automática: role 3d6 para os Dados de Vida destruídos.";if("D#".equals(code))return "Destruição automática: role 4d6 para os Dados de Vida destruídos.";int target;try{target=Integer.parseInt(code);}catch(Exception e){return "Resultado inválido.";}return "2d6 = "+roll2d6+" contra "+target+"+ — "+(roll2d6>=target?"AFASTADO: role 2d6 para os Dados de Vida afetados.":"SEM EFEITO.");}
    public String reactionResult(int roll){if(roll<=2)return "Ataque imediato";if(roll<=5)return "Hostil; novo teste ou ataque provável";if(roll<=8)return "Incerto ou neutro; novo teste";if(roll<=11)return "Amigável";return "Entusiasticamente amigável";}
    public String surpriseResult(int partyRoll,int enemyRoll){boolean p=partyRoll<=2,e=enemyRoll<=2;if(p&&e)return "Ambos os lados surpreendidos: ambos perdem um round";if(p)return "Grupo surpreendido: o outro lado recebe um round livre";if(e)return "Oponente surpreendido: o grupo recebe um round livre";return "Nenhum lado surpreendido";}
    public boolean moraleHolds(int roll2d6,int morale){return roll2d6<=morale;}

    public JSONObject mechanicsCatalog(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();try{out.put("system",SYSTEM_ID).put("edition",SYSTEM_LABEL)
                .put("deterministic",new JSONArray().put("attack_tables_by_class_level").put("descending_armor_class").put("saving_throw_tables").put("morale").put("reaction").put("surprise").put("movement_encumbrance").put("exploration_travel").put("lost_checks").put("encounter_checks").put("thief_skills").put("turn_undead").put("weapon_mastery").put("spell_progression").put("class_progression").put("xp").put("treasure_and_equipment"))
                .put("manual_compiled",new JSONArray().put("monster_special_rules").put("domain_management").put("followers").put("war").put("economy"))
                .put("guard","Domínio, seguidores, guerra, economia e efeitos específicos só são executáveis quando um procedimento atômico do manual ativo estiver completo e revisável.");}catch(Exception ignored){AppLog.ignored("MystaraRulesEngine","Falha ao montar catálogo de mecânicas.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject randomCharacter(){/*BR_JSON_WRAP*/try{
        try{int str=roll3d6(),intel=roll3d6(),wis=roll3d6(),dex=roll3d6(),con=roll3d6(),cha=roll3d6();String alignment=ALIGNMENTS[random.nextInt(ALIGNMENTS.length)];List<String> legal=new ArrayList<String>();for(String candidate:CLASSES)if(!"Druida".equals(candidate)&&classRequirementsMet(candidate,str,intel,wis,dex,con,alignment,0))legal.add(candidate);String cls=bestFittingClass(legal,str,intel,wis,dex,con);int level=1;int maxHp=Math.max(1,rollDie(hitDie(cls))+abilityModifier(con));int startingGold=roll3d6()*10;StartingGear gear=buyStartingGear(cls,startingGold);JSONArray skills=randomSkills(cls,level,intel,0);JSONArray mastery=randomMastery(cls,level,0);String spells=randomInitialSpells(cls,level);JSONObject o=new JSONObject();o.put("system",SYSTEM_ID);o.put("name",randomName(ancestryForClass(cls)));o.put("species",ancestryForClass(cls));o.put("class_name",cls);o.put("level",level);o.put("former_cleric_level",0);o.put("alignment",alignment);o.put("str",str);o.put("int_score",intel);o.put("wis",wis);o.put("dex",dex);o.put("con",con);o.put("cha",cha);o.put("hp",maxHp);o.put("max_hp",maxHp);o.put("armor",gear.armor);o.put("shield",gear.shield);o.put("xp",0);o.put("starting_gold_rolled",startingGold);o.put("gold",gear.remainingGold);o.put("encumbrance_mos",gear.encumbranceMos+gear.remainingGold);o.put("equipment_encumbrance_mos",gear.encumbranceMos);o.put("skills_optional",skills);o.put("weapon_mastery",mastery);o.put("equipment",gear.description());o.put("spells",spells);o.put("notes",randomHistory(cls,ancestryForClass(cls),alignment));o.put("automatic_rules_audit","Nível 1; 3d6 em ordem; ouro 3d6×10 gasto item a item; carga calculada em mos; CA descendente; opções de arma e classe verificadas localmente.");o.put("rules_source","Enciclopédia de Regras — criação, classes, equipamento, perícias e especialização em armas");return normalize(o);}catch(Exception ex){return blankCharacter();}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject blankCharacter(){/*BR_JSON_WRAP*/try{try{JSONObject o=new JSONObject();o.put("system",SYSTEM_ID);o.put("name","Aventureiro");o.put("species","Humano");o.put("class_name","Guerreiro");o.put("level",1);o.put("former_cleric_level",0);o.put("alignment","Neutro");o.put("str",10);o.put("int_score",10);o.put("wis",10);o.put("dex",10);o.put("con",10);o.put("cha",10);o.put("hp",1);o.put("max_hp",1);o.put("armor","Sem armadura");o.put("shield",false);o.put("xp",0);o.put("gold",0);o.put("encumbrance_mos",0);o.put("skills_optional",new JSONArray());o.put("weapon_mastery",new JSONArray());o.put("equipment","");o.put("spells","");o.put("notes","");return normalize(o);}catch(Exception ex){return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private String bestFittingClass(List<String> legal,int str,int intel,int wis,int dex,int con){String best=legal.isEmpty()?"Guerreiro":legal.get(0);int bestScore=-999;for(String cls:legal){int score=random.nextInt(5);if("Clérigo".equals(cls))score+=wis;if("Guerreiro".equals(cls)||"Anão".equals(cls))score+=str+con/3;if("Mago".equals(cls))score+=intel;if("Ladrão".equals(cls))score+=dex;if("Elfo".equals(cls))score+=(str+intel)/2;if("Halfling".equals(cls))score+=(dex+con)/2;if("Místico".equals(cls))score+=(wis+dex)/2-2;if(score>bestScore){bestScore=score;best=cls;}}return best;}
    private JSONArray randomSkills(String cls,int level,int intelligence,int xp){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();ArrayList<String> pool=new ArrayList<String>(Arrays.asList(GENERAL_SKILLS));Collections.shuffle(pool,random);int slots=generalSkillSlots(cls,level,intelligence,xp);for(int i=0;i<Math.min(slots,pool.size());i++)out.put(pool.get(i));return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private JSONArray randomMastery(String cls,int level,int xp){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();int count=weaponOptions(cls,level,xp);if(count<=0)return out;ArrayList<String> pool=new ArrayList<String>(Arrays.asList(WEAPONS));if("Mago".equals(cls)){pool.clear();pool.add("Adaga");}else if("Clérigo".equals(cls)){pool.clear();pool.addAll(Arrays.asList("Clava","Cajado","Maça","Martelo de guerra","Funda"));}else if("Druida".equals(cls)){pool.clear();pool.addAll(Arrays.asList("Cajado","Clava","Foice","Funda"));}Collections.shuffle(pool,random);try{for(int i=0;i<Math.min(count,pool.size());i++)out.put(new JSONObject().put("weapon",pool.get(i)).put("level","Básico"));}catch(Exception ignored){AppLog.ignored("MystaraRulesEngine","Falha tolerada; a execução continuou.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private String randomInitialSpells(String cls,int level){if(("Mago".equals(cls)||"Elfo".equals(cls))&&level==1){String[] second={"Encantar Pessoas","Mísseis Mágicos","Sono","Escudo"};return "Livro de feitiços: Ler Magia; "+second[random.nextInt(second.length)]+".";}return defaultSpells(cls,level);}
    private String randomHistory(String cls,String ancestry,String alignment){String[] origins={"criado perto de uma estrada comercial","nascido numa aldeia de fronteira","educado entre artesãos e viajantes","sobrevivente de uma comunidade isolada","aprendiz de um mentor exigente"};String[] traits={"fala pouco e observa tudo","é caloroso, mas desconfiado diante de promessas fáceis","transforma o medo em humor seco","não abandona quem depende de sua palavra","tem curiosidade perigosa por lugares antigos"};String[] goals={"provar seu valor longe de casa","recuperar uma dívida ou herança perdida","mapear rotas que ninguém ousa percorrer","descobrir a verdade sobre um desaparecimento","conquistar recursos para proteger sua comunidade"};String height="Humano".equals(ancestry)?(155+random.nextInt(46))+" cm":"Anão".equals(ancestry)?(115+random.nextInt(31))+" cm":"Elfo".equals(ancestry)?(150+random.nextInt(41))+" cm":(85+random.nextInt(31))+" cm";return "Aparência: "+ancestry+", altura aproximada "+height+", equipamento marcado pelo uso.\nPersonalidade: "+traits[random.nextInt(traits.length)]+".\nHistórico: "+origins[random.nextInt(origins.length)]+".\nObjetivo: "+goals[random.nextInt(goals.length)]+".\nAlinhamento: "+alignment+". Classe: "+cls+".";}
    public JSONObject normalize(JSONObject source){/*BR_JSON_WRAP*/try{
        try{JSONObject o=new JSONObject(source==null?"{}":source.toString());String cls=o.optString("class_name","Guerreiro");int level=Math.max(1,Math.min(maxLevel(cls),o.optInt("level",1)));int str=clampAbility(o.optInt("str",10)),intel=clampAbility(o.optInt("int_score",10)),wis=clampAbility(o.optInt("wis",10)),dex=clampAbility(o.optInt("dex",10)),con=clampAbility(o.optInt("con",10)),cha=clampAbility(o.optInt("cha",10));String alignment=o.optString("alignment","Neutro");if(!Arrays.asList(ALIGNMENTS).contains(alignment))alignment="Neutro";String armor=o.optString("armor",defaultArmor(cls));boolean shield=o.optBoolean("shield",defaultShield(cls));if(!armorAllowed(cls,armor,shield)){armor=defaultArmor(cls);shield=defaultShield(cls);}int mos=Math.max(0,o.optInt("encumbrance_mos",0));int xp=Math.max(0,o.optInt("xp",0));int rank=o.has("attack_rank")?Math.max(0,Math.min(13,o.optInt("attack_rank",0))):derivedAttackRank(cls,level,xp);int maxHp=Math.max(1,o.optInt("max_hp",Math.max(1,hitDie(cls)+abilityModifier(con))));int hp=Math.max(0,Math.min(maxHp,o.optInt("hp",maxHp)));o.put("system",SYSTEM_ID);o.put("species",ancestryForClass(cls));o.put("class_name",cls);o.put("level",level);o.put("alignment",alignment);o.put("str",str);o.put("int_score",intel);o.put("wis",wis);o.put("dex",dex);o.put("con",con);o.put("cha",cha);o.put("armor",armor);o.put("shield",shield);o.put("ac",armorClass(armor,shield,dex));o.put("hp",hp);o.put("max_hp",maxHp);o.put("xp_bonus",xpBonus(cls,str,intel,wis,dex));o.put("xp_minimum_for_level",minimumXpForLevel(cls,level));o.put("xp_next_level",nextLevelXp(cls,level));o.put("progression_summary",progressionSummary(cls,level,xp));o.put("spell_slots",new JSONArray(spellSlots(cls,level)));o.put("spell_slots_display",spellSlotsDisplay(cls,level));o.put("level_limit_note",levelLimitNote(cls));o.put("movement_normal",normalMoveMeters(mos));o.put("movement_encounter",encounterMoveMeters(mos));o.put("attack_rank",rank);o.put("attack_target_ac0",attackTargetAc0(cls,level,rank));o.put("saving_throws",savesJson(cls,level));JSONArray skills=o.optJSONArray("skills_optional");if(skills==null)skills=new JSONArray();o.put("skills_optional",skills);o.put("skills_display",joinJsonArray(skills));JSONArray mastery=o.optJSONArray("weapon_mastery");if(mastery==null)mastery=new JSONArray();o.put("weapon_mastery",mastery);o.put("weapon_mastery_display",masteryDisplay(mastery));o.put("class_features",classFeatures(cls,level));return o;}catch(Exception e){return source==null?new JSONObject():source;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private int clampAbility(int n){return Math.max(3,Math.min(18,n));}
    public JSONObject savesJson(String cls,int level){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();int[] s=savingThrows(cls,level);try{for(int i=0;i<SAVE_NAMES.length;i++)o.put(SAVE_NAMES[i],s[i]);}catch(Exception ignored){AppLog.ignored("MystaraRulesEngine","Falha tolerada; a execução continuou.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public String joinJsonArray(JSONArray a){StringBuilder b=new StringBuilder();if(a==null)return "";for(int i=0;i<a.length();i++){Object v=a.opt(i);String x=v instanceof JSONObject?((JSONObject)v).optString("name",v.toString()):String.valueOf(v);if(x==null||x.trim().isEmpty())continue;if(b.length()>0)b.append(", ");b.append(x.trim());}return b.toString();}
    public String masteryDisplay(JSONArray a){StringBuilder b=new StringBuilder();if(a==null)return "";for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;if(b.length()>0)b.append("; ");b.append(o.optString("weapon","Arma")).append(" — ").append(o.optString("level","Básico"));}return b.toString();}

    public JSONObject thiefSkillsJson(int level){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();int[] row=THIEF_SKILLS[Math.max(0,Math.min(35,level-1))];try{for(int i=0;i<THIEF_SKILL_NAMES.length;i++)o.put(THIEF_SKILL_NAMES[i],row[i]);}catch(Exception ignored){AppLog.ignored("MystaraRulesEngine","Falha tolerada; a execução continuou.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public String classFeatures(String cls,int level){
        if("Clérigo".equals(cls))return "Afastar mortos-vivos pela tabela de 2d6; feitiços clericais a partir do 2º nível; armas sem corte ou ponta.";
        if("Guerreiro".equals(cls))return level>=9?"Especialista em armas e combatente veterano; opções de domínio e seguidores.":"Todas as armas e armaduras; melhor progressão de ataque.";
        if("Mago".equals(cls))return "Livro de feitiços e magia arcana; sem armadura; adaga apenas.";
        if("Ladrão".equals(cls))return "Habilidades percentuais de ladrão; ataque pelas costas; couro apenas, sem escudo.";
        if("Anão".equals(cls))return "Infravisão; detectar construções; resistência anã; progressão de ataque de guerreiro.";
        if("Elfo".equals(cls))return "Guerreiro e usuário de magia; infravisão; detectar portas secretas; imunidade à paralisia de carniçais.";
        if("Halfling".equals(cls))return "Bônus defensivos; precisão com projéteis; esconder-se; resistência halfling.";
        if("Druida".equals(cls))return "Clérigo neutro convertido a partir do 9º nível; feitiços clericais e druídicos, sem afastar mortos-vivos; desafios obrigatórios do 30º ao 36º nível.";
        if("Místico".equals(cls))return "Combate desarmado, bônus de CA, movimento e habilidades místicas; sem armadura.";
        return "";
    }
    public String defaultArmor(String cls){if("Mago".equals(cls)||"Místico".equals(cls))return "Sem armadura";if("Ladrão".equals(cls)||"Druida".equals(cls))return "Armadura de couro";return "Cota de malha";}
    public boolean defaultShield(String cls){return !"Mago".equals(cls)&&!"Místico".equals(cls)&&!"Ladrão".equals(cls);}
    public String defaultEquipment(String cls,String armor,boolean shield,int gold){String weapon="Espada normal";if("Clérigo".equals(cls))weapon="Maça e funda";else if("Mago".equals(cls))weapon="Adaga";else if("Ladrão".equals(cls))weapon="Espada curta, adaga e ferramentas de ladrão";else if("Místico".equals(cls))weapon="Cajado";else if("Druida".equals(cls))weapon="Cajado e foice";else if("Halfling".equals(cls))weapon="Espada curta e funda";else if("Anão".equals(cls))weapon="Machado de batalha e martelo";else if("Elfo".equals(cls))weapon="Espada normal e arco longo";String defense="Sem armadura".equals(armor)?"sem armadura":armor.toLowerCase(new Locale("pt","BR"));return defense+(shield?", escudo":"")+", "+weapon+", mochila, 7 rações, cantil, corda de 15 m, 6 tochas e pederneira. Fundo inicial antes de conferir compras: "+gold+" po.";}
    public String defaultSpells(String cls,int level){if("Mago".equals(cls)||"Elfo".equals(cls))return level==1?"Livro de feitiços: Ler Magia; escolha com o Mestre um segundo feitiço entre Encantar Pessoas, Mísseis Mágicos, Sono ou Escudo.":"Feitiços conforme a tabela da classe e o livro de feitiços.";if("Clérigo".equals(cls))return level<2?"Nenhum feitiço no 1º nível.":"Feitiços clericais conforme a tabela da classe.";if("Druida".equals(cls))return "Feitiços druídicos conforme a tabela da classe.";return "";}

    public String randomName(String ancestry){String[] human={"Aldric","Beatriz","Corwin","Daria","Edric","Flávia","Garrick","Helena","Ilyan","Joana"};String[] dwarf={"Borin","Dagna","Fargrim","Hilda","Korin","Ragna"};String[] elf={"Aelar","Belwen","Caelynn","Erevan","Lethiel","Sylvar"};String[] half={"Bungo","Cora","Dodo","Esme","Milo","Pippa"};String[] pool="Anão".equals(ancestry)?dwarf:("Elfo".equals(ancestry)?elf:("Halfling".equals(ancestry)?half:human));return pool[random.nextInt(pool.length)];}

    private StartingGear buyStartingGear(String cls,int gold){
        StartingGear g=new StartingGear(gold);String weapon="Espada normal";int weaponCost=10,weaponMos=60;if("Clérigo".equals(cls)){weapon="Maça";weaponCost=5;weaponMos=30;}else if("Mago".equals(cls)){weapon="Adaga";weaponCost=3;weaponMos=10;}else if("Ladrão".equals(cls)){weapon="Adaga";weaponCost=3;weaponMos=10;}else if("Místico".equals(cls)){weapon="Cajado";weaponCost=5;weaponMos=40;}else if("Halfling".equals(cls)||"Elfo".equals(cls)){weapon="Espada curta";weaponCost=7;weaponMos=30;}else if("Anão".equals(cls)){weapon="Machado de mão";weaponCost=7;weaponMos=60;}
        if("Mago".equals(cls)||"Místico".equals(cls)){g.buy(weapon,weaponCost,weaponMos);g.armor="Sem armadura";}
        else if("Ladrão".equals(cls)){g.buy("Ferramentas de Ladrão",25,10);g.buy(weapon,weaponCost,weaponMos);if(g.canBuy(20)){g.buy("Armadura de couro",20,200);g.armor="Armadura de couro";}else g.armor="Sem armadura";}
        else{
            int reserve=weaponCost;boolean shieldAllowed=defaultShield(cls);
            if(g.remainingGold>=40+reserve+(shieldAllowed?10:0)){g.buy("Cota de malha",40,400);g.armor="Cota de malha";}
            else if(g.remainingGold>=30+reserve+(shieldAllowed?10:0)){g.buy("Armadura de escamas",30,300);g.armor="Armadura de escamas";}
            else if(g.remainingGold>=20+reserve){g.buy("Armadura de couro",20,200);g.armor="Armadura de couro";}
            else g.armor="Sem armadura";
            g.buy(weapon,weaponCost,weaponMos);if(shieldAllowed&&g.canBuy(10)){g.buy("Escudo",10,100);g.shield=true;}
        }
        g.buyIfPossible("Mochila",5,20);g.buyIfPossible("Ração comum (1 semana)",5,200);g.buyIfPossible("Pederneira",3,5);g.buyIfPossible("Cantil cheio",1,30);g.buyIfPossible("Corda de 15 m",1,50);g.buyIfPossible("6 tochas",1,120);
        return g;
    }
    private static final class StartingGear{
        final int startingGold;int remainingGold,encumbranceMos;String armor="Sem armadura";boolean shield=false;final ArrayList<String> items=new ArrayList<String>();
        StartingGear(int gold){startingGold=Math.max(0,gold);remainingGold=startingGold;}
        boolean canBuy(int cost){return remainingGold>=cost;}
        boolean buy(String name,int cost,int mos){if(!canBuy(cost))return false;remainingGold-=cost;encumbranceMos+=Math.max(0,mos);items.add(name+" ("+cost+" po, "+mos+" mos)");return true;}
        void buyIfPossible(String name,int cost,int mos){buy(name,cost,mos);}
        String description(){StringBuilder b=new StringBuilder();for(int i=0;i<items.size();i++){if(i>0)b.append(", ");b.append(items.get(i));}b.append(". Ouro rolado: ").append(startingGold).append(" po; gasto: ").append(startingGold-remainingGold).append(" po; restante: ").append(remainingGold).append(" po. Carga dos itens: ").append(encumbranceMos).append(" mos; moedas restantes: ").append(remainingGold).append(" mos.");return b.toString();}
    }


    private static String norm(String s){String n=s==null?"":java.text.Normalizer.normalize(s.toLowerCase(Locale.US),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replace('_',' ');}
}
