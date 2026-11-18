package br.edu.unisinos.catalog_service.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.catalog_service.dto.MovieDTO;
import br.edu.unisinos.catalog_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.catalog_service.dto.SearchResponseDTO;
import br.edu.unisinos.catalog_service.dto.TmdbMovieDTO;
import br.edu.unisinos.catalog_service.model.Movie;
import br.edu.unisinos.catalog_service.repository.MovieRepository;

@Service
public class CatalogService {
	private final MovieRepository repository;
	private final TmdbService tmdbService;

	public CatalogService(MovieRepository repository, TmdbService tmdbService) {
		this.repository = repository;
		this.tmdbService = tmdbService;
	}

	// Resultado do cadastro: o filme e se ele foi criado agora (201) ou ja existia (200)
	public record RegisterResult(MovieDTO movie, boolean created) {}

	// RN-01: busca por nome no TMDB
	public SearchResponseDTO search(String query, int page) {
		return tmdbService.search(query.trim(), page);
	}

	// RN-02: registro idempotente por externalId
	public RegisterResult register(Long externalId) {
		var existing = repository.findByExternalId(externalId);
		if (existing.isPresent()) {
			return new RegisterResult(toDto(existing.get()), false);
		}

		TmdbMovieDTO details = tmdbService.getMovieDetails(externalId);

		Movie movie = new Movie();
		movie.setExternalId(details.externalId());
		movie.setTitle(details.title());
		movie.setPosterPath(details.posterPath());
		movie.setReleaseYear(details.releaseYear());

		try {
			return new RegisterResult(toDto(repository.save(movie)), true);
		} catch (DataIntegrityViolationException e) {
			// Duas requisicoes simultaneas para o mesmo filme: a outra gravou primeiro
			return repository.findByExternalId(externalId)
					.map(m -> new RegisterResult(toDto(m), false))
					.orElseThrow(() -> e);
		}
	}

	// RN-06, RN-07, RN-08: dois filmes distintos sorteados no banco
	public RandomPairResponseDTO randomPair() {
		List<Movie> pair = repository.findRandomPair();
		if (pair.size() < 2) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"É preciso ter pelo menos 2 filmes cadastrados no catálogo para iniciar uma batalha.");
		}
		return new RandomPairResponseDTO(toDto(pair.get(0)), toDto(pair.get(1)));
	}

	public MovieDTO findById(Long id) {
		return repository.findById(id)
				.map(this::toDto)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Filme " + id + " não encontrado no catálogo."));
	}

	public List<MovieDTO> findAll() {
		return repository.findAll().stream().map(this::toDto).toList();
	}

	private MovieDTO toDto(Movie movie) {
		return new MovieDTO(
				movie.getId(),
				movie.getExternalId(),
				movie.getTitle(),
				movie.getPosterPath(),
				movie.getReleaseYear());
	}
}
