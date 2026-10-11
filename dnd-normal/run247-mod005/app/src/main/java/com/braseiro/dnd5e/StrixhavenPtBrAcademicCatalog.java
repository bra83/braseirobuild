package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Catálogo PT-BR e dados canônicos de UX da vida acadêmica de Strixhaven. */
public final class StrixhavenPtBrAcademicCatalog {
    private StrixhavenPtBrAcademicCatalog(){}

    private static JSONObject extra(String canonical,String label,String s1,String s2,String... members){
        JSONObject o=new JSONObject();try{o.put("canonical",canonical).put("label",label).put("skills",new JSONArray().put(s1).put(s2));JSONArray m=new JSONArray();for(String x:members)m.put(x);o.put("members",m);}catch(Exception ignored){}return o;
    }
    public static JSONArray extracurriculars(){
        JSONArray a=new JSONArray();
        a.put(extra("Dead Languages Society","Sociedade de Línguas Mortas","Atletismo","História","Drazhomir Yarnask","Quentillius Antiphiun Melentor III"));
        a.put(extra("Distinguished Society of Fine Artists","Sociedade Distinta de Belas-Artes","Atuação","Prestidigitação","Cadoras Damellawar","Nora Ann Wu"));
        a.put(extra("Dragonchess Club","Clube de Xadrez Dracônico","Enganação","Investigação","Bhedum “Rampart” Sooviij","Tilana Kapule"));
        a.put(extra("Dragonsguard Historical Society","Sociedade Histórica Dragonsguard","Arcana","História","Bhedum “Rampart” Sooviij"));
        a.put(extra("Fantastical Horticulture Club","Clube de Horticultura Fantástica","Natureza","Sobrevivência","Urzmaktok Grojsh"));
        a.put(extra("Future Entrepreneurs of Strixhaven","Futuros Empreendedores de Strixhaven","Intuição","Persuasão","Grayson Wildemere"));
        a.put(extra("Intramural Gymnastics Club","Clube de Ginástica Intramuros","Acrobacia","Atuação","Zanther Bowen"));
        a.put(extra("Intramural Silkball Club","Clube de Silkball Intramuros","Atletismo","Intimidação","Javenesh Stoutclaw","Melwythorne","Tilana Kapule"));
        a.put(extra("Intramural Water-Dancing Club","Clube de Dança Aquática Intramuros","Atletismo","Atuação","Larine Arneza"));
        a.put(extra("Live-Action Roleplaying Guild","Guilda de RPG ao Vivo","Adestrar Animais","Atuação","Cadoras Damellawar","Rosimyffenbip “Rosie” Wuzfeddlims"));
        a.put(extra("Mage Tower Cheer Squad","Equipe de Torcida da Torre dos Magos","Percepção","Persuasão","Zanther Bowen"));
        a.put(extra("Playactors Drama Guild","Guilda de Teatro dos Atores","Arcana","Enganação","Quentillius Antiphiun Melentor III","Rubina Larkingdale"));
        a.put(extra("Strixhaven Iron-Lifters Society","Sociedade de Levantadores de Ferro de Strixhaven","Atletismo","Medicina","Greta Gorunn"));
        a.put(extra("Strixhaven Show Band Association","Associação da Banda de Strixhaven","Prestidigitação","Atuação","Aurora Luna Wynterstarr","Rubina Larkingdale"));
        a.put(extra("Strixhaven Star","Jornal Estrela de Strixhaven","Investigação","Intuição","Grayson Wildemere","Mina Lee"));
        a.put(extra("Student-Mages of Faith","Magos Estudantes da Fé","Intuição","Religião","Melwythorne","Shuvadri Glintmantle"));
        return a;
    }
    public static String[] extracurricularLabels(){JSONArray a=extracurriculars();String[] out=new String[a.length()];for(int i=0;i<a.length();i++)out[i]=a.optJSONObject(i).optString("label");return out;}
    public static JSONObject extracurricularAt(int index){JSONArray a=extracurriculars();if(a.length()==0)return new JSONObject();return a.optJSONObject(Math.max(0,Math.min(index,a.length()-1)));}

    private static JSONObject job(String place,String canonical,String coworker,String... positions){JSONObject o=new JSONObject();try{o.put("place",place).put("canonical",canonical).put("coworker",coworker);JSONArray p=new JSONArray();for(String x:positions)p.put(x);o.put("positions",p);}catch(Exception ignored){}return o;}
    public static JSONArray jobs(){JSONArray a=new JSONArray();
        a.put(job("Biblioplex","Biblioplex","Drazhomir Yarnask","Atendente de livros","Repositor de livros","Atendente do café","Equipe de limpeza","Jardineiro","Atendente da loja"));
        a.put(job("Taverna Bow’s End","Bow’s End Tavern","Javenesh Stoutclaw","Gerente assistente","Equipe de limpeza","Cozinheiro","Recepcionista","Garçom"));
        a.put(job("Terrenos do Campus","Campus Grounds","Shuvadri Glintmantle","Removedor de grafites","Cuidador do gramado","Coletor de lixo leve","Varredor de calçadas","Reparador de estátuas","Coletor de lixo"));
        a.put(job("Laboratórios de Magia do Campus","Campus Magic Labs","Urzmaktok Grojsh","Equipe de limpeza","Preparador de espécimes","Parceiro voluntário de laboratório"));
        a.put(job("Dormitórios","Dormitories","Nora Ann Wu","Equipe de limpeza","Assistente de eventos","Atendente da recepção","Assistente residente"));
        a.put(job("Café Firejolt","Firejolt Cafe","Mina Lee","Barista","Caixa","Equipe de limpeza","Garçom"));
        a.put(job("Campos Intramuros","Intramural Fields","Rosimyffenbip “Rosie” Wuzfeddlims","Equipe de limpeza","Controlador de público","Assistente de equipamento","Jardineiro","Árbitro"));
        a.put(job("Sociedade de Artes Cênicas de Strixhaven","Strixhaven Performing Arts Society","Larine Arneza","Equipe de limpeza","Assistente geral","Especialista em lanches","Contrarregra","Bilheteiro","Recepcionista"));
        a.put(job("Estádio de Strixhaven","Strixhaven Stadium","Aurora Luna Wynterstarr / Greta Gorunn","Equipe de limpeza","Assistente de entretenimento","Assistente de equipamento","Jardineiro","Especialista em lanches","Bilheteiro","Recepcionista"));
        return a;}
    public static String[] jobLabels(){JSONArray a=jobs();String[] out=new String[a.length()];for(int i=0;i<a.length();i++)out[i]=a.optJSONObject(i).optString("place");return out;}
    public static JSONObject jobAt(int index){JSONArray a=jobs();return a.optJSONObject(Math.max(0,Math.min(index,a.length()-1)));}
    public static String[] jsonStrings(JSONArray a){if(a==null)return new String[0];String[] out=new String[a.length()];for(int i=0;i<a.length();i++)out[i]=a.optString(i);return out;}

    public static boolean canJoinExtracurricularNow(Context c){String id=StrixhavenOfficialCampaignRuntime.progress(c).optString("event_id","");return "y1_first_day".equals(id)||"y2_back".equals(id)||"y3_invited".equals(id)||"y4_shadow".equals(id);}

    public static String blockReason(String code){
        if("tutoring_required".equals(code))return "A tutoria obrigatória suspende emprego e atividades extracurriculares até você ser aprovado em uma prova posterior neste ano.";
        if("join_only_at_start_of_academic_year".equals(code))return "Novas atividades extracurriculares só podem ser iniciadas no começo do ano acadêmico.";
        if("extracurricular_required".equals(code))return "Escolha uma atividade extracurricular.";
        if("already_joined".equals(code))return "O estudante já participa dessa atividade.";
        if("extracurricular_limit".equals(code))return "Limite de atividades atingido. Com emprego ativo, apenas uma atividade extracurricular pode conceder benefício.";
        if("job_requires_at_most_one_extracurricular".equals(code))return "Para assumir um emprego, mantenha no máximo uma atividade extracurricular ativa.";
        if("employer_required".equals(code))return "Escolha um emprego do campus.";
        if("job_wage_not_active".equals(code))return "Você não possui um emprego ativo elegível para salário nesta semana.";
        if("job_wage_already_claimed_for_week".equals(code))return "O salário desta semana já foi recebido.";
        if("friendship_threshold_not_met".equals(code))return "São necessários pelo menos 2 Pontos de Relacionamento positivos para tornar este colega uma Pessoa Querida.";
        if("no_beloved_inspiration".equals(code))return "Não há Inspiração de Pessoa Querida disponível.";
        if("no_matching_student_die".equals(code))return "Não há Dado do Estudante disponível para essa perícia.";
        return code==null||code.length()==0?"A ação acadêmica não pôde ser concluída.":"Ação acadêmica indisponível.";
    }
    public static String examResult(String code){if("failed".equals(code))return "REPROVADO";if("passed".equals(code))return "APROVADO";if("aced".equals(code))return "DESTAQUE";return code;}

    public static String campaignTitle(String title){
        if("Campus Kerfuffle".equals(title))return "Confusão no Campus";
        if("Hunt for Mage Tower".equals(title))return "Caçada à Torre dos Magos";
        if("The Magister's Masquerade".equals(title))return "O Baile de Máscaras do Magister";
        if("A Reckoning in Ruins".equals(title))return "Acerto de Contas nas Ruínas";
        return title;
    }
    public static String eventTitle(String t){
        if(t==null)return "";
        String[][] m=new String[][]{
            {"First Day on Campus","Primeiro Dia no Campus"},{"Orientation Challenge","Desafio de Orientação"},{"Frog’s the Word","A Palavra é Sapo"},{"Exam: Slaadi","Prova: Slaadi"},{"Work Hard, Play Harder","Trabalhe Muito, Divirta-se Mais"},{"Exam: Owlbears","Prova: Ursocoruja"},{"Campus Daredevils","Audaciosos do Campus"},{"Exam: Otyughs","Prova: Otyugh"},{"All the World’s a Stage","O Mundo Inteiro é um Palco"},{"Secrets in Sedgemoor","Segredos em Sedgemoor"},
            {"Back in Session","De Volta às Aulas"},{"Mascots Everywhere!","Mascotes por Toda Parte!"},{"Exam: Glyph of Warding","Prova: Glifo de Proteção"},{"Party at the Rose Stage","Festa no Palco Rosa"},{"Practice Makes Perfect","A Prática Leva à Perfeição"},{"Exam: Symbol Glyphs","Prova: Glifos de Símbolo"},{"Getting Some Air","Tomando um Pouco de Ar"},{"Dangerous Knowledge","Conhecimento Perigoso"},{"Exam: Augury","Prova: Augúrio"},{"Tell Us in Song!","Conte-nos em uma Canção!"},{"Battle of Strixhaven","Batalha de Strixhaven"},
            {"Cordially Invited","Cordialmente Convidado"},{"Reception at Bow’s End","Recepção na Bow’s End"},{"Fate at Furygale","Destino em Furygale"},{"Exam: Relics","Prova: Relíquias"},{"Dressing for Success","Vestindo-se para o Sucesso"},{"Party Preparations","Preparativos para a Festa"},{"Exam: Devices","Prova: Dispositivos"},{"Suspicions","Suspeitas"},{"Fancying Up Footwork","Aprimorando os Passos"},{"Preparing the Hall","Preparando o Salão"},{"Exam: Unearthed Arcana","Prova: Arcana Desenterrada"},{"A Starlit Night","Uma Noite Estrelada"},{"Midnight at Lorehold","Meia-noite em Lorehold"},{"Aftermath","Consequências"},
            {"A Sinister Shadow","Uma Sombra Sinistra"},{"A Brief Distraction","Uma Breve Distração"},{"Vegetation Identification","Identificação de Vegetação"},{"Clue in the Bayou","Pista no Pântano"},{"No Time to Lose","Sem Tempo a Perder"},{"Into the Badlands","Rumo às Terras Áridas"},{"This Is Surreal, Isn’t It?","Isto é Surreal, Não é?"},{"Ruins of Caerdoon","Ruínas de Caerdoon"},{"Confronting Murgaxor","Confrontando Murgaxor"}
        };
        for(String[] x:m)if(x[0].equals(t))return x[1];return t;
    }
    public static String examCourse(String s){if("Magical Physiologies".equals(s))return "Fisiologias Mágicas";if("Scrivening and Symbology".equals(s))return "Escrita Mágica e Simbologia";if("Advanced Arcane History".equals(s))return "História Arcana Avançada";if("Arcane Herbology".equals(s))return "Herbologia Arcana";return s;}
    public static String examTitle(String s){if("Owlbears".equals(s))return "Ursocoruja";if("Glyph of Warding".equals(s))return "Glifo de Proteção";if("Symbol Glyphs".equals(s))return "Glifos de Símbolo";if("Augury".equals(s))return "Augúrio";if("Relics".equals(s))return "Relíquias";if("Devices".equals(s))return "Dispositivos";if("Unearthed Arcana".equals(s))return "Arcana Desenterrada";if("Vegetation Identification".equals(s))return "Identificação de Vegetação";return s;}
}
