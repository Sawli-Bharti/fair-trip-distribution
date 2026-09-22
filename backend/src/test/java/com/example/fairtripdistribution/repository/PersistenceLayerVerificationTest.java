package com.example.fairtripdistribution.repository;

import com.example.fairtripdistribution.model.entity.Zone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PersistenceLayerVerificationTest {

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    public void contextLoadsAndSchemaIsGenerated() {
        // Just injecting the repository ensures the JPA context loads
        // and the schema generation (ddl-auto) does not throw exceptions.
        assertThat(zoneRepository).isNotNull();
    }

    @Test
    public void canSaveAndRetrieveEntity() {
        Zone zone = new Zone();
        zone.setCode("NORTH_1");
        zone.setName("North Zone 1");
        
        Zone saved = zoneRepository.save(zone);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(zoneRepository.findById(saved.getId())).isPresent();
    }
}

