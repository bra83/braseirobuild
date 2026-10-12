"""Use compiled primary-book enemy statistics in the live combat plan."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Mansion encounter anchor '+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def mansion_ui(s):
 anchor='        if("start".equals(action)){'
 start=s.index('    private String applyCombatPlan(');end=s.index('    private String buildHeroAiContext(',start);part=s[start:end]
 part=once(part,anchor,anchor+'''
            String room=combat.optString("canonical_room");
            if(room.length()>0){
                if(kingmakerService==null||!kingmakerService.enabled()||!KingmakerPreludePolicy.isMansionPhase(kingmakerRepository.campaignStoryPhase()))throw new IllegalStateException("Encontros da mansão exigem presença no prólogo Kingmaker.");
                JSONObject encounter=KingmakerCampaignGate.mansionEncounter(room);
                if("A1".equals(room)&&"perception".equals(combat.optString("initiative_mode")))encounter.optJSONArray("enemies").optJSONObject(0).put("initiative_bonus",3);
                combat.put("enemies",encounter.optJSONArray("enemies")).put("source_ref",encounter.optString("source_ref"));
            }
''')
 part=once(part,'hp,hp,ac,e.optString("conditions"),false,"",0,e.optString("token_uri",""))','hp,Math.max(hp,Math.min(999,e.optInt("max_hp",hp))),ac,e.optString("conditions"),false,"",0,e.optString("token_uri",""))')
 anchor='JSONObject enemyResources=new JSONObject().put("block_id",e.optString("block_id")).put("attacks",e.optString("attacks")).put("speed",e.optString("speed"));'
 part=once(part,anchor,anchor+'''
                    if(e.optJSONObject("official_stats")!=null)enemyResources.put("official_stats",new JSONObject(e.optJSONObject("official_stats").toString())).put("source_ref",e.optString("source_ref"));
''')
 s=s[:start]+part+s[end:]
 anchor='p.append("MUNDO VIVO ESTRUTURADO:'
 s=once(s,anchor,'if(kingmakerService!=null&&kingmakerService.enabled())p.append("KINGMAKER NA MANSÃO: combat.start usa canonical_room A1, A4, A6, A11, A21 ou A23. O motor fornece as fichas, contagens e condições do livro. Não invente PV, CA ou ataques desses adversários. Gromog está cego e usa suas estatísticas especiais; Jurgrindor inicia ferido com 25/50 PV. Não mova essas batalhas para fora da mansão.\\n");'+anchor)
 start=s.index('    private void showKingmakerCampaignJournal()');end=s.index('    private void showKingmakerQuestObjectives(',start);part=s[start:end]
 part=once(part,'.setNegativeButton("Voltar",null).show();','.setNegativeButton("Voltar",null).setNeutralButton("MAPAS DA MANSÃO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){showKingmakerManorMaps();}}).show();');s=s[:start]+part+s[end:]
 helper='''
    private void showKingmakerManorMaps(){
        new AlertDialog.Builder(this).setTitle("Mansão Aldori • mapas oficiais").setItems(new String[]{"Ala leste • A1–A13","Ala oeste • A14–A24"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
            android.webkit.WebView map=new android.webkit.WebView(MainActivity.this);map.getSettings().setJavaScriptEnabled(false);map.getSettings().setBuiltInZoomControls(true);map.getSettings().setDisplayZoomControls(false);map.getSettings().setUseWideViewPort(true);map.getSettings().setLoadWithOverviewMode(true);map.setMinimumHeight(dp(450));
            String asset=which==0?"manor_east_primary.jpg":"manor_west_primary.jpg";
            map.loadDataWithBaseURL("file:///android_asset/pathfinder/kingmaker/maps/","<html><head><meta name='viewport' content='width=device-width, initial-scale=1'></head><body style='margin:0;background:#fff'><img style='width:100%' src='"+asset+"'></body></html>","text/html","UTF-8",null);
            new AlertDialog.Builder(MainActivity.this).setTitle(which==0?"Ala leste • quadrado = 1,5 m":"Ala oeste • quadrado = 1,5 m").setView(map).setPositiveButton("FECHAR",null).show();
        }}).setNegativeButton("Voltar",null).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
