package com.back.matchduo.domain.gameaccount.entity;

import jakarta.persistence.*;

@Entity
public class GameAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
