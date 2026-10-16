@echo off
setlocal
set "APP_HOME=%~dp0"
pushd "%APP_HOME%" >nul
java -cp "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
set "EXIT_CODE=%ERRORLEVEL%"
popd >nul
exit /b %EXIT_CODE%
