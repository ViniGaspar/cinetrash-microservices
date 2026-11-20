package br.edu.unisinos.ranking_service.dto;

public record RankingVoteRequestDTO(
		Long movieId,
		Long externalId,
		String title,
		String posterPath,
		Integer releaseYear
) {}
