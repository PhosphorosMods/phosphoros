package lucifertotem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lucifertotem.block.BlockWatery;
import lucifertotem.block.WateryEvents;
import lucifertotem.hillman.*;
import lucifertotem.world.LightfallChunkGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 1.1.1 self-tests: watery sea decor, Alabastron self-dose, Doctor Ammonius' voice + sound assets, distant-voice defaults. */
public class TestHarness111 {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.1.1] " + what); }

    public static void run(MinecraftServer srv, WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp, int R) throws Exception {
        WorldServer w = (WorldServer) tp.world;
        BlockPos base = new BlockPos(tp.posX, 170, tp.posZ + 24);
        try { wateryTests(w, lf, tp, base, R); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] watery tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] watery", t); }
        try { selfDoseTests(tp); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] self-dose tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] self-dose", t); }
        try { soundTests(); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] sound tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] sounds", t); }
        try { voiceTests(w, tp, base.up(12)); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] voice tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] voice", t); }
        try { animTests(w, tp, base.up(20)); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] animation tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] anim", t); }
        try { fishingTests(w); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] fishing tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] fishing", t); }
        try { breedTests(w, tp, base.up(28)); } catch (Throwable t) { TestHarness.check(false, "[1.1.1] wheat/breeding/drops tests threw " + t); LuciferTotem.LOG.error("[LT][TEST] breed", t); }
    }

    // ------------------------------------------------------------ 1) watery sea decor
    static Block[] watery() { return new Block[] {ModContent.TENDRIL, ModContent.TENDRIL_BULB, ModContent.DUSK_CORAL, ModContent.STARFISH, ModContent.CRYSTAL_CORAL}; }

    static void wateryTests(WorldServer w, WorldServer lf, EntityPlayerMP tp, BlockPos c, int R) {
        IBlockState water = Blocks.WATER.getDefaultState();
        boolean props = true; StringBuilder why = new StringBuilder();
        for (Block b : watery()) {
            IBlockState s = b.getDefaultState();
            boolean ok = s.getMaterial() == Material.WATER && s.getLightOpacity() == 3 && water.getLightOpacity() == 3 && s.getValue(BlockLiquid.LEVEL) == 0
                && b.getMetaFromState(s) == 0 && b.getStateFromMeta(0) == s && Block.getStateById(Block.getStateId(s)) == s
                && !s.isOpaqueCube() && s.getCollisionBoundingBox(w, c) == Block.NULL_AABB && Item.getItemFromBlock(b) != Items.AIR && b.getCreativeTab() == LuciferTotem.TAB;
            if (!ok) { props = false; why.append(b.getRegistryName()).append(' '); }
        }
        check(props, "tendril, bulb, dusk coral, starfish, crystal coral: Material.WATER + LEVEL 0, meta 0 (old saves load), opacity 3 like water, no collision, item in the tab " + why);

        // a glass tank of still water on stone, high in the sky
        for (BlockPos q : BlockPos.getAllInBox(c.add(-3, -1, -3), c.add(3, 5, 9))) {
            int dx = q.getX() - c.getX(), dy = q.getY() - c.getY(), dz = q.getZ() - c.getZ();
            IBlockState s = Blocks.AIR.getDefaultState();
            if (dz <= 3) {
                if (dy == -1) s = Blocks.STONE.getDefaultState();
                else if ((Math.abs(dx) == 3 || Math.abs(dz) == 3) && dy <= 4) s = Blocks.GLASS.getDefaultState();
                else if (dy <= 3) s = water;
            } else if (dy == -1) s = Blocks.STONE.getDefaultState();
            w.setBlockState(q, s, 2);
        }
        BlockPos dry = c.add(0, 0, 6);
        w.setBlockState(c.add(1, 3, 1), Blocks.STONE.getDefaultState(), 2);   // a ledge to hang crystal coral from
        BlockPos hang = c.add(1, 2, 1), mid = c.add(-1, 2, -1);
        boolean place = ModContent.TENDRIL.canPlaceBlockAt(w, c) && ModContent.STARFISH.canPlaceBlockAt(w, c) && ModContent.DUSK_CORAL.canPlaceBlockAt(w, c) && ModContent.CRYSTAL_CORAL.canPlaceBlockAt(w, c)
            && ModContent.CRYSTAL_CORAL.canPlaceBlockAt(w, hang);
        boolean refuse = !ModContent.TENDRIL.canPlaceBlockAt(w, dry) && !ModContent.STARFISH.canPlaceBlockAt(w, dry) && !ModContent.CRYSTAL_CORAL.canPlaceBlockAt(w, dry)
            && !ModContent.TENDRIL.canPlaceBlockAt(w, mid) && !ModContent.STARFISH.canPlaceBlockAt(w, hang) && !w.mayPlace(ModContent.DUSK_CORAL, dry, false, EnumFacing.UP, null)
            && w.mayPlace(ModContent.DUSK_CORAL, c, false, EnumFacing.UP, null);
        check(place && refuse, "placement: only into still water and only when supported (sea floor; crystal coral may also hang from rock); refused in air and mid-water (place=" + place + ", refuse=" + refuse + ")");

        // a stalk with a bulb; water around stays water
        w.setBlockState(c, ModContent.TENDRIL.getDefaultState(), 3);
        w.setBlockState(c.up(), ModContent.TENDRIL_BULB.getDefaultState(), 3);
        check(w.getBlockState(c).getBlock() == ModContent.TENDRIL && w.getBlockState(c.up()).getBlock() == ModContent.TENDRIL_BULB
            && source(w.getBlockState(c.east())) && source(w.getBlockState(c.up(2))), "a tendril with a bulb stands in the water; the water around it stays full water (" + w.getBlockState(c) + ", " + w.getBlockState(c.up()) + ", " + w.getBlockState(c.east()) + ", " + w.getBlockState(c.up(2)) + ")");

        // drowning / swimming inside the decor
        EntityPig pig = new EntityPig(w);
        pig.setPosition(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        w.spawnEntity(pig);
        boolean inside = pig.isInsideOfMaterial(Material.WATER), swims = pig.handleWaterMovement();
        StringBuilder each = new StringBuilder();
        boolean all = true;
        for (Block b : new Block[] {ModContent.TENDRIL, ModContent.DUSK_CORAL, ModContent.STARFISH, ModContent.CRYSTAL_CORAL}) {
            w.setBlockState(c, b.getDefaultState(), 2);
            boolean in = pig.isInsideOfMaterial(Material.WATER);
            all &= in; each.append(b.getRegistryName().getPath()).append('=').append(in).append(' ');
        }
        w.setBlockState(c, ModContent.GLOW_CRYSTAL.getDefaultState(), 2);
        boolean oldPocket = !pig.isInsideOfMaterial(Material.WATER);
        check(inside && swims && all, "a head inside the decor is under water (isInsideOfMaterial(WATER)) and the body swims (" + each + ")");
        check(oldPocket, "control: the old dry glow crystal in the same spot was a breathable air pocket");
        w.setBlockState(c, ModContent.TENDRIL.getDefaultState(), 2);
        pig.setAir(0);
        float h0 = pig.getHealth();
        for (int i = 0; i < 25; i++) { pig.hurtResistantTime = 0; pig.onEntityUpdate(); }
        check(pig.getHealth() < h0, "drowning: a pig with its head in a tendril runs out of air and takes drown damage (" + h0 + " -> " + pig.getHealth() + ")");
        pig.setDead();
        if (tp.world == w) {
            double ox = tp.posX, oy = tp.posY, oz = tp.posZ;
            tp.setPosition(c.getX() + 0.5, c.getY() + 1.5 - tp.getEyeHeight(), c.getZ() + 0.5);   // eyes in the bulb
            w.setBlockState(c.up(), ModContent.TENDRIL_BULB.getDefaultState(), 2);
            boolean pin = tp.isInsideOfMaterial(Material.WATER);
            tp.setPosition(ox, oy, oz);
            check(pin, "a player with eyes in a tendril bulb is under water (no air pocket to breathe in)");
        }

        // breaking it leaves water, and the stalk above follows (drops, becomes water)
        int items0 = w.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(c).grow(3)).size();
        IBlockState st = w.getBlockState(c);
        st.getBlock().removedByPlayer(st, w, c, tp, true);
        int items1 = w.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(c).grow(3)).size();
        check(source(w.getBlockState(c)) && source(w.getBlockState(c.up())) && items1 > items0, "broken: the cell refills with still water (no air pocket); the unsupported bulb above pops off as an item and becomes water");

        // a bucket can't scoop the decor away
        w.setBlockState(c, ModContent.DUSK_CORAL.getDefaultState(), 2);
        ActionResult<ItemStack> br = net.minecraftforge.event.ForgeEventFactory.onBucketUse(tp, w, new ItemStack(Items.BUCKET), new RayTraceResult(new Vec3d(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5), EnumFacing.UP, c));
        check(br != null && br.getType() == EnumActionResult.FAIL && w.getBlockState(c).getBlock() == ModContent.DUSK_CORAL, "an empty bucket can't scoop up the coral (it would have deleted it)");

        // old chunks: dry glow crystals under the sea become crystal coral on load
        BlockPos sea = seaSpot(lf, new BlockPos(tp.posX, 0, tp.posZ), R);
        if (sea == null) check(false, "found a sea column near the gate for the conversion test");
        else {
            lf.setBlockState(sea, ModContent.GLOW_CRYSTAL.getDefaultState(), 2);
            Chunk ch = lf.getChunk(sea);
            int n = WateryEvents.convert(lf, ch);
            check(n >= 1 && lf.getBlockState(sea).getBlock() == ModContent.CRYSTAL_CORAL, "pre-1.1.1 chunks: underwater glow crystals are turned into crystal coral (" + n + " in that chunk)");
        }

        // worldgen audit of the generated sea around the gate
        int dryCrystals = 0, airPockets = 0, decor = 0, seaCols = 0;
        java.util.Map<String, Integer> tally = new java.util.TreeMap<>();
        int SEA = LightfallChunkGenerator.SEA_LEVEL;
        int cx0 = (int) Math.floor(tp.posX) >> 4, cz0 = (int) Math.floor(tp.posZ) >> 4;
        for (int cx = cx0 - R + 1; cx < cx0 + R; cx++) for (int cz = cz0 - R + 1; cz < cz0 + R; cz++) {
            if (!lf.isChunkGeneratedAt(cx, cz)) continue;
            Chunk ch = lf.getChunk(cx, cz);
            if (!ch.isTerrainPopulated()) continue;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (ch.getBlockState(x, SEA, z).getMaterial() != Material.WATER) continue;
                seaCols++;
                for (int y = SEA - 1; y > 1; y--) {
                    IBlockState s = ch.getBlockState(x, y, z);
                    Block b = s.getBlock();
                    if (b == ModContent.GLOW_CRYSTAL) dryCrystals++;
                    if (b instanceof BlockWatery) { decor++; tally.merge(b.getRegistryName().getPath(), 1, Integer::sum); }
                    if (s.getMaterial() == Material.AIR) { airPockets++; if (airPockets <= 3) LuciferTotem.LOG.info("[LT][TEST] sea air pocket at {} {} {}", ch.x * 16 + x, y, ch.z * 16 + z); }
                    if (s.getMaterial() != Material.WATER && s.getMaterial() != Material.AIR && s.isFullCube()) break; // the floor / a structure: below it is not sea
                }
            }
        }
        LuciferTotem.LOG.info("[LT][TEST] watery sea decor tally: {} in {} sea columns", tally, seaCols);
        check(seaCols > 1000 && decor > 200 && tally.size() >= 5, "worldgen places the watery versions in the sea: " + tally);
        check(dryCrystals == 0, "no dry (air-pocket) glow crystals left under the sea surface by worldgen (" + dryCrystals + ")");
        check(airPockets == 0, "no unintended air pockets in the open sea columns (" + airPockets + ")");
    }

    /** a full water source (still or just woken up to flowing by a neighbour update, which settles back) */
    static boolean source(IBlockState s) { return s.getBlock() instanceof BlockLiquid && s.getMaterial() == Material.WATER && s.getValue(BlockLiquid.LEVEL) == 0; }

    static BlockPos seaSpot(WorldServer lf, BlockPos near, int R) {
        int SEA = LightfallChunkGenerator.SEA_LEVEL;
        for (int r = 0; r < R * 16; r += 3) for (int a = 0; a < 16; a++) {
            int x = near.getX() + (int) (Math.cos(a * Math.PI / 8) * r), z = near.getZ() + (int) (Math.sin(a * Math.PI / 8) * r);
            if (!lf.isChunkGeneratedAt(x >> 4, z >> 4)) continue;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, SEA, z);
            if (lf.getBlockState(m).getBlock() != Blocks.WATER) continue;
            while (m.getY() > 2 && lf.getBlockState(m).getBlock() == Blocks.WATER) m.setY(m.getY() - 1);
            if (m.getY() < SEA - 4 && lf.getBlockState(m).isFullCube() && lf.getBlockState(m.up(2)).getBlock() == Blocks.WATER) return m.up().toImmutable();
        }
        return null;
    }

    // ------------------------------------------------------------ 2) Alabastron self-dose
    static void selfDoseTests(EntityPlayerMP tp) {
        TestHarness110.reset(tp);
        if (Alabastron.isLodged(tp)) Alabastron.unlodge(tp);
        tp.setSneaking(true);
        TestHarness110.hold(tp, TestHarness110.lacedWith(new PotionEffect(MobEffects.SPEED, 3600, 0)));
        tp.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        int s0 = Alabastron.selfDoses;
        ActionResult<ItemStack> r = HItems.ALABASTRON.onItemRightClick(tp.world, tp, EnumHand.MAIN_HAND);
        PotionEffect sp = tp.getActivePotionEffect(MobEffects.SPEED);
        check(r.getType() == EnumActionResult.SUCCESS && Alabastron.selfDoses == s0 + 1 && sp != null && sp.getAmplifier() == 1 && sp.getDuration() == 5400,
            "sneak + use a laced alabastron: self-administered boosted dose Speed I 3:00 -> " + sp);
        check(tp.getHeldItemMainhand().isEmpty() && Alabastron.isLodged(tp) && !Alabastron.laced(Alabastron.lodgedStack(tp)), "...and the empty flask lodges on yourself");
        TestHarness110.hold(tp, TestHarness110.lacedWith(new PotionEffect(MobEffects.JUMP_BOOST, 600, 0)));
        HItems.ALABASTRON.onItemRightClick(tp.world, tp, EnumHand.MAIN_HAND);
        check(Alabastron.laced(tp.getHeldItemMainhand()) && !tp.isPotionActive(MobEffects.JUMP_BOOST), "only one fits: a second self-dose isn't used up");
        tp.setSneaking(false);
        ActionResult<ItemStack> r2 = HItems.ALABASTRON.onItemRightClick(tp.world, tp, EnumHand.MAIN_HAND);
        check(r2.getType() == EnumActionResult.PASS && Alabastron.laced(tp.getHeldItemMainhand()), "not sneaking: no self-dose");
        tp.setSneaking(true);
        ItemStack pot = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.STRENGTH);
        tp.setHeldItem(EnumHand.OFF_HAND, pot);
        ActionResult<ItemStack> r3 = HItems.ALABASTRON.onItemRightClick(tp.world, tp, EnumHand.MAIN_HAND);
        check(r3.getType() == EnumActionResult.PASS && tp.getHeldItemOffhand().getCount() == 1 && Alabastron.effects(tp.getHeldItemMainhand()).get(0).getPotion() == MobEffects.JUMP_BOOST,
            "a potion in the off hand means lacing, never self-dosing (and a laced flask isn't relaced)");
        // retrieve your own: sneak + empty main hand
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        tp.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        tp.inventory.clear();
        boolean pulled = Alabastron.pullOwn(tp);
        boolean got = false;
        for (int i = 0; i < tp.inventory.getSizeInventory(); i++) if (tp.inventory.getStackInSlot(i).getItem() == HItems.ALABASTRON) got = true;
        check(pulled && !Alabastron.isLodged(tp) && got, "sneak + use with an empty hand pulls your own flask back out");
        // lacing still works while sneaking
        TestHarness110.hold(tp, new ItemStack(HItems.ALABASTRON));
        tp.setHeldItem(EnumHand.OFF_HAND, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.REGENERATION));
        HItems.ALABASTRON.onItemRightClick(tp.world, tp, EnumHand.MAIN_HAND);
        check(Alabastron.laced(tp.getHeldItemMainhand()) && !Alabastron.isLodged(tp), "an empty flask + potion in the off hand still laces (even sneaking), no dose");
        check(net.minecraft.util.text.translation.I18n.canTranslate("tooltip.lucifertotem.alabastron.self") && net.minecraft.util.text.translation.I18n.canTranslate("msg.lucifertotem.alabastron.self"),
            "self-dose lang: tooltip hint + 'Self-administered!'");
        tp.setSneaking(false);
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ 3) sound assets
    static void soundTests() throws Exception {
        JsonObject sj;
        try (InputStream in = LuciferTotem.class.getResourceAsStream("/assets/lucifertotem/sounds.json")) {
            sj = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        int events = 0, files = 0, missing = 0, badFmt = 0, unreg = 0, empty = 0;
        StringBuilder bad = new StringBuilder();
        for (Map.Entry<String, JsonElement> e : sj.entrySet()) {
            events++;
            if (!ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(LuciferTotem.MODID, e.getKey()))) { unreg++; bad.append("unregistered:").append(e.getKey()).append(' '); }
            if (e.getValue().getAsJsonObject().getAsJsonArray("sounds").size() == 0) empty++;
            for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                JsonObject o = s.isJsonObject() ? s.getAsJsonObject() : null;
                String name = o != null ? o.get("name").getAsString() : s.getAsString();
                String type = o != null && o.has("type") ? o.get("type").getAsString() : "file";
                if (!type.equals("file") || !name.startsWith(LuciferTotem.MODID + ":")) continue;
                files++;
                String path = name.substring(name.indexOf(':') + 1);
                if (!path.equals(path.toLowerCase())) { badFmt++; bad.append("uppercase:").append(path).append(' '); }
                try (InputStream in = LuciferTotem.class.getResourceAsStream("/assets/lucifertotem/sounds/" + path + ".ogg")) {
                    if (in == null) { missing++; bad.append("missing:").append(path).append(' '); continue; }
                    byte[] h = new byte[64]; int n = 0, k;
                    while (n < 64 && (k = in.read(h, n, 64 - n)) > 0) n += k;
                    // Ogg page + Vorbis identification header: channels and sample rate
                    String tag = new String(h, 0, 4, StandardCharsets.ISO_8859_1);
                    int at = -1;
                    for (int i = 0; i < n - 7; i++) if (h[i] == 1 && h[i + 1] == 'v' && h[i + 2] == 'o' && h[i + 3] == 'r' && h[i + 4] == 'b' && h[i + 5] == 'i' && h[i + 6] == 's') { at = i; break; }
                    int ch = at < 0 ? -1 : h[at + 11] & 0xff;
                    int rate = at < 0 ? -1 : (h[at + 12] & 0xff) | (h[at + 13] & 0xff) << 8 | (h[at + 14] & 0xff) << 16 | (h[at + 15] & 0xff) << 24;
                    if (!tag.equals("OggS") || ch != 1 || rate != 44100) { badFmt++; bad.append("format:").append(path).append(" ch=").append(ch).append(" rate=").append(rate).append(' '); }
                }
            }
        }
        int ours = 0, notInJson = 0;
        for (SoundEvent se : ForgeRegistries.SOUND_EVENTS.getValuesCollection())
            if (se.getRegistryName().getNamespace().equals(LuciferTotem.MODID)) { ours++; if (!sj.has(se.getRegistryName().getPath())) { notInJson++; bad.append("not-in-json:").append(se.getRegistryName()).append(' '); } }
        check(events > 50 && unreg == 0 && notInJson == 0 && empty == 0, "sounds.json in the jar parses; all " + events + " events registered (" + ours + " lucifertotem sound events), none empty " + bad);
        check(files >= 57 && missing == 0 && badFmt == 0, "every lucifertotem sound file exists in the jar, lowercase, Ogg Vorbis mono 44.1 kHz (" + files + " refs) " + bad);
        check(HSounds.LINES.length == AmmoniusClips.NAME.length && HSounds.LINES.length >= 30 && HSounds.SQUISH_SMALL != null
            && ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(LuciferTotem.MODID, "ammonius.line." + AmmoniusClips.NAME[0])),
            "one sound event per Ammonius clip (" + HSounds.LINES.length + ") + the audible small squish");
    }

    // ------------------------------------------------------------ 4) Doctor Ammonius' voice
    static void voiceTests(WorldServer w, EntityPlayerMP tp, BlockPos at) {
        check(LTConfig.distantVoiceMinutesLightfall == 6 && LTConfig.distantVoiceMinutesOverworld == 25,
            "distant voice: ~6 min Lightfall / ~25 min Overworld (new keys distantVoiceMinutes*, so old 20/90 configs no longer hold it back)");
        check(LTConfig.ammoniusTalkTicksMin == 40 && LTConfig.ammoniusTalkTicksMax == 80 && Math.abs(LTConfig.ammoniusVolume - 1.8) < 1e-6, "Ammonius voice config: talk every 40-80 ticks, volume 1.8");
        for (BlockPos q : BlockPos.getAllInBox(at.add(-8, -1, -8), at.add(8, 3, 8))) w.setBlockState(q, q.getY() == at.getY() - 1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        double ox = tp.posX, oy = tp.posY, oz = tp.posZ;
        tp.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EntityAmmonius am = new EntityAmmonius(w);
        am.setPosition(at.getX() + 6.5, at.getY(), at.getZ() + 0.5);
        w.spawnEntity(am);
        am.setNoAI(true);
        int l0 = EntityAmmonius.linesSpoken, g0 = EntityAmmonius.greetings, rep0 = EntityAmmonius.repeats;
        for (int i = 0; i < 400; i++) { am.onLivingUpdate(); am.ticksExisted++; }
        int lines = EntityAmmonius.linesSpoken - l0;
        check(EntityAmmonius.greetings == g0 + 1, "Doctor Ammonius greets a player on first sight within 12 blocks (once)");
        check(lines >= 5 && lines <= 11 && EntityAmmonius.repeats == rep0, "an assault on the senses: " + lines + " voice lines in 20 s (every 2-4 s, never over himself), never the same clip twice in a row");
        check(Math.abs(am.getSoundVolume() - 1.8f) < 1e-4, "his voice is loud: volume " + am.getSoundVolume() + " (heard to ~29 blocks)");
        int b0 = EntityAmmonius.potionBlurts;
        boolean heard = HEvents.potionHeard(tp, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.HEALING));
        boolean purple = false;
        for (int k : AmmoniusClips.PURPLE) if (k == am.lastClip) purple = true;
        boolean water = HEvents.potionHeard(tp, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
        check(heard && purple && EntityAmmonius.potionBlurts == b0 + 1 && !water, "drinking a potion within 16 blocks makes him blurt a PURPLE line (plain water doesn't)");
        am.setDead();
        tp.setPosition(ox, oy, oz);
    }

    static void platform(WorldServer w, BlockPos at) {
        for (BlockPos q : BlockPos.getAllInBox(at.add(-9, -1, -9), at.add(9, 4, 9))) w.setBlockState(q, q.getY() == at.getY() - 1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
    }

    // ------------------------------------------------------------ 5) erratic animations
    static void animTests(WorldServer w, EntityPlayerMP tp, BlockPos at) throws Exception {
        platform(w, at);
        EntityAmmonius am = new EntityAmmonius(w);
        am.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        w.spawnEntity(am);
        // the talk trigger is a synced DataParameter
        am.getDataManager().setClean();
        int t0 = am.talkSeed();
        am.speak(AmmoniusClips.PURPLE);
        java.util.List<net.minecraft.network.datasync.EntityDataManager.DataEntry<?>> dirty = am.getDataManager().getDirty();
        boolean talkDirty = false;
        if (dirty != null) for (net.minecraft.network.datasync.EntityDataManager.DataEntry<?> d : dirty) if (d.getKey().equals(EntityAmmonius.TALK)) talkDirty = true;
        io.netty.buffer.ByteBuf raw = io.netty.buffer.Unpooled.buffer();
        net.minecraft.network.PacketBuffer buf = new net.minecraft.network.PacketBuffer(raw);
        net.minecraft.network.datasync.EntityDataManager.writeEntries(dirty, buf);
        java.util.List<net.minecraft.network.datasync.EntityDataManager.DataEntry<?>> back = net.minecraft.network.datasync.EntityDataManager.readEntries(buf);
        Object val = null;
        if (back != null) for (net.minecraft.network.datasync.EntityDataManager.DataEntry<?> d : back) if (d.getKey().getId() == EntityAmmonius.TALK.getId()) val = d.getValue();
        check(talkDirty && am.talkSeed() == t0 + 1 && am.talkKind() == AmmoniusPose.PURPLE && Integer.valueOf(am.getDataManager().get(EntityAmmonius.TALK)).equals(val),
            "voice line -> TALK DataParameter marked dirty and survives the network encode/decode (" + val + ")");
        am.ticksExisted = 500;
        am.notifyDataManagerChange(EntityAmmonius.TALK);
        check(am.talkStart == 500, "a TALK change starts the client's gesture burst (talkStart=" + am.talkStart + ")");
        // the idle state machine (server) cycles through the moods
        java.util.Set<Integer> modes = new java.util.TreeSet<>();
        boolean freezeThenLurch = false; int prev = am.animMode();
        am.setNoAI(true);
        for (int i = 0; i < 3000; i++) {
            am.onLivingUpdate(); am.ticksExisted++;
            int m = am.animMode(); modes.add(m);
            if (prev == AmmoniusPose.FREEZE && m == AmmoniusPose.LURCH) freezeThenLurch = true;
            if (prev == AmmoniusPose.FREEZE && m != AmmoniusPose.FREEZE && m != AmmoniusPose.LURCH) freezeThenLurch = false;
            prev = m;
        }
        check(modes.size() >= 5 && freezeThenLurch, "idle moods: pacing, freeze -> lurch, looking around, crouch-and-spring, lecturing to nobody; seen " + modes);
        am.setDead();
        // the pose math: finite, in range, and the signature moves happen
        java.util.Random r = new java.util.Random(7);
        boolean finite = true, spun = false, hopped = false, shudder = false, purpleUp = true;
        for (int i = 0; i < 20000; i++) {
            int kind = r.nextInt(3), seed = r.nextInt(5000), mode = r.nextInt(AmmoniusPose.MODES);
            float age = r.nextFloat() * 10000, ta = r.nextFloat() * 80 - 10, ma = r.nextFloat() * 200;
            float[] p = AmmoniusPose.compute(age, ta, kind, seed, mode, ma, r.nextInt(999));
            for (float f : p) if (Float.isNaN(f) || Float.isInfinite(f) || Math.abs(f) > 400) finite = false;
            if (Math.abs(p[AmmoniusPose.R_X]) > 3.3f || Math.abs(p[AmmoniusPose.L_X]) > 3.3f) finite = false;
            if (p[AmmoniusPose.SPIN] > 30 && p[AmmoniusPose.SPIN] < 330) spun = true;
            if (p[AmmoniusPose.HOP] > 0.2f) hopped = true;
            if (Math.abs(p[AmmoniusPose.SHUDDER]) > 2) shudder = true;
            if (kind == AmmoniusPose.PURPLE && ta > 8 && ta < 30 && !(p[AmmoniusPose.R_X] < -2.2f && p[AmmoniusPose.L_X] < -2.2f)) purpleUp = false;
        }
        check(finite && spun && hopped && shudder && purpleUp, "pose math: always finite/in range; spins, hops and shudders occur; both arms thrown up on PURPLE lines");
    }

    // ------------------------------------------------------------ 6) wheat: tempt, breed, grow; drops
    static void breedTests(WorldServer w, EntityPlayerMP tp, BlockPos at) throws Exception {
        platform(w, at);
        TestHarness110.reset(tp);
        double ox = tp.posX, oy = tp.posY, oz = tp.posZ;
        net.minecraft.world.WorldServer tw = (WorldServer) tp.world;
        boolean sameWorld = tw == w;
        if (sameWorld) tp.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EntityAmmonius a = new EntityAmmonius(w), b = new EntityAmmonius(w);
        a.setPosition(at.getX() + 4.5, at.getY(), at.getZ() + 0.5); b.setPosition(at.getX() + 5.5, at.getY(), at.getZ() + 0.5);
        w.spawnEntity(a); w.spawnEntity(b);
        check(a instanceof net.minecraft.entity.passive.EntityAnimal && a.isBreedingItem(new ItemStack(Items.WHEAT)) && !a.isBreedingItem(new ItemStack(Items.EMERALD)), "Doctor Ammonius is an animal now: wheat is his breeding item");
        // tempt
        net.minecraft.entity.ai.EntityAITempt tempt = a.tempt;
        TestHarness110.hold(tp, ItemStack.EMPTY);
        boolean notTempted = tempt != null && !tempt.shouldExecute();
        TestHarness110.hold(tp, new ItemStack(Items.WHEAT, 64));
        boolean tempted = tempt != null && tempt.shouldExecute();
        double d0 = a.getDistance(tp);
        if (tempted) { tempt.startExecuting(); for (int i = 0; i < 60; i++) { tempt.updateTask(); a.getNavigator().onUpdateNavigation(); a.getMoveHelper().onUpdateMoveHelper(); a.onLivingUpdate(); a.ticksExisted++; } }
        double d1 = a.getDistance(tp);
        check(sameWorld && notTempted && tempted && d1 < d0 - 0.5, "tempted by wheat: follows a player holding wheat (" + String.format("%.1f -> %.1f blocks", d0, d1) + "), ignores an empty hand");
        // feeding: love mode, no trades
        a.setPosition(at.getX() + 4.5, at.getY(), at.getZ() + 0.5);
        int trades0 = EntityAmmonius.trades;
        a.processInteract(tp, EnumHand.MAIN_HAND);
        b.processInteract(tp, EnumHand.MAIN_HAND);
        check(a.isInLove() && b.isInLove() && EntityAmmonius.trades == trades0 && tp.getHeldItemMainhand().getCount() == 62, "fed wheat: both in love mode, 2 wheat used, no trade triggered");
        // breeding: let the mating AI run
        tp.setPosition(at.getX() - 8.5, at.getY(), at.getZ() - 8.5);
        TestHarness110.hold(tp, ItemStack.EMPTY);
        java.util.List<EntityAmmonius> before = w.getEntitiesWithinAABB(EntityAmmonius.class, new net.minecraft.util.math.AxisAlignedBB(at).grow(12));
        EntityAmmonius baby = null;
        for (int i = 0; i < 600 && baby == null; i++) {
            a.onUpdate(); b.onUpdate(); a.ticksExisted++; b.ticksExisted++;
            for (EntityAmmonius e : w.getEntitiesWithinAABB(EntityAmmonius.class, new net.minecraft.util.math.AxisAlignedBB(at).grow(12))) if (e.isChild() && !before.contains(e)) baby = e;
        }
        check(baby != null && baby.getGrowingAge() < -20000 && !baby.canDespawnForTest(), "breeding spawns a persistent baby Doctor Ammonius (age " + (baby == null ? "-" : baby.getGrowingAge()) + ")");
        if (baby != null) {
            check(baby.width < a.width * 0.6f && baby.height < a.height * 0.6f, "the baby is half size (" + baby.width + " x " + baby.height + ")");
            ItemStack em = new ItemStack(Items.EMERALD, 5);
            TestHarness110.hold(tp, em);
            int tr = EntityAmmonius.trades;
            baby.processInteract(tp, EnumHand.MAIN_HAND);
            check(EntityAmmonius.trades == tr && tp.getHeldItemMainhand().getCount() == 5, "babies don't trade");
            java.util.List<EntityItem> it0 = w.getEntitiesWithinAABB(EntityItem.class, baby.getEntityBoundingBox().grow(3));
            baby.dropForTest(3);
            check(w.getEntitiesWithinAABB(EntityItem.class, baby.getEntityBoundingBox().grow(3), e -> !e.isDead).size() == w.getEntitiesWithinAABB(EntityItem.class, baby.getEntityBoundingBox().grow(3)).size() - 0 && w.getEntitiesWithinAABB(EntityItem.class, baby.getEntityBoundingBox().grow(3)).size() == it0.size(), "babies drop nothing");
            baby.setGrowingAge(-2);
            for (int i = 0; i < 4; i++) { baby.onLivingUpdate(); baby.ticksExisted++; }
            check(!baby.isChild() && baby.height > 1.8f, "the baby grows up (normally after ~20 min)");
            baby.setDead();
        }
        // drops (with Villager Drop if installed)
        for (EntityItem ei : w.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(at).grow(12))) ei.setDead();
        EntityAmmonius d = new EntityAmmonius(w);
        d.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 6.5); w.spawnEntity(d);
        int chunks = 0, bottles = 0, books = 0;
        for (int k = 0; k < 10; k++) {
            d.dropForTest(3);
            for (EntityItem ei : w.getEntitiesWithinAABB(EntityItem.class, d.getEntityBoundingBox().grow(3))) {
                if (ei.isDead) continue;
                ItemStack s = ei.getItem();
                if (EntityAmmonius.villagerChunk() != null && s.getItem() == EntityAmmonius.villagerChunk()) chunks += s.getCount();
                if (s.getItem() == Items.EXPERIENCE_BOTTLE) bottles += s.getCount();
                if (s.getItem() == Items.BOOK) books++;
                ei.setDead();
            }
        }
        boolean vd = net.minecraftforge.fml.common.Loader.isModLoaded("villagerdrop");
        check(bottles >= 10 && bottles <= 60 && books == 10 && (vd ? chunks >= 10 && chunks <= 60 : EntityAmmonius.villagerChunk() == null),
            "drops (looting III, x10): " + bottles + " bottles o' enchanting, " + books + " books" + (vd ? ", " + chunks + " Villager Drop chunks (villagerdrop:piece_of_villager)" : ", Villager Drop not installed: skipped, no crash"));
        d.setDead(); a.setDead(); b.setDead();
        tp.setPosition(ox, oy, oz);
        TestHarness110.reset(tp);
    }

    // ------------------------------------------------------------ 7) fishing
    static void fishingTests(WorldServer w) {
        net.minecraft.world.storage.loot.LootTable t = w.getLootTableManager().getLootTableFromLocation(net.minecraft.world.storage.loot.LootTableList.GAMEPLAY_FISHING);
        java.util.Random r = new java.util.Random(1234);
        int[] totems = new int[2], fish = new int[2], other = new int[2], total = new int[2];
        java.util.Set<String> kinds = new java.util.HashSet<>();
        for (int luck = 0; luck < 2; luck++) for (int i = 0; i < 10000; i++) {
            net.minecraft.world.storage.loot.LootContext ctx = new net.minecraft.world.storage.loot.LootContext.Builder(w).withLuck(luck * 3).build();
            for (ItemStack s : t.generateLootForPools(r, ctx)) {
                total[luck]++;
                if (s.getItem() == ModContent.TOTEM) totems[luck]++;
                else if (s.getItem() == Items.FISH) fish[luck]++;
                else other[luck]++;
                kinds.add(s.getItem().getRegistryName().getNamespace());
            }
        }
        double pct = 100.0 * totems[0] / 10000, pctL = 100.0 * totems[1] / 10000;
        LuciferTotem.LOG.info("[LT][TEST] fishing ({}): luck0 totem {}% fish {} other {} items {}; luck3 totem {}%", FishingLoot.injected, pct, fish[0], other[0], total[0], pctL);
        check(Math.abs(pct - LTConfig.totemFishingChancePercent) < 1.5 && total[0] >= 9900 && total[0] <= 10100, String.format("fishing: %.2f%% of 10k catches are a Totem of Lucifer (config %.1f%%, %s), one item per catch", pct, LTConfig.totemFishingChancePercent, FishingLoot.injected));
        check(pctL > pct && pctL < pct + 5, String.format("Luck of the Sea III raises it slightly: %.2f%%", pctL));
        check(fish[0] + other[0] > 9000 && fish[0] > 300 && (!net.minecraftforge.fml.common.Loader.isModLoaded("harvestcraft") || kinds.contains("harvestcraft")),
            "other fishing loot intact (vanilla fish " + fish[0] + ", junk/treasure/other mods' fish " + other[0] + "; item sources " + kinds + ")");
    }
}
