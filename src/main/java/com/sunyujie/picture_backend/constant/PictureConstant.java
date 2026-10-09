package com.sunyujie.picture_backend.constant;

import java.util.Arrays;
import java.util.List;

public interface PictureConstant {

    /**
     * 私有目录前缀（用户 id 由调用方拼接，常量在类加载时初始化一次，不能放动态值）
     * 注意：COS key 是相对路径，不能以 / 开头，否则会产生桶根空目录，
     * 且数据万象产物的 key 在各服务间归一化不一致，导致复制时 NoSuchKey
     */
    String PRIVATE_IMAGE_DIR_PREFIX = "private/image/";

    /**
     * 公开图片目录前缀
     */
    String PUBLIC_IMAGE_DIR_PREFIX = "public/image/";

    /**
     * 图片临时目录前缀
     */
    String TEMP_IMAGE_DIR_PREFIX = "temp/image/";

    /**
     * 头像正式目录前缀（头像需要公开展示，独立于公共图库）
     */
    String AVATAR_IMAGE_DIR_PREFIX = "avatar/image/";

    /**
     * 最大文件大小
     */
    long MAX_FILE_SIZE = 1024 * 1024 * 5L;

    /**
     * 允许的文件类型
     */
    List<String> ALLOWED_FILE_TYPES = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");

    /**
     * 最大获取图片数量
     */
    int MAX_COUNT = 30;

    /**
     * 相似图片推荐最大返回数量
     */
    int SIMILAR_PICTURE_COUNT = 8;

    /**
     * 抓取地址
     */
    String FETCH_URL = "https://cn.bing.com/images/async?q=%s&mmasync=1";

    /**
     * 缩略图文件名标识（追加在主文件名后）
     */
    String THUMBNAIL_FILE_TAG = "_thumb";

    /**
     * 缩略图处理规则：imageView2 模式 2（限框等比、不裁剪），宽高均不超过 200，输出 webp
     */
    String THUMBNAIL_RULE = "imageView2/2/w/200/h/200/format/webp";

    /**
     * 图片详情多级缓存 key 前缀（拼接图片 id）；
     * 生产端（queryById）和清理端（evict / AsyncCacheManager）共用，避免拼写漂移
     */
    String PICTURE_VO_CACHE_KEY_PREFIX = "picture:picture:queryById:";

}
