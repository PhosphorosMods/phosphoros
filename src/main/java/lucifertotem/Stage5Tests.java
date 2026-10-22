package lucifertotem;

import lucifertotem.hillman.HData;
import lucifertotem.hillman.HPotions;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.v130.*;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/** 1.3.0 stage 5 "Monsters & Mazes" */
public class Stage5Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0/5] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("registration", () -> registration(srv));
        guard("drakaina", () -> drakaina(lf, tp, pad));
        guard("hydra", () -> hydra(lf, tp, pad));
        guard("asklepieion", () -> asklepieion(lf, tp, pad));
        guard("labyrinth", () -> labyrinth(lf, tp, pad));
    }

    static void registration(MinecraftServer srv) {
        check(Stage5.DRAKAINA_SCALE != null && Stage5.HYDRA_VENOM != null && Stage5.RED_THREAD != null && Stage5.KLINE != null, "Drakaina Scale, Hydra Venom, Red Thread, Kline registered");
        int e = 0; for (Class<? extends net.minecraft.entity.Entity> c : java.util.Arrays.asList(EntityDrakaina.class, EntityHydra.class, EntityVenomSpit.class))
            if (net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(c) != null) e++;
        check(e == 3, "Drakaina, Hydra and venom entities registered (" + e + ")");
        int a = 0; String[] advs = {"drakaina", "hydra", "labyrinth", "asklepieion"};
        for (String n : advs) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == advs.length, "stage 5 advancements load (" + a + "/" + advs.length + ")");
        int q = 0; for (Interview.Q k : Interview.QUESTIONS) if (k.adv == null || TestHarness130.advancement(srv, k.adv) != null) q++;
        check(q == Interview.QUESTIONS.length, "every interview question's advancement now exists (" + q + "/" + Interview.QUESTIONS.length + ")");
        check("drakaina".equals(Apothecary.CODE.get("lucifertotem:drakaina_scale")) && "hydra".equals(Apothecary.CODE.get("lucifertotem:hydra_venom")), "Drakaina Scale and Hydra Venom go in the Theriac Crock");
        int t = 0; for (String s : new String[]{"entity/drakaina", "entity/hydra", "blocks/kline_top", "blocks/kline_side", "blocks/kline_end", "items/drakaina_scale", "items/hydra_venom", "items/red_thread"})
            if (Stage5Tests.class.getResource("/assets/lucifertotem/textures/" + s + ".png") != null) t++;
        check(t == 8, "stage 5 textures ship (" + t + "/8)");
        check(net.minecraft.item.crafting.CraftingManager.getRecipe(new net.minecraft.util.ResourceLocation(LuciferTotem.MODID, "toxicon_arrow_hydra")) != null, "Hydra Venom makes 8 Toxicon Arrows");
    }

    static BlockPos deep(WorldServer lf, int bx, int bz, int y) {
        for (int x = bx - 2; x < bx + 18; x++) for (int z = bz - 2; z < bz + 18; z++) {
            for (int yy = y - 30; yy < y; yy++) lf.setBlockState(new BlockPos(x, yy, z), ModContent.DUSKSTONE.getDefaultState(), 2);
            lf.setBlockState(new BlockPos(x, y, z), ModContent.STAR_GRASS.getDefaultState(), 2);
            for (int yy = y + 1; yy < y + 12; yy++) lf.setBlockToAir(new BlockPos(x, yy, z));
        }
        return new BlockPos(bx + 8, y, bz + 8);
    }
    static EntityCow cow(WorldServer lf, double x, double y, double z) { EntityCow c = new EntityCow(lf); c.setLocationAndAngles(x, y, z, 0, 0); lf.spawnEntity(c); return c; }

    static void drakaina(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 440, bz = pad.getZ(), y = 200;
        BlockPos c = deep(lf, bx, bz, y);
        boolean ok = Stage5.lair(lf, bx, bz, c.getX(), y, c.getZ());
        check(ok, "a lair is carved beneath the Delphic vent");
        int ly = y - 11;
        check(lf.getBlockState(new BlockPos(c.getX() + 2, y, c.getZ() - 3)).getBlock() == Blocks.TRAPDOOR && lf.getBlockState(new BlockPos(c.getX() + 2, ly + 3, c.getZ() - 3)).getBlock() == Blocks.LADDER, "a trapdoor in the shrine floor, a ladder down");
        check(Stage3Tests.chests(lf, new BlockPos(c.getX(), ly, c.getZ()), 6, Stage5.LOOT_LAIR) == 1, "her hoard chest");
        List<EntityDrakaina> ds = lf.getEntitiesWithinAABB(EntityDrakaina.class, new AxisAlignedBB(c.getX() - 5, ly - 1, c.getZ() - 5, c.getX() + 5, ly + 5, c.getZ() + 5));
        check(ds.size() == 1, "the Drakaina coils in her lair (" + ds.size() + ")");
        check(StructureIndex.get(lf).all("drakaina_lair").contains(new BlockPos(c.getX(), ly, c.getZ()).toLong()), "the lair is recorded");
        if (ds.isEmpty()) return;
        EntityDrakaina d = ds.get(0);
        check(d.getMaxHealth() >= 80 && !d.isNonBoss(), "80 health, a boss bar");
        EntityCow v = cow(lf, c.getX() + 0.5, ly, c.getZ() - 1.5);
        int sp = EntityDrakaina.spits;
        d.attackEntityWithRangedAttack(v, 1f);
        check(EntityDrakaina.spits == sp + 1, "she spits venom");
        EntityVenomSpit s = new EntityVenomSpit(lf, d);
        s.hit(v);
        check(v.isPotionActive(MobEffects.POISON), "the venom poisons");
        v.removePotionEffect(MobEffects.POISON);
        d.attackEntityAsMob(v);
        check(v.isPotionActive(MobEffects.POISON), "so does her bite");
        check(!d.isPotionApplicable(new PotionEffect(MobEffects.POISON, 100)), "she is immune to poison");
        tp.setPositionAndUpdate(c.getX() + 0.5, ly, c.getZ() + 2.5);
        d.attackEntityFrom(DamageSource.causePlayerDamage(tp), 1000f);
        check(d.getHealth() <= 0 && TestHarness130.adv(tp, "drakaina"), "she can be slain: 'Before Apollo'");
        v.setDead();
        lf.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new AxisAlignedBB(c).grow(20, 20, 20)).forEach(net.minecraft.entity.Entity::setDead);
    }

    static void hydra(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 480, bz = pad.getZ(), y = 200;
        BlockPos c = deep(lf, bx, bz, y);
        // a sunken shrine room like LightfallStructures.crypt(): shell 9x9x7 at fy
        int fy = y - 12, cx = c.getX(), cz = c.getZ();
        for (int yy = fy; yy <= fy + 6; yy++) for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++) {
            boolean shell = yy == fy || yy == fy + 6 || Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
            lf.setBlockState(new BlockPos(x, yy, z), shell ? ModContent.DUSKSTONE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        }
        int old = LTConfig.v130.hydraPercent;
        LTConfig.v130.hydraPercent = 0;
        check(!Stage5.sunkenChapel(lf, bx, bz, cx, fy, cz), "hydraPercent=0: no Hydra");
        LTConfig.v130.hydraPercent = 100;
        boolean ok = Stage5.sunkenChapel(lf, bx, bz, cx, fy, cz);
        LTConfig.v130.hydraPercent = old;
        check(ok, "the shrine floods into a Sunken Chapel");
        check(lf.getBlockState(new BlockPos(cx + 1, fy + 1, cz + 1)).getBlock() == Blocks.WATER, "the chapel is flooded");
        check(Stage3Tests.paintings(lf, new BlockPos(cx, fy + 3, cz), "hydra") == 1, "Lady Babylon's 'Hydra' hangs in the chapel");
        List<EntityHydra> hs = lf.getEntitiesWithinAABB(EntityHydra.class, new AxisAlignedBB(cx - 5, fy, cz - 5, cx + 5, fy + 6, cz + 5));
        check(hs.size() == 1, "the Hydra lurks there (" + hs.size() + ")");
        if (hs.isEmpty()) return;
        EntityHydra h = hs.get(0);
        check(h.heads() == 3 && h.canBreatheUnderwater(), "three heads; breathes water");
        tp.setPositionAndUpdate(cx + 0.5, fy + 1, cz + 2.5);
        DamageSource src = DamageSource.causePlayerDamage(tp);
        h.hurtResistantTime = 0; h.attackEntityFrom(src, 5f);
        check(h.heads() == 3, "a light blow cuts nothing");
        h.hurtResistantTime = 0; h.attackEntityFrom(src, 10f);
        check(h.heads() == 2 && h.pending == 2, "a heavy blow cuts off a head");
        h.ticksExisted = h.regrowAt; h.onLivingUpdate();
        check(h.heads() == 4, "...and two grow back (" + h.heads() + ")");
        h.setFire(10);
        h.hurtResistantTime = 0; h.attackEntityFrom(src, 10f);
        check(h.heads() == 3 && h.regrowAt < 0, "burning: the stump is cauterized");
        h.extinguish();
        EntityCow v = cow(lf, cx + 0.5, fy + 1, cz - 3.0);
        float before = v.getHealth();
        h.attackEntityAsMob(v);
        check(v.getHealth() < before && v.isPotionActive(MobEffects.POISON), "every head bites (" + (before - v.getHealth()) + " damage)");
        v.setDead();
        h.setHeads(1); h.setHealth(h.getMaxHealth());
        h.hurtResistantTime = 0; h.attackEntityFrom(src, 10f);
        check(h.getHealth() <= 0 && TestHarness130.adv(tp, "hydra"), "cut the last head and she dies: 'Bring a Torch'");
        lf.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new AxisAlignedBB(c).grow(20, 20, 20)).forEach(net.minecraft.entity.Entity::setDead);
    }

    static void asklepieion(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 520, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        boolean ok = Gen130.build(lf, "asklepieion", bx, bz, new Random(5));
        check(ok, "an Asklepieion builds");
        int k = 0; BlockPos kl = null;
        for (BlockPos q : BlockPos.getAllInBox(c.add(-6, 0, -6), c.add(6, 3, 6))) if (lf.getBlockState(q).getBlock() == Stage5.KLINE) { k++; kl = q.toImmutable(); }
        check(k == 4, "four couches in the enkoimeterion (" + k + ")");
        check(Stage3Tests.paintings(lf, c, "asklepios") == 1, "Lady Babylon's 'Asklepios' hangs over the sacred spring");
        check(lf.getBlockState(c.add(0, 0, -2)).getBlock() == Blocks.WATER, "the sacred spring");
        check(Stage3Tests.chests(lf, c, 8, Stage5.LOOT_ASKLEPIEION) == 1, "a chest of remedies");
        if (kl == null) return;
        HData.get(tp).removeTag("asklepiosDay");
        tp.addPotionEffect(new PotionEffect(MobEffects.POISON, 400, 1));
        tp.addPotionEffect(new PotionEffect(HPotions.INSANIA, 400, 0));
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 400, 0));
        tp.addPotionEffect(new PotionEffect(MobEffects.SPEED, 400, 0));
        check("healed".equals(Stage5.incubate(lf, kl, tp)), "lie on the kline: a healing dream");
        check(!tp.isPotionActive(MobEffects.POISON) && !tp.isPotionActive(HPotions.INSANIA) && !tp.isPotionActive(V130.MANIA), "poison, Insania and Mania are gone");
        check(tp.isPotionActive(MobEffects.SPEED), "good effects stay");
        check(tp.isPotionActive(MobEffects.ABSORPTION) && TestHarness130.adv(tp, "asklepieion"), "Absorption, 'Incubation'");
        check("rested".equals(Stage5.incubate(lf, kl, tp)), "once a day");
        Stage5.dayShift += 24000;
        check("healed".equals(Stage5.incubate(lf, kl, tp)), "the next day, again");
        Stage5.dayShift = 0;
        for (net.minecraft.potion.Potion p : new net.minecraft.potion.Potion[]{MobEffects.SPEED, MobEffects.ABSORPTION, MobEffects.REGENERATION, MobEffects.BLINDNESS}) tp.removePotionEffect(p);
    }

    static void labyrinth(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        long seed = lf.getSeed();
        // the maze itself
        BlockPos fake = new BlockPos(16 * 1000, Labyrinth.LY, 16 * 1000);
        Labyrinth.Maze m = Labyrinth.maze(seed, fake);
        check(Labyrinth.maze(seed, fake) == m, "mazes are cached");
        check(!m.wall[Labyrinth.P * Labyrinth.MID + 1][0] && !m.wall[Labyrinth.P * Labyrinth.MID + 1][Labyrinth.GATE_Z], "an entrance, and a gate into the heart");
        List<int[]> path = Labyrinth.path(m, Labyrinth.MID, 0);
        check(path.size() > 10, "every maze is solvable from the entrance (" + path.size() + " cells)");
        int unreachable = 0;
        for (int i = 0; i < Labyrinth.N; i++) for (int j = 0; j < Labyrinth.N; j++) if (!Labyrinth.sanctum(i, j) && Labyrinth.path(m, i, j).isEmpty()) unreachable++;
        check(unreachable == 0, "no sealed-off corners (" + unreachable + ")");
        check(m.deadEnds.size() >= 7, "enough dead ends for the key, treasure and guards (" + m.deadEnds.size() + ")");
        // placement: one per region at most, never near the world origin or a Library
        int found = 0, clash = 0; BlockPos real = null;
        for (int rx = -6; rx <= 6; rx++) for (int rz = -6; rz <= 6; rz++) {
            BlockPos o = Labyrinth.origin(seed, rx, rz);
            if (o == null) continue;
            found++;
            if (lucifertotem.world.LibraryGen.nearest(seed, Labyrinth.center(o), 100) != null) clash++;
            if (real == null || o.distanceSq(pad) < real.distanceSq(pad)) real = o;
        }
        check(found > 20 && found < 169 && clash == 0, "labyrinths: " + found + " of 169 regions, none near a Library");
        if (real == null) return;
        // generate the real one through the normal chunk pipeline and time the slices
        long n0 = Labyrinth.nanos; int s0 = Labyrinth.slices; long w0 = Labyrinth.worst;
        Labyrinth.worst = 0;
        int cx0 = (real.getX() >> 4) - 2, cz0 = (real.getZ() >> 4) - 2, cx1 = ((real.getX() + Labyrinth.SIZE) >> 4) + 2, cz1 = ((real.getZ() + Labyrinth.SIZE) >> 4) + 2;
        for (int x = cx0; x <= cx1; x++) for (int z = cz0; z <= cz1; z++) lf.getChunk(x, z);
        int sl = Labyrinth.slices - s0;
        double totalMs = (Labyrinth.nanos - n0) / 1e6, worstMs = Labyrinth.worst / 1e6;
        Labyrinth.worst = Math.max(w0, Labyrinth.worst);
        check(sl >= 16, "the Labyrinth at " + real + " was built in " + sl + " slices");
        check(worstMs < 60 && totalMs < 1500, String.format("spread out: worst slice %.1f ms, total %.0f ms (the Library's single 2.5 s stall is not repeated)", worstMs, totalMs));
        BlockPos c = Labyrinth.center(real);
        int ox = real.getX(), oz = real.getZ(), LY = Labyrinth.LY;
        check(lf.getBlockState(new BlockPos(ox + 5, LY - 1, oz + 5)).getMaterial().isSolid(), "its floor floats at y " + LY);
        int gates = 0; for (int x = ox; x < ox + Labyrinth.SIZE; x++) if (lf.getBlockState(new BlockPos(x, LY, oz + Labyrinth.GATE_Z)).getBlock() == Stage3.HEKATE_GATE) gates++;
        check(gates == 2, "a Gate of Hekate guards the heart (" + gates + ")");
        check(Stage3Tests.paintings(lf, c, "labyrinth") == 1, "Lady Babylon's 'Labyrinth' hangs at the heart");
        check(Stage3Tests.chests(lf, c, 10, Stage5.LOOT_LABYRINTH) >= 2, "treasure at the heart");
        // (before the key count: reading a chest's slots rolls its loot)
        BlockPos ep = Labyrinth.entrance(real);
        TileEntity ete = lf.getTileEntity(ep.add(-2, 0, -1));
        String elt = ete == null ? "no chest" : ete.writeToNBT(new net.minecraft.nbt.NBTTagCompound()).getString("LootTable");
        check(Stage5.LOOT_LAB_ENTRANCE.toString().equals(elt), "an entrance chest on the porch (the Red Thread) " + elt);
        int keys = 0;
        for (TileEntity te : lf.loadedTileEntityList) if (te instanceof TileEntityChest && Math.abs(te.getPos().getX() - c.getX()) < 40 && Math.abs(te.getPos().getZ() - c.getZ()) < 40)
            for (int i = 0; i < ((TileEntityChest) te).getSizeInventory(); i++) if (((TileEntityChest) te).getStackInSlot(i).getItem() == Stage3.KEY_OF_HEKATE) keys++;
        check(keys == 1, "the Key of Hekate lies in a dead end");
        List<EntityHekateHound> hounds = lf.getEntitiesWithinAABB(EntityHekateHound.class, new AxisAlignedBB(ox, LY - 1, oz, ox + Labyrinth.SIZE, LY + 4, oz + Labyrinth.SIZE));
        long g = hounds.stream().filter(hh -> hh.guard).count();
        check(g == 3, "three of Hekate's hounds stand guard (" + g + ")");
        hounds.forEach(net.minecraft.entity.Entity::setDead);
        check(StructureIndex.get(lf).all("labyrinth").contains(new BlockPos(c.getX(), LY, c.getZ()).toLong()), "recorded for the oracles");
        // the Red Thread
        tp.setPositionAndUpdate(ox + 0.5 + Labyrinth.P * Labyrinth.MID + 1, LY, oz + 1.5);
        check("path".equals(Labyrinth.guide(tp)) && Labyrinth.lastPath > 5, "inside, the Red Thread shows the path (" + Labyrinth.lastPath + " cells)");
        tp.setPositionAndUpdate(ox - 40.5, LY - 30, oz - 40.5);
        check("outside".equals(Labyrinth.guide(tp)), "outside, it points to the entrance");
        tp.setPositionAndUpdate(c.getX() + 0.5, LY, c.getZ() + 2.5);
        check("heart".equals(Labyrinth.guide(tp)), "at the heart it is still");
        Labyrinth.checkHeart(tp);
        check(TestHarness130.adv(tp, "labyrinth"), "'The Lady at the Heart'");
        ItemStack thread = new ItemStack(Stage5.RED_THREAD);
        check(thread.getMaxDamage() == 64 && thread.getMaxStackSize() == 1, "the thread wears out after 64 pulls");
        tp.setPositionAndUpdate(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5);
    }
}
