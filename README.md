# svn-logdb
Import a log of svn repository to SQL database and create useful indexes.
Useful for staistic  analysis like:
  - commits per build vs hotfix per month
  - commits per specific build/hf per month 
  - commits, changed files, ratio, per build vs hotfix per month
  - distinct issues per hotfix vs build per month
  - distinct issues per branch per month
  - branches touched per week
  - etc. 
See sample queries in svnlogdb-sample-scripts.sql


NOTE: the project is very much oriented to Dalet svn repo structure



