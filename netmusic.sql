SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for subscribe
-- ----------------------------
DROP TABLE IF EXISTS `subscribe`;
CREATE TABLE `subscribe`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `remark`          varchar(256) NOT NULL DEFAULT '',
    `reg_name`        varchar(256) NOT NULL DEFAULT '',
    `user_id`         bigint       NOT NULL DEFAULT '0',
    `voice_list_id`   bigint       NOT NULL,
    `target_id`       varchar(256) NOT NULL DEFAULT '',
    `type`            varchar(128) NOT NULL DEFAULT '',
    `process_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `from_time`       datetime              DEFAULT NULL,
    `to_time`         datetime              DEFAULT NULL,
    `key_word`        varchar(256) NOT NULL DEFAULT '',
    `limit_sec`       int          NOT NULL DEFAULT '0',
    `video_order`     varchar(128)          DEFAULT 'PUB_NEW_FIRST_THEN_OLD',
    `net_cover`       varchar(256) NOT NULL DEFAULT '',
    `enable`          tinyint      NOT NULL DEFAULT '1',
    `crack`           tinyint      NOT NULL DEFAULT '0',
    `use_video_cover` tinyint      NOT NULL DEFAULT '0',
    `priority`        int          NOT NULL DEFAULT '0',
    `log`             varchar(8192)         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `subscribe_pk` (`user_id`, `target_id`, `key_word`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 60
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for subscribe_reg
-- ----------------------------
DROP TABLE IF EXISTS `subscribe_reg`;
CREATE TABLE `subscribe_reg`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `subscribe_id` bigint       NOT NULL DEFAULT '0',
    `regex`        varchar(512) NOT NULL DEFAULT '',
    `pos`          int          NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `subscribe_reg_pk_2` (`subscribe_id`, `pos`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`           bigint        NOT NULL AUTO_INCREMENT,
    `username`     varchar(256)  NOT NULL,
    `password`     varchar(256)  NOT NULL,
    `net_cookies`  varchar(8192) NOT NULL DEFAULT '',
    `bili_cookies` varchar(4096) NOT NULL DEFAULT '',
    `update_time`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `sys_user_pk` (`username`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 53
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table structure for upload_detail
-- ----------------------------
DROP TABLE IF EXISTS `upload_detail`;
CREATE TABLE `upload_detail`
(
    `id`              bigint         NOT NULL AUTO_INCREMENT,
    `subscribe_id`    bigint         NOT NULL DEFAULT '0',
    `user_id`         bigint         NOT NULL DEFAULT '0',
    `upload_name`     varchar(512)   NOT NULL DEFAULT '',
    `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `offset`          decimal(10, 2) NOT NULL DEFAULT '0.00',
    `begin_sec`       decimal(10, 2) NOT NULL DEFAULT '0.00',
    `end_sec`         decimal(10, 2) NOT NULL DEFAULT '0.00',
    `voice_id`        bigint         NOT NULL DEFAULT '0',
    `voice_list_id`   bigint         NOT NULL DEFAULT '0',
    `privacy`         tinyint        NOT NULL DEFAULT '0',
    `bvid`            varchar(256)   NOT NULL DEFAULT '',
    `cid`             varchar(256)   NOT NULL DEFAULT '',
    `title`           varchar(256)   NOT NULL DEFAULT '',
    `retry_times`     int            NOT NULL DEFAULT '0',
    `status`          varchar(64)    NOT NULL DEFAULT 'WAIT',
    `use_video_cover` tinyint        NOT NULL DEFAULT '0',
    `crack`           tinyint        NOT NULL DEFAULT '0',
    `priority`        int            NOT NULL DEFAULT '0',
    `instance_id`          bigint         NOT NULL DEFAULT '0',
    `log`             varchar(8192)  NOT NULL DEFAULT '',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 12745
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
