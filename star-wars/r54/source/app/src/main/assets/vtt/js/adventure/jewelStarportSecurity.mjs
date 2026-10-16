/** SWE09 The Jewel of Yavin, Flight over Fight, printed p.94:
 * commandeering a different starship requires a Hard Computers check.
 * The book also upgrades the difficulty of Piloting (Space) checks during
 * the unprepared getaway. No starship, access, pilot or pursuer is spawned.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SOURCE='SWE09 The Jewel of Yavin, p.94, After the Battle, commandeering a ship';
const KIND='JEWEL_STARPORT_SHIP_ACCESS';
const ID=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','prototype','constructor'].includes(x);
const FACES=['ability','proficiency','difficulty','challenge','boost','setback'];
const KEYS=['id','kind','version','source','shipId','actorId','characteristicValue','rank','pool','roll','success','atMinute','gmShipPhysicallyAccessibleConfirmed','gmThirdPartyShipConfirmed','gmTerminalAccessConfirmed','gmRetryAuthorized','pilotingSpaceDifficultyUpgrade'];
function proof(r){
 const pool={...buildPositivePool(r.characteristicValue,r.rank),difficulty:3,challenge:0,boost:0,setback:0};
 if(!eq(r.pool,pool)||!eq(r.roll?.pool,pool)||!Array.isArray(r.roll?.faces))throw Error('Pool oficial Difícil de Computadores adulterado.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let i=0;
 for(const type of FACES)for(let j=0;j<pool[type];j++){
  const face=r.roll.faces[i++],faces=diceFacesForTests[type];
  if(!face||face.type!==type||!Number.isSafeInteger(face.index)||face.index<0||face.index>=faces.length||!eq(face.face,faces[face.index]))
   throw Error('Faces do acesso ao porto adulteradas.');
  for(const [symbol,value] of Object.entries(face.face))raw[symbol]+=value;
 }
 if(i!==r.roll.faces.length)throw Error('Dados extras no acesso ao porto.');
 const expected=resolveSymbols(raw);
 for(const key of ['success','netSuccess','netAdvantage','triumph','despair','raw'])if(!eq(expected[key],r.roll[key]))throw Error('Símbolos do acesso ao porto adulterados.');
 if(r.success!==expected.success)throw Error('Resultado do acesso ao porto adulterado.');
}
export function replayJewelStarportSecurity(p){
 const attempts=[],successShips=new Set(),seen=new Set();let minute=-1;
 for(const [id,r] of Object.entries(p?.operations?.receipts??{})){
  if(r?.kind!==KIND)continue;
  if(p?.id!=='jewel-of-yavin'||!ID(id)||id!==r.id||r.version!==1||r.source!==SOURCE||
   Object.keys(r).some(k=>!KEYS.includes(k))||!ID(r.shipId)||!ID(r.actorId)||
   !Number.isSafeInteger(r.characteristicValue)||r.characteristicValue<0||r.characteristicValue>7||
   !Number.isSafeInteger(r.rank)||r.rank<0||r.rank>7||
   !Number.isSafeInteger(r.atMinute)||r.atMinute<minute||
   r.gmShipPhysicallyAccessibleConfirmed!==true||r.gmThirdPartyShipConfirmed!==true||r.gmTerminalAccessConfirmed!==true||
   typeof r.gmRetryAuthorized!=='boolean'||r.pilotingSpaceDifficultyUpgrade!==1||
   successShips.has(r.shipId)||seen.has(id))throw Error('Recibo de segurança do porto inválido, repetido ou fora de ordem.');
  minute=r.atMinute;seen.add(id);
  if(attempts.some(x=>x.shipId===r.shipId)&&!r.gmRetryAuthorized)throw Error('Nova tentativa de Computadores exige autorização do Mestre.');
  proof(r);if(r.success)successShips.add(r.shipId);attempts.push(clone(r));
 }
 return attempts;
}
export function validateJewelStarportSecurity(p,world=null){
 const attempts=replayJewelStarportSecurity(p);
 if(!eq(attempts,p?.operations?.starportSecurity??[]))throw Error('Segurança do porto diverge dos recibos.');
 if(world?.clock&&attempts.some(x=>x.atMinute>(world.clock.elapsedMinutes??0)))throw Error('Acesso ao porto registrado no futuro.');
 return attempts;
}
export function successfulJewelStarportAccess(p,shipId){
 return validateJewelStarportSecurity(p).find(x=>x.shipId===shipId&&x.success)||null;
}
export function attemptJewelStarportShipAccess(world,combat,vehicles,character,details={}){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
  combat?.active||p.activeEncounterId||vehicles?.combat?.active)throw Error('Acesso ao porto exige perseguição ativa e ausência de combate.');
 if(!ID(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID de acesso repetido ou inválido.');
 const ship=vehicles?.vehicles?.[vehicles?.activeVehicleId];
 if(!ship||!ID(ship.id)||details.shipId!==ship.id||!ship.hyperdrive||
  details.gmShipPhysicallyAccessibleConfirmed!==true||details.gmThirdPartyShipConfirmed!==true||
  details.gmTerminalAccessConfirmed!==true)throw Error('Mestre deve confirmar nave real de terceiro, acesso físico e terminal de segurança.');
 const attempts=validateJewelStarportSecurity(p,w);
 if(attempts.some(x=>x.shipId===ship.id&&x.success))throw Error('Nave já acessada; não repetir a rolagem.');
 if(attempts.some(x=>x.shipId===ship.id)&&details.gmRetryAuthorized!==true)throw Error('Nova tentativa exige autorização do Mestre.');
 const stat=character?.characteristics?.intelecto,rank=character?.skills?.computadoras??0,actorId=character?.id??character?.name;
 if(!ID(actorId)||!Number.isSafeInteger(stat)||stat<0||stat>7||!Number.isSafeInteger(rank)||rank<0||rank>7)
  throw Error('Ficha real de Computadores inválida.');
 const atMinute=w.clock?.elapsedMinutes??0;
 if(!Number.isSafeInteger(atMinute)||atMinute<0)throw Error('Relógio real do porto inválido.');
 const pool={...buildPositivePool(stat,rank),difficulty:3,challenge:0,boost:0,setback:0};
 const roll=rollPool(pool,details.rng??Math.random);
 const r={id:details.actionId,kind:KIND,version:1,source:SOURCE,shipId:ship.id,actorId,
  characteristicValue:stat,rank,pool,roll,success:roll.success,atMinute,
  gmShipPhysicallyAccessibleConfirmed:true,gmThirdPartyShipConfirmed:true,gmTerminalAccessConfirmed:true,
  gmRetryAuthorized:details.gmRetryAuthorized===true,pilotingSpaceDifficultyUpgrade:1};
 p.operations.receipts[r.id]=clone(r);p.operations.starportSecurity=replayJewelStarportSecurity(p);
 validateJewelStarportSecurity(p,w);
 return {world:w,adjudication:r,accessGranted:r.success};
}

/** SWE09 p.94: every Piloting (Space) roll during an unprepared
 * commandeered-ship getaway is upgraded once, until real escape is complete.
 * No upgrade to planetary piloting, astrogation or a different craft. */
export function jewelUnpreparedSpaceUpgrade(world,vehicles,skillKey='pilotar_vehiculo_espacial',vehicleId=null){
 if(skillKey!=='pilotar_vehiculo_espacial')return 0;
 const departure=world?.publishedAdventure?.operations?.bespinDeparture;
 const board=departure?.board;
 const active=vehicleId??vehicles?.activeVehicleId;
 if(!board||board.accessMode!=='BORROWED_SHIP'||departure.escape||board.shipId!==active||
    !vehicles?.vehicles?.[active])return 0;
 const proof=successfulJewelStarportAccess(world.publishedAdventure,active);
 if(!proof||proof.id!==board.accessReceiptId)throw Error('Fuga em nave de terceiro sem prova de acesso válida.');
 return 1;
}
