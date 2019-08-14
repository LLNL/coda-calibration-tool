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
package gov.llnl.gnem.apps.coda.common.gui.converters.sac;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import llnl.gnem.core.io.SAC.SACHeader;
import llnl.gnem.core.signalprocessing.Sequence;
import llnl.gnem.core.util.TimeT;

@Component
public class SacExporter {
    private static final String SEP = "_";
    private static final Logger log = LoggerFactory.getLogger(SacExporter.class);

    public Result<String> writeWaveformToDirectory(File exportDirectory, Waveform w) {
        String filename = getFileName(w);
        if (filename != null && !filename.trim().isEmpty()) {
            try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(exportDirectory.toPath().resolve(filename))))) {
                try (SACHeader header = new SACHeader()) {
                    header.setTime(new TimeT(w.getEvent().getOriginTime()));
                    header.setBeginTime(new TimeT(w.getBeginTime()));
                    header.setOriginTime(new TimeT(w.getEvent().getOriginTime()));

                    header.nevid = Integer.parseInt(w.getEvent().getEventId());
                    if (w.getAssociatedPicks() != null) {
                        for (int i = 0; i < w.getAssociatedPicks().size() && i <= 10; i++) {
                            try {
                                WaveformPick pick = w.getAssociatedPicks().get(i);
                                if (pick.getPickType().equalsIgnoreCase(PICK_TYPES.F.getPhase())) {
                                    header.f = pick.getPickTimeSecFromOrigin();
                                } else if (pick.getPickType().equalsIgnoreCase(PICK_TYPES.A.getPhase())) {
                                    header.a = pick.getPickTimeSecFromOrigin();
                                } else if (pick.getPickType().equalsIgnoreCase(PICK_TYPES.B.getPhase())) {
                                    header.b = pick.getPickTimeSecFromOrigin();
                                } else if (pick.getPickType().equalsIgnoreCase(PICK_TYPES.O.getPhase())) {
                                    header.o = pick.getPickTimeSecFromOrigin();
                                } else {
                                    header.setTimePick(i, pick.getPickTimeSecFromOrigin(), pick.getPickName());
                                }
                            } catch (RuntimeException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }

                    header.delta = (float) (1.0 / w.getSampleRate());
                    header.kstnm = w.getStream().getStation().getStationName();
                    header.kcmpnm = w.getStream().getChannelName();
                    header.stla = (float) w.getStream().getStation().getLatitude();
                    header.stlo = (float) w.getStream().getStation().getLongitude();
                    header.knetwk = w.getStream().getStation().getNetworkName();
                    header.kevnm = w.getEvent().getEventId();
                    header.evla = (float) w.getEvent().getLatitude();
                    header.evlo = (float) w.getEvent().getLongitude();
                    String depType = w.getSegmentType();
                    if (depType != null && !depType.trim().isEmpty()) {
                        if (depType.toLowerCase(Locale.ENGLISH).startsWith("dis")) {
                            header.idep = 6;
                        } else if (depType.toLowerCase(Locale.ENGLISH).startsWith("vel")) {
                            header.idep = 7;
                        } else if (depType.toLowerCase(Locale.ENGLISH).startsWith("acc")) {
                            header.idep = 8;
                        }
                    }
                    float[] sequence = new Sequence(w.getSegment()).getArray();
                    header.npts = sequence.length;
                    header.write(os);
                    for (int i = 0; i < sequence.length; i++) {
                        os.writeFloat(sequence[i]);
                    }
                    os.flush();

                    return new Result<>(true, filename);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return new Result<>(false, e.toString()).setErrors(Collections.singletonList(e));
            }
        }

        String message = "Waveform is missing required information or is malformed; waveform: " + w;
        log.error(message);
        return new Result<>(false, message);
    }

    private boolean waveformFullySpecified(Waveform w) {
        return w != null
                && w.getLowFrequency() != null
                && w.getHighFrequency() != null
                && w.getSampleRate() != null
                && w.getBeginTime() != null
                && w.getEndTime() != null
                && w.getEvent() != null
                && w.getEvent().getOriginTime() != null
                && present(w.getEvent().getEventId())
                && w.getStream() != null
                && present(w.getStream().getChannelName())
                && w.getStream().getStation() != null
                && present(w.getStream().getStation().getStationName());
    }

    public String getFileName(Waveform w) {
        if (waveformFullySpecified(w)) {
            NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();
            Stream stream = w.getStream();
            Event ev = w.getEvent();

            String segType = w.getSegmentType();
            if (segType != null && !segType.isEmpty()) {
                if (segType.length() > 3) {
                    segType = segType.substring(0, 3);
                }
                segType = segType.toUpperCase();
            } else {
                segType = "UNK";
            }
            Station sta = stream.getStation();
            String name = sta.getStationName()
                    + SEP
                    + stream.getChannelName()
                    + SEP
                    + ev.getEventId()
                    + SEP
                    + dfmt4.format(w.getLowFrequency())
                    + SEP
                    + dfmt4.format(w.getHighFrequency())
                    + SEP
                    + segType
                    + SEP
                    + ".env";
            return name.toUpperCase(Locale.ENGLISH);
        }
        return "";
    }

    private boolean present(String val) {
        return val != null && !val.toUpperCase(Locale.ENGLISH).trim().isEmpty();
    }
}
