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
package gov.llnl.gnem.apps.coda.common.gui.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.X509TrustManager;

public class CombinedTrustManager implements X509TrustManager {
    private List<X509TrustManager> managers = new ArrayList<>();

    public CombinedTrustManager(X509TrustManager... trustManagers) {
        if (trustManagers == null) {
            throw new IllegalStateException("A minimum of one X509TrustManager is required to use a CombinedTrustManager.");
        }
        for (X509TrustManager manager : trustManagers) {
            this.managers.add(manager);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String auth) throws CertificateException {
        CertificateException error = null;
        for (X509TrustManager manager : managers) {
            try {
                manager.checkClientTrusted(chain, auth);
                return;
            } catch (CertificateException ex) {
                error = ex;
            }
        }

        if (error != null) {
            throw error;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String auth) throws CertificateException {
        CertificateException error = null;
        for (X509TrustManager manager : managers) {
            try {
                manager.checkServerTrusted(chain, auth);
                return;
            } catch (CertificateException ex) {
                error = ex;
            }
        }

        if (error != null) {
            throw error;
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return managers.stream().flatMap(man -> Stream.of(man.getAcceptedIssuers())).collect(Collectors.toList()).toArray(new X509Certificate[0]);
    }

}