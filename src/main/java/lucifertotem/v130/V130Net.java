package lucifertotem.v130;

import io.netty.buffer.ByteBuf;
import lucifertotem.hillman.HData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 1.3.0 per-player cosmetic flags (mortarboard worn as a cosmetic, the ΜΥΣΤΗΣ tattoo), synced to every tracking client. */
public final class V130Net {
    private V130Net() {}
    public static final int MORTARBOARD = 1, MYSTES = 2, CROWN = 4;
    public static SimpleNetworkWrapper NET;
    /** client side: entity id -> flags */
    public static final Map<Integer, Integer> CLIENT = new ConcurrentHashMap<>();
    public static int sent = 0;

    public static void network() {
        if (NET != null) return;
        NET = NetworkRegistry.INSTANCE.newSimpleChannel("lt_v130");
        NET.registerMessage(Handler.class, MsgFlags.class, 0, Side.CLIENT);
    }

    public static int flags(EntityPlayer p) {
        int f = 0;
        if (HData.get(p).getBoolean("cosmeticMortarboard")) f |= MORTARBOARD;
        if (HData.get(p).getBoolean("mystes")) f |= MYSTES;
        if (HData.get(p).getBoolean(lucifertotem.v132.Crown.COSMETIC)) f |= CROWN;
        return f;
    }

    public static class MsgFlags implements IMessage {
        public int id, flags;
        public MsgFlags() { }
        public MsgFlags(int id, int flags) { this.id = id; this.flags = flags; }
        @Override public void fromBytes(ByteBuf b) { id = b.readInt(); flags = b.readInt(); }
        @Override public void toBytes(ByteBuf b) { b.writeInt(id); b.writeInt(flags); }
    }
    public static class Handler implements IMessageHandler<MsgFlags, IMessage> {
        @Override public IMessage onMessage(MsgFlags m, MessageContext ctx) { if (m.flags == 0) CLIENT.remove(m.id); else CLIENT.put(m.id, m.flags); return null; }
    }

    static boolean realClient(EntityPlayerMP p) { return !(p instanceof FakePlayer) && p.connection != null && p.connection.getNetworkManager() != null && p.connection.getNetworkManager().channel() != null; }

    /** tell everyone tracking p (and p) about p's flags */
    public static void sync(EntityPlayer p) {
        if (NET == null || p.world.isRemote) return;
        MsgFlags m = new MsgFlags(p.getEntityId(), flags(p));
        try { NET.sendToAllTracking(m, p); sent++; } catch (Throwable t) { }
        if (p instanceof EntityPlayerMP && realClient((EntityPlayerMP) p)) try { NET.sendTo(m, (EntityPlayerMP) p); sent++; } catch (Throwable t) { }
    }

    public static class Events {
        @SubscribeEvent public void track(PlayerEvent.StartTracking e) {
            if (!(e.getTarget() instanceof EntityPlayer) || !(e.getEntityPlayer() instanceof EntityPlayerMP) || NET == null) return;
            int f = flags((EntityPlayer) e.getTarget());
            if (f != 0 && realClient((EntityPlayerMP) e.getEntityPlayer())) try { NET.sendTo(new MsgFlags(e.getTarget().getEntityId(), f), (EntityPlayerMP) e.getEntityPlayer()); sent++; } catch (Throwable t) { }
        }
        @SubscribeEvent public void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) { sync(e.player); }
        @SubscribeEvent public void dim(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e) { sync(e.player); }
        @SubscribeEvent public void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent e) { sync(e.player); }
    }
}
