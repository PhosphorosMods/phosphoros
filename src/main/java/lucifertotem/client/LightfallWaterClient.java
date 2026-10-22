package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.world.LightfallProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;

/**
 * CLIENT ONLY. Makes the Lightfall sea glow violet.
 * <p>
 * In 1.12 the vanilla water texture is itself saturated blue (avg RGB 46,67,244) and the biome water colour is only a
 * multiplier, so no water colour can turn it purple (max red is 46/255). While the client is in the Lightfall we
 * therefore point the fluid renderer at greyscale copies of the water sprites, which the biome water colour
 * (LightfallBiome, 0xA040FF) then tints a true violet; outside the Lightfall the vanilla sprites are restored.
 * Underwater fog colour/density and the underwater screen overlay are made violet too.
 * Registered only on the client via {@code @Mod.EventBusSubscriber(value = Side.CLIENT)}.
 */
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class LightfallWaterClient {
    public static final String STILL = LuciferTotem.MODID + ":blocks/lightfall_water_still";
    public static final String FLOW = LuciferTotem.MODID + ":blocks/lightfall_water_flow";
    public static final String OVERLAY = LuciferTotem.MODID + ":blocks/lightfall_water_overlay";
    private static final ResourceLocation UNDERWATER = new ResourceLocation(LuciferTotem.MODID, "textures/misc/lightfall_underwater.png");

    /** underwater fog colour in the Lightfall (vanilla water fog is 0.02, 0.02, 0.2 = dark blue) */
    public static final float FOG_R = 0.40f, FOG_G = 0.12f, FOG_B = 0.66f;

    private static Field fFluid, fWater, fOverlay;
    private static boolean broken;

    static boolean inLightfall(Entity e) {
        return e != null && e.world != null && e.world.provider instanceof LightfallProvider;
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre e) {
        e.getMap().registerSprite(new ResourceLocation(STILL));
        e.getMap().registerSprite(new ResourceLocation(FLOW));
        e.getMap().registerSprite(new ResourceLocation(OVERLAY));
    }

    /** keep the fluid renderer's water sprites in sync with the current dimension (also re-applies after F3+T / resource reloads) */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || broken) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        try {
            if (fFluid == null) {
                fFluid = ReflectionHelper.findField(BlockRendererDispatcher.class, "field_175025_e", "fluidRenderer");
                fWater = ReflectionHelper.findField(BlockFluidRenderer.class, "field_178271_b", "atlasSpritesWater");
                fOverlay = ReflectionHelper.findField(BlockFluidRenderer.class, "field_187501_d", "atlasSpriteWaterOverlay");
            }
            Object fr = fFluid.get(mc.getBlockRendererDispatcher());
            if (fr == null) return;
            TextureAtlasSprite[] water = (TextureAtlasSprite[]) fWater.get(fr);
            TextureMap map = mc.getTextureMapBlocks();
            boolean lf = mc.world.provider instanceof LightfallProvider;
            TextureAtlasSprite still = map.getAtlasSprite(lf ? STILL : "minecraft:blocks/water_still");
            if (still == map.getMissingSprite()) return; // our sprites not stitched (e.g. a resource pack broke them): leave vanilla alone
            if (water[0] == still) return;
            water[0] = still;
            water[1] = map.getAtlasSprite(lf ? FLOW : "minecraft:blocks/water_flow");
            fOverlay.set(fr, map.getAtlasSprite(lf ? OVERLAY : "minecraft:blocks/water_overlay"));
            if (mc.renderGlobal != null) mc.renderGlobal.loadRenderers(); // rebuild chunk meshes with the new sprites
        } catch (Throwable t) {
            broken = true;
            LuciferTotem.LOG.warn("Lightfall: could not swap water sprites (another mod changed the fluid renderer?); the sea keeps the biome tint only", t);
        }
    }

    /** violet underwater fog; runs before the PURPLE-effect tint in ClientProxy so both stack */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fogColor(EntityViewRenderEvent.FogColors e) {
        if (e.getState().getMaterial() == Material.WATER && inLightfall(e.getEntity())) {
            e.setRed(FOG_R);
            e.setGreen(FOG_G);
            e.setBlue(FOG_B);
        }
    }

    /** the glowing sea is a little clearer than vanilla water (0.1) */
    @SubscribeEvent
    public static void fogDensity(EntityViewRenderEvent.FogDensity e) {
        if (e.getState().getMaterial() == Material.WATER && inLightfall(e.getEntity())) {
            float d = 0.06f;
            if (e.getEntity() instanceof EntityLivingBase) d -= EnchantmentHelper.getRespirationModifier((EntityLivingBase) e.getEntity()) * 0.015f;
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
            e.setDensity(Math.max(0.02f, d));
            e.setCanceled(true);
        }
    }

    /** violet version of the underwater screen overlay */
    @SubscribeEvent
    public static void overlay(RenderBlockOverlayEvent e) {
        if (e.getOverlayType() != RenderBlockOverlayEvent.OverlayType.WATER || !inLightfall(e.getPlayer())) return;
        e.setCanceled(true);
        EntityPlayer p = e.getPlayer();
        Minecraft.getMinecraft().getTextureManager().bindTexture(UNDERWATER);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        float f = Math.max(0.6f, p.getBrightness());
        GlStateManager.color(f, f, f, 0.45f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.pushMatrix();
        float u = -p.rotationYaw / 64.0f, v = p.rotationPitch / 64.0f;
        b.begin(7, DefaultVertexFormats.POSITION_TEX);
        b.pos(-1.0, -1.0, -0.5).tex(4.0f + u, 4.0f + v).endVertex();
        b.pos(1.0, -1.0, -0.5).tex(0.0f + u, 4.0f + v).endVertex();
        b.pos(1.0, 1.0, -0.5).tex(0.0f + u, 0.0f + v).endVertex();
        b.pos(-1.0, 1.0, -0.5).tex(4.0f + u, 0.0f + v).endVertex();
        t.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
    }
}
