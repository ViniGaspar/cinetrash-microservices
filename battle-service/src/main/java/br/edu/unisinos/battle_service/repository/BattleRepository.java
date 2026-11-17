package br.edu.unisinos.battle_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.unisinos.battle_service.model.Battle;

public interface BattleRepository extends JpaRepository<Battle, Long> {

}
