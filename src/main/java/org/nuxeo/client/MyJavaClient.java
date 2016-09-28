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

import java.util.stream.Collectors;

import org.nuxeo.client.api.ConstantsV1;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.blob.Blobs;
import org.nuxeo.client.api.objects.directory.Directory;
import org.nuxeo.client.api.objects.directory.DirectoryEntry;
import org.nuxeo.client.api.objects.directory.DirectoryEntryProperties;
import org.nuxeo.client.api.objects.directory.DirectoryManager;
import org.nuxeo.client.internals.spi.auth.PortalSSOAuthInterceptor;

public class MyJavaClient {

    private static boolean usePortalSSO = false;

    public static void main(String[] args) {
        NuxeoClient nuxeoClient = new NuxeoClient(
                "http://localhost:8080/nuxeo",
//                "https://nightly.nuxeo.com/nuxeo",
                "Administrator", "Administrator");
        if (usePortalSSO) {
            usePortalSSOAuthentication(nuxeoClient);
        }
        // For defining session and transaction timeout
        nuxeoClient = nuxeoClient.timeout(60).transactionTimeout(60);

//        testSUPNXP17085_getFiles(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001");
//        incrementVersion(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001", "minor");
        testSUPNXP17239_addEntryToDirectory(nuxeoClient, "nature", "nature1", "Nature 1");

        // To logout (shutdown the client, headers etc...)
        nuxeoClient.logout();
    }

	/**
     * https://jira.nuxeo.com/browse/JAVACLIENT-41
     *
     * @param nuxeoClient
     * @param directoryName
     * @param id
     * @param label
     */
	private static void testSUPNXP17239_addEntryToDirectory(NuxeoClient nuxeoClient, String directoryName, String id, String label) {
		DirectoryManager directoryManager = nuxeoClient.getDirectoryManager();
		Directory directory = directoryManager.fetchDirectory(directoryName);
		for (DirectoryEntry entry : directory.getDirectoryEntries()) {
			System.out.println("entry: " + entry.getProperties().getId() + ", " + entry.getProperties().getLabel());
		}
		DirectoryEntry newEntry = new DirectoryEntry();
		DirectoryEntryProperties newProps = new DirectoryEntryProperties();
		newProps.setId(id);
		newProps.setLabel(label);
		newProps.setOrdering(10000);
		newProps.setObsolete(1);
		newEntry.setProperties(newProps);
		if (directory.getDirectoryEntries().stream()
				.filter(elem -> elem.getProperties().getId().equals(id)).collect(Collectors.toList()).isEmpty()) {
			System.out.println("Ading entry...");
			DirectoryEntry createdEntry = directoryManager.createDirectoryEntry(directoryName, newEntry);
		}
		if (!directory.getDirectoryEntries().stream().filter(elem -> elem.getProperties().getId().equals(id)).collect(Collectors.toList()).isEmpty()) {
			System.out.println("directory " + directoryName + " contains entry " + newEntry.getProperties().getId());
		}
	}

	private static void incrementVersion(NuxeoClient nuxeoClient, String pathOrId, String incr) {
        System.out.println("<testSUPNXP17085_getFiles> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println("version: " + doc.getVersionLabel());
        doc = nuxeoClient.header(ConstantsV1.HEADER_VERSIONING, incr).repository().updateDocument(doc);
        System.out.println("version: " + doc.getVersionLabel());
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
