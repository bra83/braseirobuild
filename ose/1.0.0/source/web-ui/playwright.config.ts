import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  workers: 1,
  use: {
    baseURL: 'http://127.0.0.1:4173',
    viewport: { width: 415, height: 915 },
    locale: 'pt-BR',
    timezoneId: 'UTC',
    colorScheme: 'light',
    reducedMotion: 'reduce',
    deviceScaleFactor: 1,
    launchOptions: {
      args: [
        '--no-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
        '--disable-software-rasterizer',
        '--disable-lcd-text',
        '--font-render-hinting=none',
        '--force-device-scale-factor=1',
        '--run-all-compositor-stages-before-draw',
        '--disable-threaded-animation',
        '--disable-threaded-scrolling'
      ]
    }
  },
  webServer: [
    {
      command: 'python3 -m http.server 4173 --directory dist',
      url: 'http://127.0.0.1:4173',
      reuseExistingServer: false
    },
    {
      command: 'python3 -m http.server 4174 --directory ..',
      url: 'http://127.0.0.1:4174/web-ui-p1/index.html',
      reuseExistingServer: false
    }
  ]
});
