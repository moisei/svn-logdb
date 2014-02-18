@echo on
@setlocal
@call setjava > nul 2>&1
start javaw -Xmx1024M -cp "%~dp0hsqldb.jar" org.hsqldb.util.DatabaseManagerSwing -url "jdbc:hsqldb:%~dp0.svnlogDB\db" --user sa --script svnlogdb-sample-scripts.sql
@endlocal
pause
