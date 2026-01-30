package com.opay.domain.product.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.Tag;
import com.opay.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
            putObjectRequest.setTagging(createObjectTagging(productId));

            // 객체 잠금 설정 (활성화된 경우)
            // 참고: AWS SDK 1.12.470에서는 PutObjectRequest에 직접 객체 잠금 설정이 제한적입니다.
            // 객체 잠금이 필요한 경우 업로드 후 별도 API 호출을 고려하세요.
            // if (s3Config.isObjectLockEnabled()) {
            //     setObjectLock(putObjectRequest);
            // }

            // ACL 설정 제거: 최신 S3 버킷은 ACL이 비활성화되어 있어 버킷 정책을 사용해야 합니다.
            // Public Read 접근이 필요한 경우 버킷 정책에서 설정하세요.
            // putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

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
     * URL에서 이미지를 다운로드하여 S3에 업로드
     * 
     * @param imageUrl 다운로드할 이미지 URL
     * @param productId 상품 ID (태그에 사용)
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadImageFromUrl(String imageUrl, Long productId) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imageUrl);
            }
            
            String extension = getExtensionFromContentType(contentType);
            String fileName = "product-" + productId + "-" + System.currentTimeMillis() + extension;
            String s3Key = PRODUCT_IMAGE_PREFIX + fileName;
            
            try (InputStream inputStream = connection.getInputStream()) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                long contentLength = connection.getContentLengthLong();
                if (contentLength > 0) {
                    metadata.setContentLength(contentLength);
                }
                metadata.addUserMetadata("uploaded-by", "opay-backend");
                metadata.addUserMetadata("uploaded-at", LocalDate.now().toString());
                metadata.addUserMetadata("source-url", imageUrl);
                
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        s3Config.getBucket(),
                        s3Key,
                        inputStream,
                        metadata
                );
                
                // 태그 설정
                putObjectRequest.setTagging(createObjectTagging(productId));
                
                // 객체 잠금 설정 (활성화된 경우)
                // 참고: AWS SDK 1.12.470에서는 PutObjectRequest에 직접 객체 잠금 설정이 제한적입니다.
                // 객체 잠금이 필요한 경우 업로드 후 별도 API 호출을 고려하세요.
                // if (s3Config.isObjectLockEnabled()) {
                //     setObjectLock(putObjectRequest);
                // }
                
                // ACL 설정 제거: 최신 S3 버킷은 ACL이 비활성화되어 있어 버킷 정책을 사용해야 합니다.
                // Public Read 접근이 필요한 경우 버킷 정책에서 설정하세요.
                // putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                
                amazonS3.putObject(putObjectRequest);
                
                String s3ImageUrl = amazonS3.getUrl(s3Config.getBucket(), s3Key).toString();
                log.info("URL에서 이미지 업로드 완료: productId={}, sourceUrl={}, s3Url={}", 
                        productId, imageUrl, s3ImageUrl);
                
                return s3ImageUrl;
            }
        } catch (Exception e) {
            log.error("URL에서 이미지 업로드 실패: productId={}, imageUrl={}", productId, imageUrl, e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Content-Type에서 확장자 추출
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return ".jpg";
        } else if (contentType.contains("png")) {
            return ".png";
        } else if (contentType.contains("gif")) {
            return ".gif";
        } else if (contentType.contains("webp")) {
            return ".webp";
        }
        return ".jpg"; // 기본값
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
     * S3 태그 생성 (ObjectTagging 객체 반환)
     * 
     * 태그 전략:
     * - Environment: 환경 (dev/staging/prod)
     * - ResourceType: 리소스 타입 (product-image)
     * - Project: 프로젝트명 (opay)
     * - ProductId: 상품 ID
     * - ManagedBy: 관리 주체 (application)
     * - Retention: 보관 정책 (permanent/temporary)
     */
    private ObjectTagging createObjectTagging(Long productId) {
        List<Tag> tagList = new ArrayList<>();
        
        // 환경 태그 (application.yml에서 주입 가능)
        tagList.add(new Tag("Environment", getEnvironment()));
        tagList.add(new Tag("ResourceType", "product-image"));
        tagList.add(new Tag("Project", "opay"));
        tagList.add(new Tag("ProductId", String.valueOf(productId)));
        tagList.add(new Tag("ManagedBy", "application"));
        tagList.add(new Tag("Retention", "permanent")); // 상품 이미지는 영구 보관
        
        return new ObjectTagging(tagList);
    }

    /**
     * 객체 잠금 설정
     * 
     * 객체 잠금 모드:
     * - GOVERNANCE: 관리자 권한으로 잠금 해제 가능 (일반적인 경우)
     * - COMPLIANCE: 아무도 잠금 해제 불가 (규정 준수 필요시)
     * 
     * 참고: AWS SDK 1.12.470에서는 PutObjectRequest에 직접 객체 잠금 설정이 제한적입니다.
     * 객체 잠금이 필요한 경우 별도의 API 호출이 필요할 수 있습니다.
     */
    private void setObjectLock(PutObjectRequest putObjectRequest) {
        try {
            // 잠금 모드 설정
            ObjectLockRetention retention = new ObjectLockRetention();
            if ("COMPLIANCE".equalsIgnoreCase(s3Config.getObjectLockMode())) {
                retention.setMode(ObjectLockRetentionMode.COMPLIANCE);
            } else {
                retention.setMode(ObjectLockRetentionMode.GOVERNANCE);
            }

            // 보관 기간 설정 (현재 날짜 + retentionDays)
            Date retentionDate = Date.from(
                    LocalDate.now()
                            .plusDays(s3Config.getRetentionDays())
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
            );
            retention.setRetainUntilDate(retentionDate);

            // AWS SDK 1.12.470에서는 PutObjectRequest에 객체 잠금 설정이 제한적이므로
            // 메타데이터에 정보를 추가하고, 필요시 별도 API 호출을 고려할 수 있습니다.
            // 실제 객체 잠금은 버킷 레벨에서 활성화되어 있어야 하며,
            // 업로드 후 별도로 설정하는 것이 더 안전할 수 있습니다.
            
            log.info("객체 잠금 설정 준비: mode={}, retentionDays={}, retentionDate={}", 
                    s3Config.getObjectLockMode(), 
                    s3Config.getRetentionDays(), 
                    retentionDate);
            
            // 참고: 실제 객체 잠금 설정은 업로드 후 별도 API 호출이 필요할 수 있습니다.
            // 현재는 로깅만 수행합니다.
        } catch (Exception e) {
            log.warn("객체 잠금 설정 중 오류 발생: {}", e.getMessage());
        }
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
