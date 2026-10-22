package lucifertotem.lore;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HSounds;
import lucifertotem.hillman.StructureIndex;
import lucifertotem.world.LibraryGen;
import lucifertotem.world.PortalData;
import lucifertotem.world.PortalLogic;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * 1.2.0: the trail from the first Totem to the Library. Every 2 s per player (server): advancement checks; every
 * totemWhisperSeconds a carried totem whispers (actionbar) toward the next step; near a Fallen Altar the distant
 * 'PURPLE' comes more often; very rarely, the Wandering Philologist calls.
 */
public class LoreEvents {
    static final Random RNG = new Random();
    public static int whispers = 0, altarVoices = 0, loginHints = 0;
    public static String lastWhisperKey = "";

    // ------------------------------------------------------------ steps
    public static boolean done(EntityPlayer p, String name) {
        if (!(p instanceof EntityPlayerMP) || p.getServer() == null) return false;
        Advancement a = p.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(LuciferTotem.MODID, name));
        return a != null && ((EntityPlayerMP) p).getAdvancements().getProgress(a).isDone();
    }

    public static boolean hasTotem(EntityPlayer p) {
        for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == ModContent.TOTEM) return true;
        for (ItemStack s : p.inventory.offHandInventory) if (s.getItem() == ModContent.TOTEM) return true;
        return false;
    }

    public static void playTo(EntityPlayerMP p, SoundEvent e, double x, double y, double z, float vol, float pitch) {
        if (p.connection != null) p.connection.sendPacket(new SPacketSoundEffect(e, SoundCategory.AMBIENT, x, y, z, vol, pitch));
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || !(e.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP) e.player;
        if (p instanceof net.minecraftforge.common.util.FakePlayer) return;
        long t = p.world.getTotalWorldTime();
        if ((t + p.getEntityId()) % 40 != 0) return;
        tick2s(p, t);
    }

    public static void tick2s(EntityPlayerMP p, long t) {
        WorldServer w = (WorldServer) p.world;
        boolean totem = hasTotem(p);
        if (totem && HAdv.grant(p, "aww_gawd")) playTo(p, HSounds.AWW_GAWD, p.posX, p.posY + 1, p.posZ, 1.2f, 1f);  // the first time: "Aww gawd..."
        int dim = w.provider.getDimension();
        if (dim == 0) {
            BlockPos g = nearestIndexed(w, OverworldLore.GALLEY, p.getPosition(), 24);
            if (g != null) HAdv.grant(p, "galley");
            altarAmbience(p, w);
            philologistChance(p, w);
        } else if (dim == LuciferTotem.dimId) {
            HAdv.grant(p, "enter_lightfall");
            BlockPos lib = LibraryGen.nearest(w.getSeed(), p.getPosition(), 48);
            if (lib != null) HAdv.grant(p, "library");
        }
        int every = LTConfig.lore.totemWhisperSeconds;
        if (totem && every > 0) {
            net.minecraft.nbt.NBTTagCompound d = HData.get(p);
            if (t - d.getLong("lastWhisper") >= every * 20L || d.getLong("lastWhisper") > t) {
                d.setLong("lastWhisper", t);
                whisper(p);
            }
        }
    }

    static BlockPos nearestIndexed(WorldServer w, String type, BlockPos from, int r) {
        BlockPos best = null; double bd = (double) r * r;
        for (long l : StructureIndex.get(w).all(type)) {
            BlockPos q = BlockPos.fromLong(l);
            double d = q.distanceSq(from);
            if (d <= bd) { bd = d; best = q; }
        }
        return best;
    }

    // ------------------------------------------------------------ the whispers
    static ITextComponent dir(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        String[] k = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        int i = (int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
        int dist = (int) (Math.round(Math.sqrt(dx * dx + dz * dz) / 25.0) * 25);
        return new TextComponentTranslation("whisper.lucifertotem.dir", new TextComponentTranslation("whisper.lucifertotem.dir." + k[i]), Math.max(25, dist));
    }

    /** which hint fits this player now: {key, args...} */
    public static Object[] hint(EntityPlayerMP p) {
        WorldServer w = (WorldServer) p.world;
        BlockPos at = p.getPosition();
        int dim = w.provider.getDimension(), R = LTConfig.lore.whisperSearchChunks;
        if (dim == LuciferTotem.dimId) {
            if (!done(p, "library")) {
                BlockPos lib = LibraryGen.nearest(w.getSeed(), at, 4000);
                if (lib != null) return new Object[]{"whisper.lucifertotem.library", dir(at, lib)};
            }
            if (!done(p, "meet_ammonius")) return new Object[]{"whisper.lucifertotem.meet"};
            return homeHint(p);
        }
        if (dim != 0) return new Object[]{"whisper.lucifertotem.elsewhere"};
        if (!done(p, "lecture_notes")) {
            if (done(p, "galley")) return new Object[]{"whisper.lucifertotem.read"};
            BlockPos g = OverworldLore.nearest(w, OverworldLore.GALLEY, at, R);
            return g != null ? new Object[]{"whisper.lucifertotem.galley", dir(at, g)} : new Object[]{"whisper.lucifertotem.galley_far"};
        }
        if (!done(p, "fallen_altar")) {
            BlockPos a = OverworldLore.nearest(w, OverworldLore.ALTAR, at, R);
            String how = AltarLogic.isDusk(w) ? "whisper.lucifertotem.altar_dusk" : "whisper.lucifertotem.altar";
            return a != null ? new Object[]{how, dir(at, a)} : new Object[]{"whisper.lucifertotem.altar_build"};
        }
        if (!done(p, "enter_lightfall")) return new Object[]{"whisper.lucifertotem.step_through"};
        if (!done(p, "library")) return new Object[]{"whisper.lucifertotem.go_back"};
        return new Object[]{"whisper.lucifertotem.purple." + (1 + RNG.nextInt(5))};
    }

    /** the way home from the Lightfall: the nearest standing gate that leads out, else how to build one */
    public static Object[] homeHint(EntityPlayerMP p) {
        BlockPos g = nearestGateHome(p);
        if (g != null) return new Object[]{"whisper.lucifertotem.home_gate", dir(p.getPosition(), g), g.getX(), g.getY(), g.getZ()};
        return new Object[]{"whisper.lucifertotem.home_build"};
    }

    public static BlockPos nearestGateHome(EntityPlayer p) {
        WorldServer w = (WorldServer) p.world;
        PortalData d = PortalData.get();
        BlockPos best = null; double bd = Double.MAX_VALUE;
        java.util.List<PortalData.Gate> mine = new java.util.ArrayList<>();
        for (PortalData.Gate g : d.gates) if (g.dim == w.provider.getDimension()) mine.add(g);
        mine.sort(java.util.Comparator.comparingDouble(g -> g.base.distanceSq(p.getPosition())));
        int checked = 0;
        for (PortalData.Gate g : mine) {
            if (checked++ >= 4) break;   // at most 4 chunk loads
            if (PortalLogic.gateIntact(w, g.base, g.axis)) { double dd = g.base.distanceSq(p.getPosition()); if (dd < bd) { bd = dd; best = g.base; } }
        }
        return best;
    }

    /** 1.3.0: forces the next whisper to be the sign-off (self-test) */
    public static boolean forceSignOff = false;
    public static void whisper(EntityPlayerMP p) {
        Object[] h = hint(p);
        int so = LTConfig.v130.signOffWhisperOneIn;
        if (forceSignOff || (!LTConfig.testHarness && so > 0 && RNG.nextInt(so) == 0)) { h = new Object[]{"whisper.lucifertotem.signoff"}; forceSignOff = false; }
        Object[] args = new Object[h.length - 1];
        System.arraycopy(h, 1, args, 0, args.length);
        TextComponentTranslation c = new TextComponentTranslation((String) h[0], args);
        c.getStyle().setColor(TextFormatting.DARK_PURPLE).setItalic(true);
        p.sendStatusMessage(c, true);
        lastWhisperKey = (String) h[0];
        whispers++;
    }

    // ------------------------------------------------------------ first login with 1.2.0 inside the Lightfall, no way home known
    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP && !e.player.world.isRemote) loginHint((EntityPlayerMP) e.player);
    }

    public static boolean loginHint(EntityPlayerMP p) {
        if (p.dimension != LuciferTotem.dimId || PortalLogic.returnPoint(p) != null) return false;
        Object[] h = homeHint(p);
        Object[] args = new Object[h.length - 1];
        System.arraycopy(h, 1, args, 0, args.length);
        TextComponentTranslation intro = new TextComponentTranslation("whisper.lucifertotem.login");
        intro.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        TextComponentTranslation c = new TextComponentTranslation((String) h[0], args);
        c.getStyle().setColor(TextFormatting.DARK_PURPLE).setItalic(true);
        p.sendMessage(intro.appendSibling(new net.minecraft.util.text.TextComponentString(" ")).appendSibling(c));
        loginHints++;
        return true;
    }

    // ------------------------------------------------------------ reading the notes
    @SubscribeEvent
    public void rightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!e.getWorld().isRemote && LoreBooks.isNotes(e.getItemStack())) read(e.getEntityPlayer(), e.getItemStack());
    }

    public static boolean read(EntityPlayer p, ItemStack s) {
        if (!LoreBooks.isNotes(s)) return false;
        boolean any = HAdv.grant(p, "lecture_notes");
        return any;
    }

    // ------------------------------------------------------------ the altar calls
    public static void altarAmbience(EntityPlayerMP p, WorldServer w) {
        if (!LTConfig.distantAmbient || LTConfig.lore.altarAmbienceRadius <= 0 || LTConfig.distantVoiceMinutesOverworld <= 0) return;
        BlockPos a = nearestIndexed(w, OverworldLore.ALTAR, p.getPosition(), LTConfig.lore.altarAmbienceRadius);
        if (a == null) return;
        double minutes = LTConfig.distantVoiceMinutesOverworld * LTConfig.lore.altarAmbienceFactor;
        if (RNG.nextDouble() * minutes * 1200.0 >= 40.0) return;   // checked every 40 ticks
        altarVoice(p, a);
    }

    public static void altarVoice(EntityPlayerMP p, BlockPos a) {
        double dx = a.getX() - p.posX, dz = a.getZ() - p.posZ, d = Math.sqrt(dx * dx + dz * dz);
        double k = d > 28 ? 28 / d : 1;   // from the altar's direction, close enough to hear
        playTo(p, HSounds.DISTANT_AMMONIUS, p.posX + dx * k, Math.max(p.posY, a.getY()) + 6, p.posZ + dz * k, 4f, 0.95f + RNG.nextFloat() * 0.1f);
        altarVoices++;
    }

    // ------------------------------------------------------------ the Wandering Philologist
    public static void philologistChance(EntityPlayerMP p, WorldServer w) {
        double pct = LTConfig.lore.philologistChancePercentPerHour;
        if (pct <= 0 || p.isSpectator() || p.isDead) return;
        if (HData.get(p).getInteger("philoVisits") >= LTConfig.lore.philologistMaxPerPlayer) return;
        if (LTConfig.lore.philologistRequiresTotem && !done(p, "aww_gawd")) return;
        // per 2-second check: pct% per hour = pct/100 per 1800 checks
        if (RNG.nextDouble() >= pct / 100.0 / 1800.0) return;
        summonPhilologist(p);
    }

    public static EntityAmmonius summonPhilologist(EntityPlayerMP p) {
        WorldServer w = (WorldServer) p.world;
        if (p.isInWater() || p.isRiding()) return null;
        for (EntityAmmonius a : w.getEntities(EntityAmmonius.class, x -> x.philologist && !x.isDead)) if (a.philoFor != null && a.philoFor.equals(p.getUniqueID())) return null;
        for (int tries = 0; tries < 24; tries++) {
            double ang = RNG.nextDouble() * Math.PI * 2, r = 9 + RNG.nextDouble() * 6;
            int x = (int) Math.floor(p.posX + Math.cos(ang) * r), z = (int) Math.floor(p.posZ + Math.sin(ang) * r);
            for (int dy = 4; dy >= -6; dy--) {
                BlockPos q = new BlockPos(x, (int) p.posY + dy, z);
                if (w.isAirBlock(q) && w.isAirBlock(q.up()) && w.getBlockState(q.down()).isSideSolid(w, q.down(), net.minecraft.util.EnumFacing.UP)
                    && !w.getBlockState(q.down()).getMaterial().isLiquid()) {
                    EntityAmmonius a = new EntityAmmonius(w);
                    a.setLocationAndAngles(x + 0.5, q.getY(), z + 0.5, (float) Math.toDegrees(Math.atan2(-(p.posX - x), p.posZ - z)), 0);
                    a.makePhilologist(p);
                    w.spawnEntity(a);
                    w.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, x + 0.5, q.getY() + 1, z + 0.5, 80, 0.4, 1.0, 0.4, 1.0);
                    w.playSound(null, q, net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1f, 0.8f);
                    net.minecraft.nbt.NBTTagCompound d = HData.get(p);
                    d.setInteger("philoVisits", d.getInteger("philoVisits") + 1);
                    LuciferTotem.LOG.info("[LT] The Wandering Philologist visits {} at {}", p.getName(), q);
                    return a;
                }
            }
        }
        return null;
    }
}
