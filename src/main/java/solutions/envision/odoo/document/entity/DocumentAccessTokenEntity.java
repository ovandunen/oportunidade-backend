package solutions.envision.odoo.document.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "document_access_tokens")
@Getter
@Setter
public class DocumentAccessTokenEntity {

    @Id
    @Column(columnDefinition = "UUID", length = 36)
    private Long id;

    @Column(length = 1000, unique = true, nullable = false)
    private String token;

    @Column(name = "employer_id", nullable = false)
    private String employerId;

    @ElementCollection
    @CollectionTable(name = "token_candidate_ids")
    private List<Integer> candidateIds;

    @Column(name = "package_type")
    private String packageType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "max_downloads", nullable = false)
    private Integer maxDownloads;

    // Getters and setters
    // ... (standard getters/setters)
}

