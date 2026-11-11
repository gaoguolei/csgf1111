package org.jeecg.modules.pcms.rent.ocr.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentReceiptVO;
import org.jeecg.modules.pcms.rent.ocr.service.IOcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * 租金支付管理 - 阿里云单据识别。
 *
 * 识别结果只返回不落库，由前端回填表单、人工核对后再保存。
 * OCR 有识别错的可能，回单涉及金额和收款人，不做人工确认风险太大。
 *
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "租金支付-单据识别")
@RestController
@RequestMapping("/rent/ocr")
public class OcrController {

    /** multipart 传法里的部件名，前端 FormData.append 的第一个参数 */
    private static final String FILE_PART = "file";

    /** 裸传二进制时从 Content-Disposition 里抠文件名，filename="x.png" 和 filename*=UTF-8''x.png 都认 */
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?");

    @Autowired
    private IOcrService ocrService;

    @ApiOperation(value = "识别银行回单",
            notes = "Type=PaymentRecord。返回付款人、收款人、金额、日期、流水号等字段，"
                    + "同时按收款人账号和姓名匹配出候选农户；"
                    + "rawFields 里是阿里云识别出的全部键值对，没归类上的字段也能看到。"
                    + "支持两种传法：multipart/form-data 的 file 部件，或把图片二进制直接作为请求体")
    @PostMapping("/paymentReceipt")
    public Result<PaymentReceiptVO> paymentReceipt(HttpServletRequest request) {
        return Result.OK(ocrService.recognizePaymentReceipt(resolveFile(request)));
    }

    @ApiOperation(value = "通用识别（调试用）",
            notes = "自己指定 Type，用来试新单据类型能识别出哪些字段。"
                    + "常用取值：Advanced 通用 / IdCard 身份证 / BankCard 银行卡 / "
                    + "Invoice 增值税发票 / MixedInvoice 混贴票据 / Table 表格")
    @PostMapping("/recognize")
    public Result<PaymentReceiptVO> recognize(HttpServletRequest request,
                                              @RequestParam(name = "type", defaultValue = "Advanced") String type) {
        return Result.OK(ocrService.recognize(resolveFile(request), type));
    }

    /**
     * 取要识别的文件，容忍前端两种传法：
     * 1. 标准 multipart/form-data，部件名 file；
     * 2. 图片二进制直接当请求体（Content-Type 是 image/png、application/pdf 这类，不是 multipart）——
     *    原来 @RequestParam("file") MultipartFile 的签名遇到这种传法会直接抛
     *    "Current request is not a multipart request"，请求进不了方法体。
     */
    private MultipartFile resolveFile(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipart = (MultipartHttpServletRequest) request;
            MultipartFile file = multipart.getFile(FILE_PART);
            if (file == null || file.isEmpty()) {
                // 部件名不叫 file 时退一步：取第一个非空文件部件
                file = multipart.getFileMap().values().stream()
                        .filter(f -> !f.isEmpty()).findFirst().orElse(null);
            }
            if (file == null) {
                throw new JeecgBootException("请选择要识别的图片");
            }
            return file;
        }
        byte[] body;
        try {
            body = StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new JeecgBootException("读取上传文件失败：" + e.getMessage());
        }
        if (body.length == 0) {
            throw new JeecgBootException("请选择要识别的图片");
        }
        String contentType = request.getContentType();
        return new RawBodyMultipartFile(FILE_PART, resolveFilename(request, contentType), contentType, body);
    }

    /**
     * 裸传二进制时没有表单部件名，文件名从 Content-Disposition 取；
     * 取不到就按 Content-Type 推一个扩展名，只用于日志和回显，不影响识别。
     */
    private String resolveFilename(HttpServletRequest request, String contentType) {
        String disposition = request.getHeader("Content-Disposition");
        if (disposition != null) {
            Matcher m = FILENAME_PATTERN.matcher(disposition);
            if (m.find()) {
                String name = m.group(1).trim();
                try {
                    return URLDecoder.decode(name, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return name;
                }
            }
        }
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.contains("pdf")) {
            return "receipt.pdf";
        }
        if (ct.contains("png")) {
            return "receipt.png";
        }
        if (ct.contains("jpeg") || ct.contains("jpg")) {
            return "receipt.jpg";
        }
        if (ct.contains("bmp")) {
            return "receipt.bmp";
        }
        if (ct.contains("webp")) {
            return "receipt.webp";
        }
        return "receipt.bin";
    }

    /**
     * 把裸请求体包成 MultipartFile，让 service 层不用关心文件是怎么传上来的。
     * service 只用到 getInputStream/getSize/isEmpty/getOriginalFilename，这里都实现了。
     */
    private static class RawBodyMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] body;

        RawBodyMultipartFile(String name, String originalFilename, String contentType, byte[] body) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.body = body;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return body.length == 0;
        }

        @Override
        public long getSize() {
            return body.length;
        }

        @Override
        public byte[] getBytes() {
            return body.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            FileCopyUtils.copy(body, dest);
        }
    }
}
