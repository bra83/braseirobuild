package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** Identidade estável de manuais, sem apagar números semanticamente relevantes. */
public final class ManualIdentityPolicy {
    private ManualIdentityPolicy() {}

    public static boolean isMystaraTitle(String title) {
        return normalizedFileName(title).startsWith("mystara_");
    }

    public static boolean looksLikeMalformedMystaraTitle(String title) {
        String value=normalizedFileName(title);
        return value.startsWith("mystara")&&!value.startsWith("mystara_");
    }

    public static String displayTitle(String title) {
        String value=title==null?"":title.trim();
        if(!isMystaraTitle(value))return value;
        int at=value.toLowerCase(Locale.US).indexOf("mystara_");
        return at<0?value:value.substring(at+"mystara_".length());
    }

    /**
     * Agrupa somente cópias do mesmo título. Números permanecem na chave porque
     * distinguem edições, anos, volumes, níveis e fascículos (por exemplo,
     * Threshold 38 e Threshold 39).
     */
    public static String duplicateKey(String title) {
        String value=normalize(title)
                .replaceAll("\\b(pdf|comprimido|compressed|compactado|reduzido|copia|copy|lowres|low res|highres|high res)\\b"," ")
                .replaceAll("\\s+"," ").trim();
        return value;
    }

    public static String normalize(String value) {
        if(value==null)return "";
        String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        return n.replaceAll("[^a-z0-9]+"," ").trim();
    }

    private static String normalizedFileName(String title) {
        if(title==null)return "";
        String value=title.trim().replace('\\','/');int slash=value.lastIndexOf('/');
        if(slash>=0)value=value.substring(slash+1);
        return value.toLowerCase(Locale.US);
    }
}
