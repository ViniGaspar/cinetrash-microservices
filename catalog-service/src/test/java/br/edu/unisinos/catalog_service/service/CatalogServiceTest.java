package br.edu.unisinos.catalog_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.catalog_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.catalog_service.dto.TmdbMovieDTO;
import br.edu.unisinos.catalog_service.model.Movie;
import br.edu.unisinos.catalog_service.repository.MovieRepository;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

	@Mock
	private MovieRepository repository;

	@Mock
	private TmdbService tmdbService;

	@InjectMocks
	private CatalogService service;

	private static Movie movie(Long id, Long externalId, String title) {
		Movie m = new Movie();
		m.setId(id);
		m.setExternalId(externalId);
		m.setTitle(title);
		m.setPosterPath("https://image.tmdb.org/t/p/w500/x.jpg");
		m.setReleaseYear(1999);
		return m;
	}

	@Test
	void registerReturnsExistingMovieWithoutCallingTmdb() {
		when(repository.findByExternalId(603L)).thenReturn(Optional.of(movie(1L, 603L, "Matrix")));

		CatalogService.RegisterResult result = service.register(603L);

		assertThat(result.created()).isFalse();
		assertThat(result.movie().movieId()).isEqualTo(1L);
		verify(tmdbService, never()).getMovieDetails(anyLong());
		verify(repository, never()).save(any());
	}

	@Test
	void registerFetchesTmdbAndSavesNewMovie() {
		when(repository.findByExternalId(603L)).thenReturn(Optional.empty());
		when(tmdbService.getMovieDetails(603L))
				.thenReturn(new TmdbMovieDTO(603L, "Matrix", "https://image.tmdb.org/t/p/w500/x.jpg", 1999));
		when(repository.save(any(Movie.class))).thenAnswer(inv -> {
			Movie saved = inv.getArgument(0);
			saved.setId(7L);
			return saved;
		});

		CatalogService.RegisterResult result = service.register(603L);

		assertThat(result.created()).isTrue();
		assertThat(result.movie().movieId()).isEqualTo(7L);
		assertThat(result.movie().externalId()).isEqualTo(603L);
		assertThat(result.movie().title()).isEqualTo("Matrix");
		assertThat(result.movie().releaseYear()).isEqualTo(1999);
	}

	@Test
	void randomPairFailsWithBadRequestWhenCatalogHasLessThanTwoMovies() {
		when(repository.findRandomPair()).thenReturn(List.of(movie(1L, 603L, "Matrix")));

		assertThatThrownBy(() -> service.randomPair())
				.isInstanceOf(ResponseStatusException.class)
				.extracting(e -> ((ResponseStatusException) e).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void randomPairReturnsTwoDistinctMovies() {
		when(repository.findRandomPair())
				.thenReturn(List.of(movie(1L, 603L, "Matrix"), movie(2L, 604L, "Matrix Reloaded")));

		RandomPairResponseDTO pair = service.randomPair();

		assertThat(pair.movieA().movieId()).isNotEqualTo(pair.movieB().movieId());
	}

	@Test
	void findByIdFailsWithNotFoundWhenMovieDoesNotExist() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(99L))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(e -> ((ResponseStatusException) e).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
