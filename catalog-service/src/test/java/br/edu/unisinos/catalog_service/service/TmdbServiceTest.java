package br.edu.unisinos.catalog_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.catalog_service.client.TmdbClient;
import br.edu.unisinos.catalog_service.client.dto.TmdbMovie;
import br.edu.unisinos.catalog_service.dto.TmdbMovieDTO;
import feign.FeignException;
import feign.Request;

class TmdbServiceTest {
	private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

	private TmdbClient client;
	private TmdbService service;

	@BeforeEach
	void setUp() {
		client = mock(TmdbClient.class);
		service = new TmdbService(client, "fake-key", "pt-BR", IMAGE_BASE);
	}

	private static FeignException feignError(int status) {
		Request request = Request.create(Request.HttpMethod.GET, "https://api.themoviedb.org/3/movie/1",
				Map.of(), null, StandardCharsets.UTF_8, null);
		return FeignException.errorStatus("TmdbClient#getMovieDetails",
				feign.Response.builder().status(status).reason("x").request(request).headers(Map.of()).build());
	}

	@Test
	void mapsTmdbMovieToDto() {
		when(client.getMovieDetails(603L, "fake-key", "pt-BR"))
				.thenReturn(new TmdbMovie(603L, "Matrix", "/abc.jpg", "1999-03-31"));

		TmdbMovieDTO dto = service.getMovieDetails(603L);

		assertThat(dto.externalId()).isEqualTo(603L);
		assertThat(dto.title()).isEqualTo("Matrix");
		assertThat(dto.posterPath()).isEqualTo(IMAGE_BASE + "/abc.jpg");
		assertThat(dto.releaseYear()).isEqualTo(1999);
	}

	@Test
	void posterUrlIsNullWhenTmdbHasNoPoster() {
		assertThat(service.buildPosterUrl(null)).isNull();
		assertThat(service.buildPosterUrl("")).isNull();
	}

	@Test
	void releaseYearIsNullWhenDateIsMissingOrInvalid() {
		assertThat(service.extractYear(null)).isNull();
		assertThat(service.extractYear("")).isNull();
		assertThat(service.extractYear("abcd-01-01")).isNull();
		assertThat(service.extractYear("2010-07-16")).isEqualTo(2010);
	}

	@Test
	void tmdbNotFoundBecomes404() {
		when(client.getMovieDetails(1L, "fake-key", "pt-BR")).thenThrow(feignError(404));

		assertThatThrownBy(() -> service.getMovieDetails(1L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(e -> ((ResponseStatusException) e).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void tmdbUnauthorizedBecomes502() {
		when(client.getMovieDetails(1L, "fake-key", "pt-BR")).thenThrow(feignError(401));

		assertThatThrownBy(() -> service.getMovieDetails(1L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(e -> ((ResponseStatusException) e).getStatusCode())
				.isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void missingApiKeyFailsFastWithoutCallingTmdb() {
		TmdbService noKey = new TmdbService(client, "", "pt-BR", IMAGE_BASE);

		assertThatThrownBy(() -> noKey.getMovieDetails(1L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(e -> ((ResponseStatusException) e).getStatusCode())
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
