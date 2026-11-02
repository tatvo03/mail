@echo off
title Gmail Client (Dang nhap Gmail that)

echo ==========================================================
echo   KHOI CHAY GMAIL CLIENT (DANG NHAP GMAIL THAT)
echo ==========================================================

if not exist bin\ClientMain.class (
    echo Chưa có bản biên dịch, đang tự động biên dịch...
    call build.bat
)

set CP=bin;lib\javax.mail-1.6.2.jar;lib\javax.activation-1.2.0.jar;lib\sqlite-jdbc-3.36.0.3.jar;lib\flatlaf-3.4.1.jar

echo Đang khởi chạy Giao diện Gmail Client...
java -cp "%CP%" ClientMain
pause
