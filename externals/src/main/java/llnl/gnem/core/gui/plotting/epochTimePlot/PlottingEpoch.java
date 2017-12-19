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
package llnl.gnem.core.gui.plotting.epochTimePlot;

import static llnl.gnem.core.gui.plotting.jmultiaxisplot.PlotAxis.defineAxis;

import java.util.ArrayList;

import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.TickLabel;
import llnl.gnem.core.gui.plotting.TickMetrics;
import llnl.gnem.core.util.Epoch;
import llnl.gnem.core.util.TimeT;

/**
 *
 * @author dodge
 */
public class PlottingEpoch extends Epoch {

    public PlottingEpoch(double v1, double v2) {
        super(v1, v2);
    }

    public double getDurationInYears() {
        return this.getEndtime().getFractionalYear() - this.getTime().getFractionalYear();
    }

    public double getDurationInMonths() {
        return getDurationInYears() * 12;
    }

    public ArrayList<TickValue> getTicks(int numMinorTicks) {
        double years = getDurationInYears();
        if (years > 2) {
            return getYearTicks();
        } else {
            double months = years * 12;
            if (months > 2) {
                return getMonthTicks();
            } else {
                double weeks = years * 52;
                if (weeks > 2) {
                    return getWeekTicks();
                } else {
                    double days = years * TimeT.AVG_DAYS_PER_YEAR;
                    if (days > 2) {
                        return getDayTicks();
                    } else {
                        double hours = days * 24;
                        if (hours > 2) {
                            return getHourTicks();
                        } else {
                            return getSecondsTicks(numMinorTicks);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<TickValue> getYearTicks() {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        int startYear = getTime().getYear();
        int endYear = getEndtime().getYear();
        for (int year = startYear; year <= endYear; ++year) {
            TimeT aTime = new TimeT(year, 1, 0, 0, 0, 0);
            double offset = aTime.getEpochTime() - getTime().getEpochTime();
            String label = String.format("%4d", year);
            result.add(new TickValue(offset, new TickLabel(label), true, HorizAlignment.CENTER, TickTimeType.YEARS));
        }
        return result;
    }

    private ArrayList<TickValue> getMonthTicks() {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        int year = getTime().getYear();
        int month = getTime().getMonth();
        TimeT aTime = new TimeT(year, month, 1, 0, 0, 0.0);
        while (aTime.le(getEndtime())) {
            double offset = aTime.getEpochTime() - getTime().getEpochTime();
            year = aTime.getYear();
            month = aTime.getMonth();
            String yearLabel = null;
            String monthLabel = String.format("%02d", month);
            if (month == 1) {
                yearLabel = String.format("(%04d)", year);
            }
            month++;
            if (month > 12) {
                year++;
                month = 1;
            }
            aTime = new TimeT(year, month, 1, 0, 0, 0.0);
            result.add(new TickValue(offset, new TickLabel(monthLabel, yearLabel), true, HorizAlignment.CENTER, TickTimeType.MONTHS));
        }
        return result;
    }

    private ArrayList<TickValue> getWeekTicks() {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        int year = getTime().getYear();
        int doy = getTime().getDayOfYear();
        int weekNum = doy / 7;
        TimeT aTime = new TimeT(year, doy, 0, 0, 0, 0);
        while (aTime.le(getEndtime())) {
            double offset = aTime.getEpochTime() - getTime().getEpochTime();
            year = aTime.getYear();
            doy = aTime.getDayOfYear();
            weekNum = doy / 7;
            String yearLabel = null;
            String weekLabel = String.format("%02d", weekNum);
            if (weekNum < 1) {
                yearLabel = String.format("(%04d)", year);
            }
            doy += 7;
            if (doy > (aTime.isLeapYear() ? 366 : 365)) {
                year++;
                doy = 1;
            }
            aTime = new TimeT(year, doy, 0, 0, 0, 0);
            result.add(new TickValue(offset, new TickLabel(weekLabel, yearLabel), true, HorizAlignment.CENTER, TickTimeType.WEEKS));
        }
        return result;
    }

    private ArrayList<TickValue> getDayTicks() {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        int year = getTime().getYear();
        int doy = getTime().getDayOfYear();
        TimeT aTime = new TimeT(year, doy, 0, 0, 0, 0);
        while (aTime.le(getEndtime())) {
            double offset = aTime.getEpochTime() - getTime().getEpochTime();
            year = aTime.getYear();
            doy = aTime.getDayOfYear();
            String yearString = null;
            String dayString = String.format("%03d", doy);
            if (doy == 1) {
                yearString = String.format("(%04d)", year);
            }
            ++doy;
            if (doy > (aTime.isLeapYear() ? 366 : 365)) {
                year++;
                doy = 1;
            }
            aTime = new TimeT(year, doy, 0, 0, 0, 0);
            result.add(new TickValue(offset, new TickLabel(dayString, yearString), true, HorizAlignment.CENTER, TickTimeType.DAYS));
        }

        return result;
    }

    private ArrayList<TickValue> getHourTicks() {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        int year = getTime().getYear();
        int doy = getTime().getDayOfYear();
        int hour = getTime().getHour();
        TimeT aTime = new TimeT(year, doy, hour, 0, 0, 0);
        while (aTime.le(getEndtime())) {
            double offset = aTime.getEpochTime() - getTime().getEpochTime();
            year = aTime.getYear();
            doy = aTime.getDayOfYear();
            hour = aTime.getHour();
            String doyString = null;
            String hourString = String.format("%02d", hour);
            if (hour == 0) {
                doyString = String.format("(Day %03d)", doy);
            }
            ++hour;
            if (hour > 23) {
                ++doy;
                hour = 0;
                if (doy > (aTime.isLeapYear() ? 366 : 365)) {
                    year++;
                    doy = 1;
                }
            }
            aTime = new TimeT(year, doy, hour, 0, 0, 0);
            result.add(new TickValue(offset, new TickLabel(hourString, doyString), true, HorizAlignment.CENTER, TickTimeType.HOURS));
        }

        return result;
    }

    private ArrayList<TickValue> getSecondsTicks(int numMinorTicks) {
        ArrayList<TickValue> result = new ArrayList<TickValue>();
        double min = 0;
        double max = this.getEnd() - this.getTime().getEpochTime();
        TickMetrics.LinearTickMetrics ticks = defineAxis(min, max, true);
        while (ticks.hasNext()) {
            double val = ticks.getNext();
            String label = String.format("%5.2f", val);
            result.add(new TickValue(val, new TickLabel(label), true, HorizAlignment.CENTER, TickTimeType.SECONDS));
            double inc2 = ticks.getIncrement() / (numMinorTicks + 1);
            for (int j = 1; j <= numMinorTicks; ++j) {
                double v = val + j * inc2;
                result.add(new TickValue(v, new TickLabel(), false, HorizAlignment.CENTER, TickTimeType.SECONDS));

            }
        }

        return result;
    }

}
