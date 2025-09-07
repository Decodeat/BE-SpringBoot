package com.DecodEat.ocr;

import com.DecodEat.domain.products.entity.DecodeStatus;
import com.DecodEat.domain.products.entity.Product;
import com.DecodEat.domain.products.repository.ProductRepository;
import com.DecodEat.global.apiPayload.code.status.ErrorStatus;
import com.DecodEat.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);
    private static final String ocrEndPoint = "http://localhost:8000/analyze/image-url";
    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${ocr.server.url}") // application.yml에서 Python 서버 URL 주입
    private String ocrServerUrl;

    @Async("ocrTaskExecutor") // "ocrTaskExecutor"라는 스레드 풀에서 비동기 실행
    public void requestOcrAnalysis(Long productId, String imageUrl) {
        // 현재 실행 중인 스레드 이름을 로그로 출력하여 비동기 동작을 확인한다.
        logger.info("[{}] ProductID: {}에 대한 OCR 분석 작업을 시작합니다. Image URL: {}",
                Thread.currentThread().getName(), productId, imageUrl);

        try {
            // 1. Python FastAPI 서버에 OCR 분석 요청
            WebClient webClient = webClientBuilder.baseUrl(ocrServerUrl).build();
            OcrRequestDto requestDto = new OcrRequestDto(imageUrl);

            OcrResponseDto responseDto = webClient.post()
                    .uri(ocrEndPoint) // Python 서버의 실제 엔드포인트
                    .bodyValue(requestDto)
                    .retrieve() // 응답을 받기 시작
                    .bodyToMono(OcrResponseDto.class) // 응답 본문을 OcrResponseDto로 변환
                    .block(); // 비동기 결과를 동기적으로 기다림 (현재 스레드는 백그라운드 스레드이므로 괜찮음)

            // 2. 받은 결과로 DB 업데이트 (트랜잭션 메소드 호출)
            if (responseDto != null) {
                //updateProductWithOcrResult(productId, responseDto);
                System.out.println("===========================디버깅용==============================");
                System.out.println(responseDto.getIngredients().get(0));
                logger.info("[{}] ID: {}에 대한 OCR 분석 작업 완료.", Thread.currentThread().getName(), productId);
                // todo: logger.info("분석 결과: {}", responseDto.getDecodeStatus());
            } else {
                throw new IllegalStateException("OCR 서버로부터 응답을 받지 못했습니다.");
            }

        } catch (Exception e) {
            logger.error("[{}] ID: {} OCR 분석 작업 중 에러 발생", Thread.currentThread().getName(), productId, e);
            // 에러 발생 시 DB에 실패 상태를 기록하는 로직 추가
            //updateProductStatusToFailed(productId);
        }
    }

//    @Transactional
//    public void updateProductWithOcrResult(Long productId, OcrResponseDto dto) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new GeneralException(ErrorStatus.PRODUCT_NOT_EXISTED));
//
//        product.updateOcrResult(dto);
//    }

    @Transactional
    public void updateProductStatus(Long productId, DecodeStatus decodeStatus) {
        productRepository.findById(productId)
                .ifPresent(product->product.updateStatus(decodeStatus));
    }
}
