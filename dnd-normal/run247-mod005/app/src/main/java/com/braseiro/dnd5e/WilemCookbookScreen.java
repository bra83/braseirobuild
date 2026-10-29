package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Aba nativa e offline, independente do carregamento do Mestre e sem requests de IA. */
public final class WilemCookbookScreen {
    private final Activity activity; private final AppDb db; private final WilemCookbookEngine book;
    private final Runnable refresh; private String tab="Receitas";
    private static final int GOLD=0xffedc17b,TEXT=0xfff6e8d3,MUTED=0xffb7a68f,BG=0xff1c1814,CARD=0xff2c251e,ACCENT=0xff5b3825;
    public WilemCookbookScreen(Activity a,AppDb database,Runnable rerender){activity=a;db=database;book=new WilemCookbookEngine(database);refresh=rerender;}
    private int d(int v){return (int)(v*activity.getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String t,int size,int color){TextView v=new TextView(activity);v.setText(t);v.setTextSize(size);v.setTextColor(color);v.setPadding(d(7),d(7),d(7),d(7));return v;}
    private LinearLayout vertical(){LinearLayout x=new LinearLayout(activity);x.setOrientation(LinearLayout.VERTICAL);return x;}
    private LinearLayout panel(){LinearLayout x=vertical();x.setBackgroundColor(CARD);x.setPadding(d(9),d(9),d(9),d(9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,d(5),0,d(9));x.setLayoutParams(p);return x;}
    private Button btn(String label){Button b=new Button(activity);b.setText(label);b.setTextColor(TEXT);b.setAllCaps(false);b.setTextSize(13);b.setMinHeight(d(45));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));return b;}
    private void fail(Exception e){Toast.makeText(activity,e.getMessage()==null?"Acao indisponivel":e.getMessage(),Toast.LENGTH_LONG).show();}
    private void changed(){if(refresh!=null)refresh.run();}
    private static String str(JSONObject obj,String name){return obj==null?"":obj.optString(name,"");}
    public View render(){
        ScrollView s=new ScrollView(activity);s.setFillViewport(true);s.setBackgroundColor(BG);
        LinearLayout root=vertical();root.setPadding(d(11),d(11),d(11),d(28));s.addView(root);
        TextView title=text("O CALDEIRAO DE WILEM",23,GOLD);title.setTypeface(Typeface.SERIF,Typeface.BOLD);root.addView(title);
        root.addView(text("Livro de receitas, boatos, ingredientes e memorias a mesa",13,MUTED));
        WebView image=new WebView(activity);image.setBackgroundColor(Color.TRANSPARENT);image.setVerticalScrollBarEnabled(false);
        image.getSettings().setJavaScriptEnabled(false);image.loadUrl("file:///android_asset/wilem_caderno_ilustracao.svg");
        root.addView(image,new LinearLayout.LayoutParams(-1,d(262)));
        if(!book.ownerExists()){
            root.addView(text("A aba permanece exclusiva de Wilem. Importe a ficha do personagem para habilita-la.",15,TEXT));return s;
        }
        if(!book.canCook()){
            LinearLayout status=panel();status.addView(text("MODO LEITURA",15,GOLD));
            status.addView(text("Wilem nao esta no elenco ativo da companhia. O caderno pode ser consultado, mas nao e possivel cozinhar, coletar ou usar iscas.",13,TEXT));root.addView(status);
        }
        HorizontalScrollView scroll=new HorizontalScrollView(activity);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=new LinearLayout(activity);tabs.setOrientation(LinearLayout.HORIZONTAL);
        for(String t:new String[]{"Receitas","Despensa","Boatos","Mesa","Iscas","Notas"}){
            Button b=btn(t);b.setTextColor(t.equals(tab)?GOLD:TEXT);b.setOnClickListener(v->{tab=t;renderContent(root);});tabs.addView(b);
        }
        scroll.addView(tabs);root.addView(scroll);
        LinearLayout inside=vertical();inside.setTag("wilem_body");root.addView(inside);
        showTab(inside);
        return s;
    }
    private void renderContent(LinearLayout root){for(int i=0;i<root.getChildCount();i++)if("wilem_body".equals(root.getChildAt(i).getTag())){showTab((LinearLayout)root.getChildAt(i));break;}}
    private void showTab(LinearLayout body){
        body.removeAllViews();JSONObject st=book.state();boolean enabled=book.canCook();
        if(tab.equals("Receitas"))showRecipes(body,st,enabled);
        else if(tab.equals("Despensa"))showPantry(body,enabled);
        else if(tab.equals("Boatos"))showRumors(body,st,enabled);
        else if(tab.equals("Mesa"))showMeals(body,st,enabled);
        else if(tab.equals("Iscas"))showBaits(body,st,enabled);
        else showNotes(body,st);
    }
    private void showRecipes(LinearLayout body,JSONObject st,boolean enabled){
        body.addView(text("Receitas registradas | Catalogo original: "+WilemCookbookCatalog.catalogSize()+" preparacoes",14,GOLD));
        body.addView(text("Receitas desconhecidas nao sao reveladas. Wilem as aprende em Strixhaven, em viagens, nos mercados e nas conversas.",13,MUTED));
        JSONArray known=st.optJSONArray("known");if(known==null)return;
        for(int i=0;i<known.length();i++){
            JSONObject r=WilemCookbookCatalog.recipeById(known.optString(i));if(r.length()==0)continue;
            LinearLayout p=panel();TextView name=text(r.optString("name"),17,TEXT);name.setTypeface(Typeface.SERIF,Typeface.BOLD);p.addView(name);
            p.addView(text(("isca".equals(r.optString("type"))?"ISCA • ":"")+"CD "+r.optInt("dc")+" • "+r.optInt("minutes")+" min • "+r.optInt("servings")+" porcoes",12,MUTED));
            Button view=btn("VER PREPARO E INGREDIENTES");view.setOnClickListener(v->recipeDialog(r,enabled));p.addView(view);body.addView(p);
        }
    }
    private void recipeDialog(JSONObject r,boolean enabled){
        LinearLayout form=vertical();form.setPadding(d(14),d(9),d(14),d(4));
        form.addView(text(r.optString("description"),14,Color.DKGRAY));
        JSONObject needs=r.optJSONObject("ingredients");JSONArray names=needs==null?null:needs.names();
        StringBuilder items=new StringBuilder("Ingredientes necessarios:\n");
        JSONObject bag=book.pantry();JSONArray stocked=bag.optJSONArray("items");
        for(int i=0;names!=null&&i<names.length();i++){
            String key=names.optString(i);JSONObject x=WilemCookbookCatalog.ingredientById(key);
            items.append("• ").append(x.optString("name",key)).append(" x").append(needs.optInt(key,1)).append("\n");
        }
        form.addView(text(items.toString(),13,Color.DKGRAY));
        JSONArray steps=r.optJSONArray("steps");
        if(steps!=null){StringBuilder directions=new StringBuilder("PREPARO:\n");
            for(int i=0;i<steps.length();i++)directions.append(i+1).append(". ").append(steps.optString(i)).append("\n");
            form.addView(text(directions.toString(),13,Color.DKGRAY));
        }
        form.addView(text("Efeito: "+r.optString("effect_key")+". Porcoes nao se acumulam. Um preparo incomum segue CD com beneficio e risco.",13,Color.DKGRAY));
        boolean special=!"cotidiana".equals(r.optString("type"));
        form.addView(text(special?"O preparo usa a mesa 3D fisica: a face do d20, e nao um numero digitado, determina o teste.":"Comida comum: nao exige rolagem nem concede bonus magico.",13,Color.DKGRAY));
        CheckBox fire=new CheckBox(activity);fire.setText("Tenho fogo/fonte de calor");fire.setChecked(true);form.addView(fire);
        CheckBox utensils=new CheckBox(activity);utensils.setText("Utensilios de Wilem disponiveis");utensils.setChecked(true);form.addView(utensils);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(r.optString("name")).setView(form)
            .setNegativeButton("Fechar",null).setPositiveButton("PREPARAR",(x,w)->{
                try{
                    if(!enabled)throw new IllegalStateException("Wilem nao esta na party");
                    final String recipeId=r.optString("id");
                    final boolean hasFire=fire.isChecked(),hasTools=utensils.isChecked();
                    if(!special){
                        book.cook(recipeId,0,hasFire,hasTools,WilemCookbookEngine.newReceipt());changed();
                    }else{
                        Dice3DOverlay.roll(activity,activity.getSharedPreferences("settings",Activity.MODE_PRIVATE),
                            "d20","Normal","Wilem",book.ownerId(),new Dice3DOverlay.Callback(){
                                private boolean used=false;
                                public void onResult(Dice3DOverlay.RollResult rolled){
                                    if(used)return;used=true;
                                    try{book.cookWithPhysicalRoll(recipeId,rolled,hasFire,hasTools,WilemCookbookEngine.newReceipt());changed();}
                                    catch(Exception e){fail(e);}
                                }
                                public void onError(String message){fail(new IllegalStateException(message));}
                                public void onCancel(){/* Sem rolagem concluida: nenhum ingrediente consumido. */}
                            });
                    }
                }catch(Exception e){fail(e);}
            }).create();dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
    }
    private void showPantry(LinearLayout body,boolean enabled){
        body.addView(text("A DESPENSA",19,GOLD));
        body.addView(text("Os itens abaixo sao lidos diretamente do inventario estruturado da ficha de Wilem. Panelas e utensilios nao sao duplicados.",13,MUTED));
        JSONArray items=book.pantry().optJSONArray("items");
        if(items==null||items.length()==0)body.addView(text("Sem ingredientes contabilizados na ficha.",14,TEXT));
        else for(int i=0;i<items.length();i++){JSONObject x=items.optJSONObject(i);if(x!=null)body.addView(text("• "+x.optString("name")+"  ×"+x.optInt("quantity"),15,TEXT));}
        JSONArray bodies=book.harvestableCorpses();
        if(bodies.length()>0){
            Button harvest=btn("COLHER PARTE DE CRIATURA DERROTADA (D20)");
            harvest.setEnabled(enabled);harvest.setOnClickListener(v->harvestDialog(bodies));
            body.addView(harvest);
        }else body.addView(text("Nao ha carcaças elegiveis de monstros registrados no combate. Uma criatura precisa ter sido derrotada e identificada no mundo.",12,MUTED));
        Button buy=btn("COMPRAR EM MERCADO / MERCADOR DA CENA");buy.setEnabled(enabled);
        buy.setOnClickListener(v->acquireDialog());body.addView(buy);
        JSONArray forage=book.forageableIngredients();
        if(forage.length()>0){
            Button collect=btn("COLETAR NO AMBIENTE (D20)");collect.setEnabled(enabled);
            collect.setOnClickListener(v->forageDialog(forage));body.addView(collect);
        }else body.addView(text("A coleta so aparece quando o ambiente da cena combina com a origem do ingrediente. Cada item so admite uma tentativa por dia e local.",12,MUTED));
        JSONArray rewards=book.worldRewardOffers();
        if(rewards.length()>0){
            Button claim=btn("RESGATAR RECOMPENSA DE AVENTURA");claim.setEnabled(enabled);
            claim.setOnClickListener(v->claimRewardDialog(rewards));body.addView(claim);
        }
        body.addView(text("Compras verificam a cena, debitam a carteira canonica e respeitam estoque local. Coleta exige d20 fisico; materiais arcanos e tesouros exigem evento de mundo aplicado. Ingredientes nao nascem de um checkbox.",12,MUTED));
    }
    private void harvestDialog(JSONArray bodies){
        if(!book.canCook()){fail(new IllegalStateException("Wilem nao esta na companhia"));return;}
        String[] labels=new String[bodies.length()];
        for(int i=0;i<bodies.length();i++){
            JSONObject b=bodies.optJSONObject(i);
            JSONObject anatomy=b.optJSONObject("anatomy");
            labels[i]=b.optString("creature")+" — "+
                (anatomy==null?"parte identificada":anatomy.optString("cut"))+" (CD "+b.optInt("dc")+")";
        }
        LinearLayout form=vertical();
        Spinner targets=new Spinner(activity);
        targets.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));
        form.addView(targets);
        form.addView(text("Cada especie possui um corte anatomico e cautelas proprias. A colheita requer uma criatura realmente derrotada (nao estabilizada); Sabedoria e d20 fisico. Uma tentativa por corpo, inclusive em falha. A parte estraga em 24 horas.",13,Color.DKGRAY));
        TextView anatomyHelp=text("",13,Color.DKGRAY);form.addView(anatomyHelp);
        Runnable syncAnatomy=()->{
            JSONObject chosen=bodies.optJSONObject(targets.getSelectedItemPosition());
            JSONObject anatomy=chosen==null?null:chosen.optJSONObject("anatomy");
            anatomyHelp.setText(anatomy==null?"Corte nao registrado":
                anatomy.optString("cut")+"\\n"+anatomy.optString("warning"));
        };
        targets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){syncAnatomy.run();}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        syncAnatomy.run();
        new AlertDialog.Builder(activity).setTitle("Examinar criatura abatida").setView(form)
            .setNegativeButton("Cancelar",null).setPositiveButton("ROLAR D20",(dialog,which)->{
                JSONObject target=bodies.optJSONObject(targets.getSelectedItemPosition());
                if(target==null){fail(new IllegalStateException("Criatura ausente"));return;}
                Dice3DOverlay.roll(activity,activity.getSharedPreferences("settings",Activity.MODE_PRIVATE),
                    "d20","Normal","Wilem",book.ownerId(),new Dice3DOverlay.Callback(){
                        private boolean used=false;
                        public void onResult(Dice3DOverlay.RollResult rolled){
                            if(used)return;used=true;
                            try{JSONObject result=book.harvestWithPhysicalRoll(target.optLong("combatant_id"),
                                target.optString("ingredient_id"),rolled,WilemCookbookEngine.newReceipt());
                                Toast.makeText(activity,result.optBoolean("success")?
                                    "Parte aproveitavel adicionada à ficha de Wilem":
                                    "Falha de extração: corpo examinado, sem alimento seguro",Toast.LENGTH_LONG).show();
                                changed();
                            }catch(Exception e){fail(e);}
                        }
                        public void onError(String message){fail(new IllegalStateException(message));}
                        public void onCancel(){}
                    });
            }).show();
    }
    private void acquireDialog(){
        JSONArray all=WilemCookbookCatalog.ingredients();
        ArrayList<String> ids=new ArrayList<String>(),names=new ArrayList<String>();
        for(int i=0;i<all.length();i++){
            JSONObject ing=all.optJSONObject(i);
            if(ing==null||ing.optInt("tier",0)>0)continue;
            ids.add(ing.optString("id"));
            names.add(ing.optString("name")+" — "+ing.optInt("suggested_price_cp")+" PC/un.");
        }
        LinearLayout form=vertical();
        Spinner ingredient=new Spinner(activity);
        ingredient.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,names));
        form.addView(ingredient);
        EditText quantity=new EditText(activity);quantity.setInputType(2);
        quantity.setHint("Quantidade (1 a 12, sujeita a estoque)");form.addView(quantity);
        form.addView(text("Compra real: requer mercado/feira/loja ou mercador registrado na cena. O sistema debita moedas do inventario de Wilem e registra a transacao.",13,Color.DKGRAY));
        new AlertDialog.Builder(activity).setTitle("Comprar ingrediente")
            .setView(form).setNegativeButton("Voltar",null).setPositiveButton("COMPRAR",(d,w)->{
                try{
                    book.acquire(ids.get(ingredient.getSelectedItemPosition()),
                        Integer.parseInt(quantity.getText().toString().trim()),
                        "mercado_pago",true,WilemCookbookEngine.newReceipt());changed();
                }catch(Exception e){fail(e);}
            }).show();
    }
    private void forageDialog(JSONArray candidates){
        String[] descriptions=new String[candidates.length()];
        for(int i=0;i<candidates.length();i++){
            JSONObject x=candidates.optJSONObject(i);
            descriptions[i]=x.optString("name")+" (CD "+x.optInt("dc")+")";
        }
        LinearLayout form=vertical();Spinner item=new Spinner(activity);
        item.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,descriptions));
        form.addView(item);
        form.addView(text("Recurso do ambiente atual; uma unica tentativa por ingrediente/local/dia. A Sabedoria de Wilem modifica o d20 fisico. Uma falha nao fornece ingredientes.",13,Color.DKGRAY));
        new AlertDialog.Builder(activity).setTitle("Coleta ecologica")
            .setView(form).setNegativeButton("Voltar",null)
            .setPositiveButton("ROLAR D20",(d,w)->{
                JSONObject chosen=candidates.optJSONObject(item.getSelectedItemPosition());
                if(chosen==null){fail(new IllegalStateException("Ingrediente indisponivel"));return;}
                Dice3DOverlay.roll(activity,activity.getSharedPreferences("settings",Activity.MODE_PRIVATE),
                    "d20","Normal","Wilem",book.ownerId(),new Dice3DOverlay.Callback(){
                        private boolean done=false;
                        public void onResult(Dice3DOverlay.RollResult rolled){
                            if(done)return;done=true;
                            try{
                                JSONObject found=book.forageWithPhysicalRoll(chosen.optString("ingredient_id"),
                                    rolled,WilemCookbookEngine.newReceipt());
                                Toast.makeText(activity,found.optBoolean("success")?
                                    "Coleta: "+found.optInt("quantity")+" unidade(s)":
                                    "Coleta sem sucesso; tentativa do dia registrada",Toast.LENGTH_LONG).show();
                                changed();
                            }catch(Exception e){fail(e);}
                        }
                        public void onError(String message){fail(new IllegalStateException(message));}
                        public void onCancel(){}
                    });
            }).show();
    }
    private void claimRewardDialog(JSONArray available){
        String[] descriptions=new String[available.length()];
        for(int i=0;i<available.length();i++){
            JSONObject x=available.optJSONObject(i);
            JSONObject ing=WilemCookbookCatalog.ingredientById(x.optString("ingredient_id"));
            descriptions[i]=x.optString("event_title")+" — "+
                ing.optString("name")+" x"+x.optInt("quantity");
        }
        LinearLayout form=vertical();Spinner event=new Spinner(activity);
        event.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,descriptions));
        form.addView(event);
        form.addView(text("Somente eventos com consequencia mecanica aplicada e recompensa culinaria estruturada. Cada evento so concede esta recompensa uma vez.",13,Color.DKGRAY));
        new AlertDialog.Builder(activity).setTitle("Recompensa confirmada no mundo")
            .setView(form).setNegativeButton("Voltar",null)
            .setPositiveButton("RESGATAR",(d,w)->{
                try{
                    JSONObject selected=available.optJSONObject(event.getSelectedItemPosition());
                    if(selected==null)throw new IllegalStateException("Evento nao encontrado");
                    book.claimWorldReward(selected.optLong("event_id"),WilemCookbookEngine.newReceipt());
                    changed();
                }catch(Exception e){fail(e);}
            }).show();
    }
    private void showRumors(LinearLayout body,JSONObject st,boolean enabled){
        body.addView(text("BOATOS CULINARIOS",19,GOLD));
        body.addView(text("Cada boato tem lugar, fonte e nivel de certeza. Uma suspeita nao e verdade revelada.",13,MUTED));
        JSONArray arr=st.optJSONArray("rumors");
        for(int i=0;arr!=null&&i<arr.length();i++){
            JSONObject x=arr.optJSONObject(i);if(x==null)continue;LinearLayout p=panel();
            JSONObject r=WilemCookbookCatalog.recipeById(x.optString("recipe_id"));
            p.addView(text(r.optString("name",x.optString("recipe_id"))+" · "+x.optString("status"),16,GOLD));
            p.addView(text("Lugar: "+x.optString("location")+"\nFonte: "+x.optString("source")+"\n"+x.optString("text"),13,TEXT));
            if(enabled&&"ouvido".equals(x.optString("status"))){
                Button learn=btn("REGISTRAR PESQUISA CONFIRMADA");learn.setOnClickListener(v->discoveryDialog(x.optString("recipe_id")));p.addView(learn);
            }
            body.addView(p);
        }
        Button add=btn("+ ANOTAR BOATO OUVIDO");add.setEnabled(enabled);add.setOnClickListener(v->rumorDialog(st));body.addView(add);
    }
    private void rumorDialog(JSONObject st){
        JSONArray ids=st.optJSONArray("known");JSONArray hints=st.optJSONArray("rumors");ArrayList<String> options=new ArrayList<String>(),titles=new ArrayList<String>();
        if(ids!=null)for(int i=0;i<ids.length();i++)addOption(options,titles,ids.optString(i));
        if(hints!=null)for(int i=0;i<hints.length();i++){JSONObject x=hints.optJSONObject(i);if(x!=null)addOption(options,titles,x.optString("recipe_id"));}
        if(options.isEmpty())return;
        LinearLayout f=vertical();Spinner sp=new Spinner(activity);sp.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,titles));f.addView(sp);
        EditText informant=new EditText(activity);informant.setHint("Quem contou?");f.addView(informant);
        EditText note=new EditText(activity);note.setHint("O que Wilem ouviu?");note.setMinLines(3);f.addView(note);
        new AlertDialog.Builder(activity).setTitle("Boato em "+db.getSceneState().optString("location")).setView(f)
            .setNegativeButton("Voltar",null).setPositiveButton("REGISTRAR",(d,w)->{
                try{book.recordRumor(options.get(sp.getSelectedItemPosition()),informant.getText().toString(),
                    note.getText().toString(),WilemCookbookEngine.newReceipt());changed();}catch(Exception e){fail(e);}
            }).show();
    }
    private void addOption(List<String> keys,List<String> names,String id){
        if(keys.contains(id)||id.isEmpty())return;JSONObject r=WilemCookbookCatalog.recipeById(id);
        if(r.length()==0)return;keys.add(id);names.add(r.optString("name"));
    }
    private void discoveryDialog(String id){
        EditText reason=new EditText(activity);reason.setHint("Descreva a experiencia ou pesquisa realizada");reason.setMinLines(3);
        new AlertDialog.Builder(activity).setTitle("Confirmar descoberta").setView(reason)
            .setNegativeButton("Voltar",null).setPositiveButton("CONFIRMAR",(d,w)->{
                try{book.confirmDiscovery(id,reason.getText().toString(),WilemCookbookEngine.newReceipt());changed();}
                catch(Exception e){fail(e);}
            }).show();
    }
    private void showMeals(LinearLayout body,JSONObject st,boolean enabled){
        body.addView(text("REFEICOES PRONTAS E MEMORIAS",19,GOLD));
        body.addView(text("Cada prato sacia a fome e pode gerar conversas, acolhimento e lembrancas; abrigo e fogo sao necessarios para protecao contra o frio.",13,MUTED));
        JSONArray cooked=st.optJSONArray("cooked");
        for(int i=0;cooked!=null&&i<cooked.length();i++){
            JSONObject x=cooked.optJSONObject(i);if(x==null||x.optInt("remaining")<1||"isca".equals(x.optString("kind")))continue;
            JSONObject recipe=WilemCookbookCatalog.recipeById(x.optString("recipe_id"));LinearLayout p=panel();
            p.addView(text(recipe.optString("name")+" · "+x.optInt("remaining")+" porcoes",16,GOLD));
            Button serve=btn("SERVIR E REGISTRAR MOMENTO");serve.setEnabled(enabled);serve.setOnClickListener(v->serveDialog(x));p.addView(serve);body.addView(p);
        }
        JSONArray mem=st.optJSONArray("meals");for(int i=Math.max(0,(mem==null?0:mem.length())-12);mem!=null&&i<mem.length();i++){
            JSONObject m=mem.optJSONObject(i);if(m!=null)body.addView(text("• "+m.optString("actor")+" — "+m.optString("conversation_note","Refeicao compartilhada"),13,TEXT));
        }
    }
    private void serveDialog(JSONObject batch){
        List<JSONObject> heroes=new ArrayList<JSONObject>();for(JSONObject row:db.listPartyMemberships("active",80)){
            if("hero".equalsIgnoreCase(row.optString("actor_kind"))){JSONObject h=db.getCharacter(row.optLong("character_id"));if(h.length()>0)heroes.add(h);}
        }
        String[] names=new String[heroes.size()+1];names[0]="Convidado (nao controlado)";for(int i=0;i<heroes.size();i++)names[i+1]=heroes.get(i).optString("name");
        LinearLayout f=vertical();Spinner who=new Spinner(activity);who.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,names));f.addView(who);
        EditText guest=new EditText(activity);guest.setHint("Nome do convidado, se houver");f.addView(guest);
        EditText story=new EditText(activity);story.setHint("Conversa ou historia que aconteceu a mesa");story.setMinLines(2);f.addView(story);
        CheckBox shelter=new CheckBox(activity);shelter.setText("Ha fogo e abrigo adequados");f.addView(shelter);
        boolean risk=batch.optString("result").contains("risco");
        if(risk)f.addView(text("Preparo de risco: selecione um heroi da companhia. O aplicativo rola o d20 fisico e aplica o bonus de salvaguarda de Constituicao da ficha; convidados sem ficha mecanica nao recebem efeito automaticamente.",13,Color.DKGRAY));
        new AlertDialog.Builder(activity).setTitle("Servir prato").setView(f).setNegativeButton("Voltar",null)
            .setPositiveButton("SERVIR",(d,w)->{try{
                int choice=who.getSelectedItemPosition();long target=choice==0?0:heroes.get(choice-1).optLong("id");
                final String prepared=batch.optString("id"),guestName=guest.getText().toString();
                final boolean covered=shelter.isChecked();
                final String note=story.getText().toString();
                if(!risk){
                    book.serve(prepared,target,guestName,covered,note,WilemCookbookEngine.newReceipt());changed();
                }else{
                    if(target<=0)throw new IllegalStateException("Selecione um heroi para resolver a salvaguarda");
                    String actor=db.getCharacter(target).optString("name","Heroi");
                    Dice3DOverlay.roll(activity,activity.getSharedPreferences("settings",Activity.MODE_PRIVATE),
                        "d20","Normal",actor,target,new Dice3DOverlay.Callback(){
                            private boolean completed=false;
                            public void onResult(Dice3DOverlay.RollResult rolled){
                                if(completed)return;completed=true;
                                try{book.serveWithPhysicalSave(prepared,target,guestName,covered,note,
                                    rolled,WilemCookbookEngine.newReceipt());changed();}
                                catch(Exception e){fail(e);}
                            }
                            public void onError(String error){fail(new IllegalStateException(error));}
                            public void onCancel(){}
                        });
                }
            }catch(Exception e){fail(e);}}).show();
    }
    private void showBaits(LinearLayout body,JSONObject st,boolean enabled){
        body.addView(text("ISCAS DE AVENTURA",19,GOLD));
        body.addView(text("Preparar: Receitas. Posicionar nao significa que um monstro comeu. Sono, vertigem e lentidao exigem ingredientes magicos, alvo, ingestao e salvaguarda explicitos.",13,MUTED));
        JSONArray ready=st.optJSONArray("cooked");
        for(int i=0;ready!=null&&i<ready.length();i++){
            JSONObject x=ready.optJSONObject(i);if(x==null||!"isca".equals(x.optString("kind"))||x.optInt("remaining")<1)continue;
            JSONObject recipe=WilemCookbookCatalog.recipeById(x.optString("recipe_id"));LinearLayout p=panel();
            p.addView(text(recipe.optString("name")+" · "+x.optInt("remaining")+" porcoes",16,GOLD));
            Button place=btn("COLOCAR ISCA NO CENARIO");place.setEnabled(enabled);place.setOnClickListener(v->baitDialog(x));p.addView(place);body.addView(p);
        }
        JSONArray baits=st.optJSONArray("baits");
        for(int i=0;baits!=null&&i<baits.length();i++){JSONObject x=baits.optJSONObject(i);if(x!=null){
            LinearLayout p=panel();p.addView(text(x.optString("position")+" — "+x.optString("status"),14,TEXT));
            if("armada".equals(x.optString("status"))&&enabled){Button resolve=btn("RESOLVER CONSUMO (COM SALVAGUARDA)");resolve.setOnClickListener(v->resolveBaitDialog(x));p.addView(resolve);}body.addView(p);
        }}
    }
    private void baitDialog(JSONObject batch){
        EditText p=new EditText(activity);p.setHint("Posicao concreta no cenario");p.setMinLines(2);
        new AlertDialog.Builder(activity).setTitle("Posicionar isca").setView(p)
            .setNegativeButton("Voltar",null).setPositiveButton("COLOCAR",(d,w)->{
                try{book.setBait(batch.optString("id"),p.getText().toString(),WilemCookbookEngine.newReceipt());changed();}catch(Exception e){fail(e);}
            }).show();
    }
    private void resolveBaitDialog(JSONObject bait){
        List<JSONObject> monsters=new ArrayList<JSONObject>();
        for(JSONObject c:db.listCombatants())if(c.optInt("is_player",0)==0&&c.optInt("hp",0)>0)monsters.add(c);
        if(monsters.isEmpty()){Toast.makeText(activity,"Nenhuma criatura viva registrada no combate",Toast.LENGTH_LONG).show();return;}
        JSONObject recipe=WilemCookbookCatalog.recipeById(bait.optString("recipe_id"));
        boolean requiresSave=recipe.optBoolean("requires_save",false);
        String[] names=new String[monsters.size()];
        for(int i=0;i<names.length;i++)names[i]=monsters.get(i).optString("name");
        LinearLayout f=vertical();Spinner target=new Spinner(activity);
        target.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,names));f.addView(target);
        EditText bonus=new EditText(activity);bonus.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        bonus.setHint("Bonus de salvaguarda CON do statblock (ex.: +2, -1)");
        if(requiresSave)f.addView(bonus);
        CheckBox confirmedBonus=new CheckBox(activity);
        confirmedBonus.setText("Conferi o bonus no statblock desta criatura");
        if(requiresSave)f.addView(confirmedBonus);
        CheckBox consumed=new CheckBox(activity);
        consumed.setText("O Mestre confirmou ingestao real e dieta compativel");f.addView(consumed);
        CheckBox immune=new CheckBox(activity);immune.setText("Criatura imune a este efeito");f.addView(immune);
        new AlertDialog.Builder(activity).setTitle("Resolver isca").setView(f)
            .setNegativeButton("Voltar",null).setPositiveButton(requiresSave?"ROLAR SALVAGUARDA":"REGISTRAR CONSUMO",(dialog,which)->{
                try{
                    if(!consumed.isChecked())throw new IllegalStateException("Consumo e dieta precisam ser confirmados");
                    JSONObject chosen=monsters.get(target.getSelectedItemPosition());
                    final long combatant=chosen.optLong("id");
                    final boolean immuneNow=immune.isChecked();
                    if(!requiresSave){
                        book.resolveBait(bait.optString("id"),combatant,true,true,immuneNow,0,
                            WilemCookbookEngine.newReceipt());changed();
                        return;
                    }
                    if(!confirmedBonus.isChecked())throw new IllegalStateException("Confirme o bonus do statblock");
                    final int saveBonus=Integer.parseInt(bonus.getText().toString().trim());
                    Dice3DOverlay.roll(activity,activity.getSharedPreferences("settings",Activity.MODE_PRIVATE),
                        "d20","Normal",chosen.optString("name"),0,new Dice3DOverlay.Callback(){
                            private boolean completed=false;
                            public void onResult(Dice3DOverlay.RollResult rolled){
                                if(completed)return;completed=true;
                                try{book.resolveBaitWithPhysicalSave(bait.optString("id"),combatant,true,true,
                                    immuneNow,rolled,saveBonus,true,WilemCookbookEngine.newReceipt());changed();}
                                catch(Exception e){fail(e);}
                            }
                            public void onError(String message){fail(new IllegalStateException(message));}
                            public void onCancel(){}
                        });
                }catch(Exception e){fail(e);}
            }).show();
    }
    private void noteDialog(){
        LinearLayout f=vertical();EditText title=new EditText(activity);title.setHint("Assunto");f.addView(title);
        EditText body=new EditText(activity);body.setHint("Observacao, historia ou desenho descrito por Wilem");body.setMinLines(4);f.addView(body);
        new AlertDialog.Builder(activity).setTitle("Nova anotacao").setView(f).setNegativeButton("Voltar",null)
            .setPositiveButton("SALVAR",(d,w)->{
                try{book.addNote(title.getText().toString(),body.getText().toString(),
                    WilemCookbookEngine.newReceipt());changed();}catch(Exception e){fail(e);}
            }).show();
    }
    private void showNotes(LinearLayout body,JSONObject st){
        body.addView(text("ANOTACOES E DESCOBERTAS",19,GOLD));
        JSONArray notes=st.optJSONArray("notes");for(int i=0;notes!=null&&i<notes.length();i++){
            JSONObject x=notes.optJSONObject(i);if(x!=null){LinearLayout p=panel();p.addView(text(x.optString("title"),16,GOLD));p.addView(text(x.optString("text"),13,TEXT));body.addView(p);}
        }
        JSONArray experiments=st.optJSONArray("experiments");
        Button write=btn("+ ESCREVER NO CADERNO");write.setEnabled(book.canCook());
        write.setOnClickListener(v->noteDialog());body.addView(write);
        body.addView(text("Tentativas: "+(experiments==null?0:experiments.length()),14,MUTED));
        for(int i=Math.max(0,(experiments==null?0:experiments.length())-10);experiments!=null&&i<experiments.length();i++){
            JSONObject x=experiments.optJSONObject(i);if(x!=null)body.addView(text(WilemCookbookCatalog.recipeById(x.optString("recipe_id")).optString("name")+" • "+x.optString("result"),13,TEXT));
        }
    }
}
