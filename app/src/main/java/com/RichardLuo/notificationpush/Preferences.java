package com.RichardLuo.notificationpush;

import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class Preferences extends PreferenceFragmentCompat {
    private SharedPreferences preferences;
    private SwitchPreference start;
    private EditTextPreference input;
    private SwitchPreference hide;
    private String token;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preference, null);
        preferences = getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));

        start = findPreference("start");
        Objects.requireNonNull(start).setOnPreferenceClickListener(new SwitchPreference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!Objects.requireNonNull(preferences.getString("input", "")).isEmpty()) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "请填写设备ID", Toast.LENGTH_SHORT).show();
                    start.setChecked(false);
                }
                return false;
            }
        });

        CheckBoxPreference startForeground = findPreference("startForeground");
        Objects.requireNonNull(startForeground).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (start.isChecked())
                    Toast.makeText(getActivity(), "请重启通知监听", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        input = findPreference("input");

        Preference logcat = findPreference("logcat");
        Objects.requireNonNull(logcat).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder log = new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext()));
                log.setTitle("Log");
                final ByteArrayOutputStream result = new ByteArrayOutputStream();
                try {
                    InputStream is = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "*:E"}).getInputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                } catch (IOException e) {
                    Log.e("error:", "read fail");
                    Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), "读取失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                log.setPositiveButton("复制", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((ClipboardManager) Objects.requireNonNull(Objects.requireNonNull(getActivity()).getSystemService(Context.CLIPBOARD_SERVICE))).setPrimaryClip(ClipData.newPlainText("token", result.toString()));
                        Toast.makeText(getActivity(), "复制成功", Toast.LENGTH_SHORT).show();
                    }
                });
                log.setMessage(result.toString());
                log.show();
                return true;
            }
        });

        hide = findPreference("hide");
        Objects.requireNonNull(hide).setOnPreferenceClickListener(new SwitchPreference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return false;
            }
        });

        Preference tokenPreference = findPreference("token");
        Objects.requireNonNull(tokenPreference).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final AlertDialog.Builder normalDialog = new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext()));
                normalDialog.setTitle("Token");
                normalDialog.setMessage(token);
                normalDialog.setPositiveButton("复制", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((ClipboardManager) Objects.requireNonNull(Objects.requireNonNull(getActivity()).getSystemService(Context.CLIPBOARD_SERVICE))).setPrimaryClip(ClipData.newPlainText("token", token));
                        Toast.makeText(getActivity(), "复制成功", Toast.LENGTH_SHORT).show();
                    }
                });
                normalDialog.setNeutralButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                normalDialog.show();
                return false;
            }
        });

        Preference LoginPreference = findPreference("Login");
        Objects.requireNonNull(LoginPreference).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(getContext(), QQLogin.class), 200);
                Toast.makeText(getActivity(), "登录成功后返回即可", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference clear = findPreference("clear");
        Objects.requireNonNull(clear).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final NotificationManager notificationManager = Objects.requireNonNull(getContext()).getSystemService(NotificationManager.class);
                    for (NotificationChannel channel : Objects.requireNonNull(notificationManager).getNotificationChannels()) {
                        notificationManager.deleteNotificationChannel(channel.getId());
                    }
                }
                Objects.requireNonNull(getContext()).getSharedPreferences("Channels", MODE_PRIVATE).edit().clear().apply();
                Toast.makeText(getActivity(), "已删除通知渠道", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            token = "fail";
                            if (getActivity() != null)
                                Toast.makeText(getActivity(), "获取token失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        token = Objects.requireNonNull(task.getResult()).getToken();
                    }
                });

        getListView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getListView().setFitsSystemWindows(true);
//
//        final ActionBar actionBar = Objects.requireNonNull(((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar());
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            getListView().setOnScrollChangeListener(new View.OnScrollChangeListener() {
//                boolean lastScrollinTop = true;
//                ValueAnimator animation = ValueAnimator.ofInt(0, 8);
//
//                {
//                    animation.setDuration(400);
//                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                        @Override
//                        public void onAnimationUpdate(ValueAnimator animation) {
//                            actionBar.setElevation((int) animation.getAnimatedValue());
//                        }
//                    });
//                }
//
//                @Override
//                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                    if (!v.canScrollVertically(-1) && !lastScrollinTop) {
//                        animation.reverse();
//                        lastScrollinTop = true;
//                    } else if (lastScrollinTop) {
//                        animation.start();
//                        lastScrollinTop = false;
//                    }
//                }
//            });
//        } else
//            actionBar.setElevation(8);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isNotificationListenersEnabled()) {
            input.setEnabled(false);
            start.setChecked(true);
        } else {
            input.setEnabled(true);
            start.setChecked(false);
        }
        if (isAccessibilitySettingsOn(Objects.requireNonNull(getActivity()))) {
            hide.setChecked(true);
        } else {
            hide.setChecked(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final String pskey;
        final String skey;
        final String uin;
        final String token;
        if (requestCode == 200) {
            if (resultCode == 0) {
                Toast.makeText(getActivity(), "未拿到Cookies", Toast.LENGTH_SHORT).show();
                return;
            }
            pskey = Objects.requireNonNull(data.getExtras()).getString("pskey");
            skey = Objects.requireNonNull(data.getExtras()).getString("skey");
            uin = Objects.requireNonNull(data.getExtras()).getString("uin");
            token = Objects.requireNonNull(data.getExtras()).getString("token");
            assert skey != null;
            final long bkn = GetBkn(skey);
            final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(Objects.requireNonNull(getContext()).getDatabasePath("friends.db"), null);

            AlertDialog.Builder listDialog = new MaterialAlertDialogBuilder(Objects.requireNonNull(getActivity()));
            listDialog.setTitle("你需要同步");
            listDialog.setItems(new String[]{"好友", "群组"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int click) {
                    switch (click) {
                        case 0:
                            Toast.makeText(getActivity(), "开始同步好友", Toast.LENGTH_SHORT).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        HttpURLConnection connection = connect(new URL("https://qun.qq.com/cgi-bin/qun_mgr/get_friend_list"), pskey, skey, uin, token);
                                        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                                        out.writeBytes("bkn=" + bkn);
                                        out.flush();
                                        out.close();
                                        connection.connect();

                                        if (connection.getResponseCode() == 200) {
                                            String json = parseResult(connection.getInputStream());
                                            if (json.length() < 100) throw new Exception();
                                            JSONObject friendslist = new JSONObject(json.substring(37, json.length() - 1));
                                            db.execSQL("drop table if exists friends");
                                            db.execSQL("create table friends(uin text primary key,name text)");
                                            for (int i = 0; i < friendslist.length(); i++) {
                                                JSONArray friends = friendslist.getJSONObject(String.valueOf(i)).getJSONArray("mems");
                                                for (int j = 0; j < friends.length(); j++) {
                                                    JSONObject friend = friends.getJSONObject(j);
                                                    db.execSQL("INSERT INTO friends VALUES (?, ?)", new Object[]{friend.getString("uin"), Html.fromHtml(friend.getString("name").replace("&nbsp;", "%20").replace("/", "%2f")).toString()});
                                                }
                                            }
                                        }
                                        connection.disconnect();
                                    } catch (Exception e) {
                                        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getContext(), "解析错误", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        e.printStackTrace();
                                    }
                                    db.close();
                                    if (getActivity() != null)
                                        getActivity().runOnUiThread(new Runnable() {
                                            public void run() {
                                                getActivity().getSharedPreferences("groups", MODE_PRIVATE).edit().putBoolean("sync_friends", true).apply();
                                                Toast.makeText(getContext(), "同步成功", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                }
                            }.start();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "开始同步群组", Toast.LENGTH_SHORT).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        HttpURLConnection getGroup = connect(new URL("https://qun.qq.com/cgi-bin/qun_mgr/get_group_list"), pskey, skey, uin, token);
                                        DataOutputStream getGroupOut = new DataOutputStream(getGroup.getOutputStream());
                                        getGroupOut.writeBytes("bkn=" + bkn);
                                        getGroupOut.flush();
                                        getGroupOut.close();
                                        getGroup.connect();
                                        if (getGroup.getResponseCode() == 200) {
                                            String json = parseResult(getGroup.getInputStream());
                                            getGroup.disconnect();
                                            if (json.length() < 100) throw new Exception();
                                            JSONObject allList = new JSONObject(json);
                                            String joinList = allList.getJSONArray("join").toString().replace("]", ",");
                                            String manageList = allList.getJSONArray("manage").toString().substring(1);
                                            final JSONArray groupsList = new JSONArray(joinList + manageList);
                                            final String[] groupNames = new String[groupsList.length()];
                                            for (int i = 0; i < groupsList.length(); i++) {
                                                JSONObject group = groupsList.getJSONObject(i);
                                                String groupName = Html.fromHtml(group.getString("gn").replace("&amp;", "&").replace("&nbsp;", " ")).toString();
                                                groupNames[i] = groupName;
                                                Objects.requireNonNull(getActivity()).getSharedPreferences("groupsNumber", MODE_PRIVATE).edit().putString(groupName, group.getString("gc")).apply();
                                            }
                                            if (getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        final ArrayList<Integer> choices = new ArrayList<>();
                                                        AlertDialog.Builder ChoiceDialog = new MaterialAlertDialogBuilder(getActivity());
                                                        ChoiceDialog.setTitle("选择要同步的群组");
                                                        ChoiceDialog.setMultiChoiceItems(groupNames, null, new DialogInterface.OnMultiChoiceClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                                if (isChecked)
                                                                    choices.add(which);
                                                                else
                                                                    choices.remove(Integer.valueOf(which));
                                                            }
                                                        });
                                                        ChoiceDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                if (choices.isEmpty())
                                                                    return;
                                                                final ProgressDialog waitingDialog = new ProgressDialog(getActivity());
                                                                waitingDialog.setTitle("同步中");
                                                                waitingDialog.setMessage(groupNames[choices.get(0)]);
                                                                waitingDialog.setIndeterminate(true);
                                                                waitingDialog.setCancelable(false);
                                                                waitingDialog.show();
                                                                new Thread() {
                                                                    @Override
                                                                    public void run() {
                                                                        for (final Integer choice : choices) {
                                                                            if (getActivity() != null)
                                                                                getActivity().runOnUiThread(new Runnable() {
                                                                                    public void run() {
                                                                                        waitingDialog.setMessage(groupNames[choice]);
                                                                                    }
                                                                                });
                                                                            final String name = groupNames[choice];
                                                                            try {
                                                                                int temp = 41;
                                                                                int last = 0;
                                                                                db.execSQL("drop table if exists '" + name + "'");
                                                                                db.execSQL("create table '" + name + "'(uin text primary key,name text)");
                                                                                getActivity().getSharedPreferences("groups", MODE_PRIVATE).edit().putBoolean(name, true).apply();
                                                                                while (temp >= 41) {
                                                                                    HttpURLConnection getMember = connect(new URL("https://qun.qq.com/cgi-bin/qun_mgr/search_group_members"), pskey, skey, uin, token);
                                                                                    DataOutputStream getMemberout = new DataOutputStream(getMember.getOutputStream());
                                                                                    getMemberout.writeBytes("st=" + last + "&end=" + (last + 40) + "&sort=0&bkn=" + bkn + "&gc=" + groupsList.getJSONObject(choice).getInt("gc"));
                                                                                    last += 41;
                                                                                    getMemberout.flush();
                                                                                    getMemberout.close();
                                                                                    getMember.connect();
                                                                                    if (getMember.getResponseCode() == 200) {
                                                                                        String groupjson = parseResult(getMember.getInputStream());
                                                                                        getMember.disconnect();
                                                                                        JSONObject all = new JSONObject(groupjson);
                                                                                        if (groupjson.length() < 40 || all.getString("em").contains("malicious"))
                                                                                            throw new IllegalArgumentException();
                                                                                        JSONArray members;
                                                                                        if (all.has("mems"))
                                                                                            members = all.getJSONArray("mems");
                                                                                        else
                                                                                            break;
                                                                                        temp = members.length();
                                                                                        for (int j = 0; j < temp; j++) {
                                                                                            JSONObject member = members.getJSONObject(j);
                                                                                            String card = member.getString("card");
                                                                                            db.execSQL("INSERT INTO '" + name + "' VALUES (?, ?)", new Object[]{member.getString("uin"), Html.fromHtml((card.equals("") ? member.getString("nick") : card).replace("&nbsp;", "%20").replace("/", "%2f")).toString()});
                                                                                        }
                                                                                    }
                                                                                    getMember.disconnect();
                                                                                    sleep(400);
                                                                                }
                                                                                if (getActivity() != null)
                                                                                    getActivity().runOnUiThread(new Runnable() {
                                                                                        public void run() {
                                                                                            Toast.makeText(getContext(), name + "同步成功", Toast.LENGTH_SHORT).show();
                                                                                        }
                                                                                    });
                                                                            } catch (IllegalArgumentException e) {
                                                                                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                                                                                    public void run() {
                                                                                        Toast.makeText(getContext(), name + "被反恶意机制拦截", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                                e.printStackTrace();
                                                                            } catch (SocketTimeoutException e) {
                                                                                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                                                                                    public void run() {
                                                                                        Toast.makeText(getContext(), name + "网络超时", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                                e.printStackTrace();
                                                                            } catch (Exception e) {
                                                                                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                                                                                    public void run() {
                                                                                        Toast.makeText(getContext(), name + "解析错误", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                                e.printStackTrace();
                                                                            }
                                                                            try {
                                                                                sleep(400);
                                                                            } catch (InterruptedException e1) {
                                                                                e1.printStackTrace();
                                                                            }
                                                                        }
                                                                        waitingDialog.dismiss();
                                                                        db.close();
                                                                    }
                                                                }.start();
                                                            }
                                                        });
                                                        ChoiceDialog.show();
                                                    }
                                                });
                                        }
                                        getGroup.disconnect();
                                        if (getActivity() != null)
                                            getActivity().runOnUiThread(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(getContext(), "列表加载成功", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    } catch (Exception e) {
                                        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getContext(), "解析错误", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                            break;
                    }
                }
            });
            listDialog.show();
        }
    }

    private HttpURLConnection connect(URL url, String pskey, String skey, String uin, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("cookie", "p_skey=" + pskey + ";p_uin=" + uin + ";pt4_token=" + token + ";uin=" + uin + ";skey=" + skey);
        return connection;
    }

    private String parseResult(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        inputStream.close();
        return result.toString("UTF-8");
    }

    private long GetBkn(String skey) {
        int t = 5381, n = 0;
        int o = skey.length();
        for (; n < o; ++n)
            t += (t << 5) + skey.charAt(n);
        return 2147483647 & t;
    }

    private boolean isNotificationListenersEnabled() {
        String pkgName = Objects.requireNonNull(getActivity()).getPackageName();
        final String flat = Settings.Secure.getString(getActivity().getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled;
        final String service = Objects.requireNonNull(getActivity()).getPackageName() + "/" + ForegroundMonitor.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
