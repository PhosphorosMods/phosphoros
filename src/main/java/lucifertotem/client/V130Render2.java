package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.v130.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** renderers of stages 3-5 */
public final class V130Render2 {
    private V130Render2() {}
    static ResourceLocation tex(String p) { return new ResourceLocation(LuciferTotem.MODID, "textures/entity/" + p + ".png"); }

    public static void renderers() {
        stage4();
        stage5();
        final ResourceLocation[] BAC = {tex("bacchant_0"), tex("bacchant_1"), tex("bacchant_2"), tex("bacchant_3")};
        RenderingRegistry.registerEntityRenderingHandler(EntityBacchant.class, m -> new RenderBiped<EntityBacchant>(m, new ModelBiped(0f, 0f, 64, 64), 0.5f) {
            @Override protected ResourceLocation getEntityTexture(EntityBacchant e) { return BAC[Math.floorMod(e.look(), BAC.length)]; }
        });
        final ResourceLocation HUN = tex("scythian_huntress");
        RenderingRegistry.registerEntityRenderingHandler(EntityHuntress.class, m -> new RenderBiped<EntityHuntress>(m, new ModelBiped(0f, 0f, 64, 64), 0.5f) {
            @Override protected ResourceLocation getEntityTexture(EntityHuntress e) { return HUN; }
        });
        final ResourceLocation ARROW = tex("toxicon_arrow");
        RenderingRegistry.registerEntityRenderingHandler(EntityToxiconArrow.class, m -> new RenderArrow<EntityToxiconArrow>(m) {
            @Override protected ResourceLocation getEntityTexture(EntityToxiconArrow e) { return ARROW; }
        });
        final ResourceLocation HOUND = tex("hekate_hound"), EYES = tex("hekate_hound_eyes");
        RenderingRegistry.registerEntityRenderingHandler(EntityHekateHound.class, m -> {
            RenderLiving<EntityHekateHound> r = new RenderLiving<EntityHekateHound>(m, new ModelWolf(), 0.5f) {
                @Override protected ResourceLocation getEntityTexture(EntityHekateHound e) { return HOUND; }
                @Override protected float handleRotationFloat(EntityHekateHound e, float pt) { return e.getTailRotation(); }
            };
            r.addLayer(new LayerRenderer<EntityHekateHound>() {
                @Override public void doRenderLayer(EntityHekateHound e, float ls, float la, float pt, float age, float yaw, float pitch, float scale) {
                    r.bindTexture(EYES);
                    GlStateManager.enableBlend(); GlStateManager.disableAlpha();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    GlStateManager.depthMask(!e.isInvisible());
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680f, 0f);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
                    r.getMainModel().render(e, ls, la, age, yaw, pitch, scale);
                    Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
                    int i = e.getBrightnessForRender();
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536, i / 65536);
                    GlStateManager.depthMask(true); GlStateManager.disableBlend(); GlStateManager.enableAlpha();
                }
                @Override public boolean shouldCombineTextures() { return false; }
            });
            return r;
        });
    }

    static void stage4() {
        final ResourceLocation GOAT = tex("band_goat"), SINGER = tex("band_singer"), LUC = tex("lucifer"), LUC_GLOW = tex("lucifer_glow");
        RenderingRegistry.registerEntityRenderingHandler(EntityBandGoat.class, m -> new RenderLiving<EntityBandGoat>(m, new ModelGoat(), 0.6f) {
            @Override protected ResourceLocation getEntityTexture(EntityBandGoat e) { return GOAT; }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityBandSinger.class, m -> new RenderBiped<EntityBandSinger>(m, new ModelSnakeHair(), 0.5f) {
            @Override protected ResourceLocation getEntityTexture(EntityBandSinger e) { return SINGER; }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityLucifer.class, m -> {
            RenderBiped<EntityLucifer> r = new RenderBiped<EntityLucifer>(m, new ModelBiped(0f, 0f, 64, 64), 0.5f) {
                @Override protected ResourceLocation getEntityTexture(EntityLucifer e) { return LUC; }
            };
            r.addLayer(new LayerRenderer<EntityLucifer>() {      // the morning star crown and the gold trim glow in the dark
                @Override public void doRenderLayer(EntityLucifer e, float ls, float la, float pt, float age, float yaw, float pitch, float scale) {
                    r.bindTexture(LUC_GLOW);
                    GlStateManager.enableBlend(); GlStateManager.disableAlpha();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680f, 0f);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    r.getMainModel().render(e, ls, la, age, yaw, pitch, scale);
                    int i = e.getBrightnessForRender();
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536, i / 65536);
                    GlStateManager.disableBlend(); GlStateManager.enableAlpha();
                }
                @Override public boolean shouldCombineTextures() { return false; }
            });
            return r;
        });
    }

    static void stage5() {
        final ResourceLocation DRA = tex("drakaina"), HYD = tex("hydra");
        RenderingRegistry.registerEntityRenderingHandler(EntityDrakaina.class, m -> new RenderLiving<EntityDrakaina>(m, new ModelDrakaina(), 0.7f) {
            @Override protected ResourceLocation getEntityTexture(EntityDrakaina e) { return DRA; }
            @Override protected void preRenderCallback(EntityDrakaina e, float pt) { GlStateManager.scale(1.25f, 1.25f, 1.25f); }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityHydra.class, m -> new RenderLiving<EntityHydra>(m, new ModelHydra(), 1.3f) {
            @Override protected ResourceLocation getEntityTexture(EntityHydra e) { return HYD; }
            @Override protected void preRenderCallback(EntityHydra e, float pt) { GlStateManager.scale(1.4f, 1.4f, 1.4f); }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityVenomSpit.class, m -> new net.minecraft.client.renderer.entity.RenderSnowball<>(m, net.minecraft.init.Items.SLIME_BALL, Minecraft.getMinecraft().getRenderItem()));
    }

    public static void init() { MinecraftForge.EVENT_BUS.register(new V130Render2()); }

    /** silphium-seasoned food says so */
    @SubscribeEvent
    public void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e) {
        if (e.getItemStack().hasTagCompound() && e.getItemStack().getTagCompound().getBoolean(Crops.SEASONED))
            e.getToolTip().add(net.minecraft.util.text.TextFormatting.GOLD + net.minecraft.client.resources.I18n.format("tooltip.lucifertotem.seasoned"));
    }

    /** Mania: you cannot stop running and your head will not keep still */
    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        if (p == null || Minecraft.getMinecraft().isGamePaused() || !p.isPotionActive(V130.MANIA)) return;
        if (p.movementInput != null && p.movementInput.moveForward > 0 && !p.isSneaking() && p.getFoodStats().getFoodLevel() > 6) p.setSprinting(true);
        p.rotationYaw += (float) (Math.sin(p.ticksExisted * 0.7) * 2.2 + (p.getRNG().nextFloat() - 0.5f) * 3f);
        p.rotationPitch += (p.getRNG().nextFloat() - 0.5f) * 1.5f;
    }
}
