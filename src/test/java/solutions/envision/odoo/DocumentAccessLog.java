package solutions.envision.odoo;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_access_log")
public class DocumentAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String token;

    @Column(name = "employer_id")
    private String employerId;

    @Column(name = "candidate_id")
    private Integer candidateId;

    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "accessed")
    private Boolean accessed = false;

    // Getters and setters
}
