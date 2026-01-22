package solutions.envision.odoo.document.resource;

import java.util.List;

// DTOs
public record CandidateSummary(Integer id, String name, int documentCount, List<DocumentSummary> documents) {}
