package org.jeecg.modules.pcms.util;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.station.StationSignConfig;
import org.jeecg.modules.pcms.project.entity.PvCallback;
import org.jeecg.modules.pcms.project.entity.PvProjectInfo;
import org.jeecg.modules.pcms.project.service.IPvCallbackService;
import org.jeecg.modules.pcms.project.service.IPvProjectInfoService;
import org.jeecg.modules.pcms.project.signvo.*;
import org.jeecg.modules.pcms.project.stationvo.SignFlowId;
import org.jeecg.modules.pcms.project.stationvo.SignResultVo;
import org.jeecg.modules.pcms.project.stationvo.SignVo;
import org.jeecg.modules.pcms.project.stationvo.UploadVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Author gongzhou
 * @Date 2023/11/19 下午5:01
 * @Version 1.5
 */

@Component
public class FileUploadUtils {

    @Autowired
    private StationSignConfig stationSignConfig;

    @Autowired
    private IPvProjectInfoService iPvProjectInfoService;

    @Autowired
    private IPvCallbackService iPvCallbackService;
    public Result sendUploadFile(String filePath, String fileName, PvProjectInfo projectInfo, SignVo signVo) throws Exception {

        //第一步，上传本地文件到e签宝
        String fileContentMD5 = this.getFileContentMD5(filePath);
        File file = new File(filePath);
        long size = file.length();
        long l = System.currentTimeMillis();

        //设置请求头
        HashMap<String, String> headers = new HashMap<>();
        headers.put("X-Tsign-Open-App-Id", stationSignConfig.getAppId());
        headers.put("Content-Type","application/json; charset=UTF-8");
        headers.put("X-Tsign-Open-Ca-Signature",stationSignConfig.getUploadSign());
        headers.put("Accept","*/*");
        headers.put("X-Tsign-Open-Auth-Mode","Signature");
        headers.put("X-Tsign-Open-Ca-Timestamp",String.valueOf(l));

        //设置请求体
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("contentMd5",fileContentMD5);
        paramMap.put("contentType","application/pdf");
        paramMap.put("convertToPDF",true);
        paramMap.put("fileName",fileName);
        paramMap.put("fileSize",size);
        String string = JSONObject.toJSONString(paramMap);
        String result2 = HttpUtil.createPost(stationSignConfig.getUploadFilePath())
                .addHeaders(headers)
                .body(string)
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        UploadVo plantResultVo = JSON.parseObject(result2, UploadVo.class);
        //第二步，获取到上传成功的状态，以及上传地址和文件id,继续调使用该地址继续做上传
        if(plantResultVo != null ){
            Integer code1 = plantResultVo.getCode();
            if(code1 ==0){
                String fileUploadUrl = plantResultVo.getData().getFileUploadUrl();
                String fileId = plantResultVo.getData().getFileId();
                File file1 = new File(filePath);
                FileInputStream fileInputStream = new FileInputStream(file1);
                byte[] data = new byte[(int) file1.length()];
                fileInputStream.read(data);
                fileInputStream.close();

                // 连接第三方接口
                URL url = new URL(fileUploadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/pdf");
                connection.setRequestProperty("Content-MD5", fileContentMD5);
                connection.setDoOutput(true);

                // 将文件流写入请求体
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(data);
                outputStream.flush();
                outputStream.close();

                // 获取响应
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();

                // 输出响应结果
                JSONObject jsonObject = JSON.parseObject(stringBuilder.toString());
                //第三步，如果最终的状态是上传成功并且返回码是0 那么拿着文件id继续请求e签宝发起文件签署接口
                if(jsonObject !=null){
                    Integer code = (Integer)jsonObject.get("errCode");
                    if(code == 0){
                        //发起文件签署
                        //设置请求头
                        HashMap<String, String> stringHashMap = new HashMap<>();
                        stringHashMap.put("X-Tsign-Open-App-Id", stationSignConfig.getAppId());
                        stringHashMap.put("Content-Type","application/json; charset=UTF-8");
                        stringHashMap.put("X-Tsign-Open-Ca-Signature",stationSignConfig.getSignValue());
                        stringHashMap.put("Accept","*/*");
                        stringHashMap.put("X-Tsign-Open-Auth-Mode","Signature");
                        stringHashMap.put("X-Tsign-Open-Ca-Timestamp",String.valueOf(l));

                        String requestBody = this.getRequestBody(fileId, fileName, projectInfo,signVo);
                        Thread.sleep(10000);
                        //设置请求体
                        String request = HttpUtil.createPost(stationSignConfig.getSignPath())
                                .addHeaders(stringHashMap)
                                .body(requestBody)
                                .timeout(20000)//超时，毫秒
                                .execute()
                                .body();
                        SignResultVo uploadVo = JSON.parseObject(request, SignResultVo.class);
                        Integer code2 = uploadVo.getCode();
                        //发起请求之后
                        if(code2==0){
                            SignFlowId signFlowId = uploadVo.getData();
                            String signFlowId1 = signFlowId.getSignFlowId();
                            //获取到签署流程id存到数据库
                            PvCallback pvCallback = new PvCallback();
                            pvCallback.setProjectId(projectInfo.getId());
                            pvCallback.setSignFlowId(signFlowId1);
                            iPvCallbackService.save(pvCallback);
                            iPvProjectInfoService.updateIsUpload("1",projectInfo.getId());
                        }
                        return Result.ok(uploadVo);
                    }
                }
            }
        }
        return Result.ok();
    }



    public void getAndUpdateSignStatus(String signFlowId,Integer projectId) {
        String HTTPMethod ="GET";
        String Accept ="*/*";
        String ContentMD5 ="";
        String ContentType ="application/json; charset=UTF-8";
        String Date ="";
        String Headers ="";
        String PathAndParameters ="/v3/sign-flow/"+signFlowId+"/detail";
        // 组合拼接待签名字符串
        StringBuffer strBuff = new StringBuffer();
        strBuff.append(HTTPMethod).append("\n").append(Accept).append("\n").append(ContentMD5).append("\n")
                .append(ContentType).append("\n").append(Date).append("\n");
        if ("".equals(Headers)) {
            strBuff.append(Headers).append(PathAndParameters);
        } else {
            strBuff.append(Headers).append("\n").append(PathAndParameters);
        }
        String StringToSign = strBuff.toString();
        long l = System.currentTimeMillis();
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("X-Tsign-Open-App-Id", stationSignConfig.getAppId());
        stringHashMap.put("Content-Type","application/json; charset=UTF-8");
        try {
            stringHashMap.put("X-Tsign-Open-Ca-Signature",EsignEncryption.doSignatureBase64(StringToSign, stationSignConfig.getSecret()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        stringHashMap.put("Accept","*/*");
        stringHashMap.put("X-Tsign-Open-Auth-Mode","Signature");
        stringHashMap.put("X-Tsign-Open-Ca-Timestamp",String.valueOf(l));

        String request = HttpUtil.createGet(stationSignConfig.getSignEnd()+signFlowId+"/detail")
                .addHeaders(stringHashMap)
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        JSONObject jsonObject = JSONObject.parseObject(request);
        Object data = jsonObject.get("data");
        String string = JSONObject.toJSONString(data);
        JSONObject jsonObject1 = JSONObject.parseObject(string);
        Integer signFlowStatus = (Integer)jsonObject1.get("signFlowStatus");
        //如果状态为签署完成的状态，那就更新数据库
        if(signFlowStatus == 2){
            iPvProjectInfoService.updateIsUpload("2",projectId);
        }
    }

    public String signFileUpload(String signFlowId) throws Exception {
        //首先需要计算签名值，

        String HTTPMethod ="GET";
        String Accept ="*/*";
        String ContentMD5 ="";
        String ContentType ="application/json; charset=UTF-8";
        String Date ="";
        String Headers ="";
        String PathAndParameters ="/v3/sign-flow/"+signFlowId+"/file-download-url";
        // 组合拼接待签名字符串
        StringBuffer strBuff = new StringBuffer();
        strBuff.append(HTTPMethod).append("\n").append(Accept).append("\n").append(ContentMD5).append("\n")
                .append(ContentType).append("\n").append(Date).append("\n");
        if ("".equals(Headers)) {
            strBuff.append(Headers).append(PathAndParameters);
        } else {
            strBuff.append(Headers).append("\n").append(PathAndParameters);
        }

        String StringToSign = strBuff.toString();
        long l = System.currentTimeMillis();
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("X-Tsign-Open-App-Id", stationSignConfig.getAppId());
        stringHashMap.put("Content-Type","application/json; charset=UTF-8");
        stringHashMap.put("X-Tsign-Open-Ca-Signature",EsignEncryption.doSignatureBase64(StringToSign, stationSignConfig.getSecret()));
        stringHashMap.put("Accept","*/*");
        stringHashMap.put("X-Tsign-Open-Auth-Mode","Signature");
        stringHashMap.put("X-Tsign-Open-Ca-Timestamp",String.valueOf(l));

        String request = HttpUtil.createGet(stationSignConfig.getSignFileUpload()+"/v3/sign-flow/"+signFlowId+"/file-download-url")
                .addHeaders(stringHashMap)
                .timeout(20000)//超时，毫秒
                .execute()
                .body();
        return request;

    }

    /**
     *
     * 发起签署的请求体
     */

    public String getRequestBody(String fileId,String fileName, PvProjectInfo projectInfo,SignVo signVo){
        //封装
        RequestVo requestVo = new RequestVo();
        ArrayList<DocVo> docVos = new ArrayList<>();
        DocVo docVo = new DocVo();
        docVo.setFileId(fileId);
        docVo.setFileName(projectInfo.getProjectCode()+".pdf");
        docVos.add(docVo);
        requestVo.setDocs(docVos);
        SignFlowConfig signFlowConfig = new SignFlowConfig();
        AuthConfig authConfig = new AuthConfig();
        authConfig.setPsnAvailableAuthModes(new String[]{"PSN_FACE"});
        authConfig.setWillingnessAuthModes(new String[]{"PSN_FACE_TECENT","PSN_FACE_ALIPAY"});
        authConfig.setOrgAvailableAuthModes(new String[]{"ORG_BANK_TRANSFER","ORG_LEGALREP"});
        signFlowConfig.setAuthConfig(authConfig);
        ChargeConfig chargeConfig = new ChargeConfig();
        chargeConfig.setChargeMode(0);
        signFlowConfig.setChargeConfig(chargeConfig);
//        RedirectConfig redirectConfig = new RedirectConfig();
//        redirectConfig.setRedirectDelayTime("3");
//        redirectConfig.setRedirectUrl("");
//        signFlowConfig.setRedirectConfig(redirectConfig);
        SignConfig signConfig = new SignConfig();
        signConfig.setAvailableSignClientTypes("1");
        signConfig.setShowBatchDropSealButton(true);
        signFlowConfig.setSignConfig(signConfig);
        signFlowConfig.setNotifyUrl(stationSignConfig.getCallbackPath());
        System.err.println("stationSignConfig.getCallbackPath()-------------------------------------------------"+stationSignConfig.getCallbackPath());
        signFlowConfig.setAutoFinish(true);
        signFlowConfig.setAutoStart(true);
        signFlowConfig.setIdentityVerify(true);
        signFlowConfig.setSignFlowTitle("合同签署");
        requestVo.setSignFlowConfig(signFlowConfig);

        ArrayList<Signer> signers = new ArrayList<>();
        //个人签署
        Signer signer = new Signer();
        SignConfigOfSign signConfigOfSign = new SignConfigOfSign();
        signConfigOfSign.setSignOrder(1);
        signConfigOfSign.setForcedReadingTime("10");
        signer.setSignConfig(signConfigOfSign);
        NoticeConfig noticeConfig = new NoticeConfig();
        noticeConfig.setNoticeTypes("1");
        signer.setNoticeConfig(noticeConfig);
        signer.setSignerType(0);
        PsnSignerInfo psnSignerInfo = new PsnSignerInfo();
        String customerMobile = signVo.getPhone();
        String idNumber = signVo.getIdnum();
        String customerName = signVo.getArtyA();
        psnSignerInfo.setPsnAccount(customerMobile);
        PsnInfo psnInfo = new PsnInfo();
        psnInfo.setPsnIDCardType("CRED_PSN_CH_IDCARD");
        psnInfo.setPsnIDCardNum(idNumber);
        psnInfo.setPsnName(customerName);
        psnSignerInfo.setPsnInfo(psnInfo);
        signer.setPsnSignerInfo(psnSignerInfo);

        ArrayList<SignField> signFields = new ArrayList<>();
        SignField signField = new SignField();
        NormalSignFieldConfig normalSignFieldConfig = new NormalSignFieldConfig();
        normalSignFieldConfig.setAutoSign(false);
        normalSignFieldConfig.setFreeMode(false);
        normalSignFieldConfig.setMovableSignField(false);
        normalSignFieldConfig.setPsnSealStyles("0,1");
        normalSignFieldConfig.setSignFieldSize("96");
        normalSignFieldConfig.setSignFieldStyle(1);
        SignFieldPosition signFieldPosition = new SignFieldPosition();
        signFieldPosition.setPositionPage("10");
        signFieldPosition.setPositionX(130);
        signFieldPosition.setPositionY(540);
        normalSignFieldConfig.setSignFieldPosition(signFieldPosition);
        signField.setNormalSignFieldConfig(normalSignFieldConfig);
        signField.setFileId(fileId);
        signField.setSignFieldType(0);
        signField.setCustomBizNum(projectInfo.getId().toString());
        signFields.add(signField);
        signer.setSignFields(signFields);
        signers.add(signer);
        //企业签署
        Signer signer1 = new Signer();
        SignConfigOfSign signConfigOfSign1 = new SignConfigOfSign();
        signConfigOfSign1.setSignOrder(2);
        signConfigOfSign1.setForcedReadingTime("10");
        signer1.setSignConfig(signConfigOfSign1);
        NoticeConfig noticeConfig1 = new NoticeConfig();
        noticeConfig1.setNoticeTypes("1,2");
        signer1.setNoticeConfig(noticeConfig1);
        signer1.setSignerType(1);
        OrgSignerInfo orgSignerInfo = new OrgSignerInfo();
        orgSignerInfo.setOrgId(stationSignConfig.getOrgId());
        signer1.setOrgSignerInfo(orgSignerInfo);

        ArrayList<SignField> signFields1 = new ArrayList<>();
        SignField signField1 = new SignField();
        signField1.setCustomBizNum(projectInfo.getId().toString());
        signField1.setFileId(fileId);
        signField1.setSignFieldType(0);
        NormalSignFieldConfig normalSignFieldConfig1 = new NormalSignFieldConfig();
        normalSignFieldConfig1.setAutoSign(true);
        normalSignFieldConfig1.setFreeMode(false);
        normalSignFieldConfig1.setMovableSignField(false);
        normalSignFieldConfig1.setPsnSealStyles("1");
        normalSignFieldConfig1.setSignFieldStyle(2);
        normalSignFieldConfig1.setSignFieldSize("159");
        SignFieldPosition signFieldPosition1 = new SignFieldPosition();
        signFieldPosition1.setPositionPage("3");
        signFieldPosition1.setPositionX(340);
        signFieldPosition1.setPositionY(400);
        normalSignFieldConfig1.setSignFieldPosition(signFieldPosition1);
        signField1.setNormalSignFieldConfig(normalSignFieldConfig1);
        signFields1.add(signField1);

        SignField signField2 = new SignField();
        signField2.setCustomBizNum(projectInfo.getId().toString());
        signField2.setFileId(fileId);
        signField2.setSignFieldType(0);
        NormalSignFieldConfig normalSignFieldConfig2 = new NormalSignFieldConfig();
        normalSignFieldConfig2.setAutoSign(true);
        normalSignFieldConfig2.setFreeMode(false);
        normalSignFieldConfig2.setMovableSignField(false);
        normalSignFieldConfig2.setPsnSealStyles("1");
        normalSignFieldConfig2.setSignFieldSize("159");
        normalSignFieldConfig2.setSignFieldStyle(1);
        SignFieldPosition signFieldPosition2 = new SignFieldPosition();
        signFieldPosition2.setPositionPage("10");
        signFieldPosition2.setPositionX(380);
        signFieldPosition2.setPositionY(550);
        normalSignFieldConfig2.setSignFieldPosition(signFieldPosition2);
        signField2.setNormalSignFieldConfig(normalSignFieldConfig2);
        signFields1.add(signField2);

        SignField signField3 = new SignField();
        signField3.setCustomBizNum(projectInfo.getId().toString());
        signField3.setFileId(fileId);
        signField3.setSignFieldType(0);
        NormalSignFieldConfig normalSignFieldConfig3 = new NormalSignFieldConfig();
        normalSignFieldConfig3.setAutoSign(true);
        normalSignFieldConfig3.setFreeMode(false);
        normalSignFieldConfig3.setMovableSignField(false);
        normalSignFieldConfig3.setPsnSealStyles("1");
        normalSignFieldConfig3.setSignFieldSize("159");
        normalSignFieldConfig3.setSignFieldStyle(1);
        SignFieldPosition signFieldPosition3 = new SignFieldPosition();
        signFieldPosition3.setPositionPage("1");
        signFieldPosition3.setPositionX(300);
        signFieldPosition3.setPositionY(400);
        normalSignFieldConfig3.setSignFieldPosition(signFieldPosition3);
        signField3.setNormalSignFieldConfig(normalSignFieldConfig3);
        signFields1.add(signField3);

        signer1.setSignFields(signFields1);;
        signers.add(signer1);
        requestVo.setSigners(signers);
        String string = JSONObject.toJSONString(requestVo);
        return string;
    }

    /**
     * 获取文件md5 值
     * @param filePath
     * @return
     */
    public  String getFileContentMD5(String filePath) {
        // 获取文件MD5的二进制数组（128位）
        byte[] bytes = getFileMD5Bytes128(filePath);
        // 对文件MD5的二进制数组进行base64编码
        return new String(Base64.encodeBase64(bytes));
    }
    /***
     * 获取文件MD5-二进制数组（128位）
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] getFileMD5Bytes128(String filePath) {
        FileInputStream fis = null;
        byte[] md5Bytes = null;
        try {
            File file = new File(filePath);
            fis = new FileInputStream(file);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                md5.update(buffer, 0, length);
            }
            md5Bytes = md5.digest();
            fis.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return md5Bytes;
    }



    //计算签名
    public String doSignatureBase64(String message, String secret) throws Exception {
        String algorithm = "HmacSHA256";
        Mac hmacSha256;
        String digestBase64 = null;
        try {
            hmacSha256 = Mac.getInstance(algorithm);
            byte[] keyBytes = secret.getBytes("UTF-8");
            byte[] messageBytes = message.getBytes("UTF-8");
            hmacSha256.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, algorithm));
            // 使用HmacSHA256对二进制数据消息Bytes计算摘要
            byte[] digestBytes = hmacSha256.doFinal(messageBytes);
            // 把摘要后的结果digestBytes使用Base64进行编码
            digestBase64 = new String(Base64.encodeBase64(digestBytes), "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            String msg = MessageFormat.format("不支持此算法: {0}", e.getMessage());
            Exception ex = new Exception(msg);
            ex.initCause(e);
            throw ex;
        } catch (UnsupportedEncodingException e) {
            String msg = MessageFormat.format("不支持的字符编码: {0}", e.getMessage());
            Exception ex = new Exception(msg);
            ex.initCause(e);
            throw ex;
        } catch (InvalidKeyException e) {
            String msg = MessageFormat.format("无效的密钥规范: {0}", e.getMessage());
            Exception ex = new Exception(msg);
            ex.initCause(e);
            throw ex;
        }
        return digestBase64;
    }

}
