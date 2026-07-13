import { JSEncrypt } from 'jsencrypt'
import request from './request'

export async function getPublicKey() {
  const res = await request.get('/auth/public-key')
  return res.data.publicKey
}

export async function encryptPassword(password) {
  if (!password) return password
  try {
    const key = await getPublicKey()
    const encryptor = new JSEncrypt()
    encryptor.setPublicKey(key)
    return encryptor.encrypt(password) || password
  } catch (e) {
    return password
  }
}
