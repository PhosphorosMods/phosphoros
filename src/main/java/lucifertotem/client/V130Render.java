package lucifertotem.client;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.v130.EntityPeerReviewer;
import lucifertotem.v130.EntityThrownBook;
import lucifertotem.v130.V130Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** renderers and layers of 1.3.0 stages 2-5 */
public final class V130Render {
    private V130Render() {}
    static final ResourceLocation MORTAR_TEX = new ResourceLocation(LuciferTotem.MODID, "textures/models/armor/mortarboard.png");

    public static void renderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityPeerReviewer.class, m -> new RenderBiped<EntityPeerReviewer>(m, new ModelBiped(0f, 0f, 64, 64), 0.5f) {
            final ResourceLocation T = new ResourceLocation(LuciferTotem.MODID, "textures/entity/peer_reviewer.png");
            @Override protected ResourceLocation getEntityTexture(EntityPeerReviewer e) { return T; }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityThrownBook.class, m -> new RenderSnowball<>(m, Items.BOOK, Minecraft.getMinecraft().getRenderItem()));
        V130Render2.renderers();
    }

    public static void init() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        for (RenderPlayer rp : rm.getSkinMap().values()) { rp.addLayer(new MortarLayer(rp.getMainModel().bipedHead, false)); rp.addLayer(new MystesLayer(rp)); }
        Render<?> am = rm.entityRenderMap.get(EntityAmmonius.class);
        if (am instanceof RenderBiped) ((RenderBiped<?>) am).addLayer(new MortarLayer(((ModelBiped) ((RenderBiped<?>) am).getMainModel()).bipedHead, true));
        V130Render2.init();
    }

    // ------------------------------------------------------------ the mortarboard: a cap, a square board and a purple tassel. texture 64x32
    static ModelRenderer[] board(ModelBiped owner, float y) {
        ModelRenderer cap = new ModelRenderer(owner, 0, 0);
        cap.setTextureSize(64, 32);
        cap.addBox(-4f, y, -4f, 8, 3, 8, 0.45f);
        ModelRenderer top = new ModelRenderer(owner, 0, 12);
        top.setTextureSize(64, 32);
        top.addBox(-6f, y - 1.2f, -6f, 12, 1, 12, 0.2f);
        ModelRenderer tassel = new ModelRenderer(owner, 48, 0);
        tassel.setTextureSize(64, 32);
        tassel.addBox(5f, y - 0.5f, -1f, 1, 6, 1);
        return new ModelRenderer[]{cap, top, tassel};
    }

    static ModelBiped ARMOR;
    public static ModelBiped mortarboardArmor(ModelBiped def) {
        if (ARMOR == null) {
            ARMOR = new ModelBiped(0f, 0f, 64, 32);
            ARMOR.bipedHead.cubeList.clear();
            ARMOR.bipedHeadwear.cubeList.clear();
            for (ModelRenderer m : board(ARMOR, -10.5f)) ARMOR.bipedHead.addChild(m);
        }
        return ARMOR;
    }

    /** cosmetic mortarboards (players, synced flag) and Doctor Ammonius' mortarboard on top of his mitre */
    public static class MortarLayer implements LayerRenderer<EntityLivingBase> {
        final ModelRenderer head; final boolean ammonius; final ModelBiped holder = new ModelBiped(0f, 0f, 64, 32);
        final ModelRenderer[] parts;
        MortarLayer(ModelRenderer head, boolean ammonius) { this.head = head; this.ammonius = ammonius; parts = board(holder, ammonius ? -20f : -11.5f); }
        @Override public void doRenderLayer(EntityLivingBase e, float ls, float la, float pt, float age, float yaw, float pitch, float scale) {
            if (ammonius) { if (!(e instanceof EntityAmmonius) || !((EntityAmmonius) e).mortarboard()) return; }
            else {
                Integer f = V130Net.CLIENT.get(e.getEntityId());
                if (f == null || (f & V130Net.MORTARBOARD) == 0) return;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(MORTAR_TEX);
            GlStateManager.pushMatrix();
            if (e.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
            head.postRender(scale);
            GlStateManager.color(1f, 1f, 1f, 1f);
            for (ModelRenderer m : parts) m.render(scale);
            GlStateManager.popMatrix();
        }
        @Override public boolean shouldCombineTextures() { return false; }
    }

    /** ΜΥΣΤΗΣ: the Eleusinian initiate's tattoo, a band of Greek letters down the forearm and on the side of the neck (stage 4) */
    public static class MystesLayer implements LayerRenderer<EntityLivingBase> {
        static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/mystes_tattoo.png");
        final RenderPlayer r;
        MystesLayer(RenderPlayer r) { this.r = r; }
        @Override public void doRenderLayer(EntityLivingBase e, float ls, float la, float pt, float age, float yaw, float pitch, float scale) {
            if (!LTConfig.v130.showMystesTattoo || !(e instanceof EntityPlayer)) return;
            Integer f = V130Net.CLIENT.get(e.getEntityId());
            if (f == null || (f & V130Net.MYSTES) == 0) return;
            r.bindTexture(TEX);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            ModelBiped m = r.getMainModel();
            m.setModelAttributes(r.getMainModel());
            // the overlay texture uses the player skin layout; draw it on the base model, slightly inflated, like the Elytra-less outer layer
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.002f, 1.002f, 1.002f);
            m.bipedHead.render(scale); m.bipedBody.render(scale); m.bipedRightArm.render(scale); m.bipedLeftArm.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
        }
        @Override public boolean shouldCombineTextures() { return true; }
    }
}
