package br.edu.unisinos.catalog_service.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Resposta paginada do endpoint /search/movie do TMDB
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResponse(
		int page,
		List<TmdbMovie> results,
		@JsonProperty("total_pages") int totalPages,
		@JsonProperty("total_results") int totalResults
){}
