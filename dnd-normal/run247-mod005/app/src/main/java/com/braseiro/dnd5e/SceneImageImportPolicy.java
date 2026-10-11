package com.braseiro.dnd5e;

/** Pure contract for image share/deep-link admission, independent of AppDb. */
public final class SceneImageImportPolicy {
    private SceneImageImportPolicy() {}

    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_CONSUME = "com.braseiro.dnd5e.action.CONSUME_SCENE_IMAGE";

    public static Result validate(String action, String mime, boolean hasUri,
                                  String pendingFingerprint, String currentFingerprint) {
        String a = action == null ? "" : action.trim();
        if (ACTION_CONSUME.equals(a)) {
            if (!hasUri) return Result.reject("A imagem recebida não está disponível.");
            return stale(pendingFingerprint, currentFingerprint);
        }
        if (!ACTION_SEND.equals(a)) return Result.reject("Deep-link não é uma imagem compartilhada.");
        String type = mime == null ? "" : mime.trim().toLowerCase(java.util.Locale.ROOT);
        if (!type.startsWith("image/")) return Result.reject("O compartilhamento não contém uma imagem.");
        if (!hasUri) return Result.reject("O compartilhamento não trouxe um arquivo de imagem.");
        return stale(pendingFingerprint, currentFingerprint);
    }

    private static Result stale(String pending, String current) {
        String expected = pending == null ? "" : pending.trim();
        String actual = current == null ? "" : current.trim();
        if (expected.length() > 0 && !expected.equals(actual)) {
            return Result.reject("A imagem pertence a uma cena anterior.");
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
