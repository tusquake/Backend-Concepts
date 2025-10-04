package com.example.demo;

import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class CachingDemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(CachingDemoApplication.class, args);
	}

	// Initialize some sample data
	@Bean
	CommandLineRunner initDatabase(EmployeeRepository repository) {
		return args -> {
			repository.save(new Employee(null, "John Doe", "IT", 75000.0));
			repository.save(new Employee(null, "Jane Smith", "HR", 65000.0));
			repository.save(new Employee(null, "Bob Johnson", "Finance", 70000.0));
			repository.save(new Employee(null, "Alice Williams", "IT", 80000.0));
			repository.save(new Employee(null, "Charlie Brown", "Marketing", 68000.0));
			System.out.println("Sample data loaded into H2 Database!");
		};
	}
}