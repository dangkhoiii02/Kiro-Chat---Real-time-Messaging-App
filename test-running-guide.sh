#!/bin/bash
# ============================================================================
#  KIRO CHAT - AUTOMATED TEST SUITE RUNNER
#  Tự động chạy toàn bộ Test Cases (API & UI) trong thư mục automation-tests
# ============================================================================
#
#  Cách chạy:
#    chmod +x test-running-guide.sh
#    ./test-running-guide.sh
#
# ============================================================================

# Màu sắc
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ============================================================================
# HÀM TIỆN ÍCH
# ============================================================================

print_header() {
  echo ""
  echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_section() {
  echo ""
  echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
  echo -e "${BOLD}  $1${NC}"
  echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
}

# Hàm hiển thị spinner trong khi lệnh chạy ngầm
spinner() {
  local pid=$1
  local delay=0.15
  local spinstr='|/-\'
  # Đảm bảo hiển thị cursor ẩn trong quá trình chạy spinner
  tput civis
  while [ "$(ps -p "$pid" -o state= 2>/dev/null)" ]; do
    local temp=${spinstr#?}
    printf "  ${YELLOW}[%c]${NC} Đang kiểm thử..." "$spinstr"
    local spinstr=$temp${spinstr%"$temp"}
    sleep $delay
    printf "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b"
  done
  printf "                        \b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b"
  tput cnorm
}

check_port() {
  if lsof -i :"$1" -sTCP:LISTEN >/dev/null 2>&1; then
    return 0
  elif nc -z localhost "$1" 2>/dev/null; then
    return 0
  else
    return 1
  fi
}

# ============================================================================
# BẮT ĐẦU: KIỂM TRA MÔI TRƯỜNG TRƯỚC KHI CHẠY (PRE-CHECKS)
# ============================================================================

print_header "KIRO CHAT — AUTOMATED TEST RUNNER"
echo -e "  Thời gian: $(date '+%Y-%m-%d %H:%M:%S')"

# Clean up screenshots and temp files
echo -e "\n${CYAN}🧹 Dọn dẹp thư mục screenshots và file tạm cũ...${NC}"
rm -rf "${SCRIPT_DIR}/automation-tests/ui-tests/screenshots"/* 2>/dev/null
rm -rf "${SCRIPT_DIR}/automation-tests/ui-tests/temp_files"/* 2>/dev/null
echo -e "  ${GREEN}✅ Đã dọn dẹp xong.${NC}"

print_section "Kiểm tra trạng thái các dịch vụ (Prerequisites)"

SERVICES_OK=true

# Check Keycloak
if ! check_port 9093; then
  echo -e "  ${RED}❌ Dịch vụ Keycloak (Port 9093) KHÔNG HOẠT ĐỘNG!${NC}"
  SERVICES_OK=false
fi

# Check MinIO
if ! check_port 9000; then
  echo -e "  ${RED}❌ Dịch vụ MinIO (Port 9000) KHÔNG HOẠT ĐỘNG!${NC}"
  SERVICES_OK=false
fi

# Check Backend
if ! check_port 8080; then
  echo -e "  ${RED}❌ Backend Spring Boot (Port 8080) KHÔNG HOẠT ĐỘNG!${NC}"
  SERVICES_OK=false
fi

# Check Frontend
if ! check_port 4200; then
  echo -e "  ${RED}❌ Frontend Angular (Port 4200) KHÔNG HOẠT ĐỘNG!${NC}"
  SERVICES_OK=false
fi

if [ "$SERVICES_OK" = false ]; then
  echo -e "\n${RED}${BOLD}🚨 LỖI: Vui lòng đảm bảo các dịch vụ trong RUNNING_GUIDE.md đã chạy hoàn chỉnh trước khi bắt đầu test!${NC}"
  echo -e "  Hướng dẫn nhanh:"
  echo -e "    1. docker compose up -d (trong thư mục dự án)"
  echo -e "    2. cd kiro-backend && ./gradlew bootRun"
  echo -e "    3. cd kiro-frontend && npm start"
  echo ""
  exit 1
else
  echo -e "  ${GREEN}✅ Môi trường chạy test đã sẵn sàng.${NC}"
fi

# ============================================================================
# BƯỚC 1: Chạy API Automation Tests (Maven)
# ============================================================================

print_section "Bước 1: Thực thi API Integration Tests (Maven)"
echo -e "  📍 Thư mục: automation-tests/api-tests"

cd "${SCRIPT_DIR}/automation-tests/api-tests" || exit 1
./mvnw test > "${SCRIPT_DIR}/automation-tests/api-test.log" 2>&1 &
API_PID=$!

spinner $API_PID
wait $API_PID
API_EXIT_CODE=$?

# Parse kết quả API
API_SUMMARY=$(grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+" "${SCRIPT_DIR}/automation-tests/api-test.log" | tail -n 1)

if [ -n "$API_SUMMARY" ]; then
  API_TOTAL=$(echo "$API_SUMMARY" | grep -Eo "Tests run: [0-9]+" | cut -d' ' -f3)
  API_FAIL_ERR=$(echo "$API_SUMMARY" | grep -Eo "Failures: [0-9]+" | cut -d' ' -f2)
  API_ERRORS=$(echo "$API_SUMMARY" | grep -Eo "Errors: [0-9]+" | cut -d' ' -f2)
  API_FAIL=$((API_FAIL_ERR + API_ERRORS))
  API_SKIP=$(echo "$API_SUMMARY" | grep -Eo "Skipped: [0-9]+" | cut -d' ' -f2)
  API_PASS=$((API_TOTAL - API_FAIL - API_SKIP))
else
  API_TOTAL=0
  API_FAIL=0
  API_SKIP=0
  API_PASS=0
fi

if [ "$API_EXIT_CODE" -eq 0 ] && [ "$API_FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}✅ Hoàn thành API Test Suite thành công!${NC}"
  echo -e "     (Đã chạy: $API_TOTAL, Thành công: $API_PASS, Bỏ qua: $API_SKIP)"
else
  echo -e "  ${RED}❌ API Test Suite có lỗi xảy ra!${NC}"
  echo -e "     (Đã chạy: $API_TOTAL, Lỗi: $API_FAIL, Bỏ qua: $API_SKIP)"
fi

# ============================================================================
# BƯỚC 2: Chạy UI Automation Tests (Jest + Selenium)
# ============================================================================

print_section "Bước 2: Thực thi UI Web Tests (Jest + Headless Chrome)"
echo -e "  📍 Thư mục: automation-tests/ui-tests"

cd "${SCRIPT_DIR}/automation-tests/ui-tests" || exit 1
npx jest --runInBand > "${SCRIPT_DIR}/automation-tests/ui-test.log" 2>&1 &
UI_PID=$!

spinner $UI_PID
wait $UI_PID
UI_EXIT_CODE=$?

# Parse kết quả UI
UI_SUMMARY_LINE=$(grep -E "Tests:\s+" "${SCRIPT_DIR}/automation-tests/ui-test.log" | tail -n 1)

if [ -n "$UI_SUMMARY_LINE" ]; then
  if echo "$UI_SUMMARY_LINE" | grep -q "passed"; then
    UI_PASS=$(echo "$UI_SUMMARY_LINE" | grep -Eo "[0-9]+ passed" | cut -d' ' -f1)
  else
    UI_PASS=0
  fi

  if echo "$UI_SUMMARY_LINE" | grep -q "failed"; then
    UI_FAIL=$(echo "$UI_SUMMARY_LINE" | grep -Eo "[0-9]+ failed" | cut -d' ' -f1)
  else
    UI_FAIL=0
  fi

  if echo "$UI_SUMMARY_LINE" | grep -q "skipped"; then
    UI_SKIP=$(echo "$UI_SUMMARY_LINE" | grep -Eo "[0-9]+ skipped" | cut -d' ' -f1)
  else
    UI_SKIP=0
  fi

  if echo "$UI_SUMMARY_LINE" | grep -q "total"; then
    UI_TOTAL=$(echo "$UI_SUMMARY_LINE" | grep -Eo "[0-9]+ total" | cut -d' ' -f1)
  else
    UI_TOTAL=$((UI_PASS + UI_FAIL + UI_SKIP))
  fi
else
  UI_TOTAL=0
  UI_PASS=0
  UI_FAIL=0
  UI_SKIP=0
fi

if [ "$UI_EXIT_CODE" -eq 0 ] && [ "$UI_FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}✅ Hoàn thành UI Test Suite thành công!${NC}"
  echo -e "     (Đã chạy: $UI_TOTAL, Thành công: $UI_PASS, Bỏ qua: $UI_SKIP)"
else
  echo -e "  ${RED}❌ UI Test Suite có lỗi xảy ra!${NC}"
  echo -e "     (Đã chạy: $UI_TOTAL, Lỗi: $UI_FAIL, Bỏ qua: $UI_SKIP)"
fi

# ============================================================================
# KẾT QUẢ TỔNG HỢP (SUMMARY DASHBOARD)
# ============================================================================

print_header "KẾT QUẢ TỔNG HỢP TEST CASES"

TOTAL_PASS=$((API_PASS + UI_PASS))
TOTAL_FAIL=$((API_FAIL + UI_FAIL))
TOTAL_WARN=$((API_SKIP + UI_SKIP))
TOTAL_TESTS=$((API_TOTAL + UI_TOTAL))

echo ""
echo -e "  ${GREEN}✅ PASS: $TOTAL_PASS${NC}"
echo -e "  ${RED}❌ FAIL: $TOTAL_FAIL${NC}"
echo -e "  ${YELLOW}⚠️  WARN: $TOTAL_WARN${NC}"
echo -e "  ${BOLD}📊 TỔNG: $TOTAL_TESTS test cases${NC}"
echo ""

# Direct report links
echo -e "${CYAN}📄 Báo cáo chi tiết:${NC}"
echo -e "  - API Reports:      ${BOLD}automation-tests/api-tests/test-output/${NC} (Extent HTML format)"
echo -e "  - UI Allure Report: ${BOLD}automation-tests/ui-tests/allure-report/index.html${NC} (Click mở trực tiếp trên trình duyệt!)"
echo -e "  - Log chi tiết API: ${BOLD}automation-tests/api-test.log${NC}"
echo -e "  - Log chi tiết UI:  ${BOLD}automation-tests/ui-test.log${NC}"
echo ""

if [ "$TOTAL_FAIL" -eq 0 ] && [ "$TOTAL_TESTS" -gt 0 ]; then
  echo -e "  ${GREEN}${BOLD}🎉 TẤT CẢ CÁC TEST CASES ĐÃ PASS THÀNH CÔNG!${NC}"
  echo ""
  exit 0
else
  echo -e "  ${RED}${BOLD}🔧 CÓ CÁC TEST CASES BỊ THẤT BẠI. VUI LÒNG KIỂM TRA LẠI LOG CHI TIẾT!${NC}"
  echo ""
  exit 1
fi
