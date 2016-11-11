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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.nuxeo.client.api.ConstantsV1;
import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.RecordSet;
import org.nuxeo.client.api.objects.acl.ACE;
import org.nuxeo.client.api.objects.acl.ACL;
import org.nuxeo.client.api.objects.acl.ACP;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.blob.Blobs;
import org.nuxeo.client.api.objects.directory.Directory;
import org.nuxeo.client.api.objects.directory.DirectoryEntry;
import org.nuxeo.client.api.objects.directory.DirectoryEntryProperties;
import org.nuxeo.client.api.objects.directory.DirectoryManager;
import org.nuxeo.client.api.objects.upload.BatchFile;
import org.nuxeo.client.api.objects.upload.BatchUpload;
import org.nuxeo.client.api.objects.user.CurrentUser;
import org.nuxeo.client.api.objects.user.Group;
import org.nuxeo.client.api.objects.user.UserManager;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.client.internals.spi.auth.PortalSSOAuthInterceptor;
import org.nuxeo.client.internals.spi.auth.TokenAuthInterceptor;

public class MyJavaClient {

    private static boolean usePortalSSO = false;
    private static boolean useTokenAuth = false;

    public static void main(String[] args) {
        String username = "Administrator";
        String password =
//                "Administrator"
                "UGcSL7ho94" // nbme-dev
                ;
        if (useTokenAuth) {
            username = "";
            password = "";
        }
        NuxeoClient nuxeoClient = new NuxeoClient(
                "https://nbmedev.nuxeocloud.com/nuxeo",
//                "http://localhost:8080/nuxeo",
//                "https://nightly.nuxeo.com/nuxeo",
                username, password)
//                .schemas("*")
                ;
        if (usePortalSSO) {
            System.out.println("Using PORTAL_AUTH");
            usePortalSSOAuthentication(nuxeoClient);
        } else if (useTokenAuth) {
            System.out.println("Using TOKEN_AUTH");
            useTokenAuthentication(nuxeoClient, acquireToken(username, password));
        } else {
            System.out.println("Using BASIC_AUTH");
        }
        // For defining session and transaction timeout
//        nuxeoClient = nuxeoClient.timeout(60).transactionTimeout(60);

//        testSUPNXP17085_getFiles(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001");
//        incrementVersion(nuxeoClient, "/default-domain/workspaces/SUPNXP-17085/File 001", "minor");
//        testSUPNXP17239_addEntryToDirectory(nuxeoClient, "nature", "nature1", "Nature 1");
//        testSUPNXP17352_queryAverage(nuxeoClient, "SELECT AVG(dss:innerSize) FROM Document WHERE ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState <> 'deleted'");
//        testSUPNXP18038_uploadPicture(nuxeoClient, "/default-domain/workspaces/SUPNXP-18038", "Pic 001", "/tmp/pic1.jpg");
//        testSUPNXP18185_getSourceDocumentForProxy(nuxeoClient, "/default-domain/sections/Section 1/SUPNXP-18185 1");
//        testSUPNXP18288_hasPermission(nuxeoClient, "/default-domain/workspaces/ws1/vdu1", "vdu1", "Read");
//        testSUPNXP18288_hasPermission(nuxeoClient, "/default-domain/workspaces/ws1/vdu1", "vdu2", "Read");
//        callOperation(nuxeoClient, "javascript.logContextVariables", "/");
//        testSUPNXP18361_fetchBlob(nuxeoClient, "/default-domain/workspaces/ws1/File 001");
        testSUPNXP18361_fetchBlob(nuxeoClient, "/default-domain/USMLE/LibraryModel/MRI_Scan.jpg");
        CurrentUser currentUser = nuxeoClient.fetchCurrentUser();
        System.out.println("current user: " + currentUser.getUsername() + ", "
                + currentUser.getId() + ", "
                + currentUser.getUserName() + ", "
                + currentUser.getCurrentUser() + ", "
                + currentUser.getProperties() + ", "
                + currentUser.getUserName()
                );
        // To logout (shutdown the client, headers etc...)
        nuxeoClient.logout();
    }

    @SuppressWarnings("unchecked")
    private static void testSUPNXP18361_fetchBlob(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP18361_fetchBlob> " + pathOrId);
        Document doc = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        String field = "file:content";
        String filename = (String) ((Map<String, Object>)doc.get(field)).get("name");
        System.out.println(doc.getPath() + ", " + filename);
        Blob blob = doc.fetchBlob();
//        Blob blob = nuxeoClient.repository().fetchBlobByPath(pathOrId, field);
        System.out.println("Blob: " + blob);
        try {
            FileOutputStream out = new FileOutputStream("/tmp/" + filename);
            InputStream in = blob.getStream();

            int len = IOUtils.copy(in, out);
            System.out.println(len + " bytes copied");
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void callOperation(NuxeoClient nuxeoClient, String operation, String pathOrId) {
        System.out.println("<callOperation> " + operation + ", " + pathOrId);
        nuxeoClient.automation(operation).input(pathOrId).execute();
    }

    private static void testSUPNXP18288_hasPermission(NuxeoClient nuxeoClient, String pathOrId, String username, String permission) {
        System.out.println("<testSUPNXP18288_getPermissions> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println(doc.getPath());
        // simply retrieve ACLs
        UserManager userManager = nuxeoClient.getUserManager();
        ACP permissions = doc.fetchPermissions();
        System.out.println("nbr ACLs: " + permissions.getAcls().size());
        permissions.getAcls().stream().forEach(acl -> {
            System.out.println("ACL " + acl.getName());
            acl.getAces().stream().forEach(ace -> {
                System.out.println("  " + ace.getUsername() + ":" + ace.getPermission() + ":" + ace.getGranted());
            });
        });
        boolean userHasPermission = false;
        for (ACL acl : permissions.getAcls()) {
            for (ACE ace : acl.getAces()) {
                if (ace.getUsername().equals(username)) {
                    userHasPermission = true;
                    break;
                }
                try {
                    Group group = userManager.fetchGroup(ace.getUsername());
                    System.out.println("group: " + group.getGroupName());
                    // This works ONLY if members of group are users
                    if (group.getMemberGroups().stream().filter(member -> username.equals(member)).count() > 0) {
                        userHasPermission = true;
                        break;
                    }
                } catch (NuxeoClientException reason) {
                    System.err.println("error: " + reason.getStatus() + " exception: " + reason.getException());
                }
            }
            if (userHasPermission) {
                break;
            }
        }
        System.out.println("1. User '" + username + "' has permission '" + permission + "' on document '" + doc.getPath() + "': " + userHasPermission);
        // Call a custom operation
        Blob blob = (Blob) nuxeoClient.automation("UserHasPermission")
                .input(doc)
                .param("username", username)
                .param("permission", permission)
                .execute();
        try {
            System.out.println("2. User '" + username + "' has permission '" + permission + "' on document '" + doc.getPath() + "': " + IOUtils.toString(blob.getStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        client.timeout(10); // workaround to rebuild retrofit
    }

    private static void useTokenAuthentication(NuxeoClient client, String token) {
        client.setAuthenticationMethod(new TokenAuthInterceptor(token));
        client.timeout(10); // workaround to rebuild retrofit
    }

    private static String acquireToken(String username, String password) {
        // curl -u Administrator:Administrator "http://localhost:8080/nuxeo/authentication/token?applicationName=MyJavaClient&deviceId=vdutat-XPS-L421X&deviceDescription=vdutat-XPS-L421&permission=rw"
        return "a44284f6-198b-4b32-99c9-32b32abe4f92";
    }

}
