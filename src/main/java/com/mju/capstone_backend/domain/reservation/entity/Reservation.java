package com.mju.capstone_backend.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "itinerary_id", nullable = false)
    private UUID itineraryId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "booked_by", nullable = false, length = 10)
    private String bookedBy;

    @Column(name = "booking_url")
    private String bookingUrl;

    @Column(name = "external_ref_id")
    private String externalRefId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb", nullable = false)
    private String detail;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "reserved_at")
    private OffsetDateTime reservedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    public static Reservation of(UUID itineraryId, String type, String status, String bookedBy,
                                 String bookingUrl, String externalRefId, String detail,
                                 BigDecimal totalPrice, String currency, OffsetDateTime reservedAt) {
        Reservation r = new Reservation();
        r.itineraryId = itineraryId;
        r.type = type;
        r.status = status;
        r.bookedBy = bookedBy;
        r.bookingUrl = bookingUrl;
        r.externalRefId = externalRefId;
        r.detail = detail;
        r.totalPrice = totalPrice;
        r.currency = currency;
        r.reservedAt = reservedAt;
        return r;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
