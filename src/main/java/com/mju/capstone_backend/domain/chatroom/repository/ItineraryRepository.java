package com.mju.capstone_backend.domain.chatroom.repository;

import com.mju.capstone_backend.domain.chatroom.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
}
