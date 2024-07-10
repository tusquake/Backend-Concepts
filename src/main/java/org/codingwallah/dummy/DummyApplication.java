package org.codingwallah.dummy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DummyApplication {

	public static void main(String[] args) {
		System.out.println(MyController.myMethod());
		SpringApplication.run(DummyApplication.class, args);
	}

}
