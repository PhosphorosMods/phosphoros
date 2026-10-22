package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;
import java.util.Random;

/** Stage 4 "Mysteries & Music": Kykeon + the Mystery Hall, the On Air Studio + Lyre-Radio, the Devil's Flight band camp, Lucifer's interview */
public final class Stage4 {
    private Stage4() {}
    public static Item KYKEON_CUP, RECORD_DEVILS_FLIGHT;
    public static Block ON_AIR_SIGN, LYRE_RADIO, SACRUM, ANAKTORON;
    public static final ResourceLocation LOOT_HALL = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "chests/mystery_hall"));
    public static final ResourceLocation LOOT_STUDIO = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "chests/on_air_studio"));
    public static final ResourceLocation LOOT_CAMP = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "chests/band_camp"));

    public static void items(IForgeRegistry<Item> r) {
        KYKEON_CUP = V130.item(r, new HItems.ItemDrink(false, Stage4::drinkKykeon), "kykeon");
        RECORD_DEVILS_FLIGHT = V130.item(r, new Disc(), "record_devils_flight");
    }
    public static void blocks(IForgeRegistry<Block> r) {
        ON_AIR_SIGN = V130.block(r, new BlockOnAir(), "on_air_sign", true);
        LYRE_RADIO = V130.block(r, new BlockLyreRadio(), "lyre_radio", true);
        SACRUM = V130.block(r, new BlockSacrum(), "sacrum", true);
        ANAKTORON = V130.block(r, new BlockAnaktoron(), "anaktoron", true);
    }
    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "band_goat"), EntityBandGoat.class, "lucifertotem.band_goat", id++, mod, 64, 3, true, 0x151515, 0x8A6A3A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "band_singer"), EntityBandSinger.class, "lucifertotem.band_singer", id++, mod, 64, 3, true, 0x3A1A5A, 0x4E8A3A);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "lucifer"), EntityLucifer.class, "lucifertotem.lucifer", id++, mod, 80, 3, true, 0xF0E6C8, 0x8A2BE2);
        return id;
    }
    public static void tiles() { GameRegistry.registerTileEntity(TileRadio.class, new ResourceLocation(LuciferTotem.MODID, "lyre_radio")); }
    public static void init() { net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events()); }

    // ================================================================ Kykeon & the Eleusinian Mysteries
    public static final String[] SACRA = {"wheat", "pomegranate", "torch"};
    public static int visions = 0, initiations = 0, strongVisions = 0;
    /** the order of the sacred things for the hall whose Anaktoron is at a */
    public static int[] order(BlockPos a) {
        int[][] P = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        long h = a.getX() * 73856093L ^ a.getY() * 19349663L ^ a.getZ() * 83492791L;
        return P[(int) Math.floorMod(h, 6L)];
    }
    public static String compass(double dx, double dz) {
        String[] d = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        double a = Math.toDegrees(Math.atan2(-dx, dz));
        return d[(int) Math.floorMod(Math.round(a / 45.0), 8L)];
    }
    static void drinkKykeon(EntityPlayer p) {
        p.addPotionEffect(new PotionEffect(V130.KYKEON, 2400, 0));
        p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 120, 0));
        HAdv.grant(p, "kykeon");
        vision(p);
    }
    /** the vision: a torch procession going down to the nearest Mystery Hall, and the order of the sacred things */
    public static BlockPos vision(EntityPlayer p) { return vision(p, false); }
    /** strong = the Hierophant's kykeon (with ergot): twice the reach, and the exact place */
    public static BlockPos vision(EntityPlayer p, boolean strong) {
        if (p.world.isRemote) return null;
        visions++;
        if (p.world.provider.getDimension() != LuciferTotem.dimId) { HBlocks.msg(p, "msg.lucifertotem.kykeon.vision_far"); return null; }
        BlockPos hall = StructureIndex.locate((WorldServer) p.world, "mystery_hall", p.getPosition(), strong ? 128 : 64, strong ? 6 : 2);
        if (hall == null) { HBlocks.msg(p, "msg.lucifertotem.kykeon.vision_none"); return null; }
        BlockPos alt = anaktoron(p.world, hall, 14);
        int[] o = order(alt != null ? alt : hall);
        double dx = hall.getX() - p.posX, dz = hall.getZ() - p.posZ;
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 10.0) * 10;
        HBlocks.msg(p, "msg.lucifertotem.kykeon.vision", new TextComponentTranslation("dir.lucifertotem." + compass(dx, dz)), dist);
        if (strong) { strongVisions++; HBlocks.msg(p, "msg.lucifertotem.kykeon.vision_exact", hall.getX(), hall.getY(), hall.getZ()); }
        HBlocks.msg(p, "msg.lucifertotem.kykeon.order", new TextComponentTranslation("sacrum.lucifertotem." + SACRA[o[0]]), new TextComponentTranslation("sacrum.lucifertotem." + SACRA[o[1]]), new TextComponentTranslation("sacrum.lucifertotem." + SACRA[o[2]]));
        return hall;
    }
    public static BlockPos anaktoron(World w, BlockPos c, int r) {
        if (!w.isBlockLoaded(c)) return null;
        for (BlockPos q : BlockPos.getAllInBoxMutable(c.add(-r, -14, -r), c.add(r, 4, r))) if (w.getBlockState(q).getBlock() == ANAKTORON) return q.toImmutable();
        return null;
    }
    /** touching a sacred thing: "shown" / "wrong" / "sober" / "done" */
    public static String touch(World w, BlockPos pos, EntityPlayer p) {
        if (!p.isPotionActive(V130.KYKEON)) { HBlocks.msg(p, "msg.lucifertotem.sacrum.sober"); return "sober"; }
        BlockPos alt = anaktoron(w, pos, 10);
        if (alt == null) return "none";
        int sym = w.getBlockState(pos).getValue(BlockSacrum.SYMBOL);
        NBTTagCompound d = HData.get(p);
        if (d.getLong("eleusisHall") != alt.toLong()) { d.setLong("eleusisHall", alt.toLong()); d.setInteger("eleusisStep", 0); }
        int step = d.getInteger("eleusisStep");
        if (step >= 3) { HBlocks.msg(p, "msg.lucifertotem.sacrum.done"); return "done"; }
        if (order(alt)[step] == sym) {
            d.setInteger("eleusisStep", step + 1);
            w.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 1f, 0.7f + step * 0.25f);
            HBlocks.msg(p, "msg.lucifertotem.sacrum.shown", new TextComponentTranslation("sacrum.lucifertotem." + SACRA[sym]), step + 1);
            return "shown";
        }
        d.setInteger("eleusisStep", 0);
        p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 160, 0));
        w.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.BLOCKS, 0.5f, 1.4f);
        HBlocks.msg(p, "msg.lucifertotem.sacrum.wrong");
        return "wrong";
    }
    /** at the Anaktoron: "initiated" / "already" / "unready" / "sober" */
    public static String initiate(World w, BlockPos pos, EntityPlayer p) {
        NBTTagCompound d = HData.get(p);
        if (d.getBoolean("mystes")) { HBlocks.msg(p, "msg.lucifertotem.anaktoron.already"); return "already"; }
        if (!p.isPotionActive(V130.KYKEON)) { HBlocks.msg(p, "msg.lucifertotem.anaktoron.sober"); return "sober"; }
        if (d.getLong("eleusisHall") != pos.toLong() || d.getInteger("eleusisStep") < 3) { HBlocks.msg(p, "msg.lucifertotem.anaktoron.unready"); return "unready"; }
        d.setBoolean("mystes", true);
        d.setInteger("eleusisStep", 0);
        initiations++;
        V130Net.sync(p);
        p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 400, 1));
        p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 6000, 0));
        p.removePotionEffect(MobEffects.NAUSEA);
        w.playSound(null, pos, net.minecraft.init.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1f, 0.8f);
        if (w instanceof WorldServer) ((WorldServer) w).spawnParticle(EnumParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.6, 0.6, 0.6, 0.02);
        TextComponentTranslation c = new TextComponentTranslation("msg.lucifertotem.anaktoron.initiated");
        c.getStyle().setColor(TextFormatting.GOLD);
        p.sendMessage(c);
        HAdv.grant(p, "mystes");
        return "initiated";
    }

    public static class BlockSacrum extends Block {
        public static final PropertyInteger SYMBOL = PropertyInteger.create("symbol", 0, 2);
        static final AxisAlignedBB BOX = new AxisAlignedBB(0.125, 0, 0.125, 0.875, 0.875, 0.875);
        public BlockSacrum() { super(Material.ROCK); setHardness(-1f); setResistance(6000000f); setLightLevel(0.6f); setSoundType(SoundType.STONE); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, SYMBOL); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(SYMBOL); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(SYMBOL, Math.min(2, m)); }
        @Override public int damageDropped(IBlockState s) { return s.getValue(SYMBOL); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!w.isRemote && hand == EnumHand.MAIN_HAND) touch(w, pos, p);
            return true;
        }
    }
    public static class BlockAnaktoron extends Block {
        static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 0.75, 1);
        public BlockAnaktoron() { super(Material.ROCK); setHardness(-1f); setResistance(6000000f); setLightLevel(0.5f); setSoundType(SoundType.STONE); }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return BOX; }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return f == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!w.isRemote && hand == EnumHand.MAIN_HAND) initiate(w, pos, p);
            return true;
        }
        @Override @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState s, World w, BlockPos p, Random r) {
            w.spawnParticle(EnumParticleTypes.FLAME, p.getX() + 0.3 + r.nextDouble() * 0.4, p.getY() + 0.8, p.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.01, 0);
        }
    }

    // ================================================================ On Air: the sign and the Lyre-Radio
    public static class BlockOnAir extends Block {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        public BlockOnAir() { super(Material.GLASS); setHardness(0.5f); setLightLevel(0.8f); setSoundType(SoundType.GLASS); setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex(); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)); }
        @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
            return getDefaultState().withProperty(FACING, f.getAxis().isHorizontal() ? f : placer.getHorizontalFacing().getOpposite());
        }
        @Override public boolean isOpaqueCube(IBlockState s) { return false; }
        @Override public boolean isFullCube(IBlockState s) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
            switch (s.getValue(FACING)) {
                case SOUTH: return new AxisAlignedBB(0, 0.25, 0, 1, 0.75, 0.125);
                case NORTH: return new AxisAlignedBB(0, 0.25, 0.875, 1, 0.75, 1);
                case EAST: return new AxisAlignedBB(0, 0.25, 0, 0.125, 0.75, 1);
                default: return new AxisAlignedBB(0.875, 0.25, 0, 1, 0.75, 1);
            }
        }
        @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
        @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    public static int radioPlays = 0, radioRefused = 0, lastRadioClip = -1;
    public static class TileRadio extends TileEntity {
        public long last = -100000; public boolean powered;
        @Override public NBTTagCompound writeToNBT(NBTTagCompound c) { super.writeToNBT(c); c.setLong("Last", last); c.setBoolean("Powered", powered); return c; }
        @Override public void readFromNBT(NBTTagCompound c) { super.readFromNBT(c); last = c.getLong("Last"); powered = c.getBoolean("Powered"); }
    }
    public static class BlockLyreRadio extends Block implements ITileEntityProvider {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        public BlockLyreRadio() { super(Material.WOOD); setHardness(1.5f); setSoundType(SoundType.WOOD); setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
        @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex(); }
        @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)); }
        @Override public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float hx, float hy, float hz, int meta, EntityLivingBase placer) { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }
        @Override public TileEntity createNewTileEntity(World w, int meta) { return new TileRadio(); }
        @Override public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
            if (w.isRemote) return;
            TileEntity te = w.getTileEntity(pos);
            if (!(te instanceof TileRadio)) return;
            TileRadio t = (TileRadio) te;
            boolean p = w.isBlockPowered(pos);
            if (p && !t.powered) play(w, pos, t);     // rising edge only
            t.powered = p;
            t.markDirty();
        }
        @Override public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
            if (!w.isRemote && hand == EnumHand.MAIN_HAND) HBlocks.msg(p, "msg.lucifertotem.radio.hint");
            return true;
        }
    }
    /** play a clip: two in three are lectures (with their words in chat), the rest his other lines. Returns the clip or -1 (cooling down) */
    public static int play(World w, BlockPos pos, TileRadio t) {
        long now = w.getTotalWorldTime();
        if (now - t.last < LTConfig.v130.radioCooldownTicks) { radioRefused++; return -1; }
        boolean lecture = w.rand.nextInt(3) != 0;
        int[] pool = lecture ? AmmoniusClips.LECTURE : AmmoniusClips.TALK;
        int c = pool[w.rand.nextInt(pool.length)];
        for (int i = 0; i < 8 && c == lastRadioClip; i++) c = pool[w.rand.nextInt(pool.length)];
        lastRadioClip = c;
        t.last = now + AmmoniusClips.TICKS[c];
        radioPlays++;
        w.playSound(null, pos, V130.LYRE, SoundCategory.RECORDS, 1f, 1f);
        if (c < HSounds.LINES.length && HSounds.LINES[c] != null) w.playSound(null, pos, HSounds.LINES[c], SoundCategory.RECORDS, 2.5f, 1f);
        for (EntityPlayer p : w.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(24))) {
            if (lecture) {
                TextComponentTranslation m = new TextComponentTranslation("chat.lucifertotem.radio", new TextComponentTranslation("quote.lucifertotem.ammonius." + AmmoniusClips.NAME[c]));
                m.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
                p.sendMessage(m);
            }
            HAdv.grant(p, "on_air");
        }
        if (w instanceof WorldServer) ((WorldServer) w).spawnParticle(EnumParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 1);
        return c;
    }

    // ================================================================ Devil's Flight: the record
    public static class Disc extends ItemRecord {
        public Disc() { super("devils_flight", V130.disc()); }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { super.addInformation(s, w, tip, f); HItems.tips(this, tip); }
    }
    public static final String[] LYRICS = {"lyric.lucifertotem.1", "lyric.lucifertotem.2", "lyric.lucifertotem.3", "lyric.lucifertotem.4", "lyric.lucifertotem.5", "lyric.lucifertotem.6"};

    // ================================================================ events: Lucifer on the citadel roofs (old worlds too)
    public static int lucifersPlaced = 0;
    public static class Events {
        @SubscribeEvent
        public void tick(TickEvent.PlayerTickEvent e) {
            if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || !LTConfig.v130.luciferInterview) return;
            if (e.player.ticksExisted % 100 != 37 || e.player.world.provider.getDimension() != LuciferTotem.dimId) return;
            ensureLucifer((WorldServer) e.player.world, e.player.getPosition());
        }
    }
    /** the roof of the nearest recorded Morningstar Citadel within 64 blocks gets its Lucifer, if he is not there */
    public static EntityLucifer ensureLucifer(WorldServer w, BlockPos near) {
        BlockPos best = null; double bd = 64 * 64;
        for (long l : StructureIndex.get(w).all("morningstar_citadel")) { BlockPos at = BlockPos.fromLong(l); double d = at.distanceSq(near); if (d < bd) { bd = d; best = at; } }
        if (best == null) return null;
        // the citadel "at" is its south door (cx, g+1, z1): the roof is g+12, the beacon at (cx, cz)
        BlockPos roof = new BlockPos(best.getX() + 3, best.getY() + 12, best.getZ() - 4);
        if (!w.isBlockLoaded(roof) || !w.getBlockState(roof.down()).getMaterial().isSolid() || !w.isAirBlock(roof) || !w.isAirBlock(roof.up())) return null;
        List<EntityLucifer> there = w.getEntitiesWithinAABB(EntityLucifer.class, new AxisAlignedBB(roof).grow(16));
        if (!there.isEmpty()) return there.get(0);
        EntityLucifer l = new EntityLucifer(w);
        l.setLocationAndAngles(roof.getX() + 0.5, roof.getY(), roof.getZ() + 0.5, 180f, 0f);
        l.setHome(roof);
        w.spawnEntity(l);
        lucifersPlaced++;
        return l;
    }

    // ================================================================ structures
    /** the Telesterion: a small well-house over a stair shaft; under it a pillared hall with the three sacred things and the Anaktoron */
    public static boolean hall(Gen130 g) {
        int cx = g.bx + 8, cz = g.bz + 8;
        int y = g.site(cx, cz, 3, 2);
        if (y < 0) return false;
        int hy = y - 10;                                   // hall floor
        if (hy < 30 || !g.solidBelow(cx, cz, 6, y - 2, hy - 1)) return false;
        IBlockState Q = Gen130.QUARTZ(), B = g.BRICK();
        // well-house: 5x5, open front, roof
        g.foundation(cx - 2, cz - 2, cx + 2, cz + 2, y, 4);
        g.fill(cx - 2, y, cz - 2, cx + 2, y, cz + 2, B);
        g.fill(cx - 2, y + 1, cz - 2, cx + 2, y + 3, cz + 2, Gen130.AIR);
        for (int[] k : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) g.fill(cx + k[0], y + 1, cz + k[1], cx + k[0], y + 3, cz + k[1], Gen130.QPILLAR());
        g.fill(cx - 2, y + 4, cz - 2, cx + 2, y + 4, cz + 2, B);
        g.fill(cx - 2, y + 1, cz + 2, cx + 2, y + 2, cz + 2, B); g.set(cx, y + 1, cz + 2, Gen130.AIR); g.set(cx, y + 2, cz + 2, Gen130.AIR);
        g.torch(cx - 1, y + 2, cz - 1, EnumFacing.SOUTH); g.torch(cx + 1, y + 2, cz - 1, EnumFacing.SOUTH);
        // the hall: x cx-6..cx+6, z g.bz+1..g.bz+14, floor hy, ceiling hy+5
        int x0 = cx - 6, x1 = cx + 6, z0 = g.bz + 1, z1 = g.bz + 14;
        g.box(x0, hy - 1, z0, x1, hy + 5, z1, B, Gen130.AIR);
        g.fill(x0 + 1, hy - 1, z0 + 1, x1 - 1, hy - 1, z1 - 1, Q);
        for (int x = x0 + 2; x <= x1 - 2; x += 4) for (int z = z0 + 3; z <= z1 - 3; z += 4) g.fill(x, hy, z, x, hy + 4, z, Gen130.QPILLAR());
        // a ladder shaft from the well-house floor down beside the central pillar
        g.fill(cx, hy + 5, cz, cx, y - 1, cz, B);
        g.fill(cx, hy, cz + 1, cx, y, cz + 1, Gen130.AIR);
        g.ladder(cx, hy, y, cz + 1, EnumFacing.SOUTH);
        g.fill(cx - 1, hy + 5, cz + 1, cx + 1, y - 1, cz + 1, B); g.fill(cx, hy + 5, cz + 2, cx, y - 1, cz + 2, B);
        g.ladder(cx, hy, y, cz + 1, EnumFacing.SOUTH);
        // the three sacred things on pedestals (west, east, south walls), and the Anaktoron at the north end
        BlockPos alt = new BlockPos(cx, hy, z0 + 2);
        g.set(alt.getX(), alt.getY(), alt.getZ(), ANAKTORON.getDefaultState());
        int[][] ped = {{x0 + 1, z0 + 6}, {x1 - 1, z0 + 6}, {cx - 3, z1 - 1}};
        int[] sym = {0, 1, 2};
        for (int i = 2; i > 0; i--) { int j = g.r.nextInt(i + 1); int t = sym[i]; sym[i] = sym[j]; sym[j] = t; }
        for (int i = 0; i < 3; i++) { g.set(ped[i][0], hy, ped[i][1], Gen130.QPILLAR()); g.set(ped[i][0], hy + 1, ped[i][1], SACRUM.getDefaultState().withProperty(BlockSacrum.SYMBOL, sym[i])); }
        g.torch(x0 + 1, hy + 2, z0 + 3, EnumFacing.EAST); g.torch(x1 - 1, hy + 2, z0 + 3, EnumFacing.WEST);
        g.torch(x0 + 1, hy + 2, z1 - 3, EnumFacing.EAST); g.torch(x1 - 1, hy + 2, z1 - 3, EnumFacing.WEST);
        g.chest(cx + 3, hy, z1 - 1, EnumFacing.NORTH, LOOT_HALL);
        g.painting(cx + 1, hy + 2, z1 - 1, EnumFacing.NORTH, "mysteries");
        g.painting(x0 + 1, hy + 2, cz + 2, EnumFacing.EAST, "moon_goddess");
        g.at = new BlockPos(cx, y + 1, cz);
        return true;
    }

    /** the On Air Studio: a little broadcast house, ON AIR over the door, the Lyre-Radio on the desk with a button */
    public static boolean studio(Gen130 g) {
        int cx = g.bx + 8, cz = g.bz + 8;
        int y = g.site(cx, cz, 5, 2);
        if (y < 0) return false;
        int x0 = cx - 5, x1 = cx + 5, z0 = cz - 4, z1 = cz + 4;
        IBlockState P = Blocks.PLANKS.getStateFromMeta(5), W = Blocks.WOOL.getStateFromMeta(15), B = g.BRICK();
        g.foundation(x0, z0, x1, z1, y, 6);
        g.fill(x0, y, z0, x1, y, z1, B);
        g.box(x0, y, z0, x1, y + 5, z1, B, Gen130.AIR);
        g.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, P);
        g.fill(x0 + 1, y + 1, z0 + 1, x1 - 1, y + 1, z0 + 1, W);          // sound-proofing
        // windows + door (south)
        g.set(cx, y + 1, z1, Gen130.AIR); g.set(cx, y + 2, z1, Gen130.AIR);
        for (int x : new int[]{x0 + 2, x1 - 2}) g.set(x, y + 2, z1, Blocks.GLASS_PANE.getDefaultState());
        g.set(cx, y + 4, z1 + 1, ON_AIR_SIGN.getDefaultState().withProperty(BlockOnAir.FACING, EnumFacing.SOUTH));
        g.set(cx, y + 3, z1 - 1, ON_AIR_SIGN.getDefaultState().withProperty(BlockOnAir.FACING, EnumFacing.NORTH));
        // the desk: the radio between two shelves of records, a microphone (end rod on a fence), and a pressure plate to go on air
        g.set(cx, y + 1, z0 + 2, LYRE_RADIO.getDefaultState().withProperty(BlockLyreRadio.FACING, EnumFacing.SOUTH));
        g.set(cx - 1, y + 1, z0 + 2, Blocks.JUKEBOX.getDefaultState()); g.set(cx + 1, y + 1, z0 + 2, Blocks.NOTEBLOCK.getDefaultState());
        g.set(cx + 2, y + 1, z0 + 3, Blocks.DARK_OAK_FENCE.getDefaultState()); g.set(cx + 2, y + 2, z0 + 3, Blocks.END_ROD.getStateFromMeta(1));
        g.set(cx, y + 1, z0 + 3, Blocks.WOODEN_PRESSURE_PLATE.getDefaultState());
        g.set(cx, y + 1, z0 + 5, Gen130.stairs(Blocks.DARK_OAK_STAIRS.getDefaultState(), EnumFacing.SOUTH));
        // shelves, a chest, the painting
        g.fill(x0 + 1, y + 1, cz, x0 + 1, y + 3, z1 - 1, Blocks.BOOKSHELF.getDefaultState());
        g.chest(x1 - 1, y + 1, z1 - 1, EnumFacing.WEST, LOOT_STUDIO);
        g.painting(x1 - 1, y + 2, cz - 1, EnumFacing.WEST, "on_air");
        g.torch(x0 + 2, y + 3, z0 + 1, EnumFacing.SOUTH); g.torch(x1 - 2, y + 3, z0 + 1, EnumFacing.SOUTH);
        g.at = new BlockPos(cx, y + 1, cz);
        return true;
    }

    /** the Devil's Flight band camp: a wooden stage with the poster backdrop, a jukebox already playing their disc, a tent, the goat and the singer */
    public static boolean camp(Gen130 g) {
        int cx = g.bx + 8, cz = g.bz + 8;
        int y = g.site(cx, cz, 5, 2);
        if (y < 0) return false;
        IBlockState P = Blocks.PLANKS.getStateFromMeta(5), L = Blocks.LOG2.getStateFromMeta(1);
        g.foundation(cx - 5, cz - 5, cx + 5, cz + 5, y, 4);
        g.fill(cx - 5, y + 1, cz - 5, cx + 5, y + 7, cz + 5, Gen130.AIR);
        // stage: 9 x 4, one block up, backdrop wall (north) 3 high with the poster
        g.fill(cx - 4, y + 1, cz - 5, cx + 4, y + 1, cz - 2, P);
        g.fill(cx - 4, y + 2, cz - 5, cx + 4, y + 4, cz - 5, Blocks.WOOL.getStateFromMeta(15));
        for (int x : new int[]{cx - 4, cx + 4}) { g.fill(x, y + 2, cz - 2, x, y + 5, cz - 2, L); g.set(x, y + 6, cz - 2, Blocks.GLOWSTONE.getDefaultState()); }
        g.painting(cx, y + 3, cz - 4, EnumFacing.SOUTH, "devils_flight");
        // jukebox with the disc in it
        BlockPos jb = new BlockPos(cx + 3, y + 2, cz - 4);
        g.set(jb.getX(), jb.getY(), jb.getZ(), Blocks.JUKEBOX.getDefaultState().withProperty(net.minecraft.block.BlockJukebox.HAS_RECORD, true));
        TileEntity te = g.in(jb.getX(), jb.getY(), jb.getZ()) ? g.w.getTileEntity(jb) : null;
        if (te instanceof net.minecraft.block.BlockJukebox.TileEntityJukebox) ((net.minecraft.block.BlockJukebox.TileEntityJukebox) te).setRecord(new ItemStack(RECORD_DEVILS_FLIGHT));
        g.set(cx - 3, y + 2, cz - 4, Blocks.NOTEBLOCK.getDefaultState());
        // tent (south-west): purple wool A-frame
        for (int k = 0; k < 3; k++) {
            g.fill(cx - 5 + k, y + 1 + k, cz + 2, cx - 5 + k, y + 1 + k, cz + 5, Gen130.wool(net.minecraft.item.EnumDyeColor.PURPLE));
            g.fill(cx - 1 - k, y + 1 + k, cz + 2, cx - 1 - k, y + 1 + k, cz + 5, Gen130.wool(net.minecraft.item.EnumDyeColor.PURPLE));
        }
        g.chest(cx - 3, y + 1, cz + 4, EnumFacing.NORTH, LOOT_CAMP);
        // camp "fire": a ring of stone around a magma block
        g.set(cx + 3, y, cz + 3, Blocks.MAGMA.getDefaultState());
        for (int[] k : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) g.set(cx + 3 + k[0], y + 1, cz + 3 + k[1], Blocks.STONE_SLAB.getStateFromMeta(3));
        g.mob(EntityBandGoat::new, cx - 1.5, y + 2, cz - 3.5);
        EntityBandSinger s = g.mob(EntityBandSinger::new, cx + 0.5, y + 2, cz - 3.0);
        if (s != null) s.setHomePosAndDistance(new BlockPos(cx, y + 2, cz - 3), 5);
        g.at = new BlockPos(cx, y + 1, cz);
        return true;
    }
}
