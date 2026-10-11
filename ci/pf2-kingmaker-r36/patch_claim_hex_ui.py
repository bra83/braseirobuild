#!/usr/bin/env python3
"""Route UI Claim Hex to official rule resolver, rather than directly marking owned.
Requires verified R36 9.0.37 after official patches. Touches only MainActivity.java.
"""
from pathlib import Path
import sys

REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java")
OLD="else if(a==2)kingmakerRepository.claimHex(h,value(notes));"
NEW="else if(a==2){showKingmakerClaimHexDialog(h,value(notes));return;}"
MARKER="    private void showKingmakerHexDialog(){"
DIALOG='''    /** Official Kingmaker: Claim Hex must make its Kingdom skill check and persist the outcome. */
    private void showKingmakerClaimHexDialog(final String hexId,final String sourceNotes){
        final String[] skills={"exploration","intrigue","magic","wilderness"};
        final String[] labels={"Exploração","Intriga","Magia","Sobrevivência"};
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("Reivindicar Hex custa 1 RP antes do teste. O motor valida reconhecimento, adjacência, limites por turno e consequências.",13,TEXT,false));
        final Spinner selected=spinner(labels,labels[0]);
        form.addView(cardWithLabel("PERÍCIA OFICIAL",selected));
        final int rolled=new Random().nextInt(20)+1;
        final EditText natural=labeledField(form,"d20",String.valueOf(rolled),false);
        natural.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("Reivindicar Hex • "+hexId)
            .setView(scrollableDialogView(form))
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("RESOLVER",(dialog,which)->{
                try{
                    JSONObject result=kingmakerRepository.resolveClaimHex(
                        hexId,num(natural,rolled),skills[selected.getSelectedItemPosition()],sourceNotes);
                    screenCache.clear();showKingmaker();
                    JSONObject check=result.optJSONObject("check");
                    String roll=check==null?"":("\\nTeste: "+check.optString("degree_pt")+
                        " • total "+check.optInt("total")+" contra CD "+check.optInt("dc"));
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Reivindicar Hex — consequência")
                        .setMessage(result.optString("outcome_pt")+roll)
                        .setPositiveButton("Fechar",null).show();
                }catch(Exception ex){toast("Reivindicar Hex: "+ex.getMessage());}
            }).show();
    }

'''
def transform(src):
    if src.count(OLD)!=1:raise RuntimeError("Missing or ambiguous direct Claim Hex UI path")
    if src.count(MARKER)!=1 or "private void showKingmakerClaimHexDialog(" in src:
        raise RuntimeError("Unexpected Kingmaker dialog source")
    return src.replace(OLD,NEW,1).replace(MARKER,DIALOG+MARKER,1)

def apply(root):
    root=Path(root).resolve()
    v=root/"version.properties";p=root/REL
    if not v.is_file() or "VERSION_NAME=9.0.37" not in v.read_text(encoding="utf-8"):
        raise RuntimeError("Expected R36 9.0.37")
    before=p.read_text(encoding="utf-8")
    after=transform(before)
    p.write_text(after,encoding="utf-8")
    return p

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_claim_hex_ui.py <R36 project>")
    print("R36_CLAIM_UI_RESOLVER_CONNECTED",apply(sys.argv[1]))
