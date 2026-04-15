package com.mju.capstone_backend.domain.itinerary.service;

import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryRepository.Summary;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItineraryServiceImpl 단위 테스트")
class ItineraryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @InjectMocks
    private ItineraryServiceImpl itineraryService;

    private static final String CLERK_ID = "user_testClerkId";

    @BeforeEach
    void injectScheduler() throws Exception {
        var field = ItineraryServiceImpl.class.getDeclaredField("dbScheduler");
        field.setAccessible(true);
        field.set(itineraryService, Schedulers.immediate());
    }

    @Test
    @DisplayName("일정 목록 조회 - 정상 요청 시 draft 우선 정렬된 목록 반환")
    void getItineraries_success() {
        Summary draft = mockSummary(UUID.randomUUID(), "도쿄 3박 4일 여행", "draft",
                "도쿄", 4, LocalDate.of(2026, 5, 1));
        Summary completed = mockSummary(UUID.randomUUID(), "부산 2박 3일 여행", "completed",
                "부산", 3, LocalDate.of(2026, 3, 20));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findSummariesByClerkId(CLERK_ID)).thenReturn(List.of(draft, completed));

        StepVerifier.create(itineraryService.getItineraries(CLERK_ID))
                .assertNext(res -> {
                    assertThat(res.itineraries()).hasSize(2);
                    assertThat(res.itineraries().get(0).status()).isEqualTo("draft");
                    assertThat(res.itineraries().get(1).status()).isEqualTo("completed");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("일정 목록 조회 - 일정이 없는 경우 빈 배열 반환")
    void getItineraries_empty() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findSummariesByClerkId(CLERK_ID)).thenReturn(List.of());

        StepVerifier.create(itineraryService.getItineraries(CLERK_ID))
                .assertNext(res -> assertThat(res.itineraries()).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("일정 목록 조회 - 존재하지 않는 사용자는 404 반환")
    void getItineraries_userNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(itineraryService.getItineraries(CLERK_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Summary mockSummary(UUID id, String name, String status,
                                String destination, int totalDays, LocalDate startDate) {
        return new Summary() {
            @Override public UUID getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getStatus() { return status; }
            @Override public String getDestination() { return destination; }
            @Override public int getTotalDays() { return totalDays; }
            @Override public LocalDate getStartDate() { return startDate; }
        };
    }
}
