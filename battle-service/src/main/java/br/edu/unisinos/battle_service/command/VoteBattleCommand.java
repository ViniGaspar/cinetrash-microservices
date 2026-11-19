package br.edu.unisinos.battle_service.command;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.battle_service.client.CatalogClient;
import br.edu.unisinos.battle_service.client.RankingClient;
import br.edu.unisinos.battle_service.dto.MovieDTO;
import br.edu.unisinos.battle_service.dto.RankingVoteDTO;
import br.edu.unisinos.battle_service.model.Battle;
import br.edu.unisinos.battle_service.repository.BattleRepository;

public class VoteBattleCommand implements BattleCommand {
	private final Long battleId;
    private final Long chosenMovieId;
    private final BattleRepository battleRepository;
    private final CatalogClient catalogClient;
    private final RankingClient rankingClient;
	
	public VoteBattleCommand(Long battleId,  
						     Long chosenMovieId,  
						     BattleRepository battleRepository,  
						     CatalogClient catalogClient,  
						     RankingClient rankingClient) {
		this.battleId = battleId;
		this.chosenMovieId = chosenMovieId;
		this.battleRepository = battleRepository;
		this.catalogClient = catalogClient;
		this.rankingClient = rankingClient;
	}
	
	@Override
	public void execute() {
		Battle battle = battleRepository.findById(battleId)
										.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		
		if (battle.getVotedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta batalha já foi votada.");
		}
		
		if (chosenMovieId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O filme escolhido não faz parte desta batalha.");
		}
		
		battle.setChosenMovieId(chosenMovieId);
		battle.setVotedAt(LocalDateTime.now());
		battleRepository.save(battle);
		
		MovieDTO chosenMovie = catalogClient.getMovieById(chosenMovieId);
		
		RankingVoteDTO rankingVote = new RankingVoteDTO(
				chosenMovie.movieId(),
				chosenMovie.externalId(),
				chosenMovie.title(),
				chosenMovie.posterPath(),
				chosenMovie.releaseYear()
		);
		
		rankingClient.registerVote(rankingVote);
	}
	
	@Override
	public void undo() {
		Battle battle = battleRepository.findById(battleId)
										.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		battle.setChosenMovieId(null);
		battle.setVotedAt(null);
		battleRepository.save(battle);
		
		rankingClient.revertVote(chosenMovieId);
	}
	
	@Override
	public String summary() {
		return ("Voto na batalha " + battleId + " (filme " + chosenMovieId + ").");
	}
}
