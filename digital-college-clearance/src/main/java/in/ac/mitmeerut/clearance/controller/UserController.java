package in.ac.mitmeerut.clearance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.ac.mitmeerut.clearance.dto.UserDTO;
import in.ac.mitmeerut.clearance.entity.User;
import in.ac.mitmeerut.clearance.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // CREATE USER
    @PostMapping
    public ResponseEntity<UserDTO> createUser(
            @RequestBody User user) {

        UserDTO savedUser = userService.saveUser(user);

        return new ResponseEntity<>(
                savedUser,
                HttpStatus.CREATED
        );
    }

    // GET ALL USERS
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // UPDATE USER
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.updateUser(id, user)
        );
    }

    // DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "User deleted successfully"
        );
    }
}