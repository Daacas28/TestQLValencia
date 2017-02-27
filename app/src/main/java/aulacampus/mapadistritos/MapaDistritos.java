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
import java.util.ArrayList;
import java.util.List;


public class MapaDistritos extends Base {

    private List<LatLng> lista;
    private GoogleMap mMap;
    private final static int LONGITUDE_INDEX = 0;
    private final static int LATITUDE_INDEX = 1;
    private KmlPolygon polygon;
    private KmlLayer kmlLayer;
    private  boolean contains = false;
    private Feature feature;
    private boolean liesInside;

    private List<LatLng> outerBoundary;

    private LatLng latLngTest =  new LatLng(39.4871133,-0.3764911); ; // The location to test. You will initialize it with your user's location

    private List<KmlPolygon> getPolygons(Iterable<KmlContainer> containers) {
        List<KmlPolygon> polygons = new ArrayList<>();

        if (containers == null) {
            return polygons;
        }

        for (KmlContainer container : containers) {
            polygons.addAll(getPolygons(container));
        }

        return polygons;
    }

    private List<KmlPolygon> getPolygons(KmlContainer container) {
        List<KmlPolygon> polygons = new ArrayList<>();

        if (container == null) {
            return polygons;
        }

        Iterable<KmlPlacemark> placemarks = container.getPlacemarks();
        if (placemarks != null) {
            for (KmlPlacemark placemark : placemarks) {
                if (placemark.getGeometry() instanceof KmlPolygon) {
                    polygons.add((KmlPolygon) placemark.getGeometry());
                }
            }
        }

        if (container.hasContainers()) {
            polygons.addAll(getPolygons(container.getContainers()));
        }

        return polygons;
    }

    private boolean liesOnPolygon(List<KmlPolygon> polygons, LatLng test) {
        boolean lies = false;

        if (polygons == null || test == null) {
            return lies;
        }

        for (KmlPolygon polygon : polygons) {
            if (liesOnPolygon(polygon, test)) {
                lies = true;
                break;
            }
        }

        return lies;
    }

    private boolean liesOnPolygon(KmlPolygon polygon, LatLng test) {
        boolean lies = false;

        if (polygon == null || test == null) {
            return lies;
        }

        // Get the outer boundary and check if the test location lies inside
        List<LatLng> outerBoundary = polygon.getOuterBoundaryCoordinates();
        lies = PolyUtil.containsLocation(test, outerBoundary, true);

        if (lies) {
            // Get the inner boundaries and check if the test location lies inside
            List<List<LatLng>> innerBoundaries = polygon.getInnerBoundaryCoordinates();
            if (innerBoundaries != null) {
                for (List<LatLng> innerBoundary : innerBoundaries) {
                    // If the test location lies in a hole, the polygon doesn't contain the location
                    if (PolyUtil.containsLocation(test, innerBoundary, true)) {
                        lies = false;
                        break;
                    }
                }
            }
        }

        return lies;
    }

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

        for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
            builder.include(latLng);

        }

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 1));

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
                 kmlLayer = new KmlLayer(mMap,  new ByteArrayInputStream(byteArr),
                        getApplicationContext());

                kmlLayer.addLayerToMap();

                List<KmlPolygon> polygonsInLayer = getPolygons(kmlLayer.getContainers());
                liesInside = liesOnPolygon(polygonsInLayer, latLngTest);
                KmlPolygon poligono;

                    System.out.println(liesInside);

                kmlLayer.setOnFeatureClickListener(new KmlLayer.OnFeatureClickListener() {
                    @Override
                    public void onFeatureClick(Feature feature) {
                        if (liesInside) {
                            Toast.makeText(MapaDistritos.this,
                                    "Barrio Clickado: " + feature.getProperty("name"),
                                    Toast.LENGTH_SHORT).show();
                        }
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
