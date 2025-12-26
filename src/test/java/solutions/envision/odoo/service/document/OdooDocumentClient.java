package solutions.envision.odoo.service.document;


import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URI;
import java.util.*;

@ApplicationScoped
public class OdooDocumentClient {

    @ConfigProperty(name = "odoo.url")
    String odooUrl;

    @ConfigProperty(name = "odoo.db")
    String database;

    @ConfigProperty(name = "odoo.username")
    String username;

    @ConfigProperty(name = "odoo.password")
    String password;

    private XmlRpcClient modelsClient;
    private Integer uid;

    public void init() throws Exception {
        // Common endpoint
        XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
        commonConfig.setServerURL(URI.create(odooUrl + "/xmlrpc/2/common").toURL());
        XmlRpcClient commonClient = new XmlRpcClient();
        commonClient.setConfig(commonConfig);

        // Authenticate
        uid = (Integer) commonClient.execute("authenticate", Arrays.asList(
                database, username, password, Collections.emptyMap()
        ));

        // Models endpoint
        XmlRpcClientConfigImpl modelsConfig = new XmlRpcClientConfigImpl();
        modelsConfig.setServerURL(URI.create(odooUrl + "/xmlrpc/2/object").toURL());
        modelsClient = new XmlRpcClient();
        modelsClient.setConfig(modelsConfig);
    }

    /**
     * Fetch candidate CV attachments
     */
    public List<OdooDocument> getCandidateDocuments(Integer candidateId) throws Exception {
        ensureAuthenticated();

        // Search for attachments linked to candidate
        Object[] searchParams = new Object[]{
                database, uid, password,
                "ir.attachment", "search_read",
                List.of(
                        List.of(
                                List.of("res_model", "=", "hr.applicant"),
                                List.of("res_id", "=", candidateId),
                                List.of("mimetype", "in", List.of(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                ))
                        )
                ),
                Map.of(
                        "fields", Arrays.asList("id", "name", "datas", "mimetype", "file_size"),
                        "limit", 50
                )
        };

        Object[] results = (Object[]) modelsClient.execute("execute_kw", searchParams);

        List<OdooDocument> documents = new ArrayList<>();
        for (Object result : results) {
            Map<String, Object> doc = (Map<String, Object>) result;
            documents.add(new OdooDocument(
                    (Integer) doc.get("id"),
                    (String) doc.get("name"),
                    (String) doc.get("datas"), // Base64 encoded
                    (String) doc.get("mimetype"),
                    (Integer) doc.get("file_size")
            ));
        }

        return documents;
    }

    /**
     * Get candidate details (name, email, etc.)
     */
    public CandidateInfo getCandidateInfo(Integer candidateId) throws Exception {
        ensureAuthenticated();

        Object[] params = new Object[]{
                database, uid, password,
                "hr.applicant", "read",
                Arrays.asList(Arrays.asList(candidateId)),
                Map.of("fields", Arrays.asList("name", "partner_name", "email_from", "job_id"))
        };

        Object[] results = (Object[]) modelsClient.execute("execute_kw", params);

        if (results.length == 0) {
            throw new IllegalArgumentException("Candidate not found: " + candidateId);
        }

        Map<String, Object> data = (Map<String, Object>) results[0];
        return new CandidateInfo(
                candidateId,
                (String) data.get("partner_name"),
                (String) data.get("email_from")
        );
    }

    /**
     * Get candidates by job position (for package-based access)
     */
    public List<Integer> getCandidatesByJob(Integer jobId) throws Exception {
        ensureAuthenticated();

        Object[] params = new Object[]{
                database, uid, password,
                "hr.applicant", "search",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("job_id", "=", jobId),
                                Arrays.asList("active", "=", true)
                        )
                )
        };

        Object[] ids = (Object[]) modelsClient.execute("execute_kw", params);
        return Arrays.stream(ids)
                .map(id -> (Integer) id)
                .toList();
    }

    /**
     * Download single document
     */
    public byte[] downloadDocument(Integer attachmentId) throws Exception {
        ensureAuthenticated();

        Object[] params = new Object[]{
                database, uid, password,
                "ir.attachment", "read",
                Arrays.asList(Arrays.asList(attachmentId)),
                Map.of("fields", Arrays.asList("datas"))
        };

        Object[] results = (Object[]) modelsClient.execute("execute_kw", params);

        if (results.length == 0) {
            throw new IllegalArgumentException("Document not found");
        }

        Map<String, Object> doc = (Map<String, Object>) results[0];
        String base64Data = (String) doc.get("datas");

        return Base64.getDecoder().decode(base64Data);
    }

    private void ensureAuthenticated() throws Exception {
        if (uid == null) {
            init();
        }
    }
}

// DTOs
record OdooDocument(Integer id, String name, String base64Data, String mimetype, Integer fileSize) {}
record CandidateInfo(Integer id, String name, String email) {}