-- commits per build vs hotfix per month
select branch, count(1) as commits_count, year,month+1 as mon
from date_time, version, commits
where date_time.revision = version.revision and commits.revision = version.revision
group by year,month,branch
order by year DESC, month DESC, branch

-- commits per specific build/hf per month 
select fullversion, count(1) as commits_count, year,month+1 as mon
from date_time, version, commits
where date_time.revision = version.revision and commits.revision = version.revision and (branch = 'builds' or  branch = 'hotfix')
group by year,month,fullversion
order by year DESC, month DESC, fullversion DESC

-- commits, changed files, ratio, per build vs hotfix per month
select branch, count(1) as cmts, SUM(changedfilescount) as cf, SUM(changedfilescount)/COUNT(1) fpc, year,month+1 as mon
from date_time, version, commits
where date_time.revision = version.revision and commits.revision = version.revision
group by year,month,branch
order by year DESC, month DESC, branch

-- distinct issues per hotfix vs build per month
select count(distinct(reference)) as issues, issues.type, branch, year, month+1 as month
from version, issues, date_time dt
where version.revision = issues.revision and dt.revision = version.revision and (branch = 'builds' or branch = 'hotfix')
group by branch, issues.type, year, month
order by year desc, month  desc, branch, issues.type

-- distinct issues per branch per month
select count(distinct(reference)) as issues, issues.type, fullversion, productversion, year, month+1 as month, count(distinct(author)) as authors
from version, issues, date_time dt, commits
where version.revision = issues.revision and dt.revision = version.revision and (branch = 'builds' or branch = 'hotfix') and commits.revision = version.revision
group by fullversion,productversion, issues.type, year, month
order by year desc, month  desc, fullversion, issues.type

select count(distinct(reference)) as issues, issues.type, fullversion, productversion, year, month+1 as month, count(distinct(author)) as authors
from version, issues, date_time dt, commits
where version.revision = issues.revision and dt.revision = version.revision and (branch = 'builds' or branch = 'hotfix') and commits.revision = version.revision
group by fullversion,productversion, issues.type, year, month
order by year desc, month  desc, fullversion, issues.type


-- branches touched per week
select count(distinct(fullversion))  as branches, year, week
from version, date_time
where version.revision = date_time.revision and (branch = 'builds' or branch = 'hotfix')
group by year, week
order by year desc, week desc

select count(distinct(fullversion))  as branches, count(distinct(author)) as authors, year, week
from version, date_time, commits
where version.revision = date_time.revision and (branch = 'builds' or branch = 'hotfix') and commits.revision = version.revision
group by year, week
order by year desc, week desc

------------------------------------------------------------------------------------------

select count(1) from date_time where year=2013

-- CREATE UNIQUE INDEX  version_revision ON version (revision)
CREATE UNIQUE INDEX  commits_revision ON commits (revision)
CREATE UNIQUE INDEX  commits_revision ON commits (revision)

CREATE  INDEX  datetime_year ON date_time  (year)


select count(1) from date_time.year, version where datetime_year.revision = version.revision
select count(1) from date_time, version where date_time.revision = version.revision
select count(1) from date_time where date_time.year = 2013
select count(1) from date_time, version where date_time.revision = version.revision and date_time.year = 2013

select count(1), year,week 
from date_time, version 
where date_time.revision = version.revision 
group by year,week  
order by year DESC,week DESC  

select author, count(1) as commits, year,month+1 
from date_time, version, commits
where date_time.revision = version.revision and commits.revision = version.revision
group by author,year,month  
order by year DESC, month DESC, author DESC

select fullversion, count(1) as commits_count, year,month+1 as mon
from date_time, version, commits
where date_time.revision = version.revision and commits.revision = version.revision and branch = 'hotfix'
group by year,month,fullversion
order by year DESC, month DESC, fullversion DESC
