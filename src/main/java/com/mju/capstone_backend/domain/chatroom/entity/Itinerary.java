package com.mju.capstone_backend.domain.chatroom.entity;

import com.mju.capstone_backend.global.converter.IntegerListConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itineraries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "destination", nullable = false)
    private String destination;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "adult_count", nullable = false)
    private int adultCount;

    @Column(name = "child_count", nullable = false)
    private int childCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = IntegerListConverter.class)
    @Column(name = "child_ages", columnDefinition = "jsonb")
    private List<Integer> childAges;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "day_plans", columnDefinition = "jsonb", nullable = false)
    private String dayPlans;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static Itinerary of(UUID roomId, String destination, LocalDate startDate, LocalDate endDate,
                               BigDecimal budget, int adultCount, int childCount, List<Integer> childAges) {
        Itinerary itinerary = new Itinerary();
        itinerary.roomId = roomId;
        itinerary.destination = destination;
        itinerary.startDate = startDate;
        itinerary.endDate = endDate;
        itinerary.totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        itinerary.budget = budget;
        itinerary.adultCount = adultCount;
        itinerary.childCount = childCount;
        itinerary.childAges = childAges;
        itinerary.status = "draft";
        itinerary.dayPlans = "{}";
        return itinerary;
    }
}
