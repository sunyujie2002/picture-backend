package com.sunyujie.picture_backend.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片主色调提取（自研直方图算法，取代数据万象 imageAve 的「平均色」）。
 * <p>
 * 为什么不用 CI 的 Ave：实测它返回的是失真的暗色——明亮的海边蓝天图被算成 #365461 暗灰蓝、
 * 粉色人物图被算成 #9E6939 土棕色，比真实观感暗一大截，做「按颜色搜图」时基本查不到东西。
 * <p>
 * 算法（确定性、无随机、与 tools/clip_batch_embed.py 的 --color 模式逐位对齐，改动必须两边同步）：
 * 1. 线性按固定步长采样像素（大图最多采 25 万个）；
 * 2. 剔除过暗（亮度 ≤28）和过亮（≥236）的像素，避免黑底/白底吞掉主体色；
 *    若可用像素不足采样的 8%（整图近乎纯黑/纯白），退回用全部采样像素；
 * 3. RGB 各取高 4 位量化成 4096 个色桶，取像素数最多的桶；
 * 4. 主色调 = 该桶内像素的均值，按 CI 同款格式 0xrrggbb（小写）返回。
 * <p>
 * 局限：依赖 ImageIO，webp 等不支持的格式返回 null，调用方需回退 CI 的 Ave。
 */
public final class PictureColorUtil {

    private PictureColorUtil() {
    }

    /** 亮度下限：低于该值的像素视为阴影/纯黑背景，不参与主色统计 */
    private static final double LUMA_MIN = 28.0;
    /** 亮度上限：高于该值的像素视为纯白背景，不参与主色统计 */
    private static final double LUMA_MAX = 236.0;
    /** 采样像素上限：超过则按步长跳采，控制耗时与内存 */
    private static final long MAX_SAMPLE = 250_000L;
    /** 直方图桶数：RGB 各取高 4 位 → 16*16*16 = 4096 桶 */
    private static final int BINS = 4096;
    /** 可用像素占比低于该值时，放弃亮度过滤、改用全部采样像素 */
    private static final double MIN_KEEP_RATIO = 0.08;

    /**
     * 提取图片主色调。
     *
     * @param imageFile 本地图片文件（jpg / png / gif 首帧等 ImageIO 支持的格式）
     * @return 0xrrggbb（小写，与 CI imageAve 的存储格式一致）；解析失败返回 null
     */
    public static String dominantColor(File imageFile) {
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            return null;
        }
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        long total = (long) width * height;
        int stride = (int) Math.max(1, total / MAX_SAMPLE);

        long[] binCount = new long[BINS];
        long[][] binSum = new long[BINS][3];
        long sampled = 0;
        long kept = 0;
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                long index = (long) y * width + x;
                if (index % stride != 0) {
                    continue;
                }
                sampled++;
                int argb = row[x];
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                double luma = 0.299 * r + 0.587 * g + 0.114 * b;
                if (luma <= LUMA_MIN || luma >= LUMA_MAX) {
                    continue;
                }
                kept++;
                accumulate(binCount, binSum, r, g, b);
            }
        }
        // 近乎纯黑/纯白的图：亮度过滤后没剩几个像素，退回全部采样
        if (kept < sampled * MIN_KEEP_RATIO) {
            binCount = new long[BINS];
            binSum = new long[BINS][3];
            kept = 0;
            for (int y = 0; y < height; y++) {
                image.getRGB(0, y, width, 1, row, 0, width);
                for (int x = 0; x < width; x++) {
                    long index = (long) y * width + x;
                    if (index % stride != 0) {
                        continue;
                    }
                    int argb = row[x];
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    kept++;
                    accumulate(binCount, binSum, r, g, b);
                }
            }
        }
        if (kept == 0) {
            return null;
        }
        int best = 0;
        for (int i = 1; i < BINS; i++) {
            if (binCount[i] > binCount[best]) {
                best = i;
            }
        }
        if (binCount[best] == 0) {
            return null;
        }
        int r = (int) Math.round((double) binSum[best][0] / binCount[best]);
        int g = (int) Math.round((double) binSum[best][1] / binCount[best]);
        int b = (int) Math.round((double) binSum[best][2] / binCount[best]);
        return String.format("0x%02x%02x%02x", r, g, b);
    }

    private static void accumulate(long[] binCount, long[][] binSum, int r, int g, int b) {
        int bin = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
        binCount[bin]++;
        binSum[bin][0] += r;
        binSum[bin][1] += g;
        binSum[bin][2] += b;
    }
}
