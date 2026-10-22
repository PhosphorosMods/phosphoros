package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/** Sound events of 1.1.0 (files + subtitles in assets/lucifertotem/sounds.json). Registered on both sides (the server plays them). */
public class HSounds {
    public static final List<SoundEvent> ALL = new ArrayList<>();
    /** one event per Doctor Ammonius voice clip (index = AmmoniusClips) */
    public static SoundEvent[] LINES = new SoundEvent[0];
    public static SoundEvent PURPLE_SHOUT, MANDRAKE_SCREAM, MANDRAKE_SQUEAK, AMMONIUS_IDLE, AMMONIUS_HURT, AMMONIUS_DEATH, AMMONIUS_ATTACK, AMMONIUS_PURPLE,
        DISTANT_AMMONIUS, DIPSAS_HISS, RITUAL_SNAP, STAR_FALL, PYTHIA_SPEAK, TROPHONIUS_WHISPER, ALABASTRON_DOSE, ALABASTRON_BONK, SQUISH_SMALL, AWW_GAWD;

    private static SoundEvent s(String name) {
        ResourceLocation rl = new ResourceLocation(LuciferTotem.MODID, name);
        SoundEvent e = new SoundEvent(rl).setRegistryName(rl);
        ALL.add(e);
        return e;
    }

    public static void register(IForgeRegistry<SoundEvent> r) {
        PURPLE_SHOUT = s("purple_shout");
        MANDRAKE_SCREAM = s("mandrake_scream");
        MANDRAKE_SQUEAK = s("mandrake_squeak");
        AMMONIUS_IDLE = s("ammonius.idle");
        AMMONIUS_HURT = s("ammonius.hurt");
        AMMONIUS_DEATH = s("ammonius.death");
        AMMONIUS_ATTACK = s("ammonius.attack");
        AMMONIUS_PURPLE = s("ammonius.purple");
        DISTANT_AMMONIUS = s("ambient.distant_ammonius");
        DIPSAS_HISS = s("dipsas.hiss");
        RITUAL_SNAP = s("ritual.snap");
        STAR_FALL = s("ritual.star_fall");
        PYTHIA_SPEAK = s("pythia.speak");
        TROPHONIUS_WHISPER = s("trophonius.whisper");
        ALABASTRON_DOSE = s("alabastron.dose");
        ALABASTRON_BONK = s("alabastron.bonk");
        SQUISH_SMALL = s("squish.small");
        AWW_GAWD = s("ammonius.aww_gawd");   // 1.2.0: the first touch of the totem // 1.12's own entity.small_slime.squish event is empty (silent)
        LINES = new SoundEvent[AmmoniusClips.NAME.length];
        for (int i = 0; i < LINES.length; i++) LINES[i] = s("ammonius.line." + AmmoniusClips.NAME[i]);
        lucifertotem.v130.V130.sounds();
        for (SoundEvent e : ALL) r.register(e);
    }
}
