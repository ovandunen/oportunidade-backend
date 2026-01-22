package solutions.envision.odoo.dto;

public record OdooDocument(Integer id, String name, String base64Data, String mimetype, Integer fileSize) {}
