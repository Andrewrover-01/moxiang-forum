/**
 * Reusable Element-Plus form validation rule factories.
 * All rules are compatible with the `FormItemRule` type from element-plus.
 */
import type { FormItemRule } from 'element-plus'

/** A single required-field rule with a custom error message. */
export function requiredRule(message: string): FormItemRule {
  return { required: true, message, trigger: 'blur' }
}

/** Validation rules for a username field (mirrors backend constraints). */
export const usernameRules: FormItemRule[] = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 2, max: 20, message: '用户名长度需在2-20个字符之间', trigger: 'blur' },
  {
    pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/,
    message: '用户名只能包含字母、数字、下划线或汉字',
    trigger: 'blur'
  }
]

/** Validation rules for a password field (mirrors backend constraints). */
export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, max: 32, message: '密码长度需在6-32个字符之间', trigger: 'blur' }
]

/** Validation rules for an email address field. */
export const emailRules: FormItemRule[] = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
]

/** Validation rules for a post or novel title (max 100 chars). */
export const titleRules: FormItemRule[] = [
  { required: true, message: '请输入标题', trigger: 'blur' },
  { max: 100, message: '标题最多100个字符', trigger: 'blur' }
]

/** Validation rules for post content (max 50,000 chars). */
export const postContentRules: FormItemRule[] = [
  { required: true, message: '请输入内容', trigger: 'blur' },
  { max: 50_000, message: '内容最多50000个字符', trigger: 'blur' }
]

/** Validation rules for comment content (max 5,000 chars). */
export const commentContentRules: FormItemRule[] = [
  { required: true, message: '请输入评论内容', trigger: 'blur' },
  { max: 5_000, message: '评论最多5000个字符', trigger: 'blur' }
]

/** Validation rules for a novel description (max 2,000 chars, optional). */
export const novelDescRules: FormItemRule[] = [
  { max: 2_000, message: '简介最多2000个字符', trigger: 'blur' }
]

/** Validation rules for a novel category selector. */
export const novelCategoryRules: FormItemRule[] = [
  { required: true, message: '请选择分类', trigger: 'change' }
]

/** Validation rules for a forum selector in the post-create form. */
export const forumIdRules: FormItemRule[] = [
  { required: true, message: '请选择版块', trigger: 'change' }
]

/**
 * Build a "confirm password" cross-field validation rule.
 * @param getPassword – getter that returns the original password value to compare against
 */
export function confirmPasswordRule(getPassword: () => string): FormItemRule {
  return {
    required: true,
    validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
      if (!value) {
        callback(new Error('请再次输入密码'))
      } else if (value !== getPassword()) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }
}
