package com.back.matchduo.domain.party.repository;

import com.back.matchduo.domain.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {
}
