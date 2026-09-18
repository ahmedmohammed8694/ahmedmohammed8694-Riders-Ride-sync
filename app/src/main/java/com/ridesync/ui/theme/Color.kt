package com.ridesync.ui.theme

import androidx.compose.ui.graphics.Color

// High-Contrast Cockpit HUD Color System - Option 11: Solar Amber Rally Light Mode
object HudColors {
    // Polar Pearl Light Canvas & Surface Tones
    val ObsidianCanvas = Color(0xFFF8FAFC)
    val ObsidianSurface = Color(0xFFFFFFFF)
    val ObsidianElevated = Color(0xFFF1F5F9)
    val ObsidianBorder = Color(0xFFCBD5E1)
    val ObsidianModal = Color(0xFFFFFFFF)

    // Primary & Lead Accents (Solar Rally Orange & Cobalt Highway Blue)
    val CyanPrimary = Color(0xFFEA580C)      // Solar Rally Orange
    val CyanLight = Color(0xFFF97316)        // Luminous Orange Accent
    val CyanGlow = Color(0x33EA580C)         // Soft Solar Glow
    val CobaltBlue = Color(0xFF2563EB)       // Cobalt Highway Route Ribbon

    // Status Palette & Halos (Daylight Optimized)
    val StatusRiding = Color(0xFF16A34A)     // Daylight Racing Green
    val StatusRidingGlow = Color(0xFF22C55E)

    val StatusStopped = Color(0xFFCA8A04)    // Solar Gold
    val StatusStoppedGlow = Color(0xFFEAB308)

    val StatusDelayed = Color(0xFFE11D48)    // Sun-baked Trail Red
    val StatusDelayedGlow = Color(0xFFF43F5E)

    val StatusSos = Color(0xFFB91C1C)        // Warning Crimson Strobe
    val StatusSosGlow = Color(0xFFEF4444)

    // Text Hierarchy (WCAG AAA Deep Slate Contrast on Light Surface)
    val TextCrispWhite = Color(0xFF0F172A)   // Deep Slate Black for titles
    val TextCoolSilver = Color(0xFF475569)   // Cool Slate for subtitles & labels
    val TextMuted = Color(0xFF64748B)        // Muted Slate

    // 3D Bevel Rim Highlights & Frosted Borders
    val RimHighlight = Color(0x40EA580C)     // Solar Rally Orange Rim
    val RimHighlightCyan = Color(0x402563EB)
    val FrostedOverlay = Color(0xF2FFFFFF)   // Luminous Translucent Pearl Glass
    val FrostedBorder = Color(0xFFCBD5E1)
}

