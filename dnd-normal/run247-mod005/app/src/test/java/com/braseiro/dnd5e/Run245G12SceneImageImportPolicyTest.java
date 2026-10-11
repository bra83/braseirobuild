package com.braseiro.dnd5e;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Focused G12 coverage for deterministic scene-image intent admission. */
public class Run245G12SceneImageImportPolicyTest {
    @Test public void acceptsImageShareOnlyForCurrentScene() {
        SceneImageImportPolicy.Result result = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, " IMAGE/PNG ", true, "scene-7", "scene-7");
        assertTrue(result.accepted);
        assertEquals("", result.message);
    }

    @Test public void rejectsShareAndConsumeFromAnObsoleteScene() {
        SceneImageImportPolicy.Result share = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/jpeg", true, "scene-6", "scene-7");
        SceneImageImportPolicy.Result consume = SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_CONSUME, "image/jpeg", true, "scene-6", "scene-7");
        assertFalse(share.accepted);
        assertFalse(consume.accepted);
        assertEquals("A imagem pertence a uma cena anterior.", share.message);
        assertEquals(share.message, consume.message);
    }

    @Test public void rejectsWrongActionMimeAndMissingUri() {
        assertFalse(SceneImageImportPolicy.validate("other", "image/png", true, "", "").accepted);
        assertFalse(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "application/pdf", true, "", "").accepted);
        assertFalse(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_SEND, "image/png", false, "", "").accepted);
        assertFalse(SceneImageImportPolicy.validate(
                SceneImageImportPolicy.ACTION_CONSUME, "image/png", false, "", "").accepted);
    }
}
