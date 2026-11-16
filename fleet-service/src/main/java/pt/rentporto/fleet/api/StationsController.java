package pt.rentporto.fleet.api;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class StationsController implements StationsApi {

    @Override
    public List<StationResponse> stations() {
        throw new EndpointNotImplementedException();
    }
}
