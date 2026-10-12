"""Place grid-wide infrastructure, persisting location through construction."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Grid anchor count='+str(s.count(a))+' '+a[:80])
 return s.replace(a,b,1)
def repository_grid(s):
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start);part=s[start:end]
 old='''        if(def.optBoolean("requires_special_placement",false))
            throw new IllegalStateException("Estrutura de implantação especial: selecione o tipo/local exigido pelo manual Kingmaker.");'''
 new='''        if(KingmakerStructureSemantics.isGridInfrastructure(chosenKey))
            KingmakerStructureSemantics.requireGridInfrastructure(town,new JSONArray(structures(townKey)),chosenKey,buildOptions);
        else if(def.optBoolean("requires_special_placement",false))
            throw new IllegalStateException("Estrutura de implantação especial: selecione o tipo/local exigido pelo manual Kingmaker.");'''
 part=once(part,old,new)
 part=once(part,'new String[]{"trade_focus","ruin"}','new String[]{"trade_focus","ruin","urban_grid"}')
 s=s[:start]+part+s[end:]
 anchor='effects.put("official_build_effects",buildEffects);'
 s=once(s,anchor,anchor+'''
                if(KingmakerStructureSemantics.isGridInfrastructure(k))effects.put("infrastructure_instances",KingmakerStructureSemantics.appendedInfrastructure(existing,k,buildOptions));''')
 # A multiple-instance row loses its last registered installation on demolition.
 start=s.index('    public JSONObject demolishStructure(');end=s.index('    public JSONObject repairStructure(',start);part=s[start:end]
 anchor='                    v.put("count", count - 1);'
 part=once(part,anchor,anchor+'''
                    v.put("effects_json",KingmakerStructureSemantics.trimInstanceEffects(existing,count-1).toString());''')
 s=s[:start]+part+s[end:]
 # Repair changes condition, not placement, and uses the persisted instance.
 start=s.index('    public JSONObject resolveRepairStructure(String settlementKey,String structureKey,int naturalRoll');end=s.find('\n    public ',s.index('final JSONObject oldEffects=',start));part=s[start:end]
 old='if(def.optBoolean("requires_special_placement",false))'
 part=once(part,old,'if(def.optBoolean("requires_special_placement",false)&&!KingmakerStructureSemantics.isGridInfrastructure(structure))')
 s=s[:start]+part+s[end:]
 return s
def grid_ui(s):
 anchor='        final EditText tradeFocus=labeledField(form,"Foco comercial (Salão de Guilda / Loja de Ofício)","",false);'
 s=once(s,anchor,'''        final EditText urbanGrid=labeledField(form,"Malha urbana nº (iluminação / ruas / esgoto)","1",false);
        urbanGrid.setInputType(InputType.TYPE_CLASS_NUMBER);
'''+anchor)
 anchor='.put("trade_focus",value(tradeFocus))'
 s=once(s,anchor,anchor+'.put("urban_grid",num(urbanGrid,0))')
 old='Implantações especiais exigem uma regra espacial ainda não homologada e serão bloqueadas.'
 new='Iluminação mágica e ruas são instaladas na malha indicada; esgoto atende todo o assentamento. Pontes e muralhas exigem posição de borda e permanecem bloqueadas neste formulário.'
 s=once(s,old,new)
 return s
