package dev.mianbaosablecompat;

final class ChunkCoverage {
    @FunctionalInterface
    interface ChunkConsumer {
        void accept(int chunkX, int chunkZ);
    }

    static void visit(double minX, double minZ, double maxX, double maxZ, ChunkConsumer consumer) {
        if (!Double.isFinite(minX + minZ + maxX + maxZ) || maxX < minX || maxZ < minZ) return;
        int firstX = chunk(minX);
        int firstZ = chunk(minZ);
        int lastX = maxX > minX ? exclusiveMaxChunk(maxX) : chunk(maxX);
        int lastZ = maxZ > minZ ? exclusiveMaxChunk(maxZ) : chunk(maxZ);
        for (int chunkX = firstX; chunkX <= lastX; chunkX++)
            for (int chunkZ = firstZ; chunkZ <= lastZ; chunkZ++)
                consumer.accept(chunkX, chunkZ);
    }

    private static int chunk(double coordinate) {
        return (int)Math.floor(coordinate / 16.0);
    }

    private static int exclusiveMaxChunk(double coordinate) {
        return (int)Math.ceil(coordinate / 16.0) - 1;
    }

    private ChunkCoverage() {}
}
