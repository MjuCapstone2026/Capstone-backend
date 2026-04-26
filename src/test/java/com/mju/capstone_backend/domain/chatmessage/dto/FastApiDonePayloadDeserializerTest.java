package com.mju.capstone_backend.domain.chatmessage.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FastApiDonePayload.Deserializer 단위 테스트")
class FastApiDonePayloadDeserializerTest {

    private ObjectMapper mapper;

    private static final String BASE = """
            {
              "userMessage": {"content": "test", "embedding": []},
              "assistantMessage": {"content": "reply", "embedding": []},
              "memory": null
            """;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("type 필드가 없으면 Chat으로 fallback")
    void missingType_fallsBackToChat() throws Exception {
        String json = BASE + "}";

        FastApiDonePayload result = mapper.readValue(json, FastApiDonePayload.class);

        assertThat(result).isInstanceOf(FastApiDonePayload.Chat.class);
    }

    @Test
    @DisplayName("type이 null이면 Chat으로 fallback")
    void nullType_fallsBackToChat() throws Exception {
        String json = BASE + ", \"type\": null }";

        FastApiDonePayload result = mapper.readValue(json, FastApiDonePayload.class);

        assertThat(result).isInstanceOf(FastApiDonePayload.Chat.class);
    }

    @Test
    @DisplayName("type=chat이면 Chat 타입으로 역직렬화")
    void chatType_deserializesToChat() throws Exception {
        String json = BASE + ", \"type\": \"chat\" }";

        FastApiDonePayload result = mapper.readValue(json, FastApiDonePayload.class);

        assertThat(result).isInstanceOf(FastApiDonePayload.Chat.class);
        assertThat(((FastApiDonePayload.Chat) result).userMessage().content()).isEqualTo("test");
    }

    @Test
    @DisplayName("알 수 없는 type이면 역직렬화 실패")
    void unknownType_throwsException() {
        String json = BASE + ", \"type\": \"unknown_xyz\" }";

        assertThatThrownBy(() -> mapper.readValue(json, FastApiDonePayload.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown FastAPI done type: unknown_xyz");
    }
}
