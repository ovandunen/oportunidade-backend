package ao.co.oportunidade.order.model;

/**
 * Represents the different package types available for document access purchases.
 * Each package type has different token expiration durations.
 * 
 * Business Rules:
 * - BASIC: 24 hours access, suitable for quick reviews
 * - STANDARD: 48 hours access, most common package
 * - PREMIUM: 72 hours access, extended review period
 * - ENTERPRISE: 168 hours (7 days) access, for detailed analysis
 * 
 * All packages allow multi-use tokens with full audit tracking.
 * 
 * @author Oportunidade Team
 * @since Phase 1 - Critical Fixes
 */
public enum PackageType {
    
    /**
     * Basic package: 24-hour token validity.
     * Suitable for quick candidate reviews.
     */
    BASIC(24),
    
    /**
     * Standard package: 48-hour token validity.
     * Most popular option for standard hiring processes.
     */
    STANDARD(48),
    
    /**
     * Premium package: 72-hour token validity.
     * Extended time for thorough candidate evaluation.
     */
    PREMIUM(72),
    
    /**
     * Enterprise package: 168-hour (7-day) token validity.
     * For comprehensive review cycles and multiple stakeholders.
     */
    ENTERPRISE(168);
    
    private final int expirationHours;
    
    PackageType(int expirationHours) {
        this.expirationHours = expirationHours;
    }
    
    /**
     * Get the token expiration duration in hours for this package type.
     * 
     * @return Number of hours until token expires
     */
    public int getExpirationHours() {
        return expirationHours;
    }
    
    /**
     * Parse package type from string (case-insensitive).
     * 
     * @param value String representation of package type
     * @return PackageType enum value
     * @throws IllegalArgumentException if value is not a valid package type
     */
    public static PackageType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Package type cannot be null or empty");
        }
        
        try {
            return PackageType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Invalid package type: '%s'. Valid options are: BASIC, STANDARD, PREMIUM, ENTERPRISE", value)
            );
        }
    }
}
