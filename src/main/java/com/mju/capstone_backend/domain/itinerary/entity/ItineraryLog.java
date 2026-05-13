package com.mju.capstone_backend.domain.itinerary.entity;

import com.mju.capstone_backend.domain.itinerary.dto.DestinationItem;
import com.mju.capstone_backend.global.converter.DestinationItemListConverter;
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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itinerary_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "itinerary_id", nullable = false)
    private UUID itineraryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = DestinationItemListConverter.class)
    @Column(name = "destinations", columnDefinition = "jsonb")
    private List<DestinationItem> destinations;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "adult_count")
    private Integer adultCount;

    @Column(name = "child_count")
    private Integer childCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = IntegerListConverter.class)
    @Column(name = "child_ages", columnDefinition = "jsonb")
    private List<Integer> childAges;

    @Column(name = "total_days")
    private Integer totalDays;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "day_plans", columnDefinition = "jsonb", nullable = false)
    private String dayPlans;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static ItineraryLog of(Itinerary itinerary) {
        ItineraryLog log = new ItineraryLog();
        log.itineraryId = itinerary.getId();
        log.destinations = itinerary.getDestinations();
        log.budget = itinerary.getBudget();
        log.adultCount = itinerary.getAdultCount();
        log.childCount = itinerary.getChildCount();
        log.childAges = itinerary.getChildAges();
        log.totalDays = itinerary.getTotalDays();
        log.startDate = itinerary.getStartDate();
        log.endDate = itinerary.getEndDate();
        log.dayPlans = itinerary.getDayPlans();
        return log;
    }
}
