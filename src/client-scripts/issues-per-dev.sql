select top 30
    author,
    year,
    week,
    count(distinct(issues.reference)) as cnt
from
    commits,
    issues,
    date_time
where
    issues.revision = commits.revision
    and
    date_time.revision = commits.revision
    and
    author = 'slahiani'
group by
    author,
    year,
    week
order by
   author,
   year,
   week

-- details 
select top 30
    author,
    year,
    week,
    count(distinct(issues.reference)) as cnt
from
    commits,
    issues,
    date_time
where
    issues.revision = commits.revision
    and
    date_time.revision = commits.revision
    and
    author = 'slahiani'
group by
    author,
    year,
    week
order by
   author,
   year,
   week
