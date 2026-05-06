package pi.entities;

public class CryptoNewsArticle {

    private String title;
    private String description;
    private String url;
    private String imageUrl;
    private String publishedAt;
    private String sourceName;

    public CryptoNewsArticle(String title, String description, String url,
                             String imageUrl, String publishedAt, String sourceName) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.sourceName = sourceName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getSourceName() {
        return sourceName;
    }
}