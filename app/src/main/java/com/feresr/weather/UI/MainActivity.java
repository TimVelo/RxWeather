package com.feresr.weather.UI;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.feresr.weather.BuildConfig;
import com.feresr.weather.R;
import com.feresr.weather.RxWeatherApplication;
import com.feresr.weather.injector.AppComponent;
import com.feresr.weather.injector.HasComponent;
import com.feresr.weather.models.City;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;

public class MainActivity extends AppCompatActivity implements HasComponent<AppComponent>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleApiClientProvider, FragmentInteractionsListener {

    private AppComponent weatherComponent;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment, new CitiesFragment(), null);
            ft.commit();
        }

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        initializeDependencies();
    }

    private void initializeDependencies() {
        weatherComponent = ((RxWeatherApplication) getApplication()).getAppComponent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public AppComponent getComponent() {
        return weatherComponent;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
        i.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName() );
        i.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        startActivity(i);
        return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult != null && connectionResult.getErrorMessage() != null) {
            Log.e(MainActivity.this.getClass().getSimpleName(), connectionResult.getErrorMessage());
        }
    }

    @Override
    public GoogleApiClient getApiClient() {
        return googleApiClient;
    }

    @Override
    public void onCitySuggestionSelected(City city) {

        getSupportFragmentManager().popBackStack();

        //Hide soft keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        getSupportFragmentManager().executePendingTransactions();
        ((CitiesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment)).presenter.addNewCity(city);

    }

    @Override
    public void onCitySelected(City city) {
        //if PHONE
        if (city != null && city.getCityWeather() != null) {
            Intent i = new Intent(this, WeatherDetailActivity.class);
            i.putExtra(WeatherDetailActivity.ARG_CITY, city);
            startActivity(i);
        }

        //TODO: if TABLET
        /* FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ForecastFragment fragment = new ForecastFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble("lat", places.get(0).getLatLng().latitude);
        bundle.putDouble("lon", places.get(0).getLatLng().longitude);

        fragment.setArguments(bundle);
        ft.replace(R.id.fragment, fragment, null);
        ft.addToBackStack(null);
        ft.commit();*/
    }

    @Override
    public void onAddCityButtonSelected() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        SearchFragment fragment = new SearchFragment();
        ft.add(R.id.fragment, fragment, null);
        ft.addToBackStack(null);
        ft.commit();
    }
}
