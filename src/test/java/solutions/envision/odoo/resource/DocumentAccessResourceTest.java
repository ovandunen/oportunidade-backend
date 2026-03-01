package solutions.envision.odoo.resource;

import ao.co.oportunidade.document.service.DocumentAccessLogService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import solutions.envision.odoo.document.OdooDocumentClient;
import solutions.envision.odoo.document.entity.DocumentLinkRequest;
import solutions.envision.odoo.document.entity.TokenValidationResult;
import solutions.envision.odoo.document.resource.CandidateSummary;
import solutions.envision.odoo.document.resource.DocumentAccessResource;
import solutions.envision.odoo.document.service.DocumentAccessTokenService;
import solutions.envision.odoo.dto.CandidateInfo;
import solutions.envision.odoo.dto.OdooDocument;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class DocumentAccessResourceTest {

    @InjectMock
    DocumentAccessTokenService tokenService;

    @InjectMock
    OdooDocumentClient odooClient;

    @InjectMock
    HttpHeaders headers;

    @InjectMock
    DocumentAccessLogService accessLogService;

    @Inject
    DocumentAccessResource resource;

    final static String base64Part1 = "JVBERi0xLjYNJeLjz9MNCjExNTIgMCBvYmoNPDwvTGluZWFyaXplZCAxL0wgMzUzODI5L08gMTE1NC9FIDI5OTA0NS9OIDYvVCAzNTMzNzUvSCBbIDUwNSAyODJdPj4NZW5kb2JqDSAgICAgICAgICAgDQoxMTY5IDAgb2JqDTw8L0RlY29kZVBhcm1zPDwvQ29sdW1ucyA1L1ByZWRpY3RvciAxMj4+L0ZpbHRlci9GbGF0ZURlY29kZS9JRFs8QTFFMjk3RjgzQzBDRTE0Qjg4REVEQUJDMTJDNTIxNDI+PDlFOUQ2MTUwMDM1MTAxNEU5OEZFQjE4ODBGNzU2RjJBPl0vSW5kZXhbMTE1MiAzN10vSW5mbyAxMTUxIDAgUi9MZW5ndGggOTMvUHJldiAzNTMzNzYvUm9vdCAxMTUzIDAgUi9TaXplIDExODkvVHlwZS9YUmVmL1dbMSAzIDFdPj5zdHJlYW0NCmjeYmJkYBBgYGJg";
    final static String base64Part2 = "YGYGkQwTQCTTPhDJ+hRMHgaRjBogkqULLPIfTDKAyXtg8glY1hsuy+TwCkgyfg8BkZ/+Ask/y4oZmBgZWPjBtjAw0pz8z/C37D1AgAEAwCQSRg0KZW5kc3RyZWFtDWVuZG9iag1zdGFydHhyZWYNCjANCiUlRU9GDQogICAgICANCjExODggMCBvYmoNPDwvQyAyMDEvRmlsdGVyL0ZsYXRlRGVjb2RlL0kgMjIzL0xlbmd0aCAxODYvUyAxMTMvViAxNzk+PnN0cmVhbQ0KaN5iYGBgAqJOBhYGBv50BmEGBBBmYGZgBYpzXGBi7F+cYGABFLu3QeoBU9eB3bUNQA4bf7SUhG1ORsLHtb08cttMDKpuXGDsaGCAoIgOBg4QQtMKZPExsPLtAdL8QCwIsolRg0GIQbXB+gJfDpB3o8G2gD+BeRvDewYjA/44u0O23vy8AZc/MDAyuDpAnCbAwCqkBNIIdPptuIMFGVhF/gJpbgYGOyW4qBwDa7IdRC2jEkCAAQAKyixRDQplbmRzdHJlYW0NZW5kb2JqDTExNTMgMCBvYmoNPDwvQWNyb0Zvcm0gMTE3MCAwIFIvTGFuZyhkZS1ERSkvTWFya0luZm88PC9NYXJrZWQgdHJ1ZT4+L01ldGFkYXRhIDI4IDAgUi9QYWdlcyAxMTUwIDAgUi9TdHJ1Y3RUcmVlUm9vdCA1MCAwIFIvVHlwZS9DYXRhbG9nPj4NZW5kb2JqDTExNTQgMCBvYmoNPDwvQ29udGVudHNbMTE1NiAwIFIgMTE1OCAwIFIgMTE1OSAwIFIgMTE2MCAwIFIgMTE2MSAwIFIgMTE2MiAwIFIgMTE2MyAwIFIgMTE2NCAwIFJdL0Nyb3BCb3hbMCAwIDU5NS4zMiA4NDIuMDRdL0dyb3VwPDwvQ1MvRGV2aWNlUkdCL1MvVHJhbnNwYXJlbmN5L1R5cGUvR3JvdXA+Pi9NZWRpYUJveFswIDAgNTk1LjMyIDg0Mi4wNF0vUGFyZW50IDExNTAgMCBSL1Jlc291cmNlczw8L0V4dEdTdGF0ZTw8L0dTNyAxMTcxIDAgUi9HUzggMTE3MiAwIFI+Pi9Gb250PDwvRjEgMTE3NSAwIFIvRjIgMTE3OCAwIFIvRjMgMTE4MSAwIFIvRjQgMTE4NyAwIFI+Pi9Qcm9jU2V0Wy9QREYvVGV4dC9JbWFnZUIvSW1hZ2VDL0ltYWdlSV0vWE9iamVjdDw8L0ltYWdlOSAxMTY4IDAgUj4+Pj4vUm90YXRlIDAvU3RydWN0UGFyZW50cyAwL1RhYnMvUy9UeXBlL1BhZ2U+Pg1lbmRvYmoNMTE1NSAwIG9iag08";

     static String pdf;

     @BeforeAll
     public static void setUpAll() {
         pdf = base64Part1 + base64Part2;
     }


    // ==================== getAccessibleCandidates Tests ====================

    @Test
    @DisplayName("Should return candidates when token is valid")
    void testGetAccessibleCandidates_ValidToken() throws Exception {
        // Given
        final String validToken = "valid-token-123";
        final DocumentAccessToken token = createMockToken("EMP001", List.of(1, 2), 10, 5);
        final TokenValidationResult validation = new TokenValidationResult(true, null, token);

        final CandidateInfo candidate1 = new CandidateInfo(1, "John Doe", "john@example.com");
        final CandidateInfo candidate2 = new CandidateInfo(2, "Jane Smith", "jane@example.com");



        final OdooDocument doc1 = new OdooDocument(101, "CV.pdf",pdf ,"application/pdf", pdf.length());
        final OdooDocument doc2 = new OdooDocument(102, "Certificate.pdf", pdf,"application/pdf",pdf.length());

        when(tokenService.validateToken(validToken)).thenReturn(validation);
        when(odooClient.getCandidateInfo(1)).thenReturn(candidate1);
        when(odooClient.getCandidateInfo(2)).thenReturn(candidate2);
        when(odooClient.getCandidateDocuments(1)).thenReturn(List.of(doc1));
        when(odooClient.getCandidateDocuments(2)).thenReturn(List.of(doc2));

        // When
        Response response = resource.getAccessibleCandidates(validToken);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertNotNull(body);
        assertEquals(5, body.get("downloads_remaining"));

        @SuppressWarnings("unchecked")
        List<CandidateSummary> candidates = (List<CandidateSummary>) body.get("candidates");
        assertEquals(2, candidates.size());
        assertEquals("John Doe", candidates.getFirst().name());
        assertEquals(1, candidates.getFirst().documentCount());
    }

    @Test
    @DisplayName("Should return 403 when token is invalid")
    void testGetAccessibleCandidates_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        TokenValidationResult validation = new TokenValidationResult(false, "Token expired", null);

        when(tokenService.validateToken(invalidToken)).thenReturn(validation);

        // When
        Response response = resource.getAccessibleCandidates(invalidToken);

        // Then
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getEntity();
        assertEquals("Token expired", body.get("error"));
    }

    @Test
    @DisplayName("Should handle exception when fetching candidate info fails")
    void testGetAccessibleCandidates_ExceptionHandling() throws Exception {
        // Given
        String validToken = "valid-token-123";
        DocumentAccessToken token = createMockToken("EMP001", List.of(1), 10, 5);
        TokenValidationResult validation = new TokenValidationResult(true, null, token);

        when(tokenService.validateToken(validToken)).thenReturn(validation);
        when(odooClient.getCandidateInfo(1)).thenThrow(new RuntimeException("Odoo connection failed"));

        // When
        Response response = resource.getAccessibleCandidates(validToken);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();

        @SuppressWarnings("unchecked")
        List<CandidateSummary> candidates = (List<CandidateSummary>) body.get("candidates");
        assertTrue(candidates.isEmpty(), "Should filter out failed candidates");
    }

    // ==================== downloadDocument Tests ====================

    @Test
    @DisplayName("Should download document when token is valid and candidate is authorized")
    void testDownloadDocument_Success() throws Exception {
        // Given
        final String validToken = "valid-token-123";
        final Integer candidateId = 1;
        final Integer documentId = 101;

        final DocumentAccessToken token = createMockToken("EMP001", List.of(1, 2), 10, 5);
        final TokenValidationResult validation = new TokenValidationResult(true, null, token);

        final byte[] documentData = "PDF content".getBytes();
        final OdooDocument doc = new OdooDocument(101, "CV.pdf", "PDF content","application/pdf", 1024);

        when(tokenService.validateToken(validToken)).thenReturn(validation);
        when(odooClient.downloadDocument(documentId)).thenReturn(documentData);
        when(odooClient.getCandidateDocuments(candidateId)).thenReturn(List.of(doc));
        when(headers.getHeaderString("X-Forwarded-For")).thenReturn("192.168.1.1");

        // When
        try (final Response response = resource.downloadDocument(candidateId, documentId, validToken)) {

            // Then
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertArrayEquals(documentData, (byte[]) response.getEntity());
            assertEquals("attachment; filename=\"CV.pdf\"", response.getHeaderString("Content-Disposition"));
            assertEquals("application/pdf", response.getHeaderString("Content-Type"));
        }

        verify(tokenService).recordAccess(validToken, candidateId, documentId, "192.168.1.1");
    }

    @Test
    @DisplayName("Should return 403 when token is invalid for download")
    void testDownloadDocument_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        TokenValidationResult validation = new TokenValidationResult(false, "Invalid token", null);

        when(tokenService.validateToken(invalidToken)).thenReturn(validation);

        // When
        try (Response response = resource.downloadDocument(1, 101, invalidToken)) {

            // Then
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Invalid token", response.getEntity());
        }
    }

    @Test
    @DisplayName("Should return 403 when candidate is not in allowed list")
    void testDownloadDocument_UnauthorizedCandidate() {
        // Given
        String validToken = "valid-token-123";
        Integer unauthorizedCandidateId = 999;
        Integer documentId = 101;

        DocumentAccessToken token = createMockToken("EMP001", List.of(1, 2), 10, 5);
        TokenValidationResult validation = new TokenValidationResult(true, null, token);

        when(tokenService.validateToken(validToken)).thenReturn(validation);

        // When
        Response response = resource.downloadDocument(unauthorizedCandidateId, documentId, validToken);

        // Then
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        assertEquals("Access denied for this candidate", response.getEntity());
    }

    @Test
    @DisplayName("Should use X-Real-IP header when X-Forwarded-For is not available")
    void testDownloadDocument_UseRealIpHeader() throws Exception {
        // Given
        String validToken = "valid-token-123";
        Integer candidateId = 1;
        Integer documentId = 101;

        DocumentAccessToken token = createMockToken("EMP001", List.of(1), 10, 5);
        TokenValidationResult validation = new TokenValidationResult(true, null, token);

        byte[] documentData = "PDF content".getBytes();
        OdooDocument doc = new OdooDocument(101, "CV.pdf",pdf ,"application/pdf", 1024);

        when(tokenService.validateToken(validToken)).thenReturn(validation);
        when(odooClient.downloadDocument(documentId)).thenReturn(documentData);
        when(odooClient.getCandidateDocuments(candidateId)).thenReturn(List.of(doc));
        when(headers.getHeaderString("X-Forwarded-For")).thenReturn(null);
        when(headers.getHeaderString("X-Real-IP")).thenReturn("10.0.0.1");

        // When
        Response response = resource.downloadDocument(candidateId, documentId, validToken);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(tokenService).recordAccess(validToken, candidateId, documentId, "10.0.0.1");
    }

    @Test
    @DisplayName("Should return 500 when download fails")
    void testDownloadDocument_DownloadFailure() throws Exception {
        // Given
        final String validToken = "valid-token-123";
        final Integer candidateId = 1;
        final Integer documentId = 101;

        final DocumentAccessToken token = createMockToken("EMP001", List.of(1), 10, 5);
        final TokenValidationResult validation = new TokenValidationResult(true, null, token);

        when(tokenService.validateToken(validToken)).thenReturn(validation);
        when(odooClient.downloadDocument(documentId)).thenThrow(new RuntimeException("Connection failed"));

        // When
        Response response = resource.downloadDocument(candidateId, documentId, validToken);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Failed to download document", response.getEntity());
    }

    // ==================== generateDownloadLink Tests ====================

    @Test
    @DisplayName("Should generate download link when master token is valid")
    void testGenerateDownloadLink_Success() {
        // Given
        String masterToken = "master-token-123";
        Integer candidateId = 1;
        Integer documentId = 101;

        DocumentLinkRequest request = new DocumentLinkRequest(masterToken, candidateId, documentId);
        DocumentAccessToken token = createMockToken("EMP001", List.of(1, 2), 10, 5);
        TokenValidationResult validation = new TokenValidationResult(true, null, token);

        String generatedToken = "single-use-token-456";

        when(tokenService.validateToken(masterToken)).thenReturn(validation);
        when(tokenService.generateDocumentToken("EMP001", candidateId, documentId))
                .thenReturn(generatedToken);

        // When
        Response response = resource.generateDownloadLink(request);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        String downloadUrl = (String) body.get("download_url");
        assertEquals("/api/documents/download/1/101?token=single-use-token-456", downloadUrl);
        assertEquals(24, body.get("expires_in_hours"));
    }

    @Test
    @DisplayName("Should return 403 when master token is invalid")
    void testGenerateDownloadLink_InvalidMasterToken() {
        // Given
        DocumentLinkRequest request = new DocumentLinkRequest("invalid-token", 1, 101);
        TokenValidationResult validation = new TokenValidationResult(false, "Token not found", null);

        when(tokenService.validateToken("invalid-token")).thenReturn(validation);

        // When
        Map<String, String> body;
        try (Response response = resource.generateDownloadLink(request)) {

            // Then
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

            body = (Map<String, String>) response.getEntity();
        }
        assertEquals("Token not found", body.get("error"));
    }

    @Test
    @DisplayName("Should return 403 when candidate is not authorized for link generation")
    void testGenerateDownloadLink_UnauthorizedCandidate() {
        // Given
        String masterToken = "master-token-123";
        Integer unauthorizedCandidateId = 999;

        DocumentLinkRequest request = new DocumentLinkRequest(masterToken, unauthorizedCandidateId, 101);
        DocumentAccessToken token = createMockToken("EMP001", List.of(1, 2), 10, 5);
        TokenValidationResult validation = new TokenValidationResult(true, null, token);

        when(tokenService.validateToken(masterToken)).thenReturn(validation);

        // When
        final Map<String, String> body;
        try (Response response = resource.generateDownloadLink(request)) {

            // Then
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

            body = (Map<String, String>) response.getEntity();
        }
        assertEquals("Access denied", body.get("error"));
    }

    // ==================== Helper Methods ====================

    private DocumentAccessToken createMockToken(String employerId, final List<Integer> candidateIds,
                                                int maxDownloads, int downloadCount) {
        final DocumentAccessToken token = Mockito.mock(DocumentAccessToken.class);
        when(token.getEmployerId()).thenReturn(employerId);
        when(token.getCandidateIds()).thenReturn(candidateIds);
        when(token.getMaxDownloads()).thenReturn(maxDownloads);
        when(token.getDownloadCount()).thenReturn(downloadCount);
        return token;
    }
}