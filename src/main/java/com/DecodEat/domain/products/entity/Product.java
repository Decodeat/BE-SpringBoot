package com.DecodEat.domain.products.entity;

import com.DecodEat.domain.users.entity.User;
import com.DecodEat.global.common.BaseEntity;
import com.DecodEat.ocr.OcrResponseDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image", nullable = true)
    private String productImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "decode_status", nullable = false)
    private DecodeStatus decodeStatus;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default // 원재료 리스트
    private List<ProductRawMaterial> ingredients = new ArrayList<>();

    public void updateStatus(DecodeStatus status) {
        this.decodeStatus = status;
    }
}