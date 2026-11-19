package br.edu.unisinos.battle_service.dto;

public record UndoResponseDTO(
		String undoneAction,
		int remainingHistorySize
) {}
