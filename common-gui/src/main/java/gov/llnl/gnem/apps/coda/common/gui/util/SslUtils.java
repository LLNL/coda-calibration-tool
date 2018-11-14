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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslUtils {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static SSLContext initMergedSSLTrustStore(InputStream keyStoreInput) throws GeneralSecurityException, IOException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreInput, null);

        String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

        KeyManager[] keyManagers = { getSystemKeyManager(defaultAlgorithm, null) };

        TrustManager[] trustManagers = { new CombinedTrustManager(getSystemTrustManager(defaultAlgorithm, keyStore), getSystemTrustManager(defaultAlgorithm, null)) };

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    public static X509KeyManager getSystemKeyManager(String algorithm, KeyStore keystore) throws GeneralSecurityException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
        factory.init(keystore, null);
        return (X509KeyManager) Stream.of(factory.getKeyManagers()).filter(x -> x instanceof X509KeyManager).findFirst().orElse(null);
    }

    public static X509TrustManager getSystemTrustManager(String algorithm, KeyStore keystore) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
        factory.init(keystore);
        return (X509TrustManager) Stream.of(factory.getTrustManagers()).filter(x -> x instanceof X509TrustManager).findFirst().orElse(null);
    }

}