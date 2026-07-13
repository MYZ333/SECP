import { JSEncrypt } from 'jsencrypt'
import request from './request'

// 缓存后端 RSA 公钥，避免每次登录都请求
export async function getPublicKey() {
  const res = await request.get('/auth/public-key')
  return res.data.publicKey
}

/**
 * 用后端公钥加密密码。任何异常都回退为明文（后端向后兼容），保证登录不被加密环节阻断。
 */
export async function encryptPassword(password) {
  if (!password) return password
  try {
    const key = await getPublicKey()
    const encryptor = new JSEncrypt()
    encryptor.setPublicKey(key)
    const encrypted = encryptor.encrypt(password)
    return encrypted || password
  } catch (e) {
    console.warn('密码加密失败，回退明文提交', e)
    return password
  }
}
