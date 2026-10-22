package lucifertotem.client;

import lucifertotem.LuciferTotem;
import lucifertotem.hillman.AmmoniusPose;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.EntityGorgon;
import lucifertotem.hillman.EntityPythia;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderWitch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** renderers + models for the 1.1.0 humanoids */
public class RenderHillman {
    static ResourceLocation tex(String n) { return new ResourceLocation(LuciferTotem.MODID, "textures/entity/" + n + ".png"); }

    // ------------------------------------------------------------ Doctor Ammonius
    /** 64x64 skin with distinct left limbs (player layout: left arm at 32,48, left leg at 16,48; no overlay layers),
     *  plus a papal mitre with lappets. Texture map: mitre 0,32 / 6,42 / 28,42 / 44,42 / 32,32; lappets 0,44. (1.2.1: beard removed; stubble painted on.) */
    public static class ModelAmmonius extends ModelBiped {
        ModelRenderer mitre;
        public ModelAmmonius() {
            super(0f, 0f, 64, 64);
            bipedLeftArm = new ModelRenderer(this, 32, 48);
            bipedLeftArm.addBox(-1f, -2f, -2f, 4, 12, 4);
            bipedLeftArm.setRotationPoint(5f, 2f, 0f);
            bipedLeftLeg = new ModelRenderer(this, 16, 48);
            bipedLeftLeg.addBox(-2f, 0f, -2f, 4, 12, 4);
            bipedLeftLeg.setRotationPoint(1.9f, 12f, 0f);
            // papal mitre: purple-and-gold band on the crown of the head, two flat white panels (front and back, gold
            // cross) leaning slightly together with a cleft between their points, closed below by a low white filler.
            // 8.6 wide, 9 tall above the head (head = 8): not a cone.
            mitre = new ModelRenderer(this, 0, 32);
            mitre.setTextureOffset(0, 32).addBox(-4f, -9.5f, -4f, 8, 2, 8, 0.3f);     // band
            mitre.setTextureOffset(6, 42).addBox(-3.5f, -12f, -1.5f, 7, 3, 3);        // filler between the panels
            for (int side = -1; side <= 1; side += 2) {
                ModelRenderer panel = new ModelRenderer(this, 28, 42);
                panel.setRotationPoint(0f, -9f, 2f * side);
                panel.rotateAngleX = 0.1f * side;                                    // tops lean together
                panel.setTextureOffset(28, 42).addBox(-3.5f, -4f, -0.5f, 7, 4, 1);   // lower panel
                panel.setTextureOffset(44, 42).addBox(-3f, -6f, -0.5f, 6, 2, 1);     // slight taper
                panel.setTextureOffset(32, 32).addBox(-2f, -8f, -0.5f, 4, 2, 1);     // blunt point
                mitre.addChild(panel);
            }
            bipedHead.addChild(mitre);
            ModelRenderer lappets = new ModelRenderer(this, 0, 44);
            lappets.addBox(-2.5f, -8f, 4.01f, 2, 9, 0);
            lappets.addBox(0.5f, -8f, 4.01f, 2, 9, 0);
            bipedHead.addChild(lappets);
            // 1.2.1: no beard block any more; light grey stubble is painted on the face (reference sketch)
        }

        /** erratic body language: AmmoniusPose (synced idle mode + talk bursts) layered over the vanilla biped animation */
        @Override
        public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch, float scale, Entity e) {
            float[] p = null;
            if (e instanceof EntityAmmonius) {
                EntityAmmonius a = (EntityAmmonius) e;
                p = AmmoniusPose.compute(age, age - a.talkStart, a.talkKind(), a.talkSeed(), a.animMode(), age - a.modeStart, a.animSeed());
                isSneak = p[AmmoniusPose.SNEAK] > 0;
                if (p[AmmoniusPose.FREEZE_F] > 0) { limbSwingAmount = 0; age = 0; }
            }
            super.setRotationAngles(limbSwing, limbSwingAmount, age, netHeadYaw, headPitch, scale, e);
            if (p == null) { mitre.rotateAngleX = mitre.rotateAngleZ = 0; return; }
            if (p[AmmoniusPose.HEAD_ABS] > 0) { bipedHead.rotateAngleY = p[AmmoniusPose.HEAD_YAW]; bipedHead.rotateAngleX = p[AmmoniusPose.HEAD_PITCH]; }
            else { bipedHead.rotateAngleY += p[AmmoniusPose.HEAD_YAW]; bipedHead.rotateAngleX += p[AmmoniusPose.HEAD_PITCH]; }
            bipedHead.rotateAngleZ = p[AmmoniusPose.HEAD_ROLL];
            float w = p[AmmoniusPose.ARM_W];
            bipedRightArm.rotateAngleX = bipedRightArm.rotateAngleX * (1 - w) + p[AmmoniusPose.R_X] * w;
            bipedRightArm.rotateAngleY = bipedRightArm.rotateAngleY * (1 - w) + p[AmmoniusPose.R_Y] * w;
            bipedRightArm.rotateAngleZ = bipedRightArm.rotateAngleZ * (1 - w) + p[AmmoniusPose.R_Z] * w;
            bipedLeftArm.rotateAngleX = bipedLeftArm.rotateAngleX * (1 - w) + p[AmmoniusPose.L_X] * w;
            bipedLeftArm.rotateAngleY = bipedLeftArm.rotateAngleY * (1 - w) + p[AmmoniusPose.L_Y] * w;
            bipedLeftArm.rotateAngleZ = bipedLeftArm.rotateAngleZ * (1 - w) + p[AmmoniusPose.L_Z] * w;
            if (!isSneak) bipedBody.rotateAngleX = p[AmmoniusPose.BODY_PITCH];
            if (p[AmmoniusPose.FREEZE_F] > 0) { bipedRightLeg.rotateAngleX = 0; bipedLeftLeg.rotateAngleX = 0; }
            mitre.rotateAngleZ = p[AmmoniusPose.MITRE_Z];
            mitre.rotateAngleX = p[AmmoniusPose.MITRE_X];
            copyModelAngles(bipedHead, bipedHeadwear);
        }
    }

    public static class Ammonius extends RenderBiped<EntityAmmonius> {
        private static final ResourceLocation TEX = tex("doctor_ammonius");
        public Ammonius(RenderManager m) { super(m, new ModelAmmonius(), 0.5f); }
        @Override protected ResourceLocation getEntityTexture(EntityAmmonius e) { return TEX; }

        /** whole-body moves: sudden spins, hops/bobbing, shudders */
        @Override
        protected void applyRotations(EntityAmmonius e, float age, float yaw, float partial) {
            super.applyRotations(e, age, yaw, partial);
            float[] p = e.pose(partial);
            float s = e.isChild() ? 0.5f : 1f;
            if (p[AmmoniusPose.HOP] != 0) GlStateManager.translate(0, p[AmmoniusPose.HOP] * s, 0);
            if (p[AmmoniusPose.SPIN] != 0) GlStateManager.rotate(p[AmmoniusPose.SPIN], 0, 1, 0);
            if (p[AmmoniusPose.SHUDDER] != 0) GlStateManager.rotate(p[AmmoniusPose.SHUDDER], 0, 0, 1);
        }
    }

    // ------------------------------------------------------------ Pythia on her tripod
    public static class ModelPythia extends ModelBiped {
        final ModelRenderer tripod, laurel;
        public ModelPythia() {
            super(0f, 0f, 64, 64);
            tripod = new ModelRenderer(this, 0, 32);
            tripod.setRotationPoint(0, 24f, 0);
            tripod.setTextureOffset(0, 32).addBox(-5f, -13f, -5f, 10, 2, 10);  // bowl seat
            for (int k = 0; k < 3; k++) {
                ModelRenderer leg = new ModelRenderer(this, 40, 32);
                leg.addBox(-0.5f, 0f, -0.5f, 1, 12, 1);
                double a = k * Math.PI * 2 / 3;
                leg.setRotationPoint((float) Math.cos(a) * 4f, -12f, (float) Math.sin(a) * 4f);
                leg.rotateAngleZ = (float) Math.cos(a) * 0.18f;
                leg.rotateAngleX = -(float) Math.sin(a) * 0.18f;
                tripod.addChild(leg);
            }
            laurel = new ModelRenderer(this, 0, 44);
            laurel.addBox(-4.5f, -8.5f, -4.5f, 9, 2, 9);
            bipedHead.addChild(laurel);
        }
        @Override
        public void setLivingAnimations(EntityLivingBase e, float a, float b, float pt) { isRiding = true; super.setLivingAnimations(e, a, b, pt); }
        @Override
        public void render(Entity e, float a, float b, float c, float d, float f, float s) {
            isRiding = true;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -0.25f, 0); // seated on the bowl
            super.render(e, a, b, c, d, f, s);
            GlStateManager.popMatrix();
            tripod.render(s);
        }
        @Override
        public void setRotationAngles(float a, float b, float age, float yaw, float pitch, float s, Entity e) {
            isRiding = true;
            super.setRotationAngles(0, 0, age, yaw, pitch, s, e);
            bipedHead.rotateAngleZ = MathHelper.sin(age * 0.05f) * 0.08f; // swaying in the vapours
            bipedRightArm.rotateAngleX = -0.9f; bipedLeftArm.rotateAngleX = -0.9f;
            bipedRightArm.rotateAngleZ = -0.25f; bipedLeftArm.rotateAngleZ = 0.25f;
        }
    }

    public static class Pythia extends RenderBiped<EntityPythia> {
        private static final ResourceLocation TEX = tex("pythia");
        public Pythia(RenderManager m) { super(m, new ModelPythia(), 0.5f); }
        @Override protected ResourceLocation getEntityTexture(EntityPythia e) { return TEX; }
    }

    // ------------------------------------------------------------ Gorgon
    public static class ModelGorgon extends ModelBiped {
        final ModelRenderer[] snakes = new ModelRenderer[10];
        final ModelRenderer wingL, wingR, tusks;
        public ModelGorgon() {
            super(0f, 0f, 64, 64);
            for (int i = 0; i < snakes.length; i++) {
                snakes[i] = new ModelRenderer(this, 32 + (i % 2) * 6, 32);
                snakes[i].addBox(-0.5f, -6f, -0.5f, 1, 6, 1);
                double a = i * Math.PI * 2 / snakes.length;
                snakes[i].setRotationPoint((float) Math.cos(a) * 3.2f, -7f, (float) Math.sin(a) * 3.2f);
                bipedHead.addChild(snakes[i]);
            }
            tusks = new ModelRenderer(this, 48, 32);
            tusks.addBox(-2f, -2f, -4.6f, 1, 2, 1);
            tusks.addBox(1f, -2f, -4.6f, 1, 2, 1);
            bipedHead.addChild(tusks);
            wingR = new ModelRenderer(this, 0, 32);
            wingR.addBox(-12f, -2f, 0f, 12, 12, 1);
            wingR.setRotationPoint(-1.5f, 1.5f, 2.5f);
            bipedBody.addChild(wingR);
            wingL = new ModelRenderer(this, 0, 32);
            wingL.mirror = true;
            wingL.addBox(0f, -2f, 0f, 12, 12, 1);
            wingL.setRotationPoint(1.5f, 1.5f, 2.5f);
            bipedBody.addChild(wingL);
        }
        @Override
        public void setRotationAngles(float a, float b, float age, float yaw, float pitch, float s, Entity e) {
            super.setRotationAngles(a, b, age, yaw, pitch, s, e);
            for (int i = 0; i < snakes.length; i++) {
                double ang = i * Math.PI * 2 / snakes.length;
                float wig = MathHelper.sin(age * 0.2f + i) * 0.3f;
                snakes[i].rotateAngleX = (float) Math.sin(ang) * 0.7f + wig;
                snakes[i].rotateAngleZ = -(float) Math.cos(ang) * 0.7f + wig * 0.5f;
            }
            float f = 0.35f + MathHelper.sin(age * 0.1f) * 0.15f;
            wingR.rotateAngleY = f; wingL.rotateAngleY = -f;
        }
    }

    public static class Gorgon extends RenderBiped<EntityGorgon> {
        private static final ResourceLocation TEX = tex("gorgon");
        public Gorgon(RenderManager m) { super(m, new ModelGorgon(), 0.7f); }
        @Override protected ResourceLocation getEntityTexture(EntityGorgon e) { return TEX; }
        @Override protected void preRenderCallback(EntityGorgon e, float pt) { GlStateManager.scale(1.25f, 1.25f, 1.25f); }
    }

    // ------------------------------------------------------------ Thessalian witch
    public static class Witch extends RenderWitch {
        private static final ResourceLocation TEX = tex("thessalian_witch");
        public Witch(RenderManager m) { super(m); }
        @Override protected ResourceLocation getEntityTexture(EntityWitch e) { return TEX; }
    }
}
