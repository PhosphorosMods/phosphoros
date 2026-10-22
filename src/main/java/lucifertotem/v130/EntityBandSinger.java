package lucifertotem.v130;

import lucifertotem.hillman.HAdv;
import lucifertotem.hillman.HData;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * The singer of Devil's Flight: a robed woman whose hair is a nest of (friendly) snakes. Not hostile, does not petrify.
 * Talk to her for a verse; the first time, she hands you a copy of the band's disc.
 */
public class EntityBandSinger extends EntityCreature {
    public static int verses = 0, discsGiven = 0;
    int verse = 0;
    public EntityBandSinger(World w) { super(w); setSize(0.6f, 1.95f); }
    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIMoveTowardsRestriction(this, 0.6));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.5));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 10f));
        tasks.addTask(7, new EntityAILookIdle(this));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.24);
    }
    /** a verse in chat (and her disc, once per player). returns the verse index */
    public int sing(EntityPlayer p) {
        int v = verse++ % Stage4.LYRICS.length;
        verses++;
        TextComponentTranslation m = new TextComponentTranslation("chat.lucifertotem.singer", new TextComponentTranslation(Stage4.LYRICS[v]));
        m.getStyle().setColor(TextFormatting.DARK_PURPLE).setItalic(true);
        p.sendMessage(m);
        world.playSound(null, posX, posY + 1.6, posZ, SoundEvents.BLOCK_NOTE_FLUTE, SoundCategory.NEUTRAL, 0.8f, 0.6f + (v % 4) * 0.15f);
        swingArm(EnumHand.MAIN_HAND);
        if (!HData.get(p).getBoolean("dfDisc")) {
            HData.get(p).setBoolean("dfDisc", true);
            ItemStack d = new ItemStack(Stage4.RECORD_DEVILS_FLIGHT);
            if (!p.inventory.addItemStackToInventory(d)) p.dropItem(d, false);
            discsGiven++;
            p.sendMessage(new TextComponentTranslation("msg.lucifertotem.singer.disc"));
        }
        HAdv.grant(p, "band_camp");
        return v;
    }
    @Override protected boolean processInteract(EntityPlayer p, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!world.isRemote) sing(p);
        return true;
    }
    @Override public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote && rand.nextInt(30) == 0) world.spawnParticle(EnumParticleTypes.NOTE, posX, posY + 2.3, posZ, rand.nextInt(24) / 24.0, 0, 0);
    }
    @Override protected boolean canDespawn() { return false; }
    @Override protected net.minecraft.util.ResourceLocation getLootTable() { return null; }
}
