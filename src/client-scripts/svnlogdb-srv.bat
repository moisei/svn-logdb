@echo on
@setlocal
@call %~dp0setjava.bat > nul 2>&1
set cp=%~dp0..\lib\hsqldb-2.4.0.jar
call java -cp -Xmx4024M -cp "%cp%" org.hsqldb.Server -database.0 "%~dp0.svnlogDB\db" -dbname.0 svnlogdb
@endlocal
