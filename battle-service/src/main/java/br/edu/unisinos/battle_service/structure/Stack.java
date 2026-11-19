package br.edu.unisinos.battle_service.structure;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class Stack<T> {
	ArrayList<T> items;
	
	public Stack() {
		this.items = new ArrayList<T>();
	}
	
	@Override
	public String toString() {
		if (!this.isEmpty()) {
			String stringStack = this.items.toString();
			return ("Bottom -> " + stringStack + " -> Top");
		}
		else {
			return ("The Stack is Empty");
		}
 	}
	
	public boolean isEmpty() {
		return this.items.isEmpty();
	}
	
	public int size() {
		return this.items.size();
	}
	
	public void push (T item) {
		this.items.add(item);
	}
	
	public T pop() {
		if (this.isEmpty()) {
			throw new NoSuchElementException("The stack is empty");
		}
		else {
			return this.items.remove(items.size() - 1); 
		}
	}
	
	public T peek() {
		if (this.isEmpty()) {
			throw new NoSuchElementException("The stack is empty");
		}
		else {
			return this.items.get(items.size() - 1);
		}
	}
}
