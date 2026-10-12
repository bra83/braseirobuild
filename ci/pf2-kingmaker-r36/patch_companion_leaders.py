"""Expose the actual companion roster and keep one office per companion."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError("Companion leader anchor count="+str(s.count(a))+" "+a[:80])
 return s.replace(a,b,1)
def companion_leaders_repository(s):
 s=once(s,'if("companion".equals(entityKind)||leader.length()>0){','if("companion".equals(entityKind)||"npc".equals(entityKind)){')
 s=once(s,'officialAutomaticInvestmentForName(leader,roleKey)','officialAutomaticInvestmentForName(leader,roleKey,entityKind)')
 s=once(s,'private boolean officialAutomaticInvestmentForName(String name,String role){','private boolean officialAutomaticInvestmentForName(String name,String role,String entityKind){\n        if(!"companion".equals(entityKind)&&!"npc".equals(entityKind))return false;')
 s=once(s,'private boolean officialAutomaticInvestment(JSONObject leader){','private boolean officialAutomaticInvestment(JSONObject leader){\n        if(!"companion".equals(leader.optString("entity_kind"))&&!"npc".equals(leader.optString("entity_kind")))return false;')
 anchor='            if(comp.length()>0&&!KingmakerCompanionSemantics.canLead(comp,state().optString("reputation_mode","fame")))'
 insert='''            if(comp.length()>0){for(JSONObject occupied:leaders()){
                String occupiedKind=occupied.optString("entity_kind");
                if(!roleKey.equals(occupied.optString("role_key"))&&occupied.optInt("vacant")==0&&("companion".equals(occupiedKind)||"npc".equals(occupiedKind))&&(comp.optString("name").equalsIgnoreCase(occupied.optString("leader_name"))||comp.optString("companion_key").equals(occupied.optString("leader_name"))))throw new IllegalStateException("Este companheiro já ocupa outro cargo de liderança.");
            }}
'''
 return once(s,anchor,insert+anchor)
def companion_leaders_ui(s):
 start=s.index('    private void showKingmakerLeaderEditDialog(');end=s.index('\n    private ',start+20);part=s[start:end]
 anchor='JSONArray npcs=db.listNpcMemories(100);'
 replacement='''final java.util.Set<String> companionNames=new java.util.HashSet<String>();for(JSONObject companion:kingmakerRepository.companions())if(companion.optInt("available",1)==1){String display=companion.optString("name");if(!companionNames.add(display.toLowerCase(java.util.Locale.ROOT)))continue;candidates.add(new JSONObject().put("label","COMPANHEIRO • "+display).put("kind","companion").put("id",companion.optLong("id")).put("name",display));}
        JSONArray npcs=db.listNpcMemories(100);'''
 part=once(part,anchor,replacement)
 part=once(part,'if(npc!=null)candidates.add','if(npc!=null&&!companionNames.contains(npc.optString("name").toLowerCase(java.util.Locale.ROOT)))candidates.add')
 part=once(part,'if(role.optString("leader_name").equals(candidates.get(i).optString("name")))selected=i;','if(role.optString("entity_kind").equals(candidates.get(i).optString("kind"))&&(role.optLong("entity_id")>0?role.optLong("entity_id")==candidates.get(i).optLong("id"):role.optString("leader_name").equals(candidates.get(i).optString("name"))))selected=i;')
 return s[:start]+part+s[end:]
