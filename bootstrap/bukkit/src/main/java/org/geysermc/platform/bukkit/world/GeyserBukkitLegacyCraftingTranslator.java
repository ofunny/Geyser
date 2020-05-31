/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.bukkit.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.CraftingData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.platform.bukkit.GeyserBukkitPlugin;
import us.myles.ViaVersion.protocols.protocol1_13_1to1_13.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;

import java.util.*;

@AllArgsConstructor
public class GeyserBukkitLegacyCraftingTranslator implements Listener {

    GeyserConnector connector;
    boolean isLegacy;
    boolean isViaVersion;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (GeyserSession session : connector.getPlayers().values()) {
            if (event.getPlayer() == Bukkit.getPlayer(session.getPlayerEntity().getUsername())) {
                GeyserBukkitPlugin.getPlayerToSessionMap().put(event.getPlayer(), session);
                if (isLegacy && isViaVersion) {
                    System.out.println("Sending recipes");
                    sendAllRecipes(session);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        GeyserBukkitPlugin.getPlayerToSessionMap().remove(event.getPlayer());
    }

    public static void sendAllRecipes(GeyserSession session) {
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            ItemData output = translateToBedrock(recipe.getResult());
            output = ItemData.of(output.getId(), output.getDamage(), output.getCount(), null);
            if (output.getId() == 0) continue;
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                System.out.println(shapedRecipe.getIngredientMap().toString());
                ItemData[] input = new ItemData[shapedRecipe.getIngredientMap().size()];
                for (int i = 0; i < input.length; i++) {
                    ItemData itemData = translateToBedrock(shapedRecipe.getIngredientMap().get((char) ('a' + i)));
                    input[i] = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount());
                }
                UUID uuid = UUID.randomUUID();
                craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                        3, 3, input,
                        new ItemData[]{output}, uuid, "crafting_table", 0));
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                ItemData[] input = new ItemData[shapelessRecipe.getIngredientList().size()];
                for (int i = 0; i < input.length; i++) {
                    input[i] = translateToBedrock(shapelessRecipe.getIngredientList().get(i));
                }
                UUID uuid = UUID.randomUUID();
                craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                        input, new ItemData[]{output}, uuid, "crafting_table", 0));
            }
        }
        System.out.println(craftingDataPacket.getCraftingData());
        session.sendUpstreamPacket(craftingDataPacket);
        session.setSentDeclareRecipesPacket(true);
    }

    @SuppressWarnings("deprecation")
    private static ItemData translateToBedrock(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack != null && itemStack.getData() != null) {
            int legacyId = (itemStack.getType().getId() << 4) | (itemStack.getData().getData() & 0xFFFF);
            // old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15
            int thirteenId;
            if (MappingData.oldToNewItems.containsKey(legacyId)) {
                thirteenId = MappingData.oldToNewItems.get(legacyId);
            } else if (MappingData.oldToNewItems.containsKey((itemStack.getType().getId() << 4) | (0))) {
                thirteenId = MappingData.oldToNewItems.get((itemStack.getType().getId() << 4) | (0));
            } else {
                System.out.println("NO ID FOUND FOR " + itemStack.toString());
                return ItemData.AIR;
            }
            int thirteenPointOneId = InventoryPackets.getNewItemId(thirteenId);
            int fourteenId = us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData.oldToNewItems.get(thirteenPointOneId);
            int fifteenId = us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData.oldToNewItems.get(fourteenId);
            ItemStack mcItemStack = new ItemStack(fifteenId, itemStack.getAmount());
            ItemData finalData = ItemTranslator.translateToBedrock(mcItemStack);
            return ItemData.of(finalData.getId(), finalData.getDamage(), finalData.getCount(), null);
        }
        System.out.println("Returning air.");
        return ItemData.AIR;
    }
}
