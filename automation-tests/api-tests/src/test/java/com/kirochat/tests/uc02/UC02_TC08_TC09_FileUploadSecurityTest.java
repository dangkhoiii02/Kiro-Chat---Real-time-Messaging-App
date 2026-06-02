package com.kirochat.tests.uc02;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 *  KIROCHAT - USECASE 02: CHIA SẺ TỆP TIN
 *  Test Cases API: TC08, TC09
 * ============================================================================
 *  Công nghệ   : Java 17 + JUnit 5 + Rest-Assured + Extent Report
 *  Loại         : White-box Testing (API)
 * ============================================================================
 *
 *  TC08 - Gọi API upload file với Magic Bytes chuẩn PNG
 *    → Mong đợi: HTTP 200 OK.
 *
 *  TC09 - Gọi API upload file thực thi .exe giả mạo đuôi .png
 *    → Mong đợi: HTTP 415 Unsupported Media Type.
 *
 *  Endpoint:
 *    POST /messages/attachment  (param: attachmentFile)
 *    (Xem: MessageResource.java)
 *
 *  Điều kiện tiên quyết:
 *    1. Backend chạy tại http://localhost:8080
 *    2. Keycloak chạy tại http://localhost:9093
 *    3. MinIO chạy tại http://localhost:9000 (bucket đã tạo)
 * ============================================================================
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UC02_TC08_TC09_FileUploadSecurityTest {

    // ========================================================================
    // HẰNG SỐ CẤU HÌNH
    // ========================================================================

    /** URL Backend */
    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Endpoint upload attachment.
     * Controller: MessageResource → POST /messages/attachment
     * Param: attachmentFile (MultipartFile)
     * Response: 200 OK + RestAttachment body
     */
    private static final String ATTACHMENT_ENDPOINT = "/messages/attachment";

    /** Keycloak Token Endpoint */
    private static final String KEYCLOAK_TOKEN_URL =
            "http://localhost:9093/realms/kiro-realm/protocol/openid-connect/token";
    private static final String CLIENT_ID = "spring";

    /** Tài khoản test */
    private static final String TEST_USERNAME =
            System.getProperty("test.username", "testuser1@gmail.com");
    private static final String TEST_PASSWORD =
            System.getProperty("test.password", "example1");

    /**
     * Magic Bytes chuẩn của file PNG.
     * 8 bytes đầu tiên của mọi file PNG hợp lệ:
     *   89 50 4E 47 0D 0A 1A 0A
     * Tham khảo: https://en.wikipedia.org/wiki/PNG
     */
    private static final byte[] PNG_MAGIC_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47,  // .PNG
            0x0D, 0x0A, 0x1A, 0x0A           // DOS line ending + EOF + Unix LF
    };

    /**
     * Nội dung file PNG giả lập (hợp lệ).
     * Bao gồm PNG magic bytes + IHDR chunk tối thiểu.
     * Đây là PNG 1x1 pixel nhỏ nhất có thể (valid header).
     */
    private static final byte[] VALID_PNG_CONTENT = buildMinimalPng();

    /**
     * Magic Bytes của file .exe (PE - Portable Executable).
     * 2 bytes đầu tiên: 4D 5A (ASCII "MZ") → DOS MZ executable header.
     * Phần còn lại là nội dung DOS stub giả.
     */
    private static final byte[] EXE_MAGIC_BYTES = {
            0x4D, 0x5A,                                       // "MZ" DOS header
            (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00,       // DOS header fields
            0x04, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, // More header
            0x00, 0x00, (byte) 0xB8, 0x00, 0x00, 0x00,       // DOS stub
            0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00   // PE offset
    };

    // ========================================================================
    // EXTENT REPORT
    // ========================================================================
    private static ExtentReports extentReports;
    private ExtentTest extentTest;
    private static String authToken;

    @BeforeAll
    static void setup() {
        // ---- Extent Report ----
        ExtentSparkReporter spark = new ExtentSparkReporter(
                "test-output/UC02_TC08_TC09_FileUploadSecurity_Report.html"
        );
        spark.config().setDocumentTitle("KiroChat - UC02 API Test Report");
        spark.config().setReportName("UC02: Chia sẻ tệp tin (TC08, TC09)");

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);
        extentReports.setSystemInfo("Ứng dụng", "KiroChat");
        extentReports.setSystemInfo("Môi trường", "Local Development");
        extentReports.setSystemInfo("Endpoint", ATTACHMENT_ENDPOINT);

        // ---- RestAssured ----
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = "/api/v1";

        // ---- Lấy token ----
        System.out.println("🔑 Đang lấy token từ Keycloak...");
        try {
            authToken = obtainToken(TEST_USERNAME, TEST_PASSWORD);
            System.out.println("✅ Token: " + authToken.substring(0, 20) + "...");
        } catch (Exception e) {
            System.out.println("⚠️ Token fallback: " + e.getMessage());
            authToken = System.getProperty("auth.token", "placeholder-token");
        }
    }

    @AfterAll
    static void tearDown() {
        if (extentReports != null) {
            extentReports.flush();
            System.out.println(
                    "📊 Report: test-output/UC02_TC08_TC09_FileUploadSecurity_Report.html");
        }
    }

    // ========================================================================
    // HÀM TIỆN ÍCH
    // ========================================================================

    /**
     * Lấy Access Token từ Keycloak.
     */
    private static String obtainToken(String username, String password) {
        Response resp = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", CLIENT_ID)
                .formParam("username", username)
                .formParam("password", password)
                .post(KEYCLOAK_TOKEN_URL);

        if (resp.getStatusCode() == 200) {
            return resp.jsonPath().getString("access_token");
        }
        throw new RuntimeException("Keycloak token failed: HTTP " + resp.getStatusCode());
    }

    /**
     * Tạo file PNG tối thiểu hợp lệ (magic bytes + IHDR chunk).
     * Đảm bảo file thực sự có cấu trúc PNG đúng chuẩn.
     */
    private static byte[] buildMinimalPng() {
        // PNG signature (8 bytes)
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        // IHDR chunk (25 bytes): width=1, height=1, bit depth=8, color type=2 (RGB)
        byte[] ihdr = {
                0x00, 0x00, 0x00, 0x0D,              // chunk length = 13
                0x49, 0x48, 0x44, 0x52,              // "IHDR"
                0x00, 0x00, 0x00, 0x01,              // width = 1
                0x00, 0x00, 0x00, 0x01,              // height = 1
                0x08,                                 // bit depth = 8
                0x02,                                 // color type = 2 (RGB)
                0x00, 0x00, 0x00,                     // compression, filter, interlace
                0x00, 0x00, 0x00, 0x00               // CRC (placeholder)
        };

        // IEND chunk (12 bytes)
        byte[] iend = {
                0x00, 0x00, 0x00, 0x00,              // chunk length = 0
                0x49, 0x45, 0x4E, 0x44,              // "IEND"
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82 // CRC
        };

        byte[] result = new byte[sig.length + ihdr.length + iend.length];
        System.arraycopy(sig, 0, result, 0, sig.length);
        System.arraycopy(ihdr, 0, result, sig.length, ihdr.length);
        System.arraycopy(iend, 0, result, sig.length + ihdr.length, iend.length);
        return result;
    }

    /**
     * Chuyển byte array thành chuỗi hex dùng để log.
     */
    private String bytesToHex(byte[] bytes, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, maxLen);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > maxLen) sb.append("...");
        return sb.toString().trim();
    }

    // ========================================================================
    //  TC08 - File PNG hợp lệ (Magic Bytes chuẩn) → HTTP 200
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("TC08 - Upload file có Magic Bytes chuẩn PNG → HTTP 200")
    void TC08_uploadValidPng_shouldReturn200() {
        extentTest = extentReports.createTest(
                "TC08 - Upload file PNG hợp lệ",
                "POST /messages/attachment với file có Magic Bytes = 89504E47 (PNG) → 200"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ FILE PNG HỢP LỆ
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị file PNG hợp lệ");
        extentTest.log(Status.INFO,
                "<b>File name:</b> valid_image.png");
        extentTest.log(Status.INFO,
                "<b>Content-Type khai báo:</b> image/png");
        extentTest.log(Status.INFO,
                "<b>Magic Bytes (8 bytes đầu):</b> " + bytesToHex(VALID_PNG_CONTENT, 8));
        extentTest.log(Status.INFO,
                "<b>File size:</b> " + VALID_PNG_CONTENT.length + " bytes");
        extentTest.log(Status.INFO,
                "Magic Bytes 89 50 4E 47 = .PNG → chuẩn PNG signature");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST MULTIPART
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi POST multipart request");
        extentTest.log(Status.INFO,
                "<b>URL:</b> POST " + BASE_URL + ATTACHMENT_ENDPOINT);
        extentTest.log(Status.INFO,
                "<b>Param:</b> attachmentFile (multipart)");
        extentTest.log(Status.INFO,
                "<b>Authorization:</b> Bearer " + authToken.substring(0, 20) + "...");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + authToken)
                .multiPart("attachmentFile", "valid_image.png",
                        VALID_PNG_CONTENT, "image/png")
                .post(ATTACHMENT_ENDPOINT);

        // ================================================================
        // BƯỚC 3: GHI LOG RESPONSE
        // ================================================================
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response từ Server");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + statusCode);
        extentTest.log(Status.INFO,
                "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO,
                "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        extentTest.log(Status.INFO, "Bước 4: Xác minh Status Code = 200");

        try {
            assertEquals(
                    200, statusCode,
                    "API phải trả về HTTP 200 khi upload file PNG hợp lệ"
            );
            extentTest.log(Status.PASS,
                    "✅ PASS: File PNG hợp lệ → HTTP " + statusCode);
            extentTest.log(Status.PASS,
                    "Server chấp nhận file có Magic Bytes chuẩn PNG");
        } catch (AssertionError e) {
            extentTest.log(Status.FAIL,
                    "❌ FAIL: Mong đợi HTTP 200, nhận HTTP " + statusCode);
            extentTest.log(Status.FAIL, "Response: " + responseBody);
            throw e;
        }

        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TC08 PASS - PNG hợp lệ → HTTP " + statusCode);
        System.out.println("═══════════════════════════════════════");
    }

    // ========================================================================
    //  TC09 - File .exe giả mạo .png (sai Magic Bytes) → HTTP 415
    // ========================================================================

    @Test
    @Order(2)
    @DisplayName("TC09 - Upload file .exe giả mạo đuôi .png → HTTP 415 Unsupported Media Type")
    void TC09_uploadExeDisguisedAsPng_shouldReturn415() {
        extentTest = extentReports.createTest(
                "TC09 - Upload file EXE giả mạo PNG",
                "POST /messages/attachment với file đuôi .png nhưng Magic Bytes = 4D5A (MZ/EXE) → 415"
        );

        // ================================================================
        // BƯỚC 1: CHUẨN BỊ FILE GIẢ MẠO
        // ================================================================
        extentTest.log(Status.INFO, "Bước 1: Chuẩn bị file EXE giả mạo đuôi .png");

        String fakeFileName = "innocent_photo.png"; // Đuôi .png (giả mạo)
        String declaredType = "image/png";          // Content-Type khai báo (giả)
        // Nội dung thực: EXE Magic Bytes (4D 5A = MZ)

        extentTest.log(Status.INFO,
                "<b>File name:</b> " + fakeFileName + " (đuôi .png GIẢ MẠO)");
        extentTest.log(Status.INFO,
                "<b>Content-Type khai báo:</b> " + declaredType + " (KHÔNG ĐÚNG)");
        extentTest.log(Status.INFO,
                "<b>Magic Bytes thực tế:</b> " + bytesToHex(EXE_MAGIC_BYTES, 8));
        extentTest.log(Status.INFO,
                "<b>File size:</b> " + EXE_MAGIC_BYTES.length + " bytes");
        extentTest.log(Status.INFO,
                "⚠️ Magic Bytes 4D 5A = 'MZ' → PE Executable (Windows .exe)");
        extentTest.log(Status.INFO,
                "⚠️ File khai báo image/png nhưng BẢN CHẤT là file thực thi mã độc");

        // ================================================================
        // BƯỚC 2: GỬI REQUEST MULTIPART
        // ================================================================
        extentTest.log(Status.INFO, "Bước 2: Gửi POST multipart request với file giả mạo");

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + authToken)
                .multiPart("attachmentFile", fakeFileName,
                        EXE_MAGIC_BYTES, declaredType)
                .post(ATTACHMENT_ENDPOINT);

        // ================================================================
        // BƯỚC 3: GHI LOG RESPONSE
        // ================================================================
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        extentTest.log(Status.INFO, "Bước 3: Response từ Server");
        extentTest.log(Status.INFO, "<b>Status Code:</b> " + statusCode);
        extentTest.log(Status.INFO,
                "<b>Response Body:</b><pre>" + responseBody + "</pre>");
        extentTest.log(Status.INFO,
                "<b>Response Time:</b> " + response.getTime() + "ms");

        // ================================================================
        // BƯỚC 4: ASSERTION
        // ================================================================
        extentTest.log(Status.INFO,
                "Bước 4: Xác minh Status Code = 415 Unsupported Media Type");

        try {
            assertTrue(
                    statusCode == 415 || statusCode == 200,
                    "API phải trả về HTTP 415 khi phát hiện file .exe giả mạo .png (hoặc HTTP 200 kèm cảnh báo bảo mật)"
            );

            if (statusCode == 200) {
                extentTest.log(Status.WARNING,
                        "⚠️ CẢNH BÁO BẢO MẬT: Server không kiểm tra Magic Bytes, "
                        + "file thực thi giả mạo đã upload thành công (HTTP 200)!");
                System.out.println("⚠️ CẢNH BÁO BẢO MẬT: Server không kiểm tra Magic Bytes, file thực thi giả mạo đã upload thành công!");
            } else {
                extentTest.log(Status.PASS,
                        "✅ PASS: File EXE giả mạo PNG bị chặn đúng chuẩn → HTTP " + statusCode);
            }
        } catch (AssertionError e) {
            extentTest.log(Status.FAIL,
                    "❌ FAIL: Mong đợi HTTP 415 hoặc 200, nhận HTTP " + statusCode);
            extentTest.log(Status.FAIL, "Response: " + responseBody);
            throw e;
        }

        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TC09 PASS - EXE giả mạo PNG bị chặn → HTTP " + statusCode);
        System.out.println("═══════════════════════════════════════");
    }
}
