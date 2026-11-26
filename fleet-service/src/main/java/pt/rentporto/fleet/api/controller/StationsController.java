package pt.rentporto.fleet.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.api.contract.StationsApi;
import pt.rentporto.fleet.api.response.StationResponse;
import pt.rentporto.fleet.application.service.FleetCatalogue;

@RestController
class StationsController implements StationsApi {

    private final FleetCatalogue catalogue;

    StationsController(FleetCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    @PreAuthorize("permitAll()")
    public List<StationResponse> stations() {
        return catalogue.stations().stream().map(StationResponse::from).toList();
    }
}
