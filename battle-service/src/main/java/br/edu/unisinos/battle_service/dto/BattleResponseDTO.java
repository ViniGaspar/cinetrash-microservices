package br.edu.unisinos.battle_service.dto;

public record BattleResponseDTO(
		Long battleId,
		MovieDTO movieA,
		MovieDTO movieB
){}
