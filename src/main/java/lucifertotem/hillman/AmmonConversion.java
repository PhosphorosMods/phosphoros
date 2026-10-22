package lucifertotem.hillman;

import lucifertotem.LTConfig;
import lucifertotem.LuciferTotem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

/**
 * 1.2.1: a laced Alabastron, delivered from behind, makes a scholar remember himself. A grown villager of a
 * configured profession (default: librarian) becomes Doctor Ammonius, rants briefly, opens a purple rift and steps
 * through it. The rift stays open (see RiftLogic).
 */
public final class AmmonConversion {
    private AmmonConversion() {}
    public static int conversions = 0;

    public static String professionOf(EntityVillager v) {
        VillagerRegistry.VillagerProfession pr = v.getProfessionForge();
        return pr == null || pr.getRegistryName() == null ? "" : pr.getRegistryName().toString();
    }

    public static boolean professionListed(String reg) {
        for (String s : LTConfig.ammon.professions) {
            if (s == null) continue;
            String t = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (t.isEmpty()) continue;
            if (!t.contains(":")) t = "minecraft:" + t;
            if (t.equals(reg)) return true;
        }
        return false;
    }

    public static boolean eligible(EntityLivingBase t) {
        if (!LTConfig.ammon.conversionEnabled || !(t instanceof EntityVillager) || !t.isEntityAlive()) return false;
        EntityVillager v = (EntityVillager) t;
        return !v.isChild() && professionListed(professionOf(v));
    }

    /** the villager becomes an Ammon; the emptied flask drops where he stood */
    public static EntityAmmonius convert(EntityPlayer by, EntityVillager v, ItemStack emptyFlask) {
        WorldServer w = (WorldServer) v.world;
        EntityAmmonius a = new EntityAmmonius(w);
        a.setLocationAndAngles(v.posX, v.posY, v.posZ, v.rotationYaw, v.rotationPitch);
        a.renderYawOffset = v.renderYawOffset; a.rotationYawHead = v.rotationYawHead; a.prevRotationYawHead = v.rotationYawHead;
        if (v.hasCustomName()) a.setCustomNameTag(v.getCustomNameTag());
        a.makeRiftMaker(by);
        if (!emptyFlask.isEmpty()) {
            EntityItem it = new EntityItem(w, v.posX, v.posY + 0.5, v.posZ, emptyFlask);
            it.setDefaultPickupDelay();
            w.spawnEntity(it);
        }
        if (Alabastron.isLodged(v)) {   // an earlier flask still stuck in him: it falls out too
            ItemStack old = Alabastron.unlodge(v);
            w.spawnEntity(new EntityItem(w, v.posX, v.posY + 0.5, v.posZ, old));
        }
        v.setDead();
        w.spawnEntity(a);
        w.spawnParticle(EnumParticleTypes.SPELL_WITCH, a.posX, a.posY + 1, a.posZ, 60, 0.4, 0.8, 0.4, 0.1);
        w.spawnParticle(EnumParticleTypes.PORTAL, a.posX, a.posY + 1, a.posZ, 80, 0.4, 0.8, 0.4, 0.6);
        float vol = (float) LTConfig.ammoniusVolume;
        w.playSound(null, a.posX, a.posY + 1.6, a.posZ, HSounds.AWW_GAWD, SoundCategory.NEUTRAL, vol, 1.0f);
        w.playSound(null, a.posX, a.posY + 1.0, a.posZ, HSounds.ALABASTRON_DOSE, SoundCategory.PLAYERS, 1.0f, 0.9f);
        w.playSound(null, a.posX, a.posY + 1.0, a.posZ, HSounds.SQUISH_SMALL, SoundCategory.PLAYERS, 1.0f, 1.1f);
        HAdv.grant(by, "tenure_track");
        conversions++;
        LuciferTotem.LOG.info("A librarian remembered he was Doctor Ammonius at {} (dosed by {})", a.getPosition(), by.getName());
        return a;
    }
}
