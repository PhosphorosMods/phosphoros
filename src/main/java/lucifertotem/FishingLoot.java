package lucifertotem;

import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.1.1: the Totem of Lucifer is a fairly common catch (every dimension uses gameplay/fishing). We ADD an entry to the
 * vanilla "main" pool (junk 10 / treasure 5 / fish 85 = 100), so the totem replaces ~p% of catches instead of being an
 * extra item; quality 1 lets Luck of the Sea (and Luck) raise it a little. If another mod removed the main pool we fall
 * back to an extra pool of our own with a p% chance. Nothing is replaced, so HarvestCraft etc. keep their fishing loot.
 */
public class FishingLoot {
    public static final String ENTRY = LuciferTotem.MODID + ":totem_of_lucifer";
    public static String injected = "none";

    /** main-pool weight that makes the totem ~p% of a 100-weight pool */
    public static int weight(double p) { return p <= 0 ? 0 : Math.max(1, (int) Math.round(100.0 * p / (100.0 - p))); }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void load(LootTableLoadEvent e) {
        if (!LootTableList.GAMEPLAY_FISHING.equals(e.getName()) || LTConfig.totemFishingChancePercent <= 0 || e.getTable() == null) return;
        double p = LTConfig.totemFishingChancePercent;
        LootPool main = e.getTable().getPool("main");
        if (main != null && !main.isFrozen() && main.getEntry(ENTRY) == null) {
            main.addEntry(new LootEntryItem(ModContent.TOTEM, weight(p), 1, new LootFunction[0], new LootCondition[0], ENTRY));
            injected = "main pool, weight " + weight(p);
        } else if (main == null && e.getTable().getPool(ENTRY) == null) {
            e.getTable().addPool(new LootPool(new LootEntry[]{new LootEntryItem(ModContent.TOTEM, 1, 0, new LootFunction[0], new LootCondition[0], ENTRY)},
                new LootCondition[]{new RandomChance((float) (p / 100.0))}, new RandomValueRange(1), new RandomValueRange(0), ENTRY));
            injected = "own pool, chance " + p + "%";
        }
        LuciferTotem.LOG.info("[LT] Totem of Lucifer added to fishing loot ({})", injected);
    }
}
