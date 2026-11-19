package br.edu.unisinos.catalog_service.service;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.catalog_service.client.TmdbClient;
import br.edu.unisinos.catalog_service.client.dto.TmdbMovie;
import br.edu.unisinos.catalog_service.client.dto.TmdbSearchResponse;
import br.edu.unisinos.catalog_service.dto.SearchResponseDTO;
import br.edu.unisinos.catalog_service.dto.TmdbMovieDTO;
import feign.FeignException;

// Encapsula o acesso ao TMDB: injeta chave/idioma, converte o formato do TMDB e traduz erros
@Service
public class TmdbService {
	private final TmdbClient client;
	private final String apiKey;
	private final String language;
	private final String imageBaseUrl;

	public TmdbService(
			TmdbClient client,
			@Value("${tmdb.api-key}") String apiKey,
			@Value("${tmdb.language}") String language,
			@Value("${tmdb.image-base-url}") String imageBaseUrl) {
		this.client = client;
		this.apiKey = apiKey;
		this.language = language;
		this.imageBaseUrl = imageBaseUrl;
	}

	public SearchResponseDTO search(String query, int page) {
		TmdbSearchResponse response = call(() -> client.searchMovies(query, page, apiKey, language));

		List<TmdbMovieDTO> results = response.results() == null
				? List.of()
				: response.results().stream().map(this::toDto).toList();

		return new SearchResponseDTO(response.page(), response.totalPages(), response.totalResults(), results);
	}

	public TmdbMovieDTO getMovieDetails(Long externalId) {
		return toDto(call(() -> client.getMovieDetails(externalId, apiKey, language)));
	}

	TmdbMovieDTO toDto(TmdbMovie movie) {
		return new TmdbMovieDTO(
				movie.id(),
				movie.title(),
				buildPosterUrl(movie.posterPath()),
				extractYear(movie.releaseDate()));
	}

	// RN-04: URL completa do poster, ou null se o TMDB nao tiver poster
	String buildPosterUrl(String posterPath) {
		if (posterPath == null || posterPath.isBlank()) {
			return null;
		}
		return imageBaseUrl + posterPath;
	}

	// RN-05: ano (4 digitos) de "YYYY-MM-DD", ou null se ausente/invalido
	Integer extractYear(String releaseDate) {
		if (releaseDate == null || releaseDate.length() < 4) {
			return null;
		}
		try {
			return Integer.parseInt(releaseDate.substring(0, 4));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private <T> T call(Supplier<T> request) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"TMDB_API_KEY não configurada no catalog-service.");
		}
		try {
			return request.get();
		} catch (FeignException.NotFound e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Filme não encontrado no TMDB.");
		} catch (FeignException e) {
			// 401 (chave invalida), 429 (limite), 5xx ou falha de conexao com o TMDB
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
					"Falha ao consultar o TMDB (status " + e.status() + ").");
		}
	}
}
