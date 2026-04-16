package com.mju.capstone_backend.domain.itinerary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.capstone_backend.domain.chatroom.entity.ChatRoom;
import com.mju.capstone_backend.domain.chatroom.repository.ChatRoomRepository;
import com.mju.capstone_backend.domain.itinerary.dto.GetItinerariesResponse;
import com.mju.capstone_backend.domain.itinerary.dto.GetItineraryLogsResponse;
import com.mju.capstone_backend.domain.itinerary.dto.PatchDayPlansRequest;
import com.mju.capstone_backend.domain.itinerary.dto.PatchItineraryRequest;
import com.mju.capstone_backend.domain.itinerary.entity.Itinerary;
import com.mju.capstone_backend.domain.itinerary.entity.ItineraryLog;
import com.mju.capstone_backend.domain.itinerary.repository.ItineraryLogRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItineraryServiceImpl 단위 테스트")
class ItineraryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private ItineraryLogRepository itineraryLogRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ItineraryServiceImpl itineraryService;

    private static final String CLERK_ID = "user_testClerkId";
    private static final UUID ITINERARY_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();

    @BeforeEach
    void injectScheduler() throws Exception {
        var schedulerField = ItineraryServiceImpl.class.getDeclaredField("dbScheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(itineraryService, Schedulers.immediate());

        var mapperField = ItineraryServiceImpl.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(itineraryService, new ObjectMapper());
    }

    // ─── getItineraries ───────────────────────────────────────────────────────

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

    // ─── getItinerary ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("일정 상세 조회 - 정상 요청 시 dayPlans에 index 부여 후 반환")
    void getItinerary_success() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[" +
                "{\"time\":\"12:00 ~ 14:00\",\"plan_name\":\"점심\",\"place\":\"식당\",\"note\":\"\",\"status\":\"todo\"}," +
                "{\"time\":\"09:00 ~ 11:00\",\"plan_name\":\"경복궁\",\"place\":\"경복궁\",\"note\":\"\",\"status\":\"done\"}" +
                "]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 3박 4일 여행");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.getItinerary(CLERK_ID, ITINERARY_ID))
                .assertNext(res -> {
                    assertThat(res.itineraryId()).isEqualTo(ITINERARY_ID);
                    assertThat(res.name()).isEqualTo("서울 3박 4일 여행");
                    var items = res.dayPlans().get("2026-05-01");
                    assertThat(items).hasSize(2);
                    // 09:00 이 먼저 → index 0
                    assertThat(items.get(0).get("index")).isEqualTo(0);
                    assertThat(items.get(0).get("plan_name")).isEqualTo("경복궁");
                    assertThat(items.get(1).get("index")).isEqualTo(1);
                    assertThat(items.get(1).get("plan_name")).isEqualTo("점심");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("일정 상세 조회 - 존재하지 않는 사용자는 404 반환")
    void getItinerary_userNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(itineraryService.getItinerary(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("일정 상세 조회 - 존재하지 않는 일정은 404 반환")
    void getItinerary_notFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.empty());

        StepVerifier.create(itineraryService.getItinerary(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("일정 상세 조회 - 다른 사용자의 일정 접근 시 403 반환")
    void getItinerary_otherUser_returns403() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, "user_otherClerkId", "타인의 일정");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.getItinerary(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == FORBIDDEN)
                .verify();
    }

    // ─── getItineraryLogs ─────────────────────────────────────────────────────

    @Test
    @DisplayName("수정 이력 조회 - 이력이 있으면 createdAt 오름차순 목록 반환")
    void getItineraryLogs_success() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        ItineraryLog log1 = mockItineraryLog(ITINERARY_ID, BigDecimal.valueOf(500000),
                OffsetDateTime.parse("2026-04-03T20:00:00+00:00"));
        ItineraryLog log2 = mockItineraryLog(ITINERARY_ID, BigDecimal.valueOf(600000),
                OffsetDateTime.parse("2026-04-05T14:30:00+00:00"));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.findByItineraryIdOrderByCreatedAtDesc(ITINERARY_ID))
                .thenReturn(List.of(log2, log1));

        StepVerifier.create(itineraryService.getItineraryLogs(CLERK_ID, ITINERARY_ID))
                .assertNext(res -> {
                    assertThat(res.itineraryId()).isEqualTo(ITINERARY_ID);
                    assertThat(res.logs()).hasSize(2);
                    assertThat(res.logs().get(0).budget()).isEqualByComparingTo(BigDecimal.valueOf(600000));
                    assertThat(res.logs().get(1).budget()).isEqualByComparingTo(BigDecimal.valueOf(500000));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("수정 이력 조회 - 이력이 없으면 빈 배열 반환")
    void getItineraryLogs_empty() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.findByItineraryIdOrderByCreatedAtDesc(ITINERARY_ID))
                .thenReturn(List.of());

        StepVerifier.create(itineraryService.getItineraryLogs(CLERK_ID, ITINERARY_ID))
                .assertNext(res -> assertThat(res.logs()).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("수정 이력 조회 - 존재하지 않는 사용자는 404 반환")
    void getItineraryLogs_userNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(itineraryService.getItineraryLogs(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("수정 이력 조회 - 존재하지 않는 일정은 404 반환")
    void getItineraryLogs_itineraryNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.empty());

        StepVerifier.create(itineraryService.getItineraryLogs(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("수정 이력 조회 - 다른 사용자의 일정 접근 시 403 반환")
    void getItineraryLogs_otherUser_returns403() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, "user_otherClerkId", "타인의 일정");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.getItineraryLogs(CLERK_ID, ITINERARY_ID))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == FORBIDDEN)
                .verify();
    }

    // ─── patchItinerary ───────────────────────────────────────────────────────

    @Test
    @DisplayName("기본 정보 수정 - 정상 요청 시 수정된 일정 반환")
    void patchItinerary_success() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[],\"2026-05-02\":[],\"2026-05-03\":[],\"2026-05-04\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, BigDecimal.valueOf(300000), 3, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> {
                    assertThat(res.itineraryId()).isEqualTo(ITINERARY_ID);
                    assertThat(res.budget()).isEqualByComparingTo(BigDecimal.valueOf(300000));
                    assertThat(res.adultCount()).isEqualTo(3);
                })
                .verifyComplete();

        verify(itineraryLogRepository).save(any(ItineraryLog.class));
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    @DisplayName("기본 정보 수정 - 날짜 범위 확대 시 새 날짜 키가 day_plans에 추가됨")
    void patchItinerary_dateRangeExpand_addsDayPlanKeys() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[],\"2026-05-02\":[],\"2026-05-03\":[],\"2026-05-04\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        // 4박5일 → 5박6일 (endDate 하루 연장)
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, LocalDate.of(2026, 5, 5), null, null, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> {
                    assertThat(res.endDate()).isEqualTo(LocalDate.of(2026, 5, 5));
                    assertThat(res.totalDays()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("기본 정보 수정 - 날짜 범위 축소 시 범위 밖 day_plans 키 제거됨")
    void patchItinerary_dateRangeNarrow_removesDayPlanKeys() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[],\"2026-05-02\":[],\"2026-05-03\":[],\"2026-05-04\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        // endDate를 5월2일로 축소
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, LocalDate.of(2026, 5, 2), null, null, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> {
                    assertThat(res.endDate()).isEqualTo(LocalDate.of(2026, 5, 2));
                    assertThat(res.totalDays()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("기본 정보 수정 - startDate가 endDate보다 늦으면 400 반환")
    void patchItinerary_startAfterEnd_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchItineraryRequest request = new PatchItineraryRequest(
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1),
                null, null, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - adultCount가 0이면 400 반환")
    void patchItinerary_adultCountZero_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, 0, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - childCount만 전달하면 400 반환")
    void patchItinerary_childCountWithoutChildAges_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, null, 1, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - childAges 길이가 childCount와 불일치하면 400 반환")
    void patchItinerary_childAgesMismatch_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, null, 2, List.of(5));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - 요청 필드가 모두 기존 값과 동일하면 400 반환")
    void patchItinerary_noChanges_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[],\"2026-05-02\":[],\"2026-05-03\":[],\"2026-05-04\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        // 기존 값: startDate=2026-05-01, endDate=2026-05-04, budget=500000, adultCount=2
        PatchItineraryRequest request = new PatchItineraryRequest(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4),
                BigDecimal.valueOf(500000), 2, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST
                        && rse.getReason().contains("No changes detected"))
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - 존재하지 않는 사용자는 404 반환")
    void patchItinerary_userNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, null, null, null);

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - 존재하지 않는 일정은 404 반환")
    void patchItinerary_itineraryNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.empty());

        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, null, null, null);

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("기본 정보 수정 - 다른 사용자의 일정 수정 시 403 반환")
    void patchItinerary_otherUser_returns403() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, "user_otherClerkId", "타인의 일정");
        PatchItineraryRequest request = new PatchItineraryRequest(
                null, null, null, null, null, null);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchItinerary(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == FORBIDDEN)
                .verify();
    }

    // ─── patchDayPlans ────────────────────────────────────────────────────────

    @Test
    @DisplayName("day_plans 수정 - 정상 요청 시 병합된 전체 dayPlans 반환")
    void patchDayPlans_success() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[],\"2026-05-02\":[],\"2026-05-03\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        Map<String, List<Map<String, Object>>> requestDayPlans = Map.of(
                "2026-05-01", List.of(
                        Map.of("plan_name", "경복궁", "time", "09:00 ~ 12:00",
                                "place", "경복궁", "note", "", "status", "todo")
                )
        );
        PatchDayPlansRequest request = new PatchDayPlansRequest(requestDayPlans);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> {
                    assertThat(res.itineraryId()).isEqualTo(ITINERARY_ID);
                    assertThat(res.dayPlans()).containsKey("2026-05-01");
                    assertThat(res.dayPlans()).containsKey("2026-05-02");
                    assertThat(res.dayPlans()).containsKey("2026-05-03");
                    assertThat(res.dayPlans().get("2026-05-01")).hasSize(1);
                    assertThat(res.dayPlans().get("2026-05-01").get(0).get("plan_name")).isEqualTo("경복궁");
                    assertThat(res.dayPlans().get("2026-05-02")).isEmpty();
                })
                .verifyComplete();

        verify(itineraryLogRepository).save(any(ItineraryLog.class));
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    @DisplayName("day_plans 수정 - 여러 아이템이 time 오름차순으로 정렬되어 저장됨")
    void patchDayPlans_itemsSortedByTime() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        Map<String, List<Map<String, Object>>> requestDayPlans = Map.of(
                "2026-05-01", List.of(
                        Map.of("plan_name", "저녁", "time", "18:00 ~ 20:00", "place", "식당", "status", "todo"),
                        Map.of("plan_name", "아침", "time", "08:00 ~ 09:00", "place", "카페", "status", "todo")
                )
        );
        PatchDayPlansRequest request = new PatchDayPlansRequest(requestDayPlans);

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> {
                    List<Map<String, Object>> items = res.dayPlans().get("2026-05-01");
                    assertThat(items.get(0).get("plan_name")).isEqualTo("아침");
                    assertThat(items.get(1).get("plan_name")).isEqualTo("저녁");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("day_plans 수정 - 빈 배열 전달 시 해당 날짜 아이템 전체 삭제")
    void patchDayPlans_emptyArray_clearsDate() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[{\"plan_name\":\"경복궁\",\"time\":\"09:00 ~ 12:00\",\"place\":\"경복궁\",\"note\":\"\",\"status\":\"todo\"}]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        PatchDayPlansRequest request = new PatchDayPlansRequest(Map.of("2026-05-01", List.of()));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(itineraryLogRepository.save(any(ItineraryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .assertNext(res -> assertThat(res.dayPlans().get("2026-05-01")).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("day_plans 수정 - 일정 범위 밖 날짜는 400 반환")
    void patchDayPlans_dateOutOfRange_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        // Itinerary의 startDate=2026-05-01, endDate=2026-05-04
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-05-10", List.of()));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST
                        && rse.getReason().contains("is out of the itinerary date range"))
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 존재하지 않는 날짜(2026-02-31)는 별도 400 반환")
    void patchDayPlans_nonExistentDate_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-02-31", List.of()));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST
                        && rse.getReason().contains("Invalid date"))
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 필수 필드 누락 시 400 반환")
    void patchDayPlans_missingRequiredField_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-05-01", List.of(
                        Map.of("plan_name", "경복궁", "time", "09:00 ~ 12:00") // place, status 누락
                )));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - time 형식 오류 시 400 반환")
    void patchDayPlans_invalidTimeFormat_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-05-01", List.of(
                        Map.of("plan_name", "경복궁", "time", "9:00~12:00",
                                "place", "경복궁", "status", "todo")
                )));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - status 값 오류 시 400 반환")
    void patchDayPlans_invalidStatus_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-05-01", List.of(
                        Map.of("plan_name", "경복궁", "time", "09:00 ~ 12:00",
                                "place", "경복궁", "status", "pending") // 잘못된 status
                )));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 동일 날짜 내 시간 겹침 시 400 반환")
    void patchDayPlans_timeOverlap_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");
        PatchDayPlansRequest request = new PatchDayPlansRequest(
                Map.of("2026-05-01", List.of(
                        Map.of("plan_name", "A", "time", "09:00 ~ 13:00", "place", "A", "status", "todo"),
                        Map.of("plan_name", "B", "time", "12:00 ~ 14:00", "place", "B", "status", "todo")
                )));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 존재하지 않는 사용자는 404 반환")
    void patchDayPlans_userNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(false);

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID,
                        new PatchDayPlansRequest(Map.of())))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 존재하지 않는 일정은 404 반환")
    void patchDayPlans_itineraryNotFound_returns404() {
        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.empty());

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID,
                        new PatchDayPlansRequest(Map.of())))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 기존 데이터와 동일한 내용 전달 시 400 반환")
    void patchDayPlans_noChanges_returns400() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID,
                "{\"2026-05-01\":[{\"plan_name\":\"경복궁\",\"time\":\"09:00 ~ 12:00\",\"place\":\"경복궁\",\"note\":\"\",\"status\":\"todo\"}]}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, CLERK_ID, "서울 여행");

        PatchDayPlansRequest request = new PatchDayPlansRequest(Map.of(
                "2026-05-01", List.of(
                        Map.of("plan_name", "경복궁", "time", "09:00 ~ 12:00",
                                "place", "경복궁", "note", "", "status", "todo")
                )
        ));

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID, request))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == BAD_REQUEST
                        && rse.getReason().contains("No changes detected"))
                .verify();
    }

    @Test
    @DisplayName("day_plans 수정 - 다른 사용자의 일정 수정 시 403 반환")
    void patchDayPlans_otherUser_returns403() {
        Itinerary itinerary = mockItinerary(ITINERARY_ID, ROOM_ID, "{}");
        ChatRoom chatRoom = mockChatRoom(ROOM_ID, "user_otherClerkId", "타인의 일정");

        when(userRepository.existsById(CLERK_ID)).thenReturn(true);
        when(itineraryRepository.findById(ITINERARY_ID)).thenReturn(Optional.of(itinerary));
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(chatRoom));

        StepVerifier.create(itineraryService.patchDayPlans(CLERK_ID, ITINERARY_ID,
                        new PatchDayPlansRequest(Map.of())))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == FORBIDDEN)
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

    private Itinerary mockItinerary(UUID id, UUID roomId, String dayPlans) {
        Itinerary itinerary = Itinerary.of(roomId, "서울",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4),
                BigDecimal.valueOf(500000), 2, 1, List.of(5));
        try {
            var idField = Itinerary.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(itinerary, id);

            var dayPlansField = Itinerary.class.getDeclaredField("dayPlans");
            dayPlansField.setAccessible(true);
            dayPlansField.set(itinerary, dayPlans);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return itinerary;
    }

    private ItineraryLog mockItineraryLog(UUID itineraryId, BigDecimal budget, OffsetDateTime createdAt) {
        Itinerary base = mockItinerary(UUID.randomUUID(), UUID.randomUUID(), "{}");
        try {
            var budgetField = Itinerary.class.getDeclaredField("budget");
            budgetField.setAccessible(true);
            budgetField.set(base, budget);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ItineraryLog log = ItineraryLog.of(base);
        try {
            var idField = ItineraryLog.class.getDeclaredField("itineraryId");
            idField.setAccessible(true);
            idField.set(log, itineraryId);

            var createdAtField = ItineraryLog.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(log, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return log;
    }

    private ChatRoom mockChatRoom(UUID id, String clerkId, String name) {
        ChatRoom chatRoom = ChatRoom.of(clerkId, name);
        try {
            var idField = ChatRoom.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chatRoom, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return chatRoom;
    }
}
