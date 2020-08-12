/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.world.border;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * This class is nothing more than a wrapper for the clients session.
 * Actually big parts of the clients session are not designed to be thread safe by default,
 * what means, we could run into thread interference or memory consistency errors.
 *
 * Since I have to access at least some members of the session periodically from
 * a different thread I wanna make sure, that no developer gets inveigled to use the
 * passed session or any of it's members wildly within the world border task.
 *
 * That's why I only allow access to parts I consider to be thread safe or
 * parts I altered to be thread safe in the provided manner.
 */
public class WorldBorderSessionWrapper {

    private final GeyserSession session;
    private final PlayerEntity player;
    private final EntityType entityType;

    public WorldBorderSessionWrapper(GeyserSession session) {
        this.session = session;
        this.player = session.getPlayerEntity();
        this.entityType = player.getEntityType();
    }// end constructor

    /*
     * I made the players position, rotation and onGround volatile and allow read only.
     * So it can be shared safely across threads. The entity type is an Enum and
     * thread safe by default.
     *
     * Moving an entity is the only method that actually needs locking on write to become
     * thread safe. You should not worry about it, since it will mostly be to written
     * by the main thread, what means that it will hardly bring any downsides with it.
     *
     * Note, if you need to write to a session member one day, volatile won't be enough!
     */

    /**
     * Returns a clone of the current entity position. Since it is volatile and a clone it is thread safe
     * but use a local reference if you use it more than once, otherwise you would clone it all the time!
     * @return a clone of the entities position
     */
    public Vector3f getEntityPosition() {
        return player.getPosition().clone();
    }

    /**
     * Returns a clone of the current entity rotation. Since it is volatile and a clone it is thread safe
     * but use a local reference if you use it more than once, otherwise you would clone it all the time!
     * @return a clone of the entities position
     */
    public Vector3f getEntityRotation() {
        return player.getRotation().clone();
    }// end getEntityRotation

    /**
     * Returns boolean if the entity is on the ground. Since it is volatile it is thread safe.
     * @return a boolean if the entity is on the ground.
     */
    public boolean isEntityOnGround() { return player.isOnGround(); }// end isEntityOnGround

    /**
     * Returns the entity offset from an enum so it is thread safe.
     * @return the actual offset as float.
     */
    public float getEntityOffset() { return entityType.getOffset(); }// end isEntityOnGround

    /**
     * This method includes a write lock so writing to it across threads won't cause any problems.
     */
    public void moveEntityAbsolute(Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        player.moveAbsolute(session, position, rotation, isOnGround, false);
    }// end moveAbsolute

    /**
     * Same thing as above for isClosed().
     * Even if setting a boolean is an atomic operation, I need a visibility guarantee through volatile.
     */
    public boolean isInvalid() {
        return session == null || session.isClosed();
    }//end isSessionInvalid

    /**
     * All following 3 methods are thread safe by default because they are mostly stateless,
     * except of "upstream.sendPacket" what itself is final and uses a thread safe queue in the end.
     * Just don't change that behaviour in the GeyserSession and we are all good!
     */
    public void playSound(Vector3f position, String sound, float volume, float pitch) {
        session.playSound(position, sound, volume, pitch);
    }// end playSound

    public void spawnParticle(Vector3f position, LevelEventType particle, int count, int data) {
        session.spawnParticle(position, particle, count, data);
    }// end spawnParticle

    public void showActionBar(String text) {
        session.showActionBar(text);
    }// end showActionBar

}// end class WorldBorderSessionWrapper
