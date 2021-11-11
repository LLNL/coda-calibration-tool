#!/bin/bash
HOME="/opt/cct" USER="cct" LANG="en_US.UTF-8" /usr/bin/vncserver :1 -geometry 800x600 -depth 16 -SecurityTypes None &
HOME="/opt/cct" USER="cct" LANG="en_US.UTF-8" /usr/sbin/xrdp-sesman --nodaemon &
HOME="/opt/cct" USER="cct" LANG="en_US.UTF-8" /usr/sbin/xrdp --nodaemon &
/usr/bin/websockify --web=/usr/share/novnc/ 8080 localhost:5901 2> /dev/null &
export DISPLAY=:1.0

sleep 10
java -Djava.security.disableSystemPropertiesFile=true -jar /opt/cct/coda-calibration-standalone.jar