package org.estimilian;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/* Условие:
3a. Задание можно выполнить на любом языке программирования.
        Задача: разработать программу, которая на основании данных сервиса https://openweathermap.org/ (требует регистрации, достаточно бесплатного плана Free) будет выводить следующие данные для Вашего города:
        1. День, с минимальной разницей "ощущаемой" и фактической температуры ночью (с указанием разницы в градусах Цельсия)
        2. Максимальную продолжительностью светового дня (считать, как разницу между временем заката и рассвета) за ближайшие 5 дней (включая текущий), с указанием даты.
*/
public class main {

    private static final int CONNECTION_TIMEOUT = 2000;
    private static HttpURLConnection con;

    public static void main(String[] args)  {

        try {
            final URL url = new URL("https://api.openweathermap.org/data/2.5/onecall?lat=59.8944&lon=30.2642&exclude=current,minutely,hourly,alerts&appid=64574a482bc195a2464cfc6541ebe674");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(CONNECTION_TIMEOUT);
            con.setReadTimeout(CONNECTION_TIMEOUT);

        } catch (Exception exception) {exception.printStackTrace();
            System.out.println("Something went wrong while creating URL connection and sending GET request");

        }

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            final StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            String jsonString = content.toString();
            JSONParser jsonParser = new JSONParser();
            JSONObject forecast = (JSONObject) jsonParser.parse(jsonString);

            JSONArray daily = (JSONArray) forecast.get("daily");

            HashMap day, dayTemp;
            double factNightTemp, feelNightTemp, minSpread = 100000.0;
            Instant tempDate = Instant.now();
            Instant sunDate = Instant.now();
            long sunset, sunrise, maxDayLength = -1;
            ZoneId zone = ZoneId.of("Europe/Moscow");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            for (int a = 0; a < 5; a++) {
                day = (HashMap) daily.get(a);
                dayTemp = (HashMap) day.get("temp");
                try { //Бывает целое или дробное, нужно проверить оба варианта
                    factNightTemp = (double) dayTemp.get("night");
                } catch (ClassCastException cl) {
                    Long l = (long) dayTemp.get("night");
                    factNightTemp = l.doubleValue();
                }
                dayTemp = (HashMap) day.get("feels_like");
                try { //Бывает целое или дробное, нужно проверить оба варианта
                    feelNightTemp = (double) dayTemp.get("night");
                } catch (ClassCastException cl) {
                    Long l = (long) dayTemp.get("night");
                    feelNightTemp = l.doubleValue();
                }

                if (Math.abs(factNightTemp - feelNightTemp) < minSpread) {
                    minSpread = Math.abs(factNightTemp - feelNightTemp);
                    tempDate = Instant.ofEpochSecond((long) day.get("dt"));
                }
                sunrise = (long) day.get("sunrise");
                sunset = (long) day.get("sunset");
                if (maxDayLength < (sunset - sunrise)) {
                    maxDayLength = sunset - sunrise;
                    sunDate = Instant.ofEpochSecond((long) day.get("dt"));
                }
            }

            System.out.print("Минимальная разница ощущаемой и фактической температуры ночью в ближайшие 5 дней будет ");
            System.out.println(String.format("%.2f", minSpread) + " градусов Цельсия.");
            System.out.println("Это произойдет " + dtf.format(ZonedDateTime.ofInstant(tempDate, zone)));
            System.out.println("_________________________________________________");
            System.out.print("Самый длинный день из ближайших пяти будет ");
            System.out.println(dtf.format(ZonedDateTime.ofInstant(sunDate, zone)));
            System.out.println("Он составит " + maxDayLength / 3600 + " часов, " +
                    maxDayLength % 3600 / 60 + " минут.");

        } catch (final Exception ex) {
            ex.printStackTrace();
            System.out.println("something went wrong while JSON processing. Try to check API source data. ");
        }
    }
}
