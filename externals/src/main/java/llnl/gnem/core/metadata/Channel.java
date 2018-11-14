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
package llnl.gnem.core.metadata;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dodge1
 */
public class Channel {

    private static final Logger log = LoggerFactory.getLogger(Channel.class);

    private final BandCode band;
    private final InstrumentCode instrument;
    private final OrientationCode orientation;

    public Channel(BandCode band, InstrumentCode instrument, OrientationCode code) {
        this.band = band;
        this.instrument = instrument;
        this.orientation = code;
    }

    public Channel(String chan) {
        if (chan.length() < 3) {
            String msg = String.format("Failed constructing Channel. Channel string must be at least 3-characters long! (Supplied = %s)", chan);
            log.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        try {
            band = BandCode.valueOf(chan.substring(0, 1).toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new BandCodeException("Failed constructing channel because of invalid BAND code.", ex);
        }

        try {
            instrument = InstrumentCode.valueOf(chan.substring(1, 2).toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new InstrumentCodeException("Failed constructing channel because of invalid Instrument code.", ex);
        }
        String ocode = chan.substring(2, 3).toUpperCase(Locale.ENGLISH);
        try {
            orientation = OrientationCode.getEnumValue(ocode);
        } catch (IllegalArgumentException ex) {
            throw new OrientationCodeException("Failed constructing channel because of invalid Orientation code.", ex);
        }

    }

    public BandCode getBandCode() {
        return band;
    }

    public InstrumentCode getInstrumentCode() {
        return instrument;
    }

    /**
     * @return the code
     */
    public OrientationCode getOrientationCode() {
        return orientation;
    }

    public String getFDSNChannelString() {
        return band.toString() + instrument.toString() + orientation.toString();
    }

    static class BandCodeException extends IllegalArgumentException {

        public BandCodeException(String msg) {
            super(msg);
        }

        public BandCodeException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }

    static class InstrumentCodeException extends IllegalArgumentException {

        public InstrumentCodeException(String msg) {
            super(msg);
        }

        public InstrumentCodeException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }

    static class OrientationCodeException extends IllegalArgumentException {

        public OrientationCodeException(String msg) {
            super(msg);
        }

        public OrientationCodeException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }
}
