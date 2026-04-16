package com.mju.capstone_backend.domain.itinerary.repository;

import com.mju.capstone_backend.domain.itinerary.entity.ItineraryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryLogRepository extends JpaRepository<ItineraryLog, UUID> {
}
