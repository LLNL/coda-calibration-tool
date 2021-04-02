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
package llnl.gnem.core.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that provides utility methods for loading arrays of primitive types
 * for files. Files are assumed to have one value per line and to have no
 * extraneous lines ( comments etc. ).
 */
public class FileInputArrayLoader {

    /**
     * Read the specified text file and return an array of ints with one element
     * per line in the file. The file is assumed to have one int value per line
     * and no empty lines or lines with characters not interpretable as an int.
     *
     * @param filename
     *            The name of the file to be read.
     * @return The array of ints read from the file.
     * @throws IOException
     *             Exception thrown if there is an error reading the file.
     */
    public static int[] fillIntsFromFile(final String filename) throws IOException {
        String[] intStrings = fillStrings(filename);
        int[] result = new int[intStrings.length];
        for (int j = 0; j < intStrings.length; ++j) {
            result[j] = Integer.parseInt(intStrings[j].trim());
        }

        return result;
    }

    public static String[] fillStrings(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        return fillStrings(in, true);
    }

    /**
     * Read the specified text file and return an array of Strings with one
     * element per line in the file.
     *
     * @param filename
     *            The name of the file to be read.
     * @return The array of Strings read from the file.
     * @throws IOException
     *             Exception thrown if there is an error reading the file.
     */
    public static String[] fillStrings(final String filename) throws IOException {
        boolean discardEmptyLines = true;
        return fillStrings(filename, discardEmptyLines);
    }

    public static String[] fillStrings(final String filename, boolean discardEmptyLines) throws IOException {
        try (FileReader file = new FileReader(filename); BufferedReader input = new BufferedReader(file);) {
            return fillStrings(input, discardEmptyLines);
        }
    }

    private static String[] fillStrings(BufferedReader input, boolean discardEmptyLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = input.readLine()) != null) {
                if (line.trim().length() > 0 || !discardEmptyLines) {
                    lines.add(line);
                }
            }
            return lines.toArray(new String[0]);
        } catch (IOException x) {
            input.close();
            throw x;
        }
    }
}
