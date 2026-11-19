package br.edu.unisinos.catalog_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterMovieRequestDTO(
		@NotNull(message = "externalId é obrigatório")
		@Positive(message = "externalId deve ser positivo")
		Long externalId
){}
