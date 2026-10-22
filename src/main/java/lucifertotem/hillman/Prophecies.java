package lucifertotem.hillman;

import lucifertotem.LuciferTotem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** True-but-cryptic directions in the manner of an oracle: the eight Greek winds and distances in stadia. */
public class Prophecies {
    public static final String[] WINDS = {"boreas", "kaikias", "apeliotes", "euros", "notos", "lips", "zephyros", "skiron"}; // N NE E SE S SW W NW
    public static final String[] OVERWORLD = {"Village", "Temple", "Stronghold", "Mansion", "Monument"};

    /** compass sector 0..7 (0 = north = -Z) of the direction from -> to */
    public static int sector(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        double ang = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = north, 90 = east
        if (ang < 0) ang += 360;
        return (int) Math.floor((ang + 22.5) / 45.0) % 8;
    }

    public static ITextComponent wind(BlockPos from, BlockPos to) { return new TextComponentTranslation("wind.lucifertotem." + WINDS[sector(from, to)]); }

    public static int blocks(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    /** "<place> lies N stadia (~B blocks) towards <wind>" */
    public static ITextComponent line(String type, BlockPos from, BlockPos to) {
        int b = blocks(from, to);
        String st = String.format("%.1f", b / 185.0); // a stadion is about 185 m
        return new TextComponentTranslation("prophecy.lucifertotem.line", new TextComponentTranslation("place.lucifertotem." + type),
            st, b, wind(from, to), to.getY());
    }

    public static BlockPos find(World w, BlockPos from, String type, int forceBudget) {
        if (!(w instanceof WorldServer)) return null;
        WorldServer ws = (WorldServer) w;
        if (ws.provider.getDimension() == LuciferTotem.dimId) return StructureIndex.locate(ws, type, from, 32, forceBudget);
        try { return ws.findNearestStructure(type, from, false); } catch (Throwable t) { return null; }
    }

    /** short pointer for Doctor Ammonius ("Notos, 3.2 stadia") */
    public static ITextComponent pointTo(World w, BlockPos from, String type, int forceBudget) {
        BlockPos to = find(w, from, type, forceBudget);
        if (to == null) return null;
        return line(type, from, to);
    }

    /** the Pythia's answer: a riddle opening, up to three true pointers, and a closing */
    public static List<ITextComponent> oracle(World w, EntityPlayer p, BlockPos from) {
        List<ITextComponent> out = new ArrayList<>();
        Random r = new Random(w.getTotalWorldTime() ^ p.getUniqueID().getLeastSignificantBits());
        out.add(new TextComponentTranslation("pythia.lucifertotem.open." + (1 + r.nextInt(4))));
        boolean lightfall = w.provider.getDimension() == LuciferTotem.dimId;
        String[] types = lightfall ? new String[]{"library", "morningstar_citadel", "trophonius_pit", "witch_circle", "fallen_spire", "sunken_shrine"} : OVERWORLD;
        int said = 0, budget = 6;
        for (String t : types) {
            if (said >= 3) break;
            BlockPos to = find(w, from, t, budget);
            budget = Math.max(0, budget - StructureIndex.lastForced);
            if (to != null) { out.add(line(lightfall ? t : "ow_" + t.toLowerCase(), from, to)); said++; }
        }
        // a coven at work?
        for (Long c : EntityThessalianWitch.ACTIVE.keySet()) {
            BlockPos cp = BlockPos.fromLong(c);
            if (lightfall && blocks(from, cp) < 600) { out.add(new TextComponentTranslation("pythia.lucifertotem.witches", wind(from, cp), EntityThessalianWitch.ACTIVE.get(c) / 10)); break; }
        }
        if (said == 0) out.add(new TextComponentTranslation("pythia.lucifertotem.nothing"));
        out.add(new TextComponentTranslation(r.nextInt(8) == 0 ? "pythia.lucifertotem.croesus" : "pythia.lucifertotem.close." + (1 + r.nextInt(3))));
        return out;
    }

    /** Sibylline Leaves: pages as JSON text components (translated on the reading client) */
    public static List<String> sibylPages(World w, EntityPlayer p) {
        List<String> pages = new ArrayList<>();
        Random r = new Random(w.getTotalWorldTime() * 31 + p.getName().hashCode());
        BlockPos from = new BlockPos(p);
        pages.add(json(new TextComponentTranslation("sibyl.lucifertotem.title")));
        boolean lightfall = w.provider.getDimension() == LuciferTotem.dimId;
        String[] types = lightfall ? new String[]{"delphic_vent", "light_shrine", "witch_circle", "trophonius_pit"} : OVERWORLD;
        int budget = 4;
        for (String t : types) {
            BlockPos to = find(w, from, t, budget);
            budget = Math.max(0, budget - StructureIndex.lastForced);
            if (to != null) pages.add(json(new TextComponentTranslation("sibyl.lucifertotem.page", line(lightfall ? t : "ow_" + t.toLowerCase(), from, to))));
        }
        int verses = 2 + r.nextInt(2);
        for (int i = 0; i < verses; i++) pages.add(json(new TextComponentTranslation("sibyl.lucifertotem.verse." + (1 + r.nextInt(8)))));
        pages.add(json(new TextComponentTranslation("sibyl.lucifertotem.end")));
        return pages;
    }

    static String json(ITextComponent c) { return ITextComponent.Serializer.componentToJson(c); }
}
