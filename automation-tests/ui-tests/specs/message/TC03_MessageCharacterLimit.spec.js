/**
 * ============================================================================
 *  KIROCHAT - AUTOMATION UI TEST
 * ============================================================================
 *  Test Case ID : TC03
 *  Tên          : Kiểm thử giao diện chặn tin nhắn vượt quá 5000 ký tự
 *  Loại         : Black-box Testing (Giao diện)
 *  Công nghệ   : NodeJS + Jest + Selenium WebDriver + Allure Report
 * ============================================================================
 *  Mô tả:
 *    Xác minh rằng hệ thống KiroChat chặn người dùng gửi tin nhắn có nội dung
 *    vượt quá giới hạn 5000 ký tự. Khi nhập chuỗi 5001 ký tự và bấm nút Gửi,
 *    hệ thống phải hiển thị thông báo lỗi Toast "Vượt quá giới hạn 5000 ký tự".
 *
 *  Điều kiện tiên quyết:
 *    1. KiroChat Frontend đang chạy tại http://localhost:4200
 *    2. Keycloak đang chạy tại http://localhost:9093
 *    3. Backend đang chạy tại http://localhost:8080
 *    4. Tài khoản test đã được tạo trong Keycloak
 *    5. ChromeDriver đã được cài đặt và khớp phiên bản Chrome
 *
 *  Kết quả mong đợi:
 *    - Toast lỗi xuất hiện với nội dung "Vượt quá giới hạn 5000 ký tự"
 *    - Tin nhắn KHÔNG được gửi đi
 *    - Nội dung textarea vẫn giữ nguyên (không bị xóa)
 * ============================================================================
 */

const {
  createDriver,
  loginToKiroChat,
  takeScreenshot,
  attachScreenshotToAllure,
  waitForElement,
  waitForElementVisible,
  waitForToast,
  BASE_URL,
  By,
  until,
} = require("../../helpers/driver.helper");

// ============================================================================
// HẰNG SỐ DÙNG TRONG TESTCASE
// ============================================================================

/** Số ký tự tối đa cho phép trong 1 tin nhắn */
const MAX_MESSAGE_LENGTH = 5000;

/** Số ký tự sẽ nhập để vượt giới hạn (5001 ký tự) */
const EXCEEDED_LENGTH = MAX_MESSAGE_LENGTH + 1;

/** Nội dung Toast lỗi mong đợi khi vượt giới hạn ký tự */
const EXPECTED_ERROR_TOAST = "Vượt quá giới hạn 5000 ký tự";

/** Thời gian chờ tối đa cho Toast xuất hiện (ms) */
const TOAST_WAIT_TIMEOUT = 10000;

/**
 * Tạo chuỗi ký tự với độ dài chính xác.
 * Sử dụng ký tự 'a' lặp lại để đảm bảo chuỗi thuần text.
 *
 * @param {number} length - Độ dài chuỗi cần tạo
 * @returns {string} Chuỗi ký tự có độ dài chính xác
 */
function generateString(length) {
  return "a".repeat(length);
}

// ============================================================================
// BIẾN TOÀN CỤC
// ============================================================================

/** Instance WebDriver dùng chung cho toàn bộ test suite */
let driver;

// ============================================================================
// TEST SUITE
// ============================================================================

describe("KiroChat - Kiểm thử giới hạn ký tự tin nhắn", () => {
  /**
   * SETUP: Khởi tạo trước MỖI testcase
   * - Tạo mới Chrome WebDriver instance
   * - Thực hiện đăng nhập vào KiroChat qua Keycloak
   * - Điều hướng đến trang chat
   */
  beforeEach(async () => {
    // ---- Bước 1: Khởi tạo WebDriver ----
    // Tạo Chrome driver mới cho mỗi test để đảm bảo trạng thái sạch
    driver = await createDriver();
    console.log("✅ WebDriver đã được khởi tạo thành công");

    // ---- Bước 2: Đăng nhập vào KiroChat ----
    // Hàm loginToKiroChat xử lý toàn bộ flow OAuth2 qua Keycloak
    await loginToKiroChat(driver);
    console.log("✅ Đăng nhập KiroChat thành công");
  });

  /**
   * TEARDOWN: Dọn dẹp sau MỖI testcase (BẮT BUỘC theo quy chuẩn)
   *
   * Quy trình:
   *  1. Kiểm tra testcase vừa chạy có FAIL không
   *  2. Nếu FAIL → chụp ảnh màn hình và đính kèm vào Allure Report
   *  3. Đóng WebDriver (luôn luôn, dù PASS hay FAIL)
   *
   * ⚠️ Block này là BẮT BUỘC theo quy chuẩn đồ án:
   *    "Bắt buộc phải có block afterEach để chụp ảnh màn hình nếu testcase
   *     fail và ghi ra file base64 đính kèm Allure Report"
   */
  afterEach(async () => {
    try {
      // ---- Kiểm tra test hiện tại có FAIL không ----
      // Jest không cung cấp trực tiếp trạng thái test trong afterEach,
      // nên ta sử dụng biến global jasmine hoặc try-catch ở test body.
      // Cách tiếp cận an toàn: luôn chụp screenshot khi kết thúc test.
      const testName =
        expect.getState().currentTestName || "unknown_test";
      const testFailed =
        expect.getState().numPassingAsserts === 0 ||
        expect.getState().suppressedErrors?.length > 0;

      if (testFailed) {
        console.log(
          `❌ Test "${testName}" FAILED - Đang chụp ảnh màn hình...`
        );

        // ---- Chụp ảnh màn hình ----
        // takeScreenshot trả về chuỗi base64 của file PNG
        const screenshotBase64 = await takeScreenshot(driver, testName);

        // ---- Đính kèm vào Allure Report ----
        // Ghi file base64 vào thư mục allure-results để Allure đọc
        if (screenshotBase64) {
          attachScreenshotToAllure(
            screenshotBase64,
            `FAIL - ${testName}`
          );
          console.log("📎 Screenshot đã đính kèm Allure Report");

          // Ghi thêm file base64 thuần (theo yêu cầu đồ án)
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
      // Nếu chụp ảnh thất bại, log lỗi nhưng KHÔNG throw
      // để không che đi lỗi gốc của testcase
      console.error(
        `⚠️ Không thể chụp screenshot: ${screenshotError.message}`
      );
    } finally {
      // ---- LUÔN LUÔN đóng WebDriver ----
      // Đây là cleanup bắt buộc để tránh tồn đọng process Chrome
      if (driver) {
        await driver.quit();
        console.log("🔒 WebDriver đã được đóng");
      }
    }
  });

  // ==========================================================================
  // TESTCASE CHÍNH: TC03
  // ==========================================================================

  test("TC03 - Kiểm thử giao diện chặn tin nhắn vượt quá 5000 ký tự", async () => {
    /**
     * ========================================================================
     * BƯỚC 1: ĐIỀU HƯỚNG ĐẾN MÀN HÌNH CHAT
     * ========================================================================
     * Sau khi đăng nhập, cần mở một cuộc hội thoại cụ thể để có thể nhập
     * tin nhắn. Ta sẽ click vào cuộc hội thoại đầu tiên trong danh sách chat.
     */
    console.log("📌 Bước 1: Điều hướng đến màn hình chat...");

    // Chờ danh sách chat hiển thị (chat-list component)
    // Trang KiroChat có route /chat sau khi đăng nhập
    await driver.wait(
      until.urlContains("/chat"),
      15000,
      "Không thể điều hướng đến trang chat"
    );

    // Click vào cuộc hội thoại đầu tiên trong danh sách
    // Component chat-list hiển thị các item conversation
    const firstConversation = await waitForElement(
      driver,
      By.xpath(
        '//div[contains(@class, "chat") or contains(@class, "conversation")]' +
          '[contains(@class, "item") or contains(@class, "list")]' +
          '//div[contains(@class, "cursor-pointer") or @role="button"][1]'
      ),
      15000
    );
    await firstConversation.click();
    console.log("✅ Đã click vào cuộc hội thoại đầu tiên");

    // Chờ vùng tin nhắn (message-area) hiển thị
    await waitForElement(driver, By.id("message-area"), 10000);
    console.log("✅ Vùng tin nhắn đã hiển thị");

    /**
     * ========================================================================
     * BƯỚC 2: ĐỊNH VỊ Ô NHẬP TIN NHẮN (TEXTAREA)
     * ========================================================================
     * KiroChat sử dụng component <app-message-input> chứa <textarea>
     * với các thuộc tính:
     *  - ngModel gắn vào signal content
     *  - placeholder="Type a message..."
     *  - Element reference #textarea
     */
    console.log("📌 Bước 2: Định vị ô nhập tin nhắn...");

    // Tìm textarea trong component message-input
    // Sử dụng XPath kết hợp nhiều tiêu chí để đảm bảo đúng element
    const messageTextarea = await waitForElementVisible(
      driver,
      By.xpath(
        '//app-message-input//textarea' +
          '[contains(@placeholder, "Type a message") or contains(@class, "rounded-lg")]'
      ),
      10000
    );
    console.log("✅ Đã tìm thấy ô nhập tin nhắn (textarea)");

    /**
     * ========================================================================
     * BƯỚC 3: TẠO CHUỖI 5001 KÝ TỰ VÀ NHẬP VÀO TEXTAREA
     * ========================================================================
     * Tạo chuỗi text dài chính xác 5001 ký tự (vượt giới hạn 5000).
     * Sử dụng JavaScript executeScript để nhập nhanh (sendKeys quá chậm
     * với chuỗi dài) và trigger sự kiện input để Angular detect thay đổi.
     */
    console.log(
      `📌 Bước 3: Nhập chuỗi ${EXCEEDED_LENGTH} ký tự vào textarea...`
    );

    // Tạo chuỗi test: 5001 ký tự 'a'
    const longMessage = generateString(EXCEEDED_LENGTH);
    console.log(`   📏 Độ dài chuỗi đã tạo: ${longMessage.length} ký tự`);

    // Verify chuỗi đúng 5001 ký tự trước khi nhập
    expect(longMessage.length).toBe(EXCEEDED_LENGTH);

    // Sử dụng JavaScript để nhập chuỗi dài (nhanh hơn sendKeys rất nhiều)
    // Đồng thời trigger sự kiện 'input' để Angular Change Detection hoạt động
    await driver.executeScript(
      `
      const textarea = arguments[0];
      // Xóa nội dung cũ
      textarea.value = '';
      // Gán giá trị mới
      textarea.value = arguments[1];
      // Trigger sự kiện input để Angular ngModel nhận giá trị
      textarea.dispatchEvent(new Event('input', { bubbles: true }));
      // Trigger sự kiện change để đảm bảo binding
      textarea.dispatchEvent(new Event('change', { bubbles: true }));
    `,
      messageTextarea,
      longMessage
    );
    console.log("✅ Đã nhập chuỗi 5001 ký tự vào textarea");

    // Verify textarea thực sự chứa chuỗi đã nhập
    const textareaValue = await messageTextarea.getAttribute("value");
    console.log(
      `   📏 Độ dài thực tế trong textarea: ${textareaValue.length} ký tự`
    );
    expect(textareaValue.length).toBeGreaterThan(MAX_MESSAGE_LENGTH);

    /**
     * ========================================================================
     * BƯỚC 4: BẤM NÚT GỬI TIN NHẮN
     * ========================================================================
     * Click vào nút Gửi (Send button) trong component message-input.
     * Nút Gửi có icon fa-paper-plane và aria-label "Send message".
     */
    console.log("📌 Bước 4: Bấm nút Gửi tin nhắn...");

    // Tìm nút Gửi bằng aria-label hoặc icon
    const sendButton = await waitForElement(
      driver,
      By.xpath(
        '//app-message-input//button' +
          '[contains(@aria-label, "Send message") or ' +
          './/i[contains(@class, "fa-paper-plane")]]'
      ),
      5000
    );

    // Click nút Gửi
    await sendButton.click();
    console.log("✅ Đã bấm nút Gửi");

    /**
     * ========================================================================
     * BƯỚC 5: XÁC MINH (VERIFY) - TOAST LỖI XUẤT HIỆN
     * ========================================================================
     * Sau khi bấm Gửi, hệ thống phải hiển thị Toast/Alert lỗi với nội dung:
     *   "Vượt quá giới hạn 5000 ký tự"
     *
     * Cách tìm Toast:
     *  - KiroChat sử dụng component <app-error-alert> hiển thị lỗi
     *  - Hoặc Toast/Snackbar từ notification service
     *  - Tìm bằng XPath chứa text mong đợi
     */
    console.log("📌 Bước 5: Xác minh Toast lỗi xuất hiện...");

    // Chờ Toast/Error Alert chứa nội dung lỗi xuất hiện
    const toastElement = await waitForToast(
      driver,
      EXPECTED_ERROR_TOAST,
      TOAST_WAIT_TIMEOUT
    );

    // Lấy text hiển thị của Toast
    const toastText = await toastElement.getText();
    console.log(`   📢 Nội dung Toast: "${toastText}"`);

    // ---- ASSERTION CHÍNH ----
    // Xác nhận Toast chứa nội dung lỗi mong đợi
    // Sử dụng expect().toBe() hoặc toContain() theo quy chuẩn
    expect(toastText).toContain(EXPECTED_ERROR_TOAST);
    console.log(
      `✅ PASS - Toast lỗi hiển thị đúng: "${EXPECTED_ERROR_TOAST}"`
    );

    /**
     * ========================================================================
     * BƯỚC 6: XÁC MINH BỔ SUNG - TIN NHẮN KHÔNG ĐƯỢC GỬI ĐI
     * ========================================================================
     * Kiểm tra thêm rằng:
     *  1. Textarea vẫn giữ nguyên nội dung (chưa bị xóa)
     *  2. Không có tin nhắn mới xuất hiện trong vùng chat (message-area)
     */
    console.log(
      "📌 Bước 6: Xác minh tin nhắn không được gửi đi..."
    );

    // Kiểm tra textarea vẫn còn nội dung (không bị reset)
    const textareaAfterSend =
      await messageTextarea.getAttribute("value");
    console.log(
      `   📏 Textarea sau khi bấm Gửi: ${textareaAfterSend.length} ký tự`
    );

    // Nội dung textarea phải vẫn còn (vì tin nhắn bị chặn, không gửi)
    expect(textareaAfterSend.length).toBeGreaterThan(0);
    console.log(
      "✅ PASS - Textarea vẫn giữ nguyên nội dung (tin nhắn không gửi)"
    );

    // Kiểm tra không có bubble tin nhắn mới chứa chuỗi 5001 ký tự
    // trong khu vực message-area
    const messageArea = await driver.findElement(By.id("message-area"));
    const lastMessageBubbles = await messageArea.findElements(
      By.css("app-message-bubble")
    );

    if (lastMessageBubbles.length > 0) {
      // Lấy tin nhắn cuối cùng
      const lastBubble =
        lastMessageBubbles[lastMessageBubbles.length - 1];
      const lastBubbleText = await lastBubble.getText();

      // Tin nhắn cuối KHÔNG được chứa chuỗi 5001 ký tự vừa nhập
      expect(lastBubbleText.length).not.toBe(EXCEEDED_LENGTH);
      console.log(
        "✅ PASS - Không có tin nhắn 5001 ký tự trong vùng chat"
      );
    }

    // ========================================================================
    // KẾT LUẬN: TC03 - PASS
    // ========================================================================
    console.log("═══════════════════════════════════════════════");
    console.log("✅ TC03 - PASS: Hệ thống đã chặn tin nhắn vượt quá 5000 ký tự");
    console.log("   📢 Toast lỗi hiển thị đúng nội dung mong đợi");
    console.log("   🚫 Tin nhắn không được gửi đi");
    console.log("═══════════════════════════════════════════════");
  });
});
