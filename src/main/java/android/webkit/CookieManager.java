package android.webkit;

public class CookieManager {
    private static final CookieManager instance = new CookieManager();

    public static CookieManager getInstance() {
        return instance;
    }

    public String getCookie(String url) {
        return null;
    }

    public void setCookie(String url, String value) {
    }
}
