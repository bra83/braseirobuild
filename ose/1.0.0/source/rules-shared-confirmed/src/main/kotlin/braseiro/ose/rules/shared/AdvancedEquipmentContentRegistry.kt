package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile

data class AdvancedEquipmentContentDescriptor(
    val contentId: String,
    val category: String,
    val sourceRow: Int,
    val displayName: String,
    val datasetId: String,
    val sourceSha256: String
)

object AdvancedEquipmentContentRegistry {
    const val DATASET_ID = "ose.advanced.player.equipment.audit.v1"
    const val SOURCE_SHA256 = "9c8c23ecf86c41bc17ee8b9acf409549c02898a6ccf77c7047b51ff8d85c7eb3"
    const val STATUS = "AUDITED_STRUCTURED_INTERIM_AUTHORITY_VERSION_UNPROVEN"
    private val records = listOf(
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_01", "GEAR", 1, "Mochila", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_02", "GEAR", 2, "Pé de cabra", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_03", "GEAR", 3, "Alho", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_04", "GEAR", 4, "Gancho", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_05", "GEAR", 5, "Martelo (pequeno)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_06", "GEAR", 6, "Símbolo sagrado", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_07", "GEAR", 7, "Água benta (frasco)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_08", "GEAR", 8, "Pontas de ferro (12)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_09", "GEAR", 9, "Lanterna", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_10", "GEAR", 10, "Espelho (tamanho à mão, aço)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_11", "GEAR", 11, "Óleo (1 frasco)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_12", "GEAR", 12, "Vara (3m de comprimento, madeira)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_13", "GEAR", 13, "Rações (conservadas, 7 dias)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_14", "GEAR", 14, "Rações (padrão, 7 dias)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_15", "GEAR", 15, "Corda (15m)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_16", "GEAR", 16, "Saco (grande)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_17", "GEAR", 17, "Saco (pequeno)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_18", "GEAR", 18, "Estacas (3) e martelo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_19", "GEAR", 19, "Ferramentas de Ladrões", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_20", "GEAR", 20, "Caixa de Isqueiro (pederneira e aço)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_21", "GEAR", 21, "Tochas (6)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_22", "GEAR", 22, "Odre", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_23", "GEAR", 23, "Vinho (2 litros)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_GEAR_24", "GEAR", 24, "Wolfsbane (1 molho)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_01", "WEAPON_PURCHASE", 1, "Machado de batalha", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_02", "WEAPON_PURCHASE", 2, "Clava", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_03", "WEAPON_PURCHASE", 3, "Besta", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_04", "WEAPON_PURCHASE", 4, "Adaga", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_05", "WEAPON_PURCHASE", 5, "Machado de mão", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_06", "WEAPON_PURCHASE", 6, "Dardo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_07", "WEAPON_PURCHASE", 7, "Lança", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_08", "WEAPON_PURCHASE", 8, "Arco longo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_09", "WEAPON_PURCHASE", 9, "Maça", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_10", "WEAPON_PURCHASE", 10, "Pistola", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_11", "WEAPON_PURCHASE", 11, "Arco curto", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_12", "WEAPON_PURCHASE", 12, "Espada curta", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_13", "WEAPON_PURCHASE", 13, "Adaga de prata", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_14", "WEAPON_PURCHASE", 14, "Estilingue", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_15", "WEAPON_PURCHASE", 15, "Lança", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_16", "WEAPON_PURCHASE", 16, "Cajado", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_17", "WEAPON_PURCHASE", 17, "Espada", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_18", "WEAPON_PURCHASE", 18, "Espada de duas mãos", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_PURCHASE_19", "WEAPON_PURCHASE", 19, "Martelo de guerra", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_AMMO_01", "AMMO", 1, "Flechas (aljava de 20)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_AMMO_02", "AMMO", 2, "Virotes (caixa de 30)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_AMMO_03", "AMMO", 3, "Flecha com ponta de prata (1)", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_AMMO_04", "AMMO", 4, "Pedras de Estilingue", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_ARMOR_01", "ARMOR", 1, "Couro", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_ARMOR_02", "ARMOR", 2, "Cota de malha", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_ARMOR_03", "ARMOR", 3, "Cota de Placa", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_ARMOR_04", "ARMOR", 4, "Escudo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_01", "WEAPON_COMBAT", 1, "Machado de batalha", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_02", "WEAPON_COMBAT", 2, "Clava", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_03", "WEAPON_COMBAT", 3, "Besta", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_04", "WEAPON_COMBAT", 4, "Adaga", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_05", "WEAPON_COMBAT", 5, "Machado de mão", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_06", "WEAPON_COMBAT", 6, "Frasco de água benta", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_07", "WEAPON_COMBAT", 7, "Dardo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_08", "WEAPON_COMBAT", 8, "Lança", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_09", "WEAPON_COMBAT", 9, "Arco longo", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_10", "WEAPON_COMBAT", 10, "Maça", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_11", "WEAPON_COMBAT", 11, "Frasco de óleo, queimando", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_12", "WEAPON_COMBAT", 12, "Pistola", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_13", "WEAPON_COMBAT", 13, "Arco curto", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_14", "WEAPON_COMBAT", 14, "Espada curta", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_15", "WEAPON_COMBAT", 15, "Adaga de prata", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_16", "WEAPON_COMBAT", 16, "Estilingue", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_17", "WEAPON_COMBAT", 17, "Lança", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_18", "WEAPON_COMBAT", 18, "Cajado", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_19", "WEAPON_COMBAT", 19, "Espada", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_20", "WEAPON_COMBAT", 20, "Tocha", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_21", "WEAPON_COMBAT", 21, "Espada de duas mãos", DATASET_ID, SOURCE_SHA256),
        AdvancedEquipmentContentDescriptor("ADV_EQUIP_WEAPON_COMBAT_22", "WEAPON_COMBAT", 22, "Martelo de guerra", DATASET_ID, SOURCE_SHA256),
    )
    private val byId = records.associateBy { it.contentId }

    fun all(profile: RuleProfile): List<AdvancedEquipmentContentDescriptor> =
        if (profile == RuleProfile.OSE_ADVANCED_FANTASY) records else emptyList()

    fun lookup(profile: RuleProfile, datasetId: String, contentId: String): AdvancedEquipmentContentDescriptor? {
        if (profile != RuleProfile.OSE_ADVANCED_FANTASY || datasetId != DATASET_ID) return null
        return byId[contentId]
    }

    /** Display names are intentionally not lookup keys: the source contains two distinct rows rendered as "Lança". */
    fun lookupByDisplayNameForbidden(@Suppress("UNUSED_PARAMETER") name: String): Nothing? = null
}
