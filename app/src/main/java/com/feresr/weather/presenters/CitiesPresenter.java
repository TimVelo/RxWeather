package com.feresr.weather.presenters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.feresr.weather.NetworkListener;
import com.feresr.weather.NetworkReceiver;
import com.feresr.weather.UI.CitiesAdapter;
import com.feresr.weather.UI.FragmentInteractionsListener;
import com.feresr.weather.UI.RecyclerItemClickListener;
import com.feresr.weather.UI.SettingsActivity;
import com.feresr.weather.domain.GetCitiesUseCase;
import com.feresr.weather.domain.GetCityForecastUseCase;
import com.feresr.weather.domain.RemoveCityUseCase;
import com.feresr.weather.domain.SaveCityUseCase;
import com.feresr.weather.models.City;
import com.feresr.weather.presenters.views.View;
import com.feresr.weather.storage.SimpleCache;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Fernando on 6/11/2015.
 */
public class CitiesPresenter implements Presenter, NetworkListener, android.view.View.OnClickListener, RecyclerItemClickListener.OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private CitiesAdapter citiesAdapter;
    private GetCityForecastUseCase getCityWeatherUseCase;
    private RemoveCityUseCase removeCityUseCase;
    private CitiesView citiesView;
    private CompositeSubscription subscriptions;
    private GetCitiesUseCase getCitiesUseCase;
    private GoogleApiClient googleApiClient;
    private SaveCityUseCase saveCityUseCase;
    private FragmentInteractionsListener fragmentInteractionListener;
    private NetworkReceiver networkReceiver;
    private Context context;
    private UpdatesReceiver updatesReceiver;


    @Inject
    public CitiesPresenter(Context context, GetCitiesUseCase getCitiesUseCase, GetCityForecastUseCase getCityForecastUseCase, RemoveCityUseCase removeCityUseCase, SaveCityUseCase saveCityUseCase) {
        super();
        this.getCityWeatherUseCase = getCityForecastUseCase;
        this.saveCityUseCase = saveCityUseCase;
        this.getCitiesUseCase = getCitiesUseCase;
        this.removeCityUseCase = removeCityUseCase;
        this.subscriptions = new CompositeSubscription();
        networkReceiver = new NetworkReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkReceiver.setListener(this);
        context.registerReceiver(networkReceiver, intentFilter);
        this.context = context;

        updatesReceiver =  new UpdatesReceiver();
        IntentFilter updatesIntentFilter = new IntentFilter("com.feresr.weather.UPDATE_WEATHER_DATA");
        context.registerReceiver(updatesReceiver, updatesIntentFilter);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        boolean alarmUp = (PendingIntent.getBroadcast(context, 0,
                new Intent("com.feresr.weather.UPDATE_WEATHER_DATA"),
                PendingIntent.FLAG_NO_CREATE) != null);

        if (!alarmUp) {
            Intent intent = new Intent("com.feresr.weather.UPDATE_WEATHER_DATA");
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SimpleCache.REFRESH_TIME,
                    SimpleCache.REFRESH_TIME, pi);
        }
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onPause() {

    }

    @Override
    public void attachView(View v) {
        citiesView = (CitiesView) v;
    }

    @Override
    public void attachIncomingArg(Bundle intent) {

    }

    public void setAdapter(CitiesAdapter adapter) {
        this.citiesAdapter = adapter;
    }

    @Override
    public void onCreate() {
        reloadCities();
    }

    private void reloadCities() {
        Subscription subscription = getCitiesUseCase.execute().doOnNext(new Action1<List<City>>() {
            @Override
            public void call(List<City> cities) {
                //Add all cities to view
                citiesView.addCities(cities);
            }
        }).flatMapIterable(new Func1<List<City>, Iterable<City>>() {
            @Override
            public Iterable<City> call(List<City> cities) {
                return cities;
            }
        }).flatMap(new Func1<City, Observable<City>>() {
            @Override
            public Observable<City> call(City city) {
                getCityWeatherUseCase.setCity(city);
                return getCityWeatherUseCase.execute();
            }
        }).subscribe(new Subscriber<City>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                //Could not fetch weather
                if (e != null) {
                    Log.e(this.getClass().getSimpleName(), e.toString());
                }
            }

            @Override
            public void onNext(City city) {
                city.setState(City.STATE_DONE);
                citiesView.updateCity(city);
            }
        });

        subscriptions.add(subscription);
    }

    @Override
    public void onDestroy() {
        networkReceiver.setListener(null);
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        context.unregisterReceiver(networkReceiver);
        context.unregisterReceiver(updatesReceiver);
        subscriptions.unsubscribe();
    }

    public void addNewCity(final City city) {
        if (city.getLat() == null || city.getLon() == null) {
            if (googleApiClient.isConnected()) {


                Places.GeoDataApi.getPlaceById(googleApiClient, city.getId()).setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (places != null && places.getCount() >= 1 && places.get(0) != null) {
                            city.setState(City.STATE_FETCHING);
                            Place place = places.get(0);
                            city.setLat(place.getLatLng().latitude);
                            city.setLon(place.getLatLng().longitude);
                            saveCityUseCase.setCity(city);

                            //Touches UI, we unsubscribe when completed, but also on conf changes (subscription.unsubscribe)
                            subscriptions.add(saveCityUseCase.execute().flatMap(new Func1<City, Observable<City>>() {
                                @Override
                                public Observable<City> call(City city) {
                                    citiesView.addCity(city);
                                    getCityWeatherUseCase.setCity(city);
                                    getCityWeatherUseCase.setFetchIfExpired(true);
                                    return getCityWeatherUseCase.execute();
                                }
                            }).subscribe(new Subscriber<City>() {
                                @Override
                                public void onCompleted() {
                                    subscriptions.remove(this);
                                    this.unsubscribe();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e != null) {
                                        Log.e(this.getClass().getSimpleName(), e.toString());
                                    }
                                }

                                @Override
                                public void onNext(City city) {
                                    city.setState(City.STATE_DONE);
                                    citiesView.updateCity(city);
                                }
                            }));

                            DataBufferUtils.freezeAndClose(places);
                        }

                    }
                });
            } else {
                Log.e(this.getClass().getSimpleName(), "GoogleApiClient not connected");
            }
        }
    }

    public void onRemoveCity(City city) {
        removeCityUseCase.setCity(city);

        //Does not touch UI, let it run on the background and un-subscribe by itself
        removeCityUseCase.execute().subscribe(new Subscriber<City>() {
            @Override
            public void onCompleted() {
                this.unsubscribe();
            }

            @Override
            public void onError(Throwable e) {
                if (e != null) {
                    Log.e("error", e.toString());
                }
            }

            @Override
            public void onNext(City city) {
                Log.e("onRemoveCity", "city removed");
            }
        });
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    @Override
    public void onClick(android.view.View v) {
        fragmentInteractionListener.onAddCityButtonSelected();
    }

    public void setFragmentInteractionListener(FragmentInteractionsListener fragmentInteractionListener) {
        this.fragmentInteractionListener = fragmentInteractionListener;
    }

    @Override
    public void onItemClick(android.view.View view, int position) {
        fragmentInteractionListener.onCitySelected(citiesAdapter.getCities().get(position));
    }

    @Override
    public void onNetworkStateChanged(boolean online) {
        if (online) {
            subscriptions.add(Observable.from(citiesAdapter.getCities()).flatMap(new Func1<City, Observable<City>>() {
                @Override
                public Observable<City> call(City city) {
                    if (city.getCityWeather() == null) {
                        getCityWeatherUseCase.setCity(city);
                        return getCityWeatherUseCase.execute();
                    }
                    return null;
                }
            }).subscribe(new Subscriber<City>() {
                @Override
                public void onCompleted() {
                    subscriptions.remove(this);
                    this.unsubscribe();
                }

                @Override
                public void onError(Throwable e) {
                    if (e != null) {
                        Log.e(this.getClass().getSimpleName(), e.toString());
                    }
                }

                @Override
                public void onNext(City city) {
                    city.setState(City.STATE_DONE);
                    citiesView.updateCity(city);
                }
            }));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case SettingsActivity.PREF_UNIT:
                if (sharedPreferences.getString(key, "celsius").equals("celsius")) {
                    citiesView.showTemperatureInCelsius();
                } else {
                    citiesView.showTemperatureInFahrenheit();
                }
            break;
            case SettingsActivity.GRIDVIEW:
                if (sharedPreferences.getBoolean(key, false)) {
                    citiesView.setSetColumns(2);
                } else {
                    citiesView.setSetColumns(1);
                }
            break;
        }
    }

    @Override
    public void onRefresh() {
        subscriptions.add(Observable.from(citiesAdapter.getCities()).flatMap(new Func1<City, Observable<City>>() {
            @Override
            public Observable<City> call(City city) {
                getCityWeatherUseCase.setCity(city);
                getCityWeatherUseCase.setFetchIfExpired(true);
                return getCityWeatherUseCase.execute();
            }
        }).subscribe(new Subscriber<City>() {
            @Override
            public void onCompleted() {
                subscriptions.remove(this);
                this.unsubscribe();
            }

            @Override
            public void onError(Throwable e) {
                if (e != null) {
                    Log.e(this.getClass().getSimpleName(), e.toString());
                }
            }

            @Override
            public void onNext(City city) {
                city.setState(City.STATE_DONE);
                citiesView.updateCity(city);
            }
        }));
    }

    public class UpdatesReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            reloadCities();
        }
    }
}