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
    long MAX_FILE_SIZE = 1024 * 1024 * 10L;

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
     * 相似图片推荐的余弦相似度阈值（CLIP 语义向量，0~1）：≥ 该值才视为"相似"。
     * 取代原 pHash（近重复检测，无法区分同场景 AI 图组与无关图）。
     * 阈值按本库 60 张图 / 1770 对实测标定（中位数仅 0.564）：
     * - 0.90~0.95：真成组区（同场景 AI 图组、同系列风景）
     * - 0.82~0.90：同题材次相似，仍不违和
     * - 0.80~0.82：同题材为主（古风配古风、动漫少女配动漫少女）
     * - 0.78 以下：开始混入跨题材；0.70 会退化"同风格推荐"（平均每图能凑 10.9 条，塞满 8 条上限）
     * 取 0.80：覆盖 34 张图、平均 4.2 条/图，宽泛感足够且质量未掉。
     * 注意：网传"0.7 以上都算相似"不成立，CLIP 余弦无通用阈值，必须按库实测。
     */
    double CLIP_SIMILARITY_THRESHOLD = 0.80;

    /**
     * 上传查重的 pHash 汉明距离阈值（64 位指纹）：≤ 该值判定为"同一张图"。
     * 仅用于上传时查重，不参与相似推荐。
     * 依据：同一张图的压缩/缩放/裁剪版本距离通常 0~8；不同图（哪怕同场景）距离 ≥ 14。
     * 取 5 偏严，宁漏勿误，避免把"同场景不同构图"的图误判成重复而挡住上传。
     * 查重范围严格隔离：公共图库（spaceId 为空）只跟公共图库比，
     * 空间内图片只跟同一 spaceId 比，跨空间绝不查重。
     */
    int PHASH_DUPLICATE_DISTANCE = 5;

    /**
     * 主色调筛选的同色系距离阈值（ColorUtil.colorDistance 加权欧氏距离，0~约765）：
     * 只返回距离 ≤ 该值的图片，超出即判定"颜色不符"直接不返回，不做任何补位。
     * <p>
     * 标定依据（本库 61 张图、主色调为自研直方图算法后的实测）：
     * 库内主色两两距离中位数 279、p25 = 122。
     * 取 120 时搜碧海蓝(#2986b7)命中 8、粉(#FFC0CB)命中 14、白 7、黑 6，同色系干净命中；
     * 纯红/纯绿等库里没有对应色系的查询如实返回空。
     */
    double COLOR_FILTER_MAX_DISTANCE = 120.0;

    /**
     * 抓取地址
     */
    String FETCH_URL = "https://cn.bing.com/images/async?q=%s&mmasync=1";

    /**
     * 缩略图文件名标识（追加在主文件名后）
     */
    String THUMBNAIL_FILE_TAG = "_thumb";

    /**
     * 缩略图边长上限（px）：主图宽高都 ≤ 此值的图片视为"小图"，不再生成缩略图，
     * 前端直接用小原图（已足够清晰且更省事）。同时作为 imageView2 的缩放框，避免卡片放大后糊。
     */
    int THUMBNAIL_SIZE = 400;

    /**
     * 缩略图处理规则：imageView2 模式 2（限框等比、不裁剪，保持原图比例），边长不超过 400，输出 webp 且质量 85。
     * 旧规则仅 200px + 默认压缩，卡片一放大就糊；提到 400 并指定 q/85 后清晰度显著提升。
     */
    String THUMBNAIL_RULE = String.format("imageView2/2/w/%d/h/%d/format/webp/q/85", THUMBNAIL_SIZE, THUMBNAIL_SIZE);

    /**
     * 缩略图固定输出格式（与主图格式无关，主图保留原始格式，缩略图统一 webp 省流量）
     */
    String THUMBNAIL_FORMAT = "webp";

    /**
     * 图片详情多级缓存 key 前缀（拼接图片 id）；
     * 生产端（queryById）和清理端（evict / AsyncCacheManager）共用，避免拼写漂移
     */
    String PICTURE_VO_CACHE_KEY_PREFIX = "picture:picture:queryById:";

}
