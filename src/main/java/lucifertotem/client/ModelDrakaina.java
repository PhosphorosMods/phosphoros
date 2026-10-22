package lucifertotem.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** the Drakaina: a robed woman from the waist up, a long scaled serpent's tail coiling behind her (texture 64x64) */
public class ModelDrakaina extends ModelBase {
    final ModelRenderer head, hair, body, armR, armL, t1, t2, t3, t4;
    public ModelDrakaina() {
        textureWidth = 64; textureHeight = 64;
        head = new ModelRenderer(this, 0, 0); head.addBox(-4f, -8f, -4f, 8, 8, 8); head.setRotationPoint(0f, 0f, 0f);
        hair = new ModelRenderer(this, 32, 0); hair.addBox(-4f, -8f, -4f, 8, 8, 8, 0.5f); head.addChild(hair);
        body = new ModelRenderer(this, 16, 16); body.addBox(-4f, 0f, -2f, 8, 12, 4); body.setRotationPoint(0f, 0f, 0f);
        armR = new ModelRenderer(this, 40, 16); armR.addBox(-3f, -2f, -2f, 4, 12, 4); armR.setRotationPoint(-5f, 2f, 0f);
        armL = new ModelRenderer(this, 40, 16); armL.mirror = true; armL.addBox(-1f, -2f, -2f, 4, 12, 4); armL.setRotationPoint(5f, 2f, 0f);
        t1 = new ModelRenderer(this, 0, 32); t1.addBox(-4f, 0f, -3f, 8, 8, 6); t1.setRotationPoint(0f, 12f, 0f);
        t2 = new ModelRenderer(this, 28, 32); t2.addBox(-3f, 0f, 0f, 6, 5, 10); t2.setRotationPoint(0f, 7f, -1f); t1.addChild(t2);
        t3 = new ModelRenderer(this, 0, 48); t3.addBox(-2f, 0f, 0f, 4, 4, 10); t3.setRotationPoint(0f, 1f, 9f); t2.addChild(t3);
        t4 = new ModelRenderer(this, 28, 48); t4.addBox(-1f, 0f, 0f, 2, 3, 8); t4.setRotationPoint(0f, 1f, 9f); t3.addChild(t4);
    }
    @Override public void render(Entity e, float ls, float la, float age, float yaw, float pitch, float scale) {
        setRotationAngles(ls, la, age, yaw, pitch, scale, e);
        head.render(scale); body.render(scale); armR.render(scale); armL.render(scale); t1.render(scale);
    }
    @Override public void setRotationAngles(float ls, float la, float age, float yaw, float pitch, float scale, Entity e) {
        head.rotateAngleY = yaw * 0.017453292f; head.rotateAngleX = pitch * 0.017453292f;
        float sway = MathHelper.sin(age * 0.12f + ls * 0.5f);
        armR.rotateAngleX = -0.4f + MathHelper.cos(age * 0.09f) * 0.1f - la * 0.6f; armR.rotateAngleZ = 0.15f;
        armL.rotateAngleX = -0.4f - MathHelper.cos(age * 0.09f) * 0.1f - la * 0.6f; armL.rotateAngleZ = -0.15f;
        t2.rotateAngleY = sway * 0.35f; t3.rotateAngleY = sway * 0.55f; t4.rotateAngleY = -sway * 0.7f + 0.4f;
        t2.rotateAngleX = 0.1f; t3.rotateAngleX = 0.05f;
    }
}
