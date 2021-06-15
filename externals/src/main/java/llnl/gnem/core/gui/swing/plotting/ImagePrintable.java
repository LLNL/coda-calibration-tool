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
package llnl.gnem.core.gui.swing.plotting;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author dodge1
 */
public class ImagePrintable implements Printable {

    private final double x, y, pageWidth;
    private final ArrayList<BufferedImage> images;
    private final double pageHeight;

    public ImagePrintable(PrinterJob printJob, PageFormat pageFormat, Collection<BufferedImage> images) {
        x = pageFormat.getImageableX();
        y = pageFormat.getImageableY();
        pageWidth = pageFormat.getImageableWidth();
        pageHeight = pageFormat.getImageableHeight();
        this.images = new ArrayList<>(images);
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {

        Graphics2D g2 = (Graphics2D) g;
        if (pageIndex < images.size()) {
            BufferedImage image = images.get(pageIndex);
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            double scaleW = 1;
            double scaleH = 1;
            if (pageWidth < image.getWidth()) {
                scaleW = pageWidth / image.getWidth();
            }
            if (pageHeight < image.getHeight()) {
                scaleH = pageHeight / image.getHeight();
            }
            double scale = Math.min(scaleW, scaleH);
            if (scale < 1) {
                imageWidth *= scale;
                imageHeight *= scale;
            }

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, (int) x, (int) y, imageWidth, imageHeight, null);
            return PAGE_EXISTS;
        } else {
            return NO_SUCH_PAGE;
        }
    }

}
