package br.edu.unisinos.ranking_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.unisinos.ranking_service.dto.RankingItemDTO;
import br.edu.unisinos.ranking_service.model.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
	@Query("""
            SELECT new br.edu.unisinos.ranking_service.dto.RankingItemDTO(
                rm.movieId,
                rm.externalId,
                rm.title,
                rm.posterPath,
                rm.releaseYear,
                COUNT(v.id),
                MAX(v.votedAt)
            )
            FROM Vote v
            JOIN v.rankedMovie rm
            GROUP BY rm.id, rm.movieId, rm.externalId, rm.title, rm.posterPath, rm.releaseYear
            ORDER BY COUNT(v.id) DESC, MAX(v.votedAt) DESC
            """)
	
	List<RankingItemDTO> findTopRankedMovies(Pageable pageable);

    Optional<Vote> findFirstByRankedMovie_MovieIdOrderByVotedAtDescIdDesc(Long movieId);
}
