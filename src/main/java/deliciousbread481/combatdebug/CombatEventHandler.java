package deliciousbread481.combatdebug;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = "combatdebug")
public class CombatEventHandler {

    private static int forgeBusID = -1;
    private static boolean busIDResolved = false;

    private static String dim(Entity e) {
        if (e == null || e.world == null) {
            return "?";
        }
        return String.valueOf(e.world.provider.getDimension());
    }

    private static String thread() {
        return Thread.currentThread().getName();
    }

    private static String side(Entity e) {
        if (e == null || e.world == null) {
            return "?";
        }
        return e.world.isRemote ? "CLIENT" : "SERVER";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity target = event.getTarget();
        CombatDebug.logger.warn(
                "[AttackEntityEvent] side={} attacker={} attackerDim={} target={} targetDim={} thread={} canceled={}",
                side(player), player, dim(player), target, dim(target), thread(), event.isCanceled());
        if (event.isCanceled()) {
            reportCancellers(event);
        }
    }

    private static void reportCancellers(Event event) {
        try {
            int busID = getForgeBusID();
            if (busID < 0) {
                return;
            }
            IEventListener[] listeners = event.getListenerList().getListeners(busID);
            CombatDebug.logger.warn("[AttackEntityEvent] 事件被取消，候选取消者监听器：");
            EventPriority current = null;
            for (IEventListener listener : listeners) {
                if (listener instanceof EventPriority) {
                    current = (EventPriority) listener;
                    continue;
                }
                if (current == EventPriority.NORMAL) {
                    continue;
                }
                if (listener instanceof ASMEventHandler) {
                    String readable = listener.toString();
                    if (readable.contains("deliciousbread481")) {
                        continue;
                    }
                    CombatDebug.logger.warn("    priority={} listener={} mod={}",
                            current, readable, ownerOf((ASMEventHandler) listener));
                }
            }
        } catch (Throwable t) {
            CombatDebug.logger.warn("reportCancellers 失败：{}", t.toString());
        }
    }

    private static String ownerOf(ASMEventHandler handler) {
        try {
            Field ownerField = ASMEventHandler.class.getDeclaredField("owner");
            ownerField.setAccessible(true);
            Object owner = ownerField.get(handler);
            if (owner instanceof ModContainer) {
                ModContainer mc = (ModContainer) owner;
                return mc.getName() + " (" + mc.getModId() + ")";
            }
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static int getForgeBusID() {
        if (busIDResolved) {
            return forgeBusID;
        }
        try {
            Field busIDField = EventBus.class.getDeclaredField("busID");
            busIDField.setAccessible(true);
            forgeBusID = busIDField.getInt(MinecraftForge.EVENT_BUS);
        } catch (Throwable t) {
            CombatDebug.logger.warn("解析 EventBus.busID 失败：{}", t.toString());
            forgeBusID = -1;
        }
        busIDResolved = true;
        return forgeBusID;
    }
}