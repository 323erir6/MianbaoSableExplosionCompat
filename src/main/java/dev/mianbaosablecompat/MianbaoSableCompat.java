package dev.mianbaosablecompat;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("mianbao_sable_explosion_compat")
public final class MianbaoSableCompat {
    public MianbaoSableCompat(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, CompatConfig.SPEC);
    }
}
