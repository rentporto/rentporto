package pt.rentporto.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.VehicleCategory;

interface SpringDataVehicleCategories extends JpaRepository<VehicleCategory, AcrissCode> {
}
