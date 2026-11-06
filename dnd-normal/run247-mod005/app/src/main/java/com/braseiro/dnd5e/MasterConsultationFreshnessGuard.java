package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Prevents a previous consultation answer from being recycled as the response
 * to a different question. It never affects the story or the player's choices. */
public final class MasterConsultationFreshnessGuard {
    private MasterConsultationFreshnessGuard() {}

    static boolean repeatsPreviousForDifferentQuestion(String question,String answer,JSONObject previous){
        if(previous==null||question==null||answer==null)return false;
        String currentQuestion=fold(question),previousQuestion=fold(previous.optString("question",""));
        String currentAnswer=fold(answer),previousAnswer=fold(previous.optString("answer",""));
        return !currentQuestion.isEmpty()&&!previousQuestion.isEmpty()
            &&!currentQuestion.equals(previousQuestion)
            &&currentAnswer.length()>=90&&currentAnswer.equals(previousAnswer);
    }
    private static String fold(String value){
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
}
