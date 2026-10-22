package lucifertotem.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** a biped (64x64 legacy layout) whose hair is six small snakes (texture 0,32), gently swaying */
public class ModelSnakeHair extends ModelBiped {
    final ModelRenderer[] snakes = new ModelRenderer[6];
    public ModelSnakeHair() {
        super(0f, 0f, 64, 64);
        float[][] at = {{-3.5f, -8f, -1f}, {2.5f, -8f, -1f}, {-3.5f, -8f, 3f}, {2.5f, -8f, 3f}, {-1f, -8.5f, 3.5f}, {-0.5f, -8.5f, 0.5f}};
        for (int i = 0; i < 6; i++) {
            ModelRenderer s = new ModelRenderer(this, 0, 32 + (i % 2) * 8);
            s.addBox(-0.5f, -5f, -0.5f, 1, 5, 1);
            ModelRenderer h = new ModelRenderer(this, 8, 32 + (i % 2) * 8); h.addBox(-1f, -6.5f, -2f, 2, 2, 3); s.addChild(h);
            s.setRotationPoint(at[i][0], at[i][1] + 1f, at[i][2]);
            snakes[i] = s; bipedHead.addChild(s);
        }
    }
    @Override public void setRotationAngles(float ls, float la, float age, float yaw, float pitch, float scale, Entity e) {
        super.setRotationAngles(ls, la, age, yaw, pitch, scale, e);
        for (int i = 0; i < snakes.length; i++) {
            float side = snakes[i].rotationPointX < 0 ? -1 : 1;
            snakes[i].rotateAngleZ = side * (0.5f + MathHelper.sin(age * 0.12f + i) * 0.15f);
            snakes[i].rotateAngleX = (snakes[i].rotationPointZ > 1 ? 0.6f : -0.15f) + MathHelper.cos(age * 0.1f + i * 1.7f) * 0.12f;
        }
    }
}
