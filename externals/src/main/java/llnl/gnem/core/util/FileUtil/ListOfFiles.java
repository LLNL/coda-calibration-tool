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
package llnl.gnem.core.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListOfFiles implements Enumeration<InputStream> {
    private static final Logger log = LoggerFactory.getLogger(ListOfFiles.class);
    private String[] listOfFiles;
    private int current = 0;

    /**
     * creates a list of files given an Array of Strings
     * 
     * @param listOfFiles
     */
    public ListOfFiles(String[] listOfFiles) {
        this.listOfFiles = listOfFiles;
    }

    /**
     * creates a list of the file paths for a Vector of File objects
     * 
     * @param list
     *            - a Vector of File objects
     */
    public ListOfFiles(Vector<File> list) {
        int size = list.size();
        String[] listarray = new String[size];

        for (int ii = 0; ii < listarray.length; ii++) {
            listarray[ii] = list.elementAt(ii).getAbsolutePath();
        }

        this.listOfFiles = listarray;
    }

    @Override
    public boolean hasMoreElements() {
        if (current < listOfFiles.length) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public InputStream nextElement() {
        InputStream in = null;
        if (!hasMoreElements()) {
            throw new NoSuchElementException("No more files.");
        } else {
            String nextElement = listOfFiles[current];
            current++;
            try {
                in = Files.newInputStream(Paths.get(nextElement));
            } catch (IOException e) {
                log.warn("ListOfFiles: Can't open {}", nextElement, e);
            }
        }
        return in;
    }
}
