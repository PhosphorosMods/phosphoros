package lucifertotem.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/** A blob of cartoonish purple goo: drips, falls, and lands as a small fading splat on the ground. */
public class ParticleGoo extends Particle {
    private boolean landed;

    public ParticleGoo(World w, double x, double y, double z, double mx, double my, double mz, boolean puddle) {
        super(w, x, y, z, 0, 0, 0);
        motionX = mx; motionY = my; motionZ = mz;
        float v = rand.nextFloat() * 0.12f;
        setRBGColorF(0.58f + v, 0.14f + v * 0.5f, 0.86f + v * 0.5f);
        setParticleTextureIndex(113);
        particleGravity = 0.9f;
        particleMaxAge = 50 + rand.nextInt(30);
        particleScale *= 0.9f;
        setSize(0.02f, 0.02f);
        if (puddle) land();
    }

    private void land() {
        landed = true;
        motionX = motionY = motionZ = 0;
        setParticleTextureIndex(114);
        particleScale = 1.6f + rand.nextFloat() * 0.8f;
        particleMaxAge = particleAge + 40 + rand.nextInt(40);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) { setExpired(); return; }
        if (landed) {
            float left = (particleMaxAge - particleAge) / 20f;
            if (left < 1f) setAlphaF(Math.max(0f, left));
            return;
        }
        if (particleAge > 6) setParticleTextureIndex(112); // let go of the body
        motionY -= 0.04 * particleGravity;
        move(motionX, motionY, motionZ);
        motionX *= 0.9; motionY *= 0.98; motionZ *= 0.9;
        if (onGround) land();
    }
}
