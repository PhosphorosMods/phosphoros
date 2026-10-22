package lucifertotem.paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 1.2.1: the Lady Babylon paintings (own textures at textures/painting/<name>.png, 16 px per block). */
public final class Paintings {
    private Paintings() {}
    public static final class Variant {
        public final String name; public final int w, h;   // blocks
        Variant(String name, int w, int h) { this.name = name; this.w = w; this.h = h; }
        public int px() { return w * 16; }
        public int py() { return h * 16; }
        public String titleKey() { return "painting.lucifertotem." + name + ".title"; }
        public String authorKey() { return "painting.lucifertotem." + name + ".author"; }
        @Override public String toString() { return name + " " + w + "x" + h; }
    }
    public static final List<Variant> ALL, CLASSIC;
    static {
        List<Variant> l = new ArrayList<>();
        l.add(new Variant("aletheia", 2, 2));          // green-framed portrait inside a hooded snake, 'αλήθεια'
        l.add(new Variant("on_air", 2, 2));            // studio collage: ON AIR sign, hooded figure with a sword
        l.add(new Variant("gilded_oval", 2, 2));       // ornate gilded oval frame, a dreamy blue scene
        l.add(new Variant("moon_goddess", 1, 2));      // robed goddess under a starry moon, arms raised
        l.add(new Variant("devils_flight", 4, 2));     // band poster: Ammonius at the mic, black horned goat, robed Medusa singer
        l.add(new Variant("purple_theriac", 1, 1));    // still life: a cup of purple theriac, snake, figs
        l.add(new Variant("library_burning", 2, 1));   // the Library of Alexandria burning at dusk
        l.add(new Variant("galen", 1, 1));             // Galen, physician to emperors, with his theriac jar
        // 1.3.0: hung in the new structures (the Library keeps drawing from the first eight)
        l.add(new Variant("bacchanal", 2, 1));         // robed maenads dancing under the vines around a wine krater
        l.add(new Variant("hekate", 1, 2));            // three-faced Hekate with two torches at a night crossroads
        l.add(new Variant("mysteries", 2, 2));         // torch procession of robed initiates to Eleusis, the kykeon cup
        l.add(new Variant("asklepios", 1, 2));         // the healer with his serpent staff, a patient asleep on a kline
        l.add(new Variant("labyrinth", 2, 2));         // the maze from above, a red thread, the Lady at its heart
        l.add(new Variant("hydra", 2, 1));             // the many-headed water serpent in a flooded chapel
        ALL = Collections.unmodifiableList(l);
        CLASSIC = ALL.subList(0, 8);
    }
    public static Variant byName(String n) {
        for (Variant v : ALL) if (v.name.equals(n)) return v;
        return null;
    }
    public static Variant orDefault(String n) { Variant v = byName(n); return v != null ? v : ALL.get(0); }
}
