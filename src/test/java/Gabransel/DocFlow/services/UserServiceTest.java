package Gabransel.DocFlow.services;

import Gabransel.DocFlow.dto.*;
import Gabransel.DocFlow.entities.User;
import Gabransel.DocFlow.exceptions.BusinessException;
import Gabransel.DocFlow.exceptions.ResourceNotFoundException;
import Gabransel.DocFlow.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private User user(Long id, String email, User.UserRole role) {
        User u = new User(new HashSet<>(Set.of(role)), "hash", email, "Nome");
        u.setId(id);
        u.setActive(true);
        return u;
    }

    // ===================== findAll =====================

    @Test
    void findAll_mapsPageToDtosKeepingMetadata() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(
                user(1L, "a@email.com", User.UserRole.USER),
                user(2L, "b@email.com", User.UserRole.ADMIN)), pageable, 25);
        when(userRepository.findAllByActiveTrue(pageable)).thenReturn(page);

        Page<UserResponseDto> result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).email()).isEqualTo("a@email.com");
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    // ===================== findById =====================

    @Test
    void findById_ownerFindsSelf_returnsDto() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        UserResponseDto result = userService.findById(1L, me);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("gabriel@email.com");
    }

    @Test
    void findById_adminFindsOther_returnsDto() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User other = user(2L, "outro@email.com", User.UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        assertThat(userService.findById(2L, admin).id()).isEqualTo(2L);
    }

    @Test
    void findById_adminFindsInactive_returnsDto() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User inactive = user(2L, "inativo@email.com", User.UserRole.USER);
        inactive.setActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThat(userService.findById(2L, admin).id()).isEqualTo(2L);
    }

    @Test
    void findById_userFindsOther_throwsNotFound() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        User other = user(2L, "outro@email.com", User.UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.findById(2L, me))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_idNotFound_throwsNotFound() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L, me))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===================== update =====================

    @Test
    void update_nullFields_changeNothing() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        userService.update(1L, new UpdateUserDto(null, null), me);

        assertThat(me.getName()).isEqualTo("Nome");
        assertThat(me.getEmail()).isEqualTo("gabriel@email.com");
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
    }

    @Test
    void update_onlyName_changesNameKeepsEmail() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        UserResponseDto result = userService.update(1L, new UpdateUserDto("Novo Nome", null), me);

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(me.getEmail()).isEqualTo("gabriel@email.com");
    }

    @Test
    void update_newFreeEmail_changesEmail() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.existsByEmailAndIdNot("novo@email.com", 1L)).thenReturn(false);

        assertThat(userService.update(1L, new UpdateUserDto(null, "novo@email.com"), me).email())
                .isEqualTo("novo@email.com");
    }

    @Test
    void update_emailUsedByOther_throwsAndKeepsEmail() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.existsByEmailAndIdNot("ocupado@email.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L,
                new UpdateUserDto(null, "ocupado@email.com"), me))
                .isInstanceOf(BusinessException.class);

        assertThat(me.getEmail()).isEqualTo("gabriel@email.com");
    }

    @Test
    void update_ownEmailResent_doesNotConflict() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.existsByEmailAndIdNot("gabriel@email.com", 1L)).thenReturn(false);

        assertThat(userService.update(1L, new UpdateUserDto(null, "gabriel@email.com"), me).email())
                .isEqualTo("gabriel@email.com");
        verify(userRepository).existsByEmailAndIdNot("gabriel@email.com", 1L);
    }

    @Test
    void update_userEditsOther_throwsNotFound() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        User other = user(2L, "outro@email.com", User.UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.update(2L, new UpdateUserDto("Hack", null), me))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(other.getName()).isEqualTo("Nome");
    }

    // ===================== changePassword =====================

    @Test
    void changePassword_valid_encodesAndSavesNewPassword() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(passwordEncoder.matches("atual123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "hash")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novoHash");

        userService.changePassword(1L, new ChangePasswordDto("atual123", "novaSenha123"), me);

        verify(passwordEncoder).encode("novaSenha123");
        assertThat(me.getPassword()).isEqualTo("novoHash");
    }

    @Test
    void changePassword_wrongCurrent_throwsAndKeepsPassword() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordDto("errada", "novaSenha123"), me))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Incorrect current password.");

        verify(passwordEncoder, never()).encode(any());
        assertThat(me.getPassword()).isEqualTo("hash");
    }

    // senhas textualmente diferentes, mesmo hash: prova a regra, não a colisão de stubs
    @Test
    void changePassword_newEqualsCurrent_throwsAndKeepsPassword() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(passwordEncoder.matches("atual123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L,
                new ChangePasswordDto("atual123", "novaSenha123"), me))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The new password must be different from the current one.");

        verify(passwordEncoder, never()).encode(any());
        assertThat(me.getPassword()).isEqualTo("hash");
    }

    // sem stub de findById: o service compara ids antes de buscar
    @Test
    void changePassword_adminOnOtherUser_throwsNotFound() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User other = user(2L, "outro@email.com", User.UserRole.USER);

        assertThatThrownBy(() -> userService.changePassword(2L,
                new ChangePasswordDto("qualquer", "novaSenha123"), admin))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(passwordEncoder);
        assertThat(other.getPassword()).isEqualTo("hash");
    }

    // ===================== updateRole =====================

    @Test
    void updateRole_promoteUserToAdmin_works() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User target = user(2L, "user@email.com", User.UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.updateRole(2L, new UpdateRoleDto(User.UserRole.ADMIN), admin);

        assertThat(target.getRoles()).containsExactly(User.UserRole.ADMIN);
    }

    @Test
    void updateRole_demoteAdminWithOthers_works() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User target = user(2L, "admin2@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN)).thenReturn(2L);

        userService.updateRole(2L, new UpdateRoleDto(User.UserRole.USER), admin);

        assertThat(target.getRoles()).containsExactly(User.UserRole.USER);
    }

    @Test
    void updateRole_demoteLastAdmin_throwsAndKeepsRole() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(1L,
                new UpdateRoleDto(User.UserRole.USER), admin))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot demote the last active ADMIN.");

        assertThat(admin.getRoles()).containsExactly(User.UserRole.ADMIN);
    }

    @Test
    void updateRole_keepAdminAsAdminWhenOnlyOne_works() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        userService.updateRole(1L, new UpdateRoleDto(User.UserRole.ADMIN), admin);

        assertThat(admin.getRoles()).containsExactly(User.UserRole.ADMIN);
        verify(userRepository, never()).countByRolesContainingAndActiveTrue(any());
    }

    // ===================== deleteUser =====================

    @Test
    void deleteUser_adminDeactivatesUser_setsInactive() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User target = user(2L, "user@email.com", User.UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.deleteUser(2L, admin);

        assertThat(target.isActive()).isFalse();
    }

    @Test
    void deleteUser_adminDeletesSelf_throwsAndStaysActive() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.deleteUser(1L, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You cannot delete your own account.");

        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void deleteUser_lastAdmin_throwsAndStaysActive() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        User targetAdmin = user(2L, "admin2@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(2L, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot deactivate the last active ADMIN.");

        assertThat(targetAdmin.isActive()).isTrue();
    }

    // ===================== deleteOwnUser =====================

    @Test
    void deleteOwnUser_correctPassword_setsInactive() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        userService.deleteOwnUser(new DeleteOwnUserDto("senha123"), me);

        assertThat(me.isActive()).isFalse();
    }

    @Test
    void deleteOwnUser_wrongPassword_throwsAndStaysActive() {
        User me = user(1L, "gabriel@email.com", User.UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteOwnUser(new DeleteOwnUserDto("errada"), me))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Incorrect password.");

        assertThat(me.isActive()).isTrue();
    }

    @Test
    void deleteOwnUser_lastAdmin_throwsAndStaysActive() {
        User admin = user(1L, "admin@email.com", User.UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(userRepository.countByRolesContainingAndActiveTrue(User.UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteOwnUser(new DeleteOwnUserDto("senha123"), admin))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot deactivate the last active ADMIN.");

        assertThat(admin.isActive()).isTrue();
    }
}