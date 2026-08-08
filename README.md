# Perbandingan Kinerja Flutter vs Android Native (Kotlin)

Repositori ini berisi **source code**, **dokumentasi**, dan **skrip analisis statistik** untuk penelitian skripsi mengenai perbandingan kinerja aplikasi mobile berbasis **Flutter** dan **Android Native (Kotlin)** pada perangkat Android.

**Tautan repositori:**  
https://github.com/Luxferex/Perbandingan-Kinerja-Mobile---Skripsi

---

## Ringkasan Penelitian

Penelitian ini membandingkan kinerja dua pendekatan pengembangan aplikasi Android:

| Aspek | Flutter | Android Native |
|--------|---------|----------------|
| Bahasa / framework | Dart + Flutter | Kotlin |
| HTTP client | Dio | Retrofit + OkHttp |
| Database lokal | SQLite (`sqflite`) | SQLite (Room) |
| UI list | `ListView` / Flutter widgets | `RecyclerView` |

### Skenario pengujian
1. **HTTP Request** — mengambil data dari API publik  
2. **Rendering List** — generate & render 1000 item dummy  
3. **SQLite CRUD** — INSERT, SELECT, UPDATE, DELETE (1000 data dummy)

### Metrik yang diukur
- Waktu eksekusi (ms)
- Penggunaan CPU (%)
- Penggunaan memori RSS (MB)
- **Storage size** (ukuran APK release)
- **Minimum memory** (nilai minimum RSS dari hasil pengujian)

---

## Struktur Proyek

```text
Perbandingan-Kinerja-Mobile---Skripsi/
├── README.md                 ← dokumentasi utama (file ini)
├── Flutter-app/              ← source code aplikasi Flutter
├── Kotlin-app/               ← source code aplikasi Android Native (Kotlin)
└── analisis_statistik3.py    ← skrip analisis statistik (Python)
```

| Folder / File | Keterangan |
|---------------|------------|
| `Flutter-app/` | Aplikasi benchmark Flutter (source code lengkap) |
| `Kotlin-app/` | Aplikasi benchmark Kotlin/Android Native (source code lengkap) |
| `analisis_statistik3.py` | Analisis deskriptif & inferensial (Shapiro-Wilk, Welch / Mann-Whitney, effect size) + grafik |

> **Catatan EXE:** aplikasi ini berbasis **Android (APK)**, bukan desktop Windows. Tidak ada file `.exe`. Artefak instalasi yang setara adalah **APK release**.

---

## Data & Database

Penelitian ini **tidak memakai database server eksternal milik sendiri**. Ada dua jenis data:

### 1) API publik (skenario HTTP)
- **Endpoint:** `https://jsonplaceholder.typicode.com/posts`
- **Query:** `_limit=100`
- **Cache-busting:** setiap run menambahkan `_nonce` unik + header `Cache-Control: no-cache, no-store` dan `Pragma: no-cache`
- **Fungsi:** mensimulasikan permintaan jaringan HTTP yang realistis dengan beban data yang sama di Flutter dan Kotlin

JSONPlaceholder adalah API *fake REST* publik; response berupa daftar post (id, userId, title, body). Konten response dapat identik antar run, tetapi identitas request dibuat unik agar tidak bias cache.

### 2) Data dummy lokal (skenario Rendering & SQLite)
- **Jumlah:** 1000 item
- **Isi:** deterministik, contoh `Item 1`, `Item 2`, … agar workload Flutter = Kotlin
- **Struktur record:**
  - `id` (INTEGER)
  - `userId` (INTEGER)
  - `title` (TEXT)
  - `body` (TEXT)

### 3) Database lokal SQLite
- **Nama file DB:** `benchmark_posts.db`
- **Flutter:** dibuat otomatis oleh `sqflite` di penyimpanan app  
- **Kotlin:** dibuat otomatis oleh **Room** di penyimpanan app  
- **Tabel:** `posts` (skema sama di kedua aplikasi)
- Database **di-generate saat runtime** saat skenario SQLite dijalankan (bukan file `.db` terpisah yang harus di-upload). Setelah pengujian, data dapat di-clear oleh aplikasi.

---

## Aplikasi & APK (artefak program)

### Package / Application ID
| Aplikasi | Application ID |
|----------|----------------|
| Flutter | `com.example.perbandingan_kinerja_flutter` |
| Kotlin | `com.benchmark.androidnative` |

### Ukuran APK release (storage size)
| Aplikasi | File APK (contoh) | Ukuran |
|----------|-------------------|--------|
| Flutter | `app-arm64-v8a-release.apk` | ± **17,37 MB** |
| Kotlin | `app-release.apk` | ± **2,34 MB** |

APK dihasilkan dari build lokal:

**Flutter**
```bash
cd Flutter-app
flutter build apk --release --split-per-abi
```
Hasil: `Flutter-app/build/app/outputs/flutter-apk/`

**Kotlin**
```bash
cd Kotlin-app
./gradlew.bat assembleRelease
```
Hasil: `Kotlin-app/app/build/outputs/apk/release/app-release.apk`

> Untuk lampiran yudisium, unggah APK release (Flutter arm64 + Kotlin release) bersama tautan GitHub ini, atau simpan APK di Google Drive / GitHub Releases lalu cantumkan tautannya di form yudisium.

---

## Cara Menjalankan Source Code

### Prasyarat
- Flutter SDK (untuk `Flutter-app`)
- Android Studio + JDK (untuk `Kotlin-app`)
- Perangkat Android / emulator (disarankan perangkat fisik yang sama untuk pengujian kinerja)
- Koneksi internet (wajib untuk skenario HTTP)

### Menjalankan Flutter
```bash
cd Flutter-app
flutter pub get
flutter run --release
# atau install APK release hasil build
```

### Menjalankan Kotlin
1. Buka folder `Kotlin-app` di Android Studio  
2. Sync Gradle → Run  
atau:
```bash
cd Kotlin-app
./gradlew.bat installRelease
```

### Alur pengujian di aplikasi
1. Buka **Persiapan Pengujian** (checklist: WiFi, brightness, background apps, dsb.)
2. Jalankan skenario: HTTP / Rendering / SQLite
3. Atur jumlah repetisi, jalankan benchmark
4. Lihat ringkasan hasil → ekspor CSV
5. Analisis CSV dengan `analisis_statistik3.py`

---

## Analisis Statistik

```bash
python analisis_statistik3.py "benchmark_flutter.csv" "benchmark_kotlin.csv"
```

**Output:**
- `hasil_uji_statistik.xlsx`
- `statistik_deskriptif.xlsx`
- Folder `charts/` (grafik perbandingan metrik)

**Alur uji:**
1. Shapiro-Wilk (uji normalitas per kelompok)
2. Jika keduanya normal → Welch’s t-test; jika tidak → Mann-Whitney U
3. Effect size: Hedges’ g* atau Rank-biserial *r*

---

## Teknologi Utama

### Flutter (`Flutter-app`)
- Flutter / Dart
- Provider
- Dio
- sqflite
- path / path_provider
- share_plus (ekspor hasil)

### Kotlin (`Kotlin-app`)
- Kotlin + Android SDK
- Retrofit + OkHttp + Gson
- Room (SQLite)
- ViewModel + LiveData
- Coroutines
- RecyclerView + ViewBinding

### Analisis
- Python 3
- pandas, numpy, scipy
- matplotlib (opsional, untuk grafik)
- openpyxl / Excel writer (untuk `.xlsx`)

---

## Isi Lampiran Yudisium (checklist)

Gunakan daftar ini saat mengisi form “tautan semua file program”:

| Item | Status / lokasi |
|------|------------------|
| README | file ini (`README.md`) |
| Source code Flutter | folder `Flutter-app/` |
| Source code Kotlin | folder `Kotlin-app/` |
| Skrip analisis | `analisis_statistik3.py` |
| APK Flutter (release) | hasil build / Drive / Releases |
| APK Kotlin (release) | hasil build / Drive / Releases |
| Database | SQLite lokal `benchmark_posts.db` (auto-generate); API: JSONPlaceholder |
| EXE desktop | **tidak ada** (aplikasi Android) |
| Data hasil (CSV/Excel) | hasil ekspor app + output `analisis_statistik3.py` |

---

## Catatan Metodologi Singkat

- Pengujian dilakukan pada perangkat Android yang sama untuk kedua aplikasi.
- Warm-up HTTP dilakukan sebelum pengukuran resmi (tidak dihitung sebagai data uji).
- Skenario HTTP memakai cache-busting (`_nonce` + header no-cache) agar setiap run melakukan request fresh.
- Data dummy Rendering/SQLite dibuat deterministik agar beban kerja antar framework setara.
- Metrik memori memakai RSS proses; storage size memakai ukuran APK release.

---

## Lisensi & Penggunaan

Proyek ini dibuat untuk keperluan **penelitian skripsi**.  
API JSONPlaceholder adalah layanan publik pihak ketiga; penggunaannya mengikuti ketentuan layanan tersebut.
