package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mju.capstone_backend.domain.itinerary.dto.DestinationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FastAPI POST /api/v1/ai-messages done 이벤트 페이로드.
 * type 값에 따라 구체 구현체로 역직렬화된다.
 * memory는 type에 무관하며, 이번 턴 갱신이 있으면 non-null, 변경 없으면 null.
 */
@JsonDeserialize(using = FastApiDonePayload.Deserializer.class)
public sealed interface FastApiDonePayload
        permits FastApiDonePayload.Chat,
                FastApiDonePayload.Itinerary,
                FastApiDonePayload.Change,
                FastApiDonePayload.Reservation,
                FastApiDonePayload.Cancel {

    MessagePayload userMessage();
    MessagePayload assistantMessage();
    MemoryPayload memory(); // type 무관, 이번 턴 갱신 없으면 null

    // ─── type별 구체 타입 ─────────────────────────────────────────────────────

    @JsonDeserialize
    record Chat(
            MessagePayload userMessage,
            MessagePayload assistantMessage,
            MemoryPayload memory
    ) implements FastApiDonePayload {}

    @JsonDeserialize
    record Itinerary(
            MessagePayload userMessage,
            MessagePayload assistantMessage,
            MemoryPayload memory,
            ItineraryData itinerary
    ) implements FastApiDonePayload {}

    @JsonDeserialize
    record Change(
            MessagePayload userMessage,
            MessagePayload assistantMessage,
            MemoryPayload memory,
            ChangeData change
    ) implements FastApiDonePayload {}

    @JsonDeserialize
    record Reservation(
            MessagePayload userMessage,
            MessagePayload assistantMessage,
            MemoryPayload memory,
            ReservationData reservation
    ) implements FastApiDonePayload {}

    @JsonDeserialize
    record Cancel(
            MessagePayload userMessage,
            MessagePayload assistantMessage,
            MemoryPayload memory,
            CancelData cancel
    ) implements FastApiDonePayload {}

    // ─── 공통 중첩 타입 ───────────────────────────────────────────────────────

    record MessagePayload(
            String content,
            List<Float> embedding
    ) {}

    record MemoryPayload(
            String aiSummary,
            Map<String, Object> preferences
    ) {}

    // ─── type별 전용 중첩 타입 ────────────────────────────────────────────────

    record ItineraryData(
            Map<String, List<Map<String, Object>>> dayPlans
    ) {}

    record ChangeData(
            List<DestinationItem> destinations,
            BigDecimal budget,
            Integer adultCount,
            Integer childCount,
            List<Integer> childAges
    ) {}

    record ReservationData(
            String type,
            String bookingUrl,
            String externalRefId,
            Map<String, Object> detail,
            BigDecimal totalPrice,
            String currency,
            OffsetDateTime reservedAt
    ) {}

    record CancelData(
            UUID reservationId,
            OffsetDateTime cancelledAt
    ) {}

    // ─── Jackson Deserializer ─────────────────────────────────────────────────

    class Deserializer extends StdDeserializer<FastApiDonePayload> {

        private static final Logger log = LoggerFactory.getLogger(Deserializer.class);

        public Deserializer() {
            super(FastApiDonePayload.class);
        }

        @Override
        public FastApiDonePayload deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            String type = node.path("type").asText(null);
            if (node.isObject()) {
                ((ObjectNode) node).remove("type");
            }
            if (type == null || type.isBlank()) {
                log.warn("FastAPI done payload type is null or missing — falling back to chat");
                return mapper.treeToValue(node, Chat.class);
            }
            return switch (type) {
                case "chat"        -> mapper.treeToValue(node, Chat.class);
                case "itinerary"   -> mapper.treeToValue(node, Itinerary.class);
                case "change"      -> mapper.treeToValue(node, Change.class);
                case "reservation" -> mapper.treeToValue(node, Reservation.class);
                case "cancel"      -> mapper.treeToValue(node, Cancel.class);
                default            -> throw new IllegalArgumentException(
                        "Unknown FastAPI done type: " + type);
            };
        }
    }
}
