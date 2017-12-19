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
package gov.llnl.gnem.apps.coda.calibration.gui.util;

/**
 * <p>
 * Runtime exception extension of {@link IllegalStateException} without the
 * stack trace building. Generally expected to be used in control flow when we
 * want to return a human readable error message or warning when we may also
 * need to return or throw other exceptions where we want to spend the cycles to
 * build a full stack trace.
 * </p>
 * 
 * <p>
 * Note that throwing one of these will still rewind the stack, so you should
 * still expect some performance overhead on these
 * </p>
 */
public class LightweightIllegalStateException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public LightweightIllegalStateException(String message) {
        super(message);
    }

    public LightweightIllegalStateException() {
        super();
    }

    public LightweightIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public LightweightIllegalStateException(Throwable cause) {
        super(cause);
    }

    /**
     * No-op that simply return the instance instead of calling the native
     * fillInStackTrace method.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
