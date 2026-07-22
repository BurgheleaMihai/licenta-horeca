package com.licenta.horeca.employee.service;

import com.licenta.horeca.employee.dto.user.CreateUserRequest;
import com.licenta.horeca.employee.dto.user.UpdateUserRequest;
import com.licenta.horeca.employee.dto.user.UserResponse;
import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.entity.User;
import com.licenta.horeca.user.enums.RoleType;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.user.repository.RoleRepository;
import com.licenta.horeca.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmployeeShiftService employeeShiftService;

    public EmployeeManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmployeeShiftService employeeShiftService
    ) {
        this.userRepository =
                userRepository;

        this.roleRepository =
                roleRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.employeeShiftService =
                employeeShiftService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(
            Long id
    ) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Utilizatorul nu exista."
                                )
                        );

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request
    ) {
        if (userRepository
                .findByEmail(request.getEmail())
                .isPresent()) {
            throw new BusinessException(
                    "Exista deja un utilizator cu acest email."
            );
        }

        Role role =
                roleRepository
                        .findByName(request.getRole())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Rolul nu exista."
                                )
                        );

        User user =
                new User();

        user.setFullName(
                request.getFullName()
        );

        user.setEmail(
                request.getEmail()
        );

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(role);

        user.setActive(true);

        User savedUser =
                userRepository.save(user);

        return mapToResponse(savedUser);
    }

    /*
     * Actualizează numele, emailul și rolul utilizatorului.
     *
     * Dacă rolul se schimbă, turele asociate rolului vechi
     * sunt finalizate înainte de salvarea noului rol.
     */
    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request
    ) {
        User user =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Utilizatorul nu exista."
                                )
                        );

        boolean emailChanged =
                !user.getEmail()
                        .equalsIgnoreCase(
                                request.getEmail()
                        );

        if (emailChanged
                && userRepository
                .findByEmail(request.getEmail())
                .isPresent()) {
            throw new BusinessException(
                    "Emailul este deja utilizat."
            );
        }

        Role role =
                roleRepository
                        .findByName(request.getRole())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Rolul nu exista."
                                )
                        );

        RoleType currentRole =
                user.getRole().getName();

        RoleType newRole =
                role.getName();

        if (currentRole != newRole) {
            employeeShiftService
                    .closeOpenShiftsForRoleChange(
                            user.getId()
                    );
        }

        user.setFullName(
                request.getFullName()
        );

        user.setEmail(
                request.getEmail()
        );

        user.setRole(role);

        User updatedUser =
                userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    /*
     * Activează sau dezactivează contul.
     *
     * Dezactivarea închide tura activă și anulează
     * programările viitoare ale angajatului.
     */
    @Transactional
    public void changeStatus(
            Long id,
            boolean active
    ) {
        User user =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Utilizatorul nu exista."
                                )
                        );

        if (user.isActive() == active) {
            return;
        }

        if (!active) {
            employeeShiftService
                    .closeOpenShiftsForAccountDeactivation(
                            user.getId()
                    );
        }

        user.setActive(active);

        userRepository.save(user);
    }

    /*
     * Schimbă direct rolul utilizatorului.
     */
    @Transactional
    public void changeRole(
            Long id,
            RoleType roleType
    ) {
        User user =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Utilizatorul nu exista."
                                )
                        );

        Role role =
                roleRepository
                        .findByName(roleType)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Rolul nu exista."
                                )
                        );

        RoleType currentRole =
                user.getRole().getName();

        if (currentRole == roleType) {
            return;
        }

        employeeShiftService
                .closeOpenShiftsForRoleChange(
                        user.getId()
                );

        user.setRole(role);

        userRepository.save(user);
    }

    private UserResponse mapToResponse(
            User user
    ) {
        UserResponse response =
                new UserResponse();

        response.setId(
                user.getId()
        );

        response.setFullName(
                user.getFullName()
        );

        response.setEmail(
                user.getEmail()
        );

        response.setRole(
                user.getRole().getName()
        );

        response.setActive(
                user.isActive()
        );

        return response;
    }
}