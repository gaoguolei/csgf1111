package org.jeecg.modules.pcms.util;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import okhttp3.*;
import org.jeecg.modules.pcms.energy.vo.ResultLoginApiVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SolarGetTokenUtils {


    private static String loginUrl;
    private static String username;
    private static String password;
    private static String appKey;
    private static String appSecret;

    @Value("${solar.login}")
    private String loginUrlValue;

    @Value("${solar.username}")
    private String usernameValue;

    @Value("${solar.password}")
    private String passwordValue;

    @Value("${solar.appkey}")
    private String appKeyValue;

    @Value("${solar.appsecret}")
    private String appSecretValue;

    @PostConstruct
    public void init() {
        loginUrl = this.loginUrlValue;
        username = this.usernameValue;
        password = this.passwordValue;
        appKey = this.appKeyValue;
        appSecret = this.appSecretValue;
    }

    /**
     * 阳光云获取登录token
     * @return
     * @throws IOException
     */
    public static String sendLoginSolarRequest() throws IOException {
        // 1. 创建OkHttpClient实例，并配置超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)    // 连接超时
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)      // 写入超时
                .build();

        // 2. 构建请求参数Map
        Map<String, String> params = new HashMap<>();
        params.put("user_account", username);
        params.put("user_password", password);
        params.put("appkey", appKey);

        // 3. 转换为JSON字符串（使用Gson库，需引入依赖）
        Gson gson = new Gson();
        String jsonBody = gson.toJson(params);

        // 4. 创建JSON类型的请求体
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody  // 明确指定JSON类型和编码
        );

        // 5. 构建请求
        Request request = new Request.Builder()
                .url(loginUrl)
                .addHeader("x-access-key", appSecret)  // 添加请求头
                .post(requestBody)                     // 设置JSON请求体
                .build();

        // 6. 执行请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 优化点5：处理非200状态码的情况
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }
            //反解析成java对象并返回token
            ResponseBody body = response.body();
            ResultLoginApiVo resultLoginApiVo = JSONObject.parseObject(body.string(), ResultLoginApiVo.class);
            String token = resultLoginApiVo.getResultData().getToken();
            if(token != null && !token.isEmpty()){
                return token;
            }
            return "";
        }
    }
}
