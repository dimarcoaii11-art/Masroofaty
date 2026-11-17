package com.masroofaty.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    String[] sites = {"دمنهور", "برج العرب", "العامرية", "أبو العباس"};
    String[] accountants = {"محمد حسن", "مصطفي الليبي", "محمود الجبالي", "محمود الجبالي"};
    // Numbers converted to international Egypt format for WhatsApp.
    String[] phones = {"201008481312", "20108279475", "201034462200", "201034462200"};
    String[] categories = {"ضيافة", "انتقالات", "مشتريات", "أدوات ومهمات", "نثريات", "أخرى"};
    SharedPreferences prefs;
    LinearLayout historyBox;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("masroofaty", MODE_PRIVATE);
        buildScreen();
    }

    TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(16); t.setTextColor(0xff202124);
        t.setPadding(0, 14, 0, 6);
        return t;
    }

    EditText input(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextSize(16); e.setInputType(type);
        e.setSingleLine(type == 2);
        e.setPadding(18, 8, 18, 8);
        return e;
    }

    void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 32);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setBackgroundColor(0xfff6f7f9);
        scroll.addView(page);

        TextView title = label("مصروفاتي");
        title.setTextSize(28); title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER); title.setTextColor(0xff174a72);
        page.addView(title);
        TextView sub = label("تسجيل مصروفات المواقع وإرسالها للمحاسب");
        sub.setGravity(Gravity.CENTER); page.addView(sub);

        page.addView(label("الموقع"));
        Spinner siteSpinner = new Spinner(this);
        siteSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sites));
        page.addView(siteSpinner);

        page.addView(label("بند المصروف"));
        Spinner catSpinner = new Spinner(this);
        catSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        page.addView(catSpinner);

        page.addView(label("المبلغ بالجنيه"));
        EditText amount = input("مثال: 200", 8194);
        page.addView(amount);

        page.addView(label("بيان المصروف"));
        EditText details = input("اكتب تفاصيل المصروف", 1);
        page.addView(details);

        Button send = new Button(this);
        send.setText("تسجيل وتجهيز رسالة واتساب");
        page.addView(send);

        page.addView(label("سجل المصروفات على هذا الجهاز"));
        historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyBox);
        renderHistory();

        send.setOnClickListener(v -> {
            String raw = amount.getText().toString().trim().replace(',', '.');
            if (raw.isEmpty()) { amount.setError("أدخل المبلغ"); return; }
            double value;
            try { value = Double.parseDouble(raw); if (value <= 0) throw new Exception(); }
            catch (Exception ex) { amount.setError("أدخل مبلغاً صحيحاً"); return; }

            int idx = siteSpinner.getSelectedItemPosition();
            String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            String note = details.getText().toString().trim();
            String message = "مصروف جديد - تطبيق مصروفاتي\n"
                    + "رقم العملية: " + id + "\n"
                    + "الموقع: " + sites[idx] + "\n"
                    + "المحاسب: " + accountants[idx] + "\n"
                    + "البند: " + categories[catSpinner.getSelectedItemPosition()] + "\n"
                    + "المبلغ: " + String.format(Locale.US, "%.2f", value) + " جنيه\n"
                    + "البيان: " + (note.isEmpty() ? "—" : note) + "\n"
                    + "التاريخ: " + date + "\n\n"
                    + "يرجى مراجعة المصروف وتسجيله.\n"
                    + "حالة التأكيد عبر رابط المحاسب ستُفعّل بعد ربط خدمة التأكيد الإلكترونية.";

            try {
                JSONArray arr = new JSONArray(prefs.getString("items", "[]"));
                JSONObject item = new JSONObject();
                item.put("id", id); item.put("site", sites[idx]); item.put("category", categories[catSpinner.getSelectedItemPosition()]);
                item.put("amount", value); item.put("details", note); item.put("date", date); item.put("status", "في انتظار التسجيل");
                arr.put(item); prefs.edit().putString("items", arr.toString()).apply();
                renderHistory();
            } catch (Exception ignored) {}

            try {
                String url = "https://wa.me/" + phones[idx] + "?text=" + URLEncoder.encode(message, "UTF-8");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "تعذر فتح واتساب. تأكد من تثبيته.", Toast.LENGTH_LONG).show();
            }
        });
        setContentView(scroll);
    }

    void renderHistory() {
        if (historyBox == null) return;
        historyBox.removeAllViews();
        try {
            JSONArray arr = new JSONArray(prefs.getString("items", "[]"));
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                TextView row = label(o.optString("site") + " | " + o.optString("category")
                        + " | " + o.optString("amount") + " ج.م\n"
                        + o.optString("date") + " | " + o.optString("status")
                        + " | رقم: " + o.optString("id"));
                row.setBackgroundColor(0xffffffff);
                row.setPadding(14, 14, 14, 14);
                historyBox.addView(row);
            }
            if (arr.length() == 0) historyBox.addView(label("لا توجد مصروفات مسجلة بعد."));
        } catch (Exception ignored) {}
    }
}
