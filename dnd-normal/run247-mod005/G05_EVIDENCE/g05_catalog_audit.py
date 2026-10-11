#!/usr/bin/env python3
"""Deterministic G05 catalogue inventory; never infers gameplay rules from prose."""
from pathlib import Path
import json,csv,sys,collections
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/braseiro/dnd5e'
out=root/'G05_EVIDENCE';out.mkdir(exist_ok=True)
files=list(dict.fromkeys(list(src.glob('*BestiaryRuntimeEngine.java'))+list(src.glob('*CompiledBestiaryEngine.java'))+list(src.glob('*RuntimeEngine.java'))))
records=[];issues=[];count=collections.Counter()
for f in files:
    for line in f.read_text(encoding='utf8').splitlines():
        t=line.strip().rstrip(',')
        if not (t.startswith('"{') and t.endswith('}"')):continue
        try:
            item=json.loads(json.loads(t))
        except (ValueError, TypeError):continue
        if not isinstance(item,dict) or 'id' not in item or 'actions' not in item:continue
        records.append(item)
        for a in item.get('actions',[]):
            if not isinstance(a,dict):continue
            count['total_actions']+=1
            dc=a.get('save_dc',0) or 0
            ability=str(a.get('save_ability','') or '').strip().lower()
            attack=('to_hit' in a)
            effect=str(a.get('effect_on_fail','') or '').strip()
            condition=str(a.get('condition_on_fail','') or '').strip()
            category=str(a.get('category','action') or 'action').strip().lower()
            candidates=[]
            if dc and ability not in ('str','dex','con','int','wis','cha'):
                candidates.append('attack_save_dc_invalid_ability' if attack else 'save_action_dc_invalid_ability')
            if attack and not dc and (condition or effect):candidates.append('hit_rider_without_save_dc_or_typed_handler')
            if category not in ('action','bonus','bonus_action'):candidates.append('special_action_requires_own_trigger_budget')
            if str(a.get('recharge','') or '').strip() and not __import__('re').fullmatch(r'[1-6](?:-[1-6])?',str(a['recharge']).strip()):
                candidates.append('unsupported_recharge_notation')
            if effect and dc and not __import__('re').fullmatch(r'(?:push|pull):(?:5|10|15|20|25|30|35|40|45|50|55|60)ft(?:;prone)?',effect.lower()):
                if not (condition and {v.strip().lower() for v in effect.split(',')}=={v.strip().lower() for v in condition.split(',')}):
                    candidates.append('save_effect_requires_source_bound_typed_executor')
            for issue in candidates:
                issues.append({'issue':issue,'source_java':f.name,'source_book':item.get('source_book',''),
                    'statblock_id':item['id'],'action':a.get('name',''),'category':category,
                    'save_dc':dc,'save_ability':ability,'effect_token':effect[:240],'condition_token':condition[:200]})
                count[issue]+=1
print('G05_CATALOG_RECORDS',len(records),'ACTIONS',count['total_actions'],'ISSUES',len(issues))
with (out/'G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv').open('w',newline='',encoding='utf-8-sig') as w:
    writer=csv.DictWriter(w,fieldnames=['issue','source_java','source_book','statblock_id','action','category','save_dc','save_ability','effect_token','condition_token']);writer.writeheader();writer.writerows(issues)
(out/'G05_CATALOG_AUDIT.json').write_text(json.dumps({'records_scanned':len(records),'actions_scanned':count['total_actions'],
    'issue_rows':len(issues),'issue_counts':dict(count),'coverage_status':'NOT_FULLY_IMPLEMENTED',
    'policy':'No inferred DC/save ability/movement/condition without published source and executable rule'},indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
