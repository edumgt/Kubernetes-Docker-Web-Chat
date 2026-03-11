const { chromium } = require('playwright');
const crypto = require('node:crypto');
const fs = require('node:fs/promises');

const baseUrl = process.env.BASE_URL || 'http://host.docker.internal:9999';
const outputDir = process.env.OUTPUT_DIR || 'docs/screenshots';

function base64Url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function signJwt() {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: 'test1',
    name: 'test1',
    provider: 'local',
    iat: now,
    exp: now + 3600,
  };

  const headerPart = base64Url(JSON.stringify(header));
  const payloadPart = base64Url(JSON.stringify(payload));
  const message = `${headerPart}.${payloadPart}`;
  const signature = crypto
    .createHmac('sha256', 'yourSuperSecretKeyYourSuperSecretKey')
    .update(message)
    .digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  return `${message}.${signature}`;
}

async function waitForUp(page, url, attempts = 30) {
  for (let i = 0; i < attempts; i += 1) {
    try {
      const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 5000 });
      if (response && response.ok()) {
        return;
      }
    } catch (e) {
      // retry
    }
    await page.waitForTimeout(2000);
  }
  throw new Error(`Service not reachable: ${url}`);
}

(async () => {
  await fs.mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1536, height: 960 } });
  const page = await context.newPage();

  const loginUrl = `${baseUrl}/auth/login`;
  await waitForUp(page, loginUrl);
  await page.waitForTimeout(1200);
  await page.screenshot({
    path: `${outputDir}/01-app-running-login-page.png`,
    fullPage: true,
  });

  const jwtToken = signJwt();
  await context.addCookies([
    {
      name: 'JWT_TOKEN',
      value: jwtToken,
      domain: 'host.docker.internal',
      path: '/',
      httpOnly: false,
      secure: false,
    },
  ]);

  await page.goto(`${baseUrl}/chat`, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await page.waitForSelector('#searchQuery', { timeout: 15000 });
  await page.fill('#searchQuery', 'test');
  await page.waitForTimeout(1300);
  await page.screenshot({
    path: `${outputDir}/02-after-login-home-page.png`,
    fullPage: true,
  });

  await page.goto(`${baseUrl}/chat/public`, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await page.waitForSelector('#messageInput', { timeout: 15000 });
  await page.waitForTimeout(1600);
  await page.screenshot({
    path: `${outputDir}/03-after-login-chat-page.png`,
    fullPage: true,
  });

  await browser.close();
  console.log('Screenshots created in', outputDir);
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
