package com.back.matchduo.domain.gameaccount.repository;

import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameAccountRepository extends JpaRepository<GameAccount,Long> {
}
