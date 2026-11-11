package org.jeecg.modules.pcms.rent.service;

import java.io.File;
import java.util.List;

import org.jeecg.modules.pcms.rent.dto.RentAttachVO;
import org.jeecg.modules.pcms.rent.entity.TRentAttach;
import org.springframework.web.multipart.MultipartFile;

/**
 * 租金附件。对应需求稿第 7 条（详情页附件）和第 13 条（银行回单入附件）。
 *
 * @Version: V1.0
 */
public interface IRentAttachService {

    /** 某台账的附件列表，按上传时间正序 */
    List<RentAttachVO> listByLedger(Long ledgerId);

    /**
     * 人工上传附件。可传的时机按业务类型区分：
     * ATTACH 业务附件——待提交、已驳回、纠纷暂停期间可传（需求第7条），已支付后也可补传（需求第15条）；
     * AUDIT  审核附件——审核人在审核中驳回时可传（需求第15条）。
     *
     * @param bizType 不传默认按 ATTACH 处理
     */
    RentAttachVO upload(Long ledgerId, MultipartFile file, String bizType);

    /**
     * 存一张银行回单到台账附件里。
     * 对账流程调用，不校验审核状态——回单是既成事实，
     * 哪怕台账已经审核通过，回单也必须留档。
     */
    TRentAttach saveReceipt(Long ledgerId, MultipartFile file);

    /**
     * 存一张银行回单（字节流版）。
     * PDF 拆图后每一页都是字节流，不再是 MultipartFile，所以单开这个入口。
     *
     * @param fileName 展示用的文件名，扩展名决定落盘格式
     */
    TRentAttach saveReceipt(Long ledgerId, byte[] bytes, String fileName);

    /**
     * 把一张已落盘的附件挂到另一条台账上，复用同一个文件。
     *
     * 付款清单是一张图对多条台账（一页几十行、每行归一户），
     * 如果每户都存一份原图，一张清单就能撑出几十 MB。
     * 所以文件只落盘一次，其余台账建一条指向同一个文件的记录。
     *
     * @param origin 已落盘的附件（取其文件路径、文件名、大小）
     */
    TRentAttach linkReceipt(Long ledgerId, TRentAttach origin);

    /** 删除附件，只有普通附件且台账处于可编辑状态时才允许 */
    void delete(Long attachId);

    /** 取附件实体，下载和预览用 */
    TRentAttach getExisting(Long attachId);

    /** 附件在服务器上的真实文件 */
    File resolveFile(TRentAttach attach);
}
