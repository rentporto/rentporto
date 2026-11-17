package pt.rentporto.fleet.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class StationsController implements StationsApi {

    @Override
    @PreAuthorize("permitAll()")
    public List<StationResponse> stations() {
        throw new EndpointNotImplementedException();
    }
}
