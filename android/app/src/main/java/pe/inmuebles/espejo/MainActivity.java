package pe.inmuebles.espejo;

import android.os.Bundle;
import android.webkit.WebSettings;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El mapa de remax.pe pide los mosaicos de OpenStreetMap por http://.
        // Permitimos solo contenido mixto pasivo (imágenes); los scripts http siguen bloqueados.
        getBridge().getWebView().getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }
}
