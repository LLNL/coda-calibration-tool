package gov.llnl.gnem.apps.coda.common.mapping.utils;

import gov.llnl.gnem.apps.coda.common.mapping.WMSLayerDescriptor;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import gov.llnl.gnem.apps.coda.common.mapping.api.Line;

public class LeafletToJavascript {

    public String createJsShapeRepresentation(final GeoShape shape) {
        //TODO: Encapsulate this better...
        final StringBuilder sb = new StringBuilder();

        if (shape instanceof Line) {
            final Line line = (Line) shape;
            sb.append("if (!markers.has(\"");
            sb.append(line.getId());
            sb.append("\")) {");
            sb.append("marker = L.polyline([");
            sb.append('[');
            sb.append(line.getStartLocation().getLatitude());
            sb.append(',');
            sb.append(line.getStartLocation().getLongitude());
            sb.append(']');
            sb.append(',');
            sb.append('[');
            sb.append(line.getEndLocation().getLatitude());
            sb.append(',');
            sb.append(line.getEndLocation().getLongitude());
            sb.append(']');
            sb.append(']');
            sb.append(", {color: 'black', interactive: false, weight: 1, pane: 'background-pane', bubblingMouseEvents: false, smoothFactor: 1}");
            sb.append(')');
            sb.append(".addTo(lineGroup);");
            sb.append("marker._uid = \"");
            sb.append(shape.getId());
            sb.append("\"; markers.set(marker._uid, lineGroup.getLayerId(marker)); }");
        }
        return sb.toString();
    }

    public String createJsIconRepresentation(final Icon icon) {
        //TODO: Encapsulate this better...
        final StringBuilder sb = new StringBuilder();

        sb.append("if (!markers.has(\"");
        sb.append(icon.getId());
        sb.append("\")) {");

        switch (icon.getType()) {
        case TRIANGLE_UP:
            sb.append("marker = L.shapeMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(',');
            sb.append(icon.getLocation().getLongitude());
            sb.append("], {");
            sb.append("shape: 'triangle-up',");
            sb.append(getTriangleStyle(icon.getStyle()));
            sb.append("radius: 4");
            sb.append("})");
            break;
        case CIRCLE:
        case DEFAULT:
            sb.append("marker = L.circleMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(',');
            sb.append(icon.getLocation().getLongitude());
            sb.append(']');
            sb.append(getCircleStyle(icon.getStyle(), icon.getIconSize()));
            sb.append(')');
            break;
        default:
            return "";
        }
        if (icon.getIconSelectionCallback() != null) {
            sb.append(addCallbacks(icon.getId(), icon.getFriendlyName()));
        }
        if (icon.shouldBeAnnotated()) {
            sb.append(addAnnotationTooltip(icon.getFriendlyName()));
        }
        sb.append(".addTo(iconGroup);");
        sb.append("marker._uid = \"");
        sb.append(icon.getId());
        sb.append("\"; markers.set(marker._uid, iconGroup.getLayerId(marker)); }");
        return sb.toString();
    }

    private String addAnnotationTooltip(final String name) {
        return ".bindTooltip('" + name + "', { permanent: true })";
    }

    private String addCallbacks(final String id, final String name) {
        return ".on('click', function() { iconCallbackHandler.accept(true, \""
                + id
                + "\"); }).bindPopup('"
                + name
                + "').on('popupclose', function() { iconCallbackHandler.accept(false, \""
                + id
                + "\"); }).on('mouseover', function() { if (\""
                + id
                + "\" !== mouseoverIconId) { mouseoverIconId = \""
                + id
                + "\"; }}).on('mouseout', function() { if (\""
                + id
                + "\" === mouseoverIconId) { mouseoverIconId = null; }})";
    }

    private String getTriangleStyle(final IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = "color: 'white', fillColor: 'white', opacity: 1, fillOpacity: 1, lineJoin: 'mitre', pane: 'important-event-pane', interactive: false,";
            break;
        case BACKGROUND:
            jsonStyle = "color: 'gray', fillColor: 'gray', opacity: 1, fillOpacity: 1, lineJoin: 'mitre',";
            break;
        case DEFAULT:
        default:
            jsonStyle = "color: 'yellow', fillColor: 'yellow', opacity: 1, fillOpacity: 1, lineJoin: 'mitre',";
            break;
        }
        return jsonStyle;
    }

    private String getCircleStyle(final IconStyles style, Double size) {
        String jsonStyle;

        double radius;
        if (size == null || size == 0.0) {
            radius = 5.0;
        } else {
            radius = size;
        }
        switch (style) {
        case FOCUSED:
            jsonStyle = ", { radius: " + radius + ", color: 'black', fillColor: '#ffffff', opacity: 1, fillOpacity: 1, pane: 'important-event-pane', interactive: false }";
            break;
        case BACKGROUND:
            jsonStyle = ", { radius: " + radius + ", color: 'black', fillColor: '#505050', opacity: 1, fillOpacity: 1 }";
            break;
        case DEFAULT:
        default:
            jsonStyle = ", { radius: " + radius + ", color: 'black', fillColor: '#ff0000', opacity: 1, fillOpacity: 1 }";
            break;
        }
        return jsonStyle;
    }

    public String createJsWmsRepresentation(WMSLayerDescriptor layer) {
        StringBuilder sb = new StringBuilder();

        String layerId = layer.getName().replaceAll(" ", "");

        sb.append("var wms");
        sb.append(layerId);
        sb.append(" = ");
        sb.append("L.tileLayer.wms('");
        sb.append(layer.getUrl());
        sb.append("', { layers: '");
        for (int i = 0; i < layer.getLayers().size(); i++) {
            sb.append(layer.getLayers().get(i));
            if (i != layer.getLayers().size() - 1) {
                sb.append(':');
            }
        }
        sb.append("'});");
        sb.append("layerControl.addOverlay(wms");
        sb.append(layerId);
        sb.append(",\"");
        sb.append(layer.getName());
        sb.append("\");");
        return sb.toString();
    }

}
