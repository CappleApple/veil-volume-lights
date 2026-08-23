package com.cappleapple.veilvolumelights.client.render;

import com.cappleapple.veilvolumelights.api.client.VolumeLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds and coalesces transparent block volumes affected by a submitted light. */
final class TransparentMediumDetector {
    private static final float RAY_STEP = 0.40F;
    private static final float BOUNDS_EPSILON = 0.0005F;

    static List<TransparentMediumBuffer.Medium> find(ClientLevel level, VolumeLight light) {
        Map<Long, TransparentMediumBuffer.Medium> found = new LinkedHashMap<>();
        captureAt(level, light.position(), found);
        if (light instanceof VolumeLight.Point point) {
            scanPoint(level, point, found);
        } else if (light instanceof VolumeLight.Spot spot) {
            scanSpot(level, spot, found);
        } else if (light instanceof VolumeLight.Area area) {
            scanArea(level, area, found);
        }
        return mergeContiguous(new ArrayList<>(found.values()));
    }

    /** The source medium must not depend on whether a representative ray advances into it. */
    private static void captureAt(ClientLevel level, Vector3f source,
                                  Map<Long, TransparentMediumBuffer.Medium> found) {
        BlockPos position = BlockPos.containing(source.x, source.y, source.z);
        BlockState state = level.getBlockState(position);
        MediumProperties properties = properties(level, position, state);
        if (properties != null) {
            found.put(position.asLong(), createMedium(level, position, state, properties));
        }
    }

    private static void scanSpot(ClientLevel level, VolumeLight.Spot light,
                                 Map<Long, TransparentMediumBuffer.Medium> found) {
        Vector3f forward = new Vector3f(light.forward()).normalize();
        Vector3f up = new Vector3f(light.up()).normalize();
        Vector3f right = new Vector3f(forward).cross(up).normalize();
        float edgeScale = (float) Math.tan(Math.toRadians(light.outerConeAngleDegrees() * 0.5F));
        trace(level, light.position(), forward, light.range(), found);
        traceRing(level, light.position(), forward, right, up, edgeScale,
                light.range(), 0.25F, 8, found);
        traceRing(level, light.position(), forward, right, up, edgeScale,
                light.range(), 0.50F, 12, found);
        traceRing(level, light.position(), forward, right, up, edgeScale,
                light.range(), 0.75F, 16, found);
        traceRing(level, light.position(), forward, right, up, edgeScale,
                light.range(), 0.96F, 20, found);
    }

    private static void scanArea(ClientLevel level, VolumeLight.Area light,
                                 Map<Long, TransparentMediumBuffer.Medium> found) {
        Vector3f forward = new Vector3f(light.forward()).normalize();
        Vector3f up = new Vector3f(light.up()).normalize();
        Vector3f right = new Vector3f(forward).cross(up).normalize();
        float edgeScale = (float) Math.tan(Math.toRadians(light.spreadAngleDegrees() * 0.5F));
        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                Vector3f origin = new Vector3f(light.position())
                        .fma(horizontal * light.width() * 0.5F, right)
                        .fma(vertical * light.height() * 0.5F, up);
                trace(level, origin, forward, light.range(), found);
                traceRing(level, origin, forward, right, up, edgeScale,
                        light.range(), 0.92F, 8, found);
            }
        }
    }

    private static void scanPoint(ClientLevel level, VolumeLight.Point light,
                                  Map<Long, TransparentMediumBuffer.Medium> found) {
        Vector3f source = light.position();
        int radius = Math.max(1, (int) Math.ceil(light.range()));
        int minimumX = (int) Math.floor(source.x) - radius;
        int minimumY = Math.max(level.getMinBuildHeight(), (int) Math.floor(source.y) - radius);
        int minimumZ = (int) Math.floor(source.z) - radius;
        int maximumX = (int) Math.floor(source.x) + radius;
        int maximumY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(source.y) + radius);
        int maximumZ = (int) Math.floor(source.z) + radius;
        float rangeSquared = light.range() * light.range();
        List<Candidate> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int y = minimumY; y <= maximumY; y++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                for (int x = minimumX; x <= maximumX; x++) {
                    float centerX = x + 0.5F;
                    float centerY = y + 0.5F;
                    float centerZ = z + 0.5F;
                    float distanceSquared = source.distanceSquared(centerX, centerY, centerZ);
                    if (distanceSquared > rangeSquared) {
                        continue;
                    }
                    position.set(x, y, z);
                    BlockState state = level.getBlockState(position);
                    MediumProperties properties = properties(level, position, state);
                    if (properties != null) {
                        candidates.add(new Candidate(position.immutable(), state, properties, distanceSquared));
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distanceSquared));
        for (Candidate candidate : candidates) {
            if (found.size() >= TransparentMediumBuffer.MAX_MEDIA) {
                break;
            }
            if (unobstructed(level, source, candidate.position())) {
                found.put(candidate.position().asLong(), createMedium(
                        level, candidate.position(), candidate.state(), candidate.properties()
                ));
            }
        }
    }

    private static boolean unobstructed(ClientLevel level, Vector3f source, BlockPos target) {
        Vector3f targetCenter = new Vector3f(target.getX() + 0.5F, target.getY() + 0.5F, target.getZ() + 0.5F);
        Vector3f direction = targetCenter.sub(source, new Vector3f());
        float distance = direction.length();
        if (distance <= 0.001F) {
            return true;
        }
        direction.div(distance);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        long targetKey = target.asLong();
        long previous = Long.MIN_VALUE;
        for (float step = 0.0F; step < distance; step += RAY_STEP) {
            position.set(Math.floor(source.x + direction.x * step),
                    Math.floor(source.y + direction.y * step),
                    Math.floor(source.z + direction.z * step));
            long key = position.asLong();
            if (key == previous || key == targetKey) {
                continue;
            }
            previous = key;
            BlockState state = level.getBlockState(position);
            if (blocksBeam(level, position, state)) {
                return false;
            }
        }
        return true;
    }

    private static void traceRing(ClientLevel level, Vector3f source, Vector3f forward,
                                  Vector3f right, Vector3f up, float edgeScale, float range,
                                  float radius, int samples,
                                  Map<Long, TransparentMediumBuffer.Medium> found) {
        for (int sample = 0; sample < samples && found.size() < TransparentMediumBuffer.MAX_MEDIA; sample++) {
            double angle = Math.PI * 2.0 * sample / samples;
            Vector3f direction = new Vector3f(forward)
                    .fma((float) Math.cos(angle) * radius * edgeScale, right)
                    .fma((float) Math.sin(angle) * radius * edgeScale, up)
                    .normalize();
            trace(level, source, direction, range, found);
        }
    }

    private static void trace(ClientLevel level, Vector3f source, Vector3f direction, float range,
                              Map<Long, TransparentMediumBuffer.Medium> found) {
        long previousPosition = Long.MIN_VALUE;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (float distance = 0.0F;
             distance <= range && found.size() < TransparentMediumBuffer.MAX_MEDIA;
             distance += RAY_STEP) {
            position.set(Math.floor(source.x + direction.x * distance),
                    Math.floor(source.y + direction.y * distance),
                    Math.floor(source.z + direction.z * distance));
            long packedPosition = position.asLong();
            if (packedPosition == previousPosition) {
                continue;
            }
            previousPosition = packedPosition;
            BlockState state = level.getBlockState(position);
            MediumProperties properties = properties(level, position, state);
            if (properties != null) {
                found.computeIfAbsent(packedPosition, ignored -> createMedium(
                        level, position.immutable(), state, properties
                ));
            } else if (blocksBeam(level, position, state)) {
                return;
            }
        }
    }

    private static TransparentMediumBuffer.Medium createMedium(
            ClientLevel level, BlockPos position, BlockState state, MediumProperties properties) {
        VoxelShape shape = state.getShape(level, position);
        AABB bounds = shape.isEmpty() ? new AABB(position) : shape.bounds().move(position);
        return new TransparentMediumBuffer.Medium(
                new Vector3f((float) bounds.minX - BOUNDS_EPSILON,
                        (float) bounds.minY - BOUNDS_EPSILON,
                        (float) bounds.minZ - BOUNDS_EPSILON),
                new Vector3f((float) bounds.maxX + BOUNDS_EPSILON,
                        (float) bounds.maxY + BOUNDS_EPSILON,
                        (float) bounds.maxZ + BOUNDS_EPSILON),
                new Vector3f(properties.tint), properties.throughput,
                properties.densityBoost, properties.requiresSurfacePass
        );
    }

    private static MediumProperties properties(ClientLevel level, BlockPos position, BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) {
            return colored(BiomeColors.getAverageWaterColor(level, position), 0.62F, 0.98F, 2.50F);
        }
        Block block = state.getBlock();
        if (block instanceof StainedGlassBlock stainedGlass) {
            return dye(stainedGlass.getColor(), 0.98F, 2.50F);
        }
        if (block instanceof StainedGlassPaneBlock stainedGlassPane) {
            return dye(stainedGlassPane.getColor(), 0.98F, 2.50F);
        }
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE || block instanceof TransparentBlock) {
            return new MediumProperties(
                    new Vector3f(0.96F, 0.985F, 1.0F), 0.97F, 2.50F, false);
        }
        if (block instanceof IceBlock || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) {
            return new MediumProperties(
                    new Vector3f(0.72F, 0.88F, 1.0F), 0.90F, 2.30F, true);
        }
        return null;
    }

    private static MediumProperties dye(DyeColor dye, float throughput, float densityBoost) {
        return new MediumProperties(opticalDyeTint(dye), throughput, densityBoost, true);
    }

    private static Vector3f opticalDyeTint(DyeColor dye) {
        return switch (dye) {
            case WHITE -> new Vector3f(0.98F, 0.98F, 0.98F);
            case ORANGE -> new Vector3f(1.00F, 0.34F, 0.03F);
            case MAGENTA -> new Vector3f(1.00F, 0.04F, 0.78F);
            case LIGHT_BLUE -> new Vector3f(0.08F, 0.58F, 1.00F);
            case YELLOW -> new Vector3f(1.00F, 0.92F, 0.03F);
            case LIME -> new Vector3f(0.18F, 1.00F, 0.08F);
            case PINK -> new Vector3f(1.00F, 0.24F, 0.46F);
            case GRAY -> new Vector3f(0.22F, 0.23F, 0.24F);
            case LIGHT_GRAY -> new Vector3f(0.60F, 0.62F, 0.64F);
            case CYAN -> new Vector3f(0.03F, 1.00F, 1.00F);
            case PURPLE -> new Vector3f(0.46F, 0.04F, 1.00F);
            case BLUE -> new Vector3f(0.02F, 0.28F, 1.00F);
            case BROWN -> new Vector3f(0.36F, 0.13F, 0.03F);
            case GREEN -> new Vector3f(0.04F, 1.00F, 0.28F);
            case RED -> new Vector3f(1.00F, 0.04F, 0.02F);
            case BLACK -> new Vector3f(0.025F, 0.025F, 0.03F);
        };
    }

    private static MediumProperties colored(int color, float strength, float throughput, float densityBoost) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float brightest = Math.max(0.001F, Math.max(red, Math.max(green, blue)));
        red /= brightest;
        green /= brightest;
        blue /= brightest;
        return new MediumProperties(new Vector3f(
                1.0F + (red - 1.0F) * strength,
                1.0F + (green - 1.0F) * strength,
                1.0F + (blue - 1.0F) * strength
        ), throughput, densityBoost, true);
    }

    private static List<TransparentMediumBuffer.Medium> mergeContiguous(
            List<TransparentMediumBuffer.Medium> media) {
        List<TransparentMediumBuffer.Medium> merged = new ArrayList<>(media);
        boolean changed;
        do {
            changed = false;
            mergeSearch:
            for (int firstIndex = 0; firstIndex < merged.size(); firstIndex++) {
                for (int secondIndex = firstIndex + 1; secondIndex < merged.size(); secondIndex++) {
                    TransparentMediumBuffer.Medium combined = tryMerge(
                            merged.get(firstIndex), merged.get(secondIndex));
                    if (combined != null) {
                        merged.set(firstIndex, combined);
                        merged.remove(secondIndex);
                        changed = true;
                        break mergeSearch;
                    }
                }
            }
        } while (changed);
        return merged;
    }

    private static TransparentMediumBuffer.Medium tryMerge(
            TransparentMediumBuffer.Medium first, TransparentMediumBuffer.Medium second) {
        if (!first.tint().equals(second.tint(), 1.0E-5F)
                || Math.abs(first.throughput() - second.throughput()) >= 1.0E-5F
                || Math.abs(first.densityBoost() - second.densityBoost()) >= 1.0E-5F
                || first.requiresSurfacePass() != second.requiresSurfacePass()) {
            return null;
        }
        Vector3f firstMinimum = first.minimum();
        Vector3f firstMaximum = first.maximum();
        Vector3f secondMinimum = second.minimum();
        Vector3f secondMaximum = second.maximum();
        boolean mergeable = false;
        for (int axis = 0; axis < 3; axis++) {
            int otherA = (axis + 1) % 3;
            int otherB = (axis + 2) % 3;
            if (sameExtent(firstMinimum, firstMaximum, secondMinimum, secondMaximum, otherA)
                    && sameExtent(firstMinimum, firstMaximum, secondMinimum, secondMaximum, otherB)
                    && intervalsTouch(firstMinimum.get(axis), firstMaximum.get(axis),
                    secondMinimum.get(axis), secondMaximum.get(axis))) {
                mergeable = true;
                break;
            }
        }
        if (!mergeable) {
            return null;
        }
        return new TransparentMediumBuffer.Medium(
                new Vector3f(Math.min(firstMinimum.x, secondMinimum.x),
                        Math.min(firstMinimum.y, secondMinimum.y),
                        Math.min(firstMinimum.z, secondMinimum.z)),
                new Vector3f(Math.max(firstMaximum.x, secondMaximum.x),
                        Math.max(firstMaximum.y, secondMaximum.y),
                        Math.max(firstMaximum.z, secondMaximum.z)),
                new Vector3f(first.tint()), first.throughput(), first.densityBoost(),
                first.requiresSurfacePass());
    }

    private static boolean sameExtent(Vector3f firstMinimum, Vector3f firstMaximum,
                                      Vector3f secondMinimum, Vector3f secondMaximum, int axis) {
        return Math.abs(firstMinimum.get(axis) - secondMinimum.get(axis)) < 1.0E-4F
                && Math.abs(firstMaximum.get(axis) - secondMaximum.get(axis)) < 1.0E-4F;
    }

    private static boolean intervalsTouch(float firstMinimum, float firstMaximum,
                                          float secondMinimum, float secondMaximum) {
        return firstMaximum + 1.0E-4F >= secondMinimum
                && secondMaximum + 1.0E-4F >= firstMinimum;
    }

    private static boolean blocksBeam(ClientLevel level, BlockPos position, BlockState state) {
        return !state.isAir() && state.canOcclude() && state.getLightBlock(level, position) >= 15;
    }

    private record MediumProperties(
            Vector3f tint,
            float throughput,
            float densityBoost,
            boolean requiresSurfacePass
    ) {
    }

    private record Candidate(BlockPos position, BlockState state,
                             MediumProperties properties, float distanceSquared) {
    }

    private TransparentMediumDetector() {
    }
}
