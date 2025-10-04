package com.example.demo.controller;

import com.example.demo.entity.Employee;
import com.example.demo.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public Iterable<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        System.out.println("\nStarting request for employee ID: " + id);

        Employee employee = employeeService.getEmployeeById(id);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Request completed in: " + duration + "ms");

        if (duration < 500) {
            System.out.println("FAST! Data served from CACHE");
        } else {
            System.out.println("SLOW! Data fetched from DATABASE");
        }

        return employee;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.createEmployee(employee);
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        return employeeService.updateEmployee(employee);
    }

    @DeleteMapping("/{id}")
    public String deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return "Employee deleted successfully";
    }

    @DeleteMapping("/cache/clear")
    public String clearCache() {
        employeeService.clearCache();
        return "Cache cleared successfully";
    }
}
