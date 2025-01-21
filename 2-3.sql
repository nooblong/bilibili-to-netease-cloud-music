alter table sys_user
    add is_admin int default 0 not null;

alter table upload_detail
    add upload_retry_times int default 0 not null after retry_times;

alter table upload_detail
    add upload_status varchar(64) default 'WAIT' not null;