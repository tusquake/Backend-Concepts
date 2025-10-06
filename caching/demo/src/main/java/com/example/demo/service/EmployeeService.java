package com.example.demo.service;

import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * @Cacheable - Caches the result of this method
     * If called with same ID, returns from cache without executing method
     */
    @Cacheable(value = "employees", key = "#id")
    public Employee getEmployeeById(Long id) {
        System.out.println("🔍 Fetching employee from H2 Database for ID: " + id);
        simulateSlowService(); // Simulate slow database query
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    /**
     * Get all employees - Not cached to show difference
     */
    public Iterable<Employee> getAllEmployees() {
        System.out.println("Fetching all employees from H2 Database");
        return employeeRepository.findAll();
    }

    /**
     * @CachePut - Updates the cache after executing the method
     * Method always executes and result is put in cache
     */
    @CachePut(value = "employees", key = "#employee.id")
    public Employee updateEmployee(Employee employee) {
        System.out.println("✏️ Updating employee in H2 Database: " + employee.getId());

        // Check if employee exists
        Employee existingEmployee = employeeRepository.findById(employee.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employee.getId()));

        // Update fields
        existingEmployee.setName(employee.getName());
        existingEmployee.setDepartment(employee.getDepartment());
        existingEmployee.setSalary(employee.getSalary());

        return employeeRepository.save(existingEmployee);
    }

    /**
     * Create new employee
     */
    public Employee createEmployee(Employee employee) {
        System.out.println("Creating new employee in H2 Database");
        employee.setId(null); // Ensure it's a new record
        return employeeRepository.save(employee);
    }

    /**
     * @CacheEvict - Removes entry from cache
     * When employee is deleted, remove from cache
     */
    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        System.out.println("Deleting employee from H2 Database: " + id);

        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }

        employeeRepository.deleteById(id);
    }

    /**
     * @CacheEvict with allEntries - Clears entire cache
     */
    @CacheEvict(value = "employees", allEntries = true)
    public void clearCache() {
        System.out.println("Clearing entire employee cache");
    }

    // Simulate slow database call
    private void simulateSlowService() {
        try {
            Thread.sleep(2000); // 2 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
