/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.util.Observable;

import llnl.gnem.core.util.TimeT;


/**
 * The PickDataBridge class provides a means of signaling changes in pick data
 * to interested observers. Changes in pick position, window duration, and pick
 * std are all made accessible to observers through this class. Pick times
 * within a PickDataBridge are stored in two parts; a time and a reference
 * time. The time plus the reference time are assumed to be equal to the epoch
 * time of the pick. For plots in which the X-axis has units of epoch time, the
 * reference time is zero. For those plots in which the time has been reduced
 * by some reference time, the PickDataBridge class should be constructed with
 * the appropriate reference time.
 *
 * @author Doug Dodge
 */
public class PickDataBridge extends Observable {
    /**
     * Constructor for the PickDataBridge objectzero. assuming a reference time of
     * zero.
     *
     * @param time     The epoch time of the pick
     * @param deltim   The uncertainty of the time in seconds
     * @param duration The duration of any associated window in seconds
     */
    public PickDataBridge( double time, double deltim, double duration )
    {
        super();
        this.time = time;
        this.deltim = deltim;
        this.duration = duration;
        reftime = 0.0;
        dirty = false;
    }

    /**
     * Constructor for the PickDataBridge object assuming a non-zero reference
     * time.
     *
     * @param time     The epoch time of the pick
     * @param deltim   The uncertainty of the time in seconds
     * @param duration The duration of any associated window in seconds
     * @param reftime  The pick reference time in seconds.
     */
    public PickDataBridge( double time, double deltim, double duration, double reftime )
    {
        super();
        this.time = time - reftime;
        this.deltim = deltim;
        this.duration = duration;
        this.reftime = reftime;
        dirty = false;
    }


    /**
     * Sets the dirty attribute of the PickDataBridge object. When read in from the
     * database dirty is false until the window is adjusted. Newly-created picks have
     * dirty set to true.
     *
     * @param v The new dirty value
     */
    public void setDirty( boolean v )
    {
        dirty = v;
    }

    /**
     * Gets the dirty attribute of the PickDataBridge object. When read in from the
     * database dirty is false until the window is adjusted. Newly-created picks have
     * dirty set to true.
     *
     * @return The dirty value
     */
    public boolean getDirty()
    {
        return dirty;
    }


    /**
     * Converts a PickDataBridge from a zero reference time to a non-zero
     * reference time.
     *
     * @param reftime The new reference time to assign to this PickDataBridge
     *                object
     */
    public void makeTimeRelative( double reftime )
    {
        time = getTime() - reftime;
        this.reftime = reftime;
        endTime = time;
    }

    /**
     * Gets the time as an epoch time (time plus reference time).
     *
     * @return The epoch time value
     */
    public double getTime()
    {
        return time + reftime;
    }

    /**
     * Gets the time relative to the reference time. This is the time of the pick
     * as it is displayed in the plot.
     *
     * @return The relative Time value
     */
    public double getRelativeTime()
    {
        return time;
    }

    public void changeReferenceTime( double newRefTime )
    {
        double dt = newRefTime - reftime;
        time -= dt;
        endTime -= dt;
        reftime = newRefTime;
    }

    /**
     * Gets the reference Time of te PickDataBridge object
     *
     * @return The reference Time value
     */
    public double getReferenceTime()
    {
        return reftime;
    }

    /**
     * Gets the deltim ( time uncertainty ) of the PickDataBridge object
     *
     * @return The deltim value
     */
    public double getDeltim()
    {
        return deltim;
    }

    /**
     * Gets the duration in seconds of a window associated with this pick
     *
     * @return The duration value
     */
    public double getDuration()
    {
        return duration;
    }

    public String getInfoString()
    {
        double epochtime = time + reftime;
        StringBuffer sb = new StringBuffer( "Pick at: " );
        sb.append( new TimeT( epochtime ).toString() );
        sb.append( " with std = " + deltim );
        sb.append( " and window duration = " + duration );
        return sb.toString();
    }

    public String toString()
    {
        return getInfoString();
    }

    /**
     * Sets the time of the PickDataBridge object. This method is expected to be
     * called by a JmultiaxisPlot's mouse motion listener in response to dragging
     * of a pick or its associated window.
     *
     * @param time      The new time value
     * @param initiator A reference to the object that was being dragged.
     */
    protected void setTime( double time, Object initiator )
    {
        this.time = time;
        setChanged();
        notifyObservers( initiator );
    }

    /**
     * Sets the deltim of the PickDataBridge object. This method is expected to be
     * called by a JmultiaxisPlot's mouse motion listener in response to dragging
     * of a pick's error bars.
     *
     * @param deltim    The new deltim value
     * @param initiator A reference to the object that was being dragged.
     */
    protected void setDeltim( double deltim, Object initiator )
    {
        this.deltim = deltim;
        setChanged();
        notifyObservers( initiator );
    }

    /**
     * Sets the duration value of the PickDataBridge object. This method is
     * expected to be called by a JmultiaxisPlot's mouse motion listener in
     * response to dragging of a pick or a JWindowHandle.
     *
     * @param duration  The new duration value
     * @param initiator A reference to the object that was being dragged.
     */
    void setDuration( double duration, Object initiator )
    {
        this.duration = duration;
        endTime = time + duration;
        setChanged();
        notifyObservers( initiator );
    }

    public void setTimeNoNotify( double time )
    {
        this.time = time;
    }

    public void setDurationNoNotify( double duration )
    {
        this.duration = duration;
    }

    public void setDeltimNoNotify( double deltim )
    {
        this.deltim = deltim;
    }


    public void setEndTime( double v )
    {
        endTime = v;
    }


    protected double time;
    protected double reftime;
    protected double duration;
    protected double deltim;
    protected boolean dirty;
    protected double endTime;
}

