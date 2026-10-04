package com.app.calculatorvault;

import android.app.Activity;
import android.app.role.RoleManager;
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
    private static final String SECRET = "333=333";
    private static final String KEY_BACKGROUND = "background_style";
    private static final String KEY_LAYOUT = "app_layout";
    private static final String KEY_SHOW_LABELS = "show_app_labels";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_SORT = "app_sort";

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

    private enum Page {
        CALCULATOR, PRIVATE, MANAGE, SETTINGS, LAUNCHER
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBars();

        if (isHomeIntent(getIntent())) {
            showLauncher();
        } else {
            showCalculator();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (isHomeIntent(intent)) {
            showLauncher();
        } else {
            showCalculator();
        }
    }

    private boolean isHomeIntent(Intent intent) {
        return intent != null
                && intent.getCategories() != null
                && intent.getCategories().contains(Intent.CATEGORY_HOME);
    }

    private void setBars() {
        getWindow().setStatusBarColor(backgroundColor());
        getWindow().setNavigationBarColor(SURFACE);
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
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

    private int backgroundColor() {
        int style = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_BACKGROUND, 0);
        if (style == 1) return Color.rgb(239, 247, 255);
        if (style == 2) return Color.rgb(250, 246, 238);
        return BG;
    }

    private int displayMode() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_LAYOUT, 0);
    }

    private boolean showAppLabels() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_SHOW_LABELS, true);
    }

    private int iconSizeDp() {
        int size = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_ICON_SIZE, 1);
        if (size == 0) return 36;
        if (size == 2) return 54;
        return 44;
    }

    private int appSort() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_SORT, 0);
    }

    private void sortApps(List<AppInfo> apps) {
        final boolean reverse = appSort() == 1;
        Collections.sort(apps, (a, b) -> {
            int result = a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT));
            return reverse ? -result : result;
        });
    }

    private Button gearButton() {
        Button button = actionButton("⚙");
        button.setTextSize(21);
        button.setTextColor(ACCENT_DARK);
        button.setContentDescription("הגדרות");
        button.setOnClickListener(v -> showSettings());
        return button;
    }

    private Button settingChoice(String value, boolean selected) {
        Button button = actionButton(selected ? "✓  " + value : value);
        button.setTextSize(14);
        button.setTextColor(selected ? ACCENT_DARK : TEXT);
        button.setBackground(rounded(selected ? ACCENT_SOFT : SURFACE, 16));
        return button;
    }

    private TextView settingsSection(String value) {
        TextView view = label(value, 15, TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return view;
    }

    private void showSettings() {
        page = Page.SETTINGS;

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = actionButton("← חזרה");
        back.setOnClickListener(v -> showPrivateApps());

        TextView title = heading("הגדרות");

        header.addView(back, new LinearLayout.LayoutParams(dp(90), dp(46)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        titleLp.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleLp);
        root.addView(header);

        root.addView(settingsSection("מראה"), new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout backgrounds = new LinearLayout(this);
        backgrounds.setGravity(Gravity.CENTER_VERTICAL);
        int bgStyle = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_BACKGROUND, 0);
        String[] bgNames = {"בהיר", "תכלת עדין", "שמנת"};
        for (int i = 0; i < bgNames.length; i++) {
            final int choice = i;
            Button button = settingChoice(bgNames[i], bgStyle == i);
            button.setOnClickListener(v -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_BACKGROUND, choice).apply();
                showSettings();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            backgrounds.addView(button, lp);
        }
        root.addView(backgrounds, new LinearLayout.LayoutParams(-1, dp(56)));

        root.addView(settingsSection("תצוגת האפליקציות"), new LinearLayout.LayoutParams(-1, dp(40)));

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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            layouts.addView(button, lp);
        }
        root.addView(layouts, new LinearLayout.LayoutParams(-1, dp(56)));

        root.addView(settingsSection("שמות אייקונים"), new LinearLayout.LayoutParams(-1, dp(38)));
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
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(-1, dp(52));
        labelsLp.setMargins(0, dp(2), 0, dp(6));
        root.addView(labels, labelsLp);

        root.addView(settingsSection("גודל האייקונים"), new LinearLayout.LayoutParams(-1, dp(38)));
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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            iconSizes.addView(button, lp);
        }
        root.addView(iconSizes, new LinearLayout.LayoutParams(-1, dp(56)));

        root.addView(settingsSection("סדר האפליקציות"), new LinearLayout.LayoutParams(-1, dp(38)));
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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            sorts.addView(button, lp);
        }
        root.addView(sorts, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView note = sectionText("ההגדרות נשמרות אוטומטית ומשפיעות על מסך הבית ועל האזור הפרטי.");
        note.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        note.setBackground(rounded(ACCENT_SOFT, 16));
        note.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, dp(54));
        noteLp.setMargins(0, dp(12), 0, dp(10));
        root.addView(note, noteLp);

        Button reset = actionButton("איפוס הגדרות תצוגה");
        reset.setTextColor(DANGER);
        reset.setBackground(rounded(SURFACE, 16));
        reset.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_BACKGROUND).remove(KEY_LAYOUT).remove(KEY_SHOW_LABELS)
                    .remove(KEY_ICON_SIZE).remove(KEY_SORT).apply();
            showSettings();
        });
        root.addView(reset, new LinearLayout.LayoutParams(-1, dp(50)));
    }

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor());
        root.setPadding(dp(16), dp(8), dp(16), dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
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

        header.addView(badge, new LinearLayout.LayoutParams(dp(62), dp(30)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        titleLp.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleLp);
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

        TextView hint = sectionText("מחשבון רגיל  •  הסימן שבחרת נשאר מוצג עד =");
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(26)));

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

        TextView secretHint = sectionText("333=333");
        secretHint.setGravity(Gravity.CENTER);
        secretHint.setTextColor(Color.TRANSPARENT);
        root.addView(secretHint, new LinearLayout.LayoutParams(-1, dp(18)));

        updateCalculatorDisplay();
    }

    private void pressKey(String key) {
        secretBuffer += key;
        if (secretBuffer.length() > SECRET.length()) {
            secretBuffer = secretBuffer.substring(secretBuffer.length() - SECRET.length());
        }
        if (SECRET.equals(secretBuffer)) {
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
        expression = fmt.format(stored) + " " + newOp;
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

        TextView info = sectionText("סמן ✓ ליד אפליקציה כדי להסתיר אותה מהמסך הראשי של CalculatorVault.");
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

    private void showLauncher() {
        page = Page.LAUNCHER;

        LinearLayout root = pageRoot();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = heading("Home");

        Button vault = actionButton("אזור פרטי");
        vault.setOnClickListener(v -> showPrivateApps());

        Button gear = gearButton();

        header.addView(vault, new LinearLayout.LayoutParams(dp(96), dp(46)));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        titleLp.setMargins(dp(8), 0, 0, 0);
        header.addView(title, titleLp);

        LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(dp(52), dp(46));
        gearLp.setMargins(dp(6), 0, 0, 0);
        header.addView(gear, gearLp);
        root.addView(header);

        LinearLayout homeActions = new LinearLayout(this);
        homeActions.setGravity(Gravity.CENTER_VERTICAL);

        Button setHome = actionButton(isCurrentHome() ? "✓ מוגדר כמסך בית" : "הגדר כמסך הבית");
        setHome.setTextColor(isCurrentHome() ? ACCENT_DARK : Color.WHITE);
        setHome.setBackground(rounded(isCurrentHome() ? ACCENT_SOFT : ACCENT, 16));
        setHome.setOnClickListener(v -> requestHomeLauncher());

        Button refresh = actionButton("רענון");
        refresh.setOnClickListener(v -> showLauncher());

        homeActions.addView(setHome, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(dp(82), dp(48));
        refreshLp.setMargins(dp(8), 0, 0, 0);
        homeActions.addView(refresh, refreshLp);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, dp(54));
        actionsLp.setMargins(0, dp(2), 0, dp(6));
        root.addView(homeActions, actionsLp);

        Set<String> hidden = hiddenSet();
        List<AppInfo> apps = new ArrayList<>();
        for (AppInfo app : launchableApps()) if (!hidden.contains(app.pkg)) apps.add(app);
        sortApps(apps);

        if (apps.isEmpty()) {
            TextView empty = label("אין אפליקציות להצגה", 16, MUTED);
            empty.setGravity(Gravity.CENTER);
            root.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else if (displayMode() == 2) {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (AppInfo app : apps) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), 0, dp(10), 0);
                row.setBackground(rounded(SURFACE, 18));
                row.setElevation(dp(1));
                row.setClickable(true);
                row.setFocusable(true);
                row.setOnClickListener(v -> launchPackage(app));

                ImageView icon = new ImageView(this);
                icon.setImageDrawable(app.icon);
                icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                row.addView(icon, new LinearLayout.LayoutParams(dp(iconSizeDp()), dp(iconSizeDp())));

                if (showAppLabels()) {
                    TextView name = label(app.label, 15, TEXT);
                    name.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -1, 1f);
                    nameLp.setMargins(dp(12), 0, 0, 0);
                    row.addView(name, nameLp);
                }

                list.addView(row, rowParams(72));
            }
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

        TextView footer = sectionText(
                "כדי שההסתרה תשפיע על מסך הבית של המכשיר, יש לבחור ב־CalculatorVault כמסך הבית."
        );
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(42)));
    }

    private boolean isCurrentHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager role = getSystemService(RoleManager.class);
                return role != null
                        && role.isRoleAvailable(RoleManager.ROLE_HOME)
                        && role.isRoleHeld(RoleManager.ROLE_HOME);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private void requestHomeLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager role = getSystemService(RoleManager.class);
                if (role != null
                        && role.isRoleAvailable(RoleManager.ROLE_HOME)
                        && !role.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(
                            role.createRequestRoleIntent(RoleManager.ROLE_HOME),
                            1001
                    );
                    return;
                }
            }

            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        } catch (Exception e) {
            Toast.makeText(this, "פתח את הגדרות מסך הבית ובחר CalculatorVault", Toast.LENGTH_LONG).show();
        }
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
        } else if (page == Page.LAUNCHER) {
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}
