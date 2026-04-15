package com.mju.capstone_backend.domain.itinerary.repository;

import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    Optional<Itinerary> findByRoomId(UUID roomId);
}
