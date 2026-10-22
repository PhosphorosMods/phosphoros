package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.paint.EntityLTPainting;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/** Like vanilla RenderPainting, but the front comes from the painting's own texture; back and edges from the vanilla atlas' wooden back (read only). */
public class RenderLTPainting extends Render<EntityLTPainting> {
    static final ResourceLocation VANILLA = new ResourceLocation("textures/painting/paintings_kristoffer_zetterstrand.png");
    static final Map<String, ResourceLocation> TEX = new HashMap<>();

    public RenderLTPainting(RenderManager m) { super(m); }

    @Override
    protected ResourceLocation getEntityTexture(EntityLTPainting e) {
        return TEX.computeIfAbsent(e.art.name, n -> new ResourceLocation(LuciferTotem.MODID, "textures/painting/" + n + ".png"));
    }

    @Override
    public void doRender(EntityLTPainting e, double x, double y, double z, float yaw, float pt) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0f - yaw, 0.0f, 1.0f, 0.0f);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
        if (renderOutlines) { GlStateManager.enableColorMaterial(); GlStateManager.enableOutlineMode(getTeamColor(e)); }
        int w = e.art.px(), h = e.art.py();
        bindEntityTexture(e);
        quads(e, w, h, true);
        bindTexture(VANILLA);
        quads(e, w, h, false);
        if (renderOutlines) { GlStateManager.disableOutlineMode(); GlStateManager.disableColorMaterial(); }
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(e, x, y, z, yaw, pt);
    }

    private void quads(EntityLTPainting e, int w, int h, boolean front) {
        float x0 = -w / 2.0f, y0 = -h / 2.0f;
        Tessellator t = Tessellator.getInstance();
        for (int i = 0; i < w / 16; i++) for (int j = 0; j < h / 16; j++) {
            float r = x0 + (i + 1) * 16, l = x0 + i * 16, top = y0 + (j + 1) * 16, bot = y0 + j * 16;
            light(e, (r + l) / 2.0f, (top + bot) / 2.0f);
            BufferBuilder b = t.getBuffer();
            b.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            if (front) {
                float u0 = (float) (w - i * 16) / w, u1 = (float) (w - (i + 1) * 16) / w, v0 = (float) (h - j * 16) / h, v1 = (float) (h - (j + 1) * 16) / h;
                b.pos(r, bot, -0.5).tex(u1, v0).normal(0, 0, -1).endVertex();
                b.pos(l, bot, -0.5).tex(u0, v0).normal(0, 0, -1).endVertex();
                b.pos(l, top, -0.5).tex(u0, v1).normal(0, 0, -1).endVertex();
                b.pos(r, top, -0.5).tex(u1, v1).normal(0, 0, -1).endVertex();
            } else {
                b.pos(r, top, 0.5).tex(0.75, 0.0).normal(0, 0, 1).endVertex();
                b.pos(l, top, 0.5).tex(0.8125, 0.0).normal(0, 0, 1).endVertex();
                b.pos(l, bot, 0.5).tex(0.8125, 0.0625).normal(0, 0, 1).endVertex();
                b.pos(r, bot, 0.5).tex(0.75, 0.0625).normal(0, 0, 1).endVertex();
                b.pos(r, top, -0.5).tex(0.75, 0.001953125).normal(0, 1, 0).endVertex();
                b.pos(l, top, -0.5).tex(0.8125, 0.001953125).normal(0, 1, 0).endVertex();
                b.pos(l, top, 0.5).tex(0.8125, 0.001953125).normal(0, 1, 0).endVertex();
                b.pos(r, top, 0.5).tex(0.75, 0.001953125).normal(0, 1, 0).endVertex();
                b.pos(r, bot, 0.5).tex(0.75, 0.001953125).normal(0, -1, 0).endVertex();
                b.pos(l, bot, 0.5).tex(0.8125, 0.001953125).normal(0, -1, 0).endVertex();
                b.pos(l, bot, -0.5).tex(0.8125, 0.001953125).normal(0, -1, 0).endVertex();
                b.pos(r, bot, -0.5).tex(0.75, 0.001953125).normal(0, -1, 0).endVertex();
                b.pos(r, top, 0.5).tex(0.751953125, 0.0).normal(-1, 0, 0).endVertex();
                b.pos(r, bot, 0.5).tex(0.751953125, 0.0625).normal(-1, 0, 0).endVertex();
                b.pos(r, bot, -0.5).tex(0.751953125, 0.0625).normal(-1, 0, 0).endVertex();
                b.pos(r, top, -0.5).tex(0.751953125, 0.0).normal(-1, 0, 0).endVertex();
                b.pos(l, top, -0.5).tex(0.751953125, 0.0).normal(1, 0, 0).endVertex();
                b.pos(l, bot, -0.5).tex(0.751953125, 0.0625).normal(1, 0, 0).endVertex();
                b.pos(l, bot, 0.5).tex(0.751953125, 0.0625).normal(1, 0, 0).endVertex();
                b.pos(l, top, 0.5).tex(0.751953125, 0.0).normal(1, 0, 0).endVertex();
            }
            t.draw();
        }
    }

    private void light(EntityLTPainting e, float cx, float cy) {
        int x = MathHelper.floor(e.posX), y = MathHelper.floor(e.posY + cy / 16.0f), z = MathHelper.floor(e.posZ);
        EnumFacing f = e.facingDirection;
        if (f == EnumFacing.NORTH) x = MathHelper.floor(e.posX + cx / 16.0f);
        if (f == EnumFacing.WEST) z = MathHelper.floor(e.posZ - cx / 16.0f);
        if (f == EnumFacing.SOUTH) x = MathHelper.floor(e.posX - cx / 16.0f);
        if (f == EnumFacing.EAST) z = MathHelper.floor(e.posZ + cx / 16.0f);
        int l = renderManager.world.getCombinedLight(new BlockPos(x, y, z), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l % 65536, l / 65536);
        GlStateManager.color(1.0f, 1.0f, 1.0f);
    }
}
