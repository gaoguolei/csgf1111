package org.jeecg.modules.pcms.rent.ocr.service;

import java.util.List;

import org.jeecg.modules.pcms.rent.ocr.dto.PaymentListItemVO;
import org.jeecg.modules.pcms.rent.ocr.dto.PaymentReceiptVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云单据识别。
 *
 * @Version: V1.0
 */
public interface IOcrService {

    /**
     * 识别银行回单/付款回单。
     * 只返回结果不落库，由前端回填表单、人工核对后再保存。
     *
     * @param file 回单图片或PDF，不超过10MB
     */
    PaymentReceiptVO recognizePaymentReceipt(MultipartFile file);

    /**
     * 按子图拆分识别：一张图里有多份单据时返回多条结果。
     *
     * 供"PDF 拆图后逐页识别"的批量对账使用——一页并排两张回单，
     * 阿里云会切成两个子图，这里就返回两条，每条的字段只属于自己那份，
     * 不会像 recognizePaymentReceipt 那样合并成一份、分不清谁是谁。
     *
     * 不落库、不匹配农户，纯识别结果。
     *
     * @param imageBytes 图片二进制
     * @return 每份单据一条；一张都没认出来时返回一条空结果（便于调用方提示）
     */
    List<PaymentReceiptVO> recognizeReceipts(byte[] imageBytes);

    /**
     * 按表格识别，把付款清单拆成一行一笔。
     *
     * 客户发来的"付款明细"是批量代发清单（序号|收款人|收款账号|代发金额|…），
     * 一张表几十笔。这种版式没有回单的那些特征（没有收款方户名栏、没有金额大写、
     * 没有回单编号），按 PaymentRecord 识别要么被阿里云以"图片类型不符"拒掉，
     * 要么只抽出零星字段，所以改用 Table 模式按行列取。
     *
     * 列是按表头文字定位的（找含"收款账号"的列），不靠固定列序——
     * 客户换个模板多一列少一列也能认。
     *
     * @param imageBytes 图片二进制
     * @return 每一行一笔；认不出表头或没有任何有效行时返回空列表
     */
    List<PaymentListItemVO> recognizePaymentList(byte[] imageBytes);

    /**
     * 通用识别，只返回整页文字。
     *
     * 给"按标签抠字段"的兜底用。各家银行的回单版式差别很大——
     * 宁波银行是表格版、海南银行是"标签：值"的记账回执版，
     * 阿里云的 PaymentRecord 只认常见版式，像海南银行这种就抽不出字段。
     * 通用识别会把整页文字都给出来，我们按"收款人账号："这种标签自己抠，
     * 反而比依赖它的模型更稳。
     *
     * @param imageBytes 图片二进制
     * @return 整页文字，识别不出时返回 null
     */
    String recognizeText(byte[] imageBytes);

    /**
     * 通用识别，Type 自己指定。
     * 调试新单据类型时用它先看看阿里云能识别出哪些字段，再决定要不要写映射。
     *
     * @param type 阿里云的 Type 取值，如 Advanced / IdCard / BankCard / Invoice
     */
    PaymentReceiptVO recognize(MultipartFile file, String type);
}
