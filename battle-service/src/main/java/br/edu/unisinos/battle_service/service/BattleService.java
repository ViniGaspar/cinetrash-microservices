package br.edu.unisinos.battle_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.edu.unisinos.battle_service.client.CatalogClient;
import br.edu.unisinos.battle_service.client.RankingClient;
import br.edu.unisinos.battle_service.command.BattleCommand;
import br.edu.unisinos.battle_service.command.BattleCommandHistory;
import br.edu.unisinos.battle_service.command.VoteBattleCommand;
import br.edu.unisinos.battle_service.dto.BattleInfoDTO;
import br.edu.unisinos.battle_service.dto.BattleResponseDTO;
import br.edu.unisinos.battle_service.dto.RandomPairResponseDTO;
import br.edu.unisinos.battle_service.dto.UndoResponseDTO;
import br.edu.unisinos.battle_service.dto.VoteRequestDTO;
import br.edu.unisinos.battle_service.model.Battle;
import br.edu.unisinos.battle_service.repository.BattleRepository;
import jakarta.transaction.Transactional;

@Service
public class BattleService {
	@Autowired
	private BattleRepository battleRepository;
	
	@Autowired
	private CatalogClient catalogClient;
	
	@Autowired
	private RankingClient rankingClient;
	
	@Autowired
	private BattleCommandHistory commandHistory;
	
	public BattleResponseDTO createBattle() {
		RandomPairResponseDTO pair = catalogClient.getRandomPair();
		
		Battle battle = new Battle();
		battle.setMovieAId(pair.movieA().movieId());
		battle.setMovieBId(pair.movieB().movieId());
		battle = battleRepository.save(battle);
		
		return new BattleResponseDTO(battle.getId(), pair.movieA(), pair.movieB());
	}
	
	
	@Transactional
	public void vote(Long battleId, VoteRequestDTO request) {
		BattleCommand voteCommand = new VoteBattleCommand(battleId, 
														  request.chosenMovieId(), 
														  battleRepository,  
														  catalogClient,  
														  rankingClient);
		
		commandHistory.execute(voteCommand);
	}
	
	@Transactional
	public UndoResponseDTO undoLastVote() {
		String undone = commandHistory.undoLast();
		return new UndoResponseDTO(undone, commandHistory.size());
	}
	
	public List<BattleInfoDTO> getAll() {
		return battleRepository.findAllByOrderByIdAsc()
							   .stream()
							   .map(b -> new BattleInfoDTO(
									   b.getId(),
				                       b.getMovieAId(),
				                       b.getMovieBId(),
				                       b.getChosenMovieId(),
				                       b.getCreatedAt(),
				                       b.getVotedAt()
							   ))
							   .toList();
	}
}
