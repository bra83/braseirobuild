#!/usr/bin/env python3
"""
Clean Braseiro OSE equipment icon PNG assets on disk.
Removes the legacy outer black frame while strictly preserving internal line art,
shading, weapon silhouettes, and alpha transparency.
Generates an audit grid image for visual verification.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from collections import deque

def clean_frame(im):
    im = im.convert('RGBA')
    w, h = im.size
    pixels = list(im.getdata())
    grid = [[pixels[y * w + x] for x in range(w)] for y in range(h)]
    visited = [[False] * w for _ in range(h)]
    queue = deque()

    def is_outer_dark(r, g, b, a):
        return a > 50 and max(r, g, b) < 40

    for x in range(w):
        for y in (0, h - 1):
            if is_outer_dark(*grid[y][x]) and not visited[y][x]:
                visited[y][x] = True
                queue.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if is_outer_dark(*grid[y][x]) and not visited[y][x]:
                visited[y][x] = True
                queue.append((x, y))

    while queue:
        x, y = queue.popleft()
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                if is_outer_dark(*grid[ny][nx]):
                    visited[ny][nx] = True
                    queue.append((nx, ny))

    out_pixels = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = grid[y][x]
            if visited[y][x]:
                out_pixels.append((0, 0, 0, 0))
            else:
                out_pixels.append((r, g, b, a))

    out_im = Image.new('RGBA', (w, h))
    out_im.putdata(out_pixels)
    return out_im

def main():
    repo_root = Path(__file__).resolve().parent.parent
    icons_dir = repo_root / 'ose/1.0.0/source/web-ui-p1/equipment-icons'
    qa_dir = repo_root / 'qa/ose'
    qa_dir.mkdir(parents=True, exist_ok=True)

    icons = sorted(list(icons_dir.glob('*.png')))
    assert len(icons) == 102, f'Expected 102 icons, found {len(icons)}'

    print(f'Starting cleanup of {len(icons)} equipment icons on disk...')
    cleaned_images = []
    
    for p in icons:
        orig = Image.open(p)
        cleaned = clean_frame(orig)
        cleaned.save(p, format='PNG', optimize=True)
        
        # Verify
        w, h = cleaned.size
        # Edge pixels must be transparent
        edge_alphas = []
        for x in range(w):
            edge_alphas.append(cleaned.getpixel((x, 0))[3])
            edge_alphas.append(cleaned.getpixel((x, h - 1))[3])
        for y in range(h):
            edge_alphas.append(cleaned.getpixel((0, y))[3])
            edge_alphas.append(cleaned.getpixel((w - 1, y))[3])
        assert all(a == 0 for a in edge_alphas), f'{p.name} has non-transparent edge pixels!'
        
        # Visible artwork must remain
        total_visible = sum(1 for px in cleaned.getdata() if px[3] > 0)
        assert total_visible > 500, f'{p.name} lost artwork: only {total_visible} visible pixels!'
        
        cleaned_images.append((p.stem, cleaned))

    print(f'All 102 icons cleaned and validated successfully on disk.')

    # Build 11 columns x 10 rows visual contact sheets for light, dark, and checkerboard
    cols = 11
    rows = (len(icons) + cols - 1) // cols
    cell_w, cell_h = 136, 148
    grid_w = cols * cell_w
    grid_h = rows * cell_h

    modes = [
        ('equipment_sheet_light.png', 'LIGHT', (248, 246, 240, 255), (40, 36, 32, 255)),
        ('equipment_sheet_dark.png', 'DARK', (22, 22, 26, 255), (220, 215, 205, 255)),
        ('equipment_sheet_checker.png', 'CHECKER', None, (30, 26, 22, 255)),
        ('equipment_icons_grid.png', 'CHECKER_DARK', None, (200, 200, 210, 255)),
    ]

    for filename, mode_type, bg_color, text_color in modes:
        if mode_type == 'LIGHT' or mode_type == 'DARK':
            sheet_img = Image.new('RGBA', (grid_w, grid_h), bg_color)
            draw = ImageDraw.Draw(sheet_img)
        elif mode_type == 'CHECKER':
            sheet_img = Image.new('RGBA', (grid_w, grid_h), (255, 255, 255, 255))
            draw = ImageDraw.Draw(sheet_img)
            chk_size = 10
            for gy in range(0, grid_h, chk_size):
                for gx in range(0, grid_w, chk_size):
                    if ((gx // chk_size) + (gy // chk_size)) % 2 == 1:
                        draw.rectangle([gx, gy, gx + chk_size - 1, gy + chk_size - 1], fill=(218, 218, 224, 255))
        else: # CHECKER_DARK
            sheet_img = Image.new('RGBA', (grid_w, grid_h), (24, 24, 28, 255))
            draw = ImageDraw.Draw(sheet_img)
            chk_size = 8
            for gy in range(0, grid_h, chk_size):
                for gx in range(0, grid_w, chk_size):
                    if ((gx // chk_size) + (gy // chk_size)) % 2 == 1:
                        draw.rectangle([gx, gy, gx + chk_size - 1, gy + chk_size - 1], fill=(34, 34, 40, 255))

        for idx, (name, img) in enumerate(cleaned_images):
            col = idx % cols
            row = idx // cols
            x = col * cell_w + 4
            y = row * cell_h + 4

            # Paste transparent icon
            sheet_img.paste(img, (x, y), img)
            # Short label
            short_name = name.replace('CLASSIC_EQUIP_', 'C_').replace('ADV_EQUIP_', 'A_')
            draw.text((x + 2, y + 130), short_name, fill=text_color)

        out_path = qa_dir / filename
        sheet_img.save(out_path, format='PNG')
        print(f'Saved visual audit contact sheet: {out_path}')

if __name__ == '__main__':
    main()
