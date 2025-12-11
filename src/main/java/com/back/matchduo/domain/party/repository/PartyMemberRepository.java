package com.back.matchduo.domain.party.repository;

import com.back.matchduo.domain.party.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    // 1. 특정 파티의 멤버 목록 조회 (가입된 상태만 가져오거나, 전체 다 가져오거나)
    // 용도: GET /api/v1/parties/{partyId}/members (파티원 목록 보여주기)
    List<PartyMember> findByPartyId(Long partyId);

    // 2. 내가 참여한 파티 목록 조회
    // 용도: GET /api/v1/users/me/parties (내 파티 목록)
    List<PartyMember> findByUserId(Long userId);

    // 3. 이미 참여했는지 확인 (중복 참여 방지)
    // 용도: 파티 참여 신청 시 검증 로직 (Validation)
    boolean existsByPartyIdAndUserId(Long partyId, Long userId);

    // 4. 특정 파티에서 내 멤버 정보 찾기
    // 용도: 탈퇴하거나 강퇴할 때 내 멤버 ID를 찾기 위해 사용
    Optional<PartyMember> findByPartyIdAndUserId(Long partyId, Long userId);
}