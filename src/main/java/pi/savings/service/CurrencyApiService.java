package pi.savings.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public class CurrencyApiService {

    private final CurrencyRateService currencyRateService;

    public CurrencyApiService() {
        this(new CurrencyRateService());
    }

    CurrencyApiService(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    public Map<String, Double> loadMonthRates(YearMonth month, String baseCurrency, String targetCurrency) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return currencyRateService.getHistoricalRatesBetweenCurrencies(baseCurrency, targetCurrency, start, end);
    }

    public void clearCache() {
        currencyRateService.clearCache();
    }
}
