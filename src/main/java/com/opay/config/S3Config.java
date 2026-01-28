package com.opay.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 설정 클래스
 * S3 클라이언트 빈을 생성하고 버킷 정보를 관리
 */
@Configuration
@Getter
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.object-lock.enabled:false}")
    private boolean objectLockEnabled;

    @Value("${aws.s3.object-lock.mode:GOVERNANCE}")
    private String objectLockMode; // GOVERNANCE or COMPLIANCE

    @Value("${aws.s3.object-lock.retention-days:30}")
    private int retentionDays;

    @Value("${aws.environment:dev}")
    private String environment;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}
