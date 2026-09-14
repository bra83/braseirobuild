# BLOCK02 BROAD AUDIT V3

- Critical: **0**
- High: **5**
- Medium: **0**
- JS checked: **4**
- Static refs: **13**, missing **0**
- PNG refs: **6**
- Runtime shell loaded: **True**
- Broken rendered images: **0**
- Navigation controls exercised: **5**

## Findings
- **HIGH · NAV_CLICK** — Visible nav click failed: CENA — `Locator.click: Timeout 1500ms exceeded.
Call log:
  - waiting for locator("nav button:visible,.bottom-nav button:visible,[data-tab]:visible").first
    - locator resolved to <button type="button" aria`
- **HIGH · NAV_CLICK** — Visible nav click failed: PERSONAGEM — `Locator.click: Timeout 1500ms exceeded.
Call log:
  - waiting for locator("nav button:visible,.bottom-nav button:visible,[data-tab]:visible").nth(1)
    - locator resolved to <button type="button" cla`
- **HIGH · NAV_CLICK** — Visible nav click failed: ITENS — `Locator.click: Timeout 1500ms exceeded.
Call log:
  - waiting for locator("nav button:visible,.bottom-nav button:visible,[data-tab]:visible").nth(2)
    - locator resolved to <button type="button" cla`
- **HIGH · NAV_CLICK** — Visible nav click failed: NAVE — `Locator.click: Timeout 1500ms exceeded.
Call log:
  - waiting for locator("nav button:visible,.bottom-nav button:visible,[data-tab]:visible").nth(3)
    - locator resolved to <button type="button" cla`
- **HIGH · NAV_CLICK** — Visible nav click failed: REGISTROS — `Locator.click: Timeout 1500ms exceeded.
Call log:
  - waiting for locator("nav button:visible,.bottom-nav button:visible,[data-tab]:visible").nth(4)
    - locator resolved to <button type="button" cla`
