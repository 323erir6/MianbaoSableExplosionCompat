package dev.mianbaosablecompat;

import org.joml.Matrix4dc;
import org.joml.Vector3d;

/** Voxel-center sampling. Each target block belongs to one source cell only. */
public final class CellProjection {
    @FunctionalInterface
    public interface CellConsumer { void accept(int x, int y, int z); }

    public static void visit(Matrix4dc forward, Matrix4dc inverse,
                             int x, int y, int z, CellConsumer consumer) {
        Vector3d v = new Vector3d();
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < 8; i++) {
            forward.transformPosition(v.set(x + (i & 1), y + ((i >> 1) & 1), z + ((i >> 2) & 1)));
            minX = Math.min(minX, v.x); minY = Math.min(minY, v.y); minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x); maxY = Math.max(maxY, v.y); maxZ = Math.max(maxZ, v.z);
        }
        if (!Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ)) return;
        // Centers, not the much larger transformed AABB, decide membership.
        for (int tx = (int)Math.ceil(minX - 0.5); tx <= Math.floor(maxX - 0.5); tx++)
            for (int ty = (int)Math.ceil(minY - 0.5); ty <= Math.floor(maxY - 0.5); ty++)
                for (int tz = (int)Math.ceil(minZ - 0.5); tz <= Math.floor(maxZ - 0.5); tz++) {
                    inverse.transformPosition(v.set(tx + 0.5, ty + 0.5, tz + 0.5));
                    // Tiny tolerance only to stabilize integer boundaries after large plot translations.
                    if (inside(v.x - x) && inside(v.y - y) && inside(v.z - z))
                        consumer.accept(tx, ty, tz);
                }
    }
    private static boolean inside(double n) { return n >= -1e-7 && n < 1.0 - 1e-7; }
    private CellProjection() {}
}
