package com.hybridiize.oasisfarm.farm;

import org.bukkit.Location;

public class Region {
    private final Location pos1;
    private final Location pos2;

    public Region(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public boolean contains(Location loc) {
        if (loc == null || !loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public Location getCenter() {
        double x = (pos1.getX() + pos2.getX()) / 2.0;
        double y = (pos1.getY() + pos2.getY()) / 2.0;
        double z = (pos1.getZ() + pos2.getZ()) / 2.0;
        return new Location(pos1.getWorld(), x, y, z);
    }
}