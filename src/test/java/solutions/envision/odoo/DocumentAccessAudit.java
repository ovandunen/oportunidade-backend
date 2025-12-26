package solutions.envision.odoo;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_access_audit")
public class DocumentAccessAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String token;

    @Column(name = "candidate_id")
    private Integer candidateId;

    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    // Getters and setters
}
