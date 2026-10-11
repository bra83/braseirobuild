package com.braseiro.dnd5e;

/**
 * Política única do pipeline de voz.
 *
 * Manter estes valores em um só lugar impede que o watchdog da interface,
 * os timeouts HTTP, o cache e a voz nativa evoluam com configurações
 * incompatíveis.
 */
public final class NarrationPlaybackPolicy {
    /** Atualiza apenas o status; não troca de voz enquanto a requisição continua ativa. */
    public static final long CHARON_WAIT_STATUS_MS = 15000L;
    public static final long CHARON_LONG_WAIT_STATUS_MS = 45000L;
    public static final int CHARON_CONNECT_TIMEOUT_MS = 15000;
    public static final int CHARON_READ_TIMEOUT_MS = 60000;
    public static final int CHARON_RECOVERABLE_RETRIES = 1;
    public static final float ANDROID_SPEECH_RATE = 0.94f;
    public static final float ANDROID_PITCH = 0.92f;
    public static final String CACHE_PIPELINE_ID = "charon-v11-primeira-fala-rapida";
    public static final String STYLE_ID = "pt-BR-profundo-sabio-humano-contido-v3";

    private NarrationPlaybackPolicy() {}

    /**
     * O formato separa direção e transcrição. Isso reduz a chance de o modelo
     * ler instruções ou variar a interpretação entre blocos da mesma cena.
     */
    public static String charonPrompt(String text) {
        String transcript = PortugueseNarrationSanitizer.sanitize(text == null ? "" : text);
        return "PERFIL DE ÁUDIO\n"
                + "Idioma: português brasileiro (pt-BR).\n"
                + "Voz: narrador profundo, sábio, humano e contido; dicção natural, sem caricatura.\n"
                + "DIREÇÃO\n"
                + "Mantenha exatamente o mesmo timbre, altura, velocidade, intensidade, distância do microfone "
                + "e grau de emoção em todos os blocos desta cena. Faça pausas naturais sem reiniciar a "
                + "interpretação em parágrafos ou capítulos. Não leia estas instruções. Não acrescente, retire "
                + "ou substitua palavras.\n"
                + "TRANSCRIÇÃO\n"
                + transcript;
    }
}
