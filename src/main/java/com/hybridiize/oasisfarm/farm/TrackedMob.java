package com.hybridiize.oasisfarm.farm;

/**
 * A simple data container to hold runtime information about a mob
 * spawned by the plugin.
 */
public class TrackedMob {
    private final String farmId;
    private final String templateId;

    public TrackedMob(String farmId, String templateId) {
        this.farmId = farmId;
        this.templateId = templateId;
    }

    public String getFarmId() {
        return farmId;
    }

    public String getTemplateId() {
        return templateId;
    }
}