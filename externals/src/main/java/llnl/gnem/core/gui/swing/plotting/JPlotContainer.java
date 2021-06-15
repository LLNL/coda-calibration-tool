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
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.filechooser.FileFilter;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * JPlotContainer is the base class for all plot containers that host a single
 * plot (JBasicPlot). The principal function of this class is to manage the
 * layout of the JBasicPlot and to render its border and background
 */
public abstract class JPlotContainer extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(JPlotContainer.class);
    private static final double DEFAULT_OFFSET = 17.0;
    private static final double DEFAULT_WIDTH = 16.0;
    protected CoordinateTransform coordTransform;
    protected double HorizontalOffset;
    // Distance in units from Canvas left edge to drawing area
    protected double VerticalOffset;
    // Distance in units from Canvas top edge to top of drawing area
    protected double BoxHeight;
    // Drawing area in drawing units
    protected double BoxWidth;
    // Drawing area in drawing units
    protected double borderWidth;
    // Border width is set independently, and is in millimeters
    // You can also access the drawing region dimensions in pixels. Note that
    // these values will typically change every time a form is resized, so they
    // must be used immediately after being obtained.
    protected int left;
    protected int top;
    protected int plotWidth;
    protected int plotHeight;
    protected Title title = null;
    protected DrawingUnits unitsMgr = null;
    protected Graphics ActiveGraphics;
    protected DrawingRegion PlotRegion = null;
    protected DrawingRegion PlotBorder = null;
    protected boolean showBorder;
    private static boolean allowXor = true;
    protected boolean forceFullRender = false;

    /**
     * Constructor for the JPlotContainer object
     */
    public JPlotContainer() {
        ActiveGraphics = getGraphics();
        unitsMgr = new DrawingUnits();
        title = new Title();
        HorizontalOffset = DEFAULT_OFFSET;
        VerticalOffset = DEFAULT_OFFSET;
        borderWidth = DEFAULT_WIDTH;
        PlotRegion = new DrawingRegion();
        PlotBorder = new DrawingRegion();
        showBorder = true;
        //       AxisVisible = true;
    }

    public abstract void Render(Graphics g);

    public abstract void Render(Graphics g, double HOffset, double VertOffset, double boxWidth, double boxHeight);

    /**
     * Called by the graphics system when this component must be updated.
     *
     * @param g
     *            The graphics context on which to render the component.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Render(g);
    }

    /**
     * Gets the plotRegion of the JPlotContainer object. The PlotRegion is the
     * interior of the axis. This is a DrawingRegion object, and is used to
     * control how the interior and border are brushed and stroked.
     *
     * @return The plotRegion value
     */
    public DrawingRegion getPlotRegion() {
        return PlotRegion;
    }

    public static void setAllowXor(boolean v) {
        allowXor = v;
    }

    public static boolean getAllowXor() {
        return allowXor;
    }

    public abstract void setPolyLineUsage(boolean value);

    /**
     * Gets the visible attribute of the JPlotContainer object. By default, a
     * JPlotContainer is visible, meaning that when it is rendered, the Canvas
     * gets updated. However, it can be set to be invisible.
     *
     * @return The visible value
     */
    public boolean getVisible() {
        return isVisible();
    }

    /**
     * Gets the current graphics context of the JPlotContainer object
     *
     * @return The active Graphics value
     */
    public Graphics getActiveGraphics() {
        return ActiveGraphics;
    }

    /**
     * Gets the plotBorder of the JPlotContainer object. This DrawingRegion
     * controls the brushing and stroking of the plot margin, the region outside
     * of the plot interior containing the axes with their annotations and the
     * plot title.
     *
     * @return The plotBorder value
     */
    public DrawingRegion getPlotBorder() {
        return PlotBorder;
    }

    /**
     * Gets the title of the JPlotContainer object. The title is an object that
     * contains a String to be displayed at the top of the plot and a set of
     * methods controlling how that String is displayed.
     *
     * @return The title value
     */
    public Title getTitle() {
        return title;
    }

    /**
     * Sets the horizontal Offset of the JPlotContainer object. This is the
     * distance in millimeters from the left edge of the container of the
     * JPlotContainer to the left edge of the JPlotContainer left margin.
     *
     * @param v
     *            The new horizontal Offset value.
     */
    public void setHorizontalOffset(double v) {
        HorizontalOffset = v;
    }

    /**
     * Sets the verticalOffset attribute of the JPlotContainer object. This is
     * the distance in millimeters from the top of the container holding the
     * JPlotContainer to the top of the top margin of the JPlotContainer.
     *
     * @param v
     *            The new verticalOffset value
     */
    public void setVerticalOffset(double v) {
        VerticalOffset = v;
    }

    /**
     * Sets the height of the plot interior in millimeters.
     *
     * @param v
     *            The new box Height value
     */
    public void setBoxHeight(double v) {
        BoxHeight = v;
    }

    /**
     * Sets the width of the plot interior in millimeters
     *
     * @param v
     *            The new box Width value
     */
    public void setBoxWidth(double v) {
        BoxWidth = v;
    }

    /**
     * Gets the offset of the left edge of the plot interior in pixels relative
     * to the left edge of the graphics context.
     *
     * @return The plotLeft value
     */
    public int getPlotLeft() {
        return left;
    }

    /**
     * Gets the offset from the top of the graphics context to the top of the
     * plot interior in pixels.
     *
     * @return The plotTop value
     */
    public int getPlotTop() {
        return top;
    }

    /**
     * Gets the width of the plot interior in pixels
     *
     * @return The plotWidth value
     */
    public int getPlotWidth() {
        return plotWidth;
    }

    /**
     * Gets the height of the plot interior in pixels.
     *
     * @return The plotHeight value
     */
    public int getPlotHeight() {
        return plotHeight;
    }

    /**
     * Gets the width of the plot border in millimeters
     *
     * @return The borderWidth value
     */
    public double getBorderWidth() {
        return borderWidth;
    }

    /**
     * Sets the width (in mm) of the plot border
     *
     * @param v
     *            The new borderWidth value
     */
    public void setBorderWidth(double v) {
        borderWidth = v;
    }

    //
    /**
     * Sets the showBorder attribute of the JPlotContainer object.
     * JPlotContainer plots are, by default drawn with a border around the plot
     * region. The border can be filled with a color and pattern and can include
     * a bounding rectangle. To control whether the border is drawn or not, use
     * the ShowBorder method.
     *
     * @param v
     *            The new showBorder value
     */
    public void setShowBorder(boolean v) {
        showBorder = v;
    }

    /**
     * Gets the units manager object of the JPlotContainer object. The units
     * manager maps millimeters topixel values, and is used for laying out parts
     * of the plot. End users of JPlotContainer should have no need to access
     * the Units Manager.
     *
     * @return The unitsMgr value
     */
    public DrawingUnits getUnitsMgr() {
        return unitsMgr;
    }

    public CoordinateTransform getCoordinateTransform() {
        return coordTransform;
    }

    public void setCoordinateTransform(CoordinateTransform transform) {
        coordTransform = transform;
    }

    public boolean isForceFullRender() {
        return forceFullRender;
    }

    public void setForceFullRender(boolean forceFullRender) {
        this.forceFullRender = forceFullRender;
    }

    /**
     * This method will set the settings that lead to best looking svg output of
     * complex shapes.
     */
    private void setupSvgExportSettings() {
        RepaintManager cm = RepaintManager.currentManager(this);
        cm.setDoubleBufferingEnabled(false);
        this.setPolyLineUsage(false);
        this.setForceFullRender(true);
    }

    /**
     * This method will change back the settings to optimal screen quality (vs.
     * optimal print/svg quality)
     */
    private void revertSvgExportSettings() {
        RepaintManager cm = RepaintManager.currentManager(this);
        cm.setDoubleBufferingEnabled(true);
        this.setPolyLineUsage(true);
        this.setForceFullRender(false);
    }

    public void exportSVG(String filename) throws UnsupportedEncodingException, FileNotFoundException, SVGGraphics2DIOException {
        exportSVG(new File(filename));
    }

    public void exportSVG(File file) throws UnsupportedEncodingException, FileNotFoundException, SVGGraphics2DIOException {
        // Set the properties that will make for the best quality SVG
        // Copied from: JPlotContainer.printCurrentPlot method
        // Only putting method call here and not in other exportSvg calls because all roads lead here in the end
        // Logic to set these properties is now centralized in JPlotContainer so all apps immediately see advantage
        setupSvgExportSettings();

        SVGGraphics2D svgGenerator = renderSVG(createDocument());

        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes     
        try (Writer out = new OutputStreamWriter(Files.newOutputStream(file.toPath()), "UTF-8")) {
            svgGenerator.stream(out, useCSS);
        } catch (IOException e) {
            log.error("Unable to write file {}", file.getAbsolutePath(), e);
        }

        // put the settings back to optimal screen quality
        revertSvgExportSettings();
    }

    public void transcode(PrintTranscoder t, PageFormat format) throws Exception {
        SVGDocument document = createDocument();
        SVGGraphics2D g = new SVGGraphics2D(document);

        double scaleX = format.getImageableWidth() / getWidth();
        double scaleY = format.getImageableHeight() / getHeight();
        double scale = Math.min(scaleX, scaleY);
        g.scale(scale, scale);
        renderSVG(g);

        // Populate the document root with the generated SVG content
        Element root = document.getDocumentElement();
        g.getRoot(root);

        TranscoderInput input = new TranscoderInput(document);
        t.transcode(input, null);
    }

    private SVGGraphics2D renderSVG(SVGDocument document) {
        // Create an instance of the SVG Generator.
        return renderSVG(new SVGGraphics2D(document));
    }

    private SVGGraphics2D renderSVG(SVGGraphics2D g) {
        // Ask the test to render into the SVG Graphics2D implementation.
        Render(g);
        return g;
    }

    private static SVGDocument createDocument() {
        // Get a DOMImplementation.
        DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        SVGDocument document = (SVGDocument) domImpl.createDocument(svgNS, "svg", null);
        return document;
    }

    public static void printCurrentPlot(final JPlotContainer gui) {

        int dpi = 600;
        float plotBoxWidthMM = 150;
        float offsetToPlotBoxMM = 30;
        BufferedImage image = createBufferedImage(dpi, gui, offsetToPlotBoxMM, plotBoxWidthMM);
        final ArrayList<BufferedImage> images = new ArrayList<>();
        images.add(image);
        PrinterJob pjob = PrinterJob.getPrinterJob();
        pjob.setPrintable(new ImagePrintable(pjob, new PageFormat(), images));
        pjob.setJobName("Print Current plot");
        pjob.setCopies(1);

        if (pjob.printDialog()) {
            new PrintWorker(pjob).execute();
        }
    }

    public static BufferedImage createBufferedImage(int dpi, final JPlotContainer gui, float offsetToPlotBoxMM, float plotBoxWidthMM) {
        double scale = (double) dpi / 72;
        double pageWidth = 8;
        double pixels = pageWidth * dpi;
        BufferedImage bi = new BufferedImage((int) pixels, (int) pixels, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        boolean drawBox = gui.getPlotBorder().getDrawBox();
        gui.getPlotBorder().setDrawBox(false);
        int fontsize = gui.getTitle().getFontSize();
        gui.getTitle().setFontSize(10);
        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(scale, scale);
        RepaintManager cm = RepaintManager.currentManager(gui);
        cm.setDoubleBufferingEnabled(false);
        gui.setPolyLineUsage(false);
        gui.setForceFullRender(true);
        gui.Render(g2d, offsetToPlotBoxMM, offsetToPlotBoxMM, plotBoxWidthMM, plotBoxWidthMM);
        gui.getPlotBorder().setDrawBox(drawBox);
        gui.getTitle().setFontSize(fontsize);
        gui.setPolyLineUsage(true);
        gui.setForceFullRender(false);
        cm.setDoubleBufferingEnabled(true);
        g.dispose();
        return bi;
    }

    public void exportSVG() {
        FileFilter svgFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".svg");
            }

            @Override
            public String getDescription() {
                return "SVG Files";
            }
        };

        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(svgFilter);
        chooser.setFileFilter(svgFilter);
        File saveFile = new File("plot.svg");
        chooser.setSelectedFile(saveFile);
        int rval = chooser.showSaveDialog(null);
        if (rval == JFileChooser.APPROVE_OPTION) {
            saveFile = chooser.getSelectedFile();
            try {
                exportSVG(saveFile);
            } catch (UnsupportedEncodingException | FileNotFoundException | SVGGraphics2DIOException e) {
            }
        }
    }

    public void print() {
        printCurrentPlot(this);
    }

    public static void printAllPlots(Collection<? extends JPlotContainer> plots) {

        int dpi = 600;
        float plotBoxWidthMM = 150;
        float offsetToPlotBoxMM = 30;
        final ArrayList<BufferedImage> images = new ArrayList<>();
        for (JPlotContainer plot : plots) {
            images.add(JPlotContainer.createBufferedImage(dpi, plot, offsetToPlotBoxMM, plotBoxWidthMM));
        }

        PrinterJob pjob = PrinterJob.getPrinterJob();
        pjob.setPrintable(new ImagePrintable(pjob, new PageFormat(), images));
        pjob.setJobName("Print Current plot");
        pjob.setCopies(1);

        if (pjob.printDialog()) {
            new PrintWorker(pjob).execute();
        }

    }

}
