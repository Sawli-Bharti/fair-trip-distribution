import os

base_dir = 'backend/src/main/java/com/example/fairtripdistribution'

def write_file(subpath, content):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. Update Zone Entity
write_file('model/entity/Zone.java', '''package com.example.fairtripdistribution.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "zones")
public class Zone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal minDistance;
    
    @Column(nullable = false)
    private BigDecimal maxDistance;
    
    @Column(nullable = false)
    private boolean isActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMinDistance() { return minDistance; }
    public void setMinDistance(BigDecimal minDistance) { this.minDistance = minDistance; }
    public BigDecimal getMaxDistance() { return maxDistance; }
    public void setMaxDistance(BigDecimal maxDistance) { this.maxDistance = maxDistance; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
''')

# 2. Update VendorCapacity Entity
write_file('model/entity/VendorCapacity.java', '''package com.example.fairtripdistribution.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_capacity")
public class VendorCapacity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", unique = true, nullable = false)
    private Vendor vendor;
    
    @Column(nullable = false)
    private int totalCapacity;
    
    @Column(nullable = false)
    private int availableCapacity;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
''')

# Create Exceptions
write_file('exception/BusinessValidationException.java', '''package com.example.fairtripdistribution.exception;
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) { super(message); }
}
''')

write_file('exception/ResourceNotFoundException.java', '''package com.example.fairtripdistribution.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
''')

write_file('exception/GlobalExceptionHandler.java', '''package com.example.fairtripdistribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessValidationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
''')

# We'll just print out success
print('Models and exceptions created')
