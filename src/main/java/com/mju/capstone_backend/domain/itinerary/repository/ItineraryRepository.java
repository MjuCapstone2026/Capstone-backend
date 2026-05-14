package com.mju.capstone_backend.domain.itinerary.repository;

import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    Optional<Itinerary> findByRoomId(UUID roomId);

    @Query(value = """
            SELECT i.id              AS id,
                   c.name            AS name,
                   i.status          AS status,
                   i.destinations::text AS destinations,
                   i.total_days      AS totalDays,
                   i.start_date      AS startDate
            FROM itineraries i
            JOIN chat_rooms c ON i.room_id = c.id
            WHERE c.clerk_id = :clerkId
            ORDER BY CASE WHEN i.status = 'draft' THEN 0 ELSE 1 END,
                     i.start_date ASC
            """, nativeQuery = true)
    List<Summary> findSummariesByClerkId(@Param("clerkId") String clerkId);

    interface Summary {
        UUID getId();
        String getName();
        String getStatus();
        String getDestinations();
        int getTotalDays();
        LocalDate getStartDate();
    }
}
