"""
Script Analisis Statistik - Perbandingan Kinerja Flutter vs Android Native Kotlin
====================================================================================
Metodologi: Dua sampel INDEPENDEN (Flutter dan Kotlin dijalankan terpisah)

Cara pakai:
    python analisis_statistik.py file_flutter.csv file_kotlin.csv

Output:
    - Tabel ke terminal
    - hasil_uji_statistik.xlsx   — alur: Shapiro → Levene → t/Welch/Mann-Whitney → Cohen's d
    - statistik_deskriptif.xlsx  — mean, std, median, min, max, CV
    - charts/                    — bar chart PNG per metrik

Alur uji (Bab III):
    1. Shapiro-Wilk pada MASING-MASING kelompok (Flutter & Kotlin)
       → tentukan apakah kedua kelompok berdistribusi normal
    2a. Jika KEDUANYA normal → Uji Levene (homogenitas varians)
          - Levene p > 0,05  → Uji t dua sampel independen (equal variances)
          - Levene p ≤ 0,05  → Uji t Welch (unequal variances)
    2b. Jika SALAH SATU atau KEDUANYA tidak normal → Mann-Whitney U
    3.  Cohen's d berbasis pooled standard deviation
"""

import sys, os
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

# ── Konfigurasi ───────────────────────────────────────────────────────────────
FILE_FLUTTER = "benchmark_flutter.csv"
FILE_KOTLIN  = "benchmark_kotlin.csv"
ALPHA        = 0.05
OUTPUT_EXCEL_UJI        = "hasil_uji_statistik.xlsx"
OUTPUT_EXCEL_DESKRIPTIF = "statistik_deskriptif.xlsx"
OUTPUT_CHART_DIR        = "charts"

METRICS   = ["execution_time_ms", "cpu_percent", "memory_mb"]
METRIC_LABELS = {
    "execution_time_ms": "Waktu Eksekusi (ms)",
    "cpu_percent":       "Penggunaan CPU (%)",
    "memory_mb":         "Penggunaan Memori (MB)",
}
SCENARIOS = ["http", "rendering", "sqlite"]
SCENARIO_LABELS = {
    "http":      "HTTP Request",
    "rendering": "Rendering List",
    "sqlite":    "SQLite CRUD",
}

# ── Effect size: Cohen's d (pooled SD) ───────────────────────────────────────
def cohens_d_pooled(x, y):
    """Cohen's d untuk dua sampel independen, berbasis pooled standard deviation."""
    n1, n2 = len(x), len(y)
    s1, s2 = np.std(x, ddof=1), np.std(y, ddof=1)
    sp = np.sqrt(((n1 - 1) * s1**2 + (n2 - 1) * s2**2) / (n1 + n2 - 2))
    return (np.mean(x) - np.mean(y)) / sp if sp > 0 else 0.0

def interpret_effect(d):
    a = abs(d)
    if a < 0.2:  return "negligible"
    if a < 0.5:  return "small"
    if a < 0.8:  return "medium"
    return "large"

# ── Pipeline uji statistik ───────────────────────────────────────────────────
def run_pipeline(df_flutter, df_kotlin,
                 metrics=METRICS, scenarios=SCENARIOS, alpha=ALPHA):
    results = []
    for scenario in scenarios:
        flu_s = df_flutter[df_flutter["scenario"] == scenario].reset_index(drop=True)
        kot_s = df_kotlin [df_kotlin ["scenario"] == scenario].reset_index(drop=True)
        if len(flu_s) == 0 or len(kot_s) == 0:
            print(f"[PERINGATAN] Skenario '{scenario}' tidak ditemukan, dilewati.")
            continue

        for metric in metrics:
            if metric not in flu_s.columns:
                continue
            x = flu_s[metric].astype(float).dropna().values
            y = kot_s[metric].astype(float).dropna().values

            # ── Tahap 1: Shapiro-Wilk masing-masing kelompok ──
            _, p_sh_x = stats.shapiro(x) if len(x) >= 3 else (None, np.nan)
            _, p_sh_y = stats.shapiro(y) if len(y) >= 3 else (None, np.nan)
            both_normal = (p_sh_x > alpha) and (p_sh_y > alpha)

            # ── Tahap 2: pilih uji hipotesis ──
            if both_normal:
                _, p_lev = stats.levene(x, y)
                if p_lev > alpha:
                    test_name = "Uji t independen"
                    _, p_val  = stats.ttest_ind(x, y, equal_var=True)
                else:
                    test_name = "Uji t Welch"
                    _, p_val  = stats.ttest_ind(x, y, equal_var=False)
                    p_lev = p_lev   # same value, just documenting
            else:
                p_lev     = stats.levene(x, y)[1]  # still compute for info
                test_name = "Mann-Whitney U"
                _, p_val  = stats.mannwhitneyu(x, y, alternative="two-sided")

            sig = "Ya" if (pd.notna(p_val) and p_val < alpha) else "Tidak"

            # ── Tahap 3: Cohen's d (pooled) ──
            d      = cohens_d_pooled(x, y)
            effect = interpret_effect(d)

            results.append({
                "Skenario":             scenario,
                "Metrik":               metric,
                "n Flutter":            len(x),
                "n Kotlin":             len(y),
                "Mean Flutter":         round(float(np.mean(x)), 3),
                "Mean Kotlin":          round(float(np.mean(y)), 3),
                "Shapiro p (Flutter)":  round(float(p_sh_x), 4) if not np.isnan(p_sh_x) else None,
                "Shapiro p (Kotlin)":   round(float(p_sh_y), 4) if not np.isnan(p_sh_y) else None,
                "Kedua Normal?":        "Ya" if both_normal else "Tidak",
                "Levene p":             round(float(p_lev), 4),
                "Uji yang Dipakai":     test_name,
                "p-value":              round(float(p_val), 4) if pd.notna(p_val) else None,
                "Signifikan?":          sig,
                "Cohen's d (pooled)":   round(d, 3),
                "Effect Size":          effect,
            })
    return pd.DataFrame(results)

# ── Statistik deskriptif ─────────────────────────────────────────────────────
def build_descriptive(df_all, metrics=METRICS, scenarios=SCENARIOS):
    rows = []
    for s in scenarios:
        for fw in sorted(df_all["framework"].unique()):
            sub = df_all[(df_all["scenario"] == s) & (df_all["framework"] == fw)]
            if sub.empty:
                continue
            for m in metrics:
                if m not in sub.columns:
                    continue
                v = sub[m].astype(float)
                rows.append({
                    "Skenario": s, "Framework": fw, "Metrik": m,
                    "n":     len(v),
                    "Mean":  round(v.mean(), 3),
                    "Std":   round(v.std(ddof=1), 3),
                    "Median":round(v.median(), 3),
                    "Min":   round(v.min(), 3),
                    "Max":   round(v.max(), 3),
                    "CV (%)":round(v.std(ddof=1)/v.mean()*100, 2) if v.mean()!=0 else None,
                })
    return pd.DataFrame(rows)

# ── Grafik ───────────────────────────────────────────────────────────────────
def build_charts(df_all, metrics=METRICS, scenarios=SCENARIOS,
                 output_dir=OUTPUT_CHART_DIR):
    if not HAS_MATPLOTLIB:
        print("[PERINGATAN] matplotlib tidak tersedia, grafik dilewati.")
        return
    os.makedirs(output_dir, exist_ok=True)
    sc_labels = [SCENARIO_LABELS.get(s, s) for s in scenarios]
    frameworks = sorted(df_all["framework"].unique())
    colors = {"flutter": "#2196F3", "kotlin": "#FF9800"}

    for metric in metrics:
        if metric not in df_all.columns:
            continue
        label = METRIC_LABELS.get(metric, metric)
        fig, ax = plt.subplots(figsize=(7, 4.5))
        x = np.arange(len(scenarios))
        w = 0.7 / len(frameworks)
        for i, fw in enumerate(frameworks):
            means, stds = [], []
            for s in scenarios:
                sub = df_all[(df_all.scenario==s) & (df_all.framework==fw)][metric]
                means.append(sub.mean() if len(sub) else np.nan)
                stds.append(sub.std(ddof=1) if len(sub) else np.nan)
            offset = (i - (len(frameworks)-1)/2) * w
            ax.bar(x+offset, means, w, yerr=stds, capsize=4,
                   label=fw.capitalize(), color=colors.get(fw))
        ax.set_xticks(x); ax.set_xticklabels(sc_labels)
        ax.set_ylabel(label)
        ax.set_title(f"Perbandingan {label} antar Framework")
        ax.legend(); ax.grid(axis="y", linestyle="--", alpha=0.4)
        plt.tight_layout()
        fname = os.path.join(output_dir, f"{metric}.png")
        plt.savefig(fname, dpi=150); plt.close()
        print(f"  Grafik disimpan: {fname}")

# ── Main ─────────────────────────────────────────────────────────────────────
def main():
    if len(sys.argv) >= 3:
        path_flu, path_kot = sys.argv[1], sys.argv[2]
    else:
        path_flu, path_kot = FILE_FLUTTER, FILE_KOTLIN

    print(f"Flutter : {path_flu}\nKotlin  : {path_kot}\n")
    try:
        df_flu = pd.read_csv(path_flu)
        df_kot = pd.read_csv(path_kot)
    except FileNotFoundError as e:
        print(f"[ERROR] {e}"); sys.exit(1)

    # Auto-split jika file gabungan
    if "framework" in df_flu.columns and df_flu["framework"].nunique() > 1:
        combined = df_flu
        df_flu = combined[combined["framework"].str.lower() == "flutter"]
        df_kot = combined[combined["framework"].str.lower() == "kotlin"]
        print("[INFO] File gabungan terdeteksi, otomatis dipisah.\n")

    df_all = pd.concat([df_flu, df_kot], ignore_index=True)
    pd.set_option("display.width", 220); pd.set_option("display.max_columns", None)

    # 1. Deskriptif
    print("="*110 + "\nSTATISTIK DESKRIPTIF\n" + "="*110)
    desc = build_descriptive(df_all)
    print(desc.to_string(index=False))
    desc.to_excel(OUTPUT_EXCEL_DESKRIPTIF, index=False)
    print(f"\nDisimpan: {OUTPUT_EXCEL_DESKRIPTIF}\n")

    # 2. Uji inferensial
    print("="*110 + "\nHASIL UJI STATISTIK (Shapiro → Levene → t/Welch/MWU → Cohen's d)\n" + "="*110)
    hasil = run_pipeline(df_flu, df_kot)
    print(hasil.to_string(index=False))
    hasil.to_excel(OUTPUT_EXCEL_UJI, index=False)
    print(f"\nDisimpan: {OUTPUT_EXCEL_UJI}\n")

    # 3. Grafik
    print("="*110 + "\nMEMBUAT GRAFIK\n" + "="*110)
    build_charts(df_all)
    print("\nSelesai. Semua file siap untuk Bab IV.")

if __name__ == "__main__":
    main()