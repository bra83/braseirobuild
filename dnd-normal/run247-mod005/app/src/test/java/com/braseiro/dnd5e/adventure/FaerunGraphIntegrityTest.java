package com.braseiro.dnd5e.adventure;

import org.junit.Test;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Static contract tests for reachable Faerun scenes, valid route targets,
 * distinct choice IDs, and explicit effects. Independent of the narrative
 * smoke tests and of external copyrighted source material.
 */
public final class FaerunGraphIntegrityTest {
    @Test
    public void allFiveGraphsHaveReachableValidTargetsAndNoDuplicateChoiceIds() {
        List<AdventureModule> modules = FaerunAdventureModuleCatalog.all();
        assertEquals(5, modules.size());
        for (AdventureModule module : modules) {
            Map<String, AdventureNode> nodes = module.getNodes();
            assertNotNull(module.getId(), nodes.get("setup"));
            // "completed" is the AdventureEngine terminal sentinel, not a stored node.

            Set<String> visited = new HashSet<>();
            ArrayDeque<String> pending = new ArrayDeque<>();
            pending.add("setup");
            int meaningfulTransitions = 0;
            while (!pending.isEmpty()) {
                String nodeId = pending.removeFirst();
                if (!visited.add(nodeId)) continue;
                if ("completed".equals(nodeId)) continue;
                AdventureNode node = nodes.get(nodeId);
                assertNotNull(module.getId() + " has dangling target " + nodeId, node);
                Set<String> actionIds = new HashSet<>();
                for (AdventureTransition transition : node.getTransitions()) {
                    assertFalse(module.getId() + "/" + nodeId + ": missing action ID",
                            transition.getId().trim().isEmpty());
                    assertTrue(module.getId() + "/" + nodeId + ": duplicate action " + transition.getId(),
                            actionIds.add(transition.getId()));
                    String target = transition.getTargetNodeId();
                    assertTrue(module.getId() + "/" + nodeId + ": dangling target " + target,
                            "completed".equals(target) || nodes.containsKey(target));
                    if (transition.getEffects().length() > 0) meaningfulTransitions++;
                    pending.addLast(target);
                }
            }
            assertTrue(module.getId() + ": ending cannot be reached", visited.contains("completed"));
            Set<String> reachableScenes = new HashSet<>(visited);
            reachableScenes.remove("completed");
            assertEquals(module.getId() + ": unreachable scenes or orphan nodes", nodes.keySet(), reachableScenes);
            assertTrue(module.getId() + ": consequences absent from player choices",
                    meaningfulTransitions >= 2);
            int divergentDecisionPoints = 0;
            for (AdventureNode node : nodes.values()) {
                if (node.getTransitions().size() < 2) continue;
                Set<String> distinctOutcomes = new HashSet<>();
                for (AdventureTransition option : node.getTransitions()) {
                    assertTrue(module.getId() + "/" + node.getId() + "/" + option.getId()
                                    + ": player choice without durable consequence",
                            option.getEffects() != null && option.getEffects().length() > 0);
                    distinctOutcomes.add(option.getTargetNodeId() + "|" + option.getEffects().toString());
                }
                if (distinctOutcomes.size() > 1) divergentDecisionPoints++;
            }
            assertTrue(module.getId() + ": no materially different choices",
                    divergentDecisionPoints >= 1);
        }
    }
    @Test
    public void publishedReferencesAreExplicitlySummarizedAndOriginalsRemainOriginal() {
        for (AdventureModule module : FaerunAdventureModuleCatalog.all()) {
            boolean publishedReference = "fraif_tomb_damara".equals(module.getId())
                    || "fraif_lost_library".equals(module.getId());
            assertEquals(module.getId(), publishedReference,
                    module.getMetadata().optBoolean("is_official_published"));
            assertEquals(module.getId(),
                    publishedReference ? "official_published_adventure_adaptation"
                            : "original_scenario_compatible",
                    module.getMetadata().optString("source_fidelity"));
            assertTrue(module.getId() + ": misleading displayed origin",
                    module.getTitle().contains(publishedReference
                            ? "adaptação resumida" : "Original"));
        }
    }
}
