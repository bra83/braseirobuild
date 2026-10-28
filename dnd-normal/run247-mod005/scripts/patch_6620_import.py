#!/usr/bin/env python3
"""One-shot, exact-anchor P0 repair of the existing large MainActivity.java.

Only the D&D individual character import path is modified. This script fails
closed if the inspected upstream source has changed.
"""
from pathlib import Path

source = Path(__file__).resolve().parents[1] / "app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
raw = source.read_bytes()
text = raw.decode("utf-8")
newline = "\r\n" if b"\r\n" in raw else "\n"
old_id = 'clean.remove("id");clean.remove("token_uri");'
new_id = '/* Retain export ID as an untrusted lookup hint; UI confirmation is mandatory. */clean.remove("token_uri");'
assert text.count(old_id) == 1, "Import decode changed: refuse unsupervised edit"
text = text.replace(old_id, new_id, 1)

old_branch = '''            characterDialog(matching,imported);
            toast(matching!=null?
                "Ficha existente identificada pelo ID. Revise e salve para corrigir sem duplicar.":
                "Ficha JSON aberta como novo herói. Revise antes de salvar.");'''
new_branch = '''            if(matching!=null){
                final JSONObject confirmedExisting=matching,confirmedImport=imported;
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Importar ficha já existente")
                    .setMessage("O ID e a identidade coincidem com um personagem deste banco. Revise o conteúdo antes de substituir. Será criado um ponto de segurança antes da edição. Você também pode importar uma cópia separada.")
                    .setPositiveButton("Revisar substituição",new android.content.DialogInterface.OnClickListener(){
                        @Override public void onClick(android.content.DialogInterface dialog,int which){
                            try{
                                safety.beforeDestructiveChange("revisar importação da ficha existente");
                                characterDialog(confirmedExisting,confirmedImport);
                            }catch(Exception error){toast("Importação interrompida: "+error.getMessage());}
                        }
                    })
                    .setNegativeButton("Importar como novo",new android.content.DialogInterface.OnClickListener(){
                        @Override public void onClick(android.content.DialogInterface dialog,int which){
                            confirmedImport.remove("id");
                            characterDialog(null,confirmedImport);
                        }
                    })
                    .setNeutralButton("Cancelar",null).show();
            }else{
                imported.remove("id"); // Foreign/colliding IDs have no authority.
                characterDialog(null,imported);
                toast("Ficha JSON aberta como novo herói. Revise antes de salvar.");
            }'''
# The existing giant Android file contains mixed line endings in some regions.
# Match the exact inspected block, accepting only an LF or CRLF representation.
old_lf=old_branch
new_lf=new_branch
old_crlf=old_branch.replace("\n","\r\n")
new_crlf=new_branch.replace("\n","\r\n")
if text.count(old_lf)==1:
    text=text.replace(old_lf,new_lf,1)
elif text.count(old_crlf)==1:
    text=text.replace(old_crlf,new_crlf,1)
else:
    raise AssertionError("Import UI changed: refuse unsupervised edit")
# Both random creation entry points must respect the same fail-closed gate.
old_full = 'characterDialog(null,seed);}});b.addView(randomHero);'
new_full = ('if(!seed.optBoolean("generation_valid",false)){'
    'toast("Geração bloqueada: "+seed.optString("generation_blocked_reason","escolhas inválidas"));'
    'return;}characterDialog(null,seed);}});b.addView(randomHero);')
assert text.count(old_full)==1, "Random hero UI changed: refuse unsafe patch"
text=text.replace(old_full,new_full,1)

old_random_form = 'applyCharacterSeed(f,generated);toast("Ficha aleatória completa. Você pode editar qualquer campo antes de salvar.");'
new_random_form = ('if(!generated.optBoolean("generation_valid",false)){'
    'toast("Geração não concluída: "+generated.optString("generation_blocked_reason","escolhas obrigatórias pendentes"));'
    'return;}applyCharacterSeed(f,generated);'
    'toast("Ficha aleatória validada. Revise as escolhas antes de salvar.");')
assert text.count(old_random_form)==1, "Random form UI changed: refuse unsafe patch"
text=text.replace(old_random_form,new_random_form,1)

source.write_bytes(text.encode("utf-8"))
print("PATCH_6620_IMPORT_AND_RANDOM_GATES_APPLIED=1")
