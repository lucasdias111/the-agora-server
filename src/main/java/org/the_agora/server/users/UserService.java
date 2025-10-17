package org.the_agora.server.users;

import org.springframework.stereotype.Service;
import org.the_agora.server.users.models.User;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.users.repositories.UserRepository;

@Service
public class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserDTO(user);
    }
}
