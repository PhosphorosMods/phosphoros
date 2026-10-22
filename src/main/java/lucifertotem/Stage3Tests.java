package lucifertotem;

import lucifertotem.hillman.HItems;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.paint.EntityLTPainting;
import lucifertotem.paint.Paintings;
import lucifertotem.v130.*;
import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.List;
import java.util.Random;

/** 1.3.0 stage 3 "Wild Things": Bacchants, Scythian Huntresses, Hekate's Crossroads */
public class Stage3Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0/3] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("registration", () -> registration(srv));
        guard("bacchants", () -> bacchants(lf, tp, pad));
        guard("huntress", () -> huntress(lf, tp, pad));
        guard("hekate", () -> hekate(lf, tp, pad));
        guard("structures", () -> structures(lf, tp, pad));
    }

    static void vulnerable(EntityPlayerMP tp) {
        tp.hurtResistantTime = 0;
        try { net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, tp, 0, "field_147101_bU"); } catch (Throwable ignored) {}
    }

    static void registration(MinecraftServer srv) {
        check(Stage3.THYRSUS != null && Stage3.IVY_CROWN != null && Stage3.BACCHIC_WINE != null && Stage3.TOXICON_ARROW != null && Stage3.SCYTHIAN_BOW != null && Stage3.KEY_OF_HEKATE != null,
            "Thyrsus, Ivy Crown, Bacchic Wine, Toxicon Arrow, Scythian Bow, Key of Hekate registered");
        check(Stage3.HEKATAION != null && Stage3.HEKATE_GATE != null, "Hekataion + Gate of Hekate blocks registered");
        int e = 0; for (Class<? extends net.minecraft.entity.Entity> c : java.util.Arrays.asList(EntityBacchant.class, EntityHuntress.class, EntityToxiconArrow.class, EntityHekateHound.class))
            if (net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(c) != null) e++;
        check(e == 4, "4 stage 3 entities registered (" + e + ")");
        int a = 0; String[] advs = {"bacchanal", "ivy_crown", "toxicon", "scythian_bow", "key_of_hekate", "hekates_hounds", "unlocked"};
        for (String n : advs) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == advs.length, "stage 3 advancements load (" + a + "/" + advs.length + ")");
        check(Paintings.ALL.size() == 14 && Paintings.CLASSIC.size() == 8, "6 new Lady Babylon paintings (" + Paintings.ALL.size() + ")");
        int found = 0;
        for (Paintings.Variant v : Paintings.ALL.subList(8, Paintings.ALL.size()))
            if (Stage3Tests.class.getResource("/assets/lucifertotem/textures/painting/" + v.name + ".png") != null) found++;
        check(found == 6, "new painting textures ship (" + found + "/6)");
        int t = 0; for (String s : new String[]{"entity/bacchant_0", "entity/bacchant_3", "entity/scythian_huntress", "entity/hekate_hound", "entity/hekate_hound_eyes", "entity/toxicon_arrow", "models/armor/ivy_crown_layer_1", "blocks/hekataion", "blocks/hekate_gate_open", "items/scythian_bow_pulling_2"})
            if (Stage3Tests.class.getResource("/assets/lucifertotem/textures/" + s + ".png") != null) t++;
        check(t == 10, "stage 3 textures ship (" + t + "/10)");
    }

    static void bacchants(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        EntityBacchant b = new EntityBacchant(lf);
        b.setLocationAndAngles(pad.getX() + 2.5, pad.getY(), pad.getZ() + 0.5, 0, 0);
        b.onInitialSpawn(lf.getDifficultyForLocation(pad), null);
        lf.spawnEntity(b);
        check(b.getHeldItemMainhand().getItem() == Stage3.THYRSUS, "a Bacchant carries a thyrsus");
        EntityCow victim = new EntityCow(lf); victim.setLocationAndAngles(pad.getX() + 3.5, pad.getY(), pad.getZ() + 0.5, 0, 0); lf.spawnEntity(victim);
        int m0 = EntityBacchant.maniaHits;
        for (int i = 0; i < 40 && EntityBacchant.maniaHits == m0; i++) { victim.hurtResistantTime = 0; victim.setHealth(10f); b.attackEntityAsMob(victim); }
        check(EntityBacchant.maniaHits > m0 && victim.isPotionActive(V130.MANIA), "a Bacchant's blows can bring on Mania");
        victim.setDead();
        tp.addPotionEffect(new PotionEffect(V130.MANIA, 100, 0));
        // the Ivy Crown cures it
        ItemStack head = tp.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Stage3.IVY_CROWN));
        new Stage3.Events().living(new LivingEvent.LivingUpdateEvent(tp));
        check(!tp.isPotionActive(V130.MANIA) && TestHarness130.adv(tp, "ivy_crown"), "wearing the Ivy Crown: Mania is shrugged off, 'Crowned in Ivy'");
        tp.setItemStackToSlot(EntityEquipmentSlot.HEAD, head);
        // the thyrsus itself
        EntityCow cow = new EntityCow(lf); cow.setLocationAndAngles(pad.getX() - 2.5, pad.getY(), pad.getZ() + 0.5, 0, 0); lf.spawnEntity(cow);
        boolean mad = false;
        for (int i = 0; i < 30 && !mad; i++) { Stage3.THYRSUS.hitEntity(new ItemStack(Stage3.THYRSUS), cow, tp); mad = cow.isPotionActive(V130.MANIA); }
        check(mad, "the Thyrsus can drive its target into Mania");
        cow.setDead();
        // music calms them
        check(!b.dancing(), "no music: the Bacchant is not dancing");
        BlockPos jb = pad.add(4, 0, 4);
        lf.setBlockState(jb, Blocks.JUKEBOX.getDefaultState().withProperty(BlockJukebox.HAS_RECORD, true));
        b.setAttackTarget(tp);
        for (int i = 0; i < 25; i++) { b.ticksExisted++; b.onUpdate(); }
        check(b.dancing() && b.getAttackTarget() == null && EntityBacchant.calmedCount > 0, "a playing jukebox nearby: the pack stops fighting and dances (dancing=" + b.dancing() + " target=" + b.getAttackTarget() + " calmed=" + EntityBacchant.calmedCount + " music=" + EntityBacchant.music(lf, b.getPosition()) + " dead=" + b.isDead + " ticks=" + b.ticksExisted + " id&15=" + (b.getEntityId() & 15) + ")");
        check(TestHarness130.adv(tp, "bacchanal"), "'Music Soothes the Savage Maenad' granted to players nearby");
        lf.setBlockState(jb, Blocks.NOTEBLOCK.getDefaultState());
        check(EntityBacchant.music(lf, b.getPosition()), "a note block nearby also counts as music");
        lf.setBlockState(jb, Blocks.AIR.getDefaultState());
        check(!EntityBacchant.music(lf, b.getPosition()), "no music without a jukebox/note block");
        b.setDead();
        // bacchic wine
        tp.removePotionEffect(MobEffects.STRENGTH);
        ((HItems.ItemDrink) Stage3.BACCHIC_WINE).onItemUseFinish(new ItemStack(Stage3.BACCHIC_WINE), lf, tp);
        check(tp.isPotionActive(MobEffects.STRENGTH), "Bacchic Wine gives Strength");
        tp.removePotionEffect(MobEffects.STRENGTH); tp.removePotionEffect(MobEffects.NAUSEA);
    }

    static void huntress(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        EntityHuntress h = new EntityHuntress(lf);
        h.setLocationAndAngles(pad.getX() + 0.5, pad.getY(), pad.getZ() + 6.5, 0, 0);
        lf.spawnEntity(h);
        h.onInitialSpawn(lf.getDifficultyForLocation(pad), null);
        if (!h.isRiding()) h.mount();
        check(h.isRiding() && h.getRidingEntity() instanceof net.minecraft.entity.passive.EntityHorse, "a Scythian Huntress rides a horse");
        check(h.getHeldItemMainhand().getItem() == Stage3.SCYTHIAN_BOW, "... and carries a Scythian Bow");
        // she shoots toxicon arrows
        EntityCow cow = new EntityCow(lf); cow.setLocationAndAngles(pad.getX() + 4.5, pad.getY(), pad.getZ() - 2.5, 0, 0); lf.spawnEntity(cow);
        int s0 = EntityHuntress.shots;
        h.attackEntityWithRangedAttack(cow, 1f);
        List<EntityToxiconArrow> arrows = lf.getEntitiesWithinAABB(EntityToxiconArrow.class, new AxisAlignedBB(pad).grow(12));
        check(EntityHuntress.shots == s0 + 1 && !arrows.isEmpty(), "she looses a toxicon arrow (" + arrows.size() + ")");
        arrows.forEach(net.minecraft.entity.Entity::setDead);
        // a toxicon hit: Slowness IV then Mania
        int t0 = Stage3.toxiconHits;
        EntityToxiconArrow a = new EntityToxiconArrow(lf, pad.getX() + 4.5, pad.getY() + 1.0, pad.getZ() + 1.5);
        a.shoot(0, 0, -1, 1.5f, 0f);
        lf.spawnEntity(a);
        for (int i = 0; i < 20 && Stage3.toxiconHits == t0; i++) a.onUpdate();
        PotionEffect slow = cow.getActivePotionEffect(MobEffects.SLOWNESS);
        check(Stage3.toxiconHits == t0 + 1 && slow != null && slow.getAmplifier() == 3, "a toxicon arrow strikes: Slowness IV (" + slow + ", hits " + (Stage3.toxiconHits - t0) + ", arrow at " + String.format("%.1f,%.1f,%.1f", a.posX, a.posY, a.posZ) + " cow at " + cow.getPosition() + " dead=" + cow.isDead + ")");
        check(!cow.isPotionActive(V130.MANIA), "... no Mania yet");
        cow.getEntityData().setLong("lt_mania_at", lf.getTotalWorldTime());
        new Stage3.Events().living(new LivingEvent.LivingUpdateEvent(cow));
        check(cow.isPotionActive(V130.MANIA), "... two seconds later, Mania");
        cow.setDead(); a.setDead();
        Stage3.toxicon(tp);
        check(TestHarness130.adv(tp, "toxicon"), "'Herodotus Was Right' for the poisoned player");
        tp.removePotionEffect(MobEffects.SLOWNESS); tp.getEntityData().removeTag("lt_mania_at");
        // toxicon arrows from a vanilla bow are still toxicon arrows; the Scythian bow draws faster
        check(((net.minecraft.item.ItemArrow) Stage3.TOXICON_ARROW).createArrow(lf, new ItemStack(Stage3.TOXICON_ARROW), tp) instanceof EntityToxiconArrow, "Toxicon Arrow items fire toxicon arrows");
        double vv = bowSpeed(lf, tp, new ItemStack(Items.BOW)), sv = bowSpeed(lf, tp, new ItemStack(Stage3.SCYTHIAN_BOW));
        check(sv > vv * 1.25, String.format("after a 10-tick draw the Scythian Bow's arrow is much faster (%.2f vs vanilla %.2f)", sv, vv));
        if (h.getRidingEntity() != null) h.getRidingEntity().setDead();
        h.setDead();
        check(LTConfig.v130.huntressWeight > 0 && lucifertotem.world.LightfallBiome.class != null, "Huntresses are in the Lightfall spawn list (weight " + LTConfig.v130.huntressWeight + ")");
    }
    static double bowSpeed(WorldServer lf, EntityPlayerMP tp, ItemStack bow) {
        tp.inventory.setInventorySlotContents(20, new ItemStack(Items.ARROW, 4));
        tp.rotationPitch = 0; tp.rotationYaw = 0;
        java.util.Set<Integer> before = new java.util.HashSet<>();
        for (EntityArrow e : lf.getEntitiesWithinAABB(EntityArrow.class, tp.getEntityBoundingBox().grow(6))) before.add(e.getEntityId());
        bow.getItem().onPlayerStoppedUsing(bow, lf, tp, bow.getMaxItemUseDuration() - 10);
        double best = 0;
        for (EntityArrow e : lf.getEntitiesWithinAABB(EntityArrow.class, tp.getEntityBoundingBox().grow(6))) {
            if (before.contains(e.getEntityId())) continue;
            best = Math.max(best, Math.sqrt(e.motionX * e.motionX + e.motionY * e.motionY + e.motionZ * e.motionZ));
            e.setDead();
        }
        tp.inventory.setInventorySlotContents(20, ItemStack.EMPTY);
        return best;
    }

    static void hekate(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        BlockPos at = pad.add(-5, 0, -5);
        lf.setBlockState(at, Stage3.HEKATAION.getDefaultState());
        int idx = Stage3.offeringIndex(lf);
        check(Stage3.offer(lf, at, tp, ItemStack.EMPTY).equals("hint"), "an empty hand at the Hekataion: tonight's riddle and how long until her hour");
        ItemStack right = idx == 0 ? new ItemStack(HItems.HONEY_CAKE) : idx == 1 ? new ItemStack(Items.EGG) : new ItemStack(Items.FISH);
        ItemStack wrong = idx == 1 ? new ItemStack(HItems.HONEY_CAKE) : new ItemStack(Items.EGG);
        // the right offering at the wrong hour
        Stage3.clockShift = 0; Stage3.clockShift = 6000 - Stage3.clock(lf) % 24000;
        check(!Stage3.hekatesHour(lf), "noon is not Hekate's hour");
        int p0 = Stage3.houndPacks;
        check(Stage3.offer(lf, at, tp, right.copy()).equals("hounds") && Stage3.houndPacks == p0 + 1, "the right offering at the wrong hour: hounds");
        List<EntityHekateHound> hounds = lf.getEntitiesWithinAABB(EntityHekateHound.class, new AxisAlignedBB(at).grow(6));
        check(hounds.size() == 3 && hounds.get(0).getAttackTarget() == tp && TestHarness130.adv(tp, "hekates_hounds"), "three black hounds, hunting the offerer; 'Hekate's Hounds' (" + hounds.size() + ")");
        int b0 = EntityHekateHound.bites;
        EntityCow cow = new EntityCow(lf); cow.setLocationAndAngles(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0); lf.spawnEntity(cow);
        if (!hounds.isEmpty()) hounds.get(0).attackEntityAsMob(cow);
        check(EntityHekateHound.bites == b0 + 1 && cow.getHealth() < cow.getMaxHealth(), "a hound bites (" + cow.getHealth() + ")");
        cow.setDead();
        check(!hounds.isEmpty() && !hounds.get(0).processInteract(tp, EnumHand.MAIN_HAND) && !hounds.get(0).isTamed(), "her hounds cannot be tamed");
        hounds.forEach(net.minecraft.entity.Entity::setDead);
        tp.setHealth(20f);
        // midnight
        Stage3.clockShift = 0; Stage3.clockShift = 18000 - Stage3.clock(lf) % 24000;
        check(Stage3.hekatesHour(lf), "18000 is Hekate's hour");
        check(Stage3.offer(lf, at, tp, wrong.copy()).equals("hounds"), "the wrong offering at her hour: hounds");
        lf.getEntitiesWithinAABB(EntityHekateHound.class, new AxisAlignedBB(at).grow(6)).forEach(net.minecraft.entity.Entity::setDead);
        int k0 = TestHarness130.count(tp, Stage3.KEY_OF_HEKATE);
        check(Stage3.offer(lf, at, tp, right.copy()).equals("key") && TestHarness130.count(tp, Stage3.KEY_OF_HEKATE) == k0 + 1 && TestHarness130.adv(tp, "key_of_hekate"), "the right offering at her hour: the Key of Hekate, 'Keybearer'");
        Stage3.clockShift = 0;
        // the gate
        BlockPos g = pad.add(-7, 0, -5);
        lf.setBlockState(g, Stage3.HEKATE_GATE.getDefaultState()); lf.setBlockState(g.up(), Stage3.HEKATE_GATE.getDefaultState());
        check(lf.getBlockState(g).getCollisionBoundingBox(lf, g) != null, "a locked gate blocks the way");
        check(Stage3.open(lf, g) == 2 && lf.getBlockState(g.up()).getValue(Stage3.BlockHekateGate.OPEN), "the key opens the whole gate (2 blocks)");
        check(lf.getBlockState(g).getCollisionBoundingBox(lf, g) == null, "an open gate can be walked through");
        Stage3.HEKATE_GATE.updateTick(lf, g, lf.getBlockState(g), new Random(1));
        check(!lf.getBlockState(g).getValue(Stage3.BlockHekateGate.OPEN), "it swings shut again (scheduled)");
        lf.setBlockToAir(g); lf.setBlockToAir(g.up()); lf.setBlockToAir(at);
    }

    /** a 16x16 star-grass plateau over solid duskstone, for building structures deterministically */
    static BlockPos plateau(WorldServer lf, int bx, int bz, int y) {
        for (int x = bx - 2; x < bx + 18; x++) for (int z = bz - 2; z < bz + 18; z++) {
            for (int yy = y - 12; yy < y; yy++) lf.setBlockState(new BlockPos(x, yy, z), ModContent.DUSKSTONE.getDefaultState(), 2);
            lf.setBlockState(new BlockPos(x, y, z), ModContent.STAR_GRASS.getDefaultState(), 2);
            for (int yy = y + 1; yy < y + 24; yy++) lf.setBlockToAir(new BlockPos(x, yy, z));
        }
        return new BlockPos(bx + 8, y, bz + 8);
    }
    static int chests(WorldServer lf, BlockPos c, int r, net.minecraft.util.ResourceLocation loot) {
        int n = 0;
        for (TileEntity te : lf.loadedTileEntityList) if (te instanceof TileEntityChest && te.getPos().distanceSq(c) < r * r) {
            net.minecraft.nbt.NBTTagCompound t = te.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
            if (loot == null || loot.toString().equals(t.getString("LootTable"))) n++;
        }
        return n;
    }
    static int paintings(WorldServer lf, BlockPos c, String name) {
        int n = 0; for (EntityLTPainting p : lf.getEntitiesWithinAABB(EntityLTPainting.class, new AxisAlignedBB(c).grow(12))) if (p.art.name.equals(name)) n++;
        return n;
    }

    static void structures(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        // the 1.3.0 roll bands: own salt, sensible rates, older rolls untouched
        int[] n = new int[StructureIndex.TYPES130.length];
        for (int cx = 0; cx < 100; cx++) for (int cz = 0; cz < 100; cz++) {
            String t = StructureIndex.typeForRoll130(StructureIndex.roll130(lf.getSeed(), cx, cz));
            for (int i = 0; i < n.length; i++) if (StructureIndex.TYPES130[i].equals(t)) n[i]++;
        }
        boolean sane = true; StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length; i++) { int[] b = StructureIndex.band130(StructureIndex.TYPES130[i]); int exp = (b[1] - b[0]) * 10; sane &= n[i] > exp / 2 && n[i] < exp * 2; sb.append(StructureIndex.TYPES130[i]).append('=').append(n[i]).append(' '); }
        check(sane, "1.3.0 structure rolls per 10,000 chunks match their bands: " + sb);
        // Bacchic fountain
        int bx = pad.getX() + 200, bz = pad.getZ(), y = 200;
        BlockPos c = plateau(lf, bx, bz, y);
        boolean ok = Gen130.build(lf, "bacchic_fountain", bx, bz, new Random(5));
        check(ok, "a Bacchic Fountain builds on a flat site");
        List<EntityBacchant> pack = lf.getEntitiesWithinAABB(EntityBacchant.class, new AxisAlignedBB(c).grow(8));
        check(pack.size() >= 4 && pack.size() <= 6 && pack.stream().allMatch(e -> e.getHeldItemMainhand().getItem() == Stage3.THYRSUS), "... with a pack of 4-6 Bacchants (" + pack.size() + ")");
        check(lf.getBlockState(c.add(0, 1, -4)).getBlock() == Blocks.JUKEBOX, "... an empty jukebox (the hint)");
        check(chests(lf, c, 8, Stage3.LOOT_FOUNTAIN) == 1, "... a chest of the god's gifts");
        check(paintings(lf, c, "bacchanal") == 1, "... and Lady Babylon's 'Bacchanal' on the screen wall");
        BlockPos found = StructureIndex.locate(lf, "bacchic_fountain", c.add(40, 0, 0), 6, 0);
        check(found != null && found.distanceSq(c) < 4 * 4, "the fountain is recorded for locate (" + found + ")");
        pack.forEach(net.minecraft.entity.Entity::setDead);
        // Crossroads
        bx = pad.getX() + 240;
        c = plateau(lf, bx, bz, y);
        ok = Gen130.build(lf, "crossroads_shrine", bx, bz, new Random(6));
        check(ok, "a Crossroads Shrine builds on a solid site");
        check(lf.getBlockState(c.up()).getBlock() == Stage3.HEKATAION, "... the Hekataion stands where three roads meet");
        check(lf.getBlockState(c.add(-1, 0, -1)).getBlock() == Stage3.HEKATE_GATE, "... a Gate of Hekate in the plinth");
        check(chests(lf, c.down(6), 6, Stage3.LOOT_HEKATE) == 1, "... the vault below holds Hekate's chest");
        check(paintings(lf, c.down(6), "hekate") == 1, "... and 'Hekate of the Three Ways' hangs in the vault");
        int gravel = 0; for (BlockPos q : BlockPos.getAllInBox(c.add(-7, 0, -7), c.add(7, 0, 7))) if (lf.getBlockState(q).getBlock() == Blocks.GRAVEL) gravel++;
        check(gravel >= 15, "... three gravel roads (" + gravel + " blocks)");
        check(Gen130.outOfWindowWrites == 0, "no 1.3.0 structure wrote outside its populate window (" + Gen130.outOfWindowWrites + ")");
        check(Gen130.worst < 200_000_000L, "worst 1.3.0 structure build " + Gen130.worst / 1_000_000 + " ms");
    }
}
