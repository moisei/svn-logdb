@echo on
@setlocal
@call %~dp0setjava.bat > nul 2>&1
set cp=%~dp0hsqldb.jar;C:\Sources\vendor\hsqldb\hsqldb-2.3.2\lib\hsqldb.jar
start javaw -Xmx1024M -cp "%cp%" org.hsqldb.util.DatabaseManagerSwing -url "jdbc:hsqldb:hsql://127.0.0.1:9001/svnlogdb" --user sa --script "%~dp0svnlogdb-sample-scripts.sql"
@endlocal
