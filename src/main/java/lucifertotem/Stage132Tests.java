package lucifertotem;

import java.util.List;
import java.util.function.Function;

import lucifertotem.hillman.EntityDipsas;
import lucifertotem.hillman.HData;
import lucifertotem.v130.EntityBacchant;
import lucifertotem.v130.EntityHekateHound;
import lucifertotem.v130.EntityHuntress;
import lucifertotem.v130.EntityPeerReviewer;
import lucifertotem.v130.Gen130;
import lucifertotem.v130.Stage3;
import lucifertotem.v130.V130Net;
import lucifertotem.v132.Crown;
import lucifertotem.v132.EntityDionysus;
import lucifertotem.v132.V132;
import lucifertotem.v132.Xoanon;
import lucifertotem.v132.Tall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** 1.3.2: Dionysus (mask, xoanon, wine) and the Crown of Thorns */
public class Stage132Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.2] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("registration", () -> registration(srv));
        guard("mask", () -> mask(lf, tp, pad));
        guard("icon + statue", () -> iconStatue(lf, tp, pad));
        guard("library icon", () -> libraryIcon(lf));
        guard("xoanon", () -> xoanon(lf, tp, pad));
        guard("fountain mask", () -> fountain(lf));
        guard("crown", () -> crown(ow, lf, tp, pad));
    }

    static void registration(MinecraftServer srv) {
        check(V132.ICON != null && V132.STATUE != null && net.minecraft.item.Item.getItemFromBlock(V132.STATUE) != net.minecraft.init.Items.AIR, "Dionysus icon (wall ornament) and Dionysus Statue (with item) registered");
        check(V132.CROWN != null && V132.MASK != null && net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(EntityDionysus.class) != null, "Crown of Thorns, Dionysus mask and Dionysus registered");
        check(V132.CROWN instanceof net.minecraft.item.ItemArmor && ((net.minecraft.item.ItemArmor) V132.CROWN).armorType == EntityEquipmentSlot.HEAD, "the Crown of Thorns goes in the helmet slot");
        int a = 0; for (String n : new String[]{"xoanon", "it_is_finished", "crown_of_thorns"}) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == 3, "1.3.2 advancements load (" + a + "/3)");
        check(CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, "dionysus_mask")) != null, "Dionysus mask recipe loads");
        check(CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, "crown_of_thorns")) != null, "Crown of Thorns recipe loads (config crownRecipe=true, custom condition)");
        check(LTConfig.v132.dionysus && LTConfig.v132.crownOfThorns && LTConfig.v132.maskInFountains && LTConfig.v132.wineMiracle, "1.3.2 features default on");
        for (String r : new String[]{"textures/models/armor/crown_of_thorns.png", "textures/items/crown_of_thorns.png", "textures/entity/dionysus.png", "textures/blocks/dionysus_mask.png", "textures/blocks/dionysus_icon_lower.png", "textures/blocks/dionysus_icon_upper.png", "textures/blocks/dionysus_statue.png", "blockstates/dionysus_icon.json", "blockstates/dionysus_statue.json", "models/block/dionysus_statue_lower.json", "models/block/dionysus_statue_upper.json", "models/block/dionysus_icon_lower.json", "models/block/dionysus_icon_upper.json", "models/item/dionysus_statue.json", "models/block/dionysus_mask.json", "models/item/dionysus_mask.json", "models/item/crown_of_thorns.json", "blockstates/dionysus_mask.json"})
            check(Stage132Tests.class.getResource("/assets/lucifertotem/" + r) != null, "asset " + r);
    }

    static IBlockState mask(EnumFacing f, boolean wild) { return V132.MASK.getDefaultState().withProperty(V132.BlockMask.FACING, f).withProperty(V132.BlockMask.WILD, wild); }

    static void mask(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        BlockPos wall = pad.add(-12, 0, -12);
        lf.setBlockState(wall, Blocks.STONEBRICK.getDefaultState(), 3);
        BlockPos at = wall.south();
        lf.setBlockState(at, mask(EnumFacing.SOUTH, false), 3);
        check(lf.getBlockState(at).getBlock() == V132.MASK && V132.MASK.getMetaFromState(lf.getBlockState(at)) == EnumFacing.SOUTH.getHorizontalIndex(), "the mask hangs on a wall, facing out");
        IBlockState back = V132.MASK.getStateFromMeta(V132.MASK.getMetaFromState(mask(EnumFacing.WEST, true)));
        check(back.getValue(V132.BlockMask.FACING) == EnumFacing.WEST && back.getValue(V132.BlockMask.WILD), "facing + wild survive meta round-trip");
        NonNullList<ItemStack> d = NonNullList.create();
        V132.MASK.getDrops(d, lf, at, mask(EnumFacing.SOUTH, false), 0);
        check(d.size() == 1 && d.get(0).getItem() == net.minecraft.item.Item.getItemFromBlock(V132.MASK), "a hung (placed) mask drops itself, and only itself: " + d);
        int keep = LTConfig.v132.crownFromMaskOneIn;
        LTConfig.v132.crownFromMaskOneIn = 1;
        d = NonNullList.create(); V132.MASK.getDrops(d, lf, at, mask(EnumFacing.SOUTH, true), 0);
        boolean crown = false, self = false; for (ItemStack s : d) { if (s.getItem() == V132.CROWN) crown = true; if (s.getItem() == net.minecraft.item.Item.getItemFromBlock(V132.MASK)) self = true; }
        check(self && crown, "a wild (fountain) mask drops itself and, at 1-in-1, a Crown of Thorns: " + d);
        LTConfig.v132.crownFromMaskOneIn = 0;
        d = NonNullList.create(); V132.MASK.getDrops(d, lf, at, mask(EnumFacing.SOUTH, true), 0);
        check(d.size() == 1, "crownFromMaskOneIn=0: no crown");
        LTConfig.v132.crownFromMaskOneIn = keep;
        TestHarness130.clearDrops(lf, at, 3);
    }

    static void T(WorldServer lf, BlockPos top, boolean arms, boolean lower) {
        lf.setBlockState(top.down(), Blocks.LOG.getDefaultState(), 3);
        if (lower) lf.setBlockState(top.down(2), Blocks.LOG.getDefaultState(), 3);
        if (arms) { lf.setBlockState(top.down().east(), Blocks.LOG.getDefaultState(), 3); lf.setBlockState(top.down().west(), Blocks.LOG.getDefaultState(), 3); }
    }

    static EntityDionysus place(WorldServer lf, EntityPlayerMP tp, BlockPos at) {
        ItemStack st = new ItemStack(V132.STATUE);
        TestHarness130.hold(tp, st);
        tp.rotationYaw = 0f;
        ((net.minecraft.item.ItemBlock) st.getItem()).onItemUse(tp, lf, at.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1f, 0.5f);
        TestHarness130.hold(tp, ItemStack.EMPTY);
        List<EntityDionysus> l = lf.getEntitiesWithinAABB(EntityDionysus.class, new AxisAlignedBB(at).grow(3));
        return l.isEmpty() ? null : l.get(0);
    }

    static void xoanon(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        // wrong shapes first: a bare pole, and a T without its lower log
        BlockPos a = pad.add(10, 2, -10);
        T(lf, a, false, true);
        check(place(lf, tp, a) == null && lf.getBlockState(a).getBlock() == V132.STATUE && lf.getBlockState(a.up()).getBlock() == V132.STATUE, "statue on a two-log pole: no Dionysus (wrong shape), the statue just stands there (both halves)");
        BlockPos b = pad.add(14, 2, -10);
        T(lf, b, true, false);
        check(place(lf, tp, b) == null, "statue on arms without the lower log: no Dionysus");
        for (BlockPos q : BlockPos.getAllInBox(a.add(-2, -3, -2), b.add(2, 1, 2))) lf.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
        // the real thing
        BlockPos c = pad.add(10, 2, -14);
        T(lf, c, true, true);
        int before = Xoanon.raised;
        EntityDionysus d = place(lf, tp, c);
        check(d != null && Xoanon.raised == before + 1, "four logs in a T + the statue on top: Dionysus comes to life");
        if (d == null) return;
        check(lf.isAirBlock(c.up()) && lf.isAirBlock(c) && lf.isAirBlock(c.down()) && lf.isAirBlock(c.down(2)) && lf.isAirBlock(c.down().east()) && lf.isAirBlock(c.down().west()), "the logs and the statue are used up");
        check(d.isTamed() && d.isOwner(tp), "he belongs to whoever set the statue");
        check(TestHarness130.adv(tp, "xoanon"), "advancement: The God Who Arrives");
        check(HData.get(tp).getBoolean("codex132_dionysus") && hasCodex(tp, "dionysus"), "codex entry (written book) for Dionysus handed out");
        // sit / follow
        TestHarness130.hold(tp, ItemStack.EMPTY);
        d.processInteract(tp, EnumHand.MAIN_HAND);
        boolean sat = d.isSitting();
        d.processInteract(tp, EnumHand.MAIN_HAND);
        check(sat && !d.isSitting(), "empty hand: he sits, and again: he follows");
        // never a player
        d.setAttackTarget(tp);
        check(d.getAttackTarget() == null && !d.shouldAttackEntity(tp, tp), "he will not target a player");
        EntityZombie z = new EntityZombie(lf); z.setPosition(d.posX + 2, d.posY, d.posZ); lf.spawnEntity(z);
        d.setAttackTarget(z);
        check(d.getAttackTarget() == z && d.shouldAttackEntity(z, tp), "he takes on hostiles");
        float hp = z.getHealth(); d.attackEntityAsMob(z);
        check(z.getHealth() < hp, "his blow lands (" + hp + " -> " + z.getHealth() + ")");
        z.setDead(); d.setAttackTarget(null);
        // water into wine
        ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
        TestHarness130.hold(tp, water);
        int c0 = EntityDionysus.conversions;
        d.processInteract(tp, EnumHand.MAIN_HAND);
        check(EntityDionysus.conversions == c0 + 1 && (tp.getHeldItemMainhand().getItem() == Stage3.BACCHIC_WINE || TestHarness130.count(tp, Stage3.BACCHIC_WINE) > 0), "water bottle -> Bacchic Wine");
        check(TestHarness130.adv(tp, "it_is_finished"), "advancement: It Is Finished... Fermenting");
        // a stranger cannot
        TestHarness130.hold(tp, ItemStack.EMPTY);
        d.setDead();
        check(EntityDionysus.class.getSuperclass() == net.minecraft.entity.passive.EntityTameable.class, "Dionysus is a tameable follower (sit/follow AI)");
    }

    static void iconStatue(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        BlockPos wall = pad.add(-14, 0, 12);
        for (int y = 0; y < 3; y++) lf.setBlockState(wall.up(y), Blocks.STONEBRICK.getDefaultState(), 2);
        BlockPos at = wall.north();
        V132.ICON.placeBoth(lf, at, EnumFacing.NORTH, 3);
        check(lf.getBlockState(at).getBlock() == V132.ICON && lf.getBlockState(at.up()).getBlock() == V132.ICON && lf.getBlockState(at.up()).getValue(Tall.UPPER), "the icon is a life-sized two-block figure on the wall");
        NonNullList<ItemStack> d = NonNullList.create(); V132.ICON.getDrops(d, lf, at.up(), lf.getBlockState(at.up()), 0);
        check(d.isEmpty(), "the upper half carries no drop of its own");
        lf.destroyBlock(at, true);
        check(lf.isAirBlock(at) && lf.isAirBlock(at.up()), "break it: the whole figure comes away");
        check(TestHarness130.dropped(lf, at, net.minecraft.item.Item.getItemFromBlock(V132.STATUE), 2) == 1, "...and drops exactly one Dionysus Statue");
        TestHarness130.clearDrops(lf, at, 3);
        // the upper half, broken by a player, still yields the statue (via the lower half)
        GameType mode0 = tp.interactionManager.getGameType();
        at = at.east(5);   // (items marked dead are still listed until the end of the tick: use a fresh spot)
        V132.ICON.placeBoth(lf, at, EnumFacing.NORTH, 3);
        tp.setGameType(GameType.SURVIVAL);
        V132.ICON.onBlockHarvested(lf, at.up(), lf.getBlockState(at.up()), tp);
        lf.setBlockState(at.up(), Blocks.AIR.getDefaultState(), 3);
        check(lf.isAirBlock(at) && TestHarness130.dropped(lf, at, net.minecraft.item.Item.getItemFromBlock(V132.STATUE), 2) == 1, "break the head end: same, one statue (lower now " + lf.getBlockState(at) + ", dropped " + TestHarness130.dropped(lf, at, net.minecraft.item.Item.getItemFromBlock(V132.STATUE), 2) + ", creative " + tp.capabilities.isCreativeMode + ")");
        TestHarness130.clearDrops(lf, at, 3);
        tp.setGameType(mode0);
        for (int y = 0; y < 3; y++) lf.setBlockState(wall.up(y), Blocks.AIR.getDefaultState(), 2);
        // the statue stands on the floor, both halves
        BlockPos st = pad.add(-14, 0, 6);
        ItemStack s = new ItemStack(V132.STATUE);
        TestHarness130.hold(tp, s);
        ((net.minecraft.item.ItemBlock) s.getItem()).onItemUse(tp, lf, st.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1f, 0.5f);
        check(lf.getBlockState(st).getBlock() == V132.STATUE && lf.getBlockState(st.up()).getBlock() == V132.STATUE, "the statue item places a 2-tall statue");
        lf.destroyBlock(st.up(), false);
        lf.setBlockState(st, Blocks.AIR.getDefaultState(), 3);
        TestHarness130.hold(tp, ItemStack.EMPTY);
        check(net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(new ResourceLocation(LuciferTotem.MODID, "dionysus_icon")) == null, "the wall icon itself has no item: you take it down as a statue");
    }

    static void libraryIcon(WorldServer lf) {
        BlockPos lib = lucifertotem.world.LibraryGen.nearest(lf.getSeed(), new BlockPos(0, 64, 0), 8000);
        check(lib != null, "a Library is predicted");
        if (lib == null) return;
        BlockPos p = lucifertotem.world.LibraryGen.iconPos(lib);
        lf.getChunk(p);
        IBlockState lo = lf.getBlockState(p), up = lf.getBlockState(p.up());
        check(lo.getBlock() == V132.ICON && up.getBlock() == V132.ICON && lo.getValue(Tall.FACING) == EnumFacing.NORTH && lf.getBlockState(p.south()).isFullCube() && lf.isAirBlock(p.down()),
            "the purple-splashed Dionysus hangs over the Library door, on the inner wall, looking into the hall (" + lo + " at " + p + "; hung by worldgen this run: " + Tall.hung + ")");
    }

    static boolean hasCodex(EntityPlayerMP tp, String kind) {
        for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == Items.WRITTEN_BOOK && s.hasTagCompound() && kind.equals(s.getTagCompound().getString(V132.CODEX_TAG))) return true;
        return false;
    }

    static void fountain(WorldServer lf) {
        // build a fountain far from everything and find its wild mask
        int bx = lf.getSpawnPoint().getX() + 700, bz = lf.getSpawnPoint().getZ() + 400;
        Stage3Tests.plateau(lf, bx, bz, 200);
        boolean ok = Gen130.build(lf, "bacchic_fountain", bx, bz, new java.util.Random(132));
        check(ok, "a Bacchic fountain builds");
        if (!ok) return;
        int masks = 0, wild = 0;
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(bx, 0, bz), new BlockPos(bx + 15, 255, bz + 15))) {
            IBlockState s = lf.getBlockState(q);
            if (s.getBlock() == V132.MASK) { masks++; if (s.getValue(V132.BlockMask.WILD)) wild++; }
        }
        check(masks == 1 && wild == 1, "the fountain's pergola carries one wild Dionysus mask (" + masks + "/" + wild + ")");
    }

    static void crown(WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        GameType mode = tp.interactionManager.getGameType();
        tp.setGameType(GameType.SURVIVAL);
        ItemStack oldHead = tp.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        try {
            TestHarness130.moveTo(tp, lf, pad);
            tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(V132.CROWN));
            check(Crown.active(tp), "crowned in the Lightfall: the crown is active");
            BlockPos at = pad.add(4, 0, 4);
            String[] names = {"Bacchant", "Huntress", "Hekate hound", "Peer Reviewer", "land medusa", "zombie"};
            List<Function<World, EntityLiving>> mk = java.util.Arrays.asList(EntityBacchant::new, EntityHuntress::new, EntityHekateHound::new, EntityPeerReviewer::new, EntityLandMedusa::new, EntityZombie::new);
            java.util.List<EntityLiving> mobs = new java.util.ArrayList<>();
            for (int i = 0; i < mk.size(); i++) {
                EntityLiving m = mk.get(i).apply(lf);
                m.setPosition(at.getX() + i, at.getY(), at.getZ()); lf.spawnEntity(m); mobs.add(m);
                m.setAttackTarget(tp);
                check(m.getAttackTarget() == null, names[i] + " will not take a crowned player as its target");
                int c0 = Crown.cancelledHits;
                boolean lands = net.minecraftforge.common.ForgeHooks.onPlayerAttack(tp, DamageSource.causeMobDamage(m), 2f);
                check(!lands && Crown.cancelledHits == c0 + 1, names[i] + "'s unprovoked blow does nothing (lands=" + lands + ", cancelled " + (Crown.cancelledHits - c0) + ", spares=" + Crown.spares(m, tp) + ")");
            }
            // retaliation: strike the zombie; it (and only it) may answer
            EntityLiving z = mobs.get(5), b = mobs.get(0);
            z.hurtResistantTime = 0;
            z.attackEntityFrom(DamageSource.causePlayerDamage(tp), 1f);
            z.setAttackTarget(tp);
            check(Crown.provoked(z, tp) && z.getAttackTarget() == tp, "strike first: that mob fights back");
            check(net.minecraftforge.common.ForgeHooks.onPlayerAttack(tp, DamageSource.causeMobDamage(z), 2f), "...and its blows land");
            check(!net.minecraftforge.common.ForgeHooks.onPlayerAttack(tp, DamageSource.causeArrowDamage(new net.minecraft.entity.projectile.EntityTippedArrow(lf), mobs.get(1)), 2f), "an unprovoked Huntress' arrow does nothing either");
            b.setAttackTarget(tp);
            check(b.getAttackTarget() == null, "the rest stay peaceful (the Bacchant you did not hit)");
            // the Dipsas does not care
            EntityDipsas dip = new EntityDipsas(lf); dip.setPosition(at.getX(), at.getY(), at.getZ() + 3); lf.spawnEntity(dip);
            dip.setAttackTarget(tp);
            check(dip.getAttackTarget() == tp, "the Dipsas still hunts a crowned player");
            tp.removePotionEffect(ModContent.PURPLE);
            EntityDipsas.bite(tp);
            check(tp.isPotionActive(ModContent.PURPLE), "a Dipsas bite on a crowned head in the Lightfall gives PURPLE");
            tp.removePotionEffect(ModContent.PURPLE);
            // the advancement + codex
            Crown.Events ev = new Crown.Events();
            tp.ticksExisted = 40; ev.tick(new net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, tp));
            check(TestHarness130.adv(tp, "crown_of_thorns") && hasCodex(tp, "crown"), "advancement A Crown of Thorns + codex entry");
            // without the crown the Lightfall is as it was
            tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
            b.setAttackTarget(tp);
            check(!Crown.active(tp) && b.getAttackTarget() == tp, "crown off: the Bacchant may target you again");
            b.setAttackTarget(null);
            // cosmetic: sneak + use
            ItemStack cs = new ItemStack(V132.CROWN);
            V132.wearCosmetic(tp, cs);
            check(cs.isEmpty() && (V130Net.flags(tp) & V130Net.CROWN) != 0 && Crown.active(tp), "sneak + use: worn as a cosmetic (synced flag), and it works");
            b.setAttackTarget(tp);
            check(b.getAttackTarget() == null, "cosmetic crown pacifies too");
            check(V132.takeOffCosmetic(tp) && (V130Net.flags(tp) & V130Net.CROWN) == 0 && TestHarness130.count(tp, V132.CROWN) >= 1, "sneak + right-click: the cosmetic crown comes back off");
            // only in the Lightfall
            tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(V132.CROWN));
            for (EntityLiving m : mobs) m.setDead(); dip.setDead();
            TestHarness130.moveTo(tp, ow, ow.getTopSolidOrLiquidBlock(ow.getSpawnPoint()).up());
            EntityZombie oz = new EntityZombie(ow); oz.setPosition(tp.posX + 3, tp.posY, tp.posZ); ow.spawnEntity(oz);
            oz.setAttackTarget(tp);
            check(!Crown.active(tp) && oz.getAttackTarget() == tp, "in the Overworld the crown does nothing (zombie still targets)");
            oz.setDead();
            TestHarness130.moveTo(tp, lf, pad);
            check(Crown.exempt(new EntityDipsas(lf)), "Dipsas is exempt");
        } finally {
            tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, oldHead);
            HData.get(tp).removeTag(Crown.COSMETIC);
            tp.setGameType(mode);
            tp.setHealth(tp.getMaxHealth());
        }
    }
}
