package br.edu.unisinos.battle_service.dto;

public record MovieDTO(
		Long movieId,
		Long externalId,
		String title,
		String posterPath,
		Integer releaseYear
){}
