package deliciousbread481.combatdebug.mixin;

import deliciousbread481.combatdebug.CombatDebug;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Event.class, remap = false)
public abstract class MixinEvent {

    @Inject(method = "setCanceled", at = @At("HEAD"))
    private void combatdebug$onSetCanceled(boolean cancel, CallbackInfo ci) {
        if (!cancel) return;
        Object self = this;
        String type = self.getClass().getSimpleName();
        if (type.equals("AttackEntityEvent")
                || type.equals("LivingAttackEvent")
                || type.equals("LivingHurtEvent")
                || type.equals("LivingDamageEvent")) {
            CombatDebug.logger.warn(
                "[CombatDebug] >>> {} 被取消! thread={} 。下面是取消者(mod)的调用堆栈:",
                type, Thread.currentThread().getName(),
                new Throwable("CombatDebug cancel origin"));
        }
    }
}