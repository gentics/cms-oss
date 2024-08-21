CREATE DATABASE IF NOT EXISTS `keycloak`;
GRANT ALL privileges ON keycloak.* TO 'keycloak'@'%' IDENTIFIED BY 'keycloak';
FLUSH PRIVILEGES;
