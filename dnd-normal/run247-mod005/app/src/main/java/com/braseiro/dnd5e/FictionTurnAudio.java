package com.braseiro.dnd5e;

/** The audible answer must match the ACCEPTED text shown in the same scene. */
public final class FictionTurnAudio {
    private FictionTurnAudio(){}
    public static String select(String canonicalScene,String currentAcceptedAnswer){
        return currentAcceptedAnswer!=null&&!currentAcceptedAnswer.trim().isEmpty()
            ?currentAcceptedAnswer:(canonicalScene==null?"":canonicalScene);
    }
    public static boolean observation(String speaking,String acceptedAnswer){
        return acceptedAnswer!=null&&!acceptedAnswer.trim().isEmpty()
            &&speaking!=null&&speaking.trim().equals(acceptedAnswer.trim());
    }
}
