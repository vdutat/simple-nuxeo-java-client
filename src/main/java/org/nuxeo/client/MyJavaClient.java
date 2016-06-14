/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     vdutat
 */
package org.nuxeo.client;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.blob.Blobs;
import org.nuxeo.client.internals.spi.auth.PortalSSOAuthInterceptor;

public class MyJavaClient {

    private static boolean usePortalSSO = false;

    public static void main(String[] args) {
        NuxeoClient nuxeoClient = new NuxeoClient("http://localhost:8080/nuxeo", "Administrator", "Administrator");
        if (usePortalSSO) {
            usePortalSSOAuthentication(nuxeoClient);
        }
        // For defining session and transaction timeout
        nuxeoClient = nuxeoClient.timeout(60).transactionTimeout(60);

        testSUPNXP17085_getFiles(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001");

        // To logout (shutdown the client, headers etc...)
        nuxeoClient.logout();
    }

    private static void testSUPNXP17085_getFiles(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP17085_getFiles> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        Blobs blobs = (Blobs) nuxeoClient.automation("Document.GetBlobsByProperty")
                .input(doc)
                .param("xpath", "files:files")
                .execute();
        System.out.println("Attached files of " + doc.getPath() + ":");
        for (Blob blob : blobs.getBlobs()) {
            System.out.println(blob);
        }
    }

    private static void usePortalSSOAuthentication(NuxeoClient client) {
        client.setAuthenticationMethod(new PortalSSOAuthInterceptor("nuxeo5secretkey", "Administrator"));
    }

}
