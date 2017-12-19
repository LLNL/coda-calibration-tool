package llnl.gnem.core.io;

import java.io.PrintStream;

/**
 * Copyright (c) 2003  Regents of the University of California
 * All rights reserved
 * Author:  Dave Harris
 * Created: Jan 25, 2004
 * Time: 1:11:12 PM
 * Last Modified: Jan 25, 2004
 */



public interface DataSource {

  public void   getData( float[] dataArray );

  public void   getData( float[] dataArray, int offset, int numSamples );

  public void   skipSamples( long numSamples );

  public long   getTotalNumSamples();

  public long   getNumSamplesAvailable();

  public long   getNextSampleIndex();

  public String getChannel();

  public String getStation();

  public double getSamplingRate();

  public double getEpochStartTime();

  public double getEpochEndTime();

  public double getCurrentEpochTime();

  public void   initiate();

  public void   close();

  public void   print( PrintStream ps );


}
