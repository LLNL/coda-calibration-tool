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
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The TimeT class is a representation of epoch time (seconds before or after
 * January 1, 1970 00:00:00.0). Internally, the time is stored as a long
 * representing milliseconds (seconds * 1000) and an int representing
 * microseconds. There is no time zone manipulation within this class; all times
 * are considered to be GMT.
 *
 * @author Doug Dodge
 */
public strictfp class TimeT implements Comparable, Serializable {

    /**
     * Number of seconds in a minute
     */
    public static final int SECPERMIN = 60;
    /**
     * Number of seconds in a day
     */
    public static final int SECPERDAY = 86400;
    /**
     * Number of seconds in an hour
     */
    public static final int SECPERHOUR = 3600;

    public static final double AVG_DAYS_PER_YEAR = 365.2425;

    private static final String DEFAULT_FORMAT = "yyyy/MM/dd (DDD) HH:mm:ss.SSS";
    private static final String[] MONTH_STRING = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
    private static final String[] MONTH_ABBREVIATION = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
    private final long milliseconds;
    private final int microseconds;
    public static final double MAX_EPOCH_TIME = 9999999999.999;

    /**
     * Default Constructor for the TimeT object. Gets the system time and
     * converts it to GMT time. That time is used to initialize the TimeT
     * object.
     */
    public TimeT() {
        GregorianCalendar d = new GregorianCalendar();
        milliseconds = d.getTime().getTime();
        microseconds = 0;
    }

    /**
     * Constructor for the TimeT object that takes numerical values for year,
     * month, day, hour, minute, and second.
     *
     * @param year
     *            The year (including century, e.g. 1999)
     * @param month
     *            Month of the year January = 1, ... December = 12
     * @param day
     *            Day of the month 0 LT day LTEQ daysInMonth( year, month );
     * @param hour
     *            An integer value from 0 - 23. Note: If hour is outside those
     *            bounds, day, month, and year may be changed to force hour into
     *            its bounds.
     * @param minute
     *            An integer value from 0 - 59. As with hour, out of range
     *            values may force a cascading change of other fields.
     * @param second
     *            A double value from 0 - 59.999999
     */
    public TimeT(int year, int month, int day, int hour, int minute, double second) {
        // Get the fractional part of the seconds
        int tmp = (int) Math.rint((second - (int) second) * 1000000);
        microseconds = tmp % 1000;
        GregorianCalendar d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        d.set(year, month - 1, day, hour, minute, (int) second);
        d.set(Calendar.MILLISECOND, tmp / 1000);
        milliseconds = d.getTime().getTime();
    }

    /**
     * Constructor for the TimeT object that takes all int values for year,
     * month, day, hour, minute, second, and millisecond.
     *
     * @param year
     *            The year (including century, e.g. 1999)
     * @param month
     *            Month of the year January = 1, ... December = 12
     * @param day
     *            Day of the month 0 LT day LTEQ daysInMonth( year, month );
     * @param hour
     *            An int val ue from 0 - 23. Note: If hour is outside those
     *            bounds, day, month, and year may be changed to force hour into
     *            its bounds. @param minute An int value from 0 - 59. As with
     *            hour, out of range values may force a cascading change of
     *            other fields. @param sec An int value from 0 - 59 @param msec
     *            A n int value from 0 to 999
     */
    public TimeT(int year, int month, int day, int hour, int minute, int sec, int msec) {
        this(year, month, day, hour, minute, sec + msec / 1000.0);
    }

    /**
     * Constructor for the TimeT object that takes all int values for year, day
     * of year, hour, minute, second, and millisecond.
     *
     * @param year
     *            The year (including century, e.g. 1999)
     * @param jday
     *            day of the year ( 1 - 365 or 366 in leap years)
     * @param hour
     *            An int value from 0 - 23. Note: If hour is outside those
     *            bounds, day, month, and year may be changed to force hour into
     *            its bounds.
     * @param sec
     *            An int value from 0 - 59
     * @param msec
     *            An int value from 0 to 999
     * @param min
     *            Description of the Parameter
     */
    public TimeT(int year, int jday, int hour, int min, int sec, int msec) {
        GregorianCalendar d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        d.clear();
        d.set(Calendar.MILLISECOND, msec);
        d.set(Calendar.SECOND, sec);
        d.set(Calendar.MINUTE, min);
        d.set(Calendar.HOUR, hour);
        d.set(Calendar.DAY_OF_YEAR, jday);
        d.set(Calendar.YEAR, year);
        milliseconds = d.getTime().getTime();
        microseconds = 0;
    }

    /**
     * Constructor for the TimeT object that takes a double epoch time.
     *
     * @param epochtime
     *            A double value holding the number of seconds relative to
     *            1970/01/01 00:00:00.0
     */
    public TimeT(double epochtime) {
        milliseconds = toMilliseconds(epochtime);
        microseconds = ((int) Math.rint((epochtime - (long) epochtime) * 1000000.0)) % 1000;
    }

    /**
     * Constructor for a TimeT that takes a long holding the number of
     * milliseconds relative to 1970/01/01 00:00:00.0
     *
     * @param epochInMilliseconds
     *            the number of milliseconds relative to 1970/01/01 00:00:00.0
     */
    public TimeT(long epochInMilliseconds) {
        milliseconds = epochInMilliseconds;
        microseconds = 0;
    }

    /**
     * Constructor for the TimeT object that takes a String representation of
     * the time and a String specifying the format for the time string.
     *
     * @param timeString
     *            A String representation of the time, e.g. "1994/02/24
     *            06:17:12.234"
     * @param formatString
     *            A format String that specifies how to interpret the time
     *            String. The format String must follow the rules of
     *            SimpleDateFormat. You must not attempt to specify seconds to
     *            more than 1 millisecond of precision. Doing so, will result in
     *            an incorrect parse. For example a time String of "12.12345"
     *            with a format String of "ss.SSSSS" will result in seconds
     *            interpreted as 12.345.
     *            <p>
     *            </p>
     *            The following pattern letters are defined (all other
     *            characters from 'A' to 'Z' and from 'a' to 'z' are reserved):
     *            <p>
     *            </p>
     *            Letter Date or Time Component Presentation Examples G Era
     *            designator Text AD y Year Year 1996; 96 M Month in year Month
     *            July; Jul; 07 w Week in year Number 27 W Week in month Number
     *            2 D Day in year Number 189 d Day in month Number 10 F Day of
     *            week in month Number 2 E Day in week Text Tuesday; Tue a Am/pm
     *            marker Text PM H Hour in day (0-23) Number 0 k Hour in day
     *            (1-24) Number 24 K Hour in am/pm (0-11) Number 0 h Hour in
     *            am/pm (1-12) Number 12 m Minute in hour Number 30 s Second in
     *            minute Number 55 S Millisecond Number 978 z Time zone General
     *            time zone Pacific Standard Time; PST; GMT-08:00 Z Time zone
     *            RFC 822 time zone -0800
     *            <p>
     *            </p>
     *            Pattern letters are usually repeated, as their number
     *            determines the exact presentation: Text: For formatting, if
     *            the number of pattern letters is 4 or more, the full form is
     *            used; otherwise a short or abbreviated
     *
     * @throws ParseException
     *             This exception is thrown if the format String is incompatible
     *             with the time String or uses invalid format codes.
     * @see SimpleDateFormat
     */
    public TimeT(String timeString, String formatString) throws ParseException {
        this(getDateFrom(timeString, formatString));
    }

    public TimeT(String stringSpecifier) {
        this(new SimpleDateFormat("yyyy:DDD:HH:mm:ss.SSS Z").parse(stringSpecifier + " -0000", new ParsePosition(0)));
    }

    public TimeT(Date date) {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        GregorianCalendar d = new GregorianCalendar(tz);

        d.setTime(date);
        milliseconds = d.getTime().getTime() + d.get(Calendar.DST_OFFSET) + tz.getRawOffset();
        microseconds = 0;
    }

    /**
     * Copy constructor for the TimeT object
     *
     * @param other
     */
    public TimeT(TimeT other) {
        milliseconds = other.milliseconds;
        microseconds = other.microseconds;
    }

    /**
     * Produce a new TimeT object whose Epoch time is the sum of the Epoch times
     * in this object and the input object.
     *
     * @param other
     * @return A TimeT object whose Epoch time is the sum of the two input Epoch
     *         times.
     */
    public TimeT add(TimeT other) {
        return new TimeT(getEpochTime() + other.getEpochTime());
    }

    /**
     * Subtract the input TimeT from this TimeT and return the resulting epoch
     * time as a double.
     *
     * @param other
     * @return Resulting epoch time as a double
     */
    public TimeT subtract(TimeT other) {
        return new TimeT(getEpochTime() - other.getEpochTime());
    }

    /**
     * Subtract the input TimeT from this TimeT and return the resulting epoch
     * time as a double.
     *
     * @param other
     * @return Resulting epoch time as a double
     */
    public double subtractD(TimeT other) {
        return getEpochTime() - other.getEpochTime();
    }

    /**
     * Produce a new TimeT object whose Epoch time is the sum of the Epoch times
     * in this object and the input double (interpreted as an Epoch time).
     *
     * @param v
     *            Some number of seconds.
     * @return A TimeT object whose Epoch time is the sum of the two input Epoch
     *         times.
     */
    public TimeT add(double v) {
        return new TimeT(getEpochTime() + v);
    }

    /**
     * @param v
     * @return
     */
    public TimeT subtract(double v) {
        return add(-v);
    }

    /**
     * Return the Epoch time expressed in milliseconds relative to 1970/01/01
     * 00:00:00.0
     *
     * @return The milliseconds relative to 1970/01/01 00:00:00.0
     */
    public long epochAsLong() {
        return milliseconds + (long) Math.rint(microseconds / 1000.0);
    }

    /**
     * Gets the epochTime ( time in seconds relative to 1970/01/01 00:00:00.0
     * )of the TimeT object
     *
     * @return The epochTime value
     */

    public double getEpochTime() {
        return (milliseconds / 1000.0) + microseconds / 1000000.0;
    }

    /**
     * Gets the year attribute of the TimeT object
     *
     * @return An int value representing the year ( including the century )
     */
    public int getYear() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.YEAR);
    }

    /**
     * Gets the dayOfYear attribute of the TimeT object
     *
     * @return The dayOfYear value ( 1-365 or 1-366 in a leap year )
     */
    public int getDayOfYear() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * @return The day of the year
     */
    public int getJDay() {
        return getDayOfYear();
    }

    /**
     * Gets the jdate attribute of the TimeT object. Jdate is the year * 1000 +
     * the DayOfYear, e.g. for the 23rd day of the year 1995 the Jdate is
     * 1995023.
     *
     * @return The jdate value
     */
    public int getJdate() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.DAY_OF_YEAR) + d.get(Calendar.YEAR) * 1000;
    }

    /**
     * Gets the month attribute of the TimeT object
     *
     * @return The month value (1 - 12)
     */
    public int getMonth() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.MONTH) + 1;
    }

    /**
     * Gets the dayOfMonth attribute of the TimeT object
     *
     * @return The dayOfMonth value ( 1 - DaysInMonth( year, month )
     */
    public int getDayOfMonth() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Gets the hour attribute of the TimeT object
     *
     * @return The hour value ( 0 - 23 )
     */
    public int getHour() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Gets the minute attribute of the TimeT object
     *
     * @return The minute value ( 0 - 59 )
     */
    public int getMinute() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.MINUTE);
    }

    /**
     * Gets the second attribute of the TimeT object
     *
     * @return The second value ( 0 - 59.999999 )
     */
    public double getSecond() {
        GregorianCalendar d = getCalendar();
        return (double) (d.get(Calendar.SECOND) * 1000000 + d.get(Calendar.MILLISECOND) * 1000 + microseconds) / 1000000;
    }

    public int getCalendarSecond() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.SECOND);
    }

    public int getCalendarMilliSecond() {
        GregorianCalendar d = getCalendar();
        return d.get(Calendar.MILLISECOND);
    }

    /**
     * @return The integer seconds
     */
    public int getSec() {
        return (int) getSecond();
    }

    /**
     * @return Fractional seconds times 1000
     */
    public int getMsec() {
        return (int) (milliseconds % 1000L);
    }

    /**
     * returns the epochtime in milliseconds
     *
     * @return the epoch time in millliseconds
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    /**
     * Round the epoch time to the nearest whole second.
     *
     * @return
     */
    public TimeT roundToSec() {
        GregorianCalendar d = getCalendar();
        double ms = d.get(Calendar.MILLISECOND) / 1000.0 + microseconds / 1000000.0;
        if (ms >= 0.5) {
            d.set(Calendar.SECOND, d.get(Calendar.SECOND) + 1);
        }
        d.set(Calendar.MILLISECOND, 0);
        return new TimeT(d.getTime().getTime());
    }

    /**
     * Round the epoch time to the nearest whole minute.
     *
     * @return
     */
    public TimeT roundToMin() {
        GregorianCalendar d = getCalendar();
        int s = d.get(Calendar.SECOND);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        if (s >= 30) {
            d.set(Calendar.MINUTE, d.get(Calendar.MINUTE) + 1);
        }
        return new TimeT(d.getTime().getTime());
    }

    /**
     * Round the epoch time to the nearest whole hour.
     *
     * @return
     */
    public TimeT roundToHour() {
        GregorianCalendar d = getCalendar();
        int s = d.get(Calendar.MINUTE);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        if (s >= 30) {
            d.set(Calendar.HOUR_OF_DAY, d.get(Calendar.HOUR_OF_DAY) + 1);
        }
        return new TimeT(d.getTime().getTime());
    }

    /**
     * Round the epoch time to the nearest whole day.
     *
     * @return
     */
    public TimeT roundToDay() {
        GregorianCalendar d = getCalendar();
        int s = d.get(Calendar.HOUR_OF_DAY);
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        if (s >= 12) {
            d.set(Calendar.DAY_OF_MONTH, d.get(Calendar.DAY_OF_MONTH) + 1);
        }
        return new TimeT(d.getTime().getTime());
    }

    /**
     * Return true if this epoch time is less than the input epoch time
     *
     * @param T
     *            A TimeT object
     * @return boolean (true if this epoch time is less than the input epoch
     *         time, false otherwise)
     */
    public boolean lt(TimeT T) {
        return milliseconds + microseconds / 1000.0 < T.milliseconds + T.microseconds / 1000.0;
    }

    /**
     * Return true if this epoch time is greater than the input epoch time
     *
     * @param T
     *            A TimeT object
     * @return boolean (true if this epoch time is greater than the input epoch
     *         time, false otherwise)
     */
    public boolean gt(TimeT T) {
        return milliseconds + microseconds / 1000.0 > T.milliseconds + T.microseconds / 1000.0;
    }

    /**
     * Return true if this epoch time is less than or equal to the input epoch
     * time
     *
     * @param T
     *            A TimeT object
     * @return boolean (true if this epoch time is less than or equal to the
     *         input epoch time, false otherwise)
     */
    public boolean le(TimeT T) {
        return !(this.gt(T));
    }

    /**
     * Return true if this epoch time is greater than or equal to the input
     * epoch time
     *
     * @param T
     *            A TimeT object
     * @return boolean (true if this epoch time is greater than or equal to the
     *         input epoch time, false otherwise)
     */
    public boolean ge(TimeT T) {
        return !(this.lt(T));
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

    public Date getDate() {
        TimeZone tz = TimeZone.getTimeZone("Etc/UTC");
        GregorianCalendar d = new GregorianCalendar(tz);
        long tmp = milliseconds;
        tmp -= d.get(Calendar.DST_OFFSET);
        tmp -= tz.getRawOffset();
        return new Date(tmp);
    }

    private GregorianCalendar getCalendar() {
        GregorianCalendar d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        Date date = new Date(milliseconds);
        d.setTime(date);
        return d;
    }

    /**
     * Return a String representation of the time based on the supplied string
     * template
     *
     * @param format
     *            A SimpleDateFormat format String. Note: currently TimeString
     *            cannot display seconds to a precision better than 1
     *            millisecond, so do not attempt to use more than 3 'S'
     *            specifiers. Using more than 3 will cause the fractional
     *            seconds value to be zero-padded on the left. For example, if
     *            the seconds value is 23.1234 and you use a format specifier
     *            that includes 'ss.SSSS', the corresponding part of the output
     *            string will be '23.0123'.
     * @return A String representation of the time.
     * @see SimpleDateFormat for format rules.
     */
    public String toString(String format) {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        GregorianCalendar d = new GregorianCalendar(tz);
        Date date = new Date(milliseconds + (int) Math.rint(microseconds / 1000.0));
        d.setTime(date);
        SimpleDateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(tz);
        return df.format(d.getTime());
    }

    /**
     * Return a String representation of the time based on the default Time
     * format String.
     *
     * @return A String representation of the time.
     */
    @Override
    public String toString() {
        return toString(DEFAULT_FORMAT);
    }

    /**
     * Return true if the input Object is a TimeT object and if its millisecond
     * and microsecond fields match those from this Object. Note that equals
     * does not compare the format field, so two TimeT objects can compare true
     * even when their default formats differ.
     *
     * @param o
     *            Input Object putatively a TimeT
     * @return true if o is a TimeT with matching fields.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TimeT) {
            // No need to check for null because instanceof handles that check
            TimeT tmp = (TimeT) o;
            return tmp.milliseconds == milliseconds && tmp.microseconds == microseconds;
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code based on the microseconds and milliseconds fields of
     * the object.
     *
     * @return An int hash code value.
     */
    @Override
    public int hashCode() {
        return (int) (microseconds ^ milliseconds);
    }

    @Override
    public int compareTo(Object o) {
        TimeT other = (TimeT) (o);
        if (this.lt(other)) {
            return -1;
        } else if (this.gt(other)) {
            return 1;
        } else {
            return 0;
        }
    }

    public static long toMilliseconds(double seconds) {
        return (long) (seconds * 1000);
    }

    public static Date getDateFrom(String timeString, String formatString) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(formatString);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.parse(timeString);
    }

    public static TimeT getTimeFromDateString(String dateStr) {
        if (dateStr.length() == 11) { // Might be of form dd-MON-yyyy
            String dayStr = dateStr.substring(0, 2);
            String monStr = dateStr.substring(3, 6);
            String yearStr = dateStr.substring(7);
            String[] mon = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
            for (int j = 0; j < mon.length; ++j) {
                if (monStr.toUpperCase(Locale.ENGLISH).equals(mon[j])) {
                    try {
                        int day = Integer.parseInt(dayStr);
                        int year = Integer.parseInt(yearStr);
                        return new TimeT(year, j + 1, day, 1, 1, 0.0);
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            }
        } else if (dateStr.length() == 10) {
            String yearStr = dateStr.substring(0, 4);
            String monStr = dateStr.substring(5, 7);
            String dayStr = dateStr.substring(8);
            try {
                int year = Integer.parseInt(yearStr);
                int month = Integer.parseInt(monStr);
                int day = Integer.parseInt(dayStr);
                return new TimeT(year, month, day, 1, 1, 0.0);
            } catch (NumberFormatException e) {
                return new TimeT();
            }
        }

        return new TimeT();
    }

    public static int getMonthFromString(String month) {
        String tmp = month.toUpperCase(Locale.ENGLISH);
        for (int j = 0; j < 12; ++j) {
            if (MONTH_STRING[j].toUpperCase(Locale.ENGLISH).equals(tmp) || MONTH_ABBREVIATION[j].toUpperCase(Locale.ENGLISH).equals(tmp)) {
                return j + 1;
            }
        }
        throw new IllegalStateException("Unexpected month string: " + month);
    }

    /**
     * Gets the MaxDaysOfMonth attribute of the TimeT object
     *
     * @param year
     *            The year containing the month to process.
     * @param month
     *            The month for which days is to be obtained.
     * @return The MaxDaysOfMonth value ( )
     */
    public static int getMaxDaysOfMonth(int year, int month) {
        GregorianCalendar d = new GregorianCalendar(year, month, 1);
        return d.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Determines if the given year is a leap year.
     *
     * @param year
     *            An integer value including the century, e.g. 1999
     * @return True if the year is a leap year.
     */
    public static boolean isLeapYear(int year) {
        GregorianCalendar d = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        return d.isLeapYear(year);
    }

    public static String EpochToString(final double time) {
        TimeT tmp = new TimeT(time);
        return tmp.toString();
    }

    public static String EpochToString2(final double time, final String format) {
        TimeT tmp = new TimeT(time);
        return tmp.toString(format);
    }

    public static int EpochToJdate(final double time) {
        TimeT tmp = new TimeT(time);
        return tmp.getJdate();
    }

    public static double jdateToEpoch(final int jdate) {
        TimeT tmp = jdateToTimeT(jdate);
        return tmp.getEpochTime();
    }

    public static TimeT jdateToTimeT(final int jdate) {
        int year = jdate / 1000;
        int day = jdate % 1000;
        return new TimeT(year, day, 0, 0, 0, 0);
    }

    public double getFractionalYear() {
        int year = this.getYear();
        int doy = this.getDayOfYear();
        int hour = this.getHour();
        double second = this.getSecond();
        double daysInYear = isLeapYear() ? 366 : 365;
        return year + doy / daysInYear + hour / (daysInYear * 24) + second / (daysInYear * SECPERDAY);
    }

    public boolean isLeapYear() {
        int year = getYear();
        return ((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0));
    }

    public int getWeek() {
        return getCalendar().get(Calendar.WEEK_OF_YEAR);
    }
}
