import os

base_dir = 'backend/src/main/java/com/example/fairtripdistribution'

def write_file(subpath, content):
    path = os.path.join(base_dir, subpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# DTOs
write_file('model/dto/VendorDto.java', '''package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotBlank;
public class VendorDto {
    @NotBlank public String code;
    @NotBlank public String name;
    public boolean isActive = true;
}
''')

write_file('model/dto/ZoneDto.java', '''package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public class ZoneDto {
    @NotBlank public String code;
    @NotBlank public String name;
    @NotNull public BigDecimal minDistance;
    @NotNull public BigDecimal maxDistance;
    public boolean isActive = true;
}
''')

write_file('model/dto/VendorShareDto.java', '''package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.NotNull;
public class VendorShareDto {
    @NotNull public Long vendorId;
    public int targetBasisPoints;
}
''')

write_file('model/dto/ZoneShareConfigDto.java', '''package com.example.fairtripdistribution.model.dto;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public class ZoneShareConfigDto {
    @NotNull public TripType tripType;
    @NotNull public List<VendorShareDto> vendorShares;
}
''')

write_file('model/dto/CapacityDto.java', '''package com.example.fairtripdistribution.model.dto;
import jakarta.validation.constraints.Min;
public class CapacityDto {
    @Min(0) public int totalCapacity;
    @Min(0) public int availableCapacity;
}
''')

# Repo extensions
write_file('repository/VendorRepository.java', '''package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByCode(String code);
    boolean existsByCode(String code);
}
''')

write_file('repository/ZoneRepository.java', '''package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByCode(String code);
    boolean existsByCode(String code);
    List<Zone> findByIsActiveTrue();
}
''')

write_file('repository/VendorZoneShareRepository.java', '''package com.example.fairtripdistribution.repository;
import com.example.fairtripdistribution.model.entity.VendorZoneShare;
import com.example.fairtripdistribution.model.entity.enums.TripType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VendorZoneShareRepository extends JpaRepository<VendorZoneShare, Long> {
    List<VendorZoneShare> findByZoneIdAndTripType(Long zoneId, TripType tripType);
    void deleteByZoneIdAndTripType(Long zoneId, TripType tripType);
}
''')

print('DTOs and Repos created')
