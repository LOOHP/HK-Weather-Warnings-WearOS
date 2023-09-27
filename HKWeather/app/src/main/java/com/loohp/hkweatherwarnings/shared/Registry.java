package com.loohp.hkweatherwarnings.shared;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.AtomicDouble;
import com.loohp.hkweatherwarnings.R;
import com.loohp.hkweatherwarnings.tiles.WeatherOverviewTile;
import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile;
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile;
import com.loohp.hkweatherwarnings.utils.ConnectionUtils;
import com.loohp.hkweatherwarnings.utils.HTTPRequestUtils;
import com.loohp.hkweatherwarnings.utils.JsonUtils;
import com.loohp.hkweatherwarnings.utils.LocationUtils;
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo;
import com.loohp.hkweatherwarnings.weather.HourlyWeatherInfo;
import com.loohp.hkweatherwarnings.weather.LunarDate;
import com.loohp.hkweatherwarnings.weather.TropicalCycloneInfo;
import com.loohp.hkweatherwarnings.weather.WeatherInfo;
import com.loohp.hkweatherwarnings.weather.WeatherStatusIcon;
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Registry {

    private static Registry INSTANCE = null;

    public static synchronized Registry getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Registry(context);
        }
        return INSTANCE;
    }

    private static final String PREFERENCES_FILE_NAME = "preferences.json";

    private static JSONObject PREFERENCES = null;

    private static List<JSONObject> WEATHER_STATIONS = null;
    private static List<JSONObject> HUMIDITY_STATIONS = null;
    private static List<JSONObject> WIND_STATIONS = null;
    private static JSONObject FORECAST_STATIONS = null;

    private Registry(Context context) {
        try {
            ensureData(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTileServices(Context context) {
        TileService.getUpdater(context).requestUpdate(WeatherOverviewTile.class);
        TileService.getUpdater(context).requestUpdate(WeatherWarningsTile.class);
        TileService.getUpdater(context).requestUpdate(WeatherTipsTile.class);
    }

    public void setLanguage(String language, Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("language", language);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileServices(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLanguage() {
        if (PREFERENCES == null) {
            return "zh";
        }
        String language = PREFERENCES.optString("language");
        if (language.isEmpty()) {
            return "zh";
        }
        return language;
    }

    public void setRefreshRate(long refreshRate, Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("refreshRate", refreshRate);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileServices(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public long getRefreshRate() {
        if (PREFERENCES == null) {
            return 1800000;
        }
        long refreshRate = PREFERENCES.optLong("refreshRate", -1);
        if (refreshRate < 0) {
            return 1800000;
        }
        return refreshRate;
    }

    public Pair<String, Location> getLocation() {
        if (PREFERENCES == null) {
            return Pair.create("", null);
        }
        Object location = PREFERENCES.opt("location");
        if (location == null) {
            return Pair.create("", null);
        }
        if (location instanceof String) {
            return Pair.create((String) location, null);
        }
        JSONArray pos = (JSONArray) location;
        return Pair.create("", LocationUtils.LocationResult.fromLatLng(pos.optDouble(0), pos.optDouble(1)).getLocation());
    }

    public void setLocation(Location location, Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("location", new JSONArray(Arrays.asList(location.getLatitude(), location.getLongitude())));
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileServices(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLocationGPS(Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("location", "GPS");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileServices(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLocation(Context context) {
        try {
            if (PREFERENCES == null) {
                return;
            }
            PREFERENCES.remove("location");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileServices(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<JSONObject> getWeatherStations() {
        return WEATHER_STATIONS;
    }

    public double findDistance(double lat1, double lng1, double lat2, double lng2) {
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lng1 = Math.toRadians(lng1);
        lng2 = Math.toRadians(lng2);

        double d_lon = lng2 - lng1;
        double d_lat = lat2 - lat1;
        double a = Math.pow(Math.sin(d_lat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(d_lon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return c * 6371;
    }

    private void ensureData(Context context) throws IOException {
        if (PREFERENCES != null) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.latest_1min_temperature), StandardCharsets.UTF_8))) {
            WEATHER_STATIONS = JsonUtils.toList(new JSONObject(reader.lines().collect(Collectors.joining())).optJSONArray("features"), JSONObject.class);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.latest_1min_humidity), StandardCharsets.UTF_8))) {
            HUMIDITY_STATIONS = JsonUtils.toList(new JSONObject(reader.lines().collect(Collectors.joining())).optJSONArray("features"), JSONObject.class);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.latest_10min_wind), StandardCharsets.UTF_8))) {
            WIND_STATIONS = JsonUtils.toList(new JSONObject(reader.lines().collect(Collectors.joining())).optJSONArray("features"), JSONObject.class);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.forecast_stations), StandardCharsets.UTF_8))) {
            FORECAST_STATIONS = new JSONObject(reader.lines().collect(Collectors.joining())).optJSONObject("stations");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8))) {
                PREFERENCES = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                PREFERENCES = new JSONObject();
                PREFERENCES.put("language", "zh");
                PREFERENCES.put("refreshRate", 1800000L);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Future<List<TropicalCycloneInfo>> getTropicalCycloneInfo(Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<List<TropicalCycloneInfo>> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://pda.weather.gov.hk/locspc/android_data/TCTrackData/TC/tcFront.json");
                if (data == null) {
                    future.complete(null);
                    return;
                }
                String imagesData = HTTPRequestUtils.getTextResponse("https://pda.weather.gov.hk/locspc/android_data/TCTrackImg/png_list.myobs");
                Set<String> images = imagesData == null ? Collections.emptySet() : Arrays.stream(imagesData.split("\\R")).collect(Collectors.toSet());
                List<TropicalCycloneInfo> list = new ArrayList<>();
                JSONArray array = data.optJSONArray("TC");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject tc = array.optJSONObject(i);
                    int id = tc.optInt("tcId");
                    int displayOrder = tc.optInt("displayOrder");
                    String nameZh = tc.optString("tcName");
                    String nameEn = tc.optString("enName");
                    String imgFile = "nwp_" + id + ".png";
                    String trackStaticImageUrl = images.contains(imgFile) ? "https://pda.weather.gov.hk/locspc/android_data/TCTrackImg/" + imgFile : null;
                    list.add(new TropicalCycloneInfo(id, displayOrder, nameZh, nameEn, trackStaticImageUrl));
                }
                future.complete(list);
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<LunarDate> getLunarDate(Context context, LocalDate date) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<LunarDate> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                String climatology = null;
                if (date.equals(LocalDate.now(Shared.Companion.getHK_TIMEZONE().toZoneId()))) {
                    JSONObject data = HTTPRequestUtils.getJSONResponse("https://pda.weather.gov.hk/locspc/android_data/lunar_date_uc.xml");
                    if (data != null) {
                        String term = data.optString("solar_term");
                        climatology = term.isEmpty() ? null : term;
                    }
                }
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/lunardate.php?date=" + dateStr);
                if (data == null) {
                    future.complete(null);
                    return;
                }
                String[] s = data.optString("LunarYear").split("，");
                future.complete(new LunarDate(s[0].replace("年", ""), s[1], data.optString("LunarDate"), climatology));
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<CurrentWeatherInfo> getCurrentWeatherInfo(Context context, LocationUtils.LocationResult locationResult) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<CurrentWeatherInfo> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now(Shared.Companion.getHK_TIMEZONE().toZoneId());
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dateHourFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
                String lang = getLanguage().equals("en") ? "en" : "tc";
                String lang2 = getLanguage().equals("en") ? "en" : "uc";
                Location location;
                String tempWeatherStationName = null;
                if (locationResult != null && locationResult.isSuccess()) {
                    location = locationResult.getLocation();
                } else {
                    location = Shared.Companion.getDEFAULT_LOCATION().getLocation();
                    tempWeatherStationName = getLanguage().equals("en") ? "Hong Kong" : "香港";
                }

                AtomicDouble minDistance = new AtomicDouble(Double.MAX_VALUE);
                JSONObject weatherStation = WEATHER_STATIONS.stream().min(Comparator.comparing(s -> {
                    JSONArray pos = s.optJSONObject("geometry").optJSONArray("coordinates");
                    double distance = findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(1), pos.optDouble(0));
                    if (distance < minDistance.get()) {
                        minDistance.set(distance);
                    }
                    return distance;
                })).orElseThrow(RuntimeException::new).optJSONObject("properties");

                String actualWeatherStationName;
                if (minDistance.get() > 100) {
                    tempWeatherStationName = getLanguage().equals("en") ? "Hong Kong" : "香港";
                    actualWeatherStationName = "";
                } else if (tempWeatherStationName == null) {
                    tempWeatherStationName = weatherStation.optString("AutomaticWeatherStation_" + lang2);
                    actualWeatherStationName = tempWeatherStationName;
                } else {
                    actualWeatherStationName = weatherStation.optString("AutomaticWeatherStation_" + lang2);
                }
                String weatherStationName = tempWeatherStationName;

                String temperatureLang = lang.equals("en") ? "" : "_uc";
                List<JSONObject> temperatureData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/hko_data/regional-weather/latest_1min_temperature" + temperatureLang + ".csv");
                if (temperatureData == null) {
                    future.complete(null);
                    return;
                }
                String temperatureStationField = lang.equals("en") ? "Automatic Weather Station" : "自動氣象站";
                String defaultTemperatureStation = lang.equals("en") ? "Hong Kong Observatory" : "天文台";
                JSONObject temperatureHere = temperatureData.stream().filter(e -> e.optString(temperatureStationField).equals(actualWeatherStationName)).findFirst()
                        .orElseGet(() -> temperatureData.stream().filter(e -> e.optString(temperatureStationField).equals(defaultTemperatureStation)).findFirst().orElse(null));
                if (temperatureHere == null) {
                    future.complete(null);
                    return;
                }
                float currentTemperature = (float) temperatureHere.optDouble(lang.equals("en") ? "Air Temperature(degree Celsius)" : "氣溫（攝氏）");

                String humidityStation = HUMIDITY_STATIONS.stream().min(Comparator.comparing(s -> {
                    JSONArray pos = s.optJSONObject("geometry").optJSONArray("coordinates");
                    return findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(1), pos.optDouble(0));
                })).map(e -> e.optJSONObject("properties").optString("AutomaticWeatherStation_" + lang2)).orElse("");

                String humidityLang = lang.equals("en") ? "" : "_uc";
                List<JSONObject> humidityData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/hko_data/regional-weather/latest_1min_humidity" + humidityLang + ".csv");
                if (humidityData == null) {
                    future.complete(null);
                    return;
                }
                String humidityStationField = lang.equals("en") ? "Automatic Weather Station" : "自動氣象站";
                String defaultHumidityStation = lang.equals("en") ? "Hong Kong Observatory" : "天文台";
                JSONObject humidityHere = humidityData.stream().filter(e -> e.optString(humidityStationField).equals(humidityStation)).findFirst()
                        .orElseGet(() -> humidityData.stream().filter(e -> e.optString(humidityStationField).equals(defaultHumidityStation)).findFirst().orElse(null));
                if (humidityHere == null) {
                    future.complete(null);
                    return;
                }
                float currentHumidity = (float) humidityHere.optDouble(lang.equals("en") ? "Relative Humidity(percent)" : "相對濕度（百分比）");

                JSONObject currentWeatherData = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=" + lang);
                float uvIndex = currentWeatherData.opt("uvindex") instanceof JSONObject ? (float) currentWeatherData.optJSONObject("uvindex").optJSONArray("data").optJSONObject(0).optDouble("value") : -1F;
                WeatherStatusIcon weatherIcon = WeatherStatusIcon.getByCode(currentWeatherData.optJSONArray("icon").optInt(0));

                String forecastStation = StreamSupport.stream(Spliterators.spliteratorUnknownSize(FORECAST_STATIONS.keys(), Spliterator.ORDERED), false).min(Comparator.comparing(k -> {
                    JSONArray pos = FORECAST_STATIONS.optJSONArray(k);
                    return findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(0), pos.optDouble(1));
                })).orElse(null);
                JSONObject forecastStationData = HTTPRequestUtils.getJSONResponse("https://maps.weather.gov.hk/ocf/dat/" + forecastStation + ".xml");
                JSONArray forecastDailyData = forecastStationData.optJSONArray("DailyForecast");
                List<JSONObject> dailyForecastArray = JsonUtils.toList(forecastDailyData, JSONObject.class);

                String chanceOfRainStr = dailyForecastArray.get(0).optString("ForecastChanceOfRain");
                float chanceOfRain = Float.parseFloat(chanceOfRainStr.substring(0, chanceOfRainStr.length() - 1));

                String windLang = lang.equals("en") ? "" : "_uc";
                List<JSONObject> windData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/hko_data/regional-weather/latest_10min_wind" + windLang + ".csv");
                if (windData == null) {
                    future.complete(null);
                    return;
                }

                String windStationField = lang.equals("en") ? "Automatic Weather Station" : "自動氣象站";
                String defaultWindStation = lang.equals("en") ? "Star Ferry" : "天星碼頭";
                JSONObject windHere = WIND_STATIONS.stream().min(Comparator.comparing(s -> {
                    JSONArray pos = s.optJSONObject("geometry").optJSONArray("coordinates");
                    return findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(1), pos.optDouble(0));
                })).map(e -> {
                    String stationName = e.optJSONObject("properties").optString("AutomaticWeatherStation_" + lang2);
                    return windData.stream().filter(s -> s.optString(windStationField).equals(stationName)).findFirst().orElse(null);
                }).filter(e -> {
                    return !e.optString(lang.equals("en") ? "10-Minute Mean Wind Direction(Compass points)" : "十分鐘平均風向（方位點）").equals("N/A");
                }).orElseGet(() -> {
                    return windData.stream().filter(e -> e.optString(windStationField).equals(defaultWindStation)).findFirst().orElse(null);
                });
                String windDirection;
                float windSpeed;
                float gust;
                if (windHere == null) {
                    windDirection = null;
                    windSpeed = -1F;
                    gust = -1F;
                } else {
                    String tempWindDirection = windHere.optString(lang.equals("en") ? "10-Minute Mean Wind Direction(Compass points)" : "十分鐘平均風向（方位點）");
                    if (tempWindDirection.equals("N/A")) {
                        windDirection = null;
                        windSpeed = -1F;
                        gust = -1F;
                    } else {
                        windDirection = tempWindDirection;
                        windSpeed = (float) windHere.optDouble(lang.equals("en") ? "10-Minute Mean Speed(km/hour)" : "十分鐘平均風速（公里/小時）");
                        gust = (float) windHere.optDouble(lang.equals("en") ? "10-Minute Maximum Gust(km/hour)" : "十分鐘最高陣風風速（公里/小時）");
                    }
                }

                String todayDateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                List<JSONObject> sunData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/opendata/opendata.php?dataType=SRS&year=" + today.getYear() + "&rformat=csv", s -> s.replaceAll("[^a-zA-Z.0-9:\\-,]", ""));
                if (sunData == null) {
                    future.complete(null);
                    return;
                }
                JSONObject todaySun = sunData.stream().filter(e -> e.optString("YYYY-MM-DD").equals(todayDateStr)).findFirst().orElse(null);
                LocalTime sunriseTime = LocalTime.parse(todaySun.optString("RISE"), timeFormatter);
                LocalTime sunTransitTime = LocalTime.parse(todaySun.optString("TRAN."), timeFormatter);
                LocalTime sunsetTime = LocalTime.parse(todaySun.optString("SET"), timeFormatter);

                List<JSONObject> moonData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/opendata/opendata.php?dataType=MRS&year=" + today.getYear() + "&rformat=csv", s -> s.replaceAll("[^a-zA-Z.0-9:\\-,]", ""));
                if (moonData == null) {
                    future.complete(null);
                    return;
                }
                JSONObject todayMoon = moonData.stream().filter(e -> e.optString("YYYY-MM-DD").equals(todayDateStr)).findFirst().orElse(null);
                LocalTime moonriseTime = todayMoon.optString("RISE").isEmpty() ? null : LocalTime.parse(todayMoon.optString("RISE"), timeFormatter);
                LocalTime moonTransitTime = todayMoon.optString("TRAN.").isEmpty() ? null : LocalTime.parse(todayMoon.optString("TRAN."), timeFormatter);
                LocalTime moonsetTime = todayMoon.optString("SET").isEmpty() ? null : LocalTime.parse(todayMoon.optString("SET"), timeFormatter);

                JSONObject forecastData = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=" + lang);
                if (forecastData == null) {
                    future.complete(null);
                    return;
                }

                JSONArray dayArray = forecastData.optJSONArray("weatherForecast");
                List<WeatherInfo> forecastInfo = new ArrayList<>(dayArray.length() - 1);

                JSONObject dayObj = dayArray.optJSONObject(0);
                float highestTemperature = (float) dayObj.optJSONObject("forecastMaxtemp").optDouble("value", -Float.MAX_VALUE);
                float lowestTemperature = (float) dayObj.optJSONObject("forecastMintemp").optDouble("value", -Float.MAX_VALUE);
                float maxRelativeHumidity = (float) dayObj.optJSONObject("forecastMaxrh").optDouble("value", -Float.MAX_VALUE);
                float minRelativeHumidity = (float) dayObj.optJSONObject("forecastMinrh").optDouble("value", -Float.MAX_VALUE);

                for (int i = 0; i < dayArray.length(); i++) {
                    JSONObject forecastDayObj = dayArray.optJSONObject(i);

                    String forecastDateStr = forecastDayObj.optString("forecastDate");
                    JSONObject forecastStationDayObj = dailyForecastArray.stream().filter(e -> e.optString("ForecastDate").equals(forecastDateStr)).findFirst().orElse(null);

                    LocalDate forecastDate = LocalDate.parse(forecastDateStr, dateFormatter);
                    float forecastHighestTemperature = (float) forecastDayObj.optJSONObject("forecastMaxtemp").optDouble("value", -Float.MAX_VALUE);
                    float forecastLowestTemperature = (float) forecastDayObj.optJSONObject("forecastMintemp").optDouble("value", -Float.MAX_VALUE);
                    float forecastMaxRelativeHumidity = (float) forecastDayObj.optJSONObject("forecastMaxrh").optDouble("value", -Float.MAX_VALUE);
                    float forecastMinRelativeHumidity = (float) forecastDayObj.optJSONObject("forecastMinrh").optDouble("value", -Float.MAX_VALUE);
                    WeatherStatusIcon forecastWeatherIcon = WeatherStatusIcon.getByCode(forecastDayObj.optInt("ForecastIcon"));

                    float forecastChanceOfRain;
                    if (forecastStationDayObj == null) {
                        forecastChanceOfRain = -1F;
                    } else {
                        String forecastChanceOfRainStr = forecastStationDayObj.optString("ForecastChanceOfRain");
                        forecastChanceOfRain = Float.parseFloat(forecastChanceOfRainStr.substring(0, forecastChanceOfRainStr.length() - 1));
                    }

                    forecastInfo.add(new WeatherInfo(forecastDate, forecastHighestTemperature, forecastLowestTemperature, forecastMaxRelativeHumidity, forecastMinRelativeHumidity, forecastChanceOfRain, forecastWeatherIcon));
                }

                JSONArray hourArray = forecastStationData.optJSONArray("HourlyWeatherForecast");
                List<HourlyWeatherInfo> hourlyWeatherInfo = new ArrayList<>(hourArray.length());
                WeatherStatusIcon lastHourIcon = weatherIcon;
                for (int i = 0; i < hourArray.length(); i++) {
                    JSONObject hourObj = hourArray.optJSONObject(i);

                    LocalDateTime hour = LocalDateTime.parse(hourObj.optString("ForecastHour"), dateHourFormatter);
                    float hourTemperature = (float) hourObj.optDouble("ForecastTemperature", -Float.MAX_VALUE);
                    float hourHumidity = (float) hourObj.optDouble("ForecastRelativeHumidity", -Float.MAX_VALUE);
                    float hourWindDirection = (float) hourObj.optDouble("ForecastWindDirection", -Float.MAX_VALUE);
                    float hourWindSpeed = (float) hourObj.optDouble("ForecastWindSpeed", -Float.MAX_VALUE);
                    WeatherStatusIcon hourIcon;
                    if (hourObj.has("ForecastWeather")) {
                        hourIcon = WeatherStatusIcon.getByCode(hourObj.optInt("ForecastWeather"));
                        if (hourIcon == null) {
                            hourIcon = lastHourIcon;
                        } else {
                            lastHourIcon = hourIcon;
                        }
                    } else {
                        hourIcon = lastHourIcon;
                    }

                    hourlyWeatherInfo.add(new HourlyWeatherInfo(hour, hourTemperature, hourHumidity, hourWindDirection, hourWindSpeed, hourIcon));
                }

                future.complete(new CurrentWeatherInfo(today, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon, weatherStationName, currentTemperature, currentHumidity, uvIndex, windDirection, windSpeed, gust, sunriseTime, sunTransitTime, sunsetTime, moonriseTime, moonTransitTime, moonsetTime, forecastInfo, hourlyWeatherInfo));
            } catch (Throwable e) {
                e.printStackTrace();
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<Map<WeatherWarningsType, String>> getActiveWarnings(Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Map<WeatherWarningsType, String>> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                String lang = getLanguage().equals("en") ? "en" : "tc";

                JSONArray data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warningInfo&lang=" + lang).optJSONArray("details");
                if (data == null) {
                    future.complete(null);
                    return;
                }
                Map<WeatherWarningsType, String> warnings = new EnumMap<>(WeatherWarningsType.class);
                for (int i = 0; i < data.length(); i++) {
                    JSONObject details = data.optJSONObject(i);
                    try {
                        WeatherWarningsType warningType = WeatherWarningsType.valueOf(details.optString("warningStatementCode").toUpperCase());
                        String warningName = getLanguage().equals("en") ? warningType.getNameEn() : warningType.getNameZh();
                        JSONArray contentsArray = details.optJSONArray("contents");
                        String contents;
                        if (contentsArray == null || contentsArray.length() == 0) {
                            contents = null;
                        } else {
                            List<String> lines = JsonUtils.toList(contentsArray, String.class);
                            if (!lines.get(0).trim().equalsIgnoreCase(warningName)) {
                                lines.add(0, warningName);
                            }
                            contents = String.join("\n", lines);
                            OffsetDateTime time = OffsetDateTime.parse(details.optString("updateTime"));
                            if (getLanguage().equals("en")) {
                                contents += "\nDispatched by the Hong Kong Observatory at " + DateTimeFormatter.ofPattern("HH:mm' HKT on 'dd.MM.yyyy", Locale.ENGLISH);
                            } else {
                                contents += "\n以上天氣稿由天文台於" + DateTimeFormatter.ofPattern("yyyy年MM月dd日HH時mm分", Locale.TRADITIONAL_CHINESE).format(time) + "發出";
                            }
                        }
                        warnings.put(warningType, contents);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                future.complete(warnings);
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<List<Pair<String, Long>>> getWeatherTips(Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<List<Pair<String, Long>>> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                String lang = getLanguage().equals("en") ? "en" : "tc";
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=swt&lang=" + lang);
                if (data == null) {
                    future.complete(null);
                    return;
                }
                if (!data.has("swt")) {
                    future.complete(Collections.emptyList());
                    return;
                }
                JSONArray array = data.getJSONArray("swt");
                List<Pair<String, Long>> tips = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(obj.optString("updateTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    tips.add(Pair.create(obj.optString("desc"), zonedDateTime.toInstant().toEpochMilli()));
                }
                future.complete(tips);
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

}