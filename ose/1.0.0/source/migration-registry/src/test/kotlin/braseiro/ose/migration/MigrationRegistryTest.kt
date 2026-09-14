package braseiro.ose.migration

import kotlin.test.*

class MigrationRegistryTest {
    @Test fun `schema v1 registry is sane`() { assertEquals(1, MigrationRegistry.V1.currentSchemaVersion); assertEquals(emptyList(), MigrationRegistry.V1.path(1, 1)) }
    @Test fun `registry requires contiguous migrations`() {
        assertFailsWith<IllegalArgumentException> { MigrationRegistry(3, listOf(SchemaMigrationDescriptor(1, 2, "1-2"))) }
    }
}
