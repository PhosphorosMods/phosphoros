package lucifertotem.client;

import lucifertotem.hillman.EntityBookWraith;
import lucifertotem.hillman.EntityEnchantress;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** renderers for the Library of Alexandria's inhabitants */
public class RenderLibrary {
    public static class Enchantress extends RenderBiped<EntityEnchantress> {
        private static final ResourceLocation TEX = RenderHillman.tex("enchantress");
        public Enchantress(RenderManager m) { super(m, new ModelBiped(0f, 0f, 64, 64), 0.5f); }
        @Override protected ResourceLocation getEntityTexture(EntityEnchantress e) { return TEX; }
    }

    /** the enchanting-table book, flapping its covers like wings */
    public static class ModelFlyingBook extends ModelBook {
        @Override
        public void render(Entity e, float swing, float amt, float age, float yaw, float pitch, float scale) {
            float flip = age * 0.08f;
            float spread = 0.55f + 0.45f * MathHelper.sin(age * 0.55f);
            super.render(e, age, flip - (float) Math.floor(flip), flip + 0.3f - (float) Math.floor(flip + 0.3f), spread, 0f, scale);
        }
    }

    public static class BookWraith extends RenderLiving<EntityBookWraith> {
        private static final ResourceLocation TEX = new ResourceLocation("textures/entity/enchanting_table_book.png");
        public BookWraith(RenderManager m) { super(m, new ModelFlyingBook(), 0.2f); }
        @Override protected ResourceLocation getEntityTexture(EntityBookWraith e) { return TEX; }
        @Override
        protected void preRenderCallback(EntityBookWraith e, float partial) {
            GlStateManager.translate(0f, 1.05f + 0.06f * MathHelper.sin((e.ticksExisted + partial) * 0.3f), 0f);
            GlStateManager.rotate(90f, 1f, 0f, 0f);   // spine along the flight direction, covers out like wings
            GlStateManager.scale(0.9f, 0.9f, 0.9f);
        }
    }
}
