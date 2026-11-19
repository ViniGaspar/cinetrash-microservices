package br.edu.unisinos.catalog_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.unisinos.catalog_service.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	Optional<Movie> findByExternalId(Long externalId);

	// Sorteio direto no PostgreSQL: LIMIT 2 sobre linhas distintas ja garante dois filmes diferentes
	@Query(value = "SELECT * FROM movies ORDER BY RANDOM() LIMIT 2", nativeQuery = true)
	List<Movie> findRandomPair();
}
