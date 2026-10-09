package com.sunyujie.picture_backend.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 颜色工具类：十六进制颜色解析 + 颜色相似度计算
 * <p>
 * 数据万象返回的主色调 ave 形如 0xRRGGBB，前端取色器通常返回 #RRGGBB，
 * 本类对两种格式（以及不带前缀的 RRGGBB）统一兼容。
 */
public class ColorUtil {

    private ColorUtil() {
    }

    /**
     * 判断颜色字符串是否合法（供业务层做参数校验）
     */
    public static boolean isValidColor(String color) {
        try {
            parseRgb(color);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 把十六进制颜色解析为 RGB 数组
     *
     * @param color 支持 0xRRGGBB / #RRGGBB / RRGGBB
     * @return 下标 0=红 1=绿 2=蓝，取值范围 0~255
     * @throws IllegalArgumentException 颜色为空或格式非法
     */
    public static int[] parseRgb(String color) {
        if (StrUtil.isBlank(color)) {
            throw new IllegalArgumentException("颜色不能为空");
        }
        String hex = color.trim();
        // 统一去掉前缀，只留 6 位十六进制
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        } else if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("颜色格式非法: " + color);
        }
        try {
            int red = Integer.parseInt(hex.substring(0, 2), 16);
            int green = Integer.parseInt(hex.substring(2, 4), 16);
            int blue = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[] {red, green, blue};
        }  catch (NumberFormatException e) {
            throw new IllegalArgumentException("颜色格式非法: " + color, e);
        }
    }

    /**
     * 计算两个颜色的感知距离，值越小越相似，0 表示完全相同
     * <p>
     * 采用加权欧氏距离：人眼对绿色最敏感、蓝色最不敏感，
     * 因此给绿色通道更大的权重，结果比直接 RGB 求差更符合直觉。
     *
     * @param color1 颜色1（支持 0xRRGGBB / #RRGGBB / RRGGBB）
     * @param color2 颜色2（同上）
     * @return 颜色距离
     */
    public static double colorDistance(String color1, String color2) {
        int[] rgb1 = parseRgb(color1);
        int[] rgb2 = parseRgb(color2);
        int r1 = rgb1[0];
        int g1 = rgb1[1];
        int b1 = rgb1[2];
        int r2 = rgb2[0];
        int g2 = rgb2[1];
        int b2 = rgb2[2];
        // 红色均值：颜色偏红时红色差异更容易被察觉，偏暗时蓝色差异更明显
        int redMean = (r1 + r2) / 2;
        int deltaR = r1 - r2;
        int deltaG = g1 - g2;
        int deltaB = b1 - b2;
        double redWeight = 2 + redMean / 256.0;
        double blueWeight = 2 + (255 - redMean) / 256.0;
        return Math.sqrt(redWeight * deltaR * deltaR + 4 * deltaG * deltaG + blueWeight * deltaB * deltaB);
    }
}
