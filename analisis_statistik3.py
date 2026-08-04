"""
Script Analisis Statistik - Perbandingan Kinerja Flutter vs Android Native Kotlin
====================================================================================
Metodologi: Dua sampel INDEPENDEN (Flutter dan Kotlin dijalankan terpisah)

Cara pakai:
    python analisis_statistik3.py "Hasil Benchmark flutter.csv" "Hasil Benchmark Kotlin.csv"

Output:
    - Tabel ke terminal
    - hasil_uji_statistik.xlsx   — alur: Shapiro-Wilk -> Welch's t-test / Mann-Whitney U -> Effect size
    - statistik_deskriptif.xlsx  — mean, std, median, min, max, CV
    - charts/                    — bar chart PNG per metrik

Alur uji :
    1. Shapiro-Wilk pada MASING-MASING kelompok (Flutter & Kotlin)
       -> tentukan apakah kedua kelompok berdistribusi normal
    2a. Jika KEDUANYA normal -> Welch's t-test (langsung, tanpa uji Levene)
          Welch's t-test dipilih sebagai default karena tidak mengasumsikan
          kesamaan varians antar kelompok dan tetap valid baik varians sama
          maupun tidak (Delacre et al., 2017).
    2b. Jika SALAH SATU atau KEDUANYA tidak normal -> Mann-Whitney U
    3.  Effect size disesuaikan dengan jenis uji yang dipakai:
          - Welch's t-test      -> Hedges' g* (non-pooled standard deviation)
          - Mann-Whitney U      -> Rank-biserial correlation (r)
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

# ── Effect size 1: Hedges' g* (non-pooled SD) — untuk Welch's t-test ─────────
def hedges_g_star(x, y):
    """
    Hedges' g* berbasis non-pooled standard deviation.
    Digunakan bersama Welch's t-test karena tidak mengasumsikan
    kesamaan varians antar kelompok (Delacre et al., 2017).

        g* = (mean1 - mean2) / s*
        s* = sqrt( (s1^2 + s2^2) / 2 )
    """
    s1, s2 = np.std(x, ddof=1), np.std(y, ddof=1)
    s_star = np.sqrt((s1**2 + s2**2) / 2)
    return (np.mean(x) - np.mean(y)) / s_star if s_star > 0 else 0.0

def interpret_hedges_g(g):
    """Interpretasi mengikuti pedoman Cohen (1988): kecil=0.2, sedang=0.5, besar=0.8."""
    a = abs(g)
    if a < 0.2:  return "negligible"
    if a < 0.5:  return "small"
    if a < 0.8:  return "medium"
    return "large"

# ── Effect size 2: Rank-biserial correlation — untuk Mann-Whitney U ─────────
def rank_biserial_from_u(u_stat, n1, n2):
    """
    Rank-biserial correlation (r) dihitung dari statistik U Mann-Whitney.

        r = 1 - (2U) / (n1 * n2)

    Nilai r berkisar -1 sampai 1. Tanda menunjukkan arah perbedaan
    (mengikuti urutan kelompok x lalu y yang dikirim ke mannwhitneyu).
    """
    return 1 - (2 * u_stat) / (n1 * n2)

def interpret_rank_biserial(r):
    """Interpretasi rank-biserial correlation: kecil=0.1, sedang=0.3, besar=0.5."""
    a = abs(r)
    if a < 0.1:  return "negligible"
    if a < 0.3:  return "small"
    if a < 0.5:  return "medium"
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
            n1, n2 = len(x), len(y)

            # ── Tahap 1: Shapiro-Wilk masing-masing kelompok ──
            _, p_sh_x = stats.shapiro(x) if n1 >= 3 else (None, np.nan)
            _, p_sh_y = stats.shapiro(y) if n2 >= 3 else (None, np.nan)
            both_normal = (p_sh_x > alpha) and (p_sh_y > alpha)

            # ── Tahap 2: pilih uji hipotesis (TANPA Levene) ──
            if both_normal:
                test_name = "Welch's t-test"
                _, p_val  = stats.ttest_ind(x, y, equal_var=False)

                # ── Tahap 3a: Effect size Hedges' g* ──
                effect_value = hedges_g_star(x, y)
                effect_label = "Hedges' g*"
                effect_size  = interpret_hedges_g(effect_value)

            else:
                test_name = "Mann-Whitney U"
                u_stat, p_val = stats.mannwhitneyu(x, y, alternative="two-sided")

                # ── Tahap 3b: Effect size Rank-biserial correlation ──
                effect_value = rank_biserial_from_u(u_stat, n1, n2)
                effect_label = "Rank-biserial r"
                effect_size  = interpret_rank_biserial(effect_value)

            sig = "Ya" if (pd.notna(p_val) and p_val < alpha) else "Tidak"

            results.append({
                "Skenario":             scenario,
                "Metrik":               metric,
                "n Flutter":            n1,
                "n Kotlin":             n2,
                "Mean Flutter":         round(float(np.mean(x)), 3),
                "Mean Kotlin":          round(float(np.mean(y)), 3),
                "Shapiro p (Flutter)":  round(float(p_sh_x), 4) if not np.isnan(p_sh_x) else None,
                "Shapiro p (Kotlin)":   round(float(p_sh_y), 4) if not np.isnan(p_sh_y) else None,
                "Kedua Normal?":        "Ya" if both_normal else "Tidak",
                "Uji yang Dipakai":     test_name,
                "p-value":              round(float(p_val), 4) if pd.notna(p_val) else None,
                "Signifikan?":          sig,
                "Effect Size (jenis)":  effect_label,
                "Effect Size (nilai)":  round(float(effect_value), 3),
                "Interpretasi Efek":    effect_size,
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
    print("="*110 + "\nHASIL UJI STATISTIK (Shapiro-Wilk -> Welch's t-test / Mann-Whitney U -> Effect Size)\n" + "="*110)
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