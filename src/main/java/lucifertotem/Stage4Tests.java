package lucifertotem;

import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.v130.*;
import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/** 1.3.0 stage 4 "Mysteries & Music" */
public class Stage4Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0/4] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("registration", () -> registration(srv));
        guard("mysteries", () -> mysteries(lf, tp, pad));
        guard("radio", () -> radio(lf, tp, pad));
        guard("band camp", () -> camp(lf, tp, pad));
        guard("lucifer", () -> lucifer(srv, lf, tp, pad));
    }

    static void registration(MinecraftServer srv) {
        check(Stage4.KYKEON_CUP != null && Stage4.RECORD_DEVILS_FLIGHT instanceof ItemRecord, "Kykeon + the Devil's Flight disc registered");
        check(Stage4.ON_AIR_SIGN != null && Stage4.LYRE_RADIO != null && Stage4.SACRUM != null && Stage4.ANAKTORON != null, "ON AIR sign, Lyre-Radio, Sacred Cista, Anaktoron registered");
        int e = 0; for (Class<? extends net.minecraft.entity.Entity> c : java.util.Arrays.asList(EntityBandGoat.class, EntityBandSinger.class, EntityLucifer.class))
            if (net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(c) != null) e++;
        check(e == 3, "goat, singer and Lucifer entities registered (" + e + ")");
        int a = 0; String[] advs = {"kykeon", "mystes", "on_air", "band_camp", "devils_flight", "interview"};
        for (String n : advs) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == advs.length, "stage 4 advancements load (" + a + "/" + advs.length + ")");
        check(net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(ItemRecord.class, (ItemRecord) Stage4.RECORD_DEVILS_FLIGHT, "field_185076_b") == V130.DISC_DEVILS_FLIGHT, "the disc plays record.devils_flight");
        java.net.URL ogg = Stage4Tests.class.getResource("/assets/lucifertotem/sounds/records/devils_flight.ogg");
        long size = 0; try { size = ogg == null ? 0 : ogg.openConnection().getContentLengthLong(); } catch (Exception ignored) {}
        check(ogg != null && size > 100_000 && size < 1_500_000, "the original chiptune ships (" + size / 1024 + " KB)");
        int t = 0; for (String s : new String[]{"entity/band_goat", "entity/band_singer", "entity/lucifer", "entity/lucifer_glow", "entity/mystes_tattoo", "blocks/on_air_sign", "blocks/lyre_radio_front", "blocks/sacrum_pomegranate", "blocks/anaktoron_top", "items/kykeon", "items/record_devils_flight"})
            if (Stage4Tests.class.getResource("/assets/lucifertotem/textures/" + s + ".png") != null) t++;
        check(t == 11, "stage 4 textures ship (" + t + "/11)");
        check(srv.getCommandManager().getCommands().containsKey("ltinterview"), "/ltinterview command registered (the clickable interview menu)");
    }

    static void mysteries(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 280, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        boolean ok = Gen130.build(lf, "mystery_hall", bx, bz, new Random(7));
        check(ok, "a Mystery Hall builds (hidden under a small well-house)");
        BlockPos alt = Stage4.anaktoron(lf, c, 14);
        check(alt != null && alt.getY() < y - 5, "the Anaktoron is underground (" + alt + ")");
        int sacra = 0; java.util.Map<Integer, BlockPos> bySym = new java.util.HashMap<>();
        for (BlockPos q : BlockPos.getAllInBox(c.add(-8, -14, -8), c.add(8, 0, 8))) if (lf.getBlockState(q).getBlock() == Stage4.SACRUM) { sacra++; bySym.put(lf.getBlockState(q).getValue(Stage4.BlockSacrum.SYMBOL), q); }
        check(sacra == 3 && bySym.size() == 3, "three sacred things: wheat, pomegranate, torch (" + bySym.keySet() + ")");
        check(Stage3Tests.paintings(lf, alt == null ? c : alt, "mysteries") == 1, "'The Road to Eleusis' hangs in the hall");
        check(Stage3Tests.chests(lf, alt == null ? c : alt, 14, Stage4.LOOT_HALL) == 1, "a chest of the rites");
        if (alt == null || bySym.size() < 3) return;
        // sober: nothing
        tp.removePotionEffect(V130.KYKEON);
        check(Stage4.touch(lf, bySym.get(0), tp).equals("sober"), "sober, the sacred things mean nothing");
        check(Stage4.initiate(lf, alt, tp).equals("sober"), "sober, the Anaktoron refuses");
        // drink: the vision points to the hall and gives the order
        TestHarness130.moveTo(tp, lf, c.add(30, 1, 10));
        int v0 = Stage4.visions;
        ((HItems.ItemDrink) Stage4.KYKEON_CUP).onItemUseFinish(new ItemStack(Stage4.KYKEON_CUP), lf, tp);
        check(tp.isPotionActive(V130.KYKEON) && Stage4.visions == v0 + 1 && TestHarness130.adv(tp, "kykeon"), "Kykeon: the effect, a vision, 'Something in the Barley'");
        BlockPos seen = Stage4.vision(tp);
        check(seen != null && seen.distanceSq(c) < 9 * 9, "the vision leads to this hall (" + seen + ", " + Stage4.compass(c.getX() - tp.posX, c.getZ() - tp.posZ) + ")");
        check(Stage4.compass(0, -10).equals("north") && Stage4.compass(10, 0).equals("east") && Stage4.compass(-7, 7).equals("south-west"), "compass directions");
        int[] o = Stage4.order(alt);
        // wrong first
        int wrong = o[1];
        check(Stage4.touch(lf, bySym.get(wrong), tp).equals("wrong") && HData.get(tp).getInteger("eleusisStep") == 0, "out of order: start again (and the hall spins)");
        check(Stage4.initiate(lf, alt, tp).equals("unready"), "the Anaktoron waits until all three are shown");
        boolean shown = true;
        for (int k = 0; k < 3; k++) shown &= Stage4.touch(lf, bySym.get(o[k]), tp).equals("shown");
        check(shown && HData.get(tp).getInteger("eleusisStep") == 3, "in the vision's order: shown 1, 2, 3");
        check(Stage4.initiate(lf, alt, tp).equals("initiated") && HData.get(tp).getBoolean("mystes"), "at the Anaktoron: initiated, a ΜΥΣΤΗΣ");
        check((V130Net.flags(tp) & V130Net.MYSTES) != 0 && TestHarness130.adv(tp, "mystes"), "the tattoo flag is synced, 'ΜΥΣΤΗΣ' granted");
        check(LTConfig.v130.showMystesTattoo, "the tattoo can be hidden in the config (showMystesTattoo)");
        check(Stage4.initiate(lf, alt, tp).equals("already"), "once is enough");
        tp.removePotionEffect(V130.KYKEON); tp.removePotionEffect(net.minecraft.init.MobEffects.NAUSEA); tp.removePotionEffect(net.minecraft.init.MobEffects.NIGHT_VISION);
        TestHarness130.moveTo(tp, lf, pad);
    }

    static void radio(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 320, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        check(Gen130.build(lf, "on_air_studio", bx, bz, new Random(8)), "an On Air Studio builds");
        BlockPos radio = null; int signs = 0;
        for (BlockPos q : BlockPos.getAllInBox(c.add(-7, 0, -7), c.add(7, 8, 7))) {
            if (lf.getBlockState(q).getBlock() == Stage4.LYRE_RADIO) radio = q.toImmutable();
            if (lf.getBlockState(q).getBlock() == Stage4.ON_AIR_SIGN) signs++;
        }
        check(radio != null && signs == 2, "... with a Lyre-Radio and two glowing ON AIR signs (" + signs + ")");
        check(Stage4.ON_AIR_SIGN.getLightValue(Stage4.ON_AIR_SIGN.getDefaultState()) >= 10, "the ON AIR sign glows");
        check(Stage3Tests.paintings(lf, c, "on_air") == 1, "Lady Babylon's 'On Air' hangs in the studio");
        if (radio == null) return;
        boolean plate = false; for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.HORIZONTALS) plate |= lf.getBlockState(radio.offset(f)).getBlock() == Blocks.WOODEN_PRESSURE_PLATE;
        check(plate, "a pressure plate in front of the desk puts it on air");
        TestHarness130.moveTo(tp, lf, radio.south(2));
        int p0 = Stage4.radioPlays;
        lf.setBlockState(radio.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        check(Stage4.radioPlays == p0 + 1 && Stage4.lastRadioClip >= 0, "a redstone pulse: it plays a Doctor Ammonius clip (" + (Stage4.lastRadioClip >= 0 ? lucifertotem.hillman.AmmoniusClips.NAME[Stage4.lastRadioClip] : "-") + ")");
        check(TestHarness130.adv(tp, "on_air"), "'On Air' for listeners nearby");
        lf.neighborChanged(radio, Blocks.REDSTONE_BLOCK, radio.up());
        check(Stage4.radioPlays == p0 + 1, "held on: no replay (rising edge only)");
        lf.setBlockToAir(radio.up());
        int r0 = Stage4.radioRefused;
        lf.setBlockState(radio.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        check(Stage4.radioPlays == p0 + 1 && Stage4.radioRefused == r0 + 1, "a second pulse right away: cooling down");
        lf.setBlockToAir(radio.up());
        // after the cooldown it plays again; two in three are lectures from the new informative clips
        TileEntity te = lf.getTileEntity(radio);
        int lectures = 0, n = 0;
        java.util.Set<Integer> L = new java.util.HashSet<>(); for (int k : lucifertotem.hillman.AmmoniusClips.LECTURE) L.add(k);
        for (int i = 0; i < 60; i++) { ((Stage4.TileRadio) te).last = -100000; int cl = Stage4.play(lf, radio, (Stage4.TileRadio) te); if (cl >= 0) { n++; if (L.contains(cl)) lectures++; } }
        check(n == 60 && lectures > 25 && lectures < 55, "after the cooldown it plays again; " + lectures + "/60 were lectures");
        TestHarness130.moveTo(tp, lf, pad);
    }

    static void camp(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        int bx = pad.getX() + 360, bz = pad.getZ(), y = 200;
        BlockPos c = Stage3Tests.plateau(lf, bx, bz, y);
        check(Gen130.build(lf, "band_camp", bx, bz, new Random(9)), "a Devil's Flight band camp builds");
        BlockPos jb = null;
        for (BlockPos q : BlockPos.getAllInBox(c.add(-7, 0, -7), c.add(7, 6, 7))) if (lf.getBlockState(q).getBlock() == Blocks.JUKEBOX) jb = q.toImmutable();
        TileEntity te = jb == null ? null : lf.getTileEntity(jb);
        check(te instanceof BlockJukebox.TileEntityJukebox && ((BlockJukebox.TileEntityJukebox) te).getRecord().getItem() == Stage4.RECORD_DEVILS_FLIGHT, "the stage jukebox holds the Devil's Flight disc");
        check(Stage3Tests.paintings(lf, c, "devils_flight") == 1, "the 'Devil's Flight' band poster is the backdrop");
        check(Stage3Tests.chests(lf, c, 8, Stage4.LOOT_CAMP) == 1, "a camp chest (with another copy of the disc)");
        List<EntityBandGoat> goats = lf.getEntitiesWithinAABB(EntityBandGoat.class, new AxisAlignedBB(c).grow(8));
        List<EntityBandSinger> singers = lf.getEntitiesWithinAABB(EntityBandSinger.class, new AxisAlignedBB(c).grow(8));
        check(goats.size() == 1 && singers.size() == 1, "the black goat and the snake-haired singer are at camp");
        if (goats.isEmpty() || singers.isEmpty()) return;
        EntityBandGoat g = goats.get(0);
        g.ticksExisted = 1; g.onLivingUpdate();
        check(g.rocking, "with the disc in the jukebox, the goat headbangs");
        check(!(g instanceof net.minecraft.entity.monster.IMob) && !(singers.get(0) instanceof net.minecraft.entity.monster.IMob), "neither of them is hostile");
        TestHarness130.moveTo(tp, lf, c.up());
        int d0 = TestHarness130.count(tp, Stage4.RECORD_DEVILS_FLIGHT);
        EntityBandSinger s = singers.get(0);
        int v1 = s.sing(tp), v2 = s.sing(tp);
        check(v1 != v2 && TestHarness130.adv(tp, "band_camp"), "she sings verse after verse; 'Groupie'");
        check(TestHarness130.count(tp, Stage4.RECORD_DEVILS_FLIGHT) == d0 + 1, "... and gives you the disc once");
        TestHarness130.moveTo(tp, lf, pad);
    }

    static void lucifer(MinecraftServer srv, WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        // a stand-in citadel roof, recorded as a Morningstar Citadel (its "at" is the south door)
        int bx = pad.getX() + 400, bz = pad.getZ(), y = 180;
        BlockPos door = new BlockPos(bx + 7, y + 1, bz + 14);
        for (int x = bx; x <= bx + 14; x++) for (int z = bz; z <= bz + 14; z++) { lf.setBlockState(new BlockPos(x, y + 12, z), ModContent.DUSKSTONE_BRICKS.getDefaultState()); for (int k = 13; k < 17; k++) lf.setBlockToAir(new BlockPos(x, y + k, z)); }
        StructureIndex.record(lf, "morningstar_citadel", door);
        EntityLucifer l = Stage4.ensureLucifer(lf, door);
        check(l != null && l.posY > y + 12, "Lucifer stands on the roof of the Morningstar Citadel (" + (l == null ? "-" : l.getPosition()) + ")");
        check(Stage4.ensureLucifer(lf, door) == l, "... only one of him");
        if (l == null) return;
        check(!l.attackEntityFrom(DamageSource.GENERIC, 50f) && !l.attackEntityFrom(DamageSource.causePlayerDamage(tp), 50f), "he cannot be harmed");
        TestHarness130.moveTo(tp, lf, l.getPosition().north(2));
        Interview.menu(tp, l);
        check(Interview.ask(tp, "who").equals("answered") && TestHarness130.adv(tp, "interview"), "ask 'Who are you?': a riddling answer, 'Interview with Lucifer'");
        check(Interview.ask(tp, "mysteries").equals(TestHarness130.adv(tp, "mystes") ? "answered" : "locked"), "the Mysteries question unlocks with the 'ΜΥΣΤΗΣ' advancement");
        check(Interview.ask(tp, "labyrinth").equals(TestHarness130.adv(tp, "labyrinth") ? "answered" : "locked"), "the Labyrinth question waits for the Labyrinth");
        check(Interview.ask(tp, "nonsense").equals("unknown"), "unknown questions are ignored");
        int unlockable = 0; for (Interview.Q q : Interview.QUESTIONS) if (q.adv == null || TestHarness130.advancement(srv, q.adv) != null) unlockable++;
        check(unlockable == Interview.QUESTIONS.length, "every question's advancement exists (" + unlockable + "/" + Interview.QUESTIONS.length + ")");
        TestHarness130.moveTo(tp, lf, pad);
        check(Interview.ask(tp, "who").equals("far"), "from far away he can't hear you");
        l.setDead();
    }
}
