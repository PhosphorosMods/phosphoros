package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.hillman.EntityDipsas;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** a sand-coloured viper: head + 8 body segments slithering in a sine wave. 32x32 texture. */
public class RenderDipsas extends RenderLiving<EntityDipsas> {
    private static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/dipsas.png");
    private static final ResourceLocation TEX_SACRED = new ResourceLocation(LuciferTotem.MODID, "textures/entity/dipsas_sacred.png");

    public RenderDipsas(RenderManager m) { super(m, new Model(), 0.3f); }
    @Override protected ResourceLocation getEntityTexture(EntityDipsas e) { return e.harmless() ? TEX_SACRED : TEX; }

    public static class Model extends ModelBase {
        final ModelRenderer head, jaw, tongue;
        final ModelRenderer[] seg = new ModelRenderer[8];
        public Model() {
            textureWidth = 32; textureHeight = 32;
            head = new ModelRenderer(this, 0, 0);
            head.addBox(-2f, -2f, -5f, 4, 2, 5);
            head.setRotationPoint(0, 22f, -6f);
            jaw = new ModelRenderer(this, 0, 7);
            jaw.addBox(-1.5f, 0f, -4.5f, 3, 1, 4);
            head.addChild(jaw);
            tongue = new ModelRenderer(this, 14, 7);
            tongue.addBox(-0.5f, -0.5f, -7f, 1, 0, 2);
            head.addChild(tongue);
            for (int i = 0; i < seg.length; i++) {
                float w = i < 5 ? 3f : 3f - (i - 4) * 0.5f;
                seg[i] = new ModelRenderer(this, 0, 12 + (i % 2) * 6);
                seg[i].addBox(-w / 2f, -2f + (3 - w) / 2f, 0f, (int) Math.ceil(w), (int) Math.ceil(w) - 1, 3);
                seg[i].setRotationPoint(0, 22f, -6f + i * 2.6f);
            }
        }

        @Override
        public void render(Entity e, float limbSwing, float limbAmt, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limbSwing, limbAmt, age, yaw, pitch, scale, e);
            head.render(scale);
            for (ModelRenderer s : seg) s.render(scale);
        }

        @Override
        public void setRotationAngles(float limbSwing, float limbAmt, float age, float yaw, float pitch, float scale, Entity e) {
            float t = age * 0.25f + limbSwing * 1.5f;
            float amp = 0.25f + Math.min(1f, limbAmt) * 0.6f;
            for (int i = 0; i < seg.length; i++) {
                float off = MathHelper.sin(t - i * 0.8f) * amp * (1.2f + i * 0.35f);
                seg[i].rotationPointX = off;
                seg[i].rotateAngleY = MathHelper.cos(t - i * 0.8f) * amp * 0.5f;
            }
            head.rotateAngleY = yaw * 0.0174533f * 0.5f + MathHelper.sin(t + 0.8f) * amp * 0.2f;
            head.rotateAngleX = pitch * 0.0174533f * 0.3f;
            jaw.rotateAngleX = ((EntityDipsas) e).hurtTime > 0 ? 0.5f : 0.05f;
            tongue.showModel = MathHelper.sin(age * 0.9f) > 0.6f;
        }
    }
}
