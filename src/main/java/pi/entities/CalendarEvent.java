package pi.entities;

import java.time.LocalDate;

public class CalendarEvent {
    private final LocalDate date;
    private final String title;
    private final String type;
    private final String description;
    private final String colorTag;

    public CalendarEvent(LocalDate date, String title, String type, String description, String colorTag) {
        this.date = date;
        this.title = title;
        this.type = type;
        this.description = description;
        this.colorTag = colorTag;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getColorTag() {
        return colorTag;
    }
}
