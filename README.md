# JavaProjectUCI: Gamevault
## Description

This project is a Game management program for a video game store.  
Developed by **Quentin Baudet**, **Alexandre Baudin**, **Maxime Bézot** and **Nicolas Dupont**

## Features
- A link to your database of Video-games and Studios
- A GUI made with Swing to add, delete and update games in your digital stock
- Tools to search and sort your games

## How to use
- In MariaDb:
- - Create a database named gamevault_db using the SQL command ``CREATE DATABASE 'gamevault_db';``
- In your Java IDE:
- - Add mariadb-java-client-3.3.0.jar and jcalendar-1.4.jar to your project modules
- - change the **_DB_URL_**, **_USER_** and **_PASSWORD_** constants in GamevaultApp to match those of your database you just created
- - Run the main method and the GUI will appear