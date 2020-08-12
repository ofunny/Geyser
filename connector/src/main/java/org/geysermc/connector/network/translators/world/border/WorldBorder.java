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

import com.nukkitx.math.vector.Vector2f;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Meeeh, general getters and setters for everything is bad!
 * Bad Lombok, no Lombok, no Lombok, this is my pot pie!!!111
 * https://www.youtube.com/watch?v=yugoFc79uh8
 */
//@Getter
//@Setter
//@RequiredArgsConstructor
public class WorldBorder extends WorldBorderBoundaries {

    @Getter @Setter private @NonNull Vector2f center;
    @Getter @Setter private @NonNull double oldDiameter;
    @Getter @Setter private @NonNull double newDiameter;
    @Getter @Setter private @NonNull long speed;
    @Getter @Setter private @NonNull int warningTime;
    @Getter @Setter private @NonNull int warningBlocks;

    // Runs the onTick method
    private ScheduledFuture<?> worldBorderTask;

    /**
     * Default constructor.
     * (Don't like to much Lombok).
     *
     * @param center Vector2f of the current world border.
     * @param oldDiameter double for the diameter in blocks of the world border before it got changed or similar to newDiameter if not changed.
     * @param newDiameter double for the diameter in blocks of the new world border.
     * @param speed long for the speed to apply an expansion/shrinking of the world border. When a client joins he gets the actual border oldDiameter and the time left to reach the newDiameter.
     * @param warningTime int for the time in seconds before a shrinking world border would hit a not moving player. Creates the same visual warning effect as warningBlocks.
     * @param warningBlocks int in blocks before you reach the border to show warning particles. (In the Java client the players vision would get a red color tone).
     */
    public WorldBorder(@NonNull Vector2f center,
                       @NonNull double oldDiameter,
                       @NonNull double newDiameter,
                       @NonNull long speed,
                       @NonNull int warningTime,
                       @NonNull int warningBlocks) {

        this.center = center;
        this.oldDiameter = oldDiameter;
        this.newDiameter = newDiameter;
        this.speed = speed;
        this.warningTime = warningTime;
        this.warningBlocks = warningBlocks;

    }//end WorldBorder

    /**
     * Changes to the world border can be done on construction or via setters.
     * But no change will take effect before you gonna call this update method.
     *
     * This method should be called (one time) after a block of changes got applied to the world border.
     * It will than safely stop the clients world border task, pass the updates,
     * and start it again. Do not change anything directly in the world border task!
     * Otherwise you lose thread safety and that's not cool and also the reason
     * why the tasks member is private without getter or setter!
     *
     * @param session well the GeyserSession of the client please.
     */
    public void update(GeyserSession session) {

        /*
         * Setting the correct boundary of our world borders square.
         */
        double radius = newDiameter / 2.0D;
        setMinX(center.getX()-radius);
        setMinZ(center.getY()-radius);// Mapping 2D vector to 3D coordinates >> Y becomes Z
        setMaxX(center.getX()+radius);
        setMaxZ(center.getY()+radius);// Mapping 2D vector to 3D coordinates >> Y becomes Z

        /*
         * Caching the warning boundaries.
         * (Using this to make it any dev clear that it is a instance member).
         */
        setWarningMinX(getMinX()+this.warningBlocks);
        setWarningMinZ(getMinZ()+this.warningBlocks);
        setWarningMaxX(getMaxX()-this.warningBlocks);
        setWarningMaxZ(getMaxZ()-this.warningBlocks);

        /*
         * WorldBorderTask gets canceled and started again.
         * Is from the old code, don't like it, will change it later.
         */
        cancelTask();

        /*
         * The world border task is checking the players position and also performs the border highlighting,
         * push back, shrinking and expanding of the border.
         *
         * At the moment I run it each 200 milliseconds,
         * previously it ran each 50ms, but I really see no reason to run it that often. The only downside
         * with an higher period would be the distance a player could move behind the border until he receives
         * a push back, also the border highlight and shrinking/expanding depends on it, so do not set it too high.
         *
         * Whatever gets passed to the Task has either to be thread save (like a primitive, clone) and so on,
         * or needs special care in the tasks code than!
         */
        if (!(newDiameter >= 59999967)) {
            worldBorderTask = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(
                    new WorldBorderTask(
                                        session,
                                        getMinX(),
                                        getMinZ(),
                                        getMaxX(),
                                        getMaxZ(),
                                        getWarningMinX(),
                                        getWarningMinZ(),
                                        getWarningMaxX(),
                                        getWarningMaxZ()
                    ), 1, 200, TimeUnit.MILLISECONDS
            );
        }// if world border bigger as max world size
    }// end update

    /**
     * Cancels the task above (if needed) for any reason but lets the current run end normally.
     * Please leave the worldBorderTask private!
     */
    public void cancelTask() {
        cancelTask(false);
    }// end cancelTask

    /**
     * Cancels the task above if needed for any reason.
     * Please leave the worldBorderTask private!
     *
     * @param mayInterruptItRunning true will kill the task somewhere while running, may try not to use it!
     */
    public void cancelTask(boolean mayInterruptItRunning) {
        if(worldBorderTask != null) worldBorderTask.cancel(mayInterruptItRunning);
    }// end cancelTask

}// end class WorldBorder
