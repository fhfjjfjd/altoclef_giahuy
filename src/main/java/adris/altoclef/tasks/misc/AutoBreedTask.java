package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.entity.AbstractDoToEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Task to automatically breed a specific type of animal.
 * Finds nearby animals, approaches them, and feeds them their breeding food.
 */
public class AutoBreedTask extends AbstractDoToEntityTask {

    private final String _animalType;
    private final Item _breedingFood;
    private final Class<? extends Entity> _entityClass;
    private final TimerGame _interactDelay = new TimerGame(0.5);
    private boolean _needsFood = false;

    public AutoBreedTask(String animalType) {
        super(2.0);
        _animalType = animalType.toLowerCase();
        _breedingFood = getBreedingFood(_animalType);
        _entityClass = getEntityClass(_animalType);
    }

    private static Item getBreedingFood(String animalType) {
        return switch (animalType) {
            case "cow", "sheep" -> Items.WHEAT;
            case "pig" -> Items.CARROT;
            case "chicken" -> Items.WHEAT_SEEDS;
            default -> Items.WHEAT;
        };
    }

    private static Class<? extends Entity> getEntityClass(String animalType) {
        return switch (animalType) {
            case "cow" -> CowEntity.class;
            case "sheep" -> SheepEntity.class;
            case "pig" -> PigEntity.class;
            case "chicken" -> ChickenEntity.class;
            default -> CowEntity.class;
        };
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        mod.log("Starting auto-breed task for " + _animalType + "...");
        _interactDelay.forceElapse();
        _needsFood = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Ensure we have breeding food
        if (!mod.getInventoryTracker().hasItem(_breedingFood)) {
            _needsFood = true;
            setDebugState("Obtaining " + _breedingFood.getName().getString() + "...");
            Task gatherTask = TaskCatalogue.getItemTask(_breedingFood, 16);
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
        // Equip the breeding food and interact with the animal
        if (mod.getSlotHandler().forceEquipItem(_breedingFood)) {
            if (_interactDelay.elapsed()) {
                _interactDelay.reset();
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
                mod.getPlayer().swingHand(Hand.MAIN_HAND);
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
                entity -> entity.isAlive() && !(entity instanceof LivingEntity && ((LivingEntity) entity).isBaby()),
                _entityClass);
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof AutoBreedTask task) {
            return task._animalType.equals(_animalType);
        }
        return false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        super.onStop(mod, interruptTask);
        mod.log("Auto-breed task stopped.");
    }

    @Override
    protected String toDebugString() {
        if (_needsFood) {
            return "Auto-breed " + _animalType + ": gathering food";
        }
        return "Auto-breeding " + _animalType;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Runs indefinitely until stopped
        return false;
    }
}
