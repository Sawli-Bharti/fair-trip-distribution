package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

}
