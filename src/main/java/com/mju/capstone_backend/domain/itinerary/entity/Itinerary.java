package com.mju.capstone_backend.domain.itinerary.entity;

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
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

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    private static String buildInitialDayPlans(LocalDate startDate, LocalDate endDate) {
        StringBuilder json = new StringBuilder("{");
        LocalDate current = startDate;
        boolean first = true;
        while (!current.isAfter(endDate)) {
            if (!first) json.append(",");
            json.append("\"").append(current).append("\":[]");
            current = current.plusDays(1);
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    public void updateBasicInfo(LocalDate startDate, LocalDate endDate,
                                BigDecimal budget, Integer adultCount,
                                Integer childCount, List<Integer> childAges,
                                String updatedDayPlans) {
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (startDate != null || endDate != null) {
            this.totalDays = (int) ChronoUnit.DAYS.between(this.startDate, this.endDate) + 1;
        }
        if (budget != null) this.budget = budget;
        if (adultCount != null) this.adultCount = adultCount;
        if (childCount != null) {
            this.childCount = childCount;
            this.childAges = childAges;
        }
        if (updatedDayPlans != null) this.dayPlans = updatedDayPlans;
    }

    public void updateDayPlans(String dayPlans) {
        this.dayPlans = dayPlans;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

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
        itinerary.dayPlans = buildInitialDayPlans(startDate, endDate);
        return itinerary;
    }
}
