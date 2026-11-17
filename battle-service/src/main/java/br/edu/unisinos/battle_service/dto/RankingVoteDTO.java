package br.edu.unisinos.battle_service.dto;

public record RankingVoteDTO(
		Long movieId,
		Long externalId,
		String title,
		String posterpath,
		int releaseYear
){}
