package com.hybridiize.oasisfarm.managers;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingConfirmationManager {
    // Map<PlayerUUID, FarmID being edited>
    private final Map<UUID, String> pendingResizes = new HashMap<>();

    public void setPendingResize(Player player, String farmId) {
        pendingResizes.put(player.getUniqueId(), farmId);
    }

    public String getPendingResize(Player player) {
        return pendingResizes.get(player.getUniqueId());
    }

    public void clearPending(Player player) {
        pendingResizes.remove(player.getUniqueId());
    }
}