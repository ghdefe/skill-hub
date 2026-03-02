package com.agentskills.sharing.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil("test-encryption-key-32-bytes-ok!");
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plainText = "ghp_abc123def456ghi789";

        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        String plainText = "same-token-value";

        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // Different IVs should produce different ciphertexts
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // Both should decrypt to the same value
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    void shouldHandleEmptyString() {
        String encrypted = encryptionUtil.encrypt("");
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertThat(decrypted).isEmpty();
    }

    @Test
    void shouldHandleUnicodeContent() {
        String plainText = "token-with-unicode-中文-日本語";

        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainText);
    }
}
