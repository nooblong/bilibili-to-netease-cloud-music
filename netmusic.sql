SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for afd_order
-- ----------------------------
DROP TABLE IF EXISTS `afd_order`;
CREATE TABLE `afd_order` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `order_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                             `user_id` bigint NOT NULL,
                             `out_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                             `plan_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                             `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '订单描述',
                             `month` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '赞助月份',
                             `total_amount` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实付款金额，如有兑换码，则为0.00',
                             `show_amount` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示金额，如有折扣则为折扣前金额',
                             `status` int DEFAULT NULL,
                             `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
                             `redeem_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '兑换码ID',
                             `product_type` int DEFAULT NULL COMMENT '0表示常规方案 1表示售卖方案',
                             `create_time` datetime DEFAULT NULL,
                             `out_create_time` datetime DEFAULT NULL,
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE KEY `afd_order_pk` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for subscribe
-- ----------------------------
DROP TABLE IF EXISTS `subscribe`;
CREATE TABLE `subscribe` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `reg_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `user_id` bigint NOT NULL DEFAULT '0',
                             `voice_list_id` bigint NOT NULL,
                             `up_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `process_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `from_time` datetime DEFAULT NULL,
                             `to_time` datetime DEFAULT NULL,
                             `key_word` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `limit_sec` int NOT NULL DEFAULT '0',
                             `min_sec` int NOT NULL DEFAULT '0',
                             `video_order` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'PUB_NEW_FIRST_THEN_OLD',
                             `enable` tinyint NOT NULL DEFAULT '1',
                             `crack` tinyint NOT NULL DEFAULT '0',
                             `use_video_cover` tinyint NOT NULL DEFAULT '0',
                             `check_part` tinyint NOT NULL DEFAULT '0',
                             `priority` int NOT NULL DEFAULT '0',
                             `log` mediumtext COLLATE utf8mb4_general_ci,
                             `last_total_index` int NOT NULL DEFAULT '-1',
                             `channel_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `up_name` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `up_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `bitrate` int NOT NULL DEFAULT '320000',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=363 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `username` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                            `password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                            `net_cookies` varchar(8192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                            `bili_cookies` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                            `visit_times` int NOT NULL DEFAULT '0',
                            `visit_today` int NOT NULL DEFAULT '0',
                            `visit_today_times` int NOT NULL DEFAULT '0',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_admin` int NOT NULL DEFAULT '0',
                            `afd_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                            `total_pay` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                            `expire` datetime NOT NULL DEFAULT '2000-01-01 00:00:00',
                            `remaining` int NOT NULL DEFAULT '50',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE KEY `sys_user_pk` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11276 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for upload_detail
-- ----------------------------
DROP TABLE IF EXISTS `upload_detail`;
CREATE TABLE `upload_detail` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `subscribe_id` bigint NOT NULL DEFAULT '0',
                                 `user_id` bigint NOT NULL DEFAULT '0',
                                 `upload_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                 `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                 `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                 `offset` decimal(10,2) NOT NULL DEFAULT '0.00',
                                 `begin_sec` decimal(10,2) NOT NULL DEFAULT '0.00',
                                 `end_sec` decimal(10,2) NOT NULL DEFAULT '0.00',
                                 `voice_id` bigint NOT NULL DEFAULT '0',
                                 `voice_list_id` bigint NOT NULL DEFAULT '0',
                                 `privacy` tinyint NOT NULL DEFAULT '0',
                                 `bvid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                 `cid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                 `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                 `music_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WAIT',
                                 `music_retry_times` int NOT NULL DEFAULT '0',
                                 `use_video_cover` tinyint NOT NULL DEFAULT '0',
                                 `crack` tinyint NOT NULL DEFAULT '0',
                                 `priority` int NOT NULL DEFAULT '0',
                                 `instance_id` bigint NOT NULL DEFAULT '0',
                                 `log` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
                                 `upload_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WAIT',
                                 `upload_retry_times` int NOT NULL DEFAULT '0',
                                 `bitrate` int NOT NULL DEFAULT '320000',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 KEY `upload_detail_crack_index` (`crack`) USING BTREE,
                                 KEY `upload_detail_priority_index` (`priority`) USING BTREE,
                                 KEY `upload_detail_privacy_index` (`privacy`) USING BTREE,
                                 KEY `upload_detail_retry_times_index` (`music_retry_times`) USING BTREE,
                                 KEY `upload_detail_status_index` (`music_status`) USING BTREE,
                                 KEY `upload_detail_subscribe_id_index` (`subscribe_id`) USING BTREE,
                                 KEY `upload_detail_use_video_cover_index` (`use_video_cover`) USING BTREE,
                                 KEY `upload_detail_user_id_index` (`user_id`) USING BTREE,
                                 KEY `upload_detail_voice_list_id_index` (`voice_list_id`) USING BTREE,
                                 KEY `upload_detail_create_time_index` (`create_time`) USING BTREE,
                                 KEY `upload_detail_upload_status_index` (`upload_status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1000041731 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user_voicelist
-- ----------------------------
DROP TABLE IF EXISTS `user_voicelist`;
CREATE TABLE `user_voicelist` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `user_id` bigint NOT NULL,
                                  `voicelist_id` bigint NOT NULL,
                                  `voicelist_image` varchar(512) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
                                  `voicelist_name` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=43907 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
