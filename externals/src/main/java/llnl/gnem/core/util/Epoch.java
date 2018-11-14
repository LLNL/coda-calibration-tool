/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.util;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * The Epoch class encapsulates the concept of a time interval such as an
 * operational epoch of a site. Epochs can be compared for equality,
 * intersection, and sub/super set relation. Epoch bounds and extents can be
 * accessed and modified.
 *
 * @author Doug Dodge
 */
public class Epoch implements Serializable, Comparable {

    public static final TimeT MAX_POSSIBLE_TIME = new TimeT(9999999999.999);
    public static final TimeT MIN_POSSIBLE_TIME = new TimeT(-9999999999.999);
    private TimeT end;
    private TimeT start;
    private TimeResolution timeResolution;

    public static Epoch getBoundingEpoch(Epoch epoch1, Epoch epoch2) {
        double t1 = epoch1.getStart();
        if (epoch2.getStart() < t1) {
            t1 = epoch2.getStart();
        }
        double t2 = epoch1.getEnd();
        if (epoch2.getEnd() > t2) {
            t2 = epoch2.getEnd();
        }
        return new Epoch(t1, t2);
    }

    /**
     * Constructor for the Epoch object
     */
    public Epoch() {
        start = new TimeT(0.0);
        end = new TimeT(0.0);
        timeResolution = TimeResolution.ABS;
    }

    /**
     * Constructor for the Epoch object
     *
     * @param t1
     * @param t2
     *            The end of the Epoch Note: if T2 is less than T1 their order
     *            will be reversed in the constructor so that the epoch duration
     *            is GTEQ 0.
     */
    public Epoch(TimeT t1, TimeT t2) {
        setUpEpoch(t1, t2);
    }

    /**
     * Constructor for the Epoch object
     *
     * @param t1
     *            The start of the Epoch
     * @param t2
     *            The end of the Epoch Note: if T2 is less than T1 their order
     *            will be reversed in the constructor so that the epoch duration
     *            is GTEQ 0.
     */
    public Epoch(double t1, double t2) {
        setUpEpoch(new TimeT(t1), new TimeT(t2));
    }

    /**
     * Copy constructor for the Epoch object
     *
     * @param that
     *            input Epoch to be copied into new object
     */
    public Epoch(Epoch that) {
        start = new TimeT(that.start);
        end = new TimeT(that.end);
        timeResolution = that.timeResolution;
    }

    /**
     * Determine whether an instant in time falls within this Epoch
     *
     * @param aTime
     *            The instant in time to be checked (a TimeT)
     * @return true if the instant is within this Epoch, false otherwise.
     */
    public boolean ContainsTime(TimeT aTime) {
        return (!isEmpty() && aTime.ge(start) && aTime.le(end));
    }

    /**
     * Round the Epoch boundaries to the time resolution specified in the input
     * argument. If timeResolution is not one of the values in
     * Epoch.TimeResolution, then no rounding will occur. Note: Rounding may
     * cause the Epoch to become empty if both Start and End round to the same
     * values.
     *
     * @param timeResolution
     *            An input value that should be one of the values from
     *            Epoch.TimeResolution.
     */
    public void RoundEpochBoundaries(TimeResolution timeResolution) {
        switch (timeResolution) {
        case DAY: {
            start = start.roundToDay();
            end = end.roundToDay();
            break;
        }
        case HOUR:
            start = start.roundToHour();
            end = end.roundToHour();
            break;
        case MIN:
            start = start.roundToMin();
            end = end.roundToMin();
            break;
        case SEC:
            start = start.roundToSec();
            end = end.roundToSec();
            break;
        default:
            throw new IllegalStateException("Encountered unxpected time resolution - " + timeResolution + " - trying to round epoch boundaries. This should never happen.");
        }
    }

    @Override
    public int compareTo(Object aThat) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        //this optimization is usually worthwhile, and can
        //always be added
        if (this == aThat) {
            return EQUAL;
        }

        Epoch other = (Epoch) aThat;

        //primitive numbers follow this form
        if (this.start.lt(other.start)) {
            return BEFORE;
        }
        if (this.start.gt(other.start)) {
            return AFTER;
        }
        return EQUAL;
    }

    /**
     * Determines the duration in seconds of this Epoch.
     *
     * @return Duration of the Epoch in seconds
     */
    public double duration() {
        return getLengthInSeconds();
    }

    /**
     * Return true if the input Object is an Epoch object and if its Start and
     * End fields match those from this Object. Note that equals does not
     * compare the timeResolution field, so two Epoch objects can compare true
     * even when their timeResolutions differ.
     *
     * @param o
     *            Input Object putatively a Epoch
     * @return true if o is a Epoch with matching fields.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Epoch) {
            // No need to check for null because instanceof handles that check
            Epoch tmp = (Epoch) o;
            return tmp.start.equals(start) && tmp.end.equals(end);
        } else {
            return false;
        }
    }

    /**
     * Gets the end of the Epoch as a TimeT object
     *
     * @return The end value as a TimeT object
     */
    public double getEnd() {
        return end.getEpochTime();
    }

    /**
     * Gets the endtime attribute of the Epoch object
     *
     * @return The endtime value
     */
    public TimeT getEndtime() {
        return new TimeT(end);
    }

    /**
     * Sets the end time of this Epoch object
     *
     * @param end
     *            The new (TimeT) end value
     */
    public void setEndtime(TimeT end) {
        this.end = end;
    }

    /**
     * Return an Epoch that is the intersection of this Epoch with the input
     * Epoch
     *
     * @param other
     *            The input Epoch to be intersected with this Epoch
     * @return The result Epoch which is the intersection result.. The Epoch
     *         will be empty if the intersection is empty.
     */
    public Epoch getIntersection(Epoch other) {
        Epoch tmp = new Epoch();
        if (!intersects(other)) {
            return tmp;
        }
        if (start.ge(other.start)) {
            tmp.start = start;
        } else {
            tmp.start = other.start;
        }
        if (end.le(other.end)) {
            tmp.end = end;
        } else {
            tmp.end = other.end;
        }
        return tmp;
    }

    public Epoch union(Epoch other) {
        Epoch tmp = new Epoch();
        if (!intersects(other)) {
            return tmp;
        }
        if (start.ge(other.start)) {
            tmp.start = other.start;
        } else {
            tmp.start = start;
        }

        if (end.le(other.end)) {
            tmp.end = other.end;
        } else {
            tmp.end = end;
        }
        return tmp;
    }

    public double projection(Epoch other) {

        if (isEmpty() && other.isEmpty()) {
            return -1;
        }
        Epoch inter = getIntersection(other);
        if (inter.duration() <= 0) {
            return 0;
        }
        Epoch union = union(other);
        return inter.duration() / union.duration();
    }

    /**
     * Return the length of this Epoch in days
     *
     * @return A double value which is the length of this Epoch in days (
     *         including fractional parts of a day).
     */
    public double getLengthInDays() {
        return getLengthInSeconds() / TimeT.SECPERDAY;
    }

    /**
     * Return the length of this Epoch in seconds
     *
     * @return A double value which is the length of the Epoch in seconds
     */
    public double getLengthInSeconds() {
        return end.getEpochTime() - start.getEpochTime();
    }

    /**
     * Gets the offJdate attribute of the Epoch object
     *
     * @return The offJdate value
     */
    public int getOffJdate() {
        return end.getJdate();
    }

    /**
     * Gets the offdate attribute of the Epoch object
     *
     * @return The offdate value
     */
    public TimeT getOffdate() {
        return new TimeT(end);
    }

    /**
     * Gets the onJdate attribute of the Epoch object
     *
     * @return The onJdate value
     */
    public int getOnJdate() {
        return start.getJdate();
    }

    /**
     * Gets the ondate attribute of the Epoch object
     *
     * @return The ondate value
     */
    public TimeT getOndate() {
        return new TimeT(start);
    }

    public double getStart() {
        return start.getEpochTime();
    }

    /**
     * Gets the time attribute of the Epoch object
     *
     * @return The time value
     */
    public TimeT getTime() {
        return new TimeT(start);
    }

    /**
     * Sets the start time of this Epoch object
     *
     * @param start
     *            The new (TimeT) start value
     */
    public void setTime(TimeT start) {
        this.start = start;
    }

    /**
     * Sets the timeResolution attribute of the Epoch object
     *
     * @param resolution
     *            The new timeResolution value
     */
    public void setTimeResolution(TimeResolution resolution) {

        timeResolution = resolution;
    }

    /**
     * Gets the start of the Epoch as a TimeT object
     *
     * @return The start value as a TimeT object
     */
    public TimeT getbeginning() {
        return new TimeT(start);
    }

    /**
     * Returns a hash code based on the Start and End fields of the object.
     *
     * @return An int hash code value.
     */
    @Override
    public int hashCode() {
        return start.hashCode() ^ end.hashCode();
    }

    /**
     * @param e
     *            Description of the Parameter
     * @return Description of the Return Value
     */
    public Epoch intersection(Epoch e) {
        return getIntersection(e);
    }

    /**
     * Determine whether the input epoch intersects this Epoch
     *
     * @param e
     *            Input Epoch to be tested
     * @return true if the input Epoch intersects this Epoch, false otherwise
     */
    public boolean intersects(Epoch e) {
        if (e.start.le(end) && start.le(e.end)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return true if the Epoch is empty ( start GTEQ end )
     *
     * @return true if the Epoch is empty, false otherwise.
     */
    public boolean isEmpty() {
        return start.ge(end);
    }

    /**
     * Return true if this Epoch is a subset of the input Epoch
     *
     * @param E
     *            The input Epoch
     * @return true if this Epoch is a subset of the input Epoch, false
     *         otherwise.
     */
    public boolean isSubset(Epoch E) {
        return (E.start.le(start) && E.end.ge(end));
    }

    /**
     * Return true if this Epoch is a superset of the input Epoch
     *
     * @param E
     *            The input Epoch
     * @return true if this Epoch is a superset of the input Epoch, false
     *         otherwise.
     */
    public boolean isSuperset(Epoch E) {
        return (start.le(E.start) && end.ge(E.end));
    }

    /**
     * Determines the length in seconds of the overlap between the input Epoch
     * and this Epoch.
     *
     * @param I
     *            The input Epoch to be compared with this Epoch
     * @return The overlap length in seconds. If there is no overlap, the return
     *         value is 0.0.
     */
    public double overlapAmount(Epoch I) {
        Epoch tmp = getIntersection(I);
        return tmp.getLengthInSeconds();
    }

    /**
     * Use the object's toString method to print into a PrintStream
     *
     * @param ps
     *            The input PrintStream
     */
    public void print(PrintStream ps) {
        ps.print(toString());
    }

    /**
     * Return a string describing the state of the object
     *
     * @return A String showing the Epoch start and end times, formatted
     *         according to the time resolution.
     */
    @Override
    public String toString() {
        String on = TimeString(start, timeResolution);
        String off = TimeString(end, timeResolution);
        return "Start = " + on + ", End = " + off;
    }

    private String TimeString(TimeT T, TimeResolution resolution) {
        switch (resolution) {
        case DAY:
            return T.toString("yyyy/DDD");
        case HOUR:
            return T.toString("yyyy/DDD-HH");
        case MIN:
            return T.toString("yyyy/DDD-HH:mm");
        case SEC:
            return T.toString("yyyy/DDD-HH:mm:ss");
        default:
            return T.toString("yyyy/DDD-HH:mm:ss.SSS");
        }
    }

    private void setUpEpoch(TimeT T1, TimeT T2) {
        start = new TimeT(T1);
        end = new TimeT(T2);
        if (end.lt(start)) {
            TimeT tmp = end;
            end = start;
            start = tmp;
        }
        timeResolution = TimeResolution.ABS;
    }

    /**
     * For some actions on epochs it is useful to use a time resolution
     * appropriate for the type of epoch. For example, a site epoch may have a
     * resolution of a day, while a sensor epoch may have resolution of a
     * second. When adjusting epoch boundaries, the time resolution can be used
     * to make the epoch start and end at an appropriate instant, e.g. midnight
     * for an epoch with a resolution of a day. TimeResolution in the Epoch
     * class is specified with the enum class TimeResolution which has the
     * values: ABS, SEC, MIN, HOUR, DAY };
     *
     * @author Doug Dodge
     */
    public enum TimeResolution {

        ABS, SEC, MIN, HOUR, DAY
    }
}
