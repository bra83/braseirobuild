package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2024 encumbrance projection. The item ledger is authoritative; legacy prose is
 * read once only if an old sheet has no entries and no explicit empty-ledger marker.
 * Never manufacture weights for unidentified equipment, resurrect removed gear,
 * or overwrite a player's descriptive inventory/biography.
 *
 * Units: pounds (1 lb = 0.45359237 kg), 50 coins per pound.
 * Source for catalogue and capacity: PHB 2024 Basic Rules, Equipment and
 * Rules Glossary (https://www.dndbeyond.com/sources/dnd/br-2024/equipment).
 */
public final class CharacterInventory2024 {
    private CharacterInventory2024() {}
    private static final double KG_PER_LB=0.45359237d;
    private static final Pattern COUNT=Pattern.compile("^(\\d+)\\s+(.+)$");
    private static final Pattern MONEY=Pattern.compile("(?i)(\\d+)\\s*(?:PO|PP|PC|PE|PL)\\b");
    private static final Map<String,Double> WEIGHTS=new HashMap<>();

    private static void weight(double lb,String... aliases){
        for(String name:aliases) WEIGHTS.put(normalize(name),lb);
    }
    static {
        weight(1,"adaga","adagas","dagger");
        weight(4,"bordão","bordao","foco arcano (bordão)","bordão (foco arcano)",
            "bordao_foco","foco druídico (bordão)","foco druidico (bordao)","quarterstaff");
        weight(4,"robe","túnica","tunica","robes");
        weight(3,"grimório","grimorio","grimório de mago","grimorio de mago","spellbook");
        weight(22,"pacote de estudioso","mochila de estudioso","scholar's pack");
        weight(55,"mochila de explorador","pacote de explorador","explorer's pack",
            "mochila de explorador de masmorras","pacote de explorador de masmorras",
            "mochila de masmorras","dungeoneer's pack");
        weight(42,"mochila de assaltante","pacote de assaltante","burglar's pack");
        weight(58.5,"mochila de artista","pacote de artista","entertainer's pack");
        weight(29,"pacote de sacerdote","priest's pack");
        weight(2,"alaúde","alaude","lute","lira","lyre","martelo leve","machadinha","machadinhas");
        weight(1,"flauta","flute","foco arcano (cristal)","cristal","arco?"); // named crystal focus
        WEIGHTS.remove("arco"); // no ambiguous bow weight
        weight(4,"roupas de viajante","roupas viajante","clothes, traveler's");
        weight(7,"saco de dormir","bedroll","machado grande","greataxe");
        weight(3,"cobertor","blanket","lança","spear","espada longa","longsword",
            "cimitarra","scimitar");
        weight(10,"armadura de couro","couro","leather armor","cota de couro");
        weight(13,"armadura de couro batido","couro batido","studded leather");
        weight(6,"escudo","shield","espada grande","greatsword");
        weight(20,"camisão de malha","camisao de malha","chain shirt");
        weight(55,"cota de malha","chain mail");
        weight(4,"maça","maca","mace","kit de corda","kit de herbalismo","herbalism kit",
            "foco druídico (bordão de madeira)","wooden staff");
        WEIGHTS.put(normalize("kit de herbalismo"),3d);
        WEIGHTS.remove(normalize("kit de corda"));
        weight(2,"foice","sickle","mangual","flail","espada curta","shortsword",
            "arco curto","shortbow","arco longo","longbow","azagaia","azagaias","javelin","javelins");
        weight(1,"aljava","quiver","ferramentas de ladrão","thieves' tools");
        weight(0.05,"flecha","flechas","arrow","arrows");
        weight(3,"foco arcano (orbe)","orbe","orb");
        weight(5,"livro de conhecimento oculto","livro","book","mochila","backpack");
        weight(1,"foco arcano (varinha)","varinha","wand");
        weight(0,"foco druídico (raminho de visco)","raminho de visco","sprig of mistletoe");
        weight(8,"utensílios de cozinheiro","utensilios de cozinheiro","cook's utensils");
        weight(2,"corda de 15 m","corda de 15m"); // intentionally replaced below with SRD2024 rope
        weight(5,"corda de 15 m","corda de 15m","rope","corda");
        weight(2,"poção de cura","potion of healing"); // replaced with exact 2024 0.5 lb
        weight(0.5,"poção de cura","potion of healing");
        weight(2,"cajado","staff (arcane focus)"); // corrected below for focus staff
        weight(4,"cajado","staff (arcane focus)");
        weight(1,"saco","pouch");
    }

    public static String normalize(String s){
        return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();
    }

    /** Null means unknown, NEVER zero. Prefer explicit verified weight on an entry. */
    public static Double catalogWeight(String name,String id){
        Double v=WEIGHTS.get(normalize(name));
        return v!=null?v:WEIGHTS.get(normalize(id));
    }

    private static JSONArray legacyItems(String raw,JSONArray notes) throws Exception{
        JSONArray out=new JSONArray();
        if(raw==null||raw.trim().isEmpty())return out;
        for(String line:raw.split("\\n")){
            String n=normalize(line);
            if(n.isEmpty())continue;
            if(n.startsWith("nao comprado")||n.startsWith("nao adquirid")
                    ||n.startsWith("moedas atuais")||n.startsWith("proficiência")
                    ||n.startsWith("proficiencia"))continue;
            if(n.startsWith("antecedente")&&(n.contains("conforme")||n.contains("sem conceder")
                    ||n.contains("opcao b"))) {
                if(n.contains("conforme"))notes.put("background_equipment_not_itemized");
                continue;
            }
            String text=line;
            int colon=line.indexOf(':');
            if(colon>=0 && (n.startsWith("equipamento inicial")||n.startsWith("comprados")
                    ||n.startsWith("inventario")||n.startsWith("inventário")
                    ||n.startsWith("itens adquiridos")))
                text=line.substring(colon+1);
            for(String part:text.split("[;,]")){
                String value=part.trim().replaceAll("[.]$","").trim();
                if(value.isEmpty()||value.toLowerCase(Locale.ROOT).contains("não comprado")
                    ||value.toLowerCase(Locale.ROOT).contains("nao comprado"))continue;
                // A purchase price is not an item weight; strip price annotation only.
                value=value.replaceAll("(?i)\\s*\\(\\s*\\d+\\s*(?:PO|PP|PC|PE|PL)\\s*\\)","").trim();
                if(MONEY.matcher(value).find() &&
                     (value.matches("(?i)^\\d+\\s*(PO|PP|PC|PE|PL).*$")
                       ||normalize(value).startsWith("moedas")))continue;
                int quantity=1;
                Matcher m=COUNT.matcher(value);
                if(m.matches()){
                    try{quantity=Integer.parseInt(m.group(1));}catch(NumberFormatException e){
                        notes.put("unreadable_quantity:"+value);continue;
                    }
                    value=m.group(2).trim();
                }
                if(quantity<=0)continue;
                if(value.isEmpty())continue;
                if(normalize(value).contains(" conforme o")||normalize(value).contains(" a escolha")
                        ||normalize(value).contains(" ou "))notes.put("unresolved_choice:"+value);
                Double known=catalogWeight(value,"");
                JSONObject entry=new JSONObject().put("id","legacy_"+out.length())
                    .put("name",value).put("quantity",quantity).put("carried",true)
                    .put("source","legacy_inventory_inference");
                if(known!=null)entry.put("unit_weight_lb",known);
                out.put(entry);
            }
        }
        return out;
    }

    private static JSONArray cloneArray(JSONArray a){
        return a==null?new JSONArray():new JSONArray(a.toString());
    }

    private static boolean carried(JSONObject row){
        if(row.has("carried")&&!row.optBoolean("carried",true))return false;
        String location=normalize(row.optString("location",""));
        return !(location.equals("storage")||location.equals("stored")||location.equals("stash")
              ||location.equals("ground")||location.equals("dropped")||location.equals("depot")
              ||location.equals("deposito")||location.equals("chao")||location.equals("montaria"));
    }

    private static JSONObject coins(JSONObject sheet){
        JSONObject source=sheet.optJSONObject("coin_pieces");
        if(source!=null)return source;
        String raw=sheet.optString("inventory",sheet.optString("equipment",""));
        Matcher line=Pattern.compile("(?im)^\\s*moedas atuais\\s*:\\s*([^\\n]+)").matcher(raw);
        if(!line.find())return null;
        Matcher m=MONEY.matcher(line.group(1));
        int count=0;
        while(m.find()){
            try{count=Math.addExact(count,Integer.parseInt(m.group(1)));}
            catch(ArithmeticException|NumberFormatException bad){return null;}
        }
        try{return new JSONObject().put("count",count);}
        catch(Exception bad){return null;}
    }

    private static double coinWeight(JSONObject sheet){
        JSONObject c=coins(sheet);
        if(c==null)return 0d;
        if(c.has("count"))return Math.max(0,c.optInt("count",0))/50d;
        long total=0;
        for(String key:new String[]{"cp","sp","ep","gp","pp","pc","po","pl"}){
            Object v=c.opt(key);
            if(v==null)continue;
            if(!(v instanceof Number)||((Number)v).longValue()<0)return Double.NaN;
            total+=((Number)v).longValue();
        }
        return total/50d;
    }

    private static double sizeMultiplier(String size){
        String s=normalize(size);
        if(s.equals("tiny")||s.equals("miudo")||s.equals("minusculo"))return 0.5;
        if(s.equals("small")||s.equals("pequeno")||s.equals("medium")||s.equals("medio"))return 1;
        if(s.equals("large")||s.equals("grande"))return 2;
        if(s.equals("huge")||s.equals("enorme"))return 4;
        if(s.equals("gargantuan")||s.equals("colossal"))return 8;
        return Double.NaN;
    }

    public static JSONObject reconcile(JSONObject sheet){
        if(sheet==null||!Character2024SheetReconciler.applies(sheet))return sheet;
        try{
            JSONArray ledger=sheet.optJSONArray("inventory_ledger");
            if(ledger==null)ledger=new JSONArray();
            JSONArray notes=new JSONArray();
            boolean authoritative="structured_authoritative".equals(
                sheet.optString("inventory_ledger_status",""));
            if(ledger.length()==0&&!authoritative&&
                    !sheet.optBoolean("inventory_explicitly_empty",false)&&
                    !sheet.has("inventory_ledger_migration_version")){
                String legacy=sheet.optString("inventory",sheet.optString("equipment",""));
                JSONArray recovered=legacyItems(legacy,notes);
                if(recovered.length()>0)ledger=recovered;
                sheet.put("inventory_ledger_migration_version",1);
                sheet.put("inventory_legacy_source_hash",legacy.hashCode());
            }
            JSONArray updated=new JSONArray();
            JSONArray problems=new JSONArray();
            Set<String> ids=new HashSet<>();
            double known=0;
            int count=0,unknown=0;
            for(int i=0;i<ledger.length();i++){
                JSONObject original=ledger.optJSONObject(i);
                if(original==null){problems.put("invalid_row:"+i);unknown++;updated.put(ledger.opt(i));continue;}
                JSONObject row=new JSONObject(original.toString());
                String id=row.optString("id","");
                if(!id.isEmpty()&&!ids.add(id)){problems.put("duplicate_id:"+id);unknown++;}
                Object qtyValue=row.opt("quantity");
                int qty=row.optInt("quantity",1);
                if(qty<0||(qtyValue!=null && !(qtyValue instanceof Number))
                         ||(qtyValue instanceof Number &&
                         ((Number)qtyValue).doubleValue()!=qty)){
                    unknown++;problems.put("invalid_quantity:"+id);updated.put(row);continue;
                }
                if(qty==0||!carried(row)){updated.put(row);continue;}
                count+=qty;
                Object unit=row.opt("unit_weight_lb");
                if(unit==null){
                    Double fromCatalog=catalogWeight(row.optString("name",""),id);
                    if(fromCatalog!=null){row.put("unit_weight_lb",fromCatalog);
                        row.put("weight_source","srd2024_catalog");unit=fromCatalog;}
                }
                if(!(unit instanceof Number)||!Double.isFinite(((Number)unit).doubleValue())
                        ||((Number)unit).doubleValue()<0){
                    unknown++;problems.put("weight_unknown:"+row.optString("name",id));
                }else{
                    double value=((Number)unit).doubleValue()*qty;
                    if(!Double.isFinite(value)){unknown++;problems.put("weight_overflow:"+id);}
                    else known+=value;
                }
                updated.put(row);
            }
            String legacy=sheet.optString("inventory",sheet.optString("equipment",""));
            if(sheet.has("inventory_legacy_source_hash") &&
                    sheet.optInt("inventory_legacy_source_hash")!=legacy.hashCode())
                notes.put("legacy_inventory_text_changed_review_required");
            for(int i=0;i<notes.length();i++)problems.put(notes.optString(i));
            double coinsLb=coinWeight(sheet);
            if(!Double.isFinite(coinsLb))problems.put("invalid_coin_ledger");
            else known+=coinsLb;
            int strength=sheet.optInt("str",0);
            double size=sizeMultiplier(sheet.optString("size","Médio"));
            double special=sheet.optDouble("carrying_capacity_multiplier",1d);
            if(sheet.optBoolean("powerful_build",false)&&!sheet.has("carrying_capacity_multiplier"))
                special=2d;
            double capacity=strength*15d*size*special;
            boolean validCapacity=strength>=1&&strength<=30&&Double.isFinite(capacity)
                   &&capacity>0&&Double.isFinite(special)&&special>0;
            if(!validCapacity)problems.put("unknown_capacity_rule");
            boolean complete=unknown==0&&problems.length()==0;
            sheet.put("inventory_ledger",updated);
            sheet.put("inventory_known_weight_lb",known);
            sheet.put("inventory_known_weight_kg",known*KG_PER_LB);
            sheet.put("inventory_weight_complete",complete);
            sheet.put("inventory_unknown_weight_count",unknown);
            sheet.put("inventory_carried_quantity",count);
            sheet.put("inventory_weight_problems",problems);
            sheet.put("inventory_capacity_lb",validCapacity?capacity:JSONObject.NULL);
            sheet.put("inventory_capacity_kg",validCapacity?capacity*KG_PER_LB:JSONObject.NULL);
            sheet.put("inventory_total_weight_lb",complete?known:JSONObject.NULL);
            sheet.put("inventory_total_weight_kg",complete?known*KG_PER_LB:JSONObject.NULL);
            sheet.put("inventory_over_capacity",validCapacity&&known>capacity);
            sheet.put("inventory_weight_status",complete?"complete":"review_required");
            sheet.put("inventory_weight_display",complete?
                String.format(Locale.ROOT,"%.2f lb (%.2f kg)",known,known*KG_PER_LB):
                String.format(Locale.ROOT,">= %.2f lb (parcial; revisar itens)",known));
            // Legacy carrying_capacity remains a display string, never a computed burden.
            if(validCapacity)sheet.put("carrying_capacity",
                String.format(Locale.ROOT,"%.2f lb (%.2f kg)",capacity,capacity*KG_PER_LB));
            sheet.put("inventory_weight_projection_version",1);
        }catch(Exception e){
            throw new IllegalStateException("Falha ao reconciliar carga da ficha 2024",e);
        }
        return sheet;
    }

    /**
     * Idempotent atomic sheet mutation. Call at an authorized player/master
     * commit boundary, then persist the returned sheet in the same DB transaction.
     * An uncommitted preview must NOT write this result to storage.
     */
    public static JSONObject applyMutation(JSONObject input,JSONObject action){
        if(input==null||action==null)throw new IllegalArgumentException("sheet_and_action_required");
        String receipt=action.optString("event_id","").trim();
        if(receipt.isEmpty())throw new IllegalArgumentException("inventory_event_id_required");
        if(!Character2024SheetReconciler.applies(input))
            throw new IllegalArgumentException("requires_2024_sheet");
        JSONObject sheet=new JSONObject(input.toString());
        JSONArray applied=cloneArray(sheet.optJSONArray("inventory_event_receipts"));
        for(int i=0;i<applied.length();i++)if(receipt.equals(applied.optString(i)))
            return reconcile(sheet); // replay, never duplicate gain/loss
        JSONArray ledger=cloneArray(sheet.optJSONArray("inventory_ledger"));
        String op=normalize(action.optString("operation",""));
        String id=action.optString("item_id","").trim();
        int index=-1;
        for(int i=0;i<ledger.length();i++){
            JSONObject row=ledger.optJSONObject(i);
            if(row!=null&&id.equals(row.optString("id",""))){index=i;break;}
        }
        JSONArray next=new JSONArray();
        try{
            if(op.equals("add")){
                JSONObject item=action.optJSONObject("item");
                if(item==null||item.optString("name","").trim().isEmpty())
                    throw new IllegalArgumentException("item_name_required");
                String newId=item.optString("id","").trim();
                if(newId.isEmpty())throw new IllegalArgumentException("stable_item_id_required");
                for(int i=0;i<ledger.length();i++)if(newId.equals(
                        ledger.optJSONObject(i)==null?"":ledger.optJSONObject(i).optString("id")))
                    throw new IllegalArgumentException("duplicate_item_id");
                int qty=item.optInt("quantity",1);
                if(qty<=0)throw new IllegalArgumentException("invalid_quantity");
                for(int i=0;i<ledger.length();i++)next.put(ledger.opt(i));
                next.put(new JSONObject(item.toString()));
            }else{
                if(id.isEmpty()||index<0)throw new IllegalArgumentException("item_not_found");
                for(int i=0;i<ledger.length();i++){
                    JSONObject row=ledger.optJSONObject(i);
                    if(row==null)throw new IllegalArgumentException("invalid_ledger_row");
                    if(i!=index){next.put(row);continue;}
                    JSONObject changed=new JSONObject(row.toString());
                    if(op.equals("remove"))continue;
                    if(op.equals("adjust")){
                        Object deltaValue=action.opt("delta");
                        if(!(deltaValue instanceof Number))throw new IllegalArgumentException("delta_required");
                        long qty=(long)row.optInt("quantity",1)+((Number)deltaValue).longValue();
                        if(qty<0||qty>Integer.MAX_VALUE)throw new IllegalArgumentException("quantity_out_of_range");
                        if(qty==0)continue;
                        changed.put("quantity",(int)qty);
                    }else if(op.equals("update")){
                        for(String key:new String[]{"name","quantity","unit_weight_lb","carried",
                                "equipped","location","container_id"}){
                            if(action.has(key))changed.put(key,action.opt(key));
                        }
                    }else if(op.equals("move")){
                        if(!action.has("carried")&&!action.has("location"))
                            throw new IllegalArgumentException("location_required");
                        if(action.has("carried"))changed.put("carried",action.opt("carried"));
                        if(action.has("location"))changed.put("location",action.opt("location"));
                    }else throw new IllegalArgumentException("unknown_inventory_operation");
                    if(changed.optString("name","").trim().isEmpty())
                        throw new IllegalArgumentException("item_name_required");
                    if(changed.optInt("quantity",1)<0)
                        throw new IllegalArgumentException("invalid_quantity");
                    if(changed.has("unit_weight_lb")){
                        Object w=changed.opt("unit_weight_lb");
                        if(!(w instanceof Number)||!Double.isFinite(((Number)w).doubleValue())
                                ||((Number)w).doubleValue()<0)
                            throw new IllegalArgumentException("invalid_weight");
                    }
                    next.put(changed);
                }
            }
            sheet.put("inventory_ledger",next);
            sheet.put("inventory_ledger_status","structured_authoritative");
            sheet.put("inventory_ledger_migration_version",1);
            // The old descriptive inventory is retained for history; no resurrection.
            applied.put(receipt);
            sheet.put("inventory_event_receipts",applied);
            return reconcile(sheet);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalStateException("inventory_mutation_failed",e);}
    }
}
