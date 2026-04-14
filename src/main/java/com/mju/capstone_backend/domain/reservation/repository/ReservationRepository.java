package com.mju.capstone_backend.domain.reservation.repository;

import com.mju.capstone_backend.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByItineraryId(UUID itineraryId);
}
