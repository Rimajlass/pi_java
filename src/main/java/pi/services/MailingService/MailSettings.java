package pi.services.MailingService;

public class MailSettings {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final boolean startTls;
    private final boolean ssl;
    private final boolean debug;
    private final boolean debugPopup;
    private final String username;
    private final String password;
    private final String from;
    private final String fromName;

    public MailSettings(boolean enabled, String host, int port, boolean startTls, boolean ssl, boolean debug, boolean debugPopup, String username, String password, String from, String fromName) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.startTls = startTls;
        this.ssl = ssl;
        this.debug = debug;
        this.debugPopup = debugPopup;
        this.username = username;
        this.password = password;
        this.from = from;
        this.fromName = fromName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public boolean isSsl() {
        return ssl;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isDebugPopup() {
        return debugPopup;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFrom() {
        return from;
    }

    public String getFromName() {
        return fromName;
    }
}
