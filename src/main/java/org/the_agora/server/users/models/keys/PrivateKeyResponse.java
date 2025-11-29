package org.the_agora.server.users.models.keys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateKeyResponse {
    private Long userId;
    private String encryptedPrivateKey;

    public PrivateKeyResponse(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }
}
