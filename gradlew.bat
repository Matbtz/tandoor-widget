@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle and Java processes.
@rem For example, to pass "-Xms1024m -Xmx2048m" to Gradle, you can use one of the following options:
@rem
@rem 1. Add the following line to this file:
@rem
@rem    set DEFAULT_JVM_OPTS=-Xms1024m -Xmx2048m
@rem
@rem 2. Add the following line to your gradle.properties file:
@rem
@rem    org.gradle.jvmargs=-Xms1024m -Xmx2048m
@rem
@rem 3. Pass it as a command-line argument:
@rem
@rem    gradlew.bat -Dorg.gradle.jvmargs=-Xms1024m -Xmx2048m <task>
@rem
@rem For more information, see https://docs.gradle.org/current/userguide/build_environment.html
set DEFAULT_JVM_OPTS=

@rem Find the project root directory
set "APP_HOME=%~dp0"
:find_project_root
if exist "%APP_HOME%gradlew" goto project_root_found
if exist "%APP_HOME%gradlew.bat" goto project_root_found
cd ..
set "APP_HOME=%cd%"
goto find_project_root
:project_root_found

@rem Find the project root directory
set "PROJECT_ROOT_DIR=%APP_HOME%"
:find_project_root_dir
if exist "%PROJECT_ROOT_DIR%gradlew" goto project_root_dir_found
if exist "%PROJECT_ROOT_DIR%gradlew.bat" goto project_root_dir_found
cd ..
set "PROJECT_ROOT_DIR=%cd%"
goto find_project_root_dir
:project_root_dir_found

@rem Set the GRADLE_WRAPPER_JAR
set "GRADLE_WRAPPER_JAR=%PROJECT_ROOT_DIR%\gradle\wrapper\gradle-wrapper.jar"

@rem Set the GRADLE_WRAPPER_PROPERTIES
set "GRADLE_WRAPPER_PROPERTIES=%PROJECT_ROOT_DIR%\gradle\wrapper\gradle-wrapper.properties"

@rem Set the GRADLE_USER_HOME
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"

@rem Set the JAVA_HOME
if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVA_EXE%" (
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    goto :eof
)

@rem Collect all arguments for the java command
set "GRADLE_CMD_LINE_ARGS="
:collect_args
if "%~1"=="" goto done_collecting_args
set "GRADLE_CMD_LINE_ARGS=%GRADLE_CMD_LINE_ARGS% %1"
shift
goto collect_args
:done_collecting_args

@rem Start the wrapper
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%GRADLE_WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %GRADLE_CMD_LINE_ARGS%

:eof
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
