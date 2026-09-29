@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for AI-Powered Intelligent Workflow Agent
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_EXE="C:\Users\bhara\apache-maven-3.9.9\bin\mvn.cmd"
if exist %MAVEN_EXE% (
    %MAVEN_EXE% %*
) else (
    mvn %*
)
