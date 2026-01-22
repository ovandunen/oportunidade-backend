package solutions.envision.odoo.document.entity;

public record TokenValidationResult(boolean valid, String message, DocumentAccessTokenEntity token) {}