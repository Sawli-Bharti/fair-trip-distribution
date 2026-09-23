package com.example.fairtripdistribution.model.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "zones")
public class Zone implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal minDistance;
    
    @Column(nullable = true)
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
