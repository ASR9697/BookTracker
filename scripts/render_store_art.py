"""Renders Play Store artwork (512x512 icon, 1024x500 feature graphic) from the
same geometry as res/drawable/ic_launcher_foreground.xml. Usage:
    python render_store_art.py <out_dir> "<App Name>" "<tagline>"
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

SS = 4  # supersampling factor


def cubic(p0, p1, p2, p3, steps=48):
    pts = []
    for i in range(1, steps + 1):
        t = i / steps
        u = 1 - t
        x = u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0]
        y = u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def left_page():
    pts = [(54, 40)]
    pts += cubic((54, 40), (49.5, 35.8), (42.6, 33.5), (36, 33.5))
    pts += [(33, 33.5), (33, 71.5), (36, 71.5)]
    pts += cubic((36, 71.5), (42.6, 71.5), (49.5, 73.8), (54, 78))
    return pts


def right_page():
    return [(108 - x, y) for x, y in left_page()]


def gradient(size, top_left, bottom_right):
    w, h = size
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / w + y / h) / 2
            px[x, y] = tuple(int(a + (b - a) * t) for a, b in zip(top_left, bottom_right))
    return img


def draw_book(canvas, origin, scale):
    """Draws the book (viewport units) onto an RGBA canvas at origin/scale (px per unit)."""
    ox, oy = origin
    tr = lambda pts: [(ox + x * scale, oy + y * scale) for x, y in pts]
    d = ImageDraw.Draw(canvas)
    d.polygon(tr(left_page()), fill=(255, 244, 222, 255))
    d.polygon(tr(right_page()), fill=(242, 221, 181, 255))
    for y, w in [(43.2, 11.5), (49.2, 12.5), (55.2, 12), (61.2, 9)]:
        d.rectangle(tr([(38, y), (38 + w, y + 1.8)]), fill=(216, 196, 156, 255))
    d.rectangle(tr([(53.3, 40.6), (54.7, 77)]), fill=(27, 37, 51, 255))
    d.polygon(tr([(62, 33), (68.5, 33), (68.5, 56), (65.25, 52.2), (62, 56)]), fill=(245, 158, 11, 255))


def book_with_shadow(size_px, view_min, view_span):
    big = size_px * SS
    layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    scale = big / view_span
    draw_book(layer, (-view_min * scale, -view_min * scale), scale)
    alpha = layer.split()[3]
    shadow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    shadow.putalpha(alpha.point(lambda a: int(a * 0.45)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(big * 0.02))
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.alpha_composite(shadow, (0, int(big * 0.018)))
    out.alpha_composite(layer)
    return out.resize((size_px, size_px), Image.LANCZOS)


def render_icon(path):
    size = 512
    bg = gradient((size, size), (43, 58, 82), (18, 26, 37)).convert("RGBA")
    bg.alpha_composite(book_with_shadow(size, 18, 72))
    bg.convert("RGB").save(path, optimize=True)


def font(names, size):
    for name in names:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def render_feature(path, title, tagline):
    w, h = 1024, 500
    img = gradient((w, h), (43, 58, 82), (14, 20, 29)).convert("RGBA")
    # Soft amber glow behind the book
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((60, 40, 480, 460), fill=(245, 158, 11, 70))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(70)))
    img.alpha_composite(book_with_shadow(400, 24, 60), (70, 50))

    d = ImageDraw.Draw(img)
    left, max_width = 500, w - 500 - 48

    def fitted(names, size, text, min_size):
        # Largest size (down to min_size) at which the text fits the right column.
        while size > min_size:
            f = font(names, size)
            if d.textlength(text, font=f) <= max_width:
                return f
            size -= 2
        return font(names, min_size)

    title_font = fitted(["C:/Windows/Fonts/segoeuib.ttf", "DejaVuSans-Bold.ttf"], 84, title, 40)
    tag_font = fitted(["C:/Windows/Fonts/segoeui.ttf", "DejaVuSans.ttf"], 34, tagline, 20)
    title_box = d.textbbox((0, 0), title, font=title_font)
    tag_box = d.textbbox((0, 0), tagline, font=tag_font)
    title_h = title_box[3] - title_box[1]
    tag_h = tag_box[3] - tag_box[1]
    block_h = title_h + 28 + 6 + 24 + tag_h
    top = (h - block_h) // 2
    d.text((left, top - title_box[1]), title, font=title_font, fill=(255, 244, 222))
    bar_y = top + title_h + 24
    d.rectangle((left + 2, bar_y, left + 112, bar_y + 6), fill=(245, 158, 11))
    d.text((left + 2, bar_y + 6 + 22 - tag_box[1]), tagline, font=tag_font, fill=(242, 221, 181))
    img.convert("RGB").save(path, optimize=True)


if __name__ == "__main__":
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    name = sys.argv[2] if len(sys.argv) > 2 else "Book Tracker"
    tagline = sys.argv[3] if len(sys.argv) > 3 else "Your reading, beautifully tracked"
    render_icon(out / "play-icon-512.png")
    render_feature(out / "feature-graphic-1024x500.png", name, tagline)
    print("wrote", out)
