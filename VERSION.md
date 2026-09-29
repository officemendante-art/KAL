# KAL Version Tracking & Release History

Current Status: **In Development (Unreleased Changes)**  
Latest Release: **v1.1.0** (versionCode `2`)  
Repository: **KAL** (`com.kal`) — Offline Android Calculator & Cash Counter

---

## Unreleased Fixes & Enhancements

### Fixed
- **Restored Core Number Sizing from Original Fork**:
  - Restored `topResultGuideline` back to exact original value `0.245` (and `0.225` on large/xlarge).
  - Returned full 140dp vertical space to `expressionEditText` so top expression numbers render at their original **Big (86sp)** size (`6,464×464`).
  - Preserved `resultText` at original **Mid-size (56sp)** directly below the expression with dedicated vertical headroom (`2,999,296`).
  - Positioned the compact 28dp word preview strip strictly at the bottom below the numbers (`pagerGuideline="0.37"`), preserving the exact core layout without tampering with number typography.
- **Preview Equation Comma Tokenization**:
  - Fixed expression tokenizer regex in `formatPreviewEquation` to `\d[\d,]*(\.\d+)?` to parse comma-separated numbers (e.g. `8,989`) as a single entity without detached punctuation tokens.

### Added
- **Numbers-to-Words Inline Preview Strip**:
  - Real-time spelled-out equation and result conversion pill under the calculator result display (`89 × 89` -> `Eighty-Nine × Eighty-Nine = Seven Thousand Nine Hundred Twenty-One`).
  - Dual-end soft gradient masks (`fade_mask_left`, `fade_mask_right`) for smooth text fading into pill background, with diagonal double-headed expand icon affordance.
  - Comprehensive unit test suite in `NumberToWordsConverterTest.kt`.
- **Numbers-in-Words Bottom Popup Sheet**:
  - Modal bottom sheet displaying complete spelled-out words in prominent typography.
  - Native background blur (`RenderEffect.createBlurEffect`) on Android 12+ (API 31+) with fallback scrim dimming.
  - Pull-to-dismiss drag handle, outside tap dismissal, and predictive back gesture handling (`OnBackPressedCallback`).
- **Cyberpunk Matrix Copy Button**:
  - Compact header pill button with tap scale compression (`0.96x`).
  - Single-tap plain text clipboard copy with haptic feedback.
  - Character scramble sequence cycling through glyphs (`01X_$![]`), morphing into terminal green `#00ff87` with checkmark icon and "Done", holding for 1.9s, and scrambling back to "Copy".

### Changed
- **Indian Numbering System (INR) Spelled-out Words**:
  - Converted `NumberToWordsConverter.kt` strictly to Indian numbering terminology utilizing **Lakhs and Crores** (e.g. `8,00,021` -> **"Eight Lakh Twenty-One"**).
- **Indian Comma Grouping Across App**:
  - Standardized number grouping in `NumberFormatter.kt` to the Indian format (`1,00,000` / `12,34,567`), grouping the last 3 digits followed by 2-digit pairs across expressions, results, unit converter, and settings preview.

---

## Release History

### [1.1.0] - 2026-09-24
- **Cash Counter Coins & Full Currency Support**:
  - Added support for 3 official Indian Rupee coin denominations: ₹5, ₹2, and ₹1 with custom circular coin iconography.
  - Dynamic Amount in Words display in standard Indian numbering system (`IndianCurrencyWords.kt`).
  - Spectrum breakdown bar visually representing proportional currency distribution across all denominations.
- **Privacy & Compliance**:
  - Root `PRIVACY_POLICY.md` strictly aligned with Google Play Store Data Safety guidelines (Publisher: NORVI, Founder: DANTE, offline operation, zero data collection).
  - In-app offline privacy policy dialog clearly informing users of zero data collection without external links.
- **Developer Attribution**:
  - Developer row in About view ("NORVI • DANTE") linking to official GitHub profile.
- **Cash Counter Scroll Mistouch Fix**:
  - Removed outer row click listeners on denomination cards so scrolling through cash counter list no longer causes accidental row activations or keyboard popups.
  - Scoped touch responsiveness strictly to the middle section (count figure for direct keypad entry, `+` and `-` buttons for steppers).
- **Cash Counter Keypad Action**:
  - Replaced sequential "Next" button with a permanent "Done" button with checkmark icon to immediately dismiss the keyboard.
- **About Screen Streamlining**:
  - Consolidated into clean "App info" section (Developer, Licenses, Privacy policy, Version).
  - Locked the Version row to a static, non-clickable display showing only the active version number (`1.1.0`).

### [1.0.0] - 2026-09-21
- Initial standalone release of **KAL** (`com.kal`).
- Full Indian Rupee (INR) cash denomination counting screen for 6 banknote denominations: ₹500, ₹200, ₹100, ₹50, ₹20, and ₹10.
- Custom 4×4 numeric keypad with auto-scrolling and focus highlighting.
