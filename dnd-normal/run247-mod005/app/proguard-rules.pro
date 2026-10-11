# Regras de release: shrink/minify ativos, preservando componentes carregados por nome.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Componentes Android e WorkManager declarados no manifesto ou reconstruídos pelo runtime.
-keep class com.braseiro.dnd5e.** extends android.app.Activity { *; }
-keep class com.braseiro.dnd5e.** extends android.app.Service { *; }
-keep class com.braseiro.dnd5e.** extends android.content.BroadcastReceiver { *; }
-keep class com.braseiro.dnd5e.** extends android.content.ContentProvider { *; }
-keep class com.braseiro.dnd5e.** extends androidx.work.Worker { public <init>(...); *; }

# O indexador possui fallback reflexivo para APIs de texto do PDFBox.
-keepclassmembers class com.tom_roush.pdfbox.** {
    public *** getTextContents(...);
    public *** getText(...);
}

# Mantém metadados JSON e callbacks usados pelas bibliotecas, sem conservar toda a aplicação.
-keep class org.json.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.logging.**
