package pi.savings.ui;

import pi.savings.service.SavingsStatsService;

final class SavingsStatsController {

    private static final int DEFAULT_USER_ID = 1;
    private final SavingsStatsService statsService;

    SavingsStatsController() {
        this(new SavingsStatsService());
    }

    SavingsStatsController(SavingsStatsService statsService) {
        this.statsService = statsService;
    }

    SavingsStatsService.FrontStatsSnapshot loadFrontStats(String currency) {
        return statsService.loadFrontStats(DEFAULT_USER_ID, currency);
    }

    SavingsStatsService.BackOfficeStatsSnapshot loadBackOfficeStats(String currency) {
        return statsService.loadBackOfficeStats(currency);
    }

    SavingsStatsService.ExportBundle loadFrontExportBundle(String currency) {
        return statsService.loadFrontExportBundle(DEFAULT_USER_ID, currency);
    }

    SavingsStatsService.ExportBundle loadBackOfficeExportBundle(String currency) {
        return statsService.loadBackOfficeExportBundle(currency);
    }
}
