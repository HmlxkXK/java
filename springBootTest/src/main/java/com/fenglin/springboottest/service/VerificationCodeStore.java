package com.fenglin.springboottest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码存储（内存实现）。
 * <p>演示环境够用；生产建议换成 Redis，这样多实例部署时验证码才不会丢。
 * <p>验证码校验成功后立即消费，防止同一验证码重复注册。
 */
@Component
public class VerificationCodeStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final int codeLength;
    private final long ttlMillis;

    public VerificationCodeStore(@Value("${app.auth.code-length:6}") int codeLength,
                                 @Value("${app.auth.code-ttl-seconds:300}") long ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlMillis = ttlSeconds * 1000L;
    }

    /**
     * 为指定邮箱生成验证码并覆盖旧值。
     *
     * @return 生成的验证码明文（真实项目应只发到邮箱，不返回给调用方）
     */
    public String generate(String email) {
        cleanExpired();
        String code = randomCode();
        store.put(key(email), new Entry(code, System.currentTimeMillis() + ttlMillis));
        return code;
    }

    /**
     * 校验并消费验证码。校验通过后该验证码立即失效。
     */
    public boolean verifyAndConsume(String email, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String cacheKey = key(email);
        Entry entry = store.get(cacheKey);
        if (entry == null) {
            return false;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            store.remove(cacheKey);
            return false;
        }
        if (!entry.code.equals(code)) {
            return false;
        }
        // 原子移除（remove(key, value)）：并发提交同一验证码时只有一个请求能移除成功，
        // 其余请求返回 false。若用 get + remove 两步，两个线程都可能通过校验，
        // 进而各自注册出一个账号。Entry 未重写 equals，这里按同一对象引用比对，语义正确。
        return store.remove(cacheKey, entry);
    }

    private String randomCode() {
        int bound = (int) Math.pow(10, codeLength);
        int value = random.nextInt(bound);
        return String.format("%0" + codeLength + "d", value);
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** 顺手清理过期条目，避免 Map 无限增长 */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    private static class Entry {
        private final String code;
        private final long expireAt;

        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
