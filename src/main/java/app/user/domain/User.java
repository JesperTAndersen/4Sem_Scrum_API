package app.user.domain;

import app.shared.domain.IEntity;

import app.security.domain.Role;
import app.utils.PasswordUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User implements IEntity
{
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Getter
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Getter
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Getter
    @Setter
    @Column(name = "hashed_password", nullable = false, unique = false, length = 60)
    private String hashedPassword;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private Role role;

    @Getter
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Getter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User(String firstName, String lastName, String email, String hashedPassword)
    {
        ValidationUtil.validateNotBlank(firstName, "First name");
        ValidationUtil.validateNotBlank(lastName, "Last name");

        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = ValidationUtil.validateEmail(email);
        this.hashedPassword = hashedPassword.trim();
        this.role = Role.EMPLOYEE;
    }

    public void update(String firstName, String lastName)
    {
        ValidationUtil.validateNotBlank(firstName, "First name");
        ValidationUtil.validateNotBlank(lastName, "Last name");

        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public void changeRole(Role newRole)
    {
        ValidationUtil.validateNotNull(newRole, "Role");
        this.role = newRole;
    }

    public void changeEmail(String newEmail)
    {
        this.email = ValidationUtil.validateEmail(newEmail);
    }

    public void changePassword(String newHashedPassword)
    {
        ValidationUtil.validateNotBlank(newHashedPassword, "Password");
        this.hashedPassword = newHashedPassword;
    }

    public boolean verifyPassword(String plainTextPassword)
    {
        return PasswordUtil.verifyPassword(plainTextPassword, this.hashedPassword);
    }

    @PrePersist
    private void onCreate()
    {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate()
    {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }

}
