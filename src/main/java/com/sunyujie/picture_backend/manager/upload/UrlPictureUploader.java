package com.sunyujie.picture_backend.manager.upload;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;

/**
 * URL 上传图片：差异步骤 = 校验外链、下载到临时文件
 */
@Component
public class UrlPictureUploader extends AbstractPictureUploader<String> {

    /**
     * 模拟 Chrome 浏览器的 UA，避免被部分源站按 UA 拦截
     */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * 构造带浏览器请求头的请求（探测和下载共用）
     */
    private HttpRequest createRequest(Method method, String fileUrl) {
        HttpRequest request = HttpUtil.createRequest(method, fileUrl)
                .header(Header.USER_AGENT, USER_AGENT)
                .header(Header.ACCEPT, "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        // Referer 设为目标站源，模拟从站内页面发起的请求，降低防盗链拦截概率
        try {
            URI uri = new URI(fileUrl);
            request.header(Header.REFERER, uri.getScheme() + "://" + uri.getHost() + "/");
        } catch (URISyntaxException e) {
            // validateParam 已做语法校验，理论不会发生
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径格式错误");
        }
        return request;
    }

    @Override
    protected String validateParam(String fileUrl) {
        // 语法校验：URI 能解析（相对路径会抛 IllegalArgumentException，一并接住）
        try {
            new URI(fileUrl).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径格式错误");
        }
        // 协议白名单：URI 只校验"有协议"，还要限定只能是 http/https
        if (!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的图片路径");
        }
        // 探测链接是否可访问、类型和大小是否合法
        HttpResponse response = null;
        try {
            response = createRequest(Method.HEAD, fileUrl).execute();
            // 部分服务器不支持 HEAD，改用 GET 探测；重赋值前先关闭旧连接
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                response.close();
                response = createRequest(Method.GET, fileUrl).execute();
            }
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片链接不可访问");
            }
            // Content-Type 是 MIME 类型，转为扩展名再比对白名单
            String contentType = response.header("Content-Type");
            String picFormat = StrUtil.isBlank(contentType) ? "" : contentType.split(";")[0].trim()
                    .replace("image/", "");
            if (!PictureConstant.ALLOWED_FILE_TYPES.contains(picFormat)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误，仅支持 "
                        + String.join(",", PictureConstant.ALLOWED_FILE_TYPES) + "格式");
            }
            // 部分服务器不返回 Content-Length，无法确认大小，跳过校验
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength) && NumberUtil.isLong(contentLength)
                    && Long.parseLong(contentLength) > PictureConstant.MAX_FILE_SIZE) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制，最大 "
                        + PictureConstant.MAX_FILE_SIZE / 1024 / 1024 + " MB");
            }
            return picFormat;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected void saveToTempFile(File tempFile, String fileUrl) throws Exception {
        // 带浏览器请求头下载，避免 502/防盗链；try-with-resources 保证连接关闭
        try (HttpResponse response = createRequest(Method.GET, fileUrl).execute()) {
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "图片下载失败，状态码：" + response.getStatus());
            }
            response.writeBody(tempFile);
        }
    }

    @Override
    protected String getOriginalFilename(String fileUrl) {
        // 取 URL 路径中的文件名，去掉查询参数（如 a.png?x=1）
        int queryIndex = fileUrl.indexOf('?');
        String path = queryIndex == -1 ? fileUrl : fileUrl.substring(0, queryIndex);
        return FileUtil.getName(path);
    }
}
