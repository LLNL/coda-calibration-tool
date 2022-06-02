/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.common.mapping.api;

public class GeoBox {

    private static final Double MAX_X = 180.0;
    private static final Double MAX_Y = 90.0;
    private static final Double MIN_X = -MAX_X;
    private static final Double MIN_Y = -MAX_Y;

    private Double minX;
    private Double minY;
    private Double maxX;
    private Double maxY;

    public GeoBox(Double minX, Double minY, Double maxX, Double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public Double getMinX() {
        if (minX == null) {
            return MIN_X;
        } else {
            return minX;
        }
    }

    public GeoBox setMinX(Double minX) {
        this.minX = minX;
        return this;
    }

    public Double getMinY() {
        if (minY == null) {
            return MIN_Y;
        } else {
            return minY;
        }
    }

    public GeoBox setMinY(Double minY) {
        this.minY = minY;
        return this;
    }

    public Double getMaxX() {
        if (maxX == null) {
            return MAX_X;
        } else {
            return maxX;
        }
    }

    public GeoBox setMaxX(Double maxX) {
        this.maxX = maxX;
        return this;
    }

    public Double getMaxY() {
        if (maxY == null) {
            return MAX_Y;
        } else {
            return maxY;
        }
    }

    public GeoBox setMaxY(Double maxY) {
        this.maxY = maxY;
        return this;
    }
}
