alter table sys_user
    add is_admin int default 0 not null;

alter table upload_detail
    add upload_retry_times int default 0 not null after retry_times;

alter table upload_detail
    add upload_status varchar(64) default 'WAIT' not null;

update upload_detail set upload_status = 'SUCCESS';

create table user_voicelist
(
    id              bigint auto_increment
        primary key,
    user_id         bigint                  not null,
    voicelist_id    bigint                  not null,
    voicelist_image varchar(512) default '' not null,
    voicelist_name  varchar(256) default '' not null
);

alter table subscribe
    change target_id up_id varchar(256) default '' not null;

alter table subscribe
    add channel_ids varchar(1024) default '' not null;

alter table subscribe
    add up_name varchar(256) default '' not null;

alter table subscribe
    drop column net_cover;

alter table subscribe
    add up_image varchar(512) default '' not null after up_name;

alter table upload_detail
    change status music_status varchar(64) default 'WAIT' not null;

alter table upload_detail
    change retry_times music_retry_times int default 0 not null after music_status;

alter table upload_detail
    modify upload_retry_times int default 0 not null after upload_status;

update upload_detail set music_status = 'ONLINE'

