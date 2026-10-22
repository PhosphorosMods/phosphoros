package lucifertotem.client;

import lucifertotem.v130.EntityBandGoat;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** the Devil's Flight goat (64x32): long body, horned head with a beard; headbangs when music plays */
public class ModelGoat extends ModelBase {
    final ModelRenderer head, body, leg1, leg2, leg3, leg4;
    public ModelGoat() {
        textureWidth = 64; textureHeight = 32;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-3f, -4f, -6f, 6, 6, 7);                       // skull + snout
        ModelRenderer hornL = new ModelRenderer(this, 26, 0); hornL.addBox(1f, -9f, -1f, 2, 6, 2); hornL.rotateAngleX = -0.5f; head.addChild(hornL);
        ModelRenderer hornR = new ModelRenderer(this, 26, 0); hornR.mirror = true; hornR.addBox(-3f, -9f, -1f, 2, 6, 2); hornR.rotateAngleX = -0.5f; head.addChild(hornR);
        ModelRenderer beard = new ModelRenderer(this, 34, 0); beard.addBox(-1f, 2f, -6f, 2, 3, 1); head.addChild(beard);
        head.setRotationPoint(0f, 7f, -8f);
        body = new ModelRenderer(this, 28, 8);
        body.addBox(-4f, -9f, -6f, 8, 15, 7);
        body.setRotationPoint(0f, 10f, 2f);
        leg1 = leg(-2.5f, 14f, 7f); leg2 = leg(2.5f, 14f, 7f); leg3 = leg(-2.5f, 14f, -5f); leg4 = leg(2.5f, 14f, -5f);
    }
    ModelRenderer leg(float x, float y, float z) {
        ModelRenderer l = new ModelRenderer(this, 0, 16); l.addBox(-1.5f, 0f, -1.5f, 3, 10, 3); l.setRotationPoint(x, y, z); return l;
    }
    @Override public void render(Entity e, float ls, float la, float age, float yaw, float pitch, float scale) {
        setRotationAngles(ls, la, age, yaw, pitch, scale, e);
        head.render(scale); body.render(scale); leg1.render(scale); leg2.render(scale); leg3.render(scale); leg4.render(scale);
    }
    @Override public void setRotationAngles(float ls, float la, float age, float yaw, float pitch, float scale, Entity e) {
        head.rotateAngleY = yaw * 0.017453292f;
        head.rotateAngleX = pitch * 0.017453292f;
        if (e instanceof EntityBandGoat && ((EntityBandGoat) e).rocking) head.rotateAngleX = 0.2f + MathHelper.sin(age * 0.95f) * 0.65f;   // headbang
        body.rotateAngleX = (float) Math.PI / 2f;
        leg1.rotateAngleX = MathHelper.cos(ls * 0.6662f) * 1.4f * la;
        leg2.rotateAngleX = MathHelper.cos(ls * 0.6662f + (float) Math.PI) * 1.4f * la;
        leg3.rotateAngleX = MathHelper.cos(ls * 0.6662f + (float) Math.PI) * 1.4f * la;
        leg4.rotateAngleX = MathHelper.cos(ls * 0.6662f) * 1.4f * la;
    }
}
