package deliciousbread481.combatdebug;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CombatDebug.MODID)
public class CombatEventHandler {

    private static int playerCount() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null ? -1 : server.getCurrentPlayerCount();
    }

    private static String ctx(Entity e) {
        boolean remote = e != null && e.world != null && e.world.isRemote;
        return "[side=" + (remote ? "CLIENT" : "SERVER")
                + ", thread=" + Thread.currentThread().getName()
                + ", players=" + playerCount() + "]";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity target = event.getTarget();
        CombatDebug.logger.info("{} AttackEntityEvent: player={} target={} canceled={}",
                ctx(player), player.getName(),
                target == null ? "null" : target.getName(),
                event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        CombatDebug.logger.info("{} LivingAttackEvent: victim={} source={} amount={} canceled={}",
                ctx(event.getEntity()), event.getEntity().getName(),
                event.getSource().getDamageType(), event.getAmount(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingHurt(LivingHurtEvent event) {
        CombatDebug.logger.info("{} LivingHurtEvent: victim={} source={} amount={} canceled={}",
                ctx(event.getEntity()), event.getEntity().getName(),
                event.getSource().getDamageType(), event.getAmount(), event.isCanceled());
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDamage(LivingDamageEvent event) {
        CombatDebug.logger.info("{} LivingDamageEvent: victim={} source={} amount={} canceled={}",
                ctx(event.getEntity()), event.getEntity().getName(),
                event.getSource().getDamageType(), event.getAmount(), event.isCanceled());
    }
}