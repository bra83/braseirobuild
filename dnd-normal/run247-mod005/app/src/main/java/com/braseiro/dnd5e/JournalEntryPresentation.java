package com.braseiro.dnd5e;

/**
 * Presentation-only migration for old scene journal rows. Existing rows are
 * kept verbatim in SQLite/backups; user-facing views and exports omit internal
 * policy, source-book excerpts and model scaffolding from ACTION DECLARADA.
 */
public final class JournalEntryPresentation {
    private JournalEntryPresentation(){}
    public static String safeBody(String body){
        if(body==null)return "";
        String actionMarker="AÇÃO DECLARADA:\n";
        String narrationMarker="\n\nNARRAÇÃO DO MESTRE:\n";
        if(!body.startsWith(actionMarker))return body;
        int narr=body.indexOf(narrationMarker);
        if(narr<0)return body;
        String actionRaw=body.substring(actionMarker.length(),narr);
        String roll="";
        int rollIndex=actionRaw.indexOf("\n\nROLAGEM / RESOLUÇÃO MECÂNICA:\n");
        if(rollIndex>=0){
            roll=actionRaw.substring(rollIndex);
            actionRaw=actionRaw.substring(0,rollIndex);
        }
        String cleaned=SceneActionClock.cleanPlayerAction(actionRaw);
        String story=body.substring(narr+narrationMarker.length());
        StringBuilder out=new StringBuilder();
        if(!cleaned.isEmpty())out.append(actionMarker).append(cleaned).append("\n\n");
        if(!roll.isEmpty())out.append(roll.trim()).append("\n\n");
        out.append("NARRAÇÃO DO MESTRE:\n").append(story);
        return out.toString();
    }
}
