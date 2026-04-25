package com.mju.capstone_backend.domain.chatmessage.dto;

import java.util.List;
import java.util.Map;

public record FastApiDonePayload(
        String type,                     // 응답 타입 ("chat"|"itinerary"|"change"|"reservation"|"cancel")
        MessagePayload userMessage,
        MessagePayload assistantMessage,
        Map<String, Object> memory,      // ai_summary/preferences 갱신값. 미갱신 시 null
        ItineraryPayload itinerary,      // type="itinerary" 시 non-null
        Map<String, Object> change,      // type="change" 시 non-null
        Map<String, Object> reservation, // type="reservation" 시 non-null
        Map<String, Object> cancel       // type="cancel" 시 non-null
) {
    public record MessagePayload(
            String content,
            List<Float> embedding
    ) {
    }

    public record ItineraryPayload(
            Map<String, List<Map<String, Object>>> dayPlans
    ) {
    }
}
