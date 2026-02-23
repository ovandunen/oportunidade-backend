package solutions.envision.odoo.document.resource;

import ao.co.oportunidade.document.entity.DocumentAccessAuditLog;
import ao.co.oportunidade.document.service.DocumentAccessLogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;
import solutions.envision.odoo.document.OdooDocumentClient;
import solutions.envision.odoo.document.entity.DocumentLinkRequest;
import solutions.envision.odoo.document.entity.TokenValidationResult;
import solutions.envision.odoo.document.service.DocumentAccessTokenService;
import solutions.envision.odoo.dto.CandidateInfo;
import solutions.envision.odoo.dto.OdooDocument;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentAccessResource {

    private static final Logger LOG = Logger.getLogger(DocumentAccessResource.class);

    @Inject
    DocumentAccessLogService accessLogService;

    public DocumentAccessResource(DocumentAccessTokenService tokenService,
                                  OdooDocumentClient odooClient,
                                  @Context HttpHeaders headers) {
        this.tokenService = tokenService;
        this.odooClient = odooClient;
        this.headers = headers;
    }

    DocumentAccessTokenService tokenService;

    OdooDocumentClient odooClient;

    @Context
    HttpHeaders headers;

    /**
     * Get list of accessible candidates for employer
     */
    @GET
    @Path("/candidates")
    public Response getAccessibleCandidates(@QueryParam("token") String token) {

        final TokenValidationResult validation = tokenService.validateToken(token);

        if (!validation.valid()) {
            // Log failed access attempt
            logFailedAccess(null, null, token, null, null, 403, validation.message());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", validation.message()))
                    .build();
        }

        try {
            final List<Integer> candidateIds = validation.token().getCandidateIds();
            final List<CandidateSummary> candidates = candidateIds.stream()
                    .map(id -> {
                        try {
                                                                                                                      CandidateInfo info = odooClient.getCandidateInfo(id);
                            final List<OdooDocument> docs = odooClient.getCandidateDocuments(id);

                            return new CandidateSummary(
                                    info.id(),
                                    info.name(),
                                    docs.size(),
                                    docs.stream().map(d -> new DocumentSummary(
                                            d.id(),
                                            d.name(),
                                            d.mimetype(),
                                            d.fileSize()
                                    )).toList()
                            );
                        } catch (Exception e) {
                            LOG.error("Failed to fetch candidate info", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return Response.ok(Map.of(
                    "candidates", candidates,
                    "downloads_remaining", validation.token().getMaxDownloads() - validation.token().getDownloadCount()
            )).build();

        } catch (Exception e) {
            LOG.error("Error fetching candidates", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to fetch candidates"))
                    .build();
        }
    }

    /**
     * Download specific document
     */
    @GET
    @Path("/download/{candidateId}/{documentId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDocument(
            @PathParam("candidateId") Integer candidateId,
            @PathParam("documentId") Integer documentId,
            @QueryParam("token") String token) {

        // Extract IP address and user agent for logging
        String ipAddress = extractIpAddress();
        String userAgent = headers.getHeaderString("User-Agent");

        final TokenValidationResult validation = tokenService.validateToken(token);

        if (!validation.valid()) {
            // Log failed access attempt
            logFailedAccess(null, null, token, String.valueOf(candidateId), 
                    String.valueOf(documentId), 403, validation.message());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(validation.message())
                    .build();
        }

        // Verify candidate is in allowed list
        if (!validation.token().getCandidateIds().contains(candidateId)) {
            // Log unauthorized access attempt
            logFailedAccess(
                    null, // orderId not available in token
                    validation.token().getEmployerId(),
                    token,
                    String.valueOf(candidateId),
                    String.valueOf(documentId),
                    403,
                    "Access denied for this candidate"
            );
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Access denied for this candidate")
                    .build();
        }

        try {
            // Fetch document from Odoo
            final byte[] documentData = odooClient.downloadDocument(documentId);

            // Get document metadata for filename
            final List<OdooDocument> docs = odooClient.getCandidateDocuments(candidateId);
            final OdooDocument doc = docs.stream()
                    .filter(d -> d.id().equals(documentId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Document not found"));

            // Record access for legacy audit system
            tokenService.recordAccess(token, candidateId, documentId, ipAddress);

            // ========== PHASE 1: LOG ACCESS TO DATABASE ==========
            accessLogService.logSuccessfulAccess(
                    null, // orderId not available in token entity
                    validation.token().getEmployerId(),
                    null, // employerEmail not available in token entity
                    token,
                    DocumentAccessAuditLog.TokenType.MULTI_USE,
                    String.valueOf(candidateId),
                    String.valueOf(documentId),
                    doc.name(),
                    ipAddress,
                    userAgent,
                    (long) documentData.length
            );
            // ====================================================

            LOG.info(String.format("Document %d downloaded by employer %s from IP %s",
                    documentId, validation.token().getEmployerId(), ipAddress));

            return Response.ok(documentData)
                    .header("Content-Disposition", "attachment; filename=\"" + doc.name() + "\"")
                    .header("Content-Type", doc.mimetype())
                    .build();

        } catch (Exception e) {
            LOG.error("Failed to download document", e);
            
            // Log failed download
            logFailedAccess(
                    null, // orderId not available in token
                    validation.token().getEmployerId(),
                    token,
                    String.valueOf(candidateId),
                    String.valueOf(documentId),
                    500,
                    e.getMessage()
            );
            
            return Response.serverError()
                    .entity("Failed to download document")
                    .build();
        }
    }

    /**
     * Get single-use download link for specific document
     */
    @POST
    @Path("/generate-link")
    public Response generateDownloadLink(final DocumentLinkRequest request) {

        // Validate master token
        final TokenValidationResult validation = tokenService.validateToken(request.masterToken());

        if (!validation.valid()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", validation.message()))
                    .build();
        }

        if (!validation.token().getCandidateIds().contains(request.candidateId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Access denied"))
                    .build();
        }

        // Generate single-use token
        final String documentToken = tokenService.generateDocumentToken(
                validation.token().getEmployerId(),
                request.candidateId(),
                request.documentId()
        );

        final String downloadUrl = String.format("/api/documents/download/%d/%d?token=%s",
                request.candidateId(), request.documentId(), documentToken);

        return Response.ok(Map.of(
                "download_url", downloadUrl,
                "expires_in_hours", 24
        )).build();
    }

    /**
     * Extract IP address from request headers.
     * Checks X-Forwarded-For first (for proxied requests), then X-Real-IP, then remote address.
     * 
     * @return the IP address
     */
    private String extractIpAddress() {
        String ipAddress = headers.getHeaderString("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty()) {
            // X-Forwarded-For may contain multiple IPs, take the first one
            return ipAddress.split(",")[0].trim();
        }
        
        ipAddress = headers.getHeaderString("X-Real-IP");
        if (ipAddress != null && !ipAddress.isEmpty()) {
            return ipAddress;
        }
        
        return "unknown";
    }

    /**
     * Log a failed access attempt.
     * Helper method to reduce code duplication.
     * 
     * @param orderId the order ID (may be null)
     * @param employerId the employer ID (may be null)
     * @param token the token used
     * @param candidateId the candidate ID
     * @param documentId the document ID
     * @param httpStatusCode the HTTP status code
     * @param errorMessage the error message
     */
    private void logFailedAccess(
            UUID orderId,
            String employerId,
            String token,
            String candidateId,
            String documentId,
            Integer httpStatusCode,
            String errorMessage) {
        
        try {
            String ipAddress = extractIpAddress();
            String userAgent = headers.getHeaderString("User-Agent");
            
            accessLogService.logFailedAccess(
                    orderId,
                    employerId,
                    null, // employerEmail not available for failed attempts
                    token,
                    DocumentAccessAuditLog.TokenType.MULTI_USE,
                    candidateId,
                    documentId,
                    ipAddress,
                    userAgent,
                    httpStatusCode,
                    errorMessage
            );
        } catch (Exception e) {
            // Don't fail the request if logging fails
            LOG.errorf(e, "Failed to log access attempt");
        }
    }
}

record DocumentSummary(Integer id, String name, String mimetype, Integer fileSize) {}
