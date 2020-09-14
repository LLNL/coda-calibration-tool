/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "Polygons")
public class GeoJsonPolygon implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Lob
    @Basic
    private String rawGeoJson = "";

    public String getRawGeoJson() {
        return rawGeoJson;
    }

    public GeoJsonPolygon setRawGeoJson(String rawGeoJson) {
        this.rawGeoJson = rawGeoJson;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rawGeoJson, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonPolygon)) {
            return false;
        }
        GeoJsonPolygon other = (GeoJsonPolygon) obj;
        return Objects.equals(id, other.id) && Objects.equals(rawGeoJson, other.rawGeoJson) && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeoJsonPolygon [id=").append(id).append(", version=").append(version).append(", rawGeoJson=").append(rawGeoJson).append("]");
        return builder.toString();
    }

}