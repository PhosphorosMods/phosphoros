package lucifertotem.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Bell (two tiers + rim), a glowing core, and eight long trailing tentacles that sway. Texture 64x64. */
public class ModelMedusa extends ModelBase {
    private final ModelRenderer bell, bellTop, rim, core;
    private final ModelRenderer[] tentacles = new ModelRenderer[8];

    public ModelMedusa() {
        textureWidth = 64; textureHeight = 64;
        bell = new ModelRenderer(this, 0, 0);
        bell.addBox(-6f, -6f, -6f, 12, 6, 12);
        bell.setRotationPoint(0f, 10f, 0f);
        bellTop = new ModelRenderer(this, 0, 18);
        bellTop.addBox(-4f, -9f, -4f, 8, 3, 8);
        bellTop.setRotationPoint(0f, 10f, 0f);
        rim = new ModelRenderer(this, 0, 29);
        rim.addBox(-7f, 0f, -7f, 14, 1, 14);
        rim.setRotationPoint(0f, 10f, 0f);
        core = new ModelRenderer(this, 32, 18);
        core.addBox(-2.5f, -5f, -2.5f, 5, 5, 5);
        core.setRotationPoint(0f, 10f, 0f);
        for (int i = 0; i < 8; i++) {
            ModelRenderer t = new ModelRenderer(this, 56 - (i % 2) * 4, 0);
            t.addBox(-0.5f, 0f, -0.5f, 1, 14, 1);
            double a = i * Math.PI / 4;
            t.setRotationPoint((float) Math.cos(a) * 4.5f, 11f, (float) Math.sin(a) * 4.5f);
            tentacles[i] = t;
        }
    }

    @Override
    public void render(Entity e, float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(limbSwing, limbAmount, age, yaw, pitch, scale, e);
        core.render(scale);
        bell.render(scale);
        bellTop.render(scale);
        rim.render(scale);
        for (ModelRenderer t : tentacles) t.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale, Entity e) {
        for (int i = 0; i < 8; i++) {
            float ph = age * 0.12f + i * 0.8f;
            tentacles[i].rotateAngleX = MathHelper.sin(ph) * 0.25f + (float) Math.sin(i * Math.PI / 4) * 0.12f;
            tentacles[i].rotateAngleZ = MathHelper.cos(ph * 0.9f) * 0.25f - (float) Math.cos(i * Math.PI / 4) * 0.12f;
        }
    }
}
