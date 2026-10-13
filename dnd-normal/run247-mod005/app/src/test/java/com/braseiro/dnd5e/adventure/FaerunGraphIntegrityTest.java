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
            assertNotNull(module.getId(), nodes.get("completed"));

            Set<String> visited = new HashSet<>();
            ArrayDeque<String> pending = new ArrayDeque<>();
            pending.add("setup");
            int meaningfulTransitions = 0;
            while (!pending.isEmpty()) {
                String nodeId = pending.removeFirst();
                if (!visited.add(nodeId)) continue;
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
                            nodes.containsKey(target));
                    if (transition.getEffects().length() > 0) meaningfulTransitions++;
                    pending.addLast(target);
                }
            }
            assertTrue(module.getId() + ": ending cannot be reached", visited.contains("completed"));
            assertEquals(module.getId() + ": unreachable scenes or orphan nodes", nodes.keySet(), visited);
            assertTrue(module.getId() + ": consequences absent from player choices",
                    meaningfulTransitions >= 2);
        }
    }
}
