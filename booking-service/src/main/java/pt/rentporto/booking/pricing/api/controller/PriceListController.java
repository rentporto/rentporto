package pt.rentporto.booking.pricing.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.booking.pricing.api.contract.PriceListApi;
import pt.rentporto.booking.pricing.api.model.Coverage;
import pt.rentporto.booking.pricing.api.model.Extra;
import pt.rentporto.booking.pricing.api.model.Fee;
import pt.rentporto.booking.pricing.api.model.RateCard;
import pt.rentporto.booking.pricing.api.model.Season;
import pt.rentporto.web.error.EndpointNotImplementedException;

@RestController
class PriceListController implements PriceListApi {

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public RateCard rateCard(String categoryCode) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public RateCard saveRateCard(String categoryCode, RateCard rateCard) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Season> seasons() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Season> replaceSeasons(List<Season> seasons) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Fee> fees() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Fee> replaceFees(List<Fee> fees) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("permitAll()")
    public List<Extra> extras() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Extra> replaceExtras(List<Extra> extras) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("permitAll()")
    public List<Coverage> coverages() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('PRICING_MANAGER')")
    public List<Coverage> replaceCoverages(List<Coverage> coverages) {
        throw new EndpointNotImplementedException();
    }
}
