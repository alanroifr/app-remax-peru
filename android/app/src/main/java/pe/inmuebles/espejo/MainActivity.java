package pe.inmuebles.espejo;

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El mapa de remax.pe pide los mosaicos de OpenStreetMap por http://.
        // Permitimos solo contenido mixto pasivo (imágenes); los scripts http siguen bloqueados.
        getBridge().getWebView().getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        getBridge().setWebViewClient(new MapaWebViewClient(getBridge()));
    }

    // Pide los mosaicos de OpenStreetMap por https y con las cabeceras que exige su política
    // de uso; sin ellas el servidor puede rechazarlos y el mapa queda en gris.
    private static class MapaWebViewClient extends BridgeWebViewClient {
        MapaWebViewClient(com.getcapacitor.Bridge bridge) {
            super(bridge);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String host = request.getUrl().getHost();
            if (host != null && host.endsWith("tile.openstreetmap.org")) {
                try {
                    URL url = new URL("https://tile.openstreetmap.org" + request.getUrl().getPath());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "REMAXPeru-Android/1.0 (ventas@remax.net.pe)");
                    conn.setRequestProperty("Referer", "https://www.remax.pe/");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        return new WebResourceResponse("image/png", null, conn.getInputStream());
                    }
                } catch (Exception ignored) {
                    // Si falla, dejamos que el WebView lo intente por su cuenta.
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }
}
