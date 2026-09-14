package braseiro.ose.rules.shared

import braseiro.ose.model.*

data class StartingEquipmentEntry(
    val catalogId: String,
    val profile: RuleProfile,
    val displayName: String,
    val priceGp: Int?,
    val purchaseQuantity: Int = 1,
    val kind: InventoryItemKind,
    val unitWeightCoins: Int = 0,
    val armorLoadCategory: ArmorLoadCategory? = null,
    val armorClassDescending: Int? = null,
    val armorClassAscending: Int? = null,
    val lightSourceKind: LightSourceKind? = null,
    val weapon: WeaponRuntimeSnapshot? = null,
    val blunt: Boolean = false,
    val twoHandedOrLarge: Boolean = false,
    val sourceId: String
) {
    init {
        require(catalogId.isNotBlank() && displayName.isNotBlank() && sourceId.isNotBlank())
        require(priceGp == null || priceGp >= 0)
        require(purchaseQuantity >= 1 && unitWeightCoins >= 0)
        if (kind == InventoryItemKind.WEAPON) require(weapon != null)
    }
}

/** Source-bound starting purchase catalog. Display names are never lookup keys. */
object StartingEquipmentCatalog {
    private const val C = "Classic Rules Tome pp.40-45"
    private const val A = "Advanced Player's Tome equipment corpus pp.94-99"

    private fun w(melee:Boolean=true, missile:Boolean=false, slow:Boolean=false, d:Int=6, s:Int?=null, m:Int?=null, l:Int?=null) =
        WeaponRuntimeSnapshot(melee=melee, missile=missile, slow=slow, damageDieSides=d, shortRangeMeters=s, mediumRangeMeters=m, longRangeMeters=l)
    private fun gear(id:String,p:RuleProfile,n:String,cost:Int?,qty:Int=1,kind:InventoryItemKind=InventoryItemKind.ADVENTURING_GEAR,light:LightSourceKind?=null,src:String)=
        StartingEquipmentEntry(id,p,n,cost,qty,kind,lightSourceKind=light,sourceId=src)
    private fun weapon(id:String,p:RuleProfile,n:String,cost:Int,weight:Int,wr:WeaponRuntimeSnapshot,blunt:Boolean=false,two:Boolean=false,src:String)=
        StartingEquipmentEntry(id,p,n,cost,kind=InventoryItemKind.WEAPON,unitWeightCoins=weight,weapon=wr,blunt=blunt,twoHandedOrLarge=two,sourceId=src)
    private fun ammo(id:String,p:RuleProfile,n:String,cost:Int?,qty:Int,src:String)=StartingEquipmentEntry(id,p,n,cost,qty,InventoryItemKind.AMMUNITION,sourceId=src)
    private fun armor(id:String,p:RuleProfile,n:String,cost:Int,weight:Int,desc:Int?,asc:Int?,load:ArmorLoadCategory?,shield:Boolean=false,src:String)=
        StartingEquipmentEntry(id,p,n,cost,kind=if(shield) InventoryItemKind.SHIELD else InventoryItemKind.ARMOR,unitWeightCoins=weight,armorLoadCategory=load,armorClassDescending=desc,armorClassAscending=asc,sourceId=src)

    private fun classic(): List<StartingEquipmentEntry> {
        val p=RuleProfile.OSE_CLASSIC_FANTASY
        return listOf(
            gear("CLASSIC_EQUIP_GEAR_01",p,"Acônito",10,src=C), gear("CLASSIC_EQUIP_GEAR_02",p,"Água benta (frasco)",25,src=C),
            gear("CLASSIC_EQUIP_GEAR_03",p,"Alho",5,src=C), gear("CLASSIC_EQUIP_GEAR_04",p,"Arpéu",25,src=C),
            gear("CLASSIC_EQUIP_GEAR_05",p,"Cantil",1,src=C), gear("CLASSIC_EQUIP_GEAR_06",p,"Corda (15m)",1,src=C),
            gear("CLASSIC_EQUIP_GEAR_07",p,"Cravos de ferro (12)",1,src=C), gear("CLASSIC_EQUIP_GEAR_08",p,"Espelho de aço",5,src=C),
            gear("CLASSIC_EQUIP_GEAR_09",p,"Estacas (3) e marreta",3,src=C), gear("CLASSIC_EQUIP_GEAR_10",p,"Ferramentas de ladrão",25,src=C),
            gear("CLASSIC_EQUIP_GEAR_11",p,"Lampião",10,kind=InventoryItemKind.LIGHT_SOURCE,light=LightSourceKind.LANTERN,src=C),
            gear("CLASSIC_EQUIP_GEAR_12",p,"Martelo pequeno",2,src=C), gear("CLASSIC_EQUIP_GEAR_13",p,"Mochila",5,src=C),
            gear("CLASSIC_EQUIP_GEAR_14",p,"Óleo (frasco)",2,kind=InventoryItemKind.OIL_FLASK,src=C), gear("CLASSIC_EQUIP_GEAR_15",p,"Pé de cabra",10,src=C),
            gear("CLASSIC_EQUIP_GEAR_16",p,"Pederneira e aço",3,src=C), gear("CLASSIC_EQUIP_GEAR_17",p,"Rações comuns (7 dias)",5,7,InventoryItemKind.FOOD,src=C),
            gear("CLASSIC_EQUIP_GEAR_18",p,"Rações de viagem (7 dias)",15,7,InventoryItemKind.FOOD,src=C), gear("CLASSIC_EQUIP_GEAR_19",p,"Saco grande",2,src=C),
            gear("CLASSIC_EQUIP_GEAR_20",p,"Saco pequeno",1,src=C), gear("CLASSIC_EQUIP_GEAR_21",p,"Símbolo sagrado",25,src=C),
            gear("CLASSIC_EQUIP_GEAR_22",p,"Tochas (6)",1,6,InventoryItemKind.LIGHT_SOURCE,LightSourceKind.TORCH,C), gear("CLASSIC_EQUIP_GEAR_23",p,"Vara de madeira (3m)",1,src=C),
            gear("CLASSIC_EQUIP_GEAR_24",p,"Vinho (2 litros)",1,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_01",p,"Adaga",3,10,w(d=4),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_02",p,"Adaga de prata",30,10,w(d=4),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_03",p,"Arco curto",25,30,w(false,true,d=6,s=15,m=30,l=45),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_04",p,"Arco longo",40,30,w(false,true,d=6,s=21,m=42,l=64),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_05",p,"Arma de haste",7,150,w(slow=true,d=10),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_06",p,"Dardo",1,20,w(false,true,d=4,s=9,m=18,l=27),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_07",p,"Besta",30,50,w(false,true,true,6,24,48,73),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_08",p,"Cajado",2,40,w(slow=true,d=4),blunt=true,two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_09",p,"Clava",3,50,w(d=4),blunt=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_10",p,"Espada",10,60,w(d=8),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_11",p,"Espada curta",7,30,w(d=6),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_12",p,"Espada de duas mãos",15,150,w(slow=true,d=10),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_13",p,"Estilingue",2,20,w(false,true,d=4,s=12,m=24,l=48),blunt=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_14",p,"Lança",3,30,w(melee=true,missile=true,d=6,s=6,m=12,l=18),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_15",p,"Lança de cavalaria",5,120,w(d=6),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_16",p,"Maça",5,30,w(d=6),blunt=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_17",p,"Machado de batalha",7,50,w(d=8),two=true,src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_18",p,"Machado de mão",4,30,w(melee=true,missile=true,d=6,s=3,m=6,l=9),src=C),
            weapon("CLASSIC_EQUIP_WEAPON_PURCHASE_19",p,"Martelo de guerra",5,30,w(d=6),blunt=true,src=C),
            ammo("CLASSIC_EQUIP_AMMO_01",p,"Flechas (20)",5,20,C), ammo("CLASSIC_EQUIP_AMMO_02",p,"Flecha de prata",5,1,C),
            ammo("CLASSIC_EQUIP_AMMO_03",p,"Pedras de estilingue",0,20,C), ammo("CLASSIC_EQUIP_AMMO_04",p,"Virotes (30)",10,30,C),
            armor("CLASSIC_EQUIP_ARMOR_01",p,"Armadura de couro",20,200,7,12,ArmorLoadCategory.LIGHT,src=C),
            armor("CLASSIC_EQUIP_ARMOR_02",p,"Cota de malha",40,400,5,14,ArmorLoadCategory.HEAVY,src=C),
            armor("CLASSIC_EQUIP_ARMOR_03",p,"Armadura de placas",60,500,3,16,ArmorLoadCategory.HEAVY,src=C),
            armor("CLASSIC_EQUIP_ARMOR_04",p,"Escudo",10,100,null,null,null,true,src=C)
        )
    }

    private fun advanced(): List<StartingEquipmentEntry> {
        val p=RuleProfile.OSE_ADVANCED_FANTASY
        val gearNames=listOf("Mochila","Pé de cabra","Alho","Gancho","Martelo (pequeno)","Símbolo sagrado","Água benta (frasco)","Pontas de ferro (12)","Lanterna","Espelho (aço)","Óleo (frasco)","Vara de madeira (3m)","Rações conservadas (7 dias)","Rações padrão (7 dias)","Corda (15m)","Saco grande","Saco pequeno","Estacas (3) e martelo","Ferramentas de ladrão","Pederneira e aço","Tochas (6)","Odre","Vinho (2 litros)","Acônito")
        val costs=listOf(5,10,5,25,2,25,25,1,10,5,2,1,15,5,1,2,1,3,25,3,1,1,1,10)
        val g=gearNames.mapIndexed { i,n ->
            val id="ADV_EQUIP_GEAR_%02d".format(i+1)
            when(i+1){9->gear(id,p,n,costs[i],kind=InventoryItemKind.LIGHT_SOURCE,light=LightSourceKind.LANTERN,src=A);11->gear(id,p,n,costs[i],kind=InventoryItemKind.OIL_FLASK,src=A);13,14->gear(id,p,n,costs[i],7,InventoryItemKind.FOOD,src=A);21->gear(id,p,n,costs[i],6,InventoryItemKind.LIGHT_SOURCE,LightSourceKind.TORCH,A);else->gear(id,p,n,costs[i],src=A)}
        }
        val weapons=listOf(
            weapon("ADV_EQUIP_WEAPON_PURCHASE_01",p,"Machado de batalha",7,50,w(d=8),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_02",p,"Clava",3,50,w(d=4),blunt=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_03",p,"Besta",30,50,w(false,true,true,6,24,48,73),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_04",p,"Adaga",3,10,w(d=4),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_05",p,"Machado de mão",4,30,w(true,true,d=6,s=3,m=6,l=9),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_06",p,"Dardo",1,20,w(false,true,d=4,s=9,m=18,l=27),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_07",p,"Lança de cavalaria",5,120,w(d=6),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_08",p,"Arco longo",40,30,w(false,true,d=6,s=21,m=42,l=64),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_09",p,"Maça",5,30,w(d=6),blunt=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_10",p,"Pistola",7,150,w(slow=true,d=10),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_11",p,"Arco curto",25,30,w(false,true,d=6,s=15,m=30,l=45),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_12",p,"Espada curta",7,30,w(d=6),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_13",p,"Adaga de prata",30,10,w(d=4),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_14",p,"Estilingue",2,20,w(false,true,d=4,s=12,m=24,l=48),blunt=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_15",p,"Lança",4,30,w(true,true,d=6,s=6,m=12,l=18),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_16",p,"Cajado",2,40,w(slow=true,d=4),blunt=true,two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_17",p,"Espada",10,60,w(d=8),src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_18",p,"Espada de duas mãos",15,150,w(slow=true,d=10),two=true,src=A),
            weapon("ADV_EQUIP_WEAPON_PURCHASE_19",p,"Martelo de guerra",5,30,w(d=6),blunt=true,src=A)
        )
        val ammo=listOf(ammo("ADV_EQUIP_AMMO_01",p,"Flechas (20)",5,20,A),ammo("ADV_EQUIP_AMMO_02",p,"Virotes (30)",10,30,A),ammo("ADV_EQUIP_AMMO_03",p,"Flecha com ponta de prata",5,1,A),ammo("ADV_EQUIP_AMMO_04",p,"Pedras de estilingue",0,20,A))
        val ar=listOf(
            armor("ADV_EQUIP_ARMOR_01",p,"Couro",20,200,7,12,ArmorLoadCategory.LIGHT,src=A),
            armor("ADV_EQUIP_ARMOR_02",p,"Cota de malha",40,400,5,14,ArmorLoadCategory.HEAVY,src=A),
            armor("ADV_EQUIP_ARMOR_03",p,"Cota de placas",60,500,3,16,ArmorLoadCategory.HEAVY,src=A),
            armor("ADV_EQUIP_ARMOR_04",p,"Escudo",10,100,null,null,null,true,src=A)
        )
        return g+weapons+ammo+ar
    }

    private val records=(classic()+advanced()).associateBy { it.catalogId }
    fun all(profile: RuleProfile): List<StartingEquipmentEntry> = records.values.filter { it.profile==profile }.sortedBy { it.catalogId }
    fun lookup(profile: RuleProfile, catalogId:String): StartingEquipmentEntry? = records[catalogId]?.takeIf { it.profile==profile }
}
