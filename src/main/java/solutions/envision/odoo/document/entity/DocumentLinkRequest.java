package solutions.envision.odoo.document.entity;

public record DocumentLinkRequest(String masterToken, Integer candidateId, Integer documentId) {}