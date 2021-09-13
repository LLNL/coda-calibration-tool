/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

public class EnergyInfo {

    private double obsEnergy;
    private double logTotalEnergy;
    private double logEnergyMDAC;
    private double energyRatio;
    private double obsApparentStress;

    /**
     * @param obsEnergy
     * @param logTotalEnergy
     * @param energyMDAC
     * @param ratio
     * @param stress
     */
    public EnergyInfo(double obsEnergy, double logTotalEnergy, double energyMDAC, double ratio, double stress) {
        this.obsEnergy = obsEnergy;
        this.logTotalEnergy = logTotalEnergy;
        this.logEnergyMDAC = energyMDAC;
        this.energyRatio = ratio;
        this.obsApparentStress = stress;
    }

    public double getObsEnergy() {
        return obsEnergy;
    }

    public void setObsEnergy(double obsTotalEnergy) {
        this.obsEnergy = obsTotalEnergy;
    }

    public double getLogTotalEnergy() {
        return logTotalEnergy;
    }

    public void setLogTotalEnergy(double energy) {
        this.logTotalEnergy = energy;
    }

    public double getLogEnergyMDAC() {
        return logEnergyMDAC;
    }

    public void setLogEnergyMDAC(double energyMDAC) {
        this.logEnergyMDAC = energyMDAC;
    }

    public double getEnergyRatio() {
        return energyRatio;
    }

    public void setEnergyRatio(double energyRatio) {
        this.energyRatio = energyRatio;
    }

    public double getObsApparentStress() {
        return obsApparentStress;
    }

    public void setObsApparentStress(double obsApparentStress) {
        this.obsApparentStress = obsApparentStress;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EnergyInfo [obsEnergy=");
        builder.append(obsEnergy);
        builder.append(", logTotalEnergy=");
        builder.append(logTotalEnergy);
        builder.append(", logEnergyMDAC=");
        builder.append(logEnergyMDAC);
        builder.append(", energyRatio=");
        builder.append(energyRatio);
        builder.append(", obsApparentStress=");
        builder.append(obsApparentStress);
        builder.append("]");
        return builder.toString();
    }
}