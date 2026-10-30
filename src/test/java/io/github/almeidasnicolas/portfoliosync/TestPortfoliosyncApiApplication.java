package io.github.almeidasnicolas.portfoliosync;

import org.springframework.boot.SpringApplication;

public class TestPortfoliosyncApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(PortfoliosyncApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
