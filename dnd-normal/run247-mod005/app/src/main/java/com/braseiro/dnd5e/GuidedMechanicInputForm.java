package com.braseiro.dnd5e;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
/** Explicit player inputs, no default roll, hit or authorization silently accepted. */
public final class GuidedMechanicInputForm {
  private interface Value { Object read() throws Exception; }
  private final ScrollView scroll;
  private final Value reader;
  public GuidedMechanicInputForm(Context context,JSONObject shape){
    LinearLayout content=column(context);
    TextView help=new TextView(context);
    help.setText("Informe apenas resultados e decisões já confirmados. Nenhuma rolagem, gasto ou sucesso é presumido.");
    content.addView(help);
    reader=objectFields(context,content,shape==null?new JSONObject():shape);
    scroll=new ScrollView(context);scroll.addView(content);
  }
  public View view(){return scroll;}
  public JSONObject values() throws Exception{return (JSONObject)reader.read();}
  private static LinearLayout column(Context context){LinearLayout box=new LinearLayout(context);box.setOrientation(LinearLayout.VERTICAL);int p=(int)(12f*context.getResources().getDisplayMetrics().density);box.setPadding(p,p,p,p);return box;}
  private static Value objectFields(Context context,LinearLayout parent,JSONObject shape){
    final ArrayList<String> keys=new ArrayList<String>();final ArrayList<Value> readers=new ArrayList<Value>();
    Iterator<String> it=shape.keys();while(it.hasNext()){
      String key=it.next();keys.add(key);readers.add(field(context,parent,key,shape.opt(key)));
    }
    return new Value(){public Object read() throws Exception{JSONObject o=new JSONObject();
      for(int i=0;i<keys.size();i++){Object v=readers.get(i).read();if(v!=null)o.put(keys.get(i),v);}return o;
    }};
  }
  private static Value field(final Context ctx,LinearLayout parent,String key,Object example){
    TextView label=new TextView(ctx);label.setText(key.replace('_',' '));parent.addView(label);
    if(example instanceof Boolean){
      final Spinner choice=new Spinner(ctx);
      ArrayAdapter<String> adapter=new ArrayAdapter<String>(ctx,android.R.layout.simple_spinner_dropdown_item,
        new String[]{"Selecione a confirmação", "Sim — confirmado", "Não — confirmado"});
      choice.setAdapter(adapter);choice.setSelection(0);parent.addView(choice);
      return new Value(){public Object read(){int selected=choice.getSelectedItemPosition();
        return selected==0?null:Boolean.valueOf(selected==1);
      }};
    }
    if(example instanceof JSONObject){LinearLayout child=column(ctx);parent.addView(child);final Value inner=objectFields(ctx,child,(JSONObject)example);
      return new Value(){public Object read()throws Exception{JSONObject o=(JSONObject)inner.read();return o.length()==0?null:o;}};}
    if(example instanceof JSONArray){final JSONArray sample=(JSONArray)example;final LinearLayout entries=column(ctx);parent.addView(entries);
      final ArrayList<Value> rows=new ArrayList<Value>();
      Button more=new Button(ctx);more.setText("+ ADICIONAR ITEM");
      final Object sampleEntry=sample.length()==0?"":sample.opt(0);
      more.setOnClickListener(new View.OnClickListener(){public void onClick(View v){rows.add(field(ctx,entries,"Item "+(rows.size()+1),sampleEntry));}});
      parent.addView(more);
      rows.add(field(ctx,entries,"Item 1",sampleEntry));
      return new Value(){public Object read()throws Exception{JSONArray a=new JSONArray();for(Value row:rows){Object item=row.read();if(item!=null)a.put(item);}return a.length()==0?null:a;}};
    }
    final EditText edit=new EditText(ctx);edit.setSingleLine(true);edit.setHint(example==null?"Informe o valor":String.valueOf(example));
    if(example instanceof Number)edit.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED|InputType.TYPE_NUMBER_FLAG_DECIMAL);
    parent.addView(edit);
    final boolean numeric=example instanceof Number,whole=example instanceof Integer||example instanceof Long;
    return new Value(){public Object read(){String raw=edit.getText().toString().trim();if(raw.length()==0)return null;
      if(!numeric)return raw;
      try{if(whole)return Integer.valueOf(raw);return Double.valueOf(raw);}catch(NumberFormatException e){throw new IllegalArgumentException("Número inválido: "+raw);}
    }};
  }
}
