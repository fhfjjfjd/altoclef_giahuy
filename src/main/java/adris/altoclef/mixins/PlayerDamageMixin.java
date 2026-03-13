package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.util.PvPManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerDamageMixin {
    
    @Inject(method = "damage", at = @At("HEAD"))
    private void onPlayerDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        // Check if this is the main player and PvP mode is enabled
        if (AltoClef.INSTANCE != null && 
            AltoClef.INSTANCE.getCombatManager() != null && 
            AltoClef.INSTANCE.getCombatManager().isPvPEnabled() && 
            player.equals(AltoClef.INSTANCE.getPlayer())) {
            
            // Check if the damage is coming from another player
            Entity attacker = source.getAttacker();
            if (attacker instanceof PlayerEntity && !attacker.equals(player)) {
                // Player was attacked by another player, notify the PvP manager
                PvPManager pvpManager = AltoClef.INSTANCE.getPvPManager();
                if (pvpManager != null) {
                    pvpManager.onPlayerAttack(AltoClef.INSTANCE, (PlayerEntity) attacker);
                }
            }
        }
    }
}