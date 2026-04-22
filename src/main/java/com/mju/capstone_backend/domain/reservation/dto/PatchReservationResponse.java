package com.mju.capstone_backend.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(CancelReservationResponse.class),
        @JsonSubTypes.Type(ChangeReservationResponse.class)
})
public sealed interface PatchReservationResponse
        permits CancelReservationResponse, ChangeReservationResponse {
}
