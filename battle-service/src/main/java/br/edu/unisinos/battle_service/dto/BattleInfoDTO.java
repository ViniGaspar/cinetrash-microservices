package br.edu.unisinos.battle_service.dto;

import java.time.LocalDateTime;

public record BattleInfoDTO(
		Long id, 
        Long movieAId, 
        Long movieBId, 
        Long chosenMovieId, 
        LocalDateTime createdAt, 
        LocalDateTime votedAt
){}
