package br.edu.unisinos.ranking_service.dto;

import java.time.LocalDateTime;

public record RankingItemDTO(
		Long movieId,
		Long externalId,
		String title,
		String posterPath,
		Integer releaseYear,
		Long badnessCount,
		LocalDateTime lastVoteAt
) {}
