package com.opay.config;

import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.product.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Product 더미 데이터 초기화
 * 애플리케이션 시작 시 더미 데이터가 없으면 자동으로 생성하고 S3에 이미지 업로드
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductDataInitializer {

    private final ProductRepository productRepository;
    private final S3ImageService s3ImageService;

    @Bean
    @Profile("!prod") // 프로덕션 환경에서는 실행하지 않음
    public CommandLineRunner initializeProductData() {
        return args -> {
            // 이미 데이터가 있으면 스킵
            if (productRepository.count() > 0) {
                log.info("이미 상품 데이터가 존재합니다. 초기화를 건너뜁니다.");
                return;
            }

            log.info("상품 더미 데이터 초기화 시작...");
            List<Product> products = createDummyProducts();
            
            // S3에 이미지 업로드 및 상품 저장
            int index = 1;
            for (Product product : products) {
                try {
                    // 먼저 상품 저장하여 ID 생성
                    Product savedProduct = productRepository.save(product);
                    Long productId = savedProduct.getId();
                    
                    // Picsum Photos에서 랜덤 이미지 다운로드하여 S3에 업로드
                    String randomImageUrl = "https://picsum.photos/400/400?random=" + index;
                    String s3ImageUrl = s3ImageService.uploadImageFromUrl(randomImageUrl, productId);
                    
                    // S3 URL로 업데이트
                    savedProduct.update(
                            savedProduct.getName(),
                            savedProduct.getDescription(),
                            savedProduct.getPrice(),
                            savedProduct.getStock(),
                            savedProduct.getCategory(),
                            s3ImageUrl
                    );
                    
                    productRepository.save(savedProduct);
                    log.info("상품 생성 완료: id={}, name={}, imageUrl={}", 
                            productId, savedProduct.getName(), s3ImageUrl);
                    
                    // API 호출 제한을 고려하여 약간의 딜레이
                    Thread.sleep(200);
                    index++;
                } catch (Exception e) {
                    log.error("상품 생성 실패: name={}", product.getName(), e);
                    // 이미지 업로드 실패해도 상품은 저장 (이미지 URL 없이)
                    productRepository.save(product);
                    index++;
                }
            }
            
            log.info("상품 더미 데이터 초기화 완료: {}개 상품 생성", products.size());
        };
    }

    /**
     * 더미 상품 데이터 생성
     */
    private List<Product> createDummyProducts() {
        List<Product> products = new ArrayList<>();
        
        // 식품 카테고리
        products.add(Product.builder()
                .name("고당도 껍질 째먹는 햇사과 꿀사과")
                .description("신선하고 달콤한 국내산 사과입니다. 껍질째 먹어도 맛있어요!")
                .price(25870L)
                .stock(50)
                .category("식품")
                .averageRating(4.8)
                .reviewCount(2649)
                .build());
        
        products.add(Product.builder()
                .name("매일 갓 담가 보내는 수제 국내산 전라도 시원한 김치")
                .description("집에서 직접 담근 수제 김치입니다. 신선하고 맛있어요!")
                .price(12600L)
                .stock(30)
                .category("식품")
                .averageRating(4.9)
                .reviewCount(1619)
                .build());
        
        products.add(Product.builder()
                .name("염장 고추지 1kg 정말 맛있는 절임고추")
                .description("아삭하고 맛있는 절임고추입니다. 밑반찬으로 최고예요!")
                .price(7860L)
                .stock(40)
                .category("식품")
                .averageRating(4.7)
                .reviewCount(276)
                .build());
        
        products.add(Product.builder()
                .name("프리미엄 한우 등심 200g")
                .description("최고급 한우 등심입니다. 부드럽고 맛있어요!")
                .price(45000L)
                .stock(20)
                .category("식품")
                .averageRating(4.9)
                .reviewCount(892)
                .build());
        
        products.add(Product.builder()
                .name("신선한 유기농 채소 세트")
                .description("농장에서 직접 수확한 신선한 유기농 채소입니다.")
                .price(18900L)
                .stock(35)
                .category("식품")
                .averageRating(4.6)
                .reviewCount(523)
                .build());
        
        // 가전/디지털 카테고리
        products.add(Product.builder()
                .name("원프라임 2인용 커플 칫솔 살균기 OP-702 UVC")
                .description("자동 살균 기능이 있는 칫솔 살균기입니다.")
                .price(31260L)
                .stock(20)
                .category("가전/디지털")
                .averageRating(4.7)
                .reviewCount(571)
                .build());
        
        products.add(Product.builder()
                .name("스마트 무선 청소기 V15")
                .description("강력한 흡입력의 무선 청소기입니다. 가볍고 사용하기 편해요!")
                .price(189000L)
                .stock(15)
                .category("가전/디지털")
                .averageRating(4.8)
                .reviewCount(1245)
                .build());
        
        products.add(Product.builder()
                .name("에어프라이어 5.5L 대용량")
                .description("바삭하고 건강하게 요리할 수 있는 대용량 에어프라이어입니다.")
                .price(89000L)
                .stock(25)
                .category("가전/디지털")
                .averageRating(4.6)
                .reviewCount(892)
                .build());
        
        products.add(Product.builder()
                .name("무선 블루투스 이어폰")
                .description("고음질의 무선 이어폰입니다. 장시간 사용해도 편안해요!")
                .price(129000L)
                .stock(30)
                .category("가전/디지털")
                .averageRating(4.5)
                .reviewCount(2156)
                .build());
        
        products.add(Product.builder()
                .name("스마트 워치 프로")
                .description("건강 관리와 알림 기능이 있는 스마트 워치입니다.")
                .price(299000L)
                .stock(18)
                .category("가전/디지털")
                .averageRating(4.7)
                .reviewCount(678)
                .build());
        
        // 주방용품 카테고리
        products.add(Product.builder()
                .name("Fanood 316스텐 프라이 팬 28cm")
                .description("스테인리스 프라이팬으로 오래 사용할 수 있어요!")
                .price(34900L)
                .stock(15)
                .category("주방용품")
                .averageRating(4.6)
                .reviewCount(565)
                .build());
        
        products.add(Product.builder()
                .name("도자기 냄비 세트 3종")
                .description("아름다운 도자기 냄비 세트입니다. 건강한 요리에 좋아요!")
                .price(89000L)
                .stock(12)
                .category("주방용품")
                .averageRating(4.8)
                .reviewCount(423)
                .build());
        
        products.add(Product.builder()
                .name("실리콘 주방용품 세트")
                .description("안전하고 위생적인 실리콘 주방용품 세트입니다.")
                .price(24900L)
                .stock(28)
                .category("주방용품")
                .averageRating(4.5)
                .reviewCount(312)
                .build());
        
        products.add(Product.builder()
                .name("스텐레스 식기 세트 24P")
                .description("고급스러운 스텐레스 식기 세트입니다.")
                .price(129000L)
                .stock(10)
                .category("주방용품")
                .averageRating(4.7)
                .reviewCount(189)
                .build());
        
        products.add(Product.builder()
                .name("유리 보관 용기 세트")
                .description("밀폐가 잘 되는 유리 보관 용기 세트입니다.")
                .price(18900L)
                .stock(35)
                .category("주방용품")
                .averageRating(4.6)
                .reviewCount(456)
                .build());
        
        // 패션/의류 카테고리
        products.add(Product.builder()
                .name("면 소재 캐주얼 티셔츠")
                .description("편안하고 시원한 면 소재 티셔츠입니다.")
                .price(29900L)
                .stock(50)
                .category("패션/의류")
                .averageRating(4.5)
                .reviewCount(789)
                .build());
        
        products.add(Product.builder()
                .name("데님 청바지")
                .description("클래식한 스타일의 데님 청바지입니다.")
                .price(59000L)
                .stock(30)
                .category("패션/의류")
                .averageRating(4.6)
                .reviewCount(1234)
                .build());
        
        products.add(Product.builder()
                .name("가을 코트")
                .description("따뜻하고 스타일리시한 가을 코트입니다.")
                .price(129000L)
                .stock(15)
                .category("패션/의류")
                .averageRating(4.7)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("운동화 스니커즈")
                .description("편안한 착화감의 운동화입니다.")
                .price(89000L)
                .stock(40)
                .category("패션/의류")
                .averageRating(4.8)
                .reviewCount(1890)
                .build());
        
        products.add(Product.builder()
                .name("가방 백팩")
                .description("실용적인 백팩입니다. 수납공간이 넓어요!")
                .price(69000L)
                .stock(25)
                .category("패션/의류")
                .averageRating(4.6)
                .reviewCount(678)
                .build());
        
        // 뷰티/화장품 카테고리
        products.add(Product.builder()
                .name("수분 크림 세트")
                .description("수분감이 풍부한 크림 세트입니다.")
                .price(45000L)
                .stock(35)
                .category("뷰티/화장품")
                .averageRating(4.7)
                .reviewCount(1234)
                .build());
        
        products.add(Product.builder()
                .name("선크림 SPF50+")
                .description("자외선 차단이 뛰어난 선크림입니다.")
                .price(18900L)
                .stock(50)
                .category("뷰티/화장품")
                .averageRating(4.8)
                .reviewCount(2156)
                .build());
        
        products.add(Product.builder()
                .name("립스틱 세트 5종")
                .description("다양한 컬러의 립스틱 세트입니다.")
                .price(39000L)
                .stock(28)
                .category("뷰티/화장품")
                .averageRating(4.6)
                .reviewCount(892)
                .build());
        
        products.add(Product.builder()
                .name("페이셜 마스크팩 10매")
                .description("수분 공급에 좋은 마스크팩입니다.")
                .price(14900L)
                .stock(60)
                .category("뷰티/화장품")
                .averageRating(4.5)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("향수 50ml")
                .description("은은하고 오래 지속되는 향수입니다.")
                .price(89000L)
                .stock(20)
                .category("뷰티/화장품")
                .averageRating(4.7)
                .reviewCount(423)
                .build());
        
        // 생활용품 카테고리
        products.add(Product.builder()
                .name("세탁 세제 대용량")
                .description("강력한 세정력의 세탁 세제입니다.")
                .price(18900L)
                .stock(40)
                .category("생활용품")
                .averageRating(4.6)
                .reviewCount(1234)
                .build());
        
        products.add(Product.builder()
                .name("화장지 30롤")
                .description("부드럽고 두꺼운 화장지입니다.")
                .price(12900L)
                .stock(50)
                .category("생활용품")
                .averageRating(4.7)
                .reviewCount(2156)
                .build());
        
        products.add(Product.builder()
                .name("세제 세트")
                .description("다양한 용도의 세제 세트입니다.")
                .price(24900L)
                .stock(30)
                .category("생활용품")
                .averageRating(4.5)
                .reviewCount(789)
                .build());
        
        products.add(Product.builder()
                .name("청소용품 세트")
                .description("깨끗한 청소를 위한 용품 세트입니다.")
                .price(18900L)
                .stock(35)
                .category("생활용품")
                .averageRating(4.6)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("수건 세트 5장")
                .description("부드럽고 흡수력이 좋은 수건 세트입니다.")
                .price(29000L)
                .stock(25)
                .category("생활용품")
                .averageRating(4.7)
                .reviewCount(892)
                .build());
        
        // 유아동 카테고리
        products.add(Product.builder()
                .name("유아 장난감 세트")
                .description("안전한 소재의 유아 장난감 세트입니다.")
                .price(39000L)
                .stock(20)
                .category("유아동")
                .averageRating(4.8)
                .reviewCount(456)
                .build());
        
        products.add(Product.builder()
                .name("아기 옷 세트")
                .description("부드러운 소재의 아기 옷 세트입니다.")
                .price(29000L)
                .stock(30)
                .category("유아동")
                .averageRating(4.7)
                .reviewCount(678)
                .build());
        
        products.add(Product.builder()
                .name("유아용 식기 세트")
                .description("안전한 소재의 유아용 식기 세트입니다.")
                .price(18900L)
                .stock(25)
                .category("유아동")
                .averageRating(4.6)
                .reviewCount(423)
                .build());
        
        products.add(Product.builder()
                .name("아기 침대용품 세트")
                .description("편안한 수면을 위한 침대용품 세트입니다.")
                .price(49000L)
                .stock(15)
                .category("유아동")
                .averageRating(4.8)
                .reviewCount(312)
                .build());
        
        products.add(Product.builder()
                .name("유아 장난감 블록")
                .description("창의력을 키우는 블록 장난감입니다.")
                .price(34900L)
                .stock(28)
                .category("유아동")
                .averageRating(4.7)
                .reviewCount(567)
                .build());
        
        // 반려동물용품 카테고리
        products.add(Product.builder()
                .name("강아지 사료 10kg")
                .description("영양이 풍부한 강아지 사료입니다.")
                .price(59000L)
                .stock(20)
                .category("반려동물용품")
                .averageRating(4.8)
                .reviewCount(1234)
                .build());
        
        products.add(Product.builder()
                .name("고양이 모래 20L")
                .description("흡수력이 좋은 고양이 모래입니다.")
                .price(18900L)
                .stock(35)
                .category("반려동물용품")
                .averageRating(4.7)
                .reviewCount(892)
                .build());
        
        products.add(Product.builder()
                .name("반려동물 장난감")
                .description("반려동물이 좋아하는 장난감입니다.")
                .price(14900L)
                .stock(40)
                .category("반려동물용품")
                .averageRating(4.6)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("강아지 목줄")
                .description("편안한 착용감의 목줄입니다.")
                .price(12900L)
                .stock(50)
                .category("반려동물용품")
                .averageRating(4.5)
                .reviewCount(423)
                .build());
        
        products.add(Product.builder()
                .name("반려동물 침대")
                .description("편안한 휴식을 위한 침대입니다.")
                .price(69000L)
                .stock(12)
                .category("반려동물용품")
                .averageRating(4.7)
                .reviewCount(234)
                .build());
        
        // 자동차용품 카테고리
        products.add(Product.builder()
                .name("자동차 시트 커버")
                .description("편안하고 세련된 시트 커버입니다.")
                .price(49000L)
                .stock(18)
                .category("자동차용품")
                .averageRating(4.6)
                .reviewCount(456)
                .build());
        
        products.add(Product.builder()
                .name("차량용 공기청정기")
                .description("깨끗한 공기를 위한 공기청정기입니다.")
                .price(89000L)
                .stock(15)
                .category("자동차용품")
                .averageRating(4.7)
                .reviewCount(678)
                .build());
        
        products.add(Product.builder()
                .name("자동차 와이퍼")
                .description("깨끗한 시야를 위한 와이퍼입니다.")
                .price(18900L)
                .stock(30)
                .category("자동차용품")
                .averageRating(4.5)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("차량용 충전기")
                .description("빠른 충전이 가능한 차량용 충전기입니다.")
                .price(24900L)
                .stock(25)
                .category("자동차용품")
                .averageRating(4.6)
                .reviewCount(423)
                .build());
        
        products.add(Product.builder()
                .name("자동차 방향제")
                .description("은은한 향의 자동차 방향제입니다.")
                .price(12900L)
                .stock(40)
                .category("자동차용품")
                .averageRating(4.7)
                .reviewCount(789)
                .build());
        
        // 여행 카테고리
        products.add(Product.builder()
                .name("여행용 캐리어 24인치")
                .description("가볍고 튼튼한 여행용 캐리어입니다.")
                .price(129000L)
                .stock(20)
                .category("여행")
                .averageRating(4.8)
                .reviewCount(1234)
                .build());
        
        products.add(Product.builder()
                .name("여행용 백팩")
                .description("실용적인 여행용 백팩입니다.")
                .price(69000L)
                .stock(25)
                .category("여행")
                .averageRating(4.7)
                .reviewCount(892)
                .build());
        
        products.add(Product.builder()
                .name("여행용 세트")
                .description("여행에 필요한 용품 세트입니다.")
                .price(49000L)
                .stock(18)
                .category("여행")
                .averageRating(4.6)
                .reviewCount(567)
                .build());
        
        products.add(Product.builder()
                .name("여행용 어댑터")
                .description("다양한 국가에서 사용 가능한 어댑터입니다.")
                .price(18900L)
                .stock(35)
                .category("여행")
                .averageRating(4.5)
                .reviewCount(423)
                .build());
        
        products.add(Product.builder()
                .name("여행용 목베개")
                .description("편안한 여행을 위한 목베개입니다.")
                .price(24900L)
                .stock(30)
                .category("여행")
                .averageRating(4.7)
                .reviewCount(678)
                .build());
        
        return products;
    }
}
