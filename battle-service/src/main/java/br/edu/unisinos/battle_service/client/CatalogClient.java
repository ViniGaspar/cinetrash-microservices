package br.edu.unisinos.battle_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import br.edu.unisinos.battle_service.dto.MovieDTO;
import br.edu.unisinos.battle_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.battle_service.model.CatalogMock;

// @FeignClient(name = "CATALOG-SERVICE")
public interface CatalogClient {

	@GetMapping("/catalog/movies/random-pair")
	RandomPairResponseDTO getRandomPair();
	
	@GetMapping("/catalog/movies/{id}")
	MovieDTO getMovieById(@PathVariable Long id);
}
