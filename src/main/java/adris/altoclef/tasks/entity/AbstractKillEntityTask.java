package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.AutoToolEquip;
import adris.altoclef.util.CombatManager;
import net.minecraft.entity.Entity;

/**
 * Attacks an entity with advanced combat mechanics.
 * Uses CombatManager for critical hits and sprint-reset combos.
 */
public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {

    private static final double OTHER_FORCE_FIELD_RANGE = 2;
    private static final double CONSIDER_COMBAT_RANGE = 10;

    public AbstractKillEntityTask() {
        this(CONSIDER_COMBAT_RANGE, OTHER_FORCE_FIELD_RANGE);
    }

    public AbstractKillEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public static void equipWeapon(AltoClef mod) {
        AutoToolEquip.equipBestWeapon(mod);
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        CombatManager combat = mod.getCombatManager();

        // Equip weapon
        equipWeapon(mod);

        // Use CombatManager for optimal attack timing (critical hits + cooldown)
        if (combat.shouldAttackNow(mod)) {
            mod.getControllerExtras().attack(entity);
            combat.onAttackLanded();
        }
        return null;
    }
}
