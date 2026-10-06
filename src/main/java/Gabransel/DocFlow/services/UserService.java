package Gabransel.DocFlow.services;

import Gabransel.DocFlow.dto.*;
import Gabransel.DocFlow.entities.User;
import Gabransel.DocFlow.exceptions.BusinessException;
import Gabransel.DocFlow.exceptions.ResourceNotFoundException;
import Gabransel.DocFlow.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponseDto> findAll(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable).map(UserResponseDto::from);
    }

    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id, User authenticated) {
        User user = findOrThrow(id);
        checkOwnerOrAdmin(user, authenticated);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto update(Long id, UpdateUserDto dto, User authenticated) {
        User user = findOrThrow(id);
        checkOwnerOrAdmin(user, authenticated);

        if (dto.name() != null) {
            user.setName(dto.name());
        }
        if (dto.email() != null) {
            if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
                throw new BusinessException("Email already used");
            }
            user.setEmail(dto.email());
        }

        return UserResponseDto.from(user);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordDto dto, User authenticated) {
        User user = findOrThrow(id);
        checkOwner(user, authenticated);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect current password.");
        }
        if (passwordEncoder.matches(dto.newPassword(), user.getPassword())) {
            throw new BusinessException("The new password must be different from the current one.");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponseDto updateRole(Long id, UpdateRoleDto dto, User authenticated) {
        User user = findOrThrow(id);


        boolean rebaixandoAdmin = user.getRoles().contains(User.UserRole.ADMIN)
                && dto.role() != User.UserRole.ADMIN;
        if (rebaixandoAdmin && userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN) <= 1) {
            throw new BusinessException("Cannot demote the last active ADMIN.");
        }

        user.setRoles(java.util.Set.of(dto.role()));
        return UserResponseDto.from(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id, User authenticated) {
        User user = findOrThrow(id);

        if (user.getId().equals(authenticated.getId())) {
            throw new BusinessException("You cannot delete your own account.");
        }
        if (user.getRoles().contains(User.UserRole.ADMIN)
                && userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN) <= 1) {
            throw new BusinessException("Cannot deactivate the last active ADMIN.");
        }

        user.desactive();
    }

    @Transactional
    public void deleteOwnUser(DeleteOwnUserDto dto, User authenticated) {
        User user = findOrThrow(authenticated.getId());

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BusinessException("Incorrect password.");
        }
        if (user.getRoles().contains(User.UserRole.ADMIN)
                && userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN) <= 1) {
            throw new BusinessException("Cannot deactivate the last active ADMIN.");
        }

        user.desactive();
    }


    private void checkOwnerOrAdmin(User target, User authenticated) {
        boolean isAdmin = authenticated.getRoles().contains(User.UserRole.ADMIN);
        if (!isAdmin && !target.getId().equals(authenticated.getId())) {
            throw new ResourceNotFoundException("User not found: " + target.getId());
        }
    }

    private void checkOwner(User target, User authenticated) {
        if (!target.getId().equals(authenticated.getId())) {
            throw new ResourceNotFoundException("User not found: " + target.getId());
        }
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}