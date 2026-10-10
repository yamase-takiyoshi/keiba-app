package jp.thekeiba.x96control;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PERMISSION="com.termux.permission.RUN_COMMAND";
    private static final String TERMUX_SHELL="/data/data/com.termux/files/usr/bin/bash";
    private static final String DIR="/data/data/com.termux/files/home/keiba_x96_ingest";
    private static final int REQ=2099;
    private TextView output;
    private int afterPermission=-1;
    private static MainActivity visible;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(5,19,31));
        getWindow().setNavigationBarColor(Color.rgb(5,19,31));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setPadding(24,20,24,20);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,21,34));
        scroll.addView(root);
        root.addView(text("競馬収集  X96 TV",30,0xff7cf6ae,true));
        root.addView(text("JRA / NAR  |  Android TV  |  Termux連携",16,0xffbbd1df,false));
        LinearLayout r1=new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(r1);
        button(r1,"▶ 収集開始",0xff076c46,0);
        button(r1,"● 状態確認",0xff2853a2,1);
        LinearLayout r2=new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(r2);
        button(r2,"■ 収集停止",0xff913d38,2);
        button(r2,"✓ GitHub保存確認",0xff84651b,3);
        root.addView(text("実行結果",20,Color.WHITE,true));
        output=text("操作を選択してください。\nTermuxの既存プログラムを呼び出します。",17,Color.WHITE,false);
        output.setTextIsSelectable(true);
        output.setPadding(18,18,18,18);
        output.setBackground(bg(0xff1a344a,12));
        root.addView(output,new LinearLayout.LayoutParams(-1,-2));
        Button help=new Button(this);
        help.setAllCaps(false);
        help.setText("⚙ 接続設定・使い方");
        help.setOnClickListener(v->help());
        root.addView(help);
        setContentView(scroll);
        r1.getChildAt(0).requestFocus();
    }
    private TextView text(String s,int sp,int color,boolean strong){
        TextView t=new TextView(this);
        t.setText(s);t.setTextSize(sp);t.setTextColor(color);
        t.setPadding(4,6,4,6);
        if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private GradientDrawable bg(int color,int radius){
        GradientDrawable b=new GradientDrawable();
        b.setColor(color);b.setCornerRadius(radius);return b;
    }
    private void button(LinearLayout row,String title,int color,int action){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(title);b.setTextSize(20);b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);b.setBackground(bg(color,14));b.setFocusable(true);
        b.setOnClickListener(v->run(action));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,88,1);
        p.setMargins(6,15,6,12);
        row.addView(b,p);
    }
    private void run(int action){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(PERMISSION)!=PackageManager.PERMISSION_GRANTED){
            afterPermission=action;
            output.setText("Termuxのコマンド実行権限を許可してください。");
            requestPermissions(new String[]{PERMISSION},REQ);
            return;
        }
        final String command;
        switch(action){
            case 0: command="sh start.sh";break;
            case 1: command="sh status.sh";break;
            case 2: command="sh stop.sh";break;
            case 3: command="python collector.py verify";break;
            default:return;
        }
        Intent i=new Intent("com.termux.RUN_COMMAND");
        i.setClassName("com.termux","com.termux.app.RunCommandService");
        i.putExtra("com.termux.RUN_COMMAND_PATH",TERMUX_SHELL);
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-c","cd "+DIR+" && "+command});
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR",DIR);
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND",true);
        Intent callback=new Intent(this,ResultReceiver.class);
        int flags=PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;
        PendingIntent pi=PendingIntent.getBroadcast(this,action+100,callback,flags);
        i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT",pi);
        try{
            getPackageManager().getPackageInfo("com.termux",0);
            if(startService(i)==null){
                output.setText("Termuxサービスが起動できません。権限とTermuxの設定を確認してください。");
                return;
            }
            output.setText("Termuxに実行を依頼しました。結果を待っています…");
        }catch(PackageManager.NameNotFoundException e){
            output.setText("Termuxが見つかりません。先にTermuxをインストールしてください。");
        }catch(Exception e){
            output.setText("実行エラー: "+e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=REQ)return;
        if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED && afterPermission>=0){
            int a=afterPermission;afterPermission=-1;run(a);
        }else output.setText("権限がありません。Androidのアプリ設定→追加権限で許可してください。");
    }
    private void help(){
        output.setText("Termuxで以下を一度だけ実行します。\n\nmkdir -p ~/.termux\nprintf '\\nallow-external-apps = true\\n' >> ~/.termux/termux.properties\ntermux-reload-settings\n\n次にAndroid設定→アプリ→競馬収集 X96→追加権限で「Run commands in Termux environment」を許可します。\n\n収集スクリプト: ~/keiba_x96_ingest\n\nこのアプリに秘密トークンは保存しません。");
    }
    static void present(String s){
        if(visible!=null && visible.output!=null)visible.output.setText(s);
    }
    @Override protected void onResume(){
        super.onResume();visible=this;
        String s=getSharedPreferences("result",MODE_PRIVATE).getString("last","");
        if(!s.isEmpty() && output!=null)output.setText(s);
    }
    @Override protected void onPause(){
        if(visible==this)visible=null;
        super.onPause();
    }
}
