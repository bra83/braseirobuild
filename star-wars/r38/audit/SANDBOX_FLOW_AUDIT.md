# R38 SANDBOX FLOW AUDIT

- Modal present: **True**
- Setup action clicked: **None**
- Modal still visible after action: **True**

## Post-setup navigation
- CENA: click **False**, current=`CENA` — Locator.click: Timeout 2000ms exceeded.
Call log:
  - waiting for locator(".bottom-nav-button:visible").first
    - locator resolved to <button type="button" aria-current="page" class="bottom-nav-button">…</button>
  - attempting click action
    2 × waiting for element to be visible, enabled and stable
      - element is visible, enabled and stable
      - scrolling into view if needed
      - do
- PERSONAGEM: click **False**, current=`CENA` — Locator.click: Timeout 2000ms exceeded.
Call log:
  - waiting for locator(".bottom-nav-button:visible").nth(1)
    - locator resolved to <button type="button" class="bottom-nav-button">…</button>
  - attempting click action
    2 × waiting for element to be visible, enabled and stable
      - element is visible, enabled and stable
      - scrolling into view if needed
      - done scrolling
      
- ITENS: click **False**, current=`CENA` — Locator.click: Timeout 2000ms exceeded.
Call log:
  - waiting for locator(".bottom-nav-button:visible").nth(2)
    - locator resolved to <button type="button" class="bottom-nav-button">…</button>
  - attempting click action
    2 × waiting for element to be visible, enabled and stable
      - element is visible, enabled and stable
      - scrolling into view if needed
      - done scrolling
      
- NAVE: click **False**, current=`CENA` — Locator.click: Timeout 2000ms exceeded.
Call log:
  - waiting for locator(".bottom-nav-button:visible").nth(3)
    - locator resolved to <button type="button" class="bottom-nav-button">…</button>
  - attempting click action
    2 × waiting for element to be visible, enabled and stable
      - element is visible, enabled and stable
      - scrolling into view if needed
      - done scrolling
      
- REGISTROS: click **False**, current=`CENA` — Locator.click: Timeout 2000ms exceeded.
Call log:
  - waiting for locator(".bottom-nav-button:visible").nth(4)
    - locator resolved to <button type="button" class="bottom-nav-button">…</button>
  - attempting click action
    2 × waiting for element to be visible, enabled and stable
      - element is visible, enabled and stable
      - scrolling into view if needed
      - done scrolling
      

## Modal controls
- `{'i': 0, 'tag': 'SELECT', 'type': 'select-one', 'name': '', 'id': 'sandbox-mode', 'value': 'EOTE', 'placeholder': None, 'disabled': False, 'text': 'Edge of the Empire\nAge of Rebellion\nMista · EotE + AoR', 'aria': None}`
- `{'i': 1, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'LISTA', 'aria': None}`
- `{'i': 2, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'MAPA', 'aria': None}`
- `{'i': 3, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'ALEATÓRIO', 'aria': None}`
- `{'i': 4, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Mos Eisley · Tatooine\nPorto do deserto, contrabando e oportunidades no Borda Exterior.', 'aria': None}`
- `{'i': 5, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Nar Shaddaa\nLua dos contrabandistas, cartéis, caçadores e mercados clandestinos.', 'aria': None}`
- `{'i': 6, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Ord Mantell\nSucata, mercenários, rotas de guerra e negócios cinzentos.', 'aria': None}`
- `{'i': 7, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Corellia\nEstaleiros, pilotos, sindicatos e tráfego interestelar intenso.', 'aria': None}`
- `{'i': 8, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Ryloth\nRotas de especiarias, ocupação imperial e redes locais.', 'aria': None}`
- `{'i': 9, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Eriadu\nMundo industrial estratégico entre o Núcleo e a Borda Exterior.', 'aria': None}`
- `{'i': 10, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Socorro\nRefúgio periférico de pilotos, oficinas e contrabandistas.', 'aria': None}`
- `{'i': 11, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'Bespin\nCidade das Nuvens, tibanna, comércio e interesses imperiais.', 'aria': None}`
- `{'i': 12, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '1', 'aria': 'Escolher Mos Eisley · Tatooine'}`
- `{'i': 13, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '2', 'aria': 'Escolher Nar Shaddaa'}`
- `{'i': 14, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '3', 'aria': 'Escolher Ord Mantell'}`
- `{'i': 15, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '4', 'aria': 'Escolher Corellia'}`
- `{'i': 16, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '5', 'aria': 'Escolher Ryloth'}`
- `{'i': 17, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '6', 'aria': 'Escolher Eriadu'}`
- `{'i': 18, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '7', 'aria': 'Escolher Socorro'}`
- `{'i': 19, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': '', 'value': '', 'placeholder': None, 'disabled': False, 'text': '8', 'aria': 'Escolher Bespin'}`
- `{'i': 20, 'tag': 'BUTTON', 'type': 'button', 'name': '', 'id': 'sandbox-start-button', 'value': '', 'placeholder': None, 'disabled': False, 'text': 'CRIAR MUNDO VIVO', 'aria': None}`
