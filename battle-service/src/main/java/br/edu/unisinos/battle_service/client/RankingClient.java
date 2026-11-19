package br.edu.unisinos.battle_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import br.edu.unisinos.battle_service.dto.RankingVoteDTO;

// @FeignClient(name = "RANKING-SERVICE")
public interface RankingClient {
	@PostMapping("/ranking/votes")
	void registerVote(@RequestBody RankingVoteDTO vote);
	
	@DeleteMapping("/ranking/votes/lastest/{movieId}")
	void revertVote(@PathVariable Long movieId);
}
