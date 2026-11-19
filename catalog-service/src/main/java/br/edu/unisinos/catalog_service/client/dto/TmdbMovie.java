package br.edu.unisinos.catalog_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Filme como o TMDB devolve (usado tanto na busca quanto nos detalhes)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovie(
		Long id,
		String title,
		@JsonProperty("poster_path") String posterPath,
		@JsonProperty("release_date") String releaseDate
){}
