const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const DASHBOARD_URL = process.env.K8S_DASHBOARD_URL || "https://127.0.0.1:8443/";
const TOKEN = process.env.DASHBOARD_TOKEN || "";
const SCREENSHOT_DIR = path.resolve(__dirname, "..", "docs", "screenshots");

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

async function safeGoto(page, url) {
  try {
    await page.goto(url, { waitUntil: "networkidle", timeout: 90000 });
  } catch {
    await page.goto(url, { waitUntil: "domcontentloaded", timeout: 90000 });
  }
}

async function clickIfVisible(locator) {
  try {
    if (await locator.isVisible()) {
      await locator.click();
      return true;
    }
  } catch {
    return false;
  }
  return false;
}

async function fillToken(page, token) {
  try {
    const labeledInput = page.getByLabel(/enter token/i).first();
    if (await labeledInput.isVisible()) {
      await labeledInput.fill(token);
      return;
    }
  } catch {
    // fall through
  }

  try {
    const input = page.locator("input").first();
    if (await input.isVisible()) {
      await input.fill(token);
      return;
    }
  } catch {
    // fall through
  }

  const textarea = page.locator("textarea").first();
  await textarea.waitFor({ timeout: 30000 });
  await textarea.fill(token);
}

async function run() {
  if (!TOKEN) {
    throw new Error("DASHBOARD_TOKEN is required");
  }

  ensureDir(SCREENSHOT_DIR);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  await safeGoto(page, DASHBOARD_URL);
  await page.waitForTimeout(2000);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "04-k8s-dashboard-login.png"),
    fullPage: true
  });

  await clickIfVisible(page.getByText(/token/i).first());

  await fillToken(page, TOKEN);

  const signedIn =
    (await clickIfVisible(page.getByRole("button", { name: /sign in|signin|로그인/i }).first())) ||
    (await clickIfVisible(page.locator("button").filter({ hasText: /sign/i }).first()));

  if (!signedIn) {
    throw new Error("Could not find dashboard sign-in button");
  }

  await page.waitForTimeout(5000);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "04-k8s-dashboard-overview.png"),
    fullPage: true
  });

  await safeGoto(page, "https://127.0.0.1:8443/#/node?namespace=_all");
  await page.waitForTimeout(4000);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "05-k8s-dashboard-nodes.png"),
    fullPage: true
  });

  await safeGoto(page, "https://127.0.0.1:8443/#/pod?namespace=chat-app");
  await page.waitForTimeout(4000);
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, "06-k8s-dashboard-pods-chat-app.png"),
    fullPage: true
  });

  await browser.close();
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
