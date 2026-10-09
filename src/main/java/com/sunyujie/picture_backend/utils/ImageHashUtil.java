package com.sunyujie.picture_backend.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 图片感知哈希（pHash）工具类
 * <p>
 * 把图片压缩成 64 位指纹：缩放 32x32 → 灰度 → DCT → 取左上 8x8 低频 → 与均值比较出 64 个 bit。
 * 两张图的"相似度" = 64 - 汉明距离，距离越小越相似（0 表示指纹完全相同）。
 * 检索时只需对库内所有指纹做一次异或 + bitCount，纳秒级，无需向量数据库。
 */
public class ImageHashUtil {

    private static final int SIZE = 32;
    private static final int REDUCED_SIZE = 8;

    /**
     * 计算图片的 64 位感知哈希指纹
     *
     * @param file 图片文件（依赖 ImageIO 解码，webp 需 imageio-webp 插件）
     * @return 64 位指纹
     */
    public static long pHash(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("无法解码图片: " + file.getAbsolutePath());
        }
        // 1. 缩放到 32x32 并转灰度
        double[][] gray = new double[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int srcX = x * image.getWidth() / SIZE;
                int srcY = y * image.getHeight() / SIZE;
                int rgb = image.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[x][y] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        // 2. DCT 变换，取左上角 8x8 低频系数
        double[][] dct = applyDCT(gray);
        // 3. 计算均值（排除 [0][0] 直流分量，它只反映整体亮度）
        double sum = 0;
        for (int x = 0; x < REDUCED_SIZE; x++) {
            for (int y = 0; y < REDUCED_SIZE; y++) {
                sum += dct[x][y];
            }
        }
        double avg = (sum - dct[0][0]) / (REDUCED_SIZE * REDUCED_SIZE - 1.0);
        // 4. 大于均值记 1，否则记 0，拼成 64 位 long
        long hash = 0;
        for (int x = 0; x < REDUCED_SIZE; x++) {
            for (int y = 0; y < REDUCED_SIZE; y++) {
                hash <<= 1;
                if (dct[x][y] > avg) {
                    hash |= 1;
                }
            }
        }
        return hash;
    }

    /**
     * 汉明距离：两个指纹有多少个 bit 不同，越小越相似
     */
    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }

    private static double[][] applyDCT(double[][] f) {
        double[][] result = new double[SIZE][SIZE];
        double cu, cv;
        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                double sum = 0;
                for (int x = 0; x < SIZE; x++) {
                    for (int y = 0; y < SIZE; y++) {
                        sum += f[x][y]
                                * Math.cos((2 * x + 1) * u * Math.PI / (2.0 * SIZE))
                                * Math.cos((2 * y + 1) * v * Math.PI / (2.0 * SIZE));
                    }
                }
                cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                result[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return result;
    }
}
