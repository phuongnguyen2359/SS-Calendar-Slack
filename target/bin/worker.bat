@REM ----------------------------------------------------------------------------
@REM Copyright 2001-2004 The Apache Software Foundation.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\repo

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\com\sparkjava\spark-template-freemarker\2.0.0\spark-template-freemarker-2.0.0.jar;"%REPO%"\org\slf4j\slf4j-api\1.7.2\slf4j-api-1.7.2.jar;"%REPO%"\com\sparkjava\spark-core\2.0.0\spark-core-2.0.0.jar;"%REPO%"\org\eclipse\jetty\jetty-server\9.0.2.v20130417\jetty-server-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\orbit\javax.servlet\3.0.0.v201112011016\javax.servlet-3.0.0.v201112011016.jar;"%REPO%"\org\eclipse\jetty\jetty-http\9.0.2.v20130417\jetty-http-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-util\9.0.2.v20130417\jetty-util-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-io\9.0.2.v20130417\jetty-io-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-webapp\9.0.2.v20130417\jetty-webapp-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-xml\9.0.2.v20130417\jetty-xml-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-servlet\9.0.2.v20130417\jetty-servlet-9.0.2.v20130417.jar;"%REPO%"\org\eclipse\jetty\jetty-security\9.0.2.v20130417\jetty-security-9.0.2.v20130417.jar;"%REPO%"\org\freemarker\freemarker\2.3.19\freemarker-2.3.19.jar;"%REPO%"\com\heroku\sdk\heroku-jdbc\0.1.1\heroku-jdbc-0.1.1.jar;"%REPO%"\javax\javaee-api\7.0\javaee-api-7.0.jar;"%REPO%"\com\sun\mail\javax.mail\1.5.0\javax.mail-1.5.0.jar;"%REPO%"\javax\activation\activation\1.1\activation-1.1.jar;"%REPO%"\com\google\code\gson\gson\2.3.1\gson-2.3.1.jar;"%REPO%"\org\glassfish\tyrus\bundles\tyrus-standalone-client\1.9\tyrus-standalone-client-1.9.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-databind\2.5.4\jackson-databind-2.5.4.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-annotations\2.5.0\jackson-annotations-2.5.0.jar;"%REPO%"\com\fasterxml\jackson\core\jackson-core\2.5.4\jackson-core-2.5.4.jar;"%REPO%"\mysql\mysql-connector-java\5.1.6\mysql-connector-java-5.1.6.jar;"%REPO%"\org\json\json\20090211\json-20090211.jar;"%REPO%"\org\example\booking\1.0-SNAPSHOT\booking-1.0-SNAPSHOT.jar
set EXTRA_JVM_ARGUMENTS=
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="worker" -Dapp.repo="%REPO%" -Dbasedir="%BASEDIR%" SSCalendar %CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
