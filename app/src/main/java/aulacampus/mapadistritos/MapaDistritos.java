package aulacampus.mapadistritos;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlPolygon;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;


public class MapaDistritos extends Base {

    private List<LatLng> lista;
    private GoogleMap mMap;
    private final static int LONGITUDE_INDEX = 0;
    private final static int LATITUDE_INDEX = 1;

    protected int getLayoutId() {
        return R.layout.mapa_distritos;
    }

    public void iniciarApp() {
        try {
            mMap = getMap();
            //Podemos elegir si cogemos los datos desde local o desde una URL
           //datosLocal();
           datosUrl();
        } catch (Exception e) {
            Log.e("Excepcion", e.toString());
        }
    }

    private void datosLocal() {
        try {
            KmlLayer capaKml = new KmlLayer(mMap, R.raw.distritos, getApplicationContext());
            capaKml.addLayerToMap();
            moverCamara(capaKml);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void datosUrl() {
        new DescargarFichero(getString(R.string.kml_url)).execute();
    }

    private void moverCamara(KmlLayer kmlLayer) {
        //Coge la primera capa del KML
        KmlContainer contenedor = kmlLayer.getContainers().iterator().next();

        //La capa anidada a ese KML
        contenedor = contenedor.getContainers().iterator().next();
        //El primer punto del KML
        KmlPlacemark punto = contenedor.getPlacemarks().iterator().next();

        //Objeto del poligono del punto
        KmlPolygon polygon = (KmlPolygon) punto.getGeometry();


        //Crea la latitud y longitud de ese poligono
        LatLngBounds.Builder builder = new LatLngBounds.Builder();


        //COMIENZO POLIGONO



        for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
            builder.include(latLng);
        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 1));
        System.out.println( PolyUtil.containsLocation(39.4793,-0.3792, polygon.getOuterBoundaryCoordinates(), false));
    }

    private class DescargarFichero extends AsyncTask<String, Void, byte[]> {
        private final String mUrl;

        public DescargarFichero(String url) {
            mUrl = url;
        }

        protected byte[] doInBackground(String... params) {
            try {
                InputStream is =  new URL(mUrl).openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(byte[] byteArr) {
            try {
                KmlLayer kmlLayer = new KmlLayer(mMap, new ByteArrayInputStream(byteArr),
                        getApplicationContext());
                kmlLayer.getContainers();

                kmlLayer.addLayerToMap();

                kmlLayer.setOnFeatureClickListener(new KmlLayer.OnFeatureClickListener() {
                    @Override
                    public void onFeatureClick(Feature feature) {
                        Toast.makeText(MapaDistritos.this,
                                "Barrio Clickado: " + feature.getProperty("name"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                moverCamara(kmlLayer);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
