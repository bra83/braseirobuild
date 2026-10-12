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
 return s
