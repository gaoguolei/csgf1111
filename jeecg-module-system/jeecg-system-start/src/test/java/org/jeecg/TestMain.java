package org.jeecg;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.RestUtil;
import org.jeecg.modules.pcms.project.stationvo.StationListVo;
import org.jeecg.modules.pcms.project.vo.*;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年05月10日 14:02
 */
public class TestMain {

    @Value("${station.deviceList}")
    private String deviceList;

    @Test
    public void test01() {
        System.out.println(deviceList);
    }

    public static void main(String[] args) {
        String url="http://test.growatt.com/v1/plant/list";
        String token="6eb6f069523055a339d71e5b1f6c88cc";
        HashMap<String, Object> paramMap = new HashMap<>();
//        paramMap.put("page", pageVo.getPage());
//        paramMap.put("perpage", pageVo.getPerPage());
//        paramMap.put("search_type", pageVo.getSearchType());
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
//        paramMap.put("search_keyword", pageVo.getSearchKeyword());
        paramMap.put("search_keyword", random);
        String result2 = HttpRequest.get(url)
                .header("token", token)//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        System.err.println(result2);


    }
        // 请求地址
//        String url = "https://api.boot.jeecg.com/sys/user/list";
//        // 请求 Header （用于传递Token）
//        HttpHeaders headers = getHeaders();
//        // 请求方式是 GET 代表获取数据
//        HttpMethod method = HttpMethod.GET;
//
//        System.out.println("请求地址：" + url);
//        System.out.println("请求方式：" + method);
//
//        // 利用 RestUtil 请求该url
//        ResponseEntity<JSONObject> result = RestUtil.request(url, method, headers, null, null, JSONObject.class);
//        if (result != null && result.getBody() != null) {
//            System.out.println("返回结果：" + result.getBody().toJSONString());
//        } else {
//            System.out.println("查询失败");
//        }
    private static HttpHeaders getHeaders() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.50h-g6INOZRVnznExiawFb1U6PPjcVVA4POeYRA5a5Q";
        System.out.println("请求Token：" + token);

        HttpHeaders headers = new HttpHeaders();
        String mediaType = MediaType.APPLICATION_JSON_VALUE;
        headers.setContentType(MediaType.parseMediaType(mediaType));
        headers.set("Accept", mediaType);
        headers.set("X-Access-Token", token);
        return headers;
    }

    @Test
    public void test03(){
        //思路  拿到总数count 拿到每页数量，用总数除以每页数量得到循环次数。
        String url="http://test.growatt.com/v1/plant/list";
        String token="6eb6f069523055a339d71e5b1f6c88cc";
        HashMap<String, Object> paramMap = new HashMap<>();
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
        String result2 = HttpRequest.get(url)
                .header("token", token)//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();

        if(StringUtils.isNotBlank(result2)){
            //封装一个对象用来转换数据
            PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
                if(plantResultVo != null){
                    //拿到总数  在利用总数里面的去计算然后给前端返回
                    Integer count = plantResultVo.getData().getCount();
                    PlantResultVo data = this.getData(1, count);
                    //所有数据在这里面
                    List<StationListVo> plants = data.getData().getPlants();
                    StatisticsVo statisticsVo = new StatisticsVo();
                    //累计发电量
                    BigDecimal te = new BigDecimal(0.000);
                    //累计当前功率
                    BigDecimal cp = new BigDecimal(0.000);
                    //累计当日电量
                    BigDecimal tp = new BigDecimal(0.000);
                    //累计当月电量
                    BigDecimal me = new BigDecimal(0.000);

                    new BigDecimal(0.00);
                    for (int i = 0; i < plants.size(); i++) {
                        Integer plantId = plants.get(i).getPlantId();
                        //累计发电量
                        BigDecimal totalEnergy = new BigDecimal(plants.get(i).getTotalEnergy()).divide(new BigDecimal(1000000));
                        //当前功率
                        BigDecimal currentPower = new BigDecimal(plants.get(i).getCurrentPower()).divide(new BigDecimal(1000));
                        PowerDataVo powerCount = this.getPowerCount(plantId);
                        PowerVo powerVo = powerCount.getData();
                        if(powerVo !=null ){
                            //当日电量
                            BigDecimal todayEnergy = new BigDecimal(powerVo.getTodayEnergy()).divide(new BigDecimal(1000));
                            tp=tp.add(todayEnergy).setScale(3, BigDecimal.ROUND_HALF_UP);
                            //当月电量
                            BigDecimal monthlyEnergy = new BigDecimal(powerVo.getMonthlyEnergy()).divide(new BigDecimal(1000));
                            me=me.add(monthlyEnergy).setScale(3, BigDecimal.ROUND_HALF_UP);
                        }
                        te=te.add(totalEnergy.setScale(3, BigDecimal.ROUND_HALF_UP));
                        cp=cp.add(currentPower.setScale(3, BigDecimal.ROUND_HALF_UP));
                    }

                    statisticsVo.setTotalEfficiency(te.toString());
//                    statisticsVo.setTotalPower(cp.toString());
                    statisticsVo.setTotalTodayPower(tp.toString());
//                    statisticsVo.setTotalMonthPower(me.toString());
                }
            }
        }

    //获取全部电站的数据
    public PlantResultVo getData(Integer page,Integer perpage){
        String url="http://test.growatt.com/v1/plant/list";
        String token="6eb6f069523055a339d71e5b1f6c88cc";
        HashMap<String, Object> paramMap = new HashMap<>();
        int random=(int) ((Math.random() * 10) + 1);
        paramMap.put("search_type", random);
        paramMap.put("page", page);
        paramMap.put("perpage", perpage);
        String result2 = HttpRequest.get(url)
                .header("token", token)//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        System.err.println(result2);
        PlantResultVo plantResultVo = JSON.parseObject(result2, PlantResultVo.class);
        return plantResultVo;

    }


    //获取每个电站的每日发电量
    public PowerDataVo getPowerCount(Integer plantId){
        String url="http://test.growatt.com/v1/plant/data";
        String token="6eb6f069523055a339d71e5b1f6c88cc";
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("plant_id", plantId);
        String result2 = HttpRequest.get(url)
                .header("token", token)//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        System.err.println(result2);
        PowerDataVo plantResultVo = JSON.parseObject(result2, PowerDataVo.class);
        return plantResultVo;

    }

}
