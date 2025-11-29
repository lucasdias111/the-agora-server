package org.the_agora.server.users.models.keys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyUploadRequest {
    private String publicKey;
    private String encryptedPrivateKey;
}
