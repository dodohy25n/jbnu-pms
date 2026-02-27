package jbnu.jbnupms.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일 업로드
     */
    public String uploadFile(MultipartFile multipartFile, String dirName) {
        validateFile(multipartFile);

        String fileName = createFileName(multipartFile.getOriginalFilename(), dirName);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(bucket, fileName, inputStream, objectMetadata);
            log.info("S3 파일 업로드 성공: {}", fileName);
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    /**
     * S3에서 파일 삭제
     */
    public void deleteFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
            log.info("S3 파일 삭제 성공: {}", fileName);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * S3에서 파일 다운로드
     */
    public Resource downloadFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            S3Object s3Object = amazonS3.getObject(bucket, fileName);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * 파일명 생성 (UUID + 원본 파일명)
     */
    private String createFileName(String originalFileName, String dirName) {
        String ext = extractExt(originalFileName);
        String uuid = UUID.randomUUID().toString();
        return dirName + "/" + uuid + ext;
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExt(String originalFileName) {
        int pos = originalFileName.lastIndexOf(".");
        return originalFileName.substring(pos);
    }

    /**
     * S3 URL에서 파일명 추출
     */
    private String extractFileNameFromUrl(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();

            // 맨 앞의 '/' 제거
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // path-style URL인 경우 버킷명이 path에 포함될 수 있으므로 제거
            if (path.startsWith(bucket + "/")) {
                return path.substring(bucket.length() + 1);
            }

            return path;
        } catch (Exception e) {
            log.error("S3 파일 URL 파싱 오류: {}, error: {}", fileUrl, e.getMessage());
            // 예외 발생 시 기존 방식(fallback)으로 처리
            return fileUrl.substring(fileUrl.indexOf(bucket) + bucket.length() + 1);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_FILE);
        }

        // 파일 크기 제한 (10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }
}