package Gabransel.DocFlow.services;


import Gabransel.DocFlow.dto.UpdateUserDto;
import Gabransel.DocFlow.dto.UserResponseDto;
import Gabransel.DocFlow.entities.User;
import Gabransel.DocFlow.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponseDto> findAll(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable).map(UserResponseDto::from);
    }

    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id, User authenticated) {
        User user = findOrThrow(id);
        checkOwnerOrAdmin(id, authenticated);
        return UserResponseDto.from(findOrThrow(id));
    }

    @Transactional
    public UserResponseDto update(Long id, UpdateUserDto dto, User authenticated) {
        checkOwnerOrAdmin(id, authenticated);
        User user = findOrThrow(id);

        if (dto.name() != null) {
            user.setName(dto.name());
        }
        if (dto.email() != null) {
            if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
            }
            user.setEmail(dto.email());
        }
        return UserResponseDto.from(user);
    }


    private void checkOwnerOrAdmin(Long id, User authenticated) {
        boolean isAdmin = authenticated.getRoles().contains(User.UserRole.ADMIN);
        if (!isAdmin && !authenticated.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
}