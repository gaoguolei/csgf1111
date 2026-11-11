package org.jeecg.config.station;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author gongzhou
 * @Date 2023/11/19 下午4:56
 * @Version 1.5
 */
@Configuration
public class StationSignConfig {


    @Value("${sign.modelPath}")
    private String modelPath;

    @Value("${sign.uploadFilePath}")
    private String uploadFilePath;

    @Value("${sign.AppId}")
    private String AppId;
    @Value("${sign.orgId}")
    private String orgId;

    @Value("${sign.uploadSign}")
    private String uploadSign;
    @Value("${sign.signPath}")
    private String signPath;

    @Value("${sign.signValue}")
    private String signValue;

    @Value("${sign.callbackPath}")
    private String callbackPath;
    @Value("${sign.signFileUpload}")
    private String signFileUpload;


    @Value("${sign.signEnd}")
    private String signEnd;
    @Value("${sign.secret}")
    private String secret;

    @Value("${sign.outPath}")
    private String outPath;

    public String getOutPath() {
        return outPath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public String getUploadFilePath() {
        return uploadFilePath;
    }

    public String getAppId() {
        return AppId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getUploadSign() {
        return uploadSign;
    }

    public String getSignPath() {
        return signPath;
    }

    public String getSignValue() {
        return signValue;
    }

    public String getCallbackPath() {
        return callbackPath;
    }

    public String getSignFileUpload() {
        return signFileUpload;
    }

    public String getSecret() {
        return secret;
    }

    public String getSignEnd() {
        return signEnd;
    }
}
