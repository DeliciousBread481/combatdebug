package deliciousbread481.combatdebug;

import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity attacker = event.getEntityPlayer();
        Entity target = event.getTarget();
        CombatDebug.logger.warn("[AttackEntityEvent] attacker={} attackerDim={} target={} targetDim={} thread={} canceled={}",
                attacker, dim(attacker), target, dim(target), thread(), event.isCanceled());
        if (event.isCanceled()) {
            reportCancellers(event, "AttackEntityEvent");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity victim = event.getEntityLiving();
        CombatDebug.logger.warn("[LivingAttackEvent] victim={} victimDim={} source={} amount={} thread={} canceled={}",
                victim, dim(victim), event.getSource().getDamageType(), event.getAmount(), thread(), event.isCanceled());
        if (event.isCanceled()) {
            reportCancellers(event, "LivingAttackEvent");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity victim = event.getEntityLiving();
        CombatDebug.logger.warn("[LivingHurtEvent] victim={} victimDim={} source={} amount={} thread={} canceled={}",
                victim, dim(victim), event.getSource().getDamageType(), event.getAmount(), thread(), event.isCanceled());
        if (event.isCanceled()) {
            reportCancellers(event, "LivingHurtEvent");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDamage(LivingDamageEvent event) {
        Entity victim = event.getEntityLiving();
        CombatDebug.logger.warn("[LivingDamageEvent] victim={} victimDim={} source={} amount={} thread={} canceled={}",
                victim, dim(victim), event.getSource().getDamageType(), event.getAmount(), thread(), event.isCanceled());
        if (event.isCanceled()) {
            reportCancellers(event, "LivingDamageEvent");
        }
    }

    private static void reportCancellers(Event event, String eventName) {
        try {
            int busID = resolveForgeBusID();
            if (busID < 0) {
                CombatDebug.logger.warn("[{}] 无法解析 Forge EVENT_BUS 的 busID，跳过取消者点名。", eventName);
                return;
            }
            IEventListener[] listeners = event.getListenerList().getListeners(busID);
            CombatDebug.logger.warn("[{}] 事件被取消，候选取消者监听器列表（按优先级顺序）：", eventName);
            EventPriority phase = null;
            for (IEventListener listener : listeners) {
                if (listener instanceof EventPriority) {
                    phase = (EventPriority) listener;
                    continue;
                }
                if (listener instanceof ASMEventHandler) {
                    String readable = listener.toString();
                    if (readable != null && readable.contains("deliciousbread481")) {
                        continue;
                    }
                    String modName = ownerOf((ASMEventHandler) listener);
                    CombatDebug.logger.warn("    priority={} listener={} mod={}", phase, readable, modName);
                } else {
                    CombatDebug.logger.warn("    priority={} listener={} (非 ASMEventHandler)", phase, listener);
                }
            }
        } catch (Throwable t) {
            CombatDebug.logger.warn("[{}] 点名取消者时出错：{}", eventName, t.toString());
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
            return String.valueOf(owner);
        } catch (Throwable t) {
            return "<owner 读取失败: " + t + ">";
        }
    }

    private static int resolveForgeBusID() {
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