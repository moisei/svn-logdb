@echo on
@setlocal
@call %~dp0setjava.bat > nul 2>&1
@rem set JAVA_HOME=C:\Program Files (x86)\Java\jre7-64
@rem set path=%JAVA_HOME%\bin;%path%
set cp=%~dp0hsqldb.jar;C:\Sources\vendor\hsqldb\hsqldb-2.3.2\lib\hsqldb.jar
call java -version
call java -cp -Xmx4024M -cp "%cp%" org.hsqldb.Server -database.0 "%~dp0.svnlogDB\db" -dbname.0 svnlogdb
@endlocal
