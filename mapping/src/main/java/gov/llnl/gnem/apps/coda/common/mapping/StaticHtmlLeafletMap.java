/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.common.mapping.api.GeoBox;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.utils.LeafletToJavascript;

public class StaticHtmlLeafletMap {

    private final Set<Icon> icons = new TreeSet<>((l, r) -> l.getId().compareTo(r.getId()));
    private final Set<GeoShape> shapes = new TreeSet<>((l, r) -> l.getId().compareTo(r.getId()));
    private final Set<WMSLayerDescriptor> layers = new HashSet<>();
    private LeafletToJavascript leaflet2js = new LeafletToJavascript();
    private static final String HTML_HEADER = "<!DOCTYPE html><html lang=\"en\"><head>";
    private static final String HTML_FOOTER = "</body></html>";
    private static final DecimalFormat DFMT2 = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    static {
        DFMT2.applyPattern("#.##");
    }

    private String title = "Map";
    private String htmlShim = null;
    private GeoBox bounds = null;

    private Boolean plotScale = Boolean.TRUE;
    private double[] eventLegendSizes = null;
    private Set<String> wmsLayersToActivate = new HashSet<>(0);

    public StaticHtmlLeafletMap() {
        try (InputStream is = this.getClass().getResourceAsStream("/leaflet/leaflet-static.shim")) {
            htmlShim = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void clearIcons() {
        icons.clear();
    }

    public boolean addIcon(final Icon icon) {
        return icons.add(icon);
    }

    public boolean removeIcon(final Icon icon) {
        return icons.remove(icon);
    }

    public void addIcons(final Collection<Icon> icons) {
        this.icons.addAll(icons);
    }

    public void removeIcons(final Collection<Icon> icons) {
        this.icons.removeAll(icons);
    }

    public void addShape(final GeoShape shape) {
        shapes.add(shape);
    }

    public void removeShape(final GeoShape shape) {
        shapes.remove(shape);
    }

    public void setBounds(final GeoBox bounds) {
        this.bounds = bounds;
    }

    public void addLayer(final WMSLayerDescriptor layer) {
        if (layer != null) {
            layers.add(layer);
        }
    }

    public void activateWmsLayer(String layerName) {
        wmsLayersToActivate.add(layerName);
    }

    public String getHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTML_HEADER);
        sb.append("<title>");
        sb.append(title);
        sb.append("</title>");
        sb.append(htmlShim);

        sb.append("<script>");
        for (WMSLayerDescriptor layer : layers) {
            sb.append(addLayerToMap(layer));
        }
        sb.append(addIconsToMap(icons));
        sb.append(addShapesToMap(shapes));

        if (bounds == null) {
            sb.append("fitViewToActiveShapes();");
        } else {
            sb.append("fitBounds([[").append(bounds.getMinY()).append(",").append(bounds.getMinX()).append("], [").append(bounds.getMaxY()).append("," + bounds.getMaxX()).append("]]);");
        }

        if (plotScale == true) {
            sb.append("addScale();");
        }

        if (eventLegendSizes != null && eventLegendSizes.length > 0) {
            sb.append("addLegend([");
            for (int i = 0; i < eventLegendSizes.length - 1; i++) {
                sb.append(DFMT2.format(eventLegendSizes[i]));
                sb.append(",");
            }
            sb.append(eventLegendSizes[eventLegendSizes.length - 1]);
            sb.append("]);");
        }

        for (String layerName : wmsLayersToActivate) {
            sb.append("wms" + layerName.replaceAll(" ", "") + ".addTo(map);");
        }

        sb.append("</script>");

        sb.append(HTML_FOOTER);
        return sb.toString();
    }

    public String addLayerToMap(final WMSLayerDescriptor layer) {
        return leaflet2js.createJsWmsRepresentation(layer);
    }

    private String addIconsToMap(final Collection<? extends Icon> icons) {
        final List<? extends Icon> iconCollection = new ArrayList<>(icons);
        final StringBuilder sb = new StringBuilder();
        for (final Icon icon : iconCollection) {
            sb.append(leaflet2js.createJsIconRepresentation(icon));
        }
        return sb.toString();
    }

    private String addShapesToMap(final Collection<? extends GeoShape> shapes) {
        final List<? extends GeoShape> shapeCollection = new ArrayList<>(shapes);
        final StringBuilder sb = new StringBuilder();
        for (final GeoShape shape : shapeCollection) {
            sb.append(leaflet2js.createJsShapeRepresentation(shape));
        }
        return sb.toString();
    }

    public StaticHtmlLeafletMap setTitle(String title) {
        this.title = title;
        return this;
    }

    public StaticHtmlLeafletMap setPlotScale(Boolean plotScale) {
        if (plotScale != null) {
            this.plotScale = plotScale;
        } else {
            this.plotScale = Boolean.FALSE;
        }
        return this;
    }

    public void subtractValueFromEventIconSize(Double value) {
        if (value != null && value != 0.0) {
            for (Icon icon : icons) {
                if (IconTypes.CIRCLE.equals(icon.getType()) && icon.getIconSize() != null) {
                    icon.setIconSize(icon.getIconSize() - value);
                }
            }
        }
    }

    public void setEventLegendScaleValues(double[] eventLegendSizes) {
        this.eventLegendSizes = eventLegendSizes;
    }

    public Set<Icon> getIcons() {
        return icons;
    }

    public Set<GeoShape> getShapes() {
        return shapes;
    }

    public Set<WMSLayerDescriptor> getLayers() {
        return layers;
    }

    public String getTitle() {
        return title;
    }

    public GeoBox getBounds() {
        return bounds;
    }

    public Boolean getPlotScale() {
        return plotScale;
    }

    public double[] getEventLegendSizes() {
        return eventLegendSizes;
    }
}
