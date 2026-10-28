#!/usr/bin/env python3
"""One-shot, exact-anchor P0 repair of the existing large MainActivity.java.

Only the D&D individual character import path is modified. This script fails
closed if the inspected upstream source has changed.
"""
from pathlib import Path

source = Path(__file__).resolve().parents[1] / "app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
text = source.read_text(encoding="utf-8")
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
assert text.count(old_branch) == 1, "Import UI changed: refuse unsupervised edit"
text = text.replace(old_branch,new_branch,1)
source.write_text(text,encoding="utf-8")
print("PATCH_6620_IMPORT_ID_CONFIRMATION_APPLIED=1")
