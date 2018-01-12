package com.krlosmederos.managerabex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;


//public class MainActivity extends ActionBarActivity {
public class MainActivity extends AppCompatActivity {
    //CONSTANTES
    private static final int WAIT_TIMER = 5000;
    private static final int WAIT_CONEXION = 12000;
    //DEBUG
//    private static final String IP_UIJ = "10.10.3.203";
//    private static final String WEB_UIJ = "http://intranet.uij.edu.cu/";

    private Timer _timer;
    private TimerTask timerTask;
    private Handler handler;
    private static int _intentosConexion;
    private static String _PingCadlog;
    private static String _PortCadlog;
    private static String _UrlCadlog;
    private static String _User;

    private static final String TAG = MainActivity.class.getName();

    // Controles
    private TextView txtMensaje;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializaciones
        _intentosConexion = -1;
        _PingCadlog = _PortCadlog = _UrlCadlog = _User = "";
        handler = new Handler();
        webView = (WebView) findViewById(R.id.webView);
        txtMensaje = (TextView) findViewById(R.id.txtMensaje);

        txtMensaje.setText("INICIANDO APLICACION...");

        timerTick();
        startTimer();

        webView.getSettings().setJavaScriptEnabled(true); 	// Habilitar JavaScipts
        webView.setWebViewClient(new MyWebViewClient());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LogOff(_User);
        stopTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startTimer();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Verificar evento del boton atras y que hay historial de navegacion
        if((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // En caso contrario le pasamos el evento al padre
        return super.onKeyDown(keyCode, event);
    }

    private boolean GetCadlogParams() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ConfigCadlogManager.txt";

        File fileEvents = new File(path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileEvents));
            _UrlCadlog = "http://" + reader.readLine();
            _PingCadlog = reader.readLine();
            _PortCadlog = reader.readLine();
            reader.close();
            return true;
        }
        catch(IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private void GetUserIdFromUrl(String sUrl) {
        if(_User.equals("") && sUrl.indexOf("sUser=") > -1) {
            _User = sUrl.split("\\?")[1].split("&")[0].substring(6);
        }
        else if(sUrl.indexOf("Login") > -1) {
            _User = "";
        }
    }

    private void LogOff(String sUserId) {
        try {
            if(sUserId == "") {
                String cad_url = _UrlCadlog + "/Administracion/Menu?sUser=" + _User + "&LogOff=true&iOpcion=-1";

                URL url = new URL(cad_url);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(WAIT_TIMER);
                urlc.connect();
            }
        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void stopTimer() {
        if(_timer != null) {
            _timer.cancel();
            _timer.purge();
        }
    }

    private void startTimer() {
        _timer = new Timer();
        // Lanzar un hilo para el timer
        timerTask = new TimerTask() {
            public void run() {
                // Usar el manipulador para el Toast y para acceder al UI
                handler.post(new Runnable() {
                    public void run() {
                        timerTick();
                        //Toast.makeText(getApplicationContext(), "Probando Timer cada 5 seg", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        _timer.schedule(timerTask, 0, WAIT_TIMER);
    }

    private void timerTick() {
        try {
            //txtMensaje.setVisibility(View.VISIBLE);
            //txtMensaje.setText("CARGANDO CONFIGURACION...");
            if(GetCadlogParams()) {
                if( _intentosConexion != 0 ) {
                    txtMensaje.setText("VERIFICANDO CONEXION...");
                    txtMensaje.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.INVISIBLE);
                }
                if(isOnline(getApplicationContext())) {
                    new SiteAsyncTask().execute();
                    Log.i(TAG, "Conectado");
                }
                else {
                    Log.e(TAG, "No hay conexion");
                    _intentosConexion++;
                    txtMensaje.setText("DISPOSITIVO SIN CONEXION...");
                    webView.setVisibility(View.INVISIBLE);
                    txtMensaje.setVisibility(View.VISIBLE);
                }
            }
            else {
                Log.e(TAG, "Error en archivo de configuracion");
                _intentosConexion++;
                txtMensaje.setVisibility(View.VISIBLE);
                txtMensaje.setText("REVISAR ARCHIVO DE CONFIGURACION...");
                webView.setVisibility(View.INVISIBLE);
            }
        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage());
            _intentosConexion++;
            webView.setVisibility(View.INVISIBLE);
            txtMensaje.setVisibility(View.VISIBLE);
            txtMensaje.setText("ERROR EN CONFIGURACION...");
        }
    }

    private static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	/*
            if (Uri.parse(url).getHost().equals(_UrlCadlog)) {
                // Estoy navegando por mi sitio
                return false;
            }
            // Navegacion sitio externo
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            */
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            _intentosConexion = 0;
            webView.setVisibility(View.VISIBLE);
            txtMensaje.setVisibility(View.INVISIBLE);
            GetUserIdFromUrl(url.toString());
            //Toast.makeText(getApplicationContext(), "Sitio cargado completamente", Toast.LENGTH_SHORT).show();
        }
    }

    public class SiteAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private String siteIp;
        /*
         * Clase para lanzar un hilo secundario para testear el sitio
         * y de esa forma no bloquear el hilo principal (UI Thread)
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                URL url = new URL(siteIp);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(WAIT_CONEXION);
                urlc.connect();

                return (urlc.getResponseCode() == urlc.HTTP_OK);
            }
            catch(Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean okSite) {
            if(!okSite){
                _intentosConexion++;
                webView.setVisibility(View.INVISIBLE);
                txtMensaje.setText("RECONECTANDO SITIO ("+_intentosConexion+")...");
                txtMensaje.setVisibility(View.VISIBLE);
            }
            else {
                Log.i(TAG, "Respuesta de Sitio OK");
                _UrlCadlog = siteIp;
                if(_intentosConexion != 0) {  				// En caso que se haya perdido la conexion en algun
                    String direccion = webView.getUrl();	// momento vuelvo a cargar el sitio que estaba en el webView
                    if(direccion != null)
                        webView.loadUrl(direccion); 		// Continuo navegando por donde me quede
                    else
                        webView.loadUrl(_UrlCadlog); 		// Cargo el home del sitio
                    _intentosConexion = 0;
                    txtMensaje.setVisibility(View.VISIBLE);
                    txtMensaje.setText("CARGANDO...");
                }
            }
        }

        @Override
        protected void onPreExecute() {
            siteIp =  _UrlCadlog;
        }

        @Override
        protected void onProgressUpdate(Void... values) {}

    }


}



