package lucifertotem.paint;

import io.netty.buffer.ByteBuf;
import lucifertotem.hillman.HItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A Lady Babylon painting: a hanging entity like a vanilla painting, but with its own textures (vanilla untouched). */
public class EntityLTPainting extends EntityHanging implements IEntityAdditionalSpawnData {
    public Paintings.Variant art = Paintings.ALL.get(0);
    public static int placed = 0, broken = 0;

    public EntityLTPainting(World w) { super(w); }

    /** variant == null: the largest that fits here, at random among equals (like vanilla) */
    public EntityLTPainting(World w, BlockPos pos, EnumFacing facing, Paintings.Variant variant, Random r) {
        super(w, pos);
        if (variant != null) { art = variant; updateFacingWithBoundingBox(facing); return; }
        List<Paintings.Variant> fit = new ArrayList<>();
        int best = 0;
        for (Paintings.Variant v : Paintings.ALL) {
            art = v; updateFacingWithBoundingBox(facing);
            if (!onValidSurface()) continue;
            fit.add(v); best = Math.max(best, v.w * v.h);
        }
        final int b = best;
        fit.removeIf(v -> v.w * v.h < b);
        art = fit.isEmpty() ? Paintings.ALL.get(0) : fit.get(r.nextInt(fit.size()));
        updateFacingWithBoundingBox(facing);
    }

    @Override public int getWidthPixels() { return art.px(); }
    @Override public int getHeightPixels() { return art.py(); }

    public ItemStack asItem() { return ItemLadyBabylonPainting.of(art); }

    @Override
    public void onBroken(Entity by) {
        broken++;
        if (!world.getGameRules().getBoolean("doEntityDrops")) return;
        playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0f, 1.0f);
        if (by instanceof EntityPlayer && ((EntityPlayer) by).capabilities.isCreativeMode) return;
        entityDropItem(asItem(), 0.0f);   // keeps its picture
    }

    @Override public void playPlaceSound() { playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0f, 1.0f); }

    @Override public ItemStack getPickedResult(net.minecraft.util.math.RayTraceResult t) { return asItem(); }

    @Override
    public void writeEntityToNBT(NBTTagCompound t) { t.setString("Motive", art.name); super.writeEntityToNBT(t); }
    @Override
    public void readEntityFromNBT(NBTTagCompound t) { art = Paintings.orDefault(t.getString("Motive")); super.readEntityFromNBT(t); }

    // the FML spawn packet carries no wall position/facing/variant: send them ourselves
    @Override
    public void writeSpawnData(ByteBuf b) {
        ByteBufUtils.writeUTF8String(b, art.name);
        b.writeLong(hangingPosition.toLong());
        b.writeByte(facingDirection == null ? 2 : facingDirection.getHorizontalIndex());
    }
    @Override
    public void readSpawnData(ByteBuf b) {
        art = Paintings.orDefault(ByteBufUtils.readUTF8String(b));
        hangingPosition = BlockPos.fromLong(b.readLong());
        updateFacingWithBoundingBox(EnumFacing.byHorizontalIndex(b.readByte()));
    }

    // like EntityPainting: tracker moves must not unhook it from the wall
    @Override public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) { setPosition(x, y, z); }
    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int inc, boolean teleport) {
        BlockPos p = hangingPosition.add(x - posX, y - posY, z - posZ);
        setPosition(p.getX(), p.getY(), p.getZ());
    }
}
