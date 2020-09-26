# svn-logdb
Allow easy search and analysys of the SVN commits messages by importing the log into SQL database.

Useful examples of the possible statistical analysis:
  - commits per build vs hotfix per month
  - commits per specific build/hf per month 
  - commits, changed files, ratio, per build vs hotfix per month
  - distinct issues per hotfix vs build per month
  - distinct issues per branch per month
  - branches touched per week
  - etc.

More sample queries in the svnlogdb-sample-scripts.sql

**NOTE**: the project is very much oriented to Dalet svn repo structure
