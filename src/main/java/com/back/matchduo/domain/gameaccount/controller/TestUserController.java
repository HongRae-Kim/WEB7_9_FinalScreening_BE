package com.back.matchduo.domain.gameaccount.controller;

import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 임시 User 생성 컨트롤러
 * User Entity와 UserRepository를 사용 (User Entity 파일은 수정하지 않음)
 * GameAccount 테스트용으로만 사용
 * 나중에 User 관련 작업이 완성되면 삭제 예정
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestUserController {

    private final UserRepository userRepository;

    /**
     * 테스트용 User 생성 (임시)
     * User Entity와 UserRepository 사용 (User Entity 파일은 수정하지 않음)
     * User 도메인 작업이 완성되면 삭제 예정
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        // User Entity를 사용하여 User 생성 (User Entity 파일은 수정하지 않음)
        // User Entity에 id만 있으므로, JPA가 자동으로 ID를 생성
        User user = new User();
        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "테스트용 User가 생성되었습니다.");
        response.put("userId", savedUser.getId());
        response.put("note", "이 엔드포인트는 테스트용입니다. User 도메인 작업 완료 후 삭제 예정입니다.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

