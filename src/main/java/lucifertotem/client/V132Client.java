package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.v130.V130Net;
import lucifertotem.v132.EntityDionysus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** 1.3.2 client: the Crown of Thorns (armor model + cosmetic layer) and Dionysus */
public final class V132Client {
    private V132Client() {}
    static final ResourceLocation CROWN_TEX = new ResourceLocation(LuciferTotem.MODID, "textures/models/armor/crown_of_thorns.png");

    public static void renderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityDionysus.class, RenderDionysus::new);
    }

    public static void init() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        for (RenderPlayer rp : rm.getSkinMap().values()) rp.addLayer(new CrownLayer(rp.getMainModel().bipedHead));
    }

    /** a woven circlet: two twisted strands of dried vine with thorns sticking up and out. Head-space coordinates, y up = negative. */
    public static ModelRenderer[] crown(ModelBiped owner, float y) {
        ModelRenderer a = new ModelRenderer(owner, 0, 0); a.setTextureSize(64, 32);
        a.addBox(-4.5f, y, -5.0f, 9, 1, 1);   // front strand
        a.addBox(-4.5f, y, 4.0f, 9, 1, 1);    // back
        a.addBox(-5.0f, y, -4.5f, 1, 1, 9);   // left
        a.addBox(4.0f, y, -4.5f, 1, 1, 9);    // right
        ModelRenderer b = new ModelRenderer(owner, 0, 12); b.setTextureSize(64, 32);   // the second strand, a half step up and twisted round
        b.addBox(-4.0f, y - 1f, -4.8f, 8, 1, 1);
        b.addBox(-4.0f, y - 1f, 3.8f, 8, 1, 1);
        b.addBox(-4.8f, y - 1f, -4.0f, 1, 1, 8);
        b.addBox(3.8f, y - 1f, -4.0f, 1, 1, 8);
        ModelRenderer t = new ModelRenderer(owner, 0, 24); t.setTextureSize(64, 32);   // thorns
        float[][] up = {{-3.5f, -5.4f}, {0f, -5.5f}, {3f, -5.4f}, {-5.4f, -2f}, {-5.4f, 2.5f}, {4.4f, -1f}, {4.4f, 3f}, {-2f, 4.4f}, {2f, 4.4f}};
        for (float[] u : up) t.addBox(u[0], y - 2.2f, u[1], 1, 1, 1, -0.25f);
        float[][] out = {{-2f, -6.0f, 0}, {2f, -6.0f, 0}, {-6.0f, 0f, 1}, {5.0f, 1f, 1}, {0f, 5.0f, 0}};
        for (float[] o : out) t.addBox(o[0], y - 0.4f, o[1], 1, 1, 1, -0.3f);
        return new ModelRenderer[]{a, b, t};
    }

    static ModelBiped ARMOR;
    public static ModelBiped crownArmor() {
        if (ARMOR == null) {
            ARMOR = new ModelBiped(0f, 0f, 64, 32);
            ARMOR.bipedHead.cubeList.clear();
            ARMOR.bipedHeadwear.cubeList.clear();
            for (ModelRenderer m : crown(ARMOR, -7.6f)) ARMOR.bipedHead.addChild(m);
        }
        return ARMOR;
    }

    /** the crown worn as a cosmetic (synced flag), over any helmet */
    public static class CrownLayer implements LayerRenderer<EntityLivingBase> {
        final ModelRenderer head; final ModelBiped holder = new ModelBiped(0f, 0f, 64, 32); final ModelRenderer[] parts;
        CrownLayer(ModelRenderer head) { this.head = head; parts = crown(holder, -8.6f); }
        @Override public void doRenderLayer(EntityLivingBase e, float ls, float la, float pt, float age, float yaw, float pitch, float scale) {
            Integer f = V130Net.CLIENT.get(e.getEntityId());
            if (f == null || (f & V130Net.CROWN) == 0) return;
            Minecraft.getMinecraft().getTextureManager().bindTexture(CROWN_TEX);
            GlStateManager.pushMatrix();
            if (e.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
            head.postRender(scale);
            GlStateManager.scale(1.08f, 1.08f, 1.08f);
            GlStateManager.color(1f, 1f, 1f, 1f);
            for (ModelRenderer m : parts) m.render(scale);
            GlStateManager.popMatrix();
        }
        @Override public boolean shouldCombineTextures() { return false; }
    }

    /** Dionysus, come down off his icon: shoulder-length brown hair, full beard, cream robe with a red sash, sandals, wine splashes */
    public static class ModelDionysus extends ModelBiped {
        public ModelDionysus() {
            super(0f, 0f, 64, 64);
            bipedHeadwear.cubeList.clear();
            ModelRenderer skirt = new ModelRenderer(this, 0, 32);   // the robe falls to just above the sandals
            skirt.addBox(-4.5f, 11f, -2.5f, 9, 10, 5);
            bipedBody.addChild(skirt);
            ModelRenderer back = new ModelRenderer(this, 32, 48);   // hair down to the shoulders
            back.addBox(-4.2f, -8.2f, 3.4f, 8, 11, 1, 0.2f);
            bipedHead.addChild(back);
            ModelRenderer locks = new ModelRenderer(this, 52, 32);
            locks.addBox(-4.7f, -7.5f, -2.0f, 1, 9, 5, 0.1f);
            locks.addBox(3.7f, -7.5f, -2.0f, 1, 9, 5, 0.1f);
            bipedHead.addChild(locks);
            ModelRenderer beard = new ModelRenderer(this, 0, 52);
            beard.addBox(-3.5f, -3.5f, -4.6f, 7, 5, 1);
            bipedHead.addChild(beard);
        }
        @Override public void setLivingAnimations(EntityLivingBase e, float ls, float la, float pt) {
            super.setLivingAnimations(e, ls, la, pt);
            isRiding = e instanceof EntityDionysus && ((EntityDionysus) e).isSitting();
        }
        @Override public void setRotationAngles(float ls, float la, float age, float yaw, float pitch, float scale, Entity e) {
            super.setRotationAngles(ls, la * 0.7f, age, yaw, pitch, scale, e);   // an unhurried, serene walk
        }
    }

    public static class RenderDionysus extends RenderBiped<EntityDionysus> {
        static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/dionysus.png");
        public RenderDionysus(RenderManager m) { super(m, new ModelDionysus(), 0.5f); }
        @Override protected ResourceLocation getEntityTexture(EntityDionysus e) { return TEX; }
        @Override protected void preRenderCallback(EntityDionysus e, float pt) {
            GlStateManager.scale(1.05f, 1.05f, 1.05f);
            if (e.isSitting()) GlStateManager.translate(0f, 0.55f, 0f);
        }
    }
}
