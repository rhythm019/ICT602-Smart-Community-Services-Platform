package com.greenfield.scsp;

import com.greenfield.scsp.domain.Administrator;
import com.greenfield.scsp.domain.CouncilEmployee;
import com.greenfield.scsp.domain.IssueCategory;
import com.greenfield.scsp.domain.MaintenanceOfficer;
import com.greenfield.scsp.domain.MaintenanceTeam;
import com.greenfield.scsp.domain.Priority;
import com.greenfield.scsp.domain.Resident;
import com.greenfield.scsp.integration.EmailChannel;
import com.greenfield.scsp.integration.GisService;
import com.greenfield.scsp.integration.InAppChannel;
import com.greenfield.scsp.integration.MockGisService;
import com.greenfield.scsp.integration.PasswordHasher;
import com.greenfield.scsp.integration.Pbkdf2PasswordHasher;
import com.greenfield.scsp.integration.SmsChannel;
import com.greenfield.scsp.repository.InMemoryMaintenanceTeamRepository;
import com.greenfield.scsp.repository.InMemoryServiceRequestRepository;
import com.greenfield.scsp.repository.InMemoryUserRepository;
import com.greenfield.scsp.repository.MaintenanceTeamRepository;
import com.greenfield.scsp.repository.ServiceRequestRepository;
import com.greenfield.scsp.repository.UserRepository;
import com.greenfield.scsp.service.AuditService;
import com.greenfield.scsp.service.AuthenticationService;
import com.greenfield.scsp.service.NotificationService;
import com.greenfield.scsp.service.ServiceRequestService;
import com.greenfield.scsp.service.Session;
import com.greenfield.scsp.service.UserAdministrationService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Composition root: creates every object once and wires the layers together
 * through their interfaces (Dependency Injection by constructor).
 * <p>
 * Swapping the in-memory repositories for database ones, or the mock GIS for
 * a real provider, only requires changes in this class.
 */
public class AppContext {

    /** Password used by all seeded demo accounts. */
    public static final String DEMO_PASSWORD = "Greenfield1";

    public final Clock clock;
    public final UserRepository users;
    public final ServiceRequestRepository requests;
    public final MaintenanceTeamRepository teams;
    public final PasswordHasher hasher;
    public final GisService gis;
    public final EmailChannel email;
    public final SmsChannel sms;
    public final InAppChannel inApp;
    public final AuditService audit;
    public final NotificationService notifications;
    public final AuthenticationService auth;
    public final ServiceRequestService requestService;
    public final UserAdministrationService adminService;

    /**
     * Builds the object graph.
     *
     * @param clock time source; the real clock in production, a fixed clock in tests
     */
    public AppContext(Clock clock) {
        this.clock = clock;
        users = new InMemoryUserRepository();
        requests = new InMemoryServiceRequestRepository();
        teams = new InMemoryMaintenanceTeamRepository();
        hasher = new Pbkdf2PasswordHasher();
        gis = new MockGisService();
        email = new EmailChannel();
        sms = new SmsChannel();
        inApp = new InAppChannel();
        audit = new AuditService(clock);
        notifications = new NotificationService(List.of(email, sms, inApp), clock);
        auth = new AuthenticationService(users, hasher, audit, clock);
        requestService = new ServiceRequestService(requests, teams, gis, notifications, audit, clock);
        adminService = new UserAdministrationService(users, teams, hasher, auth, audit);
    }

    /**
     * Loads demo teams, one account per role and two sample requests, so the
     * teaching team can try every feature immediately.
     */
    public void seedDemoData() {
        String hash = hasher.hash(DEMO_PASSWORD);

        MaintenanceTeam roads = new MaintenanceTeam("T-ROADS", "Roads Crew");
        MaintenanceTeam waste = new MaintenanceTeam("T-WASTE", "Waste Services");
        MaintenanceTeam parks = new MaintenanceTeam("T-PARKS", "Parks and Gardens");
        teams.save(roads);
        teams.save(waste);
        teams.save(parks);

        Resident resident = new Resident("U-0001", "Aisha Khan", "resident@greenfield.vic.gov.au",
                hash, "0412 345 678", "12 Wattle St, Greenfield");
        CouncilEmployee employee = new CouncilEmployee("U-0002", "Daniel Lee",
                "employee@greenfield.vic.gov.au", hash, "Infrastructure");
        MaintenanceOfficer officer = new MaintenanceOfficer("U-0003", "Sam Taylor",
                "maintenance@greenfield.vic.gov.au", hash);
        Administrator admin = new Administrator("U-0004", "Priya Nair",
                "admin@greenfield.vic.gov.au", hash);
        roads.addMember(officer);
        users.save(resident);
        users.save(employee);
        users.save(officer);
        users.save(admin);

        // Two sample requests so the queue is not empty on first run.
        Session residentSession = new Session(resident, LocalDateTime.now(clock));
        Session employeeSession = new Session(employee, LocalDateTime.now(clock));
        var pothole = requestService.reportIssue(residentSession, IssueCategory.ROAD_DAMAGE,
                "Large pothole near the school crossing, cars swerving.", "45 Station Rd, Greenfield");
        requestService.reportIssue(residentSession, IssueCategory.STREET_LIGHTING,
                "Street light flickering all night outside number 8.", "8 Banksia Ave, Greenfield");
        requestService.prioritise(employeeSession, pothole.getRequestId(), Priority.HIGH);
    }
}
