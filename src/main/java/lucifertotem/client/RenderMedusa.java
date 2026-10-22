package lucifertotem.client;

import lucifertotem.EntityMedusa;
import lucifertotem.LuciferTotem;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** Translucent, full-bright, pulsing render of the Medusa. */
public class RenderMedusa extends RenderLiving<EntityMedusa> {
    private static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/medusa.png");

    public RenderMedusa(RenderManager m) { super(m, new ModelMedusa(), 0.0f); }

    @Override protected ResourceLocation getEntityTexture(EntityMedusa e) { return TEX; }

    @Override
    protected void preRenderCallback(EntityMedusa e, float pt) {
        float p = e.prevPulse + (e.pulse - e.prevPulse) * pt;
        float s = MathHelper.sin(p);
        GlStateManager.scale(1.0f + 0.10f * s, 1.0f - 0.12f * s, 1.0f + 0.10f * s); // the bell contracts and relaxes
        GlStateManager.translate(0f, 0.05f * MathHelper.sin(p * 0.5f), 0f);           // bobbing
    }

    @Override
    public void doRender(EntityMedusa e, double x, double y, double z, float yaw, float pt) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.enableNormalize();
        super.doRender(e, x, y, z, yaw, pt);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }
}
