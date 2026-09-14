package braseiro.ose.assets

import kotlin.test.*

class AssetManifestResolverTest {
    private fun resolver(): AssetManifestResolver {
        val text = checkNotNull(javaClass.getResource("/ASSET_MANIFEST.json")) { "Verified canonical manifest was not bundled for tests" }.readText()
        return AssetManifestResolver.parse(text)
    }

    @Test fun `canonical manifest loads and contains expected entries`() {
        val r = resolver()
        assertEquals(AssetManifestResolver.EXPECTED_SCHEMA, r.schema)
        assertTrue(r.assetCount() >= 101)
        assertTrue(r.contains("OSE_LIBRARY_A001"))
        assertTrue(r.contains("OSE_LIBRARY_A006"))
    }

    @Test fun `canonical exact asset resolves by semantic id and verified hash`() {
        val asset = resolver().resolve(AssetRequest(
            assetId = "OSE_LIBRARY_A006",
            expectedSha256 = "a00a2f0d2e62294facab468286988e0b21d5c7cf9907068c8c817a3fbda64329",
            expectedClassification = "EXACT_USER_ASSET"
        ))
        assertEquals("006_icone_pergaminho", asset.assetName)
        assertEquals("1TAW8B__Lz2-NzB-97VEnco4Hgqhh8sIa", asset.fileId)
    }

    @Test fun `hash mismatch is a typed presentation failure`() {
        assertFailsWith<AssetManifestException> {
            resolver().resolve(AssetRequest("OSE_LIBRARY_A006", expectedSha256 = "deadbeef"))
        }
    }

    @Test fun `missing canonical asset fails predictably`() {
        assertFailsWith<AssetManifestException> { resolver().resolve(AssetRequest("OSE_LIBRARY_DOES_NOT_EXIST")) }
    }

    @Test fun `geometry version contract can be required independently of asset semantics`() {
        val r = resolver()
        val actual = r.manifestGeometryVersion
        assertEquals(1, actual)
        r.resolve(AssetRequest("OSE_LIBRARY_A006", requiredGeometryVersion = 1))
    }
}
