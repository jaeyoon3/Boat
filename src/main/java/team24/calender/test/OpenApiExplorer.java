package team24.calender.test;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OpenApiExplorer {
    private static String secretKey = "uJtLAuqx2Kg%2BopUG6hJN5U7F6uNwEhAcxfS%2BXA2hZXcDsEEgOBs997nuYT%2B4BTYo5OqlCuEg9YyUvdUpMXUv6Q%3D%3D";

    public JSONObject getHolidayExplorer(String year, String month) throws IOException, JSONException {
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getHoliDeInfo");
        urlBuilder.append("?").append(URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)).append("=").append(secretKey); /*Service Key*/
        urlBuilder.append("&").append(URLEncoder.encode("solYear", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(year, StandardCharsets.UTF_8)); /*연 */
        urlBuilder.append("&").append(URLEncoder.encode("solMonth", StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(month.length() == 1 ? "0" + month : month, StandardCharsets.UTF_8)); /*월*/

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        System.out.println("Response code: " + conn.getResponseCode());
        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder xmlSb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            xmlSb.append(line);
        }
        rd.close();
        conn.disconnect();
        JSONObject jsonSb = XML.toJSONObject(xmlSb.toString());
        return jsonSb;
    }
}
