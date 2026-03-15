package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.entity.AbstractDoToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.HashSet;
import java.util.List;

/**
 * Task to automatically trade with villagers.
 */
public class AutoVillagerTradeTask extends Task {
    
    private final ItemTarget _targetItem;
    private final int _targetCount;
    private final Task _tradeTask;
    private Task _collectMaterialsTask = null;

    public AutoVillagerTradeTask(ItemTarget targetItem, int targetCount) {
        _targetItem = targetItem;
        _targetCount = targetCount;
        _tradeTask = new PerformTradeWithVillager(targetItem);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-villager trade for " + _targetCount + " " + 
                _targetItem.getMatches()[0].getName().getString());
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we already have enough of the target item, we're done
        if (mod.getInventoryTracker().getItemCount(_targetItem.getMatches()) >= _targetCount) {
            mod.log("Already have enough " + _targetItem.getMatches()[0].getName().getString() + ", no need to trade.");
            return null;
        }
        
        // Check if we have materials needed for trading
        // For simplicity, we'll assume we need emeralds for trading
        if (!mod.getInventoryTracker().hasItem(Items.EMERALD)) {
            if (_collectMaterialsTask == null || _collectMaterialsTask.isFinished(mod)) {
                _collectMaterialsTask = TaskCatalogue.getItemTask(Items.EMERALD, 64); // Collect some emeralds
            }
            return _collectMaterialsTask;
        }
        
        // If we have enough of the target item, we're done
        if (mod.getInventoryTracker().getItemCount(_targetItem.getMatches()) >= _targetCount) {
            return null;
        }
        
        // Look for villagers to trade with
        if (!mod.getEntityTracker().entityFound(VillagerEntity.class)) {
            return new TimeoutWanderTask(false);
        }
        
        // Perform trading with villagers
        return _tradeTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-villager trade task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoVillagerTradeTask task) {
            return task._targetItem.equals(_targetItem) && task._targetCount == _targetCount;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Auto-trading with villagers for " + _targetCount + " " + 
               _targetItem.getMatches()[0].getName().getString();
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().getItemCount(_targetItem.getMatches()) >= _targetCount;
    }

    static class PerformTradeWithVillager extends AbstractDoToEntityTask {
        
        private static final double VILLAGER_NEARBY_RADIUS = 10;
        private final TimerGame _tradeTimeout = new TimerGame(5); // 5 seconds to trade
        private final TimerGame _intervalTimeout = new TimerGame(10); // 10 seconds between trades
        private final HashSet<Entity> _blacklisted = new HashSet<>();
        private Entity _currentlyTrading = null;
        private int _lastTradeCount = 0;
        private final ItemTarget _targetItem;

        public PerformTradeWithVillager(ItemTarget targetItem) {
            super(3); // 3 block distance
            _targetItem = targetItem;
        }

        @Override
        protected void onStart(AltoClef mod) {
            super.onStart(mod);

            mod.getBehaviour().push();
            // Don't throw away our emeralds
            mod.getBehaviour().addProtectedItems(Items.EMERALD);

            // Don't attack villagers unless we've blacklisted them.
            mod.getBehaviour().addForceFieldExclusion(entity -> {
                if (entity instanceof VillagerEntity) {
                    return !_blacklisted.contains(entity);
                }
                return false;
            });
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            mod.getBehaviour().pop();
            super.onStop(mod, interruptTask);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof PerformTradeWithVillager;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (!(entity instanceof VillagerEntity villager)) {
                return null;
            }

            // If we didn't run this in a while, reset timers
            if (_intervalTimeout.elapsed()) {
                _tradeTimeout.reset();
                _intervalTimeout.reset();
            }

            // We're trading so reset the trade timeout
            if (mod.getPlayer().currentScreenHandler instanceof MerchantScreenHandler) {
                _tradeTimeout.reset();
            }

            // We're trading with a new entity.
            if (!entity.equals(_currentlyTrading)) {
                _currentlyTrading = entity;
                _tradeTimeout.reset();
            }

            if (_tradeTimeout.elapsed()) {
                // We failed trading.
                Debug.logMessage("Failed trading with current villager, blacklisting.");
                _blacklisted.add(_currentlyTrading);
                _tradeTimeout.reset();
                _currentlyTrading = null;
                return null;
            }

            setDebugState("Trading with villager");

            // Interact with the villager to open trade menu
            if (mod.getSlotHandler().forceEquipItem(Items.EMERALD)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, net.minecraft.util.Hand.MAIN_HAND);
                _intervalTimeout.reset();
            }
            
            // After opening the trade menu, we need to handle the actual trading
            // This would involve finding the right trade and clicking it
            // For now, let's just wait for the trade to happen
            
            return null;
        }

        @Override
        protected Entity getEntityTarget(AltoClef mod) {
            // Find a villager that has trades matching our target
            Entity found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(),
                    entity -> {
                        if (_blacklisted.contains(entity)
                                || !(entity instanceof VillagerEntity)
                                || (entity instanceof net.minecraft.entity.LivingEntity && ((net.minecraft.entity.LivingEntity) entity).isBaby())
                                || (_currentlyTrading != null && !entity.isInRange(_currentlyTrading, VILLAGER_NEARBY_RADIUS))) {
                            return false;
                        }
                        
                        // Check if this villager has trades for our target item
                        if (entity instanceof VillagerEntity villager) {
                            TradeOfferList trades = villager.getOffers();
                            if (trades != null) {
                                for (int i = 0; i < trades.size(); i++) {
                                    TradeOffer trade = trades.get(i);
                                    if (trade != null) {
                                        // Check if the result of this trade matches our target
                                        // In Minecraft 1.17.1, use getSellItem() instead of getOutput()
                                        Item sellItem = trade.getSellItem().getItem();
                                        if (_targetItem.matches(sellItem)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        return false;
                    }, VillagerEntity.class
            );
            
            if (found == null) {
                if (_currentlyTrading != null && (_blacklisted.contains(_currentlyTrading) || !_currentlyTrading.isAlive())) {
                    _currentlyTrading = null;
                }
                found = _currentlyTrading;
            }
            return found;
        }

        @Override
        protected String toDebugString() {
            return "Trading with villager";
        }
    }
}