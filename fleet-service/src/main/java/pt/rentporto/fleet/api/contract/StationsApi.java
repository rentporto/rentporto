package pt.rentporto.fleet.api.contract;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import pt.rentporto.fleet.api.response.StationResponse;

/**
 * Stations where vehicles are picked up and returned.
 */
@Tag(name = "Stations")
public interface StationsApi {

    /**
     * Lists the stations. Public.
     *
     * @return the stations
     */
    @GetMapping("/stations")
    @SecurityRequirements
    List<StationResponse> stations();
}
