package lucifertotem;

import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HPotions;
import lucifertotem.v130.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/** 1.3.0 stage 4.5 "The Garden of the Gods" */
public class Stage45Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0/4.5] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }
    /** a random that always rolls zero: every growth attempt succeeds */
    static final Random ZERO = new Random() { @Override public int nextInt(int n) { return 0; } @Override public float nextFloat() { return 0f; } @Override public boolean nextBoolean() { return false; } };

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("registration", () -> registration(srv));
        guard("silphium", () -> silphium(lf, tp, pad));
        guard("moly", () -> moly(lf, tp, pad));
        guard("barley", () -> barley(lf, tp, pad));
        guard("ambrosia", () -> ambrosia(ow, lf, tp, pad));
    }

    static void registration(MinecraftServer srv) {
        check(Crops.SILPHIUM_SEEDS != null && Crops.MOLY_SEEDS != null && Crops.BARLEY_SEEDS != null && Crops.AMBROSIA_SEEDS != null, "four kinds of seeds registered");
        check(Crops.SILPHIUM != null && Crops.MOLY_PLANT != null && Crops.BARLEY_PLANT != null && Crops.AMBROSIA != null && Crops.BLESSED_FARMLAND != null, "four crops + Theriac-watered farmland registered");
        check(Crops.SILPHIUM.getMaxAge() == 3 && Crops.MOLY_PLANT.getMaxAge() == 3 && Crops.BARLEY_PLANT.getMaxAge() == 7 && Crops.AMBROSIA.getMaxAge() == 4, "growth stages: silphium 4, moly 4, barley 8, ambrosia 5");
        check(Crops.MOLY_WARD != null && Crops.MOLY_WARD_TYPE != null && Crops.MOLY_WARD_TYPE.getRegistryName() != null, "the Moly Ward effect and potion");
        int a = 0; String[] advs = {"extinct_again", "hermes_gift", "you_were_warned", "food_of_the_gods"};
        for (String n : advs) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == advs.length, "crop advancements load (" + a + "/" + advs.length + ")");
        int r = 0; String[] recs = {"bread_from_barley", "kykeon_from_barley", "kykeon_hierophant", "purple_nectar", "seasoned_cooked_beef", "seasoned_bread", "seasoned_cooked_fish", "seasoned_cooked_fish_1"};
        for (String n : recs) if (net.minecraft.item.crafting.CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, n)) != null) r++;
        check(r == recs.length, "crop recipes load: bread, Kykeon, Hierophant's Kykeon, Nectar, seasoning (" + r + "/" + recs.length + ")");
        net.minecraft.item.crafting.IRecipe sb = net.minecraft.item.crafting.CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, "seasoned_cooked_beef"));
        check(sb != null && sb.getRecipeOutput().hasTagCompound() && sb.getRecipeOutput().getTagCompound().getBoolean(Crops.SEASONED), "seasoned food carries its tag (shows in JEI as a plain recipe)");
        check("silphium".equals(Apothecary.CODE.get("lucifertotem:silphium_resin")), "Silphium resin goes in the Theriac Crock");
        int t = 0; String[] tex = {"blocks/silphium_stage_3", "blocks/moly_stage_3", "blocks/barley_stage_7", "blocks/barley_stage_7_ergot", "blocks/ambrosia_stage_4", "blocks/blessed_farmland",
            "items/silphium_resin", "items/moly", "items/barley", "items/ergot", "items/purple_ambrosia", "items/purple_nectar", "items/kykeon_hierophant"};
        for (String s : tex) if (Stage45Tests.class.getResource("/assets/lucifertotem/textures/" + s + ".png") != null) t++;
        check(t == tex.length, "crop textures ship (" + t + "/" + tex.length + ")");
    }

    static BlockPos bed(World w, BlockPos at, IBlockState soil) {
        w.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 2);
        w.setBlockState(at, soil, 2);
        w.setBlockToAir(at.up());
        return at.up();
    }
    static int grow(Crops.BlockCropLT c, World w, BlockPos p, int tries) { int n = 0; for (int i = 0; i < tries; i++) if (c.tryGrow(w, p, w.getBlockState(p), ZERO)) n++; return n; }
    static NonNullList<ItemStack> drops(Crops.BlockCropLT c, World w, BlockPos p) { NonNullList<ItemStack> d = NonNullList.create(); c.getDrops(d, w, p, w.getBlockState(p), 0); return d; }
    static int count(NonNullList<ItemStack> d, net.minecraft.item.Item it) { int n = 0; for (ItemStack s : d) if (s.getItem() == it) n += s.getCount(); return n; }
    static boolean plant(EntityPlayerMP tp, World w, BlockPos soil, net.minecraft.item.Item seeds) {
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(seeds));
        return seeds.onItemUse(tp, w, soil, EnumHand.MAIN_HAND, net.minecraft.util.EnumFacing.UP, 0.5f, 1f, 0.5f) == net.minecraft.util.EnumActionResult.SUCCESS;
    }

    static void silphium(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 560, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        BlockPos sand = c.add(-4, 0, 0), stone = c.add(-2, 0, 0);
        lf.setBlockState(sand, Blocks.SAND.getDefaultState(), 2); lf.setBlockState(stone, Blocks.STONE.getDefaultState(), 2);
        check(plant(tp, lf, sand, Crops.SILPHIUM_SEEDS) && lf.getBlockState(sand.up()).getBlock() == Crops.SILPHIUM, "silphium seeds plant on sand");
        check(!plant(tp, lf, stone, Crops.SILPHIUM_SEEDS), "...but not on stone");
        BlockPos fl = c.add(0, 0, 0);
        lf.setBlockState(fl, Blocks.FARMLAND.getDefaultState(), 2);
        check(plant(tp, lf, fl, Crops.SILPHIUM_SEEDS), "...and on farmland");
        BlockPos p = sand.up();
        check(grow(Crops.SILPHIUM, lf, p, 5) == 0, "far from the sea it will not grow");
        lf.setBlockState(c.add(-4, 0, 4), Blocks.WATER.getDefaultState(), 2);
        check(grow(Crops.SILPHIUM, lf, p, 5) == 3 && Crops.SILPHIUM.isMaxAge(lf.getBlockState(p)), "near the sea it grows, slowly, to its 4th stage");
        NonNullList<ItemStack> d = drops(Crops.SILPHIUM, lf, p);
        check(count(d, Crops.SILPHIUM_RESIN) >= 1, "ripe silphium gives resin (" + d + ")");
        check(Crops.SILPHIUM.bonemealChance() < Crops.MOLY_PLANT.bonemealChance(), "bone meal works, but less often on silphium");
        // over-harvesting
        Crops.SilphiumData sd = Crops.SilphiumData.get(lf);
        boolean gone = false; int n = 0;
        for (int i = 0; i < LTConfig.v130.silphiumExtinctHarvests && !gone; i++) { gone = Crops.harvested(lf, p, tp); n++; }
        check(gone && n == LTConfig.v130.silphiumExtinctHarvests, "harvest " + n + " ripe plants in one place and it goes extinct there");
        check(TestHarness130.adv(tp, "extinct_again"), "'Extinct Again'");
        lf.setBlockState(p, Crops.SILPHIUM.getDefaultState(), 2);
        check(grow(Crops.SILPHIUM, lf, p, 5) == 0 && !Crops.SILPHIUM.canGrow(lf, p, lf.getBlockState(p), false), "extinct: nothing grows, not even with bone meal");
        BlockPos far = p.add(64, 0, 0);
        check(!sd.extinct(far, lf.getTotalWorldTime()), "...only in that area");
        Crops.timeShift = (LTConfig.v130.silphiumExtinctDays + 1) * 24000L;
        check(grow(Crops.SILPHIUM, lf, p, 1) == 1, "after " + LTConfig.v130.silphiumExtinctDays + " days it comes back");
        Crops.timeShift = 0;
        sd.until.clear();
        // seasoning
        ItemStack beef = new ItemStack(Items.COOKED_BEEF); beef.setTagInfo(Crops.SEASONED, new net.minecraft.nbt.NBTTagByte((byte) 1));
        tp.removePotionEffect(MobEffects.REGENERATION);
        check(Crops.seasoned(tp, beef) && tp.isPotionActive(MobEffects.REGENERATION) && tp.isPotionActive(MobEffects.SATURATION), "seasoned food: Saturation and a little Regeneration");
        check(!Crops.seasoned(tp, new ItemStack(Items.COOKED_BEEF)), "plain food: nothing extra");
        tp.removePotionEffect(MobEffects.REGENERATION); tp.removePotionEffect(MobEffects.SATURATION);
    }

    static void moly(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 600, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        BlockPos p = bed(lf, c, Blocks.FARMLAND.getDefaultState());
        lf.setBlockState(p, Crops.MOLY_PLANT.getDefaultState(), 2);
        check(grow(Crops.MOLY_PLANT, lf, p, 6) == 3, "moly grows through 4 stages");
        NonNullList<ItemStack> d = drops(Crops.MOLY_PLANT, lf, p);
        check(count(d, Crops.MOLY) >= 1 && count(d, Crops.MOLY_SEEDS) >= 1, "ripe moly: the herb and its seeds");
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        check(Crops.digFails(tp, ZERO), "hard for mortals to dig up: bare hands can fail");
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        boolean s = !Crops.digFails(tp, ZERO);
        tp.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.GOLDEN_HOE));
        check(s && !Crops.digFails(tp, ZERO), "shears or a golden hoe always work");
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        // the ward
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 400, 0));
        Crops.ward(tp, 600);
        check(tp.isPotionActive(Crops.MOLY_WARD) && !tp.isPotionActive(V130.MANIA), "the Moly Ward: Mania lifts at once");
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 400, 0));
        tp.addPotionEffect(new PotionEffect(HPotions.INSANIA, 400, 0));
        tp.addPotionEffect(new PotionEffect(HPotions.PETRIFYING, 100, 0));
        check(!tp.isPotionActive(V130.MANIA) && !tp.isPotionActive(HPotions.INSANIA) && !tp.isPotionActive(HPotions.PETRIFYING), "while warded: no Mania, no Insania, no petrification");
        net.minecraft.entity.monster.EntityWitch witch = new net.minecraft.entity.monster.EntityWitch(lf);
        witch.setLocationAndAngles(tp.posX + 3, tp.posY, tp.posZ, 0, 0); lf.spawnEntity(witch);
        tp.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 200, 0));
        check(!tp.isPotionActive(MobEffects.WEAKNESS), "a witch's curse slides off");
        witch.setDead();
        tp.addPotionEffect(new PotionEffect(MobEffects.SPEED, 200, 0));
        check(tp.isPotionActive(MobEffects.SPEED), "good effects still work");
        EntityHekateHound h = new EntityHekateHound(lf); h.setLocationAndAngles(tp.posX + 2, tp.posY, tp.posZ, 0, 0); lf.spawnEntity(h);
        h.setAttackTarget(tp); h.onLivingUpdate();
        check(h.getAttackTarget() == null, "Hekate's hounds will not hunt the warded");
        h.setDead();
        check(TestHarness130.adv(tp, "hermes_gift"), "'Hermes' Gift'");
        ItemStack awk = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.AWKWARD);
        ItemStack out = net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(awk, new ItemStack(Crops.MOLY));
        check(PotionUtils.getPotionFromItem(out) == Crops.MOLY_WARD_TYPE, "brew Moly into an Awkward Potion: Potion of the Moly Ward");
        ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
        check(net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(water, new ItemStack(Crops.MOLY)).isEmpty(), "...not into plain water");
        for (net.minecraft.potion.Potion q : new net.minecraft.potion.Potion[]{Crops.MOLY_WARD, MobEffects.SPEED}) tp.removePotionEffect(q);
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 100, 0));
        check(tp.isPotionActive(V130.MANIA), "without the ward, Mania takes hold again");
        tp.removePotionEffect(V130.MANIA);
    }

    static void barley(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 640, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        BlockPos soil = c; lf.setBlockState(soil, Blocks.FARMLAND.getDefaultState(), 2);
        check(plant(tp, lf, soil, Crops.BARLEY_SEEDS), "barley seeds plant on farmland");
        BlockPos p = soil.up();
        int old = LTConfig.v130.ergotPercent;
        LTConfig.v130.ergotPercent = 0;
        check(grow(Crops.BARLEY_PLANT, lf, p, 10) == 7 && !lf.getBlockState(p).getValue(Crops.ERGOT_P), "barley grows through 8 stages (ergotPercent 0: clean)");
        NonNullList<ItemStack> d = drops(Crops.BARLEY_PLANT, lf, p);
        check(count(d, Crops.BARLEY) == 1 && count(d, Crops.BARLEY_SEEDS) >= 1 && count(d, Crops.ERGOT) == 0, "ripe barley: grain and seeds");
        lf.setBlockState(p, Crops.BARLEY_PLANT.getDefaultState(), 2);
        LTConfig.v130.ergotPercent = 100;
        grow(Crops.BARLEY_PLANT, lf, p, 10);
        LTConfig.v130.ergotPercent = old;
        check(lf.getBlockState(p).getValue(Crops.ERGOT_P) && count(drops(Crops.BARLEY_PLANT, lf, p), Crops.ERGOT) == 1, "ergot on the ripe heads (purple-black), dropped with the grain");
        check(Crops.BARLEY_PLANT.getStateFromMeta(Crops.BARLEY_PLANT.getMetaFromState(lf.getBlockState(p))) == lf.getBlockState(p), "the ergot survives a save (meta round-trip)");
        tp.removePotionEffect(HPotions.INSANIA);
        new ItemStack(Crops.ERGOT).getItem().onItemUseFinish(new ItemStack(Crops.ERGOT), lf, tp);
        check(tp.isPotionActive(MobEffects.NAUSEA) && tp.isPotionActive(HPotions.INSANIA) && TestHarness130.adv(tp, "you_were_warned"), "raw ergot: Nausea and Insania ('You Were Warned')");
        tp.removePotionEffect(MobEffects.NAUSEA); tp.removePotionEffect(HPotions.INSANIA);
        int v0 = Stage4.visions;
        Crops.KYKEON_HIEROPHANT.onItemUseFinish(new ItemStack(Crops.KYKEON_HIEROPHANT), lf, tp);
        PotionEffect k = tp.getActivePotionEffect(V130.KYKEON);
        check(k != null && k.getAmplifier() == 1 && Stage4.visions == v0 + 1, "the Hierophant's Kykeon: a stronger, longer vision");
        tp.removePotionEffect(V130.KYKEON); tp.removePotionEffect(MobEffects.NAUSEA); tp.removePotionEffect(MobEffects.NIGHT_VISION);
        EntityCow cow = new EntityCow(lf); cow.setLocationAndAngles(c.getX() + 3.5, y + 1, c.getZ() + 0.5, 0, 0); lf.spawnEntity(cow);
        ItemStack b = new ItemStack(Crops.BARLEY, 2);
        check(Crops.feedBarley(tp, cow, b) && cow.isInLove() && b.getCount() == 1, "cows eat barley (and breed)");
        cow.setDead();
        check(new EntityAmmonius(lf).isBreedingItem(new ItemStack(Crops.BARLEY)), "Doctor Ammonius likes barley as well as wheat");
    }

    static void ambrosia(WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        BlockPos sp = ow.getSpawnPoint();
        BlockPos op = bed(ow, new BlockPos(sp.getX() + 40, 230, sp.getZ() + 40), Blocks.FARMLAND.getDefaultState());
        ow.setBlockState(op, Crops.AMBROSIA.getDefaultState(), 2);
        check(!Crops.AMBROSIA.canGrowHere(ow, op), "in the Overworld, on plain farmland, Ambrosia will not grow");
        ItemStack th = new ItemStack(ModContent.THERIAC);
        tp.inventory.clear();
        check(Crops.bless(ow, op.down(), tp, th) && ow.getBlockState(op.down()).getBlock() == Crops.BLESSED_FARMLAND, "right-click farmland with Theriac: Theriac-watered farmland");
        check(th.isEmpty() && TestHarness130.count(tp, Items.GLASS_BOTTLE) == 1, "the Theriac is used up (you keep the bottle)");
        check(Crops.AMBROSIA.canGrowHere(ow, op) && Crops.AMBROSIA.canBlockStay(ow, op, ow.getBlockState(op)), "on blessed farmland it grows anywhere");
        Crops.AMBROSIA.grow(ow, op, ow.getBlockState(op));
        check(Crops.AMBROSIA.getMetaFromState(ow.getBlockState(op)) == 1, "bone meal: one stage at a time");
        IBlockState fs = ow.getBlockState(op.down());
        check(fs.getValue(net.minecraft.block.BlockFarmland.MOISTURE) == 7, "blessed farmland stays moist");
        ow.setBlockToAir(op); ow.setBlockToAir(op.down()); ow.setBlockToAir(op.down(2));
        // in the Lightfall any farmland will do
        int bx = pad.getX() + 680, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        lf.setBlockState(c, Blocks.FARMLAND.getDefaultState(), 2);
        check(plant(tp, lf, c, Crops.AMBROSIA_SEEDS), "ambrosia seeds plant on farmland");
        BlockPos p = c.up();
        check(Crops.AMBROSIA.canGrowHere(lf, p) && grow(Crops.AMBROSIA, lf, p, 8) == 4, "in the Lightfall it grows through 5 stages");
        IBlockState ripe = lf.getBlockState(p);
        check(Crops.AMBROSIA.getLightValue(ripe, lf, p) >= 9, "the ripe fruit glows violet");
        NonNullList<ItemStack> d = drops(Crops.AMBROSIA, lf, p);
        check(count(d, Crops.PURPLE_AMBROSIA) == 1, "harvest: one Purple Ambrosia (seeds back only sometimes)");
        int seedsBack = 0; for (int i = 0; i < 400; i++) { NonNullList<ItemStack> e = NonNullList.create(); Crops.AMBROSIA.getDrops(e, lf, p, ripe, 0); seedsBack += count(e, Crops.AMBROSIA_SEEDS); }
        check(seedsBack > 40 && seedsBack < 170, "seeds come back at a low rate (" + seedsBack + "/400)");
        // eating it, near Doctor Ammonius
        EntityAmmonius a = new EntityAmmonius(lf); a.setLocationAndAngles(tp.posX + 3, tp.posY, tp.posZ, 0, 0); lf.spawnEntity(a);
        int b0 = EntityAmmonius.potionBlurts;
        tp.getFoodStats().setFoodLevel(2);
        new ItemStack(Crops.PURPLE_AMBROSIA).getItem().onItemUseFinish(new ItemStack(Crops.PURPLE_AMBROSIA), lf, tp);
        PotionEffect pu = tp.getActivePotionEffect(ModContent.PURPLE), ab = tp.getActivePotionEffect(MobEffects.ABSORPTION);
        check(tp.getFoodStats().getFoodLevel() == 20, "Ambrosia fills you up completely");
        check(pu != null && pu.getAmplifier() == 1 && ab != null && ab.getAmplifier() == 1 && tp.isPotionActive(MobEffects.REGENERATION), "PURPLE, Absorption II and a short Regeneration");
        check(EntityAmmonius.potionBlurts == b0 + 1, "Doctor Ammonius shouts PURPLE when you eat it near him");
        check(TestHarness130.adv(tp, "food_of_the_gods"), "'Food of the Gods'");
        a.setDead();
        int am = Crops.ambrosial;
        for (int i = 0; i < 60; i++) Crops.ambrosia(tp, false);
        check(Crops.ambrosial > am, "sometimes 'Ambrosial': you glow (" + (Crops.ambrosial - am) + "/60)");
        Crops.PURPLE_NECTAR.onItemUseFinish(new ItemStack(Crops.PURPLE_NECTAR), lf, tp);
        PotionEffect pn = tp.getActivePotionEffect(ModContent.PURPLE);
        check(pn != null && pn.getAmplifier() == 2 && pn.getDuration() > 4000, "Purple Nectar: stronger PURPLE, for longer");
        for (net.minecraft.potion.Potion q : new net.minecraft.potion.Potion[]{ModContent.PURPLE, MobEffects.ABSORPTION, MobEffects.REGENERATION, MobEffects.GLOWING}) tp.removePotionEffect(q);
        // the gifts
        HData.get(tp).removeTag("ambrosiaTest");
        tp.inventory.clear();
        check(Crops.gift(tp, "ambrosiaTest", 2) && TestHarness130.count(tp, Crops.AMBROSIA_SEEDS) == 2 && !Crops.gift(tp, "ambrosiaTest", 2), "seed gifts (Lucifer's interview, the diploma) come once");
        tp.inventory.clear();
    }
}
