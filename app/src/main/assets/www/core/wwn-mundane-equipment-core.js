export const WWN_MUNDANE_EQUIPMENT_SOURCES=Object.freeze({
  currency:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.1.0 Money and Currency'}),
  gear:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.2.0 Adventuring Gear / Beasts and Transport / Gear Bundles'}),
});

const f=v=>Object.freeze(v);
const item=(catalogId,name,{kind='GEAR',costCp,encumbrance=null,weightRule='NORMAL',quantity=1,notes=[]})=>f({catalogId,name,kind,costCp,priceSilver:costCp/10,encumbrance,weightRule,quantity,notes:f([...notes]),sourceRefs:f([WWN_MUNDANE_EQUIPMENT_SOURCES.gear])});

// WWN uses 10 cp = 1 sp and 10 sp = 1 gp. Copper is the exact internal unit so
// sub-silver prices never create floating-point mutation errors.
export const COPPER_PER_SILVER=10;
export const COPPER_PER_GOLD=100;
export function moneyToCopper({copper=0,silver=0,gold=0}={}){
  for(const [k,v] of Object.entries({copper,silver,gold}))if(!Number.isInteger(v)||v<0)throw new RangeError(`${k} must be a non-negative integer`);
  return copper+silver*COPPER_PER_SILVER+gold*COPPER_PER_GOLD;
}
export function copperToMoney(totalCopper){
  if(!Number.isInteger(totalCopper)||totalCopper<0)throw new RangeError('totalCopper must be a non-negative integer');
  const gold=Math.floor(totalCopper/COPPER_PER_GOLD);const afterGold=totalCopper%COPPER_PER_GOLD;const silver=Math.floor(afterGold/COPPER_PER_SILVER);const copper=afterGold%COPPER_PER_SILVER;
  return f({gold,silver,copper,totalCopper});
}

export const WWN_MUNDANE_EQUIPMENT=f({
  ARROWS_20:item('ARROWS_20','Arrows, 20',{kind:'AMMO',costCp:20,encumbrance:1,quantity:20}),
  BACKPACK:item('BACKPACK','Backpack',{costCp:20,encumbrance:1,weightRule:'NO_ENCUMBRANCE_WHILE_WORN'}),
  BOOTS:item('BOOTS','Boots',{costCp:20,encumbrance:1,weightRule:'NO_ENCUMBRANCE_WHILE_WORN'}),
  CANDLE:item('CANDLE','Candle',{costCp:1,encumbrance:0,weightRule:'EFFECTIVELY_WEIGHTLESS_IN_MODEST_NUMBERS'}),
  CART_ONE_HORSE:item('CART_ONE_HORSE','Cart, one-horse',{kind:'TRANSPORT',costCp:500,encumbrance:null}),
  CLOTHES_COMMON:item('CLOTHES_COMMON','Clothes, common',{costCp:250,encumbrance:1,weightRule:'NO_ENCUMBRANCE_WHILE_WORN'}),
  CLOTHES_FINE:item('CLOTHES_FINE','Clothes, fine',{costCp:1000,encumbrance:1,weightRule:'NO_ENCUMBRANCE_WHILE_WORN'}),
  CLOTHES_NOBLE:item('CLOTHES_NOBLE','Clothes, noble',{costCp:5000,encumbrance:2,weightRule:'NO_ENCUMBRANCE_WHILE_WORN'}),
  COOKING_UTENSILS:item('COOKING_UTENSILS','Cooking utensils',{costCp:40,encumbrance:1}),
  CROWBAR:item('CROWBAR','Crowbar',{costCp:40,encumbrance:1}),
  FIREWOOD_NIGHT:item('FIREWOOD_NIGHT',"Firewood, one night's fire",{costCp:2,encumbrance:4}),
  FLASK_METAL_PINT:item('FLASK_METAL_PINT','Flask, metal, one pint',{costCp:30,encumbrance:1}),
  GRAPPLING_HOOK:item('GRAPPLING_HOOK','Grappling hook',{costCp:50,encumbrance:1}),
  HAMMER_SMALL_TOOL:item('HAMMER_SMALL_TOOL','Hammer or small tool',{costCp:20,encumbrance:1}),
  HEALERS_POUCH:item('HEALERS_POUCH',"Healer's pouch",{costCp:50,encumbrance:1}),
  HURLANT_BOLTS_20:item('HURLANT_BOLTS_20','Hurlant bolts, 20',{kind:'AMMO',costCp:200,encumbrance:1,quantity:20}),
  IRON_SPIKES_10:item('IRON_SPIKES_10','Iron spikes, 10',{costCp:10,encumbrance:1,quantity:10}),
  LANTERN:item('LANTERN','Lantern',{costCp:100,encumbrance:1}),
  MIRROR_HAND:item('MIRROR_HAND','Mirror, hand',{costCp:100,encumbrance:0,weightRule:'EFFECTIVELY_WEIGHTLESS_IN_MODEST_NUMBERS'}),
  OIL_PINT:item('OIL_PINT','Oil, one pint',{costCp:10,encumbrance:1,weightRule:'BUNDLE_THREE_FOR_ONE_ENCUMBRANCE'}),
  OIL_3_BUNDLE:item('OIL_3_BUNDLE','Oil, three one-pint units bundled',{costCp:30,encumbrance:1,quantity:3,weightRule:'BUNDLED_THREE',notes:['Breaking open a bundle requires a Main Action.']}),
  PAPER_10:item('PAPER_10','Paper, 10 sheets',{costCp:10,encumbrance:0,quantity:10,weightRule:'EFFECTIVELY_WEIGHTLESS_IN_MODEST_NUMBERS'}),
  RATIONS_WEEK:item('RATIONS_WEEK','Rations, one week',{costCp:50,encumbrance:4}),
  ROPE_50:item('ROPE_50',"Rope, 50'",{costCp:20,encumbrance:2}),
  SACK:item('SACK','Sack',{costCp:10,encumbrance:1}),
  SHOVEL_PICK:item('SHOVEL_PICK','Shovel, pick, or similar tool',{costCp:40,encumbrance:2}),
  TINDER_BOX:item('TINDER_BOX','Tinder box',{costCp:10,encumbrance:0,weightRule:'EFFECTIVELY_WEIGHTLESS_IN_MODEST_NUMBERS'}),
  TORCH:item('TORCH','Torch',{costCp:2,encumbrance:1,weightRule:'BUNDLE_THREE_FOR_ONE_ENCUMBRANCE'}),
  TORCHES_3_BUNDLE:item('TORCHES_3_BUNDLE','Torches, 3 bundled',{costCp:6,encumbrance:1,quantity:3,weightRule:'BUNDLED_THREE',notes:['Breaking open a bundle requires a Main Action.']}),
  WATERSKIN_GALLON:item('WATERSKIN_GALLON','Waterskin, one gallon',{costCp:10,encumbrance:1}),
  WRITING_KIT:item('WRITING_KIT','Writing kit',{costCp:30,encumbrance:1}),

  HORSE_RIDING:item('HORSE_RIDING','Horse, riding',{kind:'BEAST',costCp:2000}),
  HORSE_DRAFT:item('HORSE_DRAFT','Horse, draft',{kind:'BEAST',costCp:1500}),
  HORSE_BATTLE_TRAINED:item('HORSE_BATTLE_TRAINED','Horse, battle-trained',{kind:'BEAST',costCp:20000}),
  MULE:item('MULE','Mule',{kind:'BEAST',costCp:300}),
  COW:item('COW','Cow',{kind:'BEAST',costCp:100}),
  OX_PLOW_TRAINED:item('OX_PLOW_TRAINED','Ox, plow-trained',{kind:'BEAST',costCp:150}),
  CHICKEN:item('CHICKEN','Chicken',{kind:'BEAST',costCp:5}),
  PIG:item('PIG','Pig',{kind:'BEAST',costCp:30}),
  DOG_WORKING:item('DOG_WORKING','Dog, working',{kind:'BEAST',costCp:200}),
  SHEEP_OR_GOAT:item('SHEEP_OR_GOAT','Sheep or goat',{kind:'BEAST',costCp:50}),
  RIVER_FERRY_PASSENGER:item('RIVER_FERRY_PASSENGER','River ferry, per passenger',{kind:'SERVICE',costCp:5}),
  SHIP_PASSAGE_DAY:item('SHIP_PASSAGE_DAY','Ship passage, per expected day',{kind:'SERVICE',costCp:20}),
  CARRIAGE_TRAVEL_MILE:item('CARRIAGE_TRAVEL_MILE','Carriage travel, per mile',{kind:'SERVICE',costCp:2}),
  ROWBOAT:item('ROWBOAT','Rowboat',{kind:'TRANSPORT',costCp:300}),
  SMALL_FISHING_BOAT:item('SMALL_FISHING_BOAT','Small fishing boat',{kind:'TRANSPORT',costCp:2000}),
  MERCHANT_SHIP:item('MERCHANT_SHIP','Merchant ship',{kind:'TRANSPORT',costCp:50000}),
  WAR_GALLEON:item('WAR_GALLEON','War galleon',{kind:'TRANSPORT',costCp:500000}),

  ARTISANS_EQUIPMENT:item('ARTISANS_EQUIPMENT',"Artisan's Equipment",{kind:'BUNDLE',costCp:500,encumbrance:5}),
  CRIMINAL_TOOLS:item('CRIMINAL_TOOLS','Criminal Tools',{kind:'BUNDLE',costCp:1000,encumbrance:3}),
  DUNGEONEERING_KIT:item('DUNGEONEERING_KIT','Dungeoneering Kit',{kind:'BUNDLE',costCp:2000,encumbrance:6}),
  NOBLE_COURTIER_OUTFIT:item('NOBLE_COURTIER_OUTFIT','Noble Courtier Outfit',{kind:'BUNDLE',costCp:10000,encumbrance:2}),
  PERFORMERS_IMPLEMENTS:item('PERFORMERS_IMPLEMENTS',"Performer's Implements",{kind:'BUNDLE',costCp:1000,encumbrance:3}),
  WILDERNESS_TRAVEL_GEAR:item('WILDERNESS_TRAVEL_GEAR','Wilderness Travel Gear',{kind:'BUNDLE',costCp:1000,encumbrance:5}),
});

export function mundaneEquipmentItem(catalogId){return WWN_MUNDANE_EQUIPMENT[String(catalogId??'').toUpperCase()]??null;}
export function mundanePurchasePlan({catalogId,quantity=1,walletCopper}){
  if(!Number.isInteger(quantity)||quantity<1)throw new RangeError('quantity must be a positive integer');
  if(!Number.isInteger(walletCopper)||walletCopper<0)throw new RangeError('walletCopper must be a non-negative integer');
  const catalogItem=mundaneEquipmentItem(catalogId);if(!catalogItem)return f({status:'UNKNOWN_CATALOG_ITEM_NO_MUTATION'});
  const costCopper=catalogItem.costCp*quantity;if(walletCopper<costCopper)return f({status:'INSUFFICIENT_FUNDS_NO_MUTATION',requiredCopper:costCopper,availableCopper:walletCopper});
  return f({status:'OK',catalogItem,quantity,costCopper,walletCopperBefore:walletCopper,walletCopperAfter:walletCopper-costCopper,moneyAfter:copperToMoney(walletCopper-costCopper),sourceRefs:[WWN_MUNDANE_EQUIPMENT_SOURCES.currency,WWN_MUNDANE_EQUIPMENT_SOURCES.gear]});
}
