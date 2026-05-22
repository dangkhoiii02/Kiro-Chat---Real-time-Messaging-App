/**
 * ============================================================================
 *  KIROCHAT - USECASE 01: NHẮN TIN THỜI GIAN THỰC
 *  Test Cases UI: TC01, TC02, TC03
 * ============================================================================
 *  Công nghệ   : NodeJS + Jest + Selenium WebDriver + Allure Report
 *  Loại         : Black-box Testing (Giao diện)
 * ============================================================================
 *
 *  TC01 - Giao diện gửi tin rỗng
 *    → Mong đợi: Nút gửi bị disabled.
 *
 *  TC02 - Giao diện gửi chuỗi đúng 5000 ký tự
 *    → Mong đợi: Gửi thành công, tin nhắn hiện lên khung chat.
 *
 *  TC03 - Giao diện gửi chuỗi 5001 ký tự
 *    → Mong đợi: Hiện Toast báo lỗi "Vượt quá giới hạn 5000 ký tự".
 *
 *  Điều kiện tiên quyết:
 *    1. Frontend (http://localhost:4200), Backend, Keycloak đang chạy
 *    2. Tài khoản test đã tạo trong Keycloak
 *    3. Tài khoản test có ít nhất 1 cuộc hội thoại
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
// HẰNG SỐ DÙNG CHUNG
// ============================================================================

/** Giới hạn ký tự tối đa cho 1 tin nhắn */
const MAX_CHAR = 5000;

/** Nội dung Toast lỗi mong đợi khi vượt giới hạn */
const ERROR_TOAST_TEXT = "Vượt quá giới hạn 5000 ký tự";

/**
 * Tạo chuỗi ký tự với độ dài chính xác.
 * @param {number} len - Số ký tự cần tạo
 * @returns {string}
 */
const genStr = (len) => "a".repeat(len);

// ============================================================================
// LOCATORS TÁI SỬ DỤNG
// ============================================================================

/** XPath đến textarea nhập tin nhắn bên trong <app-message-input> */
const TEXTAREA_XPATH =
  '//app-message-input//textarea' +
  '[contains(@placeholder, "Type a message") or contains(@class, "rounded-lg")]';

/** XPath đến nút Gửi (icon fa-paper-plane) bên trong <app-message-input> */
const SEND_BTN_XPATH =
  '//app-message-input//button' +
  '[contains(@aria-label, "Send message") or ' +
  './/i[contains(@class, "fa-paper-plane")]]';

/** XPath đến item hội thoại đầu tiên trong danh sách chat */
const FIRST_CONVERSATION_XPATH =
  '//div[contains(@class, "chat") or contains(@class, "conversation")]' +
  '[contains(@class, "item") or contains(@class, "list")]' +
  '//div[contains(@class, "cursor-pointer") or @role="button"][1]';

// ============================================================================
// BIẾN TOÀN CỤC
// ============================================================================
let driver;

// ============================================================================
// HÀM TIỆN ÍCH NỘI BỘ
// ============================================================================

/**
 * Mở cuộc hội thoại đầu tiên và chờ message-area hiển thị.
 * Dùng chung cho cả 3 test.
 */
async function navigateToFirstConversation() {
  // Chờ trang chat load
  await driver.wait(
    until.urlContains("/chat"),
    15000,
    "Không thể điều hướng đến trang chat"
  );

  // Click hội thoại đầu tiên
  const conv = await waitForElement(
    driver,
    By.xpath(FIRST_CONVERSATION_XPATH),
    15000
  );
  await conv.click();

  // Chờ vùng tin nhắn
  await waitForElement(driver, By.id("message-area"), 10000);
}

/**
 * Nhập chuỗi vào textarea bằng JS (nhanh hơn sendKeys với chuỗi dài)
 * và trigger Angular change-detection.
 */
async function typeInTextarea(textarea, text) {
  await driver.executeScript(
    `
    const el = arguments[0];
    el.value = '';
    el.value = arguments[1];
    el.dispatchEvent(new Event('input',  { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    `,
    textarea,
    text
  );
  // Chờ Angular digest
  await driver.sleep(500);
}

// ============================================================================
// DESCRIBE BLOCK CHÍNH
// ============================================================================

describe("UC01 - Nhắn tin thời gian thực (TC01, TC02, TC03)", () => {
  // --------------------------------------------------------------------------
  // SETUP / TEARDOWN
  // --------------------------------------------------------------------------

  /**
   * Trước mỗi test: khởi tạo driver mới → đăng nhập Keycloak
   */
  beforeEach(async () => {
    driver = await createDriver();
    console.log("✅ WebDriver khởi tạo");
    await loginToKiroChat(driver);
    console.log("✅ Đăng nhập thành công");
  });

  /**
   * Sau mỗi test (BẮT BUỘC theo quy chuẩn):
   *   - Nếu FAIL → chụp screenshot, lưu base64, đính kèm Allure
   *   - Luôn quit driver
   */
  afterEach(async () => {
    try {
      const testName = expect.getState().currentTestName || "unknown";
      const failed =
        expect.getState().numPassingAsserts === 0 ||
        expect.getState().suppressedErrors?.length > 0;

      if (failed) {
        console.log(`❌ FAIL "${testName}" → chụp screenshot...`);
        const b64 = await takeScreenshot(driver, testName);
        if (b64) {
          // Đính kèm Allure
          attachScreenshotToAllure(b64, `FAIL - ${testName}`);

          // Ghi file base64 thuần (yêu cầu đồ án)
          const fs = require("fs");
          const path = require("path");
          const dir = path.join(__dirname, "..", "..", "screenshots", "base64");
          if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
          const safe = testName.replace(/[^a-zA-Z0-9_-]/g, "_");
          fs.writeFileSync(path.join(dir, `${safe}.txt`), b64, "utf-8");
          console.log(`💾 Base64 → ${safe}.txt`);
        }
      } else {
        console.log(`✅ PASS "${testName}"`);
      }
    } catch (e) {
      console.error("⚠️ Screenshot error:", e.message);
    } finally {
      if (driver) {
        await driver.quit();
        console.log("🔒 WebDriver đóng");
      }
    }
  });

  // ==========================================================================
  //  TC01 - Giao diện gửi tin rỗng → Nút gửi disabled
  // ==========================================================================

  test("TC01 - Giao diện gửi tin rỗng: nút Gửi phải bị disabled", async () => {
    /**
     * BƯỚC 1: Mở cuộc hội thoại đầu tiên
     */
    console.log("📌 TC01 Bước 1: Mở cuộc hội thoại...");
    await navigateToFirstConversation();
    console.log("✅ Đã mở hội thoại");

    /**
     * BƯỚC 2: Tìm textarea và nút Gửi
     * Trong source Angular (message-input.ts dòng 64):
     *   canSend = () => this.content().trim().length > 0 && !isSending() && !isDisabled()
     * → content rỗng ⇒ canSend = false ⇒ button [disabled]="!canSend()"
     */
    console.log("📌 TC01 Bước 2: Định vị textarea và nút Gửi...");
    const textarea = await waitForElementVisible(
      driver,
      By.xpath(TEXTAREA_XPATH),
      10000
    );
    const sendBtn = await waitForElement(
      driver,
      By.xpath(SEND_BTN_XPATH),
      5000
    );
    console.log("✅ Tìm thấy textarea + nút Gửi");

    /**
     * BƯỚC 3: Xác minh textarea rỗng (mặc định)
     */
    console.log("📌 TC01 Bước 3: Xác minh textarea rỗng...");
    const currentValue = await textarea.getAttribute("value");
    expect(currentValue.trim().length).toBe(0);
    console.log("✅ Textarea rỗng (length = 0)");

    /**
     * BƯỚC 4: ASSERTION CHÍNH - Nút Gửi phải disabled
     * getAttribute("disabled") trả về "true" khi disabled, null khi enabled.
     */
    console.log("📌 TC01 Bước 4: Kiểm tra nút Gửi disabled...");
    const isDisabled = await sendBtn.getAttribute("disabled");
    expect(isDisabled).toBe("true");
    console.log("✅ Nút Gửi DISABLED khi tin rỗng");

    /**
     * BƯỚC 5: Thử nhập chỉ khoảng trắng → vẫn disabled
     */
    console.log("📌 TC01 Bước 5: Nhập khoảng trắng → kiểm tra vẫn disabled...");
    await typeInTextarea(textarea, "     ");
    const stillDisabled = await sendBtn.getAttribute("disabled");
    expect(stillDisabled).toBe("true");
    console.log("✅ Nút Gửi vẫn DISABLED khi chỉ có whitespace");

    /**
     * BƯỚC 6: Đếm tin nhắn → không có tin mới
     */
    console.log("📌 TC01 Bước 6: Xác minh không có tin nhắn mới...");
    const area = await driver.findElement(By.id("message-area"));
    const bubblesBefore = await area.findElements(By.css("app-message-bubble"));
    // Force-click (dù disabled) để chắc chắn không gửi
    try { await driver.executeScript("arguments[0].click()", sendBtn); } catch (_) {}
    await driver.sleep(1000);
    const bubblesAfter = await area.findElements(By.css("app-message-bubble"));
    expect(bubblesAfter.length).toBe(bubblesBefore.length);
    console.log("✅ Không có tin nhắn mới → TC01 PASS");

    console.log("═══════════════════════════════════════");
    console.log("✅ TC01 PASS - Nút gửi bị disabled khi tin rỗng");
    console.log("═══════════════════════════════════════");
  });

  // ==========================================================================
  //  TC02 - Giao diện gửi chuỗi đúng 5000 ký tự → Gửi thành công
  // ==========================================================================

  test("TC02 - Giao diện gửi chuỗi đúng 5000 ký tự: tin nhắn hiện lên khung chat", async () => {
    /**
     * BƯỚC 1: Mở hội thoại
     */
    console.log("📌 TC02 Bước 1: Mở cuộc hội thoại...");
    await navigateToFirstConversation();
    console.log("✅ Đã mở hội thoại");

    /**
     * BƯỚC 2: Đếm số tin nhắn hiện tại (dùng so sánh sau)
     */
    console.log("📌 TC02 Bước 2: Đếm tin nhắn hiện tại...");
    const area = await driver.findElement(By.id("message-area"));
    const bubblesBefore = await area.findElements(By.css("app-message-bubble"));
    const countBefore = bubblesBefore.length;
    console.log(`   📊 Số tin nhắn hiện tại: ${countBefore}`);

    /**
     * BƯỚC 3: Nhập đúng 5000 ký tự vào textarea
     * Sử dụng executeScript (nhanh hơn sendKeys cho chuỗi dài).
     */
    console.log("📌 TC02 Bước 3: Nhập chuỗi 5000 ký tự...");
    const textarea = await waitForElementVisible(
      driver,
      By.xpath(TEXTAREA_XPATH),
      10000
    );
    const msg5000 = genStr(MAX_CHAR);
    expect(msg5000.length).toBe(5000); // pre-check
    await typeInTextarea(textarea, msg5000);

    // Verify textarea nhận đủ ký tự
    const val = await textarea.getAttribute("value");
    console.log(`   📏 Textarea length: ${val.length}`);
    expect(val.length).toBe(MAX_CHAR);

    /**
     * BƯỚC 4: Xác minh nút Gửi ENABLED (vì 5000 ≤ giới hạn)
     */
    console.log("📌 TC02 Bước 4: Xác minh nút Gửi enabled...");
    const sendBtn = await waitForElement(
      driver,
      By.xpath(SEND_BTN_XPATH),
      5000
    );
    const isDisabled = await sendBtn.getAttribute("disabled");
    expect(isDisabled).toBeNull(); // null = không disabled
    console.log("✅ Nút Gửi ENABLED");

    /**
     * BƯỚC 5: Click nút Gửi
     */
    console.log("📌 TC02 Bước 5: Bấm nút Gửi...");
    await sendBtn.click();
    console.log("✅ Đã bấm Gửi");

    /**
     * BƯỚC 6: Chờ tin nhắn mới xuất hiện trong khung chat
     * Sau khi gửi thành công, Angular thêm <app-message-bubble> mới
     * vào #message-area. Ta chờ số bubble tăng lên.
     */
    console.log("📌 TC02 Bước 6: Chờ tin nhắn mới hiển thị...");
    await driver.wait(async () => {
      const current = await area.findElements(By.css("app-message-bubble"));
      return current.length > countBefore;
    }, 15000, "Tin nhắn mới không xuất hiện trong khung chat sau 15 giây");

    // Đếm lại
    const bubblesAfter = await area.findElements(By.css("app-message-bubble"));
    const countAfter = bubblesAfter.length;
    console.log(`   📊 Số tin nhắn sau gửi: ${countAfter}`);

    // ASSERTION CHÍNH: Có thêm ít nhất 1 tin nhắn mới
    expect(countAfter).toBeGreaterThan(countBefore);
    console.log("✅ Tin nhắn mới đã hiển thị trong khung chat");

    /**
     * BƯỚC 7: Xác minh textarea đã được reset (clear) sau khi gửi
     * Trong source (message-input.ts dòng 91): this.content.set('')
     */
    console.log("📌 TC02 Bước 7: Xác minh textarea đã reset...");
    const valAfter = await textarea.getAttribute("value");
    expect(valAfter.length).toBe(0);
    console.log("✅ Textarea đã reset về rỗng");

    console.log("═══════════════════════════════════════");
    console.log("✅ TC02 PASS - Chuỗi 5000 ký tự gửi thành công");
    console.log("═══════════════════════════════════════");
  });

  // ==========================================================================
  //  TC03 - Giao diện gửi chuỗi 5001 ký tự → Toast lỗi
  // ==========================================================================

  test("TC03 - Giao diện gửi chuỗi 5001 ký tự: hiện Toast lỗi vượt giới hạn", async () => {
    /**
     * BƯỚC 1: Mở hội thoại
     */
    console.log("📌 TC03 Bước 1: Mở cuộc hội thoại...");
    await navigateToFirstConversation();
    console.log("✅ Đã mở hội thoại");

    /**
     * BƯỚC 2: Nhập chuỗi 5001 ký tự vào textarea
     */
    console.log("📌 TC03 Bước 2: Nhập chuỗi 5001 ký tự...");
    const textarea = await waitForElementVisible(
      driver,
      By.xpath(TEXTAREA_XPATH),
      10000
    );
    const msg5001 = genStr(MAX_CHAR + 1);
    expect(msg5001.length).toBe(5001); // pre-check
    await typeInTextarea(textarea, msg5001);

    const val = await textarea.getAttribute("value");
    console.log(`   📏 Textarea length: ${val.length}`);
    expect(val.length).toBeGreaterThan(MAX_CHAR);

    /**
     * BƯỚC 3: Bấm nút Gửi
     */
    console.log("📌 TC03 Bước 3: Bấm nút Gửi...");
    const sendBtn = await waitForElement(
      driver,
      By.xpath(SEND_BTN_XPATH),
      5000
    );
    await sendBtn.click();
    console.log("✅ Đã bấm Gửi");

    /**
     * BƯỚC 4: ASSERTION CHÍNH - Toast lỗi xuất hiện
     * Mong đợi Toast/Error-alert chứa: "Vượt quá giới hạn 5000 ký tự"
     */
    console.log("📌 TC03 Bước 4: Chờ Toast lỗi xuất hiện...");
    const toast = await waitForToast(driver, ERROR_TOAST_TEXT, 10000);
    const toastText = await toast.getText();
    console.log(`   📢 Toast: "${toastText}"`);

    expect(toastText).toContain(ERROR_TOAST_TEXT);
    console.log("✅ Toast lỗi đúng nội dung mong đợi");

    /**
     * BƯỚC 5: Xác minh textarea KHÔNG bị reset (tin chưa gửi)
     */
    console.log("📌 TC03 Bước 5: Xác minh textarea còn nội dung...");
    const valAfter = await textarea.getAttribute("value");
    expect(valAfter.length).toBeGreaterThan(0);
    console.log("✅ Textarea vẫn giữ nội dung (tin nhắn bị chặn, không gửi)");

    /**
     * BƯỚC 6: Xác minh không có tin nhắn 5001 ký tự trong khung chat
     */
    console.log("📌 TC03 Bước 6: Xác minh tin nhắn không xuất hiện...");
    const area = await driver.findElement(By.id("message-area"));
    const bubbles = await area.findElements(By.css("app-message-bubble"));
    if (bubbles.length > 0) {
      const lastText = await bubbles[bubbles.length - 1].getText();
      expect(lastText.length).not.toBe(5001);
    }
    console.log("✅ Không có tin nhắn 5001 ký tự trong khung chat");

    console.log("═══════════════════════════════════════");
    console.log("✅ TC03 PASS - Toast lỗi hiển thị đúng khi vượt 5000 ký tự");
    console.log("═══════════════════════════════════════");
  });
});
