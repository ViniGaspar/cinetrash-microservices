package br.edu.unisinos.ranking_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.unisinos.ranking_service.model.RankedMovie;

public interface RankedMovieRepository extends JpaRepository<RankedMovie, Long> {
	Optional<RankedMovie> findByMovieId(Long movieId);
}
