-- V6 : phone_number INT -> BIGINT (10-digit mobile numbers overflow INT)
ALTER TABLE `user` MODIFY phone_number BIGINT;