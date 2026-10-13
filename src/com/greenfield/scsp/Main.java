package com.greenfield.scsp;

import com.greenfield.scsp.domain.Role;
import com.greenfield.scsp.presentation.AdminMenu;
import com.greenfield.scsp.presentation.ConsoleApp;
import com.greenfield.scsp.presentation.ConsoleIO;
import com.greenfield.scsp.presentation.CouncilEmployeeMenu;
import com.greenfield.scsp.presentation.MaintenanceMenu;
import com.greenfield.scsp.presentation.ResidentMenu;
import com.greenfield.scsp.presentation.RoleMenu;

import java.time.Clock;
import java.util.Map;
import java.util.Scanner;

/**
 * Program entry point for the Smart Community Services Platform prototype.
 * <p>
 * Module implemented: User Authentication and Community Issue Management
 * (FR-01 to FR-09, FR-14, FR-15, FR-18 to FR-20).
 */
public final class Main {

    private Main() {
        // utility class
    }

    /**
     * Builds the application and starts the console client.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        AppContext ctx = new AppContext(Clock.systemDefaultZone());
        ctx.seedDemoData();

        ConsoleIO io = new ConsoleIO(new Scanner(System.in), System.out);
        Map<Role, RoleMenu> menus = Map.of(
                Role.RESIDENT, new ResidentMenu(io, ctx.requestService, ctx.inApp),
                Role.COUNCIL_EMPLOYEE, new CouncilEmployeeMenu(io, ctx.requestService, ctx.clock),
                Role.MAINTENANCE_OFFICER, new MaintenanceMenu(io, ctx.requestService),
                Role.ADMINISTRATOR, new AdminMenu(io, ctx.adminService, ctx.audit));

        new ConsoleApp(io, ctx.auth, menus).run();
    }
}
