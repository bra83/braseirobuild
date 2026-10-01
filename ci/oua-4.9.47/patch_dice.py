from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_dice.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="android/app/src/main/java/com/braseiro/oumanel/OneRingDice3DOverlay.java"
t=read(rel)

t=req(
    t,
    'final TextView resultView=txt("Dados em movimento…",20,Color.WHITE,true);resultView.setGravity(Gravity.CENTER);',
    'final TextView resultView=txt("Dados prontos. Toque em ROLAR.",20,Color.WHITE,true);resultView.setGravity(Gravity.CENTER);',
    "ready message",
)
t=req(
    t,
    'final Button apply=button("APLICAR RESULTADO");apply.setVisibility(View.GONE);final Button cancel=button("CANCELAR ROLAGEM");',
    'final Button rollButton=button("ROLAR");final Button apply=button("APLICAR RESULTADO");apply.setVisibility(View.GONE);final Button cancel=button("CANCELAR ROLAGEM");',
    "roll button declaration",
)
t=req(
    t,
    'bottom.addView(resultView);bottom.addView(detailView);bottom.addView(apply);bottom.addView(cancel);root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));',
    'bottom.addView(resultView);bottom.addView(detailView);bottom.addView(rollButton);bottom.addView(apply);bottom.addView(cancel);root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));',
    "roll button layout",
)

old='apply.setOnClickListener(v->{if(envelope[0]==null)return;dialog.dismiss();if(callback!=null)callback.onResult(envelope[0]);});cancel.setOnClickListener(v->{done[0]=true;dialog.dismiss();if(callback!=null)callback.onCancel();});dialog.setOnShowListener(x->{Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);});dialog.show();'
new='rollButton.setOnClickListener(v->{if(done[0])return;rollButton.setEnabled(false);rollButton.setVisibility(View.GONE);resultView.setText("Dados em movimento…");lastNs[0]=System.nanoTime();diceView.postOnAnimation(ticker);});apply.setOnClickListener(v->{if(envelope[0]==null)return;dialog.dismiss();if(callback!=null)callback.onResult(envelope[0]);});cancel.setOnClickListener(v->{done[0]=true;dialog.dismiss();if(callback!=null)callback.onCancel();});dialog.setOnShowListener(x->{Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);});dialog.show();'
t=req(t,old,new,"explicit physical roll trigger")

write(rel,t)
print("PATCH_OUA_4.9.47_EXPLICIT_ROLL_BUTTON_OK")
