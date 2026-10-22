package lucifertotem.lore;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/** Doctor Ammonius' Lecture Notes (a vanilla written book, so any client can read it) and the loose page he leaves behind. */
public final class LoreBooks {
    private LoreBooks() {}
    public static final String TAG = "lucifertotem_notes";

    static final String[] NOTES = {
        "LECTURE NOTES\nof Doctor Ammonius,\nlate of the Library of Alexandria\n\n(Property of the Library. Not to be used as a coaster. I am looking at YOU, Ptolemy.)",
        "Class, settle down. Settle DOWN.\n\nToday: how I got out. The Library was burning. Smoke everywhere, scrolls everywhere, somehow a goat. I grabbed the important texts. And the theriac. Mostly the theriac.",
        "We ran for the harbour and took a galley. A fine Alexandrian galley! Purple sails, naturally. Then the storm. Then the rocks. Then this.\n\nIf you are reading this aboard the wreck: hello! Mind the hole in the floor.",
        "Now. PURPLE. Write it down. The dye of kings, the colour of the burning sky, the colour of the stone that opened for me.\n\nOut of the smoke there was a door, and beyond it the Lightfall, where it is always dusk. ALWAYS.",
        "PRACTICAL EXERCISE: THE DOOR\n\n1. Find a Fallen Altar: a broken frame of purple duskstone, four wide and five tall. They lie scattered across this sad, dry world. The totem will tug you toward one.",
        "2. Mend it. Every cracked or missing stone must become a whole Duskstone Brick, fourteen in all. I left bricks in the hold. Or press two obsidian with two purple dyes. Cracked bricks bake whole again in a furnace.",
        "3. Wait for DUSK. Not noon! The altar sleeps at noon, like my students.\n\n4. Strike the frame with the Totem of Lucifer. Use it ON the stone. Do not THROW it. I cannot stress this enough.",
        "5. Step through. Purple. Everywhere. You're going to love it.\n\nTo come home, step back through the door you arrived by. Lost it? Build the same frame from duskstone in the Lightfall and strike it. It remembers where you came from.",
        "On THERIAC: forty-odd ingredients, a little snake venom among them, the cure for every poison known to scholarship. I keep a cup by the bed.\n\nDo not drink from strange cups. Unless they are mine. Then: still don't.",
        "Barbarians, I am told, open the door by hurling the totem at a villager. We do not do that in this class. (It works. Do not do it.)\n\nFind me in the Library. Bring snacks.\n        - A.",
        "FOOTNOTE NINE (late addition, in purple ink):\n\nA LIBRARIAN, dosed from behind with a laced Alabastron, may remember that he was always a philologist. He will rant. He will leave by a door of purple air, and the door STAYS. Close it with the totem, kneeling."
    };

    static final String[] PAGE = {
        "A loose page, singed at the edges:\n\n\"...mend the cracked stones with whole duskstone bricks, wait for DUSK, then strike the frame with the Totem. STRIKE it, do not throw it!\"",
        "\"...the rest of my notes went down with the galley, somewhere along the coast. Purple sails. You can't miss it. Well. You can. Everyone does.\n\nPURPLE!\"\n        - A."
    };

    public static ItemStack notes() { return book("Lecture Notes", NOTES, "notes"); }
    public static ItemStack page() { return book("A Loose Page", PAGE, "page"); }

    static ItemStack book(String title, String[] pages, String kind) {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound t = new NBTTagCompound();
        t.setString("title", title);
        t.setString("author", "Dr. Ammonius");
        t.setInteger("generation", 0);
        t.setBoolean("resolved", true);
        NBTTagList l = new NBTTagList();
        for (String p : pages) {
            ITextComponent c = new TextComponentString(p);
            l.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(c)));
        }
        t.setTag("pages", l);
        t.setString(TAG, kind);
        s.setTagCompound(t);
        return s;
    }

    public static boolean isNotes(ItemStack s) {
        return !s.isEmpty() && s.getItem() == Items.WRITTEN_BOOK && s.hasTagCompound() && s.getTagCompound().hasKey(TAG);
    }
}
