package com.app.calculatorvault;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS = "vault";
    private static final String KEY_HIDDEN = "hidden_packages";
    private static final String ME = "com.app.calculatorvault";

    private final int GREEN = Color.rgb(36, 155, 88);
    private final int GREEN_DARK = Color.rgb(25, 122, 68);
    private final int BG = Color.rgb(243, 251, 246);
    private final int TEXT = Color.rgb(24, 49, 38);
    private final int MUTED = Color.rgb(108, 127, 117);
    private final DecimalFormat fmt = new DecimalFormat("0.##########");

    private TextView display;
    private String input = "";
    private double stored = 0;
    private String op = null;
    private boolean fresh = true;
    private String secret = "";

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (getIntent() != null && Intent.CATEGORY_HOME != null
                && getIntent().getCategories() != null
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

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int) radius));
        return g;
    }

    private TextView label(String text, float size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setFontFeatureSettings("kern");
        return v;
    }

    private TextView title(String text) {
        TextView v = label(text, 23, TEXT);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(GREEN_DARK);
        b.setBackground(bg(Color.WHITE, 18));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private TextView calcKey(String text, boolean accent) {
        TextView b = label(text, 21, accent ? Color.WHITE : TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(accent ? GREEN : Color.WHITE, 18));
        b.setElevation(dp(2));
        b.setClickable(true);
        return b;
    }

    private void base(LinearLayout body) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(10), dp(16), dp(10));
        setContentView(root);
        root.addView(body, new LinearLayout.LayoutParams(-1, -1));
    }

    private void showCalculator() {
        input = "";
        stored = 0;
        op = null;
        fresh = true;
        secret = "";

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView app = title("Calculator");
        header.addView(app, new LinearLayout.LayoutParams(0, dp(54), 1));
        TextView badge = label("CALC", 12, GREEN_DARK);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setBackground(bg(Color.WHITE, 30));
        header.addView(badge, new LinearLayout.LayoutParams(dp(70), dp(34)));
        body.addView(header);

        Space s1 = new Space(this);
        body.addView(s1, new LinearLayout.LayoutParams(1, 0, 1));

        display = label("0", 43, TEXT);
        display.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        display.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        display.setPadding(dp(14), 0, dp(14), 0);
        display.setBackground(bg(Color.WHITE, 22));
        display.setSingleLine(true);
        body.addView(display, new LinearLayout.LayoutParams(-1, dp(105)));

        TextView sub = label("מחשבון רגיל", 13, MUTED);
        sub.setGravity(Gravity.RIGHT);
        sub.setPadding(0, dp(6), dp(6), dp(6));
        body.addView(sub, new LinearLayout.LayoutParams(-1, dp(30)));

        String[][] keys = {
                {"AC", "⌫", "±", "÷"},
                {"7", "8", "9", "×"},
                {"4", "5", "6", "−"},
                {"1", "2", "3", "+"},
                {"%", "0", ".", "="}
        };

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (String[] row : keys) {
            LinearLayout r = new LinearLayout(this);
            r.setWeightSum(4);
            r.setPadding(0, dp(3), 0, dp(3));
            for (String key : row) {
                boolean accent = key.equals("=") || key.equals("+") || key.equals("−") || key.equals("×") || key.equals("÷");
                TextView k = calcKey(key, accent);
                k.setOnClickListener(v -> pressKey(key));
                r.addView(k, new LinearLayout.LayoutParams(0, dp(62), 1));
            }
            grid.addView(r, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        body.addView(grid, new LinearLayout.LayoutParams(-1, 0, 5));

        TextView foot = label("כניסה לאזור הפרטי: 333=333", 12, MUTED);
        foot.setGravity(Gravity.CENTER);
        body.addView(foot, new LinearLayout.LayoutParams(-1, dp(32)));

        base(body);
    }

    private void pressKey(String key) {
        secret += key;
        if (secret.length() > 7) secret = secret.substring(secret.length() - 7);
        if ("333=333".equals(secret)) {
            secret = "";
            hideKeyboardAndOpenPrivate();
            return;
        }

        if (key.equals("AC")) {
            input = "";
            stored = 0;
            op = null;
            fresh = true;
            updateDisplay();
            return;
        }

        if (key.equals("⌫")) {
            if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
            if (input.isEmpty()) input = "";
            updateDisplay();
            return;
        }

        if (key.equals("±")) {
            if (!input.isEmpty() && !input.equals("0")) {
                input = input.startsWith("-") ? input.substring(1) : "-" + input;
            }
            updateDisplay();
            return;
        }

        if (key.equals(".")) {
            if (fresh) { input = ""; fresh = false; }
            if (!input.contains(".")) input += input.isEmpty() ? "0." : ".";
            updateDisplay();
            return;
        }

        if (key.matches("\\d")) {
            if (fresh) input = "";
            fresh = false;
            if (input.length() < 14) input += key;
            updateDisplay();
            return;
        }

        if (key.equals("%")) {
            if (!input.isEmpty()) {
                try {
                    double n = Double.parseDouble(input) / 100.0;
                    input = fmt.format(n);
                } catch (Exception ignored) {}
            }
            updateDisplay();
            return;
        }

        if (isOperator(key)) {
            selectOperator(key);
            return;
        }

        if (key.equals("=")) {
            equals();
        }
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷");
    }

    private void selectOperator(String newOp) {
        if (input.isEmpty() && op == null) return;
        if (op != null && !input.isEmpty()) equals();
        if (!input.isEmpty()) {
            try { stored = Double.parseDouble(input); } catch (Exception e) { stored = 0; }
        }
        op = newOp;
        fresh = true;
    }

    private void equals() {
        if (op == null || input.isEmpty()) {
            updateDisplay();
            fresh = true;
            return;
        }
        try {
            double b = Double.parseDouble(input);
            double result;
            if (op.equals("+")) result = stored + b;
            else if (op.equals("−")) result = stored - b;
            else if (op.equals("×")) result = stored * b;
            else result = b == 0 ? Double.NaN : stored / b;
            if (Double.isNaN(result) || Double.isInfinite(result)) input = "Error";
            else input = fmt.format(result);
            op = null;
            fresh = true;
            updateDisplay();
        } catch (Exception e) {
            input = "Error";
            op = null;
            fresh = true;
            updateDisplay();
        }
    }

    private void updateDisplay() {
        if (display != null) display.setText(input.isEmpty() ? "0" : input);
    }

    private void hideKeyboardAndOpenPrivate() {
        showPrivateApps();
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
        List<android.content.pm.ResolveInfo> infos = pm.queryIntentActivities(i, PackageManager.MATCH_ALL);
        List<ResolveInfoWrap> out = new ArrayList<>();
        for (android.content.pm.ResolveInfo ri : infos) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ME.equals(ai.packageName)) continue;
            CharSequence label = ri.loadLabel(pm);
            if (label == null || label.toString().trim().isEmpty()) continue;
            out.add(new ResolveInfoWrap(ai.packageName, label.toString(), ri.activityInfo.name));
        }
        Collections.sort(out, (a,b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    private static class ResolveInfoWrap {
        String pkg, label, activity;
        ResolveInfoWrap(String p, String l, String a) { pkg = p; label = l; activity = a; }
    }

    private void showPrivateApps() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = title("אזור פרטי");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button back = actionButton("מחשבון");
        back.setOnClickListener(v -> showCalculator());
        top.addView(back, new LinearLayout.LayoutParams(dp(105), dp(46)));
        body.addView(top);

        TextView info = label("האפליקציות שבחרת מסתתרות ממסך הבית של האפליקציה.", 14, MUTED);
        info.setGravity(Gravity.RIGHT);
        info.setPadding(dp(4), 0, dp(4), dp(8));
        body.addView(info, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(4), 0, dp(4));

        Set<String> hidden = hiddenSet();
        List<ResolveInfoWrap> apps = launchableApps();
        int count = 0;
        for (ResolveInfoWrap app : apps) {
            if (!hidden.contains(app.pkg)) continue;
            list.addView(privateRow(app), new LinearLayout.LayoutParams(-1, dp(68)));
            count++;
        }
        if (count == 0) {
            TextView empty = label("עדיין לא בחרת אפליקציות להסתרה", 16, MUTED);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(130)));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button manage = actionButton("ניהול אפליקציות מוסתרות");
        manage.setTextColor(Color.WHITE);
        manage.setBackground(bg(GREEN, 18));
        manage.setOnClickListener(v -> showManageApps());
        body.addView(manage, new LinearLayout.LayoutParams(-1, dp(54)));

        base(body);
    }

    private LinearLayout privateRow(ResolveInfoWrap app) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(10), 0);
        row.setBackground(bg(Color.WHITE, 18));

        TextView icon = label("▣", 25, GREEN);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView name = label(app.label, 16, TEXT);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        Button open = actionButton("פתיחה");
        open.setOnClickListener(v -> launchPackage(app));
        row.addView(open, new LinearLayout.LayoutParams(dp(86), dp(46)));
        return row;
    }

    private void showManageApps() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = title("בחירת אפליקציות");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button done = actionButton("סיום");
        done.setOnClickListener(v -> showPrivateApps());
        top.addView(done, new LinearLayout.LayoutParams(dp(85), dp(46)));
        body.addView(top);

        TextView info = label("סמן את האפליקציות שיופיעו באזור הפרטי ולא במגירת ה־Launcher של האפליקציה.", 14, MUTED);
        info.setGravity(Gravity.RIGHT);
        info.setPadding(dp(4), 0, dp(4), dp(8));
        body.addView(info, new LinearLayout.LayoutParams(-1, dp(55)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        Set<String> hidden = hiddenSet();

        for (ResolveInfoWrap app : launchableApps()) {
            list.addView(manageRow(app, hidden), new LinearLayout.LayoutParams(-1, dp(68)));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        base(body);
    }

    private LinearLayout manageRow(ResolveInfoWrap app, Set<String> hidden) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(10), 0);
        row.setBackground(bg(Color.WHITE, 18));

        TextView name = label(app.label, 16, TEXT);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

        CheckBox box = new CheckBox(this);
        box.setChecked(hidden.contains(app.pkg));
        box.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
        box.setOnCheckedChangeListener((b, checked) -> {
            Set<String> now = hiddenSet();
            if (checked) now.add(app.pkg); else now.remove(app.pkg);
            saveHidden(now);
        });
        row.addView(box, new LinearLayout.LayoutParams(dp(56), dp(56)));
        return row;
    }

    private void showLauncher() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = title("Home");
        top.addView(t, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button vault = actionButton("פרטי");
        vault.setOnClickListener(v -> showPrivateApps());
        top.addView(vault, new LinearLayout.LayoutParams(dp(85), dp(46)));
        body.addView(top);

        TextView hint = label("מגירת אפליקציות", 14, MUTED);
        hint.setGravity(Gravity.RIGHT);
        body.addView(hint, new LinearLayout.LayoutParams(-1, dp(34)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);

        Set<String> hidden = hiddenSet();
        for (ResolveInfoWrap app : launchableApps()) {
            if (hidden.contains(app.pkg)) continue;
            TextView tile = label(app.label, 13, TEXT);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(6), dp(8), dp(6), dp(8));
            tile.setBackground(bg(Color.WHITE, 18));
            tile.setClickable(true);
            tile.setOnClickListener(v -> launchPackage(app));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(92);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(tile, lp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(grid);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = label("האפליקציה יכולה להסתיר מתוך ה־Launcher שלה אפליקציות שסומנו כפרטיות.", 12, MUTED);
        footer.setGravity(Gravity.CENTER);
        body.addView(footer, new LinearLayout.LayoutParams(-1, dp(42)));

        base(body);
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
