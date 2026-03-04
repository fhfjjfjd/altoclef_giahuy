package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;

/**
 * Advanced combat manager - handles shield blocking, critical hits,
 * sprint-reset combos, circle strafing, and smart retreat.
 */
public class CombatManager {

    public enum CombatState {
        IDLE, ENGAGING, BLOCKING, RETREATING
    }

    private CombatState _state = CombatState.IDLE;

    // Strafing
    private boolean _strafeLeft = true;
    private int _strafeTimer = 0;
    private static final int STRAFE_SWITCH_TICKS = 15;

    // Shield blocking
    private boolean _isBlocking = false;

    // Sprint reset (W-tap)
    private boolean _sprintResetPhase = false;
    private int _sprintResetTimer = 0;

    // Critical hit
    private boolean _jumpingForCrit = false;
    private int _critJumpTimer = 0;

    // Thresholds
    private static final float RETREAT_HEALTH = 6.0f;
    private static final float COOLDOWN_READY = 0.95f;
    private static final float COOLDOWN_JUMP_START = 0.65f;

    /**
     * Main tick - call each game tick when in active combat with a target.
     */
    public void tick(AltoClef mod, Entity target) {
        if (!AltoClef.inGame() || mod.getPlayer() == null || target == null) {
            reset(mod);
            return;
        }

        // Check retreat
        if (shouldRetreat(mod)) {
            _state = CombatState.RETREATING;
            stopBlocking(mod);
            stopStrafing(mod);
            return;
        }

        _state = CombatState.ENGAGING;

        double dist = mod.getPlayer().distanceTo(target);

        // Strafing in close range
        if (dist < 5.0) {
            tickStrafe(mod);
        } else {
            stopStrafing(mod);
        }

        // Shield blocking between attacks
        tickShieldBlock(mod);

        // Sprint management
        tickSprintReset(mod);

        // Critical hit jump timing
        tickCriticalHitJump(mod);
    }

    /**
     * Shield blocking: raise shield between attack cooldowns.
     * Only when sword in main hand and shield in offhand.
     */
    private void tickShieldBlock(AltoClef mod) {
        if (!hasShieldInOffhand(mod)) return;
        if (mod.getFoodChain().isTryingToEat()) return;
        if (!AutoToolEquip.hasWeaponEquipped(mod)) return;

        float cooldown = mod.getPlayer().getAttackCooldownProgress(0);

        if (cooldown < COOLDOWN_READY) {
            startBlocking(mod);
        } else {
            stopBlocking(mod);
        }
    }

    /**
     * Circle strafing: alternate left/right movement.
     */
    private void tickStrafe(AltoClef mod) {
        _strafeTimer++;
        if (_strafeTimer >= STRAFE_SWITCH_TICKS) {
            _strafeTimer = 0;
            _strafeLeft = !_strafeLeft;
        }

        if (_strafeLeft) {
            mod.getInputControls().hold(Input.MOVE_LEFT);
            mod.getInputControls().release(Input.MOVE_RIGHT);
        } else {
            mod.getInputControls().hold(Input.MOVE_RIGHT);
            mod.getInputControls().release(Input.MOVE_LEFT);
        }
    }

    private void stopStrafing(AltoClef mod) {
        mod.getInputControls().release(Input.MOVE_LEFT);
        mod.getInputControls().release(Input.MOVE_RIGHT);
    }

    /**
     * Sprint-reset (W-tap) for extra knockback.
     * After hitting: release sprint for 2 ticks, then re-sprint.
     */
    private void tickSprintReset(AltoClef mod) {
        if (_sprintResetPhase) {
            _sprintResetTimer++;
            if (_sprintResetTimer <= 2) {
                mod.getInputControls().release(Input.SPRINT);
                mod.getPlayer().setSprinting(false);
            } else {
                mod.getInputControls().hold(Input.SPRINT);
                _sprintResetPhase = false;
                _sprintResetTimer = 0;
            }
        } else {
            mod.getInputControls().hold(Input.SPRINT);
        }
    }

    /**
     * Critical hit jump timing.
     * Jump when cooldown is ~65%, attack when falling + cooldown ready.
     */
    private void tickCriticalHitJump(AltoClef mod) {
        float cooldown = mod.getPlayer().getAttackCooldownProgress(0);

        if (!_jumpingForCrit && cooldown >= COOLDOWN_JUMP_START && cooldown < COOLDOWN_READY
                && mod.getPlayer().isOnGround() && !mod.getPlayer().isTouchingWater()) {
            mod.getInputControls().tryPress(Input.JUMP);
            _jumpingForCrit = true;
            _critJumpTimer = 0;
        }

        if (_jumpingForCrit) {
            _critJumpTimer++;
            if (_critJumpTimer > 15) {
                _jumpingForCrit = false;
            }
        }
    }

    /**
     * Called after landing a hit to trigger sprint reset.
     */
    public void onAttackLanded() {
        _sprintResetPhase = true;
        _sprintResetTimer = 0;
        _jumpingForCrit = false;
    }

    /**
     * Check if conditions are right for a critical hit.
     */
    public boolean canCriticalHit(AltoClef mod) {
        if (mod.getPlayer() == null) return false;
        return !mod.getPlayer().isOnGround()
                && mod.getPlayer().getVelocity().y < 0
                && !mod.getPlayer().isTouchingWater()
                && !mod.getPlayer().isClimbing()
                && mod.getPlayer().getAttackCooldownProgress(0) >= COOLDOWN_READY;
    }

    /**
     * Should we attack now? Checks for critical hit opportunity or full cooldown.
     */
    public boolean shouldAttackNow(AltoClef mod) {
        float cooldown = mod.getPlayer().getAttackCooldownProgress(0);

        // If we can land a critical hit, attack now
        if (canCriticalHit(mod)) return true;

        // If jumping for crit but not yet falling, wait a bit
        if (_jumpingForCrit && _critJumpTimer < 10) return false;

        // Normal full cooldown attack
        return cooldown >= COOLDOWN_READY;
    }

    /**
     * Check if we should retreat to heal.
     */
    public boolean shouldRetreat(AltoClef mod) {
        float health = mod.getPlayer().getHealth();
        boolean hasFood = mod.getInventoryTracker().totalFoodScore() > 0;
        return health <= RETREAT_HEALTH && hasFood;
    }

    private boolean hasShieldInOffhand(AltoClef mod) {
        return mod.getPlayer().getOffHandStack().getItem() == Items.SHIELD;
    }

    private void startBlocking(AltoClef mod) {
        if (!_isBlocking) {
            mod.getInputControls().hold(Input.CLICK_RIGHT);
            _isBlocking = true;
            _state = CombatState.BLOCKING;
        }
    }

    private void stopBlocking(AltoClef mod) {
        if (_isBlocking) {
            mod.getInputControls().release(Input.CLICK_RIGHT);
            _isBlocking = false;
        }
    }

    public void reset(AltoClef mod) {
        if (mod != null && AltoClef.inGame() && mod.getPlayer() != null) {
            stopBlocking(mod);
            stopStrafing(mod);
            mod.getInputControls().release(Input.SPRINT);
        }
        _state = CombatState.IDLE;
        _strafeTimer = 0;
        _sprintResetPhase = false;
        _sprintResetTimer = 0;
        _jumpingForCrit = false;
        _critJumpTimer = 0;
    }

    public CombatState getState() { return _state; }
    public boolean isBlocking() { return _isBlocking; }
    public boolean isRetreating() { return _state == CombatState.RETREATING; }
}
