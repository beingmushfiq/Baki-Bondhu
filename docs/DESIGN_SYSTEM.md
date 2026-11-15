# Design System & Bengali UX Tokens — Baki Bondhu (বাকী বন্ধু)

> **Owner / Engineering**: DevCenterPoint ([devcenterpoint.com](https://devcenterpoint.com))  
> **Brand Direction**: "Modern Bengali Ledger — Minimal, Trustworthy, Refined"  
> **UI Framework**: Jetpack Compose Material 3 (M3 Expressive)

---

## 1. Design Direction & Core Philosophy

Baki Bondhu is tailored specifically for Bangladeshi shopkeepers (*দোকানদার*). In retail environments like grocery stores (*মুদির দোকান*), roadside tea stalls, and wholesale markets, the shopkeeper's hands may be dusty, the lighting may be bright daylight or dim evening bulb-light, and interaction time must be measured in seconds.

The visual identity combines:
- The **tactile clarity of a traditional paper ledger (*খাতা*)**.
- The **modern refinement and trust of a financial institution**.
- The **warmth and cultural familiarity of Bengali commerce**.

We strictly reject:
- ❌ Generic AI templates with oversized nested cards and tiny unreadable text.
- ❌ Low-contrast gray-on-white text that is invisible under shop glare.
- ❌ Arbitrary decorative gradients and gratuitous glassmorphic effects.
- ❌ Floating-point currency representations without currency symbols.

---

## 2. Color System & Design Tokens

### 2.1 Brand & Semantic Palette

| Token Name | Light Mode Hex | Dark Mode Hex | Usage |
| :--- | :--- | :--- | :--- |
| `BrandPrimary` | `#087B62` (Emerald Green) | `#2DD4BF` (Bright Mint) | Primary actions, FAB, headers, brand accents |
| `BrandSecondary` | `#075443` (Deep Forest) | `#14B8A6` (Teal) | Selected states, primary container fills |
| `SurfaceBackground` | `#F6F7F3` (Warm Ivory) | `#0E1720` (Midnight Navy) | Root application background |
| `SurfaceContainer` | `#FFFFFF` (Pure White) | `#16232B` (Slate Charcoal) | Ledger cards, dialogs, bottom sheets |
| `SurfaceVariant` | `#EFF3EF` (Soft Sage) | `#1E2D36` (Elevated Slate) | Inset inputs, chip backgrounds, search bars |
| `TextPrimary` | `#182820` (Deep Ink) | `#F1F5F9` (Bright Slate) | Headings, amounts, customer names |
| `TextSecondary` | `#64746A` (Muted Sage) | `#94A3B8` (Cool Grey) | Timestamps, notes, phone numbers |
| `BorderSubtle` | `#DFE7E0` (Pale Border) | `#273844` (Dark Border) | Card outlines, dividers |
| `HighlightAmber` | `#E6B957` (Warm Gold) | `#FBBF24` (Amber Gold) | Badges, overdue highlights, notices |

### 2.2 Financial Status Semantic Colors

Financial colors must be immediately recognizable across language barriers:
- 🔴 **Credit / Due (বাকী)**: `#DC2626` (Crimson Red) / Dark: `#F87171`
- 🟢 **Repayment / Inflow (জমা)**: `#16A34A` (Forest Green) / Dark: `#4ADE80`
- 🔵 **Cash Sale (নগদ)**: `#0284C7` (Sky Blue) / Dark: `#38BDF8`
- 🟠 **Overdue Warning (তাগাদা)**: `#D97706` (Amber Alert) / Dark: `#F59E0B`

---

## 3. Typography & Bengali Numeral Standards

### 3.1 Non-Breaking Currency Format
In Bengali typography, a line break between the Taka symbol (`৳`) and the numeral breaks readability. Baki Bondhu mandates an immutable non-breaking space (`\u00A0`):

$$\text{"৳\u00A0১,২৫০"}\quad (\text{NOT: "৳ 1250" or "৳\n1250"})$$

### 3.2 Numeral Localization Map
The `Money` and `BakiStrings` utilities map all standard ASCII digits `0–9` to their Bengali Unicode equivalents `০–৯`:

| ASCII | `0` | `1` | `2` | `3` | `4` | `5` | `6` | `7` | `8` | `9` |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Bengali** | `০` | `১` | `২` | `৩` | `৪` | `৫` | `৬` | `৭` | `৮` | `৯` |

### 3.3 Tabular Digits
All monetary amounts and transaction balances use font features configured with `tnum` (tabular numbers) so that currency digits line up vertically across ledger columns.

---

## 4. Retail Ergonomics & Touch Targets

1. **48dp Minimum Touch Boundary**:
   Every clickable element (chips, buttons, FAB, phone dials, WhatsApp triggers) enforces a minimum touch target of `48.dp` $\times$ `48.dp`.
2. **High-Contrast Floating Voice Action Button**:
   The primary microphone button uses a 64dp circle container elevated with dynamic shadows, positioned in the bottom-right corner for effortless one-handed thumb access.
3. **Edge-to-Edge Navigation Safe Insets**:
   All screens apply `WindowInsets.safeDrawing` or `safeContent` padding, ensuring status bars and 3-button Android system navigation never obscure interactive controls.
