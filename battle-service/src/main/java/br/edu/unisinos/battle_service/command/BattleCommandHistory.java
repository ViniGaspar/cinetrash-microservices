package br.edu.unisinos.battle_service.command;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unisinos.battle_service.structure.Stack;

@Component
public class BattleCommandHistory {
	private final Stack<BattleCommand> stack = new Stack<>();
	
	public synchronized void execute(BattleCommand command) {
		command.execute();
		stack.push(command);
	}
	
	public synchronized String undoLast() {
		if (stack.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há ações para desfazer.");
		}
		
		BattleCommand lastCommand = stack.peek();
		lastCommand.undo();
		stack.pop();
		
		return lastCommand.summary();
	}
	
	public synchronized int size() {
		return stack.size();
	}
	
}
