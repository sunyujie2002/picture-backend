package com.sunyujie.picture_backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.user.AddUserRequest;
import com.sunyujie.picture_backend.model.dto.user.LoginRequest;
import com.sunyujie.picture_backend.model.dto.user.QueryUserRequest;
import com.sunyujie.picture_backend.model.dto.user.RegisterRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateUserRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateMyInfoRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdatePasswordRequest;
import com.sunyujie.picture_backend.model.entity.User;
import com.sunyujie.picture_backend.model.vo.user.SimpleUserVO;
import com.sunyujie.picture_backend.model.vo.user.UserVO;

/**
 * User Service 接口
 */
public interface UserService extends IService<User> {
    Long register(RegisterRequest registerRequest);

    String login(LoginRequest loginRequest);

    UserVO getLoginUser();

    Boolean logout();

    Long addUser(AddUserRequest addUserRequest);

    Page<UserVO> queryUserList(QueryUserRequest queryUserRequest);

    Boolean deleteUser(DeleteRequest deleteRequest);

    Boolean updateUser(UpdateUserRequest updateUserRequest);

    UserVO getUserById(Long userId);

    /**
     * 关键词搜索用户（团队空间邀请成员时检索被邀请人）。
     * 关键词同时匹配账号和昵称；关键词为空返回空列表；
     * 不含当前登录用户，最多返回 20 条。
     *
     * @param keyword 账号 / 昵称关键词
     * @return 用户简要信息列表
     */
    List<SimpleUserVO> searchUsers(String keyword);

    /**
     * 用户修改自己的信息（昵称、头像、简介）
     * <p>
     * 不需要管理员权限，但只能操作本人：目标用户 id 从登录态获取，请求体中的 id 类字段不生效。
     *
     * @param updateMyInfoRequest 修改自己信息请求
     * @return 是否成功
     */
    Boolean updateMyInfo(UpdateMyInfoRequest updateMyInfoRequest);

    /**
     * 修改密码
     * <p>
     * 必须校验旧密码；修改成功后该用户在其他设备上的登录态失效，当前设备保持登录。
     *
     * @param updatePasswordRequest 修改密码请求
     * @return 是否成功
     */
    Boolean updatePassword(UpdatePasswordRequest updatePasswordRequest);

    /**
     * 根据一批用户 id 批量查询用户名，用于列表/详情场景按 id 回填名称，避免逐个查询（N+1）。
     *
     * @param userIds 用户 id 集合
     * @return userId -> userName 的映射；已逻辑删除的用户不包含在结果中
     */
    Map<Long, String> getUserNameMap(Collection<Long> userIds);
}
