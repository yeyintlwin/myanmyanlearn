-- MySQL dump 10.13  Distrib 9.5.0, for macos26.0 (arm64)
--
-- Host: localhost    Database: user_directory
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `assessment_scores`
--

DROP TABLE IF EXISTS `assessment_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessment_scores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `score_json` longtext NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assessment_scores_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assessment_scores`
--

LOCK TABLES `assessment_scores` WRITE;
/*!40000 ALTER TABLE `assessment_scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `assessment_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_attempts`
--

DROP TABLE IF EXISTS `login_attempts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_attempts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `attempt_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `success` tinyint(1) DEFAULT '0',
  `attempt_count` int DEFAULT '1',
  `last_attempt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_ip` (`ip_address`),
  KEY `idx_time` (`attempt_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_attempts`
--

LOCK TABLES `login_attempts` WRITE;
/*!40000 ALTER TABLE `login_attempts` DISABLE KEYS */;
/*!40000 ALTER TABLE `login_attempts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `user_id` varchar(50) NOT NULL,
  `pw` varchar(68) NOT NULL,
  `active` bit(1) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `profile_image` varchar(500) DEFAULT NULL,
  `email_verified` tinyint(1) DEFAULT '0',
  `otp_code` varchar(6) DEFAULT NULL,
  `otp_expires_at` timestamp NULL DEFAULT NULL,
  `current_class` varchar(100) DEFAULT NULL,
  `school_year` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES ('2480012','$2a$10$pn0n5zY7yes.EDMaDvToqOY0h6zpWPCYFt/ObYe6hR0gAl1ZE/0BC',_binary '','イェ','インルイン','2480012@i-seifu.jp',NULL,1,NULL,NULL,'H','2'),('aeiou','$2a$10$AO4i8tlJ97sK8P9H3KAKaeQh6Mo.arTvN6kAykENhFF2GBku/0t1.',_binary '\0','X','Xin','aeiou@abcd.com',NULL,0,'772421','2026-01-26 09:15:38','H','2'),('yeyintlwin','$2a$10$RPfP5yFK7LsiCcBJ6Muwd.QmwA3fe9x90s//dAsqEdFwb3rZ9Kfe2',_binary '','Ye','Yint Lwin','yeyintlwin.dev@gmail.com',NULL,1,NULL,NULL,NULL,NULL),('yeyintlwin.jp','$2a$10$lN1SJgqkS96sVjY4Fv53HOkdplLt2tiEWVXwcphhNMI7qhvIswRHK',_binary '','ရဲ','ရင့်လွင်','yeyintlwin.jp@gmail.com',NULL,1,NULL,NULL,NULL,NULL),('yeyintlwin.mm','$2a$10$XinRb8sp4M/qkxQ0QcqZH.9uzmvRb3R/GM4FESWQDYMbJBWq5fUt.',_binary '\0','Ye','Yint Lwin MM','yeyintlwin.mm@gmail.com',NULL,1,NULL,NULL,NULL,NULL),('yeyintlwin73','$2a$10$yeS4O1szLjUWoJFeY4dhzeTzVy4Feg4VlJUYFw/eTeeJ1muB8khq6',_binary '','Ye','Yint Lwin 73','yeyintlwin73@gmail.com',NULL,1,NULL,NULL,'A',NULL),('yeyintlwinx','$2a$10$G8gnZewVviT0uubdQINqJeyKFdGuIztQ1oJ.B5gLLLNVp9TRUYhaK',_binary '\0','Ye','Yint','yeyintlwin@i-seifu.jp',NULL,0,'970951','2026-01-26 09:59:14','H','2');
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `otp_verifications`
--

DROP TABLE IF EXISTS `otp_verifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `otp_verifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL,
  `email` varchar(255) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  `verified` tinyint(1) DEFAULT '0',
  `verified_at` timestamp NULL DEFAULT NULL,
  `attempts` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `otp_verifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `members` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `otp_verifications`
--

LOCK TABLES `otp_verifications` WRITE;
/*!40000 ALTER TABLE `otp_verifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `otp_verifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_token`
--

DROP TABLE IF EXISTS `password_reset_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `expire_time` datetime(6) NOT NULL,
  `token` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `idx_password_reset_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_token`
--

LOCK TABLES `password_reset_token` WRITE;
/*!40000 ALTER TABLE `password_reset_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registration_settings`
--

DROP TABLE IF EXISTS `registration_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_settings` (
  `id` tinyint NOT NULL,
  `allowed_domain` varchar(255) DEFAULT NULL,
  `enforce_domain` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration_settings`
--

LOCK TABLES `registration_settings` WRITE;
/*!40000 ALTER TABLE `registration_settings` DISABLE KEYS */;
INSERT INTO `registration_settings` VALUES (1,'i-seifu.jp',0);
/*!40000 ALTER TABLE `registration_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `user_id` varchar(50) NOT NULL,
  `role` varchar(50) NOT NULL,
  UNIQUE KEY `authorities5_idx_1` (`user_id`,`role`),
  CONSTRAINT `authorities5_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `members` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('2480012','ROLE_STUDENT'),('aeiou','ROLE_STUDENT'),('yeyintlwin','ROLE_ADMIN'),('yeyintlwin.jp','ROLE_TEACHER'),('yeyintlwin.mm','ROLE_TEACHER'),('yeyintlwin73','ROLE_STUDENT'),('yeyintlwinx','ROLE_STUDENT');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'user_directory'
--

--
-- Dumping routines for database 'user_directory'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-26 19:52:47
