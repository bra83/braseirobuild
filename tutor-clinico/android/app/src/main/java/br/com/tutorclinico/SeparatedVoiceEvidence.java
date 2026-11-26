package br.com.tutorclinico;
final class SeparatedVoiceEvidence {
    static boolean isCandidate(String speaker){return "separada_a".equals(speaker)||"separada_b".equals(speaker);}
    static String segmentId(String job,long start,long end,String speaker){return job+"_"+start+"_"+end+(isCandidate(speaker)?"_"+speaker:"");}
    static boolean candidateId(String id){return id!=null&&(id.endsWith("_separada_a")||id.endsWith("_separada_b"));}
}
