SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for subscribe
-- ----------------------------
DROP TABLE IF EXISTS `subscribe`;
CREATE TABLE `subscribe`  (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `reg_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `user_id` bigint NOT NULL DEFAULT 0,
                              `voice_list_id` bigint NOT NULL,
                              `up_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `process_time` datetime NOT NULL DEFAULT current_timestamp(),
                              `update_time` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE CURRENT_TIMESTAMP,
                              `from_time` datetime NULL DEFAULT NULL,
                              `to_time` datetime NULL DEFAULT NULL,
                              `key_word` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `limit_sec` int NOT NULL DEFAULT 0,
                              `video_order` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PUB_NEW_FIRST_THEN_OLD',
                              `enable` tinyint NOT NULL DEFAULT 1,
                              `crack` tinyint NOT NULL DEFAULT 0,
                              `use_video_cover` tinyint NOT NULL DEFAULT 0,
                              `check_part` tinyint NOT NULL DEFAULT 0,
                              `priority` int NOT NULL DEFAULT 0,
                              `log` varchar(12192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                              `last_total_index` int NOT NULL DEFAULT -1,
                              `channel_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `up_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              `up_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 157 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for subscribe_reg
-- ----------------------------
DROP TABLE IF EXISTS `subscribe_reg`;
CREATE TABLE `subscribe_reg`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `subscribe_id` bigint NOT NULL DEFAULT 0,
                                  `regex` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `pos` int NOT NULL DEFAULT 0,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `subscribe_reg_pk_2`(`subscribe_id` ASC, `pos` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `username` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                             `password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                             `net_cookies` varchar(8192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `bili_cookies` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                             `visit_times` int NOT NULL DEFAULT 0,
                             `update_time` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE CURRENT_TIMESTAMP,
                             `is_admin` int NOT NULL DEFAULT 0,
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `sys_user_pk`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11133 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for upload_detail
-- ----------------------------
DROP TABLE IF EXISTS `upload_detail`;
CREATE TABLE `upload_detail`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `subscribe_id` bigint NOT NULL DEFAULT 0,
                                  `user_id` bigint NOT NULL DEFAULT 0,
                                  `upload_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `create_time` datetime(3) NOT NULL DEFAULT current_timestamp(3),
                                  `update_time` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                  `offset` decimal(10, 2) NOT NULL DEFAULT 0.00,
                                  `begin_sec` decimal(10, 2) NOT NULL DEFAULT 0.00,
                                  `end_sec` decimal(10, 2) NOT NULL DEFAULT 0.00,
                                  `voice_id` bigint NOT NULL DEFAULT 0,
                                  `voice_list_id` bigint NOT NULL DEFAULT 0,
                                  `privacy` tinyint NOT NULL DEFAULT 0,
                                  `bvid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `cid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `music_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WAIT',
                                  `music_retry_times` int NOT NULL DEFAULT 0,
                                  `use_video_cover` tinyint NOT NULL DEFAULT 0,
                                  `crack` tinyint NOT NULL DEFAULT 0,
                                  `priority` int NOT NULL DEFAULT 0,
                                  `instance_id` bigint NOT NULL DEFAULT 0,
                                  `log` varchar(8192) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                  `upload_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WAIT',
                                  `upload_retry_times` int NOT NULL DEFAULT 0,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `upload_detail_crack_index`(`crack` ASC) USING BTREE,
                                  INDEX `upload_detail_priority_index`(`priority` ASC) USING BTREE,
                                  INDEX `upload_detail_privacy_index`(`privacy` ASC) USING BTREE,
                                  INDEX `upload_detail_retry_times_index`(`music_retry_times` ASC) USING BTREE,
                                  INDEX `upload_detail_status_index`(`music_status` ASC) USING BTREE,
                                  INDEX `upload_detail_subscribe_id_index`(`subscribe_id` ASC) USING BTREE,
                                  INDEX `upload_detail_use_video_cover_index`(`use_video_cover` ASC) USING BTREE,
                                  INDEX `upload_detail_user_id_index`(`user_id` ASC) USING BTREE,
                                  INDEX `upload_detail_voice_list_id_index`(`voice_list_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49042 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_voicelist
-- ----------------------------
DROP TABLE IF EXISTS `user_voicelist`;
CREATE TABLE `user_voicelist`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `user_id` bigint NOT NULL,
                                   `voicelist_id` bigint NOT NULL,
                                   `voicelist_image` varchar(512) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
                                   `voicelist_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1066 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
