package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Caderno privado de Wilem, amarrado a world_profile + ID de heroi.
 * Somente a ficha canonica possui os ingredientes: nao duplica o inventario.
 * Mutacoes de receita + inventario + diario sao atomicas, com recibos anti-replay.
 * O catalogo e regra OPCIONAL ORIGINAL, nao atribui poderes oficiais a ingredientes.
 */
public final class WilemCookbookEngine {
    private final AppDb db;
    public WilemCookbookEngine(AppDb database){if(database==null)throw new IllegalArgumentException("AppDb ausente");db=database;}

    public long ownerId(){
        long fallback=0,active=0;
        for(JSONObject c:db.listCharacters()){
            String n=c.optString("name","").trim().toLowerCase(Locale.ROOT);
            if(n.equals("wilem")||n.equals("willem")){
                long candidate=c.optLong("id",0);
                if(candidate<=0)continue;
                if(db.findActivePartyHero(candidate).length()>0){
                    if(active!=0)return 0;active=candidate;
                }
                if(fallback==0)fallback=candidate;else fallback=-1;
            }
        }
        return active>0?active:Math.max(0,fallback);
    }
    public boolean ownerExists(){return ownerId()>0;}
    public boolean canCook(){
        long id=ownerId();if(id<=0)return false;
        JSONObject c=db.getCharacter(id);
        return c.length()>0&&c.optInt("active",0)==1&&db.findActivePartyHero(id).length()>0;
    }
    private String profile(){return db.getActiveMapProfile();}
    public static void ensureSchema(SQLiteDatabase sql){
        sql.execSQL("CREATE TABLE IF NOT EXISTS wilem_recipe_books ("+
            "world_profile TEXT NOT NULL, character_id INTEGER NOT NULL,"+
            "state_json TEXT NOT NULL DEFAULT '{}', updated_at INTEGER NOT NULL DEFAULT 0,"+
            "PRIMARY KEY(world_profile,character_id))");
        WilemWorldSupply.ensureSchema(sql);
    }
    private static JSONObject object(String json){
        try{return new JSONObject(json==null?"{}":json);}catch(Exception e){return new JSONObject();}
    }
    private static JSONArray array(String json){
        try{return new JSONArray(json==null?"[]":json);}catch(Exception e){return new JSONArray();}
    }
    private static boolean contains(JSONArray arr,String value){
        if(arr==null)return false;for(int i=0;i<arr.length();i++)if(value.equals(arr.optString(i)))return true;return false;
    }
    private static String sceneLocation(AppDb db){return db.getSceneState().optString("location","");}
    private static double worldHour(AppDb db){
        JSONObject s=db.getSceneState();return Math.max(0,s.optInt("day_number",1)-1)*24d+s.optDouble("clock_hour",12d);
    }
    private JSONObject fresh(long id)throws Exception{
        JSONArray rumors=new JSONArray();
        rumors.put(new JSONObject().put("recipe_id","mon_coruja_urso_0").put("location","Biblioteca de Strixhaven").put("source","anotacao de aula").put("text","Talvez uma carne monstruosa possa sustentar um grupo apos uma travessia. Precisa de pesquisa.").put("status","hipotese"));
        rumors.put(new JSONObject().put("recipe_id","mon_aranha_1").put("location","Laboratorio de historia natural").put("source","estudantes de zoologia").put("text","Partes de aranha exigem remocao cuidadosa das glandulas. Nao experimentar sem identificar.").put("status","hipotese"));
        rumors.put(new JSONObject().put("recipe_id","mon_basilisco_4").put("location","Marco do Percurso").put("source","viajante ainda desconhecido").put("text","Dizem existir uma receita de basilisco, mas nao ha provas.").put("status","a_ouvir"));
        rumors.put(new JSONObject().put("recipe_id","isca_mel_fantasia").put("location","Feira de mercadores feericos").put("source","rumor nao confirmado").put("text","Um confeiteiro conhece doces que deixam certas criaturas sonolentas.").put("status","a_ouvir"));
        JSONArray notes=new JSONArray().put(new JSONObject().put("title","Primeira pagina")
            .put("text","Batatas e cebolas sao confiaveis. Nao confundir fungos luminosos com fungos comestiveis. Perguntar a Auri sobre esporos."))
            .put(new JSONObject().put("title","Caldeirao e utensilios")
            .put("text","Meu caldeirao e meus utensilios ja pertencem ao equipamento. Sem reproduzi-los na despensa."));
        return new JSONObject().put("schema",1).put("owner_id",id).put("known",WilemCookbookCatalog.starterIds())
            .put("rumors",rumors).put("notes",notes).put("experiments",new JSONArray())
            .put("cooked",new JSONArray()).put("meals",new JSONArray()).put("baits",new JSONArray()).put("harvests",new JSONArray())
            .put("receipts",new JSONArray()).put("discoveries",new JSONArray())
            .put("purchases",new JSONArray()).put("forage_attempts",new JSONArray())
            .put("world_reward_claims",new JSONArray())
            .put("created_world_hour",worldHour(db));
    }
    public synchronized JSONObject state(){
        long id=ownerId();if(id<=0)return new JSONObject();
        SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);
        Cursor cursor=sql.rawQuery("SELECT state_json FROM wilem_recipe_books WHERE world_profile=? AND character_id=?",
            new String[]{profile(),String.valueOf(id)});
        try{
            if(cursor.moveToFirst()){JSONObject current=object(cursor.getString(0));reconcileBaitExpirations(current);return current;}
        }finally{cursor.close();}
        try{
            JSONObject initial=fresh(id);write(sql,id,initial);return initial;
        }catch(Exception e){throw new IllegalStateException("Nao foi possivel inicializar o caderno",e);}
    }

    /** Nao apaga condicoes alheias: somente a que tem recibo do nosso motor. */
    private void reconcileBaitExpirations(JSONObject state){
        JSONArray baits=state.optJSONArray("baits");if(baits==null)return;
        double hour=worldHour(db);int combatRound=db.getCombatState().optInt("round_no",1);boolean changed=false;
        for(int i=0;i<baits.length();i++){JSONObject b=baits.optJSONObject(i);if(b==null||!"resolvida".equals(b.optString("status"))||!b.optBoolean("effect_applied"))continue;
            boolean due=hour>=b.optDouble("ends_world_hour",1e12)||combatRound>=b.optInt("ends_combat_round",Integer.MAX_VALUE);
            if(!due)continue;
            long targetId=b.optLong("target_id",0);JSONObject target=db.getCombatant(targetId);
            JSONObject resources=object(target.optString("resources_json","{}"));
            if(b.optString("id").equals(resources.optString("wilem_bait_source"))){
                String cond=b.optString("applied_condition");String[] parts=target.optString("conditions").split(",");
                StringBuilder next=new StringBuilder();boolean removed=false;
                for(String part:parts){String item=part.trim();if(!removed&&item.equalsIgnoreCase(cond)){removed=true;continue;}
                    if(!item.isEmpty()){if(next.length()>0)next.append(",");next.append(item);}
                }
                try{if(removed)db.updateConditionsChecked(targetId,next.toString());resources.remove("wilem_bait_source");
                    ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
                    db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
                        new String[]{String.valueOf(targetId),profile()});
                }catch(Exception ignored){/* Mantem registro para reconciliacao posterior */continue;}
            }
            try{b.put("status","expirada");changed=true;}catch(Exception ignored){}
        }
        if(changed)try{write(db.getWritableDatabase(),ownerId(),state);}catch(Exception ignored){}
    }

    private void write(SQLiteDatabase sql,long id,JSONObject state)throws Exception{
        ContentValues c=new ContentValues();c.put("world_profile",profile());c.put("character_id",id);
        c.put("state_json",state.toString());c.put("updated_at",System.currentTimeMillis());
        if(sql.insertWithOnConflict("wilem_recipe_books",null,c,SQLiteDatabase.CONFLICT_REPLACE)<0)
            throw new IllegalStateException("Caderno nao persistiu");
    }
    private static void remember(JSONObject state,String receipt)throws Exception{
        JSONArray a=state.optJSONArray("receipts");if(a==null)a=new JSONArray();
        a.put(receipt);JSONArray trimmed=new JSONArray();for(int i=Math.max(0,a.length()-120);i<a.length();i++)trimmed.put(a.opt(i));
        state.put("receipts",trimmed);
    }
    private static void requireReceipt(JSONObject state,String receipt){
        if(receipt==null||receipt.trim().length()<12)throw new IllegalArgumentException("Identidade da operacao ausente");
        if(contains(state.optJSONArray("receipts"),receipt))throw new IllegalStateException("Operacao ja confirmada; repeticao bloqueada");
    }
    private JSONObject hero(){return db.getCharacter(ownerId());}
    private JSONObject sheet(){return object(hero().optString("sheet_json","{}"));}
    private static JSONArray ledger(JSONObject sheet){
        JSONArray a=sheet.optJSONArray("inventory_ledger");return a==null?new JSONArray():a;
    }
    public JSONObject pantry(){
        JSONArray items=ledger(sheet()),all=WilemCookbookCatalog.ingredients(),out=new JSONArray();
        for(int i=0;i<items.length();i++){
            JSONObject x=items.optJSONObject(i);if(x==null||x.optInt("quantity",0)<=0)continue;
            String id=x.optString("id",""),name=x.optString("name","");
            for(int j=0;j<all.length();j++){
                JSONObject ing=all.optJSONObject(j);if(ing==null)continue;
                if(id.equals("wilem_ing_"+ing.optString("id"))||name.equalsIgnoreCase(ing.optString("name")))
                    out.put(x); // Nao replica: apenas apresenta a referencia canonica.
            }
        }
        try{return new JSONObject().put("items",out).put("owner_id",ownerId());}catch(Exception e){return new JSONObject();}
    }
    /** Ofertas do mundo, nao numeros inventados na interface. */
    public JSONArray worldRewardOffers(){
        JSONArray offers=new JSONArray();
        if(!canCook())return offers;
        JSONObject st=state();JSONArray claimed=st.optJSONArray("world_reward_claims");
        for(JSONObject ev:db.listWorldEvents(1000)){
            if(!"resolved".equals(ev.optString("status"))
                ||!"applied".equals(ev.optString("consequence_status")))continue;
            JSONObject grant=object(ev.optString("consequence_json","{}")).optJSONObject("wilem_ingredient");
            if(grant==null)continue;
            String id=grant.optString("ingredient_id");
            int qty=grant.optInt("quantity",0);
            long recipient=grant.optLong("recipient_character_id",ownerId());
            long eventId=ev.optLong("id");
            if(qty<1||qty>30||recipient!=ownerId()
                ||WilemCookbookCatalog.ingredientById(id).length()==0
                ||contains(claimed,"event:"+eventId))continue;
            try{offers.put(new JSONObject().put("event_id",eventId)
                .put("event_title",ev.optString("title")).put("ingredient_id",id)
                .put("quantity",qty));}
            catch(Exception e){throw new IllegalStateException("Oferta de evento invalida",e);}
        }
        return offers;
    }

    /** Consome recompensa estruturada JA resolvida pelo motor de eventos. Nunca duplica. */
    public synchronized JSONObject claimWorldReward(final long eventId,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject ev=db.getWorldEvent(eventId);
            if(ev.length()==0||!"resolved".equals(ev.optString("status"))
                ||!"applied".equals(ev.optString("consequence_status")))
                throw new IllegalStateException("Evento do mundo nao resolvido e aplicado");
            JSONObject grant=object(ev.optString("consequence_json","{}")).optJSONObject("wilem_ingredient");
            if(grant==null)throw new IllegalStateException("Este evento nao tem recompensa culinaria");
            String id=grant.optString("ingredient_id");int qty=grant.optInt("quantity",0);
            if(qty<1||qty>30||grant.optLong("recipient_character_id",ownerId())!=ownerId())
                throw new IllegalStateException("Recompensa nao pertence ao cozinheiro");
            JSONObject ingredient=WilemCookbookCatalog.ingredientById(id);
            if(ingredient.length()==0)throw new IllegalStateException("Ingrediente do evento desconhecido");
            JSONArray claims=st.optJSONArray("world_reward_claims");
            if(claims==null){claims=new JSONArray();st.put("world_reward_claims",claims);}
            String claim="event:"+eventId;
            if(contains(claims,claim))throw new IllegalStateException("Recompensa do evento ja resgatada");
            JSONObject ch=hero(),sheet=sheet();JSONArray items=new JSONArray(ledger(sheet).toString());
            String key="wilem_ing_"+id;int found=-1;
            for(int i=0;i<items.length();i++){
                JSONObject x=items.optJSONObject(i);
                if(x!=null&&(key.equals(x.optString("id"))
                    ||(ingredient.optString("name").equalsIgnoreCase(x.optString("name"))
                        &&x.optString("harvest_receipt").isEmpty()))){
                    if(found>=0)throw new IllegalStateException("Ingredientes duplicados no inventario");
                    found=i;
                }
            }
            JSONObject item=found>=0?items.getJSONObject(found):
                new JSONObject().put("id",key).put("name",ingredient.optString("name"))
                    .put("cookbook_ingredient_id",id).put("quantity",0);
            item.put("quantity",item.optInt("quantity",0)+qty).put("cookbook_ingredient_id",id);
            if(found>=0)items.put(found,item);else items.put(item);
            sheet.put("inventory_ledger",items)
                .put("inventory_load",InventoryLedgerEngine.totals(items,ch.optInt("str",10),false));
            db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            claims.put(claim);remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Recompensa culinaria: "+ev.optString("title"),
                ingredient.optString("name")+" x"+qty+"; evento aplicado #"+eventId,
                "culinaria",receipt,"wilem_cookbook");
            result[0]=new JSONObject().put("event_id",eventId).put("ingredient_id",id)
                .put("quantity",qty).put("claimed",true);
        }});
        return result[0];
    }

    /** Only real atlas terrain or an explicitly recorded scene resource can supply food. */
    public JSONArray forageableIngredients(){
        JSONArray out=new JSONArray();if(!canCook())return out;
        JSONObject st=state();JSONArray tried=st.optJSONArray("forage_attempts");
        int day=db.getSceneState().optInt("day_number",1);
        JSONArray ingredients=WilemCookbookCatalog.ingredients();
        for(int i=0;i<ingredients.length();i++){
            JSONObject ing=ingredients.optJSONObject(i);
            JSONObject offer=WilemWorldSupply.forageOffer(db,ing);
            if(offer.length()==0)continue;
            String attempt=day+":"+offer.optString("source_key")+":"+
                offer.optString("stock_revision")+":"+ing.optString("id");
            if(!contains(tried,attempt))out.put(offer);
        }
        return out;
    }
    /** Physical dice, canonical environmental source, finite supply, one attempt/day. */
    public synchronized JSONObject forageWithPhysicalRoll(final String ingredientId,
        final Dice3DOverlay.RollResult dice,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia");
        if(dice==null||!"d20".equals(dice.formula)||!"Normal".equals(dice.mode))
            throw new IllegalArgumentException("Rolagem fisica d20 normal obrigatoria");
        DiceRollSpec spec=DiceRollSpec.parse(dice.formula,dice.mode);
        java.util.List<Integer> faces=spec.requirePhysicalAgreement(dice.rolls,dice.physicalFaces);
        DiceRollSpec.Result check=spec.resolve(faces);
        if(check.natural!=dice.natural||check.total!=dice.total||check.natural<1||
            check.natural>20||!RollIntegrity.signature(spec.formula,dice.physicalFaces,
            spec.modifier,dice.physicsSeed,dice.total).equals(dice.integritySignature))
            throw new IllegalStateException("Dado fisico de coleta invalido");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
            JSONObject offer=WilemWorldSupply.forageOffer(db,ing);
            if(offer.length()==0)throw new IllegalStateException("Nao existe recurso ecologico validado no local");
            int day=db.getSceneState().optInt("day_number",1);
            String attempt=day+":"+offer.optString("source_key")+":"+
                offer.optString("stock_revision")+":"+ingredientId;
            JSONArray attempts=st.optJSONArray("forage_attempts");
            if(attempts==null){attempts=new JSONArray();st.put("forage_attempts",attempts);}
            if(contains(attempts,attempt))throw new IllegalStateException("Este recurso ja foi investigado aqui hoje");
            int total=check.natural+Math.floorDiv(hero().optInt("wis",10)-10,2);
            int dc=offer.optInt("dc",12);
            int quantity=total>=dc?Math.min(offer.optInt("quantity",0),1+Math.min(2,(total-dc)/5)):0;
            if(quantity>0){
                WilemWorldSupply.consumeForage(db,offer,quantity);
                JSONObject ch=hero(),sheet=sheet();JSONArray items=new JSONArray(ledger(sheet).toString());
                String key="wilem_ing_"+ingredientId;int found=-1;
                for(int i=0;i<items.length();i++){
                    JSONObject x=items.optJSONObject(i);
                    if(x!=null&&key.equals(x.optString("id"))){found=i;break;}
                }
                JSONObject item=found>=0?items.getJSONObject(found):
                    new JSONObject().put("id",key).put("name",ing.optString("name"))
                        .put("cookbook_ingredient_id",ingredientId).put("quantity",0);
                item.put("quantity",item.optInt("quantity",0)+quantity);
                if(found>=0)items.put(found,item);else items.put(item);
                sheet.put("inventory_ledger",items)
                    .put("inventory_load",InventoryLedgerEngine.totals(items,ch.optInt("str",10),false));
                db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            }
            attempts.put(attempt);remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Coleta de Wilem em "+sceneLocation(db),
                ing.optString("name")+" x"+quantity+"; "+offer.optString("source_key")+
                "; d20 "+check.natural+"; total "+total+" vs CD "+dc,
                "culinaria",receipt,"wilem_cookbook");
            result[0]=new JSONObject().put("quantity",quantity).put("success",quantity>0)
                .put("total",total).put("dc",dc).put("source_key",offer.optString("source_key"))
                .put("roll_signature",dice.integritySignature);
        }});
        return result[0];
    }

    public JSONArray marketOffers(){return canCook()?WilemWorldSupply.marketOffers(db):new JSONArray();}

    /** Legacy entry point: only one unambiguous stocked supplier may be chosen automatically. */
    public synchronized JSONObject acquire(final String ingredientId,final int amount,final String source,
        final boolean confirmed,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia");
        if(!confirmed||!"mercado_pago".equals(source))
            throw new IllegalStateException("Aquisicao manual nao aceita; exige oferta e pagamento registrado");
        JSONArray offers=marketOffers();JSONObject matched=new JSONObject();
        for(int i=0;i<offers.length();i++){
            JSONObject o=offers.optJSONObject(i);
            if(o!=null&&ingredientId.equals(o.optString("ingredient_id"))){
                if(matched.length()>0)throw new IllegalStateException("Escolha qual vendedor possui o ingrediente");
                matched=o;
            }
        }
        if(matched.length()==0)throw new IllegalStateException("Nenhum vendedor presente possui estoque registrado");
        return buyFromMarket(ingredientId,amount,matched.optString("source_key"),receipt);
    }

    /** Transactional money + finite vendor stock + persistent hero inventory. */
    public synchronized JSONObject buyFromMarket(final String ingredientId,final int amount,
        final String sourceKey,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia");
        if(amount<1||amount>10000)throw new IllegalArgumentException("Quantidade invalida");
        final JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        if(ing.length()==0||"monstro".equals(ing.optString("category"))||
            "magico".equals(ing.optString("category")))
            throw new IllegalStateException("Ingrediente sem venda comum autorizada");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject offer=WilemWorldSupply.offer(db,sourceKey,ingredientId);
            if(offer.length()==0||amount>offer.optInt("quantity"))
                throw new IllegalStateException("Vendedor ausente ou sem estoque para essa compra");
            JSONObject ch=hero(),sheet=sheet();JSONArray items=new JSONArray(ledger(sheet).toString());
            if(!sheet.has("inventory_currency_gp"))throw new IllegalStateException("Carteira 2024 ausente");
            double gp=sheet.optDouble("inventory_currency_gp",-1d);
            if(!Double.isFinite(gp)||gp<0d)throw new IllegalStateException("Saldo monetario invalido");
            long balance=Math.round(gp*100d);
            if(Math.abs(gp*100d-balance)>.001d)throw new IllegalStateException("Moedas sem precisao de cobre");
            long cost=Math.multiplyExact((long)amount,(long)offer.optInt("price_cp",-1));
            if(cost<0||balance<cost)throw new IllegalStateException("Moedas insuficientes");
            WilemWorldSupply.sell(db.getWritableDatabase(),profile(),offer,amount);
            sheet.put("inventory_currency_gp",(balance-cost)/100d);
            String key="wilem_ing_"+ingredientId;int found=-1;
            for(int i=0;i<items.length();i++){
                JSONObject item=items.optJSONObject(i);
                if(item!=null&&(key.equals(item.optString("id"))||
                    (ing.optString("name").equalsIgnoreCase(item.optString("name"))&&
                        item.optString("harvest_receipt","").isEmpty()))){
                    if(found>=0)throw new IllegalStateException("Ingrediente duplicado na ficha");
                    found=i;
                }
            }
            JSONObject item=found>=0?items.getJSONObject(found):
                new JSONObject().put("id",key).put("name",ing.optString("name")).put("quantity",0);
            item.put("quantity",item.optInt("quantity",0)+amount)
                .put("cookbook_ingredient_id",ingredientId);
            if(found>=0)items.put(found,item);else items.put(item);
            sheet.put("inventory_ledger",items)
                .put("inventory_load",InventoryLedgerEngine.totals(items,ch.optInt("str",10),false));
            db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            JSONArray purchases=st.optJSONArray("purchases");
            if(purchases==null){purchases=new JSONArray();st.put("purchases",purchases);}
            purchases.put(new JSONObject().put("day",db.getSceneState().optInt("day_number",1))
                .put("location",sceneLocation(db)).put("source_key",sourceKey)
                .put("seller",offer.optString("seller"))
                .put("stock_revision",offer.optString("stock_revision"))
                .put("ingredient_id",ingredientId).put("quantity",amount)
                .put("paid_copper",cost).put("receipt",receipt));
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Compra de Wilem com "+offer.optString("seller"),
                ing.optString("name")+" x"+amount+"; "+cost+" PC, estoque consumido: "+
                sourceKey,"culinaria",receipt,"wilem_cookbook");
            result[0]=new JSONObject().put("acquired",true).put("quantity",amount)
                .put("item",item).put("money_auto_debited",true).put("paid_copper",cost)
                .put("source_key",sourceKey);
        }});
        return result[0];
    }

    /** Corpos sao consultados no combate persistido, jamais por nome digitado. */
    private static String normalizeCreature(String s){
        String val=java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        // Tokens do combate podem ter #2 ou (3) apos o nome da mesma especie.
        val=val.replaceAll("\\s*(?:#?\\d+|\\(\\d+\\))\\s*$","");
        return val.replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static String speciesForIngredient(JSONObject ingredient){
        String name=ingredient.optString("name");
        String prefix="Parte culinaria de ";
        return name.startsWith(prefix)?name.substring(prefix.length()):"";
    }
    public JSONArray harvestableCorpses(){
        JSONArray out=new JSONArray();
        if(!canCook())return out;
        JSONArray ingredients=WilemCookbookCatalog.ingredients();
        for(JSONObject body:db.listCombatants()){
            if(body.optInt("is_player",0)==1||body.optInt("hp",1)>0||
                body.optInt("max_hp",0)<=0||body.optLong("id",0)<=0)continue;
            JSONObject life=object(body.optString("resources_json","{}"));
            if(life.optBoolean("wilem_harvested",false)||
                life.optBoolean("stable",false)||life.optBoolean("nonlethal_knockout",false))continue;
            for(int i=0;i<ingredients.length();i++){
                JSONObject ing=ingredients.optJSONObject(i);
                if(ing==null||!"monstro".equals(ing.optString("category")))continue;
                if(!normalizeCreature(body.optString("name")).equals(normalizeCreature(speciesForIngredient(ing))))continue;
                try{out.put(new JSONObject().put("combatant_id",body.optLong("id"))
                    .put("creature",body.optString("name"))
                    .put("ingredient_id",ing.optString("id"))
                    .put("anatomy",ing.optJSONObject("anatomy"))
                    .put("dc",8+Math.min(8,ing.optInt("tier",1))));}
                catch(Exception e){throw new IllegalStateException("Candidato de colheita invalido",e);}
            }
        }
        return out;
    }

    /**
     * Extracao conservadora de uma unidade por corpo derrotado.
     * A falha consome a unica tentativa; o mesmo corpo nao pode gerar material infinitamente.
     * Apenas especies textualmente confirmadas no catalogo sao elegiveis.
     */
    public synchronized JSONObject harvestWithPhysicalRoll(final long combatantId,
              final String ingredientId,final Dice3DOverlay.RollResult dice,
              final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia");
        if(dice==null||!"d20".equals(dice.formula)||!"Normal".equals(dice.mode))
            throw new IllegalArgumentException("Rolagem fisica d20 normal obrigatoria");
        DiceRollSpec spec=DiceRollSpec.parse(dice.formula,dice.mode);
        java.util.List<Integer> canonical=spec.requirePhysicalAgreement(dice.rolls,dice.physicalFaces);
        DiceRollSpec.Result calc=spec.resolve(canonical);
        if(calc.natural<1||calc.natural>20||calc.natural!=dice.natural||calc.total!=dice.total||
           !RollIntegrity.signature(spec.formula,dice.physicalFaces,spec.modifier,dice.physicsSeed,dice.total)
               .equals(dice.integritySignature))
            throw new IllegalStateException("Dado nao corresponde a face fisica assinada");
        final JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        if(!"monstro".equals(ing.optString("category")))
            throw new IllegalArgumentException("Ingrediente nao e uma parte culinaria de criatura");
        final JSONObject[] outcome={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject body=db.getCombatant(combatantId);
            if(body.length()==0||body.optInt("is_player",0)==1||body.optInt("hp",1)>0||
                body.optInt("max_hp",0)<=0)
                throw new IllegalStateException("Corpo abatido elegivel nao encontrado no combate");
            if(!normalizeCreature(body.optString("name"))
                .equals(normalizeCreature(speciesForIngredient(ing))))
                throw new IllegalStateException("A anatomia desse corpo nao corresponde ao ingrediente");
            JSONObject provenance=object(body.optString("resources_json","{}"));
            if(provenance.optBoolean("stable",false)||provenance.optBoolean("nonlethal_knockout",false))
                throw new IllegalStateException("Criatura inconsciente ou estabilizada nao pode ser colhida");
            if(provenance.optBoolean("wilem_harvested",false))
                throw new IllegalStateException("Essa criatura ja foi examinada; colheita duplicada");
            int dc=8+Math.min(8,ing.optInt("tier",1));
            int wisdomModifier=Math.floorDiv(hero().optInt("wis",10)-10,2);
            int total=calc.natural+wisdomModifier;
            boolean success=total>=dc;
            provenance.put("wilem_harvested",true).put("wilem_harvest_receipt",receipt);
            ContentValues marker=new ContentValues();marker.put("resources_json",provenance.toString());
            if(db.getWritableDatabase().update("combatants",marker,"id=? AND world_profile=?",
               new String[]{String.valueOf(combatantId),profile()})!=1)
                throw new IllegalStateException("Nao foi possivel marcar a carcaça utilizada");
            if(success){
                JSONObject ch=hero(),sheet=sheet();
                JSONArray inv=new JSONArray(ledger(sheet).toString());
                JSONObject part=new JSONObject().put("id","wilem_ing_"+ingredientId+"_body_"+combatantId)
                    .put("cookbook_ingredient_id",ingredientId).put("name",ing.optString("name"))
                    .put("quantity",1).put("harvested_from_combatant_id",combatantId)
                    .put("harvest_receipt",receipt)
                    .put("anatomy_cut",ing.optJSONObject("anatomy")==null?"":ing.optJSONObject("anatomy").optString("cut"))
                    .put("harvested_world_hour",worldHour(db))
                    .put("expires_world_hour",worldHour(db)+24d);
                inv.put(part);
                sheet.put("inventory_ledger",inv)
                    .put("inventory_load",InventoryLedgerEngine.totals(inv,ch.optInt("str",10),false));
                db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            }
            JSONArray records=st.optJSONArray("harvests");
            if(records==null){records=new JSONArray();st.put("harvests",records);}
            JSONObject entry=new JSONObject().put("id",receipt).put("combatant_id",combatantId)
                .put("creature",body.optString("name")).put("ingredient_id",ingredientId)
                .put("dice_total",calc.total).put("wisdom_modifier",wisdomModifier)
                .put("total",total).put("dc",dc).put("success",success)
                .put("world_hour",worldHour(db)).put("location",sceneLocation(db))
                .put("roll_signature",dice.integritySignature)
                .put("anatomy",ing.optJSONObject("anatomy"));
            records.put(entry);remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Colheita de Wilem: "+body.optString("name"),
                "Resultado "+total+" vs CD "+dc+"; "+(success?"1 parte aproveitavel":"nenhum alimento seguro"),
                "culinaria",receipt,"wilem_cookbook");
            outcome[0]=entry;
        }});
        return outcome[0];
    }
    public synchronized JSONObject recordRumor(final String recipeId,final String informant,final String note,
                                                final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia para ouvir e anotar");
        if(WilemCookbookCatalog.recipeById(recipeId).length()==0)throw new IllegalArgumentException("Receita desconhecida pelo catalogo");
        if(informant==null||informant.trim().length()<2||note==null||note.trim().length()<8)throw new IllegalArgumentException("Fonte e conteudo do boato obrigatorios");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject rumor=new JSONObject().put("recipe_id",recipeId).put("location",sceneLocation(db))
                .put("source",informant.trim()).put("text",note.trim()).put("status","ouvido")
                .put("heard_world_hour",worldHour(db));st.getJSONArray("rumors").put(rumor);
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            
            // Usa a rede canonica de rumores: local, distorcao, cronologia e conhecimento por heroi.
            AppDb.ensureSixZeroSchema(db.getWritableDatabase());
            ContentValues circulating=new ContentValues();
            circulating.put("world_profile",profile());circulating.put("rumor_key","culinary:"+receipt);
            circulating.put("source_event_key",receipt);
            circulating.put("origin_location",sceneLocation(db));circulating.put("current_region",sceneLocation(db));
            circulating.put("statement",note.trim());circulating.put("truth_level","nao_confirmada");
            circulating.put("distortion",10);circulating.put("confidence",55);circulating.put("secrecy",25);
            circulating.put("status","circulating");int day=Math.max(1,db.getSceneState().optInt("day_number",1));
            circulating.put("born_day",day);circulating.put("expires_day",day+90);
            circulating.put("carriers_json",new JSONArray().put(sceneLocation(db)).toString());
            circulating.put("mutations_json",new JSONArray().put(note.trim()).toString());
            circulating.put("updated_at",System.currentTimeMillis());
            long rumorId=db.getWritableDatabase().insertOrThrow("rumors",null,circulating);
            new RumorNetworkEngine(db).teachHero(rumorId,ownerId(),informant.trim(),day);
            db.addJournalFact("Boato culinario: "+informant,rumor.toString(),"boato",receipt,"wilem_cookbook");result[0]=rumor;
        }});
        return result[0];
    }
    /** Conhecimento nao e desbloqueado so por abrir o catalogo: exige fonte + pesquisa confirmadas. */
    public synchronized JSONObject confirmDiscovery(final String recipeId,final String evidence,
                                                     final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem nao esta na companhia");
        if(evidence==null||evidence.trim().length()<8)throw new IllegalArgumentException("Pesquisa ou experiencia precisa de evidencia");
        final JSONObject r=WilemCookbookCatalog.recipeById(recipeId);
        if(r.length()==0)throw new IllegalArgumentException("Receita desconhecida");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject state=state();requireReceipt(state,receipt);
            int heroLevel=hero().optInt("level",1);if(heroLevel<r.optInt("tier"))throw new IllegalStateException("Conhecimento avancado demais para o nivel");
            
            boolean heard=false;JSONArray rumors=state.optJSONArray("rumors");
            for(int i=0;rumors!=null&&i<rumors.length();i++){
                JSONObject hint=rumors.optJSONObject(i);
                if(hint!=null&&recipeId.equals(hint.optString("recipe_id"))&&"ouvido".equals(hint.optString("status")))heard=true;
            }
            if(!heard && !contains(state.optJSONArray("known"),recipeId))
                throw new IllegalStateException("Receita precisa ser ouvida em jogo ou aprendida por pesquisa confirmada pelo Mestre");
            JSONArray known=state.getJSONArray("known");if(!contains(known,recipeId))known.put(recipeId);
            state.getJSONArray("discoveries").put(new JSONObject().put("recipe_id",recipeId)
                .put("evidence",evidence.trim()).put("location",sceneLocation(db)).put("world_hour",worldHour(db)));
            remember(state,receipt);write(db.getWritableDatabase(),ownerId(),state);
            db.addJournalFact("Receita descoberta: "+r.optString("name"),evidence,"culinaria",receipt,"wilem_cookbook");
            out[0]=new JSONObject().put("known",true).put("recipe",r);
        }});
        return out[0];
    }
    private static int stock(JSONArray ledger,String ingredientId,double atHour){
        String id="wilem_ing_"+ingredientId;int qty=0;
        JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        for(int i=0;i<ledger.length();i++){
            JSONObject item=ledger.optJSONObject(i);if(item!=null&&(id.equals(item.optString("id"))||
            ingredientId.equals(item.optString("cookbook_ingredient_id"))||
            ing.optString("name").equalsIgnoreCase(item.optString("name")))&&
            (!item.has("expires_world_hour")||atHour<item.optDouble("expires_world_hour",0d)))
            qty+=Math.max(0,item.optInt("quantity",0));
        }return qty;
    }
    private static JSONArray subtract(JSONArray ledger,JSONObject ingredients,int multiplier,double atHour)throws Exception{
        JSONArray out=new JSONArray(ledger.toString());JSONArray names=ingredients.names();
        if(names==null)throw new IllegalArgumentException("Receita sem ingredientes");
        for(int j=0;j<names.length();j++){
            String ingredient=names.getString(j);int needed=ingredients.getInt(ingredient)*multiplier;
            if(needed<1||stock(out,ingredient,atHour)<needed)throw new IllegalStateException("Falta "+ingredient+" x"+needed+" (ou ingrediente estragado)");
            JSONObject ref=WilemCookbookCatalog.ingredientById(ingredient);
            for(int i=0;i<out.length()&&needed>0;i++){JSONObject x=out.optJSONObject(i);if(x==null)continue;
                if((("wilem_ing_"+ingredient).equals(x.optString("id"))||
                    ingredient.equals(x.optString("cookbook_ingredient_id"))||
                    ref.optString("name").equalsIgnoreCase(x.optString("name")))&&
                    (!x.has("expires_world_hour")||atHour<x.optDouble("expires_world_hour",0d))){
                    int spent=Math.min(needed,Math.max(0,x.optInt("quantity",0)));x.put("quantity",x.optInt("quantity",0)-spent);needed-=spent;
                }
            }
            if(needed>0)throw new IllegalStateException("Estoque inconsistente");
        }return out;
    }
    /** Caminho legado exclusivamente para comidas comuns, que nao exigem teste. */
    public synchronized JSONObject cook(final String recipeId,final int roll,final boolean fire,
                                         final boolean utensils,final String receipt)throws Exception{
        JSONObject recipe=WilemCookbookCatalog.recipeById(recipeId);
        if(recipe.length()==0)throw new IllegalArgumentException("Receita desconhecida");
        if(!"cotidiana".equals(recipe.optString("type")))
            throw new IllegalStateException("Preparo extraordinario exige d20 fisico auditavel, nao valor digitado");
        return cookInternal(recipeId,0,fire,utensils,receipt,null);
    }
    /** A face fisica e a unica fonte do d20 em pratos monstruosos e iscas. */
    public synchronized JSONObject cookWithPhysicalRoll(final String recipeId,
             final Dice3DOverlay.RollResult dice,final boolean fire,
             final boolean utensils,final String receipt)throws Exception{
        if(dice==null||!"d20".equals(dice.formula)||!"Normal".equals(dice.mode))
            throw new IllegalArgumentException("Rolagem fisica d20 normal obrigatoria");
        DiceRollSpec spec=DiceRollSpec.parse(dice.formula,dice.mode);
        java.util.List<Integer> canonical=spec.requirePhysicalAgreement(dice.rolls,dice.physicalFaces);
        DiceRollSpec.Result reconstructed=spec.resolve(canonical);
        if(reconstructed.natural<1||reconstructed.natural>20||reconstructed.natural!=dice.natural||
            reconstructed.total!=dice.total||
            !RollIntegrity.signature(spec.formula,dice.physicalFaces,spec.modifier,dice.physicsSeed,dice.total)
                .equals(dice.integritySignature))
            throw new IllegalStateException("Integridade do d20 fisico nao confirmada");
        JSONObject proof=new JSONObject().put("source","physical3d").put("formula",dice.formula)
            .put("natural",dice.natural).put("total",dice.total)
            .put("seed",dice.physicsSeed).put("signature",dice.integritySignature)
            .put("faces",new JSONArray(dice.physicalFaces));
        return cookInternal(recipeId,dice.natural,fire,utensils,receipt,proof);
    }
    private JSONObject cookInternal(final String recipeId,final int roll,final boolean fire,
                                   final boolean utensils,final String receipt,
                                   final JSONObject rollProof)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem deve estar presente na companhia");
        if(!utensils)throw new IllegalStateException("Utensilios nao disponiveis");
        final JSONObject recipe=WilemCookbookCatalog.recipeById(recipeId);
        if(recipe.length()==0)throw new IllegalArgumentException("Receita desconhecida");
        if(!fire)throw new IllegalStateException("Precisa de fonte de calor ou outra tecnica autorizada");
        if(roll<0||roll>20)throw new IllegalArgumentException("D20 fora de 0..20");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            if(!contains(st.getJSONArray("known"),recipeId))throw new IllegalStateException("Receita nao aprendida");
            if(hero().optInt("level",1)<recipe.optInt("tier",0))throw new IllegalStateException("Nivel abaixo do recomendado");
            JSONObject ch=hero(),sheet=sheet();JSONArray after=subtract(ledger(sheet),recipe.getJSONObject("ingredients"),1,worldHour(db));
            sheet.put("inventory_ledger",after).put("inventory_load",InventoryLedgerEngine.totals(after,ch.optInt("str",10),false));
            db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            boolean special=!recipe.optString("type").equals("cotidiana");
            
            if(special&&(rollProof==null||!"physical3d".equals(rollProof.optString("source"))))
                throw new IllegalStateException("D20 fisico verificado obrigatorio para comida especial ou isca");
            String tools=hero().optString("tools","")+" "+sheet.optString("tools","")+" "+sheet.optString("tool_proficiencies","")+" "+sheet.optString("proficiencies","");
            int proficiency=tools.toLowerCase(Locale.ROOT).contains("cozinh")?
                Math.max(2,2+(Math.max(1,hero().optInt("level",1))-1)/4):0;
            int check=roll+proficiency+Math.floorDiv(hero().optInt("wis",10)-10,2);
            String result=!special?"refeicao_comum":check>=recipe.optInt("dc")+5?"somente_beneficio":
                check>=recipe.optInt("dc")?"beneficio_com_risco":"somente_risco";
            JSONObject batch=new JSONObject().put("id",receipt).put("recipe_id",recipeId)
                .put("remaining",recipe.optInt("servings",2)).put("result",result)
                .put("created_world_hour",worldHour(db)).put("expires_world_hour",worldHour(db)+24)
                .put("source","wilem_cookbook").put("kind",recipe.optString("type"));
            st.getJSONArray("cooked").put(batch);
            st.getJSONArray("experiments").put(new JSONObject().put("recipe_id",recipeId)
                .put("roll",roll).put("check",check).put("result",result).put("world_hour",worldHour(db))
                .put("roll_origin",rollProof==null?"sem_rolagem_comida_comum":"physical3d")
                .put("roll_proof",rollProof==null?new JSONObject():rollProof));
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Wilem cozinhou: "+recipe.optString("name"),
                "Porcoes: "+recipe.optInt("servings")+"; resultado: "+result+"; local: "+sceneLocation(db),
                "culinaria",receipt,"wilem_cookbook");
            out[0]=batch.put("completed",true);
        }});
        return out[0];
    }
    public synchronized JSONObject serve(final String batchId,final long targetId,
                                          final String guestName,final boolean shelter,
                                          final String conversation,final String receipt)throws Exception{
        return serveWithSave(batchId,targetId,guestName,shelter,conversation,-1,receipt);
    }
    /** Bloqueia totais arbitrarios de salvaguardas: somente d20 fisico pode produzir resultado mecanico. */
    public synchronized JSONObject serveWithSave(final String batchId,final long targetId,
                                          final String guestName,final boolean shelter,
                                          final String conversation,final int constitutionSaveTotal,
                                          final String receipt)throws Exception{
        if(constitutionSaveTotal>=0)
            throw new IllegalStateException("Salvaguarda digitada nao e aceita; use a mesa de dados fisicos");
        return serveInternal(batchId,targetId,guestName,shelter,conversation,-1,null,receipt);
    }

    public synchronized JSONObject serveWithPhysicalSave(final String batchId,final long targetId,
                                          final String guestName,final boolean shelter,
                                          final String conversation,final Dice3DOverlay.RollResult roll,
                                          final String receipt)throws Exception{
        if(targetId<=0||db.findActivePartyHero(targetId).length()==0)
            throw new IllegalArgumentException("Salvaguarda culinaria automatica requer consumidor na companhia");
        if(roll==null||!"d20".equals(roll.formula)||!"Normal".equals(roll.mode))
            throw new IllegalArgumentException("Salvaguarda fisica d20 normal obrigatoria");
        DiceRollSpec spec=DiceRollSpec.parse(roll.formula,roll.mode);
        java.util.List<Integer> faces=spec.requirePhysicalAgreement(roll.rolls,roll.physicalFaces);
        DiceRollSpec.Result verified=spec.resolve(faces);
        if(verified.natural<1||verified.natural>20||verified.natural!=roll.natural
            ||verified.total!=roll.total
            ||!RollIntegrity.signature(spec.formula,roll.physicalFaces,spec.modifier,roll.physicsSeed,roll.total)
                .equals(roll.integritySignature))
            throw new IllegalStateException("Resultado fisico de salvaguarda invalido");
        int bonus=new DndRulesEngine().savingThrowBonus(db.getCharacter(targetId),"Constitution");
        JSONObject proof=new JSONObject().put("kind","constitution_save")
            .put("source","physical3d").put("actor_character_id",targetId)
            .put("d20",verified.natural).put("bonus",bonus)
            .put("total",verified.natural+bonus).put("signature",roll.integritySignature)
            .put("physics_seed",roll.physicsSeed);
        return serveInternal(batchId,targetId,guestName,shelter,conversation,
            verified.natural+bonus,proof,receipt);
    }

    private JSONObject serveInternal(final String batchId,final long targetId,
                                          final String guestName,final boolean shelter,
                                          final String conversation,final int constitutionSaveTotal,
                                          final JSONObject saveProof,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia para servir");
        if(targetId>0&&db.findActivePartyHero(targetId).length()==0)throw new IllegalArgumentException("Alvo nao esta na companhia");
        if(targetId==0&&(guestName==null||guestName.trim().length()<2))throw new IllegalArgumentException("Identifique o convidado");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);JSONArray batches=st.getJSONArray("cooked");
            JSONObject batch=null;for(int i=0;i<batches.length();i++){JSONObject x=batches.optJSONObject(i);if(x!=null&&batchId.equals(x.optString("id"))){batch=x;break;}}
            if(batch==null||batch.optInt("remaining",0)<=0)throw new IllegalStateException("Sem porcoes prontas");
            if(worldHour(db)>batch.optDouble("expires_world_hour",0))throw new IllegalStateException("Refeicao ja estragou");
            JSONObject recipe=WilemCookbookCatalog.recipeById(batch.optString("recipe_id"));
            String who=targetId>0?db.getCharacter(targetId).optString("name","Companheiro"):guestName.trim();
            boolean magic=!recipe.optString("type").equals("cotidiana")&&
                !batch.optString("result").equals("somente_risco")&&!recipe.optString("type").equals("isca");
            JSONObject effect=new JSONObject().put("receipt",receipt).put("actor",who)
                .put("recipe_id",recipe.optString("id")).put("fed",true).put("food",true)
                .put("warm_meal_with_shelter",shelter&&recipe.optBoolean("warm_meal"))
                .put("start_world_hour",worldHour(db)).put("ends_world_hour",
                    worldHour(db)+(recipe.optInt("effect_minutes",0)/60d))
                .put("effect_key",magic?recipe.optString("effect_key"):"nenhum")
                .put("effect_value",magic?recipe.optInt("effect_value",0):0)

                .put("requires_risk_resolution",batch.optString("result").contains("risco"));
            if(batch.optString("result").contains("risco")){
                if(constitutionSaveTotal<0||saveProof==null||targetId<=0)
                    throw new IllegalArgumentException("Refeicao de risco exige salvaguarda fisica do heroi na companhia");
                boolean poisoned=constitutionSaveTotal<Math.max(10,recipe.optInt("dc",10)-2);
                effect.put("constitution_save_total",constitutionSaveTotal).put("constitution_save_proof",saveProof)
                    .put("poisoned_after_consumption",poisoned)
                    .put("risk_ends_world_hour",worldHour(db)+1d);
            }
            if(targetId>0){JSONObject ch=db.getCharacter(targetId),sheet=object(ch.optString("sheet_json","{}"));
                JSONArray old=sheet.optJSONArray("meal_effects");if(old==null)old=new JSONArray();
                for(int i=0;i<old.length();i++){JSONObject active=old.optJSONObject(i);
                    if(active!=null&&active.optDouble("ends_world_hour",0)>worldHour(db)&&
                        !"nenhum".equals(active.optString("effect_key"))&&magic)
                        throw new IllegalStateException("Ja ha beneficio culinario especial ativo; nao acumular");
                }
                if(magic||effect.optBoolean("poisoned_after_consumption",false))old.put(effect);sheet.put("meal_effects",old);

                sheet.put("last_nourishing_meal_world_hour",worldHour(db));
                sheet.put("nourished_until_world_hour",worldHour(db)+24d);
                if(shelter && recipe.optBoolean("warm_meal",false))sheet.put("warmth_with_shelter_until_world_hour",worldHour(db)+1d);
                db.updateCharacterSheetChecked(targetId,sheet.toString());

                if(effect.optBoolean("poisoned_after_consumption",false)){
                    for(JSONObject combatant:db.listCombatants()){
                        if(!"character".equalsIgnoreCase(combatant.optString("source_kind"))||
                            combatant.optLong("source_id")!=targetId)continue;
                        String conditions=combatant.optString("conditions","");
                        // Nao se apropriar de condicoes ja existentes causadas por outras fontes.
                        if(ConditionTokenGuard.has(conditions,"envenenado"))continue;
                        db.updateConditionsChecked(combatant.optLong("id"),
                            conditions.isEmpty()?"envenenado":conditions+",envenenado");
                        JSONObject provenance=object(combatant.optString("resources_json","{}"));
                        provenance.put("wilem_meal_poison_receipt",receipt);
                        ContentValues source=new ContentValues();source.put("resources_json",provenance.toString());
                        db.getWritableDatabase().update("combatants",source,"world_profile=? AND id=?",
                            new String[]{profile(),String.valueOf(combatant.optLong("id"))});
                    }
                }
                if(magic&&"pv_temporarios".equals(effect.optString("effect_key"))){
                    int amount=Math.max(recipe.optInt("effect_value",1),0);
                    for(JSONObject combatant:db.listCombatants())if("character".equalsIgnoreCase(combatant.optString("source_kind"))
                        &&combatant.optLong("source_id")==targetId){
                        if(combatant.optInt("temp_hp",0)>=amount)continue;
                        ContentValues hp=new ContentValues();hp.put("temp_hp",amount);
                        db.getWritableDatabase().update("combatants",hp,"world_profile=? AND id=?",
                            new String[]{profile(),String.valueOf(combatant.optLong("id"))});
                        JSONObject provenance=object(combatant.optString("resources_json","{}"));
                        provenance.put("wilem_temp_hp_receipt",receipt).put("wilem_temp_hp_initial",amount);
                        ContentValues source=new ContentValues();source.put("resources_json",provenance.toString());
                        db.getWritableDatabase().update("combatants",source,"world_profile=? AND id=?",
                            new String[]{profile(),String.valueOf(combatant.optLong("id"))});
                    }
                }
            }

            batch.put("remaining",batch.optInt("remaining")-1);
            // A companhia so recebe credito de um dia de provisoes quando TODOS os
            // herois presentes comeram no dia. Nao credita convidados nem repete no mesmo dia.
            if(targetId>0){
                List<JSONObject> roster=new PartyDirector(db).activeHeroRoster();boolean everyone=!roster.isEmpty();
                int currentDay=Math.max(1,db.getSceneState().optInt("day_number",1));
                for(JSONObject member:roster){
                    JSONObject data=db.getCharacter(member.optLong("id"));
                    JSONObject memberSheet=object(data.optString("sheet_json","{}"));
                    if(memberSheet.optDouble("last_nourishing_meal_world_hour",-24d)<(currentDay-1)*24d)everyone=false;
                }
                if(everyone){
                    JSONObject party=db.getPartyState(),resources=object(party.optString("company_resources_json","{}"));
                    if(resources.optInt("wilem_provisions_credit_day",0)!=currentDay){
                        // Refeicoes consumidas nao fabricam provisoes no inventario.
                        // Registram saciedade coletiva para o motor de viagem/descanso.
                        resources.put("wilem_provisions_credit_day",currentDay)
                            .put("last_shared_meal_world_hour",worldHour(db))
                            .put("last_shared_meal_location",sceneLocation(db));
                        party.put("company_resources_json",resources.toString());db.savePartyState(party);
                    }
                }
            }
            st.getJSONArray("meals").put(effect.put("conversation_note",conversation==null?"":conversation.trim()));
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Mesa de Wilem: "+who,
                recipe.optString("name")+"; saciedade registrada; "+(shelter?"abrigo e fogo disponiveis; ":"")+
                (conversation==null?"":conversation.trim()),"convivencia",receipt,"wilem_cookbook");
            out[0]=effect;
        }});
        return out[0];
    }
    public synchronized JSONObject setBait(final String batchId,final String location,
                                            final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem nao esta na companhia");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            if(location==null||location.trim().length()<2)throw new IllegalArgumentException("Posicao da isca ausente");
            JSONArray batches=st.getJSONArray("cooked");JSONObject batch=null;
            for(int i=0;i<batches.length();i++){JSONObject b=batches.optJSONObject(i);if(b!=null&&batchId.equals(b.optString("id"))){batch=b;break;}}
            if(batch==null||!"isca".equals(batch.optString("kind"))||batch.optInt("remaining",0)<1)throw new IllegalStateException("Isca indisponivel");
            JSONObject bait=new JSONObject().put("id",receipt).put("recipe_id",batch.optString("recipe_id")).put("batch_quality",batch.optString("result"))
                .put("position",location.trim()).put("scene",sceneLocation(db)).put("world_hour",worldHour(db))
                .put("consumed",false).put("status","armada");batch.put("remaining",batch.optInt("remaining")-1);
            st.getJSONArray("baits").put(bait);remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Isca de Wilem",bait.toString(),"isca",receipt,"wilem_cookbook");result[0]=bait;
        }});
        return result[0];
    }
    /** Nunca incapacita so por colocacao: exige alvo real, ingestao confirmada e salvaguarda. */
    /** Iscas comuns nao exigem salvaguarda; as magicas requerem dado fisico. */
    public synchronized JSONObject resolveBait(final String baitId,final long combatantId,
        final boolean consumed,final boolean compatibleDiet,final boolean immune,
        final int constitutionSaveTotal,final String receipt)throws Exception{
        return resolveBaitInternal(baitId,combatantId,consumed,compatibleDiet,immune,
            constitutionSaveTotal,null,receipt);
    }

    private static int explicitConBonus(JSONObject data){
        if(data==null)return Integer.MIN_VALUE;
        JSONObject saves=data.optJSONObject("saving_throws");
        if(saves==null)saves=data.optJSONObject("saving_throw_bonuses");
        if(saves==null)saves=data.optJSONObject("save_bonuses");
        if(saves!=null)for(String k:new String[]{"con","constitution","Constitution","CON"}){
            if(saves.opt(k) instanceof Number)return saves.optInt(k);
        }
        if(data.opt("save_con_bonus") instanceof Number)return data.optInt("save_con_bonus");
        JSONObject abilities=data.optJSONObject("abilities");
        if(abilities==null)abilities=data.optJSONObject("ability_scores");
        if(abilities!=null)for(String k:new String[]{"con","constitution","Constitution","CON"}){
            if(abilities.opt(k) instanceof Number){
                int score=abilities.optInt(k);
                if(score>=1&&score<=30)return Math.floorDiv(score-10,2);
            }
        }
        for(String k:new String[]{"con","constitution","Constitution","CON"}){
            if(data.opt(k) instanceof Number){
                int score=data.optInt(k);
                if(score>=1&&score<=30)return Math.floorDiv(score-10,2);
            }
        }
        return Integer.MIN_VALUE;
    }
    private JSONObject authoritativeMonsterConSave(JSONObject creature){
        JSONObject resources=object(creature.optString("resources_json","{}"));
        String blockId=resources.optString("block_id","");
        if("statblock".equalsIgnoreCase(creature.optString("source_kind"))&&!blockId.isEmpty()){
            JSONObject compiled=CompiledStatblockEngine.runtimeProfile(blockId);
            if(compiled.optBoolean("found",false)){
                int bonus=explicitConBonus(compiled);
                if(bonus!=Integer.MIN_VALUE)return saveInfo(bonus,"compiled_statblock",blockId);
            }
        }
        if("npc".equalsIgnoreCase(creature.optString("source_kind"))&&creature.optLong("source_id")>0){
            JSONObject npc=db.getNpcMemory(creature.optLong("source_id"));
            if(npc.length()>0&&npc.optInt("active",0)==1){
                int bonus=explicitConBonus(object(npc.optString("stats_json","{}")));
                if(bonus!=Integer.MIN_VALUE)return saveInfo(bonus,"npc_canonical_stats",String.valueOf(npc.optLong("id")));
            }
        }
        // Monster creation/editor can persist a complete encounter statblock in resources_json.
        int canonical=explicitConBonus(resources);
        if(canonical!=Integer.MIN_VALUE)return saveInfo(canonical,"encounter_stats",String.valueOf(creature.optLong("id")));
        JSONObject established=resources.optJSONObject("wilem_constitution_profile");
        if(established!=null&&"master_confirmed".equals(established.optString("authority"))){
            int score=established.optInt("score",0),prof=established.optInt("proficiency_bonus",0);
            if(score>=1&&score<=30&&prof>=0&&prof<=9)
                return saveInfo(Math.floorDiv(score-10,2)+prof,
                    "master_confirmed_profile",String.valueOf(creature.optLong("id")));
        }
        return new JSONObject();
    }
    private static JSONObject saveInfo(int bonus,String authority,String origin){
        JSONObject result=new JSONObject();
        if(bonus< -20||bonus>30)return result;
        try{return result.put("known",true).put("bonus",bonus)
            .put("authority",authority).put("block_id",origin);}
        catch(Exception e){return new JSONObject();}
    }
    public JSONObject monsterSaveInfo(long combatantId){
        JSONObject creature=db.getCombatant(combatantId);
        if(creature.length()==0||creature.optInt("is_player",0)==1)return new JSONObject();
        return authoritativeMonsterConSave(creature);
    }
    /** One-time reusable monster Constitution PROFILE, never an arbitrary per-roll total. */
    public synchronized JSONObject registerMonsterConstitution(final long combatantId,
        final int score,final int proficiencyBonus,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem nao esta na companhia");
        if(score<1||score>30||proficiencyBonus<0||proficiencyBonus>9)
            throw new IllegalArgumentException("Valor de Constituicao ou proficiencia fora do intervalo D&D");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);
            JSONObject creature=db.getCombatant(combatantId);
            if(creature.length()==0||creature.optInt("is_player",0)==1||creature.optInt("hp",0)<=0)
                throw new IllegalStateException("Criatura viva do combate ausente");
            if(monsterSaveInfo(combatantId).optBoolean("known"))
                throw new IllegalStateException("Criatura ja possui valores mecanicos cadastrados");
            JSONObject res=object(creature.optString("resources_json","{}"));
            res.put("wilem_constitution_profile",new JSONObject()
                .put("score",score).put("proficiency_bonus",proficiencyBonus)
                .put("authority","master_confirmed").put("receipt",receipt));
            db.updateCombatantAdvancedChecked(combatantId,creature.optInt("temp_hp",0),
                creature.optString("concentration"),
                creature.optInt("reaction_used",0)==1,
                creature.optInt("death_success",0),creature.optInt("death_fail",0),
                creature.optString("conditions"),creature.optString("condition_timers_json","[]"),
                creature.optString("resistances"),creature.optString("vulnerabilities"),
                creature.optString("immunities"),res.toString());
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Ficha mecanica de criatura identificada",
                creature.optString("name")+": CON "+score+" e proficiencia de salvaguarda "+
                proficiencyBonus+"; recibo "+receipt,"combate",receipt,"wilem_cookbook");
            out[0]=monsterSaveInfo(combatantId);
            if(!out[0].optBoolean("known"))
                throw new IllegalStateException("Valor da criatura nao persistiu");
        }});
        return out[0];
    }

    /** Only exact positive immunity tokens qualify; a sentence such as "not immune"
     * must never produce a mechanical immunity. JSON arrays are accepted as well. */
    private static boolean hasConditionImmunity(Object raw,String condition){
        if(raw==null||raw==JSONObject.NULL)return false;
        if(raw instanceof JSONArray){
            JSONArray a=(JSONArray)raw;
            for(int i=0;i<a.length();i++)if(hasConditionImmunity(a.opt(i),condition))return true;
            return false;
        }
        String text=String.valueOf(raw).trim();
        if(text.startsWith("[")){
            try{return hasConditionImmunity(new JSONArray(text),condition);}
            catch(Exception ignored){return false;}
        }
        if(ConditionTokenGuard.has(text,condition))return true;
        if("inconsciente".equals(condition)){
            for(String part:text.split("[,;\\n]+")){
                String candidate=part.trim().toLowerCase(Locale.ROOT);
                if(candidate.equals("sleep immunity")||candidate.equals("immune to sleep")||
                    candidate.equals("imune a sono")||candidate.equals("imunidade a sono"))
                    return true;
            }
        }
        return false;
    }
    private static boolean immunityFrom(JSONObject obj,String condition){
        return obj!=null&&(hasConditionImmunity(obj.opt("condition_immunities"),condition)||
            hasConditionImmunity(obj.opt("immunities"),condition));
    }
    private boolean monsterConditionImmune(JSONObject target,String condition){
        JSONObject res=object(target.optString("resources_json","{}"));
        if(immunityFrom(target,condition)||immunityFrom(res,condition))return true;
        String blockId=res.optString("block_id","");
        if(!blockId.isEmpty()){
            JSONObject p=CompiledStatblockEngine.runtimeProfile(blockId);
            if(p.optBoolean("found",false)&&immunityFrom(p,condition))return true;
        }
        if("npc".equalsIgnoreCase(target.optString("source_kind"))&&target.optLong("source_id")>0){
            JSONObject npc=db.getNpcMemory(target.optLong("source_id"));
            if(npc.length()>0&&immunityFrom(object(npc.optString("stats_json","{}")),condition))
                return true;
        }
        return false;
    }

    public synchronized JSONObject resolveBaitWithPhysicalSave(final String baitId,final long combatantId,
        final boolean consumed,final boolean compatibleDiet,final boolean immune,
        final Dice3DOverlay.RollResult roll,final int conBonus,
        final boolean refereeConfirmedBonus,final String receipt)throws Exception{
        JSONObject target=db.getCombatant(combatantId);
        if(target.length()==0)throw new IllegalArgumentException("Criatura do combate nao encontrada");
        JSONObject authoritative=authoritativeMonsterConSave(target);
        if(!authoritative.optBoolean("known",false))
            throw new IllegalStateException("Criatura sem CON mecanica. Cadastre o atributo na ficha do combate antes de rolar.");
        final int effectiveBonus=authoritative.optInt("bonus");
        if(roll==null||!"d20".equals(roll.formula)||!"Normal".equals(roll.mode))
            throw new IllegalArgumentException("Rolagem fisica d20 normal obrigatoria");
        DiceRollSpec spec=DiceRollSpec.parse(roll.formula,roll.mode);
        java.util.List<Integer> faces=spec.requirePhysicalAgreement(roll.rolls,roll.physicalFaces);
        DiceRollSpec.Result checked=spec.resolve(faces);
        if(checked.natural<1||checked.natural>20||checked.natural!=roll.natural||checked.total!=roll.total
            ||!RollIntegrity.signature(spec.formula,roll.physicalFaces,spec.modifier,roll.physicsSeed,roll.total)
                .equals(roll.integritySignature))
            throw new IllegalStateException("D20 nao corresponde ao resultado fisico registrado");
        JSONObject proof=new JSONObject().put("source","physical3d").put("dice",checked.natural)
            .put("save_bonus",effectiveBonus).put("total",checked.natural+effectiveBonus)
            .put("bonus_authority",authoritative.optString("authority"))
            .put("block_id",authoritative.optString("block_id"))
            .put("signature",roll.integritySignature).put("physics_seed",roll.physicsSeed);
        return resolveBaitInternal(baitId,combatantId,consumed,compatibleDiet,immune,
            checked.natural+effectiveBonus,proof,receipt);
    }

    private JSONObject resolveBaitInternal(final String baitId,final long combatantId,
        final boolean consumed,final boolean compatibleDiet,final boolean immune,
        final int constitutionSaveTotal,final JSONObject saveProof,
        final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem nao esta na companhia");
        if(!consumed||!compatibleDiet)throw new IllegalStateException("Isca nao consumida por alvo compativel");
        // A CON baixa pode gerar salvaguarda total negativa (d20=1, bonus=-5).
        // A validade do dado ja e provada pelo recibo fisico, nao pelo sinal do total.
        if(combatantId<=0)throw new IllegalArgumentException("Alvo do combate obrigatorio");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);JSONObject bait=null;
            JSONArray arr=st.getJSONArray("baits");
            for(int i=0;i<arr.length();i++){JSONObject x=arr.optJSONObject(i);if(x!=null&&baitId.equals(x.optString("id"))){bait=x;break;}}
            if(bait==null||!"armada".equals(bait.optString("status")))throw new IllegalStateException("Isca nao armada ou ja consumida");
            JSONObject target=db.getCombatant(combatantId);
            if(target.length()==0||target.optInt("is_player",0)==1||target.optInt("hp",0)<=0)
                throw new IllegalArgumentException("Alvo deve ser criatura viva do combate");
            String baitScene=bait.optString("scene","").trim();
            String currentScene=sceneLocation(db).trim();
            if(!baitScene.isEmpty()&&!baitScene.equalsIgnoreCase(currentScene))
                throw new IllegalStateException("Isca nao esta na cena atual; nao pode afetar criatura distante");
            JSONObject recipe=WilemCookbookCatalog.recipeById(bait.optString("recipe_id"));
            if(recipe.optBoolean("requires_save",false)&&saveProof==null)
                throw new IllegalStateException("Isca fantastica requer salvaguarda com d20 fisico e bonus confirmado");
            String effect=recipe.optString("effect_key"),condition="";
            boolean failed=constitutionSaveTotal<recipe.optInt("dc");
            boolean failedCooking="somente_risco".equals(bait.optString("batch_quality","somente_risco"));
            if(!immune&&failed&&!failedCooking){
                if(effect.equals("isca_sonolencia"))condition="inconsciente";
                else if(effect.equals("isca_tontura"))condition="envenenado";
                else if(effect.equals("isca_lentidao"))condition="caido";
            }
            boolean autoImmune=!condition.isEmpty()&&monsterConditionImmune(target,condition);
            if(autoImmune)condition="";
            if(!condition.isEmpty()){
                // Timers need an active encounter to expire on the target's next turn.
                if(db.getCombatState().optInt("running",0)!=1)
                    throw new IllegalStateException("Efeito de isca temporario requer encontro ativo para expirar automaticamente.");
                String before=target.optString("conditions","");
                if(ConditionTokenGuard.has(before,condition))
                    throw new IllegalStateException("Condicao preexistente nao pode ser atribuida a esta isca.");
                String next=before.isEmpty()?condition:before+","+condition;
                JSONArray timers=MonsterConditionTimerLedger.parse(target.optString("condition_timers_json","[]"));
                for(int i=0;i<timers.length();i++){
                    JSONObject t=timers.optJSONObject(i);
                    if(t!=null&&ConditionTokenGuard.has(t.optString("name"),condition))
                        throw new IllegalStateException("Conflito com temporizador de outra fonte");
                }
                JSONObject timed=new JSONObject().put("schema",MonsterConditionTimerLedger.SCHEMA)
                    .put("name",condition).put("rounds",1)
                    .put("source_actor_id",ownerId()).put("trigger","target_turn_start")
                    .put("source_statblock","wilem_cookbook")
                    .put("source_action",effect).put("source_receipt",receipt);
                timers.put(timed);MonsterConditionTimerLedger.parse(timers.toString());
                JSONObject res=object(target.optString("resources_json","{}"));
                res.put("wilem_bait_source",baitId);
                db.updateCombatantAdvancedChecked(combatantId,target.optInt("temp_hp",0),
                    target.optString("concentration"),target.optInt("reaction_used",0)==1,
                    target.optInt("death_success",0),target.optInt("death_fail",0),
                    next,timers.toString(),target.optString("resistances"),
                    target.optString("vulnerabilities"),target.optString("immunities"),res.toString());
            }
            bait.put("status","resolvida").put("consumed",true).put("target_id",combatantId)
                .put("save_total",constitutionSaveTotal)
                .put("save_proof",saveProof==null?new JSONObject():saveProof)
                .put("effect_applied",!condition.isEmpty())
                .put("resolved_world_hour",worldHour(db)).put("applied_condition",condition)
                .put("ends_world_hour",worldHour(db)+(1d/60d))
                .put("ends_combat_round",db.getCombatState().optInt("round_no",1)+1)
                 .put("failed_cooking",failedCooking).put("immunity_auto_detected",autoImmune);
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Isca testada: "+recipe.optString("name"),bait.toString(),"isca",receipt,"wilem_cookbook");
            result[0]=bait;
        }});
        return result[0];
    }

    public synchronized JSONObject addNote(final String title,final String text,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem deve estar na companhia para escrever");
        if(title==null||title.trim().length()<2||text==null||text.trim().length()<2)throw new IllegalArgumentException("Titulo e texto necessarios");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject state=state();requireReceipt(state,receipt);
            JSONObject note=new JSONObject().put("title",title.trim()).put("text",text.trim())
              .put("location",sceneLocation(db)).put("world_hour",worldHour(db));
            state.getJSONArray("notes").put(note);remember(state,receipt);write(db.getWritableDatabase(),ownerId(),state);
            out[0]=note;
        }});
        return out[0];
    }


    /** Limpeza temporal de efeitos culinarios com origem verificada, sem apagar buffs alheios. */
    private void reconcileMeals(){
        double now=worldHour(db);
        for(JSONObject hero:db.listActiveCharacters()){
            long id=hero.optLong("id");JSONObject sheet=object(hero.optString("sheet_json","{}"));
            JSONArray effects=sheet.optJSONArray("meal_effects");if(effects==null||effects.length()==0)continue;
            JSONArray still=new JSONArray();boolean modified=false;
            for(int i=0;i<effects.length();i++){
                JSONObject effect=effects.optJSONObject(i);if(effect==null)continue;
                boolean expired=effect.optDouble("ends_world_hour",Double.MAX_VALUE)<=now;
                boolean riskExpired=effect.optDouble("risk_ends_world_hour",Double.MAX_VALUE)<=now;
                if(expired&&(!effect.optBoolean("poisoned_after_consumption",false)||riskExpired)){modified=true;}
                else still.put(effect);
                if(!expired&&!riskExpired)continue;
                for(JSONObject combatant:db.listCombatants()){
                    if(!"character".equalsIgnoreCase(combatant.optString("source_kind"))||combatant.optLong("source_id")!=id)continue;
                    JSONObject provenance=object(combatant.optString("resources_json","{}"));
                    String receipt=effect.optString("receipt");
                    try{
                        if(riskExpired&&receipt.equals(provenance.optString("wilem_meal_poison_receipt"))){
                            StringBuilder conditions=new StringBuilder();
                            for(String part:combatant.optString("conditions","").split(",")){
                                if(part.trim().equalsIgnoreCase("envenenado"))continue;
                                if(!part.trim().isEmpty()){if(conditions.length()>0)conditions.append(",");conditions.append(part.trim());}
                            }
                            db.updateConditionsChecked(combatant.optLong("id"),conditions.toString());
                            provenance.remove("wilem_meal_poison_receipt");
                        }
                        if(expired&&receipt.equals(provenance.optString("wilem_temp_hp_receipt"))){
                            int original=provenance.optInt("wilem_temp_hp_initial",-1);
                            int current=combatant.optInt("temp_hp",0);
                            if(original>0&&current>0&&current<=original){
                                ContentValues hp=new ContentValues();hp.put("temp_hp",0);
                                db.getWritableDatabase().update("combatants",hp,"id=? AND world_profile=?",
                                    new String[]{String.valueOf(combatant.optLong("id")),profile()});
                            }
                            provenance.remove("wilem_temp_hp_receipt");
                            provenance.remove("wilem_temp_hp_initial");
                        }
                        ContentValues source=new ContentValues();source.put("resources_json",provenance.toString());
                        db.getWritableDatabase().update("combatants",source,"id=? AND world_profile=?",
                            new String[]{String.valueOf(combatant.optLong("id")),profile()});
                    }catch(Exception ignored){}
                }
            }
            if(modified)try{sheet.put("meal_effects",still);db.updateCharacterSheetChecked(id,sheet.toString());}catch(Exception ignored){}
        }
    }

    /**
     * O Mestre pode apresentar pistas culinarias CONTEXTUAIS, sem revelacao gratuita
     * de formula, efeito, preparo ou verdade oculta. Deterministico por local.
     */
    private JSONArray sceneRumorCandidates(String location,JSONArray known,int heroLevel){
        JSONArray out=new JSONArray(),all=WilemCookbookCatalog.recipes();
        String where=location==null?"":location.toLowerCase(Locale.ROOT);
        if(where.trim().length()<3)return out;
        boolean foodPlace=where.contains("taverna")||where.contains("pousada")||
            where.contains("mercado")||where.contains("feira")||where.contains("marco")||
            where.contains("cozinha")||where.contains("restaurante")||where.contains("strixhaven");
        int seed=Math.abs(where.hashCode()%Math.max(1,all.length()));
        for(int offset=0;offset<all.length()&&out.length()<5;offset++){
            JSONObject r=all.optJSONObject((offset+seed)%all.length());if(r==null)continue;
            if(contains(known,r.optString("id"))||r.optInt("tier",0)>heroLevel+2)continue;
            String origin=r.optString("origin").toLowerCase(Locale.ROOT);
            boolean nature=where.contains("floresta")||where.contains("pantano")||
                where.contains("bosque")||where.contains("caverna")||where.contains("lago")||
                where.contains("montanha")||where.contains("deserto")||where.contains("litoral");
            if(!foodPlace&&!nature)continue;
            if(!foodPlace&&!origin.contains(where.trim())&&!where.contains(origin))continue;
            try{out.put(new JSONObject().put("recipe_id",r.optString("id"))
                .put("hint",r.optString("name")).put("rumor_location",location)
                .put("knowledge_status","nao_conhecido_nao_verificado")
                .put("require_npc_witness_or_source",true));}catch(Exception ignored){}
        }
        return out;
    }

    public JSONObject masterView(){
        JSONObject out=new JSONObject();
        try{
            long id=ownerId();if(id<=0)return out.put("available",false);
            reconcileMeals();JSONObject st=state();String location=sceneLocation(db);
            JSONArray nearby=new JSONArray();JSONArray rumors=st.optJSONArray("rumors");
            for(int i=0;rumors!=null&&i<rumors.length();i++){JSONObject rum=rumors.optJSONObject(i);if(rum==null||"a_ouvir".equals(rum.optString("status")))continue;
                String where=rum.optString("location","").toLowerCase(Locale.ROOT);
                if(!location.trim().isEmpty()&&!where.trim().isEmpty()&&
                   (location.toLowerCase(Locale.ROOT).contains(where)||where.contains(location.toLowerCase(Locale.ROOT))))nearby.put(rum);
            }
            out.put("available",true).put("owner","Wilem").put("owner_id",id).put("party_active",canCook())
                .put("catalog_size",WilemCookbookCatalog.catalogSize()).put("known",st.optJSONArray("known"))
                .put("nearby_rumors",nearby).put("recent_meals",tail(st.optJSONArray("meals"),6))
                .put("active_baits",tail(st.optJSONArray("baits"),6))
                .put("rumor_candidates",canCook()?sceneRumorCandidates(location,st.optJSONArray("known"),hero().optInt("level",1)):new JSONArray())
                .put("market_available",canCook()&&marketOffers().length()>0)
                .put("market_offers",marketOffers())
                .put("merchant_stock_contract",new JSONObject()
                    .put("npc_stats_json",new JSONObject().put("revision","restock-1")
                        .put("wilem_market_stock",new JSONArray().put(new JSONObject()
                            .put("ingredient_id","batata").put("quantity",8).put("price_cp",2))))
                    .put("rule","Estoque pertence ao NPC presente ou ao POI do atlas; so pode mudar por uma acao de comercio ou evento de reabastecimento. Revisao nova exige causa do mundo."))
                .put("forage_opportunities",canCook()?forageableIngredients():new JSONArray())
                .put("scene_forage_node_contract",new JSONObject()
                    .put("scene_field","clues_json")
                    .put("node",new JSONObject().put("type","wilem_forage_node")
                        .put("id","id_unico_recurso").put("ingredient_id","batata")
                        .put("quantity",3).put("revision","colheita-1"))
                    .put("rule","O Mestre so registra um recurso realmente existente e acessivel na cena ou usa o bioma do hex confirmado no atlas; nao fabricar recursos pela mera palavra 'horta' no texto. A colheita consome estoque do mundo."))
                .put("monster_constitution_contract",new JSONObject()
                    .put("sources","statblock compilado, ficha canonica de NPC, atributos mecanicos do encontro ou perfil persistente confirmado pelo Mestre")
                    .put("rule","Nunca inventar modificador ou aceitar um total digitado por jogada. Se faltar CON, o Mestre deve informar o valor do atributo uma unica vez na ficha da criatura."))
                .put("unclaimed_world_rewards",canCook()?worldRewardOffers():new JSONArray())
                .put("world_event_reward_contract",new JSONObject()
                    .put("type","world_event")
                    .put("consequence_json",new JSONObject().put("wilem_ingredient",
                        new JSONObject().put("ingredient_id","id_do_catalogo")
                            .put("quantity",1).put("recipient_character_id",id)))
                    .put("status_required","resolved")
                    .put("consequence_status_required","applied")
                    .put("rule","Criar evento real por historia/importacao; entregar somente depois da aplicacao. Nao inventar recompensa nem declarar evento aplicado sem causa."))
                .put("rules","Caderno de Wilem: regras de culinaria opcionais e originais compativeis com D&D 2024, nunca conteudo oficial. Comprar somente em mercado/mercador presente com carteira e estoque; coletar so recursos do ambiente com teste fisico e limite diario; recompensa especial so mediante evento do mundo realmente aplicado. Refeicoes comuns alimentam; poderes exigem teste e recibo. Rumor nao e verdade revelada. Isca nao atinge sem ingestao e salvaguarda.")
                .put("read_only",!canCook());
        }catch(Exception e){try{out.put("error",e.getMessage());}catch(Exception ignored){}}
        return out;
    }
    private static JSONArray tail(JSONArray a,int n){JSONArray r=new JSONArray();if(a!=null)for(int i=Math.max(0,a.length()-n);i<a.length();i++)r.put(a.opt(i));return r;}
    public static String newReceipt(){return "wilem-"+UUID.randomUUID().toString();}
}
