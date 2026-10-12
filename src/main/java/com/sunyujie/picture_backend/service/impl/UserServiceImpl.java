package com.sunyujie.picture_backend.service.impl;

import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.constant.SaTokenConstant;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.manager.CosManager;
import com.sunyujie.picture_backend.manager.async.AsyncFileManager;
import com.sunyujie.picture_backend.mapper.UserMapper;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.dto.user.AddUserRequest;
import com.sunyujie.picture_backend.model.dto.user.LoginRequest;
import com.sunyujie.picture_backend.model.dto.user.QueryUserRequest;
import com.sunyujie.picture_backend.model.dto.user.RegisterRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateUserRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateMyInfoRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdatePasswordRequest;
import com.sunyujie.picture_backend.model.entity.User;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.model.vo.user.SimpleUserVO;
import com.sunyujie.picture_backend.model.vo.user.UserVO;
import com.sunyujie.picture_backend.service.UserService;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * User Service 实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private CosManager cosManager;

    @Resource
    private AsyncFileManager asyncFileManager;

    @Override
    public Long register(RegisterRequest registerRequest) {
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String confirmPassword = registerRequest.getConfirmPassword();
        ThrowUtil.throwIf(StrUtil.hasBlank(userAccount, userPassword, confirmPassword), ErrorCode.PARAMS_ERROR,
                "用户名、密码或确认密码不能为空");
        ThrowUtil.throwIf(!userPassword.equals(confirmPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        ThrowUtil.throwIf(
                baseMapper.selectCount(lambdaQueryWrapper.eq(User::getUserAccount, userAccount)) != 0,
                ErrorCode.PARAMS_ERROR, "用户名已存在");
        // 密码加密
        userPassword = BCrypt.hashpw(userPassword, BCrypt.gensalt());
        // 插入用户
        User user = new User();
        // 提前分配雪花 id：@TableId 是 ASSIGN_ID，id 非空时 MyBatis-Plus 不会再重复生成
        Long userId = IdWorker.getId();
        user.setId(userId);
        // 默认昵称："用户_" + id 后 6 位，天然唯一；用户后续可自行修改
        String idStr = NumberUtil.toStr(userId);
        user.setUserName("用户_" + CharSequenceUtil.sub(idStr, -6, idStr.length()));
        user.setUserAccount(userAccount);
        user.setUserPassword(userPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        try {
            baseMapper.insert(user);
        } catch (Exception e) {
            log.error("插入用户失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }
        return user.getId();
    }

    @Override
    public String login(LoginRequest loginRequest) {
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        ThrowUtil.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR,
                "用户名或密码不能为空");
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        User user = baseMapper.selectOne(lambdaQueryWrapper.eq(User::getUserAccount, userAccount));
        ThrowUtil.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户名不存在或密码错误");
        ThrowUtil.throwIf(!BCrypt.checkpw(userPassword, user.getUserPassword()), ErrorCode.PARAMS_ERROR, "用户名不存在或密码错误");
        // Sa-Token 登录：签发 token 并建立登录会话
        StpUtil.login(user.getId());
        // 脱敏用户信息缓存进账户会话，供拦截器直接使用
        StpUtil.getSession().set(SaTokenConstant.LOGIN_USER, BeanUtil.copyProperties(user, UserDTO.class));
        return StpUtil.getTokenValue();
    }

    @Override
    public UserVO getLoginUser() {
        // 当前登录用户在拦截器校验 token 后已存入 UserContext，直接取即可
        UserDTO loginUser = UserContext.getLoginUser();
        ThrowUtil.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return BeanUtil.copyProperties(loginUser, UserVO.class);
    }

    @Override
    public Boolean logout() {
        // Sa-Token 注销当前登录态
        StpUtil.logout();
        return true;
    }

    @Override
    @AuthCheck
    public Long addUser(AddUserRequest userAddRequest) {
        String userAccount = userAddRequest.getUserAccount();
        String userName = userAddRequest.getUserName();
        String userRole = userAddRequest.getUserRole();
        ThrowUtil.throwIf(StrUtil.hasBlank(userAccount, userName, userRole), ErrorCode.PARAMS_ERROR,
                "账号、昵称或角色不能为空");
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        ThrowUtil.throwIf(
                baseMapper.selectCount(lambdaQueryWrapper.eq(User::getUserAccount, userAccount)) != 0,
                ErrorCode.PARAMS_ERROR, "用户账号已存在");
        ThrowUtil.throwIf(
                baseMapper.selectCount(lambdaQueryWrapper.eq(User::getUserName, userName)) != 0,
                ErrorCode.PARAMS_ERROR, "用户昵称已存在");
        // 插入用户
        User user = new User();
        // 密码加密
        String userPassword = BCrypt.hashpw("123456", BCrypt.gensalt());
        user.setUserPassword(userPassword);
        user.setUserAccount(userAccount);
        user.setUserName(userName);
        user.setUserRole(userRole);
        try {
            baseMapper.insert(user);
        } catch (Exception e) {
            log.error("插入用户失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户添加失败");
        }
        return user.getId();
    }

    @Override
    @AuthCheck
    public Page<UserVO> queryUserList(QueryUserRequest queryUserRequest) {
        QueryWrapper<User> queryWrapper = buildQueryWrapper(queryUserRequest);
        int pageNum = queryUserRequest.getPageNum();
        int pageSize = queryUserRequest.getPageSize();
        // 创建分页空对象
        Page<User> page = new Page<>(pageNum, pageSize);
        // 分页查询，获取列表
        List<User> userList = baseMapper.selectPage(page, queryWrapper).getRecords();
        // 转换为 UserVO 列表
        List<UserVO> userListVOList = userList.stream().map(user -> BeanUtil.copyProperties(user, UserVO.class))
                .collect(Collectors.toList());
        // 转换为 UserVO 分页对象
        Page<UserVO> pageVO = new Page<>(pageNum, pageSize, page.getTotal());
        // 设置列表
        pageVO.setRecords(userListVOList);
        return pageVO;
    }

    @Override
    @AuthCheck
    public Boolean deleteUser(DeleteRequest deleteRequest) {
        Long userId = deleteRequest.getId();
        ThrowUtil.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 先查出用户：删除 DB 记录前需要拿到头像地址
        User user = baseMapper.selectById(userId);
        ThrowUtil.throwIf(user == null, ErrorCode.OPERATION_ERROR, "用户不存在或已删除");
        String userAvatar = user.getUserAvatar();
        // 先删 COS 头像再删 DB：删除失败时 DB 记录还在，管理员可重试，不会产生僵尸文件。
        // 只清理我们自己 avatar 目录下的文件，外部 URL 不动
        if (StrUtil.isNotBlank(userAvatar)
                && userAvatar.startsWith(PictureConstant.AVATAR_IMAGE_DIR_PREFIX)) {
            try {
                cosManager.deleteObject(userAvatar);
            } catch (Exception e) {
                log.error("删除用户头像失败, userId = {}", userId, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户删除失败");
            }
        }
        // 删除用户
        try {
            baseMapper.deleteById(userId);
        } catch (Exception e) {
            log.error("删除用户失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户删除失败");
        }
        return true;
    }

    @Override
    @AuthCheck
    public Boolean updateUser(UpdateUserRequest updateUserRequest) {
        Long userId = updateUserRequest.getId();
        ThrowUtil.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 更新用户
        User user = BeanUtil.copyProperties(updateUserRequest, User.class);
        user.setEditTime(LocalDateTime.now());
        int result;
        try {
            result = baseMapper.updateById(user);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户更新失败");
        }
        // 影响行数为 0 说明用户不存在或已删除（业务校验放在 try 外，避免被 catch 拦截）
        ThrowUtil.throwIf(result == 0, ErrorCode.OPERATION_ERROR, "用户不存在或已删除");
        return true;
    }

    @Override
    public UserVO getUserById(Long userId) {
        ThrowUtil.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        User user = baseMapper.selectById(userId);
        ThrowUtil.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    @Override
    public List<SimpleUserVO> searchUsers(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return new ArrayList<>();
        }
        String trimmedKeyword = keyword.trim();
        Long currentUserId = UserContext.getLoginUserId();
        // 只取简要信息所需 4 列；括号包裹 OR 条件：(账号 LIKE ? OR 昵称 LIKE ?) AND id <> 当前用户
        // 不加括号会因 AND 优先级高于 OR 产生逻辑错误
        List<User> userList = baseMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUserAccount, User::getUserName, User::getUserAvatar)
                // 白名单只允许普通用户被邀请：平台 admin 天然拥有全部空间权限，不出现在结果中
                .eq(User::getUserRole, UserRoleEnum.USER.getValue())
                .and(wrapper -> wrapper.like(User::getUserAccount, trimmedKeyword)
                        .or()
                        .like(User::getUserName, trimmedKeyword))
                .ne(User::getId, currentUserId)
                .last("LIMIT 20"));
        return userList.stream()
                .map(user -> BeanUtil.copyProperties(user, SimpleUserVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Boolean updateMyInfo(UpdateMyInfoRequest updateMyInfoRequest) {
        // 目标用户只认登录态，请求体无法指定 id，天然只能改自己
        Long userId = UserContext.getLoginUserId();
        ThrowUtil.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        String userName = updateMyInfoRequest.getUserName();
        String userAvatar = updateMyInfoRequest.getUserAvatar();
        String userProfile = updateMyInfoRequest.getUserProfile();
        ThrowUtil.throwIf(userName == null && userAvatar == null && userProfile == null,
                ErrorCode.PARAMS_ERROR, "请指定至少一项要更新的内容");
        // 昵称传了就不允许纯空格；简介允许空字符串（用于清空）
        ThrowUtil.throwIf(userName != null && StrUtil.isBlank(userName),
                ErrorCode.PARAMS_ERROR, "昵称不能为空");
        // 查出旧用户：头像迁移失败回滚、清理旧头像都需要旧数据
        User oldUser = baseMapper.selectById(userId);
        ThrowUtil.throwIf(oldUser == null, ErrorCode.OPERATION_ERROR, "用户不存在或已删除");
        String finalAvatar = null;
        if (StrUtil.isNotBlank(userAvatar) && !userAvatar.equals(oldUser.getUserAvatar())) {
            // 头像与图片上传同一套流程：先传 temp，这里迁移到正式目录。
            // 只接受当前用户 temp 目录的文件，防止引用他人临时文件或任意外部地址
            String expectedTempPrefix = PictureConstant.TEMP_IMAGE_DIR_PREFIX + userId + "/";
            ThrowUtil.throwIf(!userAvatar.startsWith(expectedTempPrefix),
                    ErrorCode.PARAMS_ERROR, "头像地址非法，请重新上传");
            String avatarKey = PictureConstant.AVATAR_IMAGE_DIR_PREFIX + userId + "/"
                    + FileUtil.mainName(userAvatar) + "." + FileUtil.getSuffix(userAvatar);
            // temp → 正式目录（服务器端拷贝，不产生下载流量）
            cosManager.copyObject(userAvatar, avatarKey);
            finalAvatar = avatarKey;
        }
        // 组装更新实体：null 字段 MyBatis-Plus 默认忽略，不会覆盖成空
        User user = new User();
        user.setId(userId);
        user.setUserName(userName);
        user.setUserAvatar(finalAvatar);
        user.setUserProfile(userProfile);
        user.setEditTime(LocalDateTime.now());
        try {
            baseMapper.updateById(user);
        } catch (Exception e) {
            // DB 失败：同步回滚已复制到正式目录的头像（avatar 目录无自动兜底，必须同步尽力删成功）
            if (StrUtil.isNotBlank(finalAvatar)) {
                cosManager.deleteObject(finalAvatar);
            }
            log.error("用户更新自己信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        // DB 成功后异步清理：temp 源文件（另有 24h 兜底）+ 旧头像（只删 avatar 目录、新旧不同）
        if (StrUtil.isNotBlank(finalAvatar)) {
            List<String> unusedFileKeys = new ArrayList<>();
            unusedFileKeys.add(userAvatar);
            String oldAvatar = oldUser.getUserAvatar();
            if (StrUtil.isNotBlank(oldAvatar)
                    && oldAvatar.startsWith(PictureConstant.AVATAR_IMAGE_DIR_PREFIX)
                    && !oldAvatar.equals(finalAvatar)) {
                unusedFileKeys.add(oldAvatar);
            }
            asyncFileManager.deleteFiles(unusedFileKeys);
        }
        // 同步更新 Redis 中的登录态，避免改了昵称/头像后 getLoginUser 仍是旧信息（直到重新登录才刷新）
        refreshLoginUserInfo(userName, finalAvatar, userProfile, user.getEditTime());
        return true;
    }

    @Override
    public Boolean updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        Long userId = UserContext.getLoginUserId();
        ThrowUtil.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();
        String confirmPassword = updatePasswordRequest.getConfirmPassword();
        ThrowUtil.throwIf(StrUtil.hasBlank(oldPassword, newPassword, confirmPassword),
                ErrorCode.PARAMS_ERROR, "密码不能为空");
        ThrowUtil.throwIf(!newPassword.equals(confirmPassword),
                ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        ThrowUtil.throwIf(newPassword.equals(oldPassword),
                ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        // 查出用户，验证旧密码（与登录一致：密码错误不透露用户是否存在）
        User user = baseMapper.selectById(userId);
        ThrowUtil.throwIf(user == null, ErrorCode.OPERATION_ERROR, "用户不存在或已删除");
        ThrowUtil.throwIf(!BCrypt.checkpw(oldPassword, user.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "旧密码错误");
        user.setUserPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setEditTime(LocalDateTime.now());
        try {
            baseMapper.updateById(user);
        } catch (Exception e) {
            log.error("修改密码失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码失败");
        }
        // 其他设备下线：删除该用户的其他登录态（当前设备保留），防止密码泄露后旧会话仍可用
        evictOtherLoginSessions(userId, UserContext.getToken());
        return true;
    }

    /**
     * 下线指定用户除当前 token 外的其他登录态（Sa-Token）
     */
    private void evictOtherLoginSessions(Long userId, String currentToken) {
        try {
            // 扫描全部 token（size=-1 不限制数量）
            List<String> tokenList = StpUtil.searchTokenValue("", 0, -1, false);
            for (String token : tokenList) {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    continue;
                }
                // 属于该用户且不是当前设备的 token，按 token 注销
                if (Long.valueOf(loginId.toString()).equals(userId) && !token.equals(currentToken)) {
                    StpUtil.logoutByTokenValue(token);
                }
            }
        } catch (Exception e) {
            // 下线失败不影响密码修改结果，记录日志；其他设备最坏在 TTL 到期后自动失效
            log.error("清除其他设备登录态失败", e);
        }
    }

    /**
     * 把最新资料写回 Sa-Token 账户会话，并刷新当前线程上下文
     */
    private void refreshLoginUserInfo(String userName, String userAvatar, String userProfile,
            LocalDateTime editTime) {
        UserDTO contextUser = UserContext.getLoginUser();
        if (contextUser == null) {
            return;
        }
        try {
            SaSession session = StpUtil.getSession();
            UserDTO sessionUser = (UserDTO) session.get(SaTokenConstant.LOGIN_USER);
            if (sessionUser != null) {
                if (userName != null) {
                    sessionUser.setUserName(userName);
                }
                if (userAvatar != null) {
                    sessionUser.setUserAvatar(userAvatar);
                }
                if (userProfile != null) {
                    sessionUser.setUserProfile(userProfile);
                }
                sessionUser.setEditTime(editTime);
                session.set(SaTokenConstant.LOGIN_USER, sessionUser);
            }
            // 当前请求的 ThreadLocal 一并更新（token 保持当前设备不变）
            if (userName != null) {
                contextUser.setUserName(userName);
            }
            if (userAvatar != null) {
                contextUser.setUserAvatar(userAvatar);
            }
            if (userProfile != null) {
                contextUser.setUserProfile(userProfile);
            }
            contextUser.setEditTime(editTime);
            UserContext.setLoginUser(contextUser);
        } catch (Exception e) {
            // 登录态同步失败不影响资料更新结果，记录日志即可
            log.error("同步登录态信息失败", e);
        }
    }

    private QueryWrapper<User> buildQueryWrapper(QueryUserRequest queryUserRequest) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        String sortField = queryUserRequest.getSortField();
        String sortOrder = queryUserRequest.getSortOrder();
        // 账号查询
        queryWrapper.like(StrUtil.isNotBlank(queryUserRequest.getUserAccount()), "user_account",
                queryUserRequest.getUserAccount());
        // 用户昵称查询
        queryWrapper.like(StrUtil.isNotBlank(queryUserRequest.getUserName()), "user_name",
                queryUserRequest.getUserName());
        // 用户角色查询
        queryWrapper.eq(StrUtil.isNotBlank(queryUserRequest.getUserRole()), "user_role",
                queryUserRequest.getUserRole());
        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),
                sortOrder.equals("ascend"), StrUtil.toUnderlineCase(sortField));
        return queryWrapper;
    }

    @Override
    public Map<Long, String> getUserNameMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        // 去重并剔除 null：列表场景同一用户可能出现多次（IN 对重复值不会出错也不会返回重复行，
        // 但去重后 SQL 更短）；在公共方法内统一处理，调用方无需各自去重
        Set<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }
        // 只查 id 和昵称两列；过滤掉用户名为 null 的记录，否则 Collectors.toMap 会抛 NPE
        return baseMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUserName)
                        .in(User::getId, distinctUserIds))
                .stream()
                .filter(user -> StrUtil.isNotBlank(user.getUserName()))
                .collect(Collectors.toMap(User::getId, User::getUserName));
    }
}
