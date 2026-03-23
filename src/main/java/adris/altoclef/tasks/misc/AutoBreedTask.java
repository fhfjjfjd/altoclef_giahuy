package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.entity.AbstractDoToEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Task to automatically breed a specific type of animal.
 * Supports cow, sheep, pig, chicken, horse, wolf, and cat.
 * Tracks breeding cooldowns (5 min), breed count, auto-collects food, and respects max breed limit.
 */
public class AutoBreedTask extends AbstractDoToEntityTask {

    private static final long BREED_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes
    private static final int DEFAULT_FOOD_GATHER_COUNT = 16;

    private final String _animalType;
    private final Item _breedingFood;
    private final Item _tamingItem;
    private final Class<? extends Entity> _entityClass;
    private final TimerGame _interactDelay = new TimerGame(0.5);
    private final int _maxBreedCount;
    private boolean _needsFood = false;
    private int _breedCount = 0;

    // Track per-entity breeding cooldowns by UUID
    private final Map<UUID, Long> _breedCooldowns = new HashMap<>();

    public AutoBreedTask(String animalType) {
        this(animalType, -1);
    }

    public AutoBreedTask(String animalType, int maxBreedCount) {
        super(2.0);
        _animalType = animalType.toLowerCase();
        _breedingFood = getBreedingFood(_animalType);
        _tamingItem = getTamingItem(_animalType);
        _entityClass = getEntityClass(_animalType);
        _maxBreedCount = maxBreedCount;
    }

    public int getBreedCount() {
        return _breedCount;
    }

    private static Item getBreedingFood(String animalType) {
        return switch (animalType) {
            case "cow", "sheep" -> Items.WHEAT;
            case "pig" -> Items.CARROT;
            case "chicken" -> Items.WHEAT_SEEDS;
            case "horse" -> Items.GOLDEN_CARROT;
            case "wolf" -> Items.BEEF;
            case "cat" -> Items.COD;
            default -> Items.WHEAT;
        };
    }

    /**
     * Some animals need to be tamed first before breeding.
     */
    private static Item getTamingItem(String animalType) {
        return switch (animalType) {
            case "wolf" -> Items.BONE;
            case "cat" -> Items.COD;
            default -> null;
        };
    }

    private static Class<? extends Entity> getEntityClass(String animalType) {
        return switch (animalType) {
            case "cow" -> CowEntity.class;
            case "sheep" -> SheepEntity.class;
            case "pig" -> PigEntity.class;
            case "chicken" -> ChickenEntity.class;
            case "horse" -> HorseEntity.class;
            case "wolf" -> WolfEntity.class;
            case "cat" -> CatEntity.class;
            default -> CowEntity.class;
        };
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        mod.log("Starting auto-breed task for " + _animalType
                + (_maxBreedCount > 0 ? " (max: " + _maxBreedCount + ")" : " (unlimited)") + "...");
        _interactDelay.forceElapse();
        _needsFood = false;
        _breedCount = 0;
        _breedCooldowns.clear();
    }

    /**
     * Remove expired cooldowns from tracking map.
     */
    private void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        _breedCooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= BREED_COOLDOWN_MS);
    }

    private boolean isOnCooldown(Entity entity) {
        Long lastBred = _breedCooldowns.get(entity.getUuid());
        if (lastBred == null) return false;
        return System.currentTimeMillis() - lastBred < BREED_COOLDOWN_MS;
    }

    private void markBred(Entity entity) {
        _breedCooldowns.put(entity.getUuid(), System.currentTimeMillis());
        _breedCount++;
        cleanupCooldowns();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if we've reached the max breed count
        if (_maxBreedCount > 0 && _breedCount >= _maxBreedCount) {
            mod.log("Reached max breed count of " + _maxBreedCount + ". Stopping.");
            return null;
        }

        // For wolf/cat, ensure we have taming item first
        if (_tamingItem != null && !mod.getInventoryTracker().hasItem(_tamingItem)) {
            setDebugState("Obtaining taming item: " + _tamingItem.getName().getString() + "...");
            Task tameGather = TaskCatalogue.getItemTask(_tamingItem, 8);
            if (tameGather != null) {
                return tameGather;
            }
        }

        // Auto-collect breeding food if not enough
        int foodCount = mod.getInventoryTracker().getItemCount(_breedingFood);
        if (foodCount < 2) {
            _needsFood = true;
            setDebugState("Collecting " + _breedingFood.getName().getString() + " (" + foodCount + "/" + DEFAULT_FOOD_GATHER_COUNT + ")...");
            Task gatherTask = TaskCatalogue.getItemTask(_breedingFood, DEFAULT_FOOD_GATHER_COUNT);
            if (gatherTask != null) {
                return gatherTask;
            }
            mod.log("Cannot obtain breeding food: " + _breedingFood.getName().getString());
            return null;
        }
        _needsFood = false;

        return super.onTick(mod);
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        // Skip if this entity is on breeding cooldown
        if (isOnCooldown(entity)) {
            return null;
        }

        // For tameable animals (wolf/cat), try taming first if not tamed
        if (_tamingItem != null && entity instanceof TameableEntity tameable) {
            if (!tameable.isTamed()) {
                if (mod.getSlotHandler().forceEquipItem(_tamingItem)) {
                    if (_interactDelay.elapsed()) {
                        _interactDelay.reset();
                        mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
                        mod.getPlayer().swingHand(Hand.MAIN_HAND);
                        mod.log("Attempting to tame " + _animalType + "...");
                    }
                }
                return null;
            }
        }

        // Equip the breeding food and interact with the animal
        if (mod.getSlotHandler().forceEquipItem(_breedingFood)) {
            if (_interactDelay.elapsed()) {
                _interactDelay.reset();
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
                mod.getPlayer().swingHand(Hand.MAIN_HAND);
                markBred(entity);
                mod.log("Bred " + _animalType + "! Total: " + _breedCount
                        + (_maxBreedCount > 0 ? "/" + _maxBreedCount : ""));
            }
        }
        return null;
    }

    @Override
    protected Entity getEntityTarget(AltoClef mod) {
        if (!mod.getEntityTracker().entityFound(_entityClass)) {
            return null;
        }
        return mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(),
                entity -> entity.isAlive()
                        && !(entity instanceof LivingEntity le && le.isBaby())
                        && !isOnCooldown(entity),
                _entityClass);
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof AutoBreedTask task) {
            return task._animalType.equals(_animalType) && task._maxBreedCount == _maxBreedCount;
        }
        return false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        super.onStop(mod, interruptTask);
        mod.log("Auto-breed task stopped. Total bred: " + _breedCount);
    }

    @Override
    protected String toDebugString() {
        String countStr = " (bred: " + _breedCount + (_maxBreedCount > 0 ? "/" + _maxBreedCount : "") + ")";
        if (_needsFood) {
            return "Auto-breed " + _animalType + ": gathering food" + countStr;
        }
        return "Auto-breeding " + _animalType + countStr;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _maxBreedCount > 0 && _breedCount >= _maxBreedCount;
    }
}
