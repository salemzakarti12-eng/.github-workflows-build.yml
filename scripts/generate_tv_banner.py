import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

WIDTH, HEIGHT = 1920, 1080  # Crisp 16:9 1080p/4K launcher banner

img = Image.new("RGBA", (WIDTH, HEIGHT), (4, 11, 26, 255))
draw = ImageDraw.Draw(img)

# 1. Smooth Deep Navy Gradient Background
for y in range(HEIGHT):
    t = y / HEIGHT
    r = int(3 + t * 10)
    g = int(10 + t * 24)
    b = int(24 + t * 56)
    draw.line([(0, y), (WIDTH, y)], fill=(r, g, b, 255))

# 2. Render at 2x Super-Sampling (3840x2160) for Razor-Sharp Anti-Aliasing
SS = 2
W2, H2 = WIDTH * SS, HEIGHT * SS
overlay = Image.new("RGBA", (W2, H2), (0, 0, 0, 0))
od = ImageDraw.Draw(overlay)

# Subtle cyan radial glow behind emblem
glow = Image.new("RGBA", (W2, H2), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
gd.ellipse([220, 480, 1340, 1600], fill=(0, 229, 255, 45))
glow = glow.filter(ImageFilter.GaussianBlur(radius=180))
overlay = Image.alpha_composite(overlay, glow)
od = ImageDraw.Draw(overlay)

# TV Monitor Outer Rounded Card
tv_box = [380, 710, 1180, 1310]
od.rounded_rectangle(tv_box, radius=75, fill=(3, 10, 24, 255), outline=(0, 229, 255, 255), width=36)

# TV Stand Neck & Base
od.rectangle([735, 1310, 825, 1425], fill=(41, 121, 255, 255))
od.rounded_rectangle([580, 1425, 980, 1480], radius=26, fill=(0, 229, 255, 255))

# Bold Play Triangle inside TV
play_pts = [(695, 865), (695, 1155), (945, 1010)]
od.polygon(play_pts, fill=(255, 255, 255, 255))

# Bold Typography: "HYPNO" (White) + "STREAM" (Cyan)
font_paths = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
]
font_file = next((p for p in font_paths if os.path.exists(p)), None)

title_font = ImageFont.truetype(font_file, 330) if font_file else ImageFont.load_default()
sub_font = ImageFont.truetype(font_file, 210) if font_file else ImageFont.load_default()

od.text((1380, 720), "HYPNO", font=title_font, fill=(255, 255, 255, 255))
od.text((1385, 1090), "STREAM", font=sub_font, fill=(0, 229, 255, 255))

# Bottom Cyan Accent Bar
od.rounded_rectangle([1390, 1365, 2660, 1398], radius=16, fill=(0, 229, 255, 255))

# Downsample 3840x2160 -> 1920x1080 with Lanczos (Studio-Grade Smooth Edges)
crisp_overlay = overlay.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
final_img = Image.alpha_composite(img, crisp_overlay).convert("RGB")

# Clean up any old XML banner so Android strictly uses the 1920x1080 PNG
for old in [
    "app/src/main/res/drawable/ic_banner.xml",
    "app/src/main/res/drawable-nodpi/ic_banner.xml",
]:
    if os.path.exists(old):
        os.remove(old)

os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)
final_img.save("app/src/main/res/drawable-nodpi/ic_banner.png", "PNG", optimize=True)
print("Generated ultra-sharp 1920x1080 ic_banner.png successfully!")
