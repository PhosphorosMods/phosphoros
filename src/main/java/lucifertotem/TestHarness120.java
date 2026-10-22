package lucifertotem;

import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.lore.*;
import lucifertotem.world.LightfallTeleporter;
import lucifertotem.world.PortalData;
import lucifertotem.world.PortalLogic;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.PrintWriter;
import java.util.*;

/** 1.2.0: the wreck, the altar, the philologist, the trail, and the way home. */
public class TestHarness120 {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.2] " + what); }
    static final String[] TRAIL = {"aww_gawd", "galley", "lecture_notes", "fallen_altar", "enter_lightfall", "return_lightfall", "library", "meet_ammonius", "philologist"};
    static final String DUMP = "";   // relative to the server directory

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp) {
        int startDim = tp.dimension; BlockPos startPos = tp.getPosition();
        try { recipes(ow); } catch (Throwable t) { fail("recipes", t); }
        try { advancements(srv); } catch (Throwable t) { fail("advancements", t); }
        try { books(tp); } catch (Throwable t) { fail("books", t); }
        BlockPos[] sites = null;
        try { sites = worldgen(ow); } catch (Throwable t) { fail("worldgen", t); }
        try { altarAndTrip(srv, ow, lf, tp, sites == null ? null : sites[1]); } catch (Throwable t) { fail("altar/trip", t); }
        try { safety(ow, lf); } catch (Throwable t) { fail("safety", t); }
        try { philologist(ow, tp); } catch (Throwable t) { fail("philologist", t); }
        try { whispers(ow, lf, tp, sites == null ? null : sites[1]); } catch (Throwable t) { fail("whispers", t); }
        // leave the traveller where the older tests expect him
        if (tp.dimension != startDim) { tp.timeUntilPortal = 0; tp.changeDimension(startDim, new LightfallTeleporter(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5, 0)); }
        {
        // the test player has no network handler, so the chunk map never sees him move: re-register him where he stands now
        WorldServer w = (WorldServer) tp.world;
        w.getPlayerChunkMap().removePlayer(tp);
        tp.setPosition(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
        w.getPlayerChunkMap().addPlayer(tp);
        }
    }

    static void fail(String what, Throwable t) { check(false, what + " tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] 1.2 " + what, t); }

    static Advancement adv(MinecraftServer s, String n) { return s.getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, n)); }
    static void revoke(EntityPlayerMP p, String... names) { for (String n : names) { Advancement a = adv(p.getServer(), n); if (a != null) p.getAdvancements().revokeCriterion(a, "code"); } }

    // ------------------------------------------------------------ recipes
    static void recipes(WorldServer ow) {
        InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        inv.setInventorySlotContents(0, new ItemStack(Blocks.OBSIDIAN)); inv.setInventorySlotContents(1, new ItemStack(Items.DYE, 1, 5));
        inv.setInventorySlotContents(3, new ItemStack(Items.DYE, 1, 5)); inv.setInventorySlotContents(4, new ItemStack(Blocks.OBSIDIAN));
        ItemStack out = CraftingManager.findMatchingResult(inv, ow);
        check(out.getItem() == net.minecraft.item.Item.getItemFromBlock(ModContent.DUSKSTONE_BRICKS) && out.getCount() == 2, "2 obsidian + 2 purple dye -> 2 Duskstone Bricks (Overworld repair blocks): " + out);
        InventoryCrafting inv2 = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        for (int i : new int[]{0, 1, 3, 4}) inv2.setInventorySlotContents(i, new ItemStack(ModContent.DUSKSTONE));
        ItemStack o2 = CraftingManager.findMatchingResult(inv2, ow);
        check(o2.getItem() == net.minecraft.item.Item.getItemFromBlock(ModContent.DUSKSTONE_BRICKS) && o2.getCount() == 4, "4 duskstone -> 4 Duskstone Bricks (build a gate home inside the Lightfall): " + o2);
        ItemStack sm = FurnaceRecipes.instance().getSmeltingResult(new ItemStack(ModContent.CRACKED_DUSKSTONE_BRICKS));
        check(sm.getItem() == net.minecraft.item.Item.getItemFromBlock(ModContent.DUSKSTONE_BRICKS), "cracked duskstone bricks bake whole in a furnace");
    }

    // ------------------------------------------------------------ advancements
    static void advancements(MinecraftServer srv) {
        String[][] chain = {{"aww_gawd", "root"}, {"galley", "aww_gawd"}, {"lecture_notes", "galley"}, {"fallen_altar", "lecture_notes"}, {"enter_lightfall", "fallen_altar"},
            {"return_lightfall", "enter_lightfall"}, {"library", "enter_lightfall"}, {"meet_ammonius", "library"}, {"philologist", "aww_gawd"}};
        int ok = 0;
        for (String[] c : chain) { Advancement a = adv(srv, c[0]); if (a != null && a.getParent() != null && a.getParent().getId().getPath().equals(c[1]) && a.getDisplay() != null) ok++; else LuciferTotem.LOG.info("[LT][TEST] bad advancement {}", c[0]); }
        check(ok == chain.length, "advancement trail: totem -> galley -> notes -> altar -> Lightfall -> (return | Library -> Ammonius), + philologist: " + ok + "/" + chain.length);
    }

    // ------------------------------------------------------------ books
    static void books(EntityPlayerMP tp) {
        ItemStack n = LoreBooks.notes(), p = LoreBooks.page();
        check(LoreBooks.isNotes(n) && LoreBooks.isNotes(p) && n.getTagCompound().getTagList("pages", 8).tagCount() == 11 && "Dr. Ammonius".equals(n.getTagCompound().getString("author"))
            && net.minecraft.item.ItemWrittenBook.validBookTagContents(n.getTagCompound()), "Lecture Notes: a valid 11-page written book by Dr. Ammonius; the loose page too");
        String all = n.getTagCompound().getTagList("pages", 8).toString();
        check(all.contains("Fallen Altar") && all.contains("Duskstone Brick") && all.contains("DUSK") && all.contains("Totem of Lucifer") && all.contains("come home"), "the notes double as the portal (and return) instructions");
        revoke(tp, "lecture_notes");
        LoreEvents.read(tp, n);
        check(LoreEvents.done(tp, "lecture_notes"), "reading the notes grants 'Required Reading'");
    }

    // ------------------------------------------------------------ worldgen
    static BlockPos[] worldgen(WorldServer ow) throws Exception {
        if (ow.getWorldType() == net.minecraft.world.WorldType.FLAT) {   // superflat: no sea, no coast, ground at y=3
            LuciferTotem.LOG.info("[LT][TEST] SKIP [1.2] Overworld galley/altar generation needs a DEFAULT world (this one is superflat); run with level-type=DEFAULT");
            return new BlockPos[]{null, null};
        }
        BlockPos sp = ow.getSpawnPoint();
        BlockPos galley = null, altar = null;
        for (String type : new String[]{OverworldLore.GALLEY, OverworldLore.ALTAR}) {
            List<int[]> cand = new ArrayList<>();
            int scx = sp.getX() >> 4, scz = sp.getZ() >> 4;
            for (int dx = -90; dx <= 90; dx++) for (int dz = -90; dz <= 90; dz++)
                if (OverworldLore.candidate(ow, type, scx + dx, scz + dz)) cand.add(new int[]{scx + dx, scz + dz, dx * dx + dz * dz});
            cand.sort(Comparator.comparingInt(a -> a[2]));
            int tried = 0, built = 0; BlockPos first = null;
            long t0 = System.currentTimeMillis();
            for (int[] c : cand) {
                if (tried >= 8 || built >= 2) break;
                boolean already = ow.isChunkGeneratedAt(c[0], c[1]) && ow.getChunk(c[0], c[1]).isTerrainPopulated();
                if (!already) tried++;
                ow.getChunk(c[0], c[1]); ow.getChunk(c[0] + 1, c[1]); ow.getChunk(c[0], c[1] + 1); ow.getChunk(c[0] + 1, c[1] + 1);
                for (long l : StructureIndex.get(ow).all(type)) {
                    BlockPos p = BlockPos.fromLong(l);
                    if (p.getX() >= c[0] * 16 + 8 && p.getX() < c[0] * 16 + 24 && p.getZ() >= c[1] * 16 + 8 && p.getZ() < c[1] * 16 + 24) { built++; if (first == null) first = p; break; }
                }
            }
            LuciferTotem.LOG.info("[LT][TEST] {}: {} candidates within 90 chunks, populated {}, built {} ({} ms)", type, cand.size(), tried, built, System.currentTimeMillis() - t0);
            check(first != null, type + " generates in real Overworld worldgen (populate): " + first + " (" + built + " of " + tried + " candidate chunks)");
            if (type.equals(OverworldLore.GALLEY)) galley = first; else altar = first;
        }
        if (galley != null) {
            TileEntity te = ow.getTileEntity(galley);
            boolean notes = false; int bricks = 0;
            if (te instanceof TileEntityChest) {
                TileEntityChest c = (TileEntityChest) te;
                c.fillWithLoot(null);
                for (int i = 0; i < c.getSizeInventory(); i++) { ItemStack s = c.getStackInSlot(i); if (LoreBooks.isNotes(s)) notes = true; if (s.getItem() == net.minecraft.item.Item.getItemFromBlock(ModContent.DUSKSTONE_BRICKS)) bricks += s.getCount(); }
            }
            int wool = 0, wood = 0, ram = 0;
            for (BlockPos q : BlockPos.getAllInBox(galley.add(-17, -3, -17), galley.add(17, 8, 17))) {
                IBlockState s = ow.getBlockState(q);
                if (s.getBlock() == Blocks.WOOL && s.getValue(net.minecraft.block.BlockColored.COLOR) == net.minecraft.item.EnumDyeColor.PURPLE) wool++;
                if (s.getBlock() == Blocks.PLANKS) wood++;
                if (s.getBlock() == ModContent.HALO_BRASS) ram++;
            }
            check(notes && bricks >= 4 && wool >= 3 && wood >= 40 && ram == 1, "galley: hull " + wood + " planks, " + wool + " purple sail scraps, bronze ram; chest holds the Lecture Notes and " + bricks + " repair bricks");
            dump(ow, galley.add(-17, -4, -17), galley.add(17, 8, 17), "lt120_galley.txt", galley);
            BlockPos pred = OverworldLore.nearest(ow, OverworldLore.GALLEY, galley.add(100, 0, 100), 32);
            check(pred != null && pred.distanceSq(galley.add(100, 0, 100)) <= galley.distanceSq(galley.add(100, 0, 100)) + 1, "the whispers find the nearest galley (built or not yet generated): " + pred);
        }
        if (altar != null) {
            AltarLogic.Frame f = AltarLogic.examine(ow, altar, EnumFacing.Axis.X);
            if (f == null || f.whole + f.cracked < 10) f = AltarLogic.examine(ow, altar, EnumFacing.Axis.Z);
            boolean page = false;
            for (BlockPos q : BlockPos.getAllInBox(altar.add(-6, -2, -6), altar.add(6, 2, 6))) {
                TileEntity te = ow.getTileEntity(q);
                if (te instanceof TileEntityChest) { ((TileEntityChest) te).fillWithLoot(null); for (int i = 0; i < 27; i++) if (LoreBooks.isNotes(((TileEntityChest) te).getStackInSlot(i))) page = true; }
            }
            check(f != null && !f.complete() && f.cracked >= 2 && f.missing >= 1 && f.clear && page, "fallen altar: a broken duskstone frame (" + f + "), clear middle, a chest with a loose page");
            dump(ow, altar.add(-7, -3, -7), altar.add(7, 7, 7), "lt120_altar_broken.txt", altar);
        }
        return new BlockPos[]{galley, altar};
    }

    static void dump(WorldServer w, BlockPos a, BlockPos b, String file, BlockPos origin) {
        try (PrintWriter pw = new PrintWriter(DUMP + file, "UTF-8")) {
            for (BlockPos q : BlockPos.getAllInBox(a, b)) {
                IBlockState s = w.getBlockState(q);
                if (s.getBlock() == Blocks.AIR) continue;
                pw.println((q.getX() - origin.getX()) + " " + (q.getY() - origin.getY()) + " " + (q.getZ() - origin.getZ()) + " " + s.getBlock().getRegistryName() + " " + s.getBlock().getMetaFromState(s));
            }
        } catch (Exception e) { LuciferTotem.LOG.warn("dump failed", e); }
    }

    // ------------------------------------------------------------ the altar and the round trip
    static void altarAndTrip(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos altar) throws Exception {
        if (altar == null) {   // fall back to a hand-placed fallen altar on a stone pad
            altar = ow.getSpawnPoint().add(60, 0, 60);
            altar = new BlockPos(altar.getX(), ow.getHeight(altar).getY() + 3, altar.getZ());
            for (BlockPos q : BlockPos.getAllInBox(altar.add(-4, -1, -4), altar.add(7, -1, 4))) ow.setBlockState(q, Blocks.STONE.getDefaultState());
            OverworldLore.Altar.build(ow, altar, EnumFacing.Axis.X, new Random(4));
        }
        AltarLogic.Frame f = AltarLogic.examine(ow, altar, EnumFacing.Axis.X);
        if (f == null || f.whole + f.cracked < 10) f = AltarLogic.examine(ow, altar, EnumFacing.Axis.Z);
        EnumFacing.Axis ax = f.axis;
        long time0 = ow.getWorldTime();
        // tp goes to the Overworld, by the altar
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(altar.getX() + 0.5, altar.getY(), altar.getZ() + 3.5, 0)); }
        tp.setPositionAndUpdate(altar.getX() + 0.5, altar.getY(), altar.getZ() + 3.5);
        revoke(tp, "fallen_altar", "enter_lightfall", "return_lightfall");
        ow.setWorldTime(12500);
        boolean crackedRefused = !AltarLogic.tryLight(ow, AltarLogic.find(ow, PortalLogic.at(altar, ax, 0, 0, 0)), tp);
        // mend it
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) if (u == 0 || u == 3 || v == 0 || v == 4) ow.setBlockState(PortalLogic.at(altar, ax, u, v, 0), ModContent.DUSKSTONE_BRICKS.getDefaultState());
        ow.setWorldTime(6000);
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(ModContent.TOTEM));
        net.minecraft.util.EnumActionResult noon = ModContent.TOTEM.onItemUse(tp, ow, PortalLogic.at(altar, ax, 3, 2, 0), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        boolean noonRefused = !PortalLogic.gateIntact(ow, altar, ax);
        ow.setWorldTime(12600);
        net.minecraft.util.EnumActionResult dusk = ModContent.TOTEM.onItemUse(tp, ow, PortalLogic.at(altar, ax, 3, 2, 0), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        boolean lit = PortalLogic.gateIntact(ow, altar, ax);
        check(crackedRefused && noonRefused && noon == net.minecraft.util.EnumActionResult.SUCCESS && dusk == net.minecraft.util.EnumActionResult.SUCCESS && lit && PortalData.get().findExact(0, altar) != null && LoreEvents.done(tp, "fallen_altar"),
            "the altar refuses while cracked and at noon, lights at dusk when struck (used) with the totem; gate registered; 'Purple Masonry' granted");
        net.minecraft.util.EnumActionResult plain = ModContent.TOTEM.onItemUse(tp, ow, altar.down(5), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        check(plain == net.minecraft.util.EnumActionResult.PASS, "used on anything else the totem is still thrown (onItemUse passes)");
        dump(ow, altar.add(-7, -3, -7), altar.add(7, 7, 7), "lt120_altar_lit.txt", altar);
        ow.setWorldTime(time0);
        // through it
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, PortalLogic.at(altar, ax, 1, 1, 0));
        int[] ret = PortalLogic.returnPoint(tp);
        BlockPos feet = tp.getPosition();
        boolean safe = tp.world.getBlockState(feet.down()).getMaterial().isSolid() && tp.world.isAirBlock(feet) && tp.world.isAirBlock(feet.up()) && !tp.world.getBlockState(feet).getMaterial().isLiquid();
        check(tp.dimension == LuciferTotem.dimId && ret != null && ret[0] == 0 && new BlockPos(ret[1], ret[2], ret[3]).equals(altar) && safe && LoreEvents.done(tp, "enter_lightfall"),
            "through the altar into the Lightfall; the way home (dim 0, " + (ret == null ? "-" : ret[1] + "," + ret[2] + "," + ret[3]) + ") is in the player's NBT; arrival safe at " + feet);
        PortalData.Gate arrival = PortalData.get().find(LuciferTotem.dimId, PortalData.get().findExact(0, altar).lbase);
        check(arrival != null && arrival.linked && arrival.ldim == 0 && arrival.lbase.equals(altar) && PortalLogic.gateIntact(lf, arrival.base, arrival.axis), "a matching return gate stands at the arrival point, linked back to the altar: " + arrival);
        // (b) a gate home built far away inside the Lightfall from duskstone bricks and struck with the totem
        BlockPos far = new BlockPos(feet.getX() + 320, 110, feet.getZ() + 280);
        for (BlockPos q : BlockPos.getAllInBox(far.add(-2, -1, -3), far.add(5, -1, 3))) lf.setBlockState(q, ModContent.DUSKSTONE.getDefaultState());
        for (BlockPos q : BlockPos.getAllInBox(far.add(-2, 0, -3), far.add(5, 6, 3))) lf.setBlockState(q, Blocks.AIR.getDefaultState());
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) if (u == 0 || u == 3 || v == 0 || v == 4) lf.setBlockState(far.add(u, v, 0), ModContent.DUSKSTONE_BRICKS.getDefaultState());
        tp.setPositionAndUpdate(far.getX() + 1.5, far.getY(), far.getZ() + 2.5);
        long lt0 = lf.getWorldTime(); lf.setWorldTime(6000);   // any time of day works in the Lightfall
        net.minecraft.util.EnumActionResult r2 = ModContent.TOTEM.onItemUse(tp, lf, far.add(0, 2, 0), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        lf.setWorldTime(lt0);
        check(r2 == net.minecraft.util.EnumActionResult.SUCCESS && PortalLogic.gateIntact(lf, far, EnumFacing.Axis.X), "a duskstone-brick frame built in the Lightfall lights at any hour (Lightfall-side altar)");
        revoke(tp, "return_lightfall");
        tp.timeUntilPortal = 0;
        int trips0 = PortalLogic.returnTrips;
        PortalLogic.enterPortal(tp, far.add(1, 1, 0));
        double home = tp.getPosition().distanceSq(altar);
        check(tp.dimension == 0 && home < 64 && PortalLogic.returnTrips == trips0 + 1 && LoreEvents.done(tp, "return_lightfall"),
            "Return from the Lightfall: the far gate leads home to the altar the player came by (dist " + String.format("%.1f", Math.sqrt(home)) + "); advancement granted");
        // the altar gate breaks while the player is away: the way home still works (a new gate near the return point)
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, PortalLogic.at(altar, ax, 1, 1, 0));
        ow.setBlockState(PortalLogic.at(altar, ax, 0, 2, 0), Blocks.AIR.getDefaultState(), 3);
        boolean broken = !PortalLogic.gateIntact(ow, altar, ax);
        tp.timeUntilPortal = 0;
        PortalData.Gate back = PortalData.get().find(LuciferTotem.dimId, tp.getPosition());
        if (back == null) back = arrival;
        PortalLogic.enterPortal(tp, PortalLogic.at(back.base, back.axis, 1, 1, 0));
        BlockPos f2 = tp.getPosition();
        boolean safe2 = ow.getBlockState(f2.down()).getMaterial().isSolid() && ow.isAirBlock(f2) && ow.isAirBlock(f2.up());
        check(broken && tp.dimension == 0 && Math.sqrt(f2.distanceSq(altar)) < 64 && safe2, "with the altar broken, the Lightfall gate still brings the player home near it, onto a fresh safe gate (" + f2 + ", " + String.format("%.0f", Math.sqrt(f2.distanceSq(altar))) + " blocks from the altar)");
        // no return point at all (a player from 1.1.x): home to bed/world spawn, and a login hint
        tp.timeUntilPortal = 0;
        tp.changeDimension(LuciferTotem.dimId, new LightfallTeleporter(far.getX() + 1.5, far.getY(), far.getZ() + 2.5, 0));
        HData.get(tp).removeTag("hasReturn"); HData.get(tp).removeTag("returnPos"); HData.get(tp).removeTag("returnDim");
        int hints = LoreEvents.loginHints;
        boolean hinted = LoreEvents.loginHint(tp);
        Object[] hh = LoreEvents.homeHint(tp);
        check(hinted && LoreEvents.loginHints == hints + 1 && "whisper.lucifertotem.home_gate".equals(hh[0]), "first login in the Lightfall without a way home: the totem names the nearest gate home (" + hh[0] + ")");
        // a second, unlinked Lightfall gate with no return point: lands by the world spawn
        BlockPos far2 = far.add(40, 0, 0);
        for (BlockPos q : BlockPos.getAllInBox(far2.add(-2, -1, -3), far2.add(5, -1, 3))) lf.setBlockState(q, ModContent.DUSKSTONE.getDefaultState());
        for (BlockPos q : BlockPos.getAllInBox(far2.add(-2, 0, -3), far2.add(5, 6, 3))) lf.setBlockState(q, Blocks.AIR.getDefaultState());
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) if (u == 0 || u == 3 || v == 0 || v == 4) lf.setBlockState(far2.add(u, v, 0), ModContent.DUSKSTONE_BRICKS.getDefaultState());
        AltarLogic.light(lf, far2, EnumFacing.Axis.X);
        tp.setPositionAndUpdate(far2.getX() + 1.5, far2.getY(), far2.getZ() + 1.5);
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, far2.add(1, 1, 0));
        BlockPos sp = ow.getSpawnPoint();
        double ds = Math.sqrt(Math.pow(tp.posX - sp.getX(), 2) + Math.pow(tp.posZ - sp.getZ(), 2));
        check(tp.dimension == 0 && ds < 120, "no remembered way home: a new Lightfall gate leads to the bed / world spawn (" + String.format("%.0f", ds) + " blocks from spawn)");
    }

    // ------------------------------------------------------------ (c) never into a wall, water or the void
    static void safety(WorldServer ow, WorldServer lf) {
        BlockPos b = ow.getSpawnPoint().add(-80, 0, 40);
        b = new BlockPos(b.getX(), 90, b.getZ());
        // a gate walled in on both sides, with water in front
        for (BlockPos q : BlockPos.getAllInBox(b.add(-3, -2, -4), b.add(6, 6, 4))) ow.setBlockState(q, Blocks.STONE.getDefaultState(), 2);
        for (int u = 1; u <= 2; u++) for (int v = 0; v <= 1; v++) ow.setBlockState(PortalLogic.at(b, EnumFacing.Axis.X, u, v, 2), Blocks.WATER.getDefaultState(), 2);
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) ow.setBlockState(b.add(u, v, 0), (u == 0 || u == 3 || v == 0 || v == 4) ? ModContent.DUSKSTONE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        AltarLogic.light(ow, b, EnumFacing.Axis.X);
        PortalData.Gate g = PortalData.get().findExact(0, b);
        int carved0 = PortalLogic.carved;
        double[] at = PortalLogic.arrival(ow, g);
        BlockPos f = new BlockPos(at[0], at[1], at[2]);
        boolean ok = ow.isAirBlock(f) && ow.isAirBlock(f.up()) && ow.getBlockState(f.down()).getMaterial().isSolid() && !ow.getBlockState(f.down()).getMaterial().isLiquid();
        check(ok && PortalLogic.carved == carved0 + 1, "a gate walled in by stone (water in front): arrival clears a dry doorway rather than putting the player in a wall: " + f);
        // a gate with open ground behind: arrival uses the dry side, no digging
        BlockPos c = b.add(0, 0, 20);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-3, -1, -4), c.add(6, -1, 4))) ow.setBlockState(q, Blocks.STONE.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-3, 0, -4), c.add(6, 6, 4))) ow.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        for (int u = 1; u <= 2; u++) { ow.setBlockState(PortalLogic.at(c, EnumFacing.Axis.X, u, 0, 2), Blocks.WATER.getDefaultState(), 2); ow.setBlockState(PortalLogic.at(c, EnumFacing.Axis.X, u, 0, 1), Blocks.WATER.getDefaultState(), 2); }
        for (int u = 0; u <= 3; u++) for (int v = 0; v <= 4; v++) if (u == 0 || u == 3 || v == 0 || v == 4) ow.setBlockState(c.add(u, v, 0), ModContent.DUSKSTONE_BRICKS.getDefaultState(), 2);
        AltarLogic.light(ow, c, EnumFacing.Axis.X);
        int carved1 = PortalLogic.carved;
        double[] at2 = PortalLogic.arrival(ow, PortalData.get().findExact(0, c));
        BlockPos f2 = new BlockPos(at2[0], at2[1], at2[2]);
        check(PortalLogic.carved == carved1 && f2.getZ() < c.getZ() && !ow.getBlockState(f2).getMaterial().isLiquid() && ow.getBlockState(f2.down()).getMaterial().isSolid(), "water in front of a gate: the traveller arrives on the dry side behind it: " + f2);
        // the Lightfall's own gate sites: never water, never the void
        int bad = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos s = lucifertotem.world.PortalLogicAccess.findSite(lf, 1000 + i * 977, -700 + i * 613, EnumFacing.Axis.X);
            if (lf.getBlockState(s.down()).getMaterial().isLiquid() || lf.getBlockState(s).getMaterial().isLiquid()) bad++;
            if (s.getY() < 3) bad++;
        }
        check(bad == 0, "6 Lightfall gate sites far apart: none in water or over the void");
    }

    // ------------------------------------------------------------ the Wandering Philologist
    static void philologist(WorldServer ow, EntityPlayerMP tp) throws Exception {
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(0, 100, 0, 0)); }
        BlockPos c = ow.getSpawnPoint().add(-40, 0, -60);
        c = new BlockPos(c.getX(), 120, c.getZ());
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, -1, -20), c.add(20, -1, 20))) ow.setBlockState(q, Blocks.GRASS.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, 0, -20), c.add(20, 4, 20))) if (!ow.isAirBlock(q)) ow.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        tp.setPositionAndUpdate(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        HData.get(tp).setInteger("philoVisits", 0);
        int talk = LTConfig.lore.philologistTalkSeconds;
        LTConfig.lore.philologistTalkSeconds = 10;
        EntityAmmonius a = LoreEvents.summonPhilologist(tp);
        check(a != null && a.philologist && a.getDistance(tp) > 8 && a.getDistance(tp) < 17 && HData.get(tp).getInteger("philoVisits") == 1, "the Wandering Philologist appears 9-15 blocks from the player" + (a == null ? "" : " (" + String.format("%.1f", a.getDistance(tp)) + ")"));
        if (a == null) { LTConfig.lore.philologistTalkSeconds = talk; return; }
        float hp = a.getHealth();
        boolean hurt = a.attackEntityFrom(DamageSource.causePlayerDamage(tp), 20f);
        check(!hurt && a.getHealth() == hp && !a.isBreedingItem(new ItemStack(Items.WHEAT)), "he cannot be hurt (or bred, or traded with): not farmable");
        int rants0 = EntityAmmonius.rantsSaid;
        int pages0 = EntityAmmonius.philoPages, gone0 = EntityAmmonius.philoVanished;
        BlockPos rift = null; int riftSeen = 0;
        for (int i = 0; i < 700 && !a.isDead; i++) { a.onUpdate(); a.ticksExisted++; if (a.rift != null) { rift = a.rift; riftSeen++; } }
        int pages = 0, other = 0;
        for (EntityItem ei : ow.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(c).grow(24))) { if (LoreBooks.isNotes(ei.getItem())) pages++; else other++; ei.setDead(); }
        check(EntityAmmonius.rantsSaid > rants0 && EntityAmmonius.philoPages == pages0 + 1 && pages == 1 && other == 0 && riftSeen > 0 && a.isDead && EntityAmmonius.philoVanished == gone0 + 1,
            "he lectures (rants), drops one page of his notes, walks into a purple rift (" + rift + ", " + riftSeen + " ticks open) and is gone; no other drops");
        LTConfig.lore.philologistTalkSeconds = talk;
        // cap: once per player by default
        double pct = LTConfig.lore.philologistChancePercentPerHour; boolean req = LTConfig.lore.philologistRequiresTotem;
        LTConfig.lore.philologistChancePercentPerHour = 1e9; LTConfig.lore.philologistRequiresTotem = false;
        int v0 = EntityAmmonius.philoVisits;
        LoreEvents.philologistChance(tp, ow);
        boolean capped = EntityAmmonius.philoVisits == v0;
        HData.get(tp).setInteger("philoVisits", 0);
        LoreEvents.philologistChance(tp, ow);
        boolean again = EntityAmmonius.philoVisits == v0 + 1;
        LTConfig.lore.philologistChancePercentPerHour = pct; LTConfig.lore.philologistRequiresTotem = req;
        check(capped && again, "visits are capped per player (philologistMaxPerPlayer=" + LTConfig.lore.philologistMaxPerPlayer + "); the chance roll summons him otherwise");
        // a save never keeps him: reloaded, he steps out at once
        for (EntityAmmonius x : ow.getEntities(EntityAmmonius.class, x -> x.philologist && !x.isDead)) {
            NBTTagCompound t = new NBTTagCompound(); x.writeToNBT(t);
            EntityAmmonius y = new EntityAmmonius(ow); y.readFromNBT(t);
            y.setPosition(x.posX, x.posY, x.posZ);
            y.onLivingUpdate();
            check(y.philologist && y.isDead, "a philologist loaded from a save vanishes at once (a visit never outlasts the session)");
            x.setDead();
        }
        check(HAdv.exists(ow.getMinecraftServer(), "philologist"), "advancement 'The Wandering Philologist' exists (granted when he greets the player)");
    }

    // ------------------------------------------------------------ whispers, trail checks, altar ambience
    static void whispers(WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos altar) {
        if (tp.dimension != 0) { tp.timeUntilPortal = 0; tp.changeDimension(0, new LightfallTeleporter(0, 100, 0, 0)); }
        BlockPos sp = ow.getSpawnPoint();
        tp.setPositionAndUpdate(sp.getX() + 0.5, ow.getHeight(sp).getY(), sp.getZ() + 0.5);
        revoke(tp, TRAIL);
        tp.inventory.clear();
        LoreEvents.tick2s(tp, 1);
        boolean noTotem = !LoreEvents.done(tp, "aww_gawd");
        tp.inventory.addItemStackToInventory(new ItemStack(ModContent.TOTEM));
        LoreEvents.tick2s(tp, 2);
        check(noTotem && LoreEvents.done(tp, "aww_gawd"), "holding a Totem of Lucifer (fished, crafted or found) grants 'Aww Gawd' (and plays the clip once)");
        List<String> keys = new ArrayList<>();
        keys.add((String) LoreEvents.hint(tp)[0]);
        HAdv.grant(tp, "galley"); keys.add((String) LoreEvents.hint(tp)[0]);
        HAdv.grant(tp, "lecture_notes"); keys.add((String) LoreEvents.hint(tp)[0]);
        HAdv.grant(tp, "fallen_altar"); keys.add((String) LoreEvents.hint(tp)[0]);
        HAdv.grant(tp, "enter_lightfall"); keys.add((String) LoreEvents.hint(tp)[0]);
        LuciferTotem.LOG.info("[LT][TEST] whisper steps: {}", keys);
        check(keys.get(0).startsWith("whisper.lucifertotem.galley") && keys.get(1).equals("whisper.lucifertotem.read") && keys.get(2).startsWith("whisper.lucifertotem.altar")
            && keys.get(3).equals("whisper.lucifertotem.step_through") && keys.get(4).equals("whisper.lucifertotem.go_back"), "the totem's whispers walk the trail: " + keys);
        int w0 = LoreEvents.whispers;
        HData.get(tp).setLong("lastWhisper", 0);
        LoreEvents.tick2s(tp, 20000);
        check(LoreEvents.whispers == w0 + 1, "a carried totem whispers on its own (every " + LTConfig.lore.totemWhisperSeconds + " s)");
        // altar ambience: forced rate, from the altar's direction
        if (altar != null) {
            tp.setPositionAndUpdate(altar.getX() + 30.5, altar.getY() + 1, altar.getZ() + 0.5);
            double f = LTConfig.lore.altarAmbienceFactor; int m = LTConfig.distantVoiceMinutesOverworld;
            LTConfig.lore.altarAmbienceFactor = 0.01; LTConfig.distantVoiceMinutesOverworld = 1;
            int v0 = LoreEvents.altarVoices;
            LoreEvents.altarAmbience(tp, ow);
            LTConfig.lore.altarAmbienceFactor = f; LTConfig.distantVoiceMinutesOverworld = m;
            tp.setPositionAndUpdate(altar.getX() + 500.5, altar.getY() + 1, altar.getZ() + 0.5);
            int v1 = LoreEvents.altarVoices;
            LoreEvents.altarAmbience(tp, ow);
            check(v1 == v0 + 1 && LoreEvents.altarVoices == v1, "near a Fallen Altar the distant 'PURPLE' plays more often (every ~" + String.format("%.0f", LTConfig.distantVoiceMinutesOverworld * LTConfig.lore.altarAmbienceFactor) + " min vs " + LTConfig.distantVoiceMinutesOverworld + "); far away it doesn't");
        }
        // in the Lightfall: the Library, then home
        tp.timeUntilPortal = 0;
        BlockPos lib = lucifertotem.world.LibraryGen.nearest(lf.getSeed(), new BlockPos(0, 64, 0), 6000);
        tp.changeDimension(LuciferTotem.dimId, new LightfallTeleporter(lib.getX() + 300, 110, lib.getZ(), 0));
        String k1 = (String) LoreEvents.hint(tp)[0];
        tp.setPositionAndUpdate(lib.getX() + 0.5, 110, lib.getZ() + 0.5);
        LoreEvents.tick2s(tp, 3);
        boolean libDone = LoreEvents.done(tp, "library");
        String k2 = (String) LoreEvents.hint(tp)[0];
        check("whisper.lucifertotem.library".equals(k1) && libDone && "whisper.lucifertotem.meet".equals(k2), "in the Lightfall the totem points to the Library (" + k1 + "), then to Ammonius (" + k2 + "); reaching it grants 'Overdue Books'");
        EntityAmmonius am = new EntityAmmonius(lf);
        am.setPosition(lib.getX() + 4.5, 110, lib.getZ() + 0.5);
        lf.spawnEntity(am);
        for (int i = 0; i < 12; i++) { am.onLivingUpdate(); am.ticksExisted++; }
        check(LoreEvents.done(tp, "meet_ammonius"), "meeting Doctor Ammonius (his greeting) grants 'Office Hours'");
        am.setDead();
    }
}
