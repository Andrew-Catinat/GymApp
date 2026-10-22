@echo off
rem Lance Maven avec le JDK portable du projet, sans rien ecrire hors du dossier GymApp :
rem  - JDK      : ..\.tools\jdk-25
rem  - Maven    : telecharge par le wrapper dans .\.m2\wrapper
rem  - Depots   : .\.m2\repository (voir .mvn\maven.config)
rem Exemples : mvnw-local test    |    mvnw-local spring-boot:run
setlocal
set "JAVA_HOME=%~dp0..\.tools\jdk-25"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_USER_HOME=%~dp0.m2"
rem Certificats : utilise le magasin Windows (antivirus/proxy qui inspecte le HTTPS)
set "MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT %MAVEN_OPTS%"
cd /d "%~dp0"
call "%~dp0mvnw.cmd" %*
endlocal
