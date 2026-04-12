package com.settlements.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class BlockPosKeyUtil {
    private BlockPosKeyUtil() {
    }

    public static String toKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    public static String getDimensionId(String key) {
        String[] parts = split(key);
        return parts == null ? "" : parts[0];
    }

    public static BlockPos fromKey(String key) {
        String[] parts = split(key);
        if (parts == null) {
            return BlockPos.ZERO;
        }

        try {
            return new BlockPos(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        } catch (NumberFormatException ignored) {
            return BlockPos.ZERO;
        }
    }

    public static String toChunkKey(String key) {
        String[] parts = split(key);
        if (parts == null) {
            return "";
        }

        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[3]);
            return parts[0] + "|" + (x >> 4) + "|" + (z >> 4);
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private static String[] split(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String[] parts = key.split("\\|");
        if (parts.length != 4) {
            return null;
        }
        return parts;
    }
}