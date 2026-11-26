package pt.rentporto.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.VehicleCategory;

interface SpringDataVehicleCategories extends JpaRepository<VehicleCategory, AcrissCode> {
}
