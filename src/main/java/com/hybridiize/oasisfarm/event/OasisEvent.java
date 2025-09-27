package com.hybridiize.oasisfarm.event;

import java.util.List;

/**
 * The main container for a complete event loaded from events.yml.
 */
public class OasisEvent {
    private final String id;
    private final String targetFarm;
    private final int cooldown;
    private final int eventRadius;
    private final List<String> onEndCommands;
    private final List<EventCondition> conditions;
    private final List<EventPhase> phases;

    public OasisEvent(String id, String targetFarm, int cooldown, List<EventCondition> conditions, List<EventPhase> phases, int eventRadius, List<String> onEndCommands) {
        this.id = id;
        this.targetFarm = targetFarm;
        this.cooldown = cooldown;
        this.conditions = conditions;
        this.phases = phases;
        this.eventRadius = eventRadius;
        this.onEndCommands = onEndCommands;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getTargetFarm() { return targetFarm; }
    public int getCooldown() { return cooldown; }
    public List<EventCondition> getConditions() { return conditions; }
    public List<EventPhase> getPhases() { return phases; }
    public int getEventRadius() { return eventRadius; }
    public List<String> getOnEndCommands() { return onEndCommands; }
}