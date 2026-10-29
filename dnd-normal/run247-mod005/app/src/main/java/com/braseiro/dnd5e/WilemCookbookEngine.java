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
        for(JSONObject c:db.listCharacters()){
            String n=c.optString("name","").trim().toLowerCase(Locale.ROOT);
            if(n.equals("wilem")||n.equals("willem"))return c.optLong("id",0);
        }
        return 0;
    }
    public boolean ownerExists(){return ownerId()>0;}
    public boolean canCook(){
        long id=ownerId();if(id<=0)return false;
        JSONObject c=db.getCharacter(id);
        return c.length()>0&&c.optInt("active",0)==1&&db.findActivePartyHero(id).length()>0;
    }
    private String profile(){return db.getActiveMapProfile();}
    private void ensureTable(SQLiteDatabase sql){
        sql.execSQL("CREATE TABLE IF NOT EXISTS wilem_recipe_books ("+
            "world_profile TEXT NOT NULL, character_id INTEGER NOT NULL,"+
            "state_json TEXT NOT NULL DEFAULT '{}', updated_at INTEGER NOT NULL DEFAULT 0,"+
            "PRIMARY KEY(world_profile,character_id))");
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
        rumors.put(new JSONObject().put("recipe_id","mon_cor uja_urso_0".replace(" ","")).put("location","Biblioteca de Strixhaven").put("source","anotacao de aula").put("text","Talvez uma carne monstruosa possa sustentar um grupo apos uma travessia. Precisa de pesquisa.").put("status","hipotese"));
        rumors.put(new JSONObject().put("recipe_id","mon_aranha_1").put("location","Laboratorio de historia natural").put("source","estudantes de zoologia").put("text","Partes de aranha exigem remocao cuidadosa das glandulas. Nao experimentar sem identificar.").put("status","hipotese"));
        rumors.put(new JSONObject().put("recipe_id","mon_basilisco_4").put("location","Marco do Percurso").put("source","viajante ainda desconhecido").put("text","Dizem existir uma receita de basilisco, mas nao ha provas.").put("status","a_ouvir"));
        rumors.put(new JSONObject().put("recipe_id","isca_mel_fantasia").put("location","Feira de mercadores feericos").put("source","rumor nao confirmado").put("text","Um confeiteiro conhece doces que deixam certas criaturas sonolentas.").put("status","a_ouvir"));
        JSONArray notes=new JSONArray().put(new JSONObject().put("title","Primeira pagina")
            .put("text","Batatas e cebolas sao confiaveis. Nao confundir fungos luminosos com fungos comestiveis. Perguntar a Auri sobre esporos."))
            .put(new JSONObject().put("title","Caldeirao e utensilios")
            .put("text","Meu caldeirao e meus utensilios ja pertencem ao equipamento. Sem reproduzi-los na despensa."));
        return new JSONObject().put("schema",1).put("owner_id",id).put("known",WilemCookbookCatalog.starterIds())
            .put("rumors",rumors).put("notes",notes).put("experiments",new JSONArray())
            .put("cooked",new JSONArray()).put("meals",new JSONArray()).put("baits",new JSONArray())
            .put("receipts",new JSONArray()).put("discoveries",new JSONArray())
            .put("created_world_hour",worldHour(db));
    }
    public synchronized JSONObject state(){
        long id=ownerId();if(id<=0)return new JSONObject();
        SQLiteDatabase sql=db.getWritableDatabase();ensureTable(sql);
        Cursor cursor=sql.rawQuery("SELECT state_json FROM wilem_recipe_books WHERE world_profile=? AND character_id=?",
            new String[]{profile(),String.valueOf(id)});
        try{
            if(cursor.moveToFirst())return object(cursor.getString(0));
        }finally{cursor.close();}
        try{
            JSONObject initial=fresh(id);write(sql,id,initial);return initial;
        }catch(Exception e){throw new IllegalStateException("Nao foi possivel inicializar o caderno",e);}
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
    public synchronized JSONObject acquire(final String ingredientId,final int amount,final String source,
                                           final boolean acquisitionConfirmed,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem precisa estar na companhia para obter ingredientes em seu nome");
        if(amount<1||amount>30)throw new IllegalArgumentException("Quantidade precisa ser entre 1 e 30");
        if(!acquisitionConfirmed)throw new IllegalStateException("A compra ou coleta precisa estar confirmada; nao criar recursos do nada");
        final JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        if(ing.length()==0)throw new IllegalArgumentException("Ingrediente desconhecido");
        String src=source==null?"":source.trim().toLowerCase(Locale.ROOT);
        if(!(src.equals("mercado_pago")||src.equals("coleta_confirmada")||src.equals("recompensa_confirmada")))
            throw new IllegalArgumentException("Fonte nao confirmada");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject state=state();requireReceipt(state,receipt);
            JSONObject ch=hero(),sheet=sheet();JSONArray items=new JSONArray(ledger(sheet).toString());
            String key="wilem_ing_"+ingredientId;int found=-1;
            for(int i=0;i<items.length();i++){JSONObject x=items.optJSONObject(i);
                if(x!=null&&(key.equals(x.optString("id"))||ing.optString("name").equalsIgnoreCase(x.optString("name")))){if(found>=0)throw new IllegalStateException("Ingrediente ambiguo no inventario");found=i;}
            }
            JSONObject item=found>=0?items.getJSONObject(found):new JSONObject().put("id",key).put("name",ing.optString("name")).put("quantity",0);
            item.put("quantity",item.optInt("quantity",0)+amount).put("cookbook_ingredient_id",ingredientId);
            if(found>=0)items.put(found,item);else items.put(item);
            sheet.put("inventory_ledger",items).put("inventory_load",InventoryLedgerEngine.totals(items,ch.optInt("str",10),false));
            db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            remember(state,receipt);write(db.getWritableDatabase(),ownerId(),state);
            String cost=src.equals("mercado_pago")?"Compra JA PAGA, registrar gasto externo: "+(amount*ing.optInt("suggested_price_cp",0))+" PC sugeridos.":"Coleta/recompensa confirmada.";
            db.addJournalFact("Wilem encontrou ingredientes",ing.optString("name")+" x"+amount+"; "+cost+" Origem: "+sceneLocation(db),"culinaria",receipt,"wilem_cookbook");
            result[0]=new JSONObject().put("acquired",true).put("quantity",amount).put("item",item).put("money_auto_debited",false);
        }});
        return result[0];
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
            JSONArray known=state.getJSONArray("known");if(!contains(known,recipeId))known.put(recipeId);
            state.getJSONArray("discoveries").put(new JSONObject().put("recipe_id",recipeId)
                .put("evidence",evidence.trim()).put("location",sceneLocation(db)).put("world_hour",worldHour(db)));
            remember(state,receipt);write(db.getWritableDatabase(),ownerId(),state);
            db.addJournalFact("Receita descoberta: "+r.optString("name"),evidence,"culinaria",receipt,"wilem_cookbook");
            out[0]=new JSONObject().put("known",true).put("recipe",r);
        }});
        return out[0];
    }
    private static int stock(JSONArray ledger,String ingredientId){
        String id="wilem_ing_"+ingredientId;int qty=0;
        JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        for(int i=0;i<ledger.length();i++){
            JSONObject item=ledger.optJSONObject(i);if(item!=null&&(id.equals(item.optString("id"))||ing.optString("name").equalsIgnoreCase(item.optString("name"))))qty+=Math.max(0,item.optInt("quantity",0));
        }return qty;
    }
    private static JSONArray subtract(JSONArray ledger,JSONObject ingredients,int multiplier)throws Exception{
        JSONArray out=new JSONArray(ledger.toString());JSONArray names=ingredients.names();
        if(names==null)throw new IllegalArgumentException("Receita sem ingredientes");
        for(int j=0;j<names.length();j++){
            String ingredient=names.getString(j);int needed=ingredients.getInt(ingredient)*multiplier;
            if(needed<1||stock(out,ingredient)<needed)throw new IllegalStateException("Falta "+ingredient+" x"+needed);
            JSONObject ref=WilemCookbookCatalog.ingredientById(ingredient);
            for(int i=0;i<out.length()&&needed>0;i++){JSONObject x=out.optJSONObject(i);if(x==null)continue;
                if(("wilem_ing_"+ingredient).equals(x.optString("id"))||ref.optString("name").equalsIgnoreCase(x.optString("name"))){
                    int spent=Math.min(needed,Math.max(0,x.optInt("quantity",0)));x.put("quantity",x.optInt("quantity",0)-spent);needed-=spent;
                }
            }
            if(needed>0)throw new IllegalStateException("Estoque inconsistente");
        }return out;
    }
    public synchronized JSONObject cook(final String recipeId,final int roll,final boolean fire,
                                         final boolean utensils,final String receipt)throws Exception{
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
            JSONObject ch=hero(),sheet=sheet();JSONArray after=subtract(ledger(sheet),recipe.getJSONObject("ingredients"),1);
            sheet.put("inventory_ledger",after).put("inventory_load",InventoryLedgerEngine.totals(after,ch.optInt("str",10),false));
            db.updateCharacterSheetChecked(ownerId(),sheet.toString());
            boolean special=!recipe.optString("type").equals("cotidiana");
            int check=roll+Math.max(0,2+(hero().optInt("wis",10)-10)/2);
            String result=!special?"refeicao_comum":check>=recipe.optInt("dc")+5?"somente_beneficio":
                check>=recipe.optInt("dc")?"beneficio_com_risco":"somente_risco";
            JSONObject batch=new JSONObject().put("id",receipt).put("recipe_id",recipeId)
                .put("remaining",recipe.optInt("servings",2)).put("result",result)
                .put("created_world_hour",worldHour(db)).put("expires_world_hour",worldHour(db)+24)
                .put("source","wilem_cookbook").put("kind",recipe.optString("type"));
            st.getJSONArray("cooked").put(batch);
            st.getJSONArray("experiments").put(new JSONObject().put("recipe_id",recipeId)
                .put("roll",roll).put("check",check).put("result",result).put("world_hour",worldHour(db)));
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
            if(targetId>0){JSONObject ch=db.getCharacter(targetId),sheet=object(ch.optString("sheet_json","{}"));
                JSONArray old=sheet.optJSONArray("meal_effects");if(old==null)old=new JSONArray();
                for(int i=0;i<old.length();i++){JSONObject active=old.optJSONObject(i);
                    if(active!=null&&active.optDouble("ends_world_hour",0)>worldHour(db)&&
                        !"nenhum".equals(active.optString("effect_key"))&&magic)
                        throw new IllegalStateException("Ja ha beneficio culinario especial ativo; nao acumular");
                }
                if(magic)old.put(effect);sheet.put("meal_effects",old);
                sheet.put("last_nourishing_meal_world_hour",worldHour(db));
                db.updateCharacterSheetChecked(targetId,sheet.toString());
                if(magic&&"pv_temporarios".equals(effect.optString("effect_key"))){
                    ContentValues cv=new ContentValues();cv.put("temp_hp",Math.max(recipe.optInt("effect_value",1),0));
                    db.getWritableDatabase().update("combatants",cv,"world_profile=? AND source_kind='character' AND source_id=? AND temp_hp<?",
                        new String[]{profile(),String.valueOf(targetId),String.valueOf(recipe.optInt("effect_value",1))});
                }
            }
            batch.put("remaining",batch.optInt("remaining")-1);
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
            JSONObject bait=new JSONObject().put("id",receipt).put("recipe_id",batch.optString("recipe_id"))
                .put("position",location.trim()).put("scene",sceneLocation(db)).put("world_hour",worldHour(db))
                .put("consumed",false).put("status","armada");batch.put("remaining",batch.optInt("remaining")-1);
            st.getJSONArray("baits").put(bait);remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Isca de Wilem",bait.toString(),"isca",receipt,"wilem_cookbook");result[0]=bait;
        }});
        return result[0];
    }
    /** Nunca incapacita so por colocacao: exige alvo real, ingestao confirmada e salvaguarda. */
    public synchronized JSONObject resolveBait(final String baitId,final long combatantId,
        final boolean consumed,final boolean compatibleDiet,final boolean immune,
        final int constitutionSaveTotal,final String receipt)throws Exception{
        if(!canCook())throw new IllegalStateException("Wilem nao esta na companhia");
        if(!consumed||!compatibleDiet)throw new IllegalStateException("Isca nao consumida por alvo compativel");
        if(combatantId<=0||constitutionSaveTotal<0)throw new IllegalArgumentException("Alvo e salvaguarda explicita obrigatorios");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject st=state();requireReceipt(st,receipt);JSONObject bait=null;
            JSONArray arr=st.getJSONArray("baits");
            for(int i=0;i<arr.length();i++){JSONObject x=arr.optJSONObject(i);if(x!=null&&baitId.equals(x.optString("id"))){bait=x;break;}}
            if(bait==null||!"armada".equals(bait.optString("status")))throw new IllegalStateException("Isca nao armada ou ja consumida");
            JSONObject target=db.getCombatant(combatantId);if(target.length()==0||target.optInt("is_player",0)==1)throw new IllegalArgumentException("Alvo de monstro invalido");
            JSONObject recipe=WilemCookbookCatalog.recipeById(bait.optString("recipe_id"));
            String effect=recipe.optString("effect_key"),condition="";
            boolean failed=constitutionSaveTotal<recipe.optInt("dc");
            if(!immune&&failed){if(effect.equals("isca_sonolencia"))condition="inconsciente";else if(effect.equals("isca_tontura"))condition="envenenado";else if(effect.equals("isca_lentidao"))condition="lento";}
            if(!condition.isEmpty()){
                String before=target.optString("conditions","");String next=before.isEmpty()?condition:before+","+condition;
                db.updateConditionsChecked(combatantId,next);
            }
            bait.put("status","resolvida").put("consumed",true).put("target_id",combatantId)
                .put("save_total",constitutionSaveTotal).put("effect_applied",!condition.isEmpty())
                .put("resolved_world_hour",worldHour(db));
            remember(st,receipt);write(db.getWritableDatabase(),ownerId(),st);
            db.addJournalFact("Isca testada: "+recipe.optString("name"),bait.toString(),"isca",receipt,"wilem_cookbook");
            result[0]=bait;
        }});
        return result[0];
    }
    public JSONObject masterView(){
        JSONObject out=new JSONObject();
        try{
            long id=ownerId();if(id<=0)return out.put("available",false);
            JSONObject st=state();String location=sceneLocation(db);
            JSONArray nearby=new JSONArray();JSONArray rumors=st.optJSONArray("rumors");
            for(int i=0;rumors!=null&&i<rumors.length();i++){JSONObject rum=rumors.optJSONObject(i);if(rum==null)continue;
                String where=rum.optString("location","").toLowerCase(Locale.ROOT);
                if(location.toLowerCase(Locale.ROOT).contains(where)||where.toLowerCase(Locale.ROOT).contains(location.toLowerCase(Locale.ROOT)))nearby.put(rum);
            }
            out.put("available",true).put("owner","Wilem").put("owner_id",id).put("party_active",canCook())
                .put("catalog_size",WilemCookbookCatalog.catalogSize()).put("known",st.optJSONArray("known"))
                .put("nearby_rumors",nearby).put("recent_meals",tail(st.optJSONArray("meals"),6))
                .put("active_baits",tail(st.optJSONArray("baits"),6))
                .put("rules","Culinaria oficial 2024 + regras originais opcionais. Refeicoes comuns alimentam; poderes exigem teste e recibo. Rumor nao e verdade revelada. Isca nao atinge sem ingestao e salvaguarda.")
                .put("read_only",!canCook());
        }catch(Exception e){try{out.put("error",e.getMessage());}catch(Exception ignored){}}
        return out;
    }
    private static JSONArray tail(JSONArray a,int n){JSONArray r=new JSONArray();if(a!=null)for(int i=Math.max(0,a.length()-n);i<a.length();i++)r.put(a.opt(i));return r;}
    public static String newReceipt(){return "wilem-"+UUID.randomUUID().toString();}
}
