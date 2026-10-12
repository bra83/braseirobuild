package com.braseiro.pathfinder2e;

import java.util.List;

public final class NarrationPipelineSelfTest {
    public static void main(String[] args) {
        require(NarrationPlaybackPolicy.CHARON_WAIT_STATUS_MS == 15000L,
                "o primeiro aviso deve ocorrer em 15 s sem cancelar Charon");
        require(NarrationPlaybackPolicy.CHARON_LONG_WAIT_STATUS_MS == 45000L,
                "o aviso prolongado deve ocorrer em 45 s sem fallback antecipado");
        require(NarrationPlaybackPolicy.CHARON_CONNECT_TIMEOUT_MS == 15000,
                "Charon deve ter 15 s para conectar");
        require(NarrationPlaybackPolicy.CHARON_READ_TIMEOUT_MS == 120000,
                "Charon deve ter 120 s para entregar uma cena contínua");
        String shortScene = "A chuva cai sobre a estrada. A companhia fecha o portão.";
        require(NarrationChunker.charon(shortScene).size() == 1,
                "cena curta Charon não deve ser fragmentada");
        require(NarrationChunker.android(shortScene).size() == 1,
                "cena curta Android não deve ser fragmentada");

        String mediumScene = "A chuva engrossa sobre as pedras antigas enquanto a companhia alcança a ponte. "
                + "Do outro lado, duas lanternas se movem junto ao portão e uma sentinela ergue a mão. "
                + "O rio corre forte sob as tábuas, e o som encobre parte da conversa. "
                + "Ninguém ainda sacou uma arma, mas todos observam cada movimento. "
                + "Atrás da paliçada, um sino toca uma única vez e os cavalos presos ao poste começam a se agitar.";
        String mediumNormalized = PortugueseNarrationSanitizer.sanitize(mediumScene).replaceAll("\\s+", " ").trim();
        List<String> mediumCharon = NarrationChunker.charon(mediumNormalized);
        require(mediumCharon.size() == 1, "cena média deve usar uma única chamada Charon contínua");
        require(mediumCharon.get(0).equals(mediumNormalized), "a chamada contínua deve receber a cena média inteira");
        require(mediumNormalized.equals(join(mediumCharon)), "segmentação rápida Charon deve preservar o texto médio");

        StringBuilder longScene = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            if (longScene.length() > 0) longScene.append(' ');
            longScene.append("A companhia atravessa a floresta, observa as ruínas e mantém a formação.");
        }
        String normalized = PortugueseNarrationSanitizer.sanitize(longScene.toString())
                .replaceAll("\\s+", " ").trim();
        List<String> charon = NarrationChunker.charon(normalized);
        List<String> android = NarrationChunker.android(normalized);
        require(charon.size() >= 1 && charon.size() <= 2,
                "Charon só deve dividir textos realmente grandes");
        require(android.size() > 1 && android.size() < 16,
                "Android não pode voltar aos 30 trechos curtos");
        require(normalized.equals(join(charon)), "os blocos Charon devem preservar todo o texto");
        require(normalized.equals(join(android)), "os blocos Android devem preservar todo o texto");
        require(NarrationPlaybackPolicy.charonPrompt("A porta se abre.").endsWith("A porta se abre."),
                "o prompt deve terminar na transcrição exata");
        System.out.println("OK  pipeline Charon contínuo, cancelável e sem corte na primeira frase");
    }

    private static String join(List<String> chunks) {
        StringBuilder out = new StringBuilder();
        for (String chunk : chunks) {
            if (out.length() > 0) out.append(' ');
            out.append(chunk.trim());
        }
        return out.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
