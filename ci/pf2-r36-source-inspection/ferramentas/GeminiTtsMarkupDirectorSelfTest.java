package com.braseiro.pathfinder2e;

public final class GeminiTtsMarkupDirectorSelfTest {
    private static int checks=0;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new IllegalStateException(message);}
    public static void main(String[] args){
        String raw="A névoa cobre a estrada. Tito sussurra para o grupo não fazer barulho. Ao longe, alguém grita por ajuda.";
        String marked=GeminiTtsMarkupDirector.annotateForModel("gemini-2.5-flash-preview-tts",raw);
        check(marked.contains("[whispers]"),"2.5 deve marcar sussurro com tag documentada");
        check(marked.contains("[shouting]"),"2.5 deve marcar grito com tag documentada");
        check(GeminiTtsMarkupDirector.validate(marked),"markup 2.5 deve ser válido");
        check(GeminiTtsMarkupDirector.stripTags(marked).equals(raw),"tags não podem alterar as palavras visíveis");
        check(GeminiTtsMarkupDirector.supportsInlineTags("gemini-2.5-flash-preview-tts"),"2.5 Preview TTS deve suportar tags");
        check(GeminiTtsMarkupDirector.supportsInlineTags("gemini-3.1-flash-tts-preview"),"3.1 Preview TTS deve compartilhar markup de performance");
        String urgent=GeminiTtsMarkupDirector.annotateForModel("gemini-3.1-flash-tts-preview","Precisamos sair daqui agora.","urgente",5,"curto","Narrador");
        check(urgent.equals("Precisamos sair daqui agora."),"urgência sem verbo explícito deve ficar para as Director Notes, sem tag experimental");
        check(!GeminiTtsMarkupDirector.validate("[short pause] Algo se move."),"tags legadas de pausa não entram mais no transcript");
        check(!GeminiTtsMarkupDirector.validate("[whispering] Quem está aí?"),"alias legado deve ser bloqueado");
        String suspense=GeminiTtsMarkupDirector.annotateForModel("gemini-2.5-flash-preview-tts","O vento gélido apaga os sons da estrada. A patrulha surge entre as árvores. O silêncio volta a fechar o caminho. A fumaça cresce atrás da colina.","baixo",3,"longo","Narrador");
        check(suspense.equals(GeminiTtsMarkupDirector.stripTags(suspense)),"cena tensa sem verbo explícito não deve receber tags experimentais");
        check(GeminiTtsMarkupDirector.tagCount(suspense)==0,"arco semântico deve ser dirigido pelo prompt, não por tags arriscadas");
        System.out.println("OK: "+checks+" verificações de markup dirigido Gemini TTS.");
    }
}
