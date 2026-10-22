package lucifertotem.client;

import lucifertotem.v130.EntityHydra;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** the Hydra: a squat scaled body, four short legs, a tail, and one long neck per living head (texture 128x64) */
public class ModelHydra extends ModelBase {
    final ModelRenderer body, tail, l1, l2, l3, l4;
    final ModelRenderer[] necks = new ModelRenderer[EntityHydra.MAX_HEADS], heads = new ModelRenderer[EntityHydra.MAX_HEADS];
    public ModelHydra() {
        textureWidth = 128; textureHeight = 64;
        body = new ModelRenderer(this, 0, 0); body.addBox(-8f, -5f, -10f, 16, 10, 20); body.setRotationPoint(0f, 13f, 0f);
        tail = new ModelRenderer(this, 0, 30); tail.addBox(-2.5f, -2f, 0f, 5, 4, 16); tail.setRotationPoint(0f, 14f, 9f);
        l1 = leg(-6f, -7f); l2 = leg(6f, -7f); l3 = leg(-6f, 7f); l4 = leg(6f, 7f);
        for (int i = 0; i < necks.length; i++) {
            necks[i] = new ModelRenderer(this, 72, 0); necks[i].addBox(-1.5f, -1.5f, -14f, 3, 3, 14);
            heads[i] = new ModelRenderer(this, 72, 20); heads[i].addBox(-2.5f, -2f, -7f, 5, 4, 7); heads[i].setRotationPoint(0f, 0f, -13f);
            ModelRenderer jaw = new ModelRenderer(this, 72, 32); jaw.addBox(-2f, 0f, -6f, 4, 1, 6); jaw.setRotationPoint(0f, 2f, 0f); heads[i].addChild(jaw);
            necks[i].addChild(heads[i]);
        }
    }
    ModelRenderer leg(float x, float z) { ModelRenderer l = new ModelRenderer(this, 44, 30); l.addBox(-2.5f, 0f, -2.5f, 5, 6, 5); l.setRotationPoint(x, 18f, z); return l; }
    int n = 3;
    @Override public void render(Entity e, float ls, float la, float age, float yaw, float pitch, float scale) {
        n = e instanceof EntityHydra ? ((EntityHydra) e).heads() : 3;
        setRotationAngles(ls, la, age, yaw, pitch, scale, e);
        body.render(scale); tail.render(scale); l1.render(scale); l2.render(scale); l3.render(scale); l4.render(scale);
        for (int i = 0; i < n; i++) necks[i].render(scale);
    }
    @Override public void setRotationAngles(float ls, float la, float age, float yaw, float pitch, float scale, Entity e) {
        for (int i = 0; i < n; i++) {
            float k = n == 1 ? 0f : (i - (n - 1) / 2f) / ((n - 1) / 2f);   // -1..1 across the fan
            ModelRenderer nk = necks[i];
            nk.setRotationPoint(k * 6f, 10f, -8f);
            nk.rotateAngleY = -k * 0.55f + yaw * 0.006f + MathHelper.sin(age * 0.1f + i * 1.7f) * 0.12f;
            nk.rotateAngleX = -0.75f - (1f - Math.abs(k)) * 0.25f + MathHelper.cos(age * 0.08f + i) * 0.1f;
            heads[i].rotateAngleX = 0.9f + pitch * 0.008f;
        }
        tail.rotateAngleY = MathHelper.sin(age * 0.07f) * 0.3f; tail.rotateAngleX = -0.15f;
        l1.rotateAngleX = MathHelper.cos(ls * 0.6662f) * 1.0f * la; l4.rotateAngleX = l1.rotateAngleX;
        l2.rotateAngleX = -l1.rotateAngleX; l3.rotateAngleX = -l1.rotateAngleX;
    }
}
