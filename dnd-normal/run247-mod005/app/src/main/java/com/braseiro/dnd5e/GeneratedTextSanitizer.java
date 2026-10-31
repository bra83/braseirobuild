package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normaliza respostas curtas da IA e impede JSON/markdown bruto em campos da interface. */
public final class GeneratedTextSanitizer {
    private static final String[] COMMON_KEYS={
            "valor","value","texto","text","resultado","result","conteudo","content",
            "titulo","title","nome","name","apelido","nickname","resposta","answer"
    };

    private GeneratedTextSanitizer(){}

    public static String scalar(String raw,String preferredKey){
        return scalar(raw,preferredKey,0);
    }

    /**
     * Converte fragmentos estruturados que escaparam da resposta da IA para texto
     * humano. Ex.: "FALAR COM {NAME:Kaelen,VISUAL_DESCRIPTION:...}" vira
     * "FALAR COM Kaelen". Objetos sem identidade reconhecível são removidos em vez
     * de aparecerem crus na interface.
     */
    public static String humanText(String raw){
        String clean=unwrapFences(raw);
        if(clean.length()==0)return "";
        StringBuilder out=new StringBuilder(clean.length());
        int i=0;
        while(i<clean.length()){
            char ch=clean.charAt(i);
            if(ch!='{'){out.append(ch);i++;continue;}
            int end=findBalancedObjectEnd(clean,i);
            if(end<0){
                String fragment=clean.substring(i);
                if(hasStructuredMarkers(fragment)){
                    String replacement=extractLooseObjectField(fragment,"name");
                    if(replacement.length()==0)replacement=extractLooseObjectField(fragment,"nome");
                    if(replacement.length()>0)out.append(PortugueseNarrationSanitizer.sanitize(unwrapQuoted(replacement)));
                    break;
                }
                out.append(ch);i++;continue;
            }
            String fragment=clean.substring(i,end+1);
            if(looksLikeStructuredEntity(fragment)){
                String replacement=scalar(fragment,"name",0);
                if(replacement.length()==0)replacement=scalar(fragment,"title",0);
                if(replacement.length()==0)replacement=scalar(fragment,"label",0);
                if(replacement.length()>0&&!looksLikeStructuredEntity(replacement))out.append(replacement);
            }else out.append(fragment);
            i=end+1;
        }
        return PortugueseNarrationSanitizer.sanitize(out.toString()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .trim());
    }

    public static String narrative(String raw){
        String clean=unwrapFences(raw);
        if(looksLikeJson(clean)){
            String extracted=scalar(clean,"narration",0);
            if(extracted.length()>0&&!extracted.equals(clean))clean=extracted;
        }
        clean=stripInternalNarrativeLeak(clean);
        clean=NarrativeGuardService.sanitize(clean);
        return PortugueseNarrationSanitizer.sanitize(clean);
    }

    private static String stripInternalNarrativeLeak(String raw){
        if(raw==null||raw.trim().length()==0)return "";
        // Preserve parágrafos: a 6.2.x filtrava por sentença e remontava tudo com espaços,
        // destruindo as quebras pedidas ao Mestre e criando a “parede de texto” observada em jogo.
        String normalized=raw.replace('\r','\n').replaceAll("\\n{3,}","\n\n");
        String[] paragraphs=normalized.split("\\n\\s*\\n");StringBuilder out=new StringBuilder();
        for(String paragraph:paragraphs){
            if(NarrativeGuardService.containsInternalLeak(paragraph))continue;
            StringBuilder kept=new StringBuilder();String[] parts=paragraph.trim().split("(?<=[.!?…])\\s+|\\n+");
            for(String part:parts){String p=part.trim();if(p.length()==0)continue;
                if(NarrativeGuardService.containsInternalLeak(p))continue;
                if(kept.length()>0)kept.append(' ');kept.append(p);
            }
            if(kept.length()>0){if(out.length()>0)out.append("\n\n");out.append(kept);}
        }
        return out.toString().trim();
    }

    private static String scalar(String raw,String preferredKey,int depth){
        if(raw==null||depth>3)return "";
        String clean=unwrapFences(raw).trim();
        if(clean.length()==0)return "";
        // Objetos pseudo-JSON podem chegar truncados ou com chaves não citadas.
        // Extraia primeiro o campo solicitado quando houver marcadores estruturados.
        if(clean.startsWith("{")&&hasStructuredMarkers(clean)){
            String loose=extractLooseObjectField(clean,preferredKey);
            if(loose.length()==0)loose=extractLooseObjectField(clean,normalizeKey(preferredKey));
            if(loose.length()>0)return PortugueseNarrationSanitizer.sanitize(unwrapQuoted(loose));
        }
        // JSONTokener encerrava valores não citados no primeiro espaço. Portanto,
        // uma resposta textual normal como "Devoto Risonho" não deve ser tratada
        // como JSON; texto simples permanece inteiro.
        if(!looksLikeJson(clean)){
            return PortugueseNarrationSanitizer.sanitize(unwrapQuoted(clean));
        }
        try{
            Object parsed;
            if(clean.startsWith("{"))parsed=new JSONObject(clean);
            else if(clean.startsWith("["))parsed=new JSONArray(clean);
            else parsed=unwrapQuoted(clean);
            if(parsed instanceof JSONObject){
                JSONObject object=(JSONObject)parsed;
                String[] requested={preferredKey,normalizeKey(preferredKey)};
                for(String key:requested){
                    if(key==null||key.length()==0)continue;
                    Object rawValue=optIgnoreCase(object,key);
                    if(rawValue==null||rawValue==JSONObject.NULL)continue;
                    String value=objectValue(rawValue,preferredKey,depth+1);
                    if(value.length()>0)return value;
                }
                for(String key:COMMON_KEYS){
                    Object rawValue=optIgnoreCase(object,key);
                    if(rawValue==null||rawValue==JSONObject.NULL)continue;
                    String value=objectValue(rawValue,preferredKey,depth+1);
                    if(value.length()>0)return value;
                }
                if(object.length()==1){
                    String key=object.keys().next();
                    String value=objectValue(object.opt(key),preferredKey,depth+1);
                    if(value.length()>0)return value;
                }
                return "";
            }
            if(parsed instanceof JSONArray){
                JSONArray array=(JSONArray)parsed;
                if(array.length()==1)return objectValue(array.opt(0),preferredKey,depth+1);
                StringBuilder out=new StringBuilder();
                for(int i=0;i<array.length();i++){
                    Object item=array.opt(i);
                    if(item==null||item instanceof JSONObject||item instanceof JSONArray)continue;
                    String value=String.valueOf(item).trim();
                    if(value.length()==0)continue;
                    if(out.length()>0)out.append("; ");
                    out.append(value);
                }
                clean=out.toString();
            }else if(parsed!=null&&parsed!=JSONObject.NULL){
                clean=String.valueOf(parsed);
            }
        }catch(Exception ignored){
            String loose=extractLooseObjectField(clean,preferredKey);
            if(loose.length()==0)loose=extractLooseObjectField(clean,normalizeKey(preferredKey));
            if(loose.length()==0){
                for(String key:COMMON_KEYS){loose=extractLooseObjectField(clean,key);if(loose.length()>0)break;}
            }
            if(loose.length()>0)return PortugueseNarrationSanitizer.sanitize(unwrapQuoted(loose));
            clean=unwrapQuoted(clean);
        }
        return PortugueseNarrationSanitizer.sanitize(clean);
    }

    private static String objectValue(Object value,String preferredKey,int depth){
        if(value==null||value==JSONObject.NULL)return "";
        return scalar(String.valueOf(value),preferredKey,depth);
    }

    private static Object optIgnoreCase(JSONObject object,String key){
        if(object==null||key==null||key.length()==0)return null;
        Iterator<String> keys=object.keys();
        while(keys.hasNext()){
            String actual=keys.next();
            if(actual!=null&&actual.equalsIgnoreCase(key))return object.opt(actual);
        }
        return null;
    }

    private static String extractLooseObjectField(String raw,String key){
        if(raw==null||key==null||key.trim().isEmpty())return "";
        try{
            String expression="(?i)(?:^|[{,;])\\s*[\\\"']?"+Pattern.quote(key.trim())+
                    "[\\\"']?\\s*[:=]\\s*[\\\"']?([^,}\\]\\r\\n]+)";
            Matcher matcher=Pattern.compile(expression).matcher(raw);
            if(!matcher.find())return "";
            String value=matcher.group(1)==null?"":matcher.group(1).trim();
            value=value.replaceAll("[\\\"']\\s*$","");
            return value.trim();
        }catch(Exception ignored){
            return "";
        }
    }

    private static int findBalancedObjectEnd(String text,int start){
        int depth=0;boolean quoted=false;char quote=0;boolean escaped=false;
        for(int i=start;i<text.length();i++){
            char c=text.charAt(i);
            if(quoted){
                if(escaped){escaped=false;continue;}
                if(c=='\\'){escaped=true;continue;}
                if(c==quote){quoted=false;quote=0;}
                continue;
            }
            if(c=='"'||c=='\''){quoted=true;quote=c;continue;}
            if(c=='{')depth++;
            else if(c=='}'){depth--;if(depth==0)return i;}
        }
        return -1;
    }

    private static boolean looksLikeStructuredEntity(String fragment){
        if(fragment==null)return false;
        String n=fragment.toLowerCase(Locale.ROOT);
        return n.startsWith("{")&&n.endsWith("}")&&hasStructuredMarkers(n);
    }

    private static boolean hasStructuredMarkers(String fragment){
        if(fragment==null)return false;
        String n=fragment.toLowerCase(Locale.ROOT);
        return n.matches("(?s).*\\b(name|nome|visual_description|visualdescription|role|papel|stats_json|summary|resumo)\\b\\s*[:=].*");
    }

    private static String unwrapFences(String raw){
        String clean=raw==null?"":raw.trim();
        if(clean.startsWith("```")){
            int firstBreak=clean.indexOf('\n');
            if(firstBreak>=0)clean=clean.substring(firstBreak+1);
            if(clean.endsWith("```"))clean=clean.substring(0,clean.length()-3);
        }
        return clean.trim();
    }

    private static String unwrapQuoted(String value){
        String clean=value==null?"":value.trim();
        if(clean.length()>=2&&((clean.startsWith("\"")&&clean.endsWith("\""))||(clean.startsWith("'")&&clean.endsWith("'")))){
            clean=clean.substring(1,clean.length()-1);
        }
        return clean.replace("\\n","\n").replace("\\\"","\"").trim();
    }

    private static boolean looksLikeJson(String value){
        if(value==null)return false;
        String clean=value.trim();
        return (clean.startsWith("{")&&clean.endsWith("}"))||(clean.startsWith("[")&&clean.endsWith("]"))||(clean.startsWith("\"")&&clean.endsWith("\""));
    }

    private static String normalizeKey(String key){
        if(key==null)return "";
        if("title".equalsIgnoreCase(key))return "titulo";
        if("name".equalsIgnoreCase(key))return "nome";
        if("nickname".equalsIgnoreCase(key))return "apelido";
        if("narration".equalsIgnoreCase(key))return "narracao";
        return key;
    }
}
