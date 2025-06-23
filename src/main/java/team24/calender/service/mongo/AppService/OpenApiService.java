package team24.calender.service.mongo.AppService;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team24.calender.domain.Holiday.Holiday;
import team24.calender.repository.AppRepo.HolidayRepository;
import team24.calender.test.OpenApiExplorer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class OpenApiService {
    private final OpenApiExplorer openApiExplorer;
    private final HolidayRepository holidayRepository;

    private final MongoTemplate mongoTemplate;
    @Transactional
    public void getHolidayService() throws IOException, JSONException { // 공휴일 가져오기
        // 올해
        String solYear = String.valueOf(LocalDate.now().getYear());

        for (int solMonth = 1; solMonth <= 12; solMonth++) {
            // 숫자 2자리 맞춤
            String strMonth = String.valueOf(solMonth);
            while (strMonth.length() < 2) {
                strMonth = "0" + strMonth;
            }

            JSONObject jsonData = openApiExplorer.getHolidayExplorer(solYear, strMonth);
            JSONObject body = jsonData.getJSONObject("response").getJSONObject("body");

            if (body.getInt("totalCount") != 0) {
                // 공휴일 값이 하나일 때는 item이 jsonObject로 받아지기 때문에 조건문 사용
                if (body.getInt("totalCount") == 1) {
                    JSONObject item = body.getJSONObject("items").getJSONObject("item");
                    if (item.getString("isHoliday").equals("Y")) { // 공휴일이 맞을 경우
                        String holidayDate = String.valueOf(item.getInt("locdate"));
                        System.out.println(holidayDate);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                        LocalDate date = LocalDate.parse(holidayDate, formatter);
                        if (!holidayRepository.existsByHolidayDate(holidayDate)) { // 중복 방지
                            String holidayName = item.getString("dateName");
                            Holiday holiday = new Holiday(date, holidayName);
                            holidayRepository.save(holiday);
                        }
                    }
                } else {
                    JSONArray items = body.getJSONObject("items").getJSONArray("item");
                    for (int i = 0; i < items.length(); i++) { // 해당 월 공휴일 갯수
                        JSONObject item = items.getJSONObject(i);
                        JSONObject map = new JSONObject(new Gson().toJson(item)).getJSONObject("map");
                        if (map.getString("isHoliday").equals("Y")) { // 공휴일이 맞을 경우
                            String holidayDate = String.valueOf(item.getInt("locdate"));
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                            LocalDate date = LocalDate.parse(holidayDate, formatter);
                            if (!holidayRepository.existsByHolidayDate(holidayDate)) { // 중복 방지
                                String holidayName = map.getString("dateName");
                                Holiday holiday = new Holiday(date, holidayName);
                                holidayRepository.save(holiday);
                            }
                        }
                    }
                }
            }
            if (solYear.equals(String.valueOf(LocalDate.now().getYear())) && solMonth == 12) {
                // 내년까지 저장
                solYear = String.valueOf(LocalDate.now().plusYears(1).getYear());
                solMonth = 1;
            }
        }

    }
    public List<Holiday> holidayGet(){
        return mongoTemplate.findAll(Holiday.class);
    }
}
