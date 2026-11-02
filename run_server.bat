@echo off
title Mail Server Manager (SMTP :2525 and POP3 :1110)

echo ==========================================================
echo   KHOI CHAY MAY CHU MAIL SERVER - SMTP & POP3
echo ==========================================================

if not exist bin\ServerMain.class (
    echo Chưa có bản biên dịch, đang tự động biên dịch...
    call build.bat
)

set CP=bin;lib\javax.mail-1.6.2.jar;lib\javax.activation-1.2.0.jar;lib\sqlite-jdbc-3.36.0.3.jar;lib\flatlaf-3.4.1.jar

echo Đang khởi chạy Máy chủ Mail Server...
java -cp "%CP%" ServerMain
pause
