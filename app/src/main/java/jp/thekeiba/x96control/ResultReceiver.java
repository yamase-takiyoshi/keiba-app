package jp.thekeiba.x96control;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
public class ResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        Bundle result=null;
        if(intent!=null && intent.getExtras()!=null){
            for(String key:intent.getExtras().keySet()){
                Object v=intent.getExtras().get(key);
                if(v instanceof Bundle){
                    result=(Bundle)v;
                    if(key.toLowerCase().contains("result"))break;
                }
            }
        }
        StringBuilder out=new StringBuilder("Termux 実行結果\n");
        if(result==null)out.append("応答を受信できませんでした。Termuxで sh status.sh を確認してください。");
        else {
            for(String key:result.keySet()){
                String k=key.toLowerCase();
                Object v=result.get(key);
                if(v==null)continue;
                if(k.contains("stdout")||k.contains("stderr")||k.contains("exit_code")||k.contains("errmsg"))
                    out.append(k).append(": ").append(String.valueOf(v)).append("\n");
            }
        }
        String s=out.toString();
        if(s.length()>8000)s=s.substring(0,8000)+"\n...表示を省略";
        context.getSharedPreferences("result",Context.MODE_PRIVATE).edit().putString("last",s).apply();
        MainActivity.present(s);
    }
}
