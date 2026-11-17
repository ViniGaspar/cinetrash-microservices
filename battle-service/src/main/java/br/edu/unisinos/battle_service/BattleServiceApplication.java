package br.edu.unisinos.battle_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BattleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BattleServiceApplication.class, args);
	}

}
