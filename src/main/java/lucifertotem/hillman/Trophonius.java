package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The Oracle of Trophonius at Lebadea: fast, bring a honey cake for the snakes, descend into the pitch-black pit,
 * find your way out among the (harmless) snakes. You come back knowing where things are... and never laugh again
 * (for a day): your chat gains *(gloomily)*.
 *
 * Layout (LightfallStructures.trophonius): altar at (c, y, d), with c = window x + 8; the maze occupies the window at
 * y 5..8, arrival cell at (c-7, 6, d-7), exit block at (c+5, 6, d+5).
 */
public class Trophonius {
    public static final int MAZE_FLOOR = 5;
    public static int descents = 0, exits = 0;

    public static BlockPos arrival(BlockPos altar) { return new BlockPos(altar.getX() - 7, MAZE_FLOOR + 1, altar.getZ() - 7); }

    public static boolean fasting(EntityPlayer p) {
        if (p.getFoodStats().getFoodLevel() <= 6) return true;
        NBTTagCompound d = HData.get(p);
        return d.hasKey("lastMeal") && p.world.getTotalWorldTime() - d.getLong("lastMeal") >= 24000;
    }

    public static boolean inPit(EntityPlayer p) { return HData.get(p).getBoolean("trophIn"); }

    public static boolean tryDescend(EntityPlayer p, BlockPos altar) {
        if (inPit(p)) return false;
        if (p.getHeldItemMainhand().getItem() != HItems.HONEY_CAKE) { HBlocks.msg(p, "msg.lucifertotem.trophonius.cake"); return false; }
        if (!fasting(p) && !p.capabilities.isCreativeMode) { HBlocks.msg(p, "msg.lucifertotem.trophonius.fast"); return false; }
        if (!p.capabilities.isCreativeMode) p.getHeldItemMainhand().shrink(1);
        descend(p, altar);
        return true;
    }

    public static void descend(EntityPlayer p, BlockPos altar) {
        World w = p.world;
        descents++;
        NBTTagCompound d = HData.get(p);
        d.setBoolean("trophIn", true);
        d.setLong("trophRet", altar.up().toLong());
        d.setLong("trophAltar", altar.toLong());
        BlockPos a = arrival(altar);
        // make sure the arrival cell is open (in case of pre-existing terrain edits)
        w.setBlockToAir(a); w.setBlockToAir(a.up());
        p.setPositionAndUpdate(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0, false, false));
        w.playSound(null, a, HSounds.TROPHONIUS_WHISPER, SoundCategory.AMBIENT, 1f, 0.8f);
        HBlocks.msg(p, "msg.lucifertotem.trophonius.descend");
        // the sacred snakes: harmless
        int n = 0;
        for (int i = 0; i < 40 && n < 3; i++) {
            BlockPos s = new BlockPos(altar.getX() - 7 + 2 * w.rand.nextInt(7), MAZE_FLOOR + 1, altar.getZ() - 7 + 2 * w.rand.nextInt(7));
            if (!w.isAirBlock(s) || s.equals(a)) continue;
            EntityDipsas sn = new EntityDipsas(w);
            sn.setHarmless(true);
            sn.setLocationAndAngles(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, w.rand.nextFloat() * 360, 0);
            w.spawnEntity(sn);
            n++;
        }
    }

    /** walking into the exit (or forced, for tests) */
    public static void exit(EntityPlayer p, boolean force) {
        NBTTagCompound d = HData.get(p);
        if (!d.getBoolean("trophIn") && !force) {
            // stumbled into an exit without descending: just send them up
            BlockPos top = p.world.getTopSolidOrLiquidBlock(new BlockPos(p));
            p.setPositionAndUpdate(top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
            return;
        }
        exits++;
        d.setBoolean("trophIn", false);
        BlockPos ret = d.hasKey("trophRet") ? BlockPos.fromLong(d.getLong("trophRet")) : p.world.getTopSolidOrLiquidBlock(new BlockPos(p));
        p.removePotionEffect(MobEffects.BLINDNESS);
        p.setPositionAndUpdate(ret.getX() + 0.5, ret.getY() + 0.1, ret.getZ() + 1.5);
        p.addPotionEffect(new PotionEffect(HPotions.ORACLE, 72000, 0));
        p.addPotionEffect(new PotionEffect(HPotions.GLOOM, LTConfig.gloomTicks, 0, false, false));
        d.setLong("gloomUntil", p.world.getTotalWorldTime() + LTConfig.gloomTicks);
        p.world.playSound(null, ret, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.3f, 1.6f);
        HAdv.grant(p, "trophonius");
        HBlocks.msg(p, "msg.lucifertotem.trophonius.exit");
        reveal(p, 6);
    }

    /** list the nearest structures in chat (the Oracle's gift) */
    public static void reveal(EntityPlayer p, int budget) {
        BlockPos from = new BlockPos(p);
        boolean lightfall = p.world.provider.getDimension() == LuciferTotem.dimId;
        String[] types = Prophecies.OVERWORLD;
        if (lightfall) { types = java.util.Arrays.copyOf(StructureIndex.TYPES, StructureIndex.TYPES.length + 1); types[types.length - 1] = "library"; }
        for (String t : types) {
            BlockPos to = Prophecies.find(p.world, from, t, budget);
            budget = Math.max(0, budget - StructureIndex.lastForced);
            if (to != null) p.sendMessage(Prophecies.line(lightfall ? t : "ow_" + t.toLowerCase(), from, to));
        }
    }

    /** every 30 s while under the Oracle: nearest known structure on the action bar (index only, no generation) */
    public static void oracleTick(EntityPlayer p) {
        BlockPos from = new BlockPos(p);
        if (p.world.provider.getDimension() != LuciferTotem.dimId) return;
        StructureIndex idx = StructureIndex.get(p.world);
        String bestT = null; BlockPos best = null; double bd = Double.MAX_VALUE;
        for (String t : StructureIndex.TYPES) {
            BlockPos q = StructureIndex.nearest(idx.all(t), from, 2000);
            if (q != null && StructureIndex.sq(q, from) < bd && StructureIndex.sq(q, from) > 16 * 16) { bd = StructureIndex.sq(q, from); best = q; bestT = t; }
        }
        if (best != null) p.sendStatusMessage(Prophecies.line(bestT, from, best), true);
    }
}
