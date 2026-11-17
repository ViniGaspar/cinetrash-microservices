package br.edu.unisinos.battle_service.model;

import org.springframework.stereotype.Component;

import br.edu.unisinos.battle_service.client.RankingClient;
import br.edu.unisinos.battle_service.dto.RankingVoteDTO;

@Component
public class RankingMock implements RankingClient {
	@Override
	public void registerVote(RankingVoteDTO vote) {
		System.out.println("(Mock) Definitivamente enviei um voto");
	}
}
