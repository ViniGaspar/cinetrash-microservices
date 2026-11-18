package br.edu.unisinos.catalog_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unisinos.catalog_service.dto.MovieDTO;
import br.edu.unisinos.catalog_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.catalog_service.dto.RegisterMovieRequestDTO;
import br.edu.unisinos.catalog_service.dto.SearchResponseDTO;
import br.edu.unisinos.catalog_service.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Path completo com /catalog: o gateway repassa o caminho sem StripPrefix
@RestController
@RequestMapping("/catalog/movies")
public class MovieController {
	private final CatalogService service;

	public MovieController(CatalogService service) {
		this.service = service;
	}

	// GET /catalog/movies/search?query=matrix&page=1
	@GetMapping("/search")
	public SearchResponseDTO search(
			@RequestParam @NotBlank String query,
			@RequestParam(defaultValue = "1") @Min(1) @Max(500) int page) {
		return service.search(query, page);
	}

	// POST /catalog/movies {"externalId": 603} -> 201 se criou, 200 se ja existia
	@PostMapping
	public ResponseEntity<MovieDTO> register(@RequestBody @Valid RegisterMovieRequestDTO request) {
		CatalogService.RegisterResult result = service.register(request.externalId());
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.movie());
	}

	// GET /catalog/movies/random-pair (usado pelo battle-service)
	@GetMapping("/random-pair")
	public RandomPairResponseDTO randomPair() {
		return service.randomPair();
	}

	// GET /catalog/movies/{id} (usado pelo battle-service)
	@GetMapping("/{id}")
	public MovieDTO findById(@PathVariable Long id) {
		return service.findById(id);
	}

	// GET /catalog/movies
	@GetMapping
	public List<MovieDTO> findAll() {
		return service.findAll();
	}
}
