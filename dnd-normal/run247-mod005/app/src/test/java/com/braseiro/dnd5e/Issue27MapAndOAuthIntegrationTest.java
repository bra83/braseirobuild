package com.braseiro.dnd5e;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

/**
 * Suite de Verificação e Homologação para a Issue #27:
 * - Validação do pacote integrado Atlas de Faerûn V2 nos assets.
 * - Parsing da geometria de 21600x15390 e hexId.
 * - Preservação estrita de saves (hex atual, névoa, rota, transformações).
 * - Certificado estável debug e matching exato com SHA-1 / SHA-256 OAuth.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Issue27MapAndOAuthIntegrationTest {

    private static final String EXPECTED_SHA1 = "6F:47:92:78:1F:11:ED:FF:C8:92:AF:EB:95:69:4E:3C:03:F3:5C:DF";
    private static final String EXPECTED_SHA256 = "6A:A8:7A:24:A2:EB:47:62:C3:F4:93:C9:D5:FD:DB:F1:0B:60:CC:86:59:A9:04:23:AE:D7:CD:E8:AE:58:2B:D3";

    @Test
    public void testBundledFaerunAssetLocationAndPresence() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String assetPath = MapPackageManager.latestBundledFaerunAsset(context);
        assertNotNull("Caminho do asset bundled não deve ser nulo", assetPath);
        assertEquals("maps/PACOTE_MAPA_FAERUN_2025_PTBR_MAX_V2.zip", assetPath);

        // Verifica abertura do stream
        try (InputStream stream = context.getAssets().open(assetPath)) {
            assertNotNull("Stream do asset deve abrir com sucesso", stream);
            byte[] header = new byte[4];
            int read = stream.read(header);
            assertEquals(4, read);
            // Assinatura ZIP PK (0x50, 0x4B, 0x03, 0x04)
            assertEquals((byte) 0x50, header[0]);
            assertEquals((byte) 0x4B, header[1]);
        }
    }

    @Test
    public void testFaerunManifestGeometryParsing() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String assetPath = MapPackageManager.latestBundledFaerunAsset(context);
        File tempZip = new File(context.getCacheDir(), "test_faerun_manifest.zip");
        try (InputStream in = context.getAssets().open(assetPath);
             java.io.FileOutputStream out = new java.io.FileOutputStream(tempZip)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }

        try (ZipFile zip = new ZipFile(tempZip)) {
            ZipEntry manifestEntry = zip.getEntry("map_manifest.json");
            assertNotNull("map_manifest.json deve existir no pacote", manifestEntry);
            String manifestJson;
            try (InputStream entryStream = zip.getInputStream(manifestEntry)) {
                manifestJson = new String(entryStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            JSONObject manifest = new JSONObject(manifestJson);
            assertEquals("faerun-atlas-2025-ptbr-21600x15390-v2-max", manifest.optString("mapId"));
            assertEquals("dnd", manifest.optString("worldProfile"));

            MapManifestGeometry geometry = MapManifestGeometry.parse(manifestJson);
            assertNotNull("Geometria não deve ser nula", geometry);
            assertEquals(21600f, geometry.getImageWidth(), 0.1f);
            assertEquals(15390f, geometry.getImageHeight(), 0.1f);
            assertEquals("flat-top", geometry.getOrientation());
            assertEquals(10f, geometry.getHexScaleMiles(), 0.1f);

            // A1 deve ser o primeiro hex
            MapManifestGeometry.ManifestHex a1 = geometry.byId("A1");
            assertNotNull("Hex A1 deve existir na geometria", a1);
            assertEquals(36.0f, a1.centerX, 0.5f);
            assertEquals(31.18f, a1.centerY, 0.5f);
            assertEquals(98600, geometry.size());
        } finally {
            if (tempZip.exists()) tempZip.delete();
        }
    }

    @Test
    public void testPartyStateAndFogPreservationInAppDb() {
        Context context = RuntimeEnvironment.getApplication();
        AppDb db = new AppDb(context);

        // Simula estado salvo existente do perfil dnd
        String initialPackageId = "faerun-initial-pkg";
        String partyHex = "NW32";
        String reveals = "{\"hexIds\":[\"NW32\",\"NW33\",\"NW34\"],\"points\":[]}";
        float savedScale = 2.4f;
        float savedOx = 120.5f;
        float savedOy = 85.0f;

        db.saveMapPackageStateForProfile("dnd", "file:///maps/map.jpg", "", "{}", initialPackageId,
                partyHex, true, reveals, "Hexagonal flat-top", 72f, true,
                0.45f, 0.55f, savedScale, savedOx, savedOy);

        JSONObject stateBefore = db.getMapStateForProfile("dnd");
        assertEquals(partyHex, stateBefore.optString("current_hex_id"));
        assertEquals(1, stateBefore.optInt("fog_enabled"));
        assertEquals(savedScale, (float) stateBefore.optDouble("scale"), 0.001f);
        assertEquals(savedOx, (float) stateBefore.optDouble("offset_x"), 0.001f);
        assertEquals(savedOy, (float) stateBefore.optDouble("offset_y"), 0.001f);

        // Simula atualização com preservação de estado
        String updatedPackageId = "faerun-atlas-2025-ptbr-21600x15390-v2-max@12345";
        db.saveMapPackageStateForProfile("dnd", "file:///maps/updated_map.jpg", "", "{}", updatedPackageId,
                partyHex, true, reveals, "Hexagonal flat-top", 72f, true,
                0.45f, 0.55f, savedScale, savedOx, savedOy);

        JSONObject stateAfter = db.getMapStateForProfile("dnd");
        assertEquals("Posição do grupo deve ser mantida", partyHex, stateAfter.optString("current_hex_id"));
        assertEquals("Névoa revelada deve ser preservada", reveals, stateAfter.optString("reveals_json"));
        assertEquals("Escala salva deve ser preservada", savedScale, (float) stateAfter.optDouble("scale"), 0.001f);
        assertEquals(updatedPackageId, stateAfter.optString("map_package_id"));
    }

    @Test
    public void testOAuthSigningCertificateFingerprints() throws Exception {
        File keystoreFile = new File("signing/debug.keystore");
        if (!keystoreFile.exists()) {
            keystoreFile = new File("../signing/debug.keystore");
        }
        assertTrue("signing/debug.keystore deve existir na raiz do projeto", keystoreFile.exists());

        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, "android".toCharArray());
        }

        assertTrue("Alias 'androiddebugkey' deve existir", ks.containsAlias("androiddebugkey"));
        Certificate cert = ks.getCertificate("androiddebugkey");
        assertNotNull("Certificado não deve ser nulo", cert);
        assertTrue("Deve ser X509Certificate", cert instanceof X509Certificate);

        X509Certificate x509 = (X509Certificate) cert;
        String dn = x509.getSubjectX500Principal().getName();
        assertTrue("DN deve conter CN=Braseiro Stable Debug", dn.contains("CN=Braseiro Stable Debug"));

        byte[] der = x509.getEncoded();
        String sha1Hex = formatFingerprint(MessageDigest.getInstance("SHA-1").digest(der));
        String sha256Hex = formatFingerprint(MessageDigest.getInstance("SHA-256").digest(der));

        assertEquals("SHA-1 do certificado deve ser exatamente o cadastrado no OAuth", EXPECTED_SHA1, sha1Hex);
        assertEquals("SHA-256 do certificado deve ser exatamente o cadastrado no OAuth", EXPECTED_SHA256, sha256Hex);
    }

    private static String formatFingerprint(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
