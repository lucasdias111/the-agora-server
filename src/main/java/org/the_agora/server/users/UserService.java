package org.the_agora.server.users;

import org.springframework.stereotype.Service;
import org.the_agora.server.users.models.User;
import org.the_agora.server.users.repositories.UserRepository;

@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
	}

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void savePublicPrivateKeysForUser(Long userId, String publicKey, String encryptedPrivateKey) {
        User user = getById(userId);
        user.setPublicKey(publicKey);
        user.setEncryptedPrivateKey(encryptedPrivateKey);
        userRepository.save(user);
    }
}
