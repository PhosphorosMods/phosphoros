package lucifertotem;

public class CommonProxy {
    public void preInit() { }
    public void init() { }
    /** client-only sky flash; no-op on server */
    public void boltFlash(net.minecraft.world.World w) { }
    /** client-only goo particles behind a Land Medusa; no-op on server */
    public void gooDrip(EntityLandMedusa e) { }
    /** client-only: open the Sibylline Leaves in the book screen */
    /** client-only: an alabastron got lodged in / pulled out of an entity */
    public void alabastronState(int entityId, boolean lodged) { }
    public void openSibylBook(net.minecraft.entity.player.EntityPlayer p, net.minecraft.item.ItemStack s) { }
}
