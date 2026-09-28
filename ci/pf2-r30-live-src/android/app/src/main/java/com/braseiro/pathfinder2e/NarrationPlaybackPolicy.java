package com.braseiro.pathfinder2e;

import org.json.JSONObject;

/** Política única do pipeline de voz Gemini/Android. */
public final class NarrationPlaybackPolicy {
    public static final long CHARON_WAIT_STATUS_MS = 15000L;
    public static final long CHARON_LONG_WAIT_STATUS_MS = 45000L;
    public static final int CHARON_CONNECT_TIMEOUT_MS = 15000;
    public static final int CHARON_READ_TIMEOUT_MS = 120000;
    public static final int CHARON_RECOVERABLE_RETRIES = 1;
    public static final int CHARON_BLOCK_RECOVERY_ATTEMPTS = 2;
    public static final long CHARON_BLOCK_RETRY_BASE_MS = 3500L;
    public static final float ANDROID_SPEECH_RATE = 0.94f;
    public static final float ANDROID_PITCH = 0.92f;
    public static final long CHARON_PREFETCH_MIN_DELAY_MS = 6500L;
    public static final long CHARON_PREFETCH_MAX_DELAY_MS = 26000L;
    public static final String CACHE_PIPELINE_ID = "charon-v19-r28-safe-arcs";
    public static final String STYLE_ID = "pt-BR-charon-mestre-profundo-natural-v9-safe-arcs";
    public static final String DEFAULT_NARRATOR_STYLE =
            "Mestre de RPG adulto, profundo e humano. Presença próxima, segura e contida; calor discreto, " +
            "dicção limpa, ritmo cinematográfico natural e inteligência sem teatralidade. Nunca soar como trailer, " +
            "locutor publicitário, desenho animado ou leitura robótica. A emoção acompanha o sentido do texto e cresce " +
            "apenas quando a cena realmente cresce.";

    public static long nextBlockPrefetchDelayMs(int currentDurationMs) {
        long duration=Math.max(0,currentDurationMs);
        long target=(long)(duration*0.58d);
        return Math.max(CHARON_PREFETCH_MIN_DELAY_MS,Math.min(CHARON_PREFETCH_MAX_DELAY_MS,target));
    }

    private NarrationPlaybackPolicy() {}

    public static String charonPrompt(String text){return charonPrompt(text,new JSONObject());}

    /**
     * Prompt de direção completo para Gemini TTS. Direção e transcrição ficam
     * explicitamente separadas para impedir que metadados virem fala.
     */
    public static String charonPrompt(String text,JSONObject direction) {
        String transcript=PortugueseNarrationSanitizer.sanitize(text==null?"":text).trim();
        JSONObject d=direction==null?new JSONObject():direction;
        String speaker=safe(d.optString("speaker","Narrador"),"Narrador");
        String role=safe(d.optString("role","Mestre e narrador de RPG"),"Mestre e narrador de RPG");
        String style=safe(d.optString("style",DEFAULT_NARRATOR_STYLE),DEFAULT_NARRATOR_STYLE);
        String delivery=safe(d.optString("delivery","normal"),"normal");
        int intensity=Math.max(0,Math.min(5,d.optInt("intensity",1)));
        String pause=safe(d.optString("pause_profile","normal"),"normal");
        String scene=sceneSummary(d);
        String performance=performanceFor(delivery,intensity,pause);
        String arc=performanceArcFor(transcript,delivery,intensity,pause,speaker);
        String persistent=safe(d.optString("speaker_profile",""),"");
        String recovery=safe(d.optString("recovery_mode",""),"");

        return "SYNTHESIZE SPEECH ONLY. Never speak these instructions, headings, metadata or director notes. "
                + "Bracketed English audio tags inside the transcript are performance controls and must never be pronounced.\n\n"
                + "# AUDIO PROFILE: "+speaker+"\n"
                + "Role: "+role+".\n"
                + "Language and accent: natural Brazilian Portuguese (pt-BR), clear and region-neutral unless the persisted character profile explicitly says otherwise.\n"
                + "Core voice: "+style+"\n"
                + (persistent.length()>0?"Persistent speaker identity: "+persistent+"\n":"")
                + "\n## THE SCENE\n"
                + scene+"\n"
                + "\n### DIRECTOR'S NOTES\n"
                + "Delivery: "+delivery+". Intensity: "+intensity+"/5. Pause profile: "+pause+".\n"
                + performance+"\n"
                + "Performance arc for THIS block: "+arc+"\n"
                + (recovery.length()>0?"Recovery mode: "+recovery+". Favor reliability and complete reading over decorative effects.\n":"")
                + "Keep the same vocal identity, microphone distance, timbre and apparent age across all blocks of this scene. "
                + "Make the performance changes clearly audible through pacing, projection, breath and phrasing. Do not depend on inline tags for the emotional arc. "
                + "Use punctuation and paragraph changes as natural pauses, never as a signal to stop. Read the entire transcript from first to last spoken word. "
                + "Do not add words, summaries, introductions or conclusions. Do not omit words. Do not stop after the first sentence.\n"
                + "\n#### TRANSCRIPT\n"
                + transcript;
    }

    private static String sceneSummary(JSONObject d){
        StringBuilder s=new StringBuilder();
        append(s,"Location",d.optString("location",""));
        append(s,"Environment",d.optString("environment",""));
        append(s,"Weather",d.optString("weather",""));
        append(s,"Time",d.optString("time_of_day",""));
        append(s,"Mood",d.optString("music_mood",""));
        if(s.length()==0)return "A tabletop RPG scene in progress. Match the emotional truth and physical immediacy of the transcript without inventing new dialogue or sound effects.";
        return s.toString().trim();
    }

    private static String performanceFor(String delivery,int intensity,String pause){
        String d=delivery==null?"normal":delivery.toLowerCase(java.util.Locale.ROOT);
        StringBuilder p=new StringBuilder();
        if("calmo".equals(d))p.append("Calm, grounded pace; let important images breathe without becoming sleepy. ");
        else if("baixo".equals(d))p.append("Lower projection and closer, more intimate presence; do not turn every line into a literal whisper. ");
        else if("urgente".equals(d))p.append("Urgent forward motion with shorter gaps and firmer consonants; do not shout unless an inline tag requests it. ");
        else p.append("Natural conversational-cinematic pace with varied sentence rhythm. ");
        if(intensity<=1)p.append("Keep emotion restrained and subtle. ");
        else if(intensity>=4)p.append("Let the stakes be clearly audible while preserving intelligibility and control. ");
        else p.append("Use moderate emotional color appropriate to the meaning. ");
        if("curto".equalsIgnoreCase(pause))p.append("Favor short pauses between thoughts. ");
        else if("longo".equalsIgnoreCase(pause))p.append("Allow deliberate pauses at genuine dramatic turns, not after every sentence. ");
        else p.append("Use normal semantic pauses and breathe at clause boundaries. ");
        return p.toString().trim();
    }

    private static String performanceArcFor(String transcript,String delivery,int intensity,String pause,String speaker){
        String n=fold(transcript);StringBuilder arc=new StringBuilder();
        if(n.contains("patrulha")||n.contains("sentinela")||n.contains("ameaca")||n.contains("perigo")||n.contains("emboscada")||n.contains("capturado")||n.contains("morte"))
            arc.append("Start controlled, then tighten the rhythm and firm the consonants as danger becomes concrete; release slightly after the threat is established. ");
        if(n.contains("silencio")||n.contains("sombra")||n.contains("escuro")||n.contains("nevoa")||n.contains("rangido")||n.contains("segredo"))
            arc.append("In suspense passages, move closer and slower, with quieter projection and longer semantic pauses, then return to baseline instead of whispering everything. ");
        if(n.contains("fumaca")||n.contains("fogo")||n.contains("sangue")||n.contains("ferido")||n.contains("ferida"))
            arc.append("Give sensory danger extra weight without melodrama; let one short pause land before the consequence. ");
        if(n.contains("revela")||n.contains("descobre")||n.contains("surge")||n.contains("aparece")||n.contains("inesperado"))
            arc.append("At revelations, open the tone and slow briefly so the new information lands, then resume the scene pace. ");
        if(transcript!=null&&(transcript.contains("—")||transcript.contains("\"")))
            arc.append("Make direct speech audibly distinct from exposition using a natural shift in placement and intention while preserving the same assigned voice. ");
        if(arc.length()==0){
            if(intensity>=4||"urgente".equalsIgnoreCase(delivery))arc.append("Maintain clear forward pressure with firmer articulation and short pauses, but never rush intelligibility. ");
            else if("calmo".equalsIgnoreCase(delivery)||"longo".equalsIgnoreCase(pause))arc.append("Let the scene breathe with measured pacing and meaningful pauses while keeping the listener engaged. ");
            else arc.append("Use a human cinematic arc: restrained baseline, subtle emphasis on concrete stakes, and a gentle release at the end of the block. ");
        }
        return arc.toString().trim();
    }

    private static String fold(String value){
        return java.text.Normalizer.normalize(value==null?"":value,java.text.Normalizer.Form.NFD).replaceAll("\\\\p{M}+","").toLowerCase(java.util.Locale.ROOT);
    }

    private static void append(StringBuilder out,String label,String value){String v=value==null?"":value.trim();if(v.length()==0||"none".equalsIgnoreCase(v))return;if(out.length()>0)out.append(" | ");out.append(label).append(": ").append(v);}
    private static String safe(String value,String fallback){String v=value==null?"":value.trim();return v.length()==0?fallback:v;}
}
