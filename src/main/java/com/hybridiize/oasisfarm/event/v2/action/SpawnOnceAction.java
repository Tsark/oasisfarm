package com.hybridiize.oasisfarm.event.v2.action;

import com.hybridiize.oasisfarm.event.v2.PhaseAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnOnceAction implements PhaseAction {
    private final List<Map<String, Object>> mobsToSpawn;

    @SuppressWarnings("unchecked")
    public SpawnOnceAction(Map<?, ?> data) {
        this.mobsToSpawn = new ArrayList<>();
        Object rawMobs = data.get("mobs");

        // Check if "mobs" is a List, which it should be from the YAML
        if (rawMobs instanceof List) {
            List<?> rawList = (List<?>) rawMobs;
            for (Object item : rawList) {
                // Check if each item in the list is a Map
                if (item instanceof Map) {
                    // This cast is now much safer because we've checked the types.
                    this.mobsToSpawn.add((Map<String, Object>) item);
                }
            }
        }
    }

    public List<Map<String, Object>> getMobsToSpawn() {
        return mobsToSpawn;
    }

    @Override
    public String getType() {
        return "SPAWN_ONCE";
    }

    @Override
    public void execute() {}
}