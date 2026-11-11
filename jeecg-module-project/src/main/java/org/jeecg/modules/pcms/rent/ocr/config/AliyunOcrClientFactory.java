package org.jeecg.modules.pcms.rent.ocr.config;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OCR 客户端。
 *
 * Client 是线程安全的，全局建一个复用即可，不要每次请求都 new——
 * 每次 new 都要重新做一遍签名器初始化，白白增加耗时。
 *
 * 这里用懒加载而不是 @Bean，是为了让配置没填时应用照常启动，
 * 只在真正调用识别接口时才报错，不影响其他功能。
 *
 * @Version: V1.0
 */
@Slf4j
@Component
public class AliyunOcrClientFactory {

    @Autowired
    private AliyunOcrProperties properties;

    private volatile Client client;

    public Client getClient() {
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (client != null) {
                return client;
            }
            if (!properties.isEnabled()) {
                throw new JeecgBootException("阿里云OCR未启用，请在配置文件中设置 aliyun.ocr.enabled=true");
            }
            if (isBlank(properties.getAccessKeyId()) || isBlank(properties.getAccessKeySecret())) {
                throw new JeecgBootException("阿里云OCR的 AccessKey 未配置，请设置环境变量 ALIYUN_OCR_AK / ALIYUN_OCR_SK");
            }
            try {
                Config config = new Config()
                        .setAccessKeyId(properties.getAccessKeyId())
                        .setAccessKeySecret(properties.getAccessKeySecret())
                        .setEndpoint(properties.getEndpoint())
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setReadTimeout(properties.getReadTimeout());
                client = new Client(config);
                log.info("阿里云OCR客户端初始化完成，endpoint={}", properties.getEndpoint());
                return client;
            } catch (Exception e) {
                throw new JeecgBootException("阿里云OCR客户端初始化失败：" + e.getMessage());
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
