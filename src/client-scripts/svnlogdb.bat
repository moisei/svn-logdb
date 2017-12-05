@echo on
@setlocal
@call %~dp0setjava.bat > nul 2>&1
set cp=%~dp0..\lib\hsqldb-2.4.0.jar
start javaw -Xmx1024M -cp "%cp%" org.hsqldb.util.DatabaseManagerSwing -url "jdbc:hsqldb:%~dp0.svnlogDB\db" --user sa --script "%~dp0svnlogdb-sample-scripts.sql"
@endlocal
