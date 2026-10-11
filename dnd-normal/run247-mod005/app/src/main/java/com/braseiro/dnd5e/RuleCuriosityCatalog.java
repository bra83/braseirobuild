package com.braseiro.dnd5e;

/** Reviewed educational summaries only. Printed book pages are not PDF page indices. */
public final class RuleCuriosityCatalog {
    public static final String BOOK = "Dungeon Master's Guide (2024)";
    public static final String EDITION = "D&D 2024";
    public static final String DRIVE_FILE_ID = "1FDZwdunnLxO3FG85N-hMKEG9UTwmWBEl";
    public static final String DRIVE_URL = "https://drive.google.com/file/d/" + DRIVE_FILE_ID + "/view";

    public static final class Entry {
        public final String id, category, title, explanation, application;
        public final int printedPage, pdfPage;
        private Entry(String id, String category, String title, String explanation, String application, int printedPage) {
            this.id=id;this.category=category;this.title=title;this.explanation=explanation;
            this.application=application;this.printedPage=printedPage;this.pdfPage=printedPage+4;
        }
    }
    private static Entry e(String id,String cat,String title,String explanation,String application,int p){
        return new Entry(id,cat,title,explanation,application,p);
    }
    private static final Entry[] ITEMS = {
        e("dmg24-general-exception","regras","Uma exceção pode vencer uma regra geral",
          "Quando uma característica de classe, magia, objeto ou criatura contradiz uma regra geral, a exceção específica prevalece.",
          "Antes de negar uma ação, confira a descrição exata da característica que o personagem possui.",9),
        e("dmg24-check-outcome","testes","Nem toda tentativa pede rolagem",
          "O Mestre decide quando um teste d20 é adequado à intenção declarada e às possíveis consequências. Um dado não substitui toda decisão narrativa.",
          "Explique o objetivo antes de pedir um teste, e registre o que acontece na falha ou no sucesso.",26),
        e("dmg24-proficiency","perícias","Uma proficiência pode justificar uma abordagem diferente",
          "Ao decidir um teste, o Mestre pode considerar perícias ou ferramentas adequadas ao modo como o personagem tenta resolver o problema.",
          "Descreva como usaria sua ferramenta ou conhecimento em vez de buscar apenas o maior bônus da ficha.",27),
        e("dmg24-trying-again","exploração","Repetir um teste nem sempre é válido",
          "Depois de uma falha, uma nova tentativa pode ser impossível ou simplesmente custar tempo, dependendo da tarefa e de sua consequência.",
          "Em uma fechadura, pergunte se existe consequência por gastar mais tempo; não repita dados gratuitamente.",27),
        e("dmg24-improvising","mestre","Uma impossibilidade ainda pode criar outra oportunidade",
          "Na improvisação, o Mestre pode acolher ideias dos jogadores ou oferecer alternativas viáveis quando algo não pode acontecer como proposto.",
          "Se um plano não funcionar, procure um caminho coerente sem conceder sucesso automático.",30),
        e("dmg24-social-help","interação","Ajudar numa conversa exige contribuição",
          "Na interação social, a ação Ajudar fica mais convincente quando o segundo personagem participa da conversa ou explica como colabora.",
          "O companheiro pode citar evidências, traduzir um idioma ou apoiar o argumento, em vez de conceder bônus sem motivo.",32),
        e("dmg24-exploration-time","exploração","Exploração também exige administrar o tempo",
          "O Mestre pode acompanhar passagem de tempo, deslocamento e ações relevantes durante a exploração em vez de tratar cada investigação como instantânea.",
          "Considere patrulhas, luz, suprimentos e pressão do cenário quando uma busca demora.",33),
        e("dmg24-journey-stages","viagem","Viagem pode ocorrer em etapas significativas",
          "A viagem pode ser dividida em etapas: ritmo, narrativa, dificuldades, consumo de recursos e avanço no mapa.",
          "O guia define o ritmo, o grupo acompanha suprimentos e o mundo reage aos deslocamentos.",37),
        e("dmg24-weather","viagem","O clima pode mudar a experiência de viagem",
          "O livro apresenta procedimentos para gerar temperatura, vento e precipitação durante uma etapa, adaptáveis ao terreno e à estação.",
          "Um temporal pode mudar rastros e visibilidade; seus efeitos mecânicos só devem ser aplicados quando pertinentes.",37),
        e("dmg24-initiative","combate","O combate não começa só porque alguém rolou iniciativa",
          "Cabe ao Mestre estabelecer quando o conflito entra no procedimento de combate; o livro também oferece opções de iniciativa.",
          "Uma discussão ameaçadora pode continuar como interação até o conflito realmente começar.",41),
        e("dmg24-combat-narration","combate","Descrever ferimentos ajuda nas decisões táticas",
          "O Mestre pode comunicar o estado de um adversário por sinais visíveis, mantendo a linguagem mecânica clara quando necessário.",
          "Uma criatura muito ferida pode cambalear; isso ajuda o grupo a decidir recuar ou pressionar.",45),
        e("dmg24-new-combatants","combate","Reforços entram na ordem de iniciativa",
          "Se mais combatentes surgem no encontro, eles rolam iniciativa e passam a integrar a ordem de turnos.",
          "Uma patrulha que aparece no meio do combate não deve agir fora de um turno legítimo.",47),
        e("dmg24-chases","perseguição","Perseguições têm um procedimento próprio",
          "Uma perseguição pode exigir iniciativa e acompanhar a distância entre fugitivos e perseguidores. As ações importam para ganhar ou perder terreno.",
          "A fuga não precisa ser resolvida apenas por velocidades fixas de movimentação.",51),
        e("dmg24-magic-items","objetos mágicos","Criar um item mágico exige ponderar seus efeitos",
          "O Mestre pode modificar forma, tipo de dano e certas propriedades de itens ao criar tesouros; isso é orientação de criação, não permissão automática ao jogador.",
          "Use o procedimento apenas quando a campanha permitir criação ou adaptação de objetos mágicos.",57)
    };
    private RuleCuriosityCatalog(){}
    public static int count(){return ITEMS.length;}
    public static Entry at(int index){return ITEMS[Math.floorMod(index, ITEMS.length)];}
    public static boolean validPrintedPage(Entry e){return e!=null && e.printedPage>0 && e.pdfPage==e.printedPage+4;}
}
