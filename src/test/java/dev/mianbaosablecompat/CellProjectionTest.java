package dev.mianbaosablecompat;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import java.util.HashSet;
import java.util.Set;

/** Standalone tests: no Minecraft bootstrap or running world required. */
public final class CellProjectionTest {
    private static Set<String> cells(Matrix4dc m, int x, int y, int z) {
        Set<String> result = new HashSet<>();
        CellProjection.visit(m, new Matrix4d(m).invert(), x, y, z, (a, b, c) -> {
            if (!result.add(a + "," + b + "," + c)) throw new AssertionError("duplicate cell");
        });
        return result;
    }
    private static void equal(Object actual, Object expected) {
        if (!actual.equals(expected)) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        equal(cells(new Matrix4d(), 0, 0, 0), Set.of("0,0,0"));
        equal(cells(new Matrix4d(), -17, -64, -1), Set.of("-17,-64,-1"));
        equal(cells(new Matrix4d().translation(-100, 450, 200), 0, 0, 0), Set.of("-100,450,200"));
        equal(cells(new Matrix4d().rotationY(Math.PI / 2), 0, 0, 0), Set.of("0,0,-1"));
        equal(cells(new Matrix4d().rotationZ(Math.PI / 2), 0, 0, 0), Set.of("-1,0,0"));
        equal(cells(new Matrix4d().scaling(2), 0, 0, 0).size(), 8);
        equal(cells(new Matrix4d().scaling(2, 3, 1), 0, 0, 0).size(), 6);
        Matrix4d plot = new Matrix4d().translation(100, 70, -200).rotateY(Math.PI/2)
                .translate(-30_000_000, -100, -30_000_000);
        equal(cells(plot, 30_000_000, 100, 30_000_000), Set.of("100,70,-201"));
        Matrix4d toOther = new Matrix4d().translation(30_000_000, 100, 30_000_000)
                .rotateY(-Math.PI/2).translate(-100,-70,200);
        equal(cells(toOther, 100,70,-201), Set.of("30000000,100,30000000"));
        // Arbitrary bank/scale: tiling adjacent source cells must neither
        // duplicate target centers nor use their entire enclosing AABB.
        Matrix4d tilted = new Matrix4d().translation(0.1,0.2,0.3).rotateXYZ(.4,.7,.2).scale(1.5,.8,2);
        Set<String> union = new HashSet<>();
        for (int x=-2; x<=2; x++) for (int y=-2; y<=2; y++) for (int z=-2; z<=2; z++)
            for (String cell : cells(tilted,x,y,z))
                if (!union.add(cell)) throw new AssertionError("overlap between source cells");
        if (union.size() < 200 || union.size() > 400) throw new AssertionError("bad transformed volume " + union.size());
        equal(cells(new Matrix4d().scaling(Double.NaN), 0,0,0), Set.of());
        System.out.println("PASS: identity, negative coordinates, translation, rotations, scale, plot/world round-trip, tiling, invalid pose");
    }
}
