"""Capability-based device profiles for Barbara hosts.

This module is presentation/runtime policy only. It must never change rules, dice
results, canonical state, Atlas coordinates, saves, or package authority.
"""
from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
import json
import math
from typing import Any, Mapping

SCHEMA = "barbara-device-profile-v1"
GOLDEN_VISUAL = {"width_css_px": 415, "height_css_px": 915}


def _finite_number(value: Any, default: float = 0.0) -> float:
    try:
        out = float(value)
    except (TypeError, ValueError):
        return default
    return out if math.isfinite(out) else default


def _int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _bool(value: Any, default: bool = False) -> bool:
    return value if isinstance(value, bool) else default


def _text(value: Any) -> str:
    return str(value or "").strip()


def _safe_insets(raw: Any) -> dict[str, int]:
    src = raw if isinstance(raw, Mapping) else {}
    return {
        "left": max(0, _int(src.get("left"))),
        "top": max(0, _int(src.get("top"))),
        "right": max(0, _int(src.get("right"))),
        "bottom": max(0, _int(src.get("bottom"))),
    }


@dataclass(frozen=True)
class DeviceMetrics:
    manufacturer: str = ""
    model: str = ""
    sdk_int: int = 0
    width_px: int = 0
    height_px: int = 0
    density: float = 1.0
    density_dpi: int = 0
    refresh_hz: float = 60.0
    memory_mb: int = 0
    low_ram: bool = False
    viewport_css_width: int = 0
    viewport_css_height: int = 0
    safe_insets: dict[str, int] | None = None
    hardware_concurrency: int = 0
    device_memory_gb: float = 0.0
    webgl_max_texture_size: int = 0
    gpu_renderer: str = ""
    user_agent: str = ""

    @classmethod
    def from_mapping(cls, raw: Mapping[str, Any] | None) -> "DeviceMetrics":
        src = raw if isinstance(raw, Mapping) else {}
        density = _finite_number(src.get("density"), 1.0)
        if density <= 0:
            density = 1.0
        refresh = _finite_number(src.get("refresh_hz", src.get("refreshHz", 60.0)), 60.0)
        if refresh <= 0:
            refresh = 60.0
        return cls(
            manufacturer=_text(src.get("manufacturer")),
            model=_text(src.get("model")),
            sdk_int=max(0, _int(src.get("sdk_int", src.get("sdkInt")))),
            width_px=max(0, _int(src.get("width_px", src.get("widthPx")))),
            height_px=max(0, _int(src.get("height_px", src.get("heightPx")))),
            density=density,
            density_dpi=max(0, _int(src.get("density_dpi", src.get("densityDpi")))),
            refresh_hz=refresh,
            memory_mb=max(0, _int(src.get("memory_mb", src.get("memoryMb")))),
            low_ram=_bool(src.get("low_ram", src.get("lowRam"))),
            viewport_css_width=max(0, _int(src.get("viewport_css_width", src.get("viewportCssWidth")))),
            viewport_css_height=max(0, _int(src.get("viewport_css_height", src.get("viewportCssHeight")))),
            safe_insets=_safe_insets(src.get("safe_insets", src.get("safeInsets"))),
            hardware_concurrency=max(0, _int(src.get("hardware_concurrency", src.get("hardwareConcurrency")))),
            device_memory_gb=max(0.0, _finite_number(src.get("device_memory_gb", src.get("deviceMemoryGb")), 0.0)),
            webgl_max_texture_size=max(0, _int(src.get("webgl_max_texture_size", src.get("webglMaxTextureSize")))),
            gpu_renderer=_text(src.get("gpu_renderer", src.get("gpuRenderer"))),
            user_agent=_text(src.get("user_agent", src.get("userAgent"))),
        )

    def identity_blob(self) -> str:
        return " ".join((self.manufacturer, self.model, self.gpu_renderer, self.user_agent)).upper()


_RENDER_TIERS: dict[str, dict[str, Any]] = {
    "safe": {
        "quality_tier": "safe",
        "render_scale": 0.72,
        "max_dpr": 1.5,
        "shadow_quality": "low",
        "particle_scale": 0.35,
        "soft_texture_cache_mb": 128,
        "soft_tile_cache_mb": 96,
        "dice_visual_limit": 6,
        "target_fps": 45,
    },
    "balanced": {
        "quality_tier": "balanced",
        "render_scale": 0.86,
        "max_dpr": 1.75,
        "shadow_quality": "medium",
        "particle_scale": 0.65,
        "soft_texture_cache_mb": 256,
        "soft_tile_cache_mb": 160,
        "dice_visual_limit": 10,
        "target_fps": 60,
    },
    "high": {
        "quality_tier": "high",
        "render_scale": 0.95,
        "max_dpr": 2.0,
        "shadow_quality": "high",
        "particle_scale": 0.85,
        "soft_texture_cache_mb": 384,
        "soft_tile_cache_mb": 224,
        "dice_visual_limit": 12,
        "target_fps": 60,
    },
    "ultra": {
        "quality_tier": "ultra",
        "render_scale": 1.0,
        "max_dpr": 2.0,
        "shadow_quality": "high",
        "particle_scale": 1.0,
        "soft_texture_cache_mb": 512,
        "soft_tile_cache_mb": 256,
        "dice_visual_limit": 16,
        "target_fps": 60,
    },
}


def _known_device(metrics: DeviceMetrics) -> tuple[str | None, str | None]:
    identity = metrics.identity_blob()
    # Samsung retail model families. Region/storage suffixes may vary, so match family stem.
    if "SM-S918" in identity or "S23 ULTRA" in identity:
        return "samsung_s23_ultra", "ultra"
    if "SM-A576" in identity or "GALAXY A57" in identity:
        return "samsung_a57", "balanced"
    return None, None


def _fallback_tier(metrics: DeviceMetrics) -> str:
    if metrics.low_ram:
        return "safe"
    memory_mb = metrics.memory_mb or int(metrics.device_memory_gb * 1024)
    if memory_mb and memory_mb < 4096:
        return "safe"
    # Capability first: large texture support + adequate RAM/CPU allows high tier even
    # when the exact device model is unknown.
    strong_texture = metrics.webgl_max_texture_size >= 8192
    strong_memory = memory_mb >= 8192 if memory_mb else False
    strong_cpu = metrics.hardware_concurrency >= 8
    if strong_texture and strong_memory and strong_cpu:
        return "high"
    return "balanced"


def resolve_device_profile(raw_metrics: Mapping[str, Any] | None = None) -> dict[str, Any]:
    metrics = DeviceMetrics.from_mapping(raw_metrics)
    known_id, known_tier = _known_device(metrics)
    tier = known_tier or _fallback_tier(metrics)
    render = deepcopy(_RENDER_TIERS[tier])

    # 120 Hz panels are retained, but Barbara intentionally targets 60 fps by default to
    # protect thermals/battery during long RPG sessions. A host may render UI chrome at
    # panel rate without changing VTT/dice simulation semantics.
    panel_hz = round(metrics.refresh_hz, 2)
    viewport_css = {
        "width": metrics.viewport_css_width,
        "height": metrics.viewport_css_height,
    }
    physical = {"width": metrics.width_px, "height": metrics.height_px}

    return {
        "schema": SCHEMA,
        "profile_id": known_id or f"generic_{tier}",
        "known_device": known_id is not None,
        "identity": {
            "manufacturer": metrics.manufacturer,
            "model": metrics.model,
            "sdk_int": metrics.sdk_int,
        },
        "display": {
            "physical_px": physical,
            "viewport_css_px": viewport_css,
            "density": round(metrics.density, 4),
            "density_dpi": metrics.density_dpi,
            "panel_refresh_hz": panel_hz,
            "safe_insets_px": metrics.safe_insets or _safe_insets(None),
        },
        "golden_visual": deepcopy(GOLDEN_VISUAL),
        "render": render,
        "input": {
            "minimum_touch_target_dp": 48,
            "gesture_safe_insets": True,
        },
        "assets": {
            "preserve_master_assets": True,
            "runtime_downsample_only": True,
            "mipmaps_preferred": True,
            "tile_streaming_preferred": True,
        },
        "invariants": {
            "mechanics": True,
            "dice_result": True,
            "canonical_state": True,
            "atlas_coordinates": True,
            "save_format": True,
            "system_adapter_authority": True,
        },
    }


def adapt_runtime_quality(profile: Mapping[str, Any], *, average_fps: float | None = None, thermal_status: str | None = None) -> dict[str, Any]:
    """Return a presentation-only runtime adaptation.

    This is deliberately monotonic downward: it may reduce rendering cost but never
    promotes above the configured device tier and never changes canonical simulation.
    """
    out = deepcopy(dict(profile))
    render = out.setdefault("render", {})
    base_scale = _finite_number(render.get("render_scale"), 1.0)
    fps = _finite_number(average_fps, 0.0) if average_fps is not None else 0.0
    thermal = _text(thermal_status).lower()
    constrained = thermal in {"severe", "critical", "emergency", "shutdown"}
    slow = fps > 0 and fps < 48
    if constrained or slow:
        render["render_scale"] = round(max(0.65, base_scale - (0.12 if constrained else 0.08)), 2)
        render["particle_scale"] = round(max(0.25, _finite_number(render.get("particle_scale"), 1.0) * 0.7), 2)
        if render.get("shadow_quality") == "high":
            render["shadow_quality"] = "medium"
        elif constrained and render.get("shadow_quality") == "medium":
            render["shadow_quality"] = "low"
        out["runtime_adaptation"] = {"active": True, "reason": "thermal" if constrained else "frame_budget"}
    else:
        out["runtime_adaptation"] = {"active": False}
    return out


def resolve_device_profile_json(metrics_json: str | Mapping[str, Any] | None = None) -> str:
    if isinstance(metrics_json, str):
        try:
            raw = json.loads(metrics_json)
        except json.JSONDecodeError as exc:
            raise ValueError("invalid_device_metrics_json") from exc
    else:
        raw = metrics_json
    if raw is not None and not isinstance(raw, Mapping):
        raise ValueError("invalid_device_metrics")
    return json.dumps(resolve_device_profile(raw), ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)
