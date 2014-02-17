@setlocal
@call setjava > nul 2>&1
@set cp=hsqldb.jar;C:\Sources\vendor\hsqldb\hsqldb-2.3.2\lib\hsqldb.jar
start javaw -Xmx1024M -cp "%cp%" org.hsqldb.util.DatabaseManagerSwing -url jdbc:hsqldb:.svnlogDB\db --user sa
@endlocal
