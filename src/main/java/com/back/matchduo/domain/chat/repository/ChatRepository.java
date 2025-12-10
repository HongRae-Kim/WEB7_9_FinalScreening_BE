package com.back.matchduo.domain.chat.repository;

import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<GameAccount,Long> {
}
