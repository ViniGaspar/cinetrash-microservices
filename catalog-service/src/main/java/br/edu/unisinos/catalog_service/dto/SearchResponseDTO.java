package br.edu.unisinos.catalog_service.dto;

import java.util.List;

public record SearchResponseDTO(
		int page,
		int totalPages,
		int totalResults,
		List<TmdbMovieDTO> results
){}
