/**
 * ============================================================================
 *  KIROCHAT - AUTOMATION UI TEST
 * ============================================================================
 *  Test Case ID : TC04
 *  Tên          : Kiểm thử giao diện chặn gửi tin nhắn rỗng (whitespace-only)
 *  Loại         : Black-box Testing (Giao diện)
 *  Công nghệ   : NodeJS + Jest + Selenium WebDriver + Allure Report
 * ============================================================================
 *  Mô tả:
 *    Xác minh rằng hệ thống KiroChat KHÔNG cho phép gửi tin nhắn rỗng hoặc
 *    chỉ chứa khoảng trắng (spaces, tabs). Nút Gửi phải ở trạng thái
 *    disabled khi textarea rỗng hoặc chỉ có whitespace.
 *
 *  Điều kiện tiên quyết:
 *    1. KiroChat Frontend đang chạy tại http://localhost:4200
 *    2. Keycloak đang chạy tại http://localhost:9093
 *    3. Backend đang chạy tại http://localhost:8080
 *    4. Tài khoản test đã được tạo trong Keycloak
 *    5. ChromeDriver đã được cài đặt và khớp phiên bản Chrome
 *
 *  Kết quả mong đợi:
 *    - Nút Gửi ở trạng thái disabled khi textarea rỗng
 *    - Nút Gửi vẫn disabled khi textarea chỉ chứa khoảng trắng
 *    - Không có tin nhắn mới nào được gửi đi
 * ============================================================================
 */

const {
  createDriver,
  loginToKiroChat,
  takeScreenshot,
  attachScreenshotToAllure,
  waitForElement,
  waitForElementVisible,
  BASE_URL,
  By,
  until,
} = require("../../helpers/driver.helper");

// ============================================================================
// HẰNG SỐ DÙNG TRONG TESTCASE
// ============================================================================

/** Các chuỗi input rỗng / whitespace-only cần kiểm thử */
const EMPTY_INPUTS = [
  { label: "Chuỗi rỗng hoàn toàn", value: "" },
  { label: "Chỉ có 1 khoảng trắng", value: " " },
  { label: "Nhiều khoảng trắng", value: "     " },
  { label: "Chỉ có tab và space", value: "  \t  " },
];

// ============================================================================
// BIẾN TOÀN CỤC
// ============================================================================

/** Instance WebDriver dùng chung */
let driver;

// ============================================================================
// TEST SUITE
// ============================================================================

describe("KiroChat - Kiểm thử chặn gửi tin nhắn rỗng", () => {
  /**
   * SETUP: Khởi tạo trước MỖI testcase
   */
  beforeEach(async () => {
    // Bước 1: Khởi tạo Chrome WebDriver
    driver = await createDriver();
    console.log("✅ WebDriver đã được khởi tạo thành công");

    // Bước 2: Đăng nhập vào KiroChat qua Keycloak OAuth2
    await loginToKiroChat(driver);
    console.log("✅ Đăng nhập KiroChat thành công");
  });

  /**
   * TEARDOWN (BẮT BUỘC): Chụp screenshot khi FAIL + đính kèm Allure Report
   *
   * ⚠️ Theo quy chuẩn đồ án: "Bắt buộc phải có block afterEach để chụp ảnh
   *    màn hình nếu testcase fail và ghi ra file base64 đính kèm Allure Report"
   */
  afterEach(async () => {
    try {
      const testName =
        expect.getState().currentTestName || "unknown_test";
      const testFailed =
        expect.getState().numPassingAsserts === 0 ||
        expect.getState().suppressedErrors?.length > 0;

      if (testFailed) {
        console.log(
          `❌ Test "${testName}" FAILED - Đang chụp ảnh màn hình...`
        );

        // Chụp ảnh màn hình trả về base64
        const screenshotBase64 = await takeScreenshot(driver, testName);

        // Đính kèm vào Allure Report
        if (screenshotBase64) {
          attachScreenshotToAllure(
            screenshotBase64,
            `FAIL - ${testName}`
          );
          console.log("📎 Screenshot đã đính kèm Allure Report");

          // Ghi file base64 thuần (theo yêu cầu đồ án)
          const fs = require("fs");
          const path = require("path");
          const base64Dir = path.join(
            __dirname,
            "..",
            "..",
            "screenshots",
            "base64"
          );
          if (!fs.existsSync(base64Dir)) {
            fs.mkdirSync(base64Dir, { recursive: true });
          }
          const safeFileName = testName.replace(/[^a-zA-Z0-9_-]/g, "_");
          fs.writeFileSync(
            path.join(base64Dir, `${safeFileName}.txt`),
            screenshotBase64,
            "utf-8"
          );
          console.log(
            `💾 Base64 screenshot đã ghi ra file: ${safeFileName}.txt`
          );
        }
      } else {
        console.log(`✅ Test "${testName}" PASSED`);
      }
    } catch (screenshotError) {
      console.error(
        `⚠️ Không thể chụp screenshot: ${screenshotError.message}`
      );
    } finally {
      // LUÔN LUÔN đóng WebDriver
      if (driver) {
        await driver.quit();
        console.log("🔒 WebDriver đã được đóng");
      }
    }
  });

  // ==========================================================================
  // TESTCASE CHÍNH: TC04
  // ==========================================================================

  test("TC04 - Kiểm thử giao diện chặn gửi tin nhắn rỗng (whitespace-only)", async () => {
    /**
     * ========================================================================
     * BƯỚC 1: ĐIỀU HƯỚNG ĐẾN MÀN HÌNH CHAT
     * ========================================================================
     * Sau khi đăng nhập, mở cuộc hội thoại đầu tiên trong danh sách.
     */
    console.log("📌 Bước 1: Điều hướng đến màn hình chat...");

    // Chờ trang chat load xong
    await driver.wait(
      until.urlContains("/chat"),
      15000,
      "Không thể điều hướng đến trang chat"
    );

    // Click vào cuộc hội thoại đầu tiên
    const firstConversation = await waitForElement(
      driver,
      By.xpath('//app-chat-list//a[contains(@class, "cursor-pointer")][1]'),
      15000
    );
    await firstConversation.click();
    console.log("✅ Đã click vào cuộc hội thoại đầu tiên");

    // Chờ vùng tin nhắn hiển thị
    await waitForElement(driver, By.id("message-area"), 10000);
    console.log("✅ Vùng tin nhắn đã hiển thị");

    /**
     * ========================================================================
     * BƯỚC 2: ĐỊNH VỊ CÁC PHẦN TỬ UI CẦN KIỂM TRA
     * ========================================================================
     * Cần tìm:
     *  - textarea: ô nhập tin nhắn
     *  - sendButton: nút Gửi (có attribute [disabled]="!canSend()")
     *
     * Trong source Angular, nút Gửi bị disabled khi:
     *   canSend() = content().trim().length > 0 && !isSending() && !isDisabled()
     *   → Nếu content rỗng hoặc chỉ whitespace → canSend() = false → disabled
     */
    console.log("📌 Bước 2: Định vị textarea và nút Gửi...");

    const messageTextarea = await waitForElementVisible(
      driver,
      By.xpath(
        '//app-message-input//textarea' +
          '[contains(@placeholder, "Type a message") or contains(@class, "rounded-lg")]'
      ),
      10000
    );
    console.log("✅ Đã tìm thấy textarea");

    const sendButton = await waitForElement(
      driver,
      By.xpath(
        '//app-message-input//button' +
          '[contains(@aria-label, "Send message") or ' +
          './/i[contains(@class, "fa-paper-plane")]]'
      ),
      5000
    );
    console.log("✅ Đã tìm thấy nút Gửi");

    /**
     * ========================================================================
     * BƯỚC 3: KIỂM TRA NÚT GỬI DISABLED KHI TEXTAREA RỖNG (MẶC ĐỊNH)
     * ========================================================================
     * Khi vừa mở chat, textarea rỗng → nút Gửi phải disabled.
     * Angular binding: [disabled]="!canSend()"
     * canSend() trả về false khi content().trim().length === 0
     */
    console.log("📌 Bước 3: Kiểm tra nút Gửi disabled khi textarea rỗng mặc định...");

    // Lấy trạng thái disabled của nút Gửi
    const isDisabledByDefault = await sendButton.getAttribute("disabled");

    // ASSERTION: Nút Gửi phải disabled khi chưa nhập gì
    // getAttribute("disabled") trả về "true" hoặc null
    expect(isDisabledByDefault).toBe("true");
    console.log("✅ PASS - Nút Gửi đã disabled khi textarea rỗng mặc định");

    /**
     * ========================================================================
     * BƯỚC 4: NHẬP CÁC CHUỖI WHITESPACE VÀ KIỂM TRA NÚT GỬI VẪN DISABLED
     * ========================================================================
     * Duyệt qua danh sách các chuỗi rỗng/whitespace-only, nhập từng chuỗi
     * vào textarea và xác minh nút Gửi vẫn ở trạng thái disabled.
     */
    console.log("📌 Bước 4: Kiểm tra với các chuỗi whitespace...");

    for (const inputCase of EMPTY_INPUTS) {
      console.log(`   🔄 Đang test: "${inputCase.label}" → "${inputCase.value}"`);

      // Xóa nội dung textarea cũ và nhập chuỗi mới
      // Sử dụng executeScript vì sendKeys không gửi được tab/space thuần
      await driver.executeScript(
        `
        const textarea = arguments[0];
        textarea.value = '';
        textarea.value = arguments[1];
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
        textarea.dispatchEvent(new Event('change', { bubbles: true }));
        `,
        messageTextarea,
        inputCase.value
      );

      // Chờ Angular change detection xử lý
      await driver.sleep(500);

      // Kiểm tra nút Gửi có disabled không
      const isDisabled = await sendButton.getAttribute("disabled");

      // ASSERTION: Nút Gửi PHẢI disabled cho mọi chuỗi whitespace-only
      expect(isDisabled).toBe("true");
      console.log(`   ✅ PASS - Nút Gửi disabled với: "${inputCase.label}"`);
    }

    /**
     * ========================================================================
     * BƯỚC 5: ĐẾM SỐ TIN NHẮN TRƯỚC VÀ SAU ĐỂ XÁC MINH KHÔNG GỬI
     * ========================================================================
     * Đếm số message-bubble trong message-area trước khi thử gửi.
     * Sau khi thử click nút Gửi (dù disabled), đếm lại và so sánh.
     */
    console.log("📌 Bước 5: Xác minh không có tin nhắn mới được gửi...");

    // Đếm số tin nhắn hiện tại
    const messageArea = await driver.findElement(By.id("message-area"));
    const messagesBefore = await messageArea.findElements(
      By.css("app-message-bubble")
    );
    const countBefore = messagesBefore.length;
    console.log(`   📊 Số tin nhắn trước khi thử gửi: ${countBefore}`);

    // Thử click nút Gửi (dù disabled, click vẫn execute nhưng Angular
    // sẽ bỏ qua vì canSend() = false)
    try {
      await driver.executeScript("arguments[0].click()", sendButton);
    } catch (e) {
      // Ignore - nút disabled có thể không click được
      console.log("   ℹ️ Nút Gửi disabled, click bị bỏ qua (đúng hành vi)");
    }

    // Chờ 1 giây để đảm bảo không có tin nhắn mới
    await driver.sleep(1000);

    // Đếm lại số tin nhắn
    const messagesAfter = await messageArea.findElements(
      By.css("app-message-bubble")
    );
    const countAfter = messagesAfter.length;
    console.log(`   📊 Số tin nhắn sau khi thử gửi: ${countAfter}`);

    // ASSERTION: Số tin nhắn không đổi → không gửi tin nhắn rỗng
    expect(countAfter).toBe(countBefore);
    console.log("✅ PASS - Không có tin nhắn rỗng nào được gửi");

    /**
     * ========================================================================
     * BƯỚC 6: ĐỐI CHỨNG - NHẬP TEXT HỢP LỆ → NÚT GỬI PHẢI ENABLED
     * ========================================================================
     * Để đảm bảo nút Gửi thực sự hoạt động đúng logic, ta nhập một chuỗi
     * hợp lệ (có ký tự thật) và xác minh nút Gửi chuyển sang enabled.
     */
    console.log("📌 Bước 6: Đối chứng - nhập text hợp lệ...");

    // Nhập chuỗi hợp lệ "Hello"
    await driver.executeScript(
      `
      const textarea = arguments[0];
      textarea.value = '';
      textarea.value = 'Hello KiroChat';
      textarea.dispatchEvent(new Event('input', { bubbles: true }));
      textarea.dispatchEvent(new Event('change', { bubbles: true }));
      `,
      messageTextarea
    );

    // Chờ Angular xử lý
    await driver.sleep(500);

    // Kiểm tra nút Gửi đã enabled
    const isEnabledWithText = await sendButton.getAttribute("disabled");

    // ASSERTION: Nút Gửi KHÔNG disabled khi có text hợp lệ
    // getAttribute("disabled") trả về null khi không disabled
    expect(isEnabledWithText).toBeNull();
    console.log("✅ PASS - Nút Gửi enabled khi nhập text hợp lệ (đối chứng thành công)");

    // ========================================================================
    // KẾT LUẬN: TC04 - PASS
    // ========================================================================
    console.log("═══════════════════════════════════════════════");
    console.log("✅ TC04 - PASS: Hệ thống đã chặn gửi tin nhắn rỗng thành công");
    console.log("   🔒 Nút Gửi disabled cho mọi chuỗi rỗng/whitespace");
    console.log("   🚫 Không có tin nhắn rỗng nào được gửi đi");
    console.log("   ✅ Nút Gửi enabled khi nhập text hợp lệ (đối chứng)");
    console.log("═══════════════════════════════════════════════");
  });
});
