package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 修改资料请求体，对应前端 {@code PUT /api/auth/profile} 的 JSON body：
 * <pre>{"nickname":"...","oldPassword":"...","newPassword":"..."}</pre>
 * <p>字段均可选：只传 nickname 改昵称；传 newPassword + oldPassword 改密码。
 * 空字符串与 null 都视为“不修改”，校验正则允许空串以配合业务层的可选语义。
 */
public class ProfileRequest {

    /** 昵称同时作为登录账号，需唯一；2-50 位，允许空串表示不修改 */
    @Pattern(regexp = "^$|[\\w\\u4e00-\\u9fa5-]{2,50}",
            message = "昵称只能包含字母、数字、下划线、中文和连字符，长度 2-50")
    private String nickname;

    /** 原密码，修改密码时必填 */
    private String oldPassword;

    /** 新密码；8-64 位，允许空串表示不修改 */
    @Pattern(regexp = "^$|.{8,64}", message = "密码长度需在 8-64 位之间")
    private String newPassword;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
