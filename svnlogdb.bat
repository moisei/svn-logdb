@echo on
@setlocal
@call setjava > nul 2>&1
set cp=%~dp0hsqldb.jar;C:\Sources\vendor\hsqldb\hsqldb-2.3.2\lib\hsqldb.jar
start javaw -Xmx1024M -cp "%cp%" org.hsqldb.util.DatabaseManagerSwing -url "jdbc:hsqldb:%~dp0.svnlogDB\db" --user sa --script "%~dp0svnlogdb-sample-scripts.sql"
@endlocal
pause
