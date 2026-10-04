package com.app.calculatorvault;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS = "vault";
    private static final String KEY_HIDDEN = "hidden_packages";
    private static final String ME = "com.app.calculatorvault";

    private final int GREEN = Color.rgb(36, 155, 88);
    private final int GREEN_DARK = Color.rgb(25, 122, 68);
    private final int GREEN_SOFT = Color.rgb(229, 247, 235);
    private final int BG = Color.rgb(243, 251, 246);
    private final int TEXT = Color.rgb(24, 49, 38);
    private final int MUTED = Color.rgb(108, 127, 117);
    private final DecimalFormat fmt = new DecimalFormat("0.##########");

    private TextView expressionView;
    private TextView resultView;

    private String input = "";
    private double stored = 0;
    private String op = null;
    private String expression = "";
    private boolean fresh = true;
    private boolean showingResult = false;
    private String secret = "";

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (getIntent() != null && getIntent().getCategories() != null
                && getIntent().getCategories().contains(Intent.CATEGORY_HOME)) {
            showLauncher();
        } else {
            showCalculator();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getCategories() != null
                && intent.getCategories().contains(Intent.CATEGORY_HOME)) {
            showLauncher();
        } else {
            showCalculator();
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private TextView title(String value) {
        TextView v = text(value, 23, TEXT);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button smallButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(GREEN_DARK);
        b.setBackground(rounded(Color.WHITE, 16));
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    private TextView calculatorKey(String value, boolean accent) {
        TextView b = text(value, 21, accent ? Color.WHITE : TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(accent ? GREEN : Color.WHITE, 18));
        b.setElevation(dp(2));
        b.setClickable(true);
        return b;
    }

    private LinearLayout page() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);
        setContentView(root);
        return root;
    }

    private void showCalculator() {
        input = "";
        stored = 0;
        op = null;
        expression = "";
        fresh = true;
        showingResult = false;
        secret = "";

        LinearLayout root = page();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView appTitle = title("Calculator");
        header.addView(appTitle, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView badge = text("CALC", 11, GREEN_DARK);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(rounded(GREEN_SOFT, 30));
        header.addView(badge, new LinearLayout.LayoutParams(dp(64), dp(30)));
        root.addView(header);

        LinearLayout displayCard = new LinearLayout(this);
        displayCard.setOrientation(LinearLayout.VERTICAL);
        displayCard.setGravity(Gravity.BOTTOM);
        displayCard.setPadding(dp(16), dp(10), dp(16), dp(12));
        displayCard.setBackground(rounded(Color.WHITE, 22));

        expressionView = text("", 16, MUTED);
        expressionView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        expressionView.setSingleLine(true);
        displayCard.addView(expressionView, new LinearLayout.LayoutParams(-1, dp(34)));

        resultView = text("0", 42, TEXT);
        resultView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        resultView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultView.setSingleLine(true);
        displayCard.addView(resultView, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout.LayoutParams displayLp = new LinearLayout.LayoutParams(-1, dp(112));
        displayLp.setMargins(0, dp(4), 0, dp(6));
        root.addView(displayCard, displayLp);

        TextView sub = text("מחשבון רגיל", 12, MUTED);
        sub.setGravity(Gravity.RIGHT);
        sub.setPadding(dp(4), 0, dp(4), 0);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(22)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(5);
        grid.setUseDefaultMargins(false);

        String[][] keys = {
                {"AC", "⌫", "±", "÷"},
                {"7", "8", "9", "×"},
                {"4", "5", "6", "−"},
                {"1", "2", "3", "+"},
                {"%", "0", ".", "="}
        };

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                String key = keys[row][col];
                boolean accent = key.equals("=") || isOperator(key);
                TextView button = calculatorKey(key, accent);
                button.setOnClickListener(v -> pressKey(key));

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(row, 1f),
                        GridLayout.spec(col, 1f));
                lp.width = 0;
                lp.height = 0;
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                grid.addView(button, lp);
            }
        }

        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(grid, gridLp);

        TextView foot = text("33? לא. 333=333 פותח את האזור הפרטי", 11, MUTED);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, new LinearLayout.LayoutParams(-1, dp(30)));

        updateCalculatorDisplay();
    }

    private void pressKey(String key) {
        secret += key;
        if (secret.length() > 7) {
            secret = secret.substring(secret.length() - 7);
        }
        if ("333=333".equals(secret)) {
            secret = "";
            showPrivateApps();
            return;
        }

        if (key.equals("AC")) {
            input = "";
            stored = 0;
            op = null;
            expression = "";
            fresh = true;
            showingResult = false;
            updateCalculatorDisplay();
            return;
        }

        if (key.equals("⌫")) {
            if (showingResult) {
                input = "";
                showingResult = false;
            } else if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            updateCalculatorDisplay();
            return;
        }

        if (key.equals("±")) {
            if (!input.isEmpty() && !input.equals("0") && !input.equals("Error")) {
                input = input.startsWith("-") ? input.substring(1) : "-" + input;
            }
            updateCalculatorDisplay();
            return;
        }

        if (key.equals(".")) {
            if (showingResult || fresh) {
                input = "";
                fresh = false;
                showingResult = false;
            }
            if (!input.contains(".")) {
                input += input.isEmpty() ? "0." : ".";
            }
            updateCalculatorDisplay();
            return;
        }

        if (key.matches("\\d")) {
            if (showingResult || fresh) {
                input = "";
                showingResult = false;
                fresh = false;
            }
            if (input.length() < 14) input += key;
            updateCalculatorDisplay();
            return;
        }

        if (key.equals("%")) {
            if (!input.isEmpty() && !input.equals("Error")) {
                try {
                    input = fmt.format(Double.parseDouble(input) / 100.0);
                } catch (Exception ignored) {}
            }
            updateCalculatorDisplay();
            return;
        }

        if (isOperator(key)) {
            selectOperator(key);
            return;
        }

        if (key.equals("=")) {
            calculateFinal();
        }
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷");
    }

    private String prettyNumber(String value) {
        if (value == null || value.isEmpty()) return "0";
        return value;
    }

    private void selectOperator(String newOp) {
        if (input.isEmpty() || input.equals("Error")) return;

        try {
            stored = Double.parseDouble(input);
        } catch (Exception e) {
            return;
        }

        op = newOp;
        expression = prettyNumber(input) + " " + newOp;
        input = "";
        fresh = true;
        showingResult = false;
        updateCalculatorDisplay();
    }

    private void calculateFinal() {
        if (op == null || input.isEmpty() || input.equals("Error")) return;

        try {
            double second = Double.parseDouble(input);
            double result;

            if (op.equals("+")) result = stored + second;
            else if (op.equals("−")) result = stored - second;
            else if (op.equals("×")) result = stored * second;
            else result = second == 0 ? Double.NaN : stored / second;

            if (Double.isNaN(result) || Double.isInfinite(result)) {
                expression = prettyNumber(fmt.format(stored)) + " " + op + " " + prettyNumber(input) + " =";
                input = "Error";
            } else {
                expression = prettyNumber(fmt.format(stored)) + " " + op + " " + prettyNumber(input) + " =";
                input = fmt.format(result);
            }

            op = null;
            fresh = true;
            showingResult = true;
            updateCalculatorDisplay();
        } catch (Exception e) {
            expression = prettyNumber(input) + " =";
            input = "Error";
            op = null;
            fresh = true;
            showingResult = true;
            updateCalculatorDisplay();
        }
    }

    private void updateCalculatorDisplay() {
        if (expressionView != null) expressionView.setText(expression);
        if (resultView != null) resultView.setText(input.isEmpty() ? "0" : input);
    }

    private Set<String> hiddenSet() {
        return new HashSet<>(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getStringSet(KEY_HIDDEN, new HashSet<>()));
    }

    private void saveHidden(Set<String> set) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putStringSet(KEY_HIDDEN, new HashSet<>(set))
                .apply();
    }

    private List<ResolveInfoWrap> launchableApps() {
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> infos =
                pm.queryIntentActivities(i, PackageManager.MATCH_ALL);

        List<ResolveInfoWrap> out = new ArrayList<>();
        for (android.content.pm.ResolveInfo ri : infos) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ME.equals(ai.packageName)) continue;
            CharSequence label = ri.loadLabel(pm);
            if (label == null || label.toString().trim().isEmpty()) continue;
            out.add(new ResolveInfoWrap(ai.packageName, label.toString(), ai.name));
        }

        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    private static class ResolveInfoWrap {
        String pkg, label, activity;
        ResolveInfoWrap(String p, String l, String a) {
            pkg = p;
            label = l;
            activity = a;
        }
    }

    private void showPrivateApps() {
        LinearLayout root = page();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = title("אזור פרטי");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button back = smallButton("מחשבון");
        back.setOnClickListener(v -> showCalculator());
        top.addView(back, new LinearLayout.LayoutParams(dp(98), dp(44)));
        root.addView(top);

        TextView info = text("כאן נמצאות האפליקציות שבחרת לשמור באזור הפרטי.", 14, MUTED);
        info.setGravity(Gravity.RIGHT);
        info.setPadding(dp(4), 0, dp(4), dp(6));
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        Set<String> hidden = hiddenSet();
        int count = 0;
        for (ResolveInfoWrap app : launchableApps()) {
            if (hidden.contains(app.pkg)) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(66));
                lp.setMargins(0, dp(4), 0, dp(4));
                list.addView(privateRow(app), lp);
                count++;
            }
        }

        if (count == 0) {
            TextView empty = text("עדיין לא בחרת אפליקציות להסתרה", 16, MUTED);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(130)));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button manage = smallButton("ניהול אפליקציות מוסתרות");
        manage.setTextColor(Color.WHITE);
        manage.setBackground(rounded(GREEN, 18));
        manage.setOnClickListener(v -> showManageApps());

        LinearLayout.LayoutParams manageLp = new LinearLayout.LayoutParams(-1, dp(52));
        manageLp.setMargins(0, dp(8), 0, 0);
        root.addView(manage, manageLp);
    }

    private LinearLayout privateRow(ResolveInfoWrap app) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(10), 0);
        row.setBackground(rounded(Color.WHITE, 18));
        row.setElevation(dp(1));

        TextView icon = text("▣", 23, GREEN);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView name = text(app.label, 16, TEXT);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        Button open = smallButton("פתיחה");
        open.setOnClickListener(v -> launchPackage(app));
        row.addView(open, new LinearLayout.LayoutParams(dp(82), dp(44)));

        return row;
    }

    private void showManageApps() {
        LinearLayout root = page();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = title("בחירת אפליקציות");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button done = smallButton("סיום");
        done.setOnClickListener(v -> showPrivateApps());
        top.addView(done, new LinearLayout.LayoutParams(dp(82), dp(44)));
        root.addView(top);

        TextView info = text("סמן אפליקציות שיופיעו באזור הפרטי.", 14, MUTED);
        info.setGravity(Gravity.RIGHT);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        Set<String> hidden = hiddenSet();
        for (ResolveInfoWrap app : launchableApps()) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(66));
            lp.setMargins(0, dp(4), 0, dp(4));
            list.addView(manageRow(app, hidden), lp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private LinearLayout manageRow(ResolveInfoWrap app, Set<String> hidden) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(8), 0);
        row.setBackground(rounded(Color.WHITE, 18));
        row.setElevation(dp(1));

        TextView name = text(app.label, 16, TEXT);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        CheckBox box = new CheckBox(this);
        box.setChecked(hidden.contains(app.pkg));
        box.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
        box.setOnCheckedChangeListener((b, checked) -> {
            Set<String> now = hiddenSet();
            if (checked) now.add(app.pkg);
            else now.remove(app.pkg);
            saveHidden(now);
        });
        row.addView(box, new LinearLayout.LayoutParams(dp(54), dp(54)));
        return row;
    }

    private void showLauncher() {
        LinearLayout root = page();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = title("Home");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button vault = smallButton("פרטי");
        vault.setOnClickListener(v -> showPrivateApps());
        top.addView(vault, new LinearLayout.LayoutParams(dp(82), dp(44)));
        root.addView(top);

        TextView hint = text("מגירת אפליקציות", 14, MUTED);
        hint.setGravity(Gravity.RIGHT);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(32)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);

        Set<String> hidden = hiddenSet();
        for (ResolveInfoWrap app : launchableApps()) {
            if (hidden.contains(app.pkg)) continue;

            TextView tile = text(app.label, 13, TEXT);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(6), dp(8), dp(6), dp(8));
            tile.setBackground(rounded(Color.WHITE, 18));
            tile.setClickable(true);
            tile.setOnClickListener(v -> launchPackage(app));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = dp(90);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(tile, lp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text("האפליקציות שסומנו כפרטיות אינן מופיעות כאן.", 12, MUTED);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(38)));
    }

    private void launchPackage(ResolveInfoWrap app) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(new ComponentName(app.pkg, app.activity));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    public void requestHomeLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager role = getSystemService(RoleManager.class);
                if (role != null && role.isRoleAvailable(RoleManager.ROLE_HOME)
                        && !role.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME), 100);
                    return;
                }
            }
            startActivity(new Intent("android.settings.HOME_SETTINGS"));
        } catch (Exception ignored) {}
    }
}
