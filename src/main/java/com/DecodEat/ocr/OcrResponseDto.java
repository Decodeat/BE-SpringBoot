package com.DecodEat.ocr;

import com.DecodEat.domain.products.entity.DecodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor // JSON 직렬화를 위해 기본 생성자가 필요
public class OcrResponseDto {
    // DecodeStatus decodeStatus;

    // 원재료명
    private List<String> ingredients;

    // 영양성분: name, value, 단위, 일일섭취량% 포함 객체 리스트로 구성
    private List<NutritionFact> nutritionFacts;

    @Getter
    @NoArgsConstructor
    public static class NutritionFact {
        private String name;         // 예: "나트륨"
        private Double value;        // 예: 55
        private String unit;         // 예: "mg"
        private Double dailyValuePercentage; // 예: 5 (일일섭취량 %)
    }
}
