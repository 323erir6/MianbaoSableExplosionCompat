package dev.mianbaosablecompat;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ChunkCoverageTest {
    public static void main(String[] args) {
        expect(Set.of("0,0"), 0, 0, 16, 16);
        expect(Set.of("-1,-1"), -16, -16, 0, 0);
        expect(Set.of("1,1"), 16, 16, 16, 16);
        expect(Set.of("0,-1", "0,0", "1,-1", "1,0"), 15.9, -0.1, 16.1, 0.1);
        expect(Set.of("0,0", "0,1", "1,0", "1,1"), 0, 0, 32, 32);
        System.out.println("PASS: exact, boundary, point and negative chunk coverage");
    }

    private static void expect(Set<String> expected, double minX, double minZ, double maxX, double maxZ) {
        Set<String> actual = new LinkedHashSet<>();
        ChunkCoverage.visit(minX, minZ, maxX, maxZ,
                (chunkX, chunkZ) -> actual.add(chunkX + "," + chunkZ));
        if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
