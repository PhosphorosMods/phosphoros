package lucifertotem.v130;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import lucifertotem.ModContent;
import lucifertotem.hillman.AmmoniusClips;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.world.LibraryGen;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/** Stage 2 "Academy of Ammon U": Office Hours quests (Diploma + Mortarboard) and the Peer Reviewers. */
public final class Stage2 {
    private Stage2() {}
    public static Item DIPLOMA, MORTARBOARD, RED_PEN;
    public static final ItemArmor.ArmorMaterial GOWN = EnumHelper.addArmorMaterial("LT_MORTARBOARD", LuciferTotem.MODID + ":mortarboard", 6, new int[]{1, 1, 1, 1}, 25, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0f);
    public static final ResourceLocation LOOT_REVIEWER = LootTableList.register(new ResourceLocation(LuciferTotem.MODID, "entities/peer_reviewer"));

    public static void items(IForgeRegistry<Item> r) {
        DIPLOMA = V130.item(r, new ItemDiploma(), "diploma");
        MORTARBOARD = V130.item(r, new ItemMortarboard(), "mortarboard");
        RED_PEN = V130.item(r, new HItems.ItemTip(false), "red_pen");
    }
    public static void blocks(IForgeRegistry<Block> r) {}
    public static int entities(Object mod, int id) {
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "peer_reviewer"), EntityPeerReviewer.class, "lucifertotem.peer_reviewer", id++, mod, 64, 3, true, 0x7A5A3A, 0xC02020);
        EntityRegistry.registerModEntity(new ResourceLocation(LuciferTotem.MODID, "thrown_book"), EntityThrownBook.class, "lucifertotem.thrown_book", id++, mod, 64, 10, true);
        return id;
    }
    public static void init() {}
    public static void tiles() {}

    // ------------------------------------------------------------ quests
    public static class Quest {
        public final String id; public final Item item; public final int count; public final String clip;
        Quest(String id, Item item, int count, String clip) { this.id = id; this.item = item; this.count = count; this.clip = clip; }
    }
    public static List<Quest> QUESTS = new ArrayList<>();
    static void quests() {
        if (!QUESTS.isEmpty()) return;
        QUESTS.add(new Quest("sibyl_leaf", HItems.SIBYL_LEAF, 1, "vatican_hill_sibyl"));
        QUESTS.add(new Quest("venom", HItems.VENOM_VIAL, 3, "ios_arrow_poison"));
        QUESTS.add(new Quest("tyrian", Apothecary.TYRIAN_PURPLE, 5, "castoreum_beaver"));
        QUESTS.add(new Quest("poppy", Apothecary.POPPY_TEARS, 3, "opium_dosage_upwards"));
        QUESTS.add(new Quest("mandrake", HItems.MANDRAKE_ROOT, 1, "medea_root_for_medicine"));
        QUESTS.add(new Quest("red_pen", RED_PEN, 1, "julian_destroy_education"));
        QUESTS.add(new Quest("star_foam", HItems.STAR_FOAM, 1, "medea_unheralded_genius"));
        QUESTS.add(new Quest("theriac", ModContent.THERIAC, 1, "galen_fifty_to_sixty_drugs"));
    }
    public static Quest quest(String id) { quests(); for (Quest q : QUESTS) if (q.id.equals(id)) return q; return null; }

    public static boolean isProfessor(EntityAmmonius a) {
        if (a.getEntityData().getBoolean("lt_librarian")) return true;
        if (a.world.provider.getDimension() != LuciferTotem.dimId) return false;
        BlockPos lib = LibraryGen.nearest(a.world.getSeed(), new BlockPos(a), 400);
        return lib != null && lib.distanceSq(new BlockPos(a)) < 70 * 70;
    }

    public static int handIns = 0, graduations = 0;

    /** sneak + right-click Doctor Ammonius (in the Library) with an empty hand: Office Hours. returns a result code for the self-test */
    public static String officeHours(EntityAmmonius a, EntityPlayer p, EnumHand hand) {
        if (!LTConfig.v130.officeHours || !p.isSneaking() || hand != EnumHand.MAIN_HAND || !p.getHeldItem(hand).isEmpty()) return null;
        if (a.world.isRemote) return "client";
        if (!isProfessor(a)) return null;
        quests();
        NBTTagCompound d = HData.get(p);
        int done = d.getInteger("ouDone");
        if (d.getBoolean("ouGraduated")) { a.say("quest.lucifertotem.alumni", p.getName()); a.lecture(); return "alumni"; }
        String cur = d.getString("ouQuest");
        Quest q = cur.isEmpty() ? null : quest(cur);
        if (q == null) {
            int mask = d.getInteger("ouMask");
            List<Quest> open = new ArrayList<>();
            for (int i = 0; i < QUESTS.size(); i++) if ((mask & (1 << i)) == 0) open.add(QUESTS.get(i));
            if (open.isEmpty()) { d.setInteger("ouMask", 0); open.addAll(QUESTS); }
            q = open.get(a.getRNG().nextInt(open.size()));
            d.setString("ouQuest", q.id);
            a.say("quest.lucifertotem.assign", p.getName());
            a.say("quest.lucifertotem." + q.id, q.count);
            a.lecture();
            HAdv.grant(p, "office_hours");
            return "assigned:" + q.id;
        }
        int have = 0;
        for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == q.item) have += s.getCount();
        if (have < q.count) { a.say("quest.lucifertotem.remind", new TextComponentTranslation("quest.lucifertotem." + q.id, q.count), have); return "remind"; }
        int left = q.count;
        for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == q.item && left > 0) { int t = Math.min(left, s.getCount()); s.shrink(t); left -= t; }
        done++;
        handIns++;
        d.setInteger("ouDone", done);
        d.setString("ouQuest", "");
        quests();
        d.setInteger("ouMask", d.getInteger("ouMask") | (1 << QUESTS.indexOf(q)));
        int c = AmmoniusClips.index(q.clip);
        if (c >= 0) a.lecture(c);
        a.say("quest.lucifertotem.graded", done, LTConfig.v130.questsToGraduate);
        p.addExperience(20);
        give(p, new ItemStack(Items.EMERALD, 2));
        if (done >= LTConfig.v130.questsToGraduate) { graduate(a, p); return "graduated"; }
        return "handed:" + q.id;
    }

    public static void graduate(EntityAmmonius a, EntityPlayer p) {
        NBTTagCompound d = HData.get(p);
        d.setBoolean("ouGraduated", true);
        graduations++;
        ItemStack dip = new ItemStack(DIPLOMA);
        dip.setTagInfo("graduate", new net.minecraft.nbt.NBTTagString(p.getName()));
        give(p, dip);
        give(p, new ItemStack(MORTARBOARD));
        a.say("quest.lucifertotem.graduate", p.getName());
        int c = AmmoniusClips.index("museum_alexandria");
        if (c >= 0) a.speakClip(c);
        a.setMortarboard(true);
        HAdv.grant(p, "graduate");
        Crops.gift(p, "ambrosiaDiploma", 2);                 // the diploma bonus: seeds of the food of the gods
        a.world.playSound(null, a.posX, a.posY, a.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1f, 1f);
    }

    static void give(EntityPlayer p, ItemStack s) { if (!p.inventory.addItemStackToInventory(s)) p.dropItem(s, false); }

    // ------------------------------------------------------------ items
    public static class ItemDiploma extends HItems.ItemTip {
        public ItemDiploma() { super(true); setMaxStackSize(1); }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
            super.addInformation(s, w, tip, f);
            if (s.hasTagCompound() && s.getTagCompound().hasKey("graduate"))
                tip.add(TextFormatting.GOLD + I18n.translateToLocalFormatted("tooltip.lucifertotem.diploma_awarded", s.getTagCompound().getString("graduate")));
        }
        /** sneak + use: take a cosmetic mortarboard back off */
        @Override public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            if (!w.isRemote && p.isSneaking() && HData.get(p).getBoolean("cosmeticMortarboard")) {
                HData.get(p).setBoolean("cosmeticMortarboard", false);
                give(p, new ItemStack(MORTARBOARD));
                V130Net.sync(p);
                lucifertotem.hillman.HBlocks.msg(p, "msg.lucifertotem.mortarboard_off");
                return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(h));
            }
            return new ActionResult<>(EnumActionResult.PASS, p.getHeldItem(h));
        }
    }

    public static class ItemMortarboard extends ItemArmor {
        public ItemMortarboard() { super(GOWN, 0, EntityEquipmentSlot.HEAD); }
        @Override public String getArmorTexture(ItemStack s, net.minecraft.entity.Entity e, EntityEquipmentSlot slot, String type) { return LuciferTotem.MODID + ":textures/models/armor/mortarboard.png"; }
        @Override @SideOnly(Side.CLIENT)
        public net.minecraft.client.model.ModelBiped getArmorModel(net.minecraft.entity.EntityLivingBase e, ItemStack s, EntityEquipmentSlot slot, net.minecraft.client.model.ModelBiped def) {
            return lucifertotem.client.V130Render.mortarboardArmor(def);
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { HItems.tips(this, tip); }
        /** sneak + use: wear it as a cosmetic, on top of whatever is on your head */
        @Override public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            ItemStack s = p.getHeldItem(h);
            if (p.isSneaking()) {
                if (!w.isRemote) wearCosmetic(p, s);
                return new ActionResult<>(EnumActionResult.SUCCESS, s);
            }
            return super.onItemRightClick(w, p, h);
        }
    }

    public static boolean wearCosmetic(EntityPlayer p, ItemStack s) {
        if (HData.get(p).getBoolean("cosmeticMortarboard")) return false;
        HData.get(p).setBoolean("cosmeticMortarboard", true);
        if (!p.capabilities.isCreativeMode) s.shrink(1);
        V130Net.sync(p);
        lucifertotem.hillman.HBlocks.msg(p, "msg.lucifertotem.mortarboard_on");
        return true;
    }
}
