package lucifertotem.client;

import lucifertotem.EntityLandMedusa;
import lucifertotem.LuciferTotem;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderLandMedusa extends RenderBiped<EntityLandMedusa> {
    private static final ResourceLocation TEX = new ResourceLocation(LuciferTotem.MODID, "textures/entity/land_medusa.png");
    public RenderLandMedusa(RenderManager m) { super(m, new ModelLandMedusa(), 0.5f); }
    @Override protected ResourceLocation getEntityTexture(EntityLandMedusa e) { return TEX; }
}
