#!/usr/bin/env bash

sudo mysql -e "use mysql; SET PASSWORD FOR 'root'@'localhost' = PASSWORD('password');FLUSH PRIVILEGES;"
sudo mysql -ppassword -e "create database test_openmrs;"
sudo mysql -ppassword -e "CREATE USER 'test_user'@'localhost' IDENTIFIED BY 'password';"
sudo service mysql restart
psql -c "CREATE USER test_user WITH PASSWORD 'password' NOCREATEROLE SUPERUSER;" -U postgres -ppassword
psql -c "CREATE DATABASE test_analytics WITH OWNER test_user;" -U postgres -ppassword
psql -U test_user test_analytics -c "CREATE SCHEMA bahmni_mart_scdf;"
sudo service postgresql restart

exit "$?"
