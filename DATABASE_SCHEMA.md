# 数据库设计文档

## 核心表结构

### users - 用户表
```sql
id UUID PRIMARY KEY
username VARCHAR(50) UNIQUE NOT NULL
email VARCHAR(255) UNIQUE NOT NULL
password_hash VARCHAR(255) NOT NULL
first_name VARCHAR(100)
last_name VARCHAR(100)
is_active BOOLEAN DEFAULT true
is_verified BOOLEAN DEFAULT false
last_login_at TIMESTAMP
created_at TIMESTAMP DEFAULT NOW()
```

### roles - 角色表
```sql
id UUID PRIMARY KEY
name VARCHAR(50) UNIQUE NOT NULL
description TEXT
is_default BOOLEAN DEFAULT false
```

### permissions - 权限表
```sql
id UUID PRIMARY KEY
name VARCHAR(100) UNIQUE NOT NULL
resource VARCHAR(50) NOT NULL
action VARCHAR(50) NOT NULL
```

### 关联表
- **user_roles**: user_id + role_id
- **role_permissions**: role_id + permission_id

### 审计表
- **audit_logs**: 记录所有操作变更
- **user_sessions**: 管理用户会话
- **login_attempts**: 登录安全审计

## 迁移规范

- 使用 **Flyway** 管理数据库迁移 (Spring Boot 集成)
- 命名格式: `V{版本号}__{描述}.sql` (如: `V1__Initial_schema.sql`)
- 每个迁移脚本应包含完整的 DDL 语句
- Flyway 自动在 `flyway_schema_history` 表记录迁移历史
- 支持 repeatable 迁移 (`R__{描述}.sql`) 用于视图、存储过程等

## JPA 实体映射

### 用户实体 (User.java)
```java
@Entity
@Table(name = "users")
public class User extends AbstractEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

### 角色实体 (Role.java)
```java
@Entity
@Table(name = "roles")
public class Role extends AbstractEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
```

### 审计功能
- 使用 `@EntityListeners(AuditingEntityListener.class)` 实现自动审计
- `@CreatedDate`, `@LastModifiedDate` 自动填充时间戳
- `@CreatedBy`, `@LastModifiedBy` 记录操作用户
