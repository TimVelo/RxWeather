package com.feresr.weather.UI;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.feresr.weather.R;
import com.feresr.weather.UI.views.InfoDisplay;
import com.feresr.weather.models.CityWeather;
import com.feresr.weather.models.Currently;
import com.feresr.weather.models.Daily;
import com.feresr.weather.models.Day;
import com.feresr.weather.models.DisplayWeatherInfo;
import com.feresr.weather.models.Hourly;
import com.feresr.weather.models.Warning;
import com.feresr.weather.utils.IconManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Fernando on 19/10/2015.
 */
public class ForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int CURRENTLY = 0;
    private static final int HOURLY = 1;
    private static final int DAILY = 2;
    private static final int DAY = 3;
    private static final int WARNING = 4;
    private static final int UPDATED_AT = 5;

    private LayoutInflater inflater;
    private List<DisplayWeatherInfo> weatherInfo;
    private Context context;
    private DayForecastAdapter dayForecastAdapter;
    private boolean celsius = true;
    private Typeface font;
    private long fetchTime;

    public ForecastAdapter(Context context) {
        super();
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        font = Typeface.createFromAsset(context.getAssets(), "weathericons-regular-webfont.ttf");
        dayForecastAdapter = new DayForecastAdapter(context);
        this.weatherInfo = new ArrayList<>();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String syncConnPref = sharedPref.getString(SettingsActivity.PREF_UNIT, "celsius");
        if (syncConnPref.equals("celsius")) {
            celsius = true;
        } else {
            celsius = false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == weatherInfo.size()) {
            return UPDATED_AT;
        }
        DisplayWeatherInfo weatherInfoObject = weatherInfo.get(position);

        if (weatherInfoObject instanceof Currently) {
            return CURRENTLY;
        } else if (weatherInfoObject instanceof Hourly) {
            return HOURLY;
        } else if (weatherInfoObject instanceof Daily) {
            return DAILY;
        } else if (weatherInfoObject instanceof Day) {
            return DAY;
        } else if (weatherInfoObject instanceof Warning) {
            return WARNING;
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case CURRENTLY:
                view = this.inflater.inflate(R.layout.currently_view, parent, false);
                return new CurrentlyViewHolder(view, font);
            case HOURLY:
                view = this.inflater.inflate(R.layout.hourly_view, parent, false);
                return new HourlyViewHolder(view, new LinearLayoutManager(context));
            case DAILY:
                view = this.inflater.inflate(R.layout.daily_view, parent, false);
                return new DailyViewHolder(view);
            case DAY:
                view = this.inflater.inflate(R.layout.day_view, parent, false);
                return new DayViewHolder(view, font);
            case WARNING:
                view = this.inflater.inflate(R.layout.warning_view, parent, false);
                return new WarningViewHolder(view);
            case UPDATED_AT:
                view = this.inflater.inflate(R.layout.updated_at, parent, false);
                return new UpdatedAtViewHolder(view);
            default:
                return null;
        }
    }


    public void addForecast(CityWeather cityWeather) {
        weatherInfo.add(cityWeather.getCurrently());
        weatherInfo.add(cityWeather.getHourly());

        dayForecastAdapter.addHourForecast(cityWeather.getHourly().getData());

        fetchTime = cityWeather.getFetchTime();

        weatherInfo.add(cityWeather.getDaily());
        for (int i = 0; i < cityWeather.getDaily().getDays().size(); i++) {
            if (i == 0) {
                cityWeather.getDaily().getDays().get(i).setIsToday(true);
            }

            if (i == 1) {
                cityWeather.getDaily().getDays().get(i).setIsTomorrow(true);
            }

            if (i == cityWeather.getDaily().getDays().size() - 1) {
                cityWeather.getDaily().getDays().get(i).setIsLastDayOfWeek(true);
            }

            weatherInfo.add(cityWeather.getDaily().getDays().get(i));
        }


        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case HOURLY:
                HourlyViewHolder labelViewHolder = (HourlyViewHolder) viewHolder;
                Hourly hourly = (Hourly) weatherInfo.get(position);
                labelViewHolder.description.setText(hourly.getSummary());
                labelViewHolder.view.setBackgroundColor(IconManager.getColorResource(hourly.getIcon(), context));
                labelViewHolder.recyclerView.setAdapter(dayForecastAdapter);
                break;
            case WARNING:
                WarningViewHolder warningViewHolder = (WarningViewHolder) viewHolder;
                Warning warning = (Warning) weatherInfo.get(position);
                warningViewHolder.title.setText(warning.getText());
                warningViewHolder.explanation.setText(warning.getExplanation());
                break;

            case CURRENTLY:
                CurrentlyViewHolder currentlyViewHolder = (CurrentlyViewHolder) viewHolder;
                Currently currently = (Currently) weatherInfo.get(position);

                currentlyViewHolder.main.setBackgroundColor(currently.getColor(context));
                currentlyViewHolder.description.setText(currently.getSummary().toUpperCase());
                if (celsius) {
                    currentlyViewHolder.feelsLike.setValue(context.getString(R.string.degree, Math.round(currently.getApparentTemperature())));
                    currentlyViewHolder.temp.setText(context.getString(R.string.degree, Math.round(currently.getTemperature())));
                } else {
                    currentlyViewHolder.feelsLike.setValue(context.getString(R.string.degree, Math.round(currently.getApparentTemperature() * 1.8 + 32)));
                    currentlyViewHolder.temp.setText(context.getString(R.string.degree, Math.round(currently.getTemperature() * 1.8 + 32)));
                }
                currentlyViewHolder.humidity.setValue(Math.round(currently.getHumidity() * 100) + "%");
                currentlyViewHolder.wind.setValue(context.getString(R.string.km_h, Math.round(currently.getWindSpeed())));
                currentlyViewHolder.wind.setSubValue(currently.getWindBearingString());

                currentlyViewHolder.pressure.setValue(context.getString(R.string.hPa, Math.round(currently.getPressure())));
                currentlyViewHolder.clouds.setValue(Math.round(currently.getCloudCover() * 100) + "%");
                currentlyViewHolder.precipitation.setValue(Math.round(currently.getPrecipProbability() * 100) + "%");
                currentlyViewHolder.precipitation.setSubValue(new DecimalFormat("#.##").format(currently.getPrecipIntensity() * 100) + "cm");

                currentlyViewHolder.icon.setText(IconManager.getIconResource(currently.getIcon(), context));

                break;
            case DAILY:
                DailyViewHolder dailyViewHolder = (DailyViewHolder) viewHolder;
                Daily daily = (Daily) weatherInfo.get(position);
                dailyViewHolder.description.setText(daily.getSummary());
                break;
            case UPDATED_AT:
                UpdatedAtViewHolder updatedAtViewHolder = (UpdatedAtViewHolder) viewHolder;

                updatedAtViewHolder.updatedAt.setText(context.getString(R.string.updated_at) + " " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(fetchTime)).toUpperCase());
                break;
            case DAY:
                Day day = (Day) weatherInfo.get(position);
                DayViewHolder holder = (DayViewHolder) viewHolder;
                holder.main.setText(day.getSummary());
                if (celsius) {
                    holder.tempMax.setText(day.getTemperatureMax() + "°");
                    holder.tempMin.setText(day.getTemperatureMin() + "°");
                } else {
                    holder.tempMax.setText((Math.round(day.getTemperatureMax() * 1.8 + 32) * 100.0) / 100.0 + "°");
                    holder.tempMin.setText((Math.round(day.getTemperatureMin() * 1.8 + 32) * 100.0) / 100.0 + "°");
                }
                holder.icon.setText(IconManager.getIconResource(day.getIcon(), context));


                if (position % 2 == 0) {
                    float[] hsv = new float[3];
                    Color.colorToHSV(day.getColor(context), hsv);
                    hsv[2] *= 0.94f; // value component
                    holder.view.setBackgroundColor(Color.HSVToColor(hsv));
                } else {
                    holder.view.setBackgroundColor(day.getColor(context));
                }


                //First
                if (day.isToday()) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    holder.dayName.setText(context.getString(R.string.today));

                    holder.view.setLayoutParams(params);
                } else if (day.isLastDayOfWeek()) {
                    holder.dayName.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(day.getTime() * 1000L)).toUpperCase());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    holder.view.setLayoutParams(params);
                } else {
                    if (day.isTomorrow()) {
                        holder.dayName.setText(context.getString(R.string.tomorrow));
                    } else {
                        holder.dayName.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(day.getTime() * 1000L)).toUpperCase());
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    holder.view.setLayoutParams(params);
                }
        }
    }

    @Override
    public int getItemCount() {
        return weatherInfo.size() + 1;
    }

    public void showNoInternetWarning() {
        Warning warning = new Warning(context.getString(R.string.no_internet_title), context.getString(R.string.no_internet_description));
        weatherInfo.add(0, warning);
        notifyItemInserted(0);
    }

    public void hideNoInternetWarning() {
        if (weatherInfo.get(0) instanceof Warning) {
            weatherInfo.remove(0);
            notifyItemRemoved(0);
        }
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayName;
        TextView main;
        TextView temp;
        TextView tempMax;
        TextView icon;
        TextView tempMin;
        LinearLayout view;

        public DayViewHolder(View itemView, Typeface font) {
            super(itemView);
            view = (LinearLayout) itemView;
            dayName = (TextView) itemView.findViewById(R.id.day);
            main = (TextView) itemView.findViewById(R.id.main);
            temp = (TextView) itemView.findViewById(R.id.temp);
            tempMax = (TextView) itemView.findViewById(R.id.tempMax);
            tempMin = (TextView) itemView.findViewById(R.id.tempMin);
            icon = (TextView) itemView.findViewById(R.id.icon);
            icon.setTypeface(font);

            icon.setText(R.string.today_weather_main_icon);
        }
    }

    public static class WarningViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView explanation;

        public WarningViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            explanation = (TextView) itemView.findViewById(R.id.explanation);
        }
    }

    public static class DailyViewHolder extends RecyclerView.ViewHolder {
        TextView description;

        public DailyViewHolder(View itemView) {
            super(itemView);
            description = (TextView) itemView.findViewById(R.id.description);
        }
    }

    public static class UpdatedAtViewHolder extends RecyclerView.ViewHolder {
        TextView updatedAt;

        public UpdatedAtViewHolder(View itemView) {
            super(itemView);
            updatedAt = (TextView) itemView.findViewById(R.id.updated_at);
        }
    }

    public static class HourlyViewHolder extends RecyclerView.ViewHolder {
        TextView description;
        RecyclerView recyclerView;
        LinearLayout view;

        public HourlyViewHolder(View itemView, LinearLayoutManager linearLayoutManager) {
            super(itemView);
            view = (LinearLayout) itemView;
            description = (TextView) itemView.findViewById(R.id.description);
            recyclerView = (RecyclerView) itemView.findViewById(R.id.nextHoursRecyclerview);
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerView.setLayoutManager(linearLayoutManager);
        }
    }

    public static class CurrentlyViewHolder extends RecyclerView.ViewHolder {
        FrameLayout main;
        TextView temp;
        TextView description;
        InfoDisplay humidity;
        InfoDisplay wind;
        InfoDisplay pressure;
        InfoDisplay clouds;
        InfoDisplay precipitation;
        InfoDisplay feelsLike;
        TextView icon;

        public CurrentlyViewHolder(View itemView, Typeface font) {
            super(itemView);
            main = (FrameLayout) itemView.findViewById(R.id.main_info);
            temp = (TextView) itemView.findViewById(R.id.temp);
            description = (TextView) itemView.findViewById(R.id.description);
            humidity = (InfoDisplay) itemView.findViewById(R.id.humidity);
            wind = (InfoDisplay) itemView.findViewById(R.id.tempMax);
            pressure = (InfoDisplay) itemView.findViewById(R.id.tempMin);
            clouds = (InfoDisplay) itemView.findViewById(R.id.clouds);
            precipitation = (InfoDisplay) itemView.findViewById(R.id.precipitation);
            feelsLike = (InfoDisplay) itemView.findViewById(R.id.feels_like);

            icon = (TextView) itemView.findViewById(R.id.main_icon);
            icon.setTypeface(font);

            icon.setText(R.string.today_weather_main_icon);
        }
    }
}
