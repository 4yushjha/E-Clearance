package in.ac.mitmeerut.clearance.service;

import java.util.List;

import org.springframework.stereotype.Service;

import in.ac.mitmeerut.clearance.dto.UserDTO;
import in.ac.mitmeerut.clearance.entity.User;
import in.ac.mitmeerut.clearance.exception.UserNotFoundException;
import in.ac.mitmeerut.clearance.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // CONVERT USER ENTITY INTO DTO
    private UserDTO convertToDTO(User user) {

        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole()
        );
    }

    // CREATE USER
    public UserDTO saveUser(User user) {

        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    // GET ALL USERS
    public List<UserDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // GET USER BY ID
    public UserDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + id
                        )
                );

        return convertToDTO(user);
    }

    // UPDATE USER
    public UserDTO updateUser(Long id, User user) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + id
                        )
                );

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());

        // Update password only when provided
        if (user.getPassword() != null
                && !user.getPassword().isBlank()) {

            existingUser.setPassword(user.getPassword());
        }

        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());

        User updatedUser = userRepository.save(existingUser);

        return convertToDTO(updatedUser);
    }

    // DELETE USER
    public void deleteUser(Long id) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + id
                        )
                );

        userRepository.delete(existingUser);
    }

    // GET USER BY EMAIL (FOR LOGIN)
    public User getUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email
                        )
                );
    }
}