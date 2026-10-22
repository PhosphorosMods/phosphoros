package lucifertotem;

import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.hillman.HPotions;
import lucifertotem.lore.LoreEvents;
import lucifertotem.v130.Apothecary;
import lucifertotem.v130.EntityMurex;
import lucifertotem.v130.V130;
import lucifertotem.v130.V130Gen;
import lucifertotem.world.LightfallChunkGenerator;
import lucifertotem.world.LightfallTeleporter;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;
import java.util.Map;

/** 1.3.0 self-tests, one block per stage. */
public class TestHarness130 {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0] " + what); }
    static void fail(String what, Throwable t) { check(false, what + " tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] 1.3.0 " + what, t); }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp, int R) {
        int startDim = tp.dimension; BlockPos startPos = tp.getPosition();
        BlockPos pad = null;
        try { pad = pad(lf, tp); } catch (Throwable t) { fail("pad", t); }
        try { registration(srv, lf); } catch (Throwable t) { fail("registration", t); }
        if (pad != null) {
            try { brainSoap(lf, tp); } catch (Throwable t) { fail("brain soap", t); }
            try { poppies(lf, tp, pad, R); } catch (Throwable t) { fail("dusk poppy", t); }
            try { crock(lf, tp, pad); } catch (Throwable t) { fail("theriac crock", t); }
            try { murex(lf, tp, pad); } catch (Throwable t) { fail("murex", t); }
            try { porphyry(lf); } catch (Throwable t) { fail("porphyry", t); }
            try { signOff(lf, tp, pad); } catch (Throwable t) { fail("sign-off", t); }
            try { guestClips(lf, tp, pad); } catch (Throwable t) { fail("guest clips", t); }
            try { Stage2Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("stage 2", t); }
            try { Stage3Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("stage 3", t); }
            try { Stage4Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("stage 4", t); }
            try { Stage45Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("stage 4.5", t); }
            try { Stage5Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("stage 5", t); }
            try { Stage131Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("1.3.1", t); }
            try { Stage132Tests.run(srv, ow, lf, tp, pad, R); } catch (Throwable t) { fail("1.3.2", t); }
        }
        TestHarness110.reset(tp);
        if (tp.dimension != startDim) { tp.timeUntilPortal = 0; tp.changeDimension(startDim, new LightfallTeleporter(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5, 0)); }
        tp.setSneaking(false);
        WorldServer w = (WorldServer) tp.world;
        w.getPlayerChunkMap().removePlayer(tp);
        tp.setPosition(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
        w.getPlayerChunkMap().addPlayer(tp);
    }

    // ------------------------------------------------------------ helpers
    public static boolean adv(EntityPlayerMP p, String n) { return TestHarness110.adv(p, n); }
    public static Advancement advancement(MinecraftServer s, String n) { return s.getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, n)); }
    public static void hold(EntityPlayerMP p, ItemStack s) { p.setHeldItem(EnumHand.MAIN_HAND, s); }
    public static int count(EntityPlayerMP p, Item it) { int n = 0; for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == it) n += s.getCount(); return n; }
    public static int dropped(WorldServer w, BlockPos at, Item it, double r) {
        int n = 0;
        for (EntityItem e : w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(r))) if (e.getItem().getItem() == it) n += e.getItem().getCount();
        return n;
    }
    public static void clearDrops(WorldServer w, BlockPos at, double r) { for (EntityItem e : w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(r))) e.setDead(); }
    public static void moveTo(EntityPlayerMP tp, WorldServer w, BlockPos to) {
        if (tp.world != w) { tp.timeUntilPortal = 0; tp.changeDimension(w.provider.getDimension(), new LightfallTeleporter(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0)); }
        w.getPlayerChunkMap().removePlayer(tp);
        tp.setPosition(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        w.getPlayerChunkMap().addPlayer(tp);
    }
    public static int scan(WorldServer w, int R, Block... bs) {
        int n = 0;
        for (int cx = -R + 1; cx < R; cx++) for (int cz = -R + 1; cz < R; cz++) {
            net.minecraft.world.chunk.Chunk ch = w.getChunk(cx, cz);
            for (net.minecraft.world.chunk.storage.ExtendedBlockStorage sec : ch.getBlockStorageArray()) {
                if (sec == null) continue;
                for (int i = 0; i < 4096; i++) { Block b = sec.get(i & 15, (i >> 8) & 15, (i >> 4) & 15).getBlock(); for (Block q : bs) if (b == q) n++; }
            }
        }
        return n;
    }

    /** a star-grass test pad high above the Lightfall (41x41, y=150), air above, with the traveller standing on it */
    static BlockPos pad(WorldServer lf, EntityPlayerMP tp) {
        BlockPos c = new BlockPos(lf.getSpawnPoint().getX() + 400, 150, lf.getSpawnPoint().getZ() + 400);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, -2, -20), c.add(20, -2, 20))) lf.setBlockState(q, ModContent.DUSKSTONE.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, -1, -20), c.add(20, -1, 20))) lf.setBlockState(q, ModContent.STAR_GRASS.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(c.add(-20, 0, -20), c.add(20, 12, 20))) if (!lf.isAirBlock(q)) lf.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        moveTo(tp, lf, c);
        check(tp.world == lf && lf.getBlockState(c.down()).getBlock() == ModContent.STAR_GRASS, "1.3.0 test pad in the Lightfall at " + c);
        return c;
    }

    // ------------------------------------------------------------ registration
    static void registration(MinecraftServer srv, WorldServer lf) {
        String[] items = {"brain_soap", "poppy_tears", "imperial_theriac", "tyrian_purple", "burning_purple", "dusk_poppy", "theriac_crock"};
        int miss = 0; StringBuilder m = new StringBuilder();
        for (String n : items) if (ForgeRegistries.ITEMS.getValue(new ResourceLocation(LuciferTotem.MODID, n)) == null) { miss++; m.append(n).append(' '); }
        check(miss == 0, "stage 1 items/blocks registered " + m);
        check(V130.SOMNUS != null && V130.STINK != null && V130.MANIA != null && V130.KYKEON != null
            && ForgeRegistries.POTIONS.getValue(new ResourceLocation(LuciferTotem.MODID, "somnus")) == V130.SOMNUS, "1.3.0 potions registered (somnus, the_stink, mania, kykeon_vision)");
        check(net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(EntityMurex.class) != null, "murex entity registered");
        String[] advs = {"brain_soaps", "poppy_tears", "somnus_nap", "the_stink", "aged_to_perfection", "galens_patient", "sign_off"};
        int am = 0; for (String a : advs) if (advancement(srv, a) == null) am++;
        check(am == 0, "stage 1 advancements load (" + (advs.length - am) + "/" + advs.length + ")");
        String[] recipes = {"brain_soap", "purple_dye_from_tyrian", "medea_fire_flask_porphyry", "theriac_crock", "porphyry_dye"};
        int rm = 0; for (String r : recipes) if (CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, r)) == null) rm++;
        check(rm == 0, "stage 1 recipes load (" + (recipes.length - rm) + "/" + recipes.length + ")");
        ItemStack sm = FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Apothecary.TYRIAN_PURPLE));
        check(sm.getItem() == Apothecary.BURNING_PURPLE, "smelting Tyrian Purple gives Burning Purple: " + sm);
        int codes = 0; for (String k : Apothecary.CODE.keySet()) if (Item.getByNameOrId(k) != null) codes++;
        check(codes == Apothecary.CODE.size(), "every Galen ingredient resolves to an item (" + codes + "/" + Apothecary.CODE.size() + ")");
        List<Biome.SpawnListEntry> water = ModContent.BIOME.getSpawnableList(EnumCreatureType.WATER_CREATURE);
        boolean has = false; for (Biome.SpawnListEntry e : water) if (e.entityClass == EntityMurex.class) has = true;
        check(has, "Tyrian Murex is in the Lightfall sea spawn list");
    }

    // ------------------------------------------------------------ Brain Soaps
    static void brainSoap(WorldServer lf, EntityPlayerMP tp) {
        TestHarness110.reset(tp);
        tp.addPotionEffect(new PotionEffect(HPotions.INSANIA, 600, 0));
        tp.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 600, 0));
        tp.addPotionEffect(new PotionEffect(V130.SOMNUS, 600, 0));
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 600, 0));
        tp.addPotionEffect(new PotionEffect(MobEffects.SPEED, 600, 0));
        hold(tp, new ItemStack(Apothecary.BRAIN_SOAP, 3));
        Apothecary.BRAIN_SOAP.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
        check(!tp.isPotionActive(HPotions.INSANIA) && !tp.isPotionActive(MobEffects.NAUSEA) && !tp.isPotionActive(V130.SOMNUS) && !tp.isPotionActive(V130.MANIA) && tp.isPotionActive(MobEffects.SPEED),
            "a Brain Soap washes out Insania, Nausea, Somnus and Mania (and leaves Speed alone)");
        check(tp.getHeldItemMainhand().getCount() == 2 && adv(tp, "brain_soaps"), "the soap is used up (2 left) and 'They're on Brain Soaps' is granted");
        LTConfig.v130.brainSoaps = false;
        tp.addPotionEffect(new PotionEffect(HPotions.INSANIA, 600, 0));
        Apothecary.BRAIN_SOAP.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
        check(tp.isPotionActive(HPotions.INSANIA) && tp.getHeldItemMainhand().getCount() == 2, "config brainSoaps=false: the soap does nothing");
        LTConfig.v130.brainSoaps = true;
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ Dusk Poppy, Poppy Tears, Somnus nap
    static void poppies(WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        int n = scan(lf, R, Apothecary.DUSK_POPPY);
        check(n > 5 && V130Gen.poppyPatches > 0, "Dusk Poppies grow on the islands (" + n + " plants, " + V130Gen.poppyPatches + " patches)");
        BlockPos p = pad.add(3, 0, 0);
        lf.setBlockState(p, Apothecary.DUSK_POPPY.getDefaultState());
        ItemStack shears = new ItemStack(Items.SHEARS);
        hold(tp, shears);
        clearDrops(lf, p, 3);
        boolean used = Apothecary.DUSK_POPPY.onBlockActivated(lf, p, lf.getBlockState(p), tp, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        check(used && lf.getBlockState(p).getValue(Apothecary.BlockDuskPoppy.TAPPED) && dropped(lf, p, Apothecary.POPPY_TEARS, 3) == 1 && shears.getItemDamage() == 1,
            "shears bleed a Dusk Poppy: 1 Poppy Tears, poppy tapped, shears worn");
        boolean again = Apothecary.tap(lf, p, tp, shears);
        check(!again && dropped(lf, p, Apothecary.POPPY_TEARS, 3) == 1, "a tapped poppy gives nothing until it heals");
        Apothecary.BlockDuskPoppy bp = (Apothecary.BlockDuskPoppy) Apothecary.DUSK_POPPY;
        java.util.Random rr = new java.util.Random(3);
        for (int i = 0; i < 40 && lf.getBlockState(p).getValue(Apothecary.BlockDuskPoppy.TAPPED); i++) bp.updateTick(lf, p, lf.getBlockState(p), rr);
        check(lf.getBlockState(p).getBlock() == Apothecary.DUSK_POPPY && !lf.getBlockState(p).getValue(Apothecary.BlockDuskPoppy.TAPPED), "a tapped poppy heals on random ticks");
        // drink
        TestHarness110.reset(tp);
        double sp0 = tp.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        ItemStack tears = new ItemStack(Apothecary.POPPY_TEARS);
        hold(tp, tears);
        Apothecary.POPPY_TEARS.onItemUseFinish(tears, lf, tp);
        double sp1 = tp.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        PotionEffect so = tp.getActivePotionEffect(V130.SOMNUS);
        check(so != null && so.getDuration() > LTConfig.v130.somnusSeconds * 20 - 40 && sp1 < sp0 * 0.9 && adv(tp, "poppy_tears"), "Poppy Tears give Somnus (" + (so == null ? 0 : so.getDuration()) + " ticks), slowing " + String.format("%.3f->%.3f", sp0, sp1) + ", advancement granted");
        // the bed: Somnus -> a nap instead of an explosion
        BlockPos bed = pad.add(-4, 0, 4);
        lf.setBlockState(bed, Blocks.BED.getDefaultState().withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT).withProperty(BlockBed.FACING, EnumFacing.NORTH), 2);
        lf.setBlockState(bed.north(), Blocks.BED.getDefaultState().withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD).withProperty(BlockBed.FACING, EnumFacing.NORTH), 2);
        moveTo(tp, lf, bed.east());
        tp.setHealth(10f);
        int naps = Apothecary.naps;
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock ev = net.minecraftforge.common.ForgeHooks.onRightClickBlock(tp, EnumHand.MAIN_HAND, bed, EnumFacing.UP, new Vec3d(0.5, 0.5, 0.5));
        check(ev.isCanceled() && Apothecary.naps == naps + 1 && lf.getBlockState(bed).getBlock() == Blocks.BED && tp.isPotionActive(MobEffects.REGENERATION) && !tp.isPotionActive(V130.SOMNUS) && adv(tp, "somnus_nap"),
            "right-clicking a Lightfall bed under Somnus: dreamless nap (Regeneration, Somnus used up), bed intact, no explosion");
        check(!Apothecary.nap(tp, bed), "without Somnus there is no nap (the bed would explode as always)");
        lf.setBlockToAir(bed.north()); lf.setBlockToAir(bed);
        moveTo(tp, lf, pad);
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ Galen's Theriac Crock
    static String use(WorldServer lf, EntityPlayerMP tp, BlockPos c, ItemStack held, boolean sneak) {
        hold(tp, held); tp.setSneaking(sneak);
        String r = Apothecary.use(lf, c, tp, EnumHand.MAIN_HAND);
        tp.setSneaking(false);
        return r;
    }
    static void crock(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        TestHarness110.reset(tp);
        BlockPos c = pad.add(0, 0, -5);
        lf.setBlockState(c, Apothecary.CROCK.getDefaultState());
        Apothecary.TileCrock te = (Apothecary.TileCrock) lf.getTileEntity(c);
        check(te != null, "the crock has its tile entity");
        if (te == null) return;
        check(use(lf, tp, c, new ItemStack(Blocks.DIRT), false).equals("refused"), "dirt is refused");
        check(use(lf, tp, c, ItemStack.EMPTY, true).equals("missing"), "an empty crock cannot be sealed");
        ItemStack honey = new ItemStack(HItems.HONEY_CAKE, 2);
        check(use(lf, tp, c, honey, false).equals("added") && honey.getCount() == 1, "honey cake goes in (stack shrinks)");
        check(use(lf, tp, c, new ItemStack(HItems.HONEY_CAKE), false).equals("full"), "a second honey cake is refused (one of each herb)");
        for (int i = 0; i < 3; i++) use(lf, tp, c, new ItemStack(HItems.VENOM_VIAL), false);
        check(use(lf, tp, c, ItemStack.EMPTY, true).equals("missing") && "poppy".equals(te.missing()), "honey + 3 vipers still needs Poppy Tears");
        use(lf, tp, c, new ItemStack(Apothecary.POPPY_TEARS), false);
        check(te.venoms() == 3 && te.potency() == 3, "crock holds 3 vipers, 3 distinct ingredients");
        check(use(lf, tp, c, ItemStack.EMPTY, true).equals("sealed") && te.tier == Apothecary.THERIAC && te.sealed, "sneak + empty hand seals it: tier Theriac");
        check(use(lf, tp, c, new ItemStack(HItems.HENBANE_LEAVES), false).equals("aging"), "a sealed crock takes nothing and reports its days");
        te.sealedAt -= 3L * LTConfig.v130.crockDayTicks;
        int before = count(tp, ModContent.THERIAC);
        check(use(lf, tp, c, ItemStack.EMPTY, false).equals("collected") && count(tp, ModContent.THERIAC) == before + 3 && te.contents.isEmpty() && !te.sealed && adv(tp, "aged_to_perfection"),
            "after 3 days the crock gives 3 Theriac and is empty again (Aged to Perfection)");
        // imperial: ten distinct
        Item[] ten = {HItems.HONEY_CAKE, HItems.VENOM_VIAL, HItems.VENOM_VIAL, HItems.VENOM_VIAL, Apothecary.POPPY_TEARS, HItems.HENBANE_LEAVES, HItems.MANDRAKE_ROOT,
            Apothecary.TYRIAN_PURPLE, HItems.STAR_FOAM, HItems.AGED_VINEGAR, Items.SPIDER_EYE, Items.GOLDEN_CARROT};
        for (Item it : ten) use(lf, tp, c, new ItemStack(it), false);
        check(te.potency() == 10, "ten distinct ingredients in (" + te.potency() + ")");
        use(lf, tp, c, ItemStack.EMPTY, true);
        check(te.tier == Apothecary.IMPERIAL, "ten ingredients seal as Imperial Theriac (tier " + te.tier + ")");
        te.sealedAt -= 9L * LTConfig.v130.crockDayTicks;
        check(use(lf, tp, c, ItemStack.EMPTY, false).equals("aging"), "Imperial is not ready after 9 days");
        te.sealedAt -= 1L * LTConfig.v130.crockDayTicks;
        check(use(lf, tp, c, ItemStack.EMPTY, false).equals("collected") && count(tp, Apothecary.IMPERIAL_THERIAC) == 1 && count(tp, HItems.GREAT_THERIAC) == 1, "after 10 days: Imperial Theriac + a Great Theriac");
        // persistence of the tile
        use(lf, tp, c, new ItemStack(HItems.HONEY_CAKE), false); use(lf, tp, c, new ItemStack(HItems.VENOM_VIAL), false);
        net.minecraft.nbt.NBTTagCompound t = te.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        Apothecary.TileCrock te2 = new Apothecary.TileCrock(); te2.readFromNBT(t);
        check(te2.contents.equals(te.contents) && te2.sealed == te.sealed, "crock contents survive NBT " + te2.contents);
        clearDrops(lf, c, 4);
        lf.destroyBlock(c, false);
        check(dropped(lf, c, HItems.HONEY_CAKE, 4) == 1 && dropped(lf, c, HItems.VENOM_VIAL, 4) == 1, "breaking an unsealed crock spills its herbs back out");
        // drinking Imperial Theriac twice: the habit
        TestHarness110.reset(tp);
        HData.get(tp).setInteger("theriacTolerance", 0); HData.get(tp).setLong("theriacDay", 0);
        tp.addPotionEffect(new PotionEffect(MobEffects.POISON, 600, 0));
        ItemStack imp = new ItemStack(Apothecary.IMPERIAL_THERIAC);
        Apothecary.IMPERIAL_THERIAC.onItemUseFinish(imp, lf, tp);
        PotionEffect p1 = tp.getActivePotionEffect(ModContent.PURPLE);
        check(p1 != null && p1.getAmplifier() == 2 && !tp.isPotionActive(MobEffects.POISON) && tp.isPotionActive(MobEffects.REGENERATION) && HData.get(tp).getInteger("theriacTolerance") == 1 && adv(tp, "galens_patient"),
            "Imperial Theriac: PURPLE III " + (p1 == null ? 0 : p1.getDuration()) + "t, poison purged, Regeneration, habit 1, Galen's Patient");
        tp.removePotionEffect(ModContent.PURPLE);
        for (int i = 0; i < 4; i++) Apothecary.drinkImperial(tp);
        PotionEffect p5 = tp.getActivePotionEffect(ModContent.PURPLE);
        check(p5 != null && p1 != null && HData.get(tp).getInteger("theriacTolerance") == 5, "five doses: habit 5 (theriacTolerance)");
        tp.removePotionEffect(ModContent.PURPLE);
        Apothecary.drinkImperial(tp);
        PotionEffect p6 = tp.getActivePotionEffect(ModContent.PURPLE);
        check(p6 != null && p1 != null && p6.getDuration() < p1.getDuration() * 0.6, "the habit shortens the Purple: " + (p6 == null ? 0 : p6.getDuration()) + " < 60% of " + p1.getDuration());
        HData.get(tp).setInteger("theriacTolerance", 0);
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ Tyrian Murex
    static void murex(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        TestHarness110.reset(tp);
        // a glass tank on the pad
        BlockPos tank = pad.add(8, 0, 8);
        for (BlockPos q : BlockPos.getAllInBox(tank.add(-2, -1, -2), tank.add(2, 3, 2))) lf.setBlockState(q, Blocks.GLASS.getDefaultState(), 2);
        for (BlockPos q : BlockPos.getAllInBox(tank.add(-1, 0, -1), tank.add(1, 3, 1))) lf.setBlockState(q, Blocks.WATER.getDefaultState(), 2);
        EntityMurex m = new EntityMurex(lf);
        m.setPosition(tank.getX() + 0.5, tank.getY() + 2.5, tank.getZ() + 0.5);
        lf.spawnEntity(m);
        for (int i = 0; i < 60; i++) m.onUpdate();
        check(m.posY < tank.getY() + 1.2 && m.isInWater(), "a murex sinks to the floor of the water (y " + String.format("%.2f", m.posY - tank.getY()) + ")");
        // spawn rule: only deep water
        EntityMurex probe = new EntityMurex(lf);
        probe.setPosition(tank.getX() + 0.5, tank.getY() + 0.5, tank.getZ() + 0.5);
        check(!probe.getCanSpawnHere(), "no murex spawns above the deep sea (y " + tank.getY() + ")");
        // crushing ten
        HData.get(tp).setIntArray("murexTimes", new int[0]);
        clearDrops(lf, tank, 6);
        int stinks = Apothecary.stinks;
        for (int i = 0; i < 10; i++) {
            EntityMurex k = new EntityMurex(lf);
            k.setPosition(tank.getX() + 0.5, tank.getY() + 0.5, tank.getZ() + 0.5);
            lf.spawnEntity(k);
            k.attackEntityFrom(DamageSource.causePlayerDamage(tp), 100f);
            if (i == 8) check(!tp.isPotionActive(V130.STINK), "nine crushed murex: no Stink yet");
        }
        int purple = dropped(lf, tank, Apothecary.TYRIAN_PURPLE, 6);
        check(purple >= 10 && purple <= 30, "ten crushed murex drop Tyrian Purple (" + purple + ")");
        check(Apothecary.stinks == stinks + 1 && tp.isPotionActive(V130.STINK) && tp.isPotionActive(MobEffects.NAUSEA) && adv(tp, "the_stink"), "the tenth within a minute: The Stink (nausea + stink), advancement granted");
        EntityCow cow = new EntityCow(lf);
        cow.setPosition(tp.posX + 3, tp.posY, tp.posZ);
        lf.spawnEntity(cow);
        for (int i = 0; i < 10; i++) cow.onUpdate();
        int fled = Apothecary.stinkScatter(tp);
        check(fled >= 1 && cow.getNavigator().getPath() != null, "animals flee from The Stink (" + fled + " fled, cow has a path)");
        cow.setDead(); m.setDead();
        // the real sea
        int ok = 0, tried = 0;
        for (int dx = -40; dx <= 40 && tried < 400; dx += 4) for (int dz = -40; dz <= 40 && tried < 400; dz += 4) {
            BlockPos q = new BlockPos(lf.getSpawnPoint().getX() + dx, LightfallChunkGenerator.SEA_LEVEL - 6, lf.getSpawnPoint().getZ() + dz);
            if (lf.getBlockState(q).getMaterial() != net.minecraft.block.material.Material.WATER) continue;
            tried++;
            EntityMurex s = new EntityMurex(lf); s.setPosition(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
            if (s.getCanSpawnHere()) ok++;
        }
        check(tried == 0 || ok > 0, "murex may spawn in the deep Lightfall sea (" + ok + "/" + tried + " deep water spots accepted)");
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ Porphyry leather dye
    static void porphyry(WorldServer lf) {
        InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        inv.setInventorySlotContents(0, new ItemStack(Items.LEATHER_CHESTPLATE)); inv.setInventorySlotContents(4, new ItemStack(Apothecary.TYRIAN_PURPLE));
        ItemStack out = CraftingManager.findMatchingResult(inv, lf);
        check(out.getItem() == Items.LEATHER_CHESTPLATE && ((ItemArmor) out.getItem()).getColor(out) == Apothecary.PORPHYRY, "leather + Tyrian Purple -> Porphyry-dyed leather: " + out.getDisplayName());
        inv.setInventorySlotContents(5, new ItemStack(Apothecary.TYRIAN_PURPLE));
        check(CraftingManager.findMatchingResult(inv, lf).isEmpty(), "two purples is not the porphyry recipe");
        InventoryCrafting s = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        s.setInventorySlotContents(0, new ItemStack(Apothecary.TYRIAN_PURPLE)); s.setInventorySlotContents(1, new ItemStack(Items.CLAY_BALL)); s.setInventorySlotContents(2, new ItemStack(Items.SUGAR));
        ItemStack soap = CraftingManager.findMatchingResult(s, lf);
        check(soap.getItem() == Apothecary.BRAIN_SOAP && soap.getCount() == 2, "Tyrian Purple + clay + sugar -> 2 Brain Soaps");
    }

    // ------------------------------------------------------------ Hail Satan sign-off
    static void signOff(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        EntityAmmonius a = new EntityAmmonius(lf);
        a.setPosition(pad.getX() + 2.5, pad.getY(), pad.getZ() + 0.5);
        lf.spawnEntity(a);
        int so = EntityAmmonius.signOffs;
        int old = LTConfig.v130.signOffOneIn;
        LTConfig.v130.signOffOneIn = 0;
        for (int i = 0; i < 5; i++) a.rant();
        check(EntityAmmonius.signOffs == so, "signOffOneIn=0: no sign-off");
        LTConfig.v130.signOffOneIn = 1;
        a.rant();
        LTConfig.v130.signOffOneIn = old;
        check(EntityAmmonius.signOffs == so + 1 && adv(tp, "sign_off"), "a rant can end with the sign-off; everyone in earshot gets '...And As Always'");
        LoreEvents.forceSignOff = true;
        LoreEvents.whisper(tp);
        check("whisper.lucifertotem.signoff".equals(LoreEvents.lastWhisperKey), "a totem can whisper the sign-off (" + LoreEvents.lastWhisperKey + ")");
        LoreEvents.whisper(tp);
        check(!"whisper.lucifertotem.signoff".equals(LoreEvents.lastWhisperKey), "the sign-off whisper is rare: the harness never rolls it unforced (" + LoreEvents.lastWhisperKey + ")");
        a.setDead();
    }

    // ------------------------------------------------------------ the 2026-10-21 guest-podcast clips
    static final String[] NEW_CLIPS = {"kitchen_horse_whip", "are_you_the_god_pan", "forget_those_sumerians", "gilgamesh_bonehead", "third_grader", "bar_bar_bar", "poopy_languages",
        "devil_doesnt_let_me", "dont_buy_my_book", "freeze_you_in_your_tracks", "where_can_i_join", "smoking_the_scorpion", "black_death_are_you_ready", "get_you_psychotic",
        "victorian_bastards", "never_seen_so_many_drugs", "medea_built_euphrates", "communion_meaningless", "dark_star_lucifera", "mr_rogers_greek", "medea_unheralded_genius",
        "christ_is_a_drug_term", "ios_arrow_poison", "galen_fifty_to_sixty_drugs", "athenian_assembly_cannabis", "medea_root_for_medicine", "galene_calm_of_the_storm",
        "medea_napalm", "opium_dosage_upwards", "julian_destroy_education", "museum_alexandria", "vatican_hill_sibyl", "castoreum_beaver", "greed_stains_your_soul", "slaves_and_priests"};
    static boolean in(int[] pool, String n) { int i = lucifertotem.hillman.AmmoniusClips.index(n); for (int k : pool) if (k == i) return true; return false; }
    static void guestClips(WorldServer lf, EntityPlayerMP tp, BlockPos pad) throws Exception {
        int reg = 0, pooled = 0;
        for (String n : NEW_CLIPS) {
            int i = lucifertotem.hillman.AmmoniusClips.index(n);
            if (i >= 0 && ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(LuciferTotem.MODID, "ammonius.line." + n)) && lucifertotem.hillman.HSounds.LINES[i] != null) reg++;
            if (in(lucifertotem.hillman.AmmoniusClips.TALK, n) || in(lucifertotem.hillman.AmmoniusClips.GREET, n) || in(lucifertotem.hillman.AmmoniusClips.RANT, n)
                || in(lucifertotem.hillman.AmmoniusClips.LECTURE, n) || in(lucifertotem.hillman.AmmoniusClips.HURT, n) || in(lucifertotem.hillman.AmmoniusClips.DEATH, n)) pooled++;
        }
        check(reg == 35 && pooled == 35, "all 35 guest clips have their own sound event (" + reg + ") and feed a pool (" + pooled + ")");
        check(lucifertotem.hillman.AmmoniusClips.LECTURE.length == 15 && lucifertotem.hillman.AmmoniusClips.RANT.length == 7 && in(lucifertotem.hillman.AmmoniusClips.HURT, "forget_those_sumerians")
            && in(lucifertotem.hillman.AmmoniusClips.DEATH, "devil_doesnt_let_me") && in(lucifertotem.hillman.AmmoniusClips.GREET, "dont_buy_my_book") && in(lucifertotem.hillman.AmmoniusClips.TALK, "bar_bar_bar"),
            "pools: 15 lecture (B), 7 rant, hurt/death/greet/idle got their A clips");
        int maxT = 0; for (String n : NEW_CLIPS) maxT = Math.max(maxT, lucifertotem.hillman.AmmoniusClips.TICKS[lucifertotem.hillman.AmmoniusClips.index(n)]);
        check(maxT > 100 && maxT < 200, "guest clip lengths are known (longest " + maxT + " ticks) so he never talks over himself");
        String sj = new String(com.google.common.io.ByteStreams.toByteArray(LuciferTotem.class.getResourceAsStream("/assets/lucifertotem/sounds.json")), "UTF-8");
        com.google.gson.JsonObject o = new com.google.gson.JsonParser().parse(sj).getAsJsonObject();
        String amb = o.getAsJsonObject("ambient.distant_ammonius").getAsJsonArray("sounds").toString();
        check(amb.contains("distant_dark_star_lucifera") && amb.contains("distant_forget_those_sumerians") && amb.contains("distant_gilgamesh_bonehead"), "the 3 new distant variants are in the distant-voice pool (Lightfall + Overworld)");
        int subs = 0;
        for (String n : NEW_CLIPS) { com.google.gson.JsonObject e = o.getAsJsonObject("ammonius.line." + n); if (e != null && e.get("subtitle").getAsString().equals("subtitles.lucifertotem.ammonius.line." + n)) subs++; }
        check(subs == 35, "every guest clip has its own subtitle (" + subs + "/35)");
        boolean ships = true;
        for (String n : NEW_CLIPS) if (LuciferTotem.class.getResource("/assets/lucifertotem/sounds/ammonius/" + n + ".ogg") == null) ships = false;
        check(ships, "all 35 guest oggs ship in the jar");
        EntityAmmonius a = new EntityAmmonius(lf);
        a.setPosition(pad.getX() - 2.5, pad.getY(), pad.getZ() + 0.5);
        lf.spawnEntity(a);
        int l0 = EntityAmmonius.lectures;
        int c = a.lecture();
        check(EntityAmmonius.lectures == l0 + 1 && in(lucifertotem.hillman.AmmoniusClips.LECTURE, lucifertotem.hillman.AmmoniusClips.NAME[c]), "a lecture plays a B clip (" + lucifertotem.hillman.AmmoniusClips.NAME[c] + ")");
        EntityAmmonius.forceLecture = true;
        for (int i = 0; i < 1500 && EntityAmmonius.forceLecture; i++) a.onUpdate();
        check(!EntityAmmonius.forceLecture && EntityAmmonius.lectures == l0 + 2, "his idle chatter turns into a lecture when the dice say so (" + (EntityAmmonius.lectures - l0) + " lectures)");
        a.setDead();
    }
}
