package com.DecodEat.domain.products.service;

import com.DecodEat.domain.products.converter.ProductConverter;
import com.DecodEat.domain.products.dto.request.ProductRegisterRequestDto;
import com.DecodEat.domain.products.dto.response.*;
import com.DecodEat.domain.products.entity.DecodeStatus;
import com.DecodEat.domain.products.entity.Product;
import com.DecodEat.domain.products.entity.ProductInfoImage;
import com.DecodEat.domain.products.entity.ProductNutrition;
import com.DecodEat.domain.products.entity.RawMaterial.RawMaterialCategory;
import com.DecodEat.domain.products.repository.ProductImageRepository;
import com.DecodEat.domain.products.repository.ProductNutritionRepository;
import com.DecodEat.domain.products.repository.ProductRepository;
import com.DecodEat.domain.products.repository.ProductSpecification;
import com.DecodEat.domain.users.entity.User;
import com.DecodEat.global.aws.s3.AmazonS3Manager;
import com.DecodEat.global.dto.PageResponseDto;
import com.DecodEat.global.exception.GeneralException;
import com.DecodEat.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.DecodEat.global.apiPayload.code.status.ErrorStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductNutritionRepository productNutritionRepository;
    private final AmazonS3Manager amazonS3Manager;
    private final OcrService ocrService;


    private static final int PAGE_SIZE = 12;

    public ProductDetailDto getDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new GeneralException(PRODUCT_NOT_EXISTED));

        List<ProductInfoImage> images = productImageRepository.findByProduct(product);
        List<String> imageUrls = images.stream().map(ProductInfoImage::getImageUrl).toList();

        ProductNutrition productNutrition = productNutritionRepository.findById(id).orElseThrow(() -> new GeneralException(PRODUCT_NUTRITION_NOT_EXISTED));

        return ProductConverter.toProductDetailDto(product, imageUrls, productNutrition);
    }

    public ProductRegisterResponseDto addProduct(User user, ProductRegisterRequestDto requestDto, MultipartFile productImage, List<MultipartFile> productInfoImages) {
        String productName = requestDto.getName();
        String manufacturer = requestDto.getManufacturer();

        String productImageUrl = null;
        if (productImage != null && !productImage.isEmpty()) {
            String productImageKey = "products/" + UUID.randomUUID() + "_" + productImage.getOriginalFilename();
            productImageUrl = amazonS3Manager.uploadFile(productImageKey, productImage);
        }


        Product newProduct = Product.builder()
                .user(user)
                .productName(productName)
                .manufacturer(manufacturer)
                .productImage(productImageUrl)
                .decodeStatus(DecodeStatus.PROCESSING)
                .build();

        Product savedProduct = productRepository.save(newProduct);

        List<String> productInfoImageUrls = null;
        if (productInfoImages != null && !productInfoImages.isEmpty()) {
            List<ProductInfoImage> infoImages = productInfoImages.stream().map(image -> {
                String imageKey = "products/info/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
                String imageUrl = amazonS3Manager.uploadFile(imageKey, image);
                return ProductInfoImage.builder()
                        .product(savedProduct)
                        .imageUrl(imageUrl)
                        .build();
            }).collect(Collectors.toList());
            productImageRepository.saveAll(infoImages);

            productInfoImageUrls = infoImages.stream().map(ProductInfoImage::getImageUrl).toList();

            //todo: 일단은 구현용으로 단일 이미지 처리, 추후 리스트로 변경해야 함
            ocrService.requestOcrAnalysis(savedProduct.getProductId(), productInfoImageUrls.get(0));
        }

        return ProductConverter.toProductRegisterDto(savedProduct, productInfoImageUrls);
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO.ProductListResultDTO getProducts(Long cursorId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Slice<Product> slice = productRepository.findCompletedProductsByCursor(cursorId, pageable);

        return ProductConverter.toProductListResultDTO(slice);
    }

    public List<ProductSearchResponseDto.SearchResultPrevDto> searchProducts(String productName) {

        Specification<Product> spec = Specification.where(null);

        if (StringUtils.hasText(productName)) {
            spec = spec.and(ProductSpecification.likeProductName(productName));
        }

        Pageable pageable = PageRequest.of(0, 10, Sort.by("productName").ascending());

        return productRepository.findAll(spec, pageable)
                .stream()
                .map(ProductConverter::toSearchResultPrevDto)
                .toList();
    }

    public PageResponseDto<ProductSearchResponseDto.ProductPrevDto> searchProducts(String productName, List<RawMaterialCategory> categories, Pageable pageable) {
        // Specification을 조합
        Specification<Product> spec = Specification.where(null);

        if (StringUtils.hasText(productName)) {
            spec = spec.and(ProductSpecification.likeProductName(productName));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(ProductSpecification.hasRawMaterialCategories(categories));
        }

        // Specification과 Pageable을 사용하여 데이터 조회
        Page<Product> pagedProducts = productRepository.findAll(spec, pageable);


        if (pageable.getPageNumber() >= pagedProducts.getTotalPages() && pagedProducts.getTotalPages() > 0) {
            throw new GeneralException(PAGE_OUT_OF_RANGE);
        }

        Page<ProductSearchResponseDto.ProductPrevDto> result = pagedProducts.map(ProductConverter::toProductPrevDto);

        return new PageResponseDto<>(result);
    }

    public PageResponseDto<ProductRegisterHistoryDto> getRegisterHistory(User user, Pageable pageable){

        Long userId = user.getId();

        Page<Product> pagedProducts = productRepository.findByUserId(userId, pageable);
        Page<ProductRegisterHistoryDto> result = pagedProducts.map(ProductConverter::toProductRegisterHistoryDto);

        return new PageResponseDto<>(result);
    }
}