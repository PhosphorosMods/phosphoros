package lucifertotem.world;

/** test access to PortalLogic's package-private site finder */
public final class PortalLogicAccess {
    private PortalLogicAccess() {}
    public static net.minecraft.util.math.BlockPos findSite(net.minecraft.world.WorldServer w, int x, int z, net.minecraft.util.EnumFacing.Axis a) { return PortalLogic.findSite(w, x, z, a); }
}
