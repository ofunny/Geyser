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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.WorldBorderAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket;
import com.nukkitx.math.vector.Vector2f;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.border.WorldBorder;

@Translator(packet = ServerWorldBorderPacket.class)
public class JavaWorldBorderTranslator extends PacketTranslator<ServerWorldBorderPacket> {

    @Override
    public void translate(ServerWorldBorderPacket packet, GeyserSession session) {
        WorldBorder worldBorder = session.getWorldBorder();

        if(packet.getAction() != WorldBorderAction.INITIALIZE && worldBorder == null) return;
            /*
             * Okay sir/madam, you are getting worldBorder from the session above,
             * and if worldBorder == null you going to check the world border task?
             * I mean, what else should happen than an exception :) let me remove that for you.
             * By the way, worldBorder offers a public method for that purpose if ever needed.
             */
            //if (session.getWorldBorder().getWorldBorderTask() != null) {
            //    session.getWorldBorder().getWorldBorderTask().cancel(false);
            //}

        switch(packet.getAction()) {
            case INITIALIZE:
                /*
                 * How it should be normally.
                 */
                /*worldBorder = new WorldBorder(Vector2f.from(packet.getNewCenterX(), packet.getNewCenterZ()), packet.getOldSize(), packet.getNewSize(),
                        packet.getLerpTime(), packet.getWarningTime(), packet.getWarningBlocks());*/

/*
FIXME packet.getWarningTime() and packet.getWarningBlocks() interchanged for the INITIALIZE action an have to get fixed withing the ServerWorldBorderPacket.
For now I just interchange the methods locally since I did not want to mess with the ServerWorldBorderPacket class.
As soon as fixed, the call above can be used again!
 */
                worldBorder = new WorldBorder(Vector2f.from(packet.getNewCenterX(), packet.getNewCenterZ()), packet.getOldSize(), packet.getNewSize(),
                        packet.getLerpTime(), packet.getWarningBlocks(), packet.getWarningTime());


                session.setWorldBorder(worldBorder);
                break;
            case SET_SIZE:
                worldBorder.setOldDiameter(packet.getNewSize());
                worldBorder.setNewDiameter(packet.getNewSize());
                break;
            case LERP_SIZE:
                worldBorder.setOldDiameter(packet.getOldSize());
                worldBorder.setNewDiameter(packet.getNewSize());
                worldBorder.setSpeed(packet.getLerpTime());
                break;
            case SET_CENTER:
                worldBorder.setCenter(Vector2f.from(packet.getNewCenterX(), packet.getNewCenterZ()));
                break;
            case SET_WARNING_TIME:
                worldBorder.setWarningTime(packet.getWarningTime());
                break;
            case SET_WARNING_BLOCKS:
                worldBorder.setWarningBlocks(packet.getWarningBlocks());
                break;
        }// end switch

        /*
         * Call update after each change or nothing will take effect –> see the methods description.
         */
        worldBorder.update(session);
    }//end translate

}// end class JavaWorldBorderTranslator
