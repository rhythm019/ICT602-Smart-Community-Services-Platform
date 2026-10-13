package com.greenfield.scsp;

import com.greenfield.scsp.domain.IssueCategory;
import com.greenfield.scsp.domain.MaintenanceOfficer;
import com.greenfield.scsp.domain.Priority;
import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.Role;
import com.greenfield.scsp.domain.ServiceRequest;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.exception.AuthenticationException;
import com.greenfield.scsp.exception.AuthorizationException;
import com.greenfield.scsp.exception.BusinessRuleException;
import com.greenfield.scsp.service.Session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained automated test suite (no external libraries needed).
 * <p>
 * Each test id matches a row in the Test Plan (report section 9), and each
 * test names the requirement or business rule it verifies.
 * Run: {@code ./run-tests.sh} or {@code run-tests.bat}.
 */
public final class TestRunner {

    private static final String PW = AppContext.DEMO_PASSWORD;
    private static final String RESIDENT = "resident@greenfield.vic.gov.au";
    private static final String EMPLOYEE = "employee@greenfield.vic.gov.au";
    private static final String OFFICER = "maintenance@greenfield.vic.gov.au";
    private static final String ADMIN = "admin@greenfield.vic.gov.au";

    private final List<String> failures = new ArrayList<>();
    private int passed;

    /** Functional interface for one test body. */
    @FunctionalInterface
    private interface TestBody {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.runAll();
        System.exit(runner.failures.isEmpty() ? 0 : 1);
    }

    /** Executes every test and prints a summary. */
    private void runAll() {
        System.out.println("Smart Community Services Platform - automated tests");
        System.out.println("-".repeat(78));

        test("UT-01", "FR-02 valid login returns a session with the correct role", () -> {
            AppContext ctx = freshContext();
            Session s = ctx.auth.login(RESIDENT, PW);
            check(s.user().getRole() == Role.RESIDENT, "role should be RESIDENT");
        });

        test("UT-02", "FR-02 wrong password is rejected with a generic message", () -> {
            AppContext ctx = freshContext();
            AuthenticationException ex = expect(AuthenticationException.class,
                    () -> ctx.auth.login(RESIDENT, "wrongPass9"));
            check(ex.getMessage().equals("Invalid email or password."), "message must be generic");
        });

        test("UT-03", "FR-02 unknown email gives the same generic message", () -> {
            AppContext ctx = freshContext();
            AuthenticationException ex = expect(AuthenticationException.class,
                    () -> ctx.auth.login("nobody@x.com", PW));
            check(ex.getMessage().equals("Invalid email or password."), "message must be generic");
        });

        test("UT-04", "NFR-01 account locks after 5 failures and unlocks after 15 min", () -> {
            MutableClock clock = new MutableClock();
            AppContext ctx = new AppContext(clock);
            ctx.seedDemoData();
            for (int i = 0; i < User.MAX_FAILED_ATTEMPTS; i++) {
                expect(AuthenticationException.class, () -> ctx.auth.login(RESIDENT, "badPass99"));
            }
            AuthenticationException locked = expect(AuthenticationException.class,
                    () -> ctx.auth.login(RESIDENT, PW));
            check(locked.getMessage().contains("Too many"), "correct password must be refused while locked");
            clock.advance(Duration.ofMinutes(16));
            check(ctx.auth.login(RESIDENT, PW) != null, "login should work after the lock expires");
        });

        test("UT-05", "NFR-02 passwords are stored as salted hashes, never plain text", () -> {
            AppContext ctx = freshContext();
            User u = ctx.users.findByEmail(RESIDENT).orElseThrow();
            check(!u.getPasswordHash().contains(PW), "hash must not contain the password");
            check(!ctx.hasher.hash(PW).equals(ctx.hasher.hash(PW)), "two hashes must differ (random salt)");
        });

        test("UT-06", "FR-01 registration enforces the password policy", () -> {
            AppContext ctx = freshContext();
            expect(BusinessRuleException.class,
                    () -> ctx.auth.registerResident("Tom", "tom@mail.com", "short1", "", "1 A St, Greenfield"));
            expect(BusinessRuleException.class,
                    () -> ctx.auth.registerResident("Tom", "tom@mail.com", "lettersonly", "", "1 A St"));
        });

        test("UT-07", "FR-01 duplicate email is rejected", () -> {
            AppContext ctx = freshContext();
            expect(BusinessRuleException.class,
                    () -> ctx.auth.registerResident("Dup", RESIDENT, "Password123", "", "x"));
        });

        test("UT-08", "FR-01 new resident can register then sign in", () -> {
            AppContext ctx = freshContext();
            ctx.auth.registerResident("Mia Chen", "mia@mail.com", "Secure123", "0400 000 000",
                    "3 Elm St, Greenfield");
            check(ctx.auth.login("MIA@mail.com", "Secure123").user().getRole() == Role.RESIDENT,
                    "email login should be case-insensitive");
        });

        test("UT-09", "FR-04/FR-05 resident reports an issue and receives an id and coordinates", () -> {
            AppContext ctx = freshContext();
            Session s = ctx.auth.login(RESIDENT, PW);
            ServiceRequest r = ctx.requestService.reportIssue(s, IssueCategory.GRAFFITI,
                    "Graffiti on the library wall facing the park.", "1 Library Lane, Greenfield");
            check(r.getRequestId().startsWith("SR-"), "id format");
            check(r.getStatus() == RequestStatus.SUBMITTED, "status SUBMITTED");
            check(r.getLocation().latitude() != 0, "GIS coordinates captured");
        });

        test("UT-10", "BR-02 issue without a valid description is rejected", () -> {
            AppContext ctx = freshContext();
            Session s = ctx.auth.login(RESIDENT, PW);
            BusinessRuleException ex = expect(BusinessRuleException.class,
                    () -> ctx.requestService.reportIssue(s, IssueCategory.WASTE, "bin", "2 Oak St, Greenfield"));
            check(ex.getRuleId().equals("BR-02"), "rule BR-02");
        });

        test("UT-11", "BR-02 location outside the council area is rejected", () -> {
            AppContext ctx = freshContext();
            Session s = ctx.auth.login(RESIDENT, PW);
            expect(BusinessRuleException.class, () -> ctx.requestService.reportIssue(s,
                    IssueCategory.WASTE, "Overflowing public bin at the corner.", "10 Collins St, Melbourne"));
        });

        test("UT-12", "FR-14 reporting sends email, SMS and in-app confirmations", () -> {
            AppContext ctx = freshContext();
            int emailsBefore = ctx.email.getOutbox().size();
            int smsBefore = ctx.sms.getOutbox().size();
            Session s = ctx.auth.login(RESIDENT, PW);
            ctx.requestService.reportIssue(s, IssueCategory.PARKS,
                    "Fallen tree branch blocking the footpath.", "Greenfield Park, Greenfield");
            check(ctx.email.getOutbox().size() == emailsBefore + 1, "one email");
            check(ctx.sms.getOutbox().size() == smsBefore + 1, "one SMS");
            check(!ctx.inApp.inboxFor(s.user().getUserId()).isEmpty(), "in-app message");
        });

        test("UT-13", "BR-06 a resident cannot change priority", () -> {
            AppContext ctx = freshContext();
            Session s = ctx.auth.login(RESIDENT, PW);
            expect(AuthorizationException.class,
                    () -> ctx.requestService.prioritise(s, "SR-1002", Priority.URGENT));
            check(ctx.audit.latest().action().equals("ACCESS_DENIED"), "denial is audited");
        });

        test("UT-14", "FR-07/FR-08 employee prioritises and assigns; team is notified", () -> {
            AppContext ctx = freshContext();
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            ctx.requestService.prioritise(emp, "SR-1002", Priority.MEDIUM);
            ServiceRequest r = ctx.requestService.assign(emp, "SR-1002", "T-ROADS");
            check(r.getStatus() == RequestStatus.ASSIGNED, "status ASSIGNED");
            boolean officerNotified = ctx.inApp.inboxFor("U-0003").stream()
                    .anyMatch(n -> n.subject().contains("SR-1002"));
            check(officerNotified, "maintenance officer notified");
        });

        test("UT-15", "FR-08 cannot assign before a priority is set", () -> {
            AppContext ctx = freshContext();
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            expect(BusinessRuleException.class, () -> ctx.requestService.assign(emp, "SR-1002", "T-ROADS"));
        });

        test("UT-16", "BR-03 request cannot be completed before assignment", () -> {
            AppContext ctx = freshContext();
            ServiceRequest r = ctx.requests.findById("SR-1001").orElseThrow();
            BusinessRuleException ex = expect(BusinessRuleException.class, () ->
                    r.updateWorkStatus(RequestStatus.COMPLETED, "done", "U-0003", now(ctx)));
            check(ex.getRuleId().equals("BR-03"), "rule BR-03");
        });

        test("UT-17", "FR-09 officer of another team cannot update the work", () -> {
            AppContext ctx = freshContext();
            Session admin = ctx.auth.login(ADMIN, PW);
            ctx.adminService.createStaff(admin, Role.MAINTENANCE_OFFICER, "Lee Park",
                    "lee@greenfield.vic.gov.au", "Parks2026x", "T-PARKS");
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            ctx.requestService.assign(emp, "SR-1001", "T-ROADS");
            Session parksOfficer = ctx.auth.login("lee@greenfield.vic.gov.au", "Parks2026x");
            expect(AuthorizationException.class, () -> ctx.requestService.updateWork(
                    parksOfficer, "SR-1001", RequestStatus.IN_PROGRESS, "start"));
        });

        test("IT-01", "End-to-end: report > prioritise > assign > start > complete > feedback", () -> {
            AppContext ctx = freshContext();
            Session res = ctx.auth.login(RESIDENT, PW);
            ServiceRequest r = ctx.requestService.reportIssue(res, IssueCategory.ROAD_DAMAGE,
                    "Cracked kerb causing water pooling.", "22 River Rd, Greenfield");
            String id = r.getRequestId();
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            ctx.requestService.prioritise(emp, id, Priority.HIGH);
            ctx.requestService.assign(emp, id, "T-ROADS");
            Session off = ctx.auth.login(OFFICER, PW);
            check(ctx.requestService.myTeamWork(off).stream().anyMatch(x -> x.getRequestId().equals(id)),
                    "work appears in officer list");
            ctx.requestService.updateWork(off, id, RequestStatus.IN_PROGRESS, "Crew on site");
            ctx.requestService.updateWork(off, id, RequestStatus.COMPLETED, "Kerb repaired");
            ctx.requestService.giveFeedback(res, id, 5, "Fast fix, thanks");
            ServiceRequest done = ctx.requestService.viewRequest(res, id);
            check(done.getStatus() == RequestStatus.COMPLETED, "completed");
            check(done.getHistory().size() == 5, "five history entries, got " + done.getHistory().size());
            check(done.getFeedback().rating() == 5, "feedback stored");
            expect(BusinessRuleException.class,
                    () -> ctx.requestService.giveFeedback(res, id, 1, "second try"));
        });

        test("UT-18", "BR-08 feedback only after completion and only once", () -> {
            AppContext ctx = freshContext();
            Session res = ctx.auth.login(RESIDENT, PW);
            expect(BusinessRuleException.class, () -> ctx.requestService.giveFeedback(res, "SR-1001", 4, ""));
        });

        test("UT-19", "State machine blocks invalid transitions", () -> {
            check(!RequestStatus.SUBMITTED.canMoveTo(RequestStatus.COMPLETED), "SUBMITTED->COMPLETED");
            check(!RequestStatus.COMPLETED.canMoveTo(RequestStatus.IN_PROGRESS), "COMPLETED->IN_PROGRESS");
            check(RequestStatus.ASSIGNED.canMoveTo(RequestStatus.IN_PROGRESS), "ASSIGNED->IN_PROGRESS");
        });

        test("UT-20", "FR-06 a resident cannot view another resident's request", () -> {
            AppContext ctx = freshContext();
            ctx.auth.registerResident("Other", "other@mail.com", "Other1234", "", "9 Gum St, Greenfield");
            Session other = ctx.auth.login("other@mail.com", "Other1234");
            expect(AuthorizationException.class, () -> ctx.requestService.viewRequest(other, "SR-1001"));
        });

        test("UT-21", "FR-20 queue lists HIGH priority before unprioritised items", () -> {
            AppContext ctx = freshContext();
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            List<ServiceRequest> q = ctx.requestService.requestQueue(emp, null);
            check(q.get(0).getPriority() == Priority.HIGH, "first item should be HIGH");
        });

        test("UT-22", "BR-07 only administrators can create staff; disabled users cannot sign in", () -> {
            AppContext ctx = freshContext();
            Session emp = ctx.auth.login(EMPLOYEE, PW);
            expect(AuthorizationException.class, () -> ctx.adminService.createStaff(emp,
                    Role.ADMINISTRATOR, "X", "x@x.com", "Password1", ""));
            Session admin = ctx.auth.login(ADMIN, PW);
            ctx.adminService.setActive(admin, "U-0001", false);
            expect(AuthenticationException.class, () -> ctx.auth.login(RESIDENT, PW));
        });

        test("UT-23", "FR-19 audit log records logins and is admin-only", () -> {
            AppContext ctx = freshContext();
            Session res = ctx.auth.login(RESIDENT, PW);
            check(ctx.audit.latest().action().equals("LOGIN_SUCCESS"), "login audited");
            expect(AuthorizationException.class, () -> ctx.audit.viewLog(res));
            Session admin = ctx.auth.login(ADMIN, PW);
            check(!ctx.audit.viewLog(admin).isEmpty(), "admin can read log");
        });

        test("UT-24", "Priority targets make overdue requests detectable", () -> {
            MutableClock clock = new MutableClock();
            AppContext ctx = new AppContext(clock);
            ctx.seedDemoData();
            ServiceRequest high = ctx.requests.findById("SR-1001").orElseThrow();
            check(!high.isOverdue(now(ctx)), "not overdue at start");
            clock.advance(Duration.ofHours(25));
            check(high.isOverdue(now(ctx)), "HIGH is overdue after 25h");
        });

        test("UT-25", "LSP: every User subtype works through the base type", () -> {
            AppContext ctx = freshContext();
            for (User u : ctx.users.findAll()) {
                check(u.getRole() != null, "role defined for " + u.getUserId());
            }
            check(ctx.users.findByEmail(OFFICER).orElseThrow() instanceof MaintenanceOfficer,
                    "officer subtype");
        });

        System.out.println("-".repeat(78));
        System.out.printf("Result: %d passed, %d failed, %d total%n",
                passed, failures.size(), passed + failures.size());
        failures.forEach(f -> System.out.println("FAILED " + f));
    }

    // ---------------------------------------------------------------- helpers

    /** Runs one named test and records the result. */
    private void test(String id, String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.printf("PASS  %-6s %s%n", id, name);
        } catch (Throwable t) {
            failures.add(id + " " + name + " -> " + t.getMessage());
            System.out.printf("FAIL  %-6s %s%n        %s%n", id, name, t);
        }
    }

    /** Builds a new, seeded application on a fixed clock. */
    private static AppContext freshContext() {
        AppContext ctx = new AppContext(Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC));
        ctx.seedDemoData();
        return ctx;
    }

    private static java.time.LocalDateTime now(AppContext ctx) {
        return java.time.LocalDateTime.now(ctx.clock);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Asserts that the action throws the expected exception type. */
    private static <T extends Throwable> T expect(Class<T> type, TestBody action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but nothing was thrown");
    }

    /** Clock whose time can be moved forward, for lock-out and SLA tests. */
    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-10-14T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
