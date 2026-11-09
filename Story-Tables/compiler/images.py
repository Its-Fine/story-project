from PIL import Image, ImageOps
from io import BytesIO
from math import sqrt


def load_compressed_image(image_source: str, max_pixels: int = 921_600, min_size: int = 360, quality: int = 75, dpi: int = 96) -> BytesIO:
    with Image.open(image_source) as img:
        img = ImageOps.exif_transpose(img)

        if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
            base = Image.new("RGB", img.size, (255, 255, 255))
            alpha = img.getchannel("A") if "A" in img.getbands() else None
            base.paste(img, mask=alpha)
            img = base
        elif img.mode != "RGB":
            img = img.convert("RGB")

        width, height = img.size
        pixels = width * height

        if pixels > max_pixels:
            scale = sqrt(max_pixels / pixels)
            width = max(min_size, round(width * scale))
            height = max(min_size, round(height * scale))
            img = img.resize((width, height), Image.Resampling.LANCZOS)

        output = BytesIO()
        img.save(output, format="JPEG", quality=quality, optimize=True, progressive=True, dpi=(dpi, dpi))
        output.seek(0)
        return output
