package org.citycare.complianceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ComplianceRecord {

    public enum EntityType {
        FACILITY, PATIENT, EMERGENCY;

        @com.fasterxml.jackson.annotation.JsonCreator
        public static EntityType fromString(String value) {
            return EntityType.valueOf(value.toUpperCase());
        }
    }

    public enum Result {
        COMPLIANT, NON_COMPLIANT;

        @com.fasterxml.jackson.annotation.JsonCreator
        public static Result fromString(String value) {
            return Result.valueOf(value.toUpperCase());
        }
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complianceId;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EntityType type;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Result result;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Long officerId;

    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
