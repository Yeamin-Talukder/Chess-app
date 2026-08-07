package com.example.chess.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val light_background = Color(0xFFF8F7F5)
val light_surface = Color(0xFFFFFFFF)
val light_surfaceVariant = Color(0xFFF0ECE7)
val light_primary = Color(0xFF6CA13C)
val light_accent = Color(0xFFDAB57A)
val light_secondary = Color(0xFFDAB57A) 
val light_textPrimary = Color(0xFF1A1A1A)
val light_textSecondary = Color(0xFF555555)
val light_border = Color(0xFFDDDDDD)
val light_success = Color(0xFF4CAF50)
val light_warning = Color(0xFFFF9800)
val light_error = Color(0xFFD32F2F)
val light_onPrimary = Color(0xFFFFFFFF)

// Dark Theme Colors
val dark_background = Color(0xFF262421)
val dark_surface = Color(0xFF312E2B)
val dark_surfaceVariant = Color(0xFF3C3936)
val dark_primary = Color(0xFF81B64C)
val dark_primaryDark = Color(0xFF6CA13C)
val dark_accent = Color(0xFFF0D9B5)
val dark_secondary = Color(0xFFB58863)
val dark_textPrimary = Color(0xFFFFFFFF)
val dark_textSecondary = Color(0xFFCFCFCF)
val dark_border = Color(0xFF4A4744)
val dark_success = Color(0xFF5FAF4D)
val dark_warning = Color(0xFFE0A526)
val dark_error = Color(0xFFD9534F)
val dark_onPrimary = Color(0xFFFFFFFF)

// Chess Board Specific Colors
val board_lightSquare = Color(0xFFEFE4D3) // Warm beige
val board_darkSquare = Color(0xFF849C65) // Muted olive green
val board_borderDarkWood = Color(0xFF3B2F2F)

// Highlight Colors
val highlight_selection = Color(0x80C6E48B)
val highlight_lastMove = Color(0x80F7EC74)
val highlight_check = Color(0x80FF6B6B)
val highlight_legalMove = Color(0x6681B64C)
val highlight_capture = Color(0x66FF6B6B)

// Puzzle-Specific Colors
val puzzle_correct = Color(0xFF96BC4B)      // Lichess-style correct green
val puzzle_wrong = Color(0xFFE84A4A)        // Wrong move red
val puzzle_hint = Color(0xFFF0A030)         // Hint arrow / accent orange
val puzzle_streakFire = Color(0xFFFF7043)   // Streak fire glow

// Puzzle Rating Tier Badge Colors
val puzzle_ratingBeginner = Color(0xFF66BB6A)  // < 1200
val puzzle_ratingInter = Color(0xFF42A5F5)     // 1200–1600
val puzzle_ratingAdvanced = Color(0xFFFFA726)  // 1600–2000
val puzzle_ratingExpert = Color(0xFFEF5350)    // 2000–2500
val puzzle_ratingMaster = Color(0xFFAB47BC)    // 2500+
