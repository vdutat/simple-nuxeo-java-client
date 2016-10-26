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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.client.api.ConstantsV1;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.RecordSet;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.blob.Blobs;
import org.nuxeo.client.api.objects.directory.Directory;
import org.nuxeo.client.api.objects.directory.DirectoryEntry;
import org.nuxeo.client.api.objects.directory.DirectoryEntryProperties;
import org.nuxeo.client.api.objects.directory.DirectoryManager;
import org.nuxeo.client.api.objects.upload.BatchFile;
import org.nuxeo.client.api.objects.upload.BatchUpload;
import org.nuxeo.client.internals.spi.auth.PortalSSOAuthInterceptor;

public class MyJavaClient {

    private static boolean usePortalSSO = false;

    public static void main(String[] args) {
        NuxeoClient nuxeoClient = new NuxeoClient(
                "http://localhost:8080/nuxeo",
//                "https://nightly.nuxeo.com/nuxeo",
                "Administrator", "Administrator")
//                .schemas("*")
                ;
        if (usePortalSSO) {
            usePortalSSOAuthentication(nuxeoClient);
        }
        // For defining session and transaction timeout
        nuxeoClient = nuxeoClient.timeout(60).transactionTimeout(60);

//        testSUPNXP17085_getFiles(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001");
//        incrementVersion(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001", "minor");
//        testSUPNXP17239_addEntryToDirectory(nuxeoClient, "nature", "nature1", "Nature 1");
//        testSUPNXP17352_queryAverage(nuxeoClient, "SELECT AVG(dss:innerSize) FROM Document WHERE ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState <> 'deleted'");
//        testSUPNXP18038_uploadPicture(nuxeoClient, "/default-domain/workspaces/SUPNXP-18038", "Pic 001", "/tmp/pic1.jpg");
        testSUPNXP18185_getSourceDocumentForProxy(nuxeoClient, "/default-domain/sections/Section 1/SUPNXP-18185 1");

        // To logout (shutdown the client, headers etc...)
        nuxeoClient.logout();
    }

    private static void testSUPNXP18185_getSourceDocumentForProxy(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP18185_getSourceDocumentForProxy> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println(doc.getPath());
        doc.getProperties().forEach((key, value) -> {System.out.println(key + ": " + value);});
        System.out.println("<testSUPNXP18185_getSourceDocumentForProxy> Executing operation 'Proxy.GetSourceDocument'");
        Document liveDoc = nuxeoClient.automation("Proxy.GetSourceDocument").input(doc).execute();
        System.out.println(liveDoc.getPath());
        doc.getProperties().forEach((key, value) -> {System.out.println(key + ": " + value);});
        // TODO Auto-generated method stub

    }

    private static void testSUPNXP18038_uploadPicture(NuxeoClient nuxeoClient, String parentDocPath, String docName, String filePath) {
        System.out.println("<testSUPNXP18038_uploadPicture> " + parentDocPath + ", " + docName + ", " + filePath);
        // Batch Upload Initialization
//    	BatchUpload batchUpload = nuxeoClient.fetchUploadManager().enableChunk().chunkSize(10); // enable upload with 10-byte chunks
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager().enableChunk().chunkSize(10*1024); // enable upload with 10K chunks
//    	BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
        // Upload File
        File file = new File(filePath);
        String id_batch = batchUpload.getBatchId();
        batchUpload = batchUpload.upload(file.getName(), file.length(), "image/jpeg", id_batch, "1", file);
        // List uploaded files
        List<BatchFile> batchFiles = batchUpload.fetchBatchFiles();
        batchFiles.stream().forEach(batchFile -> System.out.println("Batch file: " + batchFile));
        // Create document
        Document doc = new Document(docName, "Picture");
        doc.set("dc:title", docName);
        doc = nuxeoClient.repository().createDocumentByPath(parentDocPath, doc);
        // Attach file
        doc.set("file:content", batchUpload.getBatchBlob());
        doc = doc.updateDocument();
        System.out.println(doc);
    }

    private static void testSUPNXP17352_queryAverage(NuxeoClient nuxeoClient, String query) {
        System.out.println("<testSUPNXP17352_queryAverage> " + query);
        RecordSet docs = (RecordSet) nuxeoClient.automation("Repository.ResultSetQuery")
                .param("query", query)
                .execute();
        if (!docs.getUuids().isEmpty()) {
            docs.getUuids().stream().forEach(result -> System.out.println("uuid: " + result));
        }
        System.out.println("Total number of results: " + docs.getUuids().size());

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
