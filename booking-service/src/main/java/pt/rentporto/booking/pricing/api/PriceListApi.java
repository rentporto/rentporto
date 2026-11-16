package pt.rentporto.booking.pricing.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Price list: rates, seasons, fees, extras and coverages. The lists are replaced as a whole.
 */
@Tag(name = "Price list")
public interface PriceListApi {

    /**
     * Reads the base daily rate of a category. Roles: PRICING_MANAGER.
     *
     * @param categoryCode ACRISS code of the category
     * @return the rate card
     */
    @GetMapping("/rate-cards/{categoryCode}")
    RateCard rateCard(@PathVariable String categoryCode);

    /**
     * Sets the base daily rate of a category; new quotes use it straight away. Roles: PRICING_MANAGER.
     *
     * @param categoryCode ACRISS code of the category
     * @param rateCard     new rate card
     * @return the stored rate card
     */
    @PutMapping("/rate-cards/{categoryCode}")
    RateCard saveRateCard(@PathVariable String categoryCode, @Valid @RequestBody RateCard rateCard);

    /**
     * Reads the seasons. Roles: PRICING_MANAGER.
     *
     * @return the seasons
     */
    @GetMapping("/seasons")
    List<Season> seasons();

    /**
     * Replaces the whole list of seasons. Roles: PRICING_MANAGER.
     *
     * @param seasons new list of seasons
     * @return the stored seasons
     */
    @PutMapping("/seasons")
    List<Season> replaceSeasons(@RequestBody List<@Valid Season> seasons);

    /**
     * Reads the fees. Roles: PRICING_MANAGER.
     *
     * @return the fees
     */
    @GetMapping("/fees")
    List<Fee> fees();

    /**
     * Replaces the whole list of fees. Roles: PRICING_MANAGER.
     *
     * @param fees new list of fees
     * @return the stored fees
     */
    @PutMapping("/fees")
    List<Fee> replaceFees(@RequestBody List<@Valid Fee> fees);

    /**
     * Reads the extras a customer can add to a quote. Public.
     *
     * @return the extras
     */
    @SecurityRequirements
    @GetMapping("/extras")
    List<Extra> extras();

    /**
     * Replaces the whole list of extras. Roles: PRICING_MANAGER.
     *
     * @param extras new list of extras
     * @return the stored extras
     */
    @PutMapping("/extras")
    List<Extra> replaceExtras(@RequestBody List<@Valid Extra> extras);

    /**
     * Reads the coverages a customer can choose. Public.
     *
     * @return the coverages
     */
    @SecurityRequirements
    @GetMapping("/coverages")
    List<Coverage> coverages();

    /**
     * Replaces the whole list of coverages. Roles: PRICING_MANAGER.
     *
     * @param coverages new list of coverages
     * @return the stored coverages
     */
    @PutMapping("/coverages")
    List<Coverage> replaceCoverages(@RequestBody List<@Valid Coverage> coverages);
}
