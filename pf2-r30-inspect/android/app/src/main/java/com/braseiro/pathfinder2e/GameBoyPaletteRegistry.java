package com.braseiro.pathfinder2e;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Biblioteca das 120 paletas Game Boy HCL Sequentials interpretadas da referência visual aprovada. */
public final class GameBoyPaletteRegistry {
    public static final class Palette {
        public final String code,name,mode,technicalId;
        public final int[] colors;
        Palette(String code,String name,int c0,int c1,int c2,int c3){this.code=code;this.name=name;this.mode="gb_"+code.toLowerCase();this.technicalId="HCL_Seque_"+code;this.colors=new int[]{c0,c1,c2,c3};}
        public String label(){return "Game Boy "+code+" — "+name;}
    }

    private static int rgb(int r,int g,int b){return Color.rgb(r,g,b);}
    private static final Palette[] ALL=new Palette[]{
        new Palette("01A","Névoa de Gelo",rgb(241,241,240), rgb(198,224,232), rgb(167,196,219), rgb(140,162,198)),
        new Palette("01B","Ardósia Portátil",rgb(140,162,198), rgb(126,125,178), rgb(115,79,149), rgb(108,6,118)),
        new Palette("02A","Sonho Lilás",rgb(241,239,244), rgb(234,212,244), rgb(223,177,220), rgb(209,138,186)),
        new Palette("02B","Violeta Elétrica",rgb(210,138,187), rgb(193,98,147), rgb(163,53,92), rgb(127,4,22)),
        new Palette("03A","Rosa de Algodão",rgb(172,181,226), rgb(177,159,215), rgb(182,134,197), rgb(189,119,186)),
        new Palette("03B","Rubi do Cartucho",rgb(188,112,185), rgb(188,88,159), rgb(180,65,125), rgb(170,58,88)),
        new Palette("04A","Sakura Pálida",rgb(243,218,216), rgb(238,183,191), rgb(209,148,177), rgb(197,122,171)),
        new Palette("04B","Ameixa Real",rgb(194,121,169), rgb(168,96,164), rgb(135,74,155), rgb(94,63,149)),
        new Palette("05A","Menta Solar",rgb(241,239,212), rgb(194,225,187), rgb(118,192,184), rgb(56,183,189)),
        new Palette("05B","Ciano Turbo",rgb(52,177,187), rgb(83,142,191), rgb(126,85,168), rgb(134,45,117)),
        new Palette("06A","Brisa de Menta",rgb(241,240,228), rgb(223,239,208), rgb(179,229,198), rgb(119,205,188)),
        new Palette("06B","Lagoa Pixel",rgb(119,205,187), rgb(53,170,181), rgb(7,118,160), rgb(47,51,132)),
        new Palette("07A","Sálvia Clara",rgb(248,246,233), rgb(237,240,212), rgb(215,227,188), rgb(182,206,160)),
        new Palette("07B","Floresta 8-Bit",rgb(181,206,160), rgb(140,180,128), rgb(88,148,94), rgb(6,111,59)),
        new Palette("08A","Areia Dourada",rgb(242,240,227), rgb(234,219,164), rgb(216,187,122), rgb(202,150,89)),
        new Palette("08B","Cobre Imperial",rgb(203,150,87), rgb(182,112,60), rgb(154,68,38), rgb(127,6,23)),
        new Palette("09A","Creme da Manhã",rgb(224,227,187), rgb(238,219,127), rgb(238,196,110), rgb(235,171,105)),
        new Palette("09B","Damasco Quente",rgb(232,170,101), rgb(216,134,93), rgb(185,83,79), rgb(144,16,62)),
        new Palette("10A","Ouro Retro",rgb(224,226,186), rgb(227,205,78), rgb(230,175,45), rgb(235,164,85)),
        new Palette("10B","Pêssego Neon",rgb(232,155,57), rgb(228,128,78), rgb(219,97,99), rgb(215,82,113)),
        new Palette("11A","Mostarda Solar",rgb(240,240,240), rgb(248,200,183), rgb(250,191,131), rgb(222,186,80)),
        new Palette("11B","Lima Pixel",rgb(221,186,78), rgb(181,183,31), rgb(126,175,26), rgb(45,166,65)),
        new Palette("12A","Pergaminho Musgo",rgb(224,224,224), rgb(238,212,190), rgb(217,198,155), rgb(189,182,121)),
        new Palette("12B","Verde Bosque",rgb(189,183,120), rgb(152,168,87), rgb(101,147,56), rgb(10,125,36)),
        new Palette("13A","Limão Ácido",rgb(250,226,52), rgb(149,213,77), rgb(0,185,126), rgb(12,156,150)),
        new Palette("13B","Índigo Tropical",rgb(4,155,149), rgb(5,112,148), rgb(57,62,123), rgb(77,5,87)),
        new Palette("14A","Citrino Pop",rgb(217,251,69), rgb(232,188,4), rgb(231,133,84), rgb(213,89,118)),
        new Palette("14B","Magenta Arcade",rgb(207,81,116), rgb(169,22,137), rgb(110,9,139), rgb(9,27,140)),
        new Palette("15A","Sol de Pêssego",rgb(249,249,154), rgb(241,180,59), rgb(238,110,83), rgb(199,72,118)),
        new Palette("15B","Uva Obscura",rgb(194,53,112), rgb(130,21,111), rgb(60,19,77), rgb(4,4,4)),
        new Palette("16A","Flamingo Doce",rgb(251,244,235), rgb(235,166,116), rgb(232,94,75), rgb(195,16,98)),
        new Palette("16B","Framboesa Noturna",rgb(195,16,98), rgb(135,1,97), rgb(73,1,71), rgb(7,7,7)),
        new Palette("17A","Céu Pastel",rgb(223,246,225), rgb(133,210,187), rgb(0,172,174), rgb(6,128,169)),
        new Palette("17B","Azul Abissal",rgb(3,128,168), rgb(66,75,123), rgb(58,34,68), rgb(7,7,7)),
        new Palette("18A","Espuma do Mar",rgb(208,250,212), rgb(163,218,188), rgb(122,184,168), rgb(97,158,152)),
        new Palette("18B","Oceano Profundo",rgb(89,156,151), rgb(58,125,132), rgb(32,93,112), rgb(21,64,95)),
        new Palette("19A","Jade Pálido",rgb(222,239,228), rgb(175,215,195), rgb(136,187,172), rgb(102,165,154)),
        new Palette("19B","Chá de Hortelã",rgb(99,163,152), rgb(60,142,135), rgb(2,116,118), rgb(14,94,104)),
        new Palette("20A","Névoa Verde",rgb(197,224,189), rgb(152,205,166), rgb(103,172,148), rgb(103,168,149)),
        new Palette("20B","Pinheiro Frio",rgb(79,163,144), rgb(53,134,128), rgb(35,106,112), rgb(44,87,97)),
        new Palette("21A","Gelo Boreal",rgb(208,236,233), rgb(170,216,216), rgb(135,193,196), rgb(103,167,179)),
        new Palette("21B","Aço Celeste",rgb(103,167,180), rgb(77,142,165), rgb(57,114,142), rgb(46,87,118)),
        new Palette("22A","Broto de Primavera",rgb(182,238,176), rgb(141,229,174), rgb(86,214,172), rgb(47,195,172)),
        new Palette("22B","Turquesa Viva",rgb(46,196,173), rgb(3,174,172), rgb(8,152,162), rgb(34,129,153)),
        new Palette("23A","Sálvia Neon",rgb(211,241,163), rgb(154,213,147), rgb(99,181,138), rgb(63,157,134)),
        new Palette("23B","Petróleo Escuro",rgb(53,156,133), rgb(3,127,122), rgb(6,95,103), rgb(17,66,82)),
        new Palette("24A","Limonada Gelada",rgb(246,252,175), rgb(184,227,163), rgb(121,194,156), rgb(73,175,153)),
        new Palette("24B","Esmeralda Clara",rgb(69,172,152), rgb(20,145,149), rgb(29,111,136), rgb(55,82,124)),
        new Palette("25A","Limão Relâmpago",rgb(232,235,94), rgb(162,207,104), rgb(92,177,117), rgb(29,165,127)),
        new Palette("25B","Verde Arcade",rgb(22,161,125), rgb(4,139,130), rgb(17,111,118), rgb(57,92,109)),
        new Palette("26A","Pêssego Leve",rgb(247,220,195), rgb(244,203,168), rgb(246,182,142), rgb(239,161,115)),
        new Palette("26B","Coral de Verão",rgb(240,160,116), rgb(240,136,90), rgb(238,111,77), rgb(233,80,66)),
        new Palette("27A","Baunilha Quente",rgb(251,245,181), rgb(248,219,151), rgb(244,194,127), rgb(241,169,115)),
        new Palette("27B","Rosa Festa",rgb(243,168,115), rgb(239,141,111), rgb(234,112,121), rgb(227,84,131)),
        new Palette("28A","Rosa Antigo",rgb(252,197,199), rgb(235,165,171), rgb(211,130,145), rgb(204,105,127)),
        new Palette("28B","Vinho de Pixel",rgb(202,100,125), rgb(173,74,104), rgb(137,53,83), rgb(104,40,66)),
        new Palette("29A","Blush Real",rgb(242,221,190), rgb(235,186,141), rgb(217,150,107), rgb(216,126,109)),
        new Palette("29B","Rosa Framboesa",rgb(213,120,96), rgb(181,93,94), rgb(149,68,86), rgb(121,53,78)),
        new Palette("30A","Pó de Rosas",rgb(239,206,164), rgb(234,182,137), rgb(228,154,112), rgb(225,134,107)),
        new Palette("30B","Cereja Veludo",rgb(219,131,101), rgb(217,107,99), rgb(197,83,101), rgb(181,81,106)),
        new Palette("31A","Dourado Claro",rgb(232,215,153), rgb(232,196,121), rgb(237,176,94), rgb(236,155,76)),
        new Palette("31B","Tangerina Viva",rgb(238,154,77), rgb(244,133,78), rgb(243,104,91), rgb(238,80,110)),
        new Palette("32A","Lavanda de Vidro",rgb(243,225,247), rgb(226,197,238), rgb(203,173,222), rgb(179,149,209)),
        new Palette("32B","Ametista Doce",rgb(180,149,210), rgb(155,130,199), rgb(129,108,181), rgb(102,94,161)),
        new Palette("33A","Orquídea Branda",rgb(245,219,216), rgb(239,183,191), rgb(218,149,178), rgb(198,120,170)),
        new Palette("33B","Ameixa de Festa",rgb(199,119,171), rgb(175,93,170), rgb(135,74,156), rgb(91,65,138)),
        new Palette("34A","Creme Rosado",rgb(239,228,152), rgb(244,193,131), rgb(236,155,136), rgb(232,126,151)),
        new Palette("34B","Rosa Chiclete",rgb(229,123,150), rgb(206,102,166), rgb(164,86,165), rgb(116,85,163)),
        new Palette("35A","Rosé Suave",rgb(238,198,206), rgb(230,167,190), rgb(206,134,166), rgb(207,129,168)),
        new Palette("35B","Malva Pop",rgb(202,111,163), rgb(173,81,145), rgb(138,56,122), rgb(117,52,111)),
        new Palette("36A","Damasco Suave",rgb(251,216,161), rgb(249,179,129), rgb(249,137,112), rgb(244,96,114)),
        new Palette("36B","Melancia Neon",rgb(244,95,113), rgb(214,58,122), rgb(170,38,117), rgb(125,35,104)),
        new Palette("37A","Manga Pastel",rgb(227,209,153), rgb(238,169,97), rgb(232,121,103), rgb(222,72,132)),
        new Palette("37B","Fúcsia Quente",rgb(224,71,133), rgb(191,36,156), rgb(140,22,155), rgb(76,34,147)),
        new Palette("38A","Areia Rosada",rgb(232,226,200), rgb(219,193,164), rgb(198,157,128), rgb(189,126,113)),
        new Palette("38B","Cacau Rosé",rgb(188,122,103), rgb(161,86,91), rgb(121,55,75), rgb(86,33,61)),
        new Palette("39A","Luz de Ouro",rgb(251,252,197), rgb(246,228,156), rgb(242,188,78), rgb(240,148,6)),
        new Palette("39B","Âmbar Ardente",rgb(238,145,1), rgb(233,86,4), rgb(185,30,14), rgb(126,11,41)),
        new Palette("40A","Mel Pixel",rgb(251,250,222), rgb(248,233,178), rgb(242,191,96), rgb(241,165,9)),
        new Palette("40B","Ouro de Outono",rgb(234,161,3), rgb(223,114,5), rgb(166,72,13), rgb(109,56,44)),
        new Palette("41A","Salmão Claro",rgb(252,244,235), rgb(251,226,193), rgb(252,191,140), rgb(246,148,93)),
        new Palette("41B","Vermelho Brasa",rgb(249,147,92), rgb(236,96,53), rgb(192,36,53), rgb(135,5,47)),
        new Palette("42A","Laranja Sorbet",rgb(253,245,236), rgb(251,221,191), rgb(249,187,128), rgb(240,146,33)),
        new Palette("42B","Abóbora Intensa",rgb(242,146,32), rgb(232,100,6), rgb(182,68,6), rgb(129,46,14)),
        new Palette("43A","Pera Gelada",rgb(254,254,226), rgb(234,244,202), rgb(192,222,160), rgb(136,197,121)),
        new Palette("43B","Folha Viva",rgb(133,197,118), rgb(68,164,95), rgb(14,117,77), rgb(6,69,52)),
        new Palette("44A","Menta Leve",rgb(249,253,222), rgb(214,241,206), rgb(149,211,187), rgb(31,187,173)),
        new Palette("44B","Aqua Profunda",rgb(24,184,172), rgb(5,148,173), rgb(15,93,154), rgb(40,29,97)),
        new Palette("45A","Coral Brando",rgb(251,243,238), rgb(251,212,195), rgb(250,167,143), rgb(251,121,101)),
        new Palette("45B","Rosa Coral",rgb(243,116,95), rgb(232,47,53), rgb(167,6,53), rgb(113,15,44)),
        new Palette("46A","Rosa Festival",rgb(250,245,242), rgb(249,213,207), rgb(249,166,181), rgb(228,113,166)),
        new Palette("46B","Magenta Roxa",rgb(229,112,166), rgb(203,47,165), rgb(133,19,133), rgb(74,3,99)),
        new Palette("47A","Lavanda Neblina",rgb(246,245,250), rgb(220,211,231), rgb(208,164,214), rgb(213,106,187)),
        new Palette("47B","Roxo Imperial",rgb(215,105,189), rgb(220,5,142), rgb(173,5,61), rgb(99,22,5)),
        new Palette("48A","Marfim Suave",rgb(251,251,254), rgb(229,227,242), rgb(197,193,217), rgb(165,156,199)),
        new Palette("48B","Azul Lavado",rgb(165,156,200), rgb(131,117,180), rgb(98,70,160), rgb(63,31,122)),
        new Palette("49A","Céu de Algodão",rgb(252,244,251), rgb(230,219,235), rgb(186,187,215), rgb(120,165,204)),
        new Palette("49B","Turquesa Lagunar",rgb(116,162,200), rgb(5,145,171), rgb(4,110,104), rgb(11,70,53)),
        new Palette("50A","Gelo Polar",rgb(244,245,249), rgb(216,220,231), rgb(176,184,209), rgb(140,163,204)),
        new Palette("50B","Azul Marinho Portátil",rgb(133,159,196), rgb(74,131,189), rgb(13,97,145), rgb(40,72,100)),
        new Palette("51A","Hortelã Clara",rgb(246,250,244), rgb(220,239,214), rgb(179,218,169), rgb(129,188,122)),
        new Palette("51B","Verde Selvagem",rgb(130,189,123), rgb(69,160,72), rgb(34,115,51), rgb(5,70,26)),
        new Palette("52A","Menta Pastel",rgb(240,250,250), rgb(219,239,238), rgb(172,221,212), rgb(115,193,171)),
        new Palette("52B","Turquesa de Cartucho",rgb(113,195,171), rgb(10,163,114), rgb(19,118,62), rgb(32,70,26)),
        new Palette("53A","Brisa de Ciano",rgb(245,247,235), rgb(219,240,217), rgb(166,222,198), rgb(88,197,185)),
        new Palette("53B","Azul Safira",rgb(84,198,186), rgb(7,166,187), rgb(18,115,175), rgb(53,56,127)),
        new Palette("54A","Nuvem Lilás",rgb(238,248,249), rgb(208,227,234), rgb(168,190,209), rgb(146,153,198)),
        new Palette("54B","Roxo de Crepúsculo",rgb(143,150,194), rgb(133,105,175), rgb(117,47,126), rgb(85,9,72)),
        new Palette("55A","Gelo de Inverno",rgb(241,246,249), rgb(211,228,237), rgb(166,190,213), rgb(130,170,208)),
        new Palette("55B","Azul Profundo",rgb(126,167,202), rgb(84,135,191), rgb(53,94,157), rgb(59,70,119)),
        new Palette("56A","Ouro Antigo",rgb(251,253,202), rgb(233,215,139), rgb(224,168,70), rgb(214,120,9)),
        new Palette("56B","Ferrugem Pixel",rgb(214,119,8), rgb(173,76,65), rgb(115,38,61), rgb(29,13,20)),
        new Palette("57A","Deserto Digital",rgb(252,232,234), rgb(246,191,173), rgb(197,159,120), rgb(148,132,105)),
        new Palette("57B","Obsidiana 8-Bit",rgb(146,131,101), rgb(97,96,78), rgb(54,57,50), rgb(4,5,4)),
        new Palette("58A","Aurora Turquesa",rgb(176,241,245), rgb(82,223,184), rgb(119,189,100), rgb(155,153,27)),
        new Palette("58B","Carnaval Pixel",rgb(155,152,6), rgb(169,110,11), rgb(162,62,78), rgb(142,17,108)),
        new Palette("59A","Rosa Botânica",rgb(250,205,238), rgb(244,165,165), rgb(192,155,37), rgb(100,142,64)),
        new Palette("59B","Selva Noturna",rgb(90,139,43), rgb(4,111,85), rgb(13,72,95), rgb(37,24,92)),
        new Palette("60A","Mostarda de Aço",rgb(248,229,68), rgb(207,192,106), rgb(160,155,120), rgb(143,143,143)),
        new Palette("60B","Noite de Ardósia",rgb(125,125,126), rgb(85,93,115), rgb(35,61,107), rgb(19,41,86))
    };

    private GameBoyPaletteRegistry(){}
    public static int count(){return ALL.length;}
    public static boolean isGameBoyMode(String mode){return mode!=null&&mode.startsWith("gb_");}
    public static Palette byMode(String mode){if(mode==null)return null;for(Palette p:ALL)if(p.mode.equals(mode))return p;return null;}
    public static Palette byLabel(String label){if(label==null)return null;for(Palette p:ALL)if(p.label().equals(label))return p;return null;}
    public static Palette byCode(String code){if(code==null)return null;for(Palette p:ALL)if(p.code.equalsIgnoreCase(code))return p;return null;}
    public static String labelForMode(String mode){Palette p=byMode(mode);return p==null?null:p.label();}
    public static String modeForLabel(String label){Palette p=byLabel(label);return p==null?null:p.mode;}
    public static String[] labels(){String[] out=new String[ALL.length];for(int i=0;i<ALL.length;i++)out[i]=ALL[i].label();return out;}
    public static List<Palette> all(){return new ArrayList<Palette>(Arrays.asList(ALL));}

    /** Quatro swatches originais, na ordem da referência. */
    public static int[] swatches(String mode){Palette p=byMode(mode);return p==null?new int[0]:p.colors.clone();}

    /**
     * Converte a paleta de quatro cores em tokens de UI. Paletas de baixo contraste são
     * estendidas apenas por mistura com preto/branco para manter legibilidade; os swatches
     * exibidos ao usuário continuam sendo exatamente as quatro cores interpretadas.
     * Ordem: BG,PANEL,PANEL2,GOLD,TEXT,MUTED,RED,GREEN,HEADER_BG,STATUS_BG,CARD_STROKE,INPUT_BG,INPUT_STROKE,HINT.
     */
    public static int[] uiColors(String mode){Palette p=byMode(mode);return p==null?new int[0]:uiColors(p);}
    public static int[] uiColors(Palette p){
        Integer[] sorted=new Integer[]{p.colors[0],p.colors[1],p.colors[2],p.colors[3]};
        Arrays.sort(sorted,new Comparator<Integer>(){public int compare(Integer a,Integer b){return Double.compare(luminance(b),luminance(a));}});
        int light=sorted[0], lightMid=sorted[1], darkMid=sorted[2], dark=sorted[3];
        double cr=contrast(light,dark);
        int bg=dark,panel=darkMid,panel2=darkMid,text=light,accent=lightMid,muted=lightMid;
        if(cr<3.25d){bg=mix(dark,Color.BLACK,0.58f);panel=mix(dark,Color.BLACK,0.38f);panel2=mix(darkMid,Color.BLACK,0.20f);text=mix(light,Color.WHITE,0.72f);}
        else if(cr<4.5d){bg=mix(dark,Color.BLACK,0.28f);panel=mix(darkMid,Color.BLACK,0.16f);text=mix(light,Color.WHITE,0.30f);}
        int header=mix(bg,Color.BLACK,0.16f),status=mix(bg,Color.BLACK,0.26f),stroke=accent,input=panel,inputStroke=accent,hint=muted;
        int semanticDark=darkMid;
        return new int[]{bg,panel,panel2,accent,text,muted,semanticDark,semanticDark,header,status,stroke,input,inputStroke,hint};
    }
    private static int mix(int a,int b,float t){t=Math.max(0f,Math.min(1f,t));int r=Math.round(Color.red(a)*(1f-t)+Color.red(b)*t),g=Math.round(Color.green(a)*(1f-t)+Color.green(b)*t),bl=Math.round(Color.blue(a)*(1f-t)+Color.blue(b)*t);return Color.rgb(r,g,bl);}
    private static double luminance(int color){double r=channel(Color.red(color)/255d),g=channel(Color.green(color)/255d),b=channel(Color.blue(color)/255d);return .2126d*r+.7152d*g+.0722d*b;}
    private static double channel(double c){return c<=.04045d?c/12.92d:Math.pow((c+.055d)/1.055d,2.4d);}
    public static double contrast(int a,int b){double la=luminance(a),lb=luminance(b);return (Math.max(la,lb)+.05d)/(Math.min(la,lb)+.05d);}
}
