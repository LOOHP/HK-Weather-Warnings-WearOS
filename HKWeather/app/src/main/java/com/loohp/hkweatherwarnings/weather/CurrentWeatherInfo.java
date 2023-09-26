package com.loohp.hkweatherwarnings.weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CurrentWeatherInfo extends WeatherInfo {

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static CurrentWeatherInfo deserialize(JSONObject jsonObject) {
        LocalDate date = LocalDate.parse(jsonObject.optString("date"), DATE_FORMATTER);
        float highestTemperature = (float) jsonObject.optDouble("highestTemperature");
        float lowestTemperature = (float) jsonObject.optDouble("lowestTemperature");
        float maxRelativeHumidity = (float) jsonObject.optDouble("maxRelativeHumidity");
        float minRelativeHumidity = (float) jsonObject.optDouble("minRelativeHumidity");
        float chanceOfRain = (float) jsonObject.optDouble("chanceOfRain");
        WeatherStatusIcon weatherIcon = WeatherStatusIcon.valueOf(jsonObject.optString("weatherIcon"));
        String weatherStation = jsonObject.optString("weatherStation");
        float currentTemperature = (float) jsonObject.optDouble("currentTemperature");
        float currentHumidity = (float) jsonObject.optDouble("currentHumidity");
        float uvIndex = (float) jsonObject.optDouble("uvIndex");
        String windDirection = jsonObject.optString("windDirection");
        float windSpeed = (float) jsonObject.optDouble("windSpeed");
        float gust = (float) jsonObject.optDouble("gust");
        LocalTime sunriseTime = LocalTime.parse(jsonObject.optString("sunriseTime"), TIME_FORMATTER);
        LocalTime sunTransitTime = LocalTime.parse(jsonObject.optString("sunTransitTime"), TIME_FORMATTER);
        LocalTime sunsetTime = LocalTime.parse(jsonObject.optString("sunsetTime"), TIME_FORMATTER);
        LocalTime moonriseTime = jsonObject.optString("moonriseTime").isEmpty() ? null : LocalTime.parse(jsonObject.optString("moonriseTime"), TIME_FORMATTER);
        LocalTime moonTransitTime = jsonObject.optString("moonTransitTime").isEmpty() ? null : LocalTime.parse(jsonObject.optString("moonTransitTime"), TIME_FORMATTER);
        LocalTime moonsetTime = jsonObject.optString("moonsetTime").isEmpty() ? null : LocalTime.parse(jsonObject.optString("moonsetTime"), TIME_FORMATTER);
        JSONArray forecastInfoArray = jsonObject.optJSONArray("forecastInfo");
        List<WeatherInfo> forecastInfo = new ArrayList<>(forecastInfoArray.length());
        for (int i = 0; i < forecastInfoArray.length(); i++) {
            forecastInfo.add(WeatherInfo.deserialize(forecastInfoArray.optJSONObject(i)));
        }
        JSONArray hourlyWeatherInfoArray = jsonObject.optJSONArray("hourlyWeatherInfo");
        List<HourlyWeatherInfo> hourlyWeatherInfo = new ArrayList<>(hourlyWeatherInfoArray.length());
        for (int i = 0; i < hourlyWeatherInfoArray.length(); i++) {
            hourlyWeatherInfo.add(HourlyWeatherInfo.deserialize(hourlyWeatherInfoArray.optJSONObject(i)));
        }
        return new CurrentWeatherInfo(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon, weatherStation, currentTemperature, currentHumidity, uvIndex, windDirection, windSpeed, gust, sunriseTime, sunTransitTime, sunsetTime, moonriseTime, moonTransitTime, moonsetTime, forecastInfo, hourlyWeatherInfo);
    }

    private final String weatherStation;
    private final float currentTemperature;
    private final float currentHumidity;
    private final float uvIndex;
    private final String windDirection;
    private final float windSpeed;
    private final float gust;
    private final LocalTime sunriseTime;
    private final LocalTime sunTransitTime;
    private final LocalTime sunsetTime;
    private final LocalTime moonriseTime;
    private final LocalTime moonTransitTime;
    private final LocalTime moonsetTime;
    private final List<WeatherInfo> forecastInfo;
    private final List<HourlyWeatherInfo> hourlyWeatherInfo;

    public CurrentWeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, WeatherStatusIcon weatherIcon, String weatherStation, float currentTemperature, float currentHumidity, float uvIndex, String windDirection, float windSpeed, float gust, LocalTime sunriseTime, LocalTime sunTransitTime, LocalTime sunsetTime, LocalTime moonriseTime, LocalTime moonTransitTime, LocalTime moonsetTime, List<WeatherInfo> forecastInfo, List<HourlyWeatherInfo> hourlyWeatherInfo) {
        super(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon);
        this.weatherStation = weatherStation;
        this.currentTemperature = currentTemperature;
        this.currentHumidity = currentHumidity;
        this.uvIndex = uvIndex;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.gust = gust;
        this.sunriseTime = sunriseTime;
        this.sunTransitTime = sunTransitTime;
        this.sunsetTime = sunsetTime;
        this.moonriseTime = moonriseTime;
        this.moonTransitTime = moonTransitTime;
        this.moonsetTime = moonsetTime;
        this.forecastInfo = Collections.unmodifiableList(forecastInfo);
        this.hourlyWeatherInfo = Collections.unmodifiableList(hourlyWeatherInfo);
    }

    public String getWeatherStation() {
        return weatherStation;
    }

    public float getCurrentTemperature() {
        return currentTemperature;
    }

    public float getCurrentHumidity() {
        return currentHumidity;
    }

    public float getUvIndex() {
        return uvIndex;
    }

    public LocalTime getSunriseTime() {
        return sunriseTime;
    }

    public LocalTime getSunTransitTime() {
        return sunTransitTime;
    }

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getMoonriseTime() {
        return moonriseTime;
    }

    public LocalTime getMoonTransitTime() {
        return moonTransitTime;
    }

    public LocalTime getMoonsetTime() {
        return moonsetTime;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public float getGust() {
        return gust;
    }

    public List<WeatherInfo> getForecastInfo() {
        return forecastInfo;
    }

    public List<HourlyWeatherInfo> getHourlyWeatherInfo() {
        return hourlyWeatherInfo;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = super.serialize();
        jsonObject.put("weatherStation", weatherStation);
        jsonObject.put("currentTemperature", currentTemperature);
        jsonObject.put("currentHumidity", currentHumidity);
        jsonObject.put("uvIndex", uvIndex);
        jsonObject.put("windDirection", windDirection);
        jsonObject.put("windSpeed", windSpeed);
        jsonObject.put("gust", gust);
        jsonObject.put("sunriseTime", sunriseTime.format(TIME_FORMATTER));
        jsonObject.put("sunTransitTime", sunTransitTime.format(TIME_FORMATTER));
        jsonObject.put("sunsetTime", sunsetTime.format(TIME_FORMATTER));
        jsonObject.put("moonriseTime", moonriseTime == null ? "" : moonriseTime.format(TIME_FORMATTER));
        jsonObject.put("moonTransitTime", moonTransitTime == null ? "" : moonTransitTime.format(TIME_FORMATTER));
        jsonObject.put("moonsetTime", moonsetTime == null ? "" : moonsetTime.format(TIME_FORMATTER));
        JSONArray forecastInfoArray = new JSONArray();
        for (WeatherInfo weatherInfo : forecastInfo) {
            forecastInfoArray.put(weatherInfo.serialize());
        }
        jsonObject.put("forecastInfo", forecastInfoArray);
        JSONArray hourlyWeatherInfoArray = new JSONArray();
        for (HourlyWeatherInfo hourlyInfo : hourlyWeatherInfo) {
            hourlyWeatherInfoArray.put(hourlyInfo.serialize());
        }
        jsonObject.put("hourlyWeatherInfo", hourlyWeatherInfoArray);
        return jsonObject;
    }

}
