package br.edu.unisinos.battle_service.model;

import org.springframework.stereotype.Component;

import br.edu.unisinos.battle_service.client.CatalogClient;
import br.edu.unisinos.battle_service.dto.MovieDTO;
import br.edu.unisinos.battle_service.dto.RandomPairResponseDTO;

@Component
public class CatalogMock implements CatalogClient{
	@Override
	public RandomPairResponseDTO getRandomPair() {
		MovieDTO movie1 = new MovieDTO(1L, 999L, "How To Train Your Dragon", "/poster1.jpg", 2010);
        MovieDTO movie2 = new MovieDTO(2L, 1000L, "Black Swan", "/poster2.jpg", 2010);
		
        return new RandomPairResponseDTO(movie1, movie2);
	}
	
	@Override
	public MovieDTO getMovieById(Long id) {
		return new MovieDTO(id, 999L, "Filme Falso Mockado!", "/fake.jpg", 2024);
	}
}
