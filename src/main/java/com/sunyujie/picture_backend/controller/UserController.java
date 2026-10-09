package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.user.AddUserRequest;
import com.sunyujie.picture_backend.model.dto.user.LoginRequest;
import com.sunyujie.picture_backend.model.dto.user.QueryUserRequest;
import com.sunyujie.picture_backend.model.dto.user.RegisterRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateUserRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdateMyInfoRequest;
import com.sunyujie.picture_backend.model.dto.user.UpdatePasswordRequest;
import com.sunyujie.picture_backend.model.vo.user.SimpleUserVO;
import com.sunyujie.picture_backend.model.vo.user.UserVO;
import com.sunyujie.picture_backend.service.UserService;
import com.sunyujie.picture_backend.utils.ResultUtil;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody RegisterRequest registerRequest) {
        Long userId = userService.register(registerRequest);
        return ResultUtil.success(userId, "注册成功");
    }

    @PostMapping("/login")
    public BaseResponse<String> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest);
        return ResultUtil.success(token, "登录成功");
    }

    @GetMapping("/getLoginUser")
    public BaseResponse<UserVO> getLoginUser() {
        UserVO userVO = userService.getLoginUser();
        return ResultUtil.success(userVO, "获取登录态成功");
    }

    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody AddUserRequest addUserRequest) {
        Long userId = userService.addUser(addUserRequest);
        return ResultUtil.success(userId, "添加用户成功");
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() {
        return ResultUtil.success(userService.logout(),"退出成功");
    }

    @PostMapping("/queryList")
    public BaseResponse<Page<UserVO>> queryUserList(@RequestBody QueryUserRequest queryUserRequest) {
        return ResultUtil.success(userService.queryUserList(queryUserRequest), "获取用户列表成功");
    }

    @GetMapping("/getById")
    public BaseResponse<UserVO> getUserById(@RequestParam Long userId) {
        UserVO userVO = userService.getUserById(userId);
        return ResultUtil.success(userVO, "获取用户详情成功");
    }

    @GetMapping("/search")
    public BaseResponse<List<SimpleUserVO>> searchUsers(@RequestParam String keyword) {
        return ResultUtil.success(userService.searchUsers(keyword), "搜索用户成功");
    }

    
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UpdateUserRequest updateUserRequest) {
        return ResultUtil.success(userService.updateUser(updateUserRequest), "更新用户成功");
    }

    @PostMapping("/updateMyInfo")
    public BaseResponse<Boolean> updateMyInfo(@RequestBody UpdateMyInfoRequest updateMyInfoRequest) {
        return ResultUtil.success(userService.updateMyInfo(updateMyInfoRequest), "更新个人信息成功");
    }

    @PostMapping("/updatePassword")
    public BaseResponse<Boolean> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest) {
        return ResultUtil.success(userService.updatePassword(updatePasswordRequest), "修改密码成功");
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        return ResultUtil.success(userService.deleteUser(deleteRequest), "删除用户成功");
    }
}
