const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const BASE_URL = process.env.BASE_URL || "http://127.0.0.1:5173";
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || "admin@test.com";
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "123456";
const SCREENSHOT_DIR = path.resolve(__dirname, "..", "docs", "screenshots");

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

async function run() {
  ensureDir(SCREENSHOT_DIR);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  await page.goto(`${BASE_URL}/backoffice`, { waitUntil: "domcontentloaded", timeout: 90000 });
  await page.waitForSelector('[data-testid="admin-login-page"]', { timeout: 60000 });
  await page.waitForTimeout(1200);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "07-backoffice-login.png"),
    fullPage: true
  });

  await page.fill("#admin-email", ADMIN_EMAIL);
  await page.fill("#admin-password", ADMIN_PASSWORD);
  await page.click('[data-testid="admin-login-button"]');
  await page.waitForSelector('[data-testid="admin-dashboard"]', { timeout: 60000 });
  await page.waitForTimeout(2500);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "08-backoffice-dashboard.png"),
    fullPage: true
  });

  await browser.close();
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
