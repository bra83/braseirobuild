from pathlib import Path
import re, sys, json

root = Path(sys.argv[1])
pkg = root / "android" / "app" / "src" / "main" / "java" / "com" / "braseiro" / "dnd5e"
if not pkg.exists():
    raise SystemExit(f"Java source dir not found: {pkg}")

def read(name):
    return (pkg / name).read_text(encoding="utf-8")

def write(name, text):
    (pkg / name).write_text(text, encoding="utf-8", newline="\n")

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f"PATCH_MISSING::{label}")
    if text.count(old) != 1:
        raise SystemExit(f"PATCH_AMBIGUOUS::{label}::{text.count(old)}")
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------
# 1) Validator: same authority as catalog/runtime, class-safe subclass gate.
# ---------------------------------------------------------------------
name = "CharacterRulesValidator.java"
t = read(name)

old = '''            String cls = lower(normalized.optString("class_name"));
            String species = lower(normalized.optString("species"));
            if (cls.length() > 0 && !containsKnown(CLASSES, cls) && !SupplementContentRegistry.isKnownClass(cls))
                errors.put("Classe não reconhecida pelo núcleo ou pelos suplementos compilados: " + normalized.optString("class_name") + ".");
            if (species.length() > 0 && !containsKnown(SPECIES, species)
'''
new = '''            String rawClass = normalized.optString("class_name");
            String baseClassName = baseClassName(rawClass);
            String cls = lower(baseClassName);
            String species = lower(normalized.optString("species"));
            JSONObject classMeta = CharacterOptionCatalogEngine.lookupClass(baseClassName);
            boolean compiledSupplementClass = classMeta.optBoolean("known", false)
                    && (classMeta.optBoolean("mechanics_compiled", false) || classMeta.optBoolean("base_class_mechanics_compiled", false));
            if (cls.length() > 0 && !containsKnown(CLASSES, cls) && !compiledSupplementClass && !SupplementContentRegistry.isKnownClass(cls))
                errors.put("Classe não reconhecida pelo núcleo ou pelos suplementos compilados: " + baseClassName + ".");
            if (species.length() > 0 && !containsKnown(SPECIES, species)
'''
t = replace_once(t, old, new, "validator-class-catalog-gate")

old = '''            int dash=cls.indexOf(" — ");if(dash<0)dash=cls.indexOf(" - ");String base=dash>0?cls.substring(0,dash).trim():cls;boolean hasSubclass=dash>0;
            int subclassLevel=revised?3:(base.contains("clérigo")||base.contains("clerigo")||base.contains("bruxo")||base.contains("feiticeiro")?1:(base.contains("druida")||base.contains("mago")?2:3));
            if(hasSubclass&&level<subclassLevel)errors.put("A subclasse foi registrada antes do nível permitido neste perfil.");
            if(!revised&&level>=subclassLevel&&!hasSubclass&&(base.contains("clérigo")||base.contains("clerigo")||base.contains("bruxo")||base.contains("feiticeiro")))errors.put("Esta classe de 2014 precisa registrar sua subclasse no nível 1.");
'''
new = '''            String rawSubclass=subclassName(rawClass);String base=lower(baseClassName);boolean hasSubclass=rawSubclass.length()>0;
            int subclassLevel=subclassMinimumLevel(baseClassName,profile);
            if(hasSubclass&&level<subclassLevel)errors.put("A subclasse foi registrada antes do nível permitido neste perfil.");
            if(hasSubclass&&level>=subclassLevel&&!isCoreSubclassFor(baseClassName,rawSubclass)){
                JSONObject subMeta=CharacterOptionCatalogEngine.validateSubclass(baseClassName,rawSubclass);
                if(!subMeta.optBoolean("selection_allowed",false))errors.put("A subclasse não pertence à classe selecionada ou não está disponível: "+rawSubclass+".");
                else if(!subMeta.optBoolean("mechanics_compiled",false))errors.put("A subclasse existe, mas suas mecânicas ainda não estão compiladas para uso: "+rawSubclass+".");
            }
            if(!revised&&level>=subclassLevel&&!hasSubclass&&(base.contains("clérigo")||base.contains("clerigo")||base.contains("bruxo")||base.contains("feiticeiro")))errors.put("Esta classe de 2014 precisa registrar sua subclasse no nível 1.");
'''
t = replace_once(t, old, new, "validator-subclass-gate")

anchor = '''    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
'''
helpers = '''    private static String baseClassName(String value) {
        String raw=value==null?"":value.trim();int dash=raw.indexOf(" — ");if(dash<0)dash=raw.indexOf(" - ");return dash>0?raw.substring(0,dash).trim():raw;
    }
    private static String subclassName(String value) {
        String raw=value==null?"":value.trim();int dash=raw.indexOf(" — ");int size=3;if(dash<0){dash=raw.indexOf(" - ");size=3;}return dash>0?raw.substring(dash+size).trim():"";
    }
    private static int subclassMinimumLevel(String cls,String profile) {
        String c=fold(cls);boolean revised=profile!=null&&(profile.contains("2024")||profile.startsWith("5.5"));
        if(revised)return 3;
        if(c.contains("clerigo")||c.contains("bruxo")||c.contains("feiticeiro"))return 1;
        if(c.contains("druida")||c.contains("mago"))return 2;
        return 3;
    }
    private static boolean isCoreSubclassFor(String cls,String subclass) {
        String c=fold(cls),s=fold(subclass);
        if(c.contains("barbar"))return oneOf(s,"berserker","coracao selvagem","arvore do mundo","fanatico","guerreiro totemico");
        if(c.contains("bardo"))return oneOf(s,"colegio da danca","colegio do glamour","colegio do conhecimento","colegio do valor");
        if(c.contains("bruxo"))return oneOf(s,"patrono arquifada","patrono celestial","patrono corruptor","patrono grande antigo","arquifada","corruptor","grande antigo");
        if(c.contains("clerigo"))return oneOf(s,"dominio da guerra","dominio da luz","dominio da trapaca","dominio da vida");
        if(c.contains("druida"))return oneOf(s,"circulo da lua","circulo da terra","circulo das estrelas","circulo do mar");
        if(c.contains("feiticeiro"))return oneOf(s,"feiticaria aberrante","feiticaria draconica","feiticaria do caos mecanico","magia selvagem","linhagem draconica");
        if(c.contains("ladino"))return oneOf(s,"trapaceiro arcano","assassino","alma faca","ladrao");
        if(c.contains("guerreiro"))return oneOf(s,"mestre de batalha","campeao","cavaleiro arcano","guerreiro psionico");
        if(c.contains("mago"))return oneOf(s,"abjurador","adivinho","evocador","ilusionista","abjuracao","adivinhacao","conjuracao","encantamento","evocacao","ilusao","necromancia","transmutacao");
        if(c.contains("monge"))return oneOf(s,"guerreiro da misericordia","guerreiro da mao aberta","guerreiro das sombras","guerreiro dos elementos","caminho da mao aberta","caminho da sombra","caminho dos quatro elementos");
        if(c.contains("paladino"))return oneOf(s,"juramento da devocao","juramento da gloria","juramento da vinganca","juramento dos ancioes");
        if(c.contains("patrulheiro"))return oneOf(s,"andarilho feerico","cacador","mestre das feras","perseguidor sombrio");
        return false;
    }
    private static boolean oneOf(String value,String... allowed){for(String x:allowed)if(value.equals(x))return true;return false;}
    private static String fold(String value){return java.text.Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}

''' + anchor
t = replace_once(t, anchor, helpers, "validator-helpers")
write(name, t)

# ---------------------------------------------------------------------
# 2) Generator: one class authority + no fake subclass placeholder.
# ---------------------------------------------------------------------
name = "CharacterSheetGenerator.java"
t = read(name)

old = '''            String cls = chooseBalanced(CLASSES, existing, "class_name");
'''
new = '''            String cls = chooseBalanced(selectableClasses(profile), existing, "class_name");
'''
t = replace_once(t, old, new, "generator-random-class-authority")

old = '''        else if (c.contains("cozinheiro")) options = new String[]{"Sous Chef","Sargento Alimentar","Mestre-Cuca","Mestre Cervejeiro","Amassador de Monstros"};
        else options = new String[]{"Subclasse disponível nos manuais habilitados"};
        if(!revised){JSONArray supplemental=CharacterOptionCatalogEngine.subclassesFor(cls);if(supplemental.length()>0)options=mergeSubclassOptions(options,supplemental);}
        return options[source.nextInt(options.length)];
'''
new = '''        else if (c.contains("cozinheiro")) options = new String[]{"Sous Chef","Sargento Alimentar","Mestre-Cuca","Mestre Cervejeiro","Amassador de Monstros"};
        else options = new String[0];
        JSONArray supplemental=CharacterOptionCatalogEngine.subclassesFor(cls);if(supplemental.length()>0)options=mergeCompiledSubclassOptions(cls,options,supplemental);
        return options.length==0?"":options[source.nextInt(options.length)];
'''
t = replace_once(t, old, new, "generator-no-fake-subclass")

old = '''    private String[] mergeSubclassOptions(String[] base,JSONArray extra){ArrayList<String> out=new ArrayList<String>();for(String x:base)if(x!=null&&!out.contains(x))out.add(x);for(int i=0;i<extra.length();i++){String x=extra.optString(i);if(x!=null&&x.length()>0&&!out.contains(x))out.add(x);}return out.toArray(new String[0]);}
'''
new = '''    private String[] mergeSubclassOptions(String[] base,JSONArray extra){ArrayList<String> out=new ArrayList<String>();for(String x:base)if(x!=null&&!out.contains(x))out.add(x);for(int i=0;i<extra.length();i++){String x=extra.optString(i);if(x!=null&&x.length()>0&&!out.contains(x))out.add(x);}return out.toArray(new String[0]);}
    private String[] mergeCompiledSubclassOptions(String cls,String[] base,JSONArray extra){ArrayList<String> out=new ArrayList<String>();for(String x:base)if(x!=null&&!out.contains(x))out.add(x);for(int i=0;i<extra.length();i++){String x=extra.optString(i);if(x==null||x.trim().isEmpty()||out.contains(x))continue;JSONObject gate=CharacterOptionCatalogEngine.validateSubclass(cls,x);if(gate.optBoolean("selection_allowed",false)&&gate.optBoolean("mechanics_compiled",false))out.add(x);}return out.toArray(new String[0]);}

    public String[] selectableClasses(String profile){
        ArrayList<String> out=new ArrayList<String>();for(String c:CLASSES)if(c!=null&&!containsNormalized(out,c))out.add(c);
        JSONArray extra=CharacterOptionCatalogEngine.allSupplementClasses();for(int i=0;i<extra.length();i++){String c=extra.optString(i);JSONObject meta=CharacterOptionCatalogEngine.lookupClass(c);if(meta.optBoolean("known",false)&&(meta.optBoolean("mechanics_compiled",false)||meta.optBoolean("base_class_mechanics_compiled",false))&&!containsNormalized(out,c))out.add(c);}
        if(!containsNormalized(out,"Artífice"))out.add("Artífice");
        return out.toArray(new String[0]);
    }

    public JSONObject recomputeMechanics(JSONObject source){
        JSONObject o;try{o=source==null?new JSONObject():new JSONObject(source.toString());applyMechanics(o);return ensureComplete(o);}catch(Exception e){return source==null?new JSONObject():source;}
    }
'''
t = replace_once(t, old, new, "generator-class-api-and-recompute")

write(name, t)

# ---------------------------------------------------------------------
# 3) PT-BR: translate source labels without inventing proper-name translations.
# ---------------------------------------------------------------------
name = "DndPtBrLocalizer.java"
t = read(name)
anchor = '''        add("INVENTORY AND EQUIPMENT", "INVENTÁRIO E EQUIPAMENTO");
'''
new = '''        add("Volo's Complete Subrace Handbook", "Manual Completo de Sub-raças de Volo");
        add("Volo Subrace Handbook", "Manual de Sub-raças de Volo");
        add("Subrace Handbook", "Manual de Sub-raças");
''' + anchor
t = replace_once(t, anchor, new, "ptbr-volo-source-labels")
write(name, t)

# ---------------------------------------------------------------------
# 4) UI: dynamic class pool + reactive dependencies + level/class subclass gating.
# ---------------------------------------------------------------------
name = "MainActivity.java"
t = read(name)

old = '''        final String[] classes={"Bárbaro","Bardo","Bruxo","Clérigo","Druida","Feiticeiro","Guerreiro","Ladino","Mago","Monge","Paladino","Patrulheiro","Artífice","Cozinheiro","Caçador de Sangue","Necromante","Cronomancer","Cavaleiro da Morte","Xamã","Atirador"};f.cls=spinner(classes,baseClass(initial.optString("class_name","Guerreiro")));form.addView(spinnerDiceCard("CLASSE",f.cls,new Runnable(){public void run(){f.cls.setSelection(new Random().nextInt(classes.length));}},"character_identity","character_skills",f,rulesProfile));f.subclass=labeledRuleField(form,"Subclasse (somente no nível permitido)",subclassOf(initial.optString("class_name")),false,"subclass",f,rulesProfile);
'''
new = '''        final String[] classes=characterGenerator.selectableClasses(rulesProfile);f.cls=spinner(classes,baseClass(initial.optString("class_name","Guerreiro")));form.addView(spinnerDiceCard("CLASSE",f.cls,new Runnable(){public void run(){f.cls.setSelection(new Random().nextInt(classes.length));}},"character_identity","character_skills",f,rulesProfile));f.subclass=labeledRuleField(form,"Subclasse (somente no nível permitido)",subclassOf(initial.optString("class_name")),false,"subclass",f,rulesProfile);
'''
t = replace_once(t, old, new, "ui-class-authority")

old = '''        f.active=check("Personagem em jogo",edit?existing.optInt("active",1)==1:initial.optBoolean("active",true));form.addView(f.active);configureSelectableMechanicFields(f,rulesProfile);scroll.addView(form);
'''
new = '''        f.active=check("Personagem em jogo",edit?existing.optInt("active",1)==1:initial.optBoolean("active",true));form.addView(f.active);configureSelectableMechanicFields(f,rulesProfile);bindCharacterDependencyListeners(f,rulesProfile);scroll.addView(form);
'''
t = replace_once(t, old, new, "ui-bind-dependencies")

old = '''    private ArrayList<String> ruleFieldChoices(String fieldKey,CharacterFormRefs refs,String profile,String current){ArrayList<String> out=new ArrayList<String>();if(current!=null&&current.trim().length()>0)out.add(current.trim());if("level".equals(fieldKey)){for(int i=1;i<=20;i++)addUnique(out,String.valueOf(i));}else if("speed".equals(fieldKey)){String[] a={"6 m","7,5 m","9 m","10,5 m","12 m"};for(String x:a)addUnique(out,x);}else if("initiative".equals(fieldKey)){for(int i=-5;i<=10;i++)addUnique(out,(i>=0?"+":"")+i);}else if("proficiency_bonus".equals(fieldKey)){for(int i=2;i<=6;i++)addUnique(out,"+"+i);}else if("hit_dice".equals(fieldKey)){String[] a={"1d6","1d8","1d10","1d12"};for(String x:a)addUnique(out,x);}else if("subclass".equals(fieldKey)){String cls=refs!=null&&refs.cls!=null&&refs.cls.getSelectedItem()!=null?String.valueOf(refs.cls.getSelectedItem()):"";JSONArray known=CharacterOptionCatalogEngine.subclassesFor(cls);for(int i=0;i<known.length();i++)addUnique(out,known.optString(i));JSONArray run163=run163SubclassesForClass(cls);for(int i=0;i<run163.length();i++)addUnique(out,run163.optString(i));JSONObject context=collectCharacterSheet(refs,profile);for(int i=0;i<24&&out.size()<24;i++){String x=characterGenerator.randomField("subclass",context);if(x!=null&&x.trim().length()>0)addUnique(out,x.trim());}addUnique(out,"Ainda sem subclasse");}if(out.size()==0)addUnique(out,"Automático conforme classe e nível");return out;}
'''
new = '''    private ArrayList<String> ruleFieldChoices(String fieldKey,CharacterFormRefs refs,String profile,String current){ArrayList<String> out=new ArrayList<String>();if(current!=null&&current.trim().length()>0)out.add(current.trim());if("level".equals(fieldKey)){for(int i=1;i<=20;i++)addUnique(out,String.valueOf(i));}else if("speed".equals(fieldKey)){String[] a={"6 m","7,5 m","9 m","10,5 m","12 m"};for(String x:a)addUnique(out,x);}else if("initiative".equals(fieldKey)){for(int i=-5;i<=10;i++)addUnique(out,(i>=0?"+":"")+i);}else if("proficiency_bonus".equals(fieldKey)){for(int i=2;i<=6;i++)addUnique(out,"+"+i);}else if("hit_dice".equals(fieldKey)){String[] a={"1d6","1d8","1d10","1d12"};for(String x:a)addUnique(out,x);}else if("subclass".equals(fieldKey)){String cls=refs!=null&&refs.cls!=null&&refs.cls.getSelectedItem()!=null?String.valueOf(refs.cls.getSelectedItem()):"";int lvl=refs==null?1:Math.max(1,Math.min(20,num(refs.level,1)));int min=subclassMinimumLevelForForm(cls,profile);if(lvl>=min){JSONArray known=CharacterOptionCatalogEngine.subclassesFor(cls);for(int i=0;i<known.length();i++){String x=known.optString(i);JSONObject gate=CharacterOptionCatalogEngine.validateSubclass(cls,x);if(gate.optBoolean("selection_allowed",false)&&gate.optBoolean("mechanics_compiled",false))addUnique(out,x);}JSONArray run163=run163SubclassesForClass(cls);for(int i=0;i<run163.length();i++)addUnique(out,run163.optString(i));JSONObject context=collectCharacterSheet(refs,profile);for(int i=0;i<24&&out.size()<24;i++){String x=characterGenerator.randomField("subclass",context);if(x!=null&&x.trim().length()>0&&!x.toLowerCase(new Locale("pt","BR")).contains("disponível nos manuais"))addUnique(out,x.trim());}}addUnique(out,lvl<min?"Disponível a partir do nível "+min:"Ainda sem subclasse");}if(out.size()==0)addUnique(out,"Automático conforme classe e nível");return out;}
'''
t = replace_once(t, old, new, "ui-subclass-choices-gate")

anchor = '''    private void addUnique(ArrayList<String> list,String value){if(value==null)return;String v=value.trim();if(v.length()==0)return;for(String x:list)if(x.equalsIgnoreCase(v))return;list.add(v);}
'''
helpers = r'''    private int subclassMinimumLevelForForm(String cls,String profile){String c=foldCharacterKey(cls);if(profile!=null&&(profile.contains("2024")||profile.startsWith("5.5")))return 3;if(c.contains("clerigo")||c.contains("bruxo")||c.contains("feiticeiro"))return 1;if(c.contains("druida")||c.contains("mago"))return 2;return 3;}
    private String foldCharacterKey(String value){return java.text.Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
    private boolean coreSubclassMatchesForm(String cls,String subclass){String c=foldCharacterKey(cls),s=foldCharacterKey(subclass);if(c.contains("barbar"))return oneOfCharacter(s,"berserker","coracao selvagem","arvore do mundo","fanatico","guerreiro totemico");if(c.contains("bardo"))return oneOfCharacter(s,"colegio da danca","colegio do glamour","colegio do conhecimento","colegio do valor");if(c.contains("bruxo"))return oneOfCharacter(s,"patrono arquifada","patrono celestial","patrono corruptor","patrono grande antigo","arquifada","corruptor","grande antigo");if(c.contains("clerigo"))return oneOfCharacter(s,"dominio da guerra","dominio da luz","dominio da trapaca","dominio da vida");if(c.contains("druida"))return oneOfCharacter(s,"circulo da lua","circulo da terra","circulo das estrelas","circulo do mar");if(c.contains("feiticeiro"))return oneOfCharacter(s,"feiticaria aberrante","feiticaria draconica","feiticaria do caos mecanico","magia selvagem","linhagem draconica");if(c.contains("ladino"))return oneOfCharacter(s,"trapaceiro arcano","assassino","alma faca","ladrao");if(c.contains("guerreiro"))return oneOfCharacter(s,"mestre de batalha","campeao","cavaleiro arcano","guerreiro psionico");if(c.contains("mago"))return oneOfCharacter(s,"abjurador","adivinho","evocador","ilusionista","abjuracao","adivinhacao","conjuracao","encantamento","evocacao","ilusao","necromancia","transmutacao");if(c.contains("monge"))return oneOfCharacter(s,"guerreiro da misericordia","guerreiro da mao aberta","guerreiro das sombras","guerreiro dos elementos","caminho da mao aberta","caminho da sombra","caminho dos quatro elementos");if(c.contains("paladino"))return oneOfCharacter(s,"juramento da devocao","juramento da gloria","juramento da vinganca","juramento dos ancioes");if(c.contains("patrulheiro"))return oneOfCharacter(s,"andarilho feerico","cacador","mestre das feras","perseguidor sombrio");return false;}
    private boolean oneOfCharacter(String value,String... allowed){for(String x:allowed)if(value.equals(x))return true;return false;}
    private boolean subclassAllowedForForm(String cls,String subclass,int level,String profile){if(subclass==null||subclass.trim().isEmpty())return true;if(level<subclassMinimumLevelForForm(cls,profile))return false;if(coreSubclassMatchesForm(cls,subclass))return true;JSONObject gate=CharacterOptionCatalogEngine.validateSubclass(cls,subclass);return gate.optBoolean("selection_allowed",false)&&gate.optBoolean("mechanics_compiled",false);}
    private boolean generatedConcept(String value){return value!=null&&value.contains(" • ")&&value.contains(" — ");}
    private boolean generatedAppearance(String value){return value!=null&&value.contains("mantém a mesma aparência entre cenas");}
    private boolean generatedClassEntry(String value){return value!=null&&value.startsWith("As experiências anteriores culminaram no caminho de ");}

    private void bindCharacterDependencyListeners(final CharacterFormRefs f,final String profile){
        if(f==null||f.species==null||f.cls==null||f.background==null||f.level==null)return;
        f.lastSpecies=String.valueOf(f.species.getSelectedItem());f.lastClass=String.valueOf(f.cls.getSelectedItem());f.lastBackground=String.valueOf(f.background.getSelectedItem());f.lastLevel=Math.max(1,Math.min(20,num(f.level,1)));
        android.widget.AdapterView.OnItemSelectedListener listener=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> parent){}public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){if(f.dependenciesBound&&!f.dependencyRefreshBusy)reconcileCharacterDependencies(f,profile);}};
        f.species.setOnItemSelectedListener(listener);f.cls.setOnItemSelectedListener(listener);f.background.setOnItemSelectedListener(listener);
        f.level.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(android.text.Editable e){if(f.dependenciesBound&&!f.dependencyRefreshBusy)f.level.post(new Runnable(){public void run(){if(f.dependenciesBound&&!f.dependencyRefreshBusy)reconcileCharacterDependencies(f,profile);}});}});
        f.dependenciesBound=true;
    }

    private void reconcileCharacterDependencies(CharacterFormRefs f,String profile){
        if(f==null||f.dependencyRefreshBusy)return;f.dependencyRefreshBusy=true;
        try{
            String species=String.valueOf(f.species.getSelectedItem()),cls=String.valueOf(f.cls.getSelectedItem()),background=String.valueOf(f.background.getSelectedItem());int level=Math.max(1,Math.min(20,num(f.level,1)));
            boolean speciesChanged=!species.equalsIgnoreCase(f.lastSpecies),classChanged=!cls.equalsIgnoreCase(f.lastClass),backgroundChanged=!background.equalsIgnoreCase(f.lastBackground),levelChanged=level!=f.lastLevel;
            if(!(speciesChanged||classChanged||backgroundChanged||levelChanged))return;
            String subclass=value(f.subclass);if(!subclassAllowedForForm(cls,subclass,level,profile)){f.subclass.setText("");subclass="";}
            JSONObject fresh=characterGenerator.baseComplete(profile,species,cls,background,level);
            if(!(speciesChanged||classChanged||backgroundChanged)){fresh.put("str",num(f.str,10));fresh.put("dex",num(f.dex,10));fresh.put("con",num(f.con,10));fresh.put("int_score",num(f.intel,10));fresh.put("wis",num(f.wis,10));fresh.put("cha",num(f.cha,10));}
            if(subclass.length()>0)fresh.put("class_name",cls+" — "+subclass);
            fresh=characterGenerator.recomputeMechanics(fresh);

            if(speciesChanged||classChanged||backgroundChanged){f.str.setText(String.valueOf(fresh.optInt("str",10)));f.dex.setText(String.valueOf(fresh.optInt("dex",10)));f.con.setText(String.valueOf(fresh.optInt("con",10)));f.intel.setText(String.valueOf(fresh.optInt("int_score",10)));f.wis.setText(String.valueOf(fresh.optInt("wis",10)));f.cha.setText(String.valueOf(fresh.optInt("cha",10)));}
            f.hp.setText(String.valueOf(fresh.optInt("hp",1)));f.maxHp.setText(String.valueOf(fresh.optInt("max_hp",1)));f.ac.setText(String.valueOf(fresh.optInt("ac",10)));f.speed.setText(fresh.optString("speed"));f.initiative.setText(fresh.optString("initiative"));f.profBonus.setText(fresh.optString("proficiency_bonus"));f.hitDice.setText(fresh.optString("hit_dice"));f.savingThrows.setText(fresh.optString("saving_throws"));f.skills.setText(fresh.optString("skills"));f.passiveSenses.setText(fresh.optString("passive_senses"));f.languages.setText(fresh.optString("languages"));f.proficiencies.setText(fresh.optString("proficiencies"));f.tools.setText(fresh.optString("tools"));f.carrying.setText(fresh.optString("carrying_capacity"));f.attacks.setText(fresh.optString("attacks"));f.features.setText(fresh.optString("features"));f.spellcasting.setText(fresh.optString("spellcasting"));f.spells.setText(fresh.optString("spells"));f.inventory.setText(fresh.optString("inventory"));
            if(generatedConcept(value(f.concept))||value(f.concept).isEmpty())f.concept.setText(fresh.optString("concept"));if(classChanged||value(f.role).isEmpty())f.role.setText(fresh.optString("role"));if(generatedAppearance(value(f.appearance))||value(f.appearance).isEmpty())f.appearance.setText(fresh.optString("appearance"));if(generatedClassEntry(value(f.classEntry))||value(f.classEntry).isEmpty())f.classEntry.setText(fresh.optString("class_entry"));
            if(speciesChanged||classChanged||backgroundChanged){f.lifePath.setText(fresh.optString("life_path"));if(value(f.history).isEmpty()||value(f.history).contains("Um acontecimento")||value(f.history).contains("HISTÓRIA DE VIDA"))f.history.setText(fresh.optString("history"));}
            f.lastSpecies=species;f.lastClass=cls;f.lastBackground=background;f.lastLevel=level;
            toast("Dependências da ficha recalculadas para "+species+" • "+cls+" • nível "+level+".");
        }catch(Exception e){AppLog.ignored("CharacterCreation","Falha ao reconciliar dependências.",e);}finally{f.dependencyRefreshBusy=false;}
    }

''' + anchor
t = replace_once(t, anchor, helpers, "ui-reactive-dependency-helpers")

old = '''    private static final class CharacterFormRefs{
        EditText name,nickname,title,subclass,level,concept,role,str,dex,con,intel,wis,cha,hp,maxHp,ac,speed,initiative,profBonus,hitDice,savingThrows,skills,passiveSenses,languages,proficiencies,tools,carrying,attacks,features,feats,featChoicesJson,spellcasting,spells,inventory,appearance,personality,ideal,bond,fear,flaw,contact,rival,debt,motivation,classEntry,history,mannerisms,quotes,levelUp,extraNotes,lifePath,oracleReferences;
        Spinner species,cls,background,alignment,origin,career,terms;CheckBox auto,active;
    }
'''
new = '''    private static final class CharacterFormRefs{
        EditText name,nickname,title,subclass,level,concept,role,str,dex,con,intel,wis,cha,hp,maxHp,ac,speed,initiative,profBonus,hitDice,savingThrows,skills,passiveSenses,languages,proficiencies,tools,carrying,attacks,features,feats,featChoicesJson,spellcasting,spells,inventory,appearance,personality,ideal,bond,fear,flaw,contact,rival,debt,motivation,classEntry,history,mannerisms,quotes,levelUp,extraNotes,lifePath,oracleReferences;
        Spinner species,cls,background,alignment,origin,career,terms;CheckBox auto,active;
        boolean dependenciesBound=false,dependencyRefreshBusy=false;String lastSpecies="",lastClass="",lastBackground="";int lastLevel=1;
    }
'''
t = replace_once(t, old, new, "ui-ref-state")

# Keep random/Mythic full seed application from being interpreted as manual structural edits.
old = '''    private void applyCharacterSeed(CharacterFormRefs f,JSONObject seed){JSONObject s=characterGenerator.ensureComplete(DndPtBrLocalizer.localizeCharacterSheet(seed));f.name.setText(s.optString("name"));
'''
new = '''    private void applyCharacterSeed(CharacterFormRefs f,JSONObject seed){boolean wasBusy=f!=null&&f.dependencyRefreshBusy;if(f!=null)f.dependencyRefreshBusy=true;JSONObject s=characterGenerator.ensureComplete(DndPtBrLocalizer.localizeCharacterSheet(seed));f.name.setText(s.optString("name"));
'''
t = replace_once(t, old, new, "ui-seed-busy-start")

old = '''if(f.active!=null)f.active.setChecked(s.optBoolean("active",true));}
'''
new = '''if(f.active!=null)f.active.setChecked(s.optBoolean("active",true));if(f!=null){f.lastSpecies=String.valueOf(f.species.getSelectedItem());f.lastClass=String.valueOf(f.cls.getSelectedItem());f.lastBackground=String.valueOf(f.background.getSelectedItem());f.lastLevel=Math.max(1,Math.min(20,num(f.level,1)));f.dependencyRefreshBusy=wasBusy;}}
'''
if old not in t:
    raise SystemExit("PATCH_MISSING::ui-seed-busy-end")
t = t.replace(old, new, 1)

write(name, t)

# ---------------------------------------------------------------------
# Static assertions.
# ---------------------------------------------------------------------
checks = {
    "validator_catalog_class_gate": "compiledSupplementClass" in read("CharacterRulesValidator.java"),
    "validator_subclass_gate": "CharacterOptionCatalogEngine.validateSubclass" in read("CharacterRulesValidator.java"),
    "generator_shared_class_pool": "selectableClasses(profile)" in read("CharacterSheetGenerator.java"),
    "no_fake_subclass_placeholder": 'else options = new String[]{"Subclasse disponível nos manuais habilitados"}' not in read("CharacterSheetGenerator.java"),
    "ui_shared_class_pool": "characterGenerator.selectableClasses(rulesProfile)" in read("MainActivity.java"),
    "ui_dependency_binding": "bindCharacterDependencyListeners(f,rulesProfile)" in read("MainActivity.java"),
    "ui_subclass_level_gate": "subclassMinimumLevelForForm" in read("MainActivity.java"),
    "ptbr_volo_label": "Manual de Sub-raças de Volo" in read("DndPtBrLocalizer.java"),
}
bad=[k for k,v in checks.items() if not v]
print(json.dumps(checks, ensure_ascii=False, indent=2))
if bad:
    raise SystemExit("STATIC_CHECK_FAILED::"+",".join(bad))
print("PATCH_RUN181_OK")
