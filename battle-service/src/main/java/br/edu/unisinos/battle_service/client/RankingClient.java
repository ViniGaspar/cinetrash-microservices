package br.edu.unisinos.battle_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import br.edu.unisinos.battle_service.dto.RankingVoteDTO;
import br.edu.unisinos.battle_service.model.RankingMock;

// @FeignClient(name = "RANKING-SERVICE")
public interface RankingClient {
	
	@PostMapping("/ranking/votes")
	void registerVote(@RequestBody RankingVoteDTO vote);
}
