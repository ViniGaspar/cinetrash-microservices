package br.edu.unisinos.battle_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.battle_service.client.CatalogClient;
import br.edu.unisinos.battle_service.client.RankingClient;
import br.edu.unisinos.battle_service.dto.BattleResponseDTO;
import br.edu.unisinos.battle_service.dto.MovieDTO;
import br.edu.unisinos.battle_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.battle_service.dto.RankingVoteDTO;
import br.edu.unisinos.battle_service.dto.VoteRequestDTO;
import br.edu.unisinos.battle_service.model.Battle;
import br.edu.unisinos.battle_service.repository.BattleRepository;

@Service
public class BattleService {
	@Autowired
	private BattleRepository repository;
	
	@Autowired
	private CatalogClient catalogClient;
	
	@Autowired
	private RankingClient rankingClient;
	
	public BattleResponseDTO createBattle() {
		RandomPairResponseDTO pair = catalogClient.getRandomPair();
		
		Battle battle = new Battle();
		battle.setMovieAId(pair.movieA().movieId());
		battle.setMovieBId(pair.movieB().movieId());
		battle = repository.save(battle);
		
		return new BattleResponseDTO(battle.getId(), pair.movieA(), pair.movieB());
	}
	
	public void vote(Long battleId, VoteRequestDTO request) {
		Battle battle = repository.findById(battleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batalha não encontrada."));
		
		if (battle.getVotedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta batalha já existe.");
		}
		
		Long chosenId = request.chosenMovieId();
		
		if (!chosenId.equals(battle.getMovieAId()) && !chosenId.equals(battle.getMovieBId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O filme escolhido não faz parte desta batalha.");
        }	
		
		battle.setChosenMovieId(chosenId);
		battle.setVotedAt(LocalDateTime.now());
		repository.save(battle);
		
		MovieDTO chosenMovie = catalogClient.getMovieById(chosenId);
		
		RankingVoteDTO rankingVote = new RankingVoteDTO(
			chosenMovie.movieId(),
			chosenMovie.externalId(),
			chosenMovie.title(),
			chosenMovie.posterpath(),
			chosenMovie.releaseYear()
		);
		
		rankingClient.registerVote(rankingVote);
	}
	
	public List<Battle> getAll() {
		return repository.findAll();
	}
	
}
