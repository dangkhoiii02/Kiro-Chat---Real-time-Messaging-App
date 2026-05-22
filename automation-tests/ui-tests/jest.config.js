/**
 * Jest Configuration cho KiroChat UI Automation Tests
 * Sử dụng Selenium WebDriver + Allure Report
 */
module.exports = {
  // Thư mục chứa test specs
  testMatch: ["**/specs/**/*.spec.js"],

  // Timeout dài hơn cho Selenium (30 giây mỗi test)
  testTimeout: 60000,

  // Sử dụng jest-circus runner (mặc định từ Jest 27+)
  testRunner: "jest-circus/runner",

  // Thiết lập reporter Allure
  reporters: [
    "default",
    [
      "jest-allure",
      {
        resultsDir: "allure-results",
      },
    ],
  ],

  // Không chạy song song (Selenium cần tuần tự)
  maxWorkers: 1,

  // Verbose output
  verbose: true,
};
