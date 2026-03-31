package com.moxiang.common.api;

/**
 * Unified API response result codes.
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "暂未登录或token已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),

    // User related
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(1003, "邮箱已被注册"),
    PASSWORD_ERROR(1004, "用户名或密码错误"),
    USER_DISABLED(1005, "账号已被禁用"),

    // Post related
    POST_NOT_FOUND(2001, "帖子不存在"),
    POST_ALREADY_LIKED(2002, "已经点赞过了"),
    POST_NOT_LIKED(2003, "尚未点赞"),

    // Novel related
    NOVEL_NOT_FOUND(3001, "小说不存在"),
    CHAPTER_NOT_FOUND(3002, "章节不存在"),
    NOVEL_ALREADY_COLLECTED(3003, "已经收藏过了"),

    // Forum related
    FORUM_NOT_FOUND(4001, "版块不存在"),

    // Comment related
    COMMENT_NOT_FOUND(5001, "评论不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
