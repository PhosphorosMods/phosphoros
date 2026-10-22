package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/** All 1.1.0 blocks. */
public class HBlocks {
    public static BlockMandrake MANDRAKE;
    public static BlockPetRoot PET_ROOT;
    public static BlockHenbane HENBANE;
    public static BlockMedeaCauldron CAULDRON;
    public static BlockMedeaFire MEDEA_FIRE;
    public static BlockVent VENT;
    public static BlockAltar ALTAR;
    public static BlockExit EXIT;
    public static BlockFallenStar FALLEN_STAR;
    public static BlockStatue STATUE;

    static <T extends Block> T named(T b, String name, boolean tab) {
        b.setRegistryName(LuciferTotem.MODID, name);
        b.setTranslationKey(LuciferTotem.MODID + "." + name);
        if (tab) b.setCreativeTab(LuciferTotem.TAB);
        return b;
    }

    public static void msg(EntityPlayer p, String key, Object... args) {
        TextComponentTranslation t = new TextComponentTranslation(key, args);
        t.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        p.sendStatusMessage(t, false);
    }

    public static final DamageSource SCREAM = new DamageSource("mandrake") {{ setDamageBypassesArmor(); }};
    public static final DamageSource MEDEA = new DamageSource("medea_fire") {{ setFireDamage(); }};

    // ---------------------------------------------------------------- Shrieking Mandrake
    /** Pulling it up screams (damage + nausea in 6 blocks) unless done the ancient way:
     *  (a) tie a leashed, tamed dog to it (right-click the plant) and walk away so the dog pulls, or
     *  (b) draw three circles with a sword (3 right-clicks) and pull it up while facing WEST (Theophrastus). */
    public static class BlockMandrake extends BlockBush {
        public static final PropertyInteger CIRCLES = PropertyInteger.create("circles", 0, 3);
        public static int screams = 0, safeHarvests = 0;
        public BlockMandrake() {
            super(Material.PLANTS);
            setSoundType(SoundType.PLANT);
            setDefaultState(blockState.getBaseState().withProperty(CIRCLES, 0));
        }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, CIRCLES); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(CIRCLES); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(CIRCLES, m & 3); }
        @Override protected boolean canSustainBush(IBlockState s) { Block b = s.getBlock(); return b == ModContent.STAR_GRASS || b == Blocks.GRASS || b == Blocks.DIRT; }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return HItems.MANDRAKE_ROOT; }

        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            ItemStack held = p.getHeldItem(hand);
            if (held.getItem() instanceof ItemSword) {
                if (!w.isRemote) {
                    int c = Math.min(3, s.getValue(CIRCLES) + 1);
                    w.setBlockState(pos, s.withProperty(CIRCLES, c), 2);
                    if (w instanceof WorldServer) for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI / 12, r = 1.2;
                        ((WorldServer) w).spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 0.1, pos.getZ() + 0.5 + Math.sin(a) * r, 1, 0, 0, 0, 0.0);
                    }
                    msg(p, c < 3 ? "msg.lucifertotem.mandrake.circle" : "msg.lucifertotem.mandrake.circles_done", c);
                }
                return true;
            }
            if (hand == EnumHand.MAIN_HAND && (held.isEmpty() || held.getItem() == net.minecraft.init.Items.LEAD)) {
                EntityWolf dog = leashedDog(w, p);
                if (dog != null) {
                    if (!w.isRemote) {
                        dog.getEntityData().setLong("lt_mandrake", pos.toLong());
                        dog.getEntityData().setLong("lt_mandrake_t", w.getTotalWorldTime());
                        msg(p, "msg.lucifertotem.mandrake.dog_tied");
                    }
                    return true;
                }
            }
            return false;
        }

        public static EntityWolf leashedDog(World w, EntityPlayer p) {
            for (EntityWolf d : w.getEntitiesWithinAABB(EntityWolf.class, p.getEntityBoundingBox().grow(12)))
                if (d.isTamed() && d.getLeashed() && d.getLeashHolder() == p) return d;
            return null;
        }

        /** breaking by hand: safe only after three circles while facing west */
        @Override
        public boolean removedByPlayer(IBlockState s, World w, BlockPos pos, EntityPlayer p, boolean willHarvest) {
            if (!w.isRemote) {
                boolean safe = s.getValue(CIRCLES) >= 3 && p.getHorizontalFacing() == EnumFacing.WEST;
                if (safe) { safeHarvests++; HAdv.grant(p, "root_cutter"); msg(p, "msg.lucifertotem.mandrake.safe"); }
                else scream(w, pos);
            }
            return super.removedByPlayer(s, w, pos, p, willHarvest);
        }

        public static void scream(World w, BlockPos pos) {
            screams++;
            w.playSound(null, pos, HSounds.MANDRAKE_SCREAM, SoundCategory.BLOCKS, 2.0f, 1.0f);
            for (EntityLivingBase e : w.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos).grow(6))) {
                if (e.getDistanceSq(pos) > 36) continue;
                e.attackEntityFrom(SCREAM, (float) lucifertotem.LTConfig.mandrakeScreamDamage);
                e.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
            }
        }

        /** dog pull: called from HEvents when the tied dog has walked away */
        public static void dogPull(World w, BlockPos pos, EntityWolf dog) {
            w.setBlockToAir(pos);
            spawnAsEntity(w, pos, new ItemStack(HItems.MANDRAKE_ROOT));
            w.playSound(null, dog.posX, dog.posY, dog.posZ, SoundEvents.ENTITY_WOLF_WHINE, SoundCategory.NEUTRAL, 1f, 1f);
            safeHarvests++;
            if (dog.getOwner() instanceof EntityPlayer) { HAdv.grant((EntityPlayer) dog.getOwner(), "root_cutter"); msg((EntityPlayer) dog.getOwner(), "msg.lucifertotem.mandrake.dog_pulled"); }
        }

        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    /** a planted mandrake root: decoration that squeaks when punched */
    public static class BlockPetRoot extends Block {
        static final AxisAlignedBB BOX = new AxisAlignedBB(0.3, 0, 0.3, 0.7, 0.5, 0.7);
        public BlockPetRoot() { super(Material.PLANTS); setSoundType(SoundType.PLANT); setHardness(0.2f); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return HItems.MANDRAKE_ROOT; }
        @Override public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult t, World w, BlockPos p, EntityPlayer pl) { return new ItemStack(HItems.MANDRAKE_ROOT); }
        @Override public void onBlockClicked(World w, BlockPos pos, EntityPlayer p) {
            if (!w.isRemote) w.playSound(null, pos, HSounds.MANDRAKE_SQUEAK, SoundCategory.BLOCKS, 0.6f, 1.2f + w.rand.nextFloat() * 0.3f);
        }
        @Override public boolean canPlaceBlockAt(World w, BlockPos p) { return super.canPlaceBlockAt(w, p) && w.getBlockState(p.down()).isSideSolid(w, p.down(), EnumFacing.UP); }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    // ---------------------------------------------------------------- Henbane (Insania)
    public static class BlockHenbane extends BlockBush {
        public BlockHenbane() { super(Material.PLANTS); setSoundType(SoundType.PLANT); }
        @Override protected boolean canSustainBush(IBlockState s) { Block b = s.getBlock(); return b == ModContent.STAR_GRASS || b == Blocks.GRASS || b == Blocks.DIRT; }
        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
            Random r = w instanceof World ? ((World) w).rand : RANDOM;
            drops.add(new ItemStack(this));
            drops.add(new ItemStack(HItems.HENBANE_LEAVES, 1 + r.nextInt(2 + fortune)));
        }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    // ---------------------------------------------------------------- Medea's Cauldron
    /** Throw in a Theriac to charge it. Charged: an adult animal (the myth's ram) stepping in comes out young,
     *  or a damaged tool thrown in comes out mended; either uses the charge. */
    public static class BlockMedeaCauldron extends Block {
        public static final PropertyBool CHARGED = PropertyBool.create("charged");
        static final AxisAlignedBB COLL = new AxisAlignedBB(0, 0, 0, 1, 0.3125, 1);
        public static int rejuvenations = 0, repairs = 0;
        public BlockMedeaCauldron() {
            super(Material.IRON);
            setHardness(2.0f); setResistance(10f); setSoundType(SoundType.METAL);
            setLightLevel(0.4f);
            setDefaultState(blockState.getBaseState().withProperty(CHARGED, false));
        }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, CHARGED); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(CHARGED) ? 1 : 0; }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(CHARGED, (m & 1) == 1); }
        @Override public int damageDropped(IBlockState s) { return 0; }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return COLL; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return f == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }

        @Override
        public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {
            if (w.isRemote || e.isDead) return;
            boolean charged = s.getValue(CHARGED);
            if (e instanceof EntityItem) {
                ItemStack st = ((EntityItem) e).getItem();
                if (!charged && (st.getItem() == ModContent.THERIAC || st.getItem() == HItems.GREAT_THERIAC)) {
                    st.shrink(1);
                    if (st.isEmpty()) e.setDead(); else ((EntityItem) e).setItem(st);
                    w.setBlockState(pos, s.withProperty(CHARGED, true), 3);
                    w.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1f, 0.8f);
                    Shout.burst(w, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);
                } else if (charged && st.isItemStackDamageable() && st.getItemDamage() > 0) {
                    st.setItemDamage(0);
                    ((EntityItem) e).setItem(st);
                    e.motionY = 0.45;
                    repairs++;
                    discharge(w, pos, s);
                }
            } else if (charged && e instanceof EntityAgeable && !((EntityAgeable) e).isChild()) {
                ((EntityAgeable) e).setGrowingAge(-24000);
                rejuvenations++;
                discharge(w, pos, s);
                if (e instanceof EntitySheep) {
                    EntityPlayer near = w.getClosestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 10, false);
                    if (near != null) HAdv.grant(near, "old_ram_new_lamb");
                }
            }
        }

        private void discharge(World w, BlockPos pos, IBlockState s) {
            w.setBlockState(pos, s.withProperty(CHARGED, false), 3);
            w.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.BLOCKS, 0.8f, 1.4f);
            if (w instanceof WorldServer) ((WorldServer) w).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.0);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
            if (!s.getValue(CHARGED)) return;
            for (int i = 0; i < 3; i++)
                w.spawnParticle(EnumParticleTypes.SPELL_MOB, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.9, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0.62, 0.19, 1.0);
        }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    // ---------------------------------------------------------------- Medea's Fire
    /** violet fire that sits on water and is not put out by it; burns entities, never spreads, dies down after ~1 minute */
    public static class BlockMedeaFire extends Block {
        public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 7);
        public BlockMedeaFire() {
            super(Material.FIRE);
            setLightLevel(1.0f); setHardness(0f);
            setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
            setTickRandomly(false);
        }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AGE); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(AGE); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(AGE, m & 7); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override public boolean isReplaceable(IBlockAccess w, BlockPos p) { return true; }
        @Override public int quantityDropped(Random r) { return 0; }
        @Override public int tickRate(World w) { return 30; }
        @Override public void onBlockAdded(World w, BlockPos pos, IBlockState s) { w.scheduleUpdate(pos, this, tickRate(w) + w.rand.nextInt(10)); }

        public static boolean validBelow(World w, BlockPos pos) {
            IBlockState b = w.getBlockState(pos.down());
            return b.getMaterial() == Material.WATER || b.isSideSolid(w, pos.down(), EnumFacing.UP);
        }
        @Override
        public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
            if (!validBelow(w, pos)) { w.setBlockToAir(pos); return; }
            int age = s.getValue(AGE);
            if (r.nextInt(3) == 0) {
                if (age >= 7) { w.setBlockToAir(pos); return; }
                w.setBlockState(pos, s.withProperty(AGE, age + 1), 4);
            }
            w.scheduleUpdate(pos, this, tickRate(w) + r.nextInt(10));
        }
        @Override
        public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
            if (!validBelow(w, pos)) w.setBlockToAir(pos);
        }
        /** water does not save you: the fire bites even swimmers */
        @Override
        public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {
            if (w.isRemote) return;
            e.setFire(6);
            e.attackEntityFrom(MEDEA, 1.5f);
        }
        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
            if (r.nextInt(16) == 0) w.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.6f + r.nextFloat(), r.nextFloat() * 0.7f + 0.3f, false);
            w.spawnParticle(EnumParticleTypes.SPELL_WITCH, pos.getX() + r.nextDouble(), pos.getY() + 0.5 + r.nextDouble() * 0.5, pos.getZ() + r.nextDouble(), 0, 0.05, 0);
        }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    // ---------------------------------------------------------------- Delphic Vapor Vent
    /** standing on it fills you with the Pneuma (needed before the Pythia will speak) */
    public static class BlockVent extends Block {
        public BlockVent() { super(Material.ROCK); setHardness(2.5f); setResistance(10f); setSoundType(SoundType.STONE); setLightLevel(0.3f); setHarvestLevel("pickaxe", 0); }
        @Override
        public void onEntityWalk(World w, BlockPos pos, Entity e) {
            if (!w.isRemote && e instanceof EntityLivingBase) ((EntityLivingBase) e).addPotionEffect(new PotionEffect(HPotions.PNEUMA, 400, 0, false, true));
        }
        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
            for (int i = 0; i < 4; i++)
                w.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.05 + r.nextDouble(), pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0.7, 0.45, 1.0);
            if (r.nextInt(30) == 0) w.playSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.3f, 0.6f, false);
        }
    }

    // ---------------------------------------------------------------- Trophonius
    public static class BlockAltar extends Block {
        public BlockAltar() { super(Material.ROCK); setHardness(-1f); setResistance(6000000f); setSoundType(SoundType.STONE); }
        @Override
        public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (hand != EnumHand.MAIN_HAND) return false;
            if (!w.isRemote) Trophonius.tryDescend(p, pos);
            return true;
        }
    }

    /** the way out of the dark: walk into it */
    public static class BlockExit extends Block {
        public BlockExit() { super(Material.PORTAL); setHardness(-1f); setResistance(6000000f); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override public int quantityDropped(Random r) { return 0; }
        @Override
        public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {
            if (!w.isRemote && e instanceof EntityPlayer) Trophonius.exit((EntityPlayer) e, false);
        }
        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
            if (r.nextInt(3) == 0) w.spawnParticle(EnumParticleTypes.SUSPENDED_DEPTH, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0.02, 0);
        }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.TRANSLUCENT; }
    }

    // ---------------------------------------------------------------- Fallen Star (witch ritual)
    public static class BlockFallenStar extends Block {
        public BlockFallenStar() { super(Material.ROCK); setHardness(3f); setResistance(15f); setSoundType(SoundType.GLASS); setLightLevel(1.0f); setHarvestLevel("pickaxe", 1); }
        @Override public Item getItemDropped(IBlockState s, Random r, int f) { return HItems.STAR_FOAM; }
        @Override public int quantityDropped(Random r) { return 2 + r.nextInt(3); }
        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
            w.spawnParticle(EnumParticleTypes.END_ROD, pos.getX() + r.nextDouble(), pos.getY() + 1.0, pos.getZ() + r.nextDouble(), 0, 0.03, 0);
        }
    }

    // ---------------------------------------------------------------- Petrified Statue (Gorgon)
    public static class BlockStatue extends Block {
        static final AxisAlignedBB BOX = new AxisAlignedBB(0.2, 0, 0.2, 0.8, 1.0, 0.8);
        public BlockStatue() { super(Material.ROCK); setHardness(1.5f); setResistance(10f); setSoundType(SoundType.STONE); setHarvestLevel("pickaxe", 0); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    }

    public static void register(net.minecraftforge.registries.IForgeRegistry<Block> r) {
        r.register(MANDRAKE = named(new BlockMandrake(), "mandrake", true));
        r.register(PET_ROOT = named(new BlockPetRoot(), "mandrake_pet", false));
        r.register(HENBANE = named(new BlockHenbane(), "henbane", true));
        r.register(CAULDRON = named(new BlockMedeaCauldron(), "medea_cauldron", true));
        r.register(MEDEA_FIRE = named(new BlockMedeaFire(), "medea_fire", false));
        r.register(VENT = named(new BlockVent(), "vapor_vent", true));
        r.register(ALTAR = named(new BlockAltar(), "trophonius_altar", true));
        r.register(EXIT = named(new BlockExit(), "trophonius_exit", true));
        r.register(FALLEN_STAR = named(new BlockFallenStar(), "fallen_star", true));
        r.register(STATUE = named(new BlockStatue(), "petrified_statue", true));
        for (Block b : new Block[]{MANDRAKE, HENBANE, CAULDRON, VENT, ALTAR, EXIT, FALLEN_STAR, STATUE}) ModContent.ITEM_BLOCKS.add(b);
    }
}
