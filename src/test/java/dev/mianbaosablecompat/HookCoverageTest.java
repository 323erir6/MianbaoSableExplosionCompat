package dev.mianbaosablecompat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.jar.JarFile;

/** Verify optional redirects against the real supported jar, not source guesses. */
public final class HookCoverageTest {
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i=0;i<annotation.values.size();i+=2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i+1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a) {
        return a == null ? List.of() : a;
    }
    public static void main(String[] args) throws Exception {
        ClassNode mixin = new ClassNode();
        try (var in = HookCoverageTest.class.getResourceAsStream("/dev/mianbaosablecompat/mixin/DirectExplosionMixin.class")) {
            new ClassReader(Objects.requireNonNull(in)).accept(mixin, 0);
        }
        List<String> targets = new ArrayList<>();
        for (AnnotationNode a : annotations(mixin.invisibleAnnotations))
            if (a.desc.endsWith("/Mixin;")) {
                for (Object t : (List<?>) value(a, "targets")) targets.add(t.toString().replace('.','/'));
            }
        Set<String> redirects = new HashSet<>(), wrappers = new HashSet<>();
        for (MethodNode m : mixin.methods) for (AnnotationNode a : annotations(m.visibleAnnotations)) {
            if (a.desc.endsWith("/Redirect;")) redirects.add(value((AnnotationNode)value(a,"at"),"target").toString());
            if (a.desc.endsWith("/WrapMethod;"))
                for (Object method : (List<?>)value(a,"method")) wrappers.add(method.toString());
        }
        if (targets.isEmpty() || redirects.isEmpty() || wrappers.isEmpty()) throw new AssertionError("Missing mixin metadata");
        int calls=0, writes=0, frames=0;
        Set<String> missed=new TreeSet<>();
        try (JarFile jar = new JarFile(args[0])) {
            for (String t : targets) {
                var entry=jar.getJarEntry(t+".class");
                if (entry==null) throw new AssertionError("Missing target "+t);
                ClassNode c=new ClassNode();
                new ClassReader(jar.getInputStream(entry)).accept(c,0);
                for (MethodNode m:c.methods) {
                    if (wrappers.contains(m.name+m.desc)) frames++;
                    for (var instruction:m.instructions) {
                        if (!(instruction instanceof MethodInsnNode call)) continue;
                        boolean block = Set.of("getBlockState","destroyBlock","setBlock","canSurvive").contains(call.name)
                                && call.owner.startsWith("net/minecraft/");
                        boolean query=call.name.equals("getEntitiesOfClass");
                        boolean guard=call.owner.equals("net/mcreator/myfirstmod/util/ExplosionBlockGuard") && call.name.equals("canDestroy");
                        if (!block && !query && !guard) continue;
                        String key="L"+call.owner+";"+call.name+call.desc;
                        if (!redirects.contains(key)) missed.add(t+"."+m.name+": "+key);
                        else {
                            calls++;
                            if (call.name.equals("destroyBlock") || call.name.equals("setBlock")) writes++;
                        }
                    }
                }
            }
        }
        for (String r:redirects) if (r.contains(";explode(")) throw new AssertionError("Must not duplicate vanilla explosions");
        if (!missed.isEmpty()) throw new AssertionError("Uncovered calls:\n"+String.join("\n",missed));
        if (writes<20 || frames<40) throw new AssertionError("Insufficient coverage");
        System.out.println("PASS: "+targets.size()+" targets, "+calls+" redirected calls, "+writes+" writes, "+frames+" scoped methods; native explosions untouched");
    }
}
