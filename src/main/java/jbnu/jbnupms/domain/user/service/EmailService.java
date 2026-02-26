package jbnu.jbnupms.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.entity.VerificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String code, VerificationType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(getSubject(type));
            helper.setText(getEmailContent(code, type), true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String getSubject(VerificationType type) {
        return switch (type) {
            case REGISTER -> "[JBNU PMS] 회원가입 이메일 인증";
            case PASSWORD_RESET -> "[JBNU PMS] 비밀번호 재설정 인증";
        };
    }

    private String getEmailContent(String code, VerificationType type) {
        String purpose = type == VerificationType.REGISTER ? "회원가입" : "비밀번호 재설정";

        return """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #4CAF50;">JBNU PMS %s 인증</h2>
                        <p>안녕하세요,</p>
                        <p>%s을 위한 인증 코드입니다:</p>
                        <div style="background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0; border-radius: 5px;">
                            %s
                        </div>
                        <p style="color: #666;">이 인증 코드는 <strong>5분간</strong> 유효합니다.</p>
                        <p style="color: #999; font-size: 12px; margin-top: 30px;">
                            본인이 요청하지 않은 경우 이 이메일을 무시하셔도 됩니다.
                        </p>
                    </div>
                </body>
                </html>
                """
                .formatted(purpose, purpose, code);
    }
}