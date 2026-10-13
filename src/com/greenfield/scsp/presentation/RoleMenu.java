package com.greenfield.scsp.presentation;

import com.greenfield.scsp.service.Session;

/**
 * A role-specific dashboard (wireframes W-02, W-04).
 * <p>
 * The main application calls {@link #show(Session)} without knowing which
 * role it is dealing with: polymorphism replaces a large if/else on role.
 */
public interface RoleMenu {

    /**
     * Runs the dashboard loop until the user signs out.
     *
     * @param session the signed-in user's session
     */
    void show(Session session);
}
