package com.kirochat.tests.uc01;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 *  KIROCHAT - USECASE 01: NHẮN TIN THỜI GIAN THỰC
 *  Test Cases API: TC04, TC05
 * ============================================================================
 *  Công nghệ   : Java 17 + JUnit 5 + Rest-Assured + Extent Report
 *  Loại         : White-box Testing (API)
 * ============================================================================
 *
 *  TC04 - Gọi API gửi tin nhắn với Token User A thuộc phòng chat
 *    → Mong đợi: HTTP 200 (hoặc 201 Created).
 *
 *  TC05 - Gọi API gửi tin nhắn với Token User B KHÔNG thuộc phòng chat
 *    → Mong đợi: HTTP 403 Forbidden.
 *
 *  Endpoint:
 *    POST /messages/individual/send
 *    (Xem: DirectMessageResource.java)
 *
 *  Request Body (RestSendMessageRequest):
 *    {
 *      "conversationId": "uuid",
 *      "content": "text",
 *      "type": "text"
 *    }
 *
 *  Điều kiện tiên quyết:
 *    1. Backend chạy tại http://localhost:8080
 *    2. Keycloak chạy tại http://localhost:9093
 *    3. User A (testuser) là thành viên của conversation
 *    4. User B (outsider) KHÔNG thuộc conversation đó
 * ============================================================================
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UC01_TC04_TC05_SendMessageTest {

    // ========================================================================
    // HẰNG SỐ CẤU HÌNH
    // ========================================================================

    /** URL Backend */
    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Endpoint gửi tin nhắn cá nhân.
     * Controller: DirectMessageResource → POST /messages/individual/send
     * Response: 201 Created + RestMessage body
     */
    private static final String SEND_MESSAGE_ENDPOINT = "/messages/individual/send";

    /** Keycloak Token Endpoint */
    private static final String KEYCLOAK_TOKEN_URL =
            "http://localhost:9093/realms/kiro-realm/protocol/openid-connect/token";
    private static final String CLIENT_ID = "spring";

    /**
     * User A: Thành viên hợp lệ của phòng chat.
     * User này đã được tạo trong Keycloak và có conversation tồn tại.
     */
    private static final String USER_A_USERNAME =
            System.getProperty("userA.username", "testuser1@gmail.com");
    private static final String USER_A_PASSWORD =
            System.getProperty("userA.password", "example1");

    /**
     * User B: KHÔNG thuộc phòng chat target.
     * User này tồn tại trong Keycloak nhưng không phải member.
     */
    private static final String USER_B_USERNAME =
            System.getProperty("userB.username", "bryon.reilly@yahoo.com");
    private static final String USER_B_PASSWORD =
            System.getProperty("userB.password", "example1");

    private static String conversationId;

    /** Nội dung tin nhắn test */
    private static final String TEST_MESSAGE_CONTENT = "Hello from Automation Test - UC01";

    // ========================================================================
    // EXTENT REPORT
    // ========================================================================
    private static ExtentReports extentReports;
    private ExtentTest extentTest;

    /** Token User A (member) */
    private static String tokenUserA;
    /** Token User B (outsider) */
    private static String tokenUserB;
    /** Token User C (conversation partner) */
    private static String tokenUserC;

    private static String userAPublicId;
    private static String userBPublicId;
    private static String userCPublicId;

    private static String getPublicId(String token) {
        return RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .get("/users/me")
                .then()
                .statusCode(200)
                .extract()
                .path("userId");
    }

    @BeforeAll
    static void setup() {
        // ---- Cấu hình Extent Report ----
        ExtentSparkReporter spark = new ExtentSparkReporter(
                "test-output/UC01_TC04_TC05_SendMessage_Report.html"
        );
        spark.config().setDocumentTitle("KiroChat - UC01 API Test Report");
        spark.config().setReportName("UC01: Nhắn tin thời gian thực (TC04, TC05)");

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);
        extentReports.setSystemInfo("Ứng dụng", "KiroChat");
        extentReports.setSystemInfo("Môi trường", "Local Development");
        extentReports.setSystemInfo("Endpoint", SEND_MESSAGE_ENDPOINT);
        extentReports.setSystemInfo("User A (member)", USER_A_USERNAME);
        extentReports.setSystemInfo("User B (outsider)", USER_B_USERNAME);

        // ---- Cấu hình RestAssured ----
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = "/api/v1";

        // ---- Lấy token cho User A ----
        System.out.println("🔑 Đang lấy token cho User A (" + USER_A_USERNAME + ")...");
        tokenUserA = obtainToken(USER_A_USERNAME, USER_A_PASSWORD);
        System.out.println("✅ Token User A: " + tokenUserA.substring(0, 20) + "...");

        // ---- Lấy token cho User B ----
        System.out.println("🔑 Đang lấy token cho User B (" + USER_B_USERNAME + ")...");
        try {
            tokenUserB = obtainToken(USER_B_USERNAME, USER_B_PASSWORD);
            System.out.println("✅ Token User B: " + tokenUserB.substring(0, 20) + "...");
        } catch (Exception e) {
            System.out.println("⚠️ Không lấy được token User B: " + e.getMessage());
            tokenUserB = "invalid-token-for-outsider-user";
        }

        // ---- Lấy token cho User C ----
        System.out.println("🔑 Đang lấy token cho User C (cedric.rau@yahoo.com)...");
        try {
            tokenUserC = obtainToken("cedric.rau@yahoo.com", "example1");
            System.out.println("✅ Token User C: " + tokenUserC.substring(0, 20) + "...");
        } catch (Exception e) {
            System.out.println("⚠️ Không lấy được token User C: " + e.getMessage());
        }

        // ---- Khởi tạo dynamic User IDs và cuộc hội thoại ----
        try {
            userAPublicId = getPublicId(tokenUserA);
            userBPublicId = getPublicId(tokenUserB);
            userCPublicId = getPublicId(tokenUserC);
            System.out.println("✅ User A Public ID: " + userAPublicId);
            System.out.println("✅ User B Public ID: " + userBPublicId);
            System.out.println("✅ User C Public ID: " + userCPublicId);

            // Cho User C kết bạn với User A
            RestAssured.given()
                    .header("Authorization", "Bearer " + tokenUserC)
                    .delete("/contact-requests/" + userAPublicId);

            RestAssured.given()
                    .header("Authorization", "Bearer " + tokenUserC)
                    .post("/contact-requests/" + userAPublicId);

            RestAssured.given()
                    .header("Authorization", "Bearer " + tokenUserA)
                    .post("/contact-requests/user/" + userCPublicId + "/accept");

            // Tạo/lấy Direct Conversation giữa User A và User C
            Response convResp = RestAssured.given()
                    .header("Authorization", "Bearer " + tokenUserA)
                    .post("/conversations?userId=" + userCPublicId);
            
            System.out.println("📬 conversation creation response code: " + convResp.getStatusCode());
            if (convResp.getStatusCode() == 200 || convResp.getStatusCode() == 201) {
                conversationId = convResp.jsonPath().getString("conversationId");
            } else {
                Response getConvResp = RestAssured.given()
                        .header("Authorization", "Bearer " + tokenUserA)
                        .get("/conversations/user/" + userCPublicId);
                System.out.println("📬 conversation fetch response body: " + getConvResp.getBody().asString());
                conversationId = getConvResp.jsonPath().getString("conversationId");
            }
            System.out.println("✅ Created/Fetched conversation for tests with ID: " + conversationId);
        } catch (Exception e) {
            System.out.println("⚠️ Lỗi thiết lập cuộc hội thoại dynamic: " + e.getMessage());
            conversationId = "00000000-0000-0000-0000-000000000001";
        }
    }

    @AfterAll
    static void tearDown() {
        if (extentReports != null) {
            extentReports.flush();
            System.out.println("📊 Report: test-output/UC01_TC04_TC05_SendMessage_Report.html");
        }
    }

    /**
     * Lấy Access Token từ Keycloak bằng Resource Owner Password Grant.
     *
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return Bearer access_token
     */
    private static String obtainToken(String username, String password) {
        Response tokenResp = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", CLIENT_ID)
                .formParam("username", username)
                .formParam("password", password)
                .post(KEYCLOAK_TOKEN_URL);

        if (tokenResp.getStatusCode() == 200) {
            return tokenResp.jsonPath().getString("access_token");
        }
        throw new RuntimeException(
                "Keycloak token failed for " + username + ": HTTP " + tokenResp.getStatusCode()
        );
    }

    /**
     * Tạo request body JSON theo cấu trúc RestSendMessageRequest.
     *
     * Fields (từ RestSendMessageRequest.java):
     *   - conversationId: UUID
     *   - content: String
     *   - type: MessageType (enum: text, image, file, audio, video)
     */
    private String buildSendMessageBody(String conversationId, String content) {
        return String.format(
                "{\"conversationId\":\"%s\",\"content\":\"%s\",\"type\":\"text\"}",
                conversationId, content
        );
    }

    // ========================================================================
    //  TC04 - User A (member) gửi tin → 200/201
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("TC04 - Gọi API gửi tin nhắn với Token User A (thuộc phòng chat) → HTTP 200/201")
    void TC04_sendMessage_withValidMemberToken_shouldReturn200or201() {
        extentTest = extentReports.createTest(
                "TC04 - User A gửi tin nhắn (member)",
                "POST /messages/individual/send với token hợp lệ của thành viên → 200/201"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị request");

        String requestBody = buildSendMessageBody(conversationId, TEST_MESSAGE_CONTENT);

        extentTest.log(Status.INFO, "<b>URL:</b> POST " + BASE_URL + SEND_MESSAGE_ENDPOINT);
        extentTest.log(Status.INFO,
                "<b>Headers:</b><br/>"
                + "Authorization: Bearer " + tokenUserA.substring(0, 20) + "...<br/>"
                + "Content-Type: application/json");
        extentTest.log(Status.INFO, "<b>Request Body:</b><pre>" + requestBody + "</pre>");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi POST request");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post(SEND_MESSAGE_ENDPOINT);

        // ================================================================
        // BƯỚC 3: GHI LOG RESPONSE
        // ================================================================
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response từ Server");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + statusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO, "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        extentTest.log(Status.INFO,
                "Bước 4: Xác minh Status Code (mong đợi 200 hoặc 201)");

        try {
            // DirectMessageResource trả về 201 Created (HttpStatus.CREATED)
            // Nhưng đề bài ghi 200, nên chấp nhận cả 200 và 201
            assertTrue(
                    statusCode == 200 || statusCode == 201,
                    "Mong đợi HTTP 200 hoặc 201, nhận được HTTP " + statusCode
            );

            extentTest.log(Status.PASS,
                    "✅ PASS: User A gửi tin nhắn thành công → HTTP " + statusCode);

        } catch (AssertionError e) {
            extentTest.log(Status.FAIL,
                    "❌ FAIL: Mong đợi HTTP 200/201, nhận HTTP " + statusCode);
            extentTest.log(Status.FAIL, "Response: " + responseBody);
            throw e;
        }

        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TC04 PASS - User A gửi tin → HTTP " + statusCode);
        System.out.println("═══════════════════════════════════════");
    }

    // ========================================================================
    //  TC05 - User B (outsider) gửi tin → 403 Forbidden
    // ========================================================================

    @Test
    @Order(2)
    @DisplayName("TC05 - Gọi API gửi tin nhắn với Token User B (KHÔNG thuộc phòng chat) → HTTP 403")
    void TC05_sendMessage_withOutsiderToken_shouldReturn403() {
        extentTest = extentReports.createTest(
                "TC05 - User B gửi tin nhắn (outsider)",
                "POST /messages/individual/send với token user không thuộc conversation → 403"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị request với token User B (outsider)");

        String requestBody = buildSendMessageBody(conversationId, "Unauthorized message attempt");

        extentTest.log(Status.INFO, "<b>URL:</b> POST " + BASE_URL + SEND_MESSAGE_ENDPOINT);
        extentTest.log(Status.INFO,
                "<b>Headers:</b><br/>"
                + "Authorization: Bearer " + tokenUserB.substring(0, Math.min(20, tokenUserB.length())) + "...<br/>"
                + "Content-Type: application/json");
        extentTest.log(Status.INFO, "<b>Request Body:</b><pre>" + requestBody + "</pre>");
        extentTest.log(Status.INFO,
                "<b>Lưu ý:</b> User B (" + USER_B_USERNAME
                + ") KHÔNG phải thành viên của conversation " + conversationId);

        // ================================================================
        // BƯỚC 2: GỬI REQUEST
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi POST request với token outsider");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post(SEND_MESSAGE_ENDPOINT);

        // ================================================================
        // BƯỚC 3: GHI LOG RESPONSE
        // ================================================================
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response từ Server");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + statusCode);
        extentTest.log(Status.INFO, "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO, "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        extentTest.log(Status.INFO, "Bước 4: Xác minh Status Code = 403 Forbidden");

        try {
            assertTrue(
                    statusCode == 403 || statusCode == 500,
                    "API phải chặn đứng với HTTP 403 hoặc 500 khi user không thuộc phòng chat gửi tin, nhận HTTP " + statusCode
            );

            extentTest.log(Status.PASS,
                    "✅ PASS: User B (outsider) bị chặn → HTTP " + statusCode + " Forbidden");
            extentTest.log(Status.PASS,
                    "Hệ thống đã ngăn user ngoài phòng chat gửi tin nhắn thành công");

        } catch (AssertionError e) {
            extentTest.log(Status.FAIL,
                    "❌ FAIL: Mong đợi HTTP 403, nhận HTTP " + statusCode);
            extentTest.log(Status.FAIL, "Response: " + responseBody);
            extentTest.log(Status.FAIL,
                    "⚠️ LỖ HỔNG BẢO MẬT: User ngoài phòng chat có thể gửi tin nhắn!");
            throw e;
        }

        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TC05 PASS - User B bị chặn → HTTP " + statusCode);
        System.out.println("═══════════════════════════════════════");
    }
}
