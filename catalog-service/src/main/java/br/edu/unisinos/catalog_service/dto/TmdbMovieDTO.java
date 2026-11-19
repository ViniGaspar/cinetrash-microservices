package br.edu.unisinos.catalog_service.dto;

// Filme vindo da busca no TMDB (ainda nao cadastrado, por isso nao tem movieId)
public record TmdbMovieDTO(
		Long externalId,
		String title,
		String posterPath,
		Integer releaseYear
){}
