package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Estrutura determinística de uma página. Nenhuma classificação torna uma regra executável. */
public final class ManualPageStructureAnalyzer {
    private static final Pattern PAGE_REF=Pattern.compile("(?iu)\\b(?:p(?:á|a)gina|page|cap(?:í|i)tulo|chapter|tabela|table)\\s+(?:anterior|previous|seguinte|next|\\d{1,4}|[ivxlcdm]{1,8})\\b");
    private static final Pattern BULLET=Pattern.compile("(?m)^\\s*(?:[-•*]|\\d{1,3}[.)])\\s+\\S+");
    private static final Pattern TABLE_ROW=Pattern.compile("(?m)^\\s*\\S+(?:\\s{2,}|\\t)\\S+(?:\\s{2,}|\\t)\\S+.*$");
    private static final Pattern STAT_LINE=Pattern.compile("(?iu)^(?:CA|AC|PV|HP|deslocamento|speed|FOR|STR|DES|DEX|CON|INT|SAB|WIS|CAR|CHA|salvaguardas?|saving throws?|perícias?|skills?|sentidos?|senses?|idiomas?|languages?|ND|CR)\\b");

    private ManualPageStructureAnalyzer(){}

    public static JSONObject analyze(String text,int pageNo){
        JSONObject out=new JSONObject();JSONArray headings=new JSONArray(),refs=new JSONArray();
        try{
            String source=text==null?"":text.replace('\u0000',' ').trim();String[] lines=source.split("\\r?\\n");
            String chapter="",section="";int tableRows=0,statRows=0,bullets=0;
            for(int i=0;i<lines.length&&i<400;i++){
                String line=lines[i].replaceAll("\\s+"," ").trim();if(line.length()==0)continue;
                if(BULLET.matcher(lines[i]).find())bullets++;
                if(TABLE_ROW.matcher(lines[i]).find())tableRows++;
                if(STAT_LINE.matcher(line).find())statRows++;
                if(isHeading(line)){
                    headings.put(new JSONObject().put("text",line).put("line",i+1));
                    if(chapter.length()==0&&looksLikeChapter(line))chapter=line;
                    section=line;
                }
                Matcher ref=PAGE_REF.matcher(line);while(ref.find()&&refs.length()<60)putUnique(refs,ref.group().trim());
            }
            String kind=statRows>=3?"creature_block":tableRows>=3?"table":bullets>=2?"list":headings.length()>0?"section":"prose";
            out.put("page",Math.max(1,pageNo)).put("chapter_title",chapter).put("section_title",section)
                    .put("content_kind",kind).put("headings",headings).put("cross_references",refs)
                    .put("continues_from_previous",startsAsContinuation(source)).put("continues_on_next",endsAsContinuation(source))
                    .put("table_row_count",tableRows).put("bullet_count",bullets).put("stat_line_count",statRows)
                    .put("page_hash",sha256(source));
        }catch(Exception error){
            AppLog.w("ManualStructure","Falha ao classificar a estrutura da página "+pageNo+".",error);
            try{out.put("page",Math.max(1,pageNo)).put("content_kind","unknown").put("review_required",true);}catch(Exception fallbackError){AppLog.w("ManualStructure","Falha ao registrar revisão obrigatória da página.",fallbackError);}
        }
        return out;
    }

    private static boolean isHeading(String line){
        if(line.length()<3||line.length()>100||line.matches(".*[.!?]$"))return false;
        if(STAT_LINE.matcher(line).find())return false;
        if(looksLikeChapter(line))return true;
        int letters=0,upper=0;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(Character.isLetter(c)){letters++;if(Character.isUpperCase(c))upper++;}}
        return letters>=3&&upper/(double)letters>=0.78d;
    }
    private static boolean looksLikeChapter(String line){return normalize(line).matches("^(capitulo|chapter|livro|book|parte|part|secao|section)\\b.*");}
    private static boolean startsAsContinuation(String text){String n=normalize(text);return n.matches("^(e|ou|mas|entao|then|and|or|but|alem disso|in addition)\\b.*")||text.startsWith("…");}
    private static boolean endsAsContinuation(String text){String s=text.trim();if(s.length()==0)return false;return s.endsWith(":")||s.endsWith(";")||s.endsWith(",")||s.endsWith("—")||s.endsWith("-");}
    private static void putUnique(JSONArray a,String value){for(int i=0;i<a.length();i++)if(value.equalsIgnoreCase(a.optString(i)))return;a.put(value);}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String sha256(String value)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] hash=d.digest(value.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:hash)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();}
}
