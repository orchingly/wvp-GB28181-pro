CREATE TABLE `wvp_record_detection` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `app` varchar(255) DEFAULT NULL,
  `stream` varchar(255) DEFAULT NULL,
  `d_type` int DEFAULT NULL,
  `act_time` datetime DEFAULT NULL,
  `act_type` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
);
