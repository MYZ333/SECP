import { JSEncrypt } from 'jsencrypt'
import request from './request'

let cachedPublicKey = null

export async function getPublicKey() {
  if (cachedPublicKey) return cachedPublicKey
  const res = await request.get('/auth/public-key')
  cachedPublicKey = res.data.publicKey
  return cachedPublicKey
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
