package com.braseiro.dnd5e;

/** Pure admission rules for Gemini portrait imports; does not access AppDb. */
public final class PersistentPortraitImportPolicy {
    private PersistentPortraitImportPolicy() {}

    public static Result validate(String targetType, long targetId, String expectedStable,
                                  String actualStable, boolean active, boolean dead) {
        String kind = targetType == null ? "" : targetType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!("HERO".equals(kind) || "NPC".equals(kind))) return Result.reject("Alvo de retrato inválido.");
        if (targetId <= 0) return Result.reject("Alvo de retrato indisponível.");
        if (!active) return Result.reject("A identidade não está mais ativa.");
        if (dead) return Result.reject("O retrato foi recusado porque o NPC está morto.");
        String expected = expectedStable == null ? "" : expectedStable.trim();
        String actual = actualStable == null ? "" : actualStable.trim();
        if ("NPC".equals(kind) && (expected.length() == 0 || !expected.equals(actual))) {
            return Result.reject("A identidade do NPC mudou antes da importação.");
        }
        return Result.accept();
    }

    public static final class Result {
        public final boolean accepted;
        public final String message;
        private Result(boolean ok, String text) { accepted = ok; message = text; }
        public static Result accept() { return new Result(true, ""); }
        public static Result reject(String text) { return new Result(false, text); }
    }
}
