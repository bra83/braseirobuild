package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

/**
 * Structured personality generated ONLY for a brand-new random character.
 * It has no mechanical authority; never mutate a player-authored profile.
 * Based on 39 distinct narrative dimensions, not on copying any PC's beliefs.
 */
public final class BraseiroRandomPersonalityEngine {
    public static final int DIMENSIONS=39;
    private BraseiroRandomPersonalityEngine() {}
    // ID, generalized label, category, optional perspective, trigger
    private static final String[][] DIMENSION_VALUES={
        {"orgulho_ferido","Reação a humilhações","principios","Não aprecia ser humilhado; a forma de reagir depende das escolhas do jogador.","humilhação"},
        {"lealdade","Lealdade a pessoas","relacoes_e_comportamento","Tende a cultivar compromissos com quem demonstra respeito e reciprocidade.","amizade"},
        {"medo_perda","Medo de perdas","relacoes_e_comportamento","Sente preocupação quando relações ou responsabilidades importantes são ameaçadas.","perda"},
        {"disfarce_emocional","Expressão das emoções","relacoes_e_comportamento","Escolhe com cuidado quando revelar a própria vulnerabilidade.","tristeza"},
        {"rebeldia","Relação com autoridade","principios","Julga ordens por seus motivos e efeitos, sem submissão ou rebeldia automática.","ordem"},
        {"perfeccionismo","Reação a erros","relacoes_e_comportamento","Procura aprender com erros; pode exigir demais de si em momentos importantes.","erro"},
        {"confianca_amigos","Confiança interpessoal","relacoes_e_comportamento","A confiança é construída por comportamento observado, não pela mera posição social.","confiança"},
        {"dinheiro_liberdade","Relação com dinheiro","relacoes_e_comportamento","Vê recursos materiais como meios para objetivos, sem assumir posses não adquiridas.","dinheiro"},
        {"ambicao_lenda","Ambição e legado","principios","Deseja que suas escolhas deixem uma contribuição pessoal, sem destino obrigatório.","legado"},
        {"rivalidade","Atitude diante de rivais","relacoes_e_comportamento","Reconhece talento alheio mesmo quando está em desacordo com alguém.","rival"},
        {"curiosidade_imprudente","Curiosidade e risco","relacoes_e_comportamento","Investiga o desconhecido, mas pode ponderar os riscos conforme a situação.","mistério"},
        {"justica_impulsiva","Resposta a injustiças","relacoes_e_comportamento","Demonstra desconforto diante de abusos; não impõe reação automática.","injustiça"},
        {"perdoar_sem_esquecer","Perdão e memória","principios","Considera reparar relações, mas pode recordar consequências de ofensas anteriores.","perdão"},
        {"promessas","Compromissos pessoais","relacoes_e_comportamento","Valoriza compromissos assumidos livremente e considera seus efeitos sobre outros.","promessa"},
        {"paciencia_esforco","Paciência no aprendizado","relacoes_e_comportamento","Respeita esforço genuíno; a própria paciência varia com as circunstâncias.","esforço"},
        {"musica_alma","Relação com música","relacoes_e_comportamento","Pode apreciar música como experiência cultural; isso não concede instrumentos ou proficiência.","música"},
        {"aceita_ajuda","Disposição para ajuda","principios","Avalia ajuda oferecida e pode retribuir sem perder sua autonomia.","ajuda"},
        {"violencia","Limites da violência","relacoes_e_comportamento","Reconhece consequências da força e pode buscar alternativas; decisões são do jogador.","combate"},
        {"mentira_para_proteger","Verdade e proteção","relacoes_e_comportamento","Pondera honestidade e proteção sem considerar a mentira uma obrigação.","mentira"},
        {"amizades","Formação de amizades","relacoes_e_comportamento","Constrói vínculos à medida que conhece pessoas em circunstâncias concretas.","amizade"},
        {"lider_instintivo","Liderança em grupo","principios","Pode assumir iniciativa quando necessário, mas respeita decisões do grupo.","liderança"},
        {"desculpas","Reconhecimento de erros","relacoes_e_comportamento","Procura reparar danos causados; pedir desculpas depende de contexto e escolha.","arrependimento"},
        {"solidao","Necessidade de recolhimento","relacoes_e_comportamento","Em certas ocasiões prefere refletir sozinho para organizar suas ideias.","solidão"},
        {"autonomia_amigos","Liberdade dos aliados","relacoes_e_comportamento","Reconhece que aliados têm objetivos e decisões próprios.","aliado"},
        {"proprio_destino","Liberdade e destino","principios","Valoriza poder escolher o próprio caminho, sem pressupor profecias reais.","destino"},
        {"etica_encantamentos","Ética de influência mental","relacoes_e_comportamento","Considera os limites éticos de controlar outras pessoas, mesmo sem possuir magia.","encantamento"},
        {"humor_afiado","Estilo de humor","relacoes_e_comportamento","Adapta o humor à situação; não presume ironia ou crueldade constantes.","humor"},
        {"consolo","Forma de consolar","relacoes_e_comportamento","Oferece apoio de forma compatível com sua personalidade e a situação.","luto"},
        {"amigo_injusto","Lealdade e limites","principios","Lealdade não implica concordar com atos injustos de aliados.","injustiça"},
        {"elogios","Reação a elogios","relacoes_e_comportamento","Aprecia reconhecimento sincero, mas sua reação varia com quem o oferece.","elogio"},
        {"autoridade_exemplo","Exemplo e respeito","relacoes_e_comportamento","Prefere respeito conquistado por atos a autoridade apenas declarada.","respeito"},
        {"vaidade_publica","Reconhecimento público","relacoes_e_comportamento","Pode gostar de ser notado, sem buscar fama em todas as situações.","fama"},
        {"fe_divina","Relação com crenças religiosas","principios","Respeita a variedade de crenças sem pressupor devoção ou poderes divinos.","religião"},
        {"mortalidade","Consciência de mortalidade","relacoes_e_comportamento","Reconhece a finitude da vida e pensa no significado das próprias escolhas.","morte"},
        {"vitorias_intimas","Celebração das conquistas","relacoes_e_comportamento","Pode dividir conquistas com aliados; não presume relações prévias específicas.","conquista"},
        {"rendicao","Tratamento de rendidos","relacoes_e_comportamento","Reconhece a possibilidade de rendição e as consequências de violência desnecessária.","rendição"},
        {"animais","Atitude perante animais","principios","Trata seres vivos com cuidado quando a situação permite, sem ganhar habilidade.","animal"},
        {"culturas","Abertura a culturas","relacoes_e_comportamento","Procura compreender costumes antes de julgar pessoas pela origem.","cultura"},
        {"credito_coletivo","Crédito por conquistas","relacoes_e_comportamento","Reconhece contribuições de aliados nas vitórias compartilhadas.","equipe"},
    };
    // Identity, personality, ideal, bond, fear, flaw, motivation
    private static final String[][] ARCHETYPES={
        {"guardião","Cuida do grupo de modo responsável, mesmo quando isso exige sacrifício pessoal.","Proteção mútua e responsabilidade.","A segurança de pessoas que conquistarem sua confiança.","Falhar ao proteger alguém quando poderia ter ajudado.","Pode assumir peso demais pelos outros.","Contribuir para que sua comunidade e companheiros sobrevivam e prosperem."},
        {"investigador","Observa padrões e busca respostas, mas reconhece o valor das pessoas.","Conhecimento usado com responsabilidade.","A verdade de um mistério ainda não esclarecido na campanha.","Ignorar um sinal importante e provocar dano evitável.","Pode insistir numa questão além do razoável.","Descobrir, aprender e compartilhar o conhecimento obtido."},
        {"mediador","Procura acordos que preservem dignidade e autonomia de todos.","Justiça, respeito e cooperação.","Pessoas com quem venha a construir confiança recíproca.","Perder oportunidades de reparar um conflito.","Às vezes evita uma conversa difícil por tempo demais.","Construir relações e reduzir conflitos sem impor escolhas."},
        {"aventureiro","Adapta-se ao inesperado e valoriza descobrir novos caminhos.","Liberdade exercida com responsabilidade.","Companheiros e lugares que venham a marcar sua jornada.","Ficar preso à rotina e abandonar objetivos importantes.","Às vezes calcula mal os riscos de uma descoberta.","Explorar, aprender e construir uma história própria."},
    };
    private static final Map<String,String[]> VARIANTS=new HashMap<>();
    private static void putOverride(String key,String[] options){VARIANTS.put(key,options);}
    static {
        putOverride("orgulho_ferido",new String[]{"Uma crítica à sua capacidade de proteger alguém pode atingi-lo.","Sente-se incomodado quando seu discernimento é desmerecido.","Não gosta de ver seus esforços de conciliação ridicularizados.","Pode levar desafios pessoais como estímulo a se superar."});
        putOverride("lealdade",new String[]{"Protege relações construídas com atos de confiança.","É leal a quem compartilha objetivos e age com integridade.","Procura manter vínculos e ouvir diferentes posições.","Valoriza aliados que respeitam a liberdade uns dos outros."});
        putOverride("ambicao_lenda",new String[]{"Prefere ser lembrado por ter ajudado pessoas.","Gostaria de deixar descobertas úteis para o mundo.","Deseja ser reconhecido por ter unido pessoas.","Gostaria de ser lembrado pelas jornadas e escolhas que realizou."});
        putOverride("curiosidade_imprudente",new String[]{"Investiga riscos quando ameaçam pessoas sob seus cuidados.","Pode perseguir respostas mesmo quando seria prudente parar.","Busca compreender divergências antes de agir.","Tem dificuldade em ignorar oportunidades de descoberta."});
        putOverride("lider_instintivo",new String[]{"Pode coordenar a proteção do grupo, respeitando a decisão de todos.","Oferece planos apoiados em observações e aceita contestação.","Busca consenso antes de propor ações conjuntas.","Às vezes toma a iniciativa primeiro e explica depois."});
        putOverride("solidao",new String[]{"Precisa de momentos de descanso depois de cuidar dos outros.","Gosta de refletir a sós sobre perguntas não resolvidas.","Reserva algum tempo para organizar emoções de conversas difíceis.","Aprecia intervalos silenciosos entre jornadas."});
        putOverride("vaidade_publica",new String[]{"Sente orgulho quando reconhecem suas atitudes protetoras.","Aprecia elogios por descobertas bem fundamentadas.","Fica satisfeito quando seu esforço coletivo é reconhecido.","Gosta de contar aventuras, sem inventar feitos que não viveu."});
        putOverride("promessas",new String[]{"Leva a sério promessas de auxílio assumidas voluntariamente.","Evita jurar o que ainda não pode verificar ou cumprir.","Procura não oferecer garantias que desprezem outra pessoa.","Prefere promessas livres a obrigações impostas."});
        putOverride("vitorias_intimas",new String[]{"Partilha conquistas com quem ajudou a sustentá-las.","Compartilha descobertas com quem contribuiu para obtê-las.","Celebra acordos com todos que participaram deles.","Prefere contar vitórias junto de quem viveu a jornada."});
        putOverride("credito_coletivo",new String[]{"Reconhece contribuições de quem manteve o grupo seguro.","Cita os participantes de cada descoberta coletiva.","Valoriza colaboração mesmo quando sua mediação foi decisiva.","Não toma para si o mérito das aventuras de todo o grupo."});
        if(DIMENSION_VALUES.length!=DIMENSIONS)throw new IllegalStateException("PERSONALITY_DIMENSIONS_NOT_39");
    }
    public static JSONObject generate(Random random) {
        if(random==null)throw new IllegalArgumentException("random required");
        int archetype=random.nextInt(ARCHETYPES.length);
        return generateForArchetype(archetype);
    }
    static JSONObject generateForArchetype(int archetype) {
        if(archetype<0||archetype>=ARCHETYPES.length)throw new IllegalArgumentException("Unknown archetype");
        try {
            JSONArray traits=new JSONArray();
            for(String[] d:DIMENSION_VALUES){
                String[] variants=VARIANTS.get(d[0]);
                String value=variants==null?d[3]:variants[archetype];
                if(value.trim().isEmpty())throw new IllegalStateException("EMPTY_PERSONALITY_TRAIT_"+d[0]);
                traits.put(new JSONObject().put("id",d[0]).put("label",d[1])
                    .put("category",d[2]).put("value",value)
                    .put("priority",d[2].equals("principios")?5:4)
                    .put("enabled",true).put("triggers",new JSONArray().put(d[4])));
            }
            return BraseiroPersonalityProfile.checked(new JSONObject()
                .put("schema",BraseiroPersonalityProfile.SCHEMA)
                .put("version",1)
                .put("profile_id","generated_profile_"+ARCHETYPES[archetype][0])
                .put("archetype",ARCHETYPES[archetype][0])
                .put("generation_status","generated_editable")
                .put("agency_policy","player_authoritative")
                .put("mechanical_modifiers",false)
                .put("npc_knowledge_policy","somente_observado_ou_informado_em_jogo")
                .put("evolution_policy","sugerir_mudancas_sem_reescrever_sem_confirmacao_do_jogador")
                .put("traits",traits));
        }catch(Exception e){throw new IllegalStateException("PERSONALITY_GENERATION_FAILED",e);}
    }

    /** New random character only; do not call on imported or edited sheets. */
    public static JSONObject applyToNewRandom(JSONObject sheet, Random random){
        if(sheet==null)throw new IllegalArgumentException("sheet null");
        JSONObject existing=sheet.optJSONObject("personality_profile");
        if(existing!=null){
            // Never overwrite existing or incomplete human-authored choices.
            try{BraseiroPersonalityProfile.checked(existing);}
            catch(Exception invalid){throw new IllegalStateException("PERSONALITY_EXISTING_INVALID",invalid);}
            return sheet;
        }
        JSONObject created=generate(random);
        if(!validateCompleteRandom(created))throw new IllegalStateException("PERSONALITY_INCOMPLETE");
        try{
            sheet.put("personality_profile",created);
            int style=-1;
            for(int i=0;i<ARCHETYPES.length;i++)
                if(ARCHETYPES[i][0].equals(created.optString("archetype")))style=i;
            if(style<0)throw new IllegalStateException("PERSONALITY_ARCHETYPE_MISSING");
            String[] values=ARCHETYPES[style];
            sheet.put("personality",values[1]);
            sheet.put("ideal",values[2]);
            sheet.put("bond",values[3]);
            sheet.put("fear",values[4]);
            sheet.put("flaw",values[5]);
            sheet.put("motivation",values[6]);
        }catch(Exception e){throw new IllegalStateException("PERSONALITY_ATTACH_FAILED",e);}
        return sheet;
    }

    /** Strict certification of a generated profile; existing user profiles remain optional. */
    public static boolean validateCompleteRandom(JSONObject profile){
        if(profile==null||!BraseiroPersonalityProfile.SCHEMA.equals(profile.optString("schema"))
           ||!profile.optString("profile_id").startsWith("generated_profile_")
           ||!"player_authoritative".equals(profile.optString("agency_policy"))
           ||profile.optBoolean("mechanical_modifiers",true))return false;
        JSONArray traits=profile.optJSONArray("traits");
        if(traits==null||traits.length()!=DIMENSIONS)return false;
        HashSet<String> seen=new HashSet<>();
        for(int i=0;i<DIMENSIONS;i++){
            JSONObject t=traits.optJSONObject(i);
            if(t==null||!seen.add(t.optString("id"))||
                !t.optString("id").equals(DIMENSION_VALUES[i][0])||
                t.optString("value").trim().isEmpty()||
                t.optString("label").trim().isEmpty())return false;
        }
        try{BraseiroPersonalityProfile.checked(profile);return true;}
        catch(Exception exception){return false;}
    }
}