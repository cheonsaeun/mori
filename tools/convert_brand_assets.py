#!/usr/bin/env python3
"""Convert the supplied path/gradient SVGs to Android vectors, without rasterization.
Original files under assets/brand are never modified. No third-party dependency.
"""
from pathlib import Path
import xml.etree.ElementTree as E

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/brand"
RES = ROOT / "android/app/src/main/res"
ANDROID = "http://schemas.android.com/apk/res/android"
AAPT = "http://schemas.android.com/aapt"
E.register_namespace("android", ANDROID)
E.register_namespace("aapt", AAPT)
def attr(key): return "{" + ANDROID + "}" + key
def number(value): return format(float(value), ".8f").rstrip("0").rstrip(".") or "0"
def color(value):
    return {"white": "#FFFFFFFF", "black": "#FF000000", "none": "#00000000"}.get(value, value)
def write(node, name, directory="drawable"):
    output = RES / directory / (name + ".xml")
    output.parent.mkdir(parents=True, exist_ok=True)
    E.indent(node, space="    ")
    E.ElementTree(node).write(output, encoding="utf-8", xml_declaration=True)

def vector(source, name, *, only=None, padded=False, monochrome=False, crop_symbol=False):
    svg = E.parse(SOURCE / source).getroot()
    bounds = list(map(float, svg.attrib["viewBox"].split()))
    assert bounds[:2] == [0, 0]
    width, height = bounds[2:]
    if padded: width = height = 500
    if crop_symbol: height = 170
    root = E.Element("vector", {attr("width"): "108dp" if padded else f"{number(width)}dp", attr("height"): "108dp" if padded else f"{number(height)}dp", attr("viewportWidth"): number(width), attr("viewportHeight"): number(height)})
    parent = root
    if padded: parent = E.SubElement(root, "group", {attr("translateX"): "50", attr("translateY"): "50"})
    if crop_symbol: parent = E.SubElement(root, "group", {attr("translateY"): "-90"})
    gradients = {node.attrib["id"]: node for node in svg.iter() if node.tag.split("}")[-1] == "linearGradient"}
    for element in svg:
        kind = element.tag.split("}")[-1]
        if kind == "defs": continue
        assert kind in {"path", "rect"}, f"Unsupported SVG element: {kind}"
        if only and kind != only: continue
        allowed = {"d", "fill", "fill-rule", "clip-rule"} if kind == "path" else {"x", "y", "width", "height", "fill"}
        assert not set(element.attrib) - allowed, f"Unsupported attributes: {element.attrib.keys()}"
        if kind == "path": data = element.attrib["d"]
        else:
            x, y = float(element.get("x", 0)), float(element.get("y", 0))
            w, h = float(element.attrib["width"]), float(element.attrib["height"])
            data = f"M{x},{y}H{x+w}V{y+h}H{x}Z"
        path = E.SubElement(parent, "path", {attr("pathData"): data})
        if element.get("fill-rule") == "evenodd": path.set(attr("fillType"), "evenOdd")
        fill = element.get("fill", "none")
        if monochrome: path.set(attr("fillColor"), "#FFFFFFFF")
        elif fill.startswith("url(#"):
            gradient = gradients[fill[5:-1]]
            assert gradient.get("gradientUnits") == "userSpaceOnUse"
            paint = E.SubElement(path, "{" + AAPT + "}attr", {"name": "android:fillColor"})
            g = E.SubElement(paint, "gradient", {attr("type"): "linear", attr("startX"): number(gradient.get("x1", 0)), attr("startY"): number(gradient.get("y1", 0)), attr("endX"): number(gradient.get("x2", 0)), attr("endY"): number(gradient.get("y2", 0)), attr("tileMode"): "clamp"})
            for stop in gradient:
                offset = stop.get("offset", "0")
                if offset.endswith("%"): offset = str(float(offset[:-1])/100)
                E.SubElement(g, "item", {attr("offset"): number(offset), attr("color"): color(stop.attrib["stop-color"])})
        else: path.set(attr("fillColor"), color(fill))
    write(root, name)

vector("MORI_symbol.svg", "mori_symbol", crop_symbol=True)
vector("MORI_logo1.svg", "mori_logo_primary")
vector("MORI_logo2.svg", "mori_logo_inverse")
vector("MORI_logo3.svg", "mori_logo_monochrome")
vector("MORI_logo_app.svg", "ic_mori")
vector("MORI_logo_app.svg", "mori_icon_background", only="rect")
vector("MORI_logo_app.svg", "mori_icon_foreground", only="path", padded=True)
vector("MORI_logo_app.svg", "mori_icon_monochrome", only="path", padded=True, monochrome=True)
for directory, themed in [("mipmap-anydpi-v26", False), ("mipmap-anydpi-v33", True)]:
    icon = E.Element("adaptive-icon")
    for layer in ["background", "foreground"] + (["monochrome"] if themed else []):
        E.SubElement(icon, layer, {attr("drawable"): "@drawable/mori_icon_" + layer})
    write(icon, "ic_launcher", directory)
print("Converted 5 SVG originals into Android vector/adaptive icon resources")
