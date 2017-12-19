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
package llnl.gnem.core.util.FileUtil;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.JFileChooser;

import llnl.gnem.core.util.ApplicationLogger;
import llnl.gnem.core.util.FileSystemException;

/**
 * User: matzel Date: Oct 1, 2004 Time: 10:20:24 AM
 */
public class FileManager
{

    private static FileManager _instance = null;

    public static FileManager getInstance()
    {
        if (_instance == null)
        {
            _instance = new FileManager();
        }
        return _instance;
    }

    /**
     * copy one file to another
     *
     * @param srcFile
     * @param destFile
     *
     */
    public static void copy(File srcFile, File destFile) throws IOException
    {
        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        if (!srcFile.exists())
        {
            String msg = String.format("Source file dir: (%s), dfile (%s) ", srcFile.getParent(), srcFile.getName());
            throw new FileSystemException(msg);
        }
        try
        {
            fis = new FileInputStream(srcFile);
            srcChannel = fis.getChannel();
            fos = new FileOutputStream(destFile);
            destChannel = fos.getChannel();
            long size = srcChannel.size();
            destChannel.transferFrom(srcChannel, 0, size);
        }
        catch (IOException e)
        {
            String msg = String.format("Copy failed: ", e.toString());
            throw new FileSystemException(msg);

        }
        finally
        {
            if (srcChannel != null)
            {
                srcChannel.close();
            }
            if (destChannel != null)
            {
                destChannel.close();
            }

            if (fis != null)
            {
                fis.close();
            }
            if (fos != null)
            {
                fos.close();
            }
        }
    }

    /**
     * derived from
     * http://java.sun.com/docs/books/tutorial/essential/io/catstreams.html
     *
     * use to concatenate a series of input stream objects The
     * SequenceInputStream creates a single input stream from multiple input
     * sources. Concatenate, uses SequenceInputStream to implement a
     * concatenation utility that sequentially concatenates files together in
     * the order they are listed.
     *
     * @param filelist - a vector of Files to concatenate
     * @param outfile - the outputfile to write the concatenated set
     * @throws IOException
     */
    public static void concatenate(Vector<File> filelist, File outfile) throws IOException
    {
        ListOfFiles mylist = new ListOfFiles(filelist);
        try (SequenceInputStream s = new SequenceInputStream(mylist);
                FileOutputStream out = new FileOutputStream(outfile);)
        {

            int c;
            while ((c = s.read()) != -1)
            {
                out.write(c);
            }
        }
    }

    /**
     * Read input from a file line by line
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public String readBuffer(String filename) throws IOException
    {
        String string, string2 = new String();
        try (BufferedReader in = new BufferedReader(new FileReader(filename));)
        {

            while ((string = in.readLine()) != null)
            {
                string2 += string + "\n";
            }
        }

        return string2;
    }

    /**
     * Read input by lines from the InputStream
     *
     * @throws IOException
     */
    public void readBufferedInput() throws IOException
    {

        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));)
        {
            System.out.print("Enter a line: ");
            System.out.println(stdin.readLine());
        }
    }

    /**
     * Read input from a string
     *
     * @param string
     * @throws IOException
     */
    public void readString(String string) throws IOException
    {
        // 2. Input from memory
        try (StringReader in2 = new StringReader(string);)
        {
            int c;
            while ((c = in2.read()) != -1)
            {
                System.out.print((char) c);
            }
        }
    }

    /**
     * Use a string tokenizer when reading a string
     *
     * @param string
     * @throws IOException
     */
    public void readStringTokens(String string) throws IOException
    {
        try (StringReader in2 = new StringReader(string);)
        {
            StringTokenizer st = new StringTokenizer(string);
            while (st.hasMoreTokens())
            {
                System.out.println(st.nextToken());
            }
        }
    }

    public DataInputStream readDataInputStream(String string) throws IOException
    {
        System.out.println("// 3. Formatted memory input");
        DataInputStream datainputstream = null;
        try
        {
            datainputstream = new DataInputStream(new ByteArrayInputStream(string.getBytes()));
            while (true)
            {
                System.out.println((char) datainputstream.readByte());
            }
        }
        catch (EOFException e)
        {
            System.err.println("End of stream");
        }

        return datainputstream;
    }

    public void writeBufferedOutput(String string) throws IOException
    {
        System.out.println("// 4. File output to IODemo.out");
        try (BufferedReader bufferedreader = new BufferedReader(new StringReader(string));
                PrintWriter printwriter = new PrintWriter(new BufferedWriter(new FileWriter("IODemo.out")));)
        {
            String s;
            int lineCount = 1;
            while ((s = bufferedreader.readLine()) != null)
            {
                printwriter.println(lineCount++ + ": " + s);
            }

        }
        catch (EOFException e)
        {
            System.err.println("End of stream");
        }
    }

    /**
     * Use JFileChooser to open a File using a generic statement
     */
    public static File[] openFile()
    {
        return openFile("", null, null);
    }

    /**
     * Use a JFileChooser to open a File
     *
     * @param type - the suffix type of file to open (e.g. "", "txt", "sac"...)
     * @param directory - a File object pointing to the directory to start the
     * search in
     * @param component the Graphical Component used to call this procedure
     * (e.g. "JFrame ...")
     * @return the opened File Note for a generic case use openFile("", null,
     * null)
     */
    public static File[] openFile(String type, File directory, Component component)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        final String filetype = type;

        if (directory != null)
        {
            chooser.setCurrentDirectory(directory);
        }
        else
        {
            chooser.setCurrentDirectory(new File("."));
        }

        chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
        {

            @Override
            public boolean accept(File f)
            {
                String name = f.getAbsoluteFile().getName().toLowerCase();
                return name.endsWith(filetype) || f.isDirectory();
            }

            @Override
            public String getDescription()
            {
                return filetype;
            }
        });

        int r = chooser.showOpenDialog(component);
        if (r != JFileChooser.APPROVE_OPTION)
        {
            return null;
        }

        File[] files = chooser.getSelectedFiles();

        return files;
    }

    /**
     * Use a JFileChooser to open a File
     *
     * @param type - the suffix type of file to open (e.g. "", "txt", "sac"...)
     * @param directory - a File object pointing to the directory to start the
     * search in
     * @param component the Graphical Component used to call this procedure
     * (e.g. "JFrame ...")
     * @param dialogtitle
     * @return the opened File Note for a generic case use openFile("", null,
     * null)
     */
    public static File[] openFile(String type, File directory, Component component, String dialogtitle)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(dialogtitle);
        final String filetype = type;

        if (directory != null)
        {
            chooser.setCurrentDirectory(directory);
        }
        else
        {
            chooser.setCurrentDirectory(new File("."));
        }

        chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
        {

            @Override
            public boolean accept(File f)
            {
                String name = f.getAbsoluteFile().getName().toLowerCase();
                return name.endsWith(filetype) || f.isDirectory();
            }

            @Override
            public String getDescription()
            {
                return filetype;
            }
        });

        int r = chooser.showOpenDialog(component);
        if (r != JFileChooser.APPROVE_OPTION)
        {
            return null;
        }

        File[] files = chooser.getSelectedFiles();

        return files;
    }

    public static File selectDirectory()
    {
        return selectDirectory(null, null);
    }

    public static File selectDirectory(File starting_directory, Component component)
    {
        return selectDirectory(starting_directory, component, "Select target directory");
    }

    public static File selectDirectory(File starting_directory, Component component, String dialogtitle)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogtitle);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (starting_directory != null)
        {
            chooser.setCurrentDirectory(starting_directory);
        }
        else
        {
            chooser.setCurrentDirectory(new File("."));
        }

        int r = chooser.showOpenDialog(component);
        if (r == JFileChooser.APPROVE_OPTION)
        {
            return chooser.getSelectedFile();
        }

        return null;
    }

    /**
     * use JFileChooser to save a file
     *
     * @param defaultfile - if not null- the working directory to save the file
     * @param component - the Graphical Component used to call this procedure
     * (e.g. "JFrame ...")
     * @return the File
     */
    public static File saveFile(File defaultfile, Component component)
    {
        return saveFile(defaultfile, component, null);
    }

    /**
     * use JFileChooser to save a file
     *
     * @param defaultfile - if not null- the working directory to save the file
     * @param component - the Graphical Component used to call this procedure
     * (e.g. "JFrame ...")
     * @return the File
     */
    public static File saveFile(File defaultfile, Component component, String dialogtitle)
    {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(dialogtitle);

        if (defaultfile != null)
        {
            if (defaultfile.isDirectory())
            {
                chooser.setCurrentDirectory(defaultfile);
            }
            else
            {
                chooser.setSelectedFile(defaultfile);
            }
        }

        int r = chooser.showSaveDialog(component);
        if (r != JFileChooser.APPROVE_OPTION)
        {
            return null;
        }

        File result = chooser.getSelectedFile();

        return result;
    }

    /**
     * utility for making subdirectories
     *
     * @param reference - references the parent directory - if reference is a
     * file it's parent will be the parent directory
     * @param subdirectory - the String denoting the desired subdirectory
     * @return the subdirectory or null if unable to create it.
     *
     * Note if the directory already exists this will return null
     */
    public static File makeDirectory(File reference, String subdirectory)
    {
        try
        {
            if (reference.isDirectory()) // if the reference is a directory - use it as the parent directory
            {
                File newdirectory = new File(reference, subdirectory);

                if (newdirectory.exists()) // check if the newdirectory exists already
                {
                    if (newdirectory.isDirectory()) // check if it exists and is a directory
                    {
                        return newdirectory;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    newdirectory.mkdir();
                    return newdirectory;
                }
            }
            else //if the reference is a file, use its parent directory as the parent for the subdirectory
            {
                File newdirectory = new File(reference.getParentFile(), subdirectory);

                // Todo Note below is redundant with above
                if (newdirectory.exists())
                {
                    if (newdirectory.isDirectory())
                    {
                        return newdirectory;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    newdirectory.mkdir();
                    return newdirectory;
                }
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Make all subdirectories given by a String Path
     *
     * @param newPathString
     * @return the File if created
     *
     * usage: e.g. String newPath = path + File.separator + year +
     * File.separator + jday + File.separator + hour;
     * FileManager.mkdirs(newPath);
     */
    public static File mkdirs(String newPathString)
    {
        File result = new File(newPathString);
        return mkdirs(result);
    }

    /**
     * Make all subdirectories for a File if they do not already exist
     *
     * @param newPath
     * @return the File if created
     */
    public static File mkdirs(File newPath)
    {
        if (newPath.exists())
        {
            return newPath;
        }
        else
        {
            boolean wasCreated = newPath.mkdirs();

            if (wasCreated)
            {
                return newPath;
            }
            else
            {
                throw new IllegalStateException("Could not create directory: " + newPath);
            }
        }
    }

    /**
     * @param file
     * @param stringbuffer
     */
    public static void writeTextFile(File file, StringBuffer stringbuffer)
    {
        // don't write a file if the stringbuffer is empty
        if (stringbuffer.length() > 0)
        {

            try (BufferedWriter out = new BufferedWriter(new FileWriter(file));)
            {
                out.write(stringbuffer.toString());
            }
            catch (IOException e)
            {
                ApplicationLogger.getInstance().log(Level.WARNING, "unable to write file: " + file.getAbsolutePath(), e);
            }
        }
        else
        {
            System.out.println("Empty StringBuffer. " + file.getAbsolutePath() + " not written");
        }
    }

    /**
     * Write a Text File Given a vector of String objects Each Vector element is
     * treated as a separate line of the file
     *
     * @param file : the File to be written
     * @param textrowvector : the vector of text strings to be written
     */
    public static void writeTextFile(File file, Vector<String> textrowvector)
    {
        StringBuffer sb = new StringBuffer();
        for (int ii = 0; ii < textrowvector.size(); ii++)
        {
            String line = textrowvector.elementAt(ii) + "\n"; // note append a newline character to each element
            sb.append(line);
        }
        writeTextFile(file, sb);
    }

    /**
     * Utility to create a BufferedWriter for a text file
     *
     * @param file the File being written to
     * @return a BufferedWriter for writing to the File
     * @throws IOException
     */
    public static BufferedWriter getBufferedWriter(File file) throws IOException
    {
        return new BufferedWriter(new FileWriter(file));
    }

    /**
     * Utility to create a BufferedReader for a text file
     *
     * @param file the File being read
     * @return a BufferedReader for writing to the File
     * @throws IOException
     */
    public static BufferedReader getBufferedReader(File file) throws IOException
    {
        return new BufferedReader(new FileReader(file));
    }

    /**
     * Utility to create a BufferedWriter for a text file
     *
     * @return a BufferedWriter for writing to the File
     * @throws IOException
     */
    public static BufferedWriter getBufferedWriter(File directory, String filename) throws IOException
    {
        File file = new File(directory, filename);
        return new BufferedWriter(new FileWriter(file));
    }

    /**
     * Utility to write a String to a text file
     *
     * @param out - the BufferedWriter
     * @param string - the String to be written
     */
    public static void write(BufferedWriter out, String string)
    {
        try
        {
            out.write(string);
        }
        catch (IOException e)
        {
            ApplicationLogger.getInstance().log(Level.WARNING, "unable to write string", e);
        }
    }

    public static void writeLine(BufferedWriter out, String string)
    {
        try
        {
            out.write(string);
            out.newLine();
        }
        catch (IOException e)
        {

            ApplicationLogger.getInstance().log(Level.WARNING, "unable to write string", e);
        }

    }

    /**
     * create a Vector object including all the lines of a text file This will
     * return a Vector of Strings - each line of the text file
     *
     * @param file
     * @return a Vector of all the lines of the text file
     */
    public static Vector<String> createTextRowVector(File file)
    {
        // this will be a vector collection of all the lines in the Text file
        Vector<String> textrowvector = new Vector<>();

        try (BufferedReader breader = new BufferedReader(new FileReader(file));)
        {
            String line;
            // 1. Reading input by lines:
            while ((line = breader.readLine()) != null)
            {
                // create a Vector of all the lines in the amplitude measurement file
                textrowvector.addElement(line);
            }
        }
        catch (IOException e)
        {
            //leaving this setting to null, upstream users currently relying on that 'feature'.
            textrowvector = null;
            ApplicationLogger.getInstance().log(Level.WARNING, "Exception thrown: " + file.getAbsolutePath(), e);
        }

        return textrowvector;
    }

    /**
     * create an ArrayList including all the lines of a text file This will
     * return a Vector of Strings - each line of the text file
     *
     * @param file
     * @return an ArrayList of all the lines of the text file
     */
    public static ArrayList<String> createTextRowCollection(File file)
    {
        // this will be a vector collection of all the lines in the Text file
        ArrayList<String> textrowlist = new ArrayList<>();

        try (BufferedReader breader = new BufferedReader(new FileReader(file));)
        {
            // 1. Reading input by lines:

            String line;
            // create a Vector of all the lines in the amplitude measurement file
            while ((line = breader.readLine()) != null)
            {
                textrowlist.add(line);
            }
        }
        catch (IOException e)
        {
            textrowlist = null;
            ApplicationLogger.getInstance().log(Level.WARNING, "Exception thrown: " + file.getAbsolutePath(), e);
        }

        return textrowlist;
    }

    /**
     * Converts the next line in a RandomAccessFile into a Vector of String
     * tokens
     *
     * e.g. ("this is a line") TO Vector("this", "is", "a", "line")
     *
     * @param file a RandomAccessfile
     * @return the Vector of the String tokens for the "next" line in the
     * RandomAccessFile
     * @throws IOException
     */
    public static Vector<String> readLineStringTokens(RandomAccessFile file) throws IOException
    {
        String s = file.readLine();
        Vector<String> tokens = getStringTokens(s);
        return tokens;
    }

    /**
     * skip a specific number of lines in a RandomAccessFile
     *
     * @param file - the RandomAccessFile
     * @param linesToSkip - an integer number of lines to skip
     * @return - the RandomAccessFile readline() String result for the last line
     * @throws IOException
     */
    public static String skipLines(RandomAccessFile file, int linesToSkip) throws IOException
    {
        String result = null;
        for (int ii = 0; ii < linesToSkip; ii++)
        {
            result = file.readLine();
        }
        return result;
    }

    public static Vector<String> getStringTokens(String string)
    {
        StringTokenizer st = new StringTokenizer(string);
        Vector<String> tokens = new Vector<>();

        int num = st.countTokens();
        for (int ii = 0; ii < num; ii++)
        {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    /**
     * This code strips the suffix from a file name
     */
    public static String stripSuffix(File file)
    {
        StringBuffer result = new StringBuffer();

        String filename = file.getAbsolutePath();
        StringTokenizer tokenizer = new StringTokenizer(filename, ".", true);
        int ntokens = tokenizer.countTokens();
        for (int ii = 0; ii < ntokens - 1; ii++)
        {
            result.append(tokenizer.nextToken());
        }

        return result.toString();
    }

    /**
     * Read an integer array from a DataInputStream and convert it to a String
     * Used for parsing MATLAB character output written using
     *
     * fwrite(fid, nchars, 'int') fwrite(fid, chararray, 'int')
     *
     * @param is the DataInputStream
     * @return the String representation of the integer array
     */
    public static String convertIntArrayToString(DataInputStream is) throws IOException
    {
        String result = "";

        int nchars = is.readInt();

        int[] character = new int[nchars];
        char[] c = new char[nchars];
        for (int ii = 0; ii < nchars; ii++)
        {
            character[ii] = is.readInt();
            c[ii] = (char) (character[ii]);
        }

        result = new String(c);

        return result;
    }

    /**
     * Write a String as an integer array to a DataOutputStream Used for writing
     * files that are readable by MATLAB using
     *
     * nchars = fread(fid, 1, 'int') matlabstring = fread(fid, nchars, 'int
     * char')
     *
     * @param string the original string
     * @param os the DataOutputStream
     */
    public static void writeStringAsIntArray(String string, DataOutputStream os)
    {
        char[] c = string.toCharArray();
        int nchars = c.length;

        try
        {
            os.writeInt(nchars);

            for (int ii = 0; ii < nchars; ii++)
            {
                int character = c[ii];
                os.writeInt(character);
            }
        }
        catch (IOException e)
        {
            ApplicationLogger.getInstance().log(Level.WARNING, "Exception thrown...", e);
        }
    }

    /**
     * List the files in a directory - limited by regex characters if defined
     * @param regex
     * @param directory
     * @return 
     */
    public static ArrayList<String> listdirectory(String regex, File directory)
    {
        FilenameFilter filter = new PatternFilter(regex);
        File[] filesAndDirs = directory.listFiles();
        
        ArrayList<String> filtered = new ArrayList<>();
        for (File file : filesAndDirs)
        {
            String parent = file.getParent();
            String name = file.getName();
            if (filter.accept(new File(parent), name))
            {
                filtered.add(file.getAbsolutePath());
            }

        }
        return filtered;
    }
    
    
}
