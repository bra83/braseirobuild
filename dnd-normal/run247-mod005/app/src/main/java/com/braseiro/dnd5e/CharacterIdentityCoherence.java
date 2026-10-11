package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Repara somente textos gerados por versões antigas que contradizem a ficha. */
public final class CharacterIdentityCoherence {
    private static final Pattern GENERATED_CLASS=Pattern.compile("(?iu)(classe\\s+)(.+?)(\\s+e mantém a mesma aparência entre cenas)");
    private static final Pattern HISTORY_CLASS=Pattern.compile("(?iu)(caminho de\\s+)(.+?)(\\.\\s+Um acontecimento)");
    private static final Pattern IDENTITY=Pattern.compile("(?iu)(identidade\\s+)[\\p{L}-]+");
    private static final Pattern SPECIES_FEATURE=Pattern.compile("(?iu)(Traços de espécie:\\s*)([^—\\n.]+)");
    private static final Pattern SUBCLASS_FEATURE=Pattern.compile("(?iu)(Subclasse de nível\\s+\\d+\\s*:\\s*)([^.\\n]+)");
    private static final String[] SPECIES={"Humano","Anão","Anã","Elfo","Elfa","Halfling","Golias","Gnomo","Gnoma","Draconato","Draconata","Tiefling","Aasimar","Orc","Meio-orc","Meio-elfo"};

    private CharacterIdentityCoherence() {}

    public static boolean repairLegacyRow(JSONObject row) {/*BR_JSON_WRAP*/try{
        if(row==null)return false;
        String species=clean(row.optString("species")),cls=clean(row.optString("class_name")),background=clean(row.isNull("background")?"":row.optString("background"));
        if(species.length()==0||cls.length()==0)return false;
        JSONObject sheet=parse(row.optString("sheet_json","{}"));
        String foreign=detectForeignSpecies(row,sheet,species);
        boolean changed=repairSheet(sheet,species,cls,background,foreign);
        changed|=repairRowField(row,"visual_description",species,cls,foreign);
        changed|=repairRowField(row,"spells",species,cls,foreign);
        changed|=repairRowField(row,"notes",species,cls,foreign);
        try{
            String serialized=sheet.toString();
            if(sheet.length()>0&&!serialized.equals(row.optString("sheet_json","{}"))){row.put("sheet_json",serialized);changed=true;}
        }catch(Exception ignored){return changed;}
        return changed;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static boolean repairSheet(JSONObject sheet,String species,String cls,String background,String foreignSpecies) {/*BR_JSON_WRAP*/try{
        if(sheet==null)return false;boolean changed=false;
        changed|=putDifferent(sheet,"species",species);
        changed|=putDifferent(sheet,"class_name",cls);
        if(clean(background).length()>0)changed|=putDifferent(sheet,"background",background);
        String[] fields={"concept","appearance","visual_description","history","life_path","features","spellcasting","spells","class_entry"};
        for(String field:fields){String before=sheet.optString(field,"");String after=repairGeneratedText(field,before,species,cls,foreignSpecies);if(!before.equals(after)){try{sheet.put(field,after);changed=true;}catch(Exception ignored){/* campo permanece como estava */}}}
        return changed;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean repairRowField(JSONObject row,String field,String species,String cls,String foreign){/*BR_JSON_WRAP*/try{
        String before=row.optString(field,"");String after=repairGeneratedText(field,before,species,cls,foreign);if(before.equals(after))return false;
        try{row.put(field,after);return true;}catch(Exception ignored){return false;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String repairGeneratedText(String field,String value,String species,String cls,String foreignSpecies){
        if(value==null||value.length()==0)return value==null?"":value;
        boolean generated=value.contains("mantém a mesma aparência entre cenas")||value.contains("Cresceu numa comunidade coerente com sua identidade")||value.contains("Traços de espécie:")||value.contains("Subclasse de nível")||("concept".equals(field)&&value.contains(" • ")&&value.contains(" — "));
        if(!generated)return value;
        String out=value;
        if("appearance".equals(field)||"visual_description".equals(field)){
            int comma=out.indexOf(',');if(comma>0)out=species+out.substring(comma);
            out=replaceGroup(GENERATED_CLASS,out,cls);
        }
        if("concept".equals(field)){
            String[] parts=out.split(" • ",3);if(parts.length>=2){parts[0]=species;parts[1]=cls;out=parts[0]+" • "+parts[1]+(parts.length==3?" • "+parts[2]:"");}
        }
        out=replaceGroup(HISTORY_CLASS,out,cls);
        out=IDENTITY.matcher(out).replaceAll(Matcher.quoteReplacement("identidade "+identityAdjective(species)));
        out=SPECIES_FEATURE.matcher(out).replaceAll(Matcher.quoteReplacement("Traços de espécie: "+species+" "));
        String subclass=subclass(cls);if(subclass.length()>0)out=replaceTail(SUBCLASS_FEATURE,out,subclass);
        if(foreignSpecies!=null&&foreignSpecies.length()>0){
            String badAdjective=identityAdjective(foreignSpecies);
            if(subclass.length()>0)out=out.replaceAll("(?iu)Linhagem\\s+"+Pattern.quote(badAdjective),Matcher.quoteReplacement(subclass));
        }
        return out.replaceAll("[ \\t]+\\n","\\n").replaceAll(" {2,}"," ").trim();
    }

    private static String replaceGroup(Pattern pattern,String value,String replacement){Matcher m=pattern.matcher(value);if(!m.find())return value;return m.replaceAll(Matcher.quoteReplacement(m.group(1)+replacement+m.group(3)));}
    private static String replaceTail(Pattern pattern,String value,String replacement){Matcher m=pattern.matcher(value);if(!m.find())return value;return m.replaceAll(Matcher.quoteReplacement(m.group(1)+replacement));}

    private static String detectForeignSpecies(JSONObject row,JSONObject sheet,String authoritative){
        String[] values={row.optString("visual_description"),row.optString("spells"),sheet.optString("appearance"),sheet.optString("visual_description"),sheet.optString("concept"),sheet.optString("features"),sheet.optString("spells")};
        for(String value:values){String n=normalize(value);for(String candidate:SPECIES)if(!normalize(candidate).equals(normalize(authoritative))&&containsWord(n,normalize(candidate)))return candidate;}
        return "";
    }

    private static boolean containsWord(String text,String word){return (" "+text+" ").contains(" "+word+" ");}
    private static String subclass(String cls){int at=cls.indexOf('—');if(at<0)at=cls.indexOf('-');return at<0?"":clean(cls.substring(at+1));}
    private static String identityAdjective(String species){String n=normalize(species);if(n.startsWith("humano"))return "humana";if(n.startsWith("anao")||n.startsWith("ana ")||n.equals("ana"))return "anã";if(n.startsWith("elf"))return "élfica";if(n.startsWith("gnom"))return "gnômica";if(n.startsWith("draconat"))return "draconata";if(n.startsWith("meio elfo"))return "meio-élfica";if(n.startsWith("meio orc"))return "meio-orc";return clean(species).toLowerCase(new Locale("pt","BR"));}
    private static JSONObject parse(String raw){try{return raw!=null&&raw.trim().startsWith("{")?new JSONObject(raw):new JSONObject();}catch(Exception ignored){return new JSONObject();}}
    private static boolean putDifferent(JSONObject target,String key,String value){/*BR_JSON_WRAP*/try{if(value==null||value.length()==0||value.equals(target.optString(key,"")))return false;try{target.put(key,value);return true;}catch(Exception ignored){return false;}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String clean(String value){return value==null?"":value.trim();}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
