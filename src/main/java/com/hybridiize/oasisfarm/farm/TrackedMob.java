package com.hybridiize.oasisfarm.farm;

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