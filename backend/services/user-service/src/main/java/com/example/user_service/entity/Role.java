package com.example.user_service.entity;

public enum Role {
    STUDENT("student"),
    INSTRUCTOR("instructor"),
    ADMINISTRATOR("admin");

    /** Tên role tương ứng bên Keycloak realm (realm-lms.json dùng chữ thường). */
    private final String realmName;

    Role(String realmName) {
        this.realmName = realmName;
    }

    public String realmName() {
        return realmName;
    }

    /** Đổi role trong JWT (realm_access.roles) sang Role của service; null nếu không nhận diện được. */
    public static Role fromRealmName(String name) {
        if (name == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.realmName.equalsIgnoreCase(name) || role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}
