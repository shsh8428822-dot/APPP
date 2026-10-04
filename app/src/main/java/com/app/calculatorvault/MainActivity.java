package com.app.calculatorvault;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "vault";
    private static final String KEY_HIDDEN = "hidden_packages";
    private static final String ME = "com.app.calculatorvault";
    private static final String UNIVERSAL_SECRET = "34613";
    private static final String LEGACY_SECRET = "333=333";
    private static final String KEY_SECRET = "secret_code";
    private static final String DEFAULT_SECRET = "33333";
    private static final String KEY_BACKGROUND = "background_style";
    private static final String KEY_LAYOUT = "app_layout";
    private static final String KEY_SHOW_LABELS = "show_app_labels";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_SORT = "app_sort";

    private static final int WALLPAPER_COUNT = 1000;
    private static final String[] WALLPAPER_FAMILIES = {
            "פסטל", "אוקיינוס", "שקיעה", "אורורה", "לילה",
            "ניאון", "זהב", "סגול", "טורקיז", "יער",
            "ורוד", "תכלת", "גרפיט", "שמנת", "קרחון",
            "אש", "לבנדר", "אמרלד", "כחול עמוק", "כסף"
    };

    private static final int BG = Color.rgb(245, 247, 250);
    private static final int SURFACE = Color.WHITE;
    private static final int SURFACE_ALT = Color.rgb(238, 242, 246);
    private static final int ACCENT = Color.rgb(36, 87, 214);
    private static final int ACCENT_DARK = Color.rgb(23, 62, 157);
    private static final int ACCENT_SOFT = Color.rgb(232, 238, 255);
    private static final int TEXT = Color.rgb(23, 32, 42);
    private static final int MUTED = Color.rgb(103, 115, 129);
    private static final int DANGER = Color.rgb(198, 59, 59);

    private final DecimalFormat fmt =
            new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.US));

    private TextView expressionView;
    private TextView resultView;

    private String input = "";
    private double stored = 0;
    private String op = null;
    private String expression = "";
    private boolean fresh = true;
    private boolean showingResult = false;
    private String secretBuffer = "";

    private Page page = Page.CALCULATOR;
    private Page settingsReturnPage = Page.PRIVATE;

    private enum Page {
        CALCULATOR, PRIVATE, MANAGE, SETTINGS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBars();
        showCalculator();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showCalculator();
    }

    private void setBars() {
        int bg = backgroundColor();
        boolean dark = isDarkColor(bg);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(dark ? Color.rgb(18, 22, 32) : SURFACE);
        int flags = 0;
        if (!dark && Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!dark && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView label(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView heading(String value) {
        TextView view = label(value, 24, TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button actionButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(ACCENT_DARK);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(SURFACE, 16));
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private TextView key(String value, int background, int foreground, float size) {
        TextView button = label(value, size, foreground);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(background, 18));
        button.setElevation(dp(2));
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(value);
        return button;
    }

    private int backgroundStyle() {
        int style = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_BACKGROUND, 0);
        return Math.max(0, Math.min(style, WALLPAPER_COUNT - 1));
    }

    private String wallpaperName(int index) {
        int safe = Math.max(0, Math.min(index, WALLPAPER_COUNT - 1));
        int family = safe / 50;
        int variant = safe % 50;
        return WALLPAPER_FAMILIES[family] + " • סגנון " + (variant + 1);
    }

    private float hueWrap(float value) {
        float h = value % 360f;
        return h < 0 ? h + 360f : h;
    }

    private int[] wallpaperColors(int index) {
        int safe = Math.max(0, Math.min(index, WALLPAPER_COUNT - 1));
        int family = safe / 50;
        int variant = safe % 50;
        float baseHue = hueWrap(family * 18f + variant * 7.2f);
        float wobble = (variant % 5) * 0.035f;
        float sat;
        float val;
        switch (family) {
            case 0: sat = 0.18f + wobble; val = 0.98f; break;
            case 1: sat = 0.58f + wobble; val = 0.96f; break;
            case 2: sat = 0.66f + wobble; val = 0.98f; break;
            case 3: sat = 0.55f + wobble; val = 0.97f; break;
            case 4: sat = 0.72f + wobble; val = 0.58f; break;
            case 5: sat = 0.90f + wobble; val = 0.98f; break;
            case 6: sat = 0.64f + wobble; val = 0.95f; break;
            case 7: sat = 0.60f + wobble; val = 0.92f; break;
            case 8: sat = 0.62f + wobble; val = 0.92f; break;
            case 9: sat = 0.55f + wobble; val = 0.82f; break;
            case 10: sat = 0.48f + wobble; val = 0.98f; break;
            case 11: sat = 0.38f + wobble; val = 0.99f; break;
            case 12: sat = 0.20f + wobble; val = 0.34f; break;
            case 13: sat = 0.24f + wobble; val = 0.99f; break;
            case 14: sat = 0.28f + wobble; val = 0.96f; break;
            case 15: sat = 0.86f + wobble; val = 0.98f; break;
            case 16: sat = 0.40f + wobble; val = 0.95f; break;
            case 17: sat = 0.62f + wobble; val = 0.90f; break;
            case 18: sat = 0.82f + wobble; val = 0.56f; break;
            default: sat = 0.12f + wobble; val = 0.92f; break;
        }
        sat = Math.min(0.98f, sat);
        int c1 = Color.HSVToColor(new float[]{baseHue, sat, val});
        int c2 = Color.HSVToColor(new float[]{hueWrap(baseHue + 24f + (variant % 7) * 3f), Math.min(0.98f, sat * 0.92f), Math.min(1f, val * 0.88f + 0.08f)});
        int c3 = Color.HSVToColor(new float[]{hueWrap(baseHue + 210f), Math.min(0.98f, sat * 0.76f), Math.min(1f, val * 0.72f + 0.18f)});
        if (family == 12 || family == 18) {
            c1 = Color.HSVToColor(new float[]{baseHue, Math.min(0.9f, sat + 0.08f), Math.min(0.68f, val + 0.08f)});
            c2 = Color.HSVToColor(new float[]{hueWrap(baseHue + 24f), Math.min(0.75f, sat + 0.02f), Math.min(0.52f, val + 0.10f)});
            c3 = Color.HSVToColor(new float[]{hueWrap(baseHue + 48f), Math.min(0.72f, sat), Math.min(0.40f, val + 0.04f)});
        }
        return new int[]{c1, c2, c3};
    }

    private boolean isDarkColor(int color) {
        double luminance = (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)) / 255.0;
        return luminance < 0.52;
    }

    private int backgroundColor() {
        return wallpaperColors(backgroundStyle())[0];
    }

    private GradientDrawable wallpaperDrawableFor(int choice) {
        int safe = Math.max(0, Math.min(choice, WALLPAPER_COUNT - 1));
        int[] colors = wallpaperColors(safe);
        GradientDrawable drawable = new GradientDrawable();
        int variant = safe % 50;
        if (variant % 5 == 1 || variant % 5 == 4) {
            drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            drawable.setGradientCenter(0.22f + (variant % 4) * 0.18f, 0.20f + (variant % 3) * 0.22f);
            drawable.setGradientRadius(dp(520));
        } else if (variant % 5 == 2) {
            drawable.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            drawable.setGradientCenter(0.42f, 0.46f);
        } else {
            drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            GradientDrawable.Orientation[] orientations = {
                    GradientDrawable.Orientation.TL_BR,
                    GradientDrawable.Orientation.TR_BL,
                    GradientDrawable.Orientation.BL_TR,
                    GradientDrawable.Orientation.BR_TL
            };
            drawable.setOrientation(orientations[variant % orientations.length]);
        }
        drawable.setColors(colors);
        drawable.setDither(true);
        return drawable;
    }

    private GradientDrawable backgroundDrawable() {
        return wallpaperDrawableFor(backgroundStyle());
    }

    private void refreshCurrentPage() {
        if (page == Page.SETTINGS) showSettings();
        else if (page == Page.PRIVATE) showPrivateApps();
        else if (page == Page.MANAGE) showManageApps();
        else showCalculator();
    }

    private void showSettings() {
        page = Page.SETTINGS;

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = actionButton("← חזרה");
        back.setOnClickListener(v -> {
            if (settingsReturnPage == Page.CALCULATOR) showCalculator();
            else if (settingsReturnPage == Page.MANAGE) showManageApps();
            else showPrivateApps();
        });

        TextView title = heading("הגדרות");

        header.addView(back, new LinearLayout.LayoutParams(dp(90), dp(46)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        titleLp.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleLp);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(18));
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setFocusable(false);
        content.setFocusableInTouchMode(false);

        content.addView(settingsSection("רקעים למחשבון ולאזור הפרטי — 1,000 אפשרויות"), new LinearLayout.LayoutParams(-1, dp(42)));

        final int currentWallpaper = backgroundStyle();
        Button wallpaperPreview = actionButton(
                "רקע " + (currentWallpaper + 1) + " מתוך " + WALLPAPER_COUNT + "\n" + wallpaperName(currentWallpaper)
        );
        wallpaperPreview.setTextSize(15);
        wallpaperPreview.setTextColor(isDarkColor(backgroundColor()) ? Color.WHITE : TEXT);
        wallpaperPreview.setGravity(Gravity.CENTER);
        wallpaperPreview.setMinHeight(dp(110));
        wallpaperPreview.setAllCaps(false);
        wallpaperPreview.setBackground(wallpaperDrawableFor(currentWallpaper));
        wallpaperPreview.setOnClickListener(v -> {
            int next = (backgroundStyle() + 1) % WALLPAPER_COUNT;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_BACKGROUND, next).apply();
            setBars();
            showSettings();
        });
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(112));
        previewLp.setMargins(0, 0, 0, dp(8));
        content.addView(wallpaperPreview, previewLp);

        LinearLayout wallpaperNav = new LinearLayout(this);
        wallpaperNav.setGravity(Gravity.CENTER_VERTICAL);
        wallpaperNav.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        String[] wallpaperActions = {"◀ הקודם", "🎲 אקראי", "הבא ▶"};
        for (int i = 0; i < wallpaperActions.length; i++) {
            final int action = i;
            Button button = settingChoice(wallpaperActions[i], false);
            button.setOnClickListener(v -> {
                int current = backgroundStyle();
                int next;
                if (action == 0) next = (current - 1 + WALLPAPER_COUNT) % WALLPAPER_COUNT;
                else if (action == 1) next = (current * 73 + 137) % WALLPAPER_COUNT;
                else next = (current + 1) % WALLPAPER_COUNT;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_BACKGROUND, next).apply();
                setBars();
                showSettings();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            wallpaperNav.addView(button, lp);
        }
        content.addView(wallpaperNav, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout jumpRow = new LinearLayout(this);
        jumpRow.setGravity(Gravity.CENTER_VERTICAL);
        jumpRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        EditText wallpaperNumber = new EditText(this);
        wallpaperNumber.setHint("מספר 1–1000");
        wallpaperNumber.setTextSize(15);
        wallpaperNumber.setSingleLine(true);
        wallpaperNumber.setGravity(Gravity.CENTER);
        wallpaperNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        wallpaperNumber.setBackground(rounded(SURFACE, 16));
        Button goWallpaper = actionButton("עבור");
        goWallpaper.setTextColor(Color.WHITE);
        goWallpaper.setBackground(rounded(ACCENT, 16));
        goWallpaper.setOnClickListener(v -> {
            try {
                int requested = Integer.parseInt(wallpaperNumber.getText().toString().trim());
                requested = Math.max(1, Math.min(WALLPAPER_COUNT, requested));
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_BACKGROUND, requested - 1).apply();
                setBars();
                showSettings();
            } catch (Exception ignored) {
                Toast.makeText(this, "הכנס מספר בין 1 ל־1000", Toast.LENGTH_SHORT).show();
            }
        });
        jumpRow.addView(wallpaperNumber, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(dp(82), dp(50));
        goLp.setMargins(dp(6), 0, 0, 0);
        jumpRow.addView(goWallpaper, goLp);
        content.addView(jumpRow, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView wallpaperNote = sectionText("כל 1,000 הרקעים נוצרים בתוך האפליקציה, כך שה־APK לא מתנפח באלפי קבצי תמונה. כל בחירה נשמרת גם למחשבון וגם לאזור הפרטי.");
        wallpaperNote.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        wallpaperNote.setBackground(rounded(ACCENT_SOFT, 14));
        wallpaperNote.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams wallpaperNoteLp = new LinearLayout.LayoutParams(-1, dp(64));
        wallpaperNoteLp.setMargins(0, dp(6), 0, dp(8));
        content.addView(wallpaperNote, wallpaperNoteLp);

        content.addView(settingsSection("תצוגת האפליקציות"), new LinearLayout.LayoutParams(-1, dp(36)));
        LinearLayout layouts = new LinearLayout(this);
        layouts.setGravity(Gravity.CENTER_VERTICAL);
        int layout = displayMode();
        String[] layoutNames = {"רשת 4", "רשת 5", "רשימה"};
        for (int i = 0; i < layoutNames.length; i++) {
            final int choice = i;
            Button button = settingChoice(layoutNames[i], layout == i);
            button.setOnClickListener(v -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_LAYOUT, choice).apply();
                showSettings();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            layouts.addView(button, lp);
        }
        content.addView(layouts, new LinearLayout.LayoutParams(-1, dp(52)));

        content.addView(settingsSection("שמות אייקונים"), new LinearLayout.LayoutParams(-1, dp(34)));
        CheckBox labels = new CheckBox(this);
        labels.setText("הצג את שם האפליקציה מתחת לאייקון");
        labels.setTextSize(15);
        labels.setTextColor(TEXT);
        labels.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        labels.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        labels.setButtonTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{ACCENT, MUTED}
        ));
        labels.setChecked(showAppLabels());
        labels.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_SHOW_LABELS, checked).apply()
        );
        content.addView(labels, new LinearLayout.LayoutParams(-1, dp(48)));

        content.addView(settingsSection("גודל האייקונים"), new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout iconSizes = new LinearLayout(this);
        iconSizes.setGravity(Gravity.CENTER_VERTICAL);
        int iconSize = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_ICON_SIZE, 1);
        String[] iconNames = {"קטן", "בינוני", "גדול"};
        for (int i = 0; i < iconNames.length; i++) {
            final int choice = i;
            Button button = settingChoice(iconNames[i], iconSize == i);
            button.setOnClickListener(v -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_ICON_SIZE, choice).apply();
                showSettings();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            iconSizes.addView(button, lp);
        }
        content.addView(iconSizes, new LinearLayout.LayoutParams(-1, dp(52)));

        content.addView(settingsSection("סדר האפליקציות"), new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout sorts = new LinearLayout(this);
        sorts.setGravity(Gravity.CENTER_VERTICAL);
        int sort = appSort();
        String[] sortNames = {"א–ב", "ב–א"};
        for (int i = 0; i < sortNames.length; i++) {
            final int choice = i;
            Button button = settingChoice(sortNames[i], sort == i);
            button.setOnClickListener(v -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_SORT, choice).apply();
                showSettings();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            sorts.addView(button, lp);
        }
        content.addView(sorts, new LinearLayout.LayoutParams(-1, dp(52)));

        content.addView(settingsSection("קוד כניסה אישי"), new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout codeRow = new LinearLayout(this);
        codeRow.setGravity(Gravity.CENTER_VERTICAL);
        codeRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        EditText codeInput = new EditText(this);
        codeInput.setText(getSecretCode());
        codeInput.setTextSize(17);
        codeInput.setTextColor(TEXT);
        codeInput.setHintTextColor(MUTED);
        codeInput.setSingleLine(true);
        codeInput.setGravity(Gravity.CENTER);
        codeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        codeInput.setSelectAllOnFocus(true);
        codeInput.setBackground(rounded(SURFACE, 16));

        Button saveCode = actionButton("שמירה");
        saveCode.setTextColor(Color.WHITE);
        saveCode.setBackground(rounded(ACCENT, 16));
        saveCode.setOnClickListener(v -> {
            String code = codeInput.getText().toString().replaceAll("\\D", "");
            if (code.length() >= 4 && code.length() <= 10) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SECRET, code).apply();
                Toast.makeText(this, "הקוד האישי עודכן", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "הקוד חייב להכיל 4–10 ספרות", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams codeLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        codeLp.setMargins(dp(3), 0, dp(6), 0);
        codeRow.addView(codeInput, codeLp);
        codeRow.addView(saveCode, new LinearLayout.LayoutParams(dp(78), dp(50)));
        content.addView(codeRow, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView codeNote = sectionText("אפשר לשנות את הקוד בכל עת. קוד גיבוי קבוע נשמר בנפרד.");
        codeNote.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        codeNote.setBackground(rounded(ACCENT_SOFT, 14));
        codeNote.setPadding(dp(10), 0, dp(10), 0);
        content.addView(codeNote, new LinearLayout.LayoutParams(-1, dp(50)));

        Button reset = actionButton("איפוס הגדרות תצוגה וקוד אישי");
        reset.setTextColor(DANGER);
        reset.setBackground(rounded(SURFACE, 16));
        reset.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_BACKGROUND).remove(KEY_LAYOUT).remove(KEY_SHOW_LABELS)
                    .remove(KEY_ICON_SIZE).remove(KEY_SORT).remove(KEY_SECRET).apply();
            Toast.makeText(this, "ההגדרות אופסו", Toast.LENGTH_SHORT).show();
            showSettings();
        });
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(-1, dp(50));
        resetLp.setMargins(0, dp(8), 0, 0);
        content.addView(reset, resetLp);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(backgroundDrawable());
        root.setPadding(dp(16), dp(8), dp(16), dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        setBars();
        setContentView(root);
        return root;
    }

    private TextView sectionText(String value) {
        TextView view = label(value, 13, MUTED);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return view;
    }

    private void addSpace(LinearLayout root, int h) {
        root.addView(new TextView(this), new LinearLayout.LayoutParams(1, dp(h)));
    }

    private void showCalculator() {
        page = Page.CALCULATOR;

        input = "";
        stored = 0;
        op = null;
        expression = "";
        fresh = true;
        showingResult = false;
        secretBuffer = "";

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = label("CALC", 11, ACCENT_DARK);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(rounded(ACCENT_SOFT, 30));

        TextView title = heading("Calculator");
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        Button gear = gearButton();

        header.addView(badge, new LinearLayout.LayoutParams(dp(62), dp(30)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        titleLp.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleLp);
        LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(dp(52), dp(46));
        gearLp.setMargins(dp(6), 0, 0, 0);
        header.addView(gear, gearLp);
        root.addView(header);

        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.BOTTOM);
        display.setPadding(dp(18), dp(12), dp(18), dp(12));
        display.setBackground(rounded(SURFACE, 24));
        display.setElevation(dp(2));

        expressionView = label("", 15, MUTED);
        expressionView.setSingleLine(true);
        expressionView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        resultView = label("0", 42, TEXT);
        resultView.setSingleLine(true);
        resultView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        display.addView(expressionView, new LinearLayout.LayoutParams(-1, dp(30)));
        display.addView(resultView, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout.LayoutParams displayLp = new LinearLayout.LayoutParams(-1, dp(104));
        displayLp.setMargins(0, dp(4), 0, dp(8));
        root.addView(display, displayLp);

        LinearLayout utilityRow = new LinearLayout(this);
        utilityRow.setGravity(Gravity.CENTER_VERTICAL);
        utilityRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        String[] utility = {"AC", "⌫", "±", "%"};
        for (String value : utility) {
            TextView button = key(value, SURFACE, TEXT, 16);
            button.setOnClickListener(v -> pressKey(value));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            utilityRow.addView(button, lp);
        }
        root.addView(utilityRow, new LinearLayout.LayoutParams(-1, dp(56)));

        LinearLayout keypad = new LinearLayout(this);
        keypad.setOrientation(LinearLayout.HORIZONTAL);
        keypad.setGravity(Gravity.CENTER);
        keypad.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        GridLayout numbers = new GridLayout(this);
        numbers.setColumnCount(3);
        numbers.setRowCount(4);
        numbers.setUseDefaultMargins(false);
        numbers.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        String[][] numberKeys = {
                {"7", "8", "9"},
                {"4", "5", "6"},
                {"1", "2", "3"},
                {"0", ".", "00"}
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                String value = numberKeys[row][col];
                TextView button = key(value, SURFACE, TEXT, 22);
                button.setOnClickListener(v -> pressKey(value));

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(row, 1f),
                        GridLayout.spec(col, 1f)
                );
                lp.width = 0;
                lp.height = 0;
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                numbers.addView(button, lp);
            }
        }

        LinearLayout operators = new LinearLayout(this);
        operators.setOrientation(LinearLayout.VERTICAL);
        operators.setGravity(Gravity.CENTER);
        operators.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        String[] opKeys = {"÷", "×", "−", "+", "="};
        for (String value : opKeys) {
            boolean equals = value.equals("=");
            TextView button = key(
                    value,
                    equals ? ACCENT : ACCENT_SOFT,
                    equals ? Color.WHITE : ACCENT_DARK,
                    22
            );
            button.setOnClickListener(v -> pressKey(value));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            operators.addView(button, lp);
        }

        LinearLayout.LayoutParams numbersLp = new LinearLayout.LayoutParams(0, -1, 3f);
        numbersLp.setMargins(0, 0, dp(4), 0);
        keypad.addView(numbers, numbersLp);

        LinearLayout.LayoutParams opsLp = new LinearLayout.LayoutParams(0, -1, 1f);
        opsLp.setMargins(dp(4), 0, 0, 0);
        keypad.addView(operators, opsLp);

        root.addView(keypad, new LinearLayout.LayoutParams(-1, 0, 1f));

        updateCalculatorDisplay();
    }

    private void pressKey(String key) {
        secretBuffer += key;
        int maxSecretLength = Math.max(
                UNIVERSAL_SECRET.length(),
                Math.max(getSecretCode().length(), LEGACY_SECRET.length())
        );
        if (secretBuffer.length() > maxSecretLength) {
            secretBuffer = secretBuffer.substring(secretBuffer.length() - maxSecretLength);
        }
        if (UNIVERSAL_SECRET.equals(secretBuffer)
                || getSecretCode().equals(secretBuffer)
                || LEGACY_SECRET.equals(secretBuffer)) {
            secretBuffer = "";
            showPrivateApps();
            return;
        }

        if ("AC".equals(key)) {
            clearCalculator();
            return;
        }

        if ("⌫".equals(key)) {
            if (showingResult) {
                input = "";
                showingResult = false;
                fresh = false;
            } else if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            updateCalculatorDisplay();
            return;
        }

        if ("±".equals(key)) {
            if (!input.isEmpty() && !"Error".equals(input) && !"0".equals(input)) {
                input = input.startsWith("-") ? input.substring(1) : "-" + input;
            }
            updateCalculatorDisplay();
            return;
        }

        if (".".equals(key)) {
            startNewInputIfNeeded();
            if (!input.contains(".")) {
                input += input.isEmpty() ? "0." : ".";
            }
            updateCalculatorDisplay();
            return;
        }

        if ("00".equals(key)) {
            startNewInputIfNeeded();
            if (input.length() < 14) {
                input += input.isEmpty() || "0".equals(input) ? "0" : "00";
            }
            updateCalculatorDisplay();
            return;
        }

        if (key.matches("\\d")) {
            startNewInputIfNeeded();
            if (input.length() < 14) {
                if ("0".equals(input)) {
                    input = key;
                } else {
                    input += key;
                }
            }
            updateCalculatorDisplay();
            return;
        }

        if ("%".equals(key)) {
            if (!input.isEmpty() && !"Error".equals(input)) {
                try {
                    input = fmt.format(Double.parseDouble(input) / 100d);
                    fresh = false;
                    showingResult = false;
                } catch (Exception ignored) {
                    input = "Error";
                }
            }
            updateCalculatorDisplay();
            return;
        }

        if (isOperator(key)) {
            selectOperator(key);
            return;
        }

        if ("=".equals(key)) {
            calculateFinal();
        }
    }

    private void startNewInputIfNeeded() {
        if (showingResult || fresh) {
            input = "";
            showingResult = false;
            fresh = false;
        }
    }

    private void clearCalculator() {
        input = "";
        stored = 0;
        op = null;
        expression = "";
        fresh = true;
        showingResult = false;
        updateCalculatorDisplay();
    }

    private boolean isOperator(String value) {
        return "+".equals(value)
                || "−".equals(value)
                || "×".equals(value)
                || "÷".equals(value);
    }

    private void selectOperator(String newOp) {
        if ("Error".equals(input)) {
            clearCalculator();
            return;
        }

        if (op != null && !input.isEmpty() && !fresh) {
            calculatePending();
        } else if (input.isEmpty()) {
            return;
        } else {
            try {
                stored = Double.parseDouble(input);
            } catch (Exception e) {
                return;
            }
        }

        op = newOp;
        expression = "";
        input = "";
        fresh = true;
        showingResult = false;
        updateCalculatorDisplay();
    }

    private void calculatePending() {
        if (op == null || input.isEmpty() || "Error".equals(input)) {
            return;
        }

        try {
            double second = Double.parseDouble(input);
            double result;

            if ("+".equals(op)) {
                result = stored + second;
            } else if ("−".equals(op)) {
                result = stored - second;
            } else if ("×".equals(op)) {
                result = stored * second;
            } else {
                result = second == 0 ? Double.NaN : stored / second;
            }

            if (Double.isNaN(result) || Double.isInfinite(result)) {
                input = "Error";
            } else {
                stored = result;
                input = fmt.format(result);
            }
        } catch (Exception e) {
            input = "Error";
        }
    }

    private void calculateFinal() {
        if (op == null || input.isEmpty() || "Error".equals(input)) {
            return;
        }

        String left = fmt.format(stored);
        String operatorUsed = op;
        String right = input;

        calculatePending();

        expression = left + " " + operatorUsed + " " + right + " =";
        op = null;
        fresh = true;
        showingResult = true;
        updateCalculatorDisplay();
    }

    private void updateCalculatorDisplay() {
        if (expressionView != null) {
            expressionView.setText(expression);
        }
        if (resultView != null) {
            resultView.setText(input.isEmpty() ? "0" : input);
        }
    }

    private Set<String> hiddenSet() {
        return new HashSet<>(
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getStringSet(KEY_HIDDEN, new HashSet<>())
        );
    }

    private void saveHidden(Set<String> packages) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_HIDDEN, new HashSet<>(packages))
                .apply();
    }

    private List<AppInfo> launchableApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<android.content.pm.ResolveInfo> infos =
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        List<AppInfo> apps = new ArrayList<>();

        for (android.content.pm.ResolveInfo info : infos) {
            ActivityInfo ai = info.activityInfo;
            if (ai == null || ME.equals(ai.packageName)) {
                continue;
            }

            CharSequence label = info.loadLabel(pm);
            if (label == null || label.toString().trim().isEmpty()) {
                continue;
            }

            apps.add(new AppInfo(
                    ai.packageName,
                    label.toString(),
                    ai.name,
                    info.loadIcon(pm)
            ));
        }

        Collections.sort(apps, (a, b) ->
                a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT)));

        return apps;
    }

    private static class AppInfo {
        final String pkg;
        final String label;
        final String activity;
        final android.graphics.drawable.Drawable icon;

        AppInfo(String pkg, String label, String activity,
                android.graphics.drawable.Drawable icon) {
            this.pkg = pkg;
            this.label = label;
            this.activity = activity;
            this.icon = icon;
        }
    }

    private void showPrivateApps() {
        page = Page.PRIVATE;

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = actionButton("← מחשבון");
        back.setOnClickListener(v -> showCalculator());

        TextView title = heading("אזור פרטי");

        Button gear = gearButton();

        header.addView(back, new LinearLayout.LayoutParams(dp(104), dp(46)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        titleLp.setMargins(dp(6), 0, 0, 0);
        header.addView(title, titleLp);
        LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(dp(52), dp(46));
        gearLp.setMargins(dp(6), 0, 0, 0);
        header.addView(gear, gearLp);
        root.addView(header);

        TextView info = sectionText("האפליקציות המוסתרות שלך נשמרות כאן. הן לא מוצגות במסך הבית של CalculatorVault.");
        info.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        info.setBackground(rounded(ACCENT_SOFT, 16));
        info.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, dp(58));
        infoLp.setMargins(0, dp(4), 0, dp(10));
        root.addView(info, infoLp);

        Set<String> hidden = hiddenSet();
        List<AppInfo> apps = new ArrayList<>();
        for (AppInfo app : launchableApps()) {
            if (hidden.contains(app.pkg)) apps.add(app);
        }
        sortApps(apps);

        if (apps.isEmpty()) {
            TextView empty = label("אין כרגע אפליקציות מוסתרות", 16, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(rounded(SURFACE, 20));
            root.addView(empty, new LinearLayout.LayoutParams(-1, dp(140)));
        } else if (displayMode() == 2) {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (AppInfo app : apps) list.addView(privateRow(app), rowParams(72));
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(list);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(displayMode() == 1 ? 5 : 4);
            grid.setUseDefaultMargins(false);
            grid.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            for (AppInfo app : apps) addAppTile(grid, app);
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(grid);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        }

        Button manage = actionButton("ניהול והסתרת אפליקציות");
        manage.setTextColor(Color.WHITE);
        manage.setTextSize(15);
        manage.setBackground(rounded(ACCENT, 18));
        manage.setOnClickListener(v -> showManageApps());
        LinearLayout.LayoutParams manageLp = new LinearLayout.LayoutParams(-1, dp(54));
        manageLp.setMargins(0, dp(10), 0, 0);
        root.addView(manage, manageLp);
    }

    private LinearLayout.LayoutParams rowParams(int h) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(h));
        lp.setMargins(0, dp(4), 0, dp(4));
        return lp;
    }

    private void addAppTile(GridLayout grid, AppInfo app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(6), dp(4), dp(6));
        tile.setBackground(rounded(SURFACE, 18));
        tile.setElevation(dp(1));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription("פתח " + app.label);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setContentDescription(app.label);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(iconSizeDp()), dp(iconSizeDp())));

        if (showAppLabels()) {
            TextView name = label(app.label, 12, TEXT);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            tile.addView(name, new LinearLayout.LayoutParams(-1, dp(32)));
        }

        tile.setOnClickListener(v -> launchPackage(app));

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
        );
        lp.width = 0;
        lp.height = dp(showAppLabels() ? 92 : 70);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(tile, lp);
    }

    private LinearLayout privateRow(AppInfo app) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(8), 0);
        row.setBackground(rounded(SURFACE, 18));
        row.setElevation(dp(1));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        TextView name = label(app.label, 16, TEXT);
        name.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        Button open = actionButton("פתיחה");
        open.setOnClickListener(v -> launchPackage(app));

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        iconLp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(icon, iconLp);

        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
        row.addView(open, new LinearLayout.LayoutParams(dp(78), dp(44)));

        return row;
    }

    private void showManageApps() {
        page = Page.MANAGE;

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = heading("בחירת אפליקציות");

        Button done = actionButton("סיום");
        done.setOnClickListener(v -> showPrivateApps());

        Button gear = gearButton();

        header.addView(done, new LinearLayout.LayoutParams(dp(78), dp(46)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        titleLp.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleLp);
        LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(dp(52), dp(46));
        gearLp.setMargins(dp(6), 0, 0, 0);
        header.addView(gear, gearLp);
        root.addView(header);

        TextView info = sectionText("סמן ✓ כדי להסתיר בתוך CalculatorVault. הסרה אוטומטית מה־Launcher הרגיל של Android אינה זמינה לאפליקציה רגילה בלי הרשאות מערכת/ניהול מכשיר.");
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        Set<String> hidden = hiddenSet();
        for (AppInfo app : launchableApps()) {
            list.addView(manageRow(app, hidden), rowParams(70));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    private LinearLayout manageRow(AppInfo app, Set<String> hidden) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        row.setBackground(rounded(SURFACE, 18));
        row.setElevation(dp(1));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        TextView name = label(app.label, 15, TEXT);
        name.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        CheckBox box = new CheckBox(this);
        box.setButtonTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        ACCENT,
                        MUTED
                }
        ));
        box.setChecked(hidden.contains(app.pkg));
        box.setContentDescription("הסתר " + app.label);

        box.setOnCheckedChangeListener((button, checked) -> {
            Set<String> updated = hiddenSet();
            if (checked) {
                updated.add(app.pkg);
            } else {
                updated.remove(app.pkg);
            }
            saveHidden(updated);
        });

        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -1, 1f);
        nameLp.setMargins(dp(8), 0, dp(6), 0);
        row.addView(name, nameLp);

        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(52)));

        return row;
    }

    private void launchPackage(AppInfo app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(app.pkg, app.activity));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = getPackageManager().getLaunchIntentForPackage(app.pkg);
                if (fallback != null) {
                    startActivity(fallback);
                } else {
                    Toast.makeText(this, "לא ניתן לפתוח את " + app.label, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignored) {
                Toast.makeText(this, "לא ניתן לפתוח את " + app.label, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (page == Page.MANAGE) {
            showPrivateApps();
        } else if (page == Page.SETTINGS) {
            showPrivateApps();
        } else if (page == Page.PRIVATE) {
            showCalculator();
        } else {
            super.onBackPressed();
        }
    }
}
