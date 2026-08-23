package com.cappleapple.veilvolumelights.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestLightSourceTracker {
    private static final Map<Long, TrackedSource> SOURCES = new LinkedHashMap<>();

    public static void see(Level level, BlockPos pos, Direction facing, TestLightShape shape) {
        SOURCES.put(pos.asLong(), new TrackedSource(
                new Source(pos.immutable(), facing, shape), level.getGameTime()));
    }

    public static Collection<Source> active(Level level) {
        long currentTick = level.getGameTime();
        SOURCES.values().removeIf(source -> source.seenTick() < currentTick - 1L);
        return SOURCES.values().stream().map(TrackedSource::source).toList();
    }

    public static void clear() {
        SOURCES.clear();
    }

    public record Source(BlockPos pos, Direction facing, TestLightShape shape) {
    }

    private record TrackedSource(Source source, long seenTick) {
    }

    private TestLightSourceTracker() {
    }
}
