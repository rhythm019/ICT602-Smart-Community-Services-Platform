package com.greenfield.scsp.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User roles used for role-based access control (FR-03).
 * <p>
 * Each role owns a fixed set of {@link Permission}s, so access rules live in
 * one place instead of being repeated across screens (NIST RBAC model).
 */
public enum Role {
    RESIDENT(EnumSet.of(
            Permission.REPORT_ISSUE,
            Permission.VIEW_OWN_REQUESTS,
            Permission.GIVE_FEEDBACK)),

    COUNCIL_EMPLOYEE(EnumSet.of(
            Permission.VIEW_ALL_REQUESTS,
            Permission.PRIORITISE_REQUEST,
            Permission.ASSIGN_WORK)),

    MAINTENANCE_OFFICER(EnumSet.of(
            Permission.UPDATE_WORK_STATUS)),

    ADMINISTRATOR(EnumSet.of(
            Permission.VIEW_ALL_REQUESTS,
            Permission.MANAGE_USERS,
            Permission.VIEW_AUDIT_LOG));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    /**
     * Checks whether this role grants a permission.
     *
     * @param permission the permission being requested
     * @return true when the role includes the permission
     */
    public boolean grants(Permission permission) {
        return permissions.contains(permission);
    }
}
