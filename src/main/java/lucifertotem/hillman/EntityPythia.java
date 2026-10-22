package lucifertotem.hillman;

import lucifertotem.ModContent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * The Pythia: the oracle seated on her tripod over the Delphic vapour vent. She will not move and cannot be harmed.
 * Breathe the Pneuma (stand on the vent) and bring her a laurel wreath (or Theriac): she answers with a true but
 * cryptic pointer to a place you name by what you hold in your OTHER hand (or the nearest wonder if nothing).
 */
public class EntityPythia extends EntityLiving {
    public static int prophecies = 0;
    public EntityPythia(World w) { super(w); setSize(0.6f, 1.6f); enablePersistence(); }

    @Override protected void initEntityAI() { tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8f, 1f)); }
    @Override protected void applyEntityAttributes() { super.applyEntityAttributes(); getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20); getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0); getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0); }
    @Override public boolean isEntityInvulnerable(DamageSource s) { return s != DamageSource.OUT_OF_WORLD && !s.isCreativePlayer(); }
    @Override public boolean canBePushed() { return false; }
    @Override protected boolean canDespawn() { return false; }
    @Override public void addVelocity(double x, double y, double z) { }

    public void say(EntityPlayer p, ITextComponent line) {
        ITextComponent t = new TextComponentTranslation("chat.lucifertotem.pythia_says", line);
        t.getStyle().setColor(TextFormatting.GOLD).setItalic(true);
        p.sendMessage(t);
    }

    @Override
    public boolean processInteract(EntityPlayer p, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (world.isRemote) return true;
        ItemStack held = p.getHeldItem(hand);
        boolean gift = held.getItem() == HItems.PYTHIAN_LAUREL || held.getItem() == ModContent.THERIAC || held.getItem() == HItems.GREAT_THERIAC;
        if (!p.isPotionActive(HPotions.PNEUMA)) { say(p, new TextComponentTranslation("pythia.lucifertotem.silent")); return true; }
        if (!gift) { say(p, new TextComponentTranslation("pythia.lucifertotem.gift")); return true; }
        long now = world.getTotalWorldTime();
        if (now - p.getEntityData().getLong("lt_pythia") < 200 && p.getEntityData().hasKey("lt_pythia")) { say(p, new TextComponentTranslation("pythia.lucifertotem.tired")); return true; }
        p.getEntityData().setLong("lt_pythia", now);
        if (!p.capabilities.isCreativeMode) held.shrink(1);
        prophecies++;
        world.playSound(null, posX, posY, posZ, HSounds.PYTHIA_SPEAK, SoundCategory.NEUTRAL, 1.2f, 1f);
        for (ITextComponent line : Prophecies.oracle(world, p, new BlockPos(this))) say(p, line);
        HAdv.grant(p, "oracle");
        return true;
    }
}
