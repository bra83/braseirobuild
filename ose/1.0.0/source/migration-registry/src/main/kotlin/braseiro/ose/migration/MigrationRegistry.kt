package braseiro.ose.migration

data class SchemaMigrationDescriptor(val fromVersion: Int, val toVersion: Int, val id: String) {
    init { require(fromVersion > 0); require(toVersion == fromVersion + 1); require(id.isNotBlank()) }
}
class MigrationRegistry(val currentSchemaVersion: Int, migrations: List<SchemaMigrationDescriptor>) {
    private val byFrom = migrations.associateBy { it.fromVersion }
    init {
        require(currentSchemaVersion > 0)
        require(byFrom.size == migrations.size)
        for (version in 1 until currentSchemaVersion) require(byFrom[version]?.toVersion == version + 1) { "Missing contiguous migration $version -> ${version + 1}" }
    }
    fun path(fromVersion: Int, toVersion: Int): List<SchemaMigrationDescriptor> {
        require(fromVersion in 1..toVersion); require(toVersion <= currentSchemaVersion)
        return (fromVersion until toVersion).map { byFrom.getValue(it) }
    }
    companion object { val V1 = MigrationRegistry(1, emptyList()) }
}
