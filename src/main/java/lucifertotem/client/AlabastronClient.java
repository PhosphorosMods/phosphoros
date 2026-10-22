package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.hillman.Alabastron;
import lucifertotem.hillman.HItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** client side of the Alabastron: which entities have one lodged, the stuck-on-the-lower-back layer, self-removal */
@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class AlabastronClient {
    public static final Set<Integer> LODGED = ConcurrentHashMap.newKeySet();
    private static ItemStack stack;

    public static void state(int id, boolean lodged) {
        Minecraft.getMinecraft().addScheduledTask(() -> { if (lodged) LODGED.add(id); else LODGED.remove(id); });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        int n = 0;
        for (RenderPlayer rp : rm.getSkinMap().values()) { rp.addLayer(new Layer(rp)); n++; }
        for (Render<?> r : rm.entityRenderMap.values())
            if (r instanceof RenderLivingBase && !(r instanceof RenderPlayer)) { ((RenderLivingBase) r).addLayer(new Layer((RenderLivingBase<?>) r)); n++; }
        LuciferTotem.LOG.info("Alabastron layer added to {} renderers", n);
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload e) { if (e.getWorld().isRemote) LODGED.clear(); }

    /** sneak + right-click air with an empty main hand: pull your own alabastron out */
    @SubscribeEvent
    public static void rightClickEmpty(PlayerInteractEvent.RightClickEmpty e) {
        if (e.getHand() != EnumHand.MAIN_HAND || !e.getEntityPlayer().isSneaking() || !e.getEntityPlayer().getHeldItemMainhand().isEmpty()) return;
        if (LODGED.contains(e.getEntityPlayer().getEntityId()) && Alabastron.NET != null) Alabastron.NET.sendToServer(new Alabastron.MsgPull());
    }

    /** the configured lodged size, clamped to the documented range */
    public static double lodgedScale() { return Math.max(0.5, Math.min(2.5, lucifertotem.LTConfig.alabastronLodgedScale)); }

    /** 1.3.1: the 3D model swaps to its purple-tinted twin when the flask is laced */
    public static void registerOverrides() {
        if (HItems.ALABASTRON == null) return;
        HItems.ALABASTRON.addPropertyOverride(new net.minecraft.util.ResourceLocation(LuciferTotem.MODID, "laced"), (s, w, e) -> Alabastron.laced(s) ? 1f : 0f);
    }

    /** seat anchor on a biped body (pixels below the shoulder pivot / behind the body centre) and the flask's tilt */
    public static final float SEAT_Y = 11.5f, SEAT_Z = 2.0f, LODGE_PITCH = -110f, LIP_INSET = 0.40f;

    public static class Layer implements LayerRenderer<EntityLivingBase> {
        final RenderLivingBase<?> r;
        Layer(RenderLivingBase<?> r) { this.r = r; }

        @Override
        public void doRenderLayer(EntityLivingBase e, float limbSwing, float limbAmt, float partial, float age, float yaw, float pitch, float scale) {
            if (!LODGED.contains(e.getEntityId()) || HItems.ALABASTRON == null) return;
            if (e.isChild()) return;                                          // never on children / baby villagers
            if (stack == null) stack = new ItemStack(HItems.ALABASTRON);
            GlStateManager.pushMatrix();
            ModelBase m = r.getMainModel();
            // 1.3.1: the seat of the trousers / robe, just behind the hip pivot where the legs meet the body (model space: +y down, +z = back)
            if (m instanceof ModelBiped) {
                if (e.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
                ((ModelBiped) m).bipedBody.postRender(scale);                 // follows the body's lean (sneaking, riding, sitting)
                GlStateManager.translate(0f, SEAT_Y * scale, SEAT_Z * scale);
            } else {
                // generic (villagers etc.): hip height of the bounding box, at its back face
                GlStateManager.translate(0f, 1.5f - e.height * 0.385f, e.width * 0.5f);
            }
            // cartoon arrow: neck into the rear, belly sticking out backward and a little up
            GlStateManager.rotate(180f, 0f, 0f, 1f);                          // model y-down -> item y-up
            GlStateManager.rotate(LODGE_PITCH, 1f, 0f, 0f);                   // flask axis (+y, lip end) now points forward and slightly down, into the body
            float k = 0.42f * (float) lodgedScale();
            GlStateManager.scale(k, k, k);
            GlStateManager.translate(0f, -LIP_INSET, 0f);                      // lip just inside the cloth, the rest outside
            Minecraft.getMinecraft().getItemRenderer().renderItem(e, stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }

        @Override public boolean shouldCombineTextures() { return false; }
    }
}
