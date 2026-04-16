package com.mju.capstone_backend.domain.itinerary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(example = """
        {
          "dayPlans": {
            "2026-05-01": [
              {
                "plan_name": "경복궁 방문",
                "time": "09:00 ~ 12:00",
                "place": "경복궁",
                "note": "한복 대여 추천",
                "status": "todo"
              },
              {
                "plan_name": "광장시장 점심",
                "time": "12:00 ~ 14:30",
                "place": "광장시장",
                "note": "",
                "status": "todo"
              }
            ],
            "2026-05-02": []
          }
        }
        """)
public record PatchDayPlansRequest(
        Map<String, List<Map<String, Object>>> dayPlans
) {
}
