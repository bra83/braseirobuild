from pathlib import Path
p=Path('app/src/main/assets/www/mochila.js')
s=p.read_text()

imp="import { WWN_EQUIPMENT_CATALOG } from './core/wwn-equipment-catalog.js';"
extra=imp+"\nimport { MundaneEquipmentService } from './core/mundane-equipment-service.js';\nimport { WWN_MUNDANE_EQUIPMENT } from './core/wwn-mundane-equipment-core.js';"
if "MundaneEquipmentService" not in s:
    if imp not in s: raise SystemExit('equipment catalog import marker missing')
    s=s.replace(imp,extra,1)

inst="if(!campaignId)throw new Error('Nenhuma campanha ativa');const service=new EquipmentService({persistence});let selectedPcId=null;"
new_inst="if(!campaignId)throw new Error('Nenhuma campanha ativa');const service=new EquipmentService({persistence});const mundaneService=new MundaneEquipmentService({persistence});let selectedPcId=null;"
if "const mundaneService=" not in s:
    if inst not in s: raise SystemExit('service marker missing')
    s=s.replace(inst,new_inst,1)

marker="function itemKindLabel(kind){const k=String(kind??'').toUpperCase();return ITEM_KIND_PTBR[k]??k}"
helpers=marker+"\nfunction walletCopper(c){return Number.isInteger(c?.silver)&&Number.isInteger(c?.copper??0)?c.silver*10+(c.copper??0):null}\nfunction currencyText(c){const total=walletCopper(c);if(total===null)return '—';const sp=Math.floor(total/10),cp=total%10;return sp&&cp?`${sp} sp ${cp} cp`:sp?`${sp} sp`:`${cp} cp`}\nfunction priceCopperText(cp){if(!Number.isInteger(cp)||cp<0)return '—';const sp=Math.floor(cp/10),rest=cp%10;return sp&&rest?`${sp} sp ${rest} cp`:sp?`${sp} sp`:`${rest} cp`}"
if "function walletCopper(" not in s:
    if marker not in s: raise SystemExit('itemKindLabel marker missing')
    s=s.replace(marker,helpers,1)

start=s.index('function renderInventory(c){')
end=s.index('function catalogAllowed(cat){',start)
render_inventory="""function renderInventory(c){const list=$('inventoryList');list.replaceChildren();const known=Array.isArray(c?.inventory);$('initialBox').classList.toggle('hidden',known);$('marketButton').disabled=!known||walletCopper(c)===null;if(!known)return;for(const i of c.inventory){const row=document.createElement('article');row.className='item';row.innerHTML=`<div class=\"item-head\"><b></b><em></em></div><small></small><div class=\"item-actions\"></div>`;row.querySelector('b').textContent=i.name??i.catalogId??i.itemId;row.querySelector('em').textContent=i.loaded===false?'DESCARREGADA':carryStateLabelPtBr(i.carryState);row.querySelector('small').textContent=itemDetail(i);const actions=row.querySelector('.item-actions');const b=document.createElement('button');const ready=String(i.carryState??'').toUpperCase()==='READIED';b.textContent=ready?'GUARDAR':'PREPARAR';b.onclick=async()=>{const r=service.setCarryState({campaignId,pcId:selectedPcId,itemId:i.itemId??i.id,to:ready?'STOWED':'READIED'});showToast(r.status==='OK'?`${i.name}: ${ready?'guardado':'preparado'}.`:r.status);render()};actions.appendChild(b);list.appendChild(row)}for(const a of c.ownedAssets??[]){const row=document.createElement('article');row.className='item asset-item';row.innerHTML='<div class=\"item-head\"><b></b><em>ATIVO</em></div><small></small>';row.querySelector('b').textContent=a.name??a.catalogId??a.assetId;row.querySelector('small').textContent=`${String(a.kind??'ATIVO').replaceAll('_',' ')} • propriedade do personagem`;list.appendChild(row)}if(!c.inventory.length&&!(c.ownedAssets??[]).length){const empty=document.createElement('div');empty.className='initial';empty.style.position='static';empty.innerHTML='<b>MOCHILA VAZIA</b><p>Abra o Mercado para adquirir equipamento com a moeda do personagem.</p>';list.appendChild(empty)}}
"""
s=s[:start]+render_inventory+s[end:]

start=s.index('function catalogAllowed(cat){')
end=s.index('function renderHeroes(s){',start)
market="""function catalogAllowed(cat){if(!cat||cat.priceSilver<=0)return false;if(String(cat.catalogId).startsWith('HURLANT'))return false;if(cat.catalogId==='HURLANT_BOLTS')return false;return ['WEAPON','ARMOR','SHIELD','AMMO'].includes(cat.kind)}
function mundaneCatalogAllowed(cat){if(!cat||!Number.isInteger(cat.costCp)||cat.costCp<0)return false;if(String(cat.catalogId).startsWith('HURLANT'))return false;return ['GEAR','BUNDLE','BEAST','TRANSPORT','SERVICE'].includes(String(cat.kind??'GEAR').toUpperCase())}
function catalogRow({name,detail,costCopper,buyAction}){const row=document.createElement('div');row.className='catalog-item';const info=document.createElement('div');const title=document.createElement('b');title.textContent=name;const small=document.createElement('small');small.textContent=detail;info.append(title,small);const buy=document.createElement('button');buy.textContent='COMPRAR';const c=pc(state()),available=walletCopper(c);buy.disabled=available===null||available<costCopper;buy.onclick=buyAction;row.append(info,buy);return row}
function renderCatalog(c){$('marketSilver').textContent=currencyText(c);const list=$('catalogList');list.replaceChildren();const cats=Object.values(WWN_EQUIPMENT_CATALOG).filter(catalogAllowed).sort((a,b)=>a.kind.localeCompare(b.kind)||a.priceSilver-b.priceSilver||a.name.localeCompare(b.name));for(const cat of cats){const extra=cat.catalogId==='CROSSBOW'?' • virotes: aquisição manual/source-bound':'';list.appendChild(catalogRow({name:cat.name,detail:`${itemKindLabel(cat.kind)} • Enc ${cat.encumbrance} • ${cat.priceSilver} sp${extra}`,costCopper:cat.priceSilver*10,buyAction:async()=>{const r=service.purchase({campaignId,pcId:selectedPcId,catalogId:cat.catalogId,carryState:'STOWED'});showToast(r.status==='OK'?`${cat.name} comprado. ${currencyText(pc(state()))} restantes.`:r.status);render();renderCatalog(pc(state()))}}))}const mundane=Object.values(WWN_MUNDANE_EQUIPMENT).filter(mundaneCatalogAllowed).sort((a,b)=>String(a.kind??'GEAR').localeCompare(String(b.kind??'GEAR'))||a.costCp-b.costCp||a.name.localeCompare(b.name));for(const cat of mundane){const enc=Number.isInteger(cat.encumbrance)?` • Enc ${cat.encumbrance}`:'';list.appendChild(catalogRow({name:cat.name,detail:`${itemKindLabel(cat.kind??'GEAR')}${enc} • ${priceCopperText(cat.costCp)}`,costCopper:cat.costCp,buyAction:async()=>{const r=mundaneService.purchase({campaignId,pcId:selectedPcId,catalogId:cat.catalogId,quantity:1,carryState:'STOWED'});showToast(r.status==='OK'?`${cat.name} adquirido. ${currencyText(pc(state()))} restantes.`:r.status);render();renderCatalog(pc(state()))}}))}}
"""
s=s[:start]+market+s[end:]

old="function render(){const s=state(),c=pc(s);$('heroName').textContent=c?.name??'—';$('silverText').textContent=Number.isInteger(c?.silver)?`${c.silver} sp`:'—';"
new="function render(){const s=state(),c=pc(s);$('heroName').textContent=c?.name??'—';$('silverText').textContent=currencyText(c);"
if old in s:s=s.replace(old,new,1)
else: raise SystemExit('render wallet marker missing')

s=s.replace("window.__BRASEIRO_MOCHILA__={campaignId,service,state,render};","window.__BRASEIRO_MOCHILA__={campaignId,service,mundaneService,state,render};")
p.write_text(s)
