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
 *
 */

package org.geysermc.connector.network.translators.world.border;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

public class WorldBorderTask extends WorldBorderBoundaries {

    /*
     * Remember, all members and their values have to be thread-local since this task
     * was designed to be thread safe, so please consider that for any changes you may apply!
     */


    /*
     * The session is not synced, so we use a wrapper to allow only things we consider to be thread safe.
     */
    private final   WorldBorderSessionWrapper session;

    /*
     * Basic settings for the push back effect.
     * (I just put them up here so you find them easily).
     */
    private final   String          pushBackSoundKey = "mob.ghast.fireball";
    private final   LevelEventType  pushBackParticle = LevelEventType.PARTICLE_EXPLODE;


    /*
     * Basic settings for the world border highlight effect.
     */
    private final   LevelEventType  worldBorderParticle = LevelEventType.PARTICLE_RISING_RED_DUST;
    private final   int             worldBorderParticleCount = 11;

    /*
     * Time in milliseconds between each world border highlighting.
     * (Can only be a multiple of the task interval, setting it not as an exact multiple
     * won't do any harm but just in case you use a stop clock while testing that PR) :)
     */
    private final   long            worldBorderParticleTimeout = 500;

    /*
     * Since run will be in the thread pool, nothing should access our vars here from outside.
     */
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private long    worldBorderLastHighlightTS = 0;


    /**
     * WorldBorderTask constructor
     */
    public WorldBorderTask(
            GeyserSession session,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            double warningMinX,
            double warningMinZ,
            double warningMaxX,
            double warningMaxZ
    ) {
        this.session = new WorldBorderSessionWrapper(session);
        setMinX(minX);
        setMinZ(minZ);
        setMaxX(maxX);
        setMaxZ(maxZ);
        setWarningMinX(warningMinX);
        setWarningMinZ(warningMinZ);
        setWarningMaxX(warningMaxX);
        setWarningMaxZ(warningMaxZ);

    }// end WorldBorderTask


    /**
     * Runs the task, I mean that should be pretty clear.
     */
    @Override
    public void run() {

        /*
         * Just a check if the session is still open and not null,
         * otherwise the thread has to abort.
         */
        if (session.isInvalid()) {
            this.cancel();
            return;
        }// end if session canceled

        /*
         * Initialisation
         * If you use getEntityPosition() more than once, always create a local,
         * since it returns a clone each time you call it!
         */
        Vector3f entityPosition = session.getEntityPosition();


        /*
          As soon as the player is outside of the border we will push him/her back.
         */
        pushBackPlayer(entityPosition);

        /*
          Show border wall with particles as soon as the player comes near
         */
        showBorder(entityPosition);

    }// run the task bro

    /**
     * Pushes the player back to respect the world border.
     */
    public void pushBackPlayer(Vector3f entityPosition) {

        /*
         * If the player is within the world border limits,
         * there is nothing to do.
         */
        if(isWithinBorderBoundaries(entityPosition)) return;

        /*
         * Get the amount of the needed push back.
         */
        Vector3f newEntityPosition = Vector3f.from(
                getSavePositionX(entityPosition.getX()),
                (double) entityPosition.getY() - session.getEntityOffset(),
                getSavePositionZ(entityPosition.getZ()));

        /*
         * Perform the push back.
         */
        session.moveEntityAbsolute(newEntityPosition, session.getEntityRotation(), session.isEntityOnGround(), false);

        /*
         * Play sound and effect.
         */
        session.playSound(entityPosition, pushBackSoundKey, 0.1F, 2.0F);
        session.spawnParticle(entityPosition, pushBackParticle, 10, 0);

        /*
         * Showing message.
         * FIXME: has to be translatable, will be implemented soon!
         */
        session.showActionBar(ChatColor.BOLD + "" + ChatColor.RED + "You have reached the world border!");

    }// end pushBackPlayer

    /**
     * Show border wall with particles as soon as the player comes near
     */
    public void showBorder(Vector3f entityPosition) {

        /*
         * Only go on, if the player is outside the warning distance!
         */
        if(isWithinWarningBoundaries(entityPosition)) return;

        /*
         * Local initialisation.
         */
        long currentTimestamp = Instant.now().toEpochMilli();

        /*
         * Only spawn particles x Milliseconds.
         */
        if((getWorldBorderLastHighlightTS() + worldBorderParticleTimeout) > currentTimestamp) return;
        setWorldBorderLastHighlightTS(currentTimestamp);

        /*
         * Check for wall distance on x
         */
        float particlePosX = entityPosition.getX() - worldBorderParticleCount;
        float particlePosY = entityPosition.getY() - session.getEntityOffset();
        float particlePosZ = entityPosition.getZ() - worldBorderParticleCount;

        if(entityPosition.getX() > getWarningMaxX()) drawWall(Vector3f.from(getMaxX(), particlePosY, particlePosZ), true);
        if(entityPosition.getX() < getWarningMinX()) drawWall(Vector3f.from(getMinX(), particlePosY, particlePosZ), true);
        if(entityPosition.getZ() > getWarningMaxZ()) drawWall(Vector3f.from(particlePosX, particlePosY, getMaxZ()), false);
        if(entityPosition.getZ() < getWarningMinZ()) drawWall(Vector3f.from(particlePosX, particlePosY, getMinZ()), false);

    }// end showBorder

    /**
     * Draws the walls particle
     * (FIXME: Alternate will get an thread local random)
     */
    private void drawWall(Vector3f position, boolean drawWallX) {
        float alternate = 2;
        for (int i = 0; i < worldBorderParticleCount; i++) {
            session.spawnParticle(position, worldBorderParticle, 1, 0);
            if(drawWallX) {
                // X wall highlight
                position = position.add(0, alternate, 2);
            } else {
                // Z wall highlight
                position = position.add(2, alternate, 0);
            }// end if x or Z wall
            alternate = -alternate;
        }// end for y
    }// end drawWallParticle

}// end WorldBorderTask