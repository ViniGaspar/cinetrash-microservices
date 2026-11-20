package br.edu.unisinos.ranking_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unisinos.ranking_service.dto.RankingItemDTO;
import br.edu.unisinos.ranking_service.dto.RankingVoteRequestDTO;
import br.edu.unisinos.ranking_service.service.RankingService;

@RestController
@RequestMapping("/ranking")
public class RankingController {
	@Autowired
	private RankingService service;
	
	@PostMapping("/votes")
	public void registerVote(@RequestBody RankingVoteRequestDTO request) {
		service.registerVote(request);
	}
	
	@GetMapping("/top10")
	public List<RankingItemDTO> getTop10() {
		return service.getTop10();
	}
	
	@DeleteMapping("/votes/latest/{movieId}")
	public void revertLastVote(@PathVariable Long movieId) {
		service.revertLastVote(movieId);
	}
}
