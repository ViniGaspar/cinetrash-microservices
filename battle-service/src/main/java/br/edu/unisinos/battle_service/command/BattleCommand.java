package br.edu.unisinos.battle_service.command;

public interface BattleCommand {
	void execute();
	void undo();
	String summary();
}
