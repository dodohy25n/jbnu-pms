package jbnu.jbnupms.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    /* todo (1) : users db에서 특정 회원의 데이터가 삭제될 떄,
        해당 회원 id를 참조하는 테이블(comment, task 등)에서의 데이터도 Cascade로 삭제되는데
        그렇다면 화면 상에서 삭제된 데이터는 어떻게 나타내어야 할까요?
        (우선 회원이 탈퇴하면 withdrawnUser 테이블에 추가되고, users 테이블에서는 isDeleted만 업데이트 되도록 구현했습니다) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* todo (2) : email의 unique 조건으로, 탈퇴 후 같은 이메일로 재가입이 불가능하도록 구현한 상태인데
        재가입 같은 경우 어떻게 처리되어야 할까요? */
    // todo (3): (2/4) 디스코드에 정리하여 질문
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 60)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 1000)
    private String profileImage;

    // "EMAIL,GOOGLE" 형태로 저장
    @Setter
    @Column(length = 20, nullable = false)
    private String provider = "EMAIL";

    @Setter
    @Column(length = 255)
    private String providerId;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Builder
    public User(String email, String password, String name, String profileImage, String provider, String providerId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider != null ? provider : "EMAIL";
        this.providerId = providerId;
        this.isDeleted = false;
    }

    // 정보 수정을 위한 메서드
    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateEmail(String email) { this.email = email; }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }

}