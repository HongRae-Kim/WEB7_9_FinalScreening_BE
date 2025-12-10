package com.back.matchduo.domain.party.entity;

import jakarta.persistence.*;

@Entity
public class Party {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
