package br.edu.unisinos.catalog_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.unisinos.catalog_service.client.dto.TmdbMovie;
import br.edu.unisinos.catalog_service.client.dto.TmdbSearchResponse;

// Cliente da API externa The Movie Database (TMDB) - URL fixa, nao passa pelo Eureka
@FeignClient(name = "tmdb", url = "${tmdb.base-url}")
public interface TmdbClient {

	@GetMapping("/search/movie")
	TmdbSearchResponse searchMovies(
			@RequestParam("query") String query,
			@RequestParam("page") int page,
			@RequestParam("api_key") String apiKey,
			@RequestParam("language") String language);

	@GetMapping("/movie/{id}")
	TmdbMovie getMovieDetails(
			@PathVariable("id") Long id,
			@RequestParam("api_key") String apiKey,
			@RequestParam("language") String language);
}
