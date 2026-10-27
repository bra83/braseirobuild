package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;

/** Voice controls are independent of narration text and D&D mechanics. */
final class GeminiVoiceUi {
    static final String[] STUDIO={
        "Zephyr","Puck","Charon","Kore","Fenrir","Leda","Orus","Aoede",
        "Callirrhoe","Autonoe","Enceladus","Iapetus","Umbriel","Algieba",
        "Despina","Erinome","Algenib","Rasalgethi","Laomedeia","Achernar",
        "Alnilam","Schedar","Gacrux","Pulcherrima","Achird",
        "Zubenelgenubi","Vindemiatrix","Sadachbia","Sadaltager","Sulafat"
    };
    static final class Fields {
        final EditText profile,style;
        Fields(EditText p,EditText s){profile=p;style=s;}
    }
    private static EditText field(Activity activity,String value,String hint){
        EditText e=new EditText(activity);e.setText(value);e.setHint(hint);e.setTextSize(15f);
        e.setMinLines(2);e.setMaxLines(4);
        return e;
    }
    private static Button button(Activity a,String text,LinearLayout target,Runnable action){
        Button b=new Button(a);b.setText(text);b.setTextSize(12f);b.setOnClickListener(v->action.run());
        target.addView(b);return b;
    }
    static Fields mount(Activity activity, LinearLayout target, GeminiService service, SharedPreferences prefs,
                        EditText model,EditText voice,EditText primaryKey,EditText fallbackKey){
        TextView title=new TextView(activity);title.setText("GEMINI 3.8 — PERSONALIZAÇÃO VOCAL");title.setTextSize(16);target.addView(title);
        EditText profile=field(activity,prefs.getString("tts_vocal_profile","Voz brasileira, dicção clara e tom natural"),"Perfil vocal");
        EditText style=field(activity,prefs.getString("tts_vocal_style","Expressivo, com ritmo natural, sem pausas artificiais"),"Estilo vocal");
        target.addView(profile);target.addView(style);
        button(activity,"SELECIONAR ENTRE 30 VOZES DE ESTÚDIO",target,()->new AlertDialog.Builder(activity)
            .setTitle("Vozes de estúdio Gemini 3.8").setItems(STUDIO,(d,i)->voice.setText(STUDIO[i]))
            .setNegativeButton("Fechar",null).show());
        button(activity,"CATÁLOGO ESTENDIDO DE VOZES (GEMINI)",target,()->{
            String key=primaryKey.getText().toString().trim();
            if(key.length()<20){error(activity,"Informe a chave Gemini para consultar o catálogo.");return;}
            service.listExtendedVoices(key,new GeminiService.VoiceCatalogCallback(){
                public void onError(String message){activity.runOnUiThread(()->error(activity,"Catálogo indisponível: "+message));}
                public void onSuccess(JSONArray voices){activity.runOnUiThread(()->{
                    ArrayList<String> names=new ArrayList<>(),ids=new ArrayList<>();
                    for(int i=0;i<voices.length();i++){
                        JSONObject o=voices.optJSONObject(i);if(o==null)continue;
                        String id=o.optString("id",o.optString("name",""));
                        if(id.startsWith("voices/"))id=id.substring(7);
                        if(id.isEmpty())continue;
                        ids.add(id);names.add(o.optString("displayName",o.optString("display_name",id))+" • "+id);
                    }
                    if(ids.isEmpty()){error(activity,"A API não retornou vozes autorizadas para esta chave.");return;}
                    LinearLayout pane=new LinearLayout(activity);pane.setOrientation(LinearLayout.VERTICAL);
                    EditText filter=field(activity,"","Filtrar nome, sotaque ou ID");filter.setSingleLine(true);pane.addView(filter);
                    ListView list=new ListView(activity);
                    ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_list_item_1,names);
                    list.setAdapter(adapter);pane.addView(list,new LinearLayout.LayoutParams(-1,(int)(activity.getResources().getDisplayMetrics().density*390)));
                    AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Biblioteca estendida: "+ids.size()+" vozes")
                        .setView(pane).setNegativeButton("Fechar",null).create();
                    filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int n,int aft){}
                        public void onTextChanged(CharSequence x,int st,int before,int count){adapter.getFilter().filter(x);}
                        public void afterTextChanged(Editable e){}
                    });
                    list.setOnItemClickListener((parent,view,pos,id)->{
                        int original=names.indexOf(adapter.getItem(pos));
                        if(original>=0){voice.setText(ids.get(original));dialog.dismiss();}
                    });
                    dialog.show();
                });}
            });
        });
        button(activity,"PRÉ-OUVIR CONFIGURAÇÃO DA VOZ",target,()->{
            String key=primaryKey.getText().toString().trim();
            if(key.length()<20){error(activity,"Informe a chave Gemini antes da prévia.");return;}
            // The actual metadata is read by GeminiService from preferences.
            prefs.edit().putString("tts_vocal_profile",profile.getText().toString())
                .putString("tts_vocal_style",style.getText().toString()).apply();
            service.synthesizeSpeech(key,fallbackKey.getText().toString().trim(),
                model.getText().toString().trim(),voice.getText().toString().trim(),
                "A noite cai sobre Faerûn. Nossa jornada está começando.",new GeminiService.FileCallback(){
                public void onError(String message){activity.runOnUiThread(()->error(activity,"Prévia de voz: "+message));}
                public void onSuccess(File file,String mime){activity.runOnUiThread(()->{
                    try{
                        MediaPlayer player=new MediaPlayer();player.setDataSource(file.getAbsolutePath());
                        player.setOnCompletionListener(mp->{mp.release();if(file.exists())file.delete();});
                        player.prepare();player.start();
                    }catch(Exception e){error(activity,"Não foi possível tocar a prévia: "+e.getMessage());}
                });}
            });
        });
        TextView hint=new TextView(activity);
        hint.setText("Perfil e estilo são metadados, nunca texto narrado. As vozes disponíveis dependem das permissões e da cota da sua chave Gemini.");
        hint.setTextSize(12f);target.addView(hint);
        return new Fields(profile,style);
    }
    private static void error(Activity a,String s){new AlertDialog.Builder(a).setMessage(s).setPositiveButton("OK",null).show();}
}
