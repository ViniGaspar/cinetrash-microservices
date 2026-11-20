package br.edu.unisinos.ranking_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.ranking_service.dto.RankingItemDTO;
import br.edu.unisinos.ranking_service.dto.RankingVoteRequestDTO;
import br.edu.unisinos.ranking_service.model.RankedMovie;
import br.edu.unisinos.ranking_service.model.Vote;
import br.edu.unisinos.ranking_service.repository.RankedMovieRepository;
import br.edu.unisinos.ranking_service.repository.VoteRepository;
import jakarta.transaction.Transactional;

@Service
public class RankingService {
	@Autowired
	private RankedMovieRepository rankedMovieRepository;
	
	@Autowired
	private VoteRepository voteRepository;
	
	@Transactional
	public void registerVote(RankingVoteRequestDTO request) {
		if (request.movieId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MovieId é obrigatório");
		}
		
		RankedMovie movie = rankedMovieRepository.findByMovieId(request.movieId())
												 .orElseGet(() -> {
													 RankedMovie newMovie = new RankedMovie();
													 newMovie.setMovieId(request.movieId());
													 newMovie.setExternalId(request.externalId());
													 newMovie.setTitle(request.title());
													 newMovie.setPosterPath(request.posterPath());
													 newMovie.setReleaseYear(request.releaseYear());
													 return rankedMovieRepository.save(newMovie);
												 });
		
		Vote vote = new Vote();
		vote.setRankedMovie(movie);
		voteRepository.save(vote);
	}
	
	@Transactional
	public void revertLastVote(Long movieId) {
		Vote latestVote = voteRepository.findFirstByRankedMovie_MovieIdOrderByVotedAtDescIdDesc(movieId)
										 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum voto encontrado para o filme ID: " + movieId));
	
		voteRepository.delete(latestVote);
	}
	
	public List<RankingItemDTO> getTop10() {
		return voteRepository.findTopRankedMovies(PageRequest.of(0, 10));
	}
}
