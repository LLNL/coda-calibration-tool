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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.sac;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.calibration.model.domain.util.PICK_TYPES;
import llnl.gnem.core.io.SAC.SACHeader;
import llnl.gnem.core.signalprocessing.Sequence;
import llnl.gnem.core.util.TimeT;

@Service
public class SacExporter {
    private static final String SEP = "_";
    private final NumberFormat dfmt1 = NumberFormatFactory.oneDecimalOneLeadingZero();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public void writeWaveformToDirectory(File exportDirectory, Waveform w) {
        try (DataOutputStream os = new DataOutputStream(Files.newOutputStream(exportDirectory.toPath().resolve(getFileName(w))))) {
            SACHeader header = new SACHeader();
            header.setTime(new TimeT(w.getEvent().getOriginTime()));
            header.setBeginTime(new TimeT(w.getBeginTime()));
            header.setOriginTime(new TimeT(w.getEvent().getOriginTime()));

            header.nevid = Integer.valueOf(w.getEvent().getEventId());
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
            float[] sequence = new Sequence(ArrayUtils.toPrimitive(w.getSegment())).getArray();
            header.npts = sequence.length;
            header.write(os);
            for (int i = 0; i < sequence.length; i++) {
                os.writeFloat(sequence[i]);
            }
            os.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getFileName(Waveform w) {
        String name = w.getStream().getStation().getStationName() + SEP + w.getStream().getChannelName() + SEP + w.getEvent().getEventId() + SEP + dfmt1.format(w.getLowFrequency()) + SEP
                + dfmt1.format(w.getHighFrequency()) + SEP + w.getSegmentType() + SEP + ".env";
        return name.toUpperCase();
    }
}
