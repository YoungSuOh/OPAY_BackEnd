package com.opay.domain.product.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.opay.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * S3 이미지 업로드 서비스
 * Product 이미지를 S3에 저장하고 태그 및 객체 잠금을 설정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final AmazonS3 amazonS3;
    private final S3Config s3Config;

    @Value("${aws.environment:dev}")
    private String environment;

    private static final String PRODUCT_IMAGE_PREFIX = "products/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 이미지 업로드
     * 
     * @param file 업로드할 이미지 파일
     * @param productId 상품 ID (태그에 사용)
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadImage(MultipartFile file, Long productId) {
        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename(), productId);
        String s3Key = PRODUCT_IMAGE_PREFIX + fileName;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = createObjectMetadata(file);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    s3Config.getBucket(),
                    s3Key,
                    inputStream,
                    metadata
            );

            // 태그 설정
            putObjectRequest.setTagging(createTagging(productId));

            // 객체 잠금 설정 (활성화된 경우)
            if (s3Config.isObjectLockEnabled()) {
                setObjectLock(putObjectRequest);
            }

            // ACL 설정 (Public Read)
            putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3.putObject(putObjectRequest);

            String imageUrl = amazonS3.getUrl(s3Config.getBucket(), s3Key).toString();
            log.info("이미지 업로드 완료: productId={}, s3Key={}, url={}", productId, s3Key, imageUrl);

            return imageUrl;
        } catch (IOException e) {
            log.error("이미지 업로드 실패: productId={}", productId, e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지 삭제
     * 
     * @param imageUrl 삭제할 이미지의 S3 URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String s3Key = extractS3KeyFromUrl(imageUrl);
            
            // 객체 잠금이 설정된 경우 삭제 불가 (Governance 모드인 경우 관리자 권한 필요)
            if (s3Config.isObjectLockEnabled()) {
                log.warn("객체 잠금이 설정된 이미지는 삭제할 수 없습니다: s3Key={}", s3Key);
                // 필요시 잠금 해제 후 삭제하는 로직 추가 가능
                return;
            }

            amazonS3.deleteObject(s3Config.getBucket(), s3Key);
            log.info("이미지 삭제 완료: s3Key={}", s3Key);
        } catch (Exception e) {
            log.error("이미지 삭제 실패: imageUrl={}", imageUrl, e);
            throw new RuntimeException("이미지 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 형식입니다. 허용된 형식: JPEG, PNG, GIF, WEBP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "파일 크기가 너무 큽니다. 최대 크기: 10MB");
        }
    }

    /**
     * 파일명 생성
     */
    private String generateFileName(String originalFilename, Long productId) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "product-" + productId + "-" + System.currentTimeMillis() + extension;
    }

    /**
     * 객체 메타데이터 생성
     */
    private ObjectMetadata createObjectMetadata(MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        metadata.addUserMetadata("uploaded-by", "opay-backend");
        metadata.addUserMetadata("uploaded-at", LocalDate.now().toString());
        return metadata;
    }

    /**
     * S3 태그 생성
     * 
     * 태그 전략:
     * - Environment: 환경 (dev/staging/prod)
     * - ResourceType: 리소스 타입 (product-image)
     * - Project: 프로젝트명 (opay)
     * - ProductId: 상품 ID
     * - ManagedBy: 관리 주체 (application)
     * - Retention: 보관 정책 (permanent/temporary)
     */
    private String createTagging(Long productId) {
        Map<String, String> tags = new HashMap<>();
        
        // 환경 태그 (application.yml에서 주입 가능)
        tags.put("Environment", getEnvironment());
        tags.put("ResourceType", "product-image");
        tags.put("Project", "opay");
        tags.put("ProductId", String.valueOf(productId));
        tags.put("ManagedBy", "application");
        tags.put("Retention", "permanent"); // 상품 이미지는 영구 보관
        
        // 태그를 URL 인코딩된 문자열로 변환
        StringBuilder tagString = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (index > 0) {
                tagString.append("&");
            }
            tagString.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
            index++;
        }
        
        return tagString.toString();
    }

    /**
     * 객체 잠금 설정
     * 
     * 객체 잠금 모드:
     * - GOVERNANCE: 관리자 권한으로 잠금 해제 가능 (일반적인 경우)
     * - COMPLIANCE: 아무도 잠금 해제 불가 (규정 준수 필요시)
     */
    private void setObjectLock(PutObjectRequest putObjectRequest) {
        ObjectLockConfiguration objectLockConfig = new ObjectLockConfiguration();
        objectLockConfig.setObjectLockEnabled(ObjectLockEnabled.Enabled);

        // 잠금 모드 설정
        ObjectLockRetention retention = new ObjectLockRetention();
        if ("COMPLIANCE".equalsIgnoreCase(s3Config.getObjectLockMode())) {
            retention.setMode(ObjectLockRetentionMode.Compliance);
        } else {
            retention.setMode(ObjectLockRetentionMode.Governance);
        }

        // 보관 기간 설정 (현재 날짜 + retentionDays)
        Date retentionDate = Date.from(
                LocalDate.now()
                        .plusDays(s3Config.getRetentionDays())
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
        retention.setRetainUntilDate(retentionDate);

        putObjectRequest.setObjectLockRetention(retention);
        
        log.info("객체 잠금 설정: mode={}, retentionDays={}, retentionDate={}", 
                s3Config.getObjectLockMode(), 
                s3Config.getRetentionDays(), 
                retentionDate);
    }

    /**
     * 환경 정보 가져오기
     */
    private String getEnvironment() {
        return environment;
    }

    /**
     * S3 URL에서 키 추출
     */
    private String extractS3KeyFromUrl(String imageUrl) {
        try {
            // URL 형식: https://bucket-name.s3.region.amazonaws.com/products/filename
            // 또는: https://s3.region.amazonaws.com/bucket-name/products/filename
            if (imageUrl.contains(s3Config.getBucket())) {
                int bucketIndex = imageUrl.indexOf(s3Config.getBucket());
                int keyStartIndex = imageUrl.indexOf("/", bucketIndex + s3Config.getBucket().length()) + 1;
                return imageUrl.substring(keyStartIndex);
            }
            throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + imageUrl);
        } catch (Exception e) {
            log.error("S3 키 추출 실패: imageUrl={}", imageUrl, e);
            throw new IllegalArgumentException("S3 URL에서 키를 추출할 수 없습니다: " + imageUrl);
        }
    }
}
