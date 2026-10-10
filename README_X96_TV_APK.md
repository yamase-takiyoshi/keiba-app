# 競馬収集 X96 — Android TV APK ビルド

X96 X6 (Android 11) 用のアプリランチャーです。Termux の既存 `~/keiba_x96_ingest` を操作します。
このブランチはAPKビルド専用で、既存の公開競馬HTMLを編集しません。

## ビルド結果をダウンロード

GitHub Actions -> `Build X96 TV APK` -> 最新の成功した実行 -> `Keiba-X96-TV-APK` を開き、ZIP内の `app-debug.apk` を取り出します。
開発者用デバッグ署名のAPKです。Android 11への手動インストールが必要です。

## インストール前の設定

X96のTermuxで次を実行:

```bash
mkdir -p ~/.termux
printf '\nallow-external-apps = true\n' >> ~/.termux/termux.properties
termux-reload-settings
```

Android設定 -> アプリ -> 競馬収集 X96 -> 権限 -> 追加権限 -> Run commands in Termux environment を許可。

## 操作

▶ 収集開始: `cd ~/keiba_x96_ingest && sh start.sh`
● 状態確認: `sh status.sh`
■ 収集停止: `sh stop.sh`
✓ GitHub保存確認: `python collector.py verify`

このAPKはオッズを直接取得しません。既存のTermux収集機へ実行を依頼します。秘密のトークン、設定ファイル、GitHub認証情報をAPKに入れません。
