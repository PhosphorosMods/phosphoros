package lucifertotem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lucifertotem.hillman.Alabastron;
import lucifertotem.hillman.AmmonConversion;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HItems;
import lucifertotem.lore.LoreEvents;
import lucifertotem.paint.EntityLTPainting;
import lucifertotem.paint.ItemLadyBabylonPainting;
import lucifertotem.paint.Paintings;
import lucifertotem.world.LightfallTeleporter;
import lucifertotem.world.PortalData;
import lucifertotem.world.PortalLogic;
import lucifertotem.world.RiftLogic;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** 1.2.1: villager -> Ammon conversion, permanent purple rifts, Lady Babylon paintings. */
public class TestHarness121 {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.2.1] " + what); }
    static void fail(String what, Throwable t) { check(false, what + " tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] 1.2.1 " + what, t); }
    static final File MARK = new File("lt121_rift.txt");   // relative to the server directory

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp) {
        int startDim = tp.dimension; BlockPos startPos = tp.getPosition();
        try { persistedFromLastRun(ow); } catch (Throwable t) { fail("persistence (previous run)", t); }
        BlockPos pad = null;
        try { pad = pad(ow, tp); } catch (Throwable t) { fail("pad", t); }
        EntityAmmonius ammon = null;
        try { ammon = conversion(ow, tp, pad); } catch (Throwable t) { fail("conversion", t); }
        BlockPos rift = null;
        try { rift = riftOpens(ow, tp, ammon); } catch (Throwable t) { fail("rift opens", t); }
        try { if (rift != null) travel(ow, lf, tp, rift); } catch (Throwable t) { fail("travel", t); }
        try { if (rift != null) persistence(ow, rift); } catch (Throwable t) { fail("persistence", t); }
        try { if (rift != null) closing(srv, ow, lf, tp, rift); } catch (Throwable t) { fail("closing", t); }
        try { midRant(ow, tp, pad); } catch (Throwable t) { fail("mid-rant reload", t); }
        try { paintings(ow, lf, tp, pad); } catch (Throwable t) { fail("paintings", t); }
        try { libraryPaintings(lf); } catch (Throwable t) { fail("library paintings", t); }
        if (tp.dimension != startDim) { tp.timeUntilPortal = 0; tp.changeDimension(startDim, new LightfallTeleporter(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5, 0)); }
        tp.setSneaking(false);
        WorldServer w = (WorldServer) tp.world;
        w.getPlayerChunkMap().removePlayer(tp);
        tp.setPosition(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
        w.getPlayerChunkMap().addPlayer(tp);
    }

    static Advancement adv(MinecraftServer s, String n) { return s.getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, n)); }

    /** a clean grass pad in the Overworld, 41x41, with air above */
    static BlockPos pad(WorldServer ow, EntityPlayerMP tp) {
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(0, 100, 0, 0)); }
        BlockPos c = ow.getSpawnPoint().add(90, 0, -90);
        c = new BlockPos(c.getX(), 120, c.getZ());
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, -1, -20), c.add(20, -1, 20))) ow.setBlockState(q, Blocks.GRASS.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, 0, -20), c.add(20, 6, 20))) if (!ow.isAirBlock(q)) ow.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        for (EntityVillager v : ow.getEntitiesWithinAABB(EntityVillager.class, new AxisAlignedBB(c).grow(24))) v.setDead();
        tp.setPositionAndUpdate(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        return c;
    }

    static ItemStack laced() {
        return Alabastron.withEffects(new ItemStack(HItems.ALABASTRON), Collections.singletonList(new PotionEffect(MobEffects.SPEED, 200, 0)));
    }

    static EntityVillager villager(WorldServer ow, BlockPos at, int prof, boolean baby) {
        EntityVillager v = new EntityVillager(ow, prof);
        if (baby) v.setGrowingAge(-24000);
        v.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        v.renderYawOffset = 0f; v.rotationYawHead = 0f; v.prevRenderYawOffset = 0f;
        ow.spawnEntity(v);
        return v;
    }

    /** yaw 0 faces +z: behind is -z, in front is +z */
    static int hit(EntityPlayerMP tp, EntityVillager v, boolean behind) {
        tp.setPositionAndUpdate(v.posX, v.posY, v.posZ + (behind ? -1.5 : 1.5));
        v.renderYawOffset = 0f;
        tp.setHeldItem(EnumHand.MAIN_HAND, laced());
        return Alabastron.strike(tp, v);
    }

    static List<EntityAmmonius> ammons(WorldServer ow, BlockPos c) {
        return ow.getEntitiesWithinAABB(EntityAmmonius.class, new AxisAlignedBB(c).grow(30), a -> a != null && a.riftMaker && !a.isDead);
    }

    // ------------------------------------------------------------ conversion
    static EntityAmmonius conversion(WorldServer ow, EntityPlayerMP tp, BlockPos c) {
        Advancement tt = adv(tp.getServer(), "tenure_track");
        if (tt != null) tp.getAdvancements().revokeCriterion(tt, "code");
        check(Arrays.asList(LTConfig.ammon.professions).contains("minecraft:librarian") && LTConfig.ammon.conversionEnabled, "config default: conversion on, professions = " + Arrays.toString(LTConfig.ammon.professions));
        // front: a bonk, nothing else
        EntityVillager lib1 = villager(ow, c.add(-6, 0, 0), 1, false);
        int rf = hit(tp, lib1, false);
        check(rf == 1 && !lib1.isDead && ammons(ow, c).isEmpty(), "a librarian struck from the FRONT is only bonked (strike=" + rf + "), stays a villager");
        // a farmer from behind: an ordinary dose
        EntityVillager farmer = villager(ow, c.add(-3, 0, 0), 0, false);
        int rfa = hit(tp, farmer, true);
        check(rfa == 2 && !farmer.isDead && Alabastron.isLodged(farmer) && ammons(ow, c).isEmpty() && "minecraft:farmer".equals(AmmonConversion.professionOf(farmer)), "a FARMER struck from behind is just dosed (strike=" + rfa + ", flask lodged), no Ammon");
        // a baby librarian: never converted
        EntityVillager baby = villager(ow, c.add(0, 0, -6), 1, true);
        int rb = hit(tp, baby, true);
        check(rb == 2 && !baby.isDead && ammons(ow, c).isEmpty(), "a baby librarian is never converted (strike=" + rb + ", ordinary dose)");
        // an empty flask: nothing
        EntityVillager lib0 = villager(ow, c.add(6, 0, -6), 1, false);
        tp.setPositionAndUpdate(lib0.posX, lib0.posY, lib0.posZ - 1.5);
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(HItems.ALABASTRON));
        int re = Alabastron.strike(tp, lib0);
        check(re == 3 && !lib0.isDead, "an EMPTY alabastron converts no one (strike=" + re + ")");
        lib0.setDead();
        // config: the farmer list converts farmers, not librarians
        String[] prof = LTConfig.ammon.professions;
        LTConfig.ammon.professions = new String[]{"farmer"};
        EntityVillager farmer2 = villager(ow, c.add(-12, 0, 6), 0, false);
        EntityVillager lib2 = villager(ow, c.add(-12, 0, 12), 1, false);
        boolean libNo = !AmmonConversion.eligible(lib2), farmYes = AmmonConversion.eligible(farmer2);
        LTConfig.ammon.professions = prof;
        lib2.setDead(); farmer2.setDead();
        check(libNo && farmYes, "the profession list is config: with professions=[farmer] a farmer is eligible and a librarian is not");
        // the real thing: a librarian, from behind
        EntityVillager lib = villager(ow, c.add(4, 0, 4), 1, false);
        lib.setCustomNameTag("Hypatia's Clerk");
        int conv0 = AmmonConversion.conversions, made0 = EntityAmmonius.ammonsMade;
        int r = hit(tp, lib, true);
        List<EntityAmmonius> as = ammons(ow, c);
        EntityAmmonius a = as.isEmpty() ? null : as.get(0);
        int flasks = 0;
        for (EntityItem ei : ow.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(lib.getPosition()).grow(3))) if (ei.getItem().getItem() == HItems.ALABASTRON) { flasks++; ei.setDead(); }
        check(r == 6 && lib.isDead && a != null && AmmonConversion.conversions == conv0 + 1 && EntityAmmonius.ammonsMade == made0 + 1 && flasks >= 1 && tp.getHeldItemMainhand().isEmpty(),
            "a LIBRARIAN struck from behind with a laced alabastron becomes Doctor Ammonius (strike=" + r + "; villager gone; the emptied flask dropped: " + flasks + ")");
        check(a != null && a.getDistance(lib.posX, lib.posY, lib.posZ) < 1.0 && "Hypatia's Clerk".equals(a.getCustomNameTag()) && a.riftMaker && tp.getUniqueID().equals(a.philoFor), "he stands where the librarian stood, keeps the name tag, and is a rift-maker bound to the player");
        check(LoreEvents.done(tp, "tenure_track"), "'Tenure Track' advancement granted");
        for (EntityVillager v : new EntityVillager[]{lib1, farmer, baby}) v.setDead();
        return a;
    }

    // ------------------------------------------------------------ the rant and the door
    static BlockPos riftOpens(WorldServer ow, EntityPlayerMP tp, EntityAmmonius a) {
        if (a == null) { check(false, "no Ammon to watch"); return null; }
        tp.setPositionAndUpdate(a.posX + 4, a.posY, a.posZ);
        int rants0 = EntityAmmonius.rantsSaid, gone0 = EntityAmmonius.ammonsGone, open0 = RiftLogic.opened;
        Map<BlockPos, net.minecraft.block.state.IBlockState> before = snapshot(ow, a.getPosition(), 8);
        for (int i = 0; i < 1200 && !a.isDead; i++) { a.onUpdate(); a.ticksExisted++; }
        BlockPos r = a.rift;
        PortalData.Gate g = r == null ? null : PortalData.get().findExact(0, r);
        check(EntityAmmonius.rantsSaid > rants0 && a.isDead && EntityAmmonius.ammonsGone == gone0 + 1 && a.riftOpened, "the Ammon rants, then steps into his rift and is gone (" + (a.isDead ? "despawned" : "still here") + ")");
        check(r != null && RiftLogic.opened == open0 + 1 && RiftLogic.intact(ow, r) && g != null && g.rift && tp.getUniqueID().toString().equals(g.maker),
            "the rift he leaves STAYS OPEN: two rift blocks at " + r + ", registered " + g + ", maker = the converting player");
        int replaced = 0;
        for (Map.Entry<BlockPos, net.minecraft.block.state.IBlockState> e : before.entrySet())
            if (e.getValue().getBlock() != Blocks.AIR && ow.getBlockState(e.getKey()) != e.getValue()) replaced++;
        check(replaced == 0, "opening it replaced no blocks (" + replaced + " changed non-air blocks within 8)");
        return r;
    }

    static Map<BlockPos, net.minecraft.block.state.IBlockState> snapshot(WorldServer w, BlockPos c, int r) {
        Map<BlockPos, net.minecraft.block.state.IBlockState> m = new HashMap<>();
        for (BlockPos q : BlockPos.getAllInBox(c.add(-r, -3, -r), c.add(r, 4, r))) m.put(q.toImmutable(), w.getBlockState(q));
        return m;
    }

    // ------------------------------------------------------------ both ways
    static void travel(WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos rift) {
        Map<BlockPos, net.minecraft.block.state.IBlockState> before = snapshot(ow, rift, 5);
        tp.setPositionAndUpdate(rift.getX() + 0.5, rift.getY(), rift.getZ() + 0.5);
        tp.timeUntilPortal = 0;
        int ent0 = RiftLogic.entered;
        ModContent.RIFT.onEntityCollision(ow, rift, ow.getBlockState(rift), tp);   // walking into it
        int[] ret = PortalLogic.returnPoint(tp);
        BlockPos feet = tp.getPosition();
        boolean safe = tp.world.getBlockState(feet.down()).getMaterial().isSolid() && tp.world.isAirBlock(feet) && tp.world.isAirBlock(feet.up());
        PortalData.Gate rg = PortalData.get().findExact(0, rift);
        PortalData.Gate there = rg != null && rg.linked ? PortalData.get().findExact(rg.ldim, rg.lbase) : null;
        check(RiftLogic.entered == ent0 + 1 && tp.dimension == LuciferTotem.dimId && ret != null && ret[0] == 0 && new BlockPos(ret[1], ret[2], ret[3]).equals(rift) && safe,
            "walking into the rift takes the player to the Lightfall (safe at " + feet + "); the way home is the rift (" + (ret == null ? "-" : ret[1] + "," + ret[2] + "," + ret[3]) + ")");
        check(there != null && there.linked && there.ldim == 0 && there.lbase.equals(rift) && PortalLogic.gateIntact(lf, there.base, there.axis), "a return gate stands in the Lightfall, linked to the rift: " + there);
        if (there == null) return;
        // back
        tp.timeUntilPortal = 0;
        int trips0 = PortalLogic.returnTrips;
        PortalLogic.enterPortal(tp, PortalLogic.at(there.base, there.axis, 1, 1, 0));
        BlockPos home = tp.getPosition();
        boolean safe2 = ow.getBlockState(home.down()).getMaterial().isSolid() && ow.isAirBlock(home) && ow.isAirBlock(home.up()) && ow.getBlockState(home).getBlock() != ModContent.RIFT;
        check(tp.dimension == 0 && Math.sqrt(home.distanceSq(rift)) <= 3 && safe2 && PortalLogic.returnTrips == trips0 + 1 && RiftLogic.intact(ow, rift),
            "and back: the Lightfall gate brings the player out beside the rift (" + home + ", " + String.format("%.1f", Math.sqrt(home.distanceSq(rift))) + " blocks), standing, not inside it; rift still open");
        int changed = 0;
        for (Map.Entry<BlockPos, net.minecraft.block.state.IBlockState> e : before.entrySet()) if (ow.getBlockState(e.getKey()) != e.getValue()) changed++;
        check(changed == 0, "the round trip dug and placed nothing around the rift (" + changed + " blocks changed)");
        // again: the same pair of doors, no new gate
        int gates0 = PortalData.get().gates.size();
        tp.timeUntilPortal = 0;
        RiftLogic.enter(tp, rift);
        boolean same = tp.dimension == LuciferTotem.dimId && Math.sqrt(tp.getPosition().distanceSq(there.base)) < 6 && PortalData.get().gates.size() == gates0;
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, PortalLogic.at(there.base, there.axis, 1, 1, 0));
        check(same && tp.dimension == 0, "a second trip reuses the same Lightfall gate (no new gates: " + PortalData.get().gates.size() + ") and comes back again");
    }

    // ------------------------------------------------------------ persistence (in-run) + a marker for the next run
    static void persistence(WorldServer ow, BlockPos rift) throws Exception {
        NBTTagCompound t = PortalData.get().writeToNBT(new NBTTagCompound());
        PortalData copy = new PortalData("lt121copy");
        copy.readFromNBT(t);
        PortalData.Gate g = copy.findExact(0, rift), o = PortalData.get().findExact(0, rift);
        check(g != null && g.rift && o != null && g.maker.equals(o.maker) && g.linked == o.linked && (!g.linked || g.lbase.equals(o.lbase)), "the rift survives a PortalData save/load round trip (rift flag, maker, link): " + g);
        PortalData.Gate f = copy.find(0, rift.up());
        check(f != null && f.base.equals(rift), "find() on the upper rift block resolves to its base");
        ow.saveAllChunks(true, null);
        PortalData.get().markDirty();
        ow.getMapStorage().saveAllData();
        Files.write(MARK.toPath(), (rift.getX() + " " + rift.getY() + " " + rift.getZ() + "\n").getBytes());
        LuciferTotem.LOG.info("[LT][TEST] 1.2.1 rift marker written for the next run: {}", rift);
    }

    static void persistedFromLastRun(WorldServer ow) throws Exception {
        if (!MARK.exists()) { LuciferTotem.LOG.info("[LT][TEST] 1.2.1 no rift marker from a previous run (fresh world)"); return; }
        String[] s = new String(Files.readAllBytes(MARK.toPath())).trim().split(" ");
        MARK.delete();
        BlockPos r = new BlockPos(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        ow.getChunk(r);
        PortalData.Gate g = PortalData.get().findExact(0, r);
        // the previous run re-opened this one at the end of its closing tests, saved, and stopped
        check(g != null && g.rift && RiftLogic.intact(ow, r), "AFTER A SERVER RESTART the rift from the previous run is still open and registered: " + r + " " + g);
    }

    // ------------------------------------------------------------ closing
    static void closing(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos rift) {
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(rift.getX() + 2.5, rift.getY(), rift.getZ() + 0.5, 0)); }
        tp.setPositionAndUpdate(rift.getX() + 2.5, rift.getY(), rift.getZ() + 0.5);
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(ModContent.TOTEM));
        // explosions, pistons, withers: it stays
        ow.createExplosion(null, rift.getX() + 0.5, rift.getY() + 1, rift.getZ() + 0.5, 3f, false);
        check(RiftLogic.intact(ow, rift), "an explosion at the rift leaves it open (it never drops, never breaks)");
        check(ModContent.RIFT.getBlockHardness(ow.getBlockState(rift), ow, rift) < 0 && ow.getBlockState(rift).getPushReaction() == net.minecraft.block.material.EnumPushReaction.BLOCK
            && !ModContent.RIFT.canEntityDestroy(ow.getBlockState(rift), ow, rift, new net.minecraft.entity.boss.EntityWither(ow)), "unbreakable by hand, immovable by pistons, wither-proof");
        // not sneaking: the totem is thrown as usual
        tp.setSneaking(false);
        EnumActionResult pass = ModContent.TOTEM.onItemUse(tp, ow, rift.up(), EnumHand.MAIN_HAND, EnumFacing.EAST, 0.5f, 0.5f, 0.5f);
        check(pass == EnumActionResult.PASS && RiftLogic.intact(ow, rift), "using the totem on it WITHOUT sneaking does nothing to it (PASS)");
        // anyoneMayClose=false and someone else's rift: refused (unless op/creative)
        PortalData.Gate g = PortalData.get().findExact(0, rift);
        String maker = g.maker;
        boolean any = LTConfig.ammon.anyoneMayClose;
        LTConfig.ammon.anyoneMayClose = false; g.maker = UUID.randomUUID().toString();
        boolean op = srv.getPlayerList().canSendCommands(tp.getGameProfile());
        tp.setSneaking(true);
        ModContent.TOTEM.onItemUse(tp, ow, rift, EnumHand.MAIN_HAND, EnumFacing.EAST, 0.5f, 0.5f, 0.5f);
        boolean refused = RiftLogic.intact(ow, rift);
        LTConfig.ammon.anyoneMayClose = any; g.maker = maker;
        check(op || refused, "with anyoneMayClose=false a stranger's rift cannot be closed by a non-op survival player" + (op ? " (skipped: the test player is op)" : ""));
        // sneak-use: closed
        PortalData.Gate there = g.linked ? PortalData.get().findExact(g.ldim, g.lbase) : null;
        int closed0 = RiftLogic.closedCount;
        EnumActionResult ok = ModContent.TOTEM.onItemUse(tp, ow, rift.up(), EnumHand.MAIN_HAND, EnumFacing.EAST, 0.5f, 0.5f, 0.5f);
        tp.setSneaking(false);
        boolean gone = ow.isAirBlock(rift) && ow.isAirBlock(rift.up()) && PortalData.get().findExact(0, rift) == null && RiftLogic.closedCount == closed0 + 1;
        check(ok == EnumActionResult.SUCCESS && gone && ow.getBlockState(rift.down()).getBlock() == Blocks.GRASS, "SNEAK + use the totem on it: the rift closes (both blocks gone, ground untouched, registry entry dropped)");
        check(there == null || !there.linked, "the Lightfall gate that led to it is unlinked (it will find a new way home): " + there);
        // breaking one half (e.g. /setblock) closes the whole rift and forgets it
        BlockPos r2 = RiftLogic.findSpot(ow, rift.add(5, 0, 5));
        PortalData.Gate g2 = r2 == null ? null : RiftLogic.open(ow, r2, EnumFacing.Axis.X, tp);
        if (g2 != null) ow.setBlockState(r2, Blocks.AIR.getDefaultState(), 3);
        check(g2 != null && ow.isAirBlock(r2) && ow.isAirBlock(r2.up()) && PortalData.get().findExact(0, r2) == null, "removing one half (commands) removes the other and forgets the rift");
        // the cap
        int cap = LTConfig.ammon.maxPermanentRifts;
        LTConfig.ammon.maxPermanentRifts = RiftLogic.count();
        BlockPos r3 = RiftLogic.findSpot(ow, rift.add(-5, 0, 5));
        PortalData.Gate g3 = r3 == null ? null : RiftLogic.open(ow, r3, EnumFacing.Axis.X, tp);
        LTConfig.ammon.maxPermanentRifts = cap;
        check(r3 != null && g3 == null && ow.isAirBlock(r3), "maxPermanentRifts caps them (refused at the cap; the Ammon then uses a temporary rift)");
        // occupied spots are refused: rifts never replace blocks
        BlockPos r4 = rift.add(0, 0, 8);
        ow.setBlockState(r4.up(), Blocks.BOOKSHELF.getDefaultState());
        boolean refusedSolid = RiftLogic.open(ow, r4, EnumFacing.Axis.X, tp) == null && ow.getBlockState(r4.up()).getBlock() == Blocks.BOOKSHELF;
        ow.setBlockState(r4.up(), Blocks.AIR.getDefaultState());
        check(refusedSolid, "a rift never opens where a block stands (a bookshelf in the way: refused, bookshelf kept)");
        // re-open one for the restart check of the next run
        BlockPos keep = RiftLogic.findSpot(ow, rift.add(10, 0, -10));
        if (keep != null && RiftLogic.open(ow, keep, EnumFacing.Axis.Z, tp) != null) {
            try { ow.saveAllChunks(true, null); ow.getMapStorage().saveAllData(); Files.write(MARK.toPath(), (keep.getX() + " " + keep.getY() + " " + keep.getZ() + "\n").getBytes()); } catch (Exception e) { LuciferTotem.LOG.error("marker", e); }
        }
    }

    // ------------------------------------------------------------ saved mid-rant
    static void midRant(WorldServer ow, EntityPlayerMP tp, BlockPos c) {
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0)); }
        tp.setPositionAndUpdate(c.getX() - 10.5, c.getY(), c.getZ() - 10.5);
        EntityAmmonius a = new EntityAmmonius(ow);
        a.setLocationAndAngles(c.getX() - 14.5, c.getY(), c.getZ() - 14.5, 0, 0);
        ow.spawnEntity(a);
        a.makeRiftMaker(tp);
        for (int i = 0; i < 20; i++) { a.onUpdate(); a.ticksExisted++; }
        NBTTagCompound t = new NBTTagCompound(); a.writeToNBT(t);
        a.setDead();
        EntityAmmonius b = new EntityAmmonius(ow); b.readFromNBT(t);
        ow.spawnEntity(b);
        int open0 = RiftLogic.opened;
        for (int i = 0; i < 5 && !b.isDead; i++) { b.onUpdate(); b.ticksExisted++; }
        BlockPos r = b.rift;
        check(b.isDead && RiftLogic.opened == open0 + 1 && r != null && RiftLogic.intact(ow, r) && PortalData.get().findExact(0, r) != null,
            "an Ammon saved mid-rant leaves at once on reload, but still leaves his door open (" + r + ")");
        if (r != null) RiftLogic.close(ow, r, null, false);
    }

    // ------------------------------------------------------------ paintings
    static void paintings(WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos c) {
        check(Paintings.ALL.size() >= 5 && Paintings.ALL.size() <= 14 && Paintings.CLASSIC.size() == 8, Paintings.ALL.size() + " Lady Babylon paintings: " + Paintings.ALL);
        boolean sizes = Paintings.ALL.stream().anyMatch(v -> v.w == 1 && v.h == 1) && Paintings.ALL.stream().anyMatch(v -> v.w == 4 && v.h == 2);
        check(sizes, "sizes range from 1x1 to 4x2");
        check(net.minecraft.entity.item.EntityPainting.EnumArt.values().length == 26, "vanilla paintings untouched (26 EnumArt values)");
        // recipe
        InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        inv.setInventorySlotContents(0, new ItemStack(Items.PAINTING)); inv.setInventorySlotContents(4, new ItemStack(Items.DYE, 1, 5));
        ItemStack out = CraftingManager.findMatchingResult(inv, ow);
        check(out.getItem() == HItems.PAINTING && ItemLadyBabylonPainting.variant(out) == null, "painting + purple dye -> Lady Babylon Painting: " + out);
        // a wall
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0)); }
        BlockPos wall = c.add(0, 0, 15);
        for (BlockPos q : BlockPos.getAllInBox(wall.add(-8, 0, 0), wall.add(8, 5, 0))) ow.setBlockState(q, Blocks.STONEBRICK.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(wall.add(-8, 0, -3), wall.add(8, 5, -1))) ow.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        tp.setPositionAndUpdate(wall.getX() + 0.5, wall.getY(), wall.getZ() - 2.5);
        // no NBT: the largest that fits on a big wall = Devil's Flight
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(HItems.PAINTING));
        EnumActionResult r1 = HItems.PAINTING.onItemUse(tp, ow, wall.add(-3, 2, 0), EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5f, 0.5f, 0.5f);
        List<EntityLTPainting> ps = ow.getEntitiesWithinAABB(EntityLTPainting.class, new AxisAlignedBB(wall).grow(10));
        EntityLTPainting p1 = ps.isEmpty() ? null : ps.get(0);
        check(r1 == EnumActionResult.SUCCESS && p1 != null && p1.art.name.equals("devils_flight") && p1.onValidSurface() && tp.getHeldItemMainhand().isEmpty() && p1.facingDirection == EnumFacing.NORTH,
            "the plain item hangs the largest painting that fits (" + (p1 == null ? "none" : p1.art) + ") on the wall, facing out");
        // a chosen variant (NBT)
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemLadyBabylonPainting.of(Paintings.byName("galen")));
        EnumActionResult r2 = HItems.PAINTING.onItemUse(tp, ow, wall.add(5, 1, 0), EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5f, 0.5f, 0.5f);
        EntityLTPainting p2 = null;
        for (EntityLTPainting p : ow.getEntitiesWithinAABB(EntityLTPainting.class, new AxisAlignedBB(wall).grow(10))) if (p != p1) p2 = p;
        check(r2 == EnumActionResult.SUCCESS && p2 != null && p2.art.name.equals("galen"), "a variant item hangs exactly its picture: " + (p2 == null ? "none" : p2.art));
        // overlap refused, floor/ceiling refused
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemLadyBabylonPainting.of(Paintings.byName("galen")));
        EnumActionResult r3 = HItems.PAINTING.onItemUse(tp, ow, wall.add(5, 1, 0), EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5f, 0.5f, 0.5f);
        EnumActionResult r4 = HItems.PAINTING.onItemUse(tp, ow, wall.add(5, -1, -2), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        check(r3 == EnumActionResult.FAIL && r4 == EnumActionResult.FAIL && tp.getHeldItemMainhand().getCount() == 1, "it will not hang over another painting, nor on a floor");
        if (p1 == null || p2 == null) return;
        // NBT round trip
        NBTTagCompound t = new NBTTagCompound(); p1.writeToNBT(t);
        EntityLTPainting q = new EntityLTPainting(ow); q.readFromNBT(t);
        check(q.art == p1.art && q.getHangingPosition().equals(p1.getHangingPosition()) && q.facingDirection == p1.facingDirection && q.getEntityBoundingBox().equals(p1.getEntityBoundingBox()) && "devils_flight".equals(t.getString("Motive")),
            "save/load keeps picture, wall position and facing (" + t.getString("Motive") + ")");
        // spawn data (what a client receives)
        ByteBuf buf = Unpooled.buffer();
        p2.writeSpawnData(buf);
        EntityLTPainting cl = new EntityLTPainting(ow);
        cl.readSpawnData(buf);
        check(cl.art == p2.art && cl.getHangingPosition().equals(p2.getHangingPosition()) && cl.facingDirection == p2.facingDirection && cl.getEntityBoundingBox().equals(p2.getEntityBoundingBox()),
            "the client spawn data carries picture, wall position and facing (multiplayer)");
        // broken: drops itself, keeping its picture
        p2.onBroken(null); p2.setDead();
        ItemStack drop = ItemStack.EMPTY;
        for (EntityItem ei : ow.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p2.getHangingPosition()).grow(3))) if (ei.getItem().getItem() == HItems.PAINTING) { drop = ei.getItem(); ei.setDead(); }
        Paintings.Variant dv = drop.isEmpty() ? null : ItemLadyBabylonPainting.variant(drop);
        check(dv != null && dv.name.equals("galen"), "a broken painting drops itself with its picture: " + drop + " " + dv);
        // the wall goes: it falls
        ow.setBlockState(wall.add(-3, 2, 0), Blocks.AIR.getDefaultState(), 3);
        check(!p1.onValidSurface(), "without its wall it is no longer valid (falls on the next hanging check)");
        p1.setDead();
    }

    static void libraryPaintings(WorldServer lf) {
        BlockPos lib = lucifertotem.world.LibraryGen.nearest(lf.getSeed(), new BlockPos(0, 64, 0), 8000);
        if (lib == null) { check(false, "no library predicted"); return; }
        int L = lucifertotem.world.LibraryGen.SIZE;
        AxisAlignedBB box = new AxisAlignedBB(lib.add(-L / 2 - 2, -5, -L / 2 - 2), lib.add(L / 2 + 2, 30, L / 2 + 2));
        List<EntityLTPainting> ps = lf.getEntitiesWithinAABB(EntityLTPainting.class, box);
        int valid = 0; Set<String> kinds = new TreeSet<>();
        for (EntityLTPainting p : ps) { if (p.onValidSurface()) valid++; kinds.add(p.art.name); }
        check(ps.size() >= 3 && valid == ps.size(), "paintings hang in the Library of Alexandria: " + ps.size() + " (all on valid walls: " + valid + "), kinds " + kinds + "; hung by worldgen this run: " + lucifertotem.world.LibraryGen.paintingsHung);
    }
}
