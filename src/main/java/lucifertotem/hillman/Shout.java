package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/** The PURPLE! easter egg: a bold violet chat line, a shout and a burst of violet spell particles. Server side only. */
public class Shout {
    public static int shouts = 0, alternates = 0; // counters for the test harness
    public static String lastLine = "";
    static final Random RNG = new Random();
    /** paraphrased parody lines (Doctor Ammonius the Gadfly); used 1 time in 50 instead of PURPLE! */
    public static final String[] ALT = {"shout.lucifertotem.alt.1", "shout.lucifertotem.alt.2", "shout.lucifertotem.alt.3", "shout.lucifertotem.alt.4", "shout.lucifertotem.alt.5"};

    /** shout at/around a player: everyone within 24 blocks sees the line */
    public static void shout(EntityPlayer target, String reason) {
        if (target == null || target.world.isRemote || !LTConfig.purpleShout) return;
        shoutAt(target.world, target, reason, HSounds.PURPLE_SHOUT);
    }

    public static void shoutAt(World w, Entity at, String reason, SoundEvent sound) { shoutAt(w, at, reason, sound, false); }

    /** fromAmmonius: the line is spoken by Doctor Ammonius ("<Doctor Ammonius> PURPLE!") */
    public static void shoutAt(World w, Entity at, String reason, SoundEvent sound, boolean fromAmmonius) {
        if (w.isRemote || !LTConfig.purpleShout) return;
        shouts++;
        ITextComponent line;
        if (fromAmmonius) {
            ITextComponent p = new TextComponentString("PURPLE!");
            p.getStyle().setColor(TextFormatting.DARK_PURPLE).setBold(true);
            line = new TextComponentTranslation("chat.lucifertotem.ammonius_says", p);
        } else if (RNG.nextInt(50) == 0) {
            alternates++;
            line = new TextComponentTranslation("shout.lucifertotem.ammonius", new TextComponentTranslation(ALT[RNG.nextInt(ALT.length)]));
            line.getStyle().setColor(TextFormatting.LIGHT_PURPLE).setItalic(true);
        } else {
            line = new TextComponentString("PURPLE!");
            line.getStyle().setColor(TextFormatting.DARK_PURPLE).setBold(true);
        }
        lastLine = line.getUnformattedText();
        for (EntityPlayer p : w.playerEntities) if (p.getDistanceSq(at) < 24 * 24) p.sendMessage(line);
        w.playSound(null, at.posX, at.posY + at.getEyeHeight(), at.posZ, sound, SoundCategory.NEUTRAL, fromAmmonius ? (float) lucifertotem.LTConfig.ammoniusVolume : 1.0f, 0.95f + RNG.nextFloat() * 0.1f);
        burst(w, at.posX, at.posY + at.height * 0.6, at.posZ);
        if (LTConfig.testHarness) LuciferTotem.LOG.info("[LT][SHOUT] {} ({})", lastLine, reason);
        if (at instanceof EntityPlayer && reason.equals("theriac")) HAdv.grant((EntityPlayer) at, "something_in_the_drink");
    }

    /** violet SPELL_MOB burst (count 0 + offsets = colour) */
    public static void burst(World w, double x, double y, double z) {
        if (!(w instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) w;
        for (int i = 0; i < 40; i++) {
            double a = RNG.nextDouble() * Math.PI * 2, r = RNG.nextDouble() * 1.2;
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB, x + Math.cos(a) * r, y + RNG.nextDouble() * 1.2 - 0.3, z + Math.sin(a) * r, 0, 0.62, 0.19, 1.0, 1.0);
        }
    }
}
