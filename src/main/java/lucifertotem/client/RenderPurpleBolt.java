package lucifertotem.client;

import lucifertotem.EntityPurpleBolt;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

/** Vanilla RenderLightningBolt geometry, recoloured to a violet Morningstar bolt with a pale-lilac core. */
public class RenderPurpleBolt extends Render<EntityPurpleBolt> {
    public RenderPurpleBolt(RenderManager m) { super(m); }

    @Override
    public void doRender(EntityPurpleBolt e, double x, double y, double z, float yaw, float pt) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        double[] ax = new double[8];
        double[] az = new double[8];
        double ox = 0.0, oz = 0.0;
        Random r0 = new Random(e.boltVertex);
        for (int i = 7; i >= 0; --i) {
            ax[i] = ox; az[i] = oz;
            ox += (double) (r0.nextInt(11) - 5);
            oz += (double) (r0.nextInt(11) - 5);
        }
        for (int layer = 0; layer < 4; ++layer) {
            Random r = new Random(e.boltVertex);
            // outer layers deep violet, inner core pale lilac
            float cr = layer == 0 ? 0.85f : 0.60f, cg = layer == 0 ? 0.70f : 0.18f, cb = layer == 0 ? 1.00f : 0.95f;
            for (int branch = 0; branch < 3; ++branch) {
                int top = 7, bottom = 0;
                if (branch > 0) top = 7 - branch;
                if (branch > 0) bottom = top - 2;
                double px = ax[top] - ox, pz = az[top] - oz;
                for (int k = top; k >= bottom; --k) {
                    double lx = px, lz = pz;
                    if (branch == 0) { px += (double) (r.nextInt(11) - 5); pz += (double) (r.nextInt(11) - 5); }
                    else { px += (double) (r.nextInt(31) - 15); pz += (double) (r.nextInt(31) - 15); }
                    buf.begin(5, DefaultVertexFormats.POSITION_COLOR);
                    double w1 = 0.1 + (double) layer * 0.2;
                    if (branch == 0) w1 *= (double) k * 0.1 + 1.0;
                    double w2 = 0.1 + (double) layer * 0.2;
                    if (branch == 0) w2 *= (double) (k - 1) * 0.1 + 1.0;
                    for (int v = 0; v < 5; ++v) {
                        double x1 = x + 0.5 - w1, z1 = z + 0.5 - w1;
                        if (v == 1 || v == 2) x1 += w1 * 2.0;
                        if (v == 2 || v == 3) z1 += w1 * 2.0;
                        double x2 = x + 0.5 - w2, z2 = z + 0.5 - w2;
                        if (v == 1 || v == 2) x2 += w2 * 2.0;
                        if (v == 2 || v == 3) z2 += w2 * 2.0;
                        buf.pos(x2 + px, y + (double) (k * 16), z2 + pz).color(cr, cg, cb, 0.32f).endVertex();
                        buf.pos(x1 + lx, y + (double) ((k + 1) * 16), z1 + lz).color(cr, cg, cb, 0.32f).endVertex();
                    }
                    tess.draw();
                }
            }
        }
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPurpleBolt e) { return null; }
}
