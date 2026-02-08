package jbnu.jbnupms.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuditLogger {
    // todo: 스레드, 프로세스 관점에서 동시에 기록되면 어떻게 관리할지, 파일 저장 ELK 적용
    private final ObjectMapper objectMapper;
    private static final String LOG_DIR = "logs/user-audit";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 사용자 감사 로그를 파일에 기록
     */
    public void log(UserAuditAction action, Long actorId, Long targetUserId,
                    Map<String, Object> beforeData, Map<String, Object> afterData) {
        try {
            // 로그 디렉토리 생성
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }

            // 날짜별 로그 파일명 생성
            String date = LocalDateTime.now().format(FORMATTER);
            String fileName = String.format("%s/user-audit-%s.log", LOG_DIR, date);

            // 로그 데이터 구성
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", LocalDateTime.now().toString());
            logData.put("action", action.name());
            logData.put("actorId", actorId != null ? actorId : "SYSTEM");
            logData.put("targetUserId", targetUserId);
            logData.put("beforeData", beforeData != null ? beforeData : Map.of());
            logData.put("afterData", afterData != null ? afterData : Map.of());

            // JSON 형식으로 변환
            String jsonLog = objectMapper.writeValueAsString(logData);

            // 파일에 기록
            try (FileWriter writer = new FileWriter(new File(fileName), true)) {
                writer.write(jsonLog + "\n");
            }

            log.info("User audit log recorded: action={}, actorId={}, targetUserId={}",
                    action, actorId, targetUserId);

        } catch (IOException e) {
            log.error("Failed to write user audit log", e);
        }
    }

    /**
     * 회원가입 로그
     */
    public void logRegister(Long userId, String email, String name, String provider) {
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("email", email);
        afterData.put("name", name);
        afterData.put("provider", provider);

        log(UserAuditAction.REGISTER, userId, userId, null, afterData);
    }

    /**
     * 프로필 업데이트 로그
     */
    public void logUpdateProfile(Long actorId, Long targetUserId,
                                 String oldName, String newName,
                                 String oldProfileImage, String newProfileImage) {
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("name", oldName);
        beforeData.put("profileImage", oldProfileImage);

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("name", newName);
        afterData.put("profileImage", newProfileImage);

        log(UserAuditAction.UPDATE_PROFILE, actorId, targetUserId, beforeData, afterData);
    }

    /**
     * 비밀번호 변경 로그
     */
    public void logChangePassword(Long userId) {
        Map<String, Object> afterData = new HashMap<>();
        afterData.put("passwordChanged", true);

        log(UserAuditAction.CHANGE_PASSWORD, userId, userId, null, afterData);
    }

    /**
     * 회원 탈퇴 로그
     */
    public void logDelete(Long actorId, Long targetUserId, String email, String reason) {
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("email", email);
        beforeData.put("isDeleted", false);

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("isDeleted", true);
        afterData.put("reason", reason);

        log(UserAuditAction.DELETE, actorId, targetUserId, beforeData, afterData);
    }

    /**
     * 관리자에 의한 사용자 정보 수정 로그
     */
    public void logAdminUpdate(Long adminId, Long targetUserId,
                               Map<String, Object> beforeData,
                               Map<String, Object> afterData) {
        log(UserAuditAction.ADMIN_UPDATE, adminId, targetUserId, beforeData, afterData);
    }
}