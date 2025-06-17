package com.hybridiize.oasisfarm.farm;

import org.bukkit.Location;

public class Region {
    private final Location pos1;
    private final Location pos2;

    public Region(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    // --- NEW GETTER METHODS ---
    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }
    // ... inside the Region class ...

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}