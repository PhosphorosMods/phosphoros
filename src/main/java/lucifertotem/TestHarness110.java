package lucifertotem;

import lucifertotem.hillman.*;
import lucifertotem.world.LightfallStructures;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

/** 1.1.0 self-tests (run from TestHarness after the 1.0.x tests). */
public class TestHarness110 {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.1] " + what); }

    static void reset(EntityPlayerMP p) {
        p.clearActivePotions();
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        p.getFoodStats().setFoodLevel(20);
        p.inventory.clear();
        p.extinguish();
        p.rotationPitch = 0;
    }

    static boolean adv(EntityPlayerMP p, String name) {
        Advancement a = p.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, name));
        return a != null && p.getAdvancements().getProgress(a).isDone();
    }

    static void hold(EntityPlayerMP p, ItemStack s) { p.setHeldItem(EnumHand.MAIN_HAND, s); }

    /** a star-grass surface with 3 air above, near p */
    static BlockPos grass(WorldServer w, BlockPos near, int skip) {
        int found = 0;
        for (int r = 0; r < 80; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (int y = 140; y > 46; y--) {
                BlockPos q = near.add(dx, 0, dz); q = new BlockPos(q.getX(), y, q.getZ());
                IBlockState s = w.getBlockState(q);
                if (s.getBlock() == Blocks.AIR) continue;
                if (s.getBlock() == ModContent.STAR_GRASS && w.isAirBlock(q.up()) && w.isAirBlock(q.up(2)) && w.isAirBlock(q.up(3)) && found++ >= skip) return q;
                break;
            }
        }
        return null;
    }

    static int countNear(WorldServer w, Class<? extends Entity> c, BlockPos p, double r) { return w.getEntitiesWithinAABB(c, new AxisAlignedBB(p).grow(r)).size(); }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp, int R) throws Exception {
        BlockPos home = tp.getPosition();
        reset(tp);
        // ------------------------------------------------------------ registration
        String[] items = {"venom_vial", "diluted_venom", "mithridatium", "young_vinegar", "aged_vinegar", "mandrake_root", "henbane_leaves", "honey_cake", "sibylline_leaves", "sibyl_leaf",
            "medea_fire_flask", "star_foam", "great_theriac", "pythian_laurel", "bronze_shield", "gorgon_blood_left", "gorgon_blood_right", "alabastron",
            "mandrake", "henbane", "medea_cauldron", "vapor_vent", "trophonius_altar", "trophonius_exit", "fallen_star", "petrified_statue"};
        List<String> miss = new ArrayList<>();
        for (String i : items) if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(LuciferTotem.MODID, i))) miss.add(i);
        for (String b : new String[]{"mandrake", "mandrake_pet", "henbane", "medea_cauldron", "medea_fire", "vapor_vent", "trophonius_altar", "trophonius_exit", "fallen_star", "petrified_statue"})
            if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(LuciferTotem.MODID, b))) miss.add("block " + b);
        for (String e : new String[]{"dipsas", "doctor_ammonius", "thessalian_witch", "pythia", "gorgon", "medea_flask"})
            if (!ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(LuciferTotem.MODID, e))) miss.add("entity " + e);
        for (String e : new String[]{"insania", "pneuma", "dipsas_thirst", "petrifying", "petrified", "oracle_of_trophonius", "trophonian_gloom"})
            if (!ForgeRegistries.POTIONS.containsKey(new ResourceLocation(LuciferTotem.MODID, e))) miss.add("effect " + e);
        for (String s : new String[]{"purple_shout", "ammonius.idle", "ammonius.hurt", "ammonius.death", "ammonius.attack", "ammonius.purple", "ambient.distant_ammonius", "mandrake_scream", "dipsas.hiss", "ritual.snap", "ritual.star_fall", "pythia.speak", "trophonius.whisper", "alabastron.dose", "alabastron.bonk"})
            if (!ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(LuciferTotem.MODID, s))) miss.add("sound " + s);
        check(miss.isEmpty(), "all 1.1.0 items/blocks/entities/effects/sounds registered; missing=" + miss);
        List<String> noLang = new ArrayList<>();
        for (String k : new String[]{"item.lucifertotem.great_theriac.name", "tile.lucifertotem.medea_cauldron.name", "entity.lucifertotem.doctor_ammonius.name", "effect.lucifertotem.insania", "chat.lucifertotem.ammonius_says", "prophecy.lucifertotem.line", "wind.lucifertotem.zephyros", "msg.lucifertotem.trophonius.exit", "advancements.lucifertotem.perseus.title", "subtitles.lucifertotem.ambient.distant_ammonius"})
            if (!net.minecraft.util.text.translation.I18n.canTranslate(k)) noLang.add(k);
        check(noLang.isEmpty(), "1.1.0 lang keys translate on the server; missing=" + noLang);
        miss.clear();
        for (String a : new String[]{"root", "something_in_the_drink", "root_cutter", "poison_king", "trophonius", "old_ram_new_lamb", "sibyl", "star_fall", "oracle", "perseus"})
            if (!HAdv.exists(srv, a)) miss.add(a);
        check(miss.isEmpty(), "10 advancements load; missing=" + miss);
        Map<String, net.minecraft.item.Item> recipes = new LinkedHashMap<>();
        recipes.put("sibylline_leaves", HItems.SIBYLLINE); recipes.put("honey_cake", HItems.HONEY_CAKE); recipes.put("young_vinegar", HItems.YOUNG_VINEGAR);
        recipes.put("diluted_venom", HItems.DILUTED_VENOM); recipes.put("medea_fire_flask", HItems.MEDEA_FLASK); recipes.put("medea_cauldron", net.minecraft.item.Item.getItemFromBlock(HBlocks.CAULDRON));
        recipes.put("great_theriac", HItems.GREAT_THERIAC); recipes.put("pythian_laurel", HItems.PYTHIAN_LAUREL); recipes.put("bronze_shield", HItems.BRONZE_SHIELD); recipes.put("alabastron", HItems.ALABASTRON);
        miss.clear();
        for (Map.Entry<String, net.minecraft.item.Item> e : recipes.entrySet()) {
            IRecipe r = CraftingManager.getRecipe(new ResourceLocation(LuciferTotem.MODID, e.getKey()));
            if (r == null || r.getRecipeOutput().getItem() != e.getValue()) miss.add(e.getKey());
        }
        check(miss.isEmpty(), "10 crafting recipes registered with the right outputs; bad=" + miss);
        {
            InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
            inv.setInventorySlotContents(0, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
            inv.setInventorySlotContents(1, new ItemStack(Items.FERMENTED_SPIDER_EYE));
            inv.setInventorySlotContents(2, new ItemStack(Items.SUGAR));
            ItemStack out = CraftingManager.findMatchingResult(inv, ow);
            check(out.getItem() == HItems.YOUNG_VINEGAR, "young vinegar crafts from a WATER bottle (item_nbt) + fermented spider eye + sugar -> " + out);
            inv.clear();
            inv.setInventorySlotContents(0, new ItemStack(Items.BOOK));
            inv.setInventorySlotContents(1, new ItemStack(Blocks.LEAVES)); inv.setInventorySlotContents(2, new ItemStack(Blocks.LEAVES)); inv.setInventorySlotContents(3, new ItemStack(Blocks.LEAVES, 1, 2));
            out = CraftingManager.findMatchingResult(inv, ow);
            check(out.getItem() == HItems.SIBYLLINE, "sibylline leaves craft from a book + any 3 leaves (ore dict) -> " + out);
        }
        ItemStack awk = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.AWKWARD);
        check(BrewingRecipeRegistry.getOutput(awk, new ItemStack(HItems.VENOM_VIAL)).getItem() == HItems.MITHRIDATIUM, "brewing: awkward potion + venom vial -> Mithridatium");
        check(BrewingRecipeRegistry.getOutput(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER), new ItemStack(HItems.VENOM_VIAL)).isEmpty(), "brewing: water + venom vial does nothing");
        for (ResourceLocation rl : new ResourceLocation[]{HContent.LOOT_DIPSAS, LuciferTotem.LOOT_DELPHI}) {
            LootTable t = lf.getLootTableManager().getLootTableFromLocation(rl);
            check(t != LootTable.EMPTY_LOOT_TABLE, "loot table " + rl + " loads");
        }

        // ------------------------------------------------------------ generation of the new structures
        Map<String, Integer> c = LightfallStructures.COUNTS;
        for (String t : new String[]{"trophonius_pit", "delphic_vent"}) check(c.getOrDefault(t, 0) > 0, "1.1.0 structure generated: " + t + " x" + c.getOrDefault(t, 0));
        StructureIndex idx = StructureIndex.get(lf);
        int wrong = 0, total = 0;
        for (String t : StructureIndex.TYPES) for (long l : idx.all(t)) {
            BlockPos p = BlockPos.fromLong(l); total++;
            int cx = Math.floorDiv(p.getX() - 8, 16), cz = Math.floorDiv(p.getZ() - 8, 16);
            if (!t.equals(StructureIndex.typeForRoll(StructureIndex.roll(lf.getSeed(), cx, cz)))) wrong++;
        }
        check(total > 20 && wrong == 0, "StructureIndex: every recorded structure (" + total + ") sits in a chunk whose predicted roll is its type (" + wrong + " mismatches) -> oracles can predict ungenerated ones");
        int herbs = 0, mandrakes = 0;
        for (int cx = -R + 1; cx < R; cx++) for (int cz = -R + 1; cz < R; cz++) {
            net.minecraft.world.chunk.Chunk ch = lf.getChunk(cx, cz);
            for (net.minecraft.world.chunk.storage.ExtendedBlockStorage sec : ch.getBlockStorageArray()) {
                if (sec == null) continue;
                for (int i = 0; i < 4096; i++) {
                    net.minecraft.block.Block b = sec.get(i & 15, (i >> 8) & 15, (i >> 4) & 15).getBlock();
                    if (b == HBlocks.HENBANE) herbs++; else if (b == HBlocks.MANDRAKE) mandrakes++;
                }
            }
        }
        check(herbs > 20 && mandrakes > 5, "henbane (" + herbs + ") and shrieking mandrakes (" + mandrakes + ") grow on the islands");
        check(c.getOrDefault("morningstar_citadel", 0) == 0 || LightfallStructures.gorgons > 0, "a Gorgon guards new citadels (gorgons=" + LightfallStructures.gorgons + ", citadels=" + c.getOrDefault("morningstar_citadel", 0) + ")");

        // ------------------------------------------------------------ 1. PURPLE! shout
        {
            int s0 = Shout.shouts;
            hold(tp, new ItemStack(ModContent.THERIAC));
            ModContent.THERIAC.onItemUseFinish(tp.getHeldItemMainhand(), lf, tp);
            check(Shout.shouts == s0 + 1 && tp.isPotionActive(ModContent.PURPLE), "drinking Theriac shouts (" + Shout.lastLine + ") and gives PURPLE");
            check(adv(tp, "something_in_the_drink") && adv(tp, "root"), "advancement 'Something in the Drink' granted");
            int m0 = HEvents.medusaShouts; int oneIn = LTConfig.purpleOnMedusaKillOneIn; LTConfig.purpleOnMedusaKillOneIn = 1;
            EntityLandMedusa lm = new EntityLandMedusa(lf); lm.setPosition(tp.posX + 2, tp.posY, tp.posZ); lf.spawnEntity(lm);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDeathEvent(lm, DamageSource.causePlayerDamage(tp)));
            LTConfig.purpleOnMedusaKillOneIn = oneIn; lm.setDead();
            check(HEvents.medusaShouts == m0 + 1, "killing a Medusa shouts PURPLE! (forced 1-in-1 for the test; default 1 in " + oneIn + ")");
            EntityAmmonius am = new EntityAmmonius(lf); am.setPosition(tp.posX + 3, tp.posY, tp.posZ); lf.spawnEntity(am);
            hold(tp, new ItemStack(ModContent.THERIAC));
            ModContent.THERIAC.onItemUseFinish(tp.getHeldItemMainhand(), lf, tp);
            check(Shout.lastLine.contains("Doctor Ammonius") && Shout.lastLine.contains("PURPLE"), "Doctor Ammonius nearby shouts PURPLE himself: " + Shout.lastLine);
            // Ammonius trades
            reset(tp);
            hold(tp, new ItemStack(Items.EMERALD, 3));
            am.processInteract(tp, EnumHand.MAIN_HAND);
            int nth = 0; for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == ModContent.THERIAC) nth += s.getCount();
            check(nth == 1 && (tp.getHeldItemMainhand().isEmpty() || tp.getHeldItemMainhand().getItem() == ModContent.THERIAC), "Doctor Ammonius sells Theriac for 3 emeralds (theriac=" + nth + ", held=" + tp.getHeldItemMainhand() + ", creative=" + tp.capabilities.isCreativeMode + ", target=" + am.getAttackTarget() + ")");
            reset(tp);
            hold(tp, new ItemStack(Items.BOOK));
            am.processInteract(tp, EnumHand.MAIN_HAND);
            ItemStack leaves = ItemStack.EMPTY;
            for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == HItems.SIBYLLINE) leaves = s;
            check(!leaves.isEmpty() && HItems.ItemSibylline.hasPages(leaves), "Doctor Ammonius turns a book into written Sibylline Leaves (" + HItems.ItemSibylline.pages(leaves).size() + " pages)");
            reset(tp);
            int r0 = EntityAmmonius.rantsSaid; am.rant(); am.processInteract(tp, EnumHand.MAIN_HAND);
            check(EntityAmmonius.rantsSaid == r0 + 1, "Doctor Ammonius rants / glosses / points to the Pythia without errors");
            for (int i = 0; i < 40; i++) am.onUpdate();
            check(am.getAttackTarget() == null, "Doctor Ammonius is neutral (no target after 40 ticks beside a player)");
            am.attackEntityFrom(DamageSource.causePlayerDamage(tp), 1f);
            for (int i = 0; i < 5; i++) am.onUpdate();
            check(am.getRevengeTarget() == tp || am.getAttackTarget() == tp, "...but hits back when struck");
            am.setDead();
            boolean listed = false;
            for (net.minecraft.world.biome.Biome.SpawnListEntry e : ModContent.BIOME.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER)) if (e.entityClass == EntityAmmonius.class) listed = true;
            boolean ow2 = false;
            for (net.minecraft.world.biome.Biome.SpawnListEntry e : Biomes.PLAINS.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER)) if (e.entityClass == EntityAmmonius.class) ow2 = true;
            check(listed && ow2, "Doctor Ammonius spawns in the Lightfall (" + listed + ") and rarely in Overworld plains (" + ow2 + ")");
        }

        // ------------------------------------------------------------ 2. Shrieking Mandrake
        {
            reset(tp);
            BlockPos g = grass(lf, home, 0);
            check(g != null, "found island turf for plant tests: " + g);
            BlockPos m = g.up();
            tp.setPositionAndUpdate(m.getX() + 1.5, m.getY(), m.getZ() + 0.5);
            lf.setBlockState(m, HBlocks.MANDRAKE.getDefaultState());
            int sc = HBlocks.BlockMandrake.screams; float h0 = tp.getHealth();
            tp.rotationYaw = 180; // north
            HBlocks.MANDRAKE.removedByPlayer(lf.getBlockState(m), lf, m, tp, true);
            check(HBlocks.BlockMandrake.screams == sc + 1 && tp.getHealth() < h0 && tp.isPotionActive(MobEffects.NAUSEA), "pulling a mandrake carelessly: it screams (" + h0 + " -> " + tp.getHealth() + " hp, nausea)");
            reset(tp);
            lf.setBlockState(m, HBlocks.MANDRAKE.getDefaultState());
            hold(tp, new ItemStack(Items.IRON_SWORD));
            for (int i = 0; i < 3; i++) HBlocks.MANDRAKE.onBlockActivated(lf, m, lf.getBlockState(m), tp, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
            check(lf.getBlockState(m).getValue(HBlocks.BlockMandrake.CIRCLES) == 3, "three sword circles recorded on the plant");
            tp.rotationYaw = 90; // west
            sc = HBlocks.BlockMandrake.screams; int safe = HBlocks.BlockMandrake.safeHarvests;
            HBlocks.MANDRAKE.removedByPlayer(lf.getBlockState(m), lf, m, tp, true);
            check(tp.getHorizontalFacing() == EnumFacing.WEST && HBlocks.BlockMandrake.screams == sc && HBlocks.BlockMandrake.safeHarvests == safe + 1 && adv(tp, "root_cutter"), "three circles + facing west: harvested silently, 'Root Cutter' granted");
            // dog
            reset(tp);
            lf.setBlockState(m, HBlocks.MANDRAKE.getDefaultState());
            EntityWolf dog = new EntityWolf(lf); dog.setPosition(m.getX() + 0.5, m.getY(), m.getZ() + 1.5); lf.spawnEntity(dog);
            dog.setTamedBy(tp); dog.setLeashHolder(tp, true);
            HBlocks.MANDRAKE.onBlockActivated(lf, m, lf.getBlockState(m), tp, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
            check(dog.getEntityData().hasKey("lt_mandrake"), "a leashed tame dog can be tied to the mandrake");
            dog.setPosition(m.getX() + 7.5, m.getY() + 1, m.getZ() + 0.5);
            sc = HBlocks.BlockMandrake.screams;
            HEvents.mandrakeDogs(tp);
            check(lf.isAirBlock(m) && HBlocks.BlockMandrake.screams == sc && countNear(lf, EntityItem.class, m, 2) > 0, "the dog walks off and pulls the root out (no scream, root dropped)");
            dog.setDead();
            for (EntityItem ei : lf.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(m).grow(3))) ei.setDead();
            // pet root
            hold(tp, new ItemStack(HItems.MANDRAKE_ROOT));
            tp.setSneaking(true);
            HItems.MANDRAKE_ROOT.onItemUse(tp, lf, g, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1f, 0.5f);
            tp.setSneaking(false);
            check(lf.getBlockState(m).getBlock() == HBlocks.PET_ROOT, "sneak-use plants the root as a squeaking pet");
            lf.setBlockToAir(m);
        }

        // ------------------------------------------------------------ 3. venom milking + Mithridatism
        {
            reset(tp);
            HData.setVenomTolerance(tp, 0); HData.get(tp).removeTag("lastDose");
            EntityCaveSpider sp = new EntityCaveSpider(lf); sp.setPosition(tp.posX + 1, tp.posY, tp.posZ); lf.spawnEntity(sp);
            hold(tp, new ItemStack(Items.GLASS_BOTTLE, 2));
            boolean h1 = Venom.milk(tp, EnumHand.MAIN_HAND, sp);
            int vials = 0; for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == HItems.VENOM_VIAL) vials += s.getCount();
            Venom.milk(tp, EnumHand.MAIN_HAND, sp);
            int vials2 = 0; for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == HItems.VENOM_VIAL) vials2 += s.getCount();
            check(h1 && vials == 1 && vials2 == 1, "a glass bottle milks venom from a cave spider; the same spider is dry for a while (" + vials + "," + vials2 + ")");
            sp.setDead();
            reset(tp);
            Venom.drinkDiluted(tp); tp.hurtResistantTime = 0; Venom.drinkDiluted(tp);
            check(HData.venomTolerance(tp) == 1, "diluted venom: +1 tolerance, only once per day (" + HData.venomTolerance(tp) + ")");
            Venom.drinkMithridatium(tp);
            check(HData.venomTolerance(tp) == 6, "Mithridatium: +5 tolerance (" + HData.venomTolerance(tp) + ")");
            check(tp.getEntityData().getCompoundTag(net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag("lucifertotem").getInteger("venomTolerance") == 6, "tolerance is stored in the player's persisted NBT (survives death)");
            reset(tp);
            HData.setVenomTolerance(tp, 50);
            tp.addPotionEffect(new PotionEffect(MobEffects.POISON, 200, 0));
            PotionEffect pe = tp.getActivePotionEffect(MobEffects.POISON);
            check(pe != null && pe.getDuration() == 100, "tolerance 50 halves poison (200 -> " + (pe == null ? 0 : pe.getDuration()) + " ticks)");
            reset(tp);
            HData.setVenomTolerance(tp, 99);
            Venom.addTolerance(tp, 1);
            tp.addPotionEffect(new PotionEffect(MobEffects.WITHER, 200, 0));
            check(!tp.isPotionActive(MobEffects.WITHER) && adv(tp, "poison_king"), "tolerance 100: immune to poison/wither, 'Mithridates' granted");
            EntityDipsas snake = new EntityDipsas(lf);
            boolean ignores = !snake.wouldTarget(tp);
            HData.setVenomTolerance(tp, 10);
            boolean bites = snake.wouldTarget(tp);
            HData.setVenomTolerance(tp, 100);
            ignores = ignores && bites;
            check(ignores, "snakes leave a Mithridates king alone");
            snake.setDead();
            HData.setVenomTolerance(tp, 0);
        }

        // ------------------------------------------------------------ 4. Trophonius
        {
            reset(tp);
            BlockPos altar = null;
            for (long l : idx.all("trophonius_pit")) { BlockPos p = BlockPos.fromLong(l); if (lf.getBlockState(p).getBlock() == HBlocks.ALTAR) { altar = p; break; } }
            check(altar != null, "a Trophonius altar exists where the index says: " + altar);
            if (altar != null) {
                BlockPos a = Trophonius.arrival(altar), ex = new BlockPos(altar.getX() + 5, Trophonius.MAZE_FLOOR + 1, altar.getZ() + 5);
                int air = 0, light = 0;
                for (int x = altar.getX() - 8; x <= altar.getX() + 6; x++) for (int z = altar.getZ() - 8; z <= altar.getZ() + 6; z++) {
                    if (lf.isAirBlock(new BlockPos(x, Trophonius.MAZE_FLOOR + 1, z))) air++;
                    light = Math.max(light, lf.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, new BlockPos(x, Trophonius.MAZE_FLOOR + 1, z)));
                }
                check(lf.isAirBlock(a) && lf.getBlockState(ex).getBlock() == HBlocks.EXIT && air >= 90 && light == 0, "the snake pit maze: arrival open, exit block in the far corner, " + air + " corridor cells, sky light " + light + " (pitch black)");
                tp.setPositionAndUpdate(altar.getX() + 0.5, altar.getY() + 1, altar.getZ() + 1.5);
                hold(tp, new ItemStack(HItems.HONEY_CAKE));
                boolean fed = Trophonius.tryDescend(tp, altar);
                check(!fed && !Trophonius.inPit(tp), "a well-fed player is refused (must fast)");
                tp.getFoodStats().setFoodLevel(5);
                boolean ok = Trophonius.tryDescend(tp, altar);
                check(ok && Trophonius.inPit(tp) && tp.getPosition().equals(a) && tp.isPotionActive(MobEffects.BLINDNESS) && tp.getHeldItemMainhand().isEmpty(), "fasting + honey cake: descended into the dark pit at " + tp.getPosition());
                check(countNear(lf, EntityDipsas.class, altar.down(altar.getY() - 6), 12) > 0, "harmless sacred snakes share the pit");
                net.minecraftforge.common.util.BlockSnapshot snap = new net.minecraftforge.common.util.BlockSnapshot(lf, a, Blocks.AIR.getDefaultState());
                lf.setBlockState(a, Blocks.TORCH.getDefaultState(), 2); // Forge fires PlaceEvent after the block is in the world
                net.minecraftforge.event.world.BlockEvent.PlaceEvent pev = new net.minecraftforge.event.world.BlockEvent.PlaceEvent(snap, lf.getBlockState(a.down()), tp, EnumHand.MAIN_HAND);
                MinecraftForge.EVENT_BUS.post(pev);
                lf.setBlockToAir(a);
                net.minecraftforge.event.world.BlockEvent.BreakEvent bev = new net.minecraftforge.event.world.BlockEvent.BreakEvent(lf, a.north(), lf.getBlockState(a.north()), tp);
                MinecraftForge.EVENT_BUS.post(bev);
                check(pev.isCanceled() && bev.isCanceled(), "no torches and no digging in the pit (place cancelled=" + pev.isCanceled() + ", break cancelled=" + bev.isCanceled() + ")");
                lf.getBlockState(ex).getBlock().onEntityCollision(lf, ex, lf.getBlockState(ex), tp);
                check(!Trophonius.inPit(tp) && tp.isPotionActive(HPotions.ORACLE) && tp.isPotionActive(HPotions.GLOOM) && HData.gloomy(tp) && adv(tp, "trophonius") && tp.getDistanceSq(altar) < 9,
                    "walking into the exit: back at the altar with the Oracle (structure locations) + a day of gloom; advancement granted");
                net.minecraftforge.event.ServerChatEvent ce = new net.minecraftforge.event.ServerChatEvent(tp, "hello", new TextComponentString("<lt_traveller> hello"));
                MinecraftForge.EVENT_BUS.post(ce);
                check(ce.getComponent().getUnformattedText().endsWith("*(gloomily)*"), "chat while gloomy: " + ce.getComponent().getUnformattedText());
                net.minecraftforge.event.ServerChatEvent ce2 = new net.minecraftforge.event.ServerChatEvent(fp, "hi", new TextComponentString("<lt_thrower> hi"));
                MinecraftForge.EVENT_BUS.post(ce2);
                check(!ce2.getComponent().getUnformattedText().contains("gloomily"), "other players' chat is unchanged");
                HData.get(tp).setLong("gloomUntil", 0);
                tp.setPositionAndUpdate(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            }
        }

        // ------------------------------------------------------------ 5. Dipsas thirst
        {
            reset(tp);
            EntityDipsas.bite(tp);
            check(tp.isPotionActive(HPotions.THIRST), "a Dipsas bite brings Dipsas Thirst");
            ItemStack bread = new ItemStack(Items.BREAD);
            tp.getFoodStats().setFoodLevel(6);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(tp, bread, 32));
            tp.getFoodStats().addStats(5, 0.6f);
            HEvents.finishUse(tp, bread);
            check(tp.getFoodStats().getFoodLevel() == 6, "food does not satisfy while thirsty (food " + tp.getFoodStats().getFoodLevel() + ")");
            HEvents.finishUse(tp, new ItemStack(Items.MILK_BUCKET));
            PotionEffect th = tp.getActivePotionEffect(HPotions.THIRST);
            check(th != null && th.getDuration() <= 600, "milk only shortens it to 30 s (" + (th == null ? 0 : th.getDuration()) + ")");
            HEvents.finishUse(tp, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
            PotionEffect th2 = tp.getActivePotionEffect(HPotions.THIRST);
            check(th2 != null && th2.getDuration() <= 300, "a water bottle halves the rest (" + (th2 == null ? 0 : th2.getDuration()) + ")");
            hold(tp, new ItemStack(HItems.AGED_VINEGAR));
            ItemStack back = HItems.AGED_VINEGAR.onItemUseFinish(tp.getHeldItemMainhand(), lf, tp);
            check(!tp.isPotionActive(HPotions.THIRST) && back.getItem() == Items.GLASS_BOTTLE, "aged vinegar cures it (bottle returned: " + back + ")");
            reset(tp);
            ItemStack yv = new ItemStack(HItems.YOUNG_VINEGAR);
            HItems.ItemYoungVinegar.born(yv, lf);
            yv.getTagCompound().setLong("born", lf.getTotalWorldTime() - 24001);
            tp.inventory.setInventorySlotContents(3, yv);
            HItems.YOUNG_VINEGAR.onUpdate(yv, lf, tp, 3, false);
            check(tp.inventory.getStackInSlot(3).getItem() == HItems.AGED_VINEGAR, "young vinegar ages into aged vinegar after a day");
            EntityDipsas sacred = new EntityDipsas(lf); sacred.setHarmless(true);
            check(!sacred.attackEntityAsMob(tp) && !tp.isPotionActive(HPotions.THIRST), "the sacred (harmless) snakes never bite");
            boolean desert = false;
            for (net.minecraft.world.biome.Biome.SpawnListEntry e : Biomes.DESERT.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER)) if (e.entityClass == EntityDipsas.class) desert = true;
            boolean lfl = false;
            for (net.minecraft.world.biome.Biome.SpawnListEntry e : ModContent.BIOME.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER)) if (e.entityClass == EntityDipsas.class) lfl = true;
            check(desert && lfl, "Dipsas spawn on Lightfall islands (" + lfl + ") and in Overworld deserts (" + desert + ")");
        }

        // ------------------------------------------------------------ 6. Sibylline Leaves
        {
            reset(tp);
            ItemStack book = new ItemStack(HItems.SIBYLLINE);
            hold(tp, book);
            HItems.SIBYLLINE.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
            List<String> pages = HItems.ItemSibylline.pages(tp.getHeldItemMainhand());
            boolean json = true;
            for (String p : pages) try { net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(p).getUnformattedText(); } catch (Exception e) { json = false; }
            check(pages.size() >= 4 && json && adv(tp, "sibyl"), "first use writes the prophecy (" + pages.size() + " JSON pages incl. true directions), 'The Sibyl's Leaves' granted");
            boolean a1 = HItems.ItemSibylline.windRoll(tp.getHeldItemMainhand()), a2 = HItems.ItemSibylline.windRoll(tp.getHeldItemMainhand());
            int hits = 0; ItemStack probe = tp.getHeldItemMainhand().copy();
            for (int i = 0; i < 1000; i++) { probe.getTagCompound().setInteger("opens", i); if (HItems.ItemSibylline.windRoll(probe)) hits++; }
            check(a1 == a2 && hits > 60 && hits < 140, "wind roll is deterministic (client = server) and ~10%: " + hits + "/1000");
            List<EntityItem> flew = HItems.ItemSibylline.scatter(lf, tp, tp.getHeldItemMainhand());
            check(flew.size() == pages.size() && tp.getHeldItemMainhand().isEmpty(), "the wind scatters the pages: " + flew.size() + " leaves flying");
            InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
            int k = 0;
            for (EntityItem ei : flew) { inv.setInventorySlotContents(k++, ei.getItem().copy()); ei.setDead(); }
            inv.setInventorySlotContents(k, new ItemStack(Items.STRING));
            ItemStack rebound = CraftingManager.findMatchingResult(inv, lf);
            check(rebound.getItem() == HItems.SIBYLLINE && HItems.ItemSibylline.pages(rebound).size() == pages.size() - 1, "re-binding the leaves with string: the wind keeps one page (" + HItems.ItemSibylline.pages(rebound).size() + "/" + pages.size() + ")");
        }

        // ------------------------------------------------------------ 7. Medea's cauldron + fire
        {
            reset(tp);
            BlockPos g = grass(lf, home, 5);
            BlockPos cp = g.up();
            lf.setBlockState(cp, HBlocks.CAULDRON.getDefaultState());
            EntityItem th = new EntityItem(lf, cp.getX() + 0.5, cp.getY() + 0.3, cp.getZ() + 0.5, new ItemStack(ModContent.THERIAC));
            lf.spawnEntity(th);
            HBlocks.CAULDRON.onEntityCollision(lf, cp, lf.getBlockState(cp), th);
            check(lf.getBlockState(cp).getValue(HBlocks.BlockMedeaCauldron.CHARGED) && th.isDead, "a Theriac thrown in charges Medea's Cauldron");
            EntitySheep ram = new EntitySheep(lf); ram.setPosition(cp.getX() + 0.5, cp.getY() + 0.32, cp.getZ() + 0.5); lf.spawnEntity(ram);
            tp.setPositionAndUpdate(cp.getX() + 2.5, cp.getY(), cp.getZ() + 0.5);
            boolean adultBefore = !ram.isChild();
            HBlocks.CAULDRON.onEntityCollision(lf, cp, lf.getBlockState(cp), ram);
            check(adultBefore && ram.isChild() && !lf.getBlockState(cp).getValue(HBlocks.BlockMedeaCauldron.CHARGED) && adv(tp, "old_ram_new_lamb"), "an old ram steps in and comes out a lamb (charge used, 'Old Ram, New Lamb')");
            ram.setDead();
            lf.setBlockState(cp, HBlocks.CAULDRON.getDefaultState().withProperty(HBlocks.BlockMedeaCauldron.CHARGED, true));
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE); pick.setItemDamage(200);
            EntityItem pi = new EntityItem(lf, cp.getX() + 0.5, cp.getY() + 0.3, cp.getZ() + 0.5, pick); lf.spawnEntity(pi);
            HBlocks.CAULDRON.onEntityCollision(lf, cp, lf.getBlockState(cp), pi);
            check(pi.getItem().getItemDamage() == 0, "a worn tool comes out mended");
            pi.setDead(); lf.setBlockToAir(cp);
            BlockPos sea = null;
            for (int r = 0; r < 200 && sea == null; r += 3) for (int dx = -r; dx <= r && sea == null; dx += 3) {
                BlockPos q = new BlockPos(home.getX() + dx, lucifertotem.world.LightfallChunkGenerator.SEA_LEVEL, home.getZ() + r);
                if (lf.getBlockState(q).getBlock() == Blocks.WATER && lf.isAirBlock(q.up())) sea = q;
            }
            check(sea != null, "found open sea: " + sea);
            if (sea != null) {
                int n = EntityMedeaFlask.ignite(lf, sea);
                IBlockState f = lf.getBlockState(sea.up());
                LuciferTotem.LOG.info("[LT][DIAG] medea after ignite: above=" + f + " at=" + lf.getBlockState(sea));
                if (f.getBlock() == HBlocks.MEDEA_FIRE) HBlocks.MEDEA_FIRE.updateTick(lf, sea.up(), f, new Random(1));
                check(n > 0 && lf.getBlockState(sea.up()).getBlock() == HBlocks.MEDEA_FIRE && lf.getBlockState(sea).getMaterial() == net.minecraft.block.material.Material.WATER, "Medea's fire burns ON the water (" + n + " flames), the water stays; above=" + lf.getBlockState(sea.up()) + " at=" + lf.getBlockState(sea));
                EntityZombie z = new EntityZombie(lf); z.setPosition(sea.getX() + 0.5, sea.getY() + 1, sea.getZ() + 0.5); lf.spawnEntity(z);
                HBlocks.MEDEA_FIRE.onEntityCollision(lf, sea.up(), f, z);
                check(z.isBurning(), "it sets creatures alight");
                z.setDead();
                for (BlockPos q : BlockPos.getAllInBox(sea.add(-1, -2, -1), sea.add(1, 3, 1))) if (lf.getBlockState(q).getBlock() == HBlocks.MEDEA_FIRE) lf.setBlockToAir(q);
            }
        }

        // ------------------------------------------------------------ 8. Henbane madness
        {
            reset(tp);
            hold(tp, new ItemStack(HItems.HENBANE_LEAVES));
            HItems.HENBANE_LEAVES.onItemUseFinish(tp.getHeldItemMainhand(), lf, tp);
            check(tp.isPotionActive(HPotions.INSANIA), "eating henbane brings Insania (client: wobble shader, phantom mobs, false sounds, blood-orange fog)");
            int cu = HEvents.insaniaCures;
            tp.addPotionEffect(new PotionEffect(ModContent.PURPLE, 200, 0));
            new HEvents().playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, tp));
            check(!tp.isPotionActive(HPotions.INSANIA) && HEvents.insaniaCures == cu + 1, "PURPLE cures it (with a shout)");
            reset(tp);
            hold(tp, new ItemStack(HItems.GREAT_THERIAC));
            tp.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 400, 1));
            tp.addPotionEffect(new PotionEffect(HPotions.THIRST, 400, 0));
            HItems.GREAT_THERIAC.onItemUseFinish(tp.getHeldItemMainhand(), lf, tp);
            PotionEffect pp = tp.getActivePotionEffect(ModContent.PURPLE);
            check(pp != null && pp.getAmplifier() == 1 && !tp.isPotionActive(MobEffects.SLOWNESS) && !tp.isPotionActive(HPotions.THIRST), "Great Theriac: PURPLE II, all ill effects washed away");
        }

        // ------------------------------------------------------------ 9. Thessalian witches
        {
            reset(tp);
            BlockPos circle = StructureIndex.locate(lf, "witch_circle", home, 40, 40);
            int natural = circle == null ? 0 : lf.getEntitiesWithinAABB(EntityThessalianWitch.class, new AxisAlignedBB(circle).grow(10), w -> w.center() != null).size();
            check(circle != null && natural >= 3, "a witch circle generates with its coven: " + circle + " (" + natural + " witches; forced " + StructureIndex.lastForced + " chunks; " + c.getOrDefault("witch_circle", 0) + " in the pre-generated area)");
            int secs = LTConfig.ritualSeconds; LTConfig.ritualSeconds = 10;
            BlockPos g = grass(lf, home, 12);
            BlockPos center = g.up();
            tp.setPositionAndUpdate(center.getX() + 6.5, center.getY(), center.getZ() + 0.5);
            List<EntityThessalianWitch> cov = new ArrayList<>();
            for (int i = 0; i < 3; i++) { EntityThessalianWitch w = new EntityThessalianWitch(lf); w.setPosition(center.getX() + 0.5 + i - 1, center.getY(), center.getZ() + 1.5); w.setCenter(center); lf.spawnEntity(w); cov.add(w); }
            int done = EntityThessalianWitch.completed, prog = 0;
            for (int t = 0; t < 400 && EntityThessalianWitch.completed == done; t++) {
                for (EntityThessalianWitch w : cov) if (!w.isDead) { w.onUpdate(); w.ticksExisted++; } // World.updateEntity normally advances ticksExisted
                if (t == 40) prog = cov.get(0).progress();
                tp.setPositionAndUpdate(center.getX() + 6.5, center.getY(), center.getZ() + 0.5);
            }
            BlockPos star = center;
            while (lf.getBlockState(star).getBlock() != HBlocks.FALLEN_STAR && star.getY() < center.getY() + 5) star = star.up();
            check(prog > 0 && EntityThessalianWitch.completed == done + 1 && lf.getBlockState(star).getBlock() == HBlocks.FALLEN_STAR && cov.get(0).isDead && adv(tp, "star_fall"),
                "three witches chanting near a player pull down the Morningstar (progress " + prog + "/1000 at 2 s -> Fallen Star, coven vanishes, 'Drawing Down the Star')");
            List<ItemStack> drops = new ArrayList<>();
            net.minecraft.util.NonNullList<ItemStack> dl = net.minecraft.util.NonNullList.create();
            HBlocks.FALLEN_STAR.getDrops(dl, lf, star, lf.getBlockState(star), 0);
            check(!dl.isEmpty() && dl.get(0).getItem() == HItems.STAR_FOAM, "the Fallen Star yields Star Foam (" + dl + ")");
            lf.setBlockToAir(star);
            cov.clear();
            for (int i = 0; i < 3; i++) { EntityThessalianWitch w = new EntityThessalianWitch(lf); w.setPosition(center.getX() + 0.5 + i - 1, center.getY(), center.getZ() + 1.5); w.setCenter(center); lf.spawnEntity(w); cov.add(w); }
            for (int t = 0; t < 41; t++) for (EntityThessalianWitch w : cov) { w.onUpdate(); w.ticksExisted++; }
            int mid = cov.get(1).progress();
            check(mid > 0 && mid < 1000 && !EntityThessalianWitch.ACTIVE.isEmpty(), "a second ritual runs (" + mid + "/1000) and is visible to the Pythia");
            int sn = EntityThessalianWitch.snapped;
            cov.get(2).setHealth(0); cov.get(2).onDeath(DamageSource.causePlayerDamage(tp)); cov.get(2).setDead();
            for (int t = 0; t < 10; t++) for (int i = 0; i < 2; i++) { cov.get(i).onUpdate(); cov.get(i).ticksExisted++; }
            check(EntityThessalianWitch.snapped > sn && cov.get(0).progress() == 0 && cov.get(1).progress() == 0, "killing a witch snaps the spell (progress reset)");
            for (EntityThessalianWitch w : cov) w.setDead();
            EntityThessalianWitch.ACTIVE.clear();
            LTConfig.ritualSeconds = secs;
        }

        // ------------------------------------------------------------ 10. Pythia + prophecies
        {
            reset(tp);
            check(Prophecies.sector(BlockPos.ORIGIN, new BlockPos(0, 0, -100)) == 0 && Prophecies.sector(BlockPos.ORIGIN, new BlockPos(100, 0, 0)) == 2 && Prophecies.sector(BlockPos.ORIGIN, new BlockPos(0, 0, 100)) == 4
                && Prophecies.sector(BlockPos.ORIGIN, new BlockPos(-100, 0, 0)) == 6 && Prophecies.sector(BlockPos.ORIGIN, new BlockPos(70, 0, -70)) == 1, "Greek wind compass: Boreas N, Kaikias NE, Apeliotes E, Notos S, Zephyros W");
            EntityPythia py = null; BlockPos vent = null;
            for (long l : idx.all("delphic_vent")) {
                BlockPos p = BlockPos.fromLong(l);
                List<EntityPythia> l2 = lf.getEntitiesWithinAABB(EntityPythia.class, new AxisAlignedBB(p).grow(4));
                if (!l2.isEmpty() && lf.getBlockState(p.down()).getBlock() == HBlocks.VENT) { py = l2.get(0); vent = p.down(); break; }
            }
            check(py != null, "a Pythia sits by a Delphic vent: " + vent);
            if (py != null) {
                tp.setPositionAndUpdate(vent.getX() + 0.5, vent.getY() + 1, vent.getZ() + 0.5);
                float hp = py.getHealth();
                py.attackEntityFrom(DamageSource.causePlayerDamage(tp), 50f);
                check(py.getHealth() == hp && !py.isDead, "the Pythia cannot be harmed");
                int n0 = EntityPythia.prophecies;
                hold(tp, new ItemStack(HItems.PYTHIAN_LAUREL));
                py.processInteract(tp, EnumHand.MAIN_HAND);
                check(EntityPythia.prophecies == n0, "without the Pneuma she stays silent");
                HBlocks.VENT.onEntityWalk(lf, vent, tp);
                check(tp.isPotionActive(HPotions.PNEUMA), "walking on the vent fills you with the Pneuma");
                py.processInteract(tp, EnumHand.MAIN_HAND);
                check(EntityPythia.prophecies == n0 + 1 && tp.getHeldItemMainhand().isEmpty() && adv(tp, "oracle"), "Pneuma + laurel: she prophesies (laurel taken, 'Delphic Oracle')");
                List<net.minecraft.util.text.ITextComponent> lines = Prophecies.oracle(lf, tp, vent);
                StringBuilder sb = new StringBuilder();
                for (net.minecraft.util.text.ITextComponent l : lines) sb.append(l.getUnformattedText()).append(" | ");
                check(lines.size() >= 3, "prophecy text: " + sb);
            }
            BlockPos tAltar = null;
            for (long l : idx.all("trophonius_pit")) tAltar = BlockPos.fromLong(l);
            BlockPos from = tAltar == null ? home : tAltar.add(150, 0, -90);
            BlockPos found = StructureIndex.locate(lf, "trophonius_pit", from, 32, 0);
            check(found != null && lf.getBlockState(found).getBlock() == HBlocks.ALTAR, "locate(trophonius_pit) from " + from + " points at a real altar: " + found);
            // a place far outside the generated area: predicted, then generated on demand to confirm
            BlockPos far = new BlockPos((R + 30) * 16, 80, 0);
            BlockPos fv = StructureIndex.locate(lf, "delphic_vent", far, 24, 8);
            check(fv == null || lf.getBlockState(fv.down()).getBlock() == HBlocks.VENT, "predictive locate beyond the explored area finds a real (freshly generated) vent or nothing: " + fv + " (forced " + StructureIndex.lastForced + " chunks)");
        }

        // ------------------------------------------------------------ 11. Gorgon
        {
            reset(tp);
            // a flat, enclosed-free test arena high above the islands (terrain-independent)
            BlockPos at = new BlockPos(home.getX(), 160, home.getZ());
            for (BlockPos q : BlockPos.getAllInBox(at.add(-4, -1, -4), at.add(10, 6, 4)))
                lf.setBlockState(q, q.getY() == at.getY() - 1 ? ModContent.DUSKSTONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
            EntityGorgon go = new EntityGorgon(lf);
            go.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5); lf.spawnEntity(go);
            go.onInitialSpawn(lf.getDifficultyForLocation(at), null);
            tp.setPositionAndUpdate(at.getX() + 6.5, at.getY(), at.getZ() + 0.5);
            look(tp, go);
            check(go.meetsGaze(tp), "looking at the Gorgon meets her gaze (gorgon " + new BlockPos(go) + " in " + lf.getBlockState(new BlockPos(go)) + ", player " + tp.getPosition() + ", sees=" + tp.canEntityBeSeen(go) + ")");
            int r = 0;
            for (int i = 0; i < 5; i++) r = go.gaze(tp);
            check(r == 2 && tp.isPotionActive(HPotions.PETRIFIED) && !tp.isPotionActive(HPotions.PETRIFYING), "five gazes petrify (Petrified, stacks cleared)");
            tp.motionY = 0.42;
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(tp));
            check(tp.motionY == 0, "petrified: cannot jump");
            reset(tp);
            look(tp, go);
            hold(tp, new ItemStack(HItems.BRONZE_SHIELD));
            tp.setActiveHand(EnumHand.MAIN_HAND);
            setUseCount(tp, 71990);
            float gh = go.getHealth();
            int rr = go.gaze(tp);
            check(rr == 3 && !tp.isPotionActive(HPotions.PETRIFYING) && go.getHealth() < gh, "blocking with the Bronze Shield: only her reflection, the gaze rebounds (" + gh + " -> " + go.getHealth() + ")");
            tp.resetActiveHand();
            reset(tp);
            look(tp, go);
            BlockPos lamp = new BlockPos(go.posX, go.posY + 1.5, go.posZ).north();
            lf.setBlockState(lamp, Blocks.GLOWSTONE.getDefaultState());
            check(go.stunned() && go.gaze(tp) == 0, "day-blind: strong light on her stuns her gaze");
            lf.setBlockToAir(lamp);
            EntityZombie z = new EntityZombie(lf); z.setPosition(at.getX() + 2.5, at.getY(), at.getZ() + 0.5); lf.spawnEntity(z);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDeathEvent(z, DamageSource.causeMobDamage(go)));
            check(lf.getBlockState(new BlockPos(z)).getBlock() == HBlocks.STATUE, "creatures the Gorgon kills turn to stone (" + lf.getBlockState(new BlockPos(z)) + " at " + new BlockPos(z) + ")");
            lf.setBlockToAir(new BlockPos(z)); z.setDead();
            go.setHealth(go.getMaxHealth() * 0.45f);
            go.onUpdate();
            check(countNear(lf, EntityDipsas.class, at, 6) >= 3, "at half health she calls Dipsas vipers");
            for (EntityDipsas d : lf.getEntitiesWithinAABB(EntityDipsas.class, new AxisAlignedBB(at).grow(8))) d.setDead();
            check(!go.isNonBoss(), "she is a boss (boss bar, Totem bolt boss damage)");
            go.attackEntityFrom(DamageSource.causePlayerDamage(tp), 1f);
            go.setHealth(0f);
            go.onDeath(DamageSource.causePlayerDamage(tp));
            boolean left = false, right = false;
            for (EntityItem ei : lf.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(4))) {
                if (ei.getItem().getItem() == HItems.BLOOD_LEFT) left = true;
                if (ei.getItem().getItem() == HItems.BLOOD_RIGHT) right = true;
                ei.setDead();
            }
            check(left && right && adv(tp, "perseus"), "she drops the blood of both veins; 'Perseus' granted");
            go.setDead();
            reset(tp);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            HItems.ItemGorgonBlood.coat(sword);
            hold(tp, sword);
            EntityZombie z2 = new EntityZombie(lf); z2.setPosition(tp.posX + 1, tp.posY, tp.posZ); lf.spawnEntity(z2);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(z2, DamageSource.causePlayerDamage(tp), 2f));
            check(z2.isPotionActive(MobEffects.WITHER) && sword.getTagCompound().getInteger("lt_gorgon_coat") == 23, "left-vein coated blade withers (23 strikes left)");
            z2.setDead();
            reset(tp);
            tp.inventory.setInventorySlotContents(5, new ItemStack(HItems.BLOOD_RIGHT));
            tp.setHealth(0.5f);
            net.minecraftforge.event.entity.living.LivingDeathEvent de = new net.minecraftforge.event.entity.living.LivingDeathEvent(tp, DamageSource.GENERIC);
            MinecraftForge.EVENT_BUS.post(de);
            check(de.isCanceled() && tp.getHealth() >= 10 && tp.inventory.getStackInSlot(5).isEmpty(), "right-vein blood raises the carrier from death once (consumed)");
            reset(tp);
        }

        // ------------------------------------------------------------ 12. voice + ambience (client plays it; check the data)
        {
            java.io.File src = net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(LuciferTotem.MODID).getSource();
            int voice = 0, amb = 0; boolean wav = false; String sj = "";
            if (src.isFile()) try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(src)) {
                for (java.util.Enumeration<? extends java.util.zip.ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                    String n = en.nextElement().getName();
                    if (n.startsWith("assets/lucifertotem/sounds/ammonius/ambient/") && n.endsWith(".ogg")) amb++;
                    else if (n.startsWith("assets/lucifertotem/sounds/ammonius/") && n.endsWith(".ogg")) voice++;
                    if (n.endsWith(".wav")) wav = true;
                }
                sj = new java.util.Scanner(z.getInputStream(z.getEntry("assets/lucifertotem/sounds.json")), "UTF-8").useDelimiter("\\A").next();
            }
            check(voice >= 40 && amb == 11 && !wav, "voice clips shipped (1.3.0: 8 + 3 guest distant variants): " + voice + " Ammonius clips + " + amb + " distant ambient variants, no wavs");
            check(sj.contains("\"ambient.distant_ammonius\"") && sj.contains("\"ammonius.purple\"") && sj.contains("\"purple_shout\""), "sounds.json defines ambient.distant_ammonius, ammonius.* and purple_shout");
            check(LTConfig.distantAmbient && LTConfig.distantVoiceMinutesLightfall > 0 && LTConfig.distantVoiceMinutesOverworld > LTConfig.distantVoiceMinutesLightfall,
                "distant ambient on by default: every ~" + LTConfig.distantVoiceMinutesLightfall + " min in the Lightfall, ~" + LTConfig.distantVoiceMinutesOverworld + " min in the Overworld (configurable, off switch)");
        }
        alabastronTests(srv, lf, tp, fp, home);
        libraryTests(srv, lf, tp, home);
        tp.setPositionAndUpdate(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        reset(tp);
    }

    static ItemStack lacedWith(PotionEffect... fx) { return Alabastron.withEffects(new ItemStack(HItems.ALABASTRON), java.util.Arrays.asList(fx)); }

    static void alabastronTests(MinecraftServer srv, WorldServer lf, EntityPlayerMP tp, EntityPlayerMP fp, BlockPos home) throws Exception {
        reset(tp);
        // crafting
        InventoryCrafting inv = new InventoryCrafting(new TestHarness.DummyContainer(), 3, 3);
        inv.setInventorySlotContents(1, new ItemStack(Blocks.HARDENED_CLAY));
        inv.setInventorySlotContents(3, new ItemStack(Blocks.STAINED_HARDENED_CLAY, 1, 11));
        inv.setInventorySlotContents(4, new ItemStack(Items.GLASS_BOTTLE));
        inv.setInventorySlotContents(5, new ItemStack(Blocks.HARDENED_CLAY));
        ItemStack flask = CraftingManager.findMatchingResult(inv, lf);
        check(flask.getItem() == HItems.ALABASTRON && !Alabastron.laced(flask), "alabastron crafts from terracotta (any colour) around a glass bottle -> " + flask);
        inv.clear();
        inv.setInventorySlotContents(0, new ItemStack(HItems.ALABASTRON));
        inv.setInventorySlotContents(4, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.STRENGTH));
        ItemStack laced = CraftingManager.findMatchingResult(inv, lf);
        List<PotionEffect> fx = Alabastron.effects(laced);
        net.minecraft.util.NonNullList<ItemStack> rest = CraftingManager.getRemainingItems(inv, lf);
        check(fx.size() == 1 && fx.get(0).getPotion() == MobEffects.STRENGTH && rest.get(4).getItem() == Items.GLASS_BOTTLE, "lacing by crafting with a vanilla potion stores its effects in NBT (" + fx + "), bottle returned");
        inv.setInventorySlotContents(4, new ItemStack(ModContent.THERIAC));
        fx = Alabastron.effects(CraftingManager.findMatchingResult(inv, lf));
        check(fx.size() == 1 && fx.get(0).getPotion() == ModContent.PURPLE, "...and with Theriac (PURPLE)");
        inv.setInventorySlotContents(4, PotionUtils.addPotionToItemStack(new ItemStack(Items.LINGERING_POTION), PotionTypes.LONG_SWIFTNESS));
        fx = Alabastron.effects(CraftingManager.findMatchingResult(inv, lf));
        rest = CraftingManager.getRemainingItems(inv, lf);
        check(fx.size() == 1 && fx.get(0).getPotion() == MobEffects.SPEED && rest.get(4).isEmpty(), "...and with a lingering potion (no bottle back)");
        inv.setInventorySlotContents(0, laced);
        check(CraftingManager.findMatchingResult(inv, lf).isEmpty(), "an already laced alabastron can't be laced again");
        // off-hand lacing
        hold(tp, new ItemStack(HItems.ALABASTRON));
        tp.setHeldItem(EnumHand.OFF_HAND, PotionUtils.addPotionToItemStack(new ItemStack(Items.SPLASH_POTION), PotionTypes.REGENERATION));
        HItems.ALABASTRON.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
        fx = Alabastron.effects(tp.getHeldItemMainhand());
        check(fx.size() == 1 && fx.get(0).getPotion() == MobEffects.REGENERATION && tp.getHeldItemOffhand().isEmpty(), "right-click with a splash potion in the off hand laces it (" + fx + ")");
        check(net.minecraft.util.text.translation.I18n.canTranslate("item.lucifertotem.alabastron.laced.name") && net.minecraft.util.text.translation.I18n.canTranslate("tooltip.lucifertotem.alabastron.lore")
            && net.minecraft.util.text.translation.I18n.canTranslate("msg.lucifertotem.alabastron.dosed") && net.minecraft.util.text.translation.I18n.canTranslate("subtitles.lucifertotem.alabastron.dose"), "alabastron lang keys present");
        reset(tp);

        // from-behind geometry: zombie faces +Z (yaw 0); its back is -Z
        BlockPos g = grass(lf, home, 30);
        BlockPos at = g.up();
        EntityZombie z = new EntityZombie(lf);
        z.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5); lf.spawnEntity(z);
        z.rotationYaw = z.renderYawOffset = z.rotationYawHead = 0;
        Entity probe = new EntityZombie(lf);
        double[][] spots = {{0, -2, 1}, {0, 2, 0}, {2 * Math.sin(Math.toRadians(50)), -2 * Math.cos(Math.toRadians(50)), 1}, {2 * Math.sin(Math.toRadians(70)), -2 * Math.cos(Math.toRadians(70)), 0}, {2, 0, 0}};
        boolean geo = true;
        for (double[] sp : spots) { probe.setPosition(z.posX + sp[0], z.posY, z.posZ + sp[1]); if (Alabastron.fromBehind(probe, z) != (sp[2] == 1)) geo = false; }
        check(geo, "from-behind check: directly behind and 50 deg off = behind; front, 70 deg and side = not (limit " + Alabastron.BEHIND_DEGREES + " deg)");

        // the dose
        tp.setPositionAndUpdate(z.posX, z.posY, z.posZ - 1.5);
        hold(tp, lacedWith(new PotionEffect(MobEffects.SPEED, 3600, 0)));
        int d0 = Alabastron.doses;
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(tp, z));
        PotionEffect sp = z.getActivePotionEffect(MobEffects.SPEED);
        check(Alabastron.doses == d0 + 1 && sp != null && sp.getAmplifier() == 1 && sp.getDuration() == 5400, "struck from behind: boosted dose Speed I 3:00 -> " + sp);
        check(tp.getHeldItemMainhand().isEmpty() && Alabastron.isLodged(z) && !Alabastron.laced(Alabastron.lodgedStack(z)), "the (now empty) alabastron leaves the hand and lodges in the target");
        hold(tp, lacedWith(new PotionEffect(MobEffects.JUMP_BOOST, 600, 0)));
        int r4 = Alabastron.strike(tp, z);
        check(r4 == 4 && Alabastron.laced(tp.getHeldItemMainhand()) && !z.isPotionActive(MobEffects.JUMP_BOOST), "only one fits: a second one isn't used up");
        // front = bonk
        EntityZombie z2 = new EntityZombie(lf);
        z2.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 4.5); lf.spawnEntity(z2);
        z2.rotationYaw = z2.renderYawOffset = 180; // facing -Z, i.e. towards the player standing on its north side
        tp.setPositionAndUpdate(z2.posX, z2.posY, z2.posZ - 1.5);
        int b0 = Alabastron.bonks;
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(tp, z2));
        check(Alabastron.bonks == b0 + 1 && !z2.isPotionActive(MobEffects.JUMP_BOOST) && Alabastron.laced(tp.getHeldItemMainhand()) && !Alabastron.isLodged(z2), "struck from the front it just bonks: no dose, not used up");
        z2.setDead();
        // instant effects: amplifier +1
        net.minecraft.entity.passive.EntityVillager v = new net.minecraft.entity.passive.EntityVillager(lf);
        v.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 8.5); lf.spawnEntity(v);
        v.rotationYaw = v.renderYawOffset = 0;
        tp.setPositionAndUpdate(v.posX, v.posY, v.posZ - 1.5);
        hold(tp, lacedWith(new PotionEffect(MobEffects.INSTANT_DAMAGE, 1, 0)));
        float vh = v.getHealth();
        Alabastron.strike(tp, v);
        check(vh - v.getHealth() >= 11.9f, "instant Harming I is delivered as Harming II (" + vh + " -> " + v.getHealth() + " hp)");
        // persistence
        NBTTagCompound t = new NBTTagCompound();
        z.writeToNBT(t);
        EntityZombie z3 = new EntityZombie(lf);
        z3.readFromNBT(t);
        check(Alabastron.isLodged(z3), "the lodged alabastron is saved with the entity (survives save/reload)");
        Alabastron.lodge(tp, new ItemStack(HItems.ALABASTRON));
        NBTTagCompound pt = new NBTTagCompound();
        tp.writeToNBT(pt);
        check(pt.getCompoundTag("ForgeData").hasKey(Alabastron.TAG_LODGED), "...and with a player (survives logout)");
        // self-removal: sneak + right-click air with an empty main hand
        tp.setSneaking(true);
        boolean pulled = Alabastron.pullOwn(tp);
        tp.setSneaking(false);
        boolean got = false; for (ItemStack s2 : tp.inventory.mainInventory) if (s2.getItem() == HItems.ALABASTRON) got = true;
        check(pulled && got && !Alabastron.isLodged(tp), "a player pulls their own out (sneak + right-click air, empty hand)");
        // retrieval by another player
        fp.inventory.clear();
        fp.setSneaking(true);
        PlayerInteractEventHelper.interact(fp, z);
        fp.setSneaking(false);
        boolean fpGot = false; for (ItemStack s2 : fp.inventory.mainInventory) if (s2.getItem() == HItems.ALABASTRON) fpGot = true;
        check(fpGot && !Alabastron.isLodged(z), "anyone sneak-right-clicking the target takes it back");
        // drop on death
        Alabastron.lodge(z, new ItemStack(HItems.ALABASTRON));
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDeathEvent(z, DamageSource.GENERIC));
        boolean drop = false;
        for (EntityItem ei : lf.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(new BlockPos(z)).grow(2))) if (ei.getItem().getItem() == HItems.ALABASTRON) { drop = true; ei.setDead(); }
        check(drop && !Alabastron.isLodged(z), "if the target dies the alabastron drops");
        z.setDead(); v.setDead();
        // players + PvP rule
        boolean pvp = srv.isPVPEnabled();
        fp.clearActivePotions(); fp.getEntityData().removeTag(Alabastron.TAG_LODGED);
        fp.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        fp.rotationYaw = fp.renderYawOffset = fp.rotationYawHead = 0;
        tp.setPositionAndUpdate(fp.posX, fp.posY, fp.posZ - 1.5);
        srv.setAllowPvp(false);
        hold(tp, lacedWith(new PotionEffect(MobEffects.NIGHT_VISION, 600, 0)));
        int r5 = Alabastron.strike(tp, fp);
        check(r5 == 5 && !fp.isPotionActive(MobEffects.NIGHT_VISION) && Alabastron.laced(tp.getHeldItemMainhand()), "PvP off: players can't be dosed (nothing used up)");
        srv.setAllowPvp(true);
        int r2 = Alabastron.strike(tp, fp);
        PotionEffect nv = fp.getActivePotionEffect(MobEffects.NIGHT_VISION);
        check(r2 == 2 && nv != null && nv.getAmplifier() == 1 && nv.getDuration() == 900 && Alabastron.isLodged(fp), "PvP on: a player struck from behind is dosed and gets one lodged");
        srv.setAllowPvp(pvp);
        Alabastron.unlodge(fp); fp.clearActivePotions(); fp.inventory.clear();
        // sound
        java.io.File src = net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(LuciferTotem.MODID).getSource();
        String sj = "";
        if (src.isFile()) try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(src)) { sj = new java.util.Scanner(zf.getInputStream(zf.getEntry("assets/lucifertotem/sounds.json")), "UTF-8").useDelimiter("\\A").next(); }
        int i0 = sj.indexOf("\"alabastron.dose\""), i1 = i0 < 0 ? -1 : sj.indexOf("}\n }", i0);
        String dose = i0 < 0 ? "" : sj.substring(i0, i1 < 0 ? sj.length() : i1);
        check(HSounds.ALABASTRON_DOSE != null && ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(LuciferTotem.MODID, "alabastron.dose")) == HSounds.ALABASTRON_DOSE
            && dose.contains("minecraft:random/eat1") && dose.contains("minecraft:random/eat3") && !sj.contains("alabastron.pop"), "sound event lucifertotem:alabastron.dose resolves (registered; low-pitched vanilla random/eat1-3 by name, layered with the small-slime squish in code)");
        reset(tp); tp.setSneaking(false);
    }

    static void libraryTests(MinecraftServer srv, WorldServer lf, EntityPlayerMP tp, BlockPos home) throws Exception {
        reset(tp);
        String[] noLang = {"entity.lucifertotem.enchantress.name", "entity.lucifertotem.book_wraith.name", "place.lucifertotem.library", "item.lucifertotem.alexandrian_map.name", "chat.lucifertotem.ammonius.library"};
        boolean lang = true; for (String k : noLang) if (!net.minecraft.util.text.translation.I18n.canTranslate(k)) lang = false;
        check(lang && ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(LuciferTotem.MODID, "enchantress")) && ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(LuciferTotem.MODID, "book_wraith")), "Enchantress + Book Wraith registered, library lang keys present");
        for (ResourceLocation rl : new ResourceLocation[]{lucifertotem.world.LibraryGen.LOOT, lucifertotem.world.LibraryGen.LOOT_APOTHECARY, lucifertotem.world.LibraryGen.LOOT_STARMAP, EntityEnchantress.LOOT}) {
            LootTable t = lf.getLootTableManager().getLootTableFromLocation(rl);
            List<ItemStack> roll = t.generateLootForPools(new Random(7), new LootContext.Builder(lf).build());
            check(t != LootTable.EMPTY_LOOT_TABLE && (rl == EntityEnchantress.LOOT || !roll.isEmpty()), "loot table " + rl + " loads; sample roll: " + roll);
        }
        BlockPos lib = lucifertotem.world.LibraryGen.nearest(lf.getSeed(), home, 8000);
        check(lib != null, "a Library of Alexandria is predicted from the seed: " + lib + " (" + (lib == null ? 0 : (int) Math.sqrt(lib.distanceSq(home.getX(), lib.getY(), home.getZ()))) + " blocks from the gate)");
        if (lib == null) return;
        int L = lucifertotem.world.LibraryGen.SIZE, M = lucifertotem.world.LibraryGen.MARGIN, Y0 = lucifertotem.world.LibraryGen.Y0;
        int ox = lib.getX() - L / 2, oz = lib.getZ() - L / 2;
        int s0 = lucifertotem.world.LibraryGen.slices; long n0 = lucifertotem.world.LibraryGen.nanos;
        int cxa = Math.floorDiv(ox - M - 20, 16) - 1, cxb = Math.floorDiv(ox + L + M + 4, 16) + 2, cza = Math.floorDiv(oz - M - 20, 16) - 1, czb = Math.floorDiv(oz + L + M + 4, 16) + 2;
        long t0 = System.nanoTime();
        for (int cx = cxa; cx <= cxb; cx++) for (int cz = cza; cz <= czb; cz++) lf.getChunk(cx, cz);
        long ms = (System.nanoTime() - t0) / 1000000;
        int sl = lucifertotem.world.LibraryGen.slices - s0;
        double perSlice = sl == 0 ? 0 : (lucifertotem.world.LibraryGen.nanos - n0) / 1e6 / sl;
        check(sl >= 9 && sl <= 25 && perSlice < 300 && perSlice * sl < 4000, "the library is built slice by slice inside populate windows (no cascade): " + sl + " slices, " + String.format("%.1f", perSlice) + " ms per slice, " + String.format("%.0f", perSlice * sl) + " ms total (" + ms + " ms for all " + ((cxb - cxa + 1) * (czb - cza + 1)) + " chunks incl. terrain)");
        check(StructureIndex.get(lf).all("library").contains(lib.toLong()), "recorded in the structure index at its centre");
        check(lf.getBlockState(lib).getBlock() == Blocks.GOLD_BLOCK && lf.getBlockState(lib.up(lucifertotem.world.LibraryGen.FH * 2 + 9)).getBlock() == Blocks.GOLD_BLOCK, "Grand Reading Hall: golden star at the centre, golden apex on the dome");
        int shelves = 0, chestsN = 0, looted = 0, spawn = 0, glass = 0;
        for (int x = ox - M; x <= ox + L + M; x++) for (int z = oz - M; z <= oz + L + M; z++) for (int y = Y0 - 1; y <= Y0 + 30; y++) {
            BlockPos q = new BlockPos(x, y, z);
            net.minecraft.block.Block b = lf.getBlockState(q).getBlock();
            if (b == Blocks.BOOKSHELF) shelves++;
            else if (b == Blocks.STAINED_GLASS) glass++;
            else if (b == Blocks.CHEST) { chestsN++; net.minecraft.tileentity.TileEntity te = lf.getTileEntity(q); if (te instanceof net.minecraft.tileentity.TileEntityChest && ((net.minecraft.tileentity.TileEntityChest) te).getLootTable() != null && ((net.minecraft.tileentity.TileEntityChest) te).getLootTable().getPath().startsWith("chests/library")) looted++; }
            else if (b == Blocks.MOB_SPAWNER) { net.minecraft.tileentity.TileEntity te = lf.getTileEntity(q); if (te != null && te.writeToNBT(new NBTTagCompound()).getCompoundTag("SpawnData").getString("id").equals("lucifertotem:enchantress")) spawn++; }
        }
        check(shelves > 900, "books everywhere: " + shelves + " bookshelves");
        check(chestsN >= 8 && looted == chestsN, "loot chests: " + looted + "/" + chestsN + " with library loot tables (incl. apothecary + star-map)");
        check(spawn >= 3, "Enchantress spawners resolve to lucifertotem:enchantress: " + spawn);
        check(glass > 100, "a purple glass dome (" + glass + " panes)");
        AxisAlignedBB box = new AxisAlignedBB(ox - M, Y0 - 2, oz - M, ox + L + M, Y0 + 40, oz + L + M);
        List<EntityAmmonius> lib1 = lf.getEntitiesWithinAABB(EntityAmmonius.class, box, a -> a.getEntityData().getBoolean("lt_librarian"));
        check(lib1.size() == 1 && lib1.get(0).hasHome() && !lib1.get(0).isDead, "Doctor Ammonius lives here: exactly one persistent librarian, home set (" + lib1.size() + ")");
        if (!lib1.isEmpty()) {
            NBTTagCompound at = new NBTTagCompound(); lib1.get(0).writeToNBT(at);
            EntityAmmonius copy = new EntityAmmonius(lf); copy.readFromNBT(at);
            check(copy.hasHome() && copy.getHomePosition().equals(lib1.get(0).getHomePosition()), "his home survives save/reload");
        }
        List<EntityEnchantress> ens = lf.getEntitiesWithinAABB(EntityEnchantress.class, box);
        int geared = 0;
        for (EntityLivingBaseHolder h : EntityLivingBaseHolder.of(lf, box)) if (h.enchanted) geared++;
        check(ens.size() >= 3, "persistent Enchantresses placed: " + ens.size());
        check(geared >= 1, "guards in enchanted gear: " + geared);
        BlockPos told = Prophecies.find(lf, home, "library", 0);
        StringBuilder or = new StringBuilder();
        for (net.minecraft.util.text.ITextComponent c : Prophecies.oracle(lf, tp, home)) or.append(c.getUnformattedText()).append(" | ");
        check(lib.equals(told) && or.toString().contains("Library of Alexandria"), "the Pythia points the way: " + or);
        ItemStack map = EntityAmmonius.alexandrianMap(lf, home);
        check(map.getItem() == Items.FILLED_MAP && map.hasTagCompound() && map.getTagCompound().getTagList("Decorations", 10).tagCount() == 1 && map.getDisplayName().contains("Alexandrian"),
            "Alexandrian Map: a filled map with the Library marked (" + map.getDisplayName() + ", " + (map.hasTagCompound() ? map.getTagCompound().getTagList("Decorations", 10) : "-") + ")");
        if (!lib1.isEmpty()) {
            hold(tp, new ItemStack(Items.MAP));
            lib1.get(0).processInteract(tp, EnumHand.MAIN_HAND);
            boolean gotMap = false; for (ItemStack q : tp.inventory.mainInventory) if (q.getItem() == Items.FILLED_MAP) gotMap = true;
            check(gotMap, "Doctor Ammonius draws you the map for an empty map");
            reset(tp);
        }
        // a top-down plan for the docs (floor 0 and floor 1)
        try (java.io.PrintWriter pw = new java.io.PrintWriter("lt_library_plan.txt", "UTF-8")) {
            for (int f = 0; f < 3; f++) {
                pw.println("floor " + f);
                for (int z = oz - M; z <= oz + L + M; z++) {
                    StringBuilder row = new StringBuilder();
                    for (int x = ox - M; x <= ox + L + M; x++) row.append(planChar(lf, new BlockPos(x, Y0 + f * lucifertotem.world.LibraryGen.FH, z)));
                    pw.println(row);
                }
            }
        }
        // the Enchantress in a test arena
        BlockPos ar = new BlockPos(home.getX() + 20, 170, home.getZ());
        for (BlockPos q : BlockPos.getAllInBox(ar.add(-8, -1, -8), ar.add(8, 6, 8))) lf.setBlockState(q, q.getY() == ar.getY() - 1 ? ModContent.DUSKSTONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        EntityEnchantress en = new EntityEnchantress(lf);
        en.setPosition(ar.getX() + 0.5, ar.getY(), ar.getZ() + 0.5); lf.spawnEntity(en);
        en.onInitialSpawn(lf.getDifficultyForLocation(ar), null);
        tp.setPositionAndUpdate(ar.getX() + 0.5, ar.getY(), ar.getZ() + 6.5);
        en.cast(tp, 1); boolean slow = tp.isPotionActive(MobEffects.SLOWNESS);
        en.cast(tp, 2); boolean weak = tp.isPotionActive(MobEffects.WEAKNESS);
        check(slow && weak, "Enchantress hexes: Slowness + Weakness");
        en.cast(tp, 0);
        check(!lf.getEntitiesWithinAABB(net.minecraft.entity.projectile.EntityShulkerBullet.class, en.getEntityBoundingBox().grow(3)).isEmpty(), "Enchantress casts a levitating Lift bolt");
        en.cast(tp, 3);
        List<EntityBookWraith> wr = lf.getEntitiesWithinAABB(EntityBookWraith.class, en.getEntityBoundingBox().grow(6));
        check(wr.size() == 2 && wr.get(0).getOwner() == en, "Enchantress calls two Book Wraiths");
        for (Entity e2 : lf.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(ar).grow(12), e2 -> !(e2 instanceof EntityPlayerMP))) e2.setDead();
        net.minecraft.entity.monster.EntityHusk h = new net.minecraft.entity.monster.EntityHusk(lf);
        lucifertotem.world.LibraryGen.equipEnchanted(h, new Random(3));
        check(h.getHeldItemMainhand().isItemEnchanted(), "library guards: random enchanted gear, 8.5 % drop chance like illagers (" + h.getHeldItemMainhand() + " " + h.getHeldItemMainhand().getEnchantmentTagList() + ")");
        reset(tp);
    }

    static char planChar(WorldServer w, BlockPos floor) {
        net.minecraft.block.Block f = w.getBlockState(floor).getBlock();
        net.minecraft.block.Block a = w.getBlockState(floor.up()).getBlock(), b = w.getBlockState(floor.up(2)).getBlock();
        if (f == Blocks.AIR && a == Blocks.AIR) return ' ';
        if (b == Blocks.BOOKSHELF || a == Blocks.BOOKSHELF) return 'B';
        if (a == Blocks.CHEST) return 'C';
        if (a == Blocks.MOB_SPAWNER) return 'S';
        if (a == Blocks.ENCHANTING_TABLE) return 'E';
        if (a == Blocks.BREWING_STAND || a == Blocks.CAULDRON) return 'A';
        if (b == Blocks.QUARTZ_BLOCK || a == Blocks.QUARTZ_BLOCK) return 'P';
        if (b == ModContent.DUSKSTONE_BRICKS) return '#';
        if (b == Blocks.STAINED_GLASS_PANE) return 'w';
        if (a == Blocks.OAK_STAIRS || a == Blocks.QUARTZ_STAIRS) return '^';
        if (a == Blocks.LADDER) return 'H';
        if (a == Blocks.OAK_FENCE) return 't';
        if (f == Blocks.GOLD_BLOCK) return '*';
        if (f == Blocks.CONCRETE) return 'n';
        if (f == ModContent.STAR_GRASS) return ',';
        if (a == Blocks.CARPET) return '-';
        return '.';
    }

    static class EntityLivingBaseHolder {
        boolean enchanted;
        static List<EntityLivingBaseHolder> of(WorldServer w, AxisAlignedBB box) {
            List<EntityLivingBaseHolder> out = new ArrayList<>();
            for (net.minecraft.entity.EntityLiving e : w.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class, box, e -> e instanceof net.minecraft.entity.monster.EntityHusk || e instanceof net.minecraft.entity.monster.EntitySkeleton)) {
                EntityLivingBaseHolder h = new EntityLivingBaseHolder();
                h.enchanted = e.getHeldItemMainhand().isItemEnchanted();
                for (ItemStack a : e.getArmorInventoryList()) if (a.isItemEnchanted()) h.enchanted = true;
                out.add(h);
            }
            return out;
        }
    }

    /** posts the sneak-right-click on an entity the way the server does */
    static class PlayerInteractEventHelper {
        static void interact(EntityPlayerMP p, Entity target) {
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(p, EnumHand.MAIN_HAND, target));
        }
    }

    static void setUseCount(EntityPlayerMP p, int v) throws Exception {
        java.lang.reflect.Field f;
        try { f = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("field_184628_bn"); }
        catch (NoSuchFieldException e) { f = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("activeItemStackUseCount"); }
        f.setAccessible(true);
        f.setInt(p, v);
    }

    static void look(EntityPlayerMP p, Entity t) {
        double dx = t.posX - p.posX, dz = t.posZ - p.posZ, dy = (t.posY + t.getEyeHeight()) - (p.posY + p.getEyeHeight());
        p.rotationYaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
        p.rotationPitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
        p.rotationYawHead = p.rotationYaw;
    }
}
