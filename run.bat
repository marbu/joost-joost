@echo off

rem Set the following keys to 1 if you want to get this script working
rem user settings HKEY_CURRENM_USER is also ok
rem HKEY_LOCAL_MACHINE\Software\Microsoft\Command Processor\EnableExtensions
rem HKEY_LOCAL_MACHINE\Software\Microsoft\Command Processor\DelayedExpansion
rem or open a new shell with the following command:
rem     cmd.exe /E:ON /V:ON

set LIB_DIR=lib
set CLASSPATH=classes
rem alternative: CLASSPATH=joost.jar

for %%f in (%LIB_DIR%\*.jar) do set CLASSPATH=!CLASSPATH!;%%f
@echo on
java -cp %CLASSPATH% net.sf.joost.Main %~1 %~2 %~3 %~4 %~5 %~6 %~7 %~8 %~9
