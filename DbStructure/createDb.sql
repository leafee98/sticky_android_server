CREATE DATABASE db_sticky_android;

DROP TABLE IF EXISTS `sticky`;

CREATE TABLE `sticky` (
  `c_id` int(11) NOT NULL AUTO_INCREMENT,
  `c_modify` datetime NOT NULL,
  `c_overview` varchar(300) DEFAULT NULL,
  `c_full_text` mediumtext DEFAULT NULL,
  PRIMARY KEY (`c_id`)
);

CREATE TRIGGER t_modify_overview_insert
    BEFORE INSERT ON db_sticky_android.sticky FOR EACH ROW
        SET NEW.c_overview = SUBSTRING(NEW.c_full_text, 1, 300)

CREATE TRIGGER t_modify_overview_update
    BEFORE UPDATE ON db_sticky_android.sticky FOR EACH ROW
        SET NEW.c_overview = SUBSTRING(NEW.c_full_text, 1, 300);