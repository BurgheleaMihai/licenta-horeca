package com.licenta.horeca.employee.service;

import com.licenta.horeca.employee.dto.user.CreateUserRequest;
import com.licenta.horeca.employee.dto.user.UpdateUserRequest;
import com.licenta.horeca.employee.dto.user.UserResponse;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.entity.User;
import com.licenta.horeca.user.enums.RoleType;
import com.licenta.horeca.user.repository.RoleRepository;
import com.licenta.horeca.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeManagementServiceTest {

    private static final Long EMPLOYEE_ID =
            1L;

    private static final String EMPLOYEE_NAME =
            "Ospatar Test";

    private static final String UPDATED_EMPLOYEE_NAME =
            "Ospatar Actualizat";

    private static final String EMPLOYEE_EMAIL =
            "waiter@test.com";

    private static final String UPDATED_EMPLOYEE_EMAIL =
            "waiter.updated@test.com";

    private static final String EMPLOYEE_PASSWORD =
            "Parola123!";

    private static final String ENCODED_PASSWORD =
            "$2a$10$encodedPasswordForTesting";

    private static final String DUPLICATE_EMAIL_MESSAGE =
            "Exista deja un utilizator cu acest email.";

    private static final String USED_EMAIL_MESSAGE =
            "Emailul este deja utilizat.";

    private static final String USER_NOT_FOUND_MESSAGE =
            "Utilizatorul nu exista.";

    private static final String ROLE_NOT_FOUND_MESSAGE =
            "Rolul nu exista.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmployeeShiftService employeeShiftService;

    @InjectMocks
    private EmployeeManagementService employeeManagementService;

    @Test
    void getAllUsersShouldReturnMappedUsers() {

        Role waiterRole =
                new Role(RoleType.WAITER);

        Role managerRole =
                new Role(RoleType.MANAGER);

        User waiter =
                createUser(
                        1L,
                        "Ospatar Test",
                        "waiter@test.com",
                        waiterRole,
                        true
                );

        User manager =
                createUser(
                        2L,
                        "Manager Test",
                        "manager@test.com",
                        managerRole,
                        false
                );

        when(userRepository.findAll())
                .thenReturn(
                        List.of(
                                waiter,
                                manager
                        )
                );

        List<UserResponse> response =
                employeeManagementService
                        .getAllUsers();

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "Ospatar Test",
                response.get(0).getFullName()
        );

        assertEquals(
                RoleType.WAITER,
                response.get(0).getRole()
        );

        assertTrue(
                response.get(0).isActive()
        );

        assertEquals(
                "Manager Test",
                response.get(1).getFullName()
        );

        assertEquals(
                RoleType.MANAGER,
                response.get(1).getRole()
        );

        assertFalse(
                response.get(1).isActive()
        );
    }

    @Test
    void getUserByIdShouldReturnMappedUser() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        when(userRepository.findById(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        UserResponse response =
                employeeManagementService
                        .getUserById(
                                EMPLOYEE_ID
                        );

        assertEquals(
                EMPLOYEE_ID,
                response.getId()
        );

        assertEquals(
                EMPLOYEE_NAME,
                response.getFullName()
        );

        assertEquals(
                EMPLOYEE_EMAIL,
                response.getEmail()
        );

        assertEquals(
                RoleType.WAITER,
                response.getRole()
        );

        assertTrue(
                response.isActive()
        );
    }

    @Test
    void getUserByIdWithMissingUserShouldThrowException() {

        when(userRepository.findById(EMPLOYEE_ID))
                .thenReturn(
                        Optional.empty()
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .getUserById(
                                                EMPLOYEE_ID
                                        )
                );

        assertEquals(
                USER_NOT_FOUND_MESSAGE,
                exception.getMessage()
        );
    }

    @Test
    void createUserShouldSaveEncodedActiveUser() {

        Role role =
                new Role(RoleType.WAITER);

        CreateUserRequest request =
                createCreateRequest();

        when(userRepository.findByEmail(EMPLOYEE_EMAIL))
                .thenReturn(
                        Optional.empty()
                );

        when(roleRepository.findByName(RoleType.WAITER))
                .thenReturn(
                        Optional.of(role)
                );

        when(passwordEncoder.encode(EMPLOYEE_PASSWORD))
                .thenReturn(
                        ENCODED_PASSWORD
                );

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User savedUser =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            savedUser,
                            "id",
                            EMPLOYEE_ID
                    );

                    return savedUser;
                });

        UserResponse response =
                employeeManagementService
                        .createUser(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(userRepository)
                .save(
                        userCaptor.capture()
                );

        User savedUser =
                userCaptor.getValue();

        assertEquals(
                EMPLOYEE_NAME,
                savedUser.getFullName()
        );

        assertEquals(
                EMPLOYEE_EMAIL,
                savedUser.getEmail()
        );

        assertEquals(
                ENCODED_PASSWORD,
                savedUser.getPassword()
        );

        assertSame(
                role,
                savedUser.getRole()
        );

        assertTrue(
                savedUser.isActive()
        );

        assertEquals(
                EMPLOYEE_ID,
                response.getId()
        );

        assertEquals(
                RoleType.WAITER,
                response.getRole()
        );

        assertTrue(
                response.isActive()
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForRoleChange(
                any(Long.class)
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForAccountDeactivation(
                any(Long.class)
        );
    }

    @Test
    void createUserWithDuplicateEmailShouldThrowException() {

        CreateUserRequest request =
                createCreateRequest();

        when(userRepository.findByEmail(EMPLOYEE_EMAIL))
                .thenReturn(
                        Optional.of(
                                new User()
                        )
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .createUser(request)
                );

        assertEquals(
                DUPLICATE_EMAIL_MESSAGE,
                exception.getMessage()
        );

        verify(
                roleRepository,
                never()
        ).findByName(
                any(RoleType.class)
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                any(String.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void createUserWithMissingRoleShouldThrowException() {

        CreateUserRequest request =
                createCreateRequest();

        when(userRepository.findByEmail(EMPLOYEE_EMAIL))
                .thenReturn(
                        Optional.empty()
                );

        when(roleRepository.findByName(RoleType.WAITER))
                .thenReturn(
                        Optional.empty()
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .createUser(request)
                );

        assertEquals(
                ROLE_NOT_FOUND_MESSAGE,
                exception.getMessage()
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                any(String.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void updateUserShouldSaveUpdatedData() {

        Role oldRole =
                new Role(RoleType.WAITER);

        Role newRole =
                new Role(RoleType.MANAGER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        oldRole,
                        true
                );

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setFullName(
                UPDATED_EMPLOYEE_NAME
        );

        request.setEmail(
                UPDATED_EMPLOYEE_EMAIL
        );

        request.setRole(
                RoleType.MANAGER
        );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(
                userRepository.findByEmail(
                        UPDATED_EMPLOYEE_EMAIL
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                roleRepository.findByName(
                        RoleType.MANAGER
                )
        ).thenReturn(
                Optional.of(newRole)
        );

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                employeeManagementService
                        .updateUser(
                                EMPLOYEE_ID,
                                request
                        );

        verify(employeeShiftService)
                .closeOpenShiftsForRoleChange(
                        EMPLOYEE_ID
                );

        verify(userRepository)
                .save(user);

        assertEquals(
                UPDATED_EMPLOYEE_NAME,
                user.getFullName()
        );

        assertEquals(
                UPDATED_EMPLOYEE_EMAIL,
                user.getEmail()
        );

        assertSame(
                newRole,
                user.getRole()
        );

        assertEquals(
                UPDATED_EMPLOYEE_NAME,
                response.getFullName()
        );

        assertEquals(
                UPDATED_EMPLOYEE_EMAIL,
                response.getEmail()
        );

        assertEquals(
                RoleType.MANAGER,
                response.getRole()
        );
    }

    @Test
    void updateUserWithoutRoleChangeShouldNotCloseShifts() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setFullName(
                UPDATED_EMPLOYEE_NAME
        );

        request.setEmail(
                UPDATED_EMPLOYEE_EMAIL
        );

        request.setRole(
                RoleType.WAITER
        );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(
                userRepository.findByEmail(
                        UPDATED_EMPLOYEE_EMAIL
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                roleRepository.findByName(
                        RoleType.WAITER
                )
        ).thenReturn(
                Optional.of(role)
        );

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                employeeManagementService
                        .updateUser(
                                EMPLOYEE_ID,
                                request
                        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForRoleChange(
                any(Long.class)
        );

        verify(userRepository)
                .save(user);

        assertEquals(
                UPDATED_EMPLOYEE_NAME,
                response.getFullName()
        );

        assertEquals(
                RoleType.WAITER,
                response.getRole()
        );
    }

    @Test
    void updateUserWithUsedEmailShouldThrowException() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setFullName(
                UPDATED_EMPLOYEE_NAME
        );

        request.setEmail(
                UPDATED_EMPLOYEE_EMAIL
        );

        request.setRole(
                RoleType.MANAGER
        );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(
                userRepository.findByEmail(
                        UPDATED_EMPLOYEE_EMAIL
                )
        ).thenReturn(
                Optional.of(
                        new User()
                )
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .updateUser(
                                                EMPLOYEE_ID,
                                                request
                                        )
                );

        assertEquals(
                USED_EMAIL_MESSAGE,
                exception.getMessage()
        );

        verify(
                roleRepository,
                never()
        ).findByName(
                any(RoleType.class)
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForRoleChange(
                any(Long.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void updateUserWithMissingUserShouldThrowException() {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setFullName(
                UPDATED_EMPLOYEE_NAME
        );

        request.setEmail(
                UPDATED_EMPLOYEE_EMAIL
        );

        request.setRole(
                RoleType.MANAGER
        );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.empty()
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .updateUser(
                                                EMPLOYEE_ID,
                                                request
                                        )
                );

        assertEquals(
                USER_NOT_FOUND_MESSAGE,
                exception.getMessage()
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void changeStatusShouldSaveNewStatus() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        employeeManagementService.changeStatus(
                EMPLOYEE_ID,
                false
        );

        assertFalse(
                user.isActive()
        );

        verify(employeeShiftService)
                .closeOpenShiftsForAccountDeactivation(
                        EMPLOYEE_ID
                );

        verify(userRepository)
                .save(user);
    }

    @Test
    void activateUserShouldNotCloseShifts() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        false
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        employeeManagementService.changeStatus(
                EMPLOYEE_ID,
                true
        );

        assertTrue(
                user.isActive()
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForAccountDeactivation(
                any(Long.class)
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void changeStatusShouldDoNothingWhenStatusIsUnchanged() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        employeeManagementService.changeStatus(
                EMPLOYEE_ID,
                true
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForAccountDeactivation(
                any(Long.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );

        assertTrue(
                user.isActive()
        );
    }

    @Test
    void changeStatusWithMissingUserShouldThrowException() {

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.empty()
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .changeStatus(
                                                EMPLOYEE_ID,
                                                false
                                        )
                );

        assertEquals(
                USER_NOT_FOUND_MESSAGE,
                exception.getMessage()
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForAccountDeactivation(
                any(Long.class)
        );
    }

    @Test
    void changeRoleShouldSaveNewRole() {

        Role oldRole =
                new Role(RoleType.WAITER);

        Role newRole =
                new Role(RoleType.MANAGER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        oldRole,
                        true
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(
                roleRepository.findByName(
                        RoleType.MANAGER
                )
        ).thenReturn(
                Optional.of(newRole)
        );

        employeeManagementService.changeRole(
                EMPLOYEE_ID,
                RoleType.MANAGER
        );

        verify(employeeShiftService)
                .closeOpenShiftsForRoleChange(
                        EMPLOYEE_ID
                );

        assertSame(
                newRole,
                user.getRole()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void changeRoleShouldDoNothingWhenRoleIsUnchanged() {

        Role role =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        role,
                        true
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(roleRepository.findByName(RoleType.WAITER))
                .thenReturn(
                        Optional.of(role)
                );

        employeeManagementService.changeRole(
                EMPLOYEE_ID,
                RoleType.WAITER
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForRoleChange(
                any(Long.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );

        assertSame(
                role,
                user.getRole()
        );
    }

    @Test
    void changeRoleWithMissingRoleShouldThrowException() {

        Role oldRole =
                new Role(RoleType.WAITER);

        User user =
                createUser(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        oldRole,
                        true
                );

        when(userRepository.findByIdForUpdate(EMPLOYEE_ID))
                .thenReturn(
                        Optional.of(user)
                );

        when(roleRepository.findByName(RoleType.MANAGER))
                .thenReturn(
                        Optional.empty()
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                employeeManagementService
                                        .changeRole(
                                                EMPLOYEE_ID,
                                                RoleType.MANAGER
                                        )
                );

        assertEquals(
                ROLE_NOT_FOUND_MESSAGE,
                exception.getMessage()
        );

        verify(
                employeeShiftService,
                never()
        ).closeOpenShiftsForRoleChange(
                any(Long.class)
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    private CreateUserRequest createCreateRequest() {

        CreateUserRequest request =
                new CreateUserRequest();

        request.setFullName(
                EMPLOYEE_NAME
        );

        request.setEmail(
                EMPLOYEE_EMAIL
        );

        request.setPassword(
                EMPLOYEE_PASSWORD
        );

        request.setRole(
                RoleType.WAITER
        );

        return request;
    }

    private User createUser(
            Long id,
            String fullName,
            String email,
            Role role,
            boolean active
    ) {
        User user =
                new User();

        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );

        user.setFullName(
                fullName
        );

        user.setEmail(
                email
        );

        user.setPassword(
                ENCODED_PASSWORD
        );

        user.setRole(
                role
        );

        user.setActive(
                active
        );

        return user;
    }
}