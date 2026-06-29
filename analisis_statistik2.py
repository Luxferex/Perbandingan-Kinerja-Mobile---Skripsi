"""
Script Analisis Statistik - Perbandingan Kinerja Flutter vs Android Native Kotlin
====================================================================================
VERSI INDEPENDENT SAMPLES (koreksi metodologi)

PENTING - mengapa berubah dari versi sebelumnya:
    Data dikumpulkan secara BERURUTAN (seluruh run Flutter dijalankan dulu,
    baru seluruh run Kotlin). Run ke-1 Flutter TIDAK berpasangan secara alami
    dengan run ke-1 Kotlin - keduanya berasal dari sesi eksekusi yang berbeda.
    Karena itu data ini adalah dua sampel INDEPENDEN, bukan data berpasangan.

    Uji berpasangan (paired t-test / Wilcoxon signed-rank) hanya valid jika ada
    pasangan alami (subjek sama diukur dua kali). Di sini tidak ada, sehingga
    digunakan uji untuk sampel independen.

Cara pakai:
    python analisis_statistik.py file_flutter.csv file_kotlin.csv

Atau edit FILE_FLUTTER / FILE_KOTLIN di bawah lalu jalankan:
    python analisis_statistik.py

Output:
    - Ringkasan ke terminal
    - hasil_uji_statistik.xlsx      -> tabel uji statistik
    - statistik_deskriptif.xlsx     -> mean, std, median, min, max, CV per skenario & framework
    - charts/execution_time_ms.png  -> grafik perbandingan waktu eksekusi
    - charts/cpu_percent.png        -> grafik perbandingan penggunaan CPU
    - charts/memory_mb.png          -> grafik perbandingan penggunaan memori

Tahapan uji (independent samples):
    1. Uji normalitas Shapiro-Wilk pada MASING-MASING grup (Flutter, Kotlin)
       -> menentukan apakah memenuhi syarat uji parametrik
    2. Jika KEDUA grup normal:
           Uji homogenitas varians Levene
               - varians homogen   -> Independent samples t-test (Student)
               - varians heterogen -> Welch's t-test
       Jika salah satu grup TIDAK normal:
           -> Mann-Whitney U test (non-parametrik)
    3. Cohen's d (pooled standard deviation) sebagai effect size
       -> seberapa besar perbedaan secara praktis
"""

import sys
import os
import pandas as pd
import numpy as np
from scipy import stats

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False

# =========================================================================
# KONFIGURASI
# =========================================================================
FILE_FLUTTER = "benchmark_flutter.csv"
FILE_KOTLIN = "benchmark_kotlin.csv"
ALPHA = 0.05
OUTPUT_EXCEL_UJI = "hasil_uji_statistik.xlsx"
OUTPUT_EXCEL_DESKRIPTIF = "statistik_deskriptif.xlsx"
OUTPUT_CHART_DIR = "charts"

METRICS = ["execution_time_ms", "cpu_percent", "memory_mb"]
METRIC_LABELS = {
    "execution_time_ms": "Waktu Eksekusi (ms)",
    "cpu_percent": "Penggunaan CPU (%)",
    "memory_mb": "Penggunaan Memori (MB)",
}
SCENARIOS = ["http", "rendering", "sqlite"]
SCENARIO_LABELS = {
    "http": "HTTP Request",
    "rendering": "Rendering List",
    "sqlite": "SQLite CRUD",
}


# =========================================================================
# EFFECT SIZE - COHEN'S D UNTUK SAMPEL INDEPENDEN
# =========================================================================
def cohens_d_independent(x, y):
    """
    Cohen's d untuk dua sampel independen, memakai pooled standard deviation.
    d = (mean_x - mean_y) / sd_pooled
    sd_pooled = sqrt( ((nx-1)*var_x + (ny-1)*var_y) / (nx+ny-2) )
    """
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    nx, ny = len(x), len(y)
    vx, vy = x.var(ddof=1), y.var(ddof=1)
    pooled_sd = np.sqrt(((nx - 1) * vx + (ny - 1) * vy) / (nx + ny - 2))
    if pooled_sd == 0:
        return 0.0
    return (x.mean() - y.mean()) / pooled_sd


def interpret_effect_size(d):
    ad = abs(d)
    if ad < 0.2:
        return "negligible"
    elif ad < 0.5:
        return "small"
    elif ad < 0.8:
        return "medium"
    else:
        return "large"


# =========================================================================
# PIPELINE UJI STATISTIK (INDEPENDENT SAMPLES)
# =========================================================================
def run_pipeline(df_flutter, df_kotlin, metrics=METRICS, scenarios=SCENARIOS, alpha=ALPHA):
    results = []

    for scenario in scenarios:
        flu_s = df_flutter[df_flutter["scenario"] == scenario]
        kot_s = df_kotlin[df_kotlin["scenario"] == scenario]

        if len(flu_s) == 0 or len(kot_s) == 0:
            print(f"[PERINGATAN] Skenario '{scenario}' tidak lengkap di salah satu file, dilewati.")
            continue

        for metric in metrics:
            if metric not in flu_s.columns or metric not in kot_s.columns:
                print(f"[PERINGATAN] Kolom '{metric}' tidak ditemukan, dilewati.")
                continue

            x = flu_s[metric].astype(float).dropna().values  # Flutter
            y = kot_s[metric].astype(float).dropna().values  # Kotlin
            nx, ny = len(x), len(y)

            # --- Tahap 1: Shapiro-Wilk pada MASING-MASING grup ---
            def safe_shapiro(v):
                if len(v) < 3:
                    return np.nan
                try:
                    return stats.shapiro(v).pvalue
                except Exception:
                    return np.nan

            sh_p_flu = safe_shapiro(x)
            sh_p_kot = safe_shapiro(y)
            both_normal = (
                pd.notna(sh_p_flu) and pd.notna(sh_p_kot)
                and sh_p_flu > alpha and sh_p_kot > alpha
            )

            # --- Tahap 2: pilih uji ---
            levene_p = np.nan
            if both_normal:
                # Uji homogenitas varians Levene (berbasis median, robust)
                try:
                    levene_p = stats.levene(x, y, center="median").pvalue
                except Exception:
                    levene_p = np.nan
                equal_var = pd.notna(levene_p) and levene_p > alpha
                if equal_var:
                    test_name = "Independent t-test"
                else:
                    test_name = "Welch's t-test"
                try:
                    p_val = stats.ttest_ind(x, y, equal_var=equal_var).pvalue
                except Exception:
                    p_val = np.nan
            else:
                test_name = "Mann-Whitney U"
                try:
                    p_val = stats.mannwhitneyu(x, y, alternative="two-sided").pvalue
                except Exception:
                    p_val = np.nan

            sig = "Signifikan" if (pd.notna(p_val) and p_val < alpha) else "Tidak signifikan"

            # --- Tahap 3: Cohen's d (pooled) ---
            d = cohens_d_independent(x, y)
            effect = interpret_effect_size(d)

            results.append({
                "Skenario": scenario,
                "Metrik": metric,
                "n Flutter": nx,
                "n Kotlin": ny,
                "Mean Flutter": round(float(np.mean(x)), 3),
                "Mean Kotlin": round(float(np.mean(y)), 3),
                "Shapiro p (Flutter)": round(sh_p_flu, 4) if pd.notna(sh_p_flu) else None,
                "Shapiro p (Kotlin)": round(sh_p_kot, 4) if pd.notna(sh_p_kot) else None,
                "Normal?": "Ya" if both_normal else "Tidak",
                "Levene p": round(levene_p, 4) if pd.notna(levene_p) else None,
                "Uji yang Dipakai": test_name,
                "p-value": round(p_val, 4) if pd.notna(p_val) else None,
                "Hasil": sig,
                "Cohen's d": round(d, 4),
                "Effect Size": effect,
            })

    return pd.DataFrame(results)


# =========================================================================
# STATISTIK DESKRIPTIF (tidak berubah)
# =========================================================================
def build_descriptive_table(df_all, metrics=METRICS, scenarios=SCENARIOS):
    rows = []
    for scenario in scenarios:
        for framework in sorted(df_all["framework"].unique()):
            sub = df_all[(df_all["scenario"] == scenario) & (df_all["framework"] == framework)]
            if sub.empty:
                continue
            for metric in metrics:
                if metric not in sub.columns:
                    continue
                vals = sub[metric].astype(float)
                rows.append({
                    "Skenario": scenario,
                    "Framework": framework,
                    "Metrik": metric,
                    "n": len(vals),
                    "Mean": round(vals.mean(), 3),
                    "Std Dev": round(vals.std(), 3),
                    "Median": round(vals.median(), 3),
                    "Min": round(vals.min(), 3),
                    "Max": round(vals.max(), 3),
                    "CV (%)": round((vals.std() / vals.mean() * 100), 2) if vals.mean() != 0 else None,
                })
    return pd.DataFrame(rows)


# =========================================================================
# GRAFIK (tidak berubah - bersifat deskriptif, tidak tergantung jenis uji)
# =========================================================================
def build_charts(df_all, metrics=METRICS, scenarios=SCENARIOS, output_dir=OUTPUT_CHART_DIR):
    if not HAS_MATPLOTLIB:
        print("[PERINGATAN] matplotlib tidak terinstal, grafik dilewati.")
        print("             Install dengan: pip install matplotlib")
        return

    os.makedirs(output_dir, exist_ok=True)
    scenario_labels = [SCENARIO_LABELS.get(s, s) for s in scenarios]
    frameworks = sorted(df_all["framework"].unique())
    colors = {"flutter": "#2196F3", "kotlin": "#FF9800"}

    for metric in metrics:
        if metric not in df_all.columns:
            continue
        label = METRIC_LABELS.get(metric, metric)

        fig, ax = plt.subplots(figsize=(7, 4.5))
        x = np.arange(len(scenarios))
        n_fw = len(frameworks)
        width = 0.7 / n_fw

        for i, fw in enumerate(frameworks):
            means, stds = [], []
            for s in scenarios:
                sub = df_all[(df_all.scenario == s) & (df_all.framework == fw)][metric]
                means.append(sub.mean() if len(sub) else np.nan)
                stds.append(sub.std() if len(sub) else np.nan)
            offset = (i - (n_fw - 1) / 2) * width
            ax.bar(x + offset, means, width, yerr=stds, capsize=4,
                   label=fw.capitalize(), color=colors.get(fw, None))

        ax.set_xticks(x)
        ax.set_xticklabels(scenario_labels)
        ax.set_ylabel(label)
        ax.set_title(f"Perbandingan {label} antar Framework")
        ax.legend()
        ax.grid(axis="y", linestyle="--", alpha=0.4)
        plt.tight_layout()

        fname = os.path.join(output_dir, f"{metric}.png")
        plt.savefig(fname, dpi=150)
        plt.close()
        print(f"  Grafik disimpan: {fname}")


def main():
    if len(sys.argv) >= 3:
        path_flutter = sys.argv[1]
        path_kotlin = sys.argv[2]
    else:
        path_flutter = FILE_FLUTTER
        path_kotlin = FILE_KOTLIN

    print(f"Membaca file Flutter : {path_flutter}")
    print(f"Membaca file Kotlin  : {path_kotlin}")
    print()

    try:
        df_flutter = pd.read_csv(path_flutter)
        df_kotlin = pd.read_csv(path_kotlin)
    except FileNotFoundError as e:
        print(f"[ERROR] File tidak ditemukan: {e}")
        print("Jalankan dengan argumen:")
        print("    python analisis_statistik.py path/ke/flutter.csv path/ke/kotlin.csv")
        sys.exit(1)

    # File gabungan (kolom framework berisi flutter & kotlin sekaligus) -> pisah otomatis
    if "framework" in df_flutter.columns and df_flutter["framework"].nunique() > 1:
        combined = df_flutter
        df_flutter = combined[combined["framework"].str.lower() == "flutter"]
        df_kotlin = combined[combined["framework"].str.lower() == "kotlin"]
        print("[INFO] Terdeteksi file gabungan, dipisah otomatis berdasarkan kolom 'framework'.\n")

    df_all = pd.concat([df_flutter, df_kotlin], ignore_index=True)

    pd.set_option("display.width", 240)
    pd.set_option("display.max_columns", None)

    # 1. Statistik deskriptif
    print("=" * 110)
    print("STATISTIK DESKRIPTIF (mean, std, median, min, max, CV)")
    print("=" * 110)
    deskriptif = build_descriptive_table(df_all)
    if deskriptif.empty:
        print("[ERROR] Statistik deskriptif kosong. Cek kolom 'scenario'/'framework'.")
    else:
        print(deskriptif.to_string(index=False))
        deskriptif.to_excel(OUTPUT_EXCEL_DESKRIPTIF, index=False)
        print(f"\nDisimpan ke: {OUTPUT_EXCEL_DESKRIPTIF}")
    print()

    # 2. Uji statistik (Shapiro per grup -> Levene -> t-test/Welch/Mann-Whitney -> Cohen's d)
    hasil = run_pipeline(df_flutter, df_kotlin)
    if hasil.empty:
        print("[ERROR] Tidak ada hasil uji. Cek kolom 'scenario' dan isi SCENARIOS.")
        sys.exit(1)

    print("=" * 110)
    print("HASIL UJI STATISTIK (INDEPENDENT SAMPLES)")
    print("Shapiro-Wilk per grup -> Levene -> Independent t-test / Welch / Mann-Whitney U -> Cohen's d")
    print("=" * 110)
    print(hasil.to_string(index=False))
    print()
    hasil.to_excel(OUTPUT_EXCEL_UJI, index=False)
    print(f"Disimpan ke: {OUTPUT_EXCEL_UJI}")

    # 3. Grafik
    print()
    print("=" * 110)
    print("MEMBUAT GRAFIK")
    print("=" * 110)
    build_charts(df_all)

    print()
    print("Selesai. Semua file siap dipakai untuk Bab IV.")


if __name__ == "__main__":
    main()
