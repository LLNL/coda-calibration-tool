//                                                                                          SACHeader.java
//  copyright 2001 Regents of the University of California
//  Author:   Dave Harris
//  Created:
//  Last Modified:  January 4, 2004
package llnl.gnem.core.io.SAC;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.util.Geometry.GeodeticCoordinate;

public class SACHeader implements Closeable {

    public float a; // first arrival time
    public float az; // s-r azimuth, degrees
    public float b; // RD initial value, time
    public float baz; // s-r backazimuth, degrees
    public float cmpaz; // component azimuth, degrees
    public float cmpinc; // component inclination, degrees
    // instance variables
    public float delta;   // RF time increment, seconds
    public float depmax;  // maximum amplitude
    public float depmen;   // mean value
    public float depmin;  // minimum amplitude
    public float dist;       // s-r distance, km
    public float e;       // RD final value, time
    public float evdp;       // event depth
    public float evel;       // event elevation
    public float evla;   // event latitude
    public float evlo;   // event longitude
    public float f;   // event end sec < nz.
    public float fmt;   // internal use
    public float gcarc;    // s-r distance, degrees
    public int idep;
    public int ievreg;
    public int ievtyp;
    public int iftype;
    public int iinst;
    public int imagsrc;
    public int imagtyp;
    public int instreg;
    public int iqual;
    public int isynth;
    public String ka;
    public String kcmpnm;
    public String kdatrd;
    public String kevnm;
    public String kf;
    public String khole;
    public String kinst;
    public String knetwk;
    public String ko;
    public String kstnm;
    public String[] kt = new String[10];
    public String kuser0;
    public String kuser1;
    public String kuser2;
    public int lcalda;
    public int leven;
    public int lovrok;
    public int lpspol;
    public float mag; // magnitude
    public int nevid;
    public int norid;
    public int npts;
    public int nsnpts;
    public int nvhdr;
    public int nwfid;
    public int nxsize;
    public int nysize;
    public int nzhour;
    public int nzjday;
    public int nzmin;
    public int nzmsec;
    public int nzsec;
    public int nzyear;
    public float o; // event start, sec < nz
    public float odelta; // observed time increment
    public float[] resp = new float[10]; // instrument response param
    public float sb; // internal use
    public float scale; // amplitude scale factor
    public float sdelta; // internal use
    public float stdp; // station depth
    public float stel; // station elevation
    public float stla; // station latitude
    public float stlo; // station longitude
    public float[] t = new float[10]; // user defined time picks
    public float unused10;
    public float unused11;
    public float unused12;
    public int unused15;
    public int unused16;
    public int unused19;
    public int unused20;
    public int unused21;
    public int unused22;
    public int unused23;
    public int unused24;
    public int unused25;
    public int unused26;
    public int unused27;
    public float unused6;
    public float unused7;
    public float unused8;
    public float unused9;
    public float[] user = new float[10]; // user variables
    public float xmaximum;
    public float xminimum;
    public float ymaximum;
    public float yminimum;
    private Iztype iztype;
    private InputStream stream;
    private boolean swapBytes;

    // default constructor
    public SACHeader() {
        swapBytes = false;

        // load default SAC header fields
        delta = FLOATDEFAULT;
        depmin = FLOATDEFAULT;
        depmax = FLOATDEFAULT;
        scale = FLOATDEFAULT;
        odelta = FLOATDEFAULT;
        b = FLOATDEFAULT;
        e = FLOATDEFAULT;
        o = FLOATDEFAULT;
        a = FLOATDEFAULT;
        fmt = FLOATDEFAULT;
        t[0] = FLOATDEFAULT;
        t[1] = FLOATDEFAULT;
        t[2] = FLOATDEFAULT;
        t[3] = FLOATDEFAULT;
        t[4] = FLOATDEFAULT;
        t[5] = FLOATDEFAULT;
        t[6] = FLOATDEFAULT;
        t[7] = FLOATDEFAULT;
        t[8] = FLOATDEFAULT;
        t[9] = FLOATDEFAULT;
        f = FLOATDEFAULT;
        resp[0] = FLOATDEFAULT;
        resp[1] = FLOATDEFAULT;
        resp[2] = FLOATDEFAULT;
        resp[3] = FLOATDEFAULT;
        resp[4] = FLOATDEFAULT;
        resp[5] = FLOATDEFAULT;
        resp[6] = FLOATDEFAULT;
        resp[7] = FLOATDEFAULT;
        resp[8] = FLOATDEFAULT;
        resp[9] = FLOATDEFAULT;
        stla = FLOATDEFAULT;
        stlo = FLOATDEFAULT;
        stel = FLOATDEFAULT;
        stdp = FLOATDEFAULT;
        evla = FLOATDEFAULT;
        evlo = FLOATDEFAULT;
        evel = FLOATDEFAULT;
        evdp = FLOATDEFAULT;
        mag = FLOATDEFAULT;
        user[0] = FLOATDEFAULT;
        user[1] = FLOATDEFAULT;
        user[2] = FLOATDEFAULT;
        user[3] = FLOATDEFAULT;
        user[4] = FLOATDEFAULT;
        user[5] = FLOATDEFAULT;
        user[6] = FLOATDEFAULT;
        user[7] = FLOATDEFAULT;
        user[8] = FLOATDEFAULT;
        user[9] = FLOATDEFAULT;
        dist = FLOATDEFAULT;
        az = FLOATDEFAULT;
        baz = FLOATDEFAULT;
        gcarc = FLOATDEFAULT;
        sb = FLOATDEFAULT;
        sdelta = FLOATDEFAULT;
        depmen = FLOATDEFAULT;
        cmpaz = FLOATDEFAULT;
        cmpinc = FLOATDEFAULT;
        xminimum = FLOATDEFAULT;
        xmaximum = FLOATDEFAULT;
        yminimum = FLOATDEFAULT;
        ymaximum = FLOATDEFAULT;
        unused6 = FLOATDEFAULT;
        unused7 = FLOATDEFAULT;
        unused8 = FLOATDEFAULT;
        unused9 = FLOATDEFAULT;
        unused10 = FLOATDEFAULT;
        unused11 = FLOATDEFAULT;
        unused12 = FLOATDEFAULT;
        nzyear = INTDEFAULT;
        nzjday = INTDEFAULT;
        nzhour = INTDEFAULT;
        nzmin = INTDEFAULT;
        nzsec = INTDEFAULT;
        nzmsec = INTDEFAULT;
        nvhdr = INTDEFAULT;
        norid = INTDEFAULT;
        nevid = INTDEFAULT;
        npts = INTDEFAULT;
        nsnpts = INTDEFAULT;
        nwfid = INTDEFAULT;
        nxsize = INTDEFAULT;
        nysize = INTDEFAULT;
        unused15 = INTDEFAULT;
        iftype = INTDEFAULT;
        idep = INTDEFAULT;
        setIztype(Iztype.IUNKN);
        unused16 = INTDEFAULT;
        iinst = INTDEFAULT;
        instreg = INTDEFAULT;
        ievreg = INTDEFAULT;
        ievtyp = INTDEFAULT;
        iqual = INTDEFAULT;
        isynth = INTDEFAULT;
        imagtyp = INTDEFAULT;
        imagsrc = INTDEFAULT;
        unused19 = INTDEFAULT;
        unused20 = INTDEFAULT;
        unused21 = INTDEFAULT;
        unused22 = INTDEFAULT;
        unused23 = INTDEFAULT;
        unused24 = INTDEFAULT;
        unused25 = INTDEFAULT;
        unused26 = INTDEFAULT;
        leven = INTDEFAULT;
        lpspol = INTDEFAULT;
        lovrok = INTDEFAULT;
        lcalda = INTDEFAULT;
        unused27 = INTDEFAULT;
        kstnm = STRINGDEFAULT;
        kevnm = "-12345          ";
        khole = STRINGDEFAULT;
        ko = STRINGDEFAULT;
        ka = STRINGDEFAULT;
        kt[0] = STRINGDEFAULT;
        kt[1] = STRINGDEFAULT;
        kt[2] = STRINGDEFAULT;
        kt[3] = STRINGDEFAULT;
        kt[4] = STRINGDEFAULT;
        kt[5] = STRINGDEFAULT;
        kt[6] = STRINGDEFAULT;
        kt[7] = STRINGDEFAULT;
        kt[8] = STRINGDEFAULT;
        kt[9] = STRINGDEFAULT;
        kf = STRINGDEFAULT;
        kuser0 = STRINGDEFAULT;
        kuser1 = STRINGDEFAULT;
        kuser2 = STRINGDEFAULT;
        kcmpnm = STRINGDEFAULT;
        knetwk = STRINGDEFAULT;
        kdatrd = STRINGDEFAULT;
        kinst = STRINGDEFAULT;

    }

    public SACHeader(String filename) throws IOException {
        this(new File(filename));
    }

    public SACHeader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public SACHeader(ObjectInput objectInput) throws IOException {
        byte[] headerImage = new byte[SACHBYTES];
        objectInput.read(headerImage);

        setFieldsFromByteArray(headerImage);
    }

    //  construct from file
    public SACHeader(InputStream sacis) throws IOException {
        byte[] headerImage = new byte[SACHBYTES];
        sacis.read(headerImage);
        this.stream = sacis;

        setFieldsFromByteArray(headerImage);
    }

    // class variables
    public static final float FLOATDEFAULT = -12345.0f;
    public static final int INTDEFAULT = -12345;
    public static final String STRINGDEFAULT = "-12345  ";
    public static final int ia = 12;
    public static final int iacc = 8;
    public static final int iamph = 3;
    public static final int ib = 9;
    public static final int ibrk = 63;
    public static final int icaltech = 64;
    public static final int ichem = 43;
    public static final int iday = 10;
    public static final int idisp = 6;
    public static final int idown = 30;
    public static final int idrop = 47;
    public static final int ieast = 28;
    public static final int ieq = 75;
    public static final int ieq1 = 76;
    public static final int ieq2 = 77;
    public static final int ievloc = 66;
    public static final int iex = 79;
    public static final int iglch = 46;
    public static final int igood = 45;
    public static final int ihglp = 35;
    public static final int ihorza = 29;
    public static final int iisc = 60;
    public static final int ijsop = 67;
    public static final int il = 83;
    public static final int illlbb = 32;
    public static final int illnl = 65;
    public static final int ilowsn = 48;
    public static final int imb = 52;
    public static final int imd = 56;
    public static final int ime = 78;
    public static final int iml = 54;
    public static final int ims = 53;
    public static final int imw = 55;
    public static final int imx = 57;
    public static final int inc = 81;
    public static final int ineic = 58;
    public static final int inorth = 27;
    public static final int inu = 80;
    public static final int inucl = 37;
    public static final int io = 11;
    public static final int io_ = 82;
    public static final int iother = 44;
    public static final int ipde = 59;
    public static final int ipostn = 39;
    public static final int ipostq = 42;
    public static final int ipren = 38;
    public static final int ipreq = 41;
    public static final int iqb = 70;
    public static final int iqb1 = 71;
    public static final int iqb2 = 72;
    public static final int iqbx = 73;
    public static final int iqmt = 74;
    public static final int iquake = 40;
    public static final int ir = 84;
    public static final int iradev = 25;
    public static final int iradnv = 23;
    public static final int ireb = 61;
    public static final int irldta = 49;
    public static final int irlim = 2;
    public static final int isro = 36;
    public static final int it = 85;
    public static final int it0 = 13;
    public static final int it1 = 14;
    public static final int it2 = 15;
    public static final int it3 = 16;
    public static final int it4 = 17;
    public static final int it5 = 18;
    public static final int it6 = 19;
    public static final int it7 = 20;
    public static final int it8 = 21;
    public static final int it9 = 22;
    public static final int itanev = 26;
    public static final int itannv = 24;
    // The enumerated header field values are stored in the header as integers.
    public static final int itime = 1;
    public static final int iu = 86;
    public static final int iunkn = 5;
    public static final int iunknown = 69;
    public static final int iup = 31;
    public static final int iuser = 68;
    public static final int iusgs = 62;
    public static final int ivel = 7;
    public static final int ivolts = 50;
    public static final int iwwsn1 = 33;
    public static final int iwwsn2 = 34;
    public static final int ixy = 4;
    public static final int ixyz = 51;
    static final int SACHWORDS = 158;
    static final int SACHBYTES = 4 * SACHWORDS;
    static final int SACHSWAP = 4 * 110;

    public static boolean isDefault(int variable) {
        return variable == INTDEFAULT;
    }

    public static boolean isDefault(float variable) {
        return variable == FLOATDEFAULT;
    }

    public static boolean isDefault(String variable) {
        return variable.trim().equals(STRINGDEFAULT.trim()); // trim white spaces
    }

    // utility routine for padding String quantities to correct length
    public static String padTo(String S, int length) {
        while (S.length() < length) {
            S += " ";
        }
        return S;
    }

    // utility routines
    /**
     * reproduces the ch allt modifications to the time variables
     *
     * @param value - the added time in seconds
     */
    public void chAllt(float value) {
        if (!isDefault(nzyear)) {
            TimeT orig = new TimeT(nzyear, nzjday, nzhour, nzmin, nzsec, nzmsec);
            orig.add(value);
            setTime(orig);
        }
    }

    /**
     * Equivalent to the ch command in SAC
     *
     * @param type : the header variable name
     * @param value: a String version of the value being assigned to the header
     */
    public void changeHeader(String type, String value) {
        if (type.equals("stla")) {
            stla = Float.parseFloat(value);
            validate();
        } else if (type.equals("stlo")) {
            stlo = Float.parseFloat(value);
            validate();
        } else if (type.equals("stel")) {
            stel = Float.parseFloat(value);
            validate();
        } else if (type.equals("stdp")) {
            stdp = Float.parseFloat(value);
            validate();
        } else if (type.equals("evla")) {
            evla = Float.parseFloat(value);
            validate();
        } else if (type.equals("evlo")) {
            evlo = Float.parseFloat(value);
            validate();
        } else if (type.equals("evel")) {
            evel = Float.parseFloat(value);
            validate();
        } else if (type.equals("evdp")) {
            evdp = Float.parseFloat(value);
            validate();
        } else if (type.equals("evid") || type.equals("nevid")) {
            nevid = Integer.parseInt(value);
        } else if (type.equals("nzyear")) {
            nzyear = Integer.parseInt(value);
        } else if (type.equals("nzjday")) {
            nzjday = Integer.parseInt(value);
        } else if (type.equals("nzhour")) {
            nzhour = Integer.parseInt(value);
        } else if (type.equals("nzmin")) {
            nzmin = Integer.parseInt(value);
        } else if (type.equals("nzsec")) {
            nzsec = Integer.parseInt(value);
        } else if (type.equals("nzmsec")) {
            nzmsec = Integer.parseInt(value);
        } else if (type.equals("kcmpnm")) {
            kcmpnm = value;
        } else if (type.equals("kstnm")) {
            kstnm = value;
        } else if (type.equals("kevnm")) {
            kevnm = value;
        } else {
            changeTimePick(type, value); // this tests for changes in the time pick headers
        }
    }

    /**
     * Equivalent to the ch command in SAC
     *
     * Checking two possibilities here 1.) generic: e.g. 'ch t0 100 ' will
     * change the "t0" header to 100 2.) specific:e.g. 'ch Lg 100 ' will change
     * all picks labelled "Lg" to 100
     *
     * @param type : the time pick header variable name
     * @param value: a String version of the float value being assigned to the
     * header
     */
    public void changeTimePick(String type, String value) {
        // Time pick headers
        StringTokenizer st = new StringTokenizer(type, "t");

        try {
            int ii = Integer.parseInt(st.nextToken());
            t[ii] = Float.parseFloat(value);
            if (isDefault(kt[ii])) {
                kt[ii] = "t" + ii;
            }
        } catch (Exception e) {
            for (int ii = 0; ii < 10; ii++) {
                if (kt[ii].trim().equals(type)) {
                    t[ii] = Float.parseFloat(value);
                    break;
                }
            }
        }
    }

    // accessor
    public boolean checkByteSwap() {
        return swapBytes;
    }

    /**
     * Clear all designated time picks and their identifiers resets to default
     * the values: t[0-10], kt[0-10], tf and kf
     */
    public void clearTimePicks() {
        t[0] = FLOATDEFAULT;
        t[1] = FLOATDEFAULT;
        t[2] = FLOATDEFAULT;
        t[3] = FLOATDEFAULT;
        t[4] = FLOATDEFAULT;
        t[5] = FLOATDEFAULT;
        t[6] = FLOATDEFAULT;
        t[7] = FLOATDEFAULT;
        t[8] = FLOATDEFAULT;
        t[9] = FLOATDEFAULT;
        f = FLOATDEFAULT;

        kt[0] = STRINGDEFAULT;
        kt[1] = STRINGDEFAULT;
        kt[2] = STRINGDEFAULT;
        kt[3] = STRINGDEFAULT;
        kt[4] = STRINGDEFAULT;
        kt[5] = STRINGDEFAULT;
        kt[6] = STRINGDEFAULT;
        kt[7] = STRINGDEFAULT;
        kt[8] = STRINGDEFAULT;
        kt[9] = STRINGDEFAULT;
        kf = STRINGDEFAULT;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    /**
     * method to replace an undefined evid with a unique number based on the
     * origin or reference time
     *
     * @return the nevid value
     */
    public int createEvid() {
        if (isDefault(nevid)) {
            TimeT timet = getOriginTime(); // first check for a defined origin - if exists, use that as the evid
            if (timet == null) {
                timet = getReferenceTime();// if origin is not defined, use the file's zero time
                if (timet == null) {
                    timet = new TimeT(0.); //todo note this probably makes no sense
                }
            }

            nevid = (int) timet.getEpochTime();  // resulting evid will be the epochtime in seconds
        }
        return nevid;
    }

    /**
     * Get the begin Epoch time as a TimeT object
     */
    public TimeT getBeginTime() {
        if (isDefault(b)) {
            return null; // b should always be set.
        }
        TimeT reftime = getReferenceTime();
        if (reftime == null) {
            reftime = new TimeT(0.);
        }
        return reftime.add(b);
    }

    /**
     * Get the end Epoch time as a TimeT object
     */
    public TimeT getEndTime() {
        TimeT begintime = getBeginTime();

        if (begintime == null) {
            return null;
        }

        float lengthInSeconds = (npts - 1.f) * delta;
        TimeT endtime = begintime.add(lengthInSeconds);
        return endtime;
    }

    public GeodeticCoordinate getEventCoordinates() throws Exception {
        GeodeticCoordinate result = new GeodeticCoordinate(evla, evlo, evdp, evel);
        return result;
    }

    public float getFloatDefault() {
        return FLOATDEFAULT;
    }

    // get methods
    public String getItype(int itype) {
        // file types
        if (itype == itime) {
            return "time";
        } else if (itype == irlim) {
            return "rlim";
        } else if (itype == iamph) {
            return "amph";
        } else if (itype == ixy) {
            return "xy";
        } else if (itype == ixyz) {
            return "xyz";
        } // dependent variable types
        else if (itype == iunkn) {
            return "unknown";
        } else if (itype == idisp) {
            return "displacement";
        } else if (itype == ivel) {
            return "velocity";
        } else if (itype == iacc) {
            return "acceleration";
        } else if (itype == ivolts) {
            return "volts";
        } // magnitude types
        else if (itype == imb) {
            return "Mb";
        } else if (itype == ims) {
            return "Ms";
        } else if (itype == iml) {
            return "Ml";
        } else if (itype == imw) {
            return "Mw";
        } else if (itype == imd) {
            return "Md";
        } else if (itype == imx) {
            return "Mx";
        } else {
            return "";
        }
    }

    public Iztype getIztype() {
        return iztype;
    }

    /**
     * Get the origin Epoch time as a TimeT object
     */
    public TimeT getOriginTime() {
        if (isDefault(o)) {
            return null;
        }

        TimeT reftime = getReferenceTime();
        if (reftime == null) {
            reftime = new TimeT(0.);
        }
        return reftime.add(o);
    }

    /**
     * Convert the date header fields into a TimeT object Note: if the nzyear...
     * variables are not set - return null
     */
    public TimeT getReferenceTime() {
        if (isDefault(nzyear) || isDefault(nzjday) || isDefault(nzhour) || isDefault(nzmin) || isDefault(nzsec) || isDefault(nzmsec)) {
            return null;
            //return new TimeT(0.);
        } else {
            return new TimeT(nzyear, nzjday, nzhour, nzmin, nzsec, nzmsec);
        }
    }

    public GeodeticCoordinate getStationCoordinates() throws Exception {
        GeodeticCoordinate result = new GeodeticCoordinate(stla, stlo, stdp, stel);
        return result;
    }

    /**
     * A simple method for retrieving pick times and names
     *
     * @param index
     *
     * Object[] timepick = sfh.getTimePick(0); Float value = (Float)
     * timepick[0]; String pickname = (String) timepick[1];
     */
    public Object[] getTimePick(int index) {
        Object[] result = new Object[2];
        Float value = t[index];
        String pickname = kt[index];

        result[0] = value;
        result[1] = pickname;

        return result;
    }

    /**
     * Look for a specific pick Note both kt[] and t[] must be defined for this
     * to return successfully
     *
     * e.g. float time = getTimePick("Pn");
     *
     * @param phase the name of the phase searched for - kt[ii] must be the same
     * - including capitalization
     * @return the pick time value, or the default value
     */
    public float getTimePick(String phase) {
        float result = FLOATDEFAULT;

        for (int ii = 0; ii < 10; ii++) {
            if (!isDefault(t[ii])) {
                if (kt[ii].trim().equals(phase)) {
                    return t[ii];
                }
            }
        }
        return result;
    }

    /**
     * Collect all the Time Picks as a Vector of Strings
     *
     * pickstring = "Pn 452.2"
     *
     * To extract the result use StringTokenizer;
     */
    public ArrayList getTimePicks() // todo change this to a <String,Float> object
    {
        ArrayList result = new ArrayList();
        for (int ii = 0; ii < 10; ii++) {
            if (!isDefault(t[ii])) {
                String pickstring = kt[ii].trim() + " " + t[ii];
                result.add(pickstring);
            }
        }
        return result;
    }

    /**
     * Make a treemap of all the defined variables
     *
     * @return a treemap of defined variables
     */
    public TreeMap<String, Object> getVariableMap() {
        TreeMap<String, Object> result = new TreeMap<String, Object>();

        if (!isDefault(delta)) {
            result.put("delta", delta);
        }
        if (!isDefault(b)) {
            result.put("begintime", getBeginTime());
        }
        if (!isDefault(o)) {
            result.put("origintime", getOriginTime());
        }
        if (!isDefault(nzyear)) {
            result.put("referencetime", getReferenceTime());
        }

        if (!isDefault(iftype)) {
            result.put("filetype", getItype(iftype));
        }
        if (!isDefault(idep)) {
            result.put("depvariabletype", getItype(idep));
        }
        if (!isDefault(imagtyp)) {
            result.put("magtype", getItype(imagtyp));
        }

        if (!isDefault(xminimum)) {
            result.put("xminimum", xminimum);
        }
        if (!isDefault(xmaximum)) {
            result.put("xmaximum", xmaximum);
        }
        if (!isDefault(yminimum)) {
            result.put("yminimum", yminimum);
        }
        if (!isDefault(ymaximum)) {
            result.put("ymaximum", ymaximum);
        }

        if (!isDefault(a)) {
            result.put("a", a);
        }
        if (!isDefault(f)) {
            result.put("f", f);
        }
        if (!isDefault(scale)) {
            result.put("scale", scale);
        }
        if (!isDefault(odelta)) {
            result.put("odelta", odelta);
        }
        if (!isDefault(sb)) {
            result.put("sb", sb);
        }
        if (!isDefault(sdelta)) {
            result.put("sdelta", sdelta);
        }
        if (!isDefault(fmt)) {
            result.put("fmt", fmt);
        }

        if (!isDefault(stla)) {
            result.put("stla", stla);
        }
        if (!isDefault(stlo)) {
            result.put("stlo", stlo);
        }
        if (!isDefault(stdp)) {
            result.put("stdp", stdp);
        }
        if (!isDefault(stel)) {
            result.put("stel", stel);
        }

        if (!isDefault(evla)) {
            result.put("evla", evla);
        }
        if (!isDefault(evlo)) {
            result.put("evlo", evlo);
        }
        if (!isDefault(evdp)) {
            result.put("evdp", evdp);
        }
        if (!isDefault(evel)) {
            result.put("evel", evel);
        }
        if (!isDefault(mag)) {
            result.put("mag", mag);
        }

        if (!isDefault(dist)) {
            result.put("dist", dist);
        }
        if (!isDefault(az)) {
            result.put("az", az);
        }

        if (!isDefault(cmpaz)) {
            result.put("cmpaz", cmpaz);
        }
        if (!isDefault(cmpinc)) {
            result.put("cmpinc", cmpinc);
        }

        for (int ii = 0; ii < 10; ii++) {
            if (!isDefault(t[ii])) {
                result.put("t" + ii, t[ii]);
            }
            if (!isDefault(kt[ii])) {
                result.put("kt" + ii, kt[ii]);
            }
            if (!isDefault(user[ii])) {
                result.put("user" + ii, user[ii]);
            }
            if (!isDefault(resp[ii])) {
                result.put("resp" + ii, resp[ii]);
            }
        }

        if (!isDefault(nvhdr)) {
            result.put("nvhdr", nvhdr);
        }
        if (!isDefault(norid)) {
            result.put("norid", norid);
        }
        if (!isDefault(nevid)) {
            result.put("nevid", nevid);
        }
        if (!isDefault(nsnpts)) {
            result.put("nsnpts", nsnpts);
        }
        if (!isDefault(nwfid)) {
            result.put("nwfid", nwfid);
        }
        if (!isDefault(nxsize)) {
            result.put("nxsize", nxsize);
        }
        if (!isDefault(nysize)) {
            result.put("nysize", nysize);
        }

        if (!isDefault(iinst)) {
            result.put("iinst", iinst);
        }
        if (!isDefault(instreg)) {
            result.put("instreg", instreg);
        }
        if (!isDefault(ievreg)) {
            result.put("ievreg", ievreg);
        }
        if (!isDefault(ievtyp)) {
            result.put("ievtyp", ievtyp);
        }
        if (!isDefault(iqual)) {
            result.put("iqual", iqual);
        }
        if (!isDefault(isynth)) {
            result.put("isynth", isynth);
        }
        if (!isDefault(imagsrc)) {
            result.put("imagsrc", imagsrc);
        }

        if (!isDefault(kstnm)) {
            result.put("kstnm", kstnm.trim());
        }
        if (!isDefault(kevnm)) {
            result.put("kevnm", kevnm.trim());
        }
        if (!isDefault(kcmpnm)) {
            result.put("kcmpnm", kcmpnm.trim());
        }
        if (!isDefault(knetwk)) {
            result.put("knetwk", knetwk.trim());
        }
        if (!isDefault(kdatrd)) {
            result.put("kdatrd", kdatrd.trim());
        }
        if (!isDefault(kinst)) {
            result.put("kinst", kinst.trim());
        }
        if (!isDefault(khole)) {
            result.put("khole", khole.trim());
        }
        if (!isDefault(ko)) {
            result.put("ko", ko.trim());
        }
        if (!isDefault(ka)) {
            result.put("ka", ka.trim());
        }
        if (!isDefault(kf)) {
            result.put("kf", kf.trim());
        }
        if (!isDefault(kuser0)) {
            result.put("kuser0", kuser0.trim());
        }
        if (!isDefault(kuser1)) {
            result.put("kuser1", kuser1.trim());
        }
        if (!isDefault(kuser2)) {
            result.put("kuser2", kuser2.trim());
        }

        // Note SAC retains several "unused" variables that normally will be default valued but may be defined by users
        if (!isDefault(unused6)) {
            result.put("unused6", unused6);
        }
        if (!isDefault(unused7)) {
            result.put("unused7", unused7);
        }
        if (!isDefault(unused8)) {
            result.put("unused8", unused8);
        }
        if (!isDefault(unused9)) {
            result.put("unused9", unused9);
        }
        if (!isDefault(unused10)) {
            result.put("unused10", unused10);
        }
        if (!isDefault(unused11)) {
            result.put("unused11", unused11);
        }
        if (!isDefault(unused12)) {
            result.put("unused12", unused12);
        }

        if (!isDefault(unused15)) {
            result.put("unused15", unused15);
        }
        if (!isDefault(unused16)) {
            result.put("unused16", unused16);
        }
        if (!isDefault(unused19)) {
            result.put("unused19", unused19);
        }
        if (!isDefault(unused20)) {
            result.put("unused20", unused20);
        }
        if (!isDefault(unused21)) {
            result.put("unused21", unused21);
        }
        if (!isDefault(unused22)) {
            result.put("unused22", unused22);
        }
        if (!isDefault(unused23)) {
            result.put("unused23", unused23);
        }
        if (!isDefault(unused24)) {
            result.put("unused24", unused24);
        }
        if (!isDefault(unused25)) {
            result.put("unused25", unused25);
        }
        if (!isDefault(unused26)) {
            result.put("unused26", unused26);
        }
        if (!isDefault(unused27)) {
            result.put("unused27", unused27);
        }

        if (leven == 0) {
            result.put("leven", false);
        } else {
            result.put("leven", true);
        }

        return result;
    }

    /**
     * Equivalent to the lh command in SAC
     *
     * @param type : the header variable name
     * @return result: a String version of the value being assigned to the
     * header
     */
    public String listHeader(String type) {
        if (type.equals("all")) {
            StringBuffer sb = new StringBuffer();
            TreeMap variablemap = getVariableMap();
            Iterator iterator = variablemap.keySet().iterator();
            while (iterator.hasNext()) {
                String variable = (String) iterator.next();
                Object value = variablemap.get(variable);

                sb.append(variable + ": " + value + "\n");

            }
            return sb.toString();
        } else {
            TreeMap variablemap = getVariableMap();
            Object value = variablemap.get(type);

            if (value != null) {
                return type + ": " + value + "\n";
            } else {
                return "no such variable";
            }
        }
    }

    /**
     * Calculate the source-receiver azimuth and backazimuth if they aren't
     * calculated already
     */
    public void setAzimuths() {
        if ((az == FLOATDEFAULT) || (baz == FLOATDEFAULT)) {
            if ((stla != FLOATDEFAULT) && (stlo != FLOATDEFAULT) && (evla != FLOATDEFAULT) && (evlo != FLOATDEFAULT)) {
                az = (float) EModel.getAzimuth(evla, evlo, stla, stlo);
                baz = (float) EModel.getAzimuth(stla, stlo, evla, evlo);
            }
        }
    }

    /**
     * b is defined by the start of the seismogram relative to the reference
     * time (nzyear, ...,nzmsec) NOTE the reference time should be already be
     * defined
     */
    public void setBeginTime(TimeT begintime) {
        TimeT reftime = getReferenceTime();

        if (reftime == null) {
            reftime = getOriginTime();           //todo these checks aren't totally consistent.
        }
        if (reftime == null) {
            reftime = new TimeT(begintime);
        }

        b = (float) begintime.subtract(reftime).getEpochTime();
        validate();
    }

    /**
     * Calculate the source-receiver distance if it isn't calculated already
     */
    public void setDistances() {
        if ((dist == FLOATDEFAULT) || (gcarc == FLOATDEFAULT)) {
            if ((stla != FLOATDEFAULT) && (stlo != FLOATDEFAULT) && (evla != FLOATDEFAULT) && (evlo != FLOATDEFAULT)) {
                dist = (float) EModel.getDistance(stla, stlo, evla, evlo);
                gcarc = (float) EModel.getGreatCircleDelta(stla, stlo, evla, evlo);
            }
        }
    }

    public void setEventLocation(float evla, float evlo, float evel, float evdp) {
        this.evla = evla;
        this.evlo = evlo;
        this.evel = evel;
        this.evdp = evdp;
        syncStationAndEvent();
    }

    public int setItype(String type) {
        int result;

        if (type.equals("time")) {
            result = itime;
        } else if (type.equals("rlim")) {
            result = irlim;
        } else if (type.equals("amph")) {
            result = iamph;
        } else if (type.equals("xy")) {
            result = ixy;
        } else if (type.equals("xyz")) {
            result = ixyz;
        } else if (type.equals("unknown")) {
            result = iunkn;
        } else if (type.equals("displacement")) {
            result = idisp;
        } else if (type.equals("velocity")) {
            result = ivel;
        } else if (type.equals("acceleration")) {
            result = iacc;
        } else if (type.equals("volts")) {
            result = ivolts;
        } else if (type.equals("Mb")) {
            result = imb;
        } else if (type.equals("Ms")) {
            result = ims;
        } else if (type.equals("Ml")) {
            result = iml;
        } else if (type.equals("Mw")) {
            result = imw;
        } else if (type.equals("Md")) {
            result = imd;
        } else if (type.equals("Mx")) {
            result = imx;
        } else {
            result = INTDEFAULT;
        }

        return result;
    }

    public void setIztype(Iztype iztype) {
        this.iztype = iztype;
    }

    /**
     * Set the origin time relative to the sac file's reference time
     *
     * @param origintime - a TimeT version of the event
     */
    public void setOriginTime(TimeT origintime) {
        TimeT reftime = getReferenceTime();
        if (reftime == null) {
            reftime = origintime;              //todo these checks aren't totally consistent.
        }
        o = (float) origintime.subtract(reftime).getEpochTime();
    }

    public void setStationLocation(float stla, float stlo, float stel, float stdp) {
        this.stla = stla;
        this.stlo = stlo;
        this.stel = stel;
        this.stdp = stdp;
        syncStationAndEvent();
    }

    /**
     * Set the nz(date) fields based on a TimeT object
     *
     * @param timet : The time as a TimeT object
     */
    public void setTime(TimeT timet) {
        // changing the reference time value will affect all the relative time entries (b,e,o,t[] etc.)
        TimeT referenceTime = getReferenceTime();  // can be null

        try {
            this.nzyear = timet.getYear();
            this.nzjday = timet.getDayOfYear();
            this.nzhour = timet.getHour();
            this.nzmin = timet.getMinute();
            this.nzsec = timet.getCalendarSecond();
            this.nzmsec = timet.getCalendarMilliSecond();
        } catch (Exception e) {
            ;
        }

        // change the b,e,o,... times  so that they are relative to the new reference time
        if (referenceTime != null) {
            float refTimeShift = (float) referenceTime.subtract(getReferenceTime()).getEpochTime();

            if (!isDefault(o)) {
                o += refTimeShift;
            }
            if (!isDefault(a)) {
                b += refTimeShift;
            }
            if (!isDefault(a)) {
                a += refTimeShift;
            }
            if (!isDefault(f)) {
                f += refTimeShift;
            }
            if (!isDefault(e)) {
                e += refTimeShift;
            }

            for (int ii = 0; ii < 10; ii++) {
                if (!isDefault(t[ii])) {
                    t[ii] += refTimeShift;
                }
            }
        }
    }

    /**
     * A simple method for defining pick times and names
     *
     * @param index
     * @param value
     * @param pickname
     */
    public void setTimePick(int index, float value, String pickname) {
        if (index >= 0 && index < 10) {
            t[index] = value;
            kt[index] = pickname;
        }
    }

    /**
     * assign variables based on a treemap
     *
     * @param variablemap contains all the variables that are not default valued
     */
    public void setVariableMap(TreeMap<String, Object> variablemap) {
        Iterator iterator = variablemap.keySet().iterator();

        // Need to set the reference time before setting any of it's dependent's (b,o,t1...)
        TimeT referencetime = (TimeT) variablemap.get("referencetime");
        setTime(referencetime);

        while (iterator.hasNext()) {
            String variable = (String) iterator.next();
            Object value = variablemap.get(variable);

            try {
                for (int ii = 0; ii < 10; ii++) {
                    if (variable.equals("t" + ii)) {
                        t[ii] = (Float) value;
                    } else if (variable.equals("kt" + ii)) {
                        kt[ii] = (String) value;
                    } else if (variable.equals("user" + ii)) {
                        try {
                            user[ii] = (Float) value;
                        } catch (Exception e) {
                            Float result = Float.parseFloat((String) value);
                            user[ii] = result;
                        }
                    } else if (variable.equals("resp" + ii)) {
                        resp[ii] = (Float) value;
                    }
                }

                if (variable.equals("delta")) {
                    delta = (Float) value;
                } else if (variable.equals("begintime")) {
                    setBeginTime((TimeT) value);
                } else if (variable.equals("origintime")) {
                    setOriginTime((TimeT) value);
                } //else if (variable.equals("referencetime")) setTime((TimeT) value);
                // Note filetype, etc are text valued (e.g. "time")
                // these get converted to the equivalent SAC integers using setItype()
                else if (variable.equals("filetype")) {
                    iftype = setItype((String) value);
                } else if (variable.equals("depvariabletype")) {
                    idep = setItype((String) value);
                } else if (variable.equals("magtype")) {
                    imagtyp = setItype((String) value);
                } else if (variable.equals("xminimum")) {
                    xminimum = (Float) value;
                } else if (variable.equals("xmaximum")) {
                    xmaximum = (Float) value;
                } else if (variable.equals("yminimum")) {
                    yminimum = (Float) value;
                } else if (variable.equals("ymaximum")) {
                    ymaximum = (Float) value;
                } else if (variable.equals("a")) {
                    a = (Float) value;
                } else if (variable.equals("f")) {
                    f = (Float) value;
                } else if (variable.equals("scale")) {
                    scale = (Float) value;
                } else if (variable.equals("odelta")) {
                    odelta = (Float) value;
                } else if (variable.equals("sb")) {
                    sb = (Float) value;
                } else if (variable.equals("sdelta")) {
                    sdelta = (Float) value;
                } else if (variable.equals("fmt")) {
                    fmt = (Float) value;
                } else if (variable.equals("stla")) {
                    stla = (Float) value;
                } else if (variable.equals("stlo")) {
                    stlo = (Float) value;
                } else if (variable.equals("stdp")) {
                    stdp = (Float) value;
                } else if (variable.equals("stel")) {
                    stel = (Float) value;
                } else if (variable.equals("evla")) {
                    evla = (Float) value;
                } else if (variable.equals("evlo")) {
                    evlo = (Float) value;
                } else if (variable.equals("evdp")) {
                    evdp = (Float) value;
                } else if (variable.equals("evel")) {
                    evel = (Float) value;
                } else if (variable.equals("mag")) {
                    mag = (Float) value;
                } else if (variable.equals("dist") || variable.equals("distance")) // TODO note dist will be recalculated if stla, stlo, evla, evlo are set
                {
                    dist = (Float) value;
                } else if (variable.equals("az") || variable.equals("azimuth")) // TODO note dist will be recalculated if stla, stlo, evla, evlo are set
                {
                    az = (Float) value;
                } else if (variable.equals("cmpaz")) {
                    cmpaz = (Float) value;
                } else if (variable.equals("cmpinc")) {
                    cmpinc = (Float) value;
                } else if (variable.equals("nvhdr")) {
                    nvhdr = (Integer) value;
                } else if (variable.equals("norid")) {
                    norid = (Integer) value;
                } else if (variable.equals("nevid")) {
                    nevid = (Integer) value;
                } else if (variable.equals("nsnpts")) {
                    nsnpts = (Integer) value;
                } else if (variable.equals("nwfid")) {
                    nwfid = (Integer) value;
                } else if (variable.equals("nxsize")) {
                    nxsize = (Integer) value;
                } else if (variable.equals("nysize")) {
                    nysize = (Integer) value;
                } else if (variable.equals("iztype")) {
                    iztype = (Iztype) value;
                } else if (variable.equals("iinst")) {
                    iinst = (Integer) value;
                } else if (variable.equals("instreg")) {
                    instreg = (Integer) value;
                } else if (variable.equals("ievreg")) {
                    ievreg = (Integer) value;
                } else if (variable.equals("ievtyp")) {
                    ievtyp = (Integer) value;
                } else if (variable.equals("iqual")) {
                    iqual = (Integer) value;
                } else if (variable.equals("isynth")) {
                    isynth = (Integer) value;
                } else if (variable.equals("imagsrc")) {
                    imagsrc = (Integer) value;
                } else if (variable.equals("kstnm")) {
                    kstnm = (String) value;
                } else if (variable.equals("kevnm")) {
                    kevnm = (String) value;
                } else if (variable.equals("kcmpnm")) {
                    kcmpnm = (String) value;
                } else if (variable.equals("knetwk")) {
                    knetwk = (String) value;
                } else if (variable.equals("kdatrd")) {
                    kdatrd = (String) value;
                } else if (variable.equals("kinst")) {
                    kinst = (String) value;
                } else if (variable.equals("khole")) {
                    khole = (String) value;
                } else if (variable.equals("ko")) {
                    ko = (String) value;
                } else if (variable.equals("ka")) {
                    ka = (String) value;
                } else if (variable.equals("kf")) {
                    kf = (String) value;
                } else if (variable.equals("kuser0")) {
                    kuser0 = (String) value;
                } else if (variable.equals("kuser1")) {
                    kuser1 = (String) value;
                } else if (variable.equals("kuser2")) {
                    kuser2 = (String) value;
                } // Note SAC's "unused" values may have been redefined by users
                else if (variable.equals("unused6")) {
                    unused6 = (Float) value;
                } else if (variable.equals("unused7")) {
                    unused7 = (Float) value;
                } else if (variable.equals("unused8")) {
                    unused8 = (Float) value;
                } else if (variable.equals("unused9")) {
                    unused9 = (Float) value;
                } else if (variable.equals("unused10")) {
                    unused10 = (Float) value;
                } else if (variable.equals("unused11")) {
                    unused11 = (Float) value;
                } else if (variable.equals("unused12")) {
                    unused12 = (Float) value;
                } else if (variable.equals("unused15")) {
                    unused15 = (Integer) value;
                } else if (variable.equals("unused16")) {
                    unused16 = (Integer) value;
                } else if (variable.equals("unused19")) {
                    unused19 = (Integer) value;
                } else if (variable.equals("unused20")) {
                    unused20 = (Integer) value;
                } else if (variable.equals("unused21")) {
                    unused21 = (Integer) value;
                } else if (variable.equals("unused22")) {
                    unused22 = (Integer) value;
                } else if (variable.equals("unused23")) {
                    unused23 = (Integer) value;
                } else if (variable.equals("unused24")) {
                    unused24 = (Integer) value;
                } else if (variable.equals("unused25")) {
                    unused25 = (Integer) value;
                } else if (variable.equals("unused26")) {
                    unused26 = (Integer) value;
                } else if (variable.equals("unused27")) {
                    unused27 = (Integer) value;
                } // Note logical variable "leven" is Boolean in the treemap
                // gets converted to SAC's integer equivalent
                else if (variable.equals("leven")) {
                    Boolean evenvalued = (Boolean) value;
                    if (evenvalued) {
                        leven = 1;
                    } else {
                        leven = 0;
                    }
                }
            } catch (Exception e) {
                try {
                    changeHeader(variable, (String) value); // todo this is a patch - not optimal solution
                } catch (Exception ee) {
                    System.out.println(variable + " " + value + " problem");
                }
            }
        }
        validate(); // ensure that required fields are set and that calculated values (dist, baz, gcarc etc.) are correct
    }

    /**
     * Certain variables are interrelated - make sure that any changes are
     * applied to each
     */
    public void validate() //TODO check where this is being used
    {
        // First verify that all the required fields are correctly set
        checkRequiredFields();
        syncStationAndEvent();
    }

    /**
     * Write the header at the start of a RandomAccessFile Note the
     * sacout.length() at the conclusion will be 158*4 = 632. Data is written
     * starting at sacout.seek(632)
     *
     * @param sacout
     */
    public void write(RandomAccessFile sacout) {

        try {
            sacout.seek(0);
        } catch (IOException ex) {
            Logger.getLogger(SACHeader.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex.getMessage());
        }

        write((DataOutput) sacout);
    }

    public void write(DataOutput outputStream) {
        try {

            // make e and b consistent
            //e = b + delta * ((float) (npts - 1));
            validate();

            outputStream.writeFloat(delta);
            outputStream.writeFloat(depmin);
            outputStream.writeFloat(depmax);
            outputStream.writeFloat(scale);
            outputStream.writeFloat(odelta);
            outputStream.writeFloat(b);
            outputStream.writeFloat(e);
            outputStream.writeFloat(o);
            outputStream.writeFloat(a);
            outputStream.writeFloat(fmt);
            outputStream.writeFloat(t[0]);
            outputStream.writeFloat(t[1]);
            outputStream.writeFloat(t[2]);
            outputStream.writeFloat(t[3]);
            outputStream.writeFloat(t[4]);
            outputStream.writeFloat(t[5]);
            outputStream.writeFloat(t[6]);
            outputStream.writeFloat(t[7]);
            outputStream.writeFloat(t[8]);
            outputStream.writeFloat(t[9]);
            outputStream.writeFloat(f);
            outputStream.writeFloat(resp[0]);
            outputStream.writeFloat(resp[1]);
            outputStream.writeFloat(resp[2]);
            outputStream.writeFloat(resp[3]);
            outputStream.writeFloat(resp[4]);
            outputStream.writeFloat(resp[5]);
            outputStream.writeFloat(resp[6]);
            outputStream.writeFloat(resp[7]);
            outputStream.writeFloat(resp[8]);
            outputStream.writeFloat(resp[9]);
            outputStream.writeFloat(stla);
            outputStream.writeFloat(stlo);
            outputStream.writeFloat(stel);
            outputStream.writeFloat(stdp);
            outputStream.writeFloat(evla);
            outputStream.writeFloat(evlo);
            outputStream.writeFloat(evel);
            outputStream.writeFloat(evdp);
            outputStream.writeFloat(mag);
            outputStream.writeFloat(user[0]);
            outputStream.writeFloat(user[1]);
            outputStream.writeFloat(user[2]);
            outputStream.writeFloat(user[3]);
            outputStream.writeFloat(user[4]);
            outputStream.writeFloat(user[5]);
            outputStream.writeFloat(user[6]);
            outputStream.writeFloat(user[7]);
            outputStream.writeFloat(user[8]);
            outputStream.writeFloat(user[9]);
            outputStream.writeFloat(dist);
            outputStream.writeFloat(az);
            outputStream.writeFloat(baz);
            outputStream.writeFloat(gcarc);
            outputStream.writeFloat(sb);
            outputStream.writeFloat(sdelta);
            outputStream.writeFloat(depmen);
            outputStream.writeFloat(cmpaz);
            outputStream.writeFloat(cmpinc);
            outputStream.writeFloat(xminimum);
            outputStream.writeFloat(xmaximum);
            outputStream.writeFloat(yminimum);
            outputStream.writeFloat(ymaximum);
            outputStream.writeFloat(unused6);
            outputStream.writeFloat(unused7);
            outputStream.writeFloat(unused8);
            outputStream.writeFloat(unused9);
            outputStream.writeFloat(unused10);
            outputStream.writeFloat(unused11);
            outputStream.writeFloat(unused12);
            outputStream.writeInt(nzyear);
            outputStream.writeInt(nzjday);
            outputStream.writeInt(nzhour);
            outputStream.writeInt(nzmin);
            outputStream.writeInt(nzsec);
            outputStream.writeInt(nzmsec);
            outputStream.writeInt(nvhdr);
            outputStream.writeInt(norid);
            outputStream.writeInt(nevid);
            outputStream.writeInt(npts);
            outputStream.writeInt(nsnpts);
            outputStream.writeInt(nwfid);
            outputStream.writeInt(nxsize);
            outputStream.writeInt(nysize);
            outputStream.writeInt(unused15);
            outputStream.writeInt(iftype);
            outputStream.writeInt(idep);
            outputStream.writeInt(getIztype().getCode());
            outputStream.writeInt(unused16);
            outputStream.writeInt(iinst);
            outputStream.writeInt(instreg);
            outputStream.writeInt(ievreg);
            outputStream.writeInt(ievtyp);
            outputStream.writeInt(iqual);
            outputStream.writeInt(isynth);
            outputStream.writeInt(imagtyp);
            outputStream.writeInt(imagsrc);
            outputStream.writeInt(unused19);
            outputStream.writeInt(unused20);
            outputStream.writeInt(unused21);
            outputStream.writeInt(unused22);
            outputStream.writeInt(unused23);
            outputStream.writeInt(unused24);
            outputStream.writeInt(unused25);
            outputStream.writeInt(unused26);
            outputStream.writeInt(leven);
            outputStream.writeInt(lpspol);
            outputStream.writeInt(lovrok);
            outputStream.writeInt(lcalda);
            outputStream.writeInt(unused27);
            kstnm = padTo(kstnm, 8);
            outputStream.writeBytes(kstnm);
            kevnm = padTo(kevnm, 16);
            outputStream.writeBytes(kevnm);
            khole = padTo(khole, 8);
            outputStream.writeBytes(khole);
            ko = padTo(ko, 8);
            outputStream.writeBytes(ko);
            ka = padTo(ka, 8);
            outputStream.writeBytes(ka);
            kt[0] = padTo(kt[0], 8);
            outputStream.writeBytes(kt[0]);
            kt[1] = padTo(kt[1], 8);
            outputStream.writeBytes(kt[1]);
            kt[2] = padTo(kt[2], 8);
            outputStream.writeBytes(kt[2]);
            kt[3] = padTo(kt[3], 8);
            outputStream.writeBytes(kt[3]);
            kt[4] = padTo(kt[4], 8);
            outputStream.writeBytes(kt[4]);
            kt[5] = padTo(kt[5], 8);
            outputStream.writeBytes(kt[5]);
            kt[6] = padTo(kt[6], 8);
            outputStream.writeBytes(kt[6]);
            kt[7] = padTo(kt[7], 8);
            outputStream.writeBytes(kt[7]);
            kt[8] = padTo(kt[8], 8);
            outputStream.writeBytes(kt[8]);
            kt[9] = padTo(kt[9], 8);
            outputStream.writeBytes(kt[9]);
            kf = padTo(kf, 8);
            outputStream.writeBytes(kf);
            kuser0 = padTo(kuser0, 8);
            outputStream.writeBytes(kuser0);
            kuser1 = padTo(kuser1, 8);
            outputStream.writeBytes(kuser1);
            kuser2 = padTo(kuser2, 8);
            outputStream.writeBytes(kuser2);
            kcmpnm = padTo(kcmpnm, 8);
            outputStream.writeBytes(kcmpnm);
            knetwk = padTo(knetwk, 8);
            outputStream.writeBytes(knetwk);
            kdatrd = padTo(kdatrd, 8);
            outputStream.writeBytes(kdatrd);
            kinst = padTo(kinst, 8);
            outputStream.writeBytes(kinst);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe.getMessage());
        }
    }

    /**
     * Check that all the required fields are set correctly (not default or
     * unrealistic values)
     *
     * npts, nvhdr, b, e, iftype, leven and delta are required fields.
     */
    private void checkRequiredFields() {
        if (npts < 0) {
            //System.out.println("Warning npts = " + npts + "\nbeing changed to npts = " + 0);
            npts = 0;
        }

        if (nvhdr < 0) {
            //System.out.println("Warning nvhdr = " + nvhdr + "\nbeing changed to nvhdr = " + 6);
            nvhdr = 6;
        }

        if (b == FLOATDEFAULT) {
            //System.out.println("Warning b = " + b + "\nbeing changed to b = " + 0);
            b = 0.f;
        }

        if (iftype < 0) {
            //System.out.println("Warning iftype = " + iftype + "\nbeing changed to iftype = 'itime'");
            iftype = itime;
        }

        if (!(leven == 0) && !(leven == 1)) {
            //System.out.println("Warning leven = " + leven + "\nbeing changed to leven = true");
            leven = 1;
        }

        if (delta == FLOATDEFAULT || delta == 0.f) {
            //System.out.println("Warning delta = " + delta + "\nbeing changed to delta = " + 1);
            delta = 1.f;
        }

        // make e and b consistent
        e = b + delta * (npts - 1);
    }

    private void setFieldsFromByteArray(byte[] headerImage) throws IOException {
        //  The header image is read into a byte array, and the nvhdr field is
        //    range tested to sense the byte order of the file.  If byte
        //    swapping is indicated, then the header bytes are swapped in
        //    the byte buffer prior to decoding the header fields.
        DataInputStream bstream = new DataInputStream(new ByteArrayInputStream(headerImage));
        bstream.mark(SACHBYTES);
        bstream.skipBytes(4 * 76);
        nvhdr = bstream.readInt();     // test nvhdr
        bstream.reset();

        if (nvhdr >= 0 && nvhdr <= 6) {
            swapBytes = false;
        } else {

            swapBytes = true;
            bstream.close();
            byte abyte;
            for (int i = 0; i < SACHSWAP; i += 4) {        // swapping method
                abyte = headerImage[i];                            // per Phil Crotwell's
                headerImage[i] = headerImage[i + 3];             // suggestion
                headerImage[i + 3] = abyte;
                abyte = headerImage[i + 1];
                headerImage[i + 1] = headerImage[i + 2];
                headerImage[i + 2] = abyte;
            }
            bstream = new DataInputStream(new ByteArrayInputStream(headerImage));
        }

//  now decode header
        delta = bstream.readFloat();
        depmin = bstream.readFloat();
        depmax = bstream.readFloat();
        scale = bstream.readFloat();
        odelta = bstream.readFloat();
        b = bstream.readFloat();
        e = bstream.readFloat();
        o = bstream.readFloat();
        a = bstream.readFloat();
        fmt = bstream.readFloat();
        t[0] = bstream.readFloat();
        t[1] = bstream.readFloat();
        t[2] = bstream.readFloat();
        t[3] = bstream.readFloat();
        t[4] = bstream.readFloat();
        t[5] = bstream.readFloat();
        t[6] = bstream.readFloat();
        t[7] = bstream.readFloat();
        t[8] = bstream.readFloat();
        t[9] = bstream.readFloat();
        f = bstream.readFloat();
        resp[0] = bstream.readFloat();
        resp[1] = bstream.readFloat();
        resp[2] = bstream.readFloat();
        resp[3] = bstream.readFloat();
        resp[4] = bstream.readFloat();
        resp[5] = bstream.readFloat();
        resp[6] = bstream.readFloat();
        resp[7] = bstream.readFloat();
        resp[8] = bstream.readFloat();
        resp[9] = bstream.readFloat();
        stla = bstream.readFloat();
        stlo = bstream.readFloat();
        stel = bstream.readFloat();
        stdp = bstream.readFloat();
        evla = bstream.readFloat();
        evlo = bstream.readFloat();
        evel = bstream.readFloat();
        evdp = bstream.readFloat();
        mag = bstream.readFloat();
        user[0] = bstream.readFloat();
        user[1] = bstream.readFloat();
        user[2] = bstream.readFloat();
        user[3] = bstream.readFloat();
        user[4] = bstream.readFloat();
        user[5] = bstream.readFloat();
        user[6] = bstream.readFloat();
        user[7] = bstream.readFloat();
        user[8] = bstream.readFloat();
        user[9] = bstream.readFloat();
        dist = bstream.readFloat();
        az = bstream.readFloat();
        baz = bstream.readFloat();
        gcarc = bstream.readFloat();
        sb = bstream.readFloat();
        sdelta = bstream.readFloat();
        depmen = bstream.readFloat();
        cmpaz = bstream.readFloat();
        cmpinc = bstream.readFloat();
        xminimum = bstream.readFloat();
        xmaximum = bstream.readFloat();
        yminimum = bstream.readFloat();
        ymaximum = bstream.readFloat();
        unused6 = bstream.readFloat();
        unused7 = bstream.readFloat();
        unused8 = bstream.readFloat();
        unused9 = bstream.readFloat();
        unused10 = bstream.readFloat();
        unused11 = bstream.readFloat();
        unused12 = bstream.readFloat();
        nzyear = bstream.readInt();
        nzjday = bstream.readInt();
        nzhour = bstream.readInt();
        nzmin = bstream.readInt();
        nzsec = bstream.readInt();
        nzmsec = bstream.readInt();
        nvhdr = bstream.readInt();
        norid = bstream.readInt();
        nevid = bstream.readInt();
        npts = bstream.readInt();
        nsnpts = bstream.readInt();
        nwfid = bstream.readInt();
        nxsize = bstream.readInt();
        nysize = bstream.readInt();
        unused15 = bstream.readInt();
        iftype = bstream.readInt();
        idep = bstream.readInt();
        setIztype(Iztype.getIztype(bstream.readInt()));
        unused16 = bstream.readInt();
        iinst = bstream.readInt();
        instreg = bstream.readInt();
        ievreg = bstream.readInt();
        ievtyp = bstream.readInt();
        iqual = bstream.readInt();
        isynth = bstream.readInt();
        imagtyp = bstream.readInt();
        imagsrc = bstream.readInt();
        unused19 = bstream.readInt();
        unused20 = bstream.readInt();
        unused21 = bstream.readInt();
        unused22 = bstream.readInt();
        unused23 = bstream.readInt();
        unused24 = bstream.readInt();
        unused25 = bstream.readInt();
        unused26 = bstream.readInt();
        leven = bstream.readInt();
        lpspol = bstream.readInt();
        lovrok = bstream.readInt();
        lcalda = bstream.readInt();
        unused27 = bstream.readInt();

        // String values
        byte[] strbuf = new byte[8];

        bstream.read(strbuf);
        kstnm = trimIgnorableCharacters(new String(strbuf));
        strbuf = new byte[16];
        bstream.read(strbuf);
        kevnm = trimIgnorableCharacters(new String(strbuf));
        strbuf = new byte[8];
        bstream.read(strbuf);
        khole = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        ko = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        ka = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[0] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[1] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[2] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[3] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[4] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[5] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[6] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[7] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[8] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kt[9] = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kf = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kuser0 = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kuser1 = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kuser2 = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kcmpnm = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        knetwk = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kdatrd = trimIgnorableCharacters(new String(strbuf));
        bstream.read(strbuf);
        kinst = trimIgnorableCharacters(new String(strbuf));
        bstream.close();
    }

    /**
     * The station and event locations are used in combination to define
     * distance, azimuth, backazimuth and gcarc Whenever one of the values is
     * changed this should be checked.
     */
    private void syncStationAndEvent() {
        if ((!isDefault(stla)) && (!isDefault(stlo)) && (!isDefault(evla)) && (!isDefault(evlo))) {
            dist = (float) EModel.getDistance(stla, stlo, evla, evlo);
            az = (float) EModel.getAzimuth(evla, evlo, stla, stlo);
            baz = (float) EModel.getAzimuth(stla, stlo, evla, evlo);
            gcarc = (float) EModel.getGreatCircleDelta(stla, stlo, evla, evlo);
        }
    }

    /**
     * Some SAC files include Unicode characters which should be ignored.
     *
     * The following Unicode characters are ignorable in a Java identifier or a
     * Unicode identifier: ISO control characters that are not whitespace o
     * '\u0000' through '\u0008' o '\u000E' through '\u001B' o '\u007F' through
     * '\u009F' all characters that have the FORMAT general category value
     *
     * @param string
     * @return the String minus the ignorable characters
     */
    final String trimIgnorableCharacters(String string) {
        if (string == null || string.isEmpty() || string.trim().isEmpty()) {
            return SACHeader.STRINGDEFAULT;
        }
        char[] charstring = string.toCharArray();
        StringBuilder sb = new StringBuilder();

        // check if any of the characters in the string are "ignorable" and create a string of those that are.
        for (int ii = 0; ii < charstring.length; ii++) {
            char c = charstring[ii];

            if (Character.isIdentifierIgnorable(c)) {
                sb.append(c);
            }
        }

        String ignorablecharacters = sb.toString();//;"\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u000E\u001B\u007F\u009F";
        StringTokenizer tokenizer = new StringTokenizer(string, ignorablecharacters);
        string = tokenizer.nextToken(); // Here we're assuming that only the first token is the legitimate string

        return string;
    }
}
