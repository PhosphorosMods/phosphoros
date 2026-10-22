package lucifertotem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 1.3.1: the Lightfall leak fix and the 3D Alabastron (render-free checks) */
public class Stage131Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.1] " + what); }
    static void guard(String n, Runnable r) { try { r.run(); } catch (Throwable t) { check(false, n + " threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); } }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        guard("alabastron model", Stage131Tests::alabastronModel);
        guard("lodged scale config", Stage131Tests::lodgedScale);
        guard("leak fix", () -> leakFix(srv, ow, lf, tp));
    }

    static JsonObject json(String path) throws Exception {
        try (InputStream in = Stage131Tests.class.getResourceAsStream("/assets/lucifertotem/" + path)) {
            if (in == null) return null;
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    static boolean png(String tex) throws Exception {
        String p = tex.substring(tex.indexOf(':') + 1);
        try (InputStream in = Stage131Tests.class.getResourceAsStream("/assets/lucifertotem/textures/" + p + ".png")) {
            if (in == null) return false;
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            return img != null && img.getWidth() == 16 && img.getHeight() == 16;
        }
    }

    static void alabastronModel() {
        try {
            JsonObject m = json("models/item/alabastron.json"), l = json("models/item/alabastron_laced.json");
            check(m != null && l != null, "alabastron.json + alabastron_laced.json load from the jar");
            JsonArray el = m.getAsJsonArray("elements");
            check(el != null && el.size() >= 8, "Alabastron is a real 3D model (" + (el == null ? 0 : el.size()) + " elements)");
            boolean inBounds = true, lip = false, lugs = false; int lugN = 0;
            float top = 0, maxW = 0, neckW = 99;
            for (JsonElement je : el) {
                JsonObject e = je.getAsJsonObject();
                JsonArray f = e.getAsJsonArray("from"), t = e.getAsJsonArray("to");
                for (int i = 0; i < 3; i++) { float a = f.get(i).getAsFloat(), b = t.get(i).getAsFloat(); if (a < -16 || b > 32 || a > b) inBounds = false; }
                String n = e.has("name") ? e.get("name").getAsString() : "";
                float w = t.get(0).getAsFloat() - f.get(0).getAsFloat();
                if (n.contains("lip")) lip = true;
                if (n.contains("lug")) lugN++;
                if (n.contains("neck")) neckW = Math.min(neckW, w);
                if (n.startsWith("body") || n.startsWith("belly") || n.startsWith("shoulder")) maxW = Math.max(maxW, w);
                top = Math.max(top, t.get(1).getAsFloat());
            }
            lugs = lugN == 2;
            check(inBounds, "every element sits inside the vanilla -16..32 model bounds");
            check(lip && lugs && neckW < maxW, "flask anatomy: flat disc lip, two lug handles, neck (" + neckW + ") narrower than the body (" + maxW + ")");
            JsonObject d = m.getAsJsonObject("display");
            boolean all = d != null;
            for (String k : new String[]{"thirdperson_righthand", "firstperson_righthand", "ground", "fixed", "gui", "head"}) all &= d != null && d.has(k);
            check(all, "display transforms for 1st/3rd person, ground, item frame, gui, head");
            JsonArray ov = m.getAsJsonArray("overrides");
            boolean laced = false;
            if (ov != null) for (JsonElement o : ov) { JsonObject oo = o.getAsJsonObject(); if (oo.getAsJsonObject("predicate").has("lucifertotem:laced") && oo.get("model").getAsString().equals("lucifertotem:item/alabastron_laced")) laced = true; }
            check(laced, "laced flasks switch to the purple-tinted model");
            check(png(m.getAsJsonObject("textures").get("body").getAsString()) && png(l.getAsJsonObject("textures").get("body").getAsString()), "stone + laced textures load as 16x16 PNGs");
            check(!m.getAsJsonObject("textures").get("body").getAsString().equals(l.getAsJsonObject("textures").get("body").getAsString()), "laced model uses its own (purple) texture");
        } catch (Exception x) { check(false, "alabastron model json: " + x); }
    }

    static void lodgedScale() {
        check(LTConfig.alabastronLodgedScale == 1.5, "alabastronLodgedScale defaults to 1.5 (" + LTConfig.alabastronLodgedScale + ")");
        try {
            net.minecraftforge.common.config.Config.RangeDouble r = LTConfig.class.getField("alabastronLodgedScale").getAnnotation(net.minecraftforge.common.config.Config.RangeDouble.class);
            check(r != null && r.min() == 0.5 && r.max() == 2.5, "alabastronLodgedScale range 0.5-2.5");
        } catch (NoSuchFieldException x) { check(false, "alabastronLodgedScale missing"); }
    }

    static void leakFix(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp) {
        check(lucifertotem.block.WateryEvents.pendingWorlds() <= 1, "old-save conversion queue is keyed per dimension, not per World object (" + lucifertotem.block.WateryEvents.pendingWorlds() + ")");
        check(LTConfig.leakGuard, "leakGuard on by default");
        net.minecraft.entity.monster.EntityZombie z = new net.minecraft.entity.monster.EntityZombie(lf);
        z.setPosition(tp.posX, tp.posY, tp.posZ);
        WorldServer here = (WorldServer) tp.world;
        z.setWorld(here);
        tp.setLastAttackedEntity(z); tp.setRevengeTarget(z);
        lucifertotem.v131.LeakGuard.forget(tp, null);
        check(tp.getLastAttackedEntity() == z && tp.getRevengeTarget() == z, "a fight in the world you are standing in is remembered");
        WorldServer other = here == ow ? lf : ow;
        net.minecraft.entity.monster.EntityZombie z2 = new net.minecraft.entity.monster.EntityZombie(other);
        tp.setLastAttackedEntity(z2); tp.setRevengeTarget(z2);
        lucifertotem.v131.LeakGuard.forget(tp, null);
        check(tp.getLastAttackedEntity() == null && tp.getRevengeTarget() == null, "references into a world you left are forgotten (the 1.3.0 Lightfall leak)");
        tp.setLastAttackedEntity(z);
        lucifertotem.v131.LeakGuard.forget(tp, here);
        check(tp.getLastAttackedEntity() == null, "an unloading world is forgotten even by players standing in it");
    }
}
