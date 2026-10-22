package lucifertotem.client;

import lucifertotem.LuciferTotem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;

import java.util.Random;

/**
 * Eternal violet dusk: a gradient sky dome, a rose-gold afterglow along the western horizon, a dense field of
 * twinkling stars, and the enormous Morningstar hanging low in the west with a soft halo.
 */
public class LightfallSkyRenderer extends IRenderHandler {
    public static final LightfallSkyRenderer INSTANCE = new LightfallSkyRenderer();
    private static final ResourceLocation STAR_TEX = new ResourceLocation(LuciferTotem.MODID, "textures/environment/morning_star.png");
    private static final ResourceLocation HALO_TEX = new ResourceLocation(LuciferTotem.MODID, "textures/environment/morning_star_halo.png");
    private float[] stars; // x,y,z,size,phase per star

    private void buildStars() {
        Random r = new Random(10842L);
        float[] out = new float[1800 * 5];
        int n = 0;
        for (int i = 0; i < 1800; i++) {
            double x = r.nextFloat() * 2f - 1f, y = r.nextFloat() * 2f - 1f, z = r.nextFloat() * 2f - 1f;
            double d = x * x + y * y + z * z;
            if (d < 1.0 && d > 0.01) {
                d = 1.0 / Math.sqrt(d);
                out[n * 5] = (float) (x * d); out[n * 5 + 1] = (float) (y * d); out[n * 5 + 2] = (float) (z * d);
                out[n * 5 + 3] = 0.12f + r.nextFloat() * 0.18f;
                out[n * 5 + 4] = r.nextFloat() * 6.283f;
                n++;
            }
        }
        stars = new float[n * 5];
        System.arraycopy(out, 0, stars, 0, n * 5);
    }

    @Override
    public void render(float pt, WorldClient world, Minecraft mc) {
        if (stars == null) buildStars();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Vec3d sky = world.getSkyColor(mc.getRenderViewEntity(), pt);
        float dim = 1.0f - 0.6f * HClient.ritual;
        float sr = (float) sky.x * dim, sg = (float) sky.y * dim, sb = (float) sky.z * dim;

        boolean cull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.enableFog();
        GlStateManager.color(sr, sg, sb);
        // upper sky plane (fog blends it into the horizon colour)
        buf.begin(7, DefaultVertexFormats.POSITION);
        buf.pos(-384, 16, -384).endVertex(); buf.pos(384, 16, -384).endVertex();
        buf.pos(384, 16, 384).endVertex(); buf.pos(-384, 16, 384).endVertex();
        tess.draw();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderHelper.disableStandardItemLighting();

        // western afterglow: a coloured fan hugging the horizon under the Morningstar (west = -X)
        GlStateManager.shadeModel(7425);
        buf.begin(6, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(-100, 6, 0).color(1.0f, 0.62f, 0.78f, 0.75f).endVertex();
        for (int j = 0; j <= 24; j++) {
            float a = (float) (j * Math.PI / 24.0) - (float) (Math.PI / 2);
            float s = MathHelper.sin(a), c = MathHelper.cos(a);
            buf.pos(-c * 85f, 38f * c - 4f, s * 125f).color(0.55f, 0.20f, 0.70f, 0.0f).endVertex();
        }
        tess.draw();
        GlStateManager.shadeModel(7424);

        // stars (twinkle slowly)
        float t = (world.getTotalWorldTime() + pt) * 0.05f;
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < stars.length; i += 5) {
            double sx = stars[i], sy = stars[i + 1], sz = stars[i + 2];
            if (sy < -0.05) continue;
            float size = stars[i + 3];
            float a = 0.55f + 0.45f * MathHelper.sin(t + stars[i + 4]);
            float bright = a * (float) Math.min(1.0, (sy + 0.05) * 4.0);
            double px = sx * 100, py = sy * 100, pz = sz * 100;
            double ang1 = Math.atan2(sx, sz), s1 = Math.sin(ang1), c1 = Math.cos(ang1);
            double ang2 = Math.atan2(Math.sqrt(sx * sx + sz * sz), sy), s2 = Math.sin(ang2), c2 = Math.cos(ang2);
            double rot = stars[i + 4], s3 = Math.sin(rot), c3 = Math.cos(rot);
            for (int v = 0; v < 4; v++) {
                double a1 = ((v & 2) - 1) * size, a2 = ((v + 1 & 2) - 1) * size;
                double b1 = a1 * c3 - a2 * s3, b2 = a2 * c3 + a1 * s3;
                double dy = b1 * s2, dd = -b1 * c2;
                double dx = dd * s1 - b2 * c1, dz = b2 * s1 + dd * c1;
                buf.pos(px + dx, py + dy, pz + dz).color(0.95f, 0.85f, 1.0f, bright).endVertex();
            }
        }
        tess.draw();

        // the Morningstar, low in the west: halo then star (additive)
        GlStateManager.enableTexture2D();
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
        float rit = HClient.ritual; // Thessalian witches drawing the star down: it sinks and swells
        GlStateManager.rotate(67.0f + 15.0f * rit, 1.0f, 0.0f, 0.0f);   // ~23 degrees above the western horizon (down to ~8 during a ritual)
        GlStateManager.rotate(t * 2.0f % 360f, 0.0f, 1.0f, 0.0f); // slow shimmer rotation
        float pulse = 1.0f + 0.04f * MathHelper.sin(t * 1.7f);
        mc.getTextureManager().bindTexture(HALO_TEX);
        GlStateManager.color(1.0f, 0.75f, 1.0f, 0.85f);
        float grow = 1.0f + 1.6f * rit;
        quad(buf, tess, 80.0f * pulse * grow);
        mc.getTextureManager().bindTexture(STAR_TEX);
        GlStateManager.color(1.0f, 1.0f - 0.3f * rit, 1.0f - 0.15f * rit, 1.0f);
        quad(buf, tess, 34.0f * grow);
        GlStateManager.popMatrix();

        GlStateManager.disableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
        // dark lower hemisphere beneath the horizon
        GlStateManager.color(sr * 0.35f, sg * 0.25f, sb * 0.45f);
        buf.begin(7, DefaultVertexFormats.POSITION);
        buf.pos(-384, -16, 384).endVertex(); buf.pos(384, -16, 384).endVertex();
        buf.pos(384, -16, -384).endVertex(); buf.pos(-384, -16, -384).endVertex();
        tess.draw();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        if (cull) GlStateManager.enableCull();
    }

    private static void quad(BufferBuilder buf, Tessellator tess, float s) {
        buf.begin(7, DefaultVertexFormats.POSITION_TEX);
        buf.pos(-s, 100.0, -s).tex(0.0, 0.0).endVertex();
        buf.pos(s, 100.0, -s).tex(1.0, 0.0).endVertex();
        buf.pos(s, 100.0, s).tex(1.0, 1.0).endVertex();
        buf.pos(-s, 100.0, s).tex(0.0, 1.0).endVertex();
        tess.draw();
    }
}
