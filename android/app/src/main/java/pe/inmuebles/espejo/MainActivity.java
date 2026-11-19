package pe.inmuebles.espejo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;
import com.getcapacitor.BridgeWebViewClient;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public class MainActivity extends BridgeActivity {
    private static final int AZUL_REMAX = Color.rgb(0, 61, 165);

    private ProgressBar barraCarga;
    private View avisoSinConexion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = getBridge().getWebView();
        // El mapa de remax.pe pide los mosaicos de OpenStreetMap por http://.
        // Permitimos solo contenido mixto pasivo (imágenes); los scripts http siguen bloqueados.
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        crearBarraCarga();
        crearAvisoSinConexion();

        getBridge().setWebViewClient(new MapaWebViewClient(getBridge()));
        webView.setWebChromeClient(new ProgresoChromeClient(getBridge()));

        // Botón Atrás: vuelve a la página anterior; si no hay, deja la app en segundo plano
        // como cualquier app de Android (antes no hacía nada en la pantalla de inicio).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                WebView wv = getBridge().getWebView();
                if (wv.canGoBack()) {
                    wv.goBack();
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private int dp(int valor) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor, getResources().getDisplayMetrics());
    }

    private int margenSuperior() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getBridge().getWebView());
        if (insets == null) return 0;
        return insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()).top;
    }

    // Barra fina azul arriba que muestra que la página está cargando.
    private void crearBarraCarga() {
        barraCarga = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        barraCarga.setMax(100);
        barraCarga.setProgressTintList(ColorStateList.valueOf(AZUL_REMAX));
        barraCarga.setProgressBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        barraCarga.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4), Gravity.TOP);
        addContentView(barraCarga, lp);
    }

    // Aviso propio cuando no hay internet, en vez de la página de error del navegador.
    private void crearAvisoSinConexion() {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setGravity(Gravity.CENTER);
        caja.setBackgroundColor(Color.rgb(244, 246, 251));
        caja.setPadding(dp(32), dp(32), dp(32), dp(32));
        caja.setClickable(true);

        TextView titulo = new TextView(this);
        titulo.setText("Sin conexión a internet");
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titulo.setTextColor(AZUL_REMAX);
        titulo.setGravity(Gravity.CENTER);

        TextView detalle = new TextView(this);
        detalle.setText("Revisa tu wifi o tus datos móviles e inténtalo de nuevo.");
        detalle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        detalle.setTextColor(Color.rgb(90, 100, 117));
        detalle.setGravity(Gravity.CENTER);
        detalle.setPadding(0, dp(12), 0, dp(24));

        Button reintentar = new Button(this);
        reintentar.setText("Reintentar");
        reintentar.setTextColor(Color.WHITE);
        reintentar.setBackgroundTintList(ColorStateList.valueOf(AZUL_REMAX));
        reintentar.setOnClickListener(v -> {
            avisoSinConexion.setVisibility(View.GONE);
            getBridge().getWebView().reload();
        });

        caja.addView(titulo);
        caja.addView(detalle);
        caja.addView(reintentar);
        caja.setVisibility(View.GONE);
        avisoSinConexion = caja;
        addContentView(caja, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private class ProgresoChromeClient extends BridgeWebChromeClient {
        ProgresoChromeClient(Bridge bridge) {
            super(bridge);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress >= 100) {
                barraCarga.setVisibility(View.GONE);
            } else {
                barraCarga.setTranslationY(margenSuperior());
                barraCarga.setVisibility(View.VISIBLE);
                barraCarga.setProgress(newProgress);
            }
        }
    }

    private class MapaWebViewClient extends BridgeWebViewClient {
        MapaWebViewClient(Bridge bridge) {
            super(bridge);
        }

        // Pide los mosaicos de OpenStreetMap por https y con las cabeceras que exige su política
        // de uso; sin ellas el servidor puede rechazarlos y el mapa queda en gris.
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

        // Los enlaces "intent://" (que usan algunos botones de WhatsApp o mapas) no se abren
        // solos; los convertimos en la app que corresponde o en su página web de respaldo.
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("intent".equals(uri.getScheme())) {
                try {
                    Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setComponent(null);
                    intent.setSelector(null);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        String respaldo = intent.getStringExtra("browser_fallback_url");
                        if (respaldo != null && respaldo.startsWith("https://")) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(respaldo)));
                        }
                    }
                } catch (URISyntaxException | ActivityNotFoundException ignored) {
                    // Enlace roto: no hacemos nada.
                }
                return true;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        private boolean fallo = false;

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            fallo = false;
            avisoSinConexion.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                fallo = true;
                barraCarga.setVisibility(View.GONE);
                avisoSinConexion.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // La página de error del navegador también "termina"; mantenemos el aviso encima.
            avisoSinConexion.setVisibility(fallo ? View.VISIBLE : View.GONE);
        }
    }
}
