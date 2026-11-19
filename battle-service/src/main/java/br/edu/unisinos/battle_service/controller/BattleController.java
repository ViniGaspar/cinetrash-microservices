package br.edu.unisinos.battle_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unisinos.battle_service.dto.BattleInfoDTO;
import br.edu.unisinos.battle_service.dto.BattleResponseDTO;
import br.edu.unisinos.battle_service.dto.UndoResponseDTO;
import br.edu.unisinos.battle_service.dto.VoteRequestDTO;
import br.edu.unisinos.battle_service.model.Battle;
import br.edu.unisinos.battle_service.service.BattleService;

@RestController
@RequestMapping("/battles")
public class BattleController {
	@Autowired
	private BattleService service;
	
	@GetMapping
	public List<BattleInfoDTO> getAll() {
		return service.getAll();
	}
	
	@PostMapping
	public BattleResponseDTO startBattle() {
		return service.createBattle();
	}
	
	@PostMapping("/undo")
	public UndoResponseDTO undoLastVote() {
		return service.undoLastVote();
	}
	
	@PostMapping("/{battleId}/vote")
	public void vote(@PathVariable Long battleId, @RequestBody VoteRequestDTO request) {
		service.vote(battleId, request);
	}
}
