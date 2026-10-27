package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

/** Form editor for optional structured traits, never changes actions or D&D bonuses. */
public final class BraseiroPersonalityEditor {
    public interface Saved { void save(JSONObject profile); }
    private BraseiroPersonalityEditor() {}
    private static TextView label(Activity ctx, String message) {
        TextView view = new TextView(ctx); view.setText(message); view.setTextSize(16); view.setPadding(6, 10, 6, 6); return view;
    }
    private static EditText entry(Activity ctx, String hint, String value) {
        EditText v = new EditText(ctx); v.setHint(hint); v.setText(value);
        v.setTextSize(15); v.setMinLines(2); v.setMaxLines(5);
        v.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        return v;
    }
    public static void show(final Activity activity, final JSONObject original, final Saved onSaved) {
        final JSONObject profile;
        try { profile = BraseiroPersonalityProfile.checked(original); }
        catch(Exception e) { new AlertDialog.Builder(activity).setMessage("Importe primeiro um perfil compatível: " + e.getMessage()).setPositiveButton("OK", null).show(); return; }
        final JSONArray traits = profile.optJSONArray("traits");
        if(traits == null || traits.length() == 0) {
            new AlertDialog.Builder(activity).setMessage("Não há traços neste perfil. Importe um arquivo de personalidade JSON.").setPositiveButton("OK", null).show();return;
        }
        ScrollView scroll = new ScrollView(activity);
        LinearLayout form = new LinearLayout(activity); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(18, 12, 18, 18); scroll.addView(form);
        form.addView(label(activity, "Edite a descrição, os gatilhos e a ativação. As escolhas do personagem permanecem sempre com o jogador."));
        final ArrayList<EditText> descriptions = new ArrayList<>();
        final ArrayList<EditText> triggers = new ArrayList<>();
        final ArrayList<CheckBox> checks = new ArrayList<>();
        for(int i=0;i<traits.length();i++) {
            JSONObject t=traits.optJSONObject(i); if(t==null)continue;
            form.addView(label(activity,(i+1)+". "+t.optString("label",t.optString("id"))));
            CheckBox on = new CheckBox(activity); on.setText("Ativo"); on.setChecked(t.optBoolean("enabled",true)); form.addView(on);checks.add(on);
            EditText v=entry(activity,"Traço",t.optString("value")); form.addView(v); descriptions.add(v);
            JSONArray ts=t.optJSONArray("triggers");StringBuilder joined=new StringBuilder();
            if(ts!=null)for(int k=0;k<ts.length();k++){if(k>0)joined.append(", ");joined.append(ts.optString(k));}
            EditText words=entry(activity,"Gatilhos, separados por vírgulas",joined.toString());form.addView(words);triggers.add(words);
            View divider=new View(activity);divider.setBackgroundColor(0x44666666);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,1);lp.setMargins(0,12,0,12);form.addView(divider,lp);
        }
        final AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Personalidade viva — "+traits.length+" campos")
            .setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar traços",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                for(int i=0;i<traits.length();i++){
                    JSONObject t=traits.getJSONObject(i);String desc=descriptions.get(i).getText().toString().trim();
                    t.put("value",desc);t.put("enabled",checks.get(i).isChecked());
                    JSONArray keys=new JSONArray();
                    for(String w:triggers.get(i).getText().toString().split(",")){if(!w.trim().isEmpty())keys.put(w.trim());}
                    t.put("triggers",keys);
                }
                JSONObject checked=BraseiroPersonalityProfile.checked(profile);
                onSaved.save(checked);dialog.dismiss();
            }catch(Exception error){new AlertDialog.Builder(activity).setMessage("Perfil inválido: "+error.getMessage()).setPositiveButton("Corrigir",null).show();}
        }));
        dialog.show();
    }
}
