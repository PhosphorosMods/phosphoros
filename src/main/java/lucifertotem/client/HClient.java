package lucifertotem.client;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Client side of 1.1.0: renderers, models, effect icons, post shaders, Insania phantoms, the witches' sky, distant voice. */
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class HClient {
    private static final ResourceLocation ICONS = new ResourceLocation(LuciferTotem.MODID, "textures/gui/effects_110.png");
    private static final Random RNG = new Random();
    /** 0..1 progress of the nearest Thessalian ritual (smoothed), drives the sky */
    public static float ritual = 0f;
    private static float ritualTarget = 0f;

    public static void drawEffectIcon(int icon, int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, icon * 18, 0, 18, 18, 144f, 18f);
    }

    public static void renderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityDipsas.class, RenderDipsas::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityAmmonius.class, RenderHillman.Ammonius::new);
        RenderingRegistry.registerEntityRenderingHandler(lucifertotem.paint.EntityLTPainting.class, RenderLTPainting::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPythia.class, RenderHillman.Pythia::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityGorgon.class, RenderHillman.Gorgon::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityThessalianWitch.class, RenderHillman.Witch::new);
        RenderingRegistry.registerEntityRenderingHandler(lucifertotem.hillman.EntityEnchantress.class, RenderLibrary.Enchantress::new);
        RenderingRegistry.registerEntityRenderingHandler(lucifertotem.hillman.EntityBookWraith.class, RenderLibrary.BookWraith::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityMedeaFlask.class, m -> new RenderSnowball<>(m, HItems.MEDEA_FLASK, Minecraft.getMinecraft().getRenderItem()));
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : HItems.allWith130()) ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        ModelLoader.setCustomStateMapper(HBlocks.MANDRAKE, new StateMap.Builder().ignore(HBlocks.BlockMandrake.CIRCLES).build());
        ModelLoader.setCustomStateMapper(HBlocks.MEDEA_FIRE, new StateMap.Builder().ignore(HBlocks.BlockMedeaFire.AGE).build());
    }

    /** Sibylline Leaves open in the vanilla written-book screen */
    public static void openBook(EntityPlayer p, ItemStack s) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound t = new NBTTagCompound();
        NBTTagList pages = new NBTTagList();
        for (String pg : HItems.ItemSibylline.pages(s)) pages.appendTag(new NBTTagString(pg));
        t.setTag("pages", pages);
        t.setString("title", net.minecraft.client.resources.I18n.format("item.lucifertotem.sibylline_leaves.name"));
        t.setString("author", "Sibyl");
        t.setBoolean("resolved", true);
        book.setTagCompound(t);
        Minecraft.getMinecraft().displayGuiScreen(new net.minecraft.client.gui.GuiScreenBook(p, book, false));
    }

    // ------------------------------------------------------------ tick
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) { phantoms.clear(); ourShader = null; ritual = ritualTarget = 0; return; }
        if (mc.isGamePaused()) return;
        EntityPlayer p = mc.player;
        shaders(mc, p);
        insania(mc, p);
        distantVoice(mc, p);
        // the witches' ritual
        if (mc.world.getTotalWorldTime() % 10 == 0) {
            float best = 0;
            for (Entity en : mc.world.loadedEntityList)
                if (en instanceof EntityThessalianWitch && en.getDistanceSq(p) < 160 * 160) best = Math.max(best, ((EntityThessalianWitch) en).progress() / 1000f);
            ritualTarget = mc.world.provider.getDimension() == LuciferTotem.dimId ? best : 0;
        }
        ritual += (ritualTarget - ritual) * 0.05f;
        if (Math.abs(ritual - ritualTarget) < 0.001f) ritual = ritualTarget;
    }

    // ------------------------------------------------------------ post shaders (only ever stop the ones we started)
    private static String ourShader = null;

    static void shaders(Minecraft mc, EntityPlayer p) {
        if (!net.minecraft.client.renderer.OpenGlHelper.shadersSupported) return;
        String want = null;
        if (LTConfig.effectShaders) {
            if (p.isPotionActive(HPotions.PETRIFIED)) want = "desaturate";
            else if (p.isPotionActive(HPotions.INSANIA)) want = "wobble";
            else if (p.isPotionActive(HPotions.PNEUMA)) want = "phosphor";
        }
        ShaderGroup g = mc.entityRenderer.getShaderGroup();
        String active = g == null ? null : g.getShaderGroupName();
        boolean oursActive = ourShader != null && active != null && active.endsWith("shaders/post/" + ourShader + ".json");
        if (ourShader != null && !oursActive) ourShader = null; // someone else replaced it (spectating, F4...)
        if (java.util.Objects.equals(want, ourShader)) return;
        if (ourShader != null) { mc.entityRenderer.stopUseShader(); ourShader = null; }
        if (want != null && !mc.entityRenderer.isShaderActive()) {
            mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/" + want + ".json"));
            ourShader = want;
        }
    }

    // ------------------------------------------------------------ Insania: phantoms that are not there
    static final List<EntityLiving> phantoms = new ArrayList<>();
    private static int nextPhantomId = -7_000_000;

    static void insania(Minecraft mc, EntityPlayer p) {
        WorldClient w = mc.world;
        boolean mad = p.isPotionActive(HPotions.INSANIA);
        phantoms.removeIf(ph -> {
            boolean gone = !mad || ph.ticksExisted > 160 || ph.getDistanceSq(p) < 2.0 || ph.world != w;
            if (!gone) { // creep closer, then fade
                double dx = p.posX - ph.posX, dz = p.posZ - ph.posZ, d = Math.sqrt(dx * dx + dz * dz);
                double sx = dx / d * 0.06, sz = dz / d * 0.06;
                ph.motionX = 0; ph.motionZ = 0;
                ph.rotationYaw = ph.rotationYawHead = ph.renderYawOffset = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
                ph.move(net.minecraft.entity.MoverType.SELF, sx, 0, sz);
                ph.limbSwingAmount = 0.6f; ph.limbSwing += 0.4f;
            } else if (ph.world == w) w.removeEntityFromWorld(ph.getEntityId());
            return gone;
        });
        if (!mad) return;
        if (LTConfig.insaniaPhantoms && phantoms.size() < 3 && RNG.nextInt(90) == 0) {
            double a = RNG.nextDouble() * Math.PI * 2, r = 7 + RNG.nextDouble() * 7;
            int x = MathHelper.floor(p.posX + Math.cos(a) * r), z = MathHelper.floor(p.posZ + Math.sin(a) * r);
            BlockPos at = null;
            for (int dy = 4; dy >= -4; dy--) {
                BlockPos q = new BlockPos(x, MathHelper.floor(p.posY) + dy, z);
                if (w.isAirBlock(q) && w.isAirBlock(q.up()) && w.getBlockState(q.down()).isSideSolid(w, q.down(), net.minecraft.util.EnumFacing.UP)) { at = q; break; }
            }
            if (at != null) {
                int k = RNG.nextInt(3);
                EntityLiving ph = k == 0 ? new EntityZombie(w) { @Override public boolean canBePushed() { return false; } @Override public boolean canBeCollidedWith() { return true; } }
                    : k == 1 ? new EntitySkeleton(w)
                    : new EntityCreeper(w) { @Override public boolean canBePushed() { return false; } };
                ph.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                ph.getEntityData().setBoolean("lt_phantom", true);
                w.addEntityToWorld(nextPhantomId--, ph);
                phantoms.add(ph);
            }
        }
        if (RNG.nextInt(220) == 0) { // whispers of things that are not there
            SoundEvent[] s = {SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundEvents.ENTITY_CREEPER_PRIMED, SoundEvents.ENTITY_SKELETON_AMBIENT, SoundEvents.ENTITY_SPIDER_AMBIENT, SoundEvents.BLOCK_WOODEN_DOOR_OPEN};
            double a = RNG.nextDouble() * Math.PI * 2;
            w.playSound(p.posX + Math.cos(a) * 4, p.posY, p.posZ + Math.sin(a) * 4, s[RNG.nextInt(s.length)], SoundCategory.HOSTILE, 0.7f, 0.9f + RNG.nextFloat() * 0.2f, false);
        }
    }

    public static boolean isPhantom(Entity e) { return e != null && e.world.isRemote && e.getEntityData().getBoolean("lt_phantom"); }

    /** swinging at a phantom: it simply is not there */
    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (isPhantom(e.getTarget())) {
            e.setCanceled(true);
            ((WorldClient) e.getTarget().world).removeEntityFromWorld(e.getTarget().getEntityId());
            phantoms.remove(e.getTarget());
        }
    }

    // ------------------------------------------------------------ distant voice (client only, far away)
    public static int distantPlayed = 0;

    /** chance per tick: 1 / (minutes * 1200); 0 minutes or the master switch off = never */
    public static int minutesFor(int dim) {
        if (!LTConfig.distantAmbient) return 0;
        if (dim == LuciferTotem.dimId) return LTConfig.distantVoiceMinutesLightfall;
        if (dim == 0) return LTConfig.distantVoiceMinutesOverworld;
        return 0;
    }

    private static int loggedDim = Integer.MIN_VALUE;
    static void distantVoice(Minecraft mc, EntityPlayer p) {
        int min = minutesFor(mc.world.provider.getDimension());
        if (loggedDim != mc.world.provider.getDimension()) {
            loggedDim = mc.world.provider.getDimension();
            LuciferTotem.LOG.debug("[LT] distant-voice tick active in dim {}: about every {} min ({})", loggedDim, min, min > 0 ? "on" : "off here");
        }
        if (min <= 0 || RNG.nextInt(min * 1200) != 0) return;
        playDistant(mc, p);
    }

    public static void playDistant(Minecraft mc, EntityPlayer p) {
        double a = RNG.nextDouble() * Math.PI * 2, r = 20 + RNG.nextDouble() * 12; // volume 4 = audible to 64 blocks; 20-32 away is far but clear
        distantPlayed++;
        mc.getSoundHandler().playSound(new PositionedSoundRecord(HSounds.DISTANT_AMMONIUS, SoundCategory.AMBIENT, 4.0f, 0.95f + RNG.nextFloat() * 0.1f,
            (float) (p.posX + Math.cos(a) * r), (float) (p.posY + 6 + RNG.nextDouble() * 10), (float) (p.posZ + Math.sin(a) * r)));
    }

    // ------------------------------------------------------------ fog: henbane turns the Lightfall blood-orange; the ritual darkens it
    @SubscribeEvent
    public static void fog(EntityViewRenderEvent.FogColors e) {
        Entity v = e.getEntity();
        if (v instanceof EntityPlayer && ((EntityPlayer) v).isPotionActive(HPotions.INSANIA)) {
            float t = (v.ticksExisted + (float) e.getRenderPartialTicks()) * 0.05f;
            float k = 0.6f + 0.15f * MathHelper.sin(t);
            e.setRed(e.getRed() * (1 - k) + 0.85f * k);
            e.setGreen(e.getGreen() * (1 - k) + 0.32f * k);
            e.setBlue(e.getBlue() * (1 - k) + 0.12f * k);
        }
        if (ritual > 0) {
            float d = 1 - 0.55f * ritual;
            e.setRed(e.getRed() * d); e.setGreen(e.getGreen() * d); e.setBlue(e.getBlue() * d);
        }
    }
}
