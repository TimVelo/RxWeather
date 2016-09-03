package com.feresr.weather.UI;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import com.feresr.weather.DI.component.ActivityComponent;
import com.feresr.weather.DI.component.ApplicationComponent;
import com.feresr.weather.R;
import com.feresr.weather.RxWeatherApplication;
import com.feresr.weather.UI.fragment.ForecastFragment;
import com.feresr.weather.UI.fragment.ForecastPagerFragment;
import com.feresr.weather.common.BaseActivity;
import com.feresr.weather.common.BasePresenter;
import com.feresr.weather.models.City;
import com.feresr.weather.utils.IconManager;

public class WeatherDetailActivity extends BaseActivity implements ForecastFragment.RecyclerViewScrollListener {

    public static final String ARG_CITY = "city";
    private ApplicationComponent weatherApiComponent;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        City city = (City) intent.getExtras().getSerializable(ARG_CITY);

        if (savedInstanceState == null) {
            FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
            ForecastPagerFragment fragment = ForecastPagerFragment.newInstance(city);
            ft.replace(R.id.container, fragment, null);
            ft.commit();
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(IconManager.getColorResource(city.getCityWeather().getCurrently().getIcon(), this));
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(city.getName().split(",")[0]);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(0);
        }
        findViewById(R.id.layout).setBackgroundColor(IconManager.getColorResource(city.getCityWeather().getCurrently().getIcon(), this));
        initializeDependencies();
    }

    @Override
    protected void injectDependencies(ActivityComponent activityComponent) {
        //nothing to inject yet
    }

    @Nullable
    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_weather_detail;
    }

    private void initializeDependencies() {
        weatherApiComponent = ((RxWeatherApplication) getApplication()).getComponent();
    }


    @Override
    public void onScrolled(int scrolled) {
        scrolled /= 2;
        if (scrolled > 255) {
            scrolled = 255;
        }
        toolbar.setBackgroundColor(Color.argb(scrolled, 5, 5, 5));
    }
}
