package lucifertotem.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;

/** Global (overworld map storage) registry of Lightfall gates and their links. Saved as data/lucifertotem_gates.dat. */
public class PortalData extends WorldSavedData {
    public static final String NAME = "lucifertotem_gates";

    public static class Gate {
        public int dim; public BlockPos base; public EnumFacing.Axis axis;
        public boolean linked; public int ldim; public BlockPos lbase; public EnumFacing.Axis laxis;
        /** 1.2.1: a purple rift (1x2 at base) rather than a 4x5 duskstone gate; maker = UUID of the player who made it */
        public boolean rift; public String maker = "";
        public Gate(int dim, BlockPos base, EnumFacing.Axis axis) { this.dim = dim; this.base = base; this.axis = axis; }
        public void linkTo(Gate o) { linked = true; ldim = o.dim; lbase = o.base; laxis = o.axis; }
        public String toString() { return (rift ? "rift" : "gate") + "[dim " + dim + " " + base + " " + axis + (linked ? " -> dim " + ldim + " " + lbase : " unlinked") + "]"; }
    }

    public final List<Gate> gates = new ArrayList<>();

    public PortalData() { super(NAME); }
    public PortalData(String n) { super(n); }

    public static PortalData get() {
        World ow = DimensionManager.getWorld(0);
        MapStorage s = ow.getMapStorage();
        PortalData d = (PortalData) s.getOrLoadData(PortalData.class, NAME);
        if (d == null) { d = new PortalData(); s.setData(NAME, d); }
        return d;
    }

    public Gate find(int dim, BlockPos p) {
        for (Gate g : gates) {
            if (g.dim != dim) continue;
            if (g.rift) { if (p.getX() == g.base.getX() && p.getZ() == g.base.getZ() && p.getY() - g.base.getY() >= 0 && p.getY() - g.base.getY() <= 1) return g; continue; }
            int dx = p.getX() - g.base.getX(), dy = p.getY() - g.base.getY(), dz = p.getZ() - g.base.getZ();
            int u = g.axis == EnumFacing.Axis.X ? dx : dz, w = g.axis == EnumFacing.Axis.X ? dz : dx;
            if (w == 0 && u >= 0 && u <= 3 && dy >= 0 && dy <= 4) return g;
        }
        return null;
    }

    public Gate findExact(int dim, BlockPos base) {
        for (Gate g : gates) if (g.dim == dim && g.base.equals(base)) return g;
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        gates.clear();
        NBTTagList l = nbt.getTagList("gates", 10);
        for (int i = 0; i < l.tagCount(); i++) {
            NBTTagCompound c = l.getCompoundTagAt(i);
            Gate g = new Gate(c.getInteger("dim"), BlockPos.fromLong(c.getLong("base")), c.getBoolean("z") ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
            if (c.getBoolean("linked")) {
                g.linked = true; g.ldim = c.getInteger("ldim"); g.lbase = BlockPos.fromLong(c.getLong("lbase"));
                g.laxis = c.getBoolean("lz") ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
            }
            g.rift = c.getBoolean("rift"); g.maker = c.getString("maker");
            gates.add(g);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList l = new NBTTagList();
        for (Gate g : gates) {
            NBTTagCompound c = new NBTTagCompound();
            c.setInteger("dim", g.dim); c.setLong("base", g.base.toLong()); c.setBoolean("z", g.axis == EnumFacing.Axis.Z);
            c.setBoolean("linked", g.linked);
            if (g.rift) { c.setBoolean("rift", true); c.setString("maker", g.maker == null ? "" : g.maker); }
            if (g.linked) { c.setInteger("ldim", g.ldim); c.setLong("lbase", g.lbase.toLong()); c.setBoolean("lz", g.laxis == EnumFacing.Axis.Z); }
            l.appendTag(c);
        }
        nbt.setTag("gates", l);
        return nbt;
    }
}
