package org.jeecg.common.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 *  图片处理工具
 * @author jun
 */
public class ImageUtil {

    /**
     * 判断文件是否为图片
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isImage(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);
        return bufferedImage != null;
    }
}
