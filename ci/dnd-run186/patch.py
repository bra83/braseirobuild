from pathlib import Path
import sys, json

root=Path(sys.argv[1])
pkg=root/"android"/"app"/"src"/"main"/"java"/"com"/"braseiro"/"dnd5e"
here=Path(__file__).resolve().parent

def read(name): return (pkg/name).read_text(encoding="utf-8")
def write(name,text): (pkg/name).write_text(text,encoding="utf-8",newline="\n")
def once(text,old,new,label):
    n=text.count(old)
    if n!=1: raise SystemExit(f"{label}: expected 1, got {n}")
    return text.replace(old,new,1)

catalog=(here/"StrixhavenPtBrAcademicCatalog.java").read_text(encoding="utf-8")
write("StrixhavenPtBrAcademicCatalog.java",catalog)

t=read("StrixhavenCompiledCampaignEngine.java")
a=t.index("    public static String yearSummary(int year){")
b=t.index("    public static String masterContext(int year,int semester){",a)
year_new=r'''    public static String yearSummary(int year){
        JSONObject c=chapterForYear(year);JSONArray p=c.optJSONArray("phases");StringBuilder b=new StringBuilder();
        b.append("Ano ").append(c.optInt("year",1)).append(" • ").append(StrixhavenPtBrAcademicCatalog.campaignTitle(c.optString("title"))).append(" • níveis ")
         .append(c.optInt("start_level",1)).append("→").append(c.optInt("end_level",4)).append("\n");
        if(p!=null)for(int i=0;i<p.length();i++){if(i>0)b.append("  ›  ");b.append(StrixhavenPtBrAcademicCatalog.eventTitle(p.optString(i)));}
        return b.toString();
    }
'''
t=t[:a]+year_new+t[b:]
a=t.index("    public static String[] examLabels(int year){")
b=t.index("    public static JSONObject examAt(int year,int index){",a)
exam_new=r'''    public static String[] examLabels(int year){
        JSONArray a=examsForYear(year);String[] out=new String[a.length()];for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);out[i]=StrixhavenPtBrAcademicCatalog.examCourse(e.optString("course"))+" — "+StrixhavenPtBrAcademicCatalog.examTitle(e.optString("title"));}return out;
    }
'''
t=t[:a]+exam_new+t[b:]
write("StrixhavenCompiledCampaignEngine.java",t)

t=read("MainActivity.java")
old='''            if(!StrixhavenOfficialCampaignRuntime.finished(this)){
                StrixhavenOfficialCampaignRuntime.Event next=StrixhavenOfficialCampaignRuntime.current(this);
                strixhavenProfile().setAcademicPeriod(next.year,next.semester);
            }else{
                strixhavenProfile().setAcademicPeriod(4,2);
            }
'''
new='''            StrixhavenAdventureProfile academicProfile=strixhavenProfile();
            if(!StrixhavenOfficialCampaignRuntime.finished(this)){
                StrixhavenOfficialCampaignRuntime.Event next=StrixhavenOfficialCampaignRuntime.current(this);
                if(next.year>ev.year){
                    for(JSONObject hero:db.listActiveCharacters())academicProfile.finishAcademicYear(hero.optLong("id"));
                    db.addAuditEvent("strixhaven_academic","Ano acadêmico concluído automaticamente","Ano "+ev.year+" concluído junto com a campanha oficial","{}",new JSONObject().put("next_year",next.year).put("next_semester",next.semester).toString(),true);
                }
                academicProfile.setAcademicPeriod(next.year,next.semester);
            }else{
                for(JSONObject hero:db.listActiveCharacters())academicProfile.finishAcademicYear(hero.optLong("id"));
                academicProfile.setAcademicPeriod(4,2);
            }
'''
t=once(t,old,new,"automatic-academic-year-transition")

old='''        final boolean[] strixMasterChatMode=new boolean[]{false};final int masterBlue=Color.rgb(96,200,230);
        LinearLayout composerModeRow=new LinearLayout(this);composerModeRow.setOrientation(LinearLayout.HORIZONTAL);Button strixActionMode=smallButton("AÇÃO",GOLD),strixMasterMode=smallButton("FALAR COM O MESTRE",Color.rgb(19,49,73));strixActionMode.setTextColor(Color.BLACK);strixMasterMode.setTextColor(masterBlue);composerModeRow.addView(strixActionMode,new LinearLayout.LayoutParams(0,dp(38),1));composerModeRow.addView(strixMasterMode,new LinearLayout.LayoutParams(0,dp(38),1));drawer.addView(composerModeRow);
'''
new='''        final boolean[] strixMasterChatMode=new boolean[]{false};final int masterBlue=Color.rgb(96,200,230);
        LinearLayout composerModeRow=new LinearLayout(this);composerModeRow.setOrientation(LinearLayout.HORIZONTAL);Button strixActionMode=smallButton("AÇÃO",GOLD),strixMasterMode=smallButton("CONSULTAR MESTRE",Color.rgb(19,49,73));strixActionMode.setTextColor(Color.BLACK);strixMasterMode.setTextColor(masterBlue);composerModeRow.addView(strixActionMode,new LinearLayout.LayoutParams(0,dp(38),1));composerModeRow.addView(strixMasterMode,new LinearLayout.LayoutParams(0,dp(38),1));drawer.addView(composerModeRow);drawer.addView(text("AÇÃO avança a ficção. CONSULTAR MESTRE responde dúvidas sem avançar relógio, mapa, NPCs ou campanha.",10,MUTED,false));
'''
t=once(t,old,new,"master-query-label")
t=once(t,'narratorInput.setHint(help?"MESTRE — pergunte sem avançar a história":"AÇÃO LIVRE — descreva o que deseja fazer");','narratorInput.setHint(help?"CONSULTA AO MESTRE — não avança a história":"AÇÃO LIVRE — descreva o que deseja fazer");',"master-query-hint")
t=once(t,'body.addView(text("FERRAMENTAS DO MESTRE",13,GOLD,true));','body.addView(text("FERRAMENTAS DO MESTRE • ADMINISTRAÇÃO",13,GOLD,true));body.addView(text("Este painel configura o comportamento do Mestre e abre ferramentas. Para apenas tirar uma dúvida sem avançar a história, use CONSULTAR MESTRE na tela principal.",10,MUTED,false));',"master-tools-explanation")
t=t.replace('new AlertDialog.Builder(this).setTitle("Mestre • Strixhaven")','new AlertDialog.Builder(this).setTitle("Ferramentas do Mestre • Strixhaven")',1)

start=t.index("    private void showStrixhavenAcademicDialog(){")
end=t.index("    private void confirmExitStrixhavenToFaerun(){",start)
academic="".join((here/f"academic_methods_{i}.txt").read_text(encoding="utf-8") for i in (1,2,3))
t=t[:start]+academic+t[end:]
write("MainActivity.java",t)

for p in pkg.glob("*.java"):
    src=p.read_text(encoding="utf-8")
    src=src.replace("RUNTIMES COMPILADOS — RUN163","RUNTIMES COMPILADOS — RUN186")
    src=src.replace("RUNTIMES COMPILADOS - RUN163","RUNTIMES COMPILADOS — RUN186")
    p.write_text(src,encoding="utf-8",newline="\n")

t=read("MainActivity.java")
checks={
 "ptbr_catalog":(pkg/"StrixhavenPtBrAcademicCatalog.java").exists(),
 "guided_exam":all(x in t for x in ("ROLAR TESTE DE ESTUDO","ROLAR TESTE 1","ROLAR TESTE 2")),
 "canonical_extras":catalog.count("a.put(extra(")==16,
 "canonical_jobs":catalog.count("a.put(job(")==9,
 "automatic_year":"Ano acadêmico concluído automaticamente" in t,
 "no_manual_end_year":"ENCERRAR ANO ACADÊMICO" not in t,
 "friendly_wage":"Salário indisponível:" not in t,
 "master_query":"CONSULTAR MESTRE" in t and "sem avançar relógio, mapa, NPCs ou campanha" in t,
 "master_admin":"FERRAMENTAS DO MESTRE • ADMINISTRAÇÃO" in t,
 "relationship_pt":"Pessoa Querida" in t and "Benefício do Vínculo" in t,
 "legacy_label_removed":"RUNTIMES COMPILADOS — RUN163" not in t,
}
bad=[k for k,v in checks.items() if not v]
print(json.dumps(checks,ensure_ascii=False,indent=2))
if bad: raise SystemExit("RUN186_PATCH_FAIL::"+",".join(bad))
print("RUN186_PATCH_OK")
