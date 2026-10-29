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
        Button acquire=btn("REGISTRAR INGREDIENTE OBTIDO");acquire.setEnabled(enabled);acquire.setOnClickListener(v->acquireDialog());body.addView(acquire);
        body.addView(text("Compras debitam o saldo em PO da ficha 2024 (convertido para PC); sem saldo, a compra e bloqueada. Partes de monstros e componentes arcanos exigem evento/colheita integrado, nunca registro livre.",12,MUTED));
    }
    private void acquireDialog(){
        JSONArray all=WilemCookbookCatalog.ingredients();String[] labels=new String[all.length()],ids=new String[all.length()];
        for(int i=0;i<all.length();i++){JSONObject x=all.optJSONObject(i);labels[i]=x.optString("name");ids[i]=x.optString("id");}
        LinearLayout form=vertical();Spinner ingredient=new Spinner(activity);ingredient.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,labels));form.addView(ingredient);
        EditText quantity=new EditText(activity);quantity.setInputType(2);quantity.setHint("Quantidade (1 a 30)");form.addView(quantity);
        String[] sources={"coleta_confirmada","mercado_pago","recompensa_confirmada"};
        Spinner source=new Spinner(activity);source.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,sources));form.addView(source);
        CheckBox check=new CheckBox(activity);check.setText("Confirmo uma fonte real da cena. Compras debitam automaticamente as moedas da ficha; nao confirmar pagamentos feitos fora do sistema.");form.addView(check);
        new AlertDialog.Builder(activity).setTitle("Registrar ingrediente").setView(form).setNegativeButton("Voltar",null)
            .setPositiveButton("ADICIONAR",(d,w)->{
                try{if(!check.isChecked())throw new IllegalStateException("Confirme a aquisicao antes de registrar");
                    book.acquire(ids[ingredient.getSelectedItemPosition()],Integer.parseInt(quantity.getText().toString().trim()),
                        sources[source.getSelectedItemPosition()],true,WilemCookbookEngine.newReceipt());changed();
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
        EditText save=new EditText(activity);save.setInputType(2);
        save.setHint("Total da salvaguarda de Constituicao do consumidor");
        if(risk)f.addView(save);
        new AlertDialog.Builder(activity).setTitle("Servir prato").setView(f).setNegativeButton("Voltar",null)
            .setPositiveButton("SERVIR",(d,w)->{try{
                int choice=who.getSelectedItemPosition();long target=choice==0?0:heroes.get(choice-1).optLong("id");
                int conTotal=risk?Integer.parseInt(save.getText().toString().trim()):-1;
                book.serveWithSave(batch.optString("id"),target,guest.getText().toString(),shelter.isChecked(),
                    story.getText().toString(),conTotal,WilemCookbookEngine.newReceipt());changed();
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
        List<JSONObject> monsters=new ArrayList<JSONObject>();for(JSONObject c:db.listCombatants())
            if(c.optInt("is_player",0)==0)monsters.add(c);
        if(monsters.isEmpty()){Toast.makeText(activity,"Nenhum monstro presente no encontro",Toast.LENGTH_LONG).show();return;}
        String[] names=new String[monsters.size()];for(int i=0;i<names.length;i++)names[i]=monsters.get(i).optString("name");
        LinearLayout f=vertical();Spinner target=new Spinner(activity);target.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,names));f.addView(target);
        EditText save=new EditText(activity);save.setInputType(2);save.setHint("Total da salvaguarda CON do monstro");f.addView(save);
        CheckBox consumed=new CheckBox(activity);consumed.setText("Mestre confirmou que comeu e dieta compativel");f.addView(consumed);
        CheckBox immune=new CheckBox(activity);immune.setText("Monstro e imune a este efeito");f.addView(immune);
        new AlertDialog.Builder(activity).setTitle("Resolucao de isca").setView(f)
            .setNegativeButton("Voltar",null).setPositiveButton("REGISTRAR",(d,w)->{try{
                if(!consumed.isChecked())throw new IllegalStateException("Consumo e dieta devem ser confirmados");
                book.resolveBait(bait.optString("id"),monsters.get(target.getSelectedItemPosition()).optLong("id"),
                    true,true,immune.isChecked(),Integer.parseInt(save.getText().toString()),WilemCookbookEngine.newReceipt());changed();
            }catch(Exception e){fail(e);}}).show();
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
