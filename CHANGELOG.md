# KAL Version Tracking & Changelog

All notable changes to the **KAL** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- **Dynamic Word Bottom Sheet Font Sizing (Hybrid Control)**:
  - Interactive two-finger pinch-to-zoom directly on the text body via `ScaleGestureDetector`, with touch-arena disallow-intercept coordination to eliminate conflicts with `NestedScrollView` scrolling and bottom sheet dragging.
  - One-handed header font size stepper button (`btnMatrixFont`) next to Copy button, featuring a minimalist circular icon button (numbers removed for a cleaner look) that cycles through calibrated sizes (`18sp` $\rightarrow$ `24sp` $\rightarrow$ `28sp` $\rightarrow$ `34sp` $\rightarrow$ `18sp`).
  - Triggered enlarge bounce animation: icon physically expands and spring-bounces on pinch zoom and button tap using `OvershootInterpolator`, playing only during the zoom interaction and never looping continuously in the background.
  - Strict font size bounding (`[18sp, 34sp]`) and automatic release snapping to prevent text overflow or broken layouts.
  - First-Time User Experience (FTUE) gesture tooltip bubble (`layoutGestureTooltip`) displaying pinch hand gesture vector icon (`ic_pinch_gesture.xml`) and guidance hint on initial sheet open, with auto-dismiss after 3.5s and instant dismissal on touch.
  - Persistent user font size preference and tooltip display tracking in `Preferences.kt`.
- **Financial Cheque-Writing Denomination Accents & Spacing**:
  - Semantic place value highlighting for all Indian denominations (`Crore`, `Lakh`, `Thousand`, `Hundred`, `Point`) in theme mint accent green (`#00B36E`) with bold weight, making financial transcription on cheques and invoices effortless.
  - Increased default font size of `tvExpandedWords` from `20sp` to `24sp`.
  - Optimized typography with enhanced line spacing (`lineSpacingMultiplier="1.42"`, `lineSpacingExtra="2dp"`) and letter tracking (`letterSpacing="0.02"`) on Inter font to eliminate glyph crowding.
- **Apple iOS-Grade 120Hz Interactive Drag Lever for Words Bottom Sheet**:
  - Engineered a high-refresh-rate bidirectional interactive lever handle allowing continuous dragging and flicking between snug compact hug mode and the keypad ceiling guideline (`pager.top`).
  - Implemented 100% GPU RenderNode `translationY` tracking to eliminate measurement and layout invalidations on `ACTION_MOVE`, locking frame rendering to 120fps (8.33ms) on high refresh rate displays.
  - Integrated AndroidX `SpringAnimation` physics with `DAMPING_RATIO_LOW_BOUNCY` (`0.75f`) for Apple's signature subtle tactile overshoot and organic settling.
  - Added continuous momentum handoff with `VelocityTracker` so upward/downward flicks seamlessly propel the lever to its anchor.
  - Added logarithmic rubber-band dampening when dragging beyond the ceiling boundary.
  - Enlarged handle hit area with a 32dp container (`layoutDragHandleArea`) supporting single-tap toggling between compact and ceiling views.
- **Numbers-to-Words Inline Preview Strip**:
  - Real-time spelled-out equation and result conversion pill under the calculator result display (`89 × 89` -> `Eighty-Nine × Eighty-Nine = Seven Thousand Nine Hundred Twenty-One`).
  - Dual-end soft gradient masks (`fade_mask_left`, `fade_mask_right`) for smooth text fading into pill background, with diagonal double-headed expand icon affordance.
  - Numerical conversion engine (`NumberToWordsConverter.kt`) supporting integers, decimals, and negative numbers with comprehensive unit test suite (`NumberToWordsConverterTest.kt`).
- **Numbers-in-Words Bottom Sheet**:
  - Modal bottom sheet displaying complete spelled-out words in prominent typography.
  - Native background blur (`RenderEffect.createBlurEffect`) on Android 12+ (API 31+) with backward-compatible scrim dimming.
  - Drag handle pull-down dismissal, outside scrim tap dismiss, and predictive back gesture handling (`OnBackPressedCallback`).
- **Cyberpunk Matrix Copy Button**:
  - Compact header pill button with tap scale compression (`0.96x`).
  - Single-tap plain text clipboard copy with haptic feedback.
  - Character scramble sequence cycling through glyphs (`01X_$![]`), morphing into cyberpunk terminal green `#00ff87` with checkmark icon and "Done", holding for 1.9s, and scrambling back to "Copy".

### Fixed
- **"In Words" Bottom Sheet Detachment / Floating Card Bug**:
  - Fixed an issue where dragging the bottom sheet upward moved the entire view with negative `translationY`, detaching the bottom edge from the screen and exposing the keypad underneath.
  - Pinned the bottom edge permanently to `parent.bottom` and implemented upward dynamic height expansion (`compactHeight` $\rightarrow$ `ceilingHeight`), ensuring the bottom remains 100% attached, completely solid white, and never floats.
- **"In Words" Bottom Sheet Over-Expansion Bug**:
  - Fixed an issue where the bottom sheet expanded all the way to the rooftop ceiling on short numbers, leaving excessive empty white space below the text.
  - Made the bottom sheet and its scrolling body wrap content snugly to the text height by default (`paddingBottom="20dp"`), dynamically calculating and clamping the height only when content actually exceeds the keypad rooftop boundary.
- **"In Words" Anti-Vanishing Subtotaling on Trailing Operators**:
  - Fixed bug where typing an operator (`+`, `-`, `×`, `÷`) caused calculation syntax errors that cleared `resultText`, making the words strip vanish and pop back in on the next digit. The engine now detects trailing operators, evaluates the prefix subtotal, and keeps the strip visible at subtle pending opacity (`alpha = 0.72`) for continuous, fluid UX.
- **Streamlined Result-Focused Words Display**:
  - Simplified words display to present the clean evaluated numerical result in words (e.g. `One Hundred Ninety-Eight`) instead of cluttered equation sentences (`Ninety-Nine + Ninety-Nine = ...`), matching banking standards and eliminating visual noise.
- **Core Number Sizing Restored from Original Fork**:
  - Restored `topResultGuideline` back to exact original value `0.245` (and `0.225` on large/xlarge), returning full vertical space (140dp) to `expressionEditText` so top expression numbers render at their original **Big (86sp)** size (`6,464×464`).
  - Preserved `resultText` at original **Mid-size (56sp)** directly below the expression with dedicated vertical headroom.
  - Positioned the compact 28dp word preview strip strictly at the bottom below the numbers (`pagerGuideline="0.37"`), preserving the exact core layout without altering number typography.
- **Preview Equation Comma Tokenization**:
  - Updated expression token regex in `formatPreviewEquation` to `\d[\d,]*(\.\d+)?` to parse comma-separated numbers (such as `8,989`) as a single entity without detached punctuation tokens.

### Changed
- **"In Words" Bottom Sheet Typography & Rooftop Constraint**:
  - Upgraded expanded words typography to Inter Semi-Bold (`@font/inter_semibold`, weight 600) for prominent, clean, minimalistic readability that naturally integrates into the app aesthetic.
  - Constrained bottom sheet vertical climb with a strict rooftop ceiling anchored at `pagerGuideline` (`app:layout_constraintTop_toTopOf="@id/pagerGuideline"`, `app:layout_constraintVertical_bias="1.0"`, `app:layout_constrainedHeight="true"`), allowing the sheet to hug the bottom for small numbers and climb gradually up to the keypad boundary.
  - Integrated seamless invisible vertical scrolling (`scrollWordsBody`) with hidden scroll indicators (`android:scrollbars="none"`, `android:overScrollMode="never"`) and auto-reset to top on display for large crore amounts.
- **"Point" Decimal Word Minty Green Accent**:
  - Dynamically highlights the word "Point" in the app's signature Minty Green / Accent color (`colorTertiary` / `#00B36E`) with bold styling in both the inline preview strip and the expanded bottom sheet whenever decimal numbers are displayed.
- **"In Words" Edge-to-Edge Redesign & Touch Sliding**:
  - Expanded the "In Words" preview container to full screen width (`layout_width="0dp"`, spanning parent start to parent end) so it touches both corners seamlessly without collapsing into a small centered floating pill on small numbers.
  - Eliminated the pill border and frame stroke for an uninterrupted, seamless blend into `?attr/colorSurface`.
  - Added horizontal finger scrolling (`HorizontalScrollView`) allowing users to slide and explore long equation words smoothly.
  - Pinned dynamic gradient fade masks to the outer edges of the screen, resolving theme `colorSurface` at runtime with scroll-aware alpha opacity.
- **Indian Numbering System (INR) Spelled-out Words**:
  - Converted `NumberToWordsConverter.kt` strictly to Indian numbering terminology utilizing **Lakhs and Crores** (e.g. `8,00,021` -> **"Eight Lakh Twenty-One"**).
- **Indian Comma Grouping Across App**:
  - Standardized number grouping in `NumberFormatter.kt` to the Indian format (`1,00,000` / `12,34,567`), grouping the last 3 digits followed by 2-digit pairs across expressions, results, unit converter, and settings preview.

---

## [1.1.0] - 2026-09-24

### Added
- **Cash Counter Coins & Full Currency Support**:
  - Support for 3 official Indian Rupee coin denominations: ₹5, ₹2, and ₹1 with custom circular coin iconography.
  - Dynamic Amount in Words display in standard Indian numbering system (`IndianCurrencyWords.kt`), with automatic space-saving collapse when keypad opens.
  - Spectrum breakdown bar visually representing proportional currency distribution across all denominations.
- **Privacy & Compliance**:
  - Root `PRIVACY_POLICY.md` strictly aligned with Google Play Store Data Safety guidelines (Publisher: NORVI, Founder: DANTE, offline operation, zero data collection).
  - In-app offline privacy policy dialog clearly informing users of zero data collection without external links.
- **Developer Attribution**:
  - Developer row in About view ("NORVI • DANTE") linking to official GitHub profile.

### Fixed
- **Cash Counter Scroll Mistouch**:
  - Removed outer row click listeners on denomination cards so scrolling through cash counter list no longer causes accidental row activations or keyboard popups.
  - Scoped touch responsiveness strictly to the middle section (count figure for direct keypad entry, `+` and `-` buttons for steppers).
- **Cash Counter Keypad Action**:
  - Replaced sequential "Next" button with a permanent "Done" button featuring a checkmark icon to immediately dismiss the keyboard, clearing row focus and restoring the Amount in Words hero view.

### Changed
- **About Screen Streamlining**:
  - Consolidated into a clean, unified "App info" section containing Developer, Licenses, Privacy policy, and Version.
  - Locked the Version row to a static, non-clickable display showing only the active version number (`1.1.0`), removing user-facing changelog dialogs.
- **Project Versioning**:
  - Bumped `versionCode` to `2` and `versionName` to `1.1.0` in `app/build.gradle.kts`.

---

## [1.0.0] - 2026-09-21

### Summary
Initial standalone release of **KAL** (`com.kal`), an autonomous, high-performance fork of Calculator-You tailored for modern Android 16 and Xiaomi HyperOS.

### Added
- **Cash Counter Feature**:
  - Full Indian Rupee (INR) cash denomination counting screen.
  - Interactive rows for 6 official banknote denominations: ₹500, ₹200, ₹100, ₹50, ₹20, and ₹10.
  - Step increment (`+`) and decrement (`-`) pill controls per denomination.
  - Real-time subtotal calculation and live Grand Total summary with note count badge.
  - In-app custom 4×4 numeric keypad with active denomination auto-scrolling and focus highlighting.
  - Standard Indian currency numbering system (2,2,3 grouping).
- **120 FPS Apple-Style Navigation**:
  - Interactive gesture slide navigation between Calculator and Cash Counter with 1/3 parallax on underlying screen.
  - Hardware layer caching (`LAYER_TYPE_HARDWARE`) for zero-stutter 120Hz display refresh.
- **Brand Identity & Iconography**:
  - Custom brand icon featuring math operators (`+`, `~`, `×`, `=`) on `#FAFAFA` canvas.
  - Full adaptive icon support (foreground, background, monochrome for Material You themed icons).

### Changed
- Complete architectural rebranding to `com.kal` (KAL).
- Cleaned About screen removing third-party donation, translation, and personal email links.
