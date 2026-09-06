# Bin Unpacker (Android)

Ek simple Android app jo `.bin` file ke andar chhupi hui files (image, zip, pdf,
audio, video, etc.) ko pehchan kar unhe alag-alag files ke roop me phone
storage me save kar deti hai.

## Yeh kaise kaam karta hai

`.bin` ek generic extension hai — iska koi ek fixed format nahi hota. Isliye
yeh app har known file-type ke "magic bytes" (jaise PNG, JPEG, ZIP/APK, PDF,
MP3, MP4, ELF, GZIP, RAR, 7z, etc.) ke liye poori file scan karta hai. Jahan
bhi koi signature milta hai, wahan se agla signature milne tak ka data ek
"item" maan liya jata hai. Agar kuch bhi pehchana na ja sake, to poori file ek
hi "Unknown/Raw" item ke roop me dikhai jaati hai.

Yeh wahi technique hai jo forensic/carving tools (jaise `binwalk`) use karte
hain.

## App me use

1. App kholein → **"BIN File Select Karein"** button dabayein.
2. Apni `.bin` file phone storage se choose karein.
3. Scan hone ke baad list me har item ka type, offset aur size dikhega.
4. **"Sab Save Karein"** dabane par sab items alag-alag files ban kar save ho
   jaate hain:

   ```
   Android/data/com.binunpacker.app/files/extracted_<filename>/
   ```

   Yeh app-specific storage folder hai, isliye kisi extra runtime permission
   ki zaroorat nahi padti.

## GitHub par build karna (APK banwana)

1. Is poore folder ko ek naye GitHub repository me push karein.
2. `.github/workflows/android-build.yml` already included hai — jaise hi aap
   `main` branch par push karenge, GitHub Actions automatically build shuru
   kar dega.
3. Repo ke **Actions** tab me jaake latest run kholein.
4. Run complete hone par neeche **Artifacts** section me
   `BinUnpacker-debug-apk` milega — usse download karke apne phone me install
   kar lein (Unknown sources install allow karna padega).

### Local machine par build (Android Studio)

Agar aap Android Studio me kholna chahte hain:

1. Android Studio → **Open** → is folder ko select karein.
2. Gradle sync khud ho jaayega (internet chahiye pehli baar).
3. **Run ▶** dabakar seedha apne connected phone/emulator par install karein,
   ya **Build → Build Bundle(s)/APK(s) → Build APK(s)** se APK banayein.

## Project structure

```
BinUnpacker/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/binunpacker/app/
│       │   ├── MainActivity.kt        (UI + file picker)
│       │   ├── BinExtractor.kt        (signature scanning + extraction logic)
│       │   ├── ExtractedEntry.kt      (data model)
│       │   └── ExtractedAdapter.kt    (RecyclerView list)
│       └── res/ (layouts, strings, theme, icon)
├── .github/workflows/android-build.yml
├── build.gradle, settings.gradle, gradle.properties
└── README.md
```

## Naye file-type signatures add karna

`BinExtractor.kt` me `SIGNATURES` list me naya `Signature(name, extension,
magicOffset, magicBytes)` add karke aur bhi formats detect karwaye ja sakte
hain.
