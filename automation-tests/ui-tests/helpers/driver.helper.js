/**
 * ============================================================================
 *  KiroChat - Lớp Helper cho Selenium WebDriver
 * ============================================================================
 *  Mục đích: Tập trung các hàm dùng chung (khởi tạo driver, login, chụp ảnh
 *  màn hình, đính kèm Allure Report...) để tránh lặp code giữa các test specs.
 * ============================================================================
 */

const { Builder, By, until } = require("selenium-webdriver");
const chrome = require("selenium-webdriver/chrome");
const fs = require("fs");
const path = require("path");

// ============================================================================
// HẰNG SỐ CẤU HÌNH
// ============================================================================

/** URL gốc của ứng dụng KiroChat Frontend (Angular, chạy trên localhost:4200) */
const BASE_URL = process.env.KIRO_BASE_URL || "http://localhost:4200";

/** URL Keycloak dùng cho xác thực OAuth2 */
const KEYCLOAK_URL = process.env.KEYCLOAK_URL || "http://localhost:9093";

/** Thời gian chờ mặc định cho WebDriverWait (ms) */
const DEFAULT_WAIT_TIMEOUT = 10000;

/** Tài khoản test mặc định (được tạo sẵn trong Keycloak) */
const TEST_USER = {
  username: process.env.TEST_USERNAME || "testuser1@gmail.com",
  password: process.env.TEST_PASSWORD || "example1",
};

// ============================================================================
// HÀM KHỞI TẠO WEBDRIVER
// ============================================================================

/**
 * Khởi tạo Chrome WebDriver với các options phù hợp cho automation testing.
 *
 * Cấu hình bao gồm:
 *  - Headless mode (không cần hiển thị trình duyệt khi chạy CI/CD)
 *  - Disable GPU (tránh lỗi trên một số hệ điều hành)
 *  - Window size cố định để đảm bảo responsive nhất quán
 *  - Disable sandbox (cần thiết cho Docker environment)
 *
 * @returns {WebDriver} Instance của Selenium Chrome WebDriver
 */
async function createDriver() {
  const options = new chrome.Options();

  // Chế độ headless - không mở giao diện trình duyệt
  // Comment dòng dưới nếu muốn xem trình duyệt chạy thực tế khi debug
  options.addArguments("--headless=new");

  // Các options bổ sung cho ổn định
  options.addArguments("--no-sandbox"); // Bắt buộc khi chạy trong Docker
  options.addArguments("--disable-dev-shm-usage"); // Tránh hết bộ nhớ /dev/shm
  options.addArguments("--disable-gpu"); // Tránh lỗi GPU trên một số OS
  options.addArguments("--window-size=1920,1080"); // Kích thước cửa sổ cố định
  options.addArguments("--disable-extensions"); // Tắt extension
  options.addArguments("--disable-popup-blocking"); // Tắt chặn popup

  // Xây dựng driver
  const driver = await new Builder()
    .forBrowser("chrome")
    .setChromeOptions(options)
    .build();

  // Thiết lập implicit wait
  await driver.manage().setTimeouts({ implicit: DEFAULT_WAIT_TIMEOUT });

  return driver;
}

// ============================================================================
// HÀM ĐĂNG NHẬP
// ============================================================================

/**
 * Thực hiện đăng nhập vào KiroChat thông qua Keycloak OAuth2.
 *
 * Flow đăng nhập:
 *  1. Truy cập trang chủ KiroChat → tự động redirect đến Keycloak
 *  2. Nhập username + password trên form Keycloak
 *  3. Submit → redirect về KiroChat đã đăng nhập
 *  4. Chờ trang chat hiển thị để xác nhận đăng nhập thành công
 *
 * @param {WebDriver} driver - Instance WebDriver đã khởi tạo
 * @param {string} username - Tên đăng nhập Keycloak
 * @param {string} password - Mật khẩu Keycloak
 */
async function loginToKiroChat(
  driver,
  username = TEST_USER.username,
  password = TEST_USER.password
) {
  // Bước 1: Truy cập trang chủ KiroChat
  await driver.get(BASE_URL);

  // Kiểm tra nếu đã đăng nhập sẵn
  try {
    const currentUrl = await driver.getCurrentUrl();
    const appRoot = await driver.findElements(By.css("app-root"));
    if (appRoot.length > 0 && !currentUrl.includes("auth")) {
      console.log("   ℹ️ Session active, bypassing login form.");
      await driver.get(BASE_URL + "/chat");
      return;
    }
  } catch (e) {
    // Bỏ qua nếu có lỗi check
  }

  // Bước 2: Chờ form đăng nhập Keycloak xuất hiện
  const usernameField = await driver.wait(
    until.elementLocated(By.id("username")),
    DEFAULT_WAIT_TIMEOUT,
    "Không tìm thấy field username trên trang đăng nhập Keycloak"
  );

  // Bước 3: Nhập thông tin đăng nhập
  await usernameField.clear();
  await usernameField.sendKeys(username);

  const passwordField = await driver.findElement(By.id("password"));
  await passwordField.clear();
  await passwordField.sendKeys(password);

  // Bước 4: Bấm nút đăng nhập
  // Keycloak button submit có id="kc-login"
  const loginButton = await driver.findElement(By.id("kc-login"));
  await loginButton.click();

  // Bước 5: Chờ redirect về KiroChat và trang chat hiển thị
  // Kiểm tra URL đã quay về BASE_URL (không còn ở Keycloak)
  await driver.wait(
    until.urlContains(BASE_URL.replace(/https?:\/\//, "")),
    DEFAULT_WAIT_TIMEOUT * 2,
    "Đăng nhập thất bại - không redirect về KiroChat"
  );

  // Chờ phần tử chính của ứng dụng chat hiển thị
  await driver.wait(
    until.elementLocated(By.css("app-root")),
    DEFAULT_WAIT_TIMEOUT,
    "Ứng dụng KiroChat không load sau khi đăng nhập"
  );

  // Điều hướng thẳng tới /chat để đảm bảo luôn ở trang chat
  await driver.get(BASE_URL + "/chat");

  // Chờ cho URL thực sự chứa /chat
  await driver.wait(
    until.urlContains("/chat"),
    DEFAULT_WAIT_TIMEOUT,
    "Không thể chuyển hướng sang trang /chat"
  );
}

// ============================================================================
// HÀM CHỤP ẢNH MÀN HÌNH (SCREENSHOT)
// ============================================================================

/**
 * Chụp ảnh màn hình hiện tại và lưu dưới dạng file PNG + base64.
 *
 * Mục đích:
 *  - Đính kèm vào Allure Report khi testcase FAIL
 *  - Giúp debug nhanh bằng cách xem trạng thái UI lúc lỗi
 *
 * @param {WebDriver} driver - Instance WebDriver
 * @param {string} testName - Tên testcase (dùng làm tên file ảnh)
 * @returns {string} Chuỗi base64 của ảnh chụp màn hình
 */
async function takeScreenshot(driver, testName) {
  try {
    // Chụp ảnh màn hình, Selenium trả về chuỗi base64
    const screenshot = await driver.takeScreenshot();

    // Tạo thư mục screenshots nếu chưa tồn tại
    const screenshotDir = path.join(__dirname, "..", "screenshots");
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true });
    }

    // Tạo tên file an toàn (thay thế ký tự đặc biệt)
    const safeFileName = testName.replace(/[^a-zA-Z0-9_-]/g, "_");
    const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
    const filePath = path.join(
      screenshotDir,
      `${safeFileName}_${timestamp}.png`
    );

    // Lưu file PNG từ base64
    fs.writeFileSync(filePath, screenshot, "base64");

    console.log(`📸 Screenshot saved: ${filePath}`);
    return screenshot; // Trả về base64 để đính kèm Allure
  } catch (error) {
    console.error(`❌ Không thể chụp ảnh màn hình: ${error.message}`);
    return null;
  }
}

// ============================================================================
// HÀM ĐÍNH KÈM SCREENSHOT VÀO ALLURE REPORT
// ============================================================================

/**
 * Ghi file base64 screenshot vào thư mục allure-results để Allure Report
 * tự động nhận diện và đính kèm.
 *
 * Allure đọc file attachment dựa trên format:
 *  - File .png trong thư mục allure-results
 *  - Reference trong file -result.json
 *
 * @param {string} base64Data - Chuỗi base64 của ảnh PNG
 * @param {string} name - Tên hiển thị trên Allure Report
 */
function attachScreenshotToAllure(base64Data, name = "Screenshot on Failure") {
  if (!base64Data) return;

  try {
    // Tạo thư mục allure-results nếu chưa tồn tại
    const allureDir = path.join(__dirname, "..", "allure-results");
    if (!fs.existsSync(allureDir)) {
      fs.mkdirSync(allureDir, { recursive: true });
    }

    // Tạo file attachment với UUID unique
    const uuid =
      Date.now().toString(36) + Math.random().toString(36).substring(2);
    const attachmentFile = path.join(allureDir, `${uuid}-attachment.png`);

    // Ghi file PNG
    fs.writeFileSync(attachmentFile, base64Data, "base64");

    console.log(`📎 Allure attachment saved: ${attachmentFile}`);
  } catch (error) {
    console.error(
      `❌ Không thể đính kèm screenshot vào Allure: ${error.message}`
    );
  }
}

// ============================================================================
// HÀM TIỆN ÍCH
// ============================================================================

/**
 * Chờ phần tử hiển thị trên trang với timeout tùy chỉnh.
 *
 * @param {WebDriver} driver - Instance WebDriver
 * @param {By} locator - Bộ định vị phần tử (By.id, By.xpath, By.css...)
 * @param {number} timeout - Thời gian chờ tối đa (ms)
 * @returns {WebElement} Phần tử đã tìm thấy
 */
async function waitForElement(
  driver,
  locator,
  timeout = DEFAULT_WAIT_TIMEOUT
) {
  return await driver.wait(until.elementLocated(locator), timeout);
}

/**
 * Chờ phần tử hiển thị (visible) trên trang.
 *
 * @param {WebDriver} driver - Instance WebDriver
 * @param {By} locator - Bộ định vị phần tử
 * @param {number} timeout - Thời gian chờ tối đa (ms)
 * @returns {WebElement} Phần tử đã visible
 */
async function waitForElementVisible(
  driver,
  locator,
  timeout = DEFAULT_WAIT_TIMEOUT
) {
  const element = await waitForElement(driver, locator, timeout);
  return await driver.wait(until.elementIsVisible(element), timeout);
}

/**
 * Chờ Toast/Notification xuất hiện với nội dung cụ thể.
 * KiroChat sử dụng component error-alert hoặc toast notification.
 *
 * @param {WebDriver} driver - Instance WebDriver
 * @param {string} expectedText - Nội dung Toast mong đợi
 * @param {number} timeout - Thời gian chờ (ms)
 * @returns {WebElement} Phần tử Toast tìm thấy
 */
async function waitForToast(
  driver,
  expectedText,
  timeout = DEFAULT_WAIT_TIMEOUT
) {
  // Tìm Toast/Error Alert bằng XPath chứa text mong đợi
  const toastLocator = By.xpath(
    `//*[contains(@class, 'error') or contains(@class, 'toast') or contains(@class, 'alert') or contains(@class, 'notification')]` +
      `[contains(text(), '${expectedText}') or .//*[contains(text(), '${expectedText}')]]`
  );

  return await driver.wait(
    until.elementLocated(toastLocator),
    timeout,
    `Toast với nội dung "${expectedText}" không xuất hiện trong ${timeout}ms`
  );
}

// ============================================================================
// XUẤT MODULE
// ============================================================================

module.exports = {
  createDriver,
  loginToKiroChat,
  takeScreenshot,
  attachScreenshotToAllure,
  waitForElement,
  waitForElementVisible,
  waitForToast,
  BASE_URL,
  KEYCLOAK_URL,
  DEFAULT_WAIT_TIMEOUT,
  TEST_USER,
  By,
  until,
};
