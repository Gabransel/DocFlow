package Gabransel.DocFlow.controllers;


import Gabransel.DocFlow.dto.*;
import Gabransel.DocFlow.security.UserPrincipal;
import Gabransel.DocFlow.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponseDto> findAll(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponseDto findById(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return userService.findById(id, principal.getUser());
    }

    @PatchMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateUserDto dto,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return userService.update(id, dto, principal.getUser());
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable Long id,
                               @RequestBody @Valid ChangePasswordDto dto,
                               @AuthenticationPrincipal UserPrincipal principal) {
        userService.changePassword(id, dto, principal.getUser());
    }

    @PatchMapping("/{id}/role")
    public UserResponseDto updateRole(@PathVariable Long id,
                                      @RequestBody @Valid UpdateRoleDto dto,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return userService.updateRole(id, dto, principal.getUser());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwnUser(@RequestBody @Valid DeleteOwnUserDto dto,
                              @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteOwnUser(dto, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id,
                           @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(id, principal.getUser());
    }
}