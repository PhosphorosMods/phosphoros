package lucifertotem;

import net.minecraft.util.math.MathHelper;

import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.mojang.authlib.GameProfile;
import lucifertotem.world.LightfallProvider;
import lucifertotem.world.LightfallStructures;
import lucifertotem.world.PortalData;
import lucifertotem.world.PortalLogic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.PrintWriter;
import java.util.*;

/** Developer self-test (config testHarness=true). Runs once at server start and logs [LT][TEST] lines. */
public class TestHarness {
    static int pass = 0, fail = 0;
    static final List<String> deaths = new ArrayList<>();

    static void check(boolean ok, String what) {
        if (ok) pass++; else fail++;
        LuciferTotem.LOG.info("[LT][TEST] {} {}", ok ? "PASS" : "FAIL", what);
    }

    public static class DeathListener {
        @SubscribeEvent
        public void onDeath(LivingDeathEvent e) {
            deaths.add(e.getEntityLiving().getClass().getSimpleName() + ":" + e.getSource().getDamageType());
        }
    }

    static class NullNetHandler extends NetHandlerPlayServer {
        NullNetHandler(MinecraftServer s, EntityPlayerMP p) { super(s, new NetworkManager(EnumPacketDirection.SERVERBOUND), p); }
        @Override public void sendPacket(Packet<?> packet) { }
    }

    static class DummyContainer extends Container {
        @Override public boolean canInteractWith(EntityPlayer p) { return false; }
    }

    public static void start() {
        try { run(); } catch (Throwable t) { fail++; pendingFinish = null; LuciferTotem.LOG.error("[LT][TEST] FAIL exception", t); }
        if (pendingFinish != null) { LuciferTotem.LOG.info("[LT][TEST] deferring natural-spawn checks by 100 ticks"); MinecraftForge.EVENT_BUS.register(new Ticker()); }
        else LuciferTotem.LOG.info("[LT][TEST] SUMMARY pass={} fail={}", pass, fail);
    }

    static void run() throws Exception {
        MinecraftServer srv = FMLCommonHandler.instance().getMinecraftServerInstance();
        MinecraftForge.EVENT_BUS.register(new DeathListener());
        WorldServer ow = srv.getWorld(0);
        int dim = LuciferTotem.dimId;

        // ---- 1. dimension
        check(LuciferTotem.dimRegistered && DimensionManager.isDimensionRegistered(dim), "dimension " + dim + " registered, type=" + DimensionManager.getProviderType(dim));
        WorldServer lf = srv.getWorld(dim);
        check(lf != null && lf.provider instanceof LightfallProvider, "Lightfall world loads: " + (lf == null ? null : lf.provider.getClass().getName()));
        check(lf.getBiome(new BlockPos(0, 80, 0)) == ModContent.BIOME, "biome is lightfall: " + lf.getBiome(new BlockPos(0, 80, 0)).getRegistryName());
        // 1.0.1: purple sea + localization
        check(ModContent.BIOME.getWaterColorMultiplier() == lucifertotem.world.LightfallBiome.WATER_COLOR && ModContent.BIOME.getWaterColorMultiplier() == 0xA040FF, "Lightfall biome water colour #" + Integer.toHexString(ModContent.BIOME.getWaterColorMultiplier()));
        {
            java.util.Set<String> seen = new java.util.TreeSet<>();
            for (int bx = -256; bx <= 256; bx += 32) for (int bz = -256; bz <= 256; bz += 32) seen.add(String.valueOf(lf.getBiome(new BlockPos(bx, 40, bz)).getRegistryName()));
            check(seen.size() == 1 && seen.contains("lucifertotem:lightfall"), "every sampled Lightfall column (sea + islands) uses our biome: " + seen);
        }
        {
            java.io.File src = net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(LuciferTotem.MODID).getSource();
            if (src.isFile()) try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(src)) {
                check(z.getEntry("assets/lucifertotem/lang/en_us.lang") != null, "jar " + src.getName() + " has assets/lucifertotem/lang/en_us.lang");
                java.util.zip.ZipEntry pm = z.getEntry("pack.mcmeta");
                String js = pm == null ? "" : new java.util.Scanner(z.getInputStream(pm), "UTF-8").useDelimiter("\\A").next();
                check(js.replace(" ", "").contains("\"pack_format\":3"), "jar has pack.mcmeta with pack_format 3 (else Forge wraps it in LegacyV2Adapter, which asks for en_US.lang)");
                check(z.getEntry("assets/lucifertotem/textures/blocks/lightfall_water_still.png") != null && z.getEntry("assets/lucifertotem/textures/misc/lightfall_underwater.png") != null, "jar has the violet water textures");
            }
            else check(false, "mod source is not a jar: " + src);
        }
        {
            java.util.List<String> missing = new java.util.ArrayList<>();
            for (net.minecraft.item.Item it : net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS)
                if (LuciferTotem.MODID.equals(it.getRegistryName().getNamespace())) { String k = it.getTranslationKey(new ItemStack(it)) + ".name"; if (!net.minecraft.util.text.translation.I18n.canTranslate(k)) missing.add(k); }
            for (net.minecraft.block.Block bl : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS)
                if (LuciferTotem.MODID.equals(bl.getRegistryName().getNamespace())) { String k = bl.getTranslationKey() + ".name"; if (!net.minecraft.util.text.translation.I18n.canTranslate(k)) missing.add(k); }
            for (net.minecraftforge.fml.common.registry.EntityEntry ee : net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES)
                if (LuciferTotem.MODID.equals(ee.getRegistryName().getNamespace())) { String k = "entity." + ee.getName() + ".name"; if (!net.minecraft.util.text.translation.I18n.canTranslate(k)) missing.add(k); }
            if (!net.minecraft.util.text.translation.I18n.canTranslate("itemGroup." + LuciferTotem.MODID)) missing.add("itemGroup." + LuciferTotem.MODID);
            check(missing.isEmpty(), "every registered lucifertotem item/block/entity + creative tab has a lang key; missing=" + missing);
            check("Totem of Lucifer".equals(net.minecraft.util.text.translation.I18n.translateToLocal("item.lucifertotem.totem_of_lucifer.name")) && "Bottle of Theriac".equals(net.minecraft.util.text.translation.I18n.translateToLocal("item.lucifertotem.theriac.name")) && "Priapus Totem".equals(net.minecraft.util.text.translation.I18n.translateToLocal("item.lucifertotem.priapus_totem.name")), "core item names translate to title case");
        }
        check(!lf.isRaining() || lf.getRainStrength(1f) == 0f, "no rain strength in Lightfall: " + lf.getRainStrength(1f));

        // ---- 2. generation
        int R = Integer.getInteger("lt.radius", 20);
        long t0 = System.currentTimeMillis();
        int n = 0;
        for (int cx = -R; cx <= R; cx++) for (int cz = -R; cz <= R; cz++) { lf.getChunkProvider().provideChunk(cx, cz); n++; }
        long dt = System.currentTimeMillis() - t0;
        LuciferTotem.LOG.info("[LT][TEST] generated {} chunks in {} ms ({} ms/chunk)", n, dt, String.format("%.2f", dt / (double) n));
        int oow = LightfallStructures.outOfWindowWrites;
        check(oow == 0, "no structure block writes outside the populate window: " + oow);
        LuciferTotem.LOG.info("[LT][TEST] structure counts: {}", LightfallStructures.COUNTS);
        check(LightfallStructures.COUNTS.getOrDefault("fallen_spire", 0) > 0, "fallen spires generated");
        check(LightfallStructures.COUNTS.getOrDefault("sunken_shrine", 0) > 0, "sunken shrines generated");
        check(LightfallStructures.COUNTS.getOrDefault("light_shrine", 0) > 0, "light shrines generated");
        check(LightfallStructures.COUNTS.getOrDefault("morningstar_citadel", 0) > 0, "morningstar citadels generated");
        LuciferTotem.LOG.info("[LT][TEST] sea feature counts: {}", lucifertotem.world.LightfallSea.COUNTS);
        check(lucifertotem.world.LightfallSea.outOfWindowWrites == 0, "no sea block writes outside the populate window: " + lucifertotem.world.LightfallSea.outOfWindowWrites);
        for (String k : new String[] {"drowned_choir", "giant_winged_skeleton", "fallen_angel_statue", "floating_halo", "whirlpool_spiral", "bone_reef", "upside_down_tree"})
            check(lucifertotem.world.LightfallSea.COUNTS.getOrDefault(k, 0) > 0, "sea feature generated: " + k + " x" + lucifertotem.world.LightfallSea.COUNTS.getOrDefault(k, 0));

        Map<String, Integer> spawners = new TreeMap<>(), chests = new TreeMap<>();
        int unresolved = 0;
        int badSpawner = 0, blocksGrass = 0, blocksCrystal = 0, blocksFeather = 0, water = 0, trees = 0;
        for (int cx = -R; cx <= R; cx++) for (int cz = -R; cz <= R; cz++) {
            Chunk c = lf.getChunk(cx, cz);
            for (TileEntity te : c.getTileEntityMap().values()) {
                if (te instanceof TileEntityMobSpawner) {
                    NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
                    String id = tag.getCompoundTag("SpawnData").getString("id");
                    spawners.merge(id, 1, Integer::sum);
                    if (id.equals(AetherCompat.VALKYRIE.toString())) valkSpawners.add(te.getPos());
                    if (!id.isEmpty() && !ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(id))) unresolved++;
                    if (id.isEmpty() || id.equals("minecraft:pig")) badSpawner++;
                } else if (te instanceof TileEntityChest) {
                    NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
                    chests.merge(tag.getString("LootTable"), 1, Integer::sum);
                }
            }
        }
        LuciferTotem.LOG.info("[LT][TEST] spawners by mob: {}", spawners);
        LuciferTotem.LOG.info("[LT][TEST] chests by loot table: {}", chests);
        Map<String, Integer> seaBlocks = new TreeMap<>();
        for (int cx = -R + 1; cx < R; cx++) for (int cz = -R + 1; cz < R; cz++) {
            Chunk c = lf.getChunk(cx, cz);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y < 60; y++) {
                net.minecraft.block.Block b = c.getBlockState(x, y, z).getBlock();
                if (b == ModContent.TENDRIL || b == ModContent.TENDRIL_BULB || b == ModContent.DUSK_CORAL || b == ModContent.STARFISH || b == ModContent.WATCHER || b == ModContent.HALO_BRASS || b == Blocks.BONE_BLOCK || b == ModContent.GLOW_CRYSTAL)
                    seaBlocks.merge(b.getRegistryName().getPath(), 1, Integer::sum);
            }
        }
        LuciferTotem.LOG.info("[LT][TEST] sea block tally (y<60): {}", seaBlocks);
        check(seaBlocks.getOrDefault("tendril_bulb", 0) > 100 && seaBlocks.getOrDefault("watcher_eye", 0) > 10 && seaBlocks.getOrDefault("starfish", 0) > 100, "weird sea flora present");
        check(unresolved == 0, "every spawner mob id resolves in the entity registry (" + unresolved + " unresolved)");
        check(AetherCompat.active() == net.minecraftforge.fml.common.Loader.isModLoaded("aether_legacy"), "Aether compat active=" + AetherCompat.active() + " (aether loaded=" + net.minecraftforge.fml.common.Loader.isModLoaded("aether_legacy") + ")");
        if (AetherCompat.active()) {
            check(!valkSpawners.isEmpty(), "Valkyrie spawners placed in Fallen Spires / Citadels: " + valkSpawners.size());
            valkyrieSpawnerTest(lf);
        } else check(!spawners.containsKey("aether_legacy:valkyrie"), "no Valkyrie spawners without the Aether");
        Set<String> allowed = new HashSet<>(Arrays.asList("aether_legacy:valkyrie", "minecraft:guardian", "minecraft:skeleton", "minecraft:stray", "minecraft:vex", "minecraft:witch", "minecraft:zombie", "minecraft:enderman", "minecraft:blaze", "lucifertotem:enchantress"));
        check(!spawners.isEmpty() && badSpawner == 0 && allowed.containsAll(spawners.keySet()), "all spawners have intended mob ids: " + spawners.keySet());

        // map dump (top block per column) for the preview image
        int min = -R * 16, max = R * 16 + 15;
        try (PrintWriter pw = new PrintWriter("lightfall_map.txt")) {
            pw.println(min + " " + max);
            for (int z = min; z <= max; z++) {
                StringBuilder sb = new StringBuilder();
                for (int x = min; x <= max; x++) {
                    int y = 200; IBlockState s = null;
                    for (; y > 0; y--) { s = lf.getBlockState(new BlockPos(x, y, z)); if (s.getMaterial() != net.minecraft.block.material.Material.AIR) break; }
                    char ch = '?';
                    if (s.getBlock() == ModContent.STAR_GRASS) { ch = 'g'; blocksGrass++; }
                    else if (s.getBlock() == ModContent.DUSKSTONE) ch = 's';
                    else if (s.getBlock() == ModContent.DUSKSTONE_BRICKS) ch = 'b';
                    else if (s.getBlock() == ModContent.GLOW_CRYSTAL) { ch = 'c'; blocksCrystal++; }
                    else if (s.getBlock() == ModContent.FALLEN_FEATHER) { ch = 'f'; blocksFeather++; }
                    else if (s.getBlock() == Blocks.WATER) { ch = 'w'; water++; }
                    else if (s.getBlock() == Blocks.LEAVES) { ch = 'l'; trees++; }
                    else if (s.getBlock() == Blocks.LOG) ch = 'o';
                    else if (s.getBlock() == Blocks.PURPUR_BLOCK || s.getBlock() == Blocks.PURPUR_PILLAR) ch = 'p';
                    else if (s.getBlock() == Blocks.CHEST) ch = 'C';
                    else if (s.getBlock() == Blocks.MOB_SPAWNER) ch = 'S';
                    sb.append(ch).append((char) ('0' + Math.max(0, Math.min(74, y / 3))));
                }
                pw.println(sb);
            }
        }
        LuciferTotem.LOG.info("[LT][TEST] surface tally: grass={} crystal={} feather={} water={} leaves={}", blocksGrass, blocksCrystal, blocksFeather, water, trees);
        check(blocksGrass > 1000 && water > 1000, "islands and sea both present");

        // loot tables load & roll
        for (ResourceLocation rl : new ResourceLocation[] {LuciferTotem.LOOT_LIGHT_SHRINE, LuciferTotem.LOOT_SPIRE, LuciferTotem.LOOT_CRYPT, LuciferTotem.LOOT_CITADEL, LuciferTotem.LOOT_CHOIR}) {
            LootTable t = lf.getLootTableManager().getLootTableFromLocation(rl);
            List<ItemStack> items = t.generateLootForPools(new Random(42), new LootContext.Builder(lf).build());
            StringBuilder sb = new StringBuilder();
            for (ItemStack s : items) sb.append(s.getCount()).append("x").append(s.getItem().getRegistryName()).append(s.isItemEnchanted() || s.getItem() == Items.ENCHANTED_BOOK ? "(ench)" : "").append(" ");
            check(t != LootTable.EMPTY_LOOT_TABLE && !items.isEmpty(), "loot table " + rl + " rolls: " + sb);
        }

        // recipes
        for (String r : new String[] {"totem_of_lucifer", "totem_of_lucifer_shards"}) {
            IRecipe rec = CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, r));
            check(rec != null && rec.getRecipeOutput().getItem() == ModContent.TOTEM, "recipe " + r + " registered -> " + (rec == null ? null : rec.getRecipeOutput()));
        }
        InventoryCrafting inv = new InventoryCrafting(new DummyContainer(), 3, 3);
        ItemStack G = new ItemStack(Blocks.GOLD_BLOCK), O = new ItemStack(Blocks.OBSIDIAN), E = new ItemStack(Items.ENDER_EYE), N = new ItemStack(Items.NETHER_STAR);
        ItemStack[] grid = {G, E, G, O, N, O, G, O, G};
        for (int i = 0; i < 9; i++) inv.setInventorySlotContents(i, grid[i].copy());
        ItemStack out = CraftingManager.findMatchingResult(inv, ow);
        check(out.getItem() == ModContent.TOTEM, "crafting grid (gold blocks/eye/obsidian/nether star) -> " + out);

        // ---- 3. combat: thrower = fake player
        EntityPlayerMP fp = FakePlayerFactory.get(ow, new GameProfile(UUID.nameUUIDFromBytes("lt_thrower".getBytes()), "lt_thrower"));
        new NullNetHandler(srv, fp);
        BlockPos sp = ow.getSpawnPoint();
        int gy = ow.getHeight(sp).getY();
        fp.setPosition(sp.getX() + 0.5, gy, sp.getZ() + 0.5);

        EntityZombie z = new EntityZombie(ow);
        z.setPosition(sp.getX() + 6.5, gy, sp.getZ() + 0.5);
        ow.spawnEntity(z);
        int boltsBefore = count(ow, EntityPurpleBolt.class);
        int vanillaBefore = ow.weatherEffects.size();
        hit(fp, z);
        check(z.isDead || z.getHealth() <= 0, "purple bolt kills a zombie (health " + z.getHealth() + ")");
        check(deaths.contains("EntityZombie:lucifer"), "zombie death source is 'lucifer': " + deaths);
        check(count(ow, EntityPurpleBolt.class) > boltsBefore, "EntityPurpleBolt spawned");
        check(ow.weatherEffects.size() == vanillaBefore && !EntityLightningBolt.class.isAssignableFrom(EntityPurpleBolt.class), "no vanilla EntityLightningBolt involved");
        BlockPos zp = new BlockPos(z);
        boolean fire = false;
        for (BlockPos q : BlockPos.getAllInBox(zp.add(-2, -2, -2), zp.add(2, 2, 2))) if (ow.getBlockState(q).getBlock() == Blocks.FIRE) fire = true;
        check(!fire, "no fire placed around the strike");

        EntityWolf wolf = new EntityWolf(ow);
        wolf.setPosition(sp.getX() + 0.5, gy, sp.getZ() + 6.5);
        ow.spawnEntity(wolf);
        wolf.setTamedBy(fp);
        float wh = wolf.getHealth();
        hit(fp, wolf);
        check(!wolf.isDead && wolf.getHealth() == wh, "tamed wolf unharmed (no friendly fire)");
        float ph = fp.getHealth();
        hit(fp, fp);
        check(!fp.isDead && fp.getHealth() == ph, "thrower/player unharmed");

        EntityWither wither = new EntityWither(ow);
        wither.setPosition(sp.getX() - 8.5, gy + 4, sp.getZ() + 0.5);
        ow.spawnEntity(wither);
        float bh = wither.getHealth();
        hit(fp, wither);
        check(!wither.isDead && wither.getHealth() < bh && wither.getHealth() > 0, "boss (wither) takes heavy damage but survives: " + bh + " -> " + wither.getHealth());
        wither.setDead();

        // ---- 4. villager -> gate
        int gatesBefore = PortalData.get().gates.size();
        EntityVillager v = new EntityVillager(ow);
        BlockPos vp = new BlockPos(sp.getX() + 20, gy, sp.getZ() + 20);
        v.setPosition(vp.getX() + 0.5, ow.getHeight(vp).getY(), vp.getZ() + 0.5);
        ow.spawnEntity(v);
        hit(fp, v);
        check(v.isDead || v.getHealth() <= 0, "villager smitten");
        PortalData d = PortalData.get();
        check(d.gates.size() == gatesBefore + 1, "gate registered (" + d.gates.size() + ")");
        PortalData.Gate g = d.gates.get(d.gates.size() - 1);
        check(g.dim == 0 && PortalLogic.gateIntact(ow, g.base, g.axis), "gate built in overworld near villager: " + g + " dist=" + Math.sqrt(g.base.distanceSq(vp)));

        // ---- 5. teleport with a test player
        EntityPlayerMP tp = new EntityPlayerMP(srv, ow, new GameProfile(UUID.nameUUIDFromBytes("lt_traveller".getBytes()), "lt_traveller"), new PlayerInteractionManager(ow));
        new NullNetHandler(srv, tp);
        BlockPos inPortal = PortalLogic.at(g.base, g.axis, 1, 1, 0);
        tp.setPosition(inPortal.getX() + 0.5, inPortal.getY(), inPortal.getZ() + 0.5);
        ow.spawnEntity(tp);
        ow.getPlayerChunkMap().addPlayer(tp);
        boolean ok = PortalLogic.enterPortal(tp, inPortal);
        check(ok && tp.dimension == dim && tp.world == lf, "traveller teleported to the Lightfall (dim now " + tp.dimension + ") at " + tp.getPosition());
        BlockPos feet = tp.getPosition();
        check(lf.getBlockState(feet.down()).getMaterial().isSolid() && lf.isAirBlock(feet) && lf.isAirBlock(feet.up()), "arrival is safe: below=" + lf.getBlockState(feet.down()).getBlock().getRegistryName() + " feet/head air");
        PortalData.Gate g2 = d.findExact(dim, g.lbase);
        check(g.linked && g.ldim == dim && g2 != null && g2.linked && g2.ldim == 0 && g2.lbase.equals(g.base) && PortalLogic.gateIntact(lf, g2.base, g2.axis), "linked return gate in Lightfall: " + g2);
        // walk back through
        tp.timeUntilPortal = 0;
        BlockPos back = PortalLogic.at(g2.base, g2.axis, 2, 2, 0);
        ok = PortalLogic.enterPortal(tp, back);
        double dist = Math.sqrt(tp.getPosition().distanceSq(g.base));
        check(ok && tp.dimension == 0 && dist < 6, "return trip lands at the original gate (dim " + tp.dimension + ", dist " + String.format("%.1f", dist) + ")");
        check(d.gates.size() == gatesBefore + 2, "no duplicate gates created on return (" + d.gates.size() + ")");
        BlockPos f2 = tp.getPosition();
        check(ow.getBlockState(f2.down()).getMaterial().isSolid() && ow.isAirBlock(f2), "overworld arrival safe");
        // second trip re-uses the same linked gate
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, inPortal);
        check(tp.dimension == dim && d.gates.size() == gatesBefore + 2 && tp.getPosition().distanceSq(g2.base) < 36, "second trip re-uses linked gate");
        medusaTests(srv, lf, tp, fp);
        landMedusaTests(lf, tp, fp);
        try { TestHarness110.run(srv, ow, lf, tp, fp, R); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception in 1.1.0 tests", t); }
        try { TestHarness111.run(srv, lf, tp, fp, R); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception in 1.1.1 tests", t); }
        try { TestHarness120.run(srv, ow, lf, tp); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception in 1.2.0 tests", t); }
        try { TestHarness121.run(srv, ow, lf, tp); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception in 1.2.1 tests", t); }
        try { TestHarness130.run(srv, ow, lf, tp, fp, R); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception in 1.3.0 tests", t); }
        final PortalData dd = d; final PortalData.Gate gg = g; final BlockPos bk = back; final EntityPlayerMP ftp = tp;
        medusaBaseline = count(lf, EntityMedusa.class); squidBaseline = count(lf, net.minecraft.entity.passive.EntitySquid.class);
        pendingFinish = () -> finishGateTests(srv, ow, lf, dd, gg, bk, ftp);
    }

    static java.util.List<BlockPos> valkSpawners = new java.util.ArrayList<>();

    /** the spawner's own rules (Forge CheckSpawn + getCanSpawnHere + collision) accept a Valkyrie around each Valkyrie spawner */
    static void valkyrieSpawnerTest(WorldServer lf) {
        int ok = 0;
        java.util.Random rnd = new java.util.Random(1);
        for (BlockPos sp : valkSpawners) {
            TileEntity te = lf.getTileEntity(sp);
            net.minecraft.tileentity.MobSpawnerBaseLogic logic = ((TileEntityMobSpawner) te).getSpawnerBaseLogic();
            boolean any = false;
            for (int i = 0; i < 300 && !any; i++) {
                Entity e = net.minecraft.entity.EntityList.createEntityByIDFromName(AetherCompat.VALKYRIE, lf);
                double x = sp.getX() + (rnd.nextDouble() - rnd.nextDouble()) * 4 + 0.5, y = sp.getY() + rnd.nextInt(3) - 1, z = sp.getZ() + (rnd.nextDouble() - rnd.nextDouble()) * 4 + 0.5;
                e.setLocationAndAngles(x, y, z, 0, 0);
                if (net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner((net.minecraft.entity.EntityLiving) e, lf, (float) x, (float) y, (float) z, logic)) any = true;
            }
            if (any) ok++;
        }
        check(ok * 10 >= valkSpawners.size() * 7, "Valkyrie spawners can actually spawn (dark, 2-high room): " + ok + "/" + valkSpawners.size());
    }

    /** natural spawns + behaviour outside the Aether (runs with live chunks) */
    static void valkyrieLiveTests(WorldServer lf, EntityPlayerMP tp) {
        double chance = LTConfig.valkyrieSpawnChance;
        LTConfig.valkyrieSpawnChance = 1.0; // only to make the rare spawn observable in a short test
        net.minecraft.world.WorldEntitySpawner sp = new net.minecraft.world.WorldEntitySpawner();
        for (int i = 0; i < 40; i++) sp.findChunksForSpawning(lf, true, false, false);
        LTConfig.valkyrieSpawnChance = chance;
        java.util.List<Entity> natural = new java.util.ArrayList<>();
        int bad = 0;
        for (Entity e : lf.loadedEntityList) if (AetherCompat.isValkyrie(e) && e.getTags().contains(AetherCompat.NATURAL_TAG)) {
            natural.add(e);
            BlockPos p = new BlockPos(e);
            net.minecraft.block.Block b = lf.getBlockState(p.down()).getBlock();
            if (!(b == ModContent.STAR_GRASS || b == ModContent.DUSKSTONE || b == ModContent.DUSKSTONE_BRICKS) || !lf.canSeeSky(p)) bad++;
        }
        check(!natural.isEmpty() && bad == 0, "Valkyries spawn naturally on Lightfall island tops: " + natural.size() + " (" + bad + " misplaced)");
        int crowd = 0;
        for (Entity a : natural) { int n = 0; for (Entity b : natural) if (a.getDistanceSq(b) < 64 * 64) n++; crowd = Math.max(crowd, n); }
        check(crowd <= LTConfig.valkyrieLocalCap + 1, "natural Valkyries respect the local cap (max " + crowd + " within 64 blocks, cap " + LTConfig.valkyrieLocalCap + ")");
        // behaviour: a fresh Valkyrie next to an unprovoking player stays calm and stable for 300 ticks
        net.minecraft.entity.EntityLiving v = (net.minecraft.entity.EntityLiving) net.minecraft.entity.EntityList.createEntityByIDFromName(AetherCompat.VALKYRIE, lf);
        v.setLocationAndAngles(tp.posX + 3, tp.posY, tp.posZ, 0, 0);
        v.onInitialSpawn(lf.getDifficultyForLocation(new BlockPos(v)), null);
        lf.spawnEntity(v);
        tp.setHealth(20f);
        for (int i = 0; i < 300 && !v.isDead; i++) { v.onUpdate(); v.ticksExisted++; }
        check(!v.isDead && v.getAttackTarget() == null && tp.getHealth() == 20f, "Valkyrie is neutral to an unprovoking player in the Lightfall (dead=" + v.isDead + ", target=" + v.getAttackTarget() + ")");
        // despawn handling: our natural ones may despawn when nobody is near, spawner/unknown ones are left alone
        net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn ev = new net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn(v);
        MinecraftForge.EVENT_BUS.post(ev);
        check(ev.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT, "untagged (spawner) Valkyrie is not force-despawned");
        v.addTag(AetherCompat.NATURAL_TAG);
        ev = new net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn(v);
        MinecraftForge.EVENT_BUS.post(ev);
        check(ev.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT, "natural Valkyrie stays while a player is within 48 blocks");
        v.setPosition(v.posX + 200, v.posY, v.posZ);
        ev = new net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn(v);
        MinecraftForge.EVENT_BUS.post(ev);
        check(ev.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW, "natural Valkyrie may despawn with nobody near");
        v.setPosition(tp.posX + 3, tp.posY, tp.posZ);
        // the Totem of Lucifer still judges a Valkyrie (they ignore non-player damage, so the bolt falls back to the thrower)
        hit(tp, v);
        check(v.isDead || v.getHealth() <= 0, "Totem of Lucifer kills a Valkyrie (health " + v.getHealth() + ")");
        for (Entity e : natural) e.setDead();
    }

    static int medusaBaseline, squidBaseline;
    static Runnable pendingFinish;
    static int deferredTicks;
    public static class Ticker {
        @SubscribeEvent
        public void tick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent e) {
            if (e.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || pendingFinish == null) return;
            if (++deferredTicks < 100) return;
            Runnable r = pendingFinish; pendingFinish = null;
            MinecraftForge.EVENT_BUS.unregister(this);
            try { r.run(); } catch (Throwable t) { fail++; LuciferTotem.LOG.error("[LT][TEST] FAIL exception (deferred)", t); }
            LuciferTotem.LOG.info("[LT][TEST] SUMMARY pass={} fail={}", pass, fail);
        }
    }

    /** Runs after ~100 real server ticks so the PlayerChunkMap around the traveller is live (natural spawning needs it). */
    static void finishGateTests(MinecraftServer srv, WorldServer ow, WorldServer lf, PortalData d, PortalData.Gate g, BlockPos back, EntityPlayerMP tp) {
        int before = medusaBaseline, squidBefore = squidBaseline; // counted before the 100 live ticks (the world spawns on its own too)
        net.minecraft.world.WorldEntitySpawner sp = new net.minecraft.world.WorldEntitySpawner();
        int spawned = 0;
        for (int i = 0; i < 60; i++) spawned += sp.findChunksForSpawning(lf, false, true, false);
        int medusae = count(lf, EntityMedusa.class) - before, squids = count(lf, net.minecraft.entity.passive.EntitySquid.class) - squidBefore;
        check(medusae > 0, "medusae spawn naturally in the Lightfall sea: +" + medusae + " (squid +" + squids + "; extra manual cycles spawned " + spawned + ", WATER_CREATURE count now " + lf.countEntities(net.minecraft.entity.EnumCreatureType.WATER_CREATURE, true) + ")");
        int notWater = 0, monsters = 0, total = 0;
        for (Entity e : lf.loadedEntityList) {
            if (e instanceof EntityMedusa) { total++; if (!e.isInWater()) notWater++; }
            if (e instanceof net.minecraft.entity.monster.IMob) monsters++;
        }
        check(notWater == 0, "all medusae are in water (" + notWater + " of " + total + " not)");
        check(total <= 3 * 289, "medusa population respects the per-chunk cap (" + total + ")");
        LuciferTotem.LOG.info("[LT][TEST] hostile mobs present in Lightfall after spawn cycles: {}", monsters);
        {
            int lmBefore = count(lf, EntityLandMedusa.class);
            double ch = LTConfig.landMedusaSpawnChance;
            LTConfig.landMedusaSpawnChance = 1.0; // the real rate is low (2%); raise it only to observe spawns in a short test
            net.minecraft.world.WorldEntitySpawner ws = new net.minecraft.world.WorldEntitySpawner();
            for (int i = 0; i < 40; i++) ws.findChunksForSpawning(lf, true, false, false);
            LTConfig.landMedusaSpawnChance = ch;
            int lm = 0, wrong = 0;
            for (Entity e : lf.loadedEntityList) if (e instanceof EntityLandMedusa && !e.isDead) {
                lm++;
                net.minecraft.block.Block b = lf.getBlockState(new BlockPos(e).down()).getBlock();
                if (e.isInWater() || !(b == ModContent.STAR_GRASS || b == ModContent.DUSKSTONE)) wrong++;
            }
            check(lm - lmBefore > 0 && wrong == 0, "land medusae spawn naturally on Lightfall islands: +" + (lm - lmBefore) + " (" + wrong + " in water / off-island)");
            for (Entity e : lf.loadedEntityList) if (e instanceof EntityLandMedusa) e.setDead();
        }
        if (AetherCompat.active()) valkyrieLiveTests(lf, tp);
        tp.timeUntilPortal = 0;
        PortalLogic.enterPortal(tp, back);
        // frame break collapses portal
        BlockPos fr = PortalLogic.at(g.base, g.axis, 0, 2, 0);
        ow.setBlockState(fr, Blocks.AIR.getDefaultState(), 3);
        check(!PortalLogic.gateIntact(ow, g.base, g.axis), "breaking the frame collapses the gate");
        ow.getPlayerChunkMap().removePlayer(tp);
        tp.world.removeEntity(tp);
        d.markDirty();
    }

    static int theriacDrops = 0, landDeaths = 0;
    public static class LandDropListener {
        @SubscribeEvent
        public void drops(net.minecraftforge.event.entity.living.LivingDropsEvent e) {
            if (!(e.getEntityLiving() instanceof EntityLandMedusa)) return;
            landDeaths++;
            for (net.minecraft.entity.item.EntityItem it : e.getDrops()) if (it.getItem().getItem() == ModContent.THERIAC) theriacDrops++;
            e.setCanceled(true);
        }
    }

    static void landMedusaTests(WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp) throws Exception {
        net.minecraft.util.ResourceLocation key = net.minecraft.entity.EntityList.getKey(EntityLandMedusa.class);
        check(key != null && net.minecraft.entity.EntityList.ENTITY_EGGS.containsKey(key), "land medusa registered with spawn egg: " + key);
        check(net.minecraft.util.text.translation.I18n.translateToLocal("entity.lucifertotem.land_medusa.name").equals("Land Medusa"), "land medusa lang name");
        // find an island top near the gate
        BlockPos top = null;
        for (int r = 2; r < 40 && top == null; r++) for (int dx = -r; dx <= r && top == null; dx++) for (int dz = -r; dz <= r && top == null; dz += (Math.abs(dx) == r ? 1 : 2 * r)) {
            BlockPos h = lf.getHeight(tp.getPosition().add(dx, 0, dz));
            if (lf.getBlockState(h.down()).getBlock() == ModContent.STAR_GRASS && lf.isAirBlock(h.up())) top = h;
        }
        check(top != null, "found an island top: " + top);
        double ch = LTConfig.landMedusaSpawnChance;
        LTConfig.landMedusaSpawnChance = 1.0;
        EntityLandMedusa probe = new EntityLandMedusa(lf);
        probe.setPosition(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        boolean onIsland = probe.getCanSpawnHere();
        BlockPos wp = findWater(lf, tp.getPosition());
        probe.setPosition(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5);
        boolean inSea = probe.getCanSpawnHere();
        probe.setPosition(top.getX() + 0.5, top.getY() + 12, top.getZ() + 0.5);
        boolean inAir = probe.getCanSpawnHere();
        LTConfig.landMedusaSpawnChance = ch;
        check(onIsland && !inSea && !inAir, "land medusa spawn rules: island top=" + onIsland + ", sea=" + inSea + ", mid-air=" + inAir);

        // neutral, then fights back with goo
        EntityLandMedusa m = new EntityLandMedusa(lf);
        m.setLocationAndAngles(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
        m.onInitialSpawn(lf.getDifficultyForLocation(top), null);
        lf.spawnEntity(m);
        tp.setHealth(20f);
        tp.clearInvulnerableDimensionChange();
        for (int i = 0; i < 200 && !m.isDead; i++) { m.onUpdate(); m.ticksExisted++; }
        check(!m.isDead && m.getAttackTarget() == null && tp.getHealth() == 20f, "land medusa is neutral when unprovoked (target=" + m.getAttackTarget() + ")");
        m.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(tp), 1f);
        for (int i = 0; i < 10; i++) { m.onUpdate(); m.ticksExisted++; }
        check(m.getAttackTarget() == tp, "land medusa fights back when struck (target=" + m.getAttackTarget() + ")");
        tp.hurtResistantTime = 0; tp.setHealth(20f); tp.clearActivePotions();
        m.attackEntityAsMob(tp);
        check(tp.getHealth() < 20f && tp.isPotionActive(net.minecraft.init.MobEffects.SLOWNESS), "land medusa melee: health " + tp.getHealth() + " + sticky goo slowness");
        tp.clearActivePotions(); tp.setHealth(20f);
        // wings: glide, no fall damage
        m.setAttackTarget(null);
        m.setPosition(top.getX() + 0.5, top.getY() + 15, top.getZ() + 0.5);
        m.motionY = 0; m.fallDistance = 0; m.onGround = false;
        float hp = m.getHealth();
        double minVy = 0;
        for (int i = 0; i < 200 && !m.onGround; i++) { m.onUpdate(); m.ticksExisted++; minVy = Math.min(minVy, m.motionY); }
        for (int i = 0; i < 5; i++) { m.onUpdate(); m.ticksExisted++; }
        check(m.onGround && m.getHealth() >= hp && minVy >= -0.2, "land medusa glides down 15 blocks (min vy " + String.format("%.2f", minVy) + ", health " + m.getHealth() + "/" + hp + ")");
        m.setDead();

        // drops (+ Looting)
        LandDropListener dl = new LandDropListener();
        MinecraftForge.EVENT_BUS.register(dl);
        for (int i = 0; i < 600; i++) {
            EntityLandMedusa d = new EntityLandMedusa(lf);
            d.setPosition(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
            lf.spawnEntity(d);
            d.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(fp), 1000f);
            d.setDead();
        }
        double r0 = theriacDrops / (double) Math.max(1, landDeaths);
        check(landDeaths == 600 && r0 > 0.11 && r0 < 0.24, "Bottle of Theriac drop rate " + theriacDrops + "/" + landDeaths + " = " + String.format("%.1f%%", r0 * 100) + " (config 17%)");
        ItemStack sword = new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
        sword.addEnchantment(net.minecraft.init.Enchantments.LOOTING, 3);
        fp.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, sword);
        theriacDrops = 0; landDeaths = 0;
        for (int i = 0; i < 600; i++) {
            EntityLandMedusa d = new EntityLandMedusa(lf);
            d.setPosition(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
            lf.spawnEntity(d);
            d.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(fp), 1000f);
            d.setDead();
        }
        double r3 = theriacDrops / (double) Math.max(1, landDeaths);
        check(r3 > r0 && r3 > 0.24, "Looting III raises the Theriac rate to " + String.format("%.1f%%", r3 * 100) + " (expected ~32%)");
        fp.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, ItemStack.EMPTY);
        MinecraftForge.EVENT_BUS.unregister(dl);

        // Bottle of Theriac -> PURPLE
        check(net.minecraftforge.fml.common.registry.ForgeRegistries.POTIONS.getValue(new net.minecraft.util.ResourceLocation("lucifertotem", "purple")) == ModContent.PURPLE, "PURPLE potion registered");
        String nm = net.minecraft.util.text.translation.I18n.translateToLocal(ModContent.PURPLE.getName());
        check("PURPLE".equals(nm), "effect display name is exactly '" + nm + "'");
        check(ModContent.PURPLE.getLiquidColor() == 0x9B30FF, "PURPLE particle colour #" + Integer.toHexString(ModContent.PURPLE.getLiquidColor()));
        tp.clearActivePotions(); tp.setHealth(20f);
        tp.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.POISON, 200, 0));
        ItemStack bottle = new ItemStack(ModContent.THERIAC);
        tp.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, bottle);
        check(ModContent.THERIAC.getItemUseAction(bottle) == net.minecraft.item.EnumAction.DRINK, "Theriac is drunk like a potion");
        ItemStack back = ModContent.THERIAC.onItemUseFinish(bottle, lf, tp);
        PotionEffect pe = tp.getActivePotionEffect(ModContent.PURPLE);
        check(back.getItem() == net.minecraft.init.Items.GLASS_BOTTLE && pe != null && pe.getDuration() == 1200, "drinking returns a glass bottle and gives PURPLE for 60 s (" + pe + ")");
        for (int i = 0; i < 2; i++) tp.ticksExisted++;
        ModContent.PURPLE.performEffect(tp, 0);
        check(!tp.isPotionActive(net.minecraft.init.MobEffects.POISON), "PURPLE cures poison");
        tp.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.WITHER, 200, 0));
        check(!tp.isPotionActive(net.minecraft.init.MobEffects.WITHER), "PURPLE wards off new wither/poison");
        tp.hurtResistantTime = 0; tp.setHealth(20f);
        tp.attackEntityFrom(net.minecraft.util.DamageSource.GENERIC, 10f);
        check(Math.abs(tp.getHealth() - 12f) < 0.01, "PURPLE: 20% less damage (10 -> " + (20f - tp.getHealth()) + ")");
        tp.setHealth(10f);
        tp.ticksExisted = 100;
        ModContent.PURPLE.performEffect(tp, 0);
        check(tp.getHealth() == 11f, "PURPLE regenerates (health " + tp.getHealth() + ")");
        tp.clearActivePotions(); tp.setHealth(20f); tp.inventory.clear();
    }

    static int priapusDrops = 0, medusaDeaths = 0;
    public static class DropListener {
        @SubscribeEvent
        public void drops(net.minecraftforge.event.entity.living.LivingDropsEvent e) {
            if (!(e.getEntityLiving() instanceof EntityMedusa)) return;
            medusaDeaths++;
            for (net.minecraft.entity.item.EntityItem it : e.getDrops()) if (it.getItem().getItem() == ModContent.PRIAPUS) priapusDrops++;
            e.setCanceled(true); // don't litter the test world
        }
    }

    static BlockPos findWater(WorldServer w, BlockPos near) {
        for (int r = 0; r < 64; r += 2) for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) {
            BlockPos p = new BlockPos(near.getX() + dx, lucifertotem.world.LightfallChunkGenerator.SEA_LEVEL - 3, near.getZ() + dz);
            boolean ok = true;
            for (int dy = -2; dy <= 3; dy++) if (w.getBlockState(p.up(dy)).getMaterial() != net.minecraft.block.material.Material.WATER) ok = false;
            if (ok) return p;
        }
        return null;
    }

    static void medusaTests(MinecraftServer srv, WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp) throws Exception {
        check(net.minecraft.entity.EntityList.getKey(EntityMedusa.class) != null && net.minecraft.entity.EntityList.ENTITY_EGGS.containsKey(net.minecraft.entity.EntityList.getKey(EntityMedusa.class)),
            "medusa registered with spawn egg: " + net.minecraft.entity.EntityList.getKey(EntityMedusa.class));
        net.minecraft.entity.passive.EntitySquid sq = new net.minecraft.entity.passive.EntitySquid(lf);
        BlockPos wp = findWater(lf, tp.getPosition());
        check(wp != null, "found open sea near the gate: " + wp);
        sq.setPosition(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5);
        check(net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(sq, lf, (float) sq.posX, (float) sq.posY, (float) sq.posZ, false) == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW, "squid allowed in the low Lightfall sea");

        // patrol behaviour
        EntityMedusa m = new EntityMedusa(lf);
        m.setPosition(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5);
        lf.spawnEntity(m);
        double sx = m.posX, sy = m.posY, sz = m.posZ;
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < 400 && !m.isDead; i++) { m.onUpdate(); m.ticksExisted++; if (i % 10 == 0) path.append(String.format(" %d:(%.1f,%.1f,%.1f %s w=%s vy=%.2f)", i, m.posX, m.posY, m.posZ, lf.getBlockState(new BlockPos(m)).getBlock().getRegistryName(), m.isInWater(), m.motionY)); } // ticksExisted is normally advanced by World.updateEntity
        if (m.isDead || !m.isInWater()) LuciferTotem.LOG.info("[LT][DIAG] medusa path" + path + " last=" + (m.getLastDamageSource() == null ? "-" : m.getLastDamageSource().getDamageType()) + " hp=" + m.getHealth() + " at " + lf.getBlockState(new BlockPos(m)) + " below " + lf.getBlockState(new BlockPos(m).down()));
        double moved = Math.sqrt(m.getDistanceSq(sx, sy, sz));
        check(!m.isDead && m.isInWater() && moved > 1.0 && m.posY < lucifertotem.world.LightfallChunkGenerator.SEA_LEVEL + 1, "medusa patrols underwater: moved " + String.format("%.1f", moved) + " blocks in 400 ticks, y=" + String.format("%.1f", m.posY) + " dead=" + m.isDead + " inWater=" + m.isInWater());

        // sting
        java.lang.reflect.Field inv = EntityPlayerMP.class.getDeclaredField("field_147101_bU");
        inv.setAccessible(true); inv.setInt(tp, 0);
        tp.hurtResistantTime = 0; tp.setHealth(20f);
        tp.clearInvulnerableDimensionChange(); // normally cleared when the client confirms the teleport
        m.onCollideWithPlayer(tp);
        check(tp.getHealth() < 20f && tp.isPotionActive(net.minecraft.init.MobEffects.POISON) && tp.isPotionActive(net.minecraft.init.MobEffects.SLOWNESS), "touching a medusa stings: health " + tp.getHealth() + ", poison+slowness");
        tp.clearActivePotions(); tp.setHealth(20f);

        // drops
        MinecraftForge.EVENT_BUS.register(new DropListener());
        for (int i = 0; i < 400; i++) {
            EntityMedusa d = new EntityMedusa(lf);
            d.setPosition(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5);
            lf.spawnEntity(d);
            d.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(fp), 1000f);
            d.setDead();
        }
        double rate = priapusDrops / (double) Math.max(1, medusaDeaths);
        check(medusaDeaths == 400 && rate > 0.10 && rate < 0.27, "Priapus Totem drop rate " + priapusDrops + "/" + medusaDeaths + " = " + String.format("%.1f%%", rate * 100) + " (config 18%)");

        // totem effect
        tp.inventory.clear();
        tp.setHealth(10f);
        CommonEvents.applyPriapus(tp, 40);
        check(!tp.isPotionActive(net.minecraft.init.MobEffects.SPEED) && tp.getHealth() == 10f, "no totem -> no effect");
        tp.inventory.setInventorySlotContents(4, new ItemStack(ModContent.PRIAPUS));
        CommonEvents.applyPriapus(tp, 40);
        PotionEffect spd = tp.getActivePotionEffect(net.minecraft.init.MobEffects.SPEED);
        check(spd != null && spd.getAmplifier() == 0 && tp.getHealth() == 11f, "totem in hotbar -> Speed I + heal (health " + tp.getHealth() + ")");
        tp.inventory.offHandInventory.set(0, new ItemStack(ModContent.PRIAPUS));
        tp.inventory.setInventorySlotContents(5, new ItemStack(ModContent.PRIAPUS));
        CommonEvents.applyPriapus(tp, 80);
        spd = tp.getActivePotionEffect(net.minecraft.init.MobEffects.SPEED);
        check(spd.getAmplifier() == 0 && tp.getHealth() == 12f, "three totems do not stack (Speed I, +1 health only)");
        tp.inventory.clear();
        tp.inventory.offHandInventory.set(0, new ItemStack(ModContent.PRIAPUS));
        check(CommonEvents.carriesPriapus(tp), "offhand totem counts");
        tp.inventory.clear();
        tp.inventory.setInventorySlotContents(20, new ItemStack(ModContent.PRIAPUS));
        check(!CommonEvents.carriesPriapus(tp), "totem in main inventory (not hotbar) does not count");
        tp.inventory.clear();
    }

    static void hit(EntityPlayerMP thrower, Entity target) {
        EntityThrownTotem t = new EntityThrownTotem(thrower.world, thrower);
        t.onImpact(new RayTraceResult(target));
    }

    static int count(WorldServer w, Class<?> c) {
        int n = 0;
        for (Entity e : w.loadedEntityList) if (c.isInstance(e) && !e.isDead) n++;
        return n;
    }
}
