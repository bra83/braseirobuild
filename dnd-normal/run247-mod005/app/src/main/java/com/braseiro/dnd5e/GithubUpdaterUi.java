package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

/** Mirrors the verified Cyberpunk RED update flow while preserving D&D saves. */
final class GithubUpdaterUi {
    static void mount(Activity a, LinearLayout root, CampaignSafetyManager safety){
        LinearLayout section=new LinearLayout(a);section.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(a);title.setText("ATUALIZAÇÃO DIRETA PELO GITHUB");
        title.setTextSize(17);section.addView(title);
        TextView hint=new TextView(a);hint.setText(
            "Verifica release de D&D, SHA-256, assinatura Android, pacote e versionCode. " +
            "Faz backup local antes da instalação; o instalador do Android exige seu consentimento. Não depende de BRELGA.");
        hint.setTextSize(13);section.addView(hint);
        TextView status=new TextView(a);status.setText("Versão instalada: "+BuildConfig.VERSION_NAME+" • Toque para verificar.");status.setTextSize(12);section.addView(status);
        Button update=new Button(a);update.setText("VERIFICAR ATUALIZAÇÕES NO GITHUB");section.addView(update);
        final GithubApkUpdater updater=new GithubApkUpdater(a);
        update.setOnClickListener(v->{
            if(updater.isBusy()){status.setText("Verificação ou download em andamento.");return;}
            // Do not request unknown-source permission merely to CHECK the version.
            // Android prompts for installation permission only if a verified newer APK exists.
            update.setEnabled(false);
            updater.checkAndDownload(new GithubApkUpdater.Reporter(){
                public void status(String message){a.runOnUiThread(()->{
                    status.setText(message);
                    if(!message.startsWith("Consultando")&&
                       !message.startsWith("Baixando")&&
                       !message.startsWith("APK verificado:"))
                        update.setEnabled(true);
                });}
                public void installationReady(final File file){
                    AppExecutors.serial().execute(()->{
                        String problem="";
                        try{safety.createSnapshot("Antes de atualizar APK D&D pelo GitHub");}
                        catch(Exception error){problem=error.getMessage()==null?error.toString():error.getMessage();}
                        final String err=problem;
                        a.runOnUiThread(()->{
                            update.setEnabled(true);
                            if(!err.isEmpty()){
                                status.setText("Instalação suspensa: backup preventivo falhou: "+err);
                                return;
                            }
                            new AlertDialog.Builder(a).setTitle("APK verificado e backup criado")
                                .setMessage("A versão é superior, a assinatura é igual à instalada e o hash confere. Instalar agora mantendo os dados existentes?")
                                .setNegativeButton("Cancelar",null)
                                .setPositiveButton("INSTALAR",(d,w)->{
                                    try{updater.requestInstall(file);}
                                    catch(Exception ex){status.setText("Instalação suspensa: "+ex.getMessage());}
                                }).show();
                        });
                    });
                }
            });
        });
        root.addView(section);
    }
}
