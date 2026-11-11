package org.jeecg.modules.pcms.rent.ocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云文字识别配置。
 *
 * AccessKey 千万不要写死在 yml 里提交到 git，用环境变量注入：
 *   aliyun.ocr.access-key-id: ${ALIYUN_OCR_AK:}
 *   aliyun.ocr.access-key-secret: ${ALIYUN_OCR_SK:}
 * 并且建议在阿里云控制台建一个 RAM 子账号，只授予 OCR 的调用权限，
 * 不要用主账号 AccessKey。
 *
 * @Version: V1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.ocr")
public class AliyunOcrProperties {

    /** 总开关，关掉后调用识别接口会直接报错提示未开启 */
    private boolean enabled = true;

    private String accessKeyId;

    private String accessKeySecret;

    /** 服务接入点，华东1杭州。其他地域见阿里云文档 */
    private String endpoint = "ocr-api.cn-hangzhou.aliyuncs.com";

    private Integer connectTimeout = 5000;

    private Integer readTimeout = 30000;

    /** 单张图片大小上限，阿里云限制 10MB */
    private long maxFileSize = 10 * 1024 * 1024L;
}
