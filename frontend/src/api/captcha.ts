import request from '@/utils/request'
import type { ApiResponse, CaptchaChallenge, CaptchaVerifyResult } from '@/types/api'

/**
 * Fetches a CAPTCHA challenge.
 * @param scene 'REGISTER' | 'POST' | 'COMMENT'
 */
export function getCaptchaChallenge(scene: string) {
  return request.get<ApiResponse<CaptchaChallenge>>('/captcha/challenge', { params: { scene } })
}

/**
 * Verifies a CAPTCHA answer and returns a single-use token.
 * @param captchaId the ID from getCaptchaChallenge
 * @param answer    slider position (0-100) for SLIDER type; null for INVISIBLE
 */
export function verifyCaptcha(captchaId: string, answer: number | null) {
  return request.post<ApiResponse<CaptchaVerifyResult>>('/captcha/verify', { captchaId, answer })
}
