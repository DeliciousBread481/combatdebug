package deliciousbread481.combatdebug;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "combatdebug")
public class CombatPriorityProbe {

    private static int busID = -1;
    private static boolean installed = false;

    private static Field LISTS_FIELD;
    private static Method GET_LISTENERS_METHOD;
    private static Field LISTENERS_FIELD;
    private static Field REBUILD_FIELD;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        ensureWrapped(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        ensureWrapped(event);
    }

    private static void ensureWrapped(Event event) {
        try {
            if (busID < 0) {
                Field f = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
                f.setAccessible(true);
                busID = f.getInt(MinecraftForge.EVENT_BUS);
            }
            ListenerList list = event.getListenerList();

            if (LISTS_FIELD == null) {
                LISTS_FIELD = ListenerList.class.getDeclaredField("lists");
                LISTS_FIELD.setAccessible(true);
            }
            Object[] insts = (Object[]) LISTS_FIELD.get(list);
            Object inst = insts[busID];

            if (GET_LISTENERS_METHOD == null) {
                GET_LISTENERS_METHOD = inst.getClass().getDeclaredMethod("getListeners");
                GET_LISTENERS_METHOD.setAccessible(true);
            }
            GET_LISTENERS_METHOD.invoke(inst);

            if (LISTENERS_FIELD == null) {
                LISTENERS_FIELD = inst.getClass().getDeclaredField("listeners");
                LISTENERS_FIELD.setAccessible(true);
            }
            if (REBUILD_FIELD == null) {
                REBUILD_FIELD = inst.getClass().getDeclaredField("rebuild");
                REBUILD_FIELD.setAccessible(true);
            }

            IEventListener[] arr = (IEventListener[]) LISTENERS_FIELD.get(inst);
            for (IEventListener l : arr) {
                if (l instanceof Wrapper) {
                    return;
                }
            }

            EventPriority phase = null;
            IEventListener[] wrapped = new IEventListener[arr.length];
            for (int i = 0; i < arr.length; i++) {
                IEventListener l = arr[i];
                if (l instanceof EventPriority) {
                    phase = (EventPriority) l;
                    wrapped[i] = l;
                } else {
                    wrapped[i] = new Wrapper(l, phase);
                }
            }
            LISTENERS_FIELD.set(inst, wrapped);
            REBUILD_FIELD.setBoolean(inst, false);

            if (!installed) {
                installed = true;
                CombatDebug.logger.warn("[Probe] 已为 {} 安装逐优先级探针", event.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            CombatDebug.logger.warn("[Probe] 安装探针失败: " + t);
        }
    }

    private static class Wrapper implements IEventListener {
        private final IEventListener delegate;
        private final EventPriority phase;

        Wrapper(IEventListener delegate, EventPriority phase) {
            this.delegate = delegate;
            this.phase = phase;
        }

        @Override
        public void invoke(Event event) {
            boolean before = event.isCancelable() && event.isCanceled();
            delegate.invoke(event);
            boolean after = event.isCancelable() && event.isCanceled();
            if (!before && after) {
                CombatDebug.logger.warn(">>> 取消者锁定: priority={} listener={} mod={}",
                        phase, delegate, ownerOf(delegate));
            }
        }
    }

    private static String ownerOf(IEventListener listener) {
        try {
            if (listener instanceof ASMEventHandler) {
                Field ownerField = ASMEventHandler.class.getDeclaredField("owner");
                ownerField.setAccessible(true);
                Object owner = ownerField.get(listener);
                if (owner instanceof ModContainer) {
                    ModContainer mc = (ModContainer) owner;
                    return mc.getName() + " (" + mc.getModId() + ")";
                }
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }
}