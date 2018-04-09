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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.apache.commons.io.IOUtils;
import org.nuxeo.client.cache.ResultCacheInMemory;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.RecordSet;
import org.nuxeo.client.objects.acl.ACE;
import org.nuxeo.client.objects.acl.ACL;
import org.nuxeo.client.objects.acl.ACP;
import org.nuxeo.client.objects.blob.Blob;
import org.nuxeo.client.objects.blob.Blobs;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.client.objects.directory.Directory;
import org.nuxeo.client.objects.directory.DirectoryEntry;
import org.nuxeo.client.objects.directory.DirectoryManager;
import org.nuxeo.client.objects.upload.BatchUpload;
import org.nuxeo.client.objects.user.Group;
import org.nuxeo.client.objects.user.User;
import org.nuxeo.client.objects.user.UserManager;
import org.nuxeo.client.spi.NuxeoClientException;
import org.nuxeo.client.spi.auth.PortalSSOAuthInterceptor;
import org.nuxeo.client.spi.auth.TokenAuthInterceptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * mvn clean compile exec:java -Dexec.mainClass="org.nuxeo.client.MyJavaClient"
 *
 * @author vdutat
 *
 */
public class MyJavaClient {

    private static boolean usePortalSSO = false;
    private static boolean useTokenAuth = false;

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        String username = "Administrator";
        String password =
                "Administrator"
//                "UGcSL7ho94" // nbme-dev
                ;
        if (useTokenAuth) {
            username = "";
            password = "";
        }
        NuxeoClient nuxeoClient = new NuxeoClient.Builder()
                // To set a cache on client (this line needs the import of nuxeo-java-client-cache)
//                .cache(new ResultCacheInMemory())
                .url(
//                "https://nbmedev.nuxeocloud.com/nuxeo"
//                "https://nbmeprod.cust.io.nuxeo.com/nuxeo"
                "http://localhost:8080/nuxeo"
//                "https://nightly.nuxeo.com/nuxeo"
                )
                .authentication(username, password)
//                .schemas("*")
                // To set read and connect timeout on http client
//                .readTimeout(60).connectTimeout(60)
                // To define session and transaction timeout in http headers
//                .timeout(60).transactionTimeout(60)
                .connect();
        if (usePortalSSO) {
            System.out.println("Using PORTAL_AUTH");
            usePortalSSOAuthentication(nuxeoClient,
//                  "nuxeo5secretkey", "Administrator"
                    "nuxeoprod810nbmebtlsecretkey", "ContentAdmin@nbme.org" // NBME prod
);
        } else if (useTokenAuth) {
            System.out.println("Using TOKEN_AUTH");
            useTokenAuthentication(nuxeoClient, acquireToken(username, password));
        } else {
            System.out.println("Using BASIC_AUTH");
        }

        if (false) {
            testSUPNXP22682_removeBlob(nuxeoClient, "/default-domain/workspaces/SUPNXP-22682/File1");
        } else {
            testSUPNXP22682_removeBlob_automation(nuxeoClient, "/default-domain/workspaces/SUPNXP-22682/File1");
        }
        User currentUser = nuxeoClient.getCurrentUser();
        System.out.println("current user: " + currentUser.getUserName() + ", "
                + currentUser.getId() + ", "
                + currentUser.getProperties() + ", "
                + currentUser.getUserName()
                );
        // To logout (shutdown the client, headers etc...)
        System.out.println("Disconnecting ...");
        nuxeoClient.disconnect();
        System.out.println("Disconnected");
        System.exit(0);
    }
    
    private static void testSUPNXP22682_removeBlob(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP22682_removeBlob> " + pathOrId);
        Document doc = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        Blob blob = doc.fetchBlob();
        System.out.println("Content: " + blob);
        doc.setPropertyValue("file:content", null);
        doc = nuxeoClient.repository().updateDocument(doc);
        Document doc2 = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        Blob blob2 = doc2.fetchBlob();
        System.out.println("Content: " + blob2);
    }

    private static void testSUPNXP22682_removeBlob_automation(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP22682_removeBlob_automation> " + pathOrId);
        Document doc = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        doc.fetchBlob(new Callback<FileBlob>() {
            @Override
            public void onFailure(Call<FileBlob> call, Throwable t) {
                System.out.println("BAD " + t.getMessage());
            }

            @Override
            public void onResponse(Call<FileBlob> call, Response<FileBlob> response) {
                if (!response.isSuccessful()) {
                    System.out.println("call failed!");
                } else {
                    System.out.println("GOOD");
                    Blob blob = response.body();
                    System.out.println("Content: " + blob);
//                    nuxeoClient.operation("Blob.RemoveFromDocument").param("xpath", "file:content").input(pathOrId).execute(new Callback<FileBlob>() {
//                        @Override
//                        public void onFailure(Call<FileBlob> call, Throwable t) {
//                            System.out.println(" BAD " + t.getMessage());
//                        }
//
//                        @Override
//                        public void onResponse(Call<FileBlob> call, Response<FileBlob> response) {
//                            if (!response.isSuccessful()) {
//                                System.out.println(" call failed!");
//                            } else {
//                                System.out.println(" GOOD");
//                                Blob blob = response.body();
//                                System.out.println(" Content: " + blob);
//                            }
//                        }
//                    });
                }
            }
        });
//        nuxeoClient.operation("Blob.RemoveFromDocument").param("xpath", "file:content").input(pathOrId).execute();
//        System.out.println("BEFORE fetch blob");
//        doc.fetchBlob();
//        System.out.println("AFTER fetch blob");
    }

    private static void testSUPNXP21489_getMemberUsers(NuxeoClient nuxeoClient, String groupName, String userName) {
    	System.out.println("<testSUPNXP21489_getMemberUsers> ");
    	UserManager userManager = nuxeoClient.header("fetch.group", "memberUsers,memberGroups,parentGroups").userManager();
    	// create group
    	Group group = new Group();
    	group.setGroupName(groupName);
    	group.setGroupLabel(groupName);
    	Group nuxeoGroup = userManager.createGroup(group);
    	System.out.println("Group " + nuxeoGroup.getGroupName() + " created");
    	// create user
    	User newUser = new User();
    	newUser.setUserName(userName);
    	newUser.setCompany("Nuxeo");
    	newUser.setEmail(userName + "@nuxeo.com");
    	newUser.setFirstName("to");
    	newUser.setLastName("to");
    	newUser.setPassword(userName);
    	User userObj = userManager.createUser(newUser);
    	System.out.println("User " + userObj.getUserName() + " created");
    	// add user to group
    	User user = userManager.addUserToGroup(userName, groupName);
    	System.out.println("User " + user.getUserName() + " added to group " + groupName);
    	// fetch group
    	Group fetchedGroup = userManager.fetchGroup(groupName);
    	System.out.println("Fetch group members of group " + fetchedGroup.getGroupName() + ": " + fetchedGroup.getMemberUsers());
    	System.out.println("Groups of user " + userName + ": " + userManager.fetchUser(userName).getGroups());
    }
    
    private static void testSUPNXP21144_fetchContentEnricher(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP21144_fetchContentEnricher> " + pathOrId);
        Document doc = nuxeoClient.schemas("*").enrichersForDocument("acls").repository().fetchDocumentByPath(pathOrId);;
        System.out.println(doc.getPath());
        System.out.println("Title:" + doc.getPropertyValue("dc:title"));
        doc.getContextParameters().forEach((key, value) -> {System.out.println(key + ": " + value);});
//        System.out.println("ACLs:" + doc.get);
        // TODO Auto-generated method stub
    }

    private static void testSUPNXP21019_fetchBlob(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP21019_fetchBlob> " + pathOrId);
        Document file = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        Blob blob = file.fetchBlob();
        System.out.println("Mimetype: " + blob.getMimeType());
    }
    
    private static void testSUPNXP20277_fetch(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP20277_fetch> " + pathOrId);
        Document doc = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        System.out.println(doc.getPath());
        System.out.println("Title:" + doc.getPropertyValue("dc:title"));
        System.out.println("Creation date" + doc.getPropertyValue("dc:created"));
    }

    @SuppressWarnings("unchecked")
    private static void testSUPNXP18361_fetchBlob(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP18361_fetchBlob> " + pathOrId);
        Document doc = nuxeoClient.schemas("dublincore", "file").repository().fetchDocumentByPath(pathOrId);
        String field = "file:content";
        String filename = (String) ((Map<String, Object>)doc.getPropertyValue(field)).get("name");
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
        nuxeoClient.operation(operation).input(pathOrId).execute();
    }

    private static void testSUPNXP18288_hasPermission(NuxeoClient nuxeoClient, String pathOrId, String username, String permission) {
        System.out.println("<testSUPNXP18288_getPermissions> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println(doc.getPath());
        // simply retrieve ACLs
        UserManager userManager = nuxeoClient.userManager();
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
//                    System.err.println("error: " + reason.getStatus() + " exception: " + reason.getException());
                }
            }
            if (userHasPermission) {
                break;
            }
        }
        System.out.println("1. User '" + username + "' has permission '" + permission + "' on document '" + doc.getPath() + "': " + userHasPermission);
        // Call a custom operation
        Blob blob = (Blob) nuxeoClient.operation("Document.UserHasPermission")
                .input(pathOrId)
                .param("username", username)
                .param("permission", permission)
                .execute();
        /* TODO
        try {
            String json = org.nuxeo.client.internals.util.IOUtils.read(blob.getStream());
            System.out.println("blob: " + json);
            Map<String, Object> jsonObject = new ObjectMapper().readValue(json, new TypeReference<Map<String,Object>>(){});
            System.out.println("2. User '" + jsonObject.get("username") + "' has permission '" + jsonObject.get("permission") + "' on document '" + jsonObject.get("path") + "': " + jsonObject.get("hasPermission"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private static void testSUPNXP18185_getSourceDocumentForProxy(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP18185_getSourceDocumentForProxy> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println(doc.getPath());
        doc.getProperties().forEach((key, value) -> {System.out.println(key + ": " + value);});
        System.out.println("<testSUPNXP18185_getSourceDocumentForProxy> Executing operation 'Proxy.GetSourceDocument'");
        Document liveDoc = nuxeoClient.operation("Proxy.GetSourceDocument").input(doc).execute();
        System.out.println(liveDoc.getPath());
        doc.getProperties().forEach((key, value) -> {System.out.println(key + ": " + value);});
    }

    /* TODO
    private static void testSUPNXP18038_uploadPicture(NuxeoClient nuxeoClient, String parentDocPath, String docName, String filePath) {
        System.out.println("<testSUPNXP18038_uploadPicture> " + parentDocPath + ", " + docName + ", " + filePath);
        // Batch Upload Initialization
//    	BatchUpload batchUpload = nuxeoClient.uploadManager().enableChunk().chunkSize(10); // enable upload with 10-byte chunks
        BatchUpload batchUpload = nuxeoClient.uploadManager().enableChunk().chunkSize(10*1024); // enable upload with 10K chunks
//    	BatchUpload batchUpload = nuxeoClient.uploadManager();
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
    */

    private static void testSUPNXP17352_queryAverage(NuxeoClient nuxeoClient, String query) {
        System.out.println("<testSUPNXP17352_queryAverage> " + query);
        RecordSet docs = (RecordSet) nuxeoClient.operation("Repository.ResultSetQuery")
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
    /* TODO
    private static void testSUPNXP17239_addEntryToDirectory(NuxeoClient nuxeoClient, String directoryName, String id, String label) {
        DirectoryManager directoryManager = nuxeoClient.directoryManager();
        Directory directory = directoryManager.directory(directoryName);
        for (DirectoryEntry entry : directory.fetchEntries().getDirectoryEntries()) {
            System.out.println("entry: " + entry.getId() + ", " + entry.getLabelProperty());
        }
        DirectoryEntry newEntry = new DirectoryEntry();
        DirectoryEntryProperties newProps = new DirectoryEntryProperties();
        newProps.setId(id);
        newProps.setLabel(label);
        newProps.setOrdering(10000);
        newProps.setObsolete(1);
        newEntry.setProperties(newProps);
        if (directory.fetchEntries().getDirectoryEntries().stream()
                .filter(elem -> elem.getId().equals(id)).collect(Collectors.toList()).isEmpty()) {
            System.out.println("Ading entry...");
            DirectoryEntry createdEntry = directoryManager.directory(directoryName).createEntry(newEntry);
        }
        if (!directory.fetchEntries().getDirectoryEntries().stream().filter(elem -> elem.getId().equals(id)).collect(Collectors.toList()).isEmpty()) {
            System.out.println("directory " + directoryName + " contains entry " + newEntry.getId());
        }
    }
    */

    /* TODO
    private static void incrementVersion(NuxeoClient nuxeoClient, String pathOrId, String incr) {
        System.out.println("<testSUPNXP17085_getFiles> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        System.out.println("version: " + doc.getVersionLabel());
        doc = nuxeoClient.header(ConstantsV1.HEADER_VERSIONING, incr).repository().updateDocument(doc);
        System.out.println("version: " + doc.getVersionLabel());
    }
    */

    private static void testSUPNXP17085_getFiles(NuxeoClient nuxeoClient, String pathOrId) {
        System.out.println("<testSUPNXP17085_getFiles> " + pathOrId);
        Document doc = nuxeoClient.repository().fetchDocumentByPath(pathOrId);
        Blobs blobs = (Blobs) nuxeoClient.operation("Document.GetBlobsByProperty")
                .input(doc)
                .param("xpath", "files:files")
                .execute();
        System.out.println("Attached files of " + doc.getPath() + ":");
        for (Blob blob : blobs.getBlobs()) {
            System.out.println(blob);
        }
    }

    private static void usePortalSSOAuthentication(NuxeoClient client, String secretKey, String username) {
        client.addOkHttpInterceptor(new PortalSSOAuthInterceptor(secretKey, username)); 
    }

    private static void useTokenAuthentication(NuxeoClient client, String token) {
        client.addOkHttpInterceptor(new TokenAuthInterceptor(token));
    }

    private static String acquireToken(String username, String password) {
        // curl -u Administrator:Administrator "http://localhost:8080/nuxeo/authentication/token?applicationName=MyJavaClient&deviceId=vdutat-XPS-L421X&deviceDescription=vdutat-XPS-L421&permission=rw"
        return "a44284f6-198b-4b32-99c9-32b32abe4f92";
    }

}
