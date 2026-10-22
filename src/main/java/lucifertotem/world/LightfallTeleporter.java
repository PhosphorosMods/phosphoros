package lucifertotem.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

/** Places the traveller at a precomputed, known-safe spot in front of the destination gate. */
public class LightfallTeleporter implements ITeleporter {
    private final double x, y, z; private final float yaw;
    public LightfallTeleporter(double x, double y, double z, float yaw) { this.x = x; this.y = y; this.z = z; this.yaw = yaw; }

    @Override
    public void placeEntity(World world, Entity e, float entityYaw) {
        e.motionX = e.motionY = e.motionZ = 0;
        e.fallDistance = 0;
        e.setLocationAndAngles(x, y, z, yaw, e.rotationPitch);
    }

    @Override public boolean isVanilla() { return false; }
}
