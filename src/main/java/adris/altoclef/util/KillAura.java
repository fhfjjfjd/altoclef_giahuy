package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.StlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controls and applies killaura with advanced combat integration.
 * Uses CombatManager for critical hits, shield blocking, sprint combos, and strafing.
 */
public class KillAura {

    private final List<Entity> _targets = new ArrayList<>();
    private final TimerGame _hitDelay = new TimerGame(0.2);
    private double _forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity _forceHit = null;

    public void tickStart(AltoClef mod) {
        _targets.clear();
        _forceHit = null;
    }

    public void applyAura(AltoClef mod, Entity entity) {
        _targets.add(entity);
        if (entity instanceof FireballEntity) _forceHit = entity;
    }

    public void tickEnd(AltoClef mod) {
        CombatManager combat = mod.getCombatManager();

        // Tick combat manager with closest target for strafing/blocking/crit
        Entity closestTarget = getClosestTarget(mod);
        if (!_targets.isEmpty() && closestTarget != null) {
            combat.tick(mod, closestTarget);
        } else {
            combat.reset(mod);
        }

        switch (mod.getModSettings().getForceFieldStrategy()) {
            case FASTEST:
                performFastestAttack(mod);
                break;
            case SMART:
                if (_targets.size() < 2) {
                    performAdvancedAttack(mod);
                } else {
                    if (_forceHit != null) {
                        attack(mod, _forceHit, true);
                        break;
                    }
                    if (_hitDelay.elapsed()) {
                        _hitDelay.reset();
                        if (closestTarget != null) {
                            attack(mod, closestTarget, true);
                        }
                    }
                }
                break;
            case DELAY:
                performAdvancedAttack(mod);
                break;
            case OFF:
                break;
        }
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    private Entity getClosestTarget(AltoClef mod) {
        if (_targets.isEmpty()) return null;
        return _targets.stream()
                .min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())))
                .orElse(null);
    }

    /**
     * Advanced attack: uses CombatManager for critical hit timing and sprint reset.
     */
    private void performAdvancedAttack(AltoClef mod) {
        if (_targets.isEmpty()) return;

        Entity target = getClosestTarget(mod);
        if (target == null) return;

        CombatManager combat = mod.getCombatManager();

        // Use CombatManager to decide optimal attack timing (crit + cooldown)
        if (combat.shouldAttackNow(mod)) {
            attack(mod, target, true);
            combat.onAttackLanded();
        }
    }

    private void performFastestAttack(AltoClef mod) {
        for (Entity entity : _targets) {
            attack(mod, entity, false);
        }
    }

    private void attack(AltoClef mod, Entity entity, boolean equipWeapon) {
        if (entity == null) return;
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange * _forceFieldRange) {
            boolean canAttack;
            if (equipWeapon) {
                canAttack = AutoToolEquip.equipBestWeapon(mod);
            } else {
                canAttack = mod.getSlotHandler().forceDeequipHitTool();
            }
            if (canAttack) {
                mod.getControllerExtras().attack(entity);
            }
        }
    }

    public enum Strategy {
        OFF,
        FASTEST,
        DELAY,
        SMART
    }
}
