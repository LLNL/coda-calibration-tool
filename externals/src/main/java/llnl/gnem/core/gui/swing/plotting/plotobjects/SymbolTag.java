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
package llnl.gnem.core.gui.swing.plotting.plotobjects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * User: dodge1 Date: Feb 15, 2006
 */
public class SymbolTag {

    private Color fillColor = Color.white;
    private Color edgeColor = Color.black;

    private Color textColor;
    private static Color grommetColor = new Color(204, 102, 0);
    private static Color grommetAccentColor = new Color(255, 153, 102);

    static int xOffset = -32;
    static int[] tagX = { 0, 14, 17, 19, 26, 26, 12, 0 };
    static int[] tagY = { 23, 9, 8, 8, 12, 16, 30, 23 };

    static int[] grommetX = { 18, 20, 21, 21, 20, 18, 17, 17, 18 };
    static int[] grommetY = { 11, 11, 12, 13, 14, 14, 13, 12, 11 };
    private TextLayout textLayout;
    private double textHalfWidth;
    private double textHalfHeight;
    private double tagRotationAngle = 0;
    private boolean visible = true;

    public SymbolTag(char text, Color textColor) {
        initialize(textColor, text);
    }

    public SymbolTag(char text, Color textColor, double rotationAngle) {
        initialize(textColor, text);
        tagRotationAngle = rotationAngle;
    }

    private void initialize(Color textColor, char text) {
        this.textColor = textColor;
        StringBuffer sb = new StringBuffer();
        sb.append(text);
        textLayout = new TextLayout(sb.toString(), new Font("Courier", Font.PLAIN, 12), new FontRenderContext(null, false, false));
        Rectangle2D bounds = textLayout.getBounds();
        textHalfWidth = bounds.getWidth() / 2;
        textHalfHeight = bounds.getHeight() / 2;
    }

    public void setFillColor(Color c) {
        fillColor = c;
    }

    public void setEdgeColor(Color c) {
        edgeColor = c;
    }

    public void render(int xOrigin, int yOrigin, Graphics g) {
        if (visible) {
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform saveXform = g2d.getTransform();

            AffineTransform transform = AffineTransform.getRotateInstance(tagRotationAngle, xOrigin, yOrigin);
            g2d.transform(transform);
            renderTagBody(xOrigin, yOrigin, g2d);
            renderGrommet(xOrigin, yOrigin, g2d);
            renderString(xOrigin, yOrigin, g2d);

            Point2D textPos = transform.transform(new Point2D.Double(xOrigin - 24 + textHalfWidth, yOrigin + 25 - textHalfHeight), null);
            g2d.setTransform(saveXform);

            PaintText(g2d, (int) (textPos.getX() - textHalfWidth), (int) (textPos.getY() + textHalfHeight));
        }
    }

    private void renderString(int xOrigin, int yOrigin, Graphics2D g2d) {
        GeneralPath path = new GeneralPath();
        xOrigin += xOffset;
        path.moveTo(xOrigin + 20, yOrigin + 12);
        path.lineTo(xOrigin + 31, yOrigin + 0);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(path);
    }

    private void renderGrommet(int xOrigin, int yOrigin, Graphics2D g2d) {
        GeneralPath path = new GeneralPath();
        xOrigin += xOffset;
        path.moveTo(xOrigin + grommetX[0], yOrigin + grommetY[0]);
        for (int j = 1; j < grommetX.length; ++j) {
            path.lineTo(xOrigin + grommetX[j], yOrigin + grommetY[j]);

        }
        g2d.setColor(grommetColor);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(path);

        // Render accent color on grommet lower edge
        path = new GeneralPath();
        path.moveTo(xOrigin + 18, yOrigin + 12);
        path.lineTo(xOrigin + 18, yOrigin + 13);
        path.lineTo(xOrigin + 20, yOrigin + 13);
        g2d.setColor(grommetAccentColor);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(path);
    }

    private void renderTagBody(int xOrigin, int yOrigin, Graphics2D g2d) {
        GeneralPath path = new GeneralPath();
        xOrigin += xOffset;
        path.moveTo(xOrigin + tagX[0], yOrigin + tagY[0]);
        for (int j = 1; j < tagX.length; ++j) {
            path.lineTo(xOrigin + tagX[j], yOrigin + tagY[j]);

        }
        g2d.setColor(fillColor);
        g2d.fill(path);
        g2d.setColor(edgeColor);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(path);

        // Render shadow on tag lower edge
        path = new GeneralPath();
        path.moveTo(xOrigin + 2, yOrigin + 24);
        path.lineTo(xOrigin + 12, yOrigin + 29);
        path.lineTo(xOrigin + 25, yOrigin + 16);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(path);
    }

    protected void PaintText(Graphics2D g2d, int x, int y) {
        // Create new font and color
        g2d.setColor(textColor);

        // Layout and render text
        textLayout.draw(g2d, x, y);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
