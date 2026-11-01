package com.example.client.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class PlayerInventoryScanner {

    private PlayerInventoryScanner() {
    }

    public static Map<String, Integer> scan(Player player) {
        Map<String, Integer> available = new HashMap<>();

        if (player == null) {
            return available;
        }

        var inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof BlockItem blockItem) {
                String blockId = BuiltInRegistries.BLOCK
                        .getKey(blockItem.getBlock())
                        .toString();

                available.merge(
                        blockId,
                        stack.getCount(),
                        Integer::sum
                );
            }
        }

        return available;
    }
}
