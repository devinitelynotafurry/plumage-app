# Plumage

An Android client for [e926](https://e926.net), the safe-only sister site of e621.
Browse your favorite tags with a swipe-based interface: swipe right to keep a post in a collection; swipe left to bury it so it never comes back.

## Features

- **Swipe-to-Collect:** Quickly organize your feed. Right for "Keep", Left for "Bury".
- **Advanced Filtering:** Built-in AI-generation filter and a customizable blocklist.
- **High-Quality Viewer:** Support for high-res images and animated GIFs/WebP.
- **Material You:** Full support for Material 3 and Dynamic Color.
- **Privacy First:** Scoped storage usage (no storage permissions required).

## Installation

1.  Download the latest APK from the [Releases](https://github.com/YOUR_USERNAME/Plumage/releases) page.
2.  Install the APK on your device.
3.  **Important:** Open **Settings** in the app and enter your e926 username or a contact handle. This ensures the app is a "good API citizen" by identifying itself to the e926 servers.

**Compatibility:** Requires Android 10 (API 29) or higher.

## Development

If you want to build Plumage from source:

1.  Clone this repository and open the folder in Android Studio (Ladybug or newer).
2.  Studio will offer to generate the Gradle wrapper on first open, or run `gradle wrapper`.
3.  Sync the project and run.

---

## Technical Details

### Being a good API citizen
e926 expects a contact handle to prevent blanket-banning clients. Plumage sends `Plumage/1.0 (your_username)` on every request. 

`RateLimiter` ensures at least 1000ms between API calls. Throttling is applied at the repository layer to avoid slowing down image pre-fetching.

### The six-tag ceiling
e926 allows six tags per search. `QueryBuilder` automatically manages this budget, accounting for `rating:s`, AI filters, and sort metatags, reporting any dropped tags to the UI.

### Filtering Layers
1. **Server Side:** `rating:s` and AI filtering.
2. **Client Side (PostFilter):** Drops deleted posts, non-safe ratings, video formats (`.webm`, `.mp4`), and anything in your "seen" ledger.
3. **User Blocklist:** A customizable list to hide content based on specific tags.

### Known Tradeoffs
- **Domain/Data Coupling:** Domain models map directly to Room entities to keep the project single-module.
- **Seen Ledger:** Applied client-side to save tag slots. `PostRepository` retries pages if a refill results in too many already-seen posts.
- **Tests:** Currently lacks automated tests. Priority areas for future testing are `QueryBuilder`, `PostFilter`, and DAO queries.
