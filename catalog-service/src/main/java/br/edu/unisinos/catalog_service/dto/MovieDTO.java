package br.edu.unisinos.catalog_service.dto;

// Filme cadastrado no catalogo (contrato consumido pelo battle-service)
public record MovieDTO(
		Long movieId,
		Long externalId,
		String title,
		String posterPath,
		Integer releaseYear
){}
