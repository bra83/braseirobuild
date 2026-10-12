package com.braseiro.pathfinder2e;

public final class PortugueseNarrationSanitizerSelfTest {
    public static void main(String[] args) {
        expect(
                "A noite cai. Sem aviso.\n\nO fogo cresce. \"Agora\" todos escutam.",
                PortugueseNarrationSanitizer.sanitize(
                        "A noite cai.Sem aviso.\n\nO fogo cresce.\"Agora\"todos escutam."
                )
        );
        expect(
                "Os agricultores seguem pela estrada.",
                PortugueseNarrationSanitizer.sanitize("Osagricultores seguem pela estrada.")
        );
        expect(
                "A carroça passa. A guarda observa.",
                PortugueseNarrationSanitizer.sanitize("A carroça passa.A guarda observa.")
        );
        expect(
                "O dado mostra 2.5. A sala mede 3D.",
                PortugueseNarrationSanitizer.sanitize("O dado mostra 2.5.A sala mede 3D.")
        );
        expect(
                "A luz banha a vasta extensão. O vento vem trazendo consigo o aroma de madeira vindo das fogueiras. Os viajantes costumam buscar novas oportunidades. Um peitoral de aço revela uma boa posição sobre a terra batida, e o grupo desfruta deste momento.",
                PortugueseNarrationSanitizer.sanitize("A luz banha a vastaextensão. O vento vem trazendoconsigo o aroma de madeiravindo das fogueiras. Os viajantescostumam buscar novasoportunidades. Um peitoral deaco revela uma boaposição sobre a terrabatida, e o grupo desfrutadeste momento.")
        );
        expect(
                "A vasta extensão dos campos",
                PortugueseNarrationSanitizer.mergeStreamingText("A vasta", "extensão dos campos")
        );
        expect(
                "A extensão dos campos",
                PortugueseNarrationSanitizer.mergeStreamingText("A exten", "são dos campos")
        );
        expect(
                "a vasta extensão",
                PortugueseNarrationSanitizer.mergeStreamingText("a vasta", " extensão")
        );
        expect(
                "a vasta extensão",
                PortugueseNarrationSanitizer.mergeStreamingText("a vasta", "extensão")
        );
        expect(
                "madeira vindo",
                PortugueseNarrationSanitizer.mergeStreamingText("madeira", " vindo")
        );
        expect(
                "posição tática",
                PortugueseNarrationSanitizer.mergeStreamingText("posição", " tática")
        );
        expect(
                "Dragon",
                PortugueseNarrationSanitizer.mergeStreamingText("Dra", "gon")
        );
        System.out.println("OK  normalização PT-BR e fronteiras de streaming preservadas");
    }

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Esperado: <" + expected + ">; obtido: <" + actual + ">");
        }
    }
}
