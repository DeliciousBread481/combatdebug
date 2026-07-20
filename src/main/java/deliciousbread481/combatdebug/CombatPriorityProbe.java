package deliciousbread481.combatdebug;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "combatdebug")
public class CombatPriorityProbe {

    private static final Set<Class<?>> WRAPPED =
            Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());


    private static int busID = -1;
    private static Field listsField;
    private static Field instListenersField;
    private static Field instRebuildField;
    private static Field asmOwnerField;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        ensureWrapped(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource() != null && "player".equals(event.getSource().getDamageType())) {
            ensureWrapped(event);
        }
    }

    private static void ensureWrapped(Event event) {
        try {
            initReflection();

            ListenerList ll = event.getListenerList();

            Object[] lists = (Object[]) listsField.get(ll);
            Object inst = lists[busID];

            inst.getClass().getMethod("getListeners").invoke(inst);

            IEventListener[] arr = (IEventListener[]) instListenersField.get(inst);
            if (arr == null) {
                return;
            }

            for (IEventListener l : arr) {
                if (l instanceof ProbeListener) {
                    return;
                }
            }

            IEventListener[] wrapped =
                    (IEventListener[]) Array.newInstance(IEventListener.class, arr.length);
            EventPriority currentBand = null;
            for (int i = 0; i < arr.length; i++) {
                IEventListener l = arr[i];
                if (l instanceof EventPriority) {
                    currentBand = (EventPriority) l;
                    wrapped[i] = l;
                } else {
                    wrapped[i] = new ProbeListener(l, currentBand);
                }
            }

            instListenersField.set(inst, wrapped);
            instRebuildField.setBoolean(inst, false);

            if (WRAPPED.add(event.getClass())) {
                CombatDebug.logger.warn("[Probe] 已为事件 {} 安装逐 listener 探针（共 {} 个真实监听器）",
                        event.getClass().getSimpleName(), arr.length);
            }
        } catch (Throwable t) {
            CombatDebug.logger.warn("[Probe] 安装探针失败: {}", t.toString());
        }
    }

    private static void initReflection() throws Exception {
        if (busID != -1) {
            return;
        }

        Field busIdF = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
        busIdF.setAccessible(true);
        int id = busIdF.getInt(MinecraftForge.EVENT_BUS);

        listsField = ListenerList.class.getDeclaredField("lists");
        listsField.setAccessible(true);

        Class<?> instClass = Class.forName(
                "net.minecraftforge.fml.common.eventhandler.ListenerList$ListenerListInst");
        instListenersField = instClass.getDeclaredField("listeners");
        instListenersField.setAccessible(true);
        instRebuildField = instClass.getDeclaredField("rebuild");
        instRebuildField.setAccessible(true);

        busID = id;
    }

    private static class ProbeListener implements IEventListener {
        private final IEventListener delegate;
        private final EventPriority band;

        ProbeListener(IEventListener delegate, EventPriority band) {
            this.delegate = delegate;
            this.band = band;
        }

        @Override
        public void invoke(Event event) {
            boolean before = event.isCancelable() && event.isCanceled();
            delegate.invoke(event);
            boolean after = event.isCancelable() && event.isCanceled();

            if (!before && after) {
                CombatDebug.logger.warn(
                        "[Probe] >>> 取消者锁定: event={} priority={} listener={} mod={}",
                        event.getClass().getSimpleName(),
                        band, delegate, ownerOf(delegate));
            }
        }
    }

    private static String ownerOf(IEventListener listener) {
        try {
            if (asmOwnerField == null) {
                Class<?> asm = Class.forName(
                        "net.minecraftforge.fml.common.eventhandler.ASMEventHandler");
                asmOwnerField = asm.getDeclaredField("owner");
                asmOwnerField.setAccessible(true);
            }
            Object owner = asmOwnerField.get(listener);
            if (owner instanceof ModContainer) {
                ModContainer mc = (ModContainer) owner;
                return mc.getName() + " (" + mc.getModId() + ")";
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }
}