package pi.savings.ui;

final class SavingsGoalsStyles {

    private SavingsGoalsStyles() {
    }

    static String build() {
        return """
                .root {
                    -fx-font-family: "Segoe UI";
                    -fx-background-color: #f5fbff;
                }
                .page-scroll {
                    -fx-background-color: transparent;
                    -fx-padding: 0;
                }
                .page-scroll > .viewport {
                    -fx-background-color: transparent;
                }
                .page {
                    -fx-background-color:
                        radial-gradient(center 15% 10%, radius 30%, rgba(0,191,229,0.14), transparent),
                        radial-gradient(center 90% 0%, radius 35%, rgba(8,26,58,0.08), transparent),
                        #f5fbff;
                }
                .hero {
                    -fx-background-color: linear-gradient(to right, rgba(8,26,58,0.92), rgba(0,191,229,0.65));
                    -fx-background-radius: 0 0 28 28;
                    -fx-padding: 42 48 36 48;
                }
                .hero-title {
                    -fx-text-fill: white;
                    -fx-font-size: 42px;
                    -fx-font-family: "Georgia";
                    -fx-font-weight: bold;
                }
                .hero-subtitle, .hero-breadcrumb {
                    -fx-text-fill: rgba(255,255,255,0.92);
                    -fx-font-size: 17px;
                }
                .hero-actions {
                    -fx-padding: 14 0 0 0;
                }
                .primary-hero-btn {
                    -fx-background-color: #00bfe5;
                    -fx-text-fill: white;
                    -fx-background-radius: 999;
                    -fx-padding: 12 24 12 24;
                    -fx-font-size: 15px;
                    -fx-font-weight: bold;
                }
                .soft-hero-btn {
                    -fx-background-color: rgba(255,255,255,0.16);
                    -fx-border-color: rgba(255,255,255,0.35);
                    -fx-border-radius: 999;
                    -fx-background-radius: 999;
                    -fx-text-fill: white;
                    -fx-padding: 12 22 12 22;
                    -fx-font-size: 15px;
                    -fx-font-weight: bold;
                }
                .content {
                    -fx-padding: 28 34 36 34;
                    -fx-spacing: 18;
                }
                .section-kicker {
                    -fx-text-fill: #00bfe5;
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                }
                .section-title {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 34px;
                    -fx-font-family: "Georgia";
                    -fx-font-weight: bold;
                }
                .section-subtitle {
                    -fx-text-fill: #35556b;
                    -fx-font-size: 14px;
                }
                .kpi-grid {
                    -fx-hgap: 18;
                    -fx-vgap: 18;
                }
                .glass-card {
                    -fx-background-color: rgba(255,255,255,0.85);
                    -fx-background-radius: 22;
                    -fx-border-color: rgba(8,26,58,0.08);
                    -fx-border-radius: 22;
                    -fx-padding: 22;
                    -fx-effect: dropshadow(gaussian, rgba(8,26,58,0.10), 28, 0.12, 0, 6);
                }
                .card-title {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 18px;
                    -fx-font-family: "Georgia";
                    -fx-font-weight: bold;
                }
                .card-subtitle {
                    -fx-text-fill: #6f7f8c;
                    -fx-font-size: 12px;
                }
                .card-value {
                    -fx-text-fill: #00bfe5;
                    -fx-font-size: 24px;
                    -fx-font-weight: bold;
                }
                .icon-bubble {
                    -fx-background-color: #00bfe5;
                    -fx-background-radius: 999;
                    -fx-text-fill: white;
                    -fx-font-size: 22px;
                    -fx-font-weight: bold;
                    -fx-alignment: center;
                    -fx-min-width: 58;
                    -fx-min-height: 58;
                    -fx-max-width: 58;
                    -fx-max-height: 58;
                }
                .tab-bar {
                    -fx-alignment: center;
                    -fx-spacing: 14;
                    -fx-padding: 6 0 2 0;
                }
                .tab-pill {
                    -fx-background-radius: 999;
                    -fx-padding: 12 24 12 24;
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-background-color: rgba(255,255,255,0.85);
                    -fx-text-fill: #173a54;
                    -fx-border-color: rgba(8,26,58,0.08);
                    -fx-border-radius: 999;
                }
                .tab-pill-active {
                    -fx-background-color: linear-gradient(to right, #00bfe5, #4ed7f4);
                    -fx-text-fill: white;
                    -fx-border-color: transparent;
                }
                .left-column {
                    -fx-pref-width: 360;
                    -fx-spacing: 18;
                }
                .right-column {
                    -fx-spacing: 18;
                }
                .field-label {
                    -fx-text-fill: #2a3c50;
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                }
                .field {
                    -fx-background-color: rgba(255,255,255,0.96);
                    -fx-background-radius: 12;
                    -fx-border-color: rgba(8,26,58,0.10);
                    -fx-border-radius: 12;
                    -fx-padding: 10 12 10 12;
                }
                .mini-row {
                    -fx-background-color: rgba(255,255,255,0.96);
                    -fx-background-radius: 14;
                    -fx-border-color: rgba(8,26,58,0.06);
                    -fx-border-radius: 14;
                    -fx-padding: 12 16 12 16;
                }
                .mini-row-label {
                    -fx-text-fill: #51677a;
                    -fx-font-size: 13px;
                }
                .mini-row-value {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 13px;
                    -fx-font-weight: bold;
                }
                .primary-btn {
                    -fx-background-color: #00bfe5;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-background-radius: 999;
                    -fx-padding: 12 22 12 22;
                }
                .ghost-btn {
                    -fx-background-color: rgba(0,191,229,0.14);
                    -fx-text-fill: #0f5670;
                    -fx-font-weight: bold;
                    -fx-background-radius: 14;
                    -fx-border-color: rgba(0,191,229,0.36);
                    -fx-border-radius: 14;
                    -fx-padding: 10 16 10 16;
                }
                .chip {
                    -fx-background-color: rgba(255,255,255,0.86);
                    -fx-background-radius: 999;
                    -fx-border-color: rgba(8,26,58,0.08);
                    -fx-border-radius: 999;
                    -fx-padding: 10 16 10 16;
                    -fx-font-weight: bold;
                    -fx-text-fill: #173a54;
                }
                .chip-active {
                    -fx-background-color: rgba(0,191,229,0.18);
                    -fx-border-color: rgba(0,191,229,0.35);
                }
                .toolbar-row {
                    -fx-spacing: 10;
                    -fx-alignment: center-left;
                }
                .history-header {
                    -fx-background-color: rgba(245,251,252,0.98);
                    -fx-background-radius: 14 14 0 0;
                    -fx-padding: 12 14 12 14;
                    -fx-border-color: rgba(8,26,58,0.08);
                    -fx-border-radius: 14 14 0 0;
                    -fx-border-width: 1 1 0 1;
                }
                .history-table {
                    -fx-border-color: rgba(8,26,58,0.08);
                    -fx-border-radius: 0 0 14 14;
                    -fx-background-color: rgba(255,255,255,0.94);
                }
                .table-head {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 13px;
                    -fx-font-weight: bold;
                }
                .table-cell {
                    -fx-text-fill: #486175;
                    -fx-font-size: 13px;
                }
                .amount-cell {
                    -fx-text-fill: #00bfe5;
                    -fx-font-weight: bold;
                }
                .goal-card {
                    -fx-background-color: rgba(255,255,255,0.94);
                    -fx-background-radius: 18;
                    -fx-border-color: rgba(8,26,58,0.06);
                    -fx-border-radius: 18;
                    -fx-padding: 16;
                }
                .goal-title {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 16px;
                    -fx-font-weight: bold;
                }
                .goal-meta {
                    -fx-text-fill: #6b7c8a;
                    -fx-font-size: 12px;
                }
                .priority-badge {
                    -fx-background-color: rgba(0,191,229,0.15);
                    -fx-background-radius: 999;
                    -fx-padding: 8 12 8 12;
                    -fx-text-fill: #0b4d62;
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                }
                .progress-track {
                    -fx-accent: #00bfe5;
                }
                .progress-track > .bar {
                    -fx-background-color: #00bfe5;
                    -fx-background-radius: 999;
                }
                .progress-track > .track {
                    -fx-background-color: #dde8ee;
                    -fx-background-radius: 999;
                }
                .progress-value {
                    -fx-text-fill: #0b1c3f;
                    -fx-font-size: 13px;
                    -fx-font-weight: bold;
                }
                """;
    }
}
