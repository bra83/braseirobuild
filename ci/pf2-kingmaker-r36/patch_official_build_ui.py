#!/usr/bin/env python3
"""R36: send the actual Kingdom CONSTRUIR UI through official Build Structure engine.

Replaces the R35 settlement form which directly inserted manually chosen costs,
effect JSON and 0-RP buildings using addStructure. Requires the R36 73-entry
catalog, prepaid construction resolver and immutable R35 source verification.
Does not delete the repository's existing save records or change DB schema.
"""
from pathlib import Path
import json
import sys

from patch_catalog_expansion import official_manifest,key_for,skill_data

UI=Path("android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java")
START="    private void showKingmakerStructureDialog(final JSONObject settlement){"
END="    private void showKingmakerTradeDialog()"
MARKER="R36_OFFICIAL_BUILD_UI_RULES_ENGINE"

def java_string(s):
    return json.dumps(str(s),ensure_ascii=True)

def array(values):
    return "new String[]{"+",".join(java_string(x) for x in values)+"}"

def transform(source):
    if MARKER in source:raise RuntimeError("Official Build UI patched twice")
    if source.count(START)!=1 or source.count(END)!=1:
        raise RuntimeError("Kingmaker settlement UI method anchor drift")
    start=source.index(START)
    end=source.index(END,start)
    before=source[start:end]
    if before.count("kingmakerRepository.addStructure(")!=1:
        raise RuntimeError("Old manual addStructure entry missing/multiple")
    if "resolveBuildStructure(" in before:
        raise RuntimeError("Old UI already uses rules resolver")
    rows=official_manifest()
    keys=[key_for(x["official_name"]) for x in rows]
    labels=[x["official_name"]+" · "+str(x["costs"]["rp"])+" RP"
            +(" · implantação especial" if x["lots"] is None else "")
            +(" · custo alternativo" if " or " in x["cost_text"] else "")
            for x in rows]
    expected=[skill_data(x["construction"])[0][0] for x in rows]
    selection=["Automático (perícia oficial)"]+sorted(set(
      skill for x in rows for skill in skill_data(x["construction"])[0]))
    method='''    /** R36_OFFICIAL_BUILD_UI_RULES_ENGINE: no manual zero-cost insertion. */
    private void showKingmakerStructureDialog(final JSONObject settlement){
        final String[] officialKeys=KEYS_TOKEN;
        final String[] officialLabels=LABELS_TOKEN;
        final String[] defaultSkills=DEFAULT_TOKEN;
        final String[] skillOptions=SKILL_TOKEN;
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("Estruturas oficiais de Kingmaker. O motor confere fase de Atividades, proficiência, nível, lotes, pré-requisitos, RP, materiais, rolagem e consequências. Infraestruturas de implantação especial e pagamentos alternativos ainda exigem a resolução apropriada e serão bloqueados, nunca cobrados silenciosamente.",12,MUTED,false));
        final Spinner choice=spinner(officialLabels,officialLabels[0]);
        form.addView(cardWithLabel("ESTRUTURA OFICIAL",choice));
        final Spinner skillChoice=spinner(skillOptions,skillOptions[0]);
        form.addView(cardWithLabel("PERÍCIA (automática ou alternativa permitida)",skillChoice));
        final String[] commodityOptions={"Sem alternativa","Pagar com Madeira","Pagar com Pedra"};
        final Spinner commodity=spinner(commodityOptions,commodityOptions[0]);
        form.addView(cardWithLabel("CUSTO ALTERNATIVO (Bridge / Watchtower)",commodity));
        final EditText roll=labeledField(form,"d20 de construção",String.valueOf(new java.util.Random().nextInt(20)+1),false);
        roll.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText notes=labeledField(form,"Notas da construção / consequência","",true);
        new AlertDialog.Builder(this).setTitle("Construir em "+settlement.optString("name"))
            .setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null)
            .setPositiveButton("RESOLVER CONSTRUÇÃO",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface d,int which){
                    try{
                        final int position=choice.getSelectedItemPosition();
                        if(position<0||position>=officialKeys.length)
                            throw new IllegalArgumentException("Selecione uma estrutura oficial.");
                        final int n=num(roll,0);
                        final String skill=(skillChoice.getSelectedItemPosition()<=0)
                             ?defaultSkills[position]:String.valueOf(skillChoice.getSelectedItem());
                        final String commodityChoice=commodity.getSelectedItemPosition()==1?"lumber":
                            commodity.getSelectedItemPosition()==2?"stone":"";
                        JSONObject result=kingmakerRepository.resolveBuildStructure(
                            settlement.optString("settlement_key"),officialKeys[position],
                            n,skill,value(notes),commodityChoice);
                        showKingmaker();
                        toast("Construção resolvida: "+result.optString("construction_status","resultado registrado")+".");
                    }catch(Exception ex){
                        toast("Construção oficial: "+ex.getMessage());
                    }
                }
            }).show();
    }

    '''
    method=method.replace("KEYS_TOKEN",array(keys)).replace("LABELS_TOKEN",array(labels))
    method=method.replace("DEFAULT_TOKEN",array(expected)).replace("SKILL_TOKEN",array(selection))
    return source[:start]+method+source[end:]

def apply(root):
    root=Path(root).resolve()
    version=root/"version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8"):
        raise RuntimeError("R36 canonical Android source required")
    target=root/UI
    original=target.read_text(encoding="utf-8")
    updated=transform(original)
    target.write_text(updated,encoding="utf-8")
    print("KINGMAKER_OFFICIAL_BUILD_UI="+str(target)+" entries=73 guarded=6+2",flush=True)

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_official_build_ui.py <R36_android_project>")
    apply(sys.argv[1])
