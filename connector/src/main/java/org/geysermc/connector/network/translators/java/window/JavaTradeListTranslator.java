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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.VillagerTrade;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerTradeListPacket;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.UpdateTradePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Translator(packet = ServerTradeListPacket.class)
public class JavaTradeListTranslator extends PacketTranslator<ServerTradeListPacket> {

    @Override
    public void translate(ServerTradeListPacket packet, GeyserSession session) {
        UpdateTradePacket tradePacket = new UpdateTradePacket();
        tradePacket.setTradeTier(packet.getVillagerLevel() - 1);
        System.out.println("Window ID " + packet.getWindowId());
        tradePacket.setWindowId((short) packet.getWindowId());
        tradePacket.setDisplayName("Villager");
        tradePacket.setUnknownInt(0);
        tradePacket.setScreen2(true);
        tradePacket.setWilling(true);
        tradePacket.setPlayerUniqueEntityId(session.getPlayerEntity().getGeyserId());
        tradePacket.setTraderUniqueEntityId(UUID.randomUUID().getMostSignificantBits());
        CompoundTagBuilder builder = CompoundTagBuilder.builder();
        List<CompoundTag> tags = new ArrayList<>();
        for (VillagerTrade trade : packet.getTrades()) {
            CompoundTagBuilder recipe = CompoundTagBuilder.builder();
            recipe.intTag("maxUses", trade.getMaxUses());
            recipe.intTag("traderExp", packet.getExperience());
            recipe.floatTag("priceMultiplierA", trade.getPriceMultiplier());
            recipe.tag(GetItemTag(session, trade.getOutput(), "sell"));
            recipe.floatTag("priceMultiplierB", 0.0f);
            recipe.intTag("buyCountB", 0);
            recipe.intTag("buyCountA", trade.getOutput().getAmount());
            recipe.intTag("demand", trade.getDemand());
            recipe.intTag("tier", packet.getVillagerLevel() - 1);
            recipe.tag(GetItemTag(session, trade.getFirstInput(), "buyA"));
//            if (trade.getSecondInput() != null) {
//                System.out.println("Second input: " + trade.getSecondInput());
//                recipe.tag(GetItemTag(session, trade.getSecondInput(), "buyB"))
//            }
            recipe.intTag("uses", trade.getNumUses());
            recipe.byteTag("rewardExp", (byte) trade.getXp());
            tags.add(recipe.buildRootTag());
        }
        builder.listTag("Recipes", CompoundTag.class, tags);
        List<CompoundTag> expTags = new ArrayList<>();
        expTags.add(CompoundTagBuilder.builder().intTag("0", 0).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("1", 10).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("2", 60).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("3", 160).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("4", 310).buildRootTag());
        builder.listTag("TierExpRequirements", CompoundTag.class, expTags);
        tradePacket.setOffers(builder.buildRootTag());
        System.out.println(tradePacket.toString());
        session.getUpstream().sendPacket(tradePacket);
    }

    private CompoundTag GetItemTag(GeyserSession session, ItemStack stack, String name) {
        ItemData itemData = Translators.getItemTranslator().translateToBedrock(session, stack);
        ItemEntry itemEntry = Translators.getItemTranslator().getItem(stack);
        CompoundTagBuilder builder = CompoundTagBuilder.builder();
        builder.byteTag("Count", (byte) itemData.getCount());
        builder.shortTag("Damage", itemData.getDamage());
        builder.stringTag("Name", itemEntry.getJavaIdentifier());
        return builder.build(name);
    }
}
