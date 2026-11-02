@echo off
title Gmail Java Client and Server - SMTP and POP3

echo ==========================================================
echo   HE THONG MAIL CLIENT & SERVER (CHAY TREN NHIEU MAY)
echo ==========================================================
echo.
echo   [1] Khoi chay MAY CHU (Mail Server - SMTP :2525 & POP3 :1110)
echo   [2] Khoi chay MAY KHACH (Mail Client - Dang nhap Gmail that)
echo   [3] Khoi chay CA HAI tren may nay (Server + Client cung luc)
echo.
set /p opt="Nhap lua chon cua ban [1, 2, hoac 3] (Mac dinh: 3): "

if "%opt%"=="1" (
    call run_server.bat
    goto end
)
if "%opt%"=="2" (
    call run_client.bat
    goto end
)

if not exist bin\Main.class (
    echo Chưa có bản biên dịch, đang tự động biên dịch...
    call build.bat
)

set CP=bin;lib\javax.mail-1.6.2.jar;lib\javax.activation-1.2.0.jar;lib\sqlite-jdbc-3.36.0.3.jar;lib\flatlaf-3.4.1.jar
echo Dang khoi chay he thong (Server + Client)...
java -cp "%CP%" Main

:end
