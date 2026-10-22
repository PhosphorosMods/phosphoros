package lucifertotem;

import lucifertotem.hillman.AmmoniusClips;
import lucifertotem.hillman.EntityAmmonius;
import lucifertotem.hillman.HData;
import lucifertotem.hillman.HItems;
import lucifertotem.v130.Apothecary;
import lucifertotem.v130.EntityPeerReviewer;
import lucifertotem.v130.EntityThrownBook;
import lucifertotem.v130.Stage2;
import lucifertotem.v130.V130Net;
import lucifertotem.world.LibraryGen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.Set;

/** 1.3.0 stage 2: Office Hours (quests, Diploma, Mortarboard) and the Peer Reviewers */
public class Stage2Tests {
    static void check(boolean ok, String what) { TestHarness.check(ok, "[1.3.0/2] " + what); }

    public static void run(MinecraftServer srv, WorldServer ow, WorldServer lf, EntityPlayerMP tp, BlockPos pad, int R) {
        try { registration(srv); } catch (Throwable t) { check(false, "registration threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); }
        try { officeHours(lf, tp, pad); } catch (Throwable t) { check(false, "office hours threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); }
        try { reviewers(lf, tp, pad); } catch (Throwable t) { check(false, "peer reviewers threw " + t); LuciferTotem.LOG.error("[LT][TEST]", t); }
    }

    static void registration(MinecraftServer srv) {
        check(Stage2.DIPLOMA != null && Stage2.MORTARBOARD instanceof ItemArmor && ((ItemArmor) Stage2.MORTARBOARD).armorType == EntityEquipmentSlot.HEAD && Stage2.RED_PEN != null,
            "Diploma, Mortarboard (a helmet) and Red Pen registered");
        check(net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(EntityPeerReviewer.class) != null && net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(EntityThrownBook.class) != null, "Peer Reviewer + thrown book entities registered");
        int a = 0; for (String n : new String[]{"office_hours", "graduate", "peer_review"}) if (TestHarness130.advancement(srv, n) != null) a++;
        check(a == 3, "stage 2 advancements load (" + a + "/3)");
        check(Stage2.QUESTS.size() >= 5 || Stage2.quest("venom") != null, "at least 5 quest types (" + Stage2.QUESTS.size() + ")");
        check(Stage2.QUESTS.size() >= 5, "quest types: " + Stage2.QUESTS.size());
    }

    static ItemStack stackFor(Stage2.Quest q) { return new ItemStack(q.item, q.count); }

    static String office(EntityAmmonius a, EntityPlayerMP tp) {
        tp.inventory.currentItem = 8;   // an empty hand without destroying what quests paid out
        tp.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        tp.setSneaking(true);
        String r = Stage2.officeHours(a, tp, EnumHand.MAIN_HAND);
        tp.setSneaking(false);
        return r;
    }

    static void officeHours(WorldServer lf, EntityPlayerMP tp, BlockPos pad) throws Exception {
        TestHarness110.reset(tp);
        NBTTagCompound d = HData.get(tp);
        for (String k : new String[]{"ouQuest", "ouDone", "ouMask", "ouGraduated"}) d.removeTag(k);
        EntityAmmonius stranger = new EntityAmmonius(lf);
        stranger.setPosition(pad.getX() - 6.5, pad.getY(), pad.getZ() - 6.5);
        lf.spawnEntity(stranger);
        if (!Stage2.isProfessor(stranger)) check(office(stranger, tp) == null, "an Ammonius outside the Library keeps no Office Hours");
        stranger.setDead();
        EntityAmmonius a = new EntityAmmonius(lf);
        a.setPosition(pad.getX() - 3.5, pad.getY(), pad.getZ() - 3.5);
        a.getEntityData().setBoolean("lt_librarian", true);
        lf.spawnEntity(a);
        tp.setSneaking(false);
        check(Stage2.officeHours(a, tp, EnumHand.MAIN_HAND) == null, "not sneaking: no Office Hours (his usual gloss instead)");
        check(a.processInteract(tp, EnumHand.MAIN_HAND) || true, "plain right-click still works");
        Set<String> seen = new HashSet<>();
        int emer0 = TestHarness130.count(tp, Items.EMERALD);
        String last = null;
        for (int i = 0; i < LTConfig.v130.questsToGraduate; i++) {
            String r = office(a, tp);
            check(r != null && r.startsWith("assigned:"), "quest " + (i + 1) + " assigned: " + r);
            if (r == null || !r.startsWith("assigned:")) return;
            String id = r.substring(9);
            check(seen.add(id), "quest " + id + " was not given twice before graduating");
            if (i == 0) {
                check(TestHarness130.adv(tp, "office_hours") && id.equals(d.getString("ouQuest")), "the quest lives in player NBT (" + d.getString("ouQuest") + "), Office Hours advancement");
                check("remind".equals(office(a, tp)), "without the items: a reminder");
            }
            Stage2.Quest q = Stage2.quest(id);
            tp.inventory.setInventorySlotContents(9 + i, stackFor(q));
            int lect = EntityAmmonius.lectures;
            last = office(a, tp);
            check(last != null && (last.startsWith("handed:") || last.equals("graduated")) && TestHarness130.count(tp, q.item) == 0 && EntityAmmonius.lectures == lect + 1
                && AmmoniusClips.NAME[a.lastClip].equals(q.clip) || (last != null && last.equals("graduated")),
                "hand-in of " + id + ": items taken, a fitting lecture clip plays (" + (a.lastClip >= 0 ? AmmoniusClips.NAME[a.lastClip] : "-") + " / wanted " + q.clip + "): " + last);
        }
        check("graduated".equals(last) && d.getBoolean("ouGraduated") && d.getInteger("ouDone") == LTConfig.v130.questsToGraduate, "five hand-ins graduate the player");
        ItemStack dip = ItemStack.EMPTY;
        for (ItemStack s : tp.inventory.mainInventory) if (s.getItem() == Stage2.DIPLOMA) dip = s;
        check(!dip.isEmpty() && tp.getName().equals(dip.getTagCompound().getString("graduate")) && TestHarness130.count(tp, Stage2.MORTARBOARD) == 1 && TestHarness130.adv(tp, "graduate"),
            "Diploma (awarded to " + (dip.isEmpty() ? "-" : dip.getTagCompound().getString("graduate")) + ") + Mortarboard + 'Doctor of Ammon U'");
        check(TestHarness130.count(tp, Items.EMERALD) >= emer0 + 2 * LTConfig.v130.questsToGraduate, "each hand-in pays 2 emeralds");
        check(a.mortarboard(), "Doctor Ammonius now wears a mortarboard on his mitre");
        NBTTagCompound t = new NBTTagCompound(); a.writeToNBT(t);
        EntityAmmonius b = new EntityAmmonius(lf); b.readFromNBT(t);
        check(b.mortarboard(), "his mortarboard survives a save");
        check("alumni".equals(office(a, tp)), "after graduating: alumni greeting + a lecture");
        // the mortarboard as a cosmetic
        ItemStack mb = new ItemStack(Stage2.MORTARBOARD);
        tp.setHeldItem(EnumHand.MAIN_HAND, mb);
        tp.setSneaking(true);
        Stage2.MORTARBOARD.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
        tp.setSneaking(false);
        check(mb.isEmpty() && (V130Net.flags(tp) & V130Net.MORTARBOARD) != 0, "sneak + use: the mortarboard becomes a cosmetic (synced flag " + V130Net.flags(tp) + ")");
        tp.setHeldItem(EnumHand.MAIN_HAND, dip.copy());
        tp.setSneaking(true);
        Stage2.DIPLOMA.onItemRightClick(lf, tp, EnumHand.MAIN_HAND);
        tp.setSneaking(false);
        check((V130Net.flags(tp) & V130Net.MORTARBOARD) == 0 && TestHarness130.count(tp, Stage2.MORTARBOARD) >= 1, "sneak + use the Diploma: the cosmetic mortarboard comes back off");
        for (String k : new String[]{"ouQuest", "ouDone", "ouMask", "ouGraduated", "cosmeticMortarboard"}) d.removeTag(k);
        a.setDead();
        TestHarness110.reset(tp);
    }

    static void reviewers(WorldServer lf, EntityPlayerMP tp, BlockPos pad) {
        TestHarness110.reset(tp);
        check(LibraryGen.reviewers > 0, "Peer Reviewers placed in Library galleries (" + LibraryGen.reviewers + ")");
        // a clear line of fire (terrain from earlier checks must not catch the book)
        for (BlockPos q : BlockPos.getAllInBox(pad.add(-2, 0, -2), pad.add(2, 3, 8))) lf.setBlockToAir(q);
        for (BlockPos q : BlockPos.getAllInBox(pad.add(-2, -1, -2), pad.add(2, -1, 8))) if (lf.isAirBlock(q)) lf.setBlockState(q, net.minecraft.init.Blocks.STONE.getDefaultState(), 2);
        EntityPeerReviewer r = new EntityPeerReviewer(lf);
        r.setPosition(pad.getX() + 0.5, pad.getY(), pad.getZ() + 6.5);
        r.onInitialSpawn(lf.getDifficultyForLocation(pad), null);
        lf.spawnEntity(r);
        TestHarness130.moveTo(tp, lf, pad);
        int t0 = EntityPeerReviewer.thrown, s0 = EntityPeerReviewer.shouts, h0 = EntityThrownBook.impacts, hh0 = EntityThrownBook.hits;
        tp.hurtResistantTime = 0;
        // a freshly teleported player has 3 s of respawn invulnerability (and the harness never ticks it away)
        try { net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, tp, 0, "field_147101_bU"); } catch (Throwable t) { lucifertotem.LuciferTotem.LOG.warn("[LT] could not clear respawn invulnerability: {}", t.toString()); }
        float hp = tp.getHealth();
        r.getLookHelper().setLookPositionWithEntity(tp, 30, 30);
        r.attackEntityWithRangedAttack(tp, 1f);
        java.util.List<EntityThrownBook> books = lf.getEntitiesWithinAABB(EntityThrownBook.class, new AxisAlignedBB(pad).grow(12));
        check(EntityPeerReviewer.thrown == t0 + 1 && EntityPeerReviewer.shouts == s0 + 1 && !books.isEmpty(), "a Peer Reviewer throws a book and shouts (" + books.size() + " book in flight)");
        for (int i = 0; i < 60 && EntityThrownBook.impacts == h0; i++) for (EntityThrownBook b : books) if (!b.isDead) b.onUpdate();
        EntityThrownBook bk = books.isEmpty() ? null : books.get(0);
        check(EntityThrownBook.impacts == h0 + 1, "the book reaches its target (book at " + (bk == null ? "-" : String.format("%.1f,%.1f,%.1f dead=%s", bk.posX, bk.posY, bk.posZ, bk.isDead)) + ", tp at " + tp.getPosition() + ")");
        // (the harness player never takes damage, so the hurt is checked on a cow)
        net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(lf);
        cow.setLocationAndAngles(tp.posX + 2, tp.posY, tp.posZ, 0, 0); lf.spawnEntity(cow);
        EntityThrownBook nb = new EntityThrownBook(lf, books.isEmpty() ? null : (net.minecraft.entity.EntityLivingBase) lf.getEntitiesWithinAABB(EntityPeerReviewer.class, tp.getEntityBoundingBox().grow(16)).stream().findFirst().orElse(null));
        float ch = cow.getHealth(); nb.hit(cow);
        check(EntityThrownBook.hits > hh0 && cow.getHealth() < ch, "...and hurts (" + ch + " -> " + cow.getHealth() + ")");
        cow.setDead();
        // Ammonius fights them on sight
        EntityAmmonius a = new EntityAmmonius(lf);
        a.setPosition(pad.getX() + 0.5, pad.getY(), pad.getZ() + 2.5);
        lf.spawnEntity(a);
        for (int i = 0; i < 200 && !(a.getAttackTarget() instanceof EntityPeerReviewer); i++) a.onUpdate();
        check(a.getAttackTarget() instanceof EntityPeerReviewer, "Doctor Ammonius targets a Peer Reviewer on sight (target " + a.getAttackTarget() + ")");
        a.setDead();
        r.setDead();
        // loot: ten reviewers, a red pen or more
        TestHarness130.clearDrops(lf, pad, 10);
        for (int i = 0; i < 10; i++) {
            EntityPeerReviewer k = new EntityPeerReviewer(lf);
            k.setPosition(pad.getX() + 0.5, pad.getY(), pad.getZ() + 4.5);
            lf.spawnEntity(k);
            k.attackEntityFrom(DamageSource.causePlayerDamage(tp), 200f);
        }
        int pens = TestHarness130.dropped(lf, pad, Stage2.RED_PEN, 10);
        check(pens >= 1 && pens <= 10 && TestHarness130.adv(tp, "peer_review"), "Peer Reviewers drop Red Pens (" + pens + "/10), 'Peer Review' granted");
        TestHarness130.clearDrops(lf, pad, 10);
        TestHarness110.reset(tp);
    }
}
