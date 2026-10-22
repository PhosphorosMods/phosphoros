package lucifertotem.v130;

import lucifertotem.LuciferTotem;
import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import net.minecraft.advancements.Advancement;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;

/**
 * "Interview with Lucifer": right-click him for a menu of questions in chat (click one to ask). Riddling answers.
 * More questions unlock as you earn the advancements they are about.
 */
public final class Interview {
    private Interview() {}
    public static final class Q {
        public final String id, adv;    // adv = advancement (lucifertotem:...) that unlocks it, null = always
        Q(String id, String adv) { this.id = id; this.adv = adv; }
    }
    public static final Q[] QUESTIONS = {
        new Q("who", null), new Q("place", null), new Q("totem", null),
        new Q("purple", "something_in_the_drink"), new Q("ammonius", "meet_ammonius"), new Q("graduate", "graduate"),
        new Q("hekate", "key_of_hekate"), new Q("mysteries", "mystes"), new Q("band", "band_camp"),
        new Q("labyrinth", "labyrinth"), new Q("hydra", "hydra"), new Q("hail", "sign_off")};
    public static int asked = 0;

    public static Q q(String id) { for (Q x : QUESTIONS) if (x.id.equals(id)) return x; return null; }
    public static boolean unlocked(EntityPlayer p, Q q) { return q.adv == null || HAdv.has(p, q.adv); }

    public static void menu(EntityPlayer p, EntityLucifer l) {
        TextComponentTranslation head = new TextComponentTranslation("interview.lucifertotem.greet");
        head.getStyle().setColor(TextFormatting.GOLD);
        p.sendMessage(head);
        for (Q q : QUESTIONS) {
            ITextComponent line;
            if (unlocked(p, q)) {
                line = new TextComponentString("  \u2726 ").appendSibling(new TextComponentTranslation("interview.lucifertotem.q." + q.id));
                line.getStyle().setColor(HData.get(p).getBoolean("asked_" + q.id) ? TextFormatting.GRAY : TextFormatting.YELLOW)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ltinterview " + q.id))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("interview.lucifertotem.click")));
            } else {
                line = new TextComponentString("  ? ").appendSibling(new TextComponentTranslation("interview.lucifertotem.locked", new TextComponentTranslation("advancements.lucifertotem." + q.adv + ".title")));
                line.getStyle().setColor(TextFormatting.DARK_GRAY);
            }
            p.sendMessage(line);
        }
    }
    /** ask a question: "answered" / "locked" / "far" / "unknown" */
    public static String ask(EntityPlayer p, String id) {
        Q q = q(id);
        if (q == null) return "unknown";
        List<EntityLucifer> near = p.world.getEntitiesWithinAABB(EntityLucifer.class, p.getEntityBoundingBox().grow(8));
        if (near.isEmpty()) { p.sendMessage(new TextComponentTranslation("interview.lucifertotem.far")); return "far"; }
        if (!unlocked(p, q)) { p.sendMessage(new TextComponentTranslation("interview.lucifertotem.not_yet")); return "locked"; }
        TextComponentTranslation me = new TextComponentTranslation("interview.lucifertotem.you", new TextComponentTranslation("interview.lucifertotem.q." + id));
        me.getStyle().setColor(TextFormatting.GRAY);
        p.sendMessage(me);
        for (int i = 1; i <= 2; i++) {
            TextComponentTranslation a = new TextComponentTranslation("interview.lucifertotem.says", new TextComponentTranslation("interview.lucifertotem.a." + id + "." + i));
            a.getStyle().setColor(TextFormatting.GOLD);
            p.sendMessage(a);
        }
        HData.get(p).setBoolean("asked_" + id, true);
        asked++;
        HAdv.grant(p, "interview");
        int n = 0; for (Q x : QUESTIONS) if (HData.get(p).getBoolean("asked_" + x.id)) n++;
        if (n >= 5) Crops.gift(p, "ambrosiaLucifer", 2);       // five good questions: a gift from the Morning Star
        near.get(0).swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        return "answered";
    }

    public static class Cmd extends CommandBase {
        @Override public String getName() { return "ltinterview"; }
        @Override public String getUsage(ICommandSender s) { return "/ltinterview <question>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean checkPermission(MinecraftServer srv, ICommandSender s) { return s instanceof EntityPlayerMP; }
        @Override public void execute(MinecraftServer srv, ICommandSender s, String[] args) {
            if (!(s instanceof EntityPlayerMP) || args.length < 1) return;
            ask((EntityPlayerMP) s, args[0]);
        }
    }
}
