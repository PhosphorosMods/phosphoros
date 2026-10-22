package lucifertotem.client;

import lucifertotem.CommonProxy;
import lucifertotem.EntityPurpleBolt;
import lucifertotem.EntityThrownTotem;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = LuciferTotem.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityThrownTotem.class, m -> new RenderSnowball<EntityThrownTotem>(m, ModContent.TOTEM, Minecraft.getMinecraft().getRenderItem()));
        RenderingRegistry.registerEntityRenderingHandler(EntityPurpleBolt.class, RenderPurpleBolt::new);
        RenderingRegistry.registerEntityRenderingHandler(lucifertotem.EntityMedusa.class, RenderMedusa::new);
        RenderingRegistry.registerEntityRenderingHandler(lucifertotem.EntityLandMedusa.class, RenderLandMedusa::new);
        HClient.renderers();
        V130Client.renderers();
    }

    @Override
    public void init() { AlabastronClient.registerOverrides(); AlabastronClient.addLayers(); V130Client.init(); }

    @Override
    public void alabastronState(int entityId, boolean lodged) { AlabastronClient.state(entityId, lodged); }

    @Override
    public void openSibylBook(net.minecraft.entity.player.EntityPlayer p, net.minecraft.item.ItemStack s) { HClient.openBook(p, s); }

    @Override
    public void boltFlash(World w) {
        if (w.isRemote) w.setLastLightningBolt(2);
    }

    @Override
    public void gooDrip(lucifertotem.EntityLandMedusa e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.particleSetting >= 2 || mc.effectRenderer == null) return;
        java.util.Random r = e.getRNG();
        double dx = e.posX - e.prevPosX, dz = e.posZ - e.prevPosZ;
        boolean moving = dx * dx + dz * dz > 0.0004;
        if (moving ? r.nextInt(2) != 0 : r.nextInt(25) != 0) return;
        float yaw = e.renderYawOffset * 0.017453292f;
        double bx = e.posX + net.minecraft.util.math.MathHelper.sin(yaw) * 0.28 + (r.nextDouble() - 0.5) * 0.2;
        double bz = e.posZ - net.minecraft.util.math.MathHelper.cos(yaw) * 0.28 + (r.nextDouble() - 0.5) * 0.2;
        mc.effectRenderer.addEffect(new ParticleGoo(e.world, bx, e.posY + 0.8 + r.nextDouble() * 0.15, bz, dx * 0.2, 0, dz * 0.2, false));
        if (moving && e.onGround && r.nextInt(20) == 0) mc.effectRenderer.addEffect(new ParticleGoo(e.world, bx, e.posY + 0.02, bz, 0, 0, 0, true));
    }

    /** PURPLE: a soft violet tint over the world */
    @SubscribeEvent
    public static void fog(net.minecraftforge.client.event.EntityViewRenderEvent.FogColors e) {
        net.minecraft.entity.Entity v = e.getEntity();
        if (ModContent.PURPLE != null && v instanceof net.minecraft.entity.EntityLivingBase && ((net.minecraft.entity.EntityLivingBase) v).isPotionActive(ModContent.PURPLE)) {
            e.setRed(e.getRed() * 0.55f + 0.30f);
            e.setGreen(e.getGreen() * 0.45f + 0.08f);
            e.setBlue(e.getBlue() * 0.55f + 0.40f);
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent e) {
        for (Block b : ModContent.ITEM_BLOCKS) item(Item.getItemFromBlock(b));
        item(ModContent.TOTEM);
        item(ModContent.SHARD);
        item(ModContent.PRIAPUS);
        item(ModContent.THERIAC);
        ModelLoader.setCustomStateMapper(ModContent.PORTAL, new StateMap.Builder().build());
        // watery sea decor: LEVEL is only there for vanilla's water code, the model ignores it
        for (Block b : ModContent.ITEM_BLOCKS) if (b instanceof lucifertotem.block.BlockWatery) ModelLoader.setCustomStateMapper(b, new StateMap.Builder().ignore(lucifertotem.block.BlockWatery.LEVEL).build());
    }

    private static void item(Item i) {
        ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
    }
}
