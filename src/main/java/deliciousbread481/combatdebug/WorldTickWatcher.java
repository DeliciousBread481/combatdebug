package deliciousbread481.combatdebug;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "combatdebug")
public class WorldTickWatcher {
    private static final Map<Integer, Long> LAST_TICK = new ConcurrentHashMap<>();

    private static final long STALL_THRESHOLD_MS = 2000L;

    private static int serverTickCount = 0;

    private static final Map<Integer, Boolean> WARNED = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END && event.world != null) {
            int dim = event.world.provider.getDimension();
            LAST_TICK.put(dim, System.currentTimeMillis());
            if (WARNED.remove(dim) != null) {
                CombatDebug.logger.warn("[WorldTickWatcher] 维度 {} 恢复 tick", dim);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++serverTickCount % 40 != 0) {
            return;
        }

        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (World world : server.worlds) {
            if (world == null) {
                continue;
            }
            int dim = world.provider.getDimension();
            Long last = LAST_TICK.get(dim);
            if (last != null && (now - last) > STALL_THRESHOLD_MS) {
                if (WARNED.putIfAbsent(dim, Boolean.TRUE) == null) {
                    CombatDebug.logger.warn("[WorldTickWatcher] 维度 {} 已停止 tick {} ms！当前玩家分布: {}",
                            dim, (now - last), playerDistribution(server));
                }
            }
        }
    }

    // 统计每个在线玩家所在维度：dim -> count / 玩家名
    private static String playerDistribution(MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p.getName()).append("@dim").append(p.dimension);
        }
        return sb.length() == 0 ? "(无玩家)" : sb.toString();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            int dim = event.getWorld().provider.getDimension();
            LAST_TICK.put(dim, System.currentTimeMillis());
            CombatDebug.logger.warn("[WorldTickWatcher] 维度 {} 已加载(Load)", dim);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            int dim = event.getWorld().provider.getDimension();
            LAST_TICK.remove(dim);
            WARNED.remove(dim);
            CombatDebug.logger.warn("[WorldTickWatcher] 维度 {} 已卸载(Unload)", dim);
        }
    }
}