"""
Script Analisis Statistik - Perbandingan Kinerja Flutter vs Android Native Kotlin
====================================================================================

Cara pakai:
    python analisis_statistik.py file_flutter.csv file_kotlin.csv

Atau edit langsung bagian FILE_FLUTTER dan FILE_KOTLIN di bawah, lalu jalankan:
    python analisis_statistik.py

Output:
    - Tabel ringkasan ke terminal
    - hasil_uji_statistik.xlsx      -> tabel uji statistik (Shapiro/Wilcoxon/t-test/Cohen's d)
    - statistik_deskriptif.xlsx     -> tabel mean, std, median, min, max per skenario & framework
    - charts/execution_time_ms.png  -> grafik perbandingan waktu eksekusi
    - charts/cpu_percent.png        -> grafik perbandingan penggunaan CPU
    - charts/memory_mb.png          -> grafik perbandingan penggunaan memori

Tahapan uji (sesuai metodologi Bab III):
    1. Uji normalitas Shapiro-Wilk pada SELISIH berpasangan (Flutter - Kotlin)
       -> menentukan uji mana yang valid dipakai di langkah 2
    2. Jika selisih berdistribusi normal (p > 0.05)  -> Paired t-test
       Jika selisih TIDAK berdistribusi normal (p <= 0.05) -> Wilcoxon signed-rank test
       -> menjawab apakah perbedaan Flutter vs Kotlin signifikan secara statistik
    3. Cohen's d (effect size berpasangan)
       -> menjawab seberapa besar perbedaan tersebut secara praktis
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
# KONFIGURASI - edit bagian ini jika tidak memakai argumen command line
# =========================================================================
FILE_FLUTTER = "benchmark_flutter.csv"   # path ke CSV hasil benchmark Flutter
FILE_KOTLIN = "benchmark_kotlin.csv"     # path ke CSV hasil benchmark Kotlin
ALPHA = 0.05                              # tingkat signifikansi
OUTPUT_EXCEL_UJI = "hasil_uji_statistik.xlsx"
OUTPUT_EXCEL_DESKRIPTIF = "statistik_deskriptif.xlsx"
OUTPUT_CHART_DIR = "charts"

METRICS = ["execution_time_ms", "cpu_percent", "memory_mb"]
METRIC_LABELS = {
    "execution_time_ms": "Waktu Eksekusi (ms)",
    "cpu_percent": "Penggunaan CPU (%)",
    "memory_mb": "Penggunaan Memori (MB)",
}
SCENARIOS = ["http", "rendering", "sqlite"]  # sesuaikan dengan nama di kolom 'scenario'
SCENARIO_LABELS = {
    "http": "HTTP Request",
    "rendering": "Rendering List",
    "sqlite": "SQLite CRUD",
}


# =========================================================================
# FUNGSI INTI - UJI STATISTIK
# =========================================================================
def cohens_d_paired(x, y):
    """Cohen's d untuk paired/dependent samples, berbasis selisih (x - y)."""
    diff = np.asarray(x) - np.asarray(y)
    sd = diff.std(ddof=1)
    if sd == 0:
        return 0.0
    return diff.mean() / sd


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


def run_pipeline(df_flutter, df_kotlin, metrics=METRICS, scenarios=SCENARIOS, alpha=ALPHA):
    """
    Menjalankan pipeline 3 tahap untuk setiap kombinasi skenario x metrik.
    Mengasumsikan data berpasangan berdasarkan urutan 'run' di tiap skenario
    (run ke-1 Flutter dibandingkan run ke-1 Kotlin, dst).
    """
    results = []

    for scenario in scenarios:
        flu_s = df_flutter[df_flutter["scenario"] == scenario].sort_values("run").reset_index(drop=True)
        kot_s = df_kotlin[df_kotlin["scenario"] == scenario].sort_values("run").reset_index(drop=True)

        n = min(len(flu_s), len(kot_s))
        if n == 0:
            print(f"[PERINGATAN] Skenario '{scenario}' tidak ditemukan di salah satu file, dilewati.")
            continue
        if len(flu_s) != len(kot_s):
            print(f"[PERINGATAN] Skenario '{scenario}': jumlah run Flutter ({len(flu_s)}) "
                  f"!= Kotlin ({len(kot_s)}). Memakai {n} run pertama dari masing-masing.")

        flu_s = flu_s.iloc[:n]
        kot_s = kot_s.iloc[:n]

        for metric in metrics:
            if metric not in flu_s.columns or metric not in kot_s.columns:
                print(f"[PERINGATAN] Kolom '{metric}' tidak ditemukan, dilewati.")
                continue

            x = flu_s[metric].astype(float).reset_index(drop=True)
            y = kot_s[metric].astype(float).reset_index(drop=True)
            diff = x - y

            # --- Tahap 1: Shapiro-Wilk pada selisih berpasangan ---
            if n < 3:
                sh_p = np.nan
                normal = False
            else:
                try:
                    _, sh_p = stats.shapiro(diff)
                    normal = sh_p > alpha
                except Exception:
                    sh_p = np.nan
                    normal = False

            # --- Tahap 2: pilih uji sesuai hasil normalitas ---
            if normal:
                test_name = "Paired t-test"
                try:
                    _, p_val = stats.ttest_rel(x, y)
                except Exception:
                    p_val = np.nan
            else:
                test_name = "Wilcoxon signed-rank"
                try:
                    _, p_val = stats.wilcoxon(x, y)
                except Exception:
                    p_val = np.nan

            sig = "Signifikan" if (pd.notna(p_val) and p_val < alpha) else "Tidak signifikan"

            # --- Tahap 3: Cohen's d ---
            d = cohens_d_paired(x, y)
            effect = interpret_effect_size(d)

            results.append({
                "Skenario": scenario,
                "Metrik": metric,
                "n": n,
                "Mean Flutter": round(x.mean(), 3),
                "Mean Kotlin": round(y.mean(), 3),
                "Shapiro p (selisih)": round(sh_p, 4) if pd.notna(sh_p) else None,
                "Distribusi Selisih": "Normal" if normal else "Tidak normal",
                "Uji yang Dipakai": test_name,
                "p-value": round(p_val, 4) if pd.notna(p_val) else None,
                "Hasil": sig,
                "Cohen's d": round(d, 4),
                "Effect Size": effect,
            })

    return pd.DataFrame(results)


# =========================================================================
# FUNGSI INTI - STATISTIK DESKRIPTIF
# =========================================================================
def build_descriptive_table(df_all, metrics=METRICS, scenarios=SCENARIOS):
    """
    Membangun tabel statistik deskriptif (mean, std, median, min, max)
    per kombinasi skenario x framework x metrik, dalam format rapi (long format)
    yang mudah dibaca/disalin ke skripsi.
    """
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
# FUNGSI INTI - GRAFIK
# =========================================================================
def build_charts(df_all, metrics=METRICS, scenarios=SCENARIOS, output_dir=OUTPUT_CHART_DIR):
    """
    Membuat grafik bar chart perbandingan Flutter vs Kotlin (dengan error bar
    standar deviasi) untuk setiap metrik, satu file PNG per metrik.
    """
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
    # Ambil path file dari argumen command line jika ada
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
        print("Pastikan path file benar, atau jalankan dengan argumen:")
        print("    python analisis_statistik.py path/ke/flutter.csv path/ke/kotlin.csv")
        sys.exit(1)

    # Jika file gabungan (kolom 'framework' berisi flutter & kotlin sekaligus),
    # pisahkan otomatis
    if "framework" in df_flutter.columns and df_flutter["framework"].nunique() > 1:
        combined = df_flutter
        df_flutter = combined[combined["framework"].str.lower() == "flutter"]
        df_kotlin = combined[combined["framework"].str.lower() == "kotlin"]
        print("[INFO] Terdeteksi file gabungan, otomatis dipisah berdasarkan kolom 'framework'.\n")

    df_all = pd.concat([df_flutter, df_kotlin], ignore_index=True)

    pd.set_option("display.width", 200)
    pd.set_option("display.max_columns", None)

    # ---------------------------------------------------------------
    # 1. Statistik deskriptif
    # ---------------------------------------------------------------
    print("=" * 100)
    print("STATISTIK DESKRIPTIF (mean, std, median, min, max, CV)")
    print("=" * 100)
    deskriptif = build_descriptive_table(df_all)
    if deskriptif.empty:
        print("[ERROR] Statistik deskriptif kosong. Cek nama kolom 'scenario'/'framework'.")
    else:
        print(deskriptif.to_string(index=False))
        deskriptif.to_excel(OUTPUT_EXCEL_DESKRIPTIF, index=False)
        print(f"\nTabel statistik deskriptif disimpan ke: {OUTPUT_EXCEL_DESKRIPTIF}")
    print()

    # ---------------------------------------------------------------
    # 2. Uji statistik (Shapiro -> t-test/Wilcoxon -> Cohen's d)
    # ---------------------------------------------------------------
    hasil = run_pipeline(df_flutter, df_kotlin)

    if hasil.empty:
        print("[ERROR] Tidak ada hasil uji yang bisa dihitung. Cek nama kolom 'scenario' dan isi SCENARIOS di script.")
        sys.exit(1)

    print("=" * 100)
    print("HASIL UJI STATISTIK (Shapiro-Wilk -> Paired t-test/Wilcoxon -> Cohen's d)")
    print("=" * 100)
    print(hasil.to_string(index=False))
    print()

    hasil.to_excel(OUTPUT_EXCEL_UJI, index=False)
    print(f"Tabel hasil uji statistik disimpan ke: {OUTPUT_EXCEL_UJI}")

    # ---------------------------------------------------------------
    # 3. Grafik perbandingan
    # ---------------------------------------------------------------
    print()
    print("=" * 100)
    print("MEMBUAT GRAFIK")
    print("=" * 100)
    build_charts(df_all)

    print()
    print("Semua file siap dipakai/disalin ke Bab IV skripsi.")


if __name__ == "__main__":
    main()