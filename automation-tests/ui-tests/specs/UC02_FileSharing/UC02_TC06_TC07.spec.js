/**
 * ============================================================================
 *  KIROCHAT - USECASE 02: CHIA SẺ TỆP TIN
 *  Test Cases UI: TC06, TC07
 * ============================================================================
 *  Công nghệ   : NodeJS + Jest + Selenium WebDriver + Allure Report
 *  Loại         : Black-box Testing (Giao diện)
 * ============================================================================
 *
 *  TC06 - Giao diện upload tệp 25.0 MB
 *    → Mong đợi: File được chấp nhận, giao diện hiển thị thanh tiến trình.
 *
 *  TC07 - Giao diện upload tệp 25.1 MB
 *    → Mong đợi: Giao diện chặn lại, hiển thị popup
 *      "Dung lượng tệp vượt quá giới hạn 25MB".
 *
 *  Điều kiện tiên quyết:
 *    1. Frontend (http://localhost:4200), Backend, Keycloak đang chạy
 *    2. Tài khoản test có ít nhất 1 cuộc hội thoại
 *    3. ChromeDriver khớp phiên bản Chrome
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

const fs = require("fs");
const path = require("path");

// ============================================================================
// HẰNG SỐ
// ============================================================================

/** Giới hạn dung lượng tệp (MB) */
const MAX_FILE_SIZE_MB = 25;

/** Kích thước file hợp lệ: đúng 25.0 MB (bytes) */
const VALID_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024; // 26 214 400 bytes

/** Kích thước file vượt giới hạn: 25.1 MB (bytes) */
const EXCEED_FILE_SIZE = Math.floor(25.1 * 1024 * 1024); // 26 319 462 bytes

/** Nội dung popup/toast mong đợi khi vượt giới hạn */
const EXCEED_ERROR_MSG = "Dung lượng tệp vượt quá giới hạn 25MB";

/** Thư mục chứa file tạm dùng cho test */
const TEMP_DIR = path.join(__dirname, "..", "..", "temp-test-files");

// ============================================================================
// LOCATORS
// ============================================================================

/** XPath nút đính kèm file (icon paperclip) */
const ATTACH_BTN_XPATH =
  '//app-message-input//button' +
  '[contains(@aria-label, "Attach file") or ' +
  './/i[contains(@class, "fa-paperclip")]]';

/** XPath input[type=file] ẩn bên trong <app-message-input> */
const FILE_INPUT_XPATH = '//app-message-input//input[@type="file"]';

/** XPath icon spinner (xuất hiện khi đang upload) */
const UPLOAD_SPINNER_XPATH =
  '//app-message-input//button' +
  '[contains(@aria-label, "Uploading")]' +
  '//i[contains(@class, "fa-spinner")]';

/** XPath hội thoại đầu tiên */
const FIRST_CONV_XPATH =
  '//div[contains(@class, "chat") or contains(@class, "conversation")]' +
  '[contains(@class, "item") or contains(@class, "list")]' +
  '//div[contains(@class, "cursor-pointer") or @role="button"][1]';

// ============================================================================
// BIẾN TOÀN CỤC
// ============================================================================
let driver;

// ============================================================================
// HÀM TẠO FILE TẠM
// ============================================================================

/**
 * Tạo file tạm với kích thước chính xác (bytes).
 * File được điền bằng ký tự null (\x00) để đạt đúng dung lượng.
 *
 * @param {string} fileName - Tên file
 * @param {number} sizeBytes - Kích thước mong muốn (bytes)
 * @returns {string} Đường dẫn tuyệt đối đến file đã tạo
 */
function createTempFile(fileName, sizeBytes) {
  if (!fs.existsSync(TEMP_DIR)) {
    fs.mkdirSync(TEMP_DIR, { recursive: true });
  }
  const filePath = path.join(TEMP_DIR, fileName);

  // Tạo buffer với kích thước chính xác
  // Dùng Buffer.alloc để đảm bảo đủ bytes (nhanh hơn fs.write)
  const buffer = Buffer.alloc(sizeBytes, 0);
  fs.writeFileSync(filePath, buffer);

  const actualSize = fs.statSync(filePath).size;
  console.log(
    `   📁 Tạo file: ${fileName} (${(actualSize / 1024 / 1024).toFixed(2)} MB)`
  );
  return filePath;
}

/**
 * Xóa thư mục file tạm sau khi test xong.
 */
function cleanupTempFiles() {
  try {
    if (fs.existsSync(TEMP_DIR)) {
      fs.rmSync(TEMP_DIR, { recursive: true, force: true });
      console.log("🧹 Đã dọn dẹp file tạm");
    }
  } catch (e) {
    console.warn("⚠️ Không thể dọn file tạm:", e.message);
  }
}

// ============================================================================
// HÀM TIỆN ÍCH
// ============================================================================

/**
 * Mở cuộc hội thoại đầu tiên.
 */
async function openFirstConversation() {
  await driver.wait(
    until.urlContains("/chat"),
    15000,
    "Không thể điều hướng đến trang chat"
  );
  const conv = await waitForElement(driver, By.xpath(FIRST_CONV_XPATH), 15000);
  await conv.click();
  await waitForElement(driver, By.id("message-area"), 10000);
}

/**
 * Gửi file đến input[type=file] ẩn bằng sendKeys.
 * Selenium cho phép sendKeys trên input[type=file] ngay cả khi hidden.
 *
 * @param {string} filePath - Đường dẫn tuyệt đối đến file
 */
async function uploadFileViaInput(filePath) {
  // Tìm input[type=file] ẩn
  const fileInput = await driver.findElement(By.xpath(FILE_INPUT_XPATH));

  // Selenium đặc biệt: sendKeys trên input[type=file] = chọn file
  // Không cần click nút Attach, gửi trực tiếp path file
  await fileInput.sendKeys(filePath);
}

// ============================================================================
// TEST SUITE
// ============================================================================

describe("UC02 - Chia sẻ tệp tin (TC06, TC07)", () => {
  // --------------------------------------------------------------------------
  // SETUP / TEARDOWN
  // --------------------------------------------------------------------------

  beforeEach(async () => {
    driver = await createDriver();
    console.log("✅ WebDriver khởi tạo");
    await loginToKiroChat(driver);
    console.log("✅ Đăng nhập thành công");
  });

  /**
   * afterEach BẮT BUỘC: Screenshot khi FAIL + base64 Allure attachment
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
          attachScreenshotToAllure(b64, `FAIL - ${testName}`);
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

  /**
   * Dọn dẹp file tạm sau toàn bộ suite
   */
  afterAll(() => {
    cleanupTempFiles();
  });

  // ==========================================================================
  //  TC06 - Upload tệp 25.0 MB → Chấp nhận, hiển thị thanh tiến trình
  // ==========================================================================

  test("TC06 - Giao diện upload tệp 25.0 MB: file được chấp nhận, hiển thị tiến trình", async () => {
    /**
     * BƯỚC 1: Tạo file tạm 25.0 MB
     * Tạo file nhị phân có kích thước chính xác 25 * 1024 * 1024 bytes.
     * Tên file: test_25mb.dat
     */
    console.log("📌 TC06 Bước 1: Tạo file tạm 25.0 MB...");
    const filePath = createTempFile("test_25mb.dat", VALID_FILE_SIZE);

    // Verify kích thước chính xác
    const fileStats = fs.statSync(filePath);
    expect(fileStats.size).toBe(VALID_FILE_SIZE);
    console.log(`✅ File tạm: ${(fileStats.size / 1024 / 1024).toFixed(2)} MB`);

    /**
     * BƯỚC 2: Mở cuộc hội thoại
     */
    console.log("📌 TC06 Bước 2: Mở cuộc hội thoại...");
    await openFirstConversation();
    console.log("✅ Đã mở hội thoại");

    /**
     * BƯỚC 3: Xác minh nút đính kèm file (paperclip) hiển thị
     * Nút này nằm trong <app-message-input>, có aria-label="Attach file"
     * và icon fa-paperclip.
     */
    console.log("📌 TC06 Bước 3: Xác minh nút Attach file...");
    const attachBtn = await waitForElementVisible(
      driver,
      By.xpath(ATTACH_BTN_XPATH),
      5000
    );
    const attachDisabled = await attachBtn.getAttribute("disabled");
    expect(attachDisabled).toBeNull(); // Nút phải enabled
    console.log("✅ Nút Attach file hiển thị và enabled");

    /**
     * BƯỚC 4: Gửi file vào input[type=file]
     * Selenium cho phép sendKeys() trên input[type=file] để chọn file
     * mà không cần mở dialog. Angular sẽ trigger sự kiện (change).
     *
     * Trong source (message-input.ts dòng 96-110):
     *   onFileSelected(event) → kiểm tra file.size > maxSize → emit fileSelect
     */
    console.log("📌 TC06 Bước 4: Upload file 25.0 MB qua input[type=file]...");
    await uploadFileViaInput(filePath);
    console.log("✅ File đã được gửi vào input");

    /**
     * BƯỚC 5: ASSERTION CHÍNH - Giao diện hiển thị trạng thái upload
     * Khi file hợp lệ, Angular component emit fileSelect → parent gọi
     * uploadAttachment → isUploading = true → hiển thị spinner.
     *
     * Kiểm tra:
     *   a) Nút Attach chuyển thành spinner (fa-spinner fa-spin)
     *   b) HOẶC aria-label chuyển thành "Uploading file"
     *   c) HOẶC có progress indicator xuất hiện
     */
    console.log("📌 TC06 Bước 5: Xác minh giao diện hiển thị tiến trình upload...");

    // Chờ spinner hoặc trạng thái uploading xuất hiện
    // Timeout 10 giây vì Angular cần thời gian xử lý file
    const uploadIndicator = await driver.wait(
      until.elementLocated(
        By.xpath(
          '//app-message-input//button[contains(@aria-label, "Uploading")]' +
            ' | //app-message-input//i[contains(@class, "fa-spinner")]' +
            ' | //*[contains(@class, "progress") or contains(@class, "uploading")]'
        )
      ),
      10000,
      "Giao diện không hiển thị trạng thái upload/tiến trình sau 10 giây"
    );

    // Xác minh element upload indicator thực sự hiển thị
    const isDisplayed = await uploadIndicator.isDisplayed();
    expect(isDisplayed).toBe(true);
    console.log("✅ Giao diện hiển thị trạng thái upload (spinner/progress)");

    /**
     * BƯỚC 6: Xác minh không có popup/toast lỗi
     * Nếu file hợp lệ → không được có thông báo lỗi.
     */
    console.log("📌 TC06 Bước 6: Xác minh không có popup lỗi...");
    let errorFound = false;
    try {
      // Chờ tối đa 2 giây xem có toast lỗi không
      await driver.wait(
        until.elementLocated(
          By.xpath(
            `//*[contains(@class, 'error') or contains(@class, 'toast')]` +
              `[contains(text(), 'vượt quá') or contains(text(), 'giới hạn')]`
          )
        ),
        2000
      );
      errorFound = true;
    } catch (_) {
      // Timeout = không tìm thấy toast lỗi = ĐÚNG hành vi mong đợi
      errorFound = false;
    }
    expect(errorFound).toBe(false);
    console.log("✅ Không có popup lỗi → file 25.0 MB được chấp nhận");

    console.log("═══════════════════════════════════════");
    console.log("✅ TC06 PASS - File 25.0 MB upload thành công, có tiến trình");
    console.log("═══════════════════════════════════════");
  });

  // ==========================================================================
  //  TC07 - Upload tệp 25.1 MB → Chặn, hiển thị popup lỗi
  // ==========================================================================

  test("TC07 - Giao diện upload tệp 25.1 MB: chặn và hiển thị popup lỗi dung lượng", async () => {
    /**
     * BƯỚC 1: Tạo file tạm 25.1 MB
     * 25.1 MB = 25.1 * 1024 * 1024 = 26 319 462 bytes
     * Vượt quá giới hạn 25 MB → hệ thống phải chặn.
     */
    console.log("📌 TC07 Bước 1: Tạo file tạm 25.1 MB...");
    const filePath = createTempFile("test_25_1mb.dat", EXCEED_FILE_SIZE);

    const fileStats = fs.statSync(filePath);
    expect(fileStats.size).toBe(EXCEED_FILE_SIZE);
    expect(fileStats.size).toBeGreaterThan(VALID_FILE_SIZE);
    console.log(`✅ File tạm: ${(fileStats.size / 1024 / 1024).toFixed(2)} MB (vượt giới hạn)`);

    /**
     * BƯỚC 2: Mở cuộc hội thoại
     */
    console.log("📌 TC07 Bước 2: Mở cuộc hội thoại...");
    await openFirstConversation();
    console.log("✅ Đã mở hội thoại");

    /**
     * BƯỚC 3: Đếm số tin nhắn hiện tại (dùng so sánh sau)
     */
    console.log("📌 TC07 Bước 3: Đếm tin nhắn hiện tại...");
    const area = await driver.findElement(By.id("message-area"));
    const bubblesBefore = await area.findElements(By.css("app-message-bubble"));
    const countBefore = bubblesBefore.length;
    console.log(`   📊 Số tin nhắn hiện tại: ${countBefore}`);

    /**
     * BƯỚC 4: Upload file 25.1 MB
     * Trong source (message-input.ts dòng 102-106):
     *   const maxSize = this.maxFileSizeMB() * 1024 * 1024;
     *   if (file.size > maxSize) {
     *     throw new Error(`File too large. Max size: ${this.maxFileSizeMB()}MB`);
     *   }
     */
    console.log("📌 TC07 Bước 4: Upload file 25.1 MB...");
    await uploadFileViaInput(filePath);
    console.log("✅ File đã được gửi vào input");

    // Chờ Angular xử lý sự kiện change → validation
    await driver.sleep(1500);

    /**
     * BƯỚC 5: ASSERTION CHÍNH - Popup/Toast lỗi xuất hiện
     * Mong đợi popup/alert/toast chứa nội dung:
     *   "Dung lượng tệp vượt quá giới hạn 25MB"
     *
     * KiroChat có thể hiển thị lỗi qua:
     *   a) <app-error-alert> component (trong direct-conversation.html dòng 59-61)
     *   b) Toast notification
     *   c) JavaScript alert (throw Error sẽ bị Angular error handler bắt)
     */
    console.log("📌 TC07 Bước 5: Xác minh popup lỗi xuất hiện...");

    // Tìm popup/toast/alert lỗi chứa thông báo vượt giới hạn
    const errorPopup = await driver.wait(
      until.elementLocated(
        By.xpath(
          `//*[` +
            `contains(@class, 'error') or contains(@class, 'toast') or ` +
            `contains(@class, 'alert') or contains(@class, 'popup') or ` +
            `contains(@class, 'notification') or contains(@class, 'snackbar')` +
            `]` +
            `[` +
            `contains(text(), '${EXCEED_ERROR_MSG}') or ` +
            `.//*[contains(text(), '${EXCEED_ERROR_MSG}')] or ` +
            `contains(text(), 'vượt quá') or ` +
            `.//*[contains(text(), 'vượt quá')] or ` +
            `contains(text(), 'too large') or ` +
            `.//*[contains(text(), 'too large')]` +
            `]`
        )
      ),
      10000,
      "Popup/Toast lỗi dung lượng tệp không xuất hiện trong 10 giây"
    );

    const popupText = await errorPopup.getText();
    console.log(`   📢 Popup lỗi: "${popupText}"`);

    // Popup phải chứa nội dung lỗi liên quan đến dung lượng
    const hasExpectedMsg =
      popupText.includes(EXCEED_ERROR_MSG) ||
      popupText.includes("vượt quá") ||
      popupText.includes("too large") ||
      popupText.includes("giới hạn");

    expect(hasExpectedMsg).toBe(true);
    console.log("✅ Popup lỗi hiển thị đúng nội dung");

    /**
     * BƯỚC 6: Xác minh KHÔNG có spinner upload
     * File bị chặn → không được bắt đầu upload → không có spinner.
     */
    console.log("📌 TC07 Bước 6: Xác minh không có spinner upload...");
    let spinnerFound = false;
    try {
      await driver.wait(
        until.elementLocated(By.xpath(UPLOAD_SPINNER_XPATH)),
        2000
      );
      spinnerFound = true;
    } catch (_) {
      spinnerFound = false;
    }
    expect(spinnerFound).toBe(false);
    console.log("✅ Không có spinner → file bị chặn trước khi upload");

    /**
     * BƯỚC 7: Xác minh không có tin nhắn file mới trong khung chat
     */
    console.log("📌 TC07 Bước 7: Xác minh không có tin nhắn file mới...");
    const bubblesAfter = await area.findElements(By.css("app-message-bubble"));
    expect(bubblesAfter.length).toBe(countBefore);
    console.log("✅ Không có tin nhắn file mới → file đã bị chặn hoàn toàn");

    console.log("═══════════════════════════════════════");
    console.log("✅ TC07 PASS - File 25.1 MB bị chặn, popup lỗi hiển thị");
    console.log("═══════════════════════════════════════");
  });
});
