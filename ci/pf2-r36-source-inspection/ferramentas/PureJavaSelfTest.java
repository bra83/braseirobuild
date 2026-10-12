import com.braseiro.pathfinder2e.NarrationChapterer;
import com.braseiro.pathfinder2e.PortugueseNarrationSanitizer;
import java.util.List;

/** Testes locais dos componentes Java puros, sem Android e sem rede. */
public final class PureJavaSelfTest {
    private static int checks;
    private static void require(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        require("de uma porta".equals(PortugueseNarrationSanitizer.sanitize("deuma porta")), "deuma não foi corrigido");
        require("A comporta antiga".equals(PortugueseNarrationSanitizer.sanitize("A comporta antiga")), "comporta foi dividida indevidamente");
        require("Velha estrutura".equals(PortugueseNarrationSanitizer.sanitize("Velhaestrutura")), "palavra colada conhecida não foi corrigida");
        require("vento forte".equals(PortugueseNarrationSanitizer.sanitize("vento vento forte")), "repetição não foi removida");
        require("Os agricultores chegam.".equals(PortugueseNarrationSanitizer.sanitize("Osagricultores chegam.")), "Osagricultores não foi corrigido");
        require("O local está vazio.".equals(PortugueseNarrationSanitizer.sanitize("Olocal está vazio.")), "Olocal não foi corrigido");
        require("Uma carroça passa.".equals(PortugueseNarrationSanitizer.sanitize("Umacarroça passa.")), "Umacarroça não foi corrigido");
        require("O mensageiro fala ao fazendeiro.".equals(PortugueseNarrationSanitizer.sanitize("O mensageiro fala aofazendeiro.")), "aofazendeiro não foi corrigido");
        require("A comporta antiga".equals(PortugueseNarrationSanitizer.sanitize("A comporta antiga")), "palavra legítima comporta foi alterada");

        StringBuilder scene = new StringBuilder();
        for (int i = 0; i < 80; i++) scene.append("A estrada segue entre colinas, e o vento anuncia mudanças no tempo. ");
        List<String> chapters = NarrationChapterer.split(scene.toString(), 650);
        require(chapters.size() > 1, "cena longa não foi dividida");
        for (String chapter : chapters) {
            require(!chapter.trim().isEmpty(), "capítulo vazio");
            require(chapter.length() <= 1150, "capítulo excedeu o limite rígido: " + chapter.length());
        }
        System.out.println("TESTES JAVA PURO: " + checks + " verificações aprovadas; " + chapters.size() + " capítulos gerados.");
    }
}
