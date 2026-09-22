package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByCode(String code);
    boolean existsByCode(String code);
    @Cacheable(value = "activeZones")
    List<Zone> findByIsActiveTrue();
}
