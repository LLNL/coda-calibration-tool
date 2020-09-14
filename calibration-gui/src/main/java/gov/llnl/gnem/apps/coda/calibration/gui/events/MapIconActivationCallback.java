/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.gui.events;

import java.util.function.Consumer;

import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.mapping.MAP_CALLBACK_EVENT_TYPE;
import gov.llnl.gnem.apps.coda.common.mapping.MapCallbackEvent;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;

public class MapIconActivationCallback implements Consumer<MapCallbackEvent> {
    private WaveformClient client;

    public MapIconActivationCallback(WaveformClient client) {
        this.client = client;
    }

    @Override
    public void accept(MapCallbackEvent evtVal) {
        if (evtVal != null && MAP_CALLBACK_EVENT_TYPE.ACTIVATION.equals(evtVal.getType())) {
            Icon icon = evtVal.getIcon();
            switch (icon.getType()) {
            case CIRCLE:
                client.setWaveformsActiveByEventId(icon.getFriendlyName(), evtVal.getFlag()).subscribe();
                break;
            case TRIANGLE_UP:
                client.setWaveformsActiveByStationName(icon.getFriendlyName(), evtVal.getFlag()).subscribe();
                break;
            case POLYGON_OUT:
                client.setWaveformsActiveOutsidePolygon(evtVal.getFlag()).subscribe();
                break;
            case POLYGON_IN:
                client.setWaveformsActiveInsidePolygon(evtVal.getFlag()).subscribe();
                break;
            case DEFAULT:
            default:
                break;
            }
        }
    }
}
