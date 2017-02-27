/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aulacampus.mapadistritos;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public abstract class Base extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mapaGoogle;

    protected int getLayoutId() {
        return R.layout.mapa;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        iniciarMapa();
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciarMapa();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (mapaGoogle != null) {
            return;
        }
        mapaGoogle = map;
        iniciarApp();
    }

    private void iniciarMapa() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    /**
     * Inicia la aplicación
     */
    protected abstract void iniciarApp();

    protected GoogleMap getMap() {
        return mapaGoogle;
    }
}
