from pathlib import Path
import sys

root=Path(sys.argv[1]) if len(sys.argv)>1 else Path('/mnt/data/run186_local')
pkg=root/'android'/'app'/'src'/'main'/'java'/'com'/'braseiro'/'dnd5e'
if not pkg.exists():
    raise SystemExit(f'SOURCE_NOT_FOUND::{pkg}')

def read(name): return (pkg/name).read_text(encoding='utf-8')
def write(name,text): (pkg/name).write_text(text,encoding='utf-8',newline='\n')

def replace_method(text, signature, replacement):
    i=text.find(signature)
    if i<0: raise SystemExit('METHOD_NOT_FOUND::'+signature)
    b=text.find('{',i)
    if b<0: raise SystemExit('METHOD_BRACE_NOT_FOUND::'+signature)
    depth=0; state='code'; esc=False; j=b
    while j<len(text):
        ch=text[j]; nx=text[j+1] if j+1<len(text) else ''
        if state=='code':
            if ch=='"': state='str'
            elif ch=="'": state='char'
            elif ch=='/' and nx=='/': state='line'; j+=1
            elif ch=='/' and nx=='*': state='block'; j+=1
            elif ch=='{': depth+=1
            elif ch=='}':
                depth-=1
                if depth==0:
                    return text[:i]+replacement+text[j+1:]
        elif state=='str':
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch=='"': state='code'
        elif state=='char':
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch=="'": state='code'
        elif state=='line':
            if ch=='\n': state='code'
        elif state=='block':
            if ch=='*' and nx=='/': state='code'; j+=1
        j+=1
    raise SystemExit('METHOD_UNTERMINATED::'+signature)

# ---------------- MainActivity: guided, PT-BR academic UX ----------------
t=read('MainActivity.java')

master=r'''private void showStrixhavenMasterToolsDialog(){
        final LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(8),dp(12),dp(8));
        body.addView(text("FERRAMENTAS DO MESTRE",13,GOLD,true));
        body.addView(text("Este painel não avança a aventura por si só. Ele serve para consultar regras, abrir manuais, configurar o estilo de narração e administrar a Vida Acadêmica.",11,MUTED,false));
        final Spinner mode=spinner(new String[]{"Narrador","Regras RAW/RAI","Sortear dos livros","Criar encontro","Criar NPC","Preparar sessão"},prefs.getString("ai_mode","Narrador"));body.addView(cardWithLabel("PERFIL / TAREFA DO MESTRE",mode));
        Button ruleChat=button("CONSULTAR O MESTRE SEM AVANÇAR A HISTÓRIA",PANEL2);ruleChat.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final EditText q=input("Pergunte sobre regra, aula, prova, magia ou situação.","",true);new AlertDialog.Builder(MainActivity.this).setTitle("Consulta de regras — sem avanço").setView(q).setNegativeButton("Cancelar",null).setPositiveButton("PERGUNTAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){String x=q.getText().toString().trim();if(x.length()>0)sendMasterSideQuestion("Consulte os manuais e explique esta regra no contexto de Strixhaven: "+x);}}).show();}});body.addView(ruleChat);
        Button attach=button("ANEXAR / TROCAR LIVRO",PANEL2);attach.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showAttachManualDialog();}});body.addView(attach);
        Button manuals=button("ABRIR MANUAIS",PANEL2);manuals.setOnClickListener(new View.OnClickListener(){public void onClick(View v){navigate("Manuais");}});body.addView(manuals);
        Button academic=button("VIDA ACADÊMICA",GREEN);academic.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenAcademicDialog();}});body.addView(academic);
        new AlertDialog.Builder(this).setTitle("Ferramentas do Mestre • Strixhaven").setView(scrollableDialogView(body)).setNegativeButton("Fechar",null).setPositiveButton("APLICAR PERFIL",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){prefs.edit().putString("ai_mode",String.valueOf(mode.getSelectedItem())).apply();toast("Perfil do Mestre: "+mode.getSelectedItem());}}).show();
    }'''
t=replace_method(t,'private void showStrixhavenMasterToolsDialog()',master)

helpers=r'''private String academicBlockReasonPt(String code){
        String c=code==null?"":code.trim();
        if("tutoring_required".equals(c))return "Tutoria obrigatória: emprego e atividades extracurriculares ficam suspensos até você passar em outra prova deste mesmo ano.";
        if("join_only_at_start_of_academic_year".equals(c))return "A entrada em atividade extracurricular só pode ser registrada no início do ano acadêmico.";
        if("extracurricular_required".equals(c))return "Informe o nome da atividade extracurricular.";
        if("already_joined".equals(c))return "Este estudante já participa dessa atividade.";
        if("extracurricular_limit".equals(c))return "Limite de atividades atingido: até duas sem emprego, ou uma quando há emprego ativo.";
        if("job_requires_at_most_one_extracurricular".equals(c))return "Para assumir um emprego, mantenha no máximo uma atividade extracurricular.";
        if("employer_required".equals(c))return "Informe o local ou empregador.";
        if("job_wage_not_active".equals(c))return "Você não possui um emprego ativo, ou o emprego está suspenso por tutoria.";
        if("job_wage_already_claimed_for_week".equals(c))return "O salário desta semana já foi recebido.";
        if("friendship_threshold_not_met".equals(c))return "São necessários pelo menos 2 pontos positivos com esse colega para torná-lo uma Pessoa Querida.";
        if("no_beloved_inspiration".equals(c))return "Não há Inspiração de Pessoa Querida disponível.";
        if("no_matching_student_die".equals(c))return "Não há Dado do Estudante aplicável a esta perícia.";
        if(c.length()==0)return "A ação não está disponível neste momento.";
        return c.replace('_',' ');
    }

    private String academicExamResultPt(String result){
        if("aced".equalsIgnoreCase(result))return "Excelente";
        if("passed".equalsIgnoreCase(result))return "Aprovado";
        if("failed".equalsIgnoreCase(result))return "Reprovado";
        return result==null?"":result;
    }

    private void rollStrixhavenAcademicCheck(final JSONObject hero,final String ability,final String skill,final int dc,final String mode,final TextView output,final CheckBox success,final String label){
        int b=rulesEngine.d20CheckBonus(hero,ability,skill);final String formula="d20"+(b>=0?"+":"")+b;final String resolvedMode=mode==null?"Normal":mode;
        rollDiceInteractive(formula,resolvedMode,hero.optString("name",""),hero.optLong("id",0),new DiceUiCallback(){public void done(DiceResult r){boolean ok=r.total>=dc;success.setChecked(ok);output.setText(label+": "+r.total+" contra CD "+dc+" — "+(ok?"SUCESSO":"FALHA")+"\n"+r.detail);try{recordDiceHistory(formula,resolvedMode,hero.optString("name",""),hero.optLong("id",0),r,new JSONObject().put("source","strixhaven_academic").put("label",label).put("dc",dc).put("ability",ability).put("skill",skill));}catch(Exception ignored){}}});
    }

    private void showStrixhavenAcademicProgressHelp(){
        JSONObject p=StrixhavenOfficialCampaignRuntime.progress(this);String title=StrixhavenCompiledCampaignEngine.localizeCampaignLabel(p.optString("title",""));
        String msg="Ano "+p.optInt("year",strixhavenProfile().year())+" • Semestre "+p.optInt("semester",strixhavenProfile().semester())+"\nMarco atual: "+title+"\nProgresso oficial: "+p.optInt("completed_events",0)+"/"+p.optInt("total",44)+" ("+p.optInt("percent",0)+"%)\n\nAno e semestre avançam automaticamente quando os marcos oficiais da campanha são realmente concluídos na ficção. Não existe avanço manual por botão. Ao mudar de ano, os Dados do Estudante obtidos em provas do ano anterior expiram automaticamente; benefícios de atividades extracurriculares permanecem enquanto a atividade continuar.";
        new AlertDialog.Builder(this).setTitle("Como avançar semestre / ano").setMessage(msg).setPositiveButton("OK",null).show();
    }

    '''
anchor='private void showStrixhavenAcademicDialog()'
i=t.find(anchor)
if i<0: raise SystemExit('ACADEMIC_ANCHOR_MISSING')
t=t[:i]+helpers+t[i:]

academic=r'''private void showStrixhavenAcademicDialog(){
        final List<JSONObject> heroes=db.listActiveCharacters();if(heroes.isEmpty()){toast("Nenhum personagem ativo.");return;}
        final StrixhavenAdventureProfile profile=strixhavenProfile();String[] names=new String[heroes.size()];for(int i=0;i<heroes.size();i++)names[i]=heroes.get(i).optString("name","Personagem");
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(8),dp(12),dp(12));
        LinearLayout official=card();official.addView(text("CAMPANHA OFICIAL • UM CURRÍCULO DE CAOS",13,GOLD,true));official.addView(text(StrixhavenCompiledCampaignEngine.yearSummary(profile.year()),11,TEXT,false));official.addView(text("4 anos • níveis 1→10 • 10 provas oficiais • 18 Colegas Estudantes • regras compiladas no aplicativo. Rolagens só acontecem quando você toca em ROLAR.",10,MUTED,false));body.addView(official);
        final Spinner hero=spinner(names,names[0]);body.addView(cardWithLabel("ESTUDANTE",hero));final TextView status=text("",12,TEXT,false);body.addView(status);
        final Runnable refresh=new Runnable(){public void run(){JSONObject h=heroes.get(hero.getSelectedItemPosition());status.setText(strixhavenAcademicSummaryText(profile.academicSummary(h.optLong("id"))));}};hero.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        body.addView(text("PROVAS",12,GOLD,true));body.addView(text("Escolha a prova oficial, configure o estudo e role Estudo/Teste 1/Teste 2 dentro do próprio fluxo. Você ainda pode marcar manualmente um resultado caso use dados físicos.",10,MUTED,false));Button exam=button("FAZER / REGISTRAR PROVA",PANEL2);exam.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenExamDialog(heroes.get(hero.getSelectedItemPosition()));}});body.addView(exam);
        body.addView(text("VIDA NO CAMPUS",12,GOLD,true));body.addView(text("Atividades, empregos e relações são persistentes. Tutoria pode suspender emprego e atividade até uma prova posterior ser aprovada.",10,MUTED,false));Button extra=button("ATIVIDADE EXTRACURRICULAR",PANEL2);extra.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenExtracurricularDialog(heroes.get(hero.getSelectedItemPosition()));}});body.addView(extra);Button job=button("EMPREGO",PANEL2);job.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenJobDialog(heroes.get(hero.getSelectedItemPosition()));}});body.addView(job);Button rel=button("RELACIONAMENTO",PANEL2);rel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenRelationshipDialog(heroes.get(hero.getSelectedItemPosition()));}});body.addView(rel);
        Button rest=button("DESCANSO LONGO — RECUPERAR BENEFÍCIOS ACADÊMICOS",PANEL2);rest.setOnClickListener(new View.OnClickListener(){public void onClick(View v){JSONObject h=heroes.get(hero.getSelectedItemPosition());profile.academicLongRest(h.optLong("id"),rulesEngine.proficiencyBonus(h.optInt("level",1)));toast("Dados do Estudante e Inspiração de Pessoa Querida recuperados conforme as regras.");refresh.run();}});body.addView(rest);
        Button wage=button("RECEBER SALÁRIO DA SEMANA",PANEL2);wage.setOnClickListener(new View.OnClickListener(){public void onClick(View v){JSONObject h=heroes.get(hero.getSelectedItemPosition());int day=Math.max(1,db.getSceneState().optInt("day_number",1)),week=((day-1)/7)+1;JSONObject next=StrixhavenAcademicRulesEngine.claimWeeklyJobWage(profile.academicState(),String.valueOf(h.optLong("id")),week);if(!next.optBoolean("job_wage_claimed",false)){toast(academicBlockReasonPt(next.optString("academic_block_reason","")));return;}profile.saveAcademicState(next);JSONObject sheet=parseLooseJson(h.optString("sheet_json","{}"));try{sheet.put("gold",Math.max(0,sheet.optInt("gold",0))+next.optInt("gold_due",5));db.updateCharacterSheet(h.optLong("id"),sheet.toString());}catch(Exception ignored){}toast("Salário recebido: +5 po.");refresh.run();}});body.addView(wage);
        Button advance=button("COMO AVANÇAR SEMESTRE / ANO",Color.rgb(94,70,44));advance.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showStrixhavenAcademicProgressHelp();}});body.addView(advance);
        refresh.run();new AlertDialog.Builder(this).setTitle("Strixhaven • Vida Acadêmica").setView(scrollableDialogView(body)).setPositiveButton("Fechar",null).show();
    }'''
t=replace_method(t,'private void showStrixhavenAcademicDialog()',academic)

summary=r'''private String strixhavenAcademicSummaryText(JSONObject s){JSONObject job=s.optJSONObject("job");String employer=(s.optBoolean("job_active",false)&&job!=null)?job.optString("employer","ativo"):"nenhum";return "Tutoria: "+(s.optBoolean("tutoring_required",false)?"OBRIGATÓRIA — emprego/atividade suspensos":"não")+"\nProvas registradas: "+s.optInt("exam_count",0)+" • Dados do Estudante: "+s.optInt("student_dice_total",0)+"\nAtividades extracurriculares: "+s.optInt("extracurricular_count",0)+" • Emprego: "+employer+"\nRelações: "+s.optInt("friends",0)+" amigo(s), "+s.optInt("rivals",0)+" rival(is), "+s.optInt("beloveds",0)+" pessoa(s) querida(s) • Inspiração de Pessoa Querida: "+s.optInt("beloved_inspiration",0);}'''
t=replace_method(t,'private String strixhavenAcademicSummaryText(JSONObject s)',summary)

exam=r'''private void showStrixhavenExamDialog(final JSONObject hero){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(12),dp(6),dp(12),dp(12));
        final JSONObject[] selectedExam={new JSONObject()};
        final EditText exam=labeledField(f,"Prova","",false),skill1=labeledField(f,"Perícia do teste 1","",false),skill2=labeledField(f,"Perícia do teste 2","",false);final TextView canonicalExamInfo=text("Escolha uma prova oficial. O aplicativo carrega CDs, atributos e perícias compilados.",11,MUTED,false);f.addView(canonicalExamInfo);
        final CheckBox skip=check("Pular estudo",false),studySuccess=check("Estudo foi bem-sucedido (marque apenas se rolou fora do app)",false),group=check("Estudo em grupo — vantagem no teste de estudo",false),allNight=check("Virar a noite estudando — +1 Exaustão; 2 rerrolagens se o estudo passar",false),cheat=check("Trapacear na prova",false),first=check("Teste 1 bem-sucedido",false),second=check("Teste 2 bem-sucedido",false),deception=check("Enganação da trapaça passou",false),sleight=check("Prestidigitação da trapaça passou",false);
        final Spinner studyAbility=spinner(new String[]{"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"},"Inteligência");final Spinner studySkill=spinner(skillNames(),"Arcanismo");final TextView studyResult=text("Estudo ainda não rolado.",11,TEXT,false),test1Result=text("Teste 1 ainda não rolado.",11,TEXT,false),test2Result=text("Teste 2 ainda não rolado.",11,TEXT,false);
        final Runnable loadExam=new Runnable(){public void run(){JSONObject e=selectedExam[0];if(e==null||e.length()==0)return;exam.setText(e.optString("course")+" — "+e.optString("title"));skill1.setText(e.optString("skill1"));skill2.setText(e.optString("skill2"));canonicalExamInfo.setText(StrixhavenCompiledCampaignEngine.examRuleText(e));studyResult.setText("Estudo: escolha qualquer combinação de atributo + perícia e role contra CD "+e.optInt("study_dc")+".");test1Result.setText("Teste 1: "+e.optString("ability1")+" ("+e.optString("skill1")+") CD "+e.optInt("dc1")+".");test2Result.setText("Teste 2: "+e.optString("ability2")+" ("+e.optString("skill2")+") CD "+e.optInt("dc2")+".");}};
        final String[] canonicalExamLabels=StrixhavenCompiledCampaignEngine.examLabels(strixhavenProfile().year());if(canonicalExamLabels.length>0){selectedExam[0]=StrixhavenCompiledCampaignEngine.examAt(strixhavenProfile().year(),0);loadExam.run();Button canonicalExam=button("ESCOLHER PROVA OFICIAL",PANEL2);canonicalExam.setOnClickListener(new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("Provas oficiais • Ano "+strixhavenProfile().year()).setItems(canonicalExamLabels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){selectedExam[0]=StrixhavenCompiledCampaignEngine.examAt(strixhavenProfile().year(),which);loadExam.run();}}).show();}});f.addView(canonicalExam);}else f.addView(text("Nenhuma prova oficial encontrada para este ano.",11,RED,true));
        f.addView(text("1. ESTUDO",12,GOLD,true));f.addView(cardWithLabel("ATRIBUTO DO ESTUDO",studyAbility));f.addView(cardWithLabel("PERÍCIA DO ESTUDO",studySkill));f.addView(skip);f.addView(group);f.addView(allNight);Button rollStudy=button("ROLAR ESTUDO",PANEL2);rollStudy.setOnClickListener(new View.OnClickListener(){public void onClick(View v){JSONObject e=selectedExam[0];if(e==null||e.length()==0){toast("Escolha uma prova oficial.");return;}if(skip.isChecked()){studySuccess.setChecked(false);studyResult.setText("Estudo pulado — nenhuma rerrolagem obtida.");return;}rollStrixhavenAcademicCheck(hero,String.valueOf(studyAbility.getSelectedItem()),String.valueOf(studySkill.getSelectedItem()),e.optInt("study_dc"),group.isChecked()?"Vantagem":"Normal",studyResult,studySuccess,"Estudo");}});f.addView(rollStudy);f.addView(studyResult);f.addView(studySuccess);
        f.addView(text("2. PROVA",12,GOLD,true));Button roll1=button("ROLAR TESTE 1",PANEL2);roll1.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final JSONObject e=selectedExam[0];if(e==null||e.length()==0){toast("Escolha uma prova oficial.");return;}String sk=value(skill1);if(sk.contains(" ou ")){final String[] opts=sk.split(" ou ");new AlertDialog.Builder(MainActivity.this).setTitle("Escolha a perícia do Teste 1").setItems(opts,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){String chosen=opts[which].trim();skill1.setText(chosen);rollStrixhavenAcademicCheck(hero,e.optString("ability1"),chosen,e.optInt("dc1"),"Normal",test1Result,first,"Teste 1");}}).show();return;}rollStrixhavenAcademicCheck(hero,e.optString("ability1"),sk,e.optInt("dc1"),"Normal",test1Result,first,"Teste 1");}});f.addView(roll1);f.addView(test1Result);f.addView(first);Button roll2=button("ROLAR TESTE 2",PANEL2);roll2.setOnClickListener(new View.OnClickListener(){public void onClick(View v){JSONObject e=selectedExam[0];if(e==null||e.length()==0){toast("Escolha uma prova oficial.");return;}rollStrixhavenAcademicCheck(hero,e.optString("ability2"),value(skill2),e.optInt("dc2"),"Normal",test2Result,second,"Teste 2");}});f.addView(roll2);f.addView(test2Result);f.addView(second);
        f.addView(text("TRAPAÇA (OPCIONAL)",12,GOLD,true));f.addView(cheat);f.addView(text("Se trapacear, resolva os testes exigidos pela situação e marque os resultados abaixo. O runtime não inventa CDs que não estejam no dado compilado.",10,MUTED,false));f.addView(deception);f.addView(sleight);
        f.addView(text("Ao registrar, o resultado da prova, a tutoria, os Dados do Estudante e a Exaustão de virar a noite são aplicados automaticamente.",11,MUTED,false));
        new AlertDialog.Builder(this).setTitle("Prova • "+hero.optString("name")).setView(scrollableDialogView(f)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR RESULTADO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){JSONObject study=StrixhavenAcademicRulesEngine.studyResolution(studySuccess.isChecked(),group.isChecked(),allNight.isChecked(),skip.isChecked());JSONObject result=StrixhavenAcademicRulesEngine.examResolution(first.isChecked(),second.isChecked(),cheat.isChecked(),deception.isChecked(),sleight.isChecked());try{result.put("study",study);}catch(Exception ignored){}if(study.optInt("exhaustion_levels_due",0)>0){JSONObject sheet=parseLooseJson(hero.optString("sheet_json","{}"));try{sheet.put("exhaustion",Math.min(6,Math.max(0,sheet.optInt("exhaustion",0))+1));db.updateCharacterSheet(hero.optLong("id"),sheet.toString());}catch(Exception ignored){}}JSONObject sum=strixhavenProfile().recordExam(hero.optLong("id"),value(exam),value(skill1),value(skill2),result);db.addAuditEvent("strixhaven_academic","Prova registrada",hero.optString("name")+" • "+value(exam),study.toString(),result.toString(),true);toast("Resultado: "+academicExamResultPt(result.optString("result"))+" • Dados do Estudante +"+result.optInt("student_dice_gained",0)+(sum.optBoolean("tutoring_required",false)?" • TUTORIA OBRIGATÓRIA":""));}}).show();
    }'''
t=replace_method(t,'private void showStrixhavenExamDialog(final JSONObject hero)',exam)

extra=r'''private void showStrixhavenExtracurricularDialog(final JSONObject hero){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(12),dp(4),dp(12),dp(12));f.addView(text("Você pode manter até duas atividades sem emprego ou uma atividade com emprego. A entrada é registrada no início do ano acadêmico. Cada atividade concede um Dado do Estudante d4 aplicável às perícias informadas.",11,MUTED,false));
        final EditText name=labeledField(f,"Atividade extracurricular","",false),skill1=labeledField(f,"Perícia beneficiada 1","",false),skill2=labeledField(f,"Perícia beneficiada 2","",false),member=labeledField(f,"Colega estudante relacionado","",false);final CheckBox start=check("Ingresso no início do ano acadêmico",true);final Spinner relation=spinner(new String[]{"Ponto positivo (+1)","Ponto negativo (-1)"},"Ponto positivo (+1)");
        Button pickStudent=button("ESCOLHER COLEGA ESTUDANTE",PANEL2);pickStudent.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final String[] students=StrixhavenCompiledCampaignEngine.fellowStudentNames();new AlertDialog.Builder(MainActivity.this).setTitle("Colegas Estudantes de Strixhaven").setItems(students,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){member.setText(students[which]);}}).show();}});f.addView(pickStudent);f.addView(start);f.addView(cardWithLabel("RELAÇÃO COM O COLEGA",relation));
        new AlertDialog.Builder(this).setTitle("Atividade extracurricular • "+hero.optString("name")).setView(scrollableDialogView(f)).setNegativeButton("Cancelar",null).setNeutralButton("SAIR DA ATIVIDADE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){if(value(name).trim().length()==0){toast("Informe o nome da atividade que deseja deixar.");return;}strixhavenProfile().quitExtracurricular(hero.optLong("id"),value(name));toast("Atividade encerrada e seu Dado do Estudante removido.");}}).setPositiveButton("INGRESSAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){JSONArray skills=new JSONArray();if(value(skill1).length()>0)skills.put(value(skill1));if(value(skill2).length()>0)skills.put(value(skill2));JSONObject next=strixhavenProfile().joinExtracurricular(hero.optLong("id"),value(name),skills,value(member),start.isChecked(),relation.getSelectedItemPosition()==0?1:-1);if(next.optBoolean("academic_action_blocked",false))toast(academicBlockReasonPt(next.optString("academic_block_reason","")));else toast("Atividade registrada. Dado do Estudante d4 ativo nas perícias informadas.");}}).show();
    }'''
t=replace_method(t,'private void showStrixhavenExtracurricularDialog(final JSONObject hero)',extra)

job=r'''private void showStrixhavenJobDialog(final JSONObject hero){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(12),dp(4),dp(12),dp(12));f.addView(text("Um emprego ativo paga 5 po por semana. Se o estudante entrar em tutoria, o emprego fica suspenso até a tutoria terminar.",11,MUTED,false));
        final EditText employer=labeledField(f,"Local / empregador","",false),title=labeledField(f,"Função","",false),coworker=labeledField(f,"Colega estudante","",false);final Spinner relation=spinner(new String[]{"Ponto positivo (+1)","Ponto negativo (-1)"},"Ponto positivo (+1)");Button pickStudent=button("ESCOLHER COLEGA ESTUDANTE",PANEL2);pickStudent.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final String[] students=StrixhavenCompiledCampaignEngine.fellowStudentNames();new AlertDialog.Builder(MainActivity.this).setTitle("Colegas Estudantes de Strixhaven").setItems(students,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){coworker.setText(students[which]);}}).show();}});f.addView(pickStudent);f.addView(cardWithLabel("RELAÇÃO COM O COLEGA",relation));
        new AlertDialog.Builder(this).setTitle("Emprego • "+hero.optString("name")).setView(scrollableDialogView(f)).setNegativeButton("Cancelar",null).setNeutralButton("SAIR DO EMPREGO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){strixhavenProfile().quitJob(hero.optLong("id"));toast("Emprego encerrado.");}}).setPositiveButton("ASSUMIR EMPREGO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){JSONObject next=strixhavenProfile().setJob(hero.optLong("id"),value(employer),value(title),value(coworker),relation.getSelectedItemPosition()==0?1:-1);if(next.optBoolean("academic_action_blocked",false))toast(academicBlockReasonPt(next.optString("academic_block_reason","")));else toast("Emprego registrado. Salário: 5 po por semana.");}}).show();
    }'''
t=replace_method(t,'private void showStrixhavenJobDialog(final JSONObject hero)',job)

rel=r'''private void showStrixhavenRelationshipDialog(final JSONObject hero){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(12),dp(4),dp(12),dp(12));f.addView(text("Escolha um Colega Estudante oficial. Respostas amistosas somam pontos; respostas rivais retiram pontos. Com 2+ pontos positivos, a relação pode ser aprofundada para Pessoa Querida.",11,MUTED,false));
        final EditText npc=labeledField(f,"Colega estudante","",false),boon=labeledField(f,"Benefício do vínculo (se conhecido)","",true),bane=labeledField(f,"Desvantagem do vínculo (se conhecida)","",true);Button canonicalStudent=button("ESCOLHER COLEGA ESTUDANTE OFICIAL",PANEL2);canonicalStudent.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final String[] students=StrixhavenCompiledCampaignEngine.fellowStudentNames();new AlertDialog.Builder(MainActivity.this).setTitle("Colegas Estudantes de Strixhaven").setItems(students,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){JSONObject s=StrixhavenCompiledCampaignEngine.fellowStudent(students[which]);npc.setText(s.optString("name"));boon.setText(s.optString("bond_boon"));bane.setText(s.optString("bond_bane"));}}).show();}});f.addView(canonicalStudent);final Spinner delta=spinner(new String[]{"Resposta amistosa (+1)","Resposta rival (-1)"},"Resposta amistosa (+1)");final CheckBox beloved=check("Aprofundar Amigo para Pessoa Querida (exige 2+ pontos)",false);f.addView(cardWithLabel("RESPOSTA",delta));f.addView(beloved);
        new AlertDialog.Builder(this).setTitle("Relacionamento • "+hero.optString("name")).setView(scrollableDialogView(f)).setNegativeButton("Cancelar",null).setPositiveButton("APLICAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){if(value(npc).trim().length()==0){toast("Escolha ou informe um colega estudante.");return;}StrixhavenAdventureProfile p=strixhavenProfile();JSONObject next=p.adjustRelationship(hero.optLong("id"),value(npc),delta.getSelectedItemPosition()==0?1:-1,value(boon),value(bane));if(beloved.isChecked()){next=p.deepenBeloved(hero.optLong("id"),value(npc));if(next.optBoolean("academic_action_blocked",false)){toast("Relação atualizada, mas Pessoa Querida não foi aplicada: "+academicBlockReasonPt(next.optString("academic_block_reason","")));return;}}toast("Relacionamento atualizado.");}}).show();
    }'''
t=replace_method(t,'private void showStrixhavenRelationshipDialog(final JSONObject hero)',rel)

progress=r'''private void processStrixhavenOfficialProgress(JSONObject env,String narration){
        try{
            if(!isStrixhavenAdventure()||StrixhavenOfficialCampaignRuntime.finished(this))return;
            StrixhavenOfficialCampaignRuntime.Event ev=StrixhavenOfficialCampaignRuntime.current(this);int previousYear=strixhavenProfile().year();
            if(!StrixhavenOfficialCampaignRuntime.completionDeclared(env,narration,ev))return;
            JSONObject advanced=StrixhavenOfficialCampaignRuntime.advance(this,ev.id);if(!advanced.optBoolean("advanced",false))return;
            int target=advanced.optInt("level_after",0);JSONArray leveled=new JSONArray();
            if(target>0){for(JSONObject hero:db.listActiveCharacters()){JSONObject result=db.applyCharacterMilestoneLevelFixedHp(hero.optLong("id"),target);leveled.put(new JSONObject().put("id",hero.optLong("id")).put("name",hero.optString("name")).put("target",target).put("applied",result.optBoolean("milestone_level_applied",false)));}}
            int nextYear=previousYear,nextSemester=strixhavenProfile().semester();boolean finished=StrixhavenOfficialCampaignRuntime.finished(this);
            if(!finished){StrixhavenOfficialCampaignRuntime.Event next=StrixhavenOfficialCampaignRuntime.current(this);nextYear=next.year;nextSemester=next.semester;}else{nextYear=4;nextSemester=2;}
            if(nextYear>previousYear||finished){for(JSONObject hero:db.listActiveCharacters())strixhavenProfile().finishAcademicYear(hero.optLong("id"));}
            strixhavenProfile().setAcademicPeriod(nextYear,nextSemester);
            JSONObject after=StrixhavenOfficialCampaignRuntime.progress(this);String finishedTitle=StrixhavenCompiledCampaignEngine.localizeCampaignLabel(ev.title);db.addAuditEvent("strixhaven_official","Marco oficial concluído",finishedTitle,env==null?"{}":env.toString(),new JSONObject(after.toString()).put("leveled",leveled).toString(),true);db.addJournalFact("Strixhaven • "+finishedTitle,"Evento oficial concluído. Progresso: "+after.optInt("completed_events")+"/"+after.optInt("total")+".","Strixhaven","strixhaven-event-"+ev.id,"compiled_runtime");setStatus(finished?"Campanha oficial de Strixhaven concluída.":"Strixhaven • próximo marco: "+StrixhavenCompiledCampaignEngine.localizeCampaignLabel(StrixhavenOfficialCampaignRuntime.current(this).title));
        }catch(Exception e){AppLog.e("StrixhavenOfficial","Falha ao aplicar progresso oficial.",e);}
    }'''
t=replace_method(t,'private void processStrixhavenOfficialProgress(JSONObject env,String narration)',progress)

# UI remnants / cards.
t=t.replace('RUNTIMES COMPILADOS — RUN163','RUNTIMES COMPILADOS — EBERRON / HERÓIS DE FAERÛN')
t=t.replace('A interface não calcula efeitos nem rola dados. Informe manualmente alvos, resultados, escolhas e confirmações exigidos pela regra; o gateway valida e só então persiste o estado.','Compatibilidade de regras compiladas. Quando uma ação exigir alvo, escolha ou resultado externo, informe somente o que a regra pedir; o gateway valida antes de persistir o estado.')
t=t.replace('officialProgress.optString("title","")','StrixhavenCompiledCampaignEngine.localizeCampaignLabel(officialProgress.optString("title",""))')
t=t.replace('"CAMPANHA OFICIAL • A CURRICULUM OF CHAOS"','"CAMPANHA OFICIAL • UM CURRÍCULO DE CAOS"')
t=t.replace('" • 18 Fellow Students. O PDF não é necessário para estes dados."','" • 18 Colegas Estudantes. O PDF não é necessário para estes dados."')
t=t.replace('Strixhaven — A Curriculum of Chaos','Strixhaven — Um Currículo de Caos')
write('MainActivity.java',t)

# ---------------- Compiled campaign: PT-BR display labels ----------------
t=read('StrixhavenCompiledCampaignEngine.java')
repl={
'"Campus Kerfuffle"':'"Confusão no Campus"','"First Day on Campus"':'"Primeiro Dia no Campus"','"Orientation Challenge"':'"Desafio de Orientação"','"Frog\'s the Word"':'"O Assunto é Sapo"','"Magical Physiologies — Exam: Slaadi"':'"Fisiologias Mágicas — Prova: Slaadi"','"Work Hard, Play Harder"':'"Estude Muito, Divirta-se Mais"','"Magical Physiologies — Exam: Owlbears"':'"Fisiologias Mágicas — Prova: Ursocorujas"','"Campus Daredevils"':'"Audaciosos do Campus"','"Magical Physiologies — Exam: Otyughs"':'"Fisiologias Mágicas — Prova: Otyughs"','"All the World\'s a Stage"':'"O Mundo Inteiro é um Palco"','"Secrets in Sedgemoor"':'"Segredos em Sedgemoor"',
'"Hunt for Mage Tower"':'"Caçada à Torre dos Magos"','"Back in Session"':'"De Volta às Aulas"','"Mascots Everywhere!"':'"Mascotes por Toda Parte!"','"Scrivening and Symbology — Exam: Glyph of Warding"':'"Escrita Mágica e Simbologia — Prova: Glifo de Proteção"','"Party at the Rose Stage"':'"Festa no Palco da Rosa"','"Practice Makes Perfect"':'"A Prática Leva à Perfeição"','"Scrivening and Symbology — Exam: Symbol Glyphs"':'"Escrita Mágica e Simbologia — Prova: Glifos de Símbolo"','"Getting Some Air"':'"Tomando um Pouco de Ar"','"Dangerous Knowledge"':'"Conhecimento Perigoso"','"Scrivening and Symbology — Exam: Augury"':'"Escrita Mágica e Simbologia — Prova: Augúrio"','"Tell Us in Song!"':'"Conte-nos Cantando!"','"Battle of Strixhaven"':'"Batalha de Strixhaven"',
'"The Magister\'s Masquerade"':'"O Baile de Máscaras do Magíster"','"Cordially Invited"':'"Cordialmente Convidado"','"Reception at Bow\'s End"':'"Recepção no Bow\'s End"','"Fate at Furygale"':'"Destino em Furygale"','"Advanced Arcane History — Exam: Relics"':'"História Arcana Avançada — Prova: Relíquias"','"Dressing for Success"':'"Vestindo-se para o Sucesso"','"Party Preparations"':'"Preparativos para a Festa"','"Advanced Arcane History — Exam: Devices"':'"História Arcana Avançada — Prova: Dispositivos"','"Suspicions"':'"Suspeitas"','"Fancying Up Footwork"':'"Refinando os Passos"','"Preparing the Hall"':'"Preparando o Salão"','"Advanced Arcane History — Exam: Unearthed Arcana"':'"História Arcana Avançada — Prova: Arcana Desenterrada"','"A Starlit Night"':'"Uma Noite Estrelada"','"Midnight at Lorehold"':'"Meia-Noite em Lorehold"','"Aftermath"':'"Consequências"',
'"A Reckoning in Ruins"':'"Acerto de Contas nas Ruínas"','"A Sinister Shadow"':'"Uma Sombra Sinistra"','"A Brief Distraction"':'"Uma Breve Distração"','"Arcane Herbology — Exam: Vegetation Identification"':'"Herbologia Arcana — Prova: Identificação de Vegetação"','"Clue in the Bayou"':'"Pista no Bayou"','"No Time to Lose"':'"Sem Tempo a Perder"','"Into the Badlands"':'"Rumo às Terras Áridas"','"This Is Surreal, Isn\'t It?"':'"Isto é Surreal, Não é?"','"Ruins of Caerdoon"':'"Ruínas de Caerdoon"','"Confronting Murgaxor"':'"Confrontando Murgaxor"',
'"Magical Physiologies"':'"Fisiologias Mágicas"','"Owlbears"':'"Ursocorujas"','"Scrivening and Symbology"':'"Escrita Mágica e Simbologia"','"Glyph of Warding"':'"Glifo de Proteção"','"Symbol Glyphs"':'"Glifos de Símbolo"','"Augury"':'"Augúrio"','"Advanced Arcane History"':'"História Arcana Avançada"','"Relics"':'"Relíquias"','"Devices"':'"Dispositivos"','"Unearthed Arcana"':'"Arcana Desenterrada"','"Arcane Herbology"':'"Herbologia Arcana"','"Vegetation Identification"':'"Identificação de Vegetação"','"Arcana ou História"':'"Arcanismo ou História"','"Arcana"':'"Arcanismo"'
}
for a,b in repl.items(): t=t.replace(a,b)

localizer=r'''
    public static String localizeCampaignLabel(String raw){
        if(raw==null)return "";String x=raw;
        String[][] map={
            {"First Day on Campus","Primeiro Dia no Campus"},{"Orientation Challenge","Desafio de Orientação"},{"Frog’s the Word","O Assunto é Sapo"},{"Frog's the Word","O Assunto é Sapo"},{"Exam: Slaadi","Prova: Slaadi"},{"Work Hard, Play Harder","Estude Muito, Divirta-se Mais"},{"Exam: Owlbears","Prova: Ursocorujas"},{"Campus Daredevils","Audaciosos do Campus"},{"Exam: Otyughs","Prova: Otyughs"},{"All the World’s a Stage","O Mundo Inteiro é um Palco"},{"All the World's a Stage","O Mundo Inteiro é um Palco"},{"Secrets in Sedgemoor","Segredos em Sedgemoor"},
            {"Back in Session","De Volta às Aulas"},{"Mascots Everywhere!","Mascotes por Toda Parte!"},{"Exam: Glyph of Warding","Prova: Glifo de Proteção"},{"Party at the Rose Stage","Festa no Palco da Rosa"},{"Practice Makes Perfect","A Prática Leva à Perfeição"},{"Exam: Symbol Glyphs","Prova: Glifos de Símbolo"},{"Getting Some Air","Tomando um Pouco de Ar"},{"Dangerous Knowledge","Conhecimento Perigoso"},{"Exam: Augury","Prova: Augúrio"},{"Tell Us in Song!","Conte-nos Cantando!"},{"Battle of Strixhaven","Batalha de Strixhaven"},
            {"Cordially Invited","Cordialmente Convidado"},{"Reception at Bow’s End","Recepção no Bow’s End"},{"Reception at Bow's End","Recepção no Bow's End"},{"Fate at Furygale","Destino em Furygale"},{"Exam: Relics","Prova: Relíquias"},{"Dressing for Success","Vestindo-se para o Sucesso"},{"Party Preparations","Preparativos para a Festa"},{"Exam: Devices","Prova: Dispositivos"},{"Suspicions","Suspeitas"},{"Fancying Up Footwork","Refinando os Passos"},{"Preparing the Hall","Preparando o Salão"},{"Exam: Unearthed Arcana","Prova: Arcana Desenterrada"},{"A Starlit Night","Uma Noite Estrelada"},{"Midnight at Lorehold","Meia-Noite em Lorehold"},{"Aftermath","Consequências"},
            {"A Sinister Shadow","Uma Sombra Sinistra"},{"A Brief Distraction","Uma Breve Distração"},{"Vegetation Identification","Identificação de Vegetação"},{"Clue in the Bayou","Pista no Bayou"},{"No Time to Lose","Sem Tempo a Perder"},{"Into the Badlands","Rumo às Terras Áridas"},{"This Is Surreal, Isn’t It?","Isto é Surreal, Não é?"},{"This Is Surreal, Isn't It?","Isto é Surreal, Não é?"},{"Ruins of Caerdoon","Ruínas de Caerdoon"},{"Confronting Murgaxor","Confrontando Murgaxor"}
        };
        for(String[] p:map)if(p[0].equals(x))return p[1];return x;
    }
'''
anchor='    public static JSONObject coverage(){'
if anchor not in t: raise SystemExit('COMPILED_COVERAGE_ANCHOR_MISSING')
t=t.replace(anchor,localizer+'\n'+anchor,1)
write('StrixhavenCompiledCampaignEngine.java',t)

# Static sanity contracts.
main=read('MainActivity.java'); comp=read('StrixhavenCompiledCampaignEngine.java')
checks={
 'guided-rolls':'ROLAR TESTE 1' in main and 'ROLAR ESTUDO' in main and 'rollStrixhavenAcademicCheck' in main,
 'block-reasons-pt':'academicBlockReasonPt' in main and 'job_wage_not_active' in main,
 'year-auto-cleanup':'finishAcademicYear(hero.optLong("id"))' in main and 'COMO AVANÇAR SEMESTRE / ANO' in main,
 'master-clear':'CONSULTAR O MESTRE SEM AVANÇAR A HISTÓRIA' in main and 'APLICAR PERFIL' in main,
 'legacy-run163-label-removed':'RUNTIMES COMPILADOS — RUN163' not in main,
 'relationship-pt':'Pessoa Querida' in main and 'ESCOLHER COLEGA ESTUDANTE OFICIAL' in main,
 'campaign-pt':'UM CURRÍCULO DE CAOS' in main and 'localizeCampaignLabel' in comp,
 'exam-pt':'Fisiologias Mágicas' in comp and 'Arcanismo' in comp,
}
print(checks)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit('RUN186_STATIC_FAIL::'+','.join(bad))
print('RUN186_PATCH_OK')
