package com.kirochat.tests;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 *  KIROCHAT - AUTOMATION API TEST
 * ============================================================================
 *  Test Case ID : TC07
 *  Tên          : Kiểm thử bảo mật API truy cập tin nhắn hội thoại
 *                 không có token xác thực
 *  Loại         : White-box Testing (API)
 *  Công nghệ   : Java + JUnit 5 + Rest-Assured + Extent Report
 * ============================================================================
 *  Mô tả:
 *    Xác minh rằng API lấy tin nhắn hội thoại của KiroChat thực thi đúng
 *    cơ chế bảo mật OAuth2/JWT. Khi gọi API mà KHÔNG có header
 *    Authorization (Bearer token), hệ thống phải từ chối với HTTP 401.
 *    Đồng thời, khi gửi token hết hạn hoặc sai format, cũng phải trả 401.
 *
 *  Endpoint kiểm thử:
 *    GET /conversations/{conversationId}/messages
 *    (Xem: ConversationMessageResource.java)
 *
 *  Điều kiện tiên quyết:
 *    1. Backend KiroChat đang chạy tại http://localhost:8080
 *    2. Spring Security + OAuth2 Resource Server đã cấu hình
 *
 *  Kết quả mong đợi:
 *    - Không có token → HTTP 401 Unauthorized
 *    - Token rỗng → HTTP 401 Unauthorized
 *    - Token sai format → HTTP 401 Unauthorized
 * ============================================================================
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TC07_UnauthorizedAccessTest {

    // ========================================================================
    // HẰNG SỐ CẤU HÌNH
    // ========================================================================

    /** URL gốc của Backend KiroChat API */
    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Endpoint lấy tin nhắn theo cuộc hội thoại.
     * Route: GET /conversations/{conversationId}/messages
     * Controller: ConversationMessageResource.java
     */
    private static final String MESSAGES_ENDPOINT = "/conversations/{conversationId}/messages";

    /** UUID giả lập cho conversationId (không cần tồn tại thực) */
    private static final String FAKE_CONVERSATION_ID = UUID.randomUUID().toString();

    /** HTTP Status mong đợi khi không có xác thực */
    private static final int EXPECTED_STATUS_UNAUTHORIZED = 401;

    /** Token hết hạn (JWT expired) - dùng để test token invalid */
    private static final String EXPIRED_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTAwMDAwMDAwMH0." +
            "invalid_signature_placeholder";

    /** Token sai format hoàn toàn */
    private static final String MALFORMED_TOKEN = "not-a-valid-jwt-token-at-all";

    // ========================================================================
    // EXTENT REPORT
    // ========================================================================

    /** Instance Extent Reports (báo cáo HTML) */
    private static ExtentReports extentReports;

    /** Test node hiện tại trong báo cáo */
    private ExtentTest extentTest;

    /**
     * Khởi tạo Extent Report TRƯỚC KHI chạy toàn bộ test suite.
     */
    @BeforeAll
    static void setupReport() {
        // Cấu hình Extent Spark Reporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(
                "test-output/TC07_UnauthorizedAccessTest_Report.html"
        );
        sparkReporter.config().setDocumentTitle("KiroChat API Security Test Report");
        sparkReporter.config().setReportName("TC07 - Unauthorized Access Protection");

        // Khởi tạo ExtentReports
        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Ứng dụng", "KiroChat");
        extentReports.setSystemInfo("Môi trường", "Local Development");
        extentReports.setSystemInfo("API Base URL", BASE_URL);
        extentReports.setSystemInfo("Tester", "QA Automation");
        extentReports.setSystemInfo("Bảo mật", "OAuth2 + JWT (Keycloak)");

        // Cấu hình RestAssured
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * Flush Extent Report SAU KHI hoàn tất.
     */
    @AfterAll
    static void tearDownReport() {
        if (extentReports != null) {
            extentReports.flush();
            System.out.println("📊 Extent Report: test-output/TC07_UnauthorizedAccessTest_Report.html");
        }
    }

    // ========================================================================
    // TEST 1: GỌI API KHÔNG CÓ HEADER AUTHORIZATION
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("TC07a - Gọi API lấy tin nhắn KHÔNG có Authorization header → 401")
    void TC07a_noAuthorizationHeader_shouldReturn401() {
        // ---- Tạo test node trong Extent Report ----
        extentTest = extentReports.createTest(
                "TC07a - Không có Authorization header",
                "Gọi GET /conversations/{id}/messages không gửi header Authorization → 401"
        );

        // ================================================================
        // BƯỚC 1: XÂY DỰNG REQUEST KHÔNG CÓ TOKEN
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Xây dựng request KHÔNG có Authorization header");

        String requestUrl = MESSAGES_ENDPOINT.replace(
                "{conversationId}", FAKE_CONVERSATION_ID
        );
        extentTest.log(Status.INFO, "<b>Request:</b> GET " + BASE_URL + requestUrl);
        extentTest.log(Status.INFO, "<b>Headers:</b> (Không có Authorization)");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi GET request không có xác thực");

        Response response = RestAssured.given()
                .pathParam("conversationId", FAKE_CONVERSATION_ID)
                .get(MESSAGES_ENDPOINT);

        // ================================================================
        // BƯỚC 3: GHI LOG RESPONSE VÀO EXTENT REPORT
        // ================================================================
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Ghi nhận Response");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + actualStatusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO, "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        extentTest.log(Status.INFO, "Bước 4: Xác minh Status Code = 401");

        try {
            assertEquals(
                    EXPECTED_STATUS_UNAUTHORIZED,
                    actualStatusCode,
                    "API phải trả về HTTP 401 khi không có Authorization header"
            );

            extentTest.log(Status.PASS,
                    "✅ PASS: API trả về HTTP " + actualStatusCode + " (đúng mong đợi)");

        } catch (AssertionError e) {
            extentTest.log(Status.FAIL,
                    "❌ FAIL: Mong đợi HTTP " + EXPECTED_STATUS_UNAUTHORIZED
                            + " nhưng nhận HTTP " + actualStatusCode);
            extentTest.log(Status.FAIL, "Response: " + responseBody);
            throw e;
        }

        System.out.println("✅ TC07a PASS - Không có token → HTTP " + actualStatusCode);
    }

    // ========================================================================
    // TEST 2: GỌI API VỚI TOKEN RỖNG
    // ========================================================================

    @Test
    @Order(2)
    @DisplayName("TC07b - Gọi API với Authorization header rỗng → 401")
    void TC07b_emptyToken_shouldReturn401() {
        extentTest = extentReports.createTest(
                "TC07b - Token rỗng",
                "Gọi API với header Authorization: Bearer (rỗng) → 401"
        );

        // ================================================================
        // BƯỚC 1: XÂY DỰNG REQUEST VỚI TOKEN RỖNG
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Xây dựng request với token rỗng");
        extentTest.log(Status.INFO, "<b>Header:</b> Authorization: Bearer (empty)");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi request");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer ")
                .pathParam("conversationId", FAKE_CONVERSATION_ID)
                .get(MESSAGES_ENDPOINT);

        // ================================================================
        // BƯỚC 3: LOG RESPONSE
        // ================================================================
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + actualStatusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        try {
            assertEquals(
                    EXPECTED_STATUS_UNAUTHORIZED,
                    actualStatusCode,
                    "API phải trả về HTTP 401 khi token rỗng"
            );
            extentTest.log(Status.PASS, "✅ PASS: Token rỗng → HTTP " + actualStatusCode);
        } catch (AssertionError e) {
            extentTest.log(Status.FAIL, "❌ FAIL: HTTP " + actualStatusCode);
            throw e;
        }

        System.out.println("✅ TC07b PASS - Token rỗng → HTTP " + actualStatusCode);
    }

    // ========================================================================
    // TEST 3: GỌI API VỚI TOKEN SAI FORMAT
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("TC07c - Gọi API với token sai format (không phải JWT) → 401")
    void TC07c_malformedToken_shouldReturn401() {
        extentTest = extentReports.createTest(
                "TC07c - Token sai format",
                "Gọi API với token không phải JWT hợp lệ → 401"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ TOKEN SAI FORMAT
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị token sai format");
        extentTest.log(Status.INFO, "<b>Token:</b> " + MALFORMED_TOKEN);
        extentTest.log(Status.INFO, "Token này không tuân theo cấu trúc JWT (header.payload.signature)");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi request với token sai format");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + MALFORMED_TOKEN)
                .pathParam("conversationId", FAKE_CONVERSATION_ID)
                .get(MESSAGES_ENDPOINT);

        // ================================================================
        // BƯỚC 3: LOG RESPONSE
        // ================================================================
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + actualStatusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        try {
            assertEquals(
                    EXPECTED_STATUS_UNAUTHORIZED,
                    actualStatusCode,
                    "API phải trả về HTTP 401 khi token sai format"
            );
            extentTest.log(Status.PASS, "✅ PASS: Token sai format → HTTP " + actualStatusCode);
        } catch (AssertionError e) {
            extentTest.log(Status.FAIL, "❌ FAIL: HTTP " + actualStatusCode);
            throw e;
        }

        System.out.println("✅ TC07c PASS - Token sai format → HTTP " + actualStatusCode);
    }

    // ========================================================================
    // TEST 4: GỌI API VỚI TOKEN HẾT HẠN
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("TC07d - Gọi API với token JWT đã hết hạn → 401")
    void TC07d_expiredToken_shouldReturn401() {
        extentTest = extentReports.createTest(
                "TC07d - Token JWT hết hạn",
                "Gọi API với JWT có exp trong quá khứ → 401"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ TOKEN HẾT HẠN
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị token JWT đã hết hạn (exp = 1000000000)");
        extentTest.log(Status.INFO, "<b>Token (truncated):</b> " +
                EXPIRED_TOKEN.substring(0, 50) + "...");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi request với token hết hạn");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + EXPIRED_TOKEN)
                .pathParam("conversationId", FAKE_CONVERSATION_ID)
                .get(MESSAGES_ENDPOINT);

        // ================================================================
        // BƯỚC 3: LOG RESPONSE
        // ================================================================
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + actualStatusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO, "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        try {
            // Token hết hạn cũng phải bị từ chối (401)
            assertEquals(
                    EXPECTED_STATUS_UNAUTHORIZED,
                    actualStatusCode,
                    "API phải trả về HTTP 401 khi token JWT đã hết hạn"
            );
            extentTest.log(Status.PASS, "✅ PASS: Token hết hạn → HTTP " + actualStatusCode);
        } catch (AssertionError e) {
            extentTest.log(Status.FAIL, "❌ FAIL: HTTP " + actualStatusCode);
            throw e;
        }

        // ================================================================
        // KẾT LUẬN TỔNG THỂ TC07
        // ================================================================
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("✅ TC07 - PASS: API bảo vệ đúng cơ chế xác thực");
        System.out.println("   🔒 Không có token → 401");
        System.out.println("   🔒 Token rỗng → 401");
        System.out.println("   🔒 Token sai format → 401");
        System.out.println("   🔒 Token hết hạn → 401");
        System.out.println("═══════════════════════════════════════════════");
    }
}
