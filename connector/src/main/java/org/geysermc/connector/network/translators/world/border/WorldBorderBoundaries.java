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
import org.geysermc.connector.entity.Entity;
import lombok.Getter;
import lombok.Setter;
import java.util.TimerTask;

/**
 * Everything related to the boundaries including
 * checks/getter/setter, should be placed here,
 * since we reuse the same code in the public WorldBorder
 * class as in the Task (with internal use only).
 *
 * Well in this class all should be gettable/settable,
 * so global Lombok sh.. is fine.
 */
@Getter
@Setter
abstract class WorldBorderBoundaries extends TimerTask {

    /**
     * Boundaries of the actual world border.
     * (The will get updated on expanding or shrinking)
     */
    private double  minX    = 0.0D;
    private double  minZ    = 0.0D;
    private double  maxX    = 0.0D;
    private double  maxZ    = 0.0D;

    /**
     * The boundaries for the for the warning visuals.
     * (I rather cache them here than calculating them over and over again).
     */
    private double warningMaxX    = 0.0D;
    private double warningMaxZ    = 0.0D;
    private double warningMinX    = 0.0D;
    private double warningMinZ    = 0.0D;

    /**
     * Calculates how close the entity is to the edge of the world border.
     * (Not really in use anymore, from the old code, but I keep it jus in case).
     *
     * @param entityPosition to check for the distance to the closest world border.
     * @return double with the distance in blocks to the closest world border.
     */
    public double getDistanceToEdge(Vector3f entityPosition) {

        double minPosZ = entityPosition.getZ() - minZ;
        double maxPosZ = maxZ - entityPosition.getZ();
        double minPosX = entityPosition.getX() - minX;
        double maxPosX = maxX - entityPosition.getX();

        return Math.min(Math.min(Math.min(minPosX, maxPosX), minPosZ), maxPosZ);
    }// end getDistanceToEdge

    /**
     * Checks if an entity is inside the world border.
     * Is absolutely fine, rather to an update on shrinking or expanding to the instance members
     * than performing an calculation on each check.
     *
     * @param entityPosition to check.
     * @return true as long the entity is within the world limits.
     */
    public boolean isWithinBorderBoundaries(Vector3f entityPosition) {
        return entityPosition.getX() > minX && entityPosition.getX() < maxX && entityPosition.getZ() > minZ && entityPosition.getZ() < maxZ;
    }// end isInsideBorderBoundaries

    /**
     * Same as isWithinBorderBoundaries(Entity entity)) but using the warning boundaries.
     * Not do ge confused. It returns true if you are within the boundaries,
     * you should only warn if a player is outside of it, so on false.
     * May I find a better method name in the future.
     *
     * @param entityPosition to check.
     * @return true as long the entity is within the world limits and not in the warning zone at the edge to the border.
     */
    public boolean isWithinWarningBoundaries(Vector3f entityPosition) {
        return entityPosition.getX() > warningMinX && entityPosition.getX() < warningMaxX && entityPosition.getZ() > warningMinZ && entityPosition.getZ() < warningMaxZ;
    }// end isWithinWarnBoundaries

    /**
     * Get a save (within the Border) X position for the entity.
     *
     * @param entityPositionX just the entities x position.
     * @return a x position within the border boundaries.
     */
    public double getSavePositionX(double entityPositionX) {
        // sure, we could return a double but the Vector3f casts it to float anyways
        if(entityPositionX >= getMaxX()) return getMaxX()-1;
        if(entityPositionX <= getMinX()) return getMinX()+1;
        return entityPositionX;
    }// end of getSavePositionX

    /**
     * Get a save (within the Border) Z position for the entity.
     *
     * @param entityPositionZ just the entities x position.
     * @return a z position within the border boundaries.
     */
    public double getSavePositionZ(double entityPositionZ) {
        // sure, we could return a double but the Vector3f casts it to float anyways
        if(entityPositionZ >= getMaxZ()) return getMaxZ()-1;
        if(entityPositionZ <= getMinZ()) return getMinZ()+1;
        return entityPositionZ;
    }// end of getSavePositionZ

    /*
     * Ohh really, that is not super nice?
     * Well, live with it :D
     * I'm not going to do any Interface quirks
     * only because I wanna extend it to my task.
     */
    @Override
    public void run() {}

}// end WorldBorderCheckable
