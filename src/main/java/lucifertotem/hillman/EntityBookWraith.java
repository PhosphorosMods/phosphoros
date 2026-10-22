package lucifertotem.hillman;

import net.minecraft.entity.monster.EntityVex;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** an animated, flapping book called by an Enchantress (a short-lived Vex at heart: flies through shelves, bites with its pages) */
public class EntityBookWraith extends EntityVex {
    public EntityBookWraith(World w) { super(w); setSize(0.5f, 0.5f); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ENDERDRAGON_FLAP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ITEMFRAME_BREAK; }
    @Override protected float getSoundVolume() { return 0.35f; }
    @Override protected float getSoundPitch() { return 1.6f; }
    @Override protected net.minecraft.util.ResourceLocation getLootTable() { return null; }
}
